package org.nrg.xnd.ontology;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.nrg.fileserver.Context;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.utils.Utils;

public abstract class XNATThesaurus
{
	/*
	 * public static final String[]
	 * xsiTypes={"projectData","subjectData","experimentData",
	 * "imageScanData","reconstructedImageData","imageAssessorData"}; public
	 * static final int PROJECT=0,SUBJECT=1,EXPERIMENT=2,IMAGESCAN=3,
	 * RECONSTRUCTEDIMAGE=4,IMAGEASSESSOR=5;
	 */
	private static LinkedList<XNATObject> m_groups = new LinkedList<XNATObject>();
	private static TreeMap<String, XNATObject.Entry> m_keys = new TreeMap<String, XNATObject.Entry>();

	public static String getQueryparams(ItemTag it)
	{
		String nm = it.GetName();
		if (nm.compareTo("Project") == 0)
			return "accessible=true";
		return "";
	}
	public static boolean isAmbiguousContext(Context c)
	{
		if (c.size() < 3)
			return false;
		if (c.getLast().GetName().toLowerCase().compareTo("experiment") == 0)
			return true;
		else
			return false;
	}
	public static Collection<Context> listSynonymContexts(Context c)
	{
		Collection<Context> cc = new LinkedList<Context>();
		cc.add(c);
		if (c.size() < 3)
			return cc;
		if (c.get(1).GetName().toLowerCase().compareTo("subject") == 0)
		{
			Context newCon = new Context(c);
			newCon.remove(1);
			cc.add(newCon);
		}
		return cc;
	}
	public static Context amendContext(Context c)
	{
		if (c.size() < 2)
			return c;
		ItemTag t = c.getLast();
		if (t.GetName().toLowerCase().compareTo("experiment") != 0)
			return c;
		String val = t.GetFirstValue();
		Context newC = new Context(c);
		val = c.get(c.size() - 1).GetFirstValue() + "_" + val;
		newC.removeLast();
		newC.add(new ItemTag(t.GetName(), val));
		return newC;
	}
	public static String getQueryParams(Context context, ItemRecord tagRecord)
	{
		if (context.size() == 0)
			return "";
		String param = "", vname, val;
		int i = 0;
		for (ItemTag it : tagRecord.getAllTags())
		{
			vname = GetVarname(it.GetName(), context);
			if (vname == null)
				continue;
			// quote illegal characters
			val = Utils.StrFormatURI(it.GetFirstValue());
			if (val != null && val.length() > 0)
			{
				if (i > 0)
					param += "&";
				if (vname.toLowerCase().compareTo("modality") == 0)
					param += "xsiType=xnat:" + val + "SessionData";
				else
					param += vname + "=" + val;
				i++;
			}
		}
		return param;
		/*
		 * if(context.getLast().GetName().compareTo("Experiment")!=0) return
		 * param; ItemTag t=tagRecord.GetTagByName("Modality"); if(t==null)
		 * return param; val=t.GetFirstValue().toLowerCase(); if(val==null ||
		 * val.length()<1) return ""; return "xsiType=xnat:"+val+"SessionData";
		 */
	}

	public static boolean Load(File f)
	{
		m_groups.clear();
		try
		{
			Document d = new SAXReader().read(f);
			Element el;
			for (Iterator<Element> it = d.getRootElement().elementIterator(); it
					.hasNext();)
			{
				el = it.next();
				if (el.getName().compareTo("group") == 0)
				{
					XNATObject xno = new XNATObject(el);
					if (xno.m_key != null)
						m_keys.put(xno.getKeyTagName(), xno.m_key);
					m_groups.add(xno);
				}
			}
			return true;
		} catch (Exception e)
		{
			return false;
		}
	}
	public static String getDefaultLocation()
	{
		return new File(Utils.GetPluginPath()
				+ "/xml_resources/xnat_thesaurus.xml").getAbsolutePath();
	}
	public static String GetVarname(String xndTag, Context context)
	{
		ItemTag level = context.getLast();
		if (level == null)
			return null;
		XNATObject.Entry key = m_keys.get(level.GetName());
		if (key == null)
			return null;
		return key.m_parent.getRESTVar(xndTag);
	}
	public static String GetIDVarname(ItemTag it)
	{
		// return m_keys.get(it.GetName()).m_varname;

		String nm = it.GetName();
		if (nm.compareTo("Subject") == 0)
			return "label";// "subjectid";
		if (nm.compareTo("Experiment") == 0 || nm.compareTo("Assessor") == 0)
			return "label";
		if (nm.compareTo("Scan") == 0 || nm.compareTo("Reconstruction") == 0)
			return "id";
		if (nm.compareTo("Resource") == 0)
			return "xnat_abstractresource_id";
		if (nm.compareTo("Project") == 0)
			return "id";
		return "label";

	}
	public static String getQueryParams(ItemTag it)
	{
		String nm = it.GetName();
		if (nm.compareTo("Project") == 0)
			return "accessible=true";
		return "";
	}
	public static String GetPostfix(ItemTag it)
	{
		String nm = it.GetName();
		if (nm.compareTo("Assessor") == 0
				|| nm.compareTo("Reconstruction") == 0)
			return "/out";
		return "";
	}
	public final static class XNATObject
	{
		public int m_number;
		public String m_name;
		public Entry m_key = null;
		private TreeMap<String, Entry> m_EntriesByXNDTag = new TreeMap<String, Entry>();
		public XNATObject(Element el)
		{
			try
			{
				m_number = Integer.parseInt(el.attributeValue("number"));
				m_name = el.attributeValue("name");
				for (Iterator<Element> ie = el.elementIterator(); ie.hasNext();)
				{
					Element nxt = ie.next();
					Entry e = new Entry(nxt, this);
					if (e.m_xndtagname != null)
					{
						if (e.m_bKey)
							m_key = e;
						m_EntriesByXNDTag.put(e.m_xndtagname, e);
					}
				}
			} catch (Exception e)
			{
			}
		}
		public String getRESTVar(String tag)
		{
			try
			{
				return m_EntriesByXNDTag.get(tag).m_varname;
			} catch (Exception e)
			{
				return null;
			}
		}
		public String getKeyTagName()
		{
			try
			{
				return m_key.m_xndtagname;
			} catch (Exception e)
			{
				return null;
			}
		}
		public final static class Entry
		{
			public boolean m_bKey = false;
			public String m_varname;
			public String m_xsiType;
			public String m_xndtagname = null;
			public XNATObject m_parent;
			public Entry(Element el, XNATObject par)
			{
				m_parent = par;
				try
				{
					if (el.getName().compareTo("key") == 0)
						m_bKey = true;
					m_xsiType = el.attributeValue("xsiType");
					m_varname = el.attributeValue("varname");
					m_xndtagname = el.attributeValue("xndtagname");
				} catch (Exception e)
				{
				}
			}
		}
	}
}