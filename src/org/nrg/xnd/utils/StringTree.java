package org.nrg.xnd.utils;

import java.util.Collection;
import java.util.LinkedList;

public class StringTree
{
	private int m_cnt = 0;
	private StringTreeElement m_root;
	public StringTree()
	{
		m_root = new StringTreeElement((String) null, (StringTreeElement) null);
	}
	public boolean Contains(String str)
	{
		return m_root.Contains(str);
	}
	public void Add(String... str)
	{
		for (String s : str)
			Add(s);
	}
	public void Add(Collection<String> cs)
	{
		for (String s : cs)
			Add(s);
	}
	public void Add(String str)
	{
		m_cnt += m_root.Add(str) ? 1 : 0;
	}
	public void Clear()
	{
		m_root.RemoveSubtree();
	}
	public String FistValue()
	{
		return m_root.GetVal();
	}
	public void Remove(String str)
	{
		StringTreeElement ste = m_root.Get(str);
		if (ste == m_root)
		{
			StringTreeElement left = m_root.GetLeft(), right = m_root
					.GetRight();
			if (left == null && right == null)
				m_root.SetVal(null);
			else if (left != null && right != null)
			{
				if (m_cnt % 2 > 0)
				{
					m_root = left;
					m_root.Add(right);
				} else
				{
					m_root = right;
					m_root.Add(left);
				}
			} else if (left != null)
			{
				m_root = left;
			} else
			{
				m_root = right;
			}
			m_root.SetParent(null);
			m_cnt--;
		} else if (ste != null)
		{
			ste.Remove();
			m_cnt--;
		}
	}
	public int Size()
	{
		return m_cnt;
	}
	public String PrintValues()
	{
		Collection<String> cs = Values();
		String res = "";
		for (String s : cs)
			res += s + ", ";
		if (res.length() > 0)
			res = res.substring(0, res.length() - 2);
		return res;
	}
	public Collection<String> Values()
	{
		LinkedList<String> vals = new LinkedList<String>();
		m_root.Values(vals);
		return vals;
	}
}