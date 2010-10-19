package org.nrg.xnd.model;

import java.io.File;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

import org.nrg.fileserver.CollectionManager;
import org.nrg.fileserver.FileCollection;
import org.nrg.xnd.utils.Utils;

public class LocalCollectionManager implements CollectionManager
{
	private TreeSet<String> m_cols = new TreeSet<String>();
	private boolean bNeedRefresh = true;
	private TreeMap<String, FileCollection> m_ColCache = new TreeMap<String, FileCollection>();
	private final static int m_CacheSize = 50;
	private RepositoryViewManager m_rvm;
	private FileCollection m_LastCollection = null;

	public LocalCollectionManager(RepositoryViewManager rvm)
	{
		m_rvm = rvm;
		Refresh();
	}
	public void Refresh()
	{
		if (bNeedRefresh)
		{
			File f = Utils.GetCollectionFolder();
			if (!f.exists())
				return;
			File[] cols = f.listFiles();
			String s;
			for (File col : cols)
			{
				s = col.getName();
				if (!m_cols.contains(s))
					m_cols.add(s);
			}
			bNeedRefresh = false;
		}
	}
	public void AddCollection(FileCollection fc)
	{
		m_cols.add(fc.GetID());
		fc.Serialize(false);
		// bNeedRefresh=true;
	}
	public void RemoveCollection(String col_id)
	{
		FileCollection fc = GetCollection(col_id);
		if (fc != null)
		{
			m_cols.remove(col_id);
			m_ColCache.remove(col_id);
			fc.Delete();
			m_LastCollection = null;
			bNeedRefresh = true;
		}
	}
	public boolean Contains(String col_id)
	{
		return m_cols.contains(col_id);
	}
	private void AddToCache(FileCollection cf)
	{
		if (m_ColCache.size() > m_CacheSize)
			m_ColCache.clear();
		m_ColCache.put(cf.GetID(), cf);
	}
	public FileCollection FindCollection(File f)
	{
		try
		{
			if (m_LastCollection != null)
			{
				if (m_LastCollection.ContainsFile(m_rvm.GetRelativePath(f
						.getAbsolutePath())))
					return m_LastCollection;
			}
			Refresh();
			String from = Utils.MaskPath(m_rvm.GetRelativePath(f.getParent()));
			String to = from;
			char ch;
			char[] charr = to.toCharArray();
			for (int i = charr.length - 1; i >= 0; i--)
			{
				if (Utils.ValidChar(charr[i]))
				{
					charr[i] = Utils.NextValidChar(charr[i]);
					break;
				}
			}
			to = new String(charr);
			Collection<String> cols = m_cols.subSet(from, to);
			FileCollection fc;
			for (String id : cols)
			{
				fc = GetCollection(id);
				if (fc.ContainsFile(m_rvm.GetRelativePath(f.getAbsolutePath())))
				{
					m_LastCollection = fc;
					return fc;
				}
			}
		} catch (Exception e)
		{
			return null;
		}
		return null;
	}

	public LocalFileCollection CreateCollection(String prefix,
			boolean bGenerateColUID)
	{
		return new LocalFileCollection(prefix, bGenerateColUID);
	}
	public FileCollection GetCollection(String col_id)
	{
		if (!Contains(col_id))
			return null;
		if (m_ColCache.containsKey(col_id))
			return m_ColCache.get(col_id);

		LocalFileCollection cf = CreateCollection(col_id, false);// new
		// LocalFileCollection(col_id,false);
		if (!cf.Serialize(true))
			return null;
		AddToCache(cf);
		return cf;
	}
}
