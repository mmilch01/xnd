package org.nrg.fileserver;

import java.io.File;
import java.util.Collection;

import org.nrg.xnd.utils.FSObject;
import org.nrg.xnd.utils.Utils;

public class ItemRecord implements Comparable
{
	protected TagMap m_Tags = new TagMap();
	protected String m_AbsolutePath = "";
	protected String m_RelativePath = "";

	public boolean isCollectionDefined()
	{
		return getTag("Collection_ID") != null;
	}
	public String getCollectionName()
	{
		final String[] def_tags = {"Project", "Subject", "Experiment", "Scan"};
		String tval;
		String r = "";
		for (String s : def_tags)
		{
			tval = this.getTagValue(s);
			if (tval == null)
				tval = "";
			if (r.length() > 0)
				r = r + "_" + tval;
			else
				r = tval;
		}
		return r;
	}
	public String getColID()
	{
		return getTagValue("Collection_ID");
	}
	@Override
	public int compareTo(Object o)
	{
		if (m_AbsolutePath != null)
			return m_AbsolutePath.compareTo(((ItemRecord) o).getAbsolutePath());
		if (m_RelativePath != null)
			return m_RelativePath.compareTo(((ItemRecord) o).getRelativePath());
		else
			return 0;
	}

	public FSObject[] listFiles(boolean bCheckDirs)
	{
		if (m_AbsolutePath == null || m_AbsolutePath.length() < 1)
			return new FSObject[0];
		File f = new File(m_AbsolutePath);
		// File[] res;
		try
		{
			// res=f.listFiles();
			return Utils.m_nfm.ListFiles(f, bCheckDirs);
		} catch (Exception e)
		{
			return new FSObject[0];
		}
	}
	public File getFile()
	{
		if (m_AbsolutePath != null && m_AbsolutePath.length() > 0)
			return new File(m_AbsolutePath);
		return null;
	}

	public String getFileParent()
	{
		File f = getFile();
		if (f == null)
			return "";
		return f.getParent();
	}

	public String getFileName()
	{
		File f = getFile();
		if (f == null && m_RelativePath != null)
			f = new File(m_RelativePath);
		if (f != null)
			return f.getName();
		return null;
	}

	public ItemRecord(ItemRecord ir)
	{
		m_Tags.addAll(ir.getTagCollection());
		m_AbsolutePath = ir.m_AbsolutePath;
		m_RelativePath = ir.m_RelativePath;
	}
	public String printTags()
	{
		ItemTag[] tags = getAllTags();
		String s = "";
		if (tags.length < 1)
			return s;
		for (int i = 0; i < tags.length; i++)
		{
			s = s + tags[i].PrintTag() + "\n";
		}
		return s;
	}
	public String getAbsolutePath()
	{
		return m_AbsolutePath;
	}
	public String getRelativePath()
	{
		if (m_RelativePath == null)
			return null;
		while (m_RelativePath.startsWith("/"))
			m_RelativePath = m_RelativePath.substring(1);
		return m_RelativePath.replace('\\', '/');
	}

	public ItemRecord(String abs_path, String rel_path)
	{
		m_AbsolutePath = abs_path;
		m_RelativePath = rel_path;
	}
	/**
	 * @param abs_path
	 *            <prefix>/<root_fold>/<suffix>/<file>
	 * @param root
	 *            <prefix>/<root_fold>
	 * @param bInclRoot
	 * @return <root_fold>/<suffix>/<file>, if bInclRoot=true; <suffix>/<file>,
	 *         otherwise
	 * 
	 */
	public static String relativeFromAbsolute(String abs_path, String root,
			boolean bInclRoot)
	{
		String res;
		if (abs_path != null && root != null && root.length() > 0
				&& abs_path.startsWith(root))
		{
			res = abs_path.substring(root.length());
			while ((res.startsWith("/") || res.startsWith("\\")))
				res = res.substring(1);
		} else
			res = null;
		if (!bInclRoot)
			return res;

		// add root
		if (res != null)
		{
			if (res.length() > 0)
				return (new File(root).getName() + "/" + res)
						.replace('\\', '/');
			else
				return new File(root).getName().replace('\\', '/');
		}
		return res;
	}
	public String getTagValue(String name)
	{
		String res = null;
		try
		{
			res = m_Tags.get(name).GetFirstValue();
		} catch (Exception e)
		{
		}
		return res;
	}
	public void tagSet(ItemTag tag)
	{
		if (tag == null)
			return;
		m_Tags.remove(tag);
		m_Tags.add(tag);
	}
	public void tagsMerge(TagSet tags)
	{
		m_Tags.mergeTags(tags);
		/*
		 * ItemTag lit; for (ItemTag it : tags) { lit =
		 * m_Tags.get(it.GetName()); if (lit == null) { m_Tags.put(it.GetName(),
		 * it); } else { lit.SetAllowMultiple(true);
		 * lit.AddValues(it.GetValues()); } }
		 */
	}
	public void tagsSet(Collection<ItemTag> tags)
	{
		m_Tags.clear();
		m_Tags.addAll(tags);
	}
	public void setAbsolutePath(String new_path)
	{
		m_AbsolutePath = new_path;
	}
	public void tagsSetFromArray(ItemTag[] tags)
	{
		for (ItemTag it : tags)
			m_Tags.add(it);
	}
	public void tagRemove(String name)
	{
		m_Tags.remove(name);
	}
	public void removeAllTags()
	{
		m_Tags.clear();
	}
	public ItemTag getTag(String name)
	{
		return m_Tags.get(name);
	}
	public TagSet getTagCollection()
	{
		return m_Tags;
	}
	public ItemTag[] getAllTags()
	{
		return m_Tags.toArray(new ItemTag[0]);
	}
}