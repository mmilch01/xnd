package org.nrg.xnd.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.nrg.fileserver.Context;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.RepositoryManager;
import org.nrg.fileserver.TagMap;
import org.nrg.fileserver.TagSet;
import org.nrg.fileserver.XNATRestAdapter;
import org.nrg.xnd.app.IImageKeys;
import org.nrg.xnd.ontology.DefaultOntologyManager;

public class VirtualFolder extends CElement
{
	private Context m_Context = new Context();
	private ItemTag m_QueryTag;
	private ItemRecord m_AssocTags;

	@Override
	public Image GetImage()
	{
		return IImageKeys.GetImage(IImageKeys.DB);
	}
	public Context getContext()
	{
		return m_Context;
	}

	public ItemRecord getAssociatedTags()
	{
		return m_AssocTags;
	}
	@Override
	public ImageDescriptor GetImageDescriptor()
	{
		return IImageKeys.GetImDescr(IImageKeys.DB);
	}
	public VirtualFolder(Context context, RepositoryManager rm,
			CElement parent, Collection<ItemTag> cit)
	{
		super(rm, parent);
		if (context == null || context.size() < 1)
			m_QueryTag = new ItemTag("*");
		else
		{
			m_QueryTag = context.getLast();
			m_Context.addAll(context);
		}
		m_AssocTags = new ItemRecord(null, GetContextPath());
		if (cit != null)
			m_AssocTags.tagsSet(cit);
	}
	public String GetContextPath()
	{
		String res = "/";
		for (ItemTag it : m_Context)
			res += it.GetFirstValue() + "/";
		return res;
	}
	public VirtualFolder(ItemTag qt, Context context, RepositoryManager rm,
			CElement parent, TagSet cit)
	{
		super(rm, parent);
		m_QueryTag = new ItemTag(qt.GetName(), qt.GetFirstValue());
		m_Context = new Context();
		m_Context.addAll(context);
		m_Context.add(m_QueryTag);
		m_AssocTags = new ItemRecord(null, GetContextPath());
		if (cit != null)
			m_AssocTags.tagsSet(cit);
	}

	public boolean HasChildren()
	{
		return (DefaultOntologyManager.ChildTagsCustom(m_Context).size() > 0);
	}

	public boolean IsRoot()
	{
		Collection<String> roots = DefaultOntologyManager.GetRoots();
		String tname = m_QueryTag.GetName();
		for (String s : roots)
			if (tname.compareTo(s) == 0)
				return true;
		return false;
	}

	@Override
	public void ApplyOperation(Object operation, int how,
			IProgressMonitor monitor)
	{
		Collection<CElement> cce = GetChildren(new TypeFilter(), monitor);
		if (cce == null)
			return;
		for (CElement ce : cce)
			ce.ApplyOperation(operation, how, monitor);
		Invalidate();
	}

	@Override
	public Collection<CElement> GetChildren(TypeFilter tf,
			IProgressMonitor monitor)
	{
		if (m_bNeedUpdateChildren)
		{
			try
			{
				boolean bXNAT = m_rm instanceof XNATRestAdapter;
				// children at current context.
				LinkedList<String> childTags = (LinkedList<String>) DefaultOntologyManager
						.ChildTagsCustom(m_Context);
				// tag-based collection representing context at current level.
				TagMap q_tags = new TagMap();
				for (String nm : childTags)
					q_tags.add(new ItemTag(nm, null, true));
				//				
				// generate a map of each query tag to its immediate children.
				TreeMap<String, Collection<String>> tagMap = DefaultOntologyManager
						.ChildMap(m_Context, childTags);

				TreeMap<ItemTag, TagMap> aTags = m_rvm.DBTagValues(m_Context,
						q_tags);
				Collection<ItemRecord> cir;
				if (bXNAT)
				{
					if (m_Context.size() > 0)
					{
						LinkedList<String> def_tags = new LinkedList<String>();
						for (String s : childTags)
							def_tags.add(s);
						cir = m_rvm.DBItemFindEx("*", m_Context, def_tags,
								childTags);
					} else
						cir = new LinkedList<ItemRecord>();
				} else
					cir = m_rvm.DBItemFindEx("*", m_Context,
							new LinkedList<String>(), childTags);
				m_Children = new LinkedList<CElement>();
				ItemTag q_tag;
				for (ItemTag it : aTags.keySet())
				{
					m_Children
							.add(new VirtualFolder(it, m_Context, m_rvm != null
									? m_rvm
									: m_rvm, this, aTags.get(it)));
					// q_tag=llit.get(0);
					// llit.remove(0);
					// m_Children.add(new
					// VirtualFolder(q_tag,m_Context,m_rvm!=null?m_rvm:m_rvm,this,llit));
				}
				/*
				 * for(ItemTag it:q_tags) { for(String val:it.GetAllValues()) {
				 * if(val!=null || !bXNAT) m_Children.add(new VirtualFolder(new
				 * ItemTag(it.GetName(),val,false),m_Context,m_rvm,this)); } }
				 */
				for (ItemRecord ir : cir)
				{
					m_Children.add(new DBElement(ir, m_rvm, this));
				}
				if (!IsRoot())
					m_Children.add(new EmptyElement(m_rvm, this));
				Collections.sort((LinkedList) m_Children);
				m_bNeedUpdateChildren = false;
			} catch (Exception e)
			{
				m_Children = null;
				e.printStackTrace();
			}
		}
		if (tf != null)
			return tf.Filter(m_Children);
		else
			return m_Children;
	}
	public String VirtualPath()
	{
		if (m_Context.size() < 1)
		{
			return m_QueryTag.GetFirstValue();
		} else
		{
			String res = "";
			for (ItemTag it : m_Context)
				res = res + it.GetFirstValue() + "/";
			return res;
		}
	}
	public ItemTag getQueryTag()
	{
		return m_QueryTag;
	}
	@Override
	public String GetLabel()
	{
		return m_QueryTag.GetName() + ":" + m_QueryTag.GetFirstValue();
		/*
		 * String s=""; for(ItemTag it:m_Context)
		 * s=s+"/"+it.GetName()+":"+it.GetFirstValue(); return s;
		 */
	}

	@Override
	public boolean IsManaged()
	{
		return true;
	}

	@Override
	protected void UpdateParent()
	{
		if (m_bNeedUpdateParent)
		{
			if (IsRoot())
				m_Parent = new FSRoot(m_rvm, null);
			else
			{
				if (m_Context != null && m_Context.size() > 1)
				{
					m_Parent = new VirtualFolder(new Context(m_Context.subList(
							0, m_Context.size() - 2)), m_rvm, null, m_AssocTags
							.getTagCollection());
				} else
					m_Parent = new FSRoot(m_rvm, null);
			}
			m_bNeedUpdateParent = false;
		}
	}

	@Override
	public int compareTo(CElement ce)
	{
		if (ce instanceof VirtualFolder)
		{
			String lbl0 = GetLabel(), lbl1 = ce.GetLabel();
			if (lbl0.startsWith("Scan:") && lbl1.startsWith("Scan:"))
			{
				try
				{
					Integer sNum = Integer.decode(lbl0.substring(5));
					Integer sNum1 = Integer.decode(lbl1.substring(5));
					return sNum.compareTo(sNum1);
				} catch (Exception e)
				{
					return lbl0.compareTo(lbl1);
				}
			}
			return lbl0.compareTo(lbl1);
		}
		if (ce instanceof EmptyElement)
			return 1;
		return -1;
	}

}
