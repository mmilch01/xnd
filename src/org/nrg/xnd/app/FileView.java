package org.nrg.xnd.app;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.nrg.fileserver.Context;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.RestRepositoryManager;
import org.nrg.fileserver.XNATRestAdapter;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.EmptyElement;
import org.nrg.xnd.model.FSFile;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.model.FSRoot;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.Resource;
import org.nrg.xnd.model.RootElement;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.model.VirtualFolder;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.rules.RuleManager;
import org.nrg.xnd.tools.HierarchyUploadManager;
import org.nrg.xnd.ui.TableItemTransfer;
import org.nrg.xnd.ui.TagEditTable;
import org.nrg.xnd.ui.dialogs.DownloadDialog;
import org.nrg.xnd.ui.dialogs.EditTagSetDialog;
import org.nrg.xnd.ui.dialogs.EditTagValueDialog;
import org.nrg.xnd.ui.dialogs.OpenConnectionDialog;
import org.nrg.xnd.utils.Utils;

public class FileView extends ViewBase
{
	static final String m_ID = "org.nrg.xnat.desktop.FileView";
	static final String TREE_FS="File view", TREE_TAG="Tag view";
	
	//UI elements
	private SashForm m_SashForm;
	private SashForm m_SashFileView;
	private TabFolder m_TreeTabFolder;
	private TabFolder m_TagTabFolder;
	private TableViewer m_TableViewer;
	private Table m_Table = null;
	private Menu m_TableContextMenu;

	protected TreeViewer InitializeTree(boolean bFS)
	{
//		m_TreeViewer = new TreeViewer(m_SashForm, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		final TreeViewer tv = new TreeViewer(m_TreeTabFolder, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		TabItem item1 =  new TabItem(m_TreeTabFolder, SWT.NONE);
		item1.setText(bFS?TREE_FS:TREE_TAG);
		item1.setControl(tv.getTree());
		
		tv.setLabelProvider(new WorkbenchLabelProvider());
		tv.setContentProvider(new FileViewTreeContentProvider()); // BaseWorkbenchContentProvider());
		Tree tree = tv.getTree();
		tree.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event evt)
			{
				if (evt.item instanceof TreeItem)
				{
					try
					{
						XNDPerspective.SelectionChanged(FileView.this,
								tv.getSelection());
						XNDApp.StartWaitCursor();
						UpdateTable();
						AppActions.GetAction(
								"org.nrg.xnat.desktop.UploadToXNAT")
								.setEnabled(CanUploadToXNAT());
						updateTagInfo();
/*
						TreeItem[] sel = m_Tree.getSelection();
						if (sel.length == 1)
						{
							ItemRecord ir = null;
							if (sel[0].getData() instanceof VirtualFolder)
								ir = ((VirtualFolder) sel[0].getData())
										.getAssociatedTags();
							else if (sel[0].getData() instanceof DBElement)
								ir = ((DBElement) sel[0].getData()).GetIR();
							m_FileInfo.setText("");
							if (ir != null)
							{
								m_FileInfo.append(ir.PrintTags());
								m_FileInfo.setSelection(0, 0);
							}
						}
*/
						XNDApp.EndWaitCursor();
					} catch (Exception e)
					{
						XNDApp.EndWaitCursor();
						e.printStackTrace();
					}
				}
			}
		});
		return tv;
