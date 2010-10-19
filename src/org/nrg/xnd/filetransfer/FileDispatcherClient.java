package org.nrg.xnd.filetransfer;

import java.io.File;
import java.net.Socket;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.RestRepositoryManager;
import org.nrg.fileserver.XNATRestAdapter;
import org.nrg.xnd.app.ConsoleView;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.utils.FSObject;
import org.nrg.xnd.utils.Utils;

public class FileDispatcherClient
{
	private Vector<Bundle> m_Outbound = new Vector<Bundle>();
	private Vector<Bundle> m_Inbound = new Vector<Bundle>();
	private DownloadThread m_dt;
	private UploadThread m_ut;

	private String m_host = "";
	private int m_port = -1;
	private RepositoryViewManager m_rmRemote = null;
	private RepositoryViewManager m_rmLocal = null;

	public void SessionInit(String server, int port,
			RepositoryViewManager remote)
	{
		m_host = server;
		m_port = port;
		m_rmLocal = XNDApp.app_localVM;
		m_rmRemote = remote;

		m_ut = new UploadThread();
		m_dt = new DownloadThread();
		m_ut.start();
		m_dt.start();
	}
	public void SessionEnd()
	{
		if (m_ut != null)
			m_ut.m_bRunning = false;
		if (m_dt != null)
			m_dt.m_bRunning = false;
	}
	public FileDispatcherClient()
	{
	}
	private LinkedList<String> GetURIs(DBElement dbe, RepositoryViewManager rvm)
	{
		ItemRecord el_ir = dbe.GetIR();
		if (dbe.IsCollection())
		{
			Collection<String> files = rvm.getCM().GetCollection(
					el_ir.getColID()).GetAllFiles();
			return new LinkedList<String>(files);
		} else
		{
			LinkedList<String> files = new LinkedList<String>();
			files.add(el_ir.getRelativePath());
			return files;
		}

	}

	public synchronized void AddToDownloadQueue(DBElement dbe, String path)
	{
		m_Inbound.add(new Bundle(dbe.GetIR(), GetURIs(dbe, m_rmRemote), path));
		// m_Inbound.addElement(new Bundle(src,dest));
	}
	public synchronized void AddToUploadQueue(DBElement dbe)
	{
		if (dbe.IsCollection())
		{
			// ??
			ConsoleView
					.AppendMessage("Warning: all files in collection "
							+ dbe.GetLabel()
							+ " will be uploaded separately. This version of XND does not support remote collection upload.");
		}
		m_Outbound.addElement(new Bundle(dbe.GetIR(), GetURIs(dbe, m_rmLocal),
				null));
	}
	private String PrepareStorage(String rel_path, String root)
	{
		FSObject fso = new FSObject(rel_path);
		String name = fso.getName();
		if (root != null)
		{
			File f = new File(root + "/" + rel_path);
			File par = f.getParentFile();
			if (par == null)
				return null;
			if (!par.exists() && !par.mkdirs())
				return null;
			return f.getAbsolutePath();
		}
		String in_fold = Utils.GetIncomingFolder() + "/downloads";
		Calendar c = Calendar.getInstance();
		in_fold += "/"
				+ String.format("%1$04d_%2$02d_%3$02d", c.get(Calendar.YEAR), c
						.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1)
				+ "/";
		boolean bStop = false;
		int num = 0;
		FSObject down;
		while (!bStop)
		{
			down = new FSObject(in_fold + String.format("data%1$x", num) + "/"
					+ name);
			if (!down.exists())
			{
				FSObject dir = new FSObject(in_fold
						+ String.format("data%1$x", num));
				if (!dir.exists() && !dir.mkdirs())
					return null;
				return down.getAbsolutePath();
			}
			num++;
		}
		return null;
	}

