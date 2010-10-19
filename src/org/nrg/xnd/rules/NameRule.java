package org.nrg.xnd.rules;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Element;
import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.ConsoleView;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.utils.FSObject;

public class NameRule extends Rule
{
	private TreeMap<String, FileFolderTemplate> m_templ = new TreeMap<String, FileFolderTemplate>();
	private IProgressMonitor m_ipm = null;

	public NameRule(RepositoryViewManager rvm, String uid)
	{
		super(rvm, uid);
	}
	@Override
	public boolean ApplyRule(Collection<CElement> cce, IProgressMonitor ipm)
	{
		boolean bRes = false;
		for (CElement ce : cce)
		{
			if (ce instanceof FSFolder)
				bRes |= ApplyRule((FSFolder) ce, ipm);
		}
		return bRes;
	}
	private boolean ApplyRule(FSFolder fsf, IProgressMonitor ipm)
	{
		boolean bRes = true;
		m_ipm = ipm;
		if (ipm != null)
			ipm.subTask(fsf.GetFSObject().getName());
		for (final FileFolderTemplate fft : m_templ.values())
		{
			if (!fft.IsRoot())
				continue;
			if (fft.Match(fsf.GetFSObject().getName()))
			{
				if (ipm != null)
				{
					if (ipm.isCanceled())
						return false;
					ipm.subTask(fsf.GetFSObject().getName());
				}
				bRes &= ApplyToSubfolders(fsf, fft,
						new LinkedList<DerivedTagRule>());
			}
		}
		return bRes;
	}
	public boolean ApplyToSubfolders(FSFolder fsf, FileFolderTemplate fft,
			Collection<DerivedTagRule> rRules)
	{
		FSObject fso = fsf.GetFSObject();
		if (m_ipm != null)
		{
			if (m_ipm.isCanceled())
				return false;
			m_ipm.subTask(fso.getAbsolutePath());
		}
		Collection<CElement> cfo = fsf.GetChildren(new TypeFilter(), null);
		Collection<FSFolder> folis = new LinkedList<FSFolder>();
		Collection<DBElement> filis = new LinkedList<DBElement>();

		for (CElement fo : cfo)
		{
			if (fo instanceof DBElement)
			{
				filis.add((DBElement) fo);
			} else if (fo instanceof FSFolder)
			{
				folis.add((FSFolder) fo);
			}
		}
		// File[] files=filis.toArray(new File[0]);
		// fi.GetFile().listFiles();
		// FSFolder[] fiChildren=folis.toArray(new FSFolder[0]);
		// fi.GetChildren();

		if (filis.size() < 1 && folis.size() < 1)
			return true;
		// if(files==null && fiChildren==null) return true;

		FileFolderTemplate[] templateFileChildren = fft.GetChildren(true);
		FileFolderTemplate[] templateFolderChildren = fft.GetChildren(false);
		DerivedTagRule[] rules = fft.GetRules();

		boolean bRes = true;
		String val;

		// process files first
		// ItemRecord[] found;

		// First, generate tags which are based on folder name (and therefore
		// are applicable to all files)
		LinkedList<ItemTag> tags = new LinkedList<ItemTag>();
		String fname = fso.getName();

		// Find tags for this folder's rules
		for (int i = 0; i < rules.length; i++)
		{
			if ((val = rules[i].GetMatchingValue(fname)) != null)
				tags.add(new ItemTag(rules[i].GetTagName(), val));
		}
		// Apply recursive folder level rules
		// DerivedTagRule dtr;
		// for(Iterator<DerivedTagRule> idtr=rRules.iterator();idtr.hasNext();)
		for (DerivedTagRule dtr : rRules)
		{
			// dtr=idtr.next();
			if (dtr.IsFolderRule())
			{
				// the folder rule has to match corresponding folder in one of
				// two ways
				if (dtr.GetRecursionType() == DerivedTagRule.RECURSIVE_FIXED
						|| dtr.GetFFT().Match(fso.getName()))
				{
					if ((val = dtr.GetMatchingValue(fname)) != null)
						tags.add(new ItemTag(dtr.GetTagName(), val));
				}
			}
		}

		// ItemTag[] fold_tags=tags.toArray(new ItemTag[0]);
		DerivedTagRule[] fileRules;

		boolean bMatch;
		// main file processing cycle
		for (DBElement dbe : filis)
		{
			bMatch = false;
			fname = dbe.GetIR().getFileName();
			// process current file-level rules.
			for (FileFolderTemplate fftc : templateFileChildren)
			{
				if (fftc.Match(fname))
				{
					fileRules = fftc.GetRules();
					for (DerivedTagRule dtrf : fileRules)
					{
						if (dtrf.IsFolderRule())
							continue;
						if ((val = dtrf.GetMatchingValue(fname)) != null)
							bRes &= m_rvm.DBTagAttach(dbe.GetIR(), new ItemTag(
									dtrf.GetTagName(), val));
					}
					bMatch = true;
				}
			}
			if (!bMatch)
				continue;
			// attach pre-calculated tags based on folder name
			for (ItemTag it : tags)
			{
				bRes &= m_rvm.DBTagAttach(dbe.GetIR(), it);
			}

			// process recursive file-level rules.
			for (DerivedTagRule dtr : rRules)
			{
				if (!dtr.IsFolderRule() && dtr.GetFFT().Match(fname))
				{
					if ((val = dtr.GetMatchingValue(fname)) != null)
						bRes &= m_rvm.DBTagAttach(dbe.GetIR(), new ItemTag(dtr
								.GetTagName(), val));
				}
			}
			/*
			 * for(Iterator<DerivedTagRule>
			 * idtr=rRules.iterator();idtr.hasNext();) { dtr=idtr.next();
			 * if(!dtr.IsFolderRule() && dtr.GetFFT().Match(fname)) {
			 * if((val=dtr.GetMatchingValue(fname))!=null) bRes &=
			 * m_rvm.DBTagAttach(found[0], new ItemTag(dtr.GetTagName(),val)); }
			 * }
			 */
		}// end of file processing cycle

		// Update recursive rule list
		LinkedList<DerivedTagRule> newRecRules = new LinkedList<DerivedTagRule>(
				rRules);
		for (DerivedTagRule dtr : rules)
		// for(int i=0; i<rules.length; i++)
		{
			if (dtr.GetRecursionType() == DerivedTagRule.RECURSIVE_NONE)
				continue;
			if (dtr.GetRecursionType() == DerivedTagRule.RECURSIVE_FIXED) // based
			// on
			// current
			// folder's
			// name
			{
				// fix the tag value and add modified rule
				dtr = new DerivedTagRule(dtr);
				dtr.SetValue(dtr.GetMatchingValue(fso.getName()));
				newRecRules.add(dtr);
			}
			// add unmodified rule
			else
				newRecRules.add(dtr);
		}

		// main folder processing cycle - using recursion
		for (FSFolder fsfc : folis)
		{
			for (FileFolderTemplate fftc : templateFolderChildren)
			{
				if (fftc.Match(fsfc.GetFSObject().getName()))
				{
					if (m_ipm != null)
					{
						if (m_ipm.isCanceled())
							return false;
					}
					bRes &= ApplyToSubfolders(fsfc, fftc, newRecRules);
				}
			}
		}
		/*
		 * for(int i=0; i<fiChildren.length; i++) { for (int j=0; j <
		 * templateFolderChildren.length; j++) {
		 * if(templateFolderChildren[j].Match
		 * (fiChildren[i].GetFSObject().getName())) { if(m_ipm!=null) {
		 * if(m_ipm.isCanceled()) return false; }
		 * 
		 * bRes &=
		 * ApplyToSubfolders(fiChildren[i],templateFolderChildren[j],newRecRules
		 * ); } } }
		 */
		return bRes;
	}
	@Override
	public boolean ParseRuleDescriptor()
	{
		try
		{
			Element el;
			String id;
			for (Iterator<Element> it = m_xmldoc.getRootElement()
					.elementIterator(); it.hasNext();)
			{
				el = it.next();
				if (el.getName().compareTo("folder") == 0
						|| el.getName().compareTo("file") == 0)
				{
					m_templ.put(el.attributeValue("ID"),
							new FileFolderTemplate(el));
				}
			}
			for (Iterator<Map.Entry<String, FileFolderTemplate>> ime = m_templ
					.entrySet().iterator(); ime.hasNext();)
			{
				ime.next().getValue().UpdateChildren(m_templ);
			}
			return true;
		} catch (Exception e)
		{
			ConsoleView.AppendMessage("Naming rule parcer exception: "
					+ e.getMessage());
			return false;
		}

	}
	private static class DerivedTagRule
	{
		private String m_TagName;
		private String m_match;
		private boolean m_bValue = true;
		private boolean m_bFolderRule = false;
		private Pattern m_pattern;
		public static final byte RECURSIVE_NONE = 0, RECURSIVE_FIXED = 1,
				RECURSIVE_PATTERN = 2;
		private byte m_RecursiveType = RECURSIVE_NONE;
		FileFolderTemplate m_fft;

