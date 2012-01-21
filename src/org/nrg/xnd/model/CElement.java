package org.nrg.xnd.model;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.nrg.fileserver.RepositoryManager;

/**
 * @author mmilch 
 * 		   CElement is a generic UI element to which all operations in a
 *         tree or table browser are applied. Implementing classes: DBElement,
 *         EmptyElement, FSFile, FSFolder, FSRoot, Resource, RootElement,
 *         VirtualFolder.
 *         
			The hiearchy of CElement with regards to class main purposes in the interface is as follows (all classes are in org.nrg.xnd.model):
			CElement: basically a tree node in the interface hierarchy. An important function is ApplyOperation() that propagates any operation to the subtree.
			EmptyElement - somewhat fake non-existent element with label ".." that responds to double-click by going up a level in the table view.
			FSRoot - (imaginary) tree root in the interface
			FSFile - unmanaged file 
			FSFolder - file system folder (note that folders are not 'managed' in XND)
			DBElement - a record in XND's database that puts a set of tags in correspondence to one or more files (in the latter case it is called 'Collection' with a service infrastructure implemented in subclasses of org.nrg.fileserver.CollectionManager). 
			Resource - element of a collection (i.e. file)
			RootElement - this one is used internally when one element need to be supplied to any function that does anything on all children of some CElement. The difference with FSRoot is rather functional; FSRoot is used in interface refresh/browsing whereas RootElement is more of a utility class.
			VirtualFolder - this one is used in tag view and replaces folder in the file view. It is the main element of the virtual hierarchy. Virtual folder is defined by a certain combination of <Tag-Value> pairs, specified in its m_Context member variable.
 */

public abstract class CElement extends PlatformObject
		implements
			Comparable<CElement>
{
	public static final int SETTAGS = 0, REMOVETAGS = 2, MANAGE = 3,
			UNMANAGE = 4, MANAGEALL = 5, UNMANAGEALL = 6,
			REMOVE_FROM_ROOTS = 7, SENDTO = 8;
	public static final int DBELEMENT = 1, EMPTYELEMENT = 2, FSFILE = 4,
			FSFOLDER = 8, FSROOT = 16, ROOTELEMENT = 32, VIRTUALFOLDER = 64;
	protected CElement m_Parent = null;
	protected Collection<CElement> m_Children = null;
	protected boolean m_bNeedUpdateChildren = true;
	protected boolean m_bNeedUpdateParent = true;
	protected RepositoryManager m_rm;
	protected RepositoryViewManager m_rvm;
	private Image m_image = null;

	public abstract Image GetImage();
	public RepositoryManager getRM()
	{
		return m_rm;
	}
	public RepositoryViewManager getRVM()
	{
		return m_rvm;
	}

	public abstract ImageDescriptor GetImageDescriptor();
	public int getType()
	{
		if (this instanceof DBElement)
			return DBELEMENT;
		if (this instanceof EmptyElement)
			return EMPTYELEMENT;
		if (this instanceof FSFile)
			return FSFILE;
		if (this instanceof FSFolder)
			return FSFOLDER;
		if (this instanceof FSRoot)
			return FSROOT;
		if (this instanceof RootElement)
			return ROOTELEMENT;
		if (this instanceof VirtualFolder)
			return VIRTUALFOLDER;
		return 0;
	}
	public static int getTypes(Collection<CElement> ce)
	{
		int type = 0;
		for (CElement c : ce)
			type |= c.getType();
		return type;
	}

	public CElement(RepositoryManager rm, CElement parent)
	{
		if (rm instanceof RepositoryViewManager)
		{
			m_rvm = (RepositoryViewManager) rm;
			m_rm = m_rvm.GetRM();
		} else
		{
			m_rvm = null;
			m_rm = rm;
		}
		m_Parent = parent;
		if (parent == null)
			m_bNeedUpdateParent = true;
	}
	public void Invalidate()
	{
		m_bNeedUpdateParent = true;
		m_bNeedUpdateChildren = true;
		if (m_Parent != null)
			m_Parent.Invalidate();
	}
	public abstract boolean IsManaged();
	public abstract Collection<CElement> GetChildren(TypeFilter tf,
			IProgressMonitor monitor);
	public void getLeaves(Collection<CElement> leaves, TypeFilter tf,
			IProgressMonitor monitor)
	{
		Collection<CElement> ch = GetChildren(tf, monitor);		
		if (ch == null || ch.size()<1)
		{
			leaves.add(this);
			return;
		}
		for(CElement ce:ch)
		{
			ce.getLeaves(leaves,tf,monitor);
		}
	}
	public CElement GetParent()
	{
		if (m_bNeedUpdateParent)
			UpdateParent();
		return m_Parent;
	}
	protected abstract void UpdateParent();
	public abstract String GetLabel();
	public abstract int compareTo(CElement ce);
	public abstract void ApplyOperation(Object operation, int how,
			IProgressMonitor monitor);
	// public abstract void ApplyOperationToAllDescendants(Object operation, int
	// how, IProgressMonitor monitor);
}