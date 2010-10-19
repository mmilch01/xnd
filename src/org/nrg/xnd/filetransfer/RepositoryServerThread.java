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

public class RepositoryServerThread extends ServerThread
{
	@Override
	public Thread GetNewThread(Socket s, RepositoryManager local,
			RepositoryManager remote)
	{
		return new Thread(new RepositoryServerThread(s, local, remote));
	}
	public RepositoryServerThread(Socket s, RepositoryManager local,
			RepositoryManager remote)
	{
		super(s, local, remote);
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
			ItemRecord ir_var;
			int int_var;
			boolean bool_var;
			try
			{
				if (m_ois == null)
					m_ois = new ObjectInputStream(new BufferedInputStream(m_s
							.getInputStream()));
				method_name = Utils.SerializeString(m_ois, null, true);
				if (method_name.compareTo("DBItemFind") == 0)
				{
					ir_var = Utils.SerializeItemRecord(m_ois, null, true);
					int_var = m_ois.readInt();
					ItemRecord[] records = m_localRM.DBItemFind(ir_var,
							int_var, true);
					m_oos.writeInt(records.length);
					if (records.length > 0)
					{
						for (int i = 0; i < records.length; i++)
							Utils.SerializeItemRecord(m_oos, records[i], false);
					}
					m_oos.flush();
				} else if (method_name.compareTo("DBTagAttach") == 0)
				{
					ir_var = Utils.SerializeItemRecord(m_ois, null, true);
					tag_var = Utils.SerializeTag(m_ois, null, true);
					bool_var = m_localRM.DBTagAttach(ir_var, tag_var);
					m_oos.writeBoolean(bool_var);
					m_oos.flush();
				} else if (method_name.compareTo("DBTagDetach") == 0)
				{
					ir_var = Utils.SerializeItemRecord(m_ois, null, true);
					tag_var = Utils.SerializeTag(m_ois, null, true);
					bool_var = m_localRM.DBTagDetach(ir_var, tag_var);
					m_oos.writeBoolean(bool_var);
					m_oos.flush();
				} else if (method_name.compareTo("DBTagFind") == 0)
				{
					String name = Utils.SerializeString(m_ois, null, true);
					String[] found = m_localRM.DBTagFind(name);
					m_oos.writeInt(found.length);
					if (found.length > 0)
					{
						for (int i = 0; i < found.length; i++)
							Utils.SerializeString(m_oos, found[i], false);
					}
					m_oos.flush();
				} else if (method_name.compareTo("NOOP") == 0) // test method
				{
					method_name = "";
				}
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