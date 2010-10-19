package org.nrg.xnd.model;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.nrg.xnd.app.IImageKeys;

public class EmptyElement extends CElement
{

	public EmptyElement(RepositoryViewManager rvm, CElement par)
	{
		super(rvm, par);
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
		return IImageKeys.GetImage(IImageKeys.DOTS);
	}

	@Override
	public ImageDescriptor GetImageDescriptor()
	{
		return IImageKeys.GetImDescr(IImageKeys.DOTS);
	}

	@Override
	public String GetLabel()
	{
		return "...";
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
		return -1;
	}

}
