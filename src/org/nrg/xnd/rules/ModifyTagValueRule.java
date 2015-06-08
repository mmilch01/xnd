package org.nrg.xnd.rules;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Element;
import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.VirtualFolder;

public class ModifyTagValueRule extends Rule
{
	private static TreeMap<String, TagMatch> m_Matches = new TreeMap<String, TagMatch>();

	public ModifyTagValueRule(RepositoryViewManager rvm, String uid)
	{
		super(rvm, uid);
	}
	/*
	 * public static ModifyTagValueRule GetInstance(RepositoryViewManager rvm) {
	 * return new ModifyTagValueRule(rvm); }
	 */
	@Override
	public boolean ParseRuleDescriptor()
	{
		try
		{
			Element el;
			for (Iterator<Element> it = m_xmldoc.getRootElement()
					.elementIterator(); it.hasNext();)
			{
				el = it.next();
				if (el.getName().toLowerCase().compareTo("tag") != 0)
					continue;
				m_Matches.put(el.attributeValue("name"), new TagMatch(el));
			}

		} catch (Exception e)
		{
			return false;
		}
		return true;
	}
	@Override
	public boolean ApplyRule(Collection<CElement> records, IProgressMonitor ipm)
	{
		boolean res = true;
		String tv;
		ItemRecord ir;
		for (CElement ce : records)
		{
			if (ce instanceof DBElement)
			{
				ir = ((DBElement) ce).GetIR();
				if (ir == null)
					continue;
				for (String s : m_Matches.keySet())
				{
					if ((tv = ir.getTagValue(s)) != null && tv.length() > 0)
					{
						tv = m_Matches.get(s).GetValue(tv);
						if (tv != null && tv.length() > 0)
							res &= m_rvm.DBTagAttach(ir, new ItemTag(s, tv));
					}
				}
			} else if (ce instanceof FSFolder || ce instanceof VirtualFolder)
			{
				ce.ApplyOperation(this, -1, ipm);
			}
		}
		return res;
	}

	final static class TagMatch
	{
		private class Replace
		{
			private Pattern m_Pattern;
			private String m_With;
			public Replace(Pattern p, String s)
			{
				m_Pattern = p;
				m_With = s;
			}
			public String doReplace(String s)
			{
				return m_Pattern.matcher(s).replaceAll(m_With);
			}

		};
		private Pattern m_substring = null;
		private Collection<Replace> m_replace = new LinkedList<Replace>();
		private String m_Value;

		public TagMatch(Element el) throws IllegalArgumentException,
				NullPointerException
		{
			Element sub;
			String s;
			Pattern p;
			for (Iterator it = el.elementIterator(); it.hasNext();)
			{
				sub = (Element) it.next();
				if (sub.getName().compareTo("substring") == 0)
				{
					m_substring = Pattern
							.compile(sub.attributeValue("pattern"));
					m_Value = sub.attributeValue("value");
					if (m_Value == null)
						throw new IllegalArgumentException();
					if (m_substring == null)
						throw new IllegalArgumentException();
				} else if (sub.getName().compareTo("replace") == 0)
				{
					p = Pattern.compile(sub.attributeValue("match"));
					s = sub.attributeValue("with");
					if (p == null || s == null)
						throw new IllegalArgumentException();
					m_replace.add(new Replace(p, s));
				}
			}
		}
		public String GetValue(String val)
		{
			String sub = GetSubstring(val);
			if (sub == null)
				return null;
			for (Replace r : m_replace)
				sub = r.doReplace(sub);
			return sub;
		}
		private String GetSubstring(String src)
		{
			if (m_substring == null)
				return src;
			Matcher matcher = m_substring.matcher(src);
			if (!matcher.matches())
				return null;
			String match;
			String res=m_Value,token;
			for (int i=1; i<=matcher.groupCount(); i++)
			{
				match=matcher.group(i);
				if (match.length()>0)
				{
					token="\\{"+i+"\\}";
					res=res.replaceAll(token,match);
				}
				else return null;
			}
			return res;
		}
	}
}
