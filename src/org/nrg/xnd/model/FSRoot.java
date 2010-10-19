package org.nrg.xnd.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class FSRoot extends CElement
{

	public FSRoot(RepositoryViewManager rvm, CElement parent)
	{
		super(rvm, parent);
	}
	@Override
	public Image GetImage()
	{
		return null;
	}
	@Override
	public ImageDescriptor GetImageDescriptor()
	{
		return null;
	}
	@Override
	public void ApplyOperation(Object operation, int how,
			IProgressMonitor monitor)
	{
	}

	@Override
	public Collection<CElement> GetChildren(TypeFilter tf,
			IProgressMonitor monitor)
	{
		if (m_bNeedUpdateChildren)
		{
			String[] roots = m_rvm.GetManagedFolders();
			m_Children = new LinkedList<CElement>();
			for (int i = 0; i < roots.length; i++)
			{
				m_Children.add(new FSFolder(new File(roots[i]), m_rvm, this));
			}
			Collections.sort((LinkedList) m_Children);
			m_bNeedUpdateChildren = false;
		}
		return tf.Filter(m_Children);
	}

	@Override
	public String GetLabel()
	{
		return null;
	}

	@Override
	public boolean IsManaged()
	{
		return false;
	}

	@Override
	protected void UpdateParent()
	{
	}

	@Override
	public int compareTo(CElement ce)
	{
		return 0;
	}

}
