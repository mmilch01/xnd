package org.nrg.xnd.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.nrg.fileserver.CollectionManager;
import org.nrg.fileserver.Context;
import org.nrg.fileserver.FileCollection;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.LocalRepositoryManager;
import org.nrg.fileserver.RepositoryManager;
import org.nrg.fileserver.RestRepositoryManager;
import org.nrg.fileserver.TagMap;
import org.nrg.fileserver.TagSet;
import org.nrg.fileserver.XNATRestAdapter;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.filetransfer.FileDispatcherClient;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.utils.Utils;

public class RepositoryViewManager extends RepositoryManager
{
	private String[] m_VisibleTags = null;
	private boolean m_bTagView = false;
	private TreeSet<String> m_ManagedFolders = new TreeSet<String>();
	private RepositoryManager m_rm;
	private TreeMap<String, TagDescr> m_TagDescrs;
	private CollectionManager m_cm = new LocalCollectionManager(this);
	private FileDispatcherClient m_fdc;

	@Override
	public CollectionManager getCM()
	{
		return m_cm;
	}
	public TreeMap<String, TagDescr> GetTagDescrs()
	{
		return m_TagDescrs;
	}

	public void InitFileTransfer()
	{
		if (m_fdc != null)
			m_fdc.SessionEnd();
		m_fdc = new FileDispatcherClient();
		if (IsLocal())
		{

		}
		if (m_rm instanceof RestRepositoryManager)
		{
			m_fdc.SessionInit(XNDApp.app_Prefs.get("RemoteAddress",
					Utils.REMOTE_ADDRESS_DEFAULT), XNDApp.app_Prefs.getInt(
					"FileClientPort", Utils.PORT_FILE_DEFAULT), this);
		} else if (m_rm instanceof XNATRestAdapter)
		{
			m_fdc.SessionInit(XNDApp.app_Prefs.get("defaultXNATUploadServer",
					""), 80, this);
		}
	}
	public FileDispatcherClient GetFDC()
	{
		return m_fdc;
	}
	public Collection<String> GetAllFiles(ItemRecord ir)
	{
		if (!ir.isCollectionDefined())
		{
			Collection<String> cs = new LinkedList<String>();
			cs.add(ir.getRelativePath());
			return cs;
		} else
		{
			FileCollection fc = m_cm.GetCollection(ir.getColID());
			if (fc == null)
			{
				Collection<String> cs = new LinkedList<String>();
				cs.add(ir.getRelativePath());
				return cs;
			} else
			{
				TreeSet<String> cs = (TreeSet<String>) (m_cm.GetCollection(ir
						.getColID()).GetAllFiles());
				if (!cs.contains(ir.getRelativePath())) // ?? should be removed.
					cs.add(ir.getRelativePath());
				// cs.add(ir.GetRelativePath()); //??for hypo project only!!!
				return cs;
			}
		}
	}

	// Specific repository manager functions
	@Override
	public Collection<ItemRecord> DBItemFindEx(String rel_path,
			TagSet tagsMatching, Collection<String> tagsDefined,
			Collection<String> tagsUndefined)
	{
		return m_rm.DBItemFindEx(rel_path, tagsMatching, tagsDefined,
				tagsUndefined);
	}
	@Override
	public TreeMap<ItemTag, TagMap> DBTagValues(final Context path,
			final TagMap query_tags)
	{
		return m_rm.DBTagValues(path, query_tags);
	}

	@Override
	public boolean DBTagAttach(ItemRecord r, ItemTag t)
	{
		return m_rm.DBTagAttach(r, FilterTag(t));
	}
	@Override
	public boolean DBTagDetach(ItemRecord r, ItemTag t)
	{
		return m_rm.DBTagDetach(r, t);
	}
	public boolean DBTagAdd(TagDescr td)
	{
		if (!IsLocal())
			return false;
		if (m_rm.DBTagAdd(td.GetName()))
		{
			m_TagDescrs.put(td.GetName(), td);
			return true;
		}
		return false;
	}
	@Override
	public boolean DBTagAdd(String lbl)
	{
		return DBTagAdd(new TagDescr(lbl));
	}
	@Override
	public String[] DBTagFind(String name)
	{
		return m_rm.DBTagFind(name);
	}
	@Override
	public boolean DBTagDelete(String name)
	{
		if (!IsLocal())
			return false;
		m_TagDescrs.remove(name);
		return m_rm.DBTagDelete(name);
	}

