package org.nrg.xnd.model;

import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.TreeMap;

import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.RepositoryManager;
import org.nrg.xnd.utils.Utils;

public class SimpleRepositoryManager extends RepositoryManager
{
	static final long serialVersionUID = 1;
	private TreeMap<String, ItemRecord> m_Items;
	private TreeMap<String, ItemTag> m_Tags;
	/**
	 * 
	 * Default constructor.
	 */
	public SimpleRepositoryManager()
	{
		m_Items = new TreeMap<String, ItemRecord>();
		m_Tags = new TreeMap<String, ItemTag>();
	}
	@Override
	public boolean DBTagAttach(ItemRecord r, ItemTag t)
	{
		ItemRecord ir = m_Items.get(r.getAbsolutePath());
		if (ir != null)
			ir.tagSet(t);
		return true;
	}
	@Override
	public boolean DBTagDetach(ItemRecord r, ItemTag t)
	{
		ItemRecord ir = m_Items.get(r.getAbsolutePath());
		if (ir != null)
			ir.tagRemove(t.GetName());
		return true;
	}
	@Override
	public boolean DBTagAdd(String lbl)
	{
		return (null != m_Tags.put(lbl, new ItemTag(lbl)));
	}
	@Override
	public String[] DBTagFind(String name)
	{
		if (m_Tags.containsKey(name))// contains(new TagAttr(name)))
		{
			String[] found = new String[1];
			found[0] = m_Tags.get(name).GetName();
			return found;
		}
		return new String[0];
	}
	@Override
	public boolean DBTagDelete(String name)
	{
		if (m_Tags.remove(name) != null)
		{
			for (final ItemRecord item : m_Items.values())
				item.tagRemove(name);
			return true;
		}
		return false;
	}
	@Override
	public boolean DBItemAdd(ItemRecord item)
	{
		return (null != m_Items.put(item.getAbsolutePath(), item));
	}
	@Override
	public boolean ItemRemove(ItemRecord template)
	{
		return (null != m_Items.remove(template.getAbsolutePath()));
	}
	@Override
	public ItemRecord[] DBItemFind(ItemRecord template, int maxRecord,
			boolean bAttachMD)
	{
		if (template.getAbsolutePath().startsWith("*")) // template search
		{
			LinkedList<ItemRecord> ll = new LinkedList<ItemRecord>();
			for (final ItemRecord ir : m_Items.values())
				if (DBMatch(ir, template, bAttachMD))
					ll.add(ir);
			return ll.toArray(new ItemRecord[0]);
		} else
		{
			if (!m_Items.containsKey(template.getAbsolutePath()))
				return (new ItemRecord[0]);
			ItemRecord[] rc = new ItemRecord[1];
			rc[0] = m_Items.get(template.getAbsolutePath());
			return rc;
		}
	}
	public boolean Serialize(Object stream, boolean is_loading)
	{
		if (!Utils.SerializeTreeMap(stream, m_Tags, Utils.SER_OBJ_ITEMTAG,
				is_loading))
			return false;
		if (!Utils.SerializeTreeMap(stream, m_Items, Utils.SER_OBJ_ITEMRECORD,
				is_loading))
			return false;
		if (stream instanceof ObjectOutputStream)
			try
			{
				((ObjectOutputStream) stream).flush();
			} catch (Exception e)
			{
			}
		return true;
	}
}