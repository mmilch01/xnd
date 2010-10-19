package org.nrg.xnd.utils;

import java.io.File;
import java.util.LinkedList;

import com.sun.jna.Native;
import com.sun.jna.Structure;

public class NativeFileManager extends FileManager
{
	private FileManager m_fm;

	public NativeFileManager()
	{
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows"))
			m_fm = new W32FileManager();
		else
			m_fm = new FileManager();
	}

	@Override
	public FSObject[] ListFiles(File f, boolean bCheckIfDir)
	{
		return m_fm.ListFiles(f, bCheckIfDir);
	}
	@Override
	public boolean IsDirectory(File f)
	{
		return m_fm.IsDirectory(f);
	}

	/*
	 * public static void main(String[] args) { File f=new
	 * File("U:\\cnda_upload\\sa10908\\Scan1"); NativeFileManager nfm=new
	 * NativeFileManager(); try { File[] files=nfm.ListFiles(f); for(File
	 * ch:files) {
	 * System.out.println(ch.getAbsolutePath()+", file"+(ch.exists()?
	 * " exists":"does not exist.")); } } catch(Exception e) {
	 * System.out.println(e.getMessage()); } }
	 */
	public static final int FILE_ATTRIBUTE_DIRECTORY = 0x00000010;
	private static final int MAX_PATH = 0x00000104;

	public class FILETIME extends Structure
	{
		public int dwLowDateTime;
		public int dwHighDateTime;
	};
	public class WIN32_FIND_DATA extends Structure
	{
		public int dwFileAttributes;
		public long ftCreationTime;
		public long ftLastAccessTime;
		public long ftLastWriteTime;
		public int nFileSizeHigh;
		public int nFileSizeLow;
		public int dwReserved0;
		// public short dwReserved1;
		public char[] cFileName = new char[MAX_PATH];
		// public String cFileName = new String(new byte[MAX_PATH]);
		public char[] cAlternateFileName = new char[14];
		// public String cAlternateFileName = new String(new byte[14]);
	};

	public interface Kernel32 extends W32API
	{
		public Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32",
				Kernel32.class, DEFAULT_OPTIONS);

		public HANDLE FindFirstFile(String lpFileName,
				WIN32_FIND_DATA lpFindFileData);
		public boolean FindNextFile(HANDLE hFindFile,
				WIN32_FIND_DATA lpFindFileData);
		public boolean FindClose(HANDLE hFindFile);
	};

	private class W32FileManager extends FileManager
	{
		Kernel32 m_k32 = Kernel32.INSTANCE;

		@Override
		public FSObject[] ListFiles(File f, boolean bCheckIfDir)
		{
			WIN32_FIND_DATA wfd = new WIN32_FIND_DATA();

			Kernel32.HANDLE hFind = m_k32.FindFirstFile(f.getAbsolutePath()
					+ "\\*", wfd);
			if (W32API.INVALID_HANDLE_VALUE.equals(hFind))
				return new FSObject[0];
			LinkedList<FSObject> cf = new LinkedList<FSObject>();
			String basepath = f.getAbsolutePath() + "/";
			FSObject fso;
			do
			{
				fso = new FSObject(basepath + ExtractString(wfd.cFileName));
				if (bCheckIfDir)
					fso
							.SetIsDir((wfd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0);
				cf.add(fso);

			} while (m_k32.FindNextFile(hFind, wfd));
			m_k32.FindClose(hFind);
			cf.removeFirst();
			cf.removeFirst();

			return cf.toArray(new FSObject[0]);
		}
		private String ExtractString(char[] array)
		{
			for (int i = 0; i < array.length; i++)
			{
				if (array[i] == 0)
					return new String(array, 0, i);
			}
			return new String(array);
		}
		@Override
		public boolean IsDirectory(File f)
		{
			try
			{
				WIN32_FIND_DATA wfd = new WIN32_FIND_DATA();
				Kernel32.HANDLE hFind = m_k32.FindFirstFile(
						f.getAbsolutePath(), wfd);
				if (W32API.INVALID_HANDLE_VALUE.equals(hFind))
					return false;
				m_k32.FindClose(hFind);
				return (wfd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0;
			} catch (Exception e)
			{
				return false;
			}
		}
	};
}