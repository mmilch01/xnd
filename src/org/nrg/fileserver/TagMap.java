package org.nrg.fileserver;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

public class TagMap implements TagSet
{
	private TreeMap<String, ItemTag> m_tags = new TreeMap<String, ItemTag>();
	@Override
	public void clear()
	{
		m_tags.clear();
	}
	public TagMap()
	{
		super();
	}
	public TagMap(Collection<String> cs)
	{
		for (String s : cs)
			m_tags.put(s, new ItemTag(s));
	}
	public ItemTag get(String tag)
	{
		return m_tags.get(tag);
	}
	@Override
	public boolean isEmpty()
	{
		return m_tags.isEmpty();
	}

	@Override
	public int size()
	{
		return m_tags.size();
	}

	@Override
	public boolean remove(Object o)
	{
		Object res = null;
		try
		{
			res = remove(((ItemTag) o).GetName());
		} catch (Exception e)
		{
		}
		return (res != null);
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		Collection<ItemTag> new_values = new LinkedList<ItemTag>();
		new_values.removeAll(m_tags.values());
		clear();
		return addAll(new_values);
	}
	public void mergeTag(ItemTag o)
	{
		if (m_tags.containsKey(o.GetName()))
		{
			m_tags.get(o.GetName()).AddValues(o.GetValues());
		} else
			add(o);
	}
	@Override
	public boolean add(ItemTag o)
	{
		return m_tags.put(o.GetName(), o) != null;
	}
	@Override
	public boolean addAll(Collection<? extends ItemTag> c)
	{
		boolean bRes = true;
		for (ItemTag it : c)
			bRes &= add(it);
		return bRes;
	}
	public boolean mergeTags(Collection<? extends ItemTag> c)
	{
		for (ItemTag it : c)
		{
			if (m_tags.containsKey(it.GetName()))
				get(it.GetName()).AddValues(it.GetValues());
			else
				add(it);
		}
		return true;
	}
	@Override
	public boolean contains(Object o)
	{
		if (o instanceof String)
		{
			return m_tags.containsKey(o);
		}
		if (o instanceof ItemTag)
		{
			return m_tags.containsKey(((ItemTag) o).GetName());
		}
		return false;
	}
	@Override
	public boolean containsAll(Collection<?> c)
	{
		return m_tags.values().containsAll(c);
	}
	@Override
	public Iterator<ItemTag> iterator()
	{
		return m_tags.values().iterator();
	}
	@Override
	public boolean retainAll(Collection<?> c)
	{
		Collection<ItemTag> new_values = new LinkedList<ItemTag>();
		new_values.addAll(m_tags.values());
		new_values.retainAll(c);
		clear();
		return addAll(new_values);
	}
	@Override
	public String toString()
	{
		String res = "";
		for (ItemTag it : m_tags.values())
		{
			String[] vals = it.GetAllValues();
			res += it.GetName();
			for (String val : vals)
			{
				res += "	" + val;
			}
			res += "\n";
		}
		return res;
	}
	public TagSet fromString(String s)
	{
		String[] tags = s.split("\n");
		for (String tg : tags)
		{
			String[] vals = tg.split("	");
			if (vals.length < 1)
				continue;
			ItemTag it = new ItemTag(vals[0]);
			for (int i = 1; i < vals.length; i++)
				it.AddValue(vals[i]);
			add(it);
		}
		return this;
	}
	@Override
	public Object[] toArray()
	{
		return m_tags.values().toArray();
	}
	@Override
	public <T> T[] toArray(T[] a)
	{
		return m_tags.values().toArray(a);
	}
}