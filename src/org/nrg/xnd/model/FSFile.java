package org.nrg.xnd.model;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.nrg.fileserver.FileCollection;
import org.nrg.fileserver.ItemRecord;
import org.nrg.xnd.app.IImageKeys;
import org.nrg.xnd.utils.FSObject;

public class FSFile extends CElement
{
	private FSObject m_fso;
	private FileCollection m_col = null;
	private boolean m_bNeedColUpdate = true;

	@Override
	public boolean IsManaged()
	{
		return false;
	}
	@Override
	public String toString()
	{
		return "File: " + m_fso.getAbsolutePath();
	}
	public FileCollection GetCollecion()
	{
		if (m_bNeedColUpdate)
		{
			m_col = m_rvm.getCM().FindCollection(m_fso);
			m_bNeedColUpdate = false;
		}
		return m_col;
	}

	@Override
	public void Invalidate()
	{
		super.Invalidate();
		m_bNeedColUpdate = true;
	}
	@Override
	public Image GetImage()
	{
		return IImageKeys.GetImage(IImageKeys.FILE_BLANK);
	}
	@Override
	public ImageDescriptor GetImageDescriptor()
	{
		return IImageKeys.GetImDescr(IImageKeys.FILE_BLANK);
	}
	public FSFile(File f, RepositoryViewManager rvm, CElement parent)
	{
		super(rvm, parent);
		m_fso = new FSObject(f, rvm.GetRelativePath(f.getAbsolutePath()));
	}
	public FSObject GetFSObject()
	{
		return m_fso;
	}
	/*
	 * private void UpdateIR() { if(m_ir==null)
	 * m_ir=m_rvm.FindItemForResource(m_fso.GetRelativePath(), true); }
	 */
	public DBElement ConvertToDBElement()
	{
		ItemRecord ir = new ItemRecord(m_fso.getAbsolutePath(), m_fso
				.GetRelativePath());
		m_rvm.ItemAdd(ir, false);
		return new DBElement(ir, m_rvm, null);
	}
	/*
	 * public void StartManaging() { m_ir=new
	 * ItemRecord(m_fso.getAbsolutePath(),m_fso.GetRelativePath());
	 * m_ir.AddResource(m_fso.GetRelativePath()); m_rvm.ItemAdd(m_ir, false); }
	 * 
	 * public void StopManaging() { UpdateIR(); if(m_ir==null) return;
	 * if(m_ir.CountResources()<=1) m_rvm.ItemRemove(m_ir); else
	 * m_rvm.ResourceRemove(m_ir, m_fso.GetRelativePath()); m_ir=null; }
	 */
	@Override
	public void ApplyOperation(Object o, int how, IProgressMonitor monitor)
	{
		if (how == MANAGEALL || how == MANAGE)
		{
			// monitor.subTask(m_fso.getAbsolutePath());
			ConvertToDBElement();
			Invalidate();
		}
	}

	@Override
	public Collection<CElement> GetChildren(TypeFilter tf,
			IProgressMonitor monitor)
	{
		return null;
	}

	@Override
	public void getLeaves(Collection<CElement> descendants, TypeFilter tf,
			IProgressMonitor monitor)
	{
	}

	@Override
	public String GetLabel()
	{
		return m_fso.getName();
	}

	@Override
	protected void UpdateParent()
	{
		if (m_rvm.IsTagView())
			m_Parent = null;
		else
			m_Parent = new FSFolder(m_fso.getParentFile(), m_rvm, null);
		m_bNeedUpdateParent = false;
	}
	@Override
	public int compareTo(CElement ce)
	{
		if (ce instanceof FSFile)
			return GetLabel().compareTo(ce.GetLabel());
		return 1;
	}

}
