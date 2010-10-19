package org.nrg.xnd.filetransfer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;

import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.RepositoryManager;
import org.nrg.xnd.utils.Utils;

public class RemoteRepositoryClient extends RepositoryManager
{
	private Socket m_socket;
	private boolean m_bConn = false;
	private ObjectInputStream m_ois;
	private ObjectOutputStream m_oos;

	// Database initialization/deinitialization
	/**
	 * Called by the framework for connecting to , as required for subsequent
	 * execution of DB-related functions (which start with DB prefix).
	 * 
	 * @return true if DB was initialized successfully.
	 */
	@Override
	public boolean SessionInit(Vector init_params)
	{
		String host = (String) (init_params.elementAt(0));
		int port = ((Integer) (init_params.elementAt(1))).intValue();
		try
		{
			m_socket = new Socket(InetAddress.getByName(host), port);
			m_oos = new ObjectOutputStream(new BufferedOutputStream(m_socket
					.getOutputStream()));
			m_ois = null;
		} catch (Exception e)
		{
			return false;
		}
		m_bConn = true;
		TestFunction();
		return true;
	};

	/**
	 * Closes connection with database.
	 * 
	 * @return true if connection was closed properly; false if there was a
	 *         timeout/resource conflict problem.
	 */
	@Override
	public boolean SessionEnd()
	{
		if (m_socket == null)
			return true;
		if (m_socket.isConnected())
		{
			try
			{
				m_socket.close();
				m_oos.close();
				m_ois.close();
			} catch (Exception e)
			{
			}
		}
		return true;
	}

	// public item manipulation
	/**
	 * Removes all matching records from the database.
	 * 
	 * @param template
	 *            Specifies item record(s) to be removed from repository.
	 * @return
	 */

	@Override
	public boolean DBItemAdd(ItemRecord item)
	{
		// not supported for remote repository
		/*
		 * boolean res=false; try { ObjectOutputStream oos=new
		 * ObjectOutputStream(m_socket.getOutputStream());
		 * Utils.SerializeString(oos, "DBItemAdd", false);
		 * Utils.SerializeItemRecord(oos, item, false); ObjectInputStream
		 * ois=new ObjectInputStream(m_socket.getInputStream());
		 * res=ois.readBoolean(); } catch(Exception e){return false;} return
		 * res;
		 */
		return false;
	}

	@Override
	public ItemRecord[] DBItemFind(ItemRecord template, int maxrecords,
			boolean bAttachMetadata)
	{
		ItemRecord[] res;
		try
		{
			Utils.SerializeString(m_oos, "DBItemFind", false);
			Utils.SerializeItemRecord(m_oos, template, false);
			m_oos.writeInt(maxrecords);
			m_oos.flush();
			if (m_ois == null)
				m_ois = new ObjectInputStream(new BufferedInputStream(m_socket
						.getInputStream()));
			int nRec = m_ois.readInt();
			if (nRec > 0)
			{
				res = new ItemRecord[nRec];
				for (int i = 0; i < nRec; i++)
					res[i] = Utils.SerializeItemRecord(m_ois, null, true);
			} else
				res = new ItemRecord[0];
		} catch (Exception e)
		{
			return new ItemRecord[0];
		}
		return res;
	}

	@Override
	public boolean DBTagAdd(String name)
	{
		// not supported for remote
		return false;
	}

	@Override
	public boolean DBTagAttach(ItemRecord item, ItemTag tag)
	{
		boolean res = false;
		try
		{
			Utils.SerializeString(m_oos, "DBTagAttach", false);
			Utils.SerializeItemRecord(m_oos, item, false);
			Utils.SerializeTag(m_oos, tag, false);
			m_oos.flush();
			if (m_ois == null)
				m_ois = new ObjectInputStream(new BufferedInputStream(m_socket
						.getInputStream()));
			res = m_ois.readBoolean();
		} catch (Exception e)
		{
			return false;
		}
		return res;
	}

	@Override
	public boolean DBTagDelete(String name)
	{
		// not supported for remote
		return false;
	}
	@Override
	public boolean DBTagDetach(ItemRecord item, ItemTag tag)
	{
		boolean res = false;
		try
		{
			Utils.SerializeString(m_oos, "DBTagDetach", false);
			Utils.SerializeItemRecord(m_oos, item, false);
			Utils.SerializeTag(m_oos, tag, false);
			m_oos.flush();
			if (m_ois == null)
				m_ois = new ObjectInputStream(new BufferedInputStream(m_socket
						.getInputStream()));
			res = m_ois.readBoolean();
		} catch (Exception e)
		{
			return false;
		}
		return res;
	}

	@Override
	public String[] DBTagFind(String name)
	{
		String[] res;
		try
		{
			Utils.SerializeString(m_oos, "DBTagFind", false);
			Utils.SerializeString(m_oos, name, false);
			m_oos.flush();
			if (m_ois == null)
				m_ois = new ObjectInputStream(new BufferedInputStream(m_socket
						.getInputStream()));
			int nRec = m_ois.readInt();
			if (nRec > 0)
			{
				res = new String[nRec];
				for (int i = 0; i < nRec; i++)
					res[i] = Utils.SerializeString(m_ois, null, true);
			} else
				res = new String[0];
		} catch (Exception e)
		{
			return new String[0];
		}
		return res;
	}

	public void TestFunction()
	{
		try
		{
			Utils.SerializeString(m_oos, "NOOP", false);
			m_oos.flush();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	@Override
	public boolean ItemRemove(ItemRecord template)
	{
		// may support if requested in the future
		return false;
	}

	public boolean Get(ItemRecord local, ItemRecord remote)
	{
		// will support in the future
		return false;
	}

	public boolean Put(ItemRecord local, ItemRecord remote)
	{
		// will support in the future
		return false;
	}
}
