package org.nrg.xnd.app;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.tools.StoreXARManager;
import org.nrg.xnd.ui.dialogs.ManageTagsDialog;
import org.nrg.xnd.ui.dialogs.UploadToXNATDialog;
import org.nrg.xnd.ui.wizards.ImportWizard;
import org.nrg.xnd.ui.wizards.ImportWizardInfoDialog;
import org.nrg.xnd.ui.wizards.QRWizard;
import org.nrg.xnd.utils.Utils;

public class AppActions extends ActionBarAdvisor
{
	/*
	 * private static IWorkbenchAction m_exitAction; private static
	 * IWorkbenchAction m_preferenceAction; private static IWorkbenchAction
	 * m_aboutAction; private static IWorkbenchAction m_manageTagsAction;
	 * private static IWorkbenchAction m_addManagedDirAction; private static
	 * IWorkbenchAction m_selectTagView; private static IWorkbenchAction
	 * m_showLocalView; private static IWorkbenchAction m_showRemoteView;
	 * private static IWorkbenchAction m_showConsoleView; private static
	 * IWorkbenchAction m_closeActiveView; private static IWorkbenchAction
	 * m_connectAction; private static IWorkbenchAction m_refreshView; private
	 * static IWorkbenchAction m_exportToXNATAction; private static
	 * IWorkbenchAction m_importFromXNATAction; private static IWorkbenchAction
	 * m_imageViewer; private static IWorkbenchAction m_viewFilterAction;
	 * private static IWorkbenchAction m_onlineUserManualAction;
	 */
	private static TreeMap<String, IWorkbenchAction> m_actions = new TreeMap<String, IWorkbenchAction>();
	public static final String ID_MANAGE_TAGS = "org.nrg.xnat.desktop.ManageTagsAction",
			ID_ADD_MANAGED_DIR = "org.nrg.xnat.desktop.AddManagedDir",
			ID_SELECT_TAG_VIEW = "org.nrg.xnat.desktop.SelectFolderView",
			ID_SHOW_LOCAL_VIEW = "org.nrg.xnat.desktop.ShowLocalView",
			ID_SHOW_REMOTE_VIEW = "org.nrg.xnat.desktop.ShowRemoteView",
			ID_SHOW_CONSOLE_VIEW = "org.nrg.xnat.desktop.ShowConsoleView",
			ID_REFRESH_VIEW = "org.nrg.xnat.desktop.RefreshView",
			ID_EXPORT_TO_XNAT = "org.nrg.xnat.desktop.UploadToXNAT",
			ID_IMPORT_FROM_XNAT = "org.nrg.xnat.desktop.DownloadFromXNAT",
			ID_CONNECT_TO_REMOTE = "org.nrg.xnat.desktop.ConnectRemote",
			ID_CLOSE_ACTIVE_VIEW = "org.nrg.xnat.desktop.CloseActiveView",
			ID_IMAGE_VIEWER = "org.nrg.xnat.desktop.ImageViewer",
			ID_FILTER = "org.nrg.xnat.destktop.DataFilter",
			ID_ONLINE_MANUAL = "org.nrg.xnat.destktop.OnlineManual",
			ID_SHOW_XNAT_VIEW = "org.nrg.xnat.desktop.ShowXNATView",
			ID_DATA_IMPORT_WIZARD = "org.nrg.xnat.desktop.DataImportWizard",
			ID_DICOM_QR_WIZARD = "org.nrg.xnat.desktop.QRWizard",
			ID_CLEAR_DB="org.nrg.xnd.ClearDB";

	public AppActions(IActionBarConfigurer configurer)
	{
		super(configurer);
	}

	public static void EnableExportAction(boolean bEnable)
	{
		m_actions.get(ID_EXPORT_TO_XNAT).setEnabled(bEnable);
	}
	private void RegisterAction(IWorkbenchAction iwa)
	{
		RegisterAction(iwa, true);
	}
	private void RegisterAction(IWorkbenchAction iwa, boolean bEnable)
	{
		register(iwa);
		iwa.setEnabled(bEnable);
		m_actions.put(iwa.getId(), iwa);
	}

