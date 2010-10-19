package org.nrg.xnd.utils;

import java.util.Collection;

public class StringTreeElement
{
	private String m_value = null;
	private StringTreeElement m_left = null, m_right = null, m_parent = null;
	private static int m_cnt = 0;
	public StringTreeElement(String val, StringTreeElement parent)
	{
		m_value = val;
		m_parent = parent;
	}
	public StringTreeElement Get(String val)
	{
		if (m_value == null)
			return null;
		int c = m_value.compareTo(val);
		if (c == 0)
			return this;
		else if (c > 0)
		{
			if (m_left != null)
				return m_left.Get(val);
			else
				return null;
		} else
		{
			if (m_right != null)
				return m_right.Get(val);
			else
				return null;
		}
	}
	public String GetVal()
	{
		return m_value;
	}
	public boolean Contains(String val)
	{
		return Get(val) != null;
	}
	public StringTreeElement GetParent()
	{
		return m_parent;
	}
	public StringTreeElement GetLeft()
	{
		return m_left;
	}
	public StringTreeElement GetRight()
	{
		return m_right;
	}
	public void SetParent(StringTreeElement par)
	{
		m_parent = par;
	}
	private void SetLeft(StringTreeElement left)
	{
		m_left = left;
	}
	private void SetRight(StringTreeElement right)
	{
		m_right = right;
	}
	public void SetVal(String val)
	{
		m_value = val;
	}
	public void RemoveSubtree()
	{
		if (m_right != null)
			m_right.RemoveSubtree();
		if (m_left != null)
			m_left.RemoveSubtree();
		m_right = null;
		m_left = null;
		m_value = null;
	}
	public void Remove()
	{
		if (m_parent == null)
		{
			if (m_left != null)
				m_left.SetParent(null);
			if (m_right != null)
				m_right.SetParent(null);
			if (m_left != null && m_right != null)
			{
				if (m_cnt % 2 > 0)
					m_left.Add(m_right);
				else
					m_right.Add(m_left);
			}
		} else
		{
			if (m_parent.GetLeft() == this)
				m_parent.SetLeft(null);
			else if (m_parent.GetRight() == this)
				m_parent.SetRight(null);
			m_parent.Add(m_left);
			m_parent.Add(m_right);
		}
		m_cnt++;
	}
	private StringTreeElement FindLeaf(String str)
	{
		if (m_value == null)
			return this;
		int c = m_value.compareTo(str);
		if (c == 0)
			return (m_right != null || m_left != null) ? null : this;
		else if (c > 0)
		{
			if (m_left != null)
				return m_left;
			else
				return this;
		} else
		{
			if (m_right != null)
				return m_right;
			else
				return this;
		}
	}

	private boolean AddChild(StringTreeElement ste)
	{
		if (ste == null)
			return false;
		String val = ste.GetVal();
		if (val == null)
			return false;
		if (m_value == null)
		{
			m_value = val;
			return true;
		}
		int c = m_value.compareTo(val);
		if (c == 0)
			return false;
		else if (c > 0)
		{
			m_left = ste;
			m_left.SetParent(this);
			return true;
		} else
		{
			m_right = ste;
			m_right.SetParent(this);
			return true;
		}
	}
	public boolean Add(StringTreeElement ste)
	{
		if (ste == null)
			return false;
		if (ste.GetVal() == null)
			return false;
		StringTreeElement leaf = FindLeaf(ste.GetVal());
		return leaf.AddChild(ste);
	}
	public void Values(Collection<String> cs)
	{
		if (m_left != null)
			m_left.Values(cs);
		if (m_right != null)
			m_right.Values(cs);
		if (m_value != null)
			cs.add(m_value);
	}
	public boolean Add(String val)
	{
		if (m_value == null)
		{
			m_value = val;
			return true;
		}
		int c = m_value.compareTo(val);
		if (c == 0)
			return false;
		else if (c > 0)
		{
			if (m_left != null)
			{
				return m_left.Add(val);
			} else
			{
				m_left = new StringTreeElement(val, this);
				return true;
			}
		} else
		{
			if (m_right != null)
			{
				return m_right.Add(val);
			} else
			{
				m_right = new StringTreeElement(val, this);
				return true;
			}
		}
	}
}
