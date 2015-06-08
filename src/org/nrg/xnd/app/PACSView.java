/**
 * 
 */
package org.nrg.xnd.app;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.nrg.fileserver.Context;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.model.RootElement;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.model.VirtualFolder;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.tools.HierarchyUploadManager;
import org.nrg.xnd.tools.ImageViewer.IProgressReporter;
import org.nrg.xnd.tools.ImageViewer.ImageViewerManager;
import org.nrg.xnd.tools.ImageViewer.RadWSProgress;
import org.nrg.xnd.ui.TableItemTransfer;
import org.nrg.xnd.ui.TagEditTable;
import org.nrg.xnd.ui.dialogs.DownloadDialog;
import org.nrg.xnd.ui.dialogs.EditTagSetDialog;
import org.nrg.xnd.ui.dialogs.EditTagValueDialog;
import org.nrg.xnd.ui.dialogs.OpenConnectionDialog;
import org.nrg.xnd.utils.Utils;
import org.nrg.xnd.utils.dicom.StudyList;
import org.nrg.xnd.utils.dicom.StudyRecord;

/**
 * @author mmilch
 * 
 */

public class PACSView extends ViewBase
{
	static protected final String m_ID = "org.nrg.xnat.desktop.PACSView";
	static protected final String TREE_FS = "File view", TREE_TAG = "Worklist view";
	private Button m_autoCineCheck;
	private Group m_viewOptions;
	private TabFolder m_TreeTabFolder;
	private TabFolder m_TagTabFolder;
	private Frame m_viewFrame;
	private Composite viewerFrame;
	private SashForm m_brSash;
	private SashForm m_rightSash;
	private List m_SourceList;
	private List m_colList;
	private TabItem m_tabItem2;
	private TabFolder m_tabFolder2;
	private TabItem m_tabItem1;
	private TabFolder m_tabFolder1;
	private SashForm m_sashForm1;
	private TreeViewer m_tv = null;
	private String m_prevColSel="";
	private String m_prevWLSel="";
	
	// private Text m_FileInfo;
	
	private ImageViewerManager m_imView;
	private Dimension m_size;

