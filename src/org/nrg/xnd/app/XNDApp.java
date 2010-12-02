package org.nrg.xnd.app;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.varia.NullAppender;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.nrg.fileserver.LocalRepositoryManager;
import org.nrg.xnat.engine.Engine;
import org.nrg.xnd.filetransfer.RemoteRepositoryClient;
import org.nrg.xnd.filetransfer.SocketServer;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.SimpleRepositoryManager;
import org.nrg.xnd.model.ViewFilter;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.ontology.XNATThesaurus;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.rules.RuleManager;
import org.nrg.xnd.tools.ClipboardManager;
import org.nrg.xnd.ui.ImageViewerFrame;
import org.nrg.xnd.utils.Utils;
import org.nrg.xnd.utils.dicom.AEList;

/**
 * This class controls all aspects of the application's execution
 */
public class XNDApp implements IApplication
{
	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.
	 * IApplicationContext)
	 */
	public static final String PLUGIN_ID = "org.nrg.xnat.desktop";
	public static RepositoryViewManager app_localVM = null;
	public static RepositoryViewManager app_remoteVM = null;
	public static RepositoryViewManager app_XNATVM = null;
	public static AEList app_aeList = new AEList();
	public static SocketServer app_rs = null;
	public static SocketServer app_fs = null;
	public static IEclipsePreferences app_Prefs = new ConfigurationScope()
			.getNode(XNDApp.PLUGIN_ID);
	private static Cursor m_WaitCursor = null;
	private static Cursor m_DefaultCursor;
	public static final byte PLATFORM_WIN32 = 1, PLATFORM_LINUX = 2,
			PLATFORM_MAC = 3, PLATFORM_UNKNOWN = 0;
	public static byte app_Platform = 0;
	private static Engine m_engine;
	private static boolean m_bWebServices = true;
	public final static byte SERVER_REPOSITORY = 0, SERVER_FILE_TRANSFER = 1;
	private static ImageViewerFrame m_ivf = null;
	public static int app_maxRecords = -1;
	public static IStatusLineManager app_Status = null;
	public static TreeMap<String, ViewFilter> app_filters = new TreeMap<String, ViewFilter>();
	public static TreeMap<String, Rule> app_Rules = new TreeMap<String, Rule>();
	public static XNDApp theApp;
	public static ClipboardManager app_ClipboardManager = new ClipboardManager();

	public static void SetStatus(String msg)
	{
		if (app_Status != null)
			app_Status.setMessage(msg);
	}
	public static void StartWaitCursor()
	{
		// if(control==null || m_WaitCursor==null) return;
		if (m_WaitCursor == null)
			return;
		Shell sh = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		if (sh != null)
			sh.setCursor(m_WaitCursor);

		// if(control.getCursor()!=null &&
		// control.getCursor().equals(m_WaitCursor)) return;
		// control.setCursor(m_WaitCursor);
	}

	public static boolean IsWebService()
	{
		return m_bWebServices;
	}
	public static void EndWaitCursor()
	{
		if (m_DefaultCursor == null)
			return;
		Shell sh = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		if (sh != null)
			sh.setCursor(m_DefaultCursor);

		// if(control!=null && m_DefaultCursor!=null)
		// control.setCursor(m_DefaultCursor);
	}
	public static ImageViewerFrame GetViewerFrame()
	{
		if (m_ivf == null)
			m_ivf = new ImageViewerFrame();
		m_ivf.setVisible(true);
		return m_ivf;
	}
	public static String[] GetArchiveList(RepositoryViewManager rvm)
	{
		if (rvm.IsLocal())
		{
			String[] s = {app_Prefs.get("RemoteAddress",
					Utils.REMOTE_ADDRESS_DEFAULT)};
			return s;
		} else
		{
			String[] s = {"local"};
			return s;
		}
	}
	public static boolean ControlServer(byte code, boolean bStart)
	{
		if (m_bWebServices)
		{
			if (code == SERVER_REPOSITORY)
			{
				if (bStart)
				{
					m_engine.startWebServices(app_Prefs.getInt("ServerPort",
							Utils.PORT_REPOSITORY_DEFAULT), false);
					return true;
				} else
				{
					try
					{
						m_engine.stopWebServices();
					} catch (Exception e)
					{
					}
					return true;
				}
			} else if (code == SERVER_FILE_TRANSFER)
			{
				return false;
			}
		} else
		{
			if (code == SERVER_REPOSITORY)
			{
				if (bStart)
					return app_rs.Start();
				else
				{
					app_rs.Stop();
					return true;
				}
			} else if (code == SERVER_FILE_TRANSFER)
			{
				if (bStart)
					return app_fs.Start();
				else
				{
					app_fs.Stop();
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * Test code to call before database initialization
	 * 
	 * @return
	 */
	public boolean TestFunction1()
	{
		System.out.println("test");
		/*
		 * StringTree st=new StringTree(); String[]
		 * cl={"abc","ab","a","ac","bec","ac","cbe"}; st.Add(cl); boolean
		 * br=st.Contains("ac"); br=st.Contains("cbe"); br=st.Contains("abc");
		 * br=st.Contains("aa"); st.Add("abc"); st.Add("abcde");
		 * 
		 * System.out.println(st.PrintValues()); st.Remove("abaaa");
		 * st.Remove("xxxx");
		 * st.Remove("abc");st.Remove("ab");st.Remove("a");st.Remove("ac");
		 * st.Remove("bec");st.Remove("ac");st.Remove("cbe"); st.Remove("aa");
		 * st.Remove("abcde"); st.Remove("yyyy");
		 * System.out.println(st.PrintValues());
		 */
		return true;
	}
	/**
	 * Test code to call after database initialization)
	 * 
	 * @return
	 */
	public boolean TestFunction2()
	{
		return true;
	}
	public Object start(IApplicationContext context) throws Exception
	{
		if (!TestFunction1())
			return IApplication.EXIT_OK;
		IImageKeys.Init();
		String userPath = Utils.GetUserFolder();
		File user_fold = new File(userPath);
		if (!user_fold.exists())
		{
			if (!user_fold.mkdir())
			{
				Utils.logger.debug("Cannot create user folder");
				return IApplication.EXIT_OK;
			}
		}

		// String
		// configpath=Platform.getStateLocation(Platform.getBundle("org.nrg.xnat.desktop")).toString();
		FileAppender appender = null;
		SimpleLayout layout = new SimpleLayout();
		try
		{
			appender = new FileAppender(layout, userPath + "/xnd.log", false);
		} catch (Exception e)
		{
		}
		Utils.logger.addAppender(appender);
		Utils.logger.setLevel(Level.INFO);
		Utils.logger.debug("log started");
		BasicConfigurator.configure(new NullAppender());

		String os = Platform.getOS();
		if (os.startsWith("win32"))
			app_Platform = PLATFORM_WIN32;
		else if (os.startsWith("linux"))
			app_Platform = PLATFORM_LINUX;
		else if (os.startsWith("mac"))
			app_Platform = PLATFORM_MAC;
		Utils.logger.info("OS type: " + Platform.getOS());

		Display display = PlatformUI.createDisplay();

		m_WaitCursor = new Cursor(display, SWT.CURSOR_WAIT);
		if ((m_DefaultCursor = new Shell().getCursor()) == null)
			m_DefaultCursor = new Cursor(display, SWT.CURSOR_ARROW);
		if (!m_bWebServices)
		{
			app_localVM = new RepositoryViewManager(
					new SimpleRepositoryManager());
			app_remoteVM = new RepositoryViewManager(
					new RemoteRepositoryClient());

			// file upload/download services
			app_rs = new SocketServer(app_localVM, app_Prefs.getInt(
					"ServerPort", Utils.PORT_REPOSITORY_DEFAULT),
					SocketServer.TYPE_RM);
			app_fs = new SocketServer(app_localVM, app_Prefs.getInt(
					"FileServerPort", Utils.PORT_FILE_DEFAULT),
					SocketServer.TYPE_FM); // Start file transfer client

			// Start file transfer server
			ControlServer(SERVER_FILE_TRANSFER, true);
			// end of file upload/downlad services
		} else
		{
			// String
			// basepath=Platform.getStateLocation(Platform.getBundle("org.nrg.xnat.desktop")).toString();
			File in_folder = new File(Utils.GetIncomingFolder());
			if (!in_folder.exists())
			{
				if (!in_folder.mkdir())
				{
					String user_dir = Utils.GetIncomingFolder();
					in_folder = new File(user_dir);
					Utils.ShowMessageBox("Error",
							"Could not create upload folder "
									+ in_folder.getAbsolutePath()
									+ ". Upload folder is defaulted to "
									+ user_dir, SWT.OK);
				}
			}
			boolean bStart = false;
			int nTries = 0;
			while (nTries < 20 && !bStart)
			{
				try
				{
					m_engine = Engine.getEngine(new File(userPath + "/db.dat"),
							new File(Utils.GetIncomingFolder() + "/"
									+ "uploads"));
					bStart = true;
				} catch (Exception e)
				{
					if (m_engine != null)
						m_engine.dispose();
					bStart = false;
				}
				nTries++;
				if (!bStart)
				{
					System.out.println("Engine could not be started, attempt "
							+ nTries);
					Thread.sleep(2000);
				}
			}
			if (!bStart)
				return IApplication.EXIT_OK;
			// m_engine.setLogsize(1);
			System.out.println("XNAT engine started");
			app_localVM = new RepositoryViewManager(
					new LocalRepositoryManager());
			try
			{
				// app_remoteVM=new RepositoryViewManager(new
				// RestRepositoryManager(
				// new
				// URL(app_Prefs.get("RemoteAddress",Utils.REMOTE_ADDRESS_DEFAULT))));
			} catch (Exception e)
			{
				app_remoteVM = null;
			}
		}

		// Start repository server
		if (app_Prefs.getBoolean("ServerRunning", false))
			ControlServer(SERVER_REPOSITORY, true);

		if (app_Prefs.getBoolean("LimitRecords", true))
			app_maxRecords = Utils.MAX_TABLE_RECORDS;
		else
			app_maxRecords = -1;

		if (app_remoteVM != null && !app_remoteVM.IsTagView())
			app_remoteVM.ToggleTagView();

		File rf = new File(RuleManager.getRuleFolder());
		if (!rf.exists())
			rf.mkdir();

		// load xml descriptors
		// System.err.println(app_Prefs.get("XMLDefaultOntology",
		// DefaultOntologyManager.getDefaultLocation()));
		if (!DefaultOntologyManager.loadOntology(app_Prefs.get(
				"XMLDefaultOntology", DefaultOntologyManager
						.getDefaultLocation())))
		{
			Utils
					.ShowMessageBox(
							"",
							"Error processing ontology XML. Will attempt to load default ontology.",
							SWT.OK);
			if(!DefaultOntologyManager.loadDefaultOntology())
				Utils.ShowMessageBox("", "Could not load default ontology", SWT.OK);
			app_Prefs.put("XMLDefaultOntology", DefaultOntologyManager.getDefaultLocation());
		}

		if (!XNATThesaurus.Load(new File(XNATThesaurus.getDefaultLocation())))
		{
			Utils
					.ShowMessageBox(
							"",
							"Error loading XNAT tag description XML. Upload to XNAT will not be available.",
							SWT.OK);
		}

		/*
		 * if(!LoadDefaultRule(Rule.RULE_DICOM)) Utils.ShowMessageBox("",
		 * "Error loading default DICOM rule.", SWT.OK);
		 * if(!LoadDefaultRule(Rule.RULE_NAMING)) Utils.ShowMessageBox("",
		 * "Error processing naming rule XML. Naming rule will not be available."
		 * , SWT.OK);
		 */
		theApp = this;
		SerializeApp(true);
		if (!TestFunction2())
			return IApplication.EXIT_OK;

		try
		{
			int returnCode = PlatformUI.createAndRunWorkbench(display,
					new AppWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART)
				return IApplication.EXIT_RESTART;
			else
			{
				SerializeApp(false);
				return IApplication.EXIT_OK;
			}
		} finally
		{
			display.dispose();
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop()
	{
		ControlServer(SERVER_REPOSITORY, false);
		ControlServer(SERVER_FILE_TRANSFER, false);
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable()
		{
			public void run()
			{
				if (!display.isDisposed())
					workbench.close();
			}
		});
		SerializeApp(false);
		try
		{
			m_engine.stopWebServices();
		} catch (Exception e)
		{
		}
	}
	public void SerializeApp(boolean is_loading)
	{
		String basepath = Utils.GetUserFolder();
		// Platform.getStateLocation(Platform.getBundle("org.nrg.xnat.desktop")).toString();
		try
		{
			File f = new File(basepath + "/filters.xml");
			if (is_loading)
			{
				// load rules
				RuleManager.LoadRules();

				if (!f.exists())
				{
					f = new File(Utils.GetPluginPath()
							+ "xml_resources/filters.xml");
				}

				app_filters.clear();
				Document d = new SAXReader().read(f);
				Element el;
				ViewFilter vf;
				for (Iterator<Element> it = d.getRootElement()
						.elementIterator(); it.hasNext();)
				{
					el = it.next();
					if (el.getName().compareTo("Filter") == 0)
					{
						vf = new ViewFilter();
						vf.Serialize(el, true);
						app_filters.put(el.attributeValue("ID"), vf);
					}
				}
			} else
			{
				RuleManager.saveRules();
				app_aeList.Save();
				Document d = DocumentHelper.createDocument();
				Element del = d.addElement("Filters");
				Element el;
				for (String key : app_filters.keySet())
				{
					el = del.addElement("Filter");
					el.addAttribute("ID", key);
					app_filters.get(key).Serialize(el, false);
				}
				XMLWriter xw = new XMLWriter(new FileWriter(f), OutputFormat
						.createPrettyPrint());
				xw.write(d);
				xw.close();
			}
		} catch (Exception e)
		{
			Utils.logger.error("Filter serialization failed: exception "
					+ e.getMessage());
		}

		if (app_localVM != null)
			app_localVM.Serialize(basepath + "/lvm.dat", is_loading);
		if (app_remoteVM != null)
			app_remoteVM.Serialize(basepath + "/rvm.dat", is_loading);
		try
		{
			if (!is_loading)
				app_Prefs.flush();
		} catch (Exception e)
		{
		}
	}
}