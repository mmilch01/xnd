/**
 * $Id: DICOMReader.java,v 1.3 2010/03/09 21:31:57 misha Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.xnd.rules.dicom;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.nrg.fileserver.ItemRecord;

/**
 * @update The code is modified from the original DICOM rule code
 * @author Kevin Archie <karchie@npg.wustl.edu>
 * 
 */
public final class DICOMReader
{
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.desktop.rules.Rule#GetUnaffectedRecords(java.util.Collection
	 * )
	 */
	private static final int DEFAULT_MAX_TAG = Tag.PixelData - 1;
	private static final String GZIP_SUFFIX = ".gz";

	public static DicomObject read(final ItemRecord item)
	{
		final String path = item.getAbsolutePath();
		if (null == path || path.length() < 1)
			return null;
		final File f = new File(path);
		return f.isFile() ? read(f, DEFAULT_MAX_TAG) : null;
	}
	/**
	 * Reads a DicomObject from the given File
	 * 
	 * @param f
	 *            DICOM data file
	 * @return A DicomObject containing the data, or null if the read fails.
	 */
	public static DicomObject read(final File f)
	{
		return read(f, DEFAULT_MAX_TAG);
	}

	private static DicomObject read(final File file, final int maxTag)
	{
		InputStream fin = null;
		BufferedInputStream bin = null;
		final DicomObject o;
		try
		{
			fin = new FileInputStream(file);
			if (file.getName().endsWith(GZIP_SUFFIX))
			{
				fin = new GZIPInputStream(fin);
			}
			bin = new BufferedInputStream(fin);
			final DicomInputStream in = new DicomInputStream(bin);
			in.setHandler(new StopTagInputHandler(maxTag + 1));
			o = in.readDicomObject();
		} catch (Exception e)
		{
			return null;
		} finally
		{
			if (bin != null)
				try
				{
					bin.close();
				} catch (IOException ignore)
				{
				}
			if (fin != null)
				try
				{
					fin.close();
				} catch (IOException ignore)
				{
				};
		}
		return o.contains(org.dcm4che2.data.Tag.SOPClassUID) ? o : null;
	}
}
