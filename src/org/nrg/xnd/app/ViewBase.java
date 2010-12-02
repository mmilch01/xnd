package org.nrg.xnd.app;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.nrg.fileserver.ItemRecord;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.VirtualFolder;
import org.nrg.xnd.tools.ImageViewer.IProgressReporter;
import org.nrg.xnd.tools.ImageViewer.SWTProgressReporter;
import org.nrg.xnd.ui.TagEditTable;
import org.nrg.xnd.ui.dialogs.DontShowAgainDialog;
import org.nrg.xnd.utils.Utils;
import org.nrg.xnd.utils.dicom.StudyList;
import org.nrg.xnd.utils.dicom.StudyRecord;

public abstract class ViewBase extends ViewPart implements IRunnableWithProgress
{
	static final int TYPE_LOCAL = 0, TYPE_REMOTE = 1, TYPE_XNAT = 2;
//	static protected String TREE_FS,TREE_TAG;
	
	protected IAdapterFactory m_AdapterFactory = new XNDAdapterFactory();
	protected RepositoryViewManager m_rvm;
	protected Vector m_RunArgs = new Vector();
	protected int m_type=TYPE_LOCAL;
	public TabTreeManager m_ttm;
	
	
	public static final int SETTAG = 0, SETDEFAULTTAGS = 1, REMOVETAG = 2,
//	MANAGE = 4, UNMANAGE = 5, REMOVE_FROM_ROOTS = 6, SENDTO = 11,
	COPY_TAGS=3,PASTE_TAGS=4,
	MANAGE=6, UNMANAGE=7, REMOVE_FROM_ROOTS=8, SENDTO=13,
	RULEDICOM = 0, RULENAMING = 1, RULEPATTERN = 2, RULECOLLECTION = 3;
	
//UI elments
	protected TagEditTable m_FileInfo;
	
	public RepositoryViewManager GetRepositoryViewManager()
	{
		return m_rvm;
	}
	protected abstract Menu CreateContextMenu(Control parent);
	protected abstract TreeViewer InitializeTree(boolean bFS);
	public abstract void Refresh(boolean bTableOnly); 
	protected abstract Collection<CElement> GetSelectedTreeElements();
	protected abstract Collection<CElement> GetSelectedTableElements();
	
	protected void updateTagInfo()
	{
//		m_FileInfo.setText("");
		m_FileInfo.getTable().clearAll();
		Collection<CElement> cce=GetSelectedElements();
		ItemRecord ir=null;
		int n=0;
		for(CElement ce:cce)
		{
			ItemRecord nxt=null;
			if(ce instanceof DBElement)
				nxt=((DBElement)ce).GetIR();
			else if(ce instanceof VirtualFolder)
				nxt=((VirtualFolder)ce).getAssociatedTags();
/*			
			if(nxt==null)
			{
				m_FileInfo.append(ce.toString()+"\n");
			}
*/			
			if(n==0) ir=nxt;
			else ir.tagsMerge(nxt.getTagCollection());
			n++;
		}
		if(ir!=null)
		{
			m_FileInfo.setTags(ir);
		}
//		m_FileInfo.setSelection(0,0);
	}
	
	
	public boolean IsLocal()
	{
		return m_type == TYPE_LOCAL;
	}
	public void ViewImages()
	{
		try
		{
			m_RunArgs.clear();
			m_RunArgs.add("ViewImages");			
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(this);
		}
		catch(Exception ex)
		{		
		}		
	}	
	public boolean CanUploadToXNAT()
	{
		if (!IsLocal())
			return false;
		if (!m_rvm.IsTagView())
			return false;
		TreeItem[] sel = m_ttm.getTree().getSelection();
		if (sel.length < 1)
			return false;
		for (TreeItem ti : sel)
		{
			CElement ce = (CElement) ti.getData();
			if (ce instanceof VirtualFolder)
			{
				String fn = ((VirtualFolder) ce).GetLabel();
				if (fn.startsWith("Experiment:") || fn.startsWith("Project:")
						|| fn.startsWith("Subject:"))
					return true;
			}
		}
		return false;
	}	
	 
