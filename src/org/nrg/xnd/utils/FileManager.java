package org.nrg.xnd.utils;

import java.io.File;

class FileManager
{
	public FSObject[] ListFiles(File f, boolean bCheckIfDir)
	{
		FSObject[] fos;
		File[] files = f.listFiles();
		fos = new FSObject[files.length];
		for (int i = 0; i < fos.length; i++)
		{
			fos[i] = new FSObject(files[i]);
		}
		if (bCheckIfDir)
		{
			for (FSObject cff : fos)
				cff.IsDir_cached();
		}
		return fos;
	}
	public boolean IsDirectory(File f)
	{
		return f.isDirectory();
	}
}