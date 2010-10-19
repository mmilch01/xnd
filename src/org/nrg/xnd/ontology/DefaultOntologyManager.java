package org.nrg.xnd.ontology;

import java.io.File;
import java.io.InputStream;
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
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.utils.Utils;

/**
 * An abstract class for performing frequently required tasks.
 * 
 * @author mmilch
 * 
 */
public final class DefaultOntologyManager
{
	private static TreeMap<String, Integer> m_tagMap = new TreeMap<String, Integer>();
	private static LinkedList<SystemTag> m_tags;
	private static String m_id, m_descr;
	// m_TagHierarchy;
	public static boolean loadOntology(String file)
	{
		return LoadTagDescriptorXML(new File(file));
	}
	public static boolean loadDefaultOntology()
	{
		return LoadTagDescriptorXML(new File(getDefaultLocation()));
	}
	public static String getDefaultLocation()
	{
		return new File(Utils.GetPluginPath()
				+ "/xml_resources/xnd_tags_default.xml").getAbsolutePath();
	}
	public static boolean LoadTagDescriptorXML(File f)
	{
		try
		{
			return LoadTagDescriptorXML(new SAXReader().read(f));
		} catch (Exception e)
		{
			return false;
		}
	}
	public static boolean LoadTagDescriptorXML(InputStream in)
	{
		try
		{
			return LoadTagDescriptorXML(new SAXReader().read(in));
		} catch (Exception e)
		{
			return false;
		}
	}
	public static Context GetContext(ItemRecord ir)
	{
		Context llit = new Context();
		Collection<String> cur_roots = GetRoots();
		if (cur_roots.size() < 1)
			return llit;
		ItemTag it;
		for (String r : cur_roots)
		{
			if ((it = ir.getTag(r)) != null)
			{
				llit.add(it);
				break;
			}
		}
		if (llit.size() < 1)
			return llit;

		boolean bFound;
		while (true)
		{
			cur_roots = ChildTagsCustom(llit);
			if (cur_roots.size() < 1)
				return llit;
			bFound = false;
			for (String r : cur_roots)
			{
				if ((it = ir.getTag(r)) != null)
				{
					llit.add(it);
					bFound = true;
					break;
				}
			}
			if (!bFound)
				return llit;
		}
	}
	public static String getID()
	{
		return m_id;
	}
	public static String getDescr()
	{
		return m_descr;
	}
	/**
	 * Loads xml ontology descriptor (has to be called to initialize)
	 * 
	 * @param d
	 * @return
	 */
	private static boolean LoadTagDescriptorXML(Document d)
	{
		try
		{
			m_tags = new LinkedList<SystemTag>();
			Element el, root = d.getRootElement();
			String val;
			m_id = root.attributeValue("id");
			m_descr = root.attributeValue("descr");
			if (root.getName().compareTo("ontology") != 0 || m_id == null)
				return false;
			for (Iterator<Element> it = d.getRootElement().elementIterator(); it
					.hasNext();)
			{
				el = it.next();
				if (el.getName().compareTo("tag") == 0)
				{
					m_tags.add(new SystemTag(el));
					m_tagMap.put(el.attributeValue("name"), m_tags.size() - 1);
				}
			}
			for (SystemTag ct : m_tags)
			{
				ct.UpdateChildren();
			}
			return true;
		} catch (Exception e)
		{
			return false;
		}
	}
	/**
	 * 
	 * @return default array with default tag descriptors.
	 * 
	 * */
	public static TagDescr[] GetDefaultTagDescrs()
	{
		if (m_tags == null)
			return new TagDescr[0];
		TagDescr[] taga = new TagDescr[m_tags.size()];
		int i = 0;
		for (final SystemTag ct : m_tags)
		{
			taga[i] = ct.GetTagDescr();
			taga[i].SetAttr(TagDescr.DEFAULT);
			i++;
		}
		return taga;
	}
	private static SystemTag GetTag(String name)
	{
		return m_tags.get(m_tagMap.get(name).intValue());
	}
	public static boolean IsDefaultTagAttribSet(String tag, int attrib)
	{
		if (m_tags == null)
			return false;
		SystemTag ct = GetTag(tag);
		if (ct == null)
			return false;
		return ct.IsAttrSet(attrib);
	}
	public static ItemTag[] GetDefaultTags()
	{
		if (m_tags == null)
			return new ItemTag[0];
		ItemTag[] tags = new ItemTag[m_tags.size()];
		int i = 0;
		for (SystemTag ct : m_tags)
		{
			tags[i++] = new ItemTag(ct.m_Name);
		}
		return tags;
	}
	public static Collection<String> GetRoots()
	{
		LinkedList<String> roots = new LinkedList<String>();
		for (final SystemTag t : m_tags)
			if (t.IsAttrSet(TagDescr.ROOT))
				roots.add(t.m_Name);
		return roots;
	}
	/*
	 * public static Collection<String> ChildTagsBelow(LinkedList<ItemTag>
	 * context) { LinkedList<String> res=new LinkedList<String>(); String[]
	 * childTags=ChildTagsCustom(context); String[] chloc; for(String
	 * ch:childTags) { context.add(new ItemTag(ch));
	 * chloc=ChildTagsCustom(context); context.removeLast(); for(String
	 * chl:chloc) res.add(chl); } }
	 */
	public static TreeMap<String, Collection<String>> ChildMap(Context context,
			Collection<String> children)
	{
		TreeMap<String, Collection<String>> res = new TreeMap<String, Collection<String>>();
		for (String ch : children)
		{
			context.add(new ItemTag(ch));
			res.put(ch, ChildTagsCustom(context));
			context.removeLast();
		}
		return res;
	}
	public static Collection<String> ChildTagsCustom(Context context)
	{
		if (context.size() < 1)
		{
			return GetRoots();
		}
		try
		{
			SystemTag root = GetTag(context.get(0).GetName());
			for (int i = 1; i < context.size(); i++)
			{
				root = root.GetChild(context.get(i).GetName());
				if (root == null)
					return new LinkedList<String>();
			}
			return root.GetChildNames();
		} catch (Exception e)
		{
			e.printStackTrace();
			return new LinkedList<String>();
		}
	}
	public static String[] GetTagValues(ItemTag tag)
	{
		SystemTag t;
		if ((t = GetTag(tag.GetName())) != null)
			return t.GetPredefinedValues();
		else
			return new String[0];
	}
	/*
	 * public static boolean IsTagFixed(ItemTag tag) { CustomTag t;
	 * if((t=m_tags.get(tag.GetName()))!=null) return t.HasFixedValues(); else
	 * return false; }
	 */
	private final static class SystemTag
	{
		public String m_Name;
		public String m_XNATAlias = null;
		private Collection<SystemTag> m_Children = null;
		private TagDescr m_TagDescr;
		private int m_order = 0;

