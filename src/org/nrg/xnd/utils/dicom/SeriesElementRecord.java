package org.nrg.xnd.utils.dicom;

import java.io.File;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

public class SeriesElementRecord implements Comparable<SeriesElementRecord>
{
	public File m_file;
	public DicomObject m_dob;
	private SeriesRecord m_ser;
	private int m_instNum=-1;
	public SeriesElementRecord(SeriesRecord parent, DicomObject dob, File f)
	{
		m_file=f; m_dob=dob; m_ser=parent;
		m_instNum=m_dob.getInt(Tag.InstanceNumber);
	}
	@Override
	public int compareTo(SeriesElementRecord o)
	{
		
		if (m_instNum<o.m_instNum) return -1;
		return (m_instNum==o.m_instNum)?0:1;
	}
}