		public DerivedTagRule(DerivedTagRule dtr)
		{
			m_TagName = dtr.m_TagName;
			m_match = dtr.m_match;
			m_bValue = dtr.m_bValue;
			m_bFolderRule = dtr.m_bFolderRule;
			m_pattern = dtr.m_pattern;
			m_RecursiveType = dtr.m_RecursiveType;
			m_fft = dtr.m_fft;
		}
		public DerivedTagRule(Element el, FileFolderTemplate fft,
				boolean bFolderRule)
		{
			m_fft = fft;
			m_TagName = el.attributeValue("name");
			m_bFolderRule = bFolderRule;
			if (el.attributeValue("value") != null)
			{
				m_bValue = true;
				m_match = el.attributeValue("value");
			} else if (el.attributeValue("pattern") != null)
			{
				m_match = el.attributeValue("pattern");
				m_pattern = Pattern.compile(m_match);
				m_bValue = false;
			}
			String tmp = el.attributeValue("recursive");
			if (tmp == null || tmp.compareTo("none") == 0)
				m_RecursiveType = RECURSIVE_NONE;
			else if (tmp.compareTo("fixed") == 0)
				m_RecursiveType = RECURSIVE_FIXED;
			else if (tmp.compareTo("pattern") == 0)
				m_RecursiveType = RECURSIVE_PATTERN;
		}
		public String GetMatchingValue(String src)
		{
			if (m_bValue)
				return m_match;
			Matcher matcher = m_pattern.matcher(src);
			if (!matcher.matches())
				return null;
			// return last group
			String match = matcher.group(matcher.groupCount());// m_pattern.matcher(src).group();
			if (match.length() > 0)
				return match;
			else
				return null;
		}
		public String GetTagName()
		{
			return m_TagName;
		}
		public byte GetRecursionType()
		{
			return m_RecursiveType;
		}
		public boolean IsFolderRule()
		{
			return m_bFolderRule;
		}
		public FileFolderTemplate GetFFT()
		{
			return m_fft;
		}
		public void SetValue(String s)
		{
			if (m_bValue)
				return;
			m_bValue = true;
			m_match = s;
		}
	}
	private static class FileFolderTemplate
	{
		public String m_ID;
		private boolean m_TreeRoot = false;
		private boolean m_bFolder = true;
		private Collection<DerivedTagRule> m_TagRules = new LinkedList<DerivedTagRule>();
		private Collection<FileFolderTemplate> m_children = null;
		private Pattern m_pattern;

