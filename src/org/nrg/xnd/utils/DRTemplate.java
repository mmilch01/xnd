package org.nrg.xnd.utils;

public class DRTemplate
{
	public String m_PatientID, m_PatientName, m_StudyID, m_StudyDate,
			m_StudyInstUID, m_Modality, m_AccessionNum, m_SerInstUID,
			m_ImageSOPInstUID, m_PatRes1;
	public DRTemplate()
	{
		Clear();
	}
	private void Clear()
	{
		m_PatientID = "";
		m_PatientName = "";
		m_StudyID = "";
		m_StudyDate = "";
		m_StudyInstUID = ""; // primary key.
		m_Modality = "";
		m_AccessionNum = "";
		m_SerInstUID = "";
		m_ImageSOPInstUID = "";
	}
	public int SetFromXML(String xml)
	{
		LightXML parser = new LightXML();
		parser.AttachParamString(xml);
		return SetFromXML(parser);
	}
	public int SetFromXML(LightXML parser)
	{
		int nValues = 0;
		String val;
		if ((val = parser.GetStringValue("patid")) != null)
		{
			m_PatientID = val;
			nValues++;
		}
		if ((val = parser.GetStringValue("staccessionnum")) != null)
		{
			m_AccessionNum = val;
			nValues++;
		}
		if ((val = parser.GetStringValue("studydate")) != null)
		{
			m_StudyDate = val;
			nValues++;
		}
		if ((val = parser.GetStringValue("patres1")) != null)
		{
			m_PatRes1 = val;
			System.out.println("iPatRes1=" + val);
			nValues++;
		}
		return nValues;
	}
} // end of class DRTemplate
