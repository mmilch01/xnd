package org.nrg.xnd.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.nrg.xnd.app.IImageKeys;
import org.nrg.xnd.rules.NameRule;
import org.nrg.xnd.utils.FSObject;

/**
 * 
 * File system directory functional representation in the UI.
 * 
 * @author mmilch
 * 
 */
public class FSFolder extends CElement
{
	private FSObject m_fso;

	@Override
	public void ApplyOperation(Object o, int how, IProgressMonitor monitor)
	{
		if (how == CElement.SENDTO)
			return;

		if (o instanceof NameRule)
		{
			((NameRule) o).ApplyRule(this, monitor);
			Invalidate();
			return;
		}

		Collection<CElement> cce = GetChildren(new TypeFilter(), monitor);
		if (cce == null)
			return;
		for (CElement ce : cce)
		{
			ce.ApplyOperation(o, how, monitor);
		}
		Invalidate();
	}
	@Override
	public Image GetImage()
	{
		return IImageKeys.GetImage(IImageKeys.FOLDER_CLOSED);
	}
	@Override
	public ImageDescriptor GetImageDescriptor()
	{
		return IImageKeys.GetImDescr(IImageKeys.FOLDER_CLOSED);
	}
	public FSFolder(File f, RepositoryViewManager rvm, CElement parent)
	{
		super(rvm, parent);
		m_fso = new FSObject(f, rvm.GetRelativePath(f.getAbsolutePath()));
	}

	public boolean IsRoot()
	{
		UpdateParent();
		return (m_Parent instanceof FSRoot);
	}

	@Override
	public boolean IsManaged()
	{
		return false;
	}
	public FSObject GetFSObject()
	{
		return m_fso;
	}
	@Override
	public Collection<CElement> GetChildren(TypeFilter tf,
			IProgressMonitor monitor)
	{
		if (m_bNeedUpdateChildren)
		{
			if (monitor != null && monitor.isCanceled())
				return null;
			if (monitor != null)
				monitor.subTask("Retrieving contents of folder "
						+ m_fso.GetRelativePath());
			FSObject[] chlst = m_fso.ListFiles(true);
			if (chlst == null || chlst.length < 1)
				return new LinkedList<CElement>();
			m_Children = new LinkedList<CElement>();
			for (FSObject f : chlst)
			{
				if (f.IsDir_cached())
					m_Children.add(new FSFolder(f, m_rvm, this));
				else
				{
					CElement dbie = DBElement.CreateDBE(f, m_rvm, this);

					if (dbie == null)
					{
						dbie = new FSFile(f, m_rvm, this);
						if (((FSFile) dbie).GetCollecion() != null)
							continue;
					}
					m_Children.add(dbie);
				}
			}
			if (!IsRoot())
				m_Children.add(new EmptyElement(m_rvm, this));
			Collections.sort((LinkedList<CElement>) m_Children);
			m_bNeedUpdateChildren = false;
		}
		return tf.Filter(m_Children);
	}
/*	
	@Override
	public void GetDescendants(Collection<CElement> descendants, TypeFilter tf,
			IProgressMonitor monitor)
	{
		if (monitor != null)
		{
			if (monitor.isCanceled())
				return;
			monitor.setTaskName("Processing folder " + m_fso.GetRelativePath());
		}
		Collection<CElement> cce = GetChildren(tf, monitor);
		MilliTimer mt = new MilliTimer(monitor);
		for (CElement ce : cce)
		{
			if (ce instanceof FSFile)
			{
				if (!mt.Check(null, ce.GetLabel()))
					return;
				if (ce.IsManaged())
					descendants.add(ce);
			} else
				// otherwise always add.
				descendants.add(ce);
		}
	}
*/	
	@Override
	public String GetLabel()
	{
		return m_fso.getName();
	}
	@Override
	protected void UpdateParent()
	{
		String[] roots = m_rvm.GetManagedFolders();
		boolean bRoot = false;
		for (int i = 0; i < roots.length; i++)
			if (m_fso.compareTo(new File(roots[i])) == 0)
			{
				bRoot = true;
				break;
			}
		if (bRoot)
			m_Parent = new FSRoot(m_rvm, null);
		else
			m_Parent = new FSFolder(m_fso.getParentFile(), m_rvm, null);
	}
	@Override
	public int compareTo(CElement ce)
	{
		if (ce instanceof FSFolder)
			return GetLabel().compareTo(ce.GetLabel());
		if (ce instanceof EmptyElement)
			return 1;
		return -1;
	}
	@Override
	public String toString()
	{
		return "Folder: " + m_fso.getAbsolutePath();
	}
}