	private boolean TransferFile(ItemRecord base_ir, String rel_path,
			String root, boolean bUpload)
	{
		Socket s = null;
		boolean bRes = false, bRest;
		String loc_path = "", s0 = "";

		try
		{
			FileTransfer im;
			// web servicesk
			// if(XNDApp.IsWebService())
			{
				if (m_rmRemote.GetRM() instanceof RestRepositoryManager)
					im = (RestRepositoryManager) m_rmRemote.GetRM();
				else if (m_rmRemote.GetRM() instanceof XNATRestAdapter)
					im = (XNATRestAdapter) m_rmRemote.GetRM();
				else
					return false;
				bRest = true;
			}
			/*
			 * //socket transfer else { s=new Socket(m_host,m_port); im=new
			 * SocketItemManager(s,m_rmLocal, m_rmRemote); bRest=false; }
			 */
			if (bUpload)
			{
				ItemRecord ir = new ItemRecord(m_rmLocal
						.GetAbsolutePath(rel_path), rel_path);
				ir.tagsSetFromArray(base_ir.getAllTags());
				// ?? remove collection ID tag.
				ir.tagRemove("Collection_ID");
				bRes = im.Put(ir, ir);
				/*
				 * if(!(im instanceof XNATRestAdapter)) else //upload file to
				 * XNAT E. {
				 * 
				 * }
				 */
			} else
			{
				loc_path = PrepareStorage(rel_path, root);
				if (loc_path == null)
				{
					Utils.logger
							.error("FileDispatcherClient.TransferFile: PrepareStorage failed");
					return false;
				}
				if ((s0 = m_rmLocal.GetRelativePath(loc_path)) == null)
				{
					Utils.logger
							.error("FileDispatcherClient.TransferFile: GetRelativePath failed");
					return false;
				}
				ItemRecord dest = new ItemRecord(loc_path, s0);
				bRes = im.Get(new ItemRecord(null, rel_path), dest);
				if (!bRes)
					Utils.logger
							.error("FileDispatcherClient.TransferFile failed to download file");

				if (im instanceof XNATRestAdapter)
				{
					dest.tagsMerge(base_ir.getTagCollection());
					// rename resource tag due to hierarchy structure
					// incompatibility
					ItemTag it = dest.getTag("Resource");
					if (it != null)
					{
						dest.tagRemove("Resource");
						dest
								.tagSet(new ItemTag("XNATResource", it
										.GetValues()));
					}
				}
				m_rmLocal.ItemAdd(dest, true);

				// bRes=im.Get(src,ir_dest,true);
				// m_rmLocal.ItemAdd(ir_dest, true);
			}
			if (!bRest && s.isConnected())
				s.close();
			return bRes;
		} catch (Exception e)
		{
			if (!bUpload && loc_path.length() > 0)
				new File(loc_path).delete();
			return false;
		}
	}
	private class Bundle
	{
		// private Vector<ItemRecord> m_Src;
		private LinkedList<String> m_files;
		// public File m_dest;
		public ItemRecord m_ir;
		public String m_Path;
		public Bundle(ItemRecord ir, LinkedList<String> files, String path)
		{
			m_files = files;
			m_ir = ir;
			m_Path = path;
			// m_Src=new Vector<ItemRecord>(src.length);
			// m_dest=dest;
			// for(int i=0; i<src.length; i++) m_Src.addElement(src[i]);
		}
		public synchronized String GetNext()
		{
			if (m_files == null || m_files.size() < 1)
				return null;
			return m_files.getLast();
			// if(m_Src.size()<1) return null;
			// ItemRecord ir=m_Src.elementAt(0);
			// return ir;
		}
		public synchronized void RemoveNext()
		{
			m_files.removeLast();
			// m_Src.removeElementAt(0);
		}
	}
	private class DownloadThread extends Thread
	{
		public boolean m_bRunning = false;
		private Bundle m_CurrentBundle = null;
		@Override
		public void run()
		{
			m_bRunning = true;
			while (m_bRunning)
			{
				try
				{
					while (m_Inbound.size() > 0 && m_bRunning)
						DownloadNextBundle();
					sleep(1000);
				} catch (Exception e)
				{
				}
			}
		}
		private void DownloadNextBundle()
		{
			if (m_CurrentBundle == null)
			{
				m_CurrentBundle = m_Inbound.elementAt(0);
				m_Inbound.removeElementAt(0);
			}
			String remote;
			int nFailedAttempts = 0, maxFailedAttempts = 1;

			try
			{
				while ((remote = m_CurrentBundle.GetNext()) != null)
				{
					String msg;
					ConsoleView.AppendMessage(remote + ": downloading");
					if (TransferFile(m_CurrentBundle.m_ir, remote,
							m_CurrentBundle.m_Path, false))
					{
						msg = remote + " download success.";
						m_CurrentBundle.RemoveNext();
						nFailedAttempts = 0;
					} else
					{
						nFailedAttempts++;
						msg = remote + "download failed, attempt "
								+ nFailedAttempts;
						if (nFailedAttempts == maxFailedAttempts)
						{
							m_CurrentBundle.RemoveNext();
							nFailedAttempts = 0;
						} else
							Thread.sleep(5000);
					}
					ConsoleView.AppendMessage(msg);
				}
				if (m_CurrentBundle.GetNext() == null)
					m_CurrentBundle = null;
			} catch (Exception e)
			{
				ConsoleView.AppendMessage("File download exception: "
						+ e.getMessage());
			}
		}
	}
	private class UploadThread extends Thread
	{
		public boolean m_bRunning = false;
		private Bundle m_CurrentBundle = null;
		@Override
		public void run()
		{
			m_bRunning = true;
			while (m_bRunning)
			{
				try
				{
					while (m_Outbound.size() > 0 && m_bRunning)
						UploadNextBundle();
					sleep(1000);
				} catch (Exception e)
				{
				}
			}
		}
		private void UploadNextBundle()
		{
			if (m_CurrentBundle == null)
			{
				m_CurrentBundle = m_Outbound.elementAt(0);
				m_Outbound.removeElementAt(0);
			}
			String local;
			int nFailedAttempts = 0, maxFailedAttempts = 1;
			try
			{
				String remoteAddr = "";
				if (m_rmRemote.GetRM() instanceof RestRepositoryManager)
					remoteAddr = ((RestRepositoryManager) (m_rmRemote).GetRM())
							.GetRoot();
				else if (m_rmRemote.GetRM() instanceof XNATRestAdapter)
					remoteAddr = ((XNATRestAdapter) m_rmRemote.GetRM())
							.GetRoot();
				while ((local = m_CurrentBundle.GetNext()) != null)
				{
					String msg;
					ConsoleView.AppendMessage("Uploading file \"" + local
							+ " to " + remoteAddr);

					if (TransferFile(m_CurrentBundle.m_ir, local, null, true))
					{
						msg = local + " upload success.";
						m_CurrentBundle.RemoveNext();
						nFailedAttempts = 0;
					} else
					{
						nFailedAttempts++;
						msg = local + " upload failed, attempt "
								+ nFailedAttempts;
						if (nFailedAttempts == maxFailedAttempts)
						{
							m_CurrentBundle.RemoveNext();
							nFailedAttempts = 0;
						}
					}
					ConsoleView.AppendMessage(msg);
				}
				if (m_CurrentBundle.GetNext() == null)
					m_CurrentBundle = null;
			} catch (Exception e)
			{
				ConsoleView.AppendMessage("File upload exception: "
						+ e.getMessage());
			}
		}
	}
}