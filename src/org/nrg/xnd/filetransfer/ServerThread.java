package org.nrg.xnd.filetransfer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.nrg.fileserver.RepositoryManager;

public abstract class ServerThread implements Runnable
{
	protected RepositoryManager m_localRM, m_remoteRM;
	protected Socket m_s;
	protected boolean m_bRunning = false;
	protected ObjectInputStream m_ois;
	protected ObjectOutputStream m_oos;

	public abstract Thread GetNewThread(Socket s, RepositoryManager local,
			RepositoryManager remote);
	public ServerThread(Socket s, RepositoryManager local,
			RepositoryManager remote)
	{
		m_s = s;
		m_localRM = local;
		m_remoteRM = remote;
	}
	protected void Dispose()
	{
		try
		{
			if (m_s != null)
				m_s.close();
			if (m_oos != null)
				m_oos.close();
			if (m_ois != null)
				m_ois.close();
		} catch (Exception e)
		{
		}
	}
	public void Stop()
	{
		m_bRunning = false;
	}
	@Override
	public abstract void run();

}
