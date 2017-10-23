package org.nrg.xnd.model;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.TreeSet;

import org.nrg.fileserver.FileCollection;
import org.nrg.xnd.utils.FSObject;
import org.nrg.xnd.utils.Utils;

/**
 * @author mmilch Collection for storing URI's of local files which implements
 *         persistent representation. Each collection is associated with a file
 *         that contains the list of collection resources.
 * 
 */
public class LocalFileCollection implements FileCollection
{
	private String m_ID;
	private TreeSet<String> m_Files = new TreeSet<String>();
	private FSObject m_fso;

	@Override
	public void Delete()
	{
		if (!m_fso.delete())
			m_fso.deleteOnExit();
	}

	@Override
	public boolean ContainsFile(String rel_path)
	{
		return m_Files.contains(rel_path);
	}
	public LocalFileCollection(String s, boolean bGenerateID)
	{
		if (bGenerateID)
		{
			m_ID = Utils.MaskPath(s) + Utils.PseudoUID("");
		} else
			m_ID = s;
		m_fso = new FSObject(Utils.GetCollectionFolder().getAbsolutePath()
				+ "/" + m_ID);
	}
	@Override
	public String GetID()
	{
		return m_ID;
	}
	@Override
	public void AddFile(String rel_path)
	{
		m_Files.add(rel_path);
	}
	@Override
	public Collection<String> GetAllFiles()
	{
		return m_Files;
	}

	@Override
	public boolean Serialize(boolean is_loading)
	{
		if (is_loading)
		{
			if (!m_fso.exists())
				return false;
			try
			{
				ObjectInputStream ois = new ObjectInputStream(
						new FileInputStream(m_fso));
				int nitems = ois.readInt();
				String s;
				for (int i = 0; i < nitems; i++)
				{
					s = Utils.SerializeString(ois, null, true);
					m_Files.add(s);
				}
				ois.close();
			} catch (Exception e)
			{
				return false;
			}
		} else
		{
			try
			{
				if (!m_fso.exists())
				{
					if (!m_fso.createNewFile())
						return false;
				}
				ObjectOutputStream oos = new ObjectOutputStream(
						new FileOutputStream(m_fso));
				oos.writeInt(m_Files.size());
				for (String s : m_Files)
				{
					Utils.SerializeString(oos, s, false);
				}
				oos.flush();
				oos.close();
			} catch (Exception e)
			{
				return false;
			}
		}
		return true;
	}
}