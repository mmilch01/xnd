package org.nrg.xnd.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.dom4j.Element;
import org.nrg.fileserver.ItemRecord;
import org.nrg.xnd.utils.Utils;

public class ViewFilter
{
	public static final int AND = 1, OR = 2, IS = 4, IS_NOT = 8, DEFINED = 16,
			EQUAL_TO = 32;
	private boolean m_bEnabled = true;

	private TreeMap<String, FilterClause> m_clauses = new TreeMap<String, FilterClause>();

	public String GetDescription(boolean bShort)
	{
		String[] cl = GetClauseList(bShort);
		String res = "";
		for (int i = 0; i < cl.length; i++)
			if (i > 0)
				res = res + " AND " + cl[i];
			else
				res = cl[i];
		return res;
	}
	public boolean FromString(String s)
	{
		if (s.length() < 1)
			return false;
		int ind = 0, ind1;
		FilterClause fc;
		boolean bEnd = false;
		do
		{
			ind1 = s.indexOf(" AND ", ind);
			if (ind1 < 0)
			{
				ind1 = s.length();
				bEnd = true;
			}
			fc = new FilterClause();
			if (!fc.FromString(s.substring(ind, ind1)))
				return false;
			m_clauses.put(Utils.PseudoUID(""), fc);
			ind = ind1 + 5;
		} while (!bEnd);
		return true;
	}
	public String[] GetClauseList(boolean bShort)
	{
		Collection<String> res = new LinkedList<String>();
		for (Map.Entry<String, FilterClause> me : m_clauses.entrySet())
			res.add(me.getValue().GetClauseString(bShort));
		return res.toArray(new String[0]);
	}
	public void SetEnabled(boolean bEn)
	{
		m_bEnabled = bEn;
	}
	public boolean IsEnabled()
	{
		return m_bEnabled;
	}

	public void AddClause(String id, TagDescr tag, int code, String match)
	{
		m_clauses.put(id, new FilterClause(tag, code, match));
	}
	public void RemoveClause(int ind)
	{
		int i = 0;
		for (Map.Entry<String, FilterClause> me : m_clauses.entrySet())
		{
			if (i == ind)
			{
				m_clauses.remove(me.getKey());
				break;
			}
			i++;
		}
	}
	public String GetClauseString(String id, boolean bShort)
	{
		return m_clauses.get(id).GetClauseString(bShort);
	}
	public void FilterLL(LinkedList<ItemRecord> llir)
	{
		if (!m_bEnabled)
			return;
		ItemRecord ir;
		for (int i = llir.size() - 1; i >= 0; i--)
			if (!Match(llir.get(i)))
				llir.remove(i);
	}
	public Collection<ItemRecord> FilterC(Collection<ItemRecord> cir)
	{
		if (!m_bEnabled)
			return cir;
		LinkedList<ItemRecord> llir = new LinkedList<ItemRecord>();
		for (ItemRecord ir : cir)
			if (Match(ir))
				llir.add(ir);
		return llir;
	}
	public boolean Match(ItemRecord ir)
	{
		if (!m_bEnabled)
			return true;
		boolean bRes = true;
		for (FilterClause fc : m_clauses.values())
		{
			bRes &= fc.Match(ir);
			if (!bRes)
				break;
		}
		return bRes;
	}
	public boolean Serialize(String file, boolean is_loading)
	{
		try
		{
			boolean bRes;
			if (is_loading)
			{
				ObjectInputStream ois = new ObjectInputStream(
						new FileInputStream(file));
				bRes = Serialize(ois, is_loading);
				ois.close();
				return bRes;
			} else
			{
				ObjectOutputStream oos = new ObjectOutputStream(
						new FileOutputStream(file));
				bRes = Serialize(oos, is_loading);
				oos.flush();
				oos.close();
				return bRes;
			}
		} catch (Exception e)
		{
			return false;
		}
	}
	public boolean Serialize(Element el, boolean is_loading)
	{
		try
		{
			if (is_loading)
			{
				m_clauses.clear();
				m_bEnabled = Boolean.valueOf(el.attributeValue("Enable"))
						.booleanValue();
				Element cl;
				FilterClause fc;
				for (Iterator<Element> ie = el.elementIterator(); ie.hasNext();)
				{
					cl = ie.next();
					if (cl.getName().compareTo("Clause") == 0)
					{
						fc = new FilterClause();
						fc.Serialize(cl, true);
						m_clauses.put(cl.attributeValue("ID"), fc);
					}
				}
			} else
			{
				el.addAttribute("Enable", Boolean.toString(m_bEnabled));
				Element cl;
				for (String id : m_clauses.keySet())
				{
					cl = el.addElement("Clause");
					cl.addAttribute("ID", id);
					m_clauses.get(id).Serialize(cl, false);
				}
			}
		} catch (Exception e)
		{
			return false;
		}
		return true;
	}
	private boolean Serialize(Object stream, boolean is_loading)
			throws IOException
	{
		if (is_loading)
		{
			ObjectInputStream ois = (ObjectInputStream) stream;
			m_clauses.clear();
			m_bEnabled = ois.readBoolean();
			int nEntries = ois.readInt();
			String s;
			for (int i = 0; i < nEntries; i++)
			{
				FilterClause fc = new FilterClause();
				s = Utils.SerializeString(ois, null, is_loading);
				fc.Serialize(stream, is_loading);
				m_clauses.put(s, fc);
			}
		} else
		{
			ObjectOutputStream oos = (ObjectOutputStream) stream;
			oos.writeBoolean(m_bEnabled);
			oos.writeInt(m_clauses.size());
			for (Map.Entry<String, FilterClause> me : m_clauses.entrySet())
			{
				Utils.SerializeString(oos, me.getKey(), is_loading);
				me.getValue().Serialize(oos, is_loading);
			}
		}
		return true;
	}
	public class FilterClause
	{
		private Pattern m_match = null;
		public int m_code = 0;
		private TagDescr m_tag;