	protected void ProcessSelectionWithProgress(Object o, int how, boolean bTree)
	{
		try
		{
			m_RunArgs.clear();
			m_RunArgs.add("ProcessSelection");
			m_RunArgs.add(o);
			m_RunArgs.add(new Integer(how));
	
			Collection<CElement> cce;
			if (bTree)
				cce = GetSelectedTreeElements();
			else
				cce = GetSelectedTableElements();
			m_RunArgs.add(cce);
	
			m_RunArgs.add(new Boolean(bTree));
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(this);
			// PlatformUI.getWorkbench().getProgressService().run(true,false,this);
			// new ProgressMonitorDialog(new Shell()).run(true, false, this);
		} 
		catch (InterruptedException e)
		{
		} catch (InvocationTargetException ite)
		{
		}				
	}
	protected abstract void TableDoubleClick(DoubleClickEvent e, IProgressMonitor ipm);
	protected abstract void ProcessSelection (final Object o, final int how, final Collection<CElement> cce, final boolean bTree, IProgressMonitor mon);
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException
	{
		String type = (String) m_RunArgs.get(0);
		if (type.compareTo("ProcessSelection") == 0)
		{
			ProcessSelection(m_RunArgs.get(1), ((Integer) m_RunArgs.get(2))
					.intValue(), (Collection<CElement>) m_RunArgs.get(3),
					((Boolean) m_RunArgs.get(4)).booleanValue(), monitor);
		}
		else if (type.compareTo("TableDoubleClick") == 0)
		{
			TableDoubleClick((DoubleClickEvent) m_RunArgs.get(1), monitor);
		}
		else if (type.compareTo("ViewImages") ==0)
		{
			viewImagesLong(monitor);
		}
		monitor.done();
	}
	public abstract Collection<CElement> GetSelectedElements();
	protected void viewImagesLong(final IProgressMonitor monitor)
	{
		monitor.setTaskName("Loading images");
		final Collection<CElement> cce = new LinkedList<CElement>();
		try
		{
			Display.getDefault().syncExec(new Runnable()
			{
				Collection<CElement> ccce;
				public void run()
				{
					ccce = GetSelectedElements();
					cce.addAll(ccce);
				}
			});
			IProgressReporter pr=new SWTProgressReporter(monitor);
			
			Collection<StudyRecord> studies = new StudyList(cce, pr)
					.getStudies();
			if(monitor.isCanceled()) return;
			if (studies.size() < 1)
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						Utils.ShowMessageBox("",
								"Found no DICOM studies in the selection", Window.OK);						
					}
				});
				return;
			}
			// otherwise, show the first study.
			StudyRecord[] arr = studies.toArray(new StudyRecord[0]);
			if (arr.length > 1)
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						DontShowAgainDialog dlg = new DontShowAgainDialog(
								"FileView.ShowOneOfManyStudies",
								"Loading single study",
								"More than one study in selection. First found study will be displayed.");
						if (dlg.NeedToShow())
							dlg.open();
					}
				});				
			}			
			if (!XNDApp.GetViewerFrame().GetViewerManager().OpenImages(arr[0],
					new SWTProgressReporter(monitor),false))
			{
				XNDApp.GetViewerFrame().setVisible(false);
				if(monitor.isCanceled()) return;
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{						
						Utils.ShowMessageBox("", "Could not open some images!",
								Window.OK);
					}
				});				
				return;
			}
		} finally
		{
		}		
	}
	class TableLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		public Image getImage(Object element)
		{
			return ((CElement) element).GetImage();
		}
		public Image getColumnImage(Object element, int columnIndex)
		{
			if (columnIndex == 0)
				return getImage(element);
			else
				return null;
		}
		public String getColumnText(Object element, int columnIndex)
		{
			if (columnIndex == 0)
				return ((CElement) element).GetLabel();
			else
			{
				if (element instanceof DBElement
						|| element instanceof VirtualFolder)
				{
					boolean bde = (element instanceof DBElement);
					String[] tags = m_rvm.GetTableTags();
					ItemRecord ir = (bde)
							? ((DBElement) element).GetIR()
							: ((VirtualFolder) element).getAssociatedTags();
					org.nrg.fileserver.ItemTag lbl = ir
							.getTag(tags[columnIndex - 1]);
					if (lbl != null)
						return lbl.PrintValues();
					else
						return "";
				} else
					return "";
			}
		}		
	} // end of class TableLabelProvider.
	
	protected class TabTreeManager 
	{
		private TabFolder m_tf;
		private TreeViewer m_fsViewer;
		private TreeViewer m_tagViewer;
		private Menu m_fsContextMenu, m_tagContextMenu;
		private boolean m_bLocal=true;
		
		public Tree getTree()
		{
			TabItem[] items=m_tf.getSelection();
			if(items.length<1) return null;
			return (Tree)items[0].getControl();
		}
		public TreeViewer getViewer()
		{
			if(isFS()) return m_fsViewer;
			else return m_tagViewer;
		}
		public boolean isFS()
		{
			if(ViewBase.this instanceof PACSView) return false;
			if(ViewBase.this instanceof FileView)
			{
				TabItem[] items=m_tf.getSelection();
				return (items[0].getText().compareTo(FileView.TREE_FS)==0);
			}
			return false;
		}
		
		public Menu getMenu()
		{			
			return getTree().getMenu();
/*			
			TabItem[] items=m_tf.getSelection();
			if(items.length<1) return null;
			return items[0].getControl().getMenu();
*/								
		}
		public void updateMenus()
		{
			if(m_bLocal)
			{
				m_fsContextMenu=CreateContextMenu(m_fsViewer.getTree());
				m_fsViewer.getTree().setMenu(m_fsContextMenu);
			}			
			m_tagContextMenu=CreateContextMenu(m_tagViewer.getTree());		
			m_tagViewer.getTree().setMenu(m_tagContextMenu);
		}
		public TabTreeManager (TabFolder tf)
		{
			m_tf=tf;
			m_bLocal=m_rvm.IsLocal();
			if(ViewBase.this instanceof FileView)
			{
				if(m_bLocal)
				{	
					m_fsViewer=InitializeTree (true);
					m_fsContextMenu=CreateContextMenu(m_fsViewer.getTree());
					m_fsViewer.getTree().setMenu(m_fsContextMenu);
				}			
				m_tagViewer=InitializeTree (false);
				m_tagContextMenu=CreateContextMenu(m_tagViewer.getTree());		
				m_tagViewer.getTree().setMenu(m_tagContextMenu);
			}
			else if(ViewBase.this instanceof PACSView)
			{
				m_tagViewer = InitializeTree(false);
				m_tagContextMenu = CreateContextMenu(m_tagViewer.getTree());
				m_tagViewer.getTree().setMenu(m_tagContextMenu);				
			}
			
			m_tf.addSelectionListener(new SelectionListener()
			{
					public void widgetDefaultSelected(SelectionEvent e)
					{
						// TODO Auto-generated method stub
					}
					public void widgetSelected(SelectionEvent e)
					{
						boolean bTagView=GetRepositoryViewManager().IsTagView();
						if((isFS() && bTagView) || (!isFS() && !bTagView))
						{
							GetRepositoryViewManager().ToggleTagView();
							Refresh(false);
						}
					}
			});
		}
	} // end of class TabTreeManager

}
