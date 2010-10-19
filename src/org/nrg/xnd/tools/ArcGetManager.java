package org.nrg.xnd.tools;

import java.io.File;

import org.nrg.xdat.webservices.ArcGetWS;
import org.nrg.xnd.app.ConsoleView;

public class ArcGetManager extends Thread
{
	private String m_usr;
	private String m_pass;
	private String m_srv;
	private String m_session;
	private File m_root;
	public ArcGetManager(String server, String user, String pass,
			String sessionID, File root)
	{
		m_srv = server;
		m_usr = user;
		m_pass = pass;
		m_session = sessionID;
		m_root = root;
	}
	public boolean IsValid()
	{
		return true;
	}
	@Override
	public void run()
	{
		ConsoleView.AppendMessage("Connecting to XNAT archive...");
		ArcGetWS ag = new ArcGetWS();
		String[] args = {"-u", m_usr, "-p", m_pass, "-host", m_srv, "-s",
				m_session, "-o", m_root.getAbsolutePath(), "-z", "true", "-di"};
		if (ag.perform(args))
			ConsoleView.AppendMessage("ArcGet: download complete");
		else
		{
			ConsoleView
					.AppendMessage("ArcGet: download failed. See Java console for details.");
			return;
		}
		ConsoleView.AppendMessage("Importing to local repository...");
		if (!GenerateTags())
			ConsoleView.AppendMessage("Import failed.");
		else
			ConsoleView.AppendMessage("Import complete.");
	}
	private boolean GenerateTags()
	{

		return true;
	}
}
