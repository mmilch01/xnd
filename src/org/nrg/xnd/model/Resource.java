package org.nrg.xnd.model;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.nrg.xnd.app.IImageKeys;
import org.nrg.xnd.utils.FSObject;

public class Resource extends CElement
{
	private FSObject m_fso;
	public Resource(String rel_path, DBElement parent, RepositoryViewManager rm)
	{
		super(rm, parent);
		m_fso = new FSObject(rm.GetAbsolutePath(rel_path));
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
		return null;
	}

	@Override
	public Image GetImage()
	{
		return IImageKeys.GetImage(IImageKeys.FRAGMENT);
	}
	public FSObject getFile(){return m_fso;}
	@Override
	public ImageDescriptor GetImageDescriptor()
	{
		return IImageKeys.GetImDescr(IImageKeys.FRAGMENT);
	}

	@Override
	public String GetLabel()
	{
		return m_fso.getName();
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
		if (ce instanceof Resource)
			return m_fso.compareTo(((Resource) ce).m_fso);
		return 1;
	}
	@Override
	public String toString()
	{
		return "Collection resource file: " + m_fso.getAbsolutePath();
	}
}