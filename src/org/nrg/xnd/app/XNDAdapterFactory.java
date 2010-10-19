package org.nrg.xnd.app;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.utils.FSObject;
import org.nrg.xnd.utils.Utils;

public class XNDAdapterFactory implements IAdapterFactory
{
	private IWorkbenchAdapter adapter = new IWorkbenchAdapter()
	{
		public Object getParent(Object o)
		{
			return ((CElement) o).GetParent();
		}
		public String getLabel(Object o)
		{
			CElement ce = (CElement) o;
			if (ce instanceof FSFolder)
			{
				FSObject fso = ((FSFolder) ce).GetFSObject();
				if ((fso != null)
						&& (fso.getAbsolutePath().compareTo(
								Utils.GetIncomingFolder()) == 0))
					return "<Incoming>";
			}
			return ((CElement) o).GetLabel();
		}
		public ImageDescriptor getImageDescriptor(Object object)
		{
			return ((CElement) object).GetImageDescriptor();
		}
		public Object[] getChildren(Object o)
		{
			final CElement[] fi = new CElement[0];
			return ((CElement) o).GetChildren(new TypeFilter(true), null)
					.toArray(fi);
		}
	};
	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		return adapter;
		/*
		 * if(adapterType==IWorkbenchAdapter.class && adaptableObject instanceof
		 * TreeElement) return treeAdapter;
		 * if(adapterType==IWorkbenchAdapter.class && adaptableObject instanceof
		 * TableElement) return tableAdapter;
		 * if(adapterType==IWorkbenchAdapter.class && adaptableObject instanceof
		 * ContactsGroup) return groupAdapter; if(adapterType ==
		 * IWorkbenchAdapter.class && adaptableObject instanceof ContactsEntry)
		 * return entryAdapter;
		 */
	}
	public Class[] getAdapterList()
	{
		return new Class[]{IWorkbenchAdapter.class};
	}
}
