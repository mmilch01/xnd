package org.nrg.fileserver;

import java.util.Collection;

import org.nrg.xnd.utils.StringTree;

/**
 * Represents a tag entry.
 * 
 * @author Mikhail Milchenko
 * 
 */
public class ItemTag implements Comparable<ItemTag>
{
	private String m_Name = "";
	// private LinkedList<String> m_Values=new LinkedList<String>();
	// private TreeMap<String,String> m_Values=new TreeMap<String,String>();
	private StringTree m_Values = new StringTree();
	public ItemTag(String tag)
	{
		m_Name = tag;
	}
	private String m_qValue = "*";
	private boolean m_bAllowMultiple = false;
	public ItemTag(String name, Collection<String> values)
	{
		m_Name = name;
		m_Values.Add(values);
		m_bAllowMultiple = true;
	}
	public ItemTag(String name, String val, boolean bAllowMultiple)
	{
		m_Name = name;
		// m_Values.add(val);
		m_Values.Add(val);
		m_bAllowMultiple = bAllowMultiple;
	}
	public boolean IsMultiValue()
	{
		return m_bAllowMultiple;
	}
	public String GetQValue()
	{
		return m_qValue;
	}
	public void SetQValue(String s)
	{
		m_qValue = s;
	}
	public void SetAllowMultiple(boolean bAllow)
	{
		m_bAllowMultiple = bAllow;
	}
	public boolean IsUniqueTag()
	{
		if (m_Name.length() < 1)
			return false;
		if (m_Name.startsWith("*"))
			return false;
		return true;
	}
	@Override
	public String toString()
	{
		return PrintTag();
	}
	public String PrintTag()
	{
		return m_Name + ": " + PrintValues();
	}
	public String PrintValues()
	{
		String res = "";
		int i = 0;
		for (String s : m_Values.Values())
		{
			if (i > 0)
				res += ", " + s;
			else
				res += s;
			i++;
		}
		return res;
	}
	public ItemTag(String name, String val)
	{
		this(name, val, false);
	}
	public String GetName()
	{
		return m_Name;
	}
	public String GetFirstValue()
	{
		if (m_Values.Size() < 1)
			return "";
		// return m_Values.get(0);
		return m_Values.FistValue();
	}
	public void AddValues(Collection<String> values)
	{
		m_Values.Add(values);
	}
	public Collection<String> GetValues()
	{
		return m_Values.Values();
	}
	public String getAllValuesAsString()
	{
		return m_Values.PrintValues();
	}
	public String[] GetAllValues()
	{
		// if (m_bAllowMultiple)
		return m_Values.Values().toArray(new String[0]);
		/*
		 * else { String[] values = new String[1]; // values[0]=m_Values.get(0);
		 * values[0] = m_Values.FistValue(); return values; }
		 */
	}
	public void RemoveValue(String val)
	{
		m_Values.Remove(val);
	}
	/**
	 * Replace previous value(s) with a new value.
	 * 
	 * @param val
	 */
	public void SetValue(String val)
	{
		m_Values.Clear();
		// m_Values.add(val);
		m_Values.Add(val);
		// m_Values.put(val, val);
	}
	public void AddValue(String val)
	{
		try
		{
			// m_Values.add(val);
			m_Values.Add(val);
			// m_Values.put(val,val);
		} catch (Exception e)
		{
			System.out.print(e.getMessage());
		}
	}
	public void ClearAllValues()
	{
		m_Values.Clear();
	}
	public int CompareFully(ItemTag lbl)
	{
		int res = m_Name.compareTo(lbl.GetName());
		if (res != 0)
			return res;
		return GetFirstValue().compareTo(lbl.GetFirstValue());
	}
	public int compareTo(ItemTag lbl)
	{
		// if(m_Name.compareTo(lbl.GetName())==0 &&
		// m_Values.values.getFirst().compareTo(lbl.GetFirstValue())==0) return
		// 0;
		// return m_Name.compareTo(lbl.GetName());
		return PrintTag().compareTo(lbl.PrintTag());
	}
}