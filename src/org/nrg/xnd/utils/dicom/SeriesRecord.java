package org.nrg.xnd.utils.dicom;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

public class SeriesRecord implements Comparable<SeriesRecord>
{
	private String m_SerInstUID, m_SerDescr;
	private LinkedList<SeriesElementRecord> m_files=new LinkedList<SeriesElementRecord>();
	private DicomObject m_dob;	
	public Collection<SeriesElementRecord> getElements(){return m_files;}
	@Override
	public int compareTo(SeriesRecord o)
	{
		return m_SerInstUID.compareTo(o.m_SerInstUID);
	}
	public DicomObject getDOB(){return m_dob;}
	public SeriesRecord(DicomObject dob)
	{
		String uid=dob.getString(Tag.SeriesInstanceUID);
		m_SerInstUID=(uid!=null)?uid:"default_series";
		m_SerDescr=dob.getString(Tag.SeriesDescription);
		m_dob=dob;
	}
	public void addFileToSeries(DicomObject dob, File f)
	{
		m_files.add(new SeriesElementRecord(this,dob,f));
	}
	public void sortSRs()
	{
		Collections.sort(m_files);
	}
}