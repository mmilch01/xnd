package org.nrg.xnd.model;

import java.util.Collection;
import java.util.LinkedList;

public class TypeFilter
{
	private int m_Type = 0;
	public static final int FSFILE = 128, FSFOLDER = 1, DBITEM = 2,
			VFOLDER = 4, COLLECTION = 8, RESOURCE = 16, ROOT = 32, EMPTY = 64;
	private static final int[] m_types = {FSFILE, FSFOLDER, DBITEM, VFOLDER,
			COLLECTION, RESOURCE, ROOT, EMPTY};
	private static int m_FullType;
	{
		m_FullType = 0;
		for (int i : m_types)
			m_FullType |= i;
	}
	public TypeFilter(int type, boolean bExclude)
	{
		if (!bExclude)
			m_Type = type;
		else
			m_Type = m_FullType & (~type);
	}
	public TypeFilter()
	{
		m_Type = m_FullType;
	}
	public boolean NeedToFilter()
	{
		return m_Type != m_FullType;
	}
	public boolean Contains(int code)
	{
		return (m_Type & code) != 0;
	}

	public Collection<CElement> Filter(Collection<CElement> cce)
	{
		if (!NeedToFilter() || cce == null)
			return cce;
		Collection<CElement> filtered = new LinkedList<CElement>();
		for (CElement ce : cce)
			if (Match(ce))
				filtered.add(ce);
		return filtered;
	}
	public TypeFilter(boolean bTree)
	{
		if (bTree)
			m_Type = FSFOLDER | VFOLDER | COLLECTION;
		else
			m_Type = m_FullType;
	}
	public boolean Match(CElement ce)
	{
		if (ce instanceof FSFile)
		{
			if ((m_Type & FSFILE) != 0)
				return true;
			else
				return false;
		}
		if (ce instanceof Resource)
		{
			if ((m_Type & RESOURCE) != 0)
				return true;
			else
				return false;
		}
		if (ce instanceof DBElement)
		{
			if ((m_Type & DBITEM) == 0)
			{
				if ((m_Type & COLLECTION) == 0)
					return false;
				else
				{
					return ((DBElement) ce).IsCollection();
				}
			}
			if ((m_Type & COLLECTION) == 0)
			{
				return !((DBElement) ce).IsCollection();
			} else
				return true;
		}
		if (ce instanceof FSFolder || ce instanceof FSRoot)
		{
			if ((m_Type & FSFOLDER) != 0)
				return true;
			else
				return false;
		}
		if (ce instanceof VirtualFolder)
		{
			if ((m_Type & VFOLDER) != 0)
				return true;
			else
				return false;
		}
		if (ce instanceof RootElement)
		{
			if ((m_Type & ROOT) != 0)
				return true;
			else
				return false;
		}
		if (ce instanceof EmptyElement)
		{
			if ((m_Type & EMPTY) != 0)
				return true;
			else
				return false;
		}
		return false;
	}
}