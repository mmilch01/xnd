package org.nrg.xnd.utils.dicom;

import java.io.File;
import java.util.Collection;
import java.util.TreeMap;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

public class StudyRecord implements Comparable<StudyRecord>
{
	public String m_PatName;
	public String m_PatID;
	public String m_Descr;
	public String m_StInstUID;
	public String m_Date;
	public DicomObject m_dob=null;
	private TreeMap<String,SeriesRecord> m_series=new TreeMap<String,SeriesRecord>();

	public Collection<SeriesRecord> getSeries()
	{
		return m_series.values();
	}
	public void addFileToStudy(DicomObject dob, File f)
	{
		if(dob==null) return;
		String s=dob.getString(Tag.SeriesInstanceUID);
		//put all non-series images to a 'default' series
		if(s==null || s.length()<1)
		{
			s="default_series";
		}
		SeriesRecord sr=m_series.get(s);
		if(sr==null)
		{
			sr=new SeriesRecord(dob);
			m_series.put(s,sr);
		}
		sr.addFileToSeries(dob, f);
	}		
	@Override
	public int compareTo(StudyRecord arg0)
	{
		return m_StInstUID.compareTo(arg0.m_StInstUID);
	}
	public StudyRecord(DicomObject dob)
	{
		m_PatName=dob.getString(Tag.PatientName);
		m_PatID=dob.getString(Tag.PatientID);
		m_Descr=dob.getString(Tag.StudyDescription);
		m_StInstUID=dob.getString(Tag.StudyInstanceUID);
		m_Date=dob.getString(Tag.StudyDate);
		m_dob=dob;
	}
}