		public TagDescr GetTagDescr()
		{
			return m_TagDescr;
		}
		public boolean IsAttrSet(int attr)
		{
			return m_TagDescr.IsSet(attr);
		}
		public SystemTag(String name)
		{
			m_Name = name;
		}
		public SystemTag(Element el)
		{
			m_Name = el.attributeValue("name");
			int descr = 0;

			// tree root
			String s = el.attributeValue("treeRoot");
			if (s != null && s.compareTo("1") == 0)
				descr |= TagDescr.ROOT;

			// table view
			s = el.attributeValue("tableView");
			if (s != null && s.compareTo("1") == 0)
				descr |= TagDescr.TABLE_DISPLAY;

			// alphanumeric no spaces
			s = el.attributeValue("anValue");
			if (s != null && s.compareTo("1") == 0)
				descr |= TagDescr.VALUE_ALPHANUMERIC;

			// tag type
			s = el.attributeValue("type");
			if (s != null)
			{
				if (s.toLowerCase().compareTo("ontology") == 0)
				{
					descr |= TagDescr.SYSTEM;
				} else if (s.toLowerCase().compareTo("service") == 0)
				{
					descr |= TagDescr.INVISIBLE;
				}
			}

			m_TagDescr = new TagDescr(m_Name, descr);
			Element sub_el;
			for (Iterator<Element> iter = el.elementIterator(); iter.hasNext();)
			{
				sub_el = iter.next();
				if (sub_el.getName().compareTo("child") == 0)
					AddChild(new SystemTag(sub_el.getText()));
				else if (sub_el.getName().compareTo("value") == 0)
				{
					AddValue(sub_el.getText());
					m_TagDescr.SetAttr(TagDescr.PREDEF_VALUES);
				}
			}
		}
		public SystemTag[] GetChildren()
		{
			SystemTag[] ar = new SystemTag[0];
			if (m_Children == null)
				return ar;
			return m_Children.toArray(ar);
		}
		public Collection<String> GetChildNames()
		{
			LinkedList<String> names = new LinkedList<String>();
			if (m_Children == null)
				return names;
			for (SystemTag ct : m_Children)
				names.add(ct.m_Name);
			return names;
		}
		public String[] GetPredefinedValues()
		{
			return m_TagDescr.GetValues();
		}
		public void AddChild(SystemTag ct)
		{
			if (m_Children == null)
				m_Children = new LinkedList<SystemTag>();
			m_Children.add(ct);
		}
		public SystemTag GetChild(String name)
		{
			for (final SystemTag ct : m_Children)
				if (ct.m_Name.compareTo(name) == 0)
					return ct;
			return null;
		}
		public void AddValue(String s)
		{
			m_TagDescr.AddValue(s);
		}
		public boolean HasFixedValues()
		{
			return m_TagDescr.IsSet(TagDescr.PREDEF_VALUES);
		}
		public void UpdateChildren()
		{
			SystemTag[] children = GetChildren();
			if (children.length < 1)
				return;
			SystemTag t;
			Collection<SystemTag> newChildren = new LinkedList<SystemTag>();
			for (int i = 0; i < children.length; i++)
			{
				t = GetTag(children[i].m_Name);
				if (t != null)
					newChildren.add(t);
			}
			m_Children.clear();
			m_Children.addAll(newChildren);
		}
	}
}