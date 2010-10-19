package org.nrg.xnd.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.ConsoleView;
import org.nrg.xnd.app.IImageKeys;
import org.nrg.xnd.filetransfer.FileDispatcherClient;
import org.nrg.xnd.rules.NameRule;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.ui.dialogs.DownloadDialog;

public class DBElement extends CElement
{
	private ItemRecord m_ir;
	private boolean m_bUpdateIR = false;

	@Override
	public void Invalidate()
	{
		super.Invalidate();
		m_bUpdateIR = true;
	}
	public DBElement(ItemRecord ir, RepositoryViewManager rvm, CElement parent)
	{
		super(rvm, parent);
		m_ir = ir;
	}
	private void UpdateIR()
	{
		if (m_bUpdateIR)
		{
			ItemRecord[] found = m_rvm.DBItemFind(m_ir, 1, true);
			if (found.length > 0)
				m_ir = found[0];
			m_bUpdateIR = false;
		}
	}

	public static DBElement CreateDBE(File f, RepositoryViewManager rvm,
			CElement parent)
	{
		ItemRecord ir = new ItemRecord(f.getAbsolutePath(), rvm
				.GetRelativePath(f.getAbsolutePath()));
		ItemRecord[] found = rvm.DBItemFind(ir, 1, true);
		if (found.length > 0)
			return new DBElement(found[0], rvm, parent);
		else
			return null;
	}
	@Override
	public Image GetImage()
	{
		if (IsCollection())
			return IImageKeys.GetImage(IImageKeys.COLLECTION);
		else
			return IImageKeys.GetImage(IImageKeys.DR);
	}
	@Override
	public ImageDescriptor GetImageDescriptor()
	{
		if (IsCollection())
			return IImageKeys.GetImDescr(IImageKeys.COLLECTION);
		else
			return IImageKeys.GetImDescr(IImageKeys.DR);
	}
	@Override
	public void ApplyOperation(Object o, int how, IProgressMonitor monitor)
	{
		if (monitor != null && monitor.isCanceled())
			return;
		if (monitor != null)
			monitor.subTask("File: " + GetLabel());

		// name rule is applied on folders only
		if (o instanceof NameRule)
			return;

		if (o instanceof Rule)
		{
			((Rule) o).ApplyRule(this, monitor);
		} else
		{
			ItemTag[] lbl = null;
			if (o instanceof ItemTag[])
				lbl = (ItemTag[]) o;
			switch (how)
			{
				case SETTAGS : // attach a tag
					for (ItemTag it : lbl)
						m_rvm.DBTagAttach(m_ir, it);
					break;
				// case SETDEFAULTTAGS : // attach a tag
				// for (ItemTag it : lbl)
				// m_rvm.DBTagAttach(m_ir, it);
				// break;
				case REMOVETAGS : // remove a tag
					if (m_ir == null)
						return;
					for (ItemTag it : lbl)
						m_rvm.DBTagDetach(m_ir, it);
					break;
				case UNMANAGEALL : // stop managing
					m_rvm.ItemRemove(m_ir);
					if (IsCollection())
						m_rvm.getCM().RemoveCollection(m_ir.getColID());
					break;
				case UNMANAGE :
					m_rvm.ItemRemove(m_ir);
					if (IsCollection())
						m_rvm.getCM().RemoveCollection(m_ir.getColID());
					break;
				case REMOVE_FROM_ROOTS :
					m_rvm.ItemRemove(m_ir);
					if (IsCollection())
						m_rvm.getCM().RemoveCollection(m_ir.getColID());
					break;
				case SENDTO :
					if (m_rvm == null)
						break;
					if (m_rvm.IsLocal())
					{
						((FileDispatcherClient) o).AddToUploadQueue(this);
						// m_rvm.GetFDC().AddToUploadQueue(this);
					} else
					{
						if (IsCollection())
						{
							ConsoleView
									.AppendMessage("downloading collection "
											+ GetLabel()
											+ " error: collection download is not supported in this version of xnd");
						} else
						{
							DownloadDialog dd = (DownloadDialog) o;
							ViewFilter vf = dd.GetFilter();
							if (vf != null)
							{
								if (!vf.Match(m_ir))
									return;
							}
							m_rvm.GetFDC().AddToDownloadQueue(this,
									dd.GetDownFolder());
						}
					}
			}
		}
		Invalidate();
	}
	public ItemRecord GetIR()
	{
		UpdateIR();
		return m_ir;
	}
	public boolean IsCollection()
	{
		UpdateIR();
		return (m_ir.isCollectionDefined());
	}
	/*
	 * public Collection<FSFile> ConvertToFSFile() { String[]
	 * res=m_ir.GetAllResources(); LinkedList<FSFile> lf=new
	 * LinkedList<FSFile>(); for(String rp:res) { lf.add(new FSFile(new
	 * File(m_rvm.GetAbsolutePath(rp)),m_rvm,null)); } return lf; }
	 */

	@Override
	public Collection<CElement> GetChildren(TypeFilter tf,
			IProgressMonitor monitor)
	{
		if (m_bNeedUpdateChildren && tf.Contains(TypeFilter.RESOURCE))
		{
			if (m_ir.isCollectionDefined())
			{
				m_Children = new LinkedList<CElement>();
				if (!m_rvm.IsLocal())
				{
					// Utils.ShowMessageBox("Warning",
					// "Remote collection listing is not supported yet",
					// Window.OK);
				} else
				{
					Collection<String> files = m_rvm.GetAllFiles(m_ir);
					for (String f : files)
					{
						m_Children.add(new Resource(f, this, m_rvm));
					}
					m_Children.add(new EmptyElement(m_rvm, this));
					Collections.sort((LinkedList) m_Children);
				}
			}
			m_bNeedUpdateChildren = false;
		}
		return m_Children;
	}
	@Override
	public String GetLabel()
	{
		UpdateIR();
		if (IsCollection())
			return m_ir.getCollectionName();
		return m_ir.getFileName();
	}

	@Override
	public boolean IsManaged()
	{
		return true;
	}

	@Override
	protected void UpdateParent()
	{
	}

	@Override
	public int compareTo(CElement ce)
	{
		if (ce instanceof DBElement)
		{
			return GetLabel().compareTo(ce.GetLabel());
		}
		if (ce instanceof FSFolder || ce instanceof VirtualFolder
				|| ce instanceof EmptyElement)
			return 1;
		return -1;
	}
	@Override
	public String toString()
	{
		return super.toString();
	}

}
