package org.nrg.xnd.utils.dicom;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.xnd.rules.dicom.Utils;
import org.nrg.xnd.utils.DRTemplate;

public class DICOMRecord
{
	public static final byte LevelPatient = 1;
	public static final byte LevelStudy = 2;
	public static final byte LevelSeries = 3;
	public static final byte LevelImage = 4;

	public static final byte iPatID = 0; // patient-unique, must be first
	public static final byte iPatName = 1;
	public static final byte iPatSex = 2;
	public static final byte iPatComments = 3;
	public static final byte iPatOtherIDs = 4;
	public static final byte iPatRes3 = 5;
	public static final byte iPatRes2 = 6;
	public static final byte iPatRes1 = 7;

	public static final byte iStInstUID = 8; // study-unique, must be first
	public static final byte iStAccessionNum = 9;
	public static final byte iStID = 10;
	public static final byte iStRefPhName = 11;
	public static final byte iStReadPhName = 12;
	public static final byte iStNumImages = 13;
	public static final byte iStDescr = 14;
	public static final byte iStModalities = 15;
	public static final byte iStRetrieveAE = 16;
	public static final byte iStAdmin = 17;
	public static final byte iStStatus = 18;
	public static final byte iStBodyParts = 19;
	public static final byte iStRes3 = 20;
	public static final byte iStRes2 = 21;
	public static final byte iStRes1 = 22;

	public static final byte iSerInstUID = 23; // series-unique, must be first
	public static final byte iSerModality = 24;
	public static final byte iSerNumber = 25;
	public static final byte iSerBodyPart = 26;
	public static final byte iSerDescr = 27;
	public static final byte iSerNumImages = 28;
	public static final byte iSerRes3 = 29;
	public static final byte iSerRes2 = 30;
	public static final byte iSerRes1 = 31;

	public static final byte iImgSOPInstUID = 32; // image-unique, must be first
	public static final byte iImgNumber = 33;
	public static final byte iImgType = 34;
	public static final byte iImgInstit = 35;
	public static final byte iImgComment = 36;
	public static final byte iImgRes3 = 37;
	public static final byte iImgRes2 = 38;
	public static final byte iImgRes1 = 39;

	public static final byte iFilename = 40; // must be last

	private Vector<String> m_Data = new Vector<String>(iFilename + 1);
	public String m_BirthTime = "";
	public String m_BirthDate = "";
	public String m_StudyTime = "";
	public String m_StudyDate = "";
	public int m_QLevel = 1;
	public int m_Quality = -1; // unspecified
	private int m_TokenPos = 0;
	public int m_iReviewed = -1;
	char m_CharBuffer[] = new char[255];

