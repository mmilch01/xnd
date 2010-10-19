package org.nrg.xnd.rules.dicom;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.dcm4che2.data.DicomObject;
import org.dom4j.Element;
import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TreeIterator;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.rules.CollectionRule;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.utils.MilliTimer;

public class DICOMRule extends Rule
{
	private Collection<TagMap> m_tagMaps;
	private boolean m_bGenerCols = true;

	public DICOMRule(String uid, RepositoryViewManager rvm)
	{
		super(rvm, uid);
	}

	public void setGenerateCollections(boolean bGen)
	{
		m_bGenerCols = bGen;
	}
	public DICOMRule(RepositoryViewManager rvm, boolean bGenCol)
	{
		super(rvm);
		m_bGenerCols = bGenCol;
	}
	/*
	 * // public DICOMRule(RepositoryViewManager rvm) // { // super(rvm); // }
	 * 
	 * public static DICOMRule GetInstance(RepositoryViewManager rvm, boolean
	 * bGenCol) { return new DICOMRule(rvm,bGenCol); }
	 */
	@Override
	public boolean ParseRuleDescriptor()
	{
		try
		{
			m_tagMaps = new LinkedList<TagMap>();
			Element root = m_xmldoc.getRootElement();
			try
			{
				m_bGenerCols = Integer.getInteger(root
						.attributeValue("generate_collections")) == 1;
			} catch (Exception e)
			{
				m_bGenerCols = true;
			}
			Element el;
			String val;
			for (Iterator<Element> it = root.elementIterator(); it.hasNext();)
			{
				el = it.next();
				if (el.getName().compareTo("tag") == 0)
				{
					m_tagMaps.add(new TagMap(el));
				}
			}
			return true;
		} catch (Exception e)
		{
			return false;
		}
	}
	private boolean MakeTags(DicomObject o, ItemRecord ir)
	{
		ItemTag t;
		for (final TagMap tm : m_tagMaps)
		{
			try
			{
				if ((t = tm.Extract(o)) == null)
					continue;
				m_rvm.DBTagAttach(ir, t);
			} catch (Exception e)
			{
				;
			}
		}
		// if(ir.GetTagValue("Project")==null)
		// m_rvm.DBTagAttach(ir, new ItemTag("Project","default"));
		return true;
	}
	@Override
	public boolean ApplyRule(Collection<CElement> cce, IProgressMonitor ipm)
	{
		if (ipm != null && ipm.isCanceled())
			return false;
		MilliTimer mt = new MilliTimer(ipm);
		ItemRecord ir;
		TreeIterator ti = new TreeIterator(cce, new TypeFilter(
				TypeFilter.RESOURCE, true));
		CElement cel;
		String fold = null;
		String this_fold = null;
		DBElement dbe;

		LinkedList<CElement> lldbe = new LinkedList<CElement>();
		boolean bUseCollections = m_bGenerCols;
		// XNDApp.app_Prefs.getBoolean("DICOMRuleUseCollections", true);

		while ((cel = ti.Next()) != null)
		{
			if (!mt.Check(null, cel.GetLabel()))
				return false;
			if (cel instanceof DBElement)
			{
				dbe = (DBElement) cel;
				ir = dbe.GetIR();
				final DicomObject o = DICOMReader.read(ir);
				if (o == null)
					continue;
				MakeTags(o, ir);
				this_fold = m_rvm.GetParentPath(dbe.GetIR().getRelativePath());

				if (fold == null)
					fold = this_fold;
				if (bUseCollections)
				{
					if (fold.compareTo(this_fold) != 0) // next folder, first
					// collect the previous.
					{
						CollectionRule.DefaultCollectionRule(m_rvm).ApplyRule(
								lldbe, ipm);
						fold = null;
						lldbe.clear();
					}
					lldbe.add(dbe);
				}
			}
			cel.Invalidate();
		}
		// for last collection, apply collection rule.
		if (bUseCollections && fold != null)
		{
			CollectionRule.DefaultCollectionRule(m_rvm).ApplyRule(lldbe, ipm);
			fold = null;
			lldbe.clear();
		}
		return true;
	}
	@Override
	public boolean GetUnaffectedRecords(Collection c)
	{
		return false;
	}
	private static class TagMap
	{
		public String m_destTag;
		private String m_defVal;
		private SimpleTagExtractor[] m_extractors;
		public TagMap(Element el)
		{
			m_destTag = el.attributeValue("name");
			m_defVal = el.attributeValue("defaultValue");
			Element dcm_tag;
			String tag;
			Collection<SimpleTagExtractor> extractors = new LinkedList<SimpleTagExtractor>();
			int group, element;
			for (Iterator<Element> it = el.elementIterator(); it.hasNext();)
			{
				dcm_tag = it.next();
				group = Integer.valueOf(dcm_tag.attributeValue("group"), 16)
						.intValue();
				element = Integer
						.valueOf(dcm_tag.attributeValue("element"), 16)
						.intValue();
				tag = String.format("(0x%1$04x,0x%2$04x)", group, element);
				extractors.add(new SimpleTagExtractor(tag, (group << 16)
						| element));
			}
			m_extractors = extractors.toArray(new SimpleTagExtractor[0]);
		}
		public TagMap(String dest, String def_val, int... src)
		{
			m_destTag = dest;
			m_extractors = new SimpleTagExtractor[src.length];
			String tag;
			for (int i = 0; i < m_extractors.length; i++)
			{
				tag = String.format("(0x%1$04x,0x%2$04x)", src[i] >> 16,
						src[i] & 0xffff);
				m_extractors[i] = new SimpleTagExtractor(tag, src[i]);
			}
			m_defVal = def_val;
		}
		public ItemTag Extract(DicomObject o)
		{
			String res;
			for (int i = 0; i < m_extractors.length; i++)
			{
				if ((res = m_extractors[i].GetFirstValue(o)) != null
						&& res.length() > 0)
					return new ItemTag(m_destTag, res);
			}
			if (m_defVal == null)
				return null;
			return new ItemTag(m_destTag, m_defVal);
		}
	}
}