		public boolean FromString(String s)
		{
			int code = 0;
			int ind, ind1 = s.indexOf("!=", 0), ind2 = s.indexOf('=', 0);
			if (ind1 < 0 && ind2 < 0)
				return false;
			if (ind1 < 0)
				ind = ind2;
			else if (ind2 > ind1)
				ind = ind1;
			else
				ind = ind2;
			String tn = s.substring(0, ind);
			m_tag = new TagDescr(tn);
			if (s.charAt(ind) == '!')
			{
				if (s.charAt(ind + 1) == '=')
				{
					code |= IS_NOT;
					ind += 2;
				} else
					return false;
			} else if (s.charAt(ind) == '=')
			{
				code |= IS;
				ind++;
			} else
				return false;
			if (s.length() <= ind)
			{
				code |= DEFINED;
			}
			if (s.charAt(ind) != '\"')
				return false;
			ind++;
			ind1 = s.indexOf('\"', ind);
			if (ind1 < 0)
				return false;
			try
			{
				m_match = Pattern.compile(s.substring(ind, ind1));
			} catch (PatternSyntaxException e)
			{
				return false;
			}
			code |= EQUAL_TO;
			m_code = code;
			return true;
		}
		public boolean Serialize(Element el, boolean is_loading)
				throws NullPointerException, NumberFormatException,
				PatternSyntaxException
		{
			if (is_loading)
			{
				m_tag = new TagDescr();
				m_tag.Serialize(el.element("TagDescr"), true);
				m_code = new Integer(el.attribute("Code").getValue())
						.intValue();
				m_match = Pattern.compile(el.attribute("Pattern").getValue());
				return true;
			} else
			{
				m_tag.Serialize(el.addElement("TagDescr"), false);
				el.addAttribute("Code", new Integer(m_code).toString());
				el.addAttribute("Pattern", m_match.pattern());
				return true;
			}
		}
		public boolean Serialize(Object stream, boolean is_loading)
				throws IOException
		{
			if (is_loading)
			{
				m_tag = new TagDescr();
				m_tag.Serialize(stream, is_loading);
				ObjectInputStream ois = (ObjectInputStream) stream;
				m_code = ois.readInt();
				String s = Utils.SerializeString(ois, null, is_loading);
				m_match = Pattern.compile(s);
			} else
			{
				m_tag.Serialize(stream, is_loading);
				ObjectOutputStream oos = (ObjectOutputStream) stream;
				oos.writeInt(m_code);
				Utils.SerializeString(oos, m_match.pattern(), is_loading);
			}
			return true;
		}

		FilterClause()
		{
		}

		FilterClause(TagDescr tag, int code, String match)
		{
			m_tag = tag;
			m_code = code;
			m_match = Pattern.compile(match);
		}
		public boolean Match(ItemRecord ir)
		{
			try
			{
				String s = ir.getTagValue(m_tag.GetName());
				if ((m_code & DEFINED) != 0)
				{
					boolean bRes = (s != null);
					return ((m_code & IS) != 0) ? bRes : !bRes;
				}
				if ((m_code & EQUAL_TO) != 0)
				{
					boolean bRes;
					if (s == null)
						bRes = false;
					else
					{
						Matcher m = m_match.matcher(ir.getTagValue(m_tag
								.GetName()));
						bRes = m.matches();
					}
					return ((m_code & IS) != 0) ? bRes : !bRes;
				}
			} catch (Exception e)
			{
				return false;
			}
			return true;
		}
		public String GetClauseString(boolean bShort)
		{
			String res;
			if (bShort)
			{
				res = m_tag.GetName() + (((m_code & IS) != 0) ? "=" : "!=");
				if ((m_code & EQUAL_TO) != 0)
					res += "\"" + m_match.toString() + "\"";
			} else
			{
				res = m_tag.GetName()
						+ (((m_code & IS) != 0) ? " is" : " is not")
						+ (((m_code & EQUAL_TO) != 0)
								? " equal to"
								: " defined");
				if ((m_code & EQUAL_TO) != 0)
					res += " \"" + m_match.toString() + "\"";
			}
			return res;
		}
	}
}