	public int GetIndexTag(int ind)
	{
		switch (ind)
		{
			case iPatID :
				return Tag.PatientID;
			case iPatName :
				return Tag.PatientName;
			case iPatSex :
				return Tag.PatientSex;
			case iPatComments :
				return Tag.PatientComments;
			case iStInstUID :
				return Tag.StudyInstanceUID;
			case iStAccessionNum :
				return Tag.AccessionNumber;
			case iStID :
				return Tag.StudyID;
			case iStRefPhName :
				return Tag.ReferringPhysicianName;
			case iStReadPhName:
				return 0x00081060;
			case iStNumImages :
				return Tag.ImagesInStudy;
			case iStDescr :
				return Tag.StudyDescription;
			case iStModalities :
				return Tag.ModalitiesInStudy;
			case iStRetrieveAE :
				return Tag.RetrieveAETitle;
			case iStAdmin :
				return (0x0008 << 16) & 0x1050;
			case iStStatus :
				return Tag.Status;

			case iSerInstUID :
				return Tag.SeriesInstanceUID;
			case iSerModality :
				return Tag.Modality;
			case iSerNumber :
				return Tag.SeriesNumber;
			case iSerBodyPart :
				return Tag.BodyPartExamined;
			case iSerDescr :
				return Tag.SeriesDescription;
			case iSerNumImages :
				return Tag.ImagesInSeries;

			case iImgSOPInstUID :
				return Tag.SOPInstanceUID;
			case iImgNumber :
				return Tag.ImageIndex;
			case iImgType :
				return Tag.ImageType;
			case iImgInstit :
				return Tag.InstitutionName;
			case iImgComment :
				return Tag.ImageComments;
		}
		return -1;
	}
	public boolean IsGuestAllowed()
	{
		String admins = GetString(iStAdmin);
		if (admins == null)
			return false;
		if (admins.indexOf("Guest", 0) >= 0 || admins.indexOf("guest", 0) >= 0)
			return true;
		else
			return false;
	}
	public String GetString(byte index)
	{
		if ((index + 1) > m_Data.size())
			return new String("");
		String str = m_Data.elementAt(index);
		if (str != null)
			str.replace('^', ' ');
		return (str != null) ? str : new String("");
	}
	public void SetExtraQueryInfo(int desiredQLevel, int desiredNumber)
	{
		int code = desiredQLevel * 1000 + desiredNumber;
		SetString(iPatRes1, Integer.toString(code));
	}
	public void ClearBelowLevel(int level)
	{
		int start = -1;
		switch (level)
		{
			case 1 :// PATIENT
				start = iStInstUID;
				break;
			case 2 :// STUDY
				start = iSerInstUID;
				break;
			case 3 :// SERIES
				start = iImgSOPInstUID;
				break;
		}
		if (start >= 0)
			for (int i = start; i < iFilename; i++)
				SetString((byte) i, "*");

	}
	public void Clear()
	{
		InitializeFromTemplate(new DRTemplate());
	}
	public void PrepareForQuery()
	{
		int clearStart;

		switch (m_QLevel)
		{
			case 1 : // patient
				clearStart = iPatID;
				m_BirthTime = "*";
				m_BirthDate = "*";
				m_StudyTime = "*";
				m_StudyDate = "*";
				break;
			case 2 : // study
				clearStart = iStInstUID;
				// m_StudyTime="*";
				// m_StudyDate="*";
				break;
			case 3 :
				clearStart = iSerInstUID;
				break;
			case 4 :
				clearStart = iImgSOPInstUID;
				break;
			default :
				return;
		}
		for (int i = clearStart; i < iFilename; i++)
			SetString((byte) i, "*");
	}
	public String FormatStudyDate()
	{
		if (m_StudyDate.length() < 8)
			return m_StudyDate;
		String res = "";// =m_StudyDate.substring(0,4);
		int mon = Integer.parseInt(m_StudyDate.substring(4, 6), 10);
		switch (mon)
		{
			case 1 :
				res = "Jan ";
				break;
			case 2 :
				res = "Feb ";
				break;
			case 3 :
				res = "Mar ";
				break;
			case 4 :
				res = "Apr ";
				break;
			case 5 :
				res = "May ";
				break;
			case 6 :
				res = "Jun ";
				break;
			case 7 :
				res = "Jul ";
				break;
			case 8 :
				res = "Aug ";
				break;
			case 9 :
				res = "Sep ";
				break;
			case 10 :
				res = "Oct ";
				break;
			case 11 :
				res = "Nov ";
				break;
			case 12 :
				res = "Dec ";
		}
		res = m_StudyDate.substring(6, 8) + " " + res
				+ m_StudyDate.substring(0, 4);
		return res;
	}
	public String FormatStudyTime()
	{
		if (m_StudyTime.length() < 6)
			return m_StudyTime;
		return m_StudyTime.substring(0, 2) + "h " + m_StudyTime.substring(2, 4)
				+ "m " + m_StudyTime.substring(4, 6) + "s";
	}
	public boolean MatchQueryDR(DICOMRecord dr)
	{
		if (m_Quality > -1 && dr.m_Quality > -1 && m_Quality != dr.m_Quality)
			return false;
		int res;
		res = ((m_Data.elementAt(iPatID))).compareTo(dr.GetString(iPatID));
		if (res != 0)
			return false;
		if (m_QLevel != dr.m_QLevel)
			return false;
		if (GetEntityIndex() != dr.GetEntityIndex())
			return false;
		if (GetNImages() != dr.GetNImages())
			return false;
		switch (m_QLevel)
		{
			case 4 : // image
				res = ((m_Data.elementAt(iSerInstUID))).compareTo(dr
						.GetString(iSerInstUID));
				return (res == 0);
			case 3 : // series
				res = ((m_Data.elementAt(iStInstUID))).compareTo(dr
						.GetString(iStInstUID));
				return (res == 0);
			default :
				return false;
		}
	}
	public boolean SetString(byte index, String str)
	{
		if ((index) > iFilename)
			return false;
		m_Data.setElementAt(str, index);
		return true;
	}
	public int GetDesiredQLevel()
	{
		int level = 0;
		try
		{
			level = (Integer.valueOf((m_Data.elementAt(iPatRes1)))).intValue();
			level = level / 1000;
		} catch (NumberFormatException e)
		{
			level = 0;
		}
		return level;
	}
	public int GetEntityIndex()
	{
		int index = -1;
		try
		{
			index = (Integer.valueOf((m_Data.elementAt(iPatRes1)))).intValue();
			index = index % 1000;
		} catch (NumberFormatException e)
		{
			index = 0;
		}
		return index;
	}
	public int GetEntitySize()
	{
		int size = 0;
		try
		{
			size = (Integer.valueOf((m_Data.elementAt(iPatRes2)))).intValue();
			size = size / 1000;
		} catch (NumberFormatException e)
		{
			size = 0;
		}
		return size;
	}
	public int GetNImages()
	{
		int n = 0;
		try
		{
			n = (Integer.valueOf((m_Data.elementAt(iPatRes2)))).intValue();
			n = n % 1000;
		} catch (NumberFormatException e)
		{
			n = 0;
		}
		return n;
	}
	public DICOMRecord()
	{
		InitializeFromTemplate(new DRTemplate());
	}
	public DICOMRecord(DICOMRecord dr)
	{
		if (dr != null)
			SetFromDR(dr);
		else
			InitializeFromTemplate(new DRTemplate());

	}
	public void initFromDOB(DicomObject dob)
	{
		m_Data.removeAllElements();
		int code;
		String val;
		Collection<String> col;
		for (byte i = 0; i <= iPatOtherIDs; i++)
			add(dob,GetIndexTag(i));
		addRes(3);

		for (byte i = iStInstUID; i <= iStBodyParts; i++)
			add(dob,GetIndexTag(i));		
		addRes(3);
		
		for (byte i = iSerInstUID; i <= iSerNumImages; i++)
			add(dob,GetIndexTag(i));		
		addRes(3);
		
		for (byte i = iImgSOPInstUID; i <= iImgComment; i++)
			add(dob,GetIndexTag(i));
		addRes(4);
		
		String s;
		s=dob.getString(Tag.StudyDate);
		m_StudyDate=(s!=null)?s:"";
		s=dob.getString(Tag.StudyTime);
		m_StudyTime=(s!=null)?s:"";
		s=dob.getString(Tag.PatientBirthDate);
		m_BirthDate=(s!=null)?s:"";
		s=dob.getString(Tag.PatientBirthTime);
		m_BirthTime=(s!=null)?s:"";
		correctForMissingUIDValues();
	}
	private void correctForMissingUIDValues()
	{
		if (GetString(iPatID).length()<1) SetString(iPatID, "default_patient");
		if (GetString(iStInstUID).length()<1) SetString(iStInstUID, "default_study");
		if (GetString(iSerInstUID).length()<1) SetString(iSerInstUID, "default_series");
		if (GetString(iImgSOPInstUID).length()<1) SetString(iImgSOPInstUID,"default_image");
	}
	private void addRes(int n){for(int i=0;i<n;i++) m_Data.add("");}
	private void add(DicomObject dob, int id)
	{
		String s=dob.getString(id);
		m_Data.add((s!=null)?s:"");
	}
	
