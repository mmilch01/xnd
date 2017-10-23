package org.nrg.xnd.tools;
import java.io.BufferedInputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.httpclient.HttpMethodBase;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.nrg.fileserver.Context;
import org.nrg.fileserver.FileCollection;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.XNATRestAdapter;
import org.nrg.xdat.webservices.CheckUserSession;
import org.nrg.xnd.app.ConsoleView;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.LocalFileCollection;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.model.Validator;
import org.nrg.xnd.model.VirtualFolder;
import org.nrg.xnd.ontology.XNATThesaurus;
import org.nrg.xnd.ontology.XNATValidator;
import org.nrg.xnd.utils.Utils;

public class HierarchyUploadManager extends Job
{
	private String m_usr;
	private String m_pass;
	private String m_host;
	private XNATRestAdapter m_xra;
	private RepositoryViewManager m_rvm;
	private File m_Archive, m_destDir;
	private VirtualFolder m_vf;
	private static String m_tempFolder = "XND_ZIP";
	private IProgressMonitor m_ipm;
	private Collection<CElement> m_uploads;
	private Validator m_validator;

	public HierarchyUploadManager(String host, String usr, String pass,
			Collection<CElement> upl, RepositoryViewManager rvm)
	{
		super("Uploading files");
		setUser(true);
		m_uploads = upl;
		m_usr = usr;
		m_pass = pass;
		m_host = host;
		m_rvm = rvm;
		m_xra = new XNATRestAdapter(host, usr, pass);
		m_validator = new XNATValidator(m_uploads, m_xra);
	}
	public String isDataValid(boolean bAutoFix)
	{
		return m_validator.validate(bAutoFix);
	}
	private boolean createHierarchy(VirtualFolder vf)
	{
		Context context = vf.getContext();

		Context newContext = new Context(context);
		Context contextToCreate = new Context();
		String q;
		HttpMethodBase method;
		m_xra.enableConnErrorReport(false);
		// first, find at which level the context should be created.
		boolean bAmbiguousContext = false;
		do
		{
			bAmbiguousContext = XNATThesaurus.isAmbiguousContext(newContext);
			q = XNATRestAdapter.FormSubQuery(newContext,null,true);
			method = m_xra.PerformConnection(XNATRestAdapter.GET, q,
					(Object) null);
			if (method != null)
			{
				method.releaseConnection();
				if (method.getStatusCode() == 200 && !bAmbiguousContext)
					break;
			}
			contextToCreate.add(0, newContext.getLast());
			newContext.removeLast();
		} while (method == null && (newContext.size() > 0));
		m_xra.enableConnErrorReport(true);
		if (contextToCreate.size() < 1)
			return true;
		// second, create sequentially all missing levels on server.
		do
		{
			newContext.add(contextToCreate.getFirst());
			contextToCreate.removeFirst();
			String params;
			if (null == (params = createHierarchyLevel(newContext, vf
					.getAssociatedTags(), null)))
			{
				// amend context path for ambiguous contexts only - this is a
				// temp fix.
				if (XNATThesaurus.isAmbiguousContext(newContext))
				{
					XNATThesaurus.amendContext(newContext);
					if (null != (createHierarchyLevel(newContext, vf
							.getAssociatedTags(), params)))
						continue;
					/*
					 * q = XNATRestAdapter.FormSubQuery(newContext); if (params
					 * != null) q += "?" + params; method =
					 * m_xra.PerformConnection(XNATRestAdapter.PUT, q, (Object)
					 * null); if (method != null)
					 * 
					 * { method.releaseConnection(); continue; }
					 */
				}
				ExpProgress("Creating hierarchy failed");
				return false;
			}
			if(method!=null) method.releaseConnection();
		} while (contextToCreate.size() > 0);
		return true;
	}
	private String createHierarchyLevel(Context newContext, ItemRecord tags,
			String params)
	{
		HttpMethodBase method;
		String q = XNATRestAdapter.FormSubQuery(newContext,null,false);
		// get additional query parameters.
		if (params == null)
			params = XNATThesaurus.getQueryParams(newContext, tags);
		if (params != null)
			q += "?" + params;
		method = m_xra.PerformConnection(XNATRestAdapter.PUT, q, (Object) null);
		if (method != null)
		{
			method.releaseConnection();
			return params;
		}
		return null;
	}
	public boolean uploadVirtualFolder(VirtualFolder vf,IProgressMonitor ipm)
	{
		// here, we need to find all sub-experiment level entries, separate them
		// and send to the server each one separately.
		// therefore, we'll need to recursively go through vf's descendents to
		// extract all such sub-entities.
		// first thing we do, is check whether this vf's level is low enough to
		// upload in one piece.
		m_vf = vf;
		ExpProgress("");

		Collection<CElement> cch = vf.GetChildren(null, m_ipm);
		// extract immediate non-vf children
		Collection<CElement> dbitems = new TypeFilter(TypeFilter.COLLECTION
				| TypeFilter.DBITEM, false).Filter(cch);
		// out of them, existing collections
		Collection<CElement> collections = new TypeFilter(
				TypeFilter.COLLECTION, false).Filter(dbitems);
		// the rest are uploaded as a standalone resource.
		Collection<CElement> files = new TypeFilter(TypeFilter.DBITEM, false)
				.Filter(dbitems);
		boolean bRes = true, iRes = true;
		if (collections.size() > 0 || files.size() > 0)
		{
			if (createHierarchy(vf))
			{
				ipm.subTask("Creating hierarchy for "+vf.GetLabel());
				if (ipm.isCanceled()) return false;
				iRes = uploadFileColAsResource(files);
				bRes &= iRes;
				if (ipm.isCanceled()) return false;
				ipm.subTask("Uploading "+vf.GetLabel());
				iRes &= uploadCollectionsAsResources(collections);
				bRes &= iRes;
				if (iRes)
					ExpProgress("success");
			} else
				return false;
			if (!bRes)
				return false;
		}
		for (CElement ce : cch)
		{
			if (ce instanceof VirtualFolder)
			{
				ipm.subTask("Uploading "+ce.GetLabel());
				bRes &= uploadVirtualFolder((VirtualFolder) ce,ipm);
			}
			if (ipm.isCanceled()) return false;
		}
		return bRes;
	}
	private boolean uploadCollectionsAsResources(Collection<CElement> cols)
	{
		if (cols.size() < 1)
			return true;
		boolean bRes = true;
		for (CElement ce : cols)
		{
			ItemRecord ir = ((DBElement) ce).GetIR();
			bRes &= uploadFileCollection(ir, m_rvm.getCM().GetCollection(
					ir.getColID()));
		}
		return bRes;
	}
	private boolean uploadFileColAsResource(Collection<CElement> files)
	{
		if (files.size() < 1)
			return true;
		ItemRecord tagRecord = new ItemRecord(null, null);
		ItemRecord ir;
		LocalFileCollection lfe = new LocalFileCollection("", true);
		for (CElement ce : files)
		{
			ir = ((DBElement) ce).GetIR();
			tagRecord.tagsMerge(ir.getTagCollection());
			lfe.AddFile(ir.getRelativePath());
		}
		return uploadFileCollection(tagRecord, lfe);
	}
	private String getFileExtension(File file) {
	    String name = file.getName();
	    try {
	        return name.substring(name.lastIndexOf(".") + 1);
	    } catch (Exception e) {
	        return "";
	    }
	}	
	private boolean uploadFileCollectionRaw(ItemRecord ir, FileCollection fc)
	{
		System.out.println("uploading files individually");
		Context context = new Context(m_vf.getContext());		
		String col_format = null, col_content = null;
		if (context.getLast().GetName().compareTo("Scan") == 0)
		{
			col_format = ir.getTagValue("coll_format");
			col_content = ir.getTagValue("coll_content");
			if (col_format != null && col_content != null) // create a resource.
			{
				context.addLast(new ItemTag("resource", col_format));
				String params = XNATThesaurus.getQueryParams(context, ir);
				if (params != null && params.length() > 0)
					params += "&content=" + col_content + "&format="
							+ col_format;
				else
					params = "content=" + col_content + "&format=" + col_format;
				if (null == createHierarchyLevel(context, ir, params))
					return false;
			}
		}
		String qb = XNATRestAdapter.FormSubQuery(context,ir,true), sub_query;
		String param;
		File fil;
		for (String f : fc.GetAllFiles())
		{
			sub_query = qb;
//			sub_query += "files/" + "files.zip" + "?inbody=true&extract=true";
			fil=new File(m_rvm.GetAbsolutePath(f));
			sub_query += "files/" + fil.getName() + "?inbody=true";
			if (getFileExtension(fil).toLowerCase().compareTo("zip")==0)
				sub_query += "files/" + fil.getName() + "?inbody=true&extract=true";
			if (col_format != null & col_content != null)
			{
				sub_query += "&format=" + col_format + "&content=" + col_content;
			}
			for (ItemTag it : ir.getAllTags())
			{
				param = XNATThesaurus.GetVarname(it.GetName(), context);
				if (param != null && param.length() > 0)
					sub_query += "&" + param + "="
							+ Utils.StrFormatURI(it.GetFirstValue());
			}
			m_ipm.subTask(fil.getName());
			HttpMethodBase hmb = m_xra.PerformConnection(XNATRestAdapter.PUT,
					sub_query, new File(m_rvm.GetAbsolutePath(f)));
			// m_xra.PerformConnection(XNATRestAdapter.PUT, sub_query, is);
			if (hmb == null) return false;
			hmb.releaseConnection();
		}		
		return true;		
	}
	private boolean uploadFileCollection(ItemRecord ir, FileCollection fc)
	{
//		try{
//			if (!XNDApp.app_Prefs.nodeExists("PrefsFileTransfer.PreZipUploads")) return false;
//		}
//		catch(Exception e){}
		if (!XNDApp.app_Prefs.getBoolean("PrefsFileTransfer.PreZipUploads", true))
			return uploadFileCollectionRaw(ir,fc);
		System.err.println("zipping files...");
		m_ipm.subTask("zipping files...");
		if (!PrepareArchive(fc))
			return false;
		// start an archiving thread, get an input stream from it.

		/*
		 * PipedInputStream is=new PipedInputStream(); Archiver arch=new
		 * Archiver(fc,is); arch.start();
		 */
		Context context = new Context(m_vf.getContext());

		// temp fix for scan resource
		String col_format = null, col_content = null;
		if (context.getLast().GetName().compareTo("Scan") == 0)
		{
			col_format = ir.getTagValue("coll_format");
			col_content = ir.getTagValue("coll_content");
			if (col_format != null && col_content != null) // create a resource.
			{
				context.addLast(new ItemTag("resource", col_format));
				String params = XNATThesaurus.getQueryParams(context, ir);
				if (params != null && params.length() > 0)
					params += "&content=" + col_content + "&format="
							+ col_format;
				else
					params = "content=" + col_content + "&format=" + col_format;
				if (null == createHierarchyLevel(context, ir, params))
					return false;
			}
		}
		String sub_query = XNATRestAdapter.FormSubQuery(context,ir,true);
		String param;
		sub_query += "files/" + "files.zip" + "?inbody=true&extract=true";
		if (col_format != null & col_content != null)
		{
			sub_query += "&format=" + col_format + "&content=" + col_content;
		}
		for (ItemTag it : ir.getAllTags())
		{
			param = XNATThesaurus.GetVarname(it.GetName(), context);
			if (param != null && param.length() > 0)
				sub_query += "&" + param + "="
						+ Utils.StrFormatURI(it.GetFirstValue());
		}
		m_ipm.subTask("uploading" + ir.getCollectionName());
		HttpMethodBase hmb = m_xra.PerformConnection(XNATRestAdapter.PUT,
				sub_query, m_Archive);
		// m_xra.PerformConnection(XNATRestAdapter.PUT, sub_query, is);
		if (hmb != null)
			hmb.releaseConnection();
		return (hmb != null);
	}
	private void CleanUp()
	{
		CleanDir(m_destDir);
		m_destDir.delete();
		m_Archive.delete();
	}
	private void ExpProgress(String s)
	{
		ConsoleView.AppendMessage("Uploading " + m_vf.GetLabel() + ": " + s);
	}
	private boolean PrepareArchive(FileCollection fc)
	{
		String tempDir = System.getProperty("java.io.tmpdir");
		m_Archive = new File(tempDir + "/xnd_vfold.zip");
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
		}