	@Override
	protected void makeActions(IWorkbenchWindow window)
	{
		// File exit
		RegisterAction(ActionFactory.QUIT.create(window));
		// About
		RegisterAction(ActionFactory.ABOUT.create(window));
		// preferences
		RegisterAction(ActionFactory.PREFERENCES.create(window));
		// perspective stuff
		RegisterAction(ActionFactory.OPEN_PERSPECTIVE_DIALOG.create(window));

		// Manage tags
		class TagsAction extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = ID_MANAGE_TAGS;
			private IWorkbenchWindow m_wnd;
			public TagsAction(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&Manage tags");
				setToolTipText("Manage tags");
				m_wnd = wnd;
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.LABEL));
			}
			@Override
			public void run()
			{
				// FileView fv=GetActiveFileView();
				Collection<FileView> views = GetFileViewList();
				// if(fv==null) return;
				ManageTagsDialog d = new ManageTagsDialog(new Shell());
				d.open();
				if (d.IsViewConfigChanged())
				{
					for (FileView fv : views)
					{
						try
						{
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getActivePage()
									.hideView(fv);
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getActivePage()
									.showView(FileView.m_ID,
											fv.getViewSite().getSecondaryId(),
											IWorkbenchPage.VIEW_ACTIVATE);
						} catch (Exception e)
						{
						}
					}
				}
			}
			public void dispose()
			{
			}
		};
		RegisterAction(new TagsAction(window));

		// Run data import wizard
		class ImportAction extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_DATA_IMPORT_WIZARD;
			IWorkbenchWindow m_wnd;
			public ImportAction(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&Data import wizard");
				setToolTipText("A wizard for importing folder data");
				m_wnd = wnd;
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.WIZARD));
			}
			@Override
			public void run()
			{
				FileView fv = GetActiveFileView();
				if (fv == null || fv.GetSelectedElements().size() < 1)
				{
					ImportWizardInfoDialog iwid = new ImportWizardInfoDialog(
							m_wnd.getShell());
					iwid.open();
					return;
				}

				WizardDialog dlg = new WizardDialog(m_wnd.getShell(),
						new ImportWizard());
				try
				{
					dlg.create();
					if (dlg.open() == Window.OK)
					{
						fv.Refresh(false);
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new ImportAction(window));

		// DICOM QR wizard
		class DICOMQRAction extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_DICOM_QR_WIZARD;
			IWorkbenchWindow m_wnd;
			public DICOMQRAction(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&Import from DICOM AE");
				setToolTipText("Import data from PACS using DICOM Query/Retrieve");
				m_wnd = wnd;
				// setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.WIZARD));
			}
			@Override
			public void run()
			{

				FileView fv = GetActiveFileView();
				/*
				 * if (fv == null || fv.GetSelectedElements().size() < 1) {
				 * ImportWizardInfoDialog iwid = new ImportWizardInfoDialog(
				 * m_wnd.getShell()); iwid.open(); return; }
				 */
				WizardDialog dlg = new WizardDialog(m_wnd.getShell(),
						new QRWizard());
				try
				{
					dlg.create();
					if (dlg.open() == Window.OK)
					{
						if (fv != null)
							fv.Refresh(false);
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new DICOMQRAction(window));

		// Add managed folder
		class AddManagedDir extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_ADD_MANAGED_DIR;
			IWorkbenchWindow m_wnd;
			public AddManagedDir(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&Add root dir");
				setToolTipText("Add root directory for managed files");
				m_wnd = wnd;
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.PLUS));
			}
			@Override
			public void run()
			{
				FileView fv = GetLocalFileView();
				if (fv == null)
					return;
				String path = Utils.SelectFolder("Browse for a directory",
						"Select a managed directory root");
				if (path != null)
				{
					if (!XNDApp.app_localVM.CanAddManagedRoot(path))
					{
						Utils
								.ShowMessageBox(
										"Error",
										"Either this directory or some of its subdirectories is already managed,\n"
												+ "a directory with the same name is already managed, or this directory\n"
												+ "contains an upload directory. Please select another directory.",
										Window.OK);
						return;
					}
					XNDApp.app_localVM.AddManagedFolder(path);
					fv.UpdateTree();
				}
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new AddManagedDir(window));

		// Clear DB
		class ClearDB extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_CLEAR_DB;
			IWorkbenchWindow m_wnd;
			public ClearDB(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&Clear all");
				setToolTipText("Clear all repository records and tags");
				m_wnd = wnd;
//				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.PLUS));
			}
			@Override
			public void run()
			{
				String[] fld=XNDApp.app_localVM.GetManagedFolders();
				for(int i=0; i<fld.length; i++)
				{
					FSFolder fold=new FSFolder(new File(fld[i]),XNDApp.app_localVM,null);
					fold.ApplyOperation(null, CElement.UNMANAGEALL, null);
					XNDApp.app_localVM.RemoveManagedFolder(fld[i]);
//					fold.ApplyOperation(null, CElement.REMOVE_FROM_ROOTS, null);
				}				
				GetLocalFileView().Refresh(false);
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new ClearDB(window));

		
		
		// Switch to folder view
		class SelectTagView extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_SELECT_TAG_VIEW;
			private IWorkbenchWindow m_wnd;
			public SelectTagView(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&Folder/Tag view");
				setToolTipText("Switch between folder and tag view");
				m_wnd = wnd;
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.TAGVIEW));

			}
			@Override
			public void run()
			{
				FileView fv = GetActiveFileView();
				if (fv == null || !fv.IsLocal())
					return;
				fv.GetRepositoryViewManager().ToggleTagView();
				fv.Refresh(false);
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new SelectTagView(window));

		class CloseActiveView extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_CLOSE_ACTIVE_VIEW;
			public CloseActiveView(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("Hide active view");
				setToolTipText("Hide active view");
			}
			@Override
			public void run()
			{
				try
				{
					IWorkbenchPart iwp = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.getActivePart();
					if (iwp instanceof IViewPart)
					{
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().hideView((IViewPart) iwp);
					}
				} catch (Exception e)
				{
				}
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new CloseActiveView(window));

		// Show remote view
		class ShowRemoteView extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_SHOW_REMOTE_VIEW;
			public ShowRemoteView(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("Show remote view");
				setToolTipText("Show remote view");
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.REMOTE));
			}
			@Override
			public void run()
			{
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().findViewReference(FileView.m_ID,
								"remote") == null)
				{
					if (PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().findViewReference(FileView.m_ID,
									"remote") == null)
					{
						try
						{
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getActivePage()
									.showView(FileView.m_ID, "remote",
											IWorkbenchPage.VIEW_ACTIVATE);
						} catch (Exception e)
						{
						}
					}
				}
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new ShowRemoteView(window));

		// Show local view
		class ShowLocalView extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_SHOW_LOCAL_VIEW;
			public ShowLocalView(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("Show local view");
				setToolTipText("Show local view");
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.LOCAL));
			}
			@Override
			public void run()
			{
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().findViewReference(FileView.m_ID,
								"local") == null)
				{
					try
					{
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(FileView.m_ID,
										"local", IWorkbenchPage.VIEW_ACTIVATE);
					} catch (Exception e)
					{
					}
				}
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new ShowLocalView(window));

		// connect to remote
		class ConnectRemote extends Action implements IWorkbenchAction
		{
			public final static String ID = AppActions.ID_CONNECT_TO_REMOTE;
			public ConnectRemote()
			{
				setId(ID);
				setText("Connect to remote");
				setToolTipText("Connect to remote repository");
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.CONNECT));
			}
			public void run()
			{
				FileView fv = GetActiveFileView();
				if (fv == null || fv.IsLocal())
					return;
				if (fv.Connect())
					fv.Refresh(false);
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new ConnectRemote());

		// show XNAT view
		class ShowXNATView extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_SHOW_XNAT_VIEW;
			public ShowXNATView(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("Show XNAT view");
				setToolTipText("Browse remote XNAT archive");
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.XNAT16));
			}
			public void run()
			{
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().findViewReference(FileView.m_ID,
								"XNAT") == null)
				{
					if (PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().findViewReference(FileView.m_ID,
									"XNAT") == null)
					{
						try
						{
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getActivePage()
									.showView(FileView.m_ID, "XNAT",
											IWorkbenchPage.VIEW_ACTIVATE);
						} catch (Exception e)
						{
						}
					}
				}
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new ShowXNATView(window));

		// Show console view
		class ShowConsoleView extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_SHOW_CONSOLE_VIEW;
			public ShowConsoleView(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("Show console view");
				setToolTipText("Show console view");
				// setImageDescriptor(
				// AbstractUIPlugin.imageDescriptorFromPlugin("org.nrg.xnat.desktop",
				// IImageKeys.LOCAL));
			}
			public void run()
			{
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().findViewReference(ConsoleView.m_ID) == null)
				{
					try
					{
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage()
								.showView(ConsoleView.m_ID, "console",
										IWorkbenchPage.VIEW_ACTIVATE);
					} catch (Exception e)
					{
					}
				}
			}
			public void dispose()
			{
			}
		}
		RegisterAction(new ShowConsoleView(window));

		// refresh view
		class RefreshView extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_REFRESH_VIEW;
			public RefreshView(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("Refresh");
				setToolTipText("Refresh view");
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.REFRESH));
			}
			public void run()
			{
				IWorkbenchPart part = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.getActivePart();
				if (part instanceof FileView)
				{
					((FileView) (part)).Refresh(false);
				} else if (part instanceof ConsoleView)
				{
					((ConsoleView) (part)).Refresh();
				} else if (part instanceof PACSView)
				{
					((PACSView) (part)).Refresh(false);
				}

			}
			public void dispose()
			{
			}
		}
		RegisterAction(new RefreshView(window));

		// upload to XNAT
		class UploadAction extends Action
				implements
					ActionFactory.IWorkbenchAction,
					ISelectionListener
		{
			public final static String ID = AppActions.ID_EXPORT_TO_XNAT;
			private IWorkbenchWindow m_wnd;
			public UploadAction(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&StoreXAR upload (legacy)");
				setToolTipText("Upload experiment to XNAT using old xml-based StoreXAR interface");
				m_wnd = wnd;
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.UPLOAD));
				m_wnd.getSelectionService().addSelectionListener(this);
			}
			public void selectionChanged(IWorkbenchPart part, ISelection sel)
			{
				boolean bEnable = false;
				if (part instanceof FileView)
				{
					FileView fv = (FileView) part;
					if (fv.IsLocal())
					{
						bEnable = fv.CanUploadToXNAT();
					}
				}
				setEnabled(bEnable);
			}
			public void run()
			{
				if (StoreXARManager.IsRunning())
				{
					Utils.ShowMessageBox("Unable to proceed",
							"Upload session is already running!", Window.OK);
					return;
				}
				FileView fv = GetActiveFileView();
				if (fv == null || !fv.IsLocal())
				{
					Utils
							.ShowMessageBox(
									"Invalid command",
									"Please activate local view and "
											+ "select project, subject or experiment to upload",
									Window.OK);
					return;
				}
				XNDApp.StartWaitCursor();
				UploadToXNATDialog d = null;
				try
				{
					d = new UploadToXNATDialog(new Shell(), fv
							.GetSelectedElements(), XNDApp.app_localVM);
				} finally
				{
					XNDApp.EndWaitCursor();
				}
				d.open();
			}
			public void dispose()
			{
				m_wnd.getSelectionService().removeSelectionListener(this);
			}
		};
		RegisterAction(new UploadAction(window), false);

		// download from XNAT
		class DownloadAction extends Action
				implements
					ActionFactory.IWorkbenchAction,
					ISelectionListener
		{
			public final static String ID = AppActions.ID_IMPORT_FROM_XNAT;
			private IWorkbenchWindow m_wnd;
			public DownloadAction(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&Download experiment from XNAT 1.4 archive");
				setToolTipText("Download from XNAT 1.4");
				m_wnd = wnd;
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.DOWNLOAD));
				m_wnd.getSelectionService().addSelectionListener(this);
			}
			public void selectionChanged(IWorkbenchPart part, ISelection sel)
			{
				/*
				 * boolean bEnable=false; if(part instanceof FileView) {
				 * FileView fv=(FileView)part; if(fv.IsLocal()) {
				 * if(((IStructuredSelection)sel).getFirstElement() instanceof
				 * TreeItem) { FolderItem
				 * fi=(FolderItem)(((IStructuredSelection)
				 * sel).getFirstElement());
				 * if(fi.GetName().startsWith("Experiment")) bEnable=true; } } }
				 * setEnabled(bEnable);
				 */
			}
			public void run()
			{
				FileView fv;
				if (m_wnd.getActivePage().getActivePart() instanceof FileView)
				{
					fv = (FileView) (m_wnd.getActivePage().getActivePart());
					if (!fv.IsLocal())
						return;
					if (fv.GetRepositoryViewManager().IsTagView())
						return;
				} else
					return;
				// ?? if(fv.GetSelectedFolder()==null) return;
				// ?? DownloadFromXNATDialog d =
				// ?? new DownloadFromXNATDialog(new
				// Shell(),fv.GetSelectedFolder());
				// ?? d.open();
			}
			public void dispose()
			{
				m_wnd.getSelectionService().removeSelectionListener(this);
			}
		};
		RegisterAction(new DownloadAction(window), false);

		// Image view action
		class ImageViewAction extends Action
				implements
					ActionFactory.IWorkbenchAction,
					ISelectionListener
		{
			public final static String ID = AppActions.ID_IMAGE_VIEWER;
			private IWorkbenchWindow m_wnd;
			public ImageViewAction(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&View images");
				setToolTipText("Radiological image viewer");
				m_wnd = wnd;
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.IMVIEWER));
				m_wnd.getSelectionService().addSelectionListener(this);
			}
			public void selectionChanged(IWorkbenchPart part, ISelection sel)
			{
				boolean bEnable = false;
				if (part instanceof FileView)
				{
					FileView fv = (FileView) part;
					if (fv.IsLocal())
					{
						bEnable = true;
					}
				}
				setEnabled(bEnable);
			}
			public void run()
			{
				IViewPart ivp = GetActiveViewPart();
				if(ivp instanceof PACSView)
				{
					((PACSView)ivp).ViewImages();
				}
				else if (ivp instanceof FileView)
				{
					FileView fv=(FileView) ivp;
					if (fv.IsLocal())
					{
						fv.ViewImages();
					}
				}
				else
				{
					Utils.ShowMessageBox("Invalid command",
							"Please activate local view and "
									+ "select images to view", Window.OK);
					return;					
				}
				
			}
			public void dispose()
			{
				m_wnd.getSelectionService().removeSelectionListener(this);
			}
		};
		RegisterAction(new ImageViewAction(window), true);
		// end of image viewer action

		// View filter action
		class FilterAction extends Action
				implements
					ActionFactory.IWorkbenchAction,
					ISelectionListener
		{
			public final static String ID = AppActions.ID_FILTER;
			private IWorkbenchWindow m_wnd;
			public FilterAction(IWorkbenchWindow wnd)
			{
				super("&Data filter", IAction.AS_CHECK_BOX);
				setId(ID);
				// setText("&Visibility filter");
				setToolTipText("Data filter");
				// if(XNDApp.app_vf!=null)
				// setChecked(XNDApp.app_vf.IsEnabled());
				m_wnd = wnd;
				setImageDescriptor(IImageKeys.GetImDescr(IImageKeys.FILTER));
				m_wnd.getSelectionService().addSelectionListener(this);
			}
			public void selectionChanged(IWorkbenchPart part, ISelection sel)
			{
				/*
				 * boolean bEnable=false; if(part instanceof FileView) {
				 * FileView fv=(FileView)part; if(fv.IsLocal()) { bEnable=true;
				 * } } setEnabled(bEnable);
				 */
			}
			public void run()
			{
				FileView fv = GetActiveFileView();
				if (fv == null || !fv.IsLocal())
				{
					Utils.ShowMessageBox("Invalid command",
							"Please activate local view and "
									+ "select images to view", Window.OK);
					return;
				}
				// setChecked(!isChecked());
				// XNDApp.app_vf.SetEnabled(isChecked());
				fv.Refresh(true);
			}
			public void dispose()
			{
				m_wnd.getSelectionService().removeSelectionListener(this);
			}
		};
		RegisterAction(new FilterAction(window));
		// end of view filter action

		// online user manual
		class OnlineManual extends Action
				implements
					ActionFactory.IWorkbenchAction
		{
			public final static String ID = AppActions.ID_ONLINE_MANUAL;
			private IWorkbenchWindow m_wnd;
			public OnlineManual(IWorkbenchWindow wnd)
			{
				setId(ID);
				setText("&Online user manual");
				m_wnd = wnd;
			}
			public void run()
			{
				try
				{
					// new
					// BrowserLauncher().openURLinBrowser("http://nrg.wustl.edu/xnd");
				} catch (Exception e)
				{
					Utils
							.ShowMessageBox(
									"Error",
									"Cannot display online help becaulse default browser could not be detected.",
									Window.OK);
				}
			}
			public void dispose()
			{
			}
		};
		RegisterAction(new OnlineManual(window), true);
		// end of online user manual action
	}
	public IWorkbenchAction Action(String id)
	{
		return m_actions.get(id);
	}
	protected void fillMenuBar(IMenuManager menuBar)
	{
		MenuManager file_mgr = new MenuManager("&Repository", "Repository");
		file_mgr.add(Action(ID_DATA_IMPORT_WIZARD));
		file_mgr.add(Action(ID_ADD_MANAGED_DIR));
		file_mgr.add(Action(ID_MANAGE_TAGS));
		file_mgr.add(new Separator());
		file_mgr.add(Action(ID_CLEAR_DB));
		file_mgr.add(new Separator());
		file_mgr.add(Action(ActionFactory.QUIT.getId()));

		MenuManager view_mgr = new MenuManager("&View", "View");
		// view_mgr.add(Action(ID_SELECT_TAG_VIEW));
		// view_mgr.add(new Separator());
		view_mgr.add(Action(ID_REFRESH_VIEW));
		view_mgr.add(Action(ID_FILTER));
		view_mgr.add(Action(ID_SHOW_LOCAL_VIEW));
		view_mgr.add(Action(ID_SHOW_REMOTE_VIEW));
		view_mgr.add(Action(ID_SHOW_XNAT_VIEW));
		view_mgr.add(Action(ID_SHOW_CONSOLE_VIEW));
		view_mgr.add(new Separator());
		view_mgr.add(Action(ID_CLOSE_ACTIVE_VIEW));
		view_mgr.add(new Separator());
		view_mgr.add(Action(ActionFactory.OPEN_PERSPECTIVE_DIALOG.getId()));
		// view_mgr.add(Action("org.eclipse.ui.perspectives.showPerspective"));
		view_mgr.add(Action(ActionFactory.PREFERENCES.getId()));

		MenuManager tools_mgr = new MenuManager("&Tools", "Tools");
		tools_mgr.add(Action(ID_EXPORT_TO_XNAT));
		tools_mgr.add(Action(ID_DICOM_QR_WIZARD));
		// tools_mgr.add(Action(ID_IMAGE_VIEWER));
		// tools_mgr.add(m_importFromXNATAction);

		MenuManager help_mgr = new MenuManager("&Help", "Help");
		// help_mgr.add(m_onlineUserManualAction);
		help_mgr.add(Action(ActionFactory.ABOUT.getId()));
		menuBar.add(file_mgr);
		menuBar.add(view_mgr);
		menuBar.add(tools_mgr);
		menuBar.add(help_mgr);
	}
	protected void fillCoolBar(ICoolBarManager mgr)
	{
		IToolBarManager toolbar = new ToolBarManager(mgr.getStyle());
		mgr.add(toolbar);
		toolbar.add(Action(ID_ADD_MANAGED_DIR));
		toolbar.add(Action(ID_DATA_IMPORT_WIZARD));
		toolbar.add(Action(ID_MANAGE_TAGS));
		// toolbar.add(Action(ID_SELECT_TAG_VIEW));
		toolbar.add(Action(ID_REFRESH_VIEW));
		toolbar.add(Action(ID_FILTER));
		toolbar.add(new Separator());
		// toolbar.add(m_connectAction);
		toolbar.add(Action(ID_SHOW_LOCAL_VIEW));
		// toolbar.add(Action(ID_SHOW_REMOTE_VIEW));
		toolbar.add(new Separator());
		toolbar.add(Action(ID_IMAGE_VIEWER));
		// toolbar.add(Action(ID_EXPORT_TO_XNAT));
		// toolbar.add(m_importFromXNATAction);
	}
	public static IWorkbenchAction GetAction(String id)
	{
		return m_actions.get(id);
	}
	public static FileView GetLocalFileView()
	{
		IViewReference[] views = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getViewReferences();
		IViewPart ivp;
		LinkedList<FileView> llfv = new LinkedList<FileView>();
		for (IViewReference ivr : views)
		{
			ivp = ivr.getView(false);
			if (ivp instanceof FileView && ((FileView) ivp).IsLocal())
				return (FileView) ivp;
		}
		return null;
	}

	public static Collection<FileView> GetFileViewList()
	{
		IViewReference[] views = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getViewReferences();
		IViewPart ivp;
		LinkedList<FileView> llfv = new LinkedList<FileView>();
		for (IViewReference ivr : views)
		{
			ivp = ivr.getView(false);
			if (ivp instanceof FileView)
				llfv.add((FileView) ivp);
		}
		return llfv;
	}
	public static PACSView GetActivePACSView()
	{
		try
		{
			IWorkbenchPart ivp;
			if ((ivp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().getActivePart()) instanceof PACSView)
				return (PACSView) ivp;
			else
				return null;
		} catch (Exception e)
		{
			return null;
		}
	}

	public static IViewPart GetActiveViewPart()
	{
		try
		{
			IWorkbenchPart ivp;
			if ((ivp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().getActivePart()) instanceof IViewPart)
				return (IViewPart) ivp;
			else
				return null;
		} catch (Exception e)
		{
			return null;
		}
	}
	public static FileView GetActiveFileView()
	{
		try
		{
			IWorkbenchPart ivp;
			if ((ivp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().getActivePart()) instanceof FileView)
				return (FileView) ivp;
			else
				return null;
		} catch (Exception e)
		{
			return null;
		}
	}
}