		public boolean IsRoot()
		{
			return m_TreeRoot;
		}
		public boolean Match(String name)
		{
			return m_pattern.matcher(name).matches();
		}

		public FileFolderTemplate(String id)
		{
			m_ID = id;
		}

		public FileFolderTemplate(Element e)
		{
			if (e.getName().compareTo("file") == 0)
				m_bFolder = false;
			m_ID = e.attributeValue("ID");
			String s;
			if ((s = e.attributeValue("pattern")) == null)
				s = m_ID;
			m_pattern = Pattern.compile(s);
			if ((e.attributeValue("treeRoot") != null)
					&& (e.attributeValue("treeRoot").compareTo("1") == 0))
				m_TreeRoot = true;

			Element subel;
			for (Iterator<Element> ie = e.elementIterator(); ie.hasNext();)
			{
				subel = ie.next();
				if (subel.getName().compareTo("tag") == 0)
					m_TagRules.add(new DerivedTagRule(subel, this, m_bFolder));
				else if (subel.getName().compareTo("child") == 0)
				{
					if (m_children == null)
						m_children = new LinkedList<FileFolderTemplate>();
					m_children.add(new FileFolderTemplate(subel.getText()));
				}
			}
		}
		public FileFolderTemplate[] GetAllChildren()
		{
			if (!m_bFolder || m_children == null)
				return new FileFolderTemplate[0];
			return m_children.toArray(new FileFolderTemplate[0]);
		}
		public FileFolderTemplate[] GetChildren(boolean bFiles)
		{
			if (!m_bFolder || m_children == null)
				return new FileFolderTemplate[0];
			LinkedList<FileFolderTemplate> llfft = new LinkedList<FileFolderTemplate>();
			for (final FileFolderTemplate nextfft : m_children)
			{
				if ((bFiles && !nextfft.m_bFolder)
						|| (!bFiles && nextfft.m_bFolder))
					llfft.add(nextfft);
			}
			return llfft.toArray(new FileFolderTemplate[0]);
		}
		public DerivedTagRule[] GetRules()
		{
			if (m_TagRules == null || m_TagRules.size() < 1)
				return new DerivedTagRule[0];
			return m_TagRules.toArray(new DerivedTagRule[0]);
		}
		public void UpdateChildren(TreeMap<String, FileFolderTemplate> map)
		{
			FileFolderTemplate[] children = GetAllChildren();
			if (children.length < 1)
				return;
			FileFolderTemplate fft;
			Collection<FileFolderTemplate> newChildren = new LinkedList<FileFolderTemplate>();
			for (int i = 0; i < children.length; i++)
			{
				fft = map.get(children[i].m_ID);
				if (fft != null)
					newChildren.add(fft);
			}
			m_children.clear();
			m_children.addAll(newChildren);
		}
	}
}