	protected TreeViewer InitializeTree(boolean bFS)
	{
		m_tv = new TreeViewer(m_TreeTabFolder, SWT.BORDER | SWT.MULTI
				| SWT.V_SCROLL | SWT.FULL_SELECTION);
		TabItem item1 = new TabItem(m_TreeTabFolder, SWT.NONE);
		item1.setText(bFS ? TREE_FS : TREE_TAG);
		item1.setControl(m_tv.getTree());

		m_tv.setLabelProvider(new WorkbenchLabelProvider());
		m_tv.setContentProvider(new PACSViewTreeContentProvider()); // BaseWorkbenchContentProvider());
		Tree tree = m_tv.getTree();

		// set tree cell appearance
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		// create default tree column
		TreeColumn col1 = new TreeColumn(tree, SWT.LEFT);
		col1.setText("Patient name");
		col1.setWidth(160);
		tree.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event evt)
			{
				if (evt.item instanceof TreeItem)
				{
					try
					{
						XNDPerspective.SelectionChanged(PACSView.this, m_tv
								.getSelection());
						XNDApp.StartWaitCursor();
						AppActions.GetAction(
								"org.nrg.xnat.desktop.UploadToXNAT")
								.setEnabled(CanUploadToXNAT());
						updateTagInfo();
						updateViewer(new RadWSProgress(m_imView.getImView()));
						XNDApp.EndWaitCursor();
					} catch (Exception e)
					{
						XNDApp.EndWaitCursor();
						e.printStackTrace();
					}
				}
			}
		});

		String[] tags = m_rvm.GetTableTags();
		TreeColumn col;
		for (int i = 0; i < tags.length; i++)
		{
			col = new TreeColumn(tree, SWT.LEFT);
			col.setText(tags[i]);
			col.setWidth(70);
		}
		TableLabelProvider lp = new TableLabelProvider();
		m_tv.setLabelProvider(lp);
		return m_tv;
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
		/*
		 * if (m_Table == null) return; int cnt = m_Table.getItemCount(); if
		 * (cnt == XNDApp.app_maxRecords)
		 * XNDApp.SetStatus("Items in current folder: " + cnt +
		 * " (MAXIMUM allowed to display in the table)"); else
		 * XNDApp.SetStatus("Items in current folder: " + cnt);
		 */
	}
	private void MakeDragSource(final PACSView fv)
	{
		/*
		 * DragSource ds = new DragSource(m_Table, DND.DROP_COPY); Transfer[]
		 * types = new Transfer[]{TableItemTransfer.getInstance()}; final
		 * TableItemTransfer tit = TableItemTransfer.getInstance();
		 * ds.setTransfer(types); ds.addDragListener(new DragSourceAdapter() {
		 * public void dragStart(DragSourceEvent e) { } public void
		 * dragSetData(DragSourceEvent e) { TableItem[] ti =
		 * m_Table.getSelection(); if (ti.length > 0) e.data = ti; } public void
		 * dragFinished(DragSourceEvent dte) { } });
		 */
	}
	private void MakeDropTarget(final PACSView fv)
	{

		DropTarget dt = new DropTarget(m_ttm.getTree(), DND.DROP_COPY
				| DND.DROP_DEFAULT);
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
	public void Refresh(boolean bTableOnly)
	{
		XNDApp.StartWaitCursor();
		updateColList();
		m_tv.getTree().removeAll();
		/*
		 * if (m_ttm.getTree() != null && m_ttm.getViewer() != null &&
		 * !bTableOnly) UpdateTree(); // UpdateTable();
		 */
		XNDApp.EndWaitCursor();
	}
	public boolean Connect()
	{
		if (m_rvm != null)
			m_rvm.SessionEnd();
		// Refresh(true);
		XNDApp.StartWaitCursor();
		Vector param = new Vector();
		// if (getViewSite().getSecondaryId().compareTo("local") == 0)
		{
			setPartName("Worklist");
			m_type = TYPE_LOCAL;
			m_rvm = XNDApp.app_localVM;
			XNDApp.SetStatus("Initializing local view...");
		}
		/*
		 * else if (getViewSite().getSecondaryId().compareTo("remote") == 0) {
		 * m_type = TYPE_REMOTE; try { String addr =
		 * XNDApp.app_Prefs.get("RemoteAddress", Utils.REMOTE_ADDRESS_DEFAULT);
		 * String port = new Integer(XNDApp.app_Prefs.getInt("ClientPort",
		 * Utils.PORT_REPOSITORY_DEFAULT)).toString(); //
		 * ConsoleView.AppendMessage("Connecting to "+addr+":"+port);
		 * XNDApp.SetStatus("Connecting to " + addr + ":" + port);
		 * 
		 * XNDApp.app_remoteVM = new RepositoryViewManager( new
		 * RestRepositoryManager(new URL(addr + ":" + port))); m_rvm =
		 * XNDApp.app_remoteVM; } catch (Exception e) { XNDApp.app_remoteVM =
		 * null; } setPartName("Remote archive: " +
		 * XNDApp.app_Prefs.get("RemoteAddress", Utils.REMOTE_ADDRESS_DEFAULT));
		 * m_rvm = XNDApp.app_remoteVM; //ConsoleView.AppendMessage(
		 * "Initializing remote view (waiting for remote host)...");
		 * XNDApp.SetStatus
		 * ("Initializing remote view (waiting for remote host)..."); if
		 * (XNDApp.app_remoteVM == null || !((RestRepositoryManager)
		 * XNDApp.app_remoteVM.GetRM()) .VerifyConnection()) {
		 * XNDApp.EndWaitCursor(); Utils.ShowMessageBox("Warning",
		 * "Unable to connect to remote repository", Window.OK); //
		 * ConsoleView.AppendMessage("Remote connection failed");
		 * XNDApp.SetStatus("Remote connection failed"); return false; }
		 * m_rvm.InitFileTransfer(); if (!m_rvm.IsTagView())
		 * m_rvm.ToggleTagView(); } else // m_type==TYPE_XNAT { m_type =
		 * TYPE_XNAT; try { OpenConnectionDialog ocd = new
		 * OpenConnectionDialog(new Shell()); ocd.open();
		 * 
		 * String serv = XNDApp.app_Prefs.get(
		 * "OpenConnectionDialog.XNATServer", "http://central.xnat.org"), usr =
		 * XNDApp.app_Prefs.get( "OpenConnectionDialog.XNATUser", "guest"), pass
		 * = XNDApp.app_Prefs .get("OpenConnectionDialog.Pass", "guest");
		 * 
		 * if (serv.length() < 1 || usr.length() < 3 || pass.length() < 4) {
		 * Utils.ShowMessageBox( "Configuration needed",
		 * "Before using XNAT view, enter required connection info under View->Preferences->Client/Server->XNAT Client"
		 * , Window.OK); } XNATRestAdapter re = new XNATRestAdapter(serv, usr,
		 * pass); setPartName("XNAT archive: " + serv); XNDApp.app_XNATVM = new
		 * RepositoryViewManager(re); m_rvm = XNDApp.app_XNATVM;
		 * XNDApp.SetStatus("Connecting"); if (!re.VerifyConnection()) {
		 * XNDApp.EndWaitCursor(); Utils.ShowMessageBox("Warning",
		 * "Cannot connect to XNAT server", Window.OK);
		 * XNDApp.SetStatus("Connection to XNAT server failed"); return false; }
		 * m_rvm.InitFileTransfer(); if (!m_rvm.IsTagView())
		 * m_rvm.ToggleTagView(); } catch (Exception e) { e.printStackTrace();
		 * XNDApp.app_XNATVM = null; } }
		 */
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
		m_rvm.ToggleTagView();
		XNDApp.EndWaitCursor();
		XNDApp.SetStatus("View initialized.");
		try
		{
			m_ttm.getViewer().setInput(null);
			UpdateTree(true);
			// m_TreeViewer.refresh();
			// m_TreeViewer.setInput(null);
			// m_TableViewer.setInput(null);
		} catch (Exception e)
		{
		}
		return true;
	}
	private void updateViewer(final IProgressReporter monitor)
	{		
		m_imView.SetPreviewMode(true);
		monitor.taskName("Loading images");
		final Collection<CElement> cce = new LinkedList<CElement>();
		if(cce.size()==1)
		{
			for(CElement ce:cce)
			{
				String l=ce.GetLabel();
				if(l==null) continue;
				if(ce.GetLabel().compareTo(m_prevWLSel)==0) return;
				else m_prevWLSel=l;
			}
		}
		
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
			
			Collection<StudyRecord> studies = new StudyList(cce, monitor)			
					.getStudies();
			if(monitor.isCanceled()) return;
			if (studies.size() < 1)
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						m_imView.updateStatus("Found no DICOM studies in the selection");
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
						m_imView.updateStatus("More than one study in selection. First study will be loaded.");
					}
				});				
			}
			if (!m_imView.OpenImages(arr[0],monitor,true))
			{
//				XNDApp.GetViewerFrame().setVisible(false);
				if(monitor.isCanceled()) return;
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						m_imView.updateStatus("Could not open some images!");
					}
				});				
				return;
			}
		} finally
		{
		}		
	}
	private void updateSources()
	{
		Collection<String> cs = XNDApp.app_aeList.getAENames();
		m_SourceList.removeAll();
		for (String s : cs)
		{
			m_SourceList.add(s);
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	public void createPartControl(Composite parent)
	{
		Connect();

		// sash form initialization
		SashForm mainSash = new SashForm(parent, SWT.SMOOTH);
		GridLayout sashLayout = new GridLayout();
		sashLayout.makeColumnsEqualWidth = false;
		mainSash.setLayout(sashLayout);

		SashForm leftPanel = new SashForm(mainSash, SWT.VERTICAL);
		{
			m_sashForm1 = new SashForm(leftPanel, SWT.VERTICAL);
			FillLayout m_sashForm1Layout = new FillLayout(
					org.eclipse.swt.SWT.VERTICAL);
			m_sashForm1Layout.type = SWT.VERTICAL;
			m_sashForm1.setLayout(m_sashForm1Layout);
			m_sashForm1.setSize(60, 30);
			{
				m_tabFolder1 = new TabFolder(m_sashForm1, SWT.NONE);
				{
					m_tabItem1 = new TabItem(m_tabFolder1, SWT.NONE);
					m_tabItem1.setText("Collections");
					{
						m_colList = new List(m_tabFolder1, SWT.BORDER | SWT.SINGLE);
						m_tabItem1.setControl(m_colList);
						m_colList.addSelectionListener(new SelectionAdapter()
						{
							public void widgetSelected(SelectionEvent evt)
							{
								UpdateTree(false);
							}
						});
					}
				}
				m_tabFolder1.setSelection(0);
			}
			{
				m_tabFolder2 = new TabFolder(m_sashForm1, SWT.NONE);
				{
					m_tabItem2 = new TabItem(m_tabFolder2, SWT.NONE);
					m_tabItem2.setText("Sources");
					{
						m_SourceList = new List(m_tabFolder2, SWT.BORDER);
						m_tabItem2.setControl(m_SourceList);
						m_SourceList.setEnabled(false);
						updateSources();
						m_SourceList.setSelection(0);
					}
				}
				m_tabFolder2.setSelection(0);
			}
		}
		updateColList();
		m_colList.select(0);		
		System.err.println(m_colList.getSelectionIndex());

		// file view sash initialization
		m_rightSash = new SashForm(mainSash, SWT.VERTICAL);
		GridLayout layout = new GridLayout();
		m_rightSash.setLayout(layout);
		GridData gd = new GridData();
		m_rightSash.setLayoutData(gd);
		m_rightSash.setOrientation(SWT.VERTICAL);
		
		SashForm wlSash = new SashForm(m_rightSash, SWT.HORIZONTAL);
		wlSash.setLayout(layout);
		wlSash.setLayoutData(gd);

		
		m_TreeTabFolder = new TabFolder(wlSash, SWT.NONE);
		m_ttm = new TabTreeManager(m_TreeTabFolder);

		// general initialization
		Platform.getAdapterManager().registerAdapters(m_AdapterFactory,
				CElement.class);

		// tree initialization
		// InitializeTree();
		UpdateTree(true);

		// table initialization

		m_TagTabFolder = new TabFolder(wlSash, SWT.NONE);
		TabItem item1 = new TabItem(m_TagTabFolder, SWT.NONE);
		item1.setText("All tags");

		// m_FileInfo = new Text(m_TagTabFolder, SWT.BORDER | SWT.V_SCROLL
		// | SWT.H_SCROLL | SWT.READ_ONLY);
		m_FileInfo = new TagEditTable(m_TagTabFolder, SWT.BORDER | SWT.V_SCROLL
				| SWT.H_SCROLL);
		// m_FileInfo.setText("");
		item1.setControl(m_FileInfo.getTable());

		{
			m_brSash = new SashForm(m_rightSash, SWT.HORIZONTAL);
			GridData m_brSashLData = new GridData();
			m_brSash.setLayoutData(m_brSashLData);
			{
				viewerFrame = new Composite(m_brSash, SWT.EMBEDDED);
				FillLayout viewerFrameLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				viewerFrame.setLayout(viewerFrameLayout);
				{
					m_viewFrame = SWT_AWT.new_Frame(viewerFrame);										
					
					m_imView=new ImageViewerManager(m_viewFrame,false);
					
					m_viewFrame.addComponentListener(new ComponentAdapter() 
					{
						public void componentResized(ComponentEvent evt) 
						{
							onResize();
						}
					});
				}
			}
			{
				m_viewOptions = new Group(m_brSash, SWT.NONE);
				GridLayout m_viewOptionsLayout = new GridLayout();
				m_viewOptionsLayout.numColumns = 2;
				m_viewOptions.setLayout(m_viewOptionsLayout);
				m_viewOptions.setText("Preview options");
				{
					m_autoCineCheck = new Button(m_viewOptions, SWT.CHECK | SWT.LEFT);
					m_autoCineCheck.setText("Auto Cine");
					GridData m_autoCineCheckLData = new GridData();
					m_autoCineCheck.setLayoutData(m_autoCineCheckLData);
					m_autoCineCheck.setEnabled(false);
				}
			}
		}
		m_rightSash.setWeights(new int[]{60,40});		
		mainSash.setWeights(new int[]{20, 80});
		wlSash.setWeights(new int[]{80,20});
		m_brSash.setWeights(new int[]{80,20});
		// Init tool bar
		InitToolBar();
		// UpdateChecks();

		parent.addFocusListener(new FocusAdapter()
		{
			public void focusGained(FocusEvent evt)
			{
				System.out.println("parent.focusGained, event=" + evt);
			}
		});
		onResize();
	}
	private void onResize()
	{
		Dimension newSz = m_viewFrame.getSize();
		m_size = newSz;
		m_imView.UpdateSize(m_size);		
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
	/*
	 * private Collection<CElement> GetSelectedElements(boolean bTree) { if
	 * (bTree) return GetSelectedTreeElements(); else return
	 * GetSelectedTableElements(); }
	 */

	/*
	 * private Collection<CElement> GetSelectedTableElements() {
	 * LinkedList<CElement> llfi = new LinkedList<CElement>(); for (TableItem ti
	 * : m_Table.getSelection()) llfi.add((CElement) ti.getData()); return llfi;
	 * }
	 */
	public Collection<CElement> GetSelectedElements()
	{
		// if (m_Table.isFocusControl())
		// return GetSelectedTableElements();
		// else
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
				m_tv.getTree().setRedraw(false);
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
					m_tv.getTree().setRedraw(true);
					XNDApp.EndWaitCursor();
					updateColList();
					UpdateTree(true);
					/*
					 * else { for (CElement ce : cce) {
					 * m_ttm.getViewer().refresh(ce); } }
					 */
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
		/*
		 * Collection<PACSView> cfv = AppActions.GetPACSViewList(); for
		 * (PACSView fv : cfv) { try { fv.m_TableContextMenu =
		 * fv.CreateContextMenu(fv.m_Table);
		 * fv.m_Table.setMenu(fv.m_TableContextMenu); fv.m_ttm.updateMenus(); //
		 * fv.m_TreeContextMenu = fv.CreateContextMenu(fv.m_Tree); //
		 * fv.m_Tree.setMenu(fv.m_TreeContextMenu); } catch (Exception e) { } }
		 */
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
						EditTagValueDialog dlg = new EditTagValueDialog(m_tv
								.getTree().getShell(), EditTagValueDialog.ADD,
								m_rvm,null);
						int res = dlg.open();
						ItemTag[] tags = {dlg.GetSelectedTag()};
						if (res == Window.OK)
							ProcessSelectionWithProgress(tags,
									CElement.SETTAGS, bTree);
						break;
					}

					case SETDEFAULTTAGS : // add default tags
						ItemTag[] tags = DefaultOntologyManager
								.GetDefaultTags();
						EditTagSetDialog dlg = new EditTagSetDialog(m_tv
								.getTree().getShell(), tags, m_rvm);
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
							Collection<ItemTag> cit = dlg.m_definedTags;
							ProcessSelectionWithProgress(cit
									.toArray(new ItemTag[0]), CElement.SETTAGS,
									bTree);
						}
						break;
					case REMOVETAG : // remove tag
					{
						EditTagValueDialog dlg1 = new EditTagValueDialog(m_tv
								.getTree().getShell(),
								EditTagValueDialog.REMOVE, m_rvm,null);
						if (dlg1.open() == org.eclipse.jface.window.Window.OK)
						{
							ItemTag[] tags1 = {dlg1.GetSelectedTag()};
							ProcessSelectionWithProgress(tags1,
									CElement.REMOVETAGS, bTree);
						}
						break;
					}
					case COPY_TAGS :
						XNDApp.app_ClipboardManager
								.toClipboard(GetSelectedElements());
						break;
					case PASTE_TAGS :
						ItemTag[] tags2 = XNDApp.app_ClipboardManager
								.fromClipboard().toArray(new ItemTag[0]);
						ProcessSelectionWithProgress(tags2, CElement.SETTAGS,
								bTree);
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
							hum.schedule();
							/*
							 * Collection<PACSView>
							 * actViews=AppActions.GetPACSViewList(); //for now,
							 * upload to a first remote archive. for(PACSView
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
		CreateMenuItem(popup, ml, "Set tag", true, 0);
		CreateMenuItem(popup, ml, "Set default tags", true, 0);
		CreateMenuItem(popup, ml, "Remove tag", true, 0);
		CreateMenuItem(popup, ml, "Copy tags", true, 0);
		CreateMenuItem(popup, ml, "Paste tags", true, 0);
		CreateMenuItem(popup, ml, "", true, SWT.SEPARATOR);
		CreateMenuItem(popup, ml, "Manage", true, 0);
		CreateMenuItem(popup, ml, "Unmanage", true, 0);
		CreateMenuItem(popup, ml, "Unmanage root directory", true, 0);
		// CreateMenuItem(popup, ml, "", true, SWT.SEPARATOR);

		/*
		 * // default rule menu Listener pl = new Listener() { public void
		 * handleEvent(Event event) { if (!(event.widget instanceof MenuItem))
		 * return; MenuItem item = (MenuItem) event.widget; // int
		 * ind=rulemenu.indexOf(item); int ind = item.getParent().indexOf(item);
		 * 
		 * switch (ind) { case RULEDICOM : // DICOM rule
		 * ProcessSelectionWithProgress(RuleManager
		 * .getDefaultRule(Rule.RULE_DICOM), -1, bTree); break; case RULENAMING
		 * : // Naming rule ProcessSelectionWithProgress(RuleManager
		 * .getDefaultRule(Rule.RULE_NAMING), -1, bTree); break; case
		 * RULEPATTERN : // Pattern rule
		 * ProcessSelectionWithProgress(RuleManager
		 * .getDefaultRule(Rule.RULE_MODTAG), -1, bTree); break; case
		 * RULECOLLECTION : // Make collection
		 * ProcessSelectionWithProgress(RuleManager
		 * .getDefaultRule(Rule.RULE_COL), -1, bTree); } } }; MenuItem
		 * defRuleItem = CreateMenuItem(popup, ml, "Apply default rule", true,
		 * SWT.CASCADE); Menu rsmenu = new Menu(defRuleItem);
		 * defRuleItem.setMenu(rsmenu); CreateMenuItem(rsmenu, pl, "DICOM",
		 * true, 0); CreateMenuItem(rsmenu, pl, "Naming", true, 0);
		 * CreateMenuItem(rsmenu, pl, "Tag pattern", true, 0);
		 * CreateMenuItem(rsmenu, pl, "Collection", true, 0);
		 * 
		 * rsmenu.addMenuListener(new MenuListener() { public void
		 * menuShown(MenuEvent e) { MenuItem[] items = ((Menu)
		 * e.widget).getItems(); int el_types =
		 * CElement.getTypes(GetSelectedTreeElements()); //
		 * GetSelectedElements(bTree)); EnableRuleMenuItem(items[RULEDICOM],
		 * el_types, Rule.RULE_DICOM); EnableRuleMenuItem(items[RULENAMING],
		 * el_types, Rule.RULE_NAMING); EnableRuleMenuItem(items[RULEPATTERN],
		 * el_types, Rule.RULE_MODTAG);
		 * EnableRuleMenuItem(items[RULECOLLECTION], el_types, Rule.RULE_COL); }
		 * public void menuHidden(MenuEvent e) { } });
		 * 
		 * // custom rule submenu. MenuItem custRuleItem = CreateMenuItem(popup,
		 * ml, "Apply custom rule", true, SWT.CASCADE); rsmenu = new
		 * Menu(custRuleItem); custRuleItem.setMenu(rsmenu);
		 * 
		 * // custom rule submenu command listener. Listener cpl = new
		 * Listener() { public void handleEvent(Event event) { if
		 * (!(event.widget instanceof MenuItem)) return; MenuItem item =
		 * (MenuItem) event.widget; ProcessSelectionWithProgress(RuleManager
		 * .getRule(item.getText()), -1, bTree); } }; for (Rule r :
		 * RuleManager.getCustomRulesOnly()) { CreateMenuItem(rsmenu, cpl,
		 * r.getuid(), true, 0); } rsmenu.addMenuListener(new MenuListener() {
		 * public void menuShown(MenuEvent e) { MenuItem[] items = ((Menu)
		 * e.widget).getItems(); int el_types =
		 * CElement.getTypes(GetSelectedTreeElements
		 * ());//GetSelectedElements(bTree)); for (MenuItem mi : items) {
		 * EnableRuleMenuItem(mi, el_types, RuleManager.getRule(
		 * mi.getText()).getType()); } } public void menuHidden(MenuEvent e) { }
		 * });
		 * 
		 * CreateMenuItem(popup, ml, "", true, SWT.SEPARATOR);
		 * CreateMenuItem(popup, ml, m_rvm.IsLocal() ? "Upload" : "Download",
		 * true, 0);
		 * 
		 * popup.addMenuListener(new MenuListener() { public void
		 * menuShown(MenuEvent e) { Menu m = (Menu) e.widget; MenuItem[] items =
		 * m.getItems(); items[SETTAG].setEnabled(false);
		 * items[SETDEFAULTTAGS].setEnabled(false);
		 * items[COPY_TAGS].setEnabled(false);
		 * items[PASTE_TAGS].setEnabled(false);
		 * items[REMOVETAG].setEnabled(false); items[MANAGE].setEnabled(false);
		 * items[UNMANAGE].setEnabled(false);
		 * items[REMOVE_FROM_ROOTS].setEnabled(false);
		 * items[SENDTO].setEnabled(false); { Collection<CElement> sel =
		 * GetSelectedTreeElements();//GetSelectedElements(bTree); for (CElement
		 * ce : sel) { if (!(ce instanceof Resource))
		 * items[PASTE_TAGS].setEnabled(true); if ((ce instanceof VirtualFolder)
		 * || (ce instanceof DBElement)) { items[COPY_TAGS].setEnabled(true); }
		 * if (ce instanceof FSFolder && sel.size() == 1) { if (((FSFolder)
		 * ce).IsRoot()) { items[REMOVE_FROM_ROOTS].setEnabled(true); } } if (ce
		 * instanceof FSFile || ce instanceof FSFolder) {
		 * items[MANAGE].setEnabled(true); } if (ce instanceof DBElement || ce
		 * instanceof VirtualFolder || ce instanceof FSFolder) {
		 * items[SENDTO].setEnabled(true); items[SETTAG].setEnabled(true);
		 * items[SETDEFAULTTAGS].setEnabled(true);
		 * items[REMOVETAG].setEnabled(true); items[UNMANAGE].setEnabled(true);
		 * } } } if (m_rvm.IsTagView()) { items[MANAGE].setEnabled(false); } }
		 * public void menuHidden(MenuEvent e) { } });
		 */
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
	public void updateColList()
	{
		//get previous selection, if any
		int isel=m_colList.getSelectionIndex();		
		String sel="";
		if(isel>=0)
		{
			sel=m_colList.getSelection()[isel];
		}
		m_colList.removeAll();

		if (!m_rvm.IsTagView())
			return;
		VirtualFolder vf = new VirtualFolder(null, m_rvm, null, null);
		Collection<CElement> cce = vf.GetChildren(new TypeFilter(), null);
		LinkedList<String> scols = new LinkedList<String>();
		for (CElement ce : cce)
		{
			if (ce instanceof VirtualFolder)
			{
				scols.add(ce.GetLabel());
			}
		}
		Collections.sort(scols);
		for (String s : scols)
			m_colList.add(s);
		
		if(sel.length()>0)
		{
			int ind=0;		
			for(String s:scols)
			{
				if (sel.compareTo(s)==0)
					m_colList.select(ind);
				ind++;
			}
		}
	}
	public void UpdateTree(boolean bForceUpdate)
	{
		String[] sel = m_colList.getSelection();
		if (sel.length < 1 || !m_rvm.IsTagView())
		{
			m_ttm.getTree().removeAll();
			return;
		}
		if(sel[0].compareTo(m_prevColSel)==0 && !bForceUpdate) return;
		m_prevColSel=sel[0];

		Context c = new Context(new ItemTag("Project", sel[0].substring(8)));
		CElement ce = new VirtualFolder(c, m_rvm, null, null);
		m_ttm.getViewer().setInput(ce);

		/*
		 * CElement ce = (CElement) m_ttm.getViewer().getInput(); if
		 * (m_rvm.IsTagView()) { if (ce == null || !(ce instanceof
		 * VirtualFolder)) { ce = new VirtualFolder(new Context(), m_rvm, null,
		 * null); m_ttm.getViewer().setInput(ce); } else ce.Invalidate(); } else
		 * { if (ce == null || ce instanceof VirtualFolder || ce instanceof
		 * FSRoot) { ce = new FSRoot(m_rvm, null); m_ttm.getTree().removeAll();
		 * m_ttm.getViewer().setInput(ce); } else { ce.Invalidate(); } }
		 */
		try
		{
			m_ttm.getViewer().refresh();
		} catch (Exception e)
		{
			System.out.println("Please refresh tree manually.");
		}
	}
	public void dispose()
	{
		m_rvm.SessionEnd();
		Platform.getAdapterManager().unregisterAdapters(m_AdapterFactory);
		super.dispose();
	}

	public void setFocus()
	{
		if (!m_rvm.IsTagView())
			m_rvm.ToggleTagView();
		updateColList();
		UpdateTree(false);
	}
	@Override
	protected Collection<CElement> GetSelectedTableElements()
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	protected void TableDoubleClick(DoubleClickEvent e, IProgressMonitor ipm)
	{
		// TODO Auto-generated method stub
		
	}
}