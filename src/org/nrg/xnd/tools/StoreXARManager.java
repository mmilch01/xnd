package org.nrg.xnd.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.XNATRestAdapter;
import org.nrg.xdat.webservices.CheckUserSession;
import org.nrg.xdat.webservices.StoreXARWS;
import org.nrg.xnd.app.ConsoleView;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.model.TreeIterator;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.model.VirtualFolder;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.utils.Utils;

public class StoreXARManager extends Job
{
	private String m_usr;
	private String m_pass;
	private String m_host;
	private ItemRecord[] m_records;
	// private TreeMap<String,Collection<DBElement>> m_ExpMap;
	private Collection<VirtualFolder> m_experiments;
	private File m_Archive;
	private File m_destDir;
	private String m_ExperimentDir = "";
	private static String m_tempFolder = "TEMP_XAR";
	private TreeMap<String, Scan> m_scans = new TreeMap<String, Scan>();
	private String m_experiment = "";
	private Collection<Resource> m_resources = new LinkedList<Resource>();
	private static boolean m_bRunning = false;
	private boolean m_bCreateSubject = true;
	private static String m_interfaceMessage = null;
	private RepositoryViewManager m_rvm;
	private TreeMap<String, File> m_Files = new TreeMap<String, File>();

	public StoreXARManager(String host, String usr, String pass,
			RepositoryViewManager rvm, Collection<VirtualFolder> experiments,
			boolean bCreateSubject)
	{
		super("Uploading experiment(s)");
		m_usr = usr;
		m_pass = pass;
		m_host = host;
		// m_ExpMap=expMap;
		m_experiments = experiments;
		m_rvm = rvm;
		m_bCreateSubject = bCreateSubject;
	}
	private String GetTag(String name)
	{
		ItemTag t;
		for (ItemRecord ir : m_records)
		{
			t = ir.getTag(name);
			if (t != null && t.GetFirstValue().length() > 0)
			{
				return t.GetFirstValue();
			}
		}
		return null;
	}
	private String GetSessionTag()
	{
		return "xnat:" + GetTag("Modality") + "Session";
	}
	private String GetURI(ItemRecord ir, String rel_path)
	{
		String uri;
		if (ir.getTag("Scan") != null)
			uri = "RAW";
		else
			uri = "RESOURCES";
		if (rel_path != null)
		{
			File f = new File(m_rvm.GetAbsolutePath(rel_path));
			return uri + "/" + f.getName();
		} else
		{
			File f = new File(ir.getAbsolutePath());
			return uri + "/" + f.getName();
		}
	}
	public boolean IsValid()
	{
		return true;
	}

	private void CollectFiles(ItemRecord ir, File dest_fold)
	{
		if (!ir.isCollectionDefined())
		{
			m_Files.put(m_ExperimentDir + "/" + GetURI(ir, null), new File(ir
					.getAbsolutePath()));
		} else
		{
			Collection<String> cs = m_rvm.GetAllFiles(ir);
			for (String s : cs)
			{
				m_Files.put(m_ExperimentDir + "/" + GetURI(ir, s), new File(
						m_rvm.GetAbsolutePath(s)));
			}
		}
	}
	/*
	 * private boolean CopyFiles(ItemRecord ir, File dest_fold) {
	 * if(!ir.IsCollectionDefined()) { return CopyFile(new
	 * File(ir.GetAbsolutePath()), new
	 * File(dest_fold.getAbsolutePath()+"/"+GetURI(ir,null))); } else {
	 * Collection<String> cs=m_rvm.GetAllFiles(ir); boolean bRes=true;
	 * for(String s:cs) bRes &= CopyFile(new File(m_rvm.GetAbsolutePath(s)),
	 * //new File(ir.GetAbsolutePath()), new
	 * File(dest_fold.getAbsolutePath()+"/"+GetURI(ir,s))); return bRes; } }
	 * private boolean CopyFile(File src, File dest) { // return
	 * Utils.CopyFile(src, dest);
	 * 
	 * try { if(!src.exists()) {
	 * ExpProgress("error: file "+src.getAbsolutePath()+" does not exist");
	 * return false; } if(!src.canRead()) {
	 * ExpProgress("error: cannot read file: "+src.getAbsolutePath()); return
	 * false; } if(dest.exists()) { if(!dest.canWrite())
	 * ExpProgress("error: cannot write temp file: "+dest.getAbsolutePath());
	 * return false; } FileInputStream fis=new FileInputStream(src);
	 * FileOutputStream fos = new FileOutputStream(dest); final byte[] buf=new
	 * byte[4096]; int bytesRead; while((bytesRead=fis.read(buf))!=-1)
	 * fos.write(buf,0,bytesRead); fis.close(); fos.close(); return true; }
	 * catch(Exception e) {
	 * ExpProgress("error: copying "+src.getAbsolutePath()+" to "
	 * +dest.getAbsolutePath()+" failed"); return false; }
	 * 
	 * }
	 */