	@Override
	public boolean DBItemAdd(ItemRecord item)
	{
		return m_rm.DBItemAdd(item);
	}
	@Override
	public boolean ItemRemove(ItemRecord template)
	{
		return m_rm.ItemRemove(template);
	}
	@Override
	public ItemRecord[] DBItemFind(ItemRecord t, int mr, boolean bAttachMD)
	{
		return m_rm.DBItemFind(t, mr, bAttachMD);
	}
	@Override
	public boolean SessionInit(Vector params)
	{
		return m_rm.SessionInit(params);
	}
	@Override
	public boolean SessionEnd()
	{
		return m_rm.SessionEnd();
	}
	public RepositoryManager GetRM()
	{
		return m_rm;
	}
	// View manager functions.
	// public RepositoryManager GetRepositoryManager(){return m_rm;}
	public ItemTag FilterTag(ItemTag tag)
	{
		TagDescr descr = m_TagDescrs.get(tag.GetName());
		if (descr == null)
			return tag;
		if (descr.IsSet(TagDescr.VALUE_ALPHANUMERIC))
		{
			tag.SetValue(Utils.StrFormat(tag.GetFirstValue()));
		}
		return tag;
	}
	public TagDescr GetTagDescr(String name)
	{
		return m_TagDescrs.get(name);
	}
	public void ToggleTagView()
	{
		m_bTagView = !m_bTagView;
	}
	public boolean IsTagView()
	{
		return m_bTagView;
	}
	public boolean IsLocal()
	{
		return (m_rm instanceof SimpleRepositoryManager || m_rm instanceof LocalRepositoryManager);
	}

	public RepositoryViewManager(RepositoryManager rm)
	{
		m_rm = rm;
		if (IsLocal())
			m_TagDescrs = new TreeMap<String, TagDescr>();
		else
			m_TagDescrs = XNDApp.app_localVM.GetTagDescrs();
		/*
		 * if(!IsLocal()) { if(m_rm instanceof RestRepositoryManager) { String[]
		 * tags=m_rm.DBTagFind("*"); TagDescr td; for(String nm:tags) {
		 * if((td=XNDApp.app_localVM.GetTagDescr(nm))==null) td=new
		 * TagDescr(nm); m_TagDescrs.put(nm, td); } } else if(m_rm instanceof
		 * XNATRestAdapter) { for(TagDescr
		 * td:DefaultOntologyManager.GetDefaultTagDescrs())
		 * m_TagDescrs.put(td.GetName(), td); } }
		 */
	}
	public String[] GetManagedFolders()
	{
		if (!IsLocal())
			return new String[0];
		String[] arr = new String[m_ManagedFolders.size() + 1];
		m_ManagedFolders.toArray(arr);
		arr[arr.length - 1] = Utils.GetIncomingFolder();
		return arr;
	}

	public ItemRecord CreateItemRecord(String abs_path)
	{
		return new ItemRecord(abs_path, GetRelativePath(abs_path));
	}

	/**
	 * Resolve absolute path for local file system for a given xnd relative
	 * path.
	 * 
	 * @param rel_path
	 *            relative path to resource, e.g. /root1/file1
	 * @return
	 */

