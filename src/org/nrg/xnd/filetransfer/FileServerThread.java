package org.nrg.xnd.filetransfer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.RepositoryManager;
import org.nrg.xnd.utils.Utils;

public class FileServerThread extends ServerThread
{
	private SocketItemManager m_sim;
	@Override
	public Thread GetNewThread(Socket s, RepositoryManager local,
			RepositoryManager remote)
	{
		return new Thread(new FileServerThread(s, local, remote));
	}

	public FileServerThread(Socket s, RepositoryManager local,
			RepositoryManager remote)
	{
		super(s, local, remote);
		m_sim = new SocketItemManager(s, local, remote);
	}
	@Override
	public void run()
	{
		m_bRunning = true;
		try
		{
			m_ois = null;
			m_oos = new ObjectOutputStream(new BufferedOutputStream(m_s
					.getOutputStream()));
		} catch (Exception e)
		{
			e.printStackTrace();
			m_bRunning = false;
			Dispose();
			return;
		}
		while (m_s.isConnected() && m_bRunning)
		{
			String method_name = "";
			ItemTag tag_var;
			ItemRecord ir_var1, ir_var2;
			int int_var;
			boolean bool_var;
			try
			{
				if (m_ois == null)
					m_ois = new ObjectInputStream(new BufferedInputStream(m_s
							.getInputStream()));
				method_name = Utils.SerializeString(m_ois, null, true);
				if (method_name.compareTo("Get") == 0)
				{
					ir_var1 = Utils.SerializeItemRecord(m_ois, null, true);
					ir_var2 = Utils.SerializeItemRecord(m_ois, null, true);
					// m_sim.Put(ir_var1,ir_var2,false);
				} else if (method_name.compareTo("Put") == 0)
				{
					ir_var1 = Utils.SerializeItemRecord(m_ois, null, true);
					ir_var2 = Utils.SerializeItemRecord(m_ois, null, true);
					// m_sim.Get(ir_var1,ir_var2,false);
				} else if (method_name.compareTo("noop") == 0)
					continue; // empty operation
				// the only other input could be the terminating sequence.
				else
					m_bRunning = false;
			} catch (Exception exx)
			{
				// exx.printStackTrace();
				Dispose();
				m_bRunning = false;
				return;
			}
		}
		Dispose();
		m_bRunning = false;
	}
}