		try
		{
			if (m_Archive.exists())
			{
				if (!m_Archive.delete())
					return false;
			}
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
					new FileOutputStream(m_Archive)));
			for (String f : fc.GetAllFiles())
			{
				if (!ZipFile(zos, "", new File(m_rvm.GetAbsolutePath(f))))
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
	private boolean ZipFile(ZipOutputStream zos, String rel_path, File file)
	{
		final byte[] buf = new byte[4096];
		BufferedInputStream bis = null;
		int nbytes;
		try
		{
			bis = new BufferedInputStream(new FileInputStream(file), 4096);
			zos.putNextEntry(new ZipEntry(rel_path + file.getName()));
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
	private boolean CheckUser()
	{
		CheckUserSession cus = new CheckUserSession();
		String[] args = {"-u", m_usr, "-p", m_pass, "-host", m_host};
		return cus.perform(args);
	}
	@Override
	protected IStatus run(IProgressMonitor ipm)
	{
		// verify user
//		BusyIndicator.showWhile(Display.getCurrent(), this.getThread());
		m_ipm = ipm;
		ipm.beginTask("Uploading", 0);
		ConsoleView.AppendMessage("upload: verifying user");
		if (!CheckUser())
		{
			ConsoleView
					.AppendMessage("upload error: could not create user session");
			return Status.OK_STATUS;
		}
		int nErrors = 0;

		TreeSet<String> exps = new TreeSet<String>();

		// run the upload cycle
		for (CElement el : m_uploads)
		{
			if (el instanceof VirtualFolder)
			{
				if (!uploadVirtualFolder((VirtualFolder) el,ipm))
				{					
					nErrors++;
					if(!ipm.isCanceled())
						ExpProgress("upload failed");
					else 
						ExpProgress("upload canceled");
				} else
				// extract experiment context from this virtual folder.
				{
					Context c = ((VirtualFolder) el).getContext()
							.getBroaderContext("Experiment");
					exps.add(c.toString());
				}
			}
		}

		// run the snapshot generation+dicom tag extraction cycle
		for (String s : exps)
		{
			if (ipm.isCanceled()) return Status.CANCEL_STATUS;
			Context cx = Context.fromString(s);
			String q = XNATRestAdapter.FormSubQuery(cx,null,false), q1, q2;
			q1 = q + "?triggerPipelines=true";
			q2 = q + "?pullDataFromHeaders=true";
			HttpMethodBase method;
			if (XNDApp.app_Prefs.getBoolean(
					"PrefsFileTransfer.ExtractDataFromDICOMHeaders", false))
			{
				method = m_xra.PerformConnection(XNATRestAdapter.PUT, q2,
						(Object) null);
				if (method == null)
				{
					ConsoleView
							.AppendMessage("Extracting data from header failed for "
									+ s);
				} else
					method.releaseConnection();
			}
			if (ipm.isCanceled()) return Status.CANCEL_STATUS;			
			if (XNDApp.app_Prefs.getBoolean(
					"PrefsFileTransfer.RunPipelinesCheck", false))
			{
				method = m_xra.PerformConnection(XNATRestAdapter.PUT, q1,
						(Object) null);
				if (method == null)
				{
					ConsoleView
							.AppendMessage("Triggering pipelines failed for "
									+ s);
				} else
					method.releaseConnection();
			}
		}
		if(ipm.isCanceled())
		{
			ConsoleView.AppendMessage("Uploading canceled");
			return Status.CANCEL_STATUS;
		}

		if (nErrors == 0)
		{
			ConsoleView.AppendMessage("Uploading finished successfully");
			return Status.OK_STATUS;
		}
		else
		{
			ConsoleView.AppendMessage("Uploading finished with " + nErrors
					+ " errors.");
			return Status.CANCEL_STATUS;
		}
		
	}
	private class Archiver extends Thread
	{
		private FileCollection m_fc;
		private PipedInputStream m_is;
		private boolean m_isRunning = false;

		public boolean isRunning()
		{
			return m_isRunning;
		}
		public Archiver(FileCollection fc, PipedInputStream is)
		{
			m_fc = fc;
			m_is = is;
		}
		@Override
		public void run()
		{
			ZipOutputStream zos = null;
			m_isRunning = true;
			try
			{
				zos = new ZipOutputStream(new PipedOutputStream(m_is));
				for (String f : m_fc.GetAllFiles())
				{
					if (!ZipFile(zos, "", new File(m_rvm.GetAbsolutePath(f))))
					{
						m_is.close();
						zos.close();
						m_isRunning = false;
						return;
					}
				}
				zos.close();
			} catch (Exception e)
			{
				try
				{
					if (zos != null)
						zos.close();
				} catch (Exception e1)
				{
				}
			}
			m_isRunning = false;
		}
	}
}