	public String GetAbsolutePath(String rel_path)
	{
		// rel_path: /root/subfold/file
		// absolute path: c://prefix/root/subfold/file
		// root path: c://prefix/root
		// subfold path: c://prefix/root/subfold

		String rp = rel_path;
		while (rp.length() > 0 && (rp.startsWith("/") || rp.startsWith("\\")))
			rp = rp.substring(1);
		if (rp.length() < 1)
			rp = rel_path;
		File f;
		for (final String fold : GetManagedFolders())
		{
			f = (new File(fold));
			String name = f.getName();
			if (rp.startsWith(name))
			{
				return f.getAbsolutePath() + rp.substring(name.length());
			}
		}
		return rel_path;
	}
	public String GetParentPath(String rel_path)
	{
		return GetRelativePath(new File(GetAbsolutePath(rel_path)).getParent());
	}
	public String GetRelativePath(String abs_path)
	{
		String res;
		final String uplFold = new File(Utils.GetIncomingFolder() + "/uploads")
				.getAbsolutePath();
		for (final String fold : GetManagedFolders())
		{
			if (fold == null || fold.length() < 1)
				continue;
			res = ItemRecord.relativeFromAbsolute(abs_path, fold, uplFold
					.compareTo(fold) != 0);
			if (res != null)
				return res;
		}
		return null; // not found
	}
	public boolean CheckTagProperty(String name, int property)
	{
		TagDescr td = m_TagDescrs.get(name);
		if (td == null)
			return false;
		return td.IsSet(property);
	}
	/**
	 * Get an array of visible tag column names
	 * 
	 * @return
	 */
	public String[] GetTableTags()
	{
		UpdateVisibleTags();
		return m_VisibleTags;
	}
	public void UpdateVisibleTags()
	{
		LinkedList<TagDescr> v = new LinkedList<TagDescr>();
		ItemTag[] tags = DefaultOntologyManager.GetDefaultTags();
		TagDescr item;
		// for(ItemTag it:tags)

		for (ItemTag it : tags)
		{
			item = m_TagDescrs.get(it.GetName());
			if (item != null && item.IsSet(TagDescr.TABLE_DISPLAY))
				v.add(item);
		}
		for (TagDescr td : m_TagDescrs.values())
		{
			// item=m_TagDescrs.get(it.GetName());
			// if(item!=null && item.IsSet(TagDescr.TABLE_DISPLAY)) v.add(item);
			if (!td.IsSet(TagDescr.DEFAULT) && td.IsSet(TagDescr.TABLE_DISPLAY))
				v.add(td);
		}

		m_VisibleTags = new String[v.size()];
		for (int i = 0; i < v.size(); i++)
			m_VisibleTags[i] = v.get(i).GetName();
	}
	public boolean CanAddManagedRoot(String s)
	{
		if (!IsLocal())
			return false;
		for (String fold : GetManagedFolders())
		{
			if (Utils.CrossCheckDirs(s, fold) != 0)
				return false;
			if (Utils.MatchFolderNames(fold, s))
				return false;
		}
		return true;
	}
	public void AddManagedFolder(String s)
	{
		m_ManagedFolders.add(s);
	}
	public void RemoveManagedFolder(String s)
	{
		m_ManagedFolders.remove(s);
	}
	public TagDescr[] GetVisibleTagList()
	{
		LinkedList<TagDescr> res = new LinkedList<TagDescr>();
		for (TagDescr td : m_TagDescrs.values())
		{
			if (td.IsSet(TagDescr.INVISIBLE))
				continue;
			res.add(td);
		}
		return res.toArray(new TagDescr[0]);
	}
	/*
	 * public void UpdateTagDefault(String name, boolean bDefault) {
	 * if(!IsLocal()) return; TagDescr item=(TagDescr)(m_TagDescrs.get(name));
	 * if(item==null) //tag attributes not stored yet { String[]
	 * names=m_rm.DBTagFind(name); if(names.length<1) return; item=new
	 * TagDescr(names[0]); m_TagDescrs.put(item.GetName(), item); }
	 * item.SetAttr(bDefault?TagDescr.DEFAULT:0); }
	 */
	public boolean UpdateTagShow(String name, boolean bShow)
	{
		TagDescr item = (m_TagDescrs.get(name));
		if (IsLocal())
		{
			if (item == null) // tag attributes not stored yet
			{
				String[] names = m_rm.DBTagFind(name);
				if (names.length < 1)
					return false;
				item = new TagDescr(names[0]);
				m_TagDescrs.put(item.GetName(), item);
			}
		}
		if (item.IsSet(TagDescr.TABLE_DISPLAY) != bShow)
		{
			item.ToggleAttr(TagDescr.TABLE_DISPLAY);
			UpdateVisibleTags();
			return true;
		}
		return false;
	}
	public void InsertSystemTags()
	{
		if (!IsLocal())
			return;
		TagDescr[] tagDescrs = DefaultOntologyManager.GetDefaultTagDescrs();
		{
			for (int i = 0; i < tagDescrs.length; i++)
			{
				// if(!m_TagDescrs.containsKey(tagDescrs[i].GetName()))
				{
					m_TagDescrs.put(tagDescrs[i].GetName(), tagDescrs[i]);
					m_rm.DBTagAdd(tagDescrs[i].GetName());
				}
			}
		}
		// ??
		m_rm.DBTagAdd("Collection_ID");
	}
	public boolean Serialize(String fname, boolean is_loading)
	{
		if (is_loading)
		{
			boolean bRes = false;
			ObjectInputStream in = null;
			try
			{
				in = new ObjectInputStream(new FileInputStream(fname));
				if (!Utils.SerializeTreeMap(in, m_TagDescrs,
						Utils.SER_OBJ_TAGATTR, is_loading))
					return false;
				if (!Utils.SerializeCollection(in, m_ManagedFolders,
						Utils.SER_OBJ_STRING, is_loading))
					return false;
				if (m_rm instanceof SimpleRepositoryManager)
					bRes = ((SimpleRepositoryManager) m_rm).Serialize(in,
							is_loading);
			} catch (Exception e)
			{
				return false;
			} finally
			{
				InsertSystemTags();
				if (in != null)
					try
					{
						in.close();
					} catch (Exception e)
					{
					};
			}
			return bRes;
		} else
		{
			ObjectOutputStream out = null;
			try
			{
				out = new ObjectOutputStream(new FileOutputStream(fname));
				if (!Utils.SerializeTreeMap(out, m_TagDescrs,
						Utils.SER_OBJ_TAGATTR, is_loading))
					return false;
				if (!Utils.SerializeCollection(out, m_ManagedFolders,
						Utils.SER_OBJ_STRING, is_loading))
					return false;
				out.flush();
				if (m_rm instanceof SimpleRepositoryManager)
					return ((SimpleRepositoryManager) m_rm).Serialize(out,
							is_loading);
			} catch (Exception e)
			{
				return false;
			} finally
			{
				if (out != null)
					try
					{
						out.close();
					} catch (Exception e)
					{
					};
			}
		}
		return true;
	}
}