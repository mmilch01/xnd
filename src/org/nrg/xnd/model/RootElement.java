package org.nrg.xnd.model;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class RootElement extends CElement
{
	@Override
	public ImageDescriptor GetImageDescriptor()
	{
		return null;
	}
	@Override
	public boolean IsManaged()
	{
		return true;
	}
	public RootElement(Collection<CElement> ch, RepositoryViewManager rvm)
	{
		super(rvm, null);
		m_Children = ch;
	}
	@Override
	public Collection<CElement> GetChildren(TypeFilter tf,
			IProgressMonitor monitor)
	{
		return m_Children;
	}
	@Override
	protected void UpdateParent()
	{
	}
	@Override
	public String GetLabel()
	{
		return null;
	}
	@Override
	public int compareTo(CElement ce)
	{
		return 0;
	}

	@Override
	public Image GetImage()
	{
		return null;
	}

	@Override
	public void ApplyOperation(Object operation, int how,
			IProgressMonitor monitor)
	{
		if (m_Children != null)
		{
			for (CElement ce : m_Children)
			{
				ce.ApplyOperation(operation, how, monitor);
			}
			Invalidate();
		}
	}
}
