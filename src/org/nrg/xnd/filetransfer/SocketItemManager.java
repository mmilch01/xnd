package org.nrg.xnd.filetransfer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.RepositoryManager;
import org.nrg.xnd.utils.Utils;

public class SocketItemManager implements FileTransfer
{
	private RepositoryManager m_localRM, m_remoteRM;
	private Socket m_s;
	private ObjectOutputStream m_oos = null;
	private ObjectInputStream m_ois = null;

	private final int max_timeout = 30000;

	public SocketItemManager(Socket s, RepositoryManager local,
			RepositoryManager remote)
	{
		m_s = s;
		m_localRM = local;
		m_remoteRM = remote;
		try
		{
			m_oos = new ObjectOutputStream(new BufferedOutputStream(m_s
					.getOutputStream()));
			// Utils.SerializeString(m_oos, "noop", false);
			// m_oos.flush();
		} catch (Exception e)
		{
		}

	}
	@Override
	public boolean Put(ItemRecord dest, ItemRecord src)
	{
		/*
		 * final byte[] buf=new byte[16384]; File localFile=GetLocalPath(local);
		 * if(remote==null) { remote = new
		 * ItemRecord(localFile.getAbsolutePath(),null); } long flen; try {
		 * flen=localFile.length(); if(isClient) { Utils.SerializeString(m_oos,
		 * "Put", false); Utils.SerializeItemRecord(m_oos, local, false);
		 * Utils.SerializeItemRecord(m_oos, remote, false); m_oos.flush(); }
		 * 
		 * // m_oos.writeLong(flen);
		 * 
		 * int len, total_len=0; BufferedOutputStream bos=new
		 * BufferedOutputStream(m_s.getOutputStream()); FileInputStream fis=new
		 * FileInputStream(localFile);
		 * Logger.getRootLogger().debug("File upload start");
		 * while((len=fis.read(buf))>0) { // len=fis.read(buf); if(len>0) {
		 * bos.write(buf,0,len); total_len+=len; } else Thread.sleep(500); }
		 * Logger
		 * .getRootLogger().debug("File upload end: bytes written = "+total_len
		 * ); bos.flush(); bos.close(); //close socket fis.close(); }
		 * catch(Exception e) {
		 * Logger.getRootLogger().debug("File upload failed: exception "
		 * +e.getMessage()); return false; } return true;
		 */
		return false;
	}
	@Override
	public boolean Get(ItemRecord src, ItemRecord dest)
	{
		return false;
		/*
		 * File localFile; if(local==null || !isClient)
		 * localFile=SuggestLocalPath(remote); else {
		 * localFile=GetLocalPath(local); } final byte[] buf=new byte[4096];
		 * long flen=0; try { if(!localFile.createNewFile()) return false; int
		 * len,off=0; if(isClient) { Utils.SerializeString(m_oos, "Get", false);
		 * m_oos.flush(); Utils.SerializeItemRecord(m_oos, remote, false);
		 * m_oos.flush(); Utils.SerializeItemRecord(m_oos, local, false);
		 * m_oos.flush(); } // if(m_ois==null) m_ois=new ObjectInputStream(new
		 * BufferedInputStream(m_s.getInputStream())); // flen=m_ois.readLong();
		 * // if(flen<=0) return false;
		 * 
		 * BufferedInputStream bis=new
		 * BufferedInputStream((m_s.getInputStream())); FileOutputStream fos=new
		 * FileOutputStream(localFile); while((len=bis.read(buf))>0) { if(len>0)
		 * { fos.write(buf,0,len); off+=len; // System.out.println(off); } else
		 * { Thread.sleep(500); } } fos.flush(); fos.close(); bis.close();
		 * //close socket } catch(Exception e) { return false; } //
		 * System.out.println("Get method successful");
		 * local.TagsSet(remote.GetAllTags());
		 * local.SetAbsolutePath(localFile.getAbsolutePath());
		 * m_localRM.ItemAdd(local,true); return true;
		 */
	}
	public void Dispose()
	{
		try
		{
			m_oos.flush();
			m_oos.close();
			m_s.close();
		} catch (Exception e)
		{
		}
	}
	public File SuggestLocalPath(ItemRecord remote_ir)
	{
//		return new File(Utils.GetIncomingFolder() + "/"
//				+ (remote_ir).getFileName());
		return new File(Utils.GetUserFolder() + "/"
				+ (remote_ir).getFileName());
		
	}
	public File GetLocalPath(ItemRecord ir)
	{
		return ir.getFile();
	}
}
