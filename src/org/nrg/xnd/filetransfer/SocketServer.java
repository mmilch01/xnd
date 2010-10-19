package org.nrg.xnd.filetransfer;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.nrg.fileserver.RepositoryManager;
import org.nrg.xnd.utils.Utils;

public class SocketServer
{
	private RepositoryManager m_rm;
	private ServerSocket m_ss;
	private boolean m_bRunning = true;
	private final int m_maxConnections = 10;
	private int m_Port;
	private ThreadServer m_ts = null;
	public final static byte TYPE_RM = 0, TYPE_FM = 1;
	private byte m_Type;

	public SocketServer(RepositoryManager mgr, int port, byte type)
	{
		m_rm = mgr;
		m_Port = port;
		m_Type = type;
	}
	public void SetPort(int port)
	{
		m_Port = port;
	}
	public boolean Start()
	{
		if (m_ts == null)
		{
			m_ts = new ThreadServer();
			m_ts.start();
			return true;
		} else if (!m_ts.isAlive())
		{
			m_ts = new ThreadServer();
			m_ts.start();
			return true;
		} else
			return false;
	}
	public void Stop()
	{
		if (m_ts.isAlive())
		{
			m_ts.m_Stopped = true;
			try
			{
				Socket s = new Socket("localhost", m_Port);
				ObjectOutputStream oos = new ObjectOutputStream(s
						.getOutputStream());
				Utils.SerializeString(oos, "SessionEnd", false);
				oos.flush();
				s.close();
			} catch (Exception e)
			{
			}
		}
	}
	private class ThreadServer extends Thread
	{
		public boolean m_Stopped = false;
		@Override
		public void run()
		{
			ServerThread st = null;
			try
			{
				m_ss = new ServerSocket(m_Port);
				if (m_Type == TYPE_RM)
					st = new RepositoryServerThread(null, m_rm,
							(RepositoryManager) null);
				else if (m_Type == TYPE_FM)
					st = new FileServerThread(null, m_rm,
							(RepositoryManager) null);
			} catch (Exception e)
			{
				e.printStackTrace();
				return;
			}
			Thread t;
			Socket s = null;
			while (!m_Stopped)
			{
				try
				{
					s = m_ss.accept();
					t = st.GetNewThread(s, m_rm, null);
					t.start();
					while (t.isAlive())
						sleep(1000);
					s.close();
				} catch (Exception e)
				{
					e.printStackTrace();
					if (s != null)
					{
						try
						{
							s.close();
						} catch (Exception e1)
						{
						}
					}
					m_Stopped = true;
				}
			}
			try
			{
				m_ss.close();
			} catch (Exception e2)
			{
			}

		}
	} // end of ThreadServer class;
}