	public void InitializeFromTemplate(DRTemplate template)
	{
		if (template == null)
			return;
		m_Data.removeAllElements();
		m_StudyDate = template.m_StudyDate;
		for (int i = 0; i <= iFilename; i++)
		{
			if (i == iPatID && template.m_PatientID != null)
				m_Data.addElement(template.m_PatientID);
			else if (i == iPatName && template.m_PatientName != null)
				m_Data.addElement(template.m_PatientName);
			else if (i == iStID && template.m_StudyID != null)
				m_Data.addElement(template.m_StudyID);
			else if (i == iStInstUID && template.m_StudyInstUID != null)
				m_Data.addElement(template.m_StudyInstUID);
			else if (i == iStModalities && template.m_Modality != null)
				m_Data.addElement(template.m_Modality);
			else if (i == iStAccessionNum && template.m_AccessionNum != null)
				m_Data.addElement(template.m_AccessionNum);
			else if (i == iPatRes1 && template.m_PatRes1 != null)
				m_Data.addElement(template.m_PatRes1);
			else
				m_Data.addElement(new String(""));
		}
	}
	public void SetUIDFieldsFromDR(DICOMRecord dr)
	{
		SetString(iPatID, dr.GetString(iPatID));
		SetString(iStAccessionNum, dr.GetString(iStAccessionNum));
		SetString(iStInstUID, dr.GetString(iStInstUID));
		SetString(iSerInstUID, dr.GetString(iSerInstUID));
		SetString(iImgSOPInstUID, dr.GetString(iImgSOPInstUID));
	}
	public void SetFromDR(DICOMRecord dr)
	{
		if (dr == null)
			return;
		m_BirthDate = dr.m_BirthDate;
		m_BirthTime = dr.m_BirthTime;
		m_QLevel = dr.m_QLevel;
		m_Quality = dr.m_Quality;
		m_StudyDate = dr.m_StudyDate;
		m_StudyTime = dr.m_StudyTime;
		m_Data.removeAllElements();
		for (int i = 0; i < iFilename; i++)
			m_Data.addElement(dr.GetString((byte) i));
	}
	public int SetFromXML(String xml)
	{
		DRTemplate template = new DRTemplate();
		int nValues = template.SetFromXML(xml);
		InitializeFromTemplate(template);
		return nValues;
	}
	private String GetNextToken(String token, String source)
	{
		String res = "";
		String ch;
		try
		{
			ch = source.substring(m_TokenPos, m_TokenPos + 1);
			m_TokenPos++;
		} catch (Exception e)
		{
			return res;
		}
		while (ch.charAt(0) != token.charAt(0))
		{
			res = res + ch;
			ch = source.substring(m_TokenPos, m_TokenPos + 1);
			m_TokenPos++;
		}
		return res;
	}
	private String Decode(String str)
	{
		String decoded = "";
		char ch;
		for (int i = 0; i < str.length(); i++)
		{
			if ((i < str.length() - 1)
					&& (str.substring(i, i + 2).compareTo("&#") == 0))
			{
				ch = (char) Integer.valueOf(str.substring(i + 2, i + 6), 16)
						.intValue();
				decoded = decoded + ch;
				i += 5;
			} else
				decoded = decoded + str.charAt(i);
		}
		return decoded;
	}
	public boolean FromString(String src)
	{
		String token = "" + (char) 1;
		String str;
		m_TokenPos = 0;
		try
		{
			m_QLevel = (byte) (new Integer(GetNextToken(token, src)).intValue());
			m_Data.removeAllElements();
			for (int i = 0; i <= iFilename; i++)
			{
				str = GetNextToken(token, src);
				if (i != iStDescr)
					m_Data.addElement(str);
				else
					m_Data.addElement(Decode(str));
			}
			m_BirthTime = GetNextToken(token, src);
			m_BirthDate = GetNextToken(token, src);
			m_StudyTime = GetNextToken(token, src);
			m_StudyDate = GetNextToken(token, src);
		} catch (Exception e)
		{
			return false;
		}
		return true;
	}
	public String ToString()
	{
		String res = "";
		char token = (char) 1;
		res += Integer.toString(m_QLevel) + token;
		try
		{
			boolean empty = false;
			if (m_Data.size() < iFilename)
				empty = true;
			for (int i = 0; i <= iFilename; i++)
			{
				if (!empty && i < iFilename)
					res += (m_Data.elementAt(i));
				else
					res += "";
				res += token;
			}
		} catch (Exception e)
		{
			return null;
		}
		res += m_BirthTime + token;
		res += m_BirthDate + token;
		res += m_StudyTime + token;
		res += m_StudyDate;
		return res;
	}
}// end of class DICOMRecord.
