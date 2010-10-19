package org.nrg.xnd.ui;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.swt.widgets.Combo;
import org.nrg.xnd.utils.Utils;

public class MemoCombo
{
	private LinkedList<String> m_vals = new LinkedList<String>();
	private int m_mval = -1;
	private String m_uid;
	public Combo m_cmb;

	public MemoCombo(Combo cmb, String UID, Collection<String> vals)
	{
		m_cmb = cmb;
		m_mval = vals.size();
		m_uid = UID;
		for (String s : m_vals)
			m_cmb.add(s);
		try
		{
			if (m_vals.size() > 0)
				m_cmb.select(0);// setText(m_vals.get(0));
		} catch (Exception e)
		{
		}
	}
	public void setSelection(String s)
	{
		if (s.length() < 1)
			return;
		for (int i = 0; i < m_cmb.getItemCount(); i++)
		{
			if (m_cmb.getItem(i).compareTo(s) == 0)
			{
				m_cmb.select(i);
				return;
			}
		}
	}
	public MemoCombo(Combo cmb, String UID, String... vals)
	{
		m_cmb = cmb;
		m_mval = vals.length;
		m_uid = UID;
		for (String s : m_vals)
			m_cmb.add(s);
	}

	// NOTE: add constructor for this class before defining any event listeners;
	// otherwise may lead to exceptions.
	public MemoCombo(Combo cmb, String UID, int maxval)
	{
		m_cmb = cmb;
		m_mval = maxval;
		m_uid = UID;
		Utils.SerializeListOfValues(m_uid, m_vals, true);
		for (String s : m_vals)
			m_cmb.add(s);
		try
		{
			if (m_vals.size() > 0)
				m_cmb.select(0);// setText(m_vals.get(0));
		} catch (Exception e)
		{
		}
	}
	public void Sync(Collection<String> vals)
	{
		LinkedList<String> newVals = new LinkedList<String>();
		// this run: add values from existing list, preserving their order.
		for (String val : m_vals)
		{
			if (vals.contains(val))
				newVals.add(val);
		}
		// next run: add values not in existing list, in the order of occurence
		// in new list.
		for (String val : vals)
		{
			if (!m_vals.contains(val))
				newVals.add(val);
		}
		m_vals = newVals;
		try
		{
			m_cmb.removeAll();
			for (String val : m_vals)
				m_cmb.add(val);
			if (m_vals.size() > 0)
				m_cmb.select(0);
		} catch (Exception e)
		{
		}
	}
	public void SetValues(String[] vals)
	{
		m_vals.clear();
		for (String s : vals)
			m_vals.add(s);
	}
	public void Save()
	{
		String txt = m_cmb.getText();
		if (txt.length() < 1)
			return;
		String s;
		int ind = -1;
		for (int i = 0; i < m_vals.size(); i++)
		{
			s = m_vals.get(i);
			if (s.compareTo(txt) == 0)
			{
				ind = i;
				break;
			}
		}
		if (ind < 0)
		{
			if (m_vals.size() >= m_mval)
				m_vals.removeLast();
			m_vals.add(0, txt);
		} else
		{
			m_vals.remove(ind);
			m_vals.add(0, txt);
		}
		Utils.SerializeListOfValues(m_uid, m_vals, false);
	}
}