	public static Element AddDefaultNamespaces(Element el)
	{
		return el.addNamespace("xnat", "http://nrg.wustl.edu/xnat")
				.addNamespace("xsi",
						"http://www.w3.org/2001/XMLSchema-instance")
				.addNamespace("cat", "http://nrg.wustl.edu/catalog");
		// .addNamespace("arc","http://nrg.wustl.edu/arc")
		// .addNamespace("behavioral", "http://nrg.wustl.edu/behavioral")
		// .addNamespace("
	}
	private String MakeExperimentLabel()
	{
		return GetTag("Experiment");
		// return
		// GetTag("Project")+"_"+GetTag("Subject")+"_"+GetTag("Experiment");
	}
	private void AddResource(ItemRecord ir)
	{
		if (!ir.isCollectionDefined())
		{
			m_resources.add(new Resource(ir, null));
		} else
		{
			Collection<String> cs = m_rvm.GetAllFiles(ir);
			for (String s : cs)
				m_resources.add(new Resource(ir, s));
		}
	}
	private boolean BuildXAR()
	{
		if (m_records.length < 1)
			return false;
		try
		{
			// create directory structure
			ExpProgress("building file list");

			String tempDir = System.getProperty("java.io.tmpdir");
			m_Archive = new File(tempDir + "/tempXAR.zip");
			m_destDir = new File(tempDir + "//" + m_tempFolder);
			try
			{
				CleanUp();
			} catch (Exception e)
			{
				ExpProgress("warning: removing the temp file "
						+ m_Archive.getAbsolutePath() + ", or folder "
						+ m_destDir.getAbsolutePath() + " failed.");
			}

			if (!m_destDir.mkdir())
			{
				ExpProgress("warning: creating temp folder "
						+ m_destDir.getAbsolutePath() + " failed");
				// return false;
			}
			File session_dir = new File(m_destDir.getAbsolutePath() + "/"
					+ GetTag("Experiment"));
			m_ExperimentDir = m_tempFolder + "/" + GetTag("Experiment");
			m_ExperimentDir = m_ExperimentDir.replace("\\", "/");

			if (!session_dir.mkdir())
			{
				ExpProgress("warning: creating temp folder "
						+ session_dir.getAbsolutePath() + " failed");
				// return false;
			}
			File raw_dir = new File(session_dir.getAbsolutePath() + "/RAW");
			if (!raw_dir.mkdir())
			{
				ExpProgress("warning: creating temp folder "
						+ raw_dir.getAbsolutePath() + " failed");
				// return false;
			}
			File proc_dir = new File(session_dir.getAbsolutePath()
					+ "/PROCESSED");
			if (!proc_dir.mkdir())
			{
				ExpProgress("warning: creating temp folder "
						+ proc_dir.getAbsolutePath() + " failed");
				// return false;
			}
			File res_dir = new File(session_dir.getAbsolutePath()
					+ "/RESOURCES");
			if (!res_dir.mkdir())
			{
				ExpProgress("warning: creating temp folder "
						+ res_dir.getAbsolutePath() + " failed");
				// return false;
			}

			// iterate through item records, copy resources to designated
			// folders while
			// assigning necessary tags
			String scanID;
			ItemTag tag;
			Scan scan;
			m_Files.clear();
			for (int i = 0; i < m_records.length; i++)
			{
				if (IsResource(m_records[i]))
				{
					AddResource(m_records[i]);
				} else
				{
					scanID = m_records[i].getTag("Scan").GetFirstValue();
					if (m_scans.containsKey(scanID))
					{
						scan = m_scans.get(scanID);
						scan.Update(m_records[i]);
					} else
					{
						scan = new Scan(scanID);
						scan.Update(m_records[i]);
						m_scans.put(scanID, scan);
					}

				}
				CollectFiles(m_records[i], session_dir);
			}
			// ExpProgress("creating session XML");
			Element el_session = AddDefaultNamespaces(
					DocumentHelper.createElement(GetSessionTag()))
					.addAttribute("ID", "").addAttribute("project",
							GetTag("Project")).addAttribute("label",
							MakeExperimentLabel())// GetTag("Experiment"))
					.addAttribute("modality", GetTag("Modality"));
			String dt = GetTag("Date"), date;
			if (dt == null || dt.length() < 8)
				date = dt;
			else
			{
				date = dt.substring(0, 4) + "-" + dt.substring(4, 6) + "-"
						+ dt.substring(6, 8);
				// date=dt.substring(4,
				// 6)+"/"+dt.substring(6,8)+"/"+dt.substring(0,4);
			}
			el_session.addElement("xnat:date")
					.setText(date != null ? date : "");
			el_session.addElement("xnat:subject_ID").setText(GetTag("Subject"));

			Element el_scans = el_session.addElement("xnat:scans");
			Scan[] scans = m_scans.values().toArray(new Scan[0]);
			OutputFormat format = OutputFormat.createPrettyPrint();
			for (int i = 0; i < scans.length; i++)
			{
				XMLWriter w = new XMLWriter(new FileWriter(session_dir
						.getAbsolutePath()
						+ "/" + scans[i].GetScanCatalogFile()), format);
				w.write(scans[i].GetScanCatalogXML());
				w.close();
				scans[i].AddSessionScanEntry(el_scans);
			}
			// to work with current xnat code
			// .img/.hdr filtering, has been fixed
			// FilterResources();
			if (m_resources.size() > 0)
			{
				Element el_resources = el_session.addElement("xnat:resources");
				for (final Resource r : m_resources)
				{
					XMLWriter w = new XMLWriter(new FileWriter(session_dir
							.getAbsolutePath()
							+ "/" + r.GetResourceCatalogFile()), format);
					w.write(r.GetResourceCatalogXML());
					w.close();
					r.AddSessionResourceEntry(el_resources);
				}
			}
			// write session.xml
			Document s_xml = DocumentHelper.createDocument();
			s_xml.add(el_session);
			XMLWriter xw = new XMLWriter(new FileWriter(m_destDir
					.getAbsolutePath()
					+ "//session.xml"), format);
			xw.write(s_xml);
			xw.close();
		} catch (Exception e)
		{
			ExpProgress("error: BuildXAR exception: " + e.getMessage());
			Utils.logger.debug("BuildXAR exception: " + e.getMessage());
			return false;
		}
		return true;
	}
	private boolean CreateArchive()
	{
		try
		{
			if (m_Archive.exists())
			{
				if (!m_Archive.delete())
					return false;
			}
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
					new FileOutputStream(m_Archive)));
			if (!ZipFolder(zos, m_destDir))
			{
				zos.close();
				return false;
			}
			for (Map.Entry<String, File> esf : m_Files.entrySet())
			{
				if (!ZipFile(zos, esf.getKey(), esf.getValue()))
					return false;
			}
			zos.flush();
			zos.close();
		} catch (Exception e)
		{
			return false;
		}
		return true;
	}
	private String GetRelativePath(File f)
	{
		int len = m_destDir.getAbsolutePath().length();
		String s = m_tempFolder + f.getAbsolutePath().substring(len);
		s = s.replace('\\', '/');
		return s;
	}
	private boolean ZipFile(ZipOutputStream zos, String rel_path, File file)
	{
		final byte[] buf = new byte[4096];
		BufferedInputStream bis = null;
		int nbytes;
		try
		{
			bis = new BufferedInputStream(new FileInputStream(file), 4096);
			zos.putNextEntry(new ZipEntry(rel_path));
			while ((nbytes = bis.read(buf, 0, buf.length)) > 0)
				zos.write(buf, 0, nbytes);
			zos.closeEntry();
			bis.close();
		} catch (Exception e)
		{
			return false;
		}
		return true;
	}

	private boolean ZipFolder(ZipOutputStream zos, File folder)
	{
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			if (files[i].isFile())
			{
				if (!ZipFile(zos, GetRelativePath(files[i]), files[i]))
					return false;
			} else if (!ZipFolder(zos, files[i]))
				return false;
		}
		return true;
	}
	private void CleanDir(File dir)
	{
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			if (files[i].isFile())
				files[i].delete();
			else
			{
				CleanDir(files[i]);
				files[i].delete();
			}
		}
	}
	private void CleanUp()
	{
		CleanDir(m_destDir);
		m_destDir.delete();
		m_Archive.delete();
		m_resources.clear();
		m_scans.clear();
	}
	private boolean PrepareArchive()
	{
		boolean bRes = true;
		if (!(bRes &= BuildXAR()))
		{
			ExpProgress("error: build XAR failed");
			return false;
		}
		ExpProgress("archiving");
		if (!(bRes &= CreateArchive()))
		{
			ExpProgress("error: archiving failed");
			return false;
		}
		return true;
	}
	public static boolean IsRunning()
	{
		return m_bRunning;
	}
	private ItemRecord[] GetRecords(VirtualFolder vf)
	{
		Collection<ItemRecord> cir = new LinkedList<ItemRecord>();
		TreeIterator ti = new TreeIterator(vf, new TypeFilter(
				TypeFilter.VFOLDER | TypeFilter.DBITEM, false));
		CElement el;
		while ((el = ti.Next()) != null)
		{
			if (el instanceof DBElement)
				cir.add(((DBElement) el).GetIR());
		}
		return cir.toArray(new ItemRecord[0]);
	}
	private void ExpProgress(String s)
	{
		ConsoleView.AppendMessage("Experiment " + m_experiment + ": " + s);
	}
	protected IStatus run(IProgressMonitor ipm)
	{
		m_bRunning = true;
		try
		{
			m_interfaceMessage = null;
			int nTotal = 0, nSuccess = 0;
			// for(final Map.Entry<String,Collection<DBElement>>
			// me:m_ExpMap.entrySet())
			int nExp = m_experiments.size();
			ipm.beginTask("Uploading", nExp);
			ConsoleView.AppendMessage("upload: verifying user");
			StoreXMLManager sxm = new StoreXMLManager(m_host, m_usr, m_pass, "");
			if (!CheckUser())
			{
				// Utils.ShowMessageBox("Upload error",
				// "Could not create user session", Window.OK);
				ConsoleView
						.AppendMessage("upload error: could not create user session");
				m_bRunning = false;
				return Status.OK_STATUS;
			}
			XNATRestAdapter xra = new XNATRestAdapter(m_host, m_usr, m_pass);
			for (VirtualFolder vf : m_experiments)
			{
				if (ipm.isCanceled())
					return Status.OK_STATUS;
				nTotal++;
				// m_experiment=me.getKey();
				m_experiment = vf.VirtualPath();
				ExpProgress("gathering files");
				m_records = GetRecords(vf);
				if (m_bCreateSubject)
				{
					ExpProgress("Creating subject "
							+ m_records[0].getTagValue("Subject"));
					if (!xra.CreateSubject(m_records[0]))
					// if(!sxm.StoreSubject(m_records[0]))
					{
						ExpProgress("error: subject creation failed. Experiment "
								+ m_experiment + " did not upload.");
						continue;
					}
				}
				ExpProgress("preparing for upload");
				if (!PrepareArchive())
				{
					ExpProgress("error: upload failed");
					continue;
				}
				ExpProgress("uploading");
				if (!Upload())
				{
					ExpProgress("error: upload failed");
					continue;
				}
				nSuccess++;
				ipm.worked(1);
			}
			String msg = nSuccess + " of " + nTotal
					+ " experiments uploaded successfully.";
			ConsoleView.AppendMessage(msg);
			m_bRunning = false;
			m_interfaceMessage = msg;
			return Status.OK_STATUS;
		} finally
		{
			m_bRunning = false;
		}
	}
	public static String GetInterfaceMessage()
	{
		String msg = m_interfaceMessage;
		m_interfaceMessage = null;
		return msg;
	}
	// temporary fix for the "feature" in XNAT Central which create virtual .hdr
	// files for .img files
	// (so that .hdr entries get duplicated in interface).
	private void FilterResources()
	{
		/*
		 * TreeMap<String,Resource> tmsr=new TreeMap<String,Resource>(); String
		 * file; //first pass, get all .img files for(final Resource
		 * r:m_resources) { file=r.GetURI(); if(file.endsWith(".img")) {
		 * if(tmsr.containsKey(file)) continue; else tmsr.put(file, r); } }
		 * //second pass, find corresponding .hdr files Collection<Resource>
		 * to_remove=new LinkedList<Resource>(); for(final Resource
		 * r:m_resources) { file=r.GetURI(); if(file.endsWith(".hdr")) { String
		 * f1=file.substring(0, file.length()-4)+".img";
		 * if(tmsr.containsKey(f1)) to_remove.add(r); } } for(final Resource r:
		 * to_remove) m_resources.remove(r);
		 */
	}
	private boolean CheckUser()
	{
		CheckUserSession cus = new CheckUserSession();
		String[] args = {"-u", m_usr, "-p", m_pass, "-host", m_host};
		return cus.perform(args);
	}
	private boolean Upload()
	{
		StoreXARWS exporter = new StoreXARWS();
		// ExpProgress("uploading");
		String[] args = {"-u", m_usr, "-p", m_pass, "-host", m_host, "-f",
				m_Archive.getAbsolutePath()};
		if (exporter.perform(args))
		{
			// ConsoleView.AppendMessage("Upload successful");
			return true;
		} else
		{
			ExpProgress("error: upload failed");
			return false;
		}
	}
	private boolean IsResource(ItemRecord ir)
	{
		return (ir.getTag("Scan") == null);
	}

	private class Resource
	{
		private Element m_CatResourceEntry;
		private Element m_ResourceTags;
		private Element m_CatTags;
		private String m_ID;
		private String m_uri;

		public Resource(ItemRecord ir, String rel_path)
		{
			m_ID = Utils.PseudoUID("res");
			// xnat:tags
			m_ResourceTags = AddDefaultNamespaces(DocumentHelper
					.createElement("xnat:tags"));
			m_CatTags = AddDefaultNamespaces(DocumentHelper
					.createElement("cat:tags"));
			ItemTag[] tags = ir.getAllTags();
			for (int i = 0; i < tags.length; i++)
			{
				if (DefaultOntologyManager.IsDefaultTagAttribSet(tags[i]
						.GetName(), TagDescr.TABLE_DISPLAY))
					continue;
				m_ResourceTags.addElement("xnat:tag").setText(
						tags[i].PrintTag());
				m_CatTags.addElement("cat:tag").setText(tags[i].PrintTag());
			}
			// xnat:resource
			m_CatResourceEntry = AddDefaultNamespaces(DocumentHelper
					.createElement("cat:entries"));
			if (rel_path != null)
				m_uri = StoreXARManager.this.GetURI(null, rel_path);
			else
				m_uri = StoreXARManager.this.GetURI(ir, null);

			m_CatResourceEntry.addElement("cat:entry").addAttribute("URI",
					m_uri).add(m_CatTags);
		}
		public String GetURI()
		{
			return m_uri;
		}
		public String GetResourceCatalogFile()
		{
			return "resource_" + m_ID + "_catalog.xml";
		}
		public Document GetResourceCatalogXML()
		{
			Document d = DocumentHelper.createDocument();
			Element el = AddDefaultNamespaces(DocumentHelper
					.createElement("cat:Catalog"));
			// el.add(m_CatTags);
			el.add(m_CatResourceEntry);
			d.add(el);
			return d;
		}
		public void AddSessionResourceEntry(Element parent)
		{
			Element el = parent.addElement("xnat:resource").addAttribute(
					"xsi:type", "xnat:resourceCatalog").addAttribute("URI",
					GetResourceCatalogFile());
			el.add(m_ResourceTags);
		}
	}
	private class Scan implements Comparable
	{
		public String m_ID = "";
		private String m_UID = "";
		private String m_SeriesDescription = "";
		private String m_ScanType = "";
		private String m_Quality = "";
		private Element m_ScanEntries;
		private String m_collContent = "";
		private String m_collFormat = "";
		private int m_currentInstanceNumber = 1;

		// private String m_Type="";
		private String m_Modality = "";

		public Scan(String id)
		{
			m_ID = id;
			m_ScanEntries = AddDefaultNamespaces(DocumentHelper
					.createElement("cat:entries"));
		}
		// temporary fix for the "feature" in XNAT Central which create virtual
		// .hdr files for .img files
		// (so that .hdr entries get duplicated in interface).
		private void FilterScanEntries()
		{
			/*
			 * TreeMap<String,Element> tmse=new TreeMap<String,Element>();
			 * String file; Element el; //first pass, get all .img files
			 * for(Iterator
			 * <Element>ie=m_ScanEntries.elementIterator();ie.hasNext();) {
			 * el=ie.next(); file=el.getName(); try {
			 * if(el.getName().compareTo("entry")!=0) continue; }
			 * catch(Exception e){continue;} file=el.attributeValue("URI");
			 * if(file.endsWith(".img")) { if(tmse.containsKey(file)) continue;
			 * else tmse.put(file, el); } }
			 * 
			 * //second pass, find corresponding .hdr files LinkedList<Element>
			 * to_remove=new LinkedList<Element>();
			 * for(Iterator<Element>ie=m_ScanEntries
			 * .elementIterator();ie.hasNext();) { el=ie.next();
			 * if(el.getName().compareTo("entry")!=0) continue;
			 * file=el.attributeValue("URI"); if(file.endsWith(".hdr")) { String
			 * f1=file.substring(0, file.length()-4)+".img";
			 * if(tmse.containsKey(f1)) to_remove.add(el); } } //remove extra
			 * elements for(Iterator<Element> ie=to_remove.iterator();
			 * ie.hasNext();) m_ScanEntries.remove(ie.next());
			 */
		}
		public int compareTo(Object o)
		{
			return m_ID.compareTo((String) o);
		}
		public String GetScanCatalogFile()
		{
			return "scan_" + m_ID + "_catalog.xml";
		}
		public Document GetScanCatalogXML()
		{
			Document d = DocumentHelper.createDocument();
			if (m_UID.length() < 1)
				m_UID = Utils.PseudoUID("ses");
			Element el = AddDefaultNamespaces(DocumentHelper.createElement(
					"cat:DCMCatalog").addAttribute("UID", m_UID));
			FilterScanEntries();
			el.add(m_ScanEntries);
			d.add(el);
			return d;
		}

		public void AddSessionScanEntry(Element parent)
		{
			// String xsitype="xnat:"+m_Modality.toLowerCase()+"ScanData";
			String xsitype = "xnat:mrScanData";
			Element el = parent.addElement("xnat:scan");
			el.addAttribute("ID", m_ID).addAttribute("type", m_ScanType)
					.addAttribute("xsi:type", xsitype);
			el.addElement("xnat:quality").setText(m_Quality);
			el.addElement("xnat:file")
					.addAttribute("URI", GetScanCatalogFile()).addAttribute(
							"xsi:type", "xnat:resourceCatalog").addAttribute(
							"format", m_collFormat).addAttribute("label",
							m_collFormat)
					.addAttribute("content", m_collContent);
			el.addElement("xnat:series_description").setText(
					m_SeriesDescription);
		}
		private void Update(ItemRecord ir)
		{
			if (!ir.isCollectionDefined())
				AddScanEntry(ir, null);
			else
			{
				Collection<String> cs = m_rvm.GetAllFiles(ir);
				for (String p : cs)
					AddScanEntry(ir, p);
			}
		}

		public void AddScanEntry(ItemRecord ir, String rel_path)
		{
			String uid;
			ItemTag tag = ir.getTag("ImgSOPInstUID");
			if (tag != null)
				uid = tag.GetFirstValue();
			else
				uid = Utils.PseudoUID("");
			m_ScanEntries.addElement("cat:entry").addAttribute("UID", uid)
					.addAttribute("URI", GetURI(ir, rel_path)).addAttribute(
							"instanceNumber",
							new Integer(m_currentInstanceNumber).toString())
					.addAttribute("xsi:type", "cat:dcmEntry");
			if (m_Modality.length() < 1)
			{
				try
				{
					m_Modality = ir.getTag("Modality").GetFirstValue();
				} catch (Exception e)
				{
					m_Modality = "MR";
				}
				if (m_Modality.length() < 1)
					m_Modality = "MR";
			}

			if (m_Quality.length() < 1)
			{
				try
				{
					m_Quality = ir.getTag("Quality").GetFirstValue();
				} catch (Exception e)
				{
					m_Quality = "usable";
				}
			}
			if (m_UID.length() < 1)
			{
				try
				{
					m_UID = ir.getTag("SeriesInstUID").GetFirstValue();
				} catch (Exception e)
				{
					m_UID = "";
				}
			}
			if (m_SeriesDescription.length() < 1)
			{
				try
				{
					m_SeriesDescription = ir.getTag("SeriesDescription")
							.GetFirstValue();
				} catch (Exception e)
				{
				}
			}
			if (m_ScanType.length() < 1)
			{
				try
				{
					m_ScanType = ir.getTag("Scan_Type").GetFirstValue();
				} catch (Exception e)
				{
				}
			}
			if (m_collContent.length() < 1)
			{
				try
				{
					m_collContent = ir.getTag("coll_content").GetFirstValue();
				} catch (Exception e)
				{
				}
			}
			if (m_collFormat.length() < 1)
			{
				try
				{
					m_collFormat = ir.getTag("coll_format").GetFirstValue();
				} catch (Exception e)
				{
				}
			}
			m_currentInstanceNumber++;
		}
	}
}