/*		
		m_TreeContextMenu = CreateContextMenu(m_Tree);
		m_Tree.setMenu(m_TreeContextMenu);
*/		
	}
	public void InitializeTable()
	{
		m_Table = new Table(m_SashFileView, SWT.RIGHT | SWT.BORDER | SWT.MULTI
				| SWT.V_SCROLL
				/* | (m_bIsLocal?SWT.CHECK:0) */| SWT.FULL_SELECTION);
		m_Table.setHeaderVisible(true);
		m_Table.setLinesVisible(false);
		{
			TableColumn col1 = new TableColumn(m_Table, SWT.LEFT);
			col1.setText("Resource Name");
			col1.setWidth(160);
		}
		m_TableContextMenu = CreateContextMenu(m_Table);
		m_Table.setMenu(m_TableContextMenu);
		String[] tags = m_rvm.GetTableTags();

		TableColumn col;
		for (int i = 0; i < tags.length; i++)
		{
			col = new TableColumn(m_Table, SWT.LEFT);
			col.setText(tags[i]);
			col.setWidth(70);
		}
		TableLabelProvider lp = new TableLabelProvider();
		if (m_type == TYPE_LOCAL)
			m_TableViewer = new CheckboxTableViewer(m_Table);
		else
			m_TableViewer = new TableViewer(m_Table);
		m_TableViewer.setLabelProvider(lp);
		m_TableViewer.setContentProvider(new FileViewTableContentProvider());
		if (m_TableViewer instanceof CheckboxTableViewer)
		{
			/*
			 * ((CheckboxTableViewer)(m_TableViewer)).addCheckStateListener(new
			 * ICheckStateListener() { public void
			 * checkStateChanged(CheckStateChangedEvent e) { CompositeElement
			 * ce=(CompositeElement)(e.getElement()); if(ce instanceof FSFile &&
			 * e.getChecked()) { DBItemElement
			 * dbie=((FSFile)ce).ConvertToDBElement(); if(dbie!=null) {
			 * m_TableViewer.remove(e); e. m_Table.in
			 * m_TableViewer.replace(element,e.get m_TableViewer.add } } else
			 * if(ce instanceof DBItemElement && !e.getChecked()) {
			 * ((DBItemElement)ce).ConvertToFSFile(); } } });
			 */
		}
		m_TableViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(final DoubleClickEvent e)
			{
				BusyIndicator.showWhile(Display.getCurrent(), new Runnable()
				{
					public void run()
					{
						TableDoubleClick(e, null);
					}
				});

				/*
				 * m_RunArgs.clear(); m_RunArgs.add("TableDoubleClick");
				 * m_RunArgs.add(e); try {
				 * PlatformUI.getWorkbench().getProgressService
				 * ().busyCursorWhile(FileView.this); } catch(Exception ex){}
				 */
			}
		});
		m_TableViewer
				.addSelectionChangedListener(new ISelectionChangedListener()
				{
					public void selectionChanged(SelectionChangedEvent e)
					{
						ISelection sel = e.getSelection();
						if (sel != null && !sel.isEmpty())
						{
							XNDPerspective.SelectionChanged(FileView.this, e
									.getSelection());
							updateTagInfo();
						}
/*						
						ISelection sel = e.getSelection();
						if (sel != null && !sel.isEmpty())
							Perspective.SelectionChanged(FileView.this, e
									.getSelection());
						TableItem[] ti = m_TableViewer.getTable()
								.getSelection();						
						m_FileInfo.setText("");
						if (ti.length == 1)
						{
							CElement ce = (CElement) ti[0].getData();
							if (ce instanceof DBElement)
							{
								ItemRecord ir = ((DBElement) ce).GetIR();
								if (m_type == TYPE_LOCAL)
									m_FileInfo.setText("File: "
											+ ir.GetAbsolutePath() + "\n");
								else
									m_FileInfo.setText("File: "
											+ ir.GetRelativePath() + "\n");
								m_FileInfo.append(ir.PrintTags());
								m_FileInfo.setSelection(0, 0);
							} else if (ce instanceof VirtualFolder)
							{
								VirtualFolder vf = (VirtualFolder) ce;
								m_FileInfo.append(vf.GetContextPath() + "\n");
								m_FileInfo.append(vf.getAssociatedTags()
										.PrintTags());
								m_FileInfo.setSelection(0, 0);
							}
						}
*/
					}
				});
		m_TableViewer.setInput(null);
		UpdateStatus();

		// setup drag and drop
		MakeDragSource(FileView.this);
		MakeDropTarget(FileView.this);
	}
		
	protected void TableDoubleClick(DoubleClickEvent e, IProgressMonitor ipm)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					final Collection<CElement> cce = GetSelectedTableElements();
					if (cce.size() != 1)
						return;
					CElement ce = null;
					for (CElement el : cce)
						ce = el;

					TreeItem[] sel = m_ttm.getTree().getSelection();
					if (sel.length < 1)
						return;

					// ?? disable listing of collection for remote view.
					if ((ce instanceof DBElement && ((DBElement) ce)
							.IsCollection())
							&& !m_rvm.IsLocal())
					{
						Utils
								.ShowMessageBox(
										"Warning",
										"This version of XND does not support remote collection listing.",
										Window.OK);
						return;
					}
					XNDApp.StartWaitCursor();
					if (ce instanceof FSFolder
							|| ce instanceof VirtualFolder
							|| (ce instanceof DBElement && ((DBElement) ce)
									.IsCollection()))
					{
						m_ttm.getViewer().expandToLevel(sel[0].getData(), 1);
						TreeItem ch = FindTreeChildElement(sel[0], ce);
						if (ch != null)
						{
							m_ttm.getTree().setSelection(ch);
							UpdateTable();
						}
						// m_TreeViewer.setSelection(selection, reveal)
					} else if (ce instanceof EmptyElement)
					{
						m_ttm.getTree().setSelection(sel[0].getParentItem());
						UpdateTable();
					}
				} catch (Exception ex)
				{
					ex.printStackTrace();
				} finally
				{
					XNDApp.EndWaitCursor();
				}

			}
		});
	}
	TreeItem FindTreeChildElement(TreeItem parent, CElement ce)
	{
		TreeItem[] cte = parent.getItems();
		for (TreeItem chce : cte)
		{
			if (ce.compareTo((CElement) chce.getData()) == 0)
				return chce;
		}
		return null;
	}
	private void UpdateStatus()
	{
		if (m_Table == null)
			return;
		int cnt = m_Table.getItemCount();
		if (cnt == XNDApp.app_maxRecords)
			XNDApp.SetStatus("Items in current folder: " + cnt
					+ " (MAXIMUM allowed to display in the table)");
		else
			XNDApp.SetStatus("Items in current folder: " + cnt);
	}
	private void MakeDragSource(final FileView fv)
	{
		DragSource ds = new DragSource(m_Table, DND.DROP_COPY);
		Transfer[] types = new Transfer[]{TableItemTransfer.getInstance()};
		final TableItemTransfer tit = TableItemTransfer.getInstance();
		ds.setTransfer(types);
		ds.addDragListener(new DragSourceAdapter()
		{
			public void dragStart(DragSourceEvent e)
			{
			}
			public void dragSetData(DragSourceEvent e)
			{
				TableItem[] ti = m_Table.getSelection();
				if (ti.length > 0)
					e.data = ti;
			}
			public void dragFinished(DragSourceEvent dte)
			{
			}
		});
	}
	private void MakeDropTarget(final FileView fv)
	{

		DropTarget dt = new DropTarget(m_ttm.getTree(), DND.DROP_COPY | DND.DROP_DEFAULT);
		Transfer[] types = new Transfer[]{TableItemTransfer.getInstance()};
		final TableItemTransfer tit = TableItemTransfer.getInstance();
		dt.setTransfer(types);

		dt.addDropListener(new DropTargetAdapter()
		{
			public void dragEnter(DropTargetEvent dte)
			{
				if (dte.detail == DND.DROP_DEFAULT)
				{
					dte.detail = DND.DROP_COPY;
				}
			}
			public void dragOver(DropTargetEvent dte)
			{
			}
			public void dragOperationChanged(DropTargetEvent dte)
			{
				if (dte.detail == DND.DROP_DEFAULT)
					dte.detail = DND.DROP_COPY;
			}
			public void dragLeave(DropTargetEvent dte)
			{
			}
			public void dropAccept(DropTargetEvent dte)
			{
			}
			public void drop(DropTargetEvent dte)
			{
				/*
				 * if(!(dte.data instanceof ItemRecord[]) || !(dte.item
				 * instanceof TreeItem)) return; if(dte.data!=null) {
				 * if(!tit.isSupportedType(dte.currentDataType)) return;
				 * fv.ProcessDnD(dte.data,(TreeItem)dte.item); fv.UpdateTable();
				 * }
				 */
			}
		});
	}
	private void InitializeFileViewForm()
	{
		m_SashFileView = new SashForm(m_SashForm, SWT.NONE);
		GridLayout layout = new GridLayout();
		m_SashFileView.setLayout(layout);
		GridData gd = new GridData();
		m_SashFileView.setLayoutData(gd);
		m_SashFileView.setOrientation(SWT.VERTICAL);
	}
	public void Refresh(boolean bTableOnly)
	{
		XNDApp.StartWaitCursor();
		if (m_ttm.getTree() != null && m_ttm.getViewer() != null && !bTableOnly)
			UpdateTree();
		UpdateTable();
		updateTagInfo();
		XNDApp.EndWaitCursor();
	}
	public boolean Connect()
	{
		if (m_rvm != null)
			m_rvm.SessionEnd();
//		Refresh(true);
		XNDApp.StartWaitCursor();
		Vector param = new Vector();
		if (getViewSite().getSecondaryId().compareTo("local") == 0)
		{
			setPartName("Local archive");
			m_type = TYPE_LOCAL;
			m_rvm = XNDApp.app_localVM;
			XNDApp.SetStatus("Initializing local view...");
			// ConsoleView.AppendMessage("Initializing local view...");
		} else if (getViewSite().getSecondaryId().compareTo("remote") == 0)
		{
			m_type = TYPE_REMOTE;
			try
			{
				String addr = XNDApp.app_Prefs.get("RemoteAddress",
						Utils.REMOTE_ADDRESS_DEFAULT);
				String port = new Integer(XNDApp.app_Prefs.getInt("ClientPort",
						Utils.PORT_REPOSITORY_DEFAULT)).toString();
				// ConsoleView.AppendMessage("Connecting to "+addr+":"+port);
				XNDApp.SetStatus("Connecting to " + addr + ":" + port);

				XNDApp.app_remoteVM = new RepositoryViewManager(
						new RestRepositoryManager(new URL(addr + ":" + port)));
				m_rvm = XNDApp.app_remoteVM;
			} catch (Exception e)
			{
				XNDApp.app_remoteVM = null;
			}
			setPartName("Remote archive: "
					+ XNDApp.app_Prefs.get("RemoteAddress",
							Utils.REMOTE_ADDRESS_DEFAULT));
			m_rvm = XNDApp.app_remoteVM;
			// ConsoleView.AppendMessage("Initializing remote view (waiting for remote host)...");
			XNDApp.SetStatus("Initializing remote view (waiting for remote host)...");
			if (XNDApp.app_remoteVM == null
					|| !((RestRepositoryManager) XNDApp.app_remoteVM.GetRM())
							.VerifyConnection())
			{
				XNDApp.EndWaitCursor();
				Utils.ShowMessageBox("Warning",
						"Unable to connect to remote repository", Window.OK);
				// ConsoleView.AppendMessage("Remote connection failed");
				XNDApp.SetStatus("Remote connection failed");
				return false;
			}
			m_rvm.InitFileTransfer();
			if (!m_rvm.IsTagView())
				m_rvm.ToggleTagView();
		} else
		// m_type==TYPE_XNAT
		{
			m_type = TYPE_XNAT;
			try
			{
				OpenConnectionDialog ocd = new OpenConnectionDialog(new Shell());
				ocd.open();

				String serv = XNDApp.app_Prefs.get(
						"OpenConnectionDialog.XNATServer",
						"http://central.xnat.org"), usr = XNDApp.app_Prefs.get(
						"OpenConnectionDialog.XNATUser", "guest"), pass = XNDApp.app_Prefs
						.get("OpenConnectionDialog.Pass", "guest");

				if (serv.length() < 1 || usr.length() < 3 || pass.length() < 4)
				{
					Utils.ShowMessageBox(
							"Configuration needed",
							"Before using XNAT view, enter required connection info under View->Preferences->Client/Server->XNAT Client",
							Window.OK);
				}
				XNATRestAdapter re = new XNATRestAdapter(serv, usr, pass);
				setPartName("XNAT archive: " + serv);
				XNDApp.app_XNATVM = new RepositoryViewManager(re);
				m_rvm = XNDApp.app_XNATVM;
				XNDApp.SetStatus("Connecting");
				if (!re.VerifyConnection())
				{
					XNDApp.EndWaitCursor();
					Utils.ShowMessageBox("Warning",
							"Cannot connect to XNAT server", Window.OK);
					XNDApp.SetStatus("Connection to XNAT server failed");
					return false;
				}
				m_rvm.InitFileTransfer();
				if (!m_rvm.IsTagView())
					m_rvm.ToggleTagView();
			} catch (Exception e)
			{
				e.printStackTrace();
				XNDApp.app_XNATVM = null;
			}
		}
		XNDApp.EndWaitCursor();

		if (!m_rvm.SessionInit(param))
		{
			MessageBox msg = new MessageBox(new Shell());
			msg.setMessage("Could not connect to archive");
			msg.open();
			XNDApp.EndWaitCursor();
			
			ConsoleView
					.AppendMessage("Remote view is not connected. "
							+ "Verify that the remote repository server is running XND and "
							+ "is reachable via network, and try to reconnect.");
			XNDApp.EndWaitCursor();
			return false;
		}
		// ConsoleView.AppendMessage("View initialized.");
		XNDApp.EndWaitCursor();
		XNDApp.SetStatus("View initialized.");
		try
		{
			m_ttm.getViewer().setInput(null);
			UpdateTree();
			// m_TreeViewer.refresh();
			// m_TreeViewer.setInput(null);
			// m_TableViewer.setInput(null);
		} catch (Exception e)
		{
		}
		return true;
	}	
	public void createPartControl(Composite parent)
	{

		Connect();

		// sash form initialization
		m_SashForm = new SashForm(parent, SWT.SMOOTH);
		GridLayout sashLayout = new GridLayout();
		sashLayout.makeColumnsEqualWidth = false;
		m_SashForm.setLayout(sashLayout);
		m_TreeTabFolder=new TabFolder(m_SashForm,SWT.NONE);
		m_ttm=new TabTreeManager(m_TreeTabFolder);
		
		// general initialization
		Platform.getAdapterManager().registerAdapters(m_AdapterFactory,
				CElement.class);

		// tree initialization
//		InitializeTree();
		if(m_rvm.IsLocal() && m_rvm.IsTagView())
			m_rvm.ToggleTagView();
		UpdateTree();

		// file view sash initialization
		InitializeFileViewForm();

		// table initialization
		InitializeTable();

		m_TagTabFolder = new TabFolder(m_SashFileView, SWT.NONE);
		TabItem item1 = new TabItem(m_TagTabFolder,SWT.NONE);
		item1.setText("All tags");
		m_FileInfo = new TagEditTable(m_TagTabFolder, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
				
//		m_FileInfo = new Text(m_TagTabFolder, SWT.BORDER | SWT.V_SCROLL
//				| SWT.H_SCROLL | SWT.READ_ONLY);
//		m_FileInfo.setText("");
		item1.setControl(m_FileInfo.getTable());
		
		m_SashForm.setWeights(new int[]{33, 66});
		m_SashFileView.setWeights(new int[]{80, 20});
		m_SashFileView.setSashWidth(7);

		// Init tool bar
		InitToolBar();
		// UpdateChecks();
	}
	private void InitToolBar()
	{
		IToolBarManager itm = getViewSite().getActionBars().getToolBarManager();
		/*
		 * if(IsLocal()) { //Add managed folder
		 * itm.add(AppActions.GetAction(AppActions.ID_ADD_MANAGED_DIR));
		 * 
		 * //Switch between virtual/actual directory view
		 * itm.add(AppActions.GetAction(AppActions.ID_SELECT_TAG_VIEW)); }
		 * //refresh view
		 * itm.add(AppActions.GetAction(AppActions.ID_REFRESH_VIEW));
		 */
		if (!IsLocal())
		{
			// Connect to remote
			itm.add(AppActions.GetAction(AppActions.ID_CONNECT_TO_REMOTE));
		}
		/*
		 * //Manage tags
		 * itm.add(AppActions.GetAction(AppActions.ID_MANAGE_TAGS));
		 * if(IsLocal()) { //upload to XNAT itm.add(new Separator());
		 * itm.add(AppActions.GetAction(AppActions.ID_EXPORT_TO_XNAT));
		 * AppActions
		 * .GetAction(AppActions.ID_EXPORT_TO_XNAT).setEnabled(CanUploadToXNAT
		 * ()); }
		 */
	}// end of InitToolBar() method.
	private Collection<CElement> GetSelectedElements(boolean bTree)
	{
		if (bTree)
			return GetSelectedTreeElements();
		else
			return GetSelectedTableElements();
	}
	protected Collection<CElement> GetSelectedTableElements()
	{
		LinkedList<CElement> llfi = new LinkedList<CElement>();
		for (TableItem ti : m_Table.getSelection())
			llfi.add((CElement) ti.getData());
		return llfi;
	}
	public Collection<CElement> GetSelectedElements()
	{
		if (m_Table.isFocusControl())
			return GetSelectedTableElements();
		else
			return GetSelectedTreeElements();
	}
	
	protected Collection<CElement> GetSelectedTreeElements()
	{
		LinkedList<CElement> llfi = new LinkedList<CElement>();
		for (TreeItem ti : m_ttm.getTree().getSelection())
			llfi.add((CElement) ti.getData());
		return llfi;
	}
	protected void ProcessSelection(final Object o, final int how,
			final Collection<CElement> cce, final boolean bTree,
			IProgressMonitor monitor)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				XNDApp.StartWaitCursor();
				// if(bTree) m_Tree.setRedraw(false);
				// else m_TableViewer.getTable().setRedraw(false);
				m_Table.setRedraw(false);
			}
		});
		try
		{
			// operations that need to process an entire scope
			if (o instanceof Rule && ((Rule) o).isSpecialRecursion())
			{
				RootElement re = new RootElement(cce, m_rvm);
				((Rule) o).ApplyRule(re, monitor);
				re.Invalidate();
			}
			// operations working on per-element basis
			else
			{
				// select download folder
				for (CElement ce : cce)
				{
					ce.ApplyOperation(o, how, monitor);
					ce.Invalidate();
					// additionally,
					if (how == CElement.REMOVE_FROM_ROOTS
							&& ce instanceof FSFolder)
					{
						String path = ((FSFolder) ce).GetFSObject()
								.getAbsolutePath();
						if (Utils.CrossCheckDirs(path, Utils
								.GetIncomingFolder()) != 0)
						{
							Utils.ShowMessageBox("Message",
									"Cannot unmanage incoming folder!",
									Window.OK);
						} else
						{
							m_rvm.RemoveManagedFolder(path);
							// UpdateTree();
						}
					}
				}
			}
		} finally
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					XNDApp.EndWaitCursor();
					if (how == CElement.REMOVE_FROM_ROOTS)
						UpdateTree();
					else
					{
						for (CElement ce : cce)
						{
							m_ttm.getViewer().refresh(ce);
						}
					}
					UpdateTable();
					m_Table.setRedraw(true);
					UpdateStatus();
				}
			});
		}
	}
	public void updateTreeMenus()
	{
		m_ttm.updateMenus();
	}
	public static void UpdateMenus()
	{
		Collection<FileView> cfv = AppActions.GetFileViewList();
		for (FileView fv : cfv)
		{
			try
			{
				fv.m_TableContextMenu = fv.CreateContextMenu(fv.m_Table);
				fv.m_Table.setMenu(fv.m_TableContextMenu);
				fv.m_ttm.updateMenus();
//				fv.m_TreeContextMenu = fv.CreateContextMenu(fv.m_Tree);
//				fv.m_Tree.setMenu(fv.m_TreeContextMenu);
			} catch (Exception e)
			{
			}
		}
	}
	protected void SetTag(String tagName, boolean bTree)
	{
		EditTagValueDialog dlg = new EditTagValueDialog(
				m_TableViewer.getTable().getShell(),
				EditTagValueDialog.ADD, m_rvm,tagName);
		int res = dlg.open();
		ItemTag[] tags = {dlg.GetSelectedTag()};
		if (res == Window.OK)
			ProcessSelectionWithProgress(tags, CElement.SETTAGS,
					bTree);
	}
	protected void RemoveTag(String tagName, boolean bTree)
	{
		if (tagName!=null)
		{
			ItemTag[] tags={new ItemTag(tagName)};
			ProcessSelectionWithProgress(tags, CElement.REMOVETAGS, bTree);
		}
		else
		{
			EditTagValueDialog dlg1 = new EditTagValueDialog(
					m_TableViewer.getTable().getShell(),
					EditTagValueDialog.REMOVE, m_rvm, tagName);
			
			if (dlg1.open() == org.eclipse.jface.window.Window.OK)
			{
				ItemTag[] tags = {dlg1.GetSelectedTag()};
				ProcessSelectionWithProgress(tags, CElement.REMOVETAGS, bTree);
			}
		}

	}
	protected Menu CreateContextMenu(Control parent)
	{
		final boolean bTree;
		if (parent instanceof Table)
			bTree = false;
		else
			bTree = true;

		Listener ml = new Listener()
		{
			public void handleEvent(Event event)
			{
				if (!(event.widget instanceof MenuItem))
					return;
				MenuItem mi = (MenuItem) event.widget;
				int ind = mi.getParent().indexOf((MenuItem) (event.widget));
				switch (ind)
				{
					case SETTAG : // add tag
					{
						break;
					}
					case SETDEFAULTTAGS : // add default tags
						ItemTag[] tags = DefaultOntologyManager
								.GetDefaultTags();
						EditTagSetDialog dlg = new EditTagSetDialog(
								m_TableViewer.getTable().getShell(), tags,
								m_rvm);
						int res = 0;
						try
						{
							res = dlg.open();
						} catch (Exception e)
						{
							e.printStackTrace();
						}
						if (res == Window.OK)
						{
							Collection<ItemTag> cit=dlg.m_definedTags;							
							ProcessSelectionWithProgress(cit.toArray(new ItemTag[0]),
									CElement.SETTAGS, bTree);
						}
						break;
					case REMOVETAG : // remove tag
					{						
						break;
					}
					case COPY_TAGS:
						XNDApp.app_ClipboardManager.toClipboard(GetSelectedElements());						
						break;
					case PASTE_TAGS:
						ItemTag[] tags2=XNDApp.app_ClipboardManager.fromClipboard().toArray(new ItemTag[0]);
						ProcessSelectionWithProgress(tags2,CElement.SETTAGS, bTree);
						break;
					case MANAGE : // start managing
						ProcessSelectionWithProgress(null, CElement.MANAGEALL,
								bTree);
						break;
					case UNMANAGE : // stop managing
						ProcessSelectionWithProgress(null,
								CElement.UNMANAGEALL, bTree);
						break;
					case REMOVE_FROM_ROOTS : // remove root folder
						ProcessSelectionWithProgress(null,
								CElement.REMOVE_FROM_ROOTS, bTree);
						break;
					case SENDTO : // upload/download
					/*
					 * String path=null; if(!m_rvm.IsLocal() &&
					 * XNDApp.app_Prefs.getBoolean
					 * ("PrefsFileTransferAutoDownload",true)) {
					 * path=Utils.SelectFolder("Browse for folder",
					 * "Select download folder"); if(path==null) return; }
					 */

						if (!m_rvm.IsLocal())
						{
							DownloadDialog dd = new DownloadDialog(new Shell());
							if (!(dd.open() == Window.OK))
								return;
							ProcessSelectionWithProgress(dd, CElement.SENDTO,
									bTree);
						} else
						// upload to remote archive.
						{
							// select an archive to upload to.
							Collection<CElement> sel = GetSelectedElements();
							if (sel.size() < 1)
								return;

							OpenConnectionDialog ocd = new OpenConnectionDialog(
									new Shell());
							if (ocd.open() != Window.OK)
								return;
							HierarchyUploadManager hum = new HierarchyUploadManager(
									ocd.getServer(), ocd.getUser(), ocd
											.getPass(), sel, XNDApp.app_localVM);
							String err;
							if ((err = hum.isDataValid(true)) != null)
							{
								Utils
										.ShowMessageBox(
												"Data validation failed",
												"Problems with selected files:\n"
														+ err, Window.OK);
								break;
							}
							hum.setUser(true);
							hum.schedule();
							
							/*
							 * Collection<FileView>
							 * actViews=AppActions.GetFileViewList(); //for now,
							 * upload to a first remote archive. for(FileView
							 * fv:actViews) {
							 * if(!fv.GetRepositoryViewManager().IsLocal()) {
							 * ProcessSelectionWithProgress(
							 * fv.GetRepositoryViewManager
							 * ().GetFDC(),CElement.SENDTO,bTree); } }
							 */
						}
						break;
				}
			}
		};
		Menu popup = new Menu(parent);
		//set ontology tags.
		MenuItem setTagItem=CreateMenuItem(popup, ml, "Set tag", true, SWT.CASCADE);
		
		CreateMenuItem(popup, ml, "Set default tags", true, 0);
		MenuItem removeTagItem=CreateMenuItem(popup, ml, "Remove tag", true, SWT.CASCADE);
		CreateMenuItem(popup, ml, "Copy tags", true, 0);
		CreateMenuItem(popup, ml, "Paste tags", true, 0);		
		CreateMenuItem(popup, ml, "", true, SWT.SEPARATOR);
		CreateMenuItem(popup, ml, "Manage", true, 0);
		CreateMenuItem(popup, ml, "Unmanage", true, 0);
		CreateMenuItem(popup, ml, "Unmanage root directory", true, 0);
		CreateMenuItem(popup, ml, "", true, SWT.SEPARATOR);

/////////////// set tag submenu 
		Listener setTagListener = new Listener()
		{
			public void handleEvent(Event event)
			{
				if (!(event.widget instanceof MenuItem))
					return;
				MenuItem item = (MenuItem) event.widget;
				SetTag(item.getText(),bTree);
			}			
		};
		Menu tlmenu=new Menu (setTagItem);
		setTagItem.setMenu(tlmenu);
		TagDescr[] tags=m_rvm.GetVisibleTagList();
		//first item in set tag submenu: set arbitrary tag.
		for (TagDescr tag : tags)
		{
			CreateMenuItem(tlmenu, setTagListener, tag.GetName(), true, 0);
		}
		CreateMenuItem(tlmenu,setTagListener,"Other tag...", true, 0);		
/////////////	
		
/////////////// remove tag submenu 
		Listener removeTagListener = new Listener()
		{
			public void handleEvent(Event event)
			{
				if (!(event.widget instanceof MenuItem))
					return;
				MenuItem item = (MenuItem) event.widget;
				RemoveTag(item.getText(),bTree);
			}
		};
		Menu rtmenu=new Menu (removeTagItem);
		removeTagItem.setMenu(rtmenu);
		//first item in set tag submenu: set arbitrary tag.
		for (TagDescr tag : tags)
		{
			CreateMenuItem(rtmenu, removeTagListener, tag.GetName(), true, 0);
		}
		CreateMenuItem(rtmenu,removeTagListener,"Other tag...", true, 0);		
/////////////			
		
			
/////// default rule menu
		Listener pl = new Listener()
		{
			public void handleEvent(Event event)
			{
				if (!(event.widget instanceof MenuItem))
					return;
				MenuItem item = (MenuItem) event.widget;
				// int ind=rulemenu.indexOf(item);
				int ind = item.getParent().indexOf(item);

				switch (ind)
				{
					case RULEDICOM : // DICOM rule
						ProcessSelectionWithProgress(RuleManager
								.getDefaultRule(Rule.RULE_DICOM), -1, bTree);
						break;
					case RULENAMING : // Naming rule
						ProcessSelectionWithProgress(RuleManager
								.getDefaultRule(Rule.RULE_NAMING), -1, bTree);
						break;
					case RULEPATTERN : // Pattern rule
						ProcessSelectionWithProgress(RuleManager
								.getDefaultRule(Rule.RULE_MODTAG), -1, bTree);
						break;
					case RULECOLLECTION : // Make collection
						ProcessSelectionWithProgress(RuleManager
								.getDefaultRule(Rule.RULE_COL), -1, bTree);
				}
			}
		};
		MenuItem defRuleItem = CreateMenuItem(popup, ml, "Apply default rule",
				true, SWT.CASCADE);
		Menu rsmenu = new Menu(defRuleItem);
		defRuleItem.setMenu(rsmenu);
		CreateMenuItem(rsmenu, pl, "DICOM", true, 0);
		CreateMenuItem(rsmenu, pl, "Naming", true, 0);
		CreateMenuItem(rsmenu, pl, "Tag pattern", true, 0);
		CreateMenuItem(rsmenu, pl, "Collection", true, 0);

		rsmenu.addMenuListener(new MenuListener()
		{
			public void menuShown(MenuEvent e)
			{
				MenuItem[] items = ((Menu) e.widget).getItems();
				int el_types = CElement.getTypes(GetSelectedElements(bTree));
				EnableRuleMenuItem(items[RULEDICOM], el_types, Rule.RULE_DICOM);
				EnableRuleMenuItem(items[RULENAMING], el_types,
						Rule.RULE_NAMING);
				EnableRuleMenuItem(items[RULEPATTERN], el_types,
						Rule.RULE_MODTAG);
				EnableRuleMenuItem(items[RULECOLLECTION], el_types,
						Rule.RULE_COL);
			}
			public void menuHidden(MenuEvent e)
			{
			}
		});
////////////////////
		
/////////////////// custom rule submenu.
		MenuItem custRuleItem = CreateMenuItem(popup, ml, "Apply custom rule",
				true, SWT.CASCADE);
		rsmenu = new Menu(custRuleItem);
		custRuleItem.setMenu(rsmenu);

		// custom rule submenu command listener.
		Listener cpl = new Listener()
		{
			public void handleEvent(Event event)
			{
				if (!(event.widget instanceof MenuItem))
					return;
				MenuItem item = (MenuItem) event.widget;
				ProcessSelectionWithProgress(RuleManager
						.getRule(item.getText()), -1, bTree);
			}
		};
		for (Rule r : RuleManager.getCustomRulesOnly())
		{
			CreateMenuItem(rsmenu, cpl, r.getuid(), true, 0);
		}
		rsmenu.addMenuListener(new MenuListener()
		{
			public void menuShown(MenuEvent e)
			{
				MenuItem[] items = ((Menu) e.widget).getItems();
				int el_types = CElement.getTypes(GetSelectedElements(bTree));
				for (MenuItem mi : items)
				{
					EnableRuleMenuItem(mi, el_types, RuleManager.getRule(
							mi.getText()).getType());
				}
			}
			public void menuHidden(MenuEvent e)
			{
			}
		});
//////////////////////////////
		CreateMenuItem(popup, ml, "", true, SWT.SEPARATOR);
		CreateMenuItem(popup, ml, m_rvm.IsLocal() ? "Upload" : "Download",
				true, 0);

		// expandable "Send to remote" menu
		/*
		 * Listener sl=new Listener() { public void handleEvent(Event event) {
		 * if (!(event.widget instanceof MenuItem)) return;
		 * ProcessSelectionWithProgress
		 * (((MenuItem)event.widget).getText(),SENDTO,bTree); } }; MenuItem
		 * sendItem=CreateMenuItem(popup,ml,"Send to",true,SWT.CASCADE); final
		 * Menu sendMenu = new Menu(sendItem); sendItem.setMenu(sendMenu);
		 * String[] archives=XNDApp.GetArchiveList(m_rvm); for(String
		 * ar:archives) CreateMenuItem(sendMenu,sl,ar,true,0);
		 * sendMenu.addMenuListener(new MenuListener() { public void
		 * menuShown(MenuEvent e) { MenuItem[] items=sendMenu.getItems();
		 * for(MenuItem mi:items) mi.setEnabled(true); } public void
		 * menuHidden(MenuEvent e){} });
		 */
		popup.addMenuListener(new MenuListener()
		{
			public void menuShown(MenuEvent e)
			{
				Menu m = (Menu) e.widget;
				MenuItem[] items = m.getItems();
				items[SETTAG].setEnabled(false);
				items[SETDEFAULTTAGS].setEnabled(false);
				items[COPY_TAGS].setEnabled(false);
				items[PASTE_TAGS].setEnabled(false);
				items[REMOVETAG].setEnabled(false);
				items[MANAGE].setEnabled(false);
				items[UNMANAGE].setEnabled(false);
				items[REMOVE_FROM_ROOTS].setEnabled(false);
				items[SENDTO].setEnabled(false);
				{
					Collection<CElement> sel = GetSelectedElements(bTree);
					for (CElement ce : sel)
					{
						if (!(ce instanceof Resource)) items[PASTE_TAGS].setEnabled(true);
						if ((ce instanceof VirtualFolder) || (ce instanceof DBElement))
						{
							items[COPY_TAGS].setEnabled(true);
						}
						if (ce instanceof FSFolder && sel.size() == 1)
						{
							if (((FSFolder) ce).IsRoot())
							{
								items[REMOVE_FROM_ROOTS].setEnabled(true);
							}
						}
						if (ce instanceof FSFile || ce instanceof FSFolder)
						{
							items[MANAGE].setEnabled(true);
						}
						if (ce instanceof DBElement || ce instanceof VirtualFolder)
						{
							items[SENDTO].setEnabled(true);							
						}
						if (ce instanceof DBElement
								|| ce instanceof VirtualFolder
								|| ce instanceof FSFolder)
						{
							items[SETTAG].setEnabled(true);
							items[SETDEFAULTTAGS].setEnabled(true);
							items[REMOVETAG].setEnabled(true);
							items[UNMANAGE].setEnabled(true);
						}
					}
				}
				if (m_rvm.IsTagView())
				{
					items[MANAGE].setEnabled(false);
				}
			}
			public void menuHidden(MenuEvent e)
			{
			}
		});
		return popup;
	}

	private void EnableRuleMenuItem(MenuItem mi, int el_types, int rule_type)
	{
		mi.setEnabled(false);
		if (m_type == TYPE_LOCAL)
		{
			if (rule_type == Rule.RULE_MACRO)
			{
				mi.setEnabled(true);
				return;
			}
			if ((el_types & CElement.DBELEMENT) != 0
					|| (el_types & CElement.VIRTUALFOLDER) != 0
					|| (el_types & CElement.FSFOLDER) != 0)
			{
				if (rule_type == Rule.RULE_DICOM
						|| rule_type == Rule.RULE_MODTAG
						|| rule_type == Rule.RULE_COL)
					mi.setEnabled(true);
			}
			if ((el_types & CElement.FSFOLDER) != 0)
				if (rule_type == Rule.RULE_NAMING)
					mi.setEnabled(true);
		}
	}

	private MenuItem CreateMenuItem(Menu m, Listener l, String text,
			boolean bEnabled, int style)
	{
		MenuItem mi = new MenuItem(m, style);
		mi.setText(text);
		mi.setEnabled(bEnabled);
		mi.addListener(SWT.Selection, l);
		return mi;
	}
	public void UpdateTree()
	{
		CElement ce = (CElement) m_ttm.getViewer().getInput();
		if (m_rvm.IsTagView())
		{
			if (ce == null || !(ce instanceof VirtualFolder))
			{
				ce = new VirtualFolder(new Context(), m_rvm, null, null);
				m_ttm.getViewer().setInput(ce);
			} else
				ce.Invalidate();
		} else
		{
			if (ce == null || ce instanceof VirtualFolder
					|| ce instanceof FSRoot)
			{
				ce = new FSRoot(m_rvm, null);
				m_ttm.getTree().removeAll();
				m_ttm.getViewer().setInput(ce);
			} else
			{
				ce.Invalidate();
			}
		}
		try
		{
			m_ttm.getViewer().refresh();
		} catch (Exception e)
		{
			System.out.println("Please refresh tree manually.");
		}
	}

	private void UpdateTable()
	{
		if (m_ttm.getTree() != null)
		{
			TreeItem[] sel = m_ttm.getTree().getSelection();
			if (sel.length < 1)
				m_TableViewer.setInput(null);
			else
				m_TableViewer.setInput((CElement) (sel[0].getData()));
		}
		UpdateStatus();
	}
	private void CompactTable()
	{
		for (int i = 0; i < m_Table.getColumnCount(); i++)
			m_Table.getColumn(i).pack();
	}
	/*
	 * private void UpdateChecks() { if(m_TableViewer instanceof
	 * CheckboxTableViewer) { CheckboxTableViewer
	 * ctv=(CheckboxTableViewer)m_TableViewer; int ind=0; TableElement fi;
	 * while((fi=(TableElement)(ctv.getElementAt(ind++)))!=null) {
	 * ctv.setChecked(fi, fi.IsManaged()); } } }
	 */
	
	public void dispose()
	{
		m_rvm.SessionEnd();
		Platform.getAdapterManager().unregisterAdapters(m_AdapterFactory);
		super.dispose();
	}

	public void setFocus()
	{
		m_Table.setFocus();
		XNDPerspective.UpdateActionView(this, m_TableViewer.getSelection());
	}
	/*
	 * public void ProcessDnD(Object data, TreeItem item) { if(data instanceof
	 * ItemRecord[]) { MessageBox mb = new MessageBox(new Shell(), SWT.OK |
	 * SWT.CANCEL); mb.setMessage("Transfer selected files?");
	 * mb.setText("Confirm file transfer"); ItemRecord[] irs=(ItemRecord[])data;
	 * if(mb.open()==SWT.OK && irs.length>0) {
	 * Logger.getRootLogger().debug("File transfer initiated: "
	 * +irs[0].GetAbsolutePath()); if(IsLocal()) //get { //
	 * XNDApp.app_fdc.AddToDownloadQueue
	 * (((TreeElement)(item.getData())).GetFile(), irs); } else //put {
	 * XNDApp.app_fdc.AddToUploadQueue(irs, null); } } } }
	 */

}