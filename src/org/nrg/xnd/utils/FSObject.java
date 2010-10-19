package org.nrg.xnd.utils;

import java.io.File;

public class FSObject extends File
{
	private boolean m_bIsDirSet = false;
	private boolean m_bIsDir = false;
	protected String m_RelativePath = null;
	public FSObject()
	{
		super("");
	}
	public String GetRelativePath()
	{
		return m_RelativePath;
	}
	public FSObject(File f, String rel_path)
	{
		super(f.getAbsolutePath());
		m_RelativePath = rel_path;
	}
	public FSObject(File f)
	{
		super(f.getAbsolutePath());
	}
	public FSObject(String path)
	{
		super(path);
	}
	public void SetIsDir(boolean bIsDir)
	{
		m_bIsDir = bIsDir;
		m_bIsDirSet = true;
	}
	public boolean IsDir_cached()
	{
		if (!m_bIsDirSet)
			m_bIsDir = isDirectory();
		return m_bIsDir;
	}
	public FSObject[] ListFiles(boolean bCheckDirs)
	{
		return ListFiles(getAbsolutePath(), bCheckDirs);
	}
	public static FSObject[] ListFiles(String abs_path, boolean bCheckDirs)
	{
		if (abs_path == null || abs_path.length() < 1)
			return new FSObject[0];
		File f = new File(abs_path);
		try
		{
			return Utils.m_nfm.ListFiles(f, bCheckDirs);
		} catch (Exception e)
		{
			return new FSObject[0];
		}
	}
}
