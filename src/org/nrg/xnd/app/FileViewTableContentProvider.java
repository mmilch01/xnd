package org.nrg.xnd.app;

import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.model.FSRoot;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.model.VirtualFolder;

public class FileViewTableContentProvider extends BaseWorkbenchContentProvider
{
	@Override
	public boolean hasChildren(Object element)
	{
		if (element instanceof FSFolder || element instanceof VirtualFolder
				|| element instanceof FSRoot)
			return true;
		return false;
	}
	@Override
	public Object[] getChildren(Object element)
	{
		return ((CElement) element).GetChildren(new TypeFilter(), null)
				.toArray();
	}
}
