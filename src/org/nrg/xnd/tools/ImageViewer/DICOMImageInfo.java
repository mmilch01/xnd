package org.nrg.xnd.tools.ImageViewer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.imageio.plugins.dcm.DicomImageReadParam;
import org.dcm4che2.imageioimpl.plugins.dcm.DicomImageReader;
import org.nrg.xnd.utils.LightXML;
import org.nrg.xnd.utils.dicom.DICOMRecord;

public class DICOMImageInfo
{
	private DicomObject m_dob;
	public DICOMImageInfo(DicomObject dob)
	{
		m_dob=dob;
	}
	public void setImageReadParams()
	{					
		int bitsStored=m_dob.getInt(Tag.BitsStored),
		samplesPerPixel=m_dob.getInt(Tag.SamplesPerPixel);		
		String phm=m_dob.getString(Tag.PhotometricInterpretation);

		
		DicomImageReadParam params=null;
		try
		{
			Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");			
			DicomImageReader r=(DicomImageReader)iter.next();
			params = (DicomImageReadParam) r.getDefaultReadParam();
		}
		catch(Exception e)
		{			
			System.err.println(e.getMessage());
			return;
		}
/*		
		
		if(samplesPerPixel==3 && bitsStored==8) //RGB
		{
			byte[] ind=new byte[256];
			for(int i=0; i<256; i++) ind[i]=(byte)(i & 0xff);
			return ImageTypeSpecifier.createIndexed(ind, ind, ind, ind, 8, DataBuffer.TYPE_INT);
		}
		if(bitsStored==16 && phm.compareTo("MONOCHROME2")==0)
		{
			return ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_SHORT, false);
		}
		else
		{
			return ImageTypeSpecifier.createGrayscale(8, DataBuffer.TYPE_BYTE, false);
		}
		Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
		ImageReader reader=null;
		try
		{
			DicomImageReader r;
			reader= iter.next();
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
		DICOMImageInfo dim=new DICOMImageInfo(ser.m_dob);

        ImageInputStream iis=null;         
        BufferedImage bi=null;
        DicomImageReadParam param = dim.getReadParams(); 
            (DicomImageReadParam) reader.getDefaultReadParam();        
        param.setDestinationType(dim.getDestinationType());
*/               
	}
	public LightXML getImageInfo()
	{
		LightXML xml=new LightXML();
		String[] s=new String[4];
		String[] ss=new String[4];
		getPatientOrientation(ss);
		for(int i=0; i<4; i++){ s[i]=""; ss[i]="";}
		String s1,s2;
		String[] sa=getTA(Tag.PatientName,Tag.Modality,Tag.StudyDate);
		sa[2]=formatDate(sa[2]);
		s[0]=addS(s[0],formS(getSA("","",""),sa));
		s[0]=addS(s[0],formS(getSA("ID","Acc. num"),getTA(Tag.PatientID,Tag.AccessionNumber)));
		s[0]=addS(s[0],formS(getSA("Born","Sex"),getTA(Tag.PatientBirthDate,Tag.PatientSex)));
		s[0]=addS(s[0],formS(getSA("Study"),getTA(Tag.StudyDescription)));
		s[0]=addS(s[0],formS(getSA("Series"),getTA(Tag.SeriesDescription)));
		s[0]=addS(s[0],formS(getSA(""),getTA(Tag.BodyPartExamined)));
		s[0]=addS(s[0],formS(getSA("Ref. Phys."),getTA(Tag.ReferringPhysicianName)));
		s[0]=addS(s[0],formS(getSA("Perf. Phys."),getTA(Tag.PerformingPhysicianName)));
		s[0]=addS(s[0],formS(getSA("Read. Phys."),getTA(0x00081060)));
		s[0]=addS(s[0],formS(getSA("Req. Phys."),getTA(Tag.RequestingPhysician)));
		s[0]=addS(s[0],formS(getSA("Dev.","Stn."),getTA(Tag.DeviceDescription,Tag.StationName)));
		s[0]=addS(s[0],formS(getSA("Hosp."),getTA(Tag.InstitutionName)));
		s[0]=addS(s[0],formS(getSA("Hist."),getTA(Tag.AdditionalPatientHistory)));
		
		//image time
		s1=m_dob.getString(0x00080033);
		if(s1==null) s1=m_dob.getString(0x00080032);
		s2=m_dob.getString(0x00080023);
		if(s2==null) s2=m_dob.getString(0x00080022);
		s1=formatTime(s1);
		s2=formatDate(s2);
		s[1]=addS(s[1],formS(getSA("Img. time",""),getSA(s1,s2)));
		//image type
		s[1]=addS(s[1],formS(getSA("Img. type"),getTA(0x00080008)));
		//image laterality
		s1=m_dob.getString(0x00200062); if(s1==null) s1=m_dob.getString(0x00200060);
		s[1]=addS(s[1],formS(getSA("Laterality"),getSA(s1)));
		//patient position and orientation
		s[1]=addS(s[1],formS(getSA("Patient orient."),getTA(0x00200020)));
		s[1]=addS(s[1],formS(getSA("Patient pos."),getTA(0x00185100)));
		s[1]=addS(s[1],formS(getSA("View pos."),getTA(0x00185101)));
		//conversion type
		s[1]=addS(s[1],formS(getSA("Conv. type"),getTA(0x00080064)));
		//image contrast
		s[1]=addS(s[1],formS(getSA("Contrast"),getTA(0x00180010)));
		//scanning sequence
		s[1]=addS(s[1],formS(getSA("Scanning seq."),getTA(0x00180020)));
		//sequence variant
		s[1]=addS(s[1],formS(getSA("Seq. variant"),getTA(0x00180021)));
		//scan options
		s[1]=addS(s[1],formS(getSA("Scan options"),getTA(0x00180022)));
		//radiation dose
		s1=m_dob.getString(0x00181155);
		if(s1!=null)
		{
			if(s1.compareTo("SC")==0)
			{
				s[1]=addS(s[1],formS(getSA("Radiation dose"),getSA("low")));
			}
			else if(s1.compareTo("GR")==0)
			{
				s[1]=addS(s[1],formS(getSA("Radiation dose"),getSA("high")));
			}
			else
			{
				s[1]=addS(s[1],formS(getSA("Radiation"),getSA(s1)));
			}
		}	
		//Frame duration
		s[1]=addS(s[1],formS(getSA("Frame durat. (msec)"),getTA(0x00181242)));
		//KVP
		s[1]=addS(s[1],formS(getSA("KVP (kV)"),getTAf(1,0x00180060)));
		//teslas		
		s[1]=addS(s[1],formS(getSA("Magnet (TL)"),getTAf(2,0x00180087)));
		//repetition time, echo time, train length
		s1=formatFloat(1,0x00180080);
		s2=formatFloat(1,0x00180081);		
		s[1]=addS(s[1],formS(getSA("TR (ms)","TE (ms)", "ET"),getSA(s1,s2,m_dob.getString(0x00180091))));		
		//Echo count
		s[1]=addS(s[1],formS(getSA("Event counts"),getTA(0x00180070)));
		//exposure
		s[1]=addS(s[1],formS(getSA("Exposure (mAs)"),getTA(0x00181152)));
		//exposure time
		s[1]=addS(s[1],formS(getSA("Exposure time (msec)"),getTA(0x00181150)));
		//tilt
		s[1]=addS(s[1],formS(getSA("Tilt (deg.)"),getTAf(1,0x00181120)));
		//IVUS Acquisition
		s[1]=addS(s[1],formS(getSA("IVUS asq."),getTA(0x00183100)));
		// Physical units in X direction
		int u=m_dob.getInt(0x00186024);
		
		switch(u)
		{
			case 0x0001: s1="%";
			case 0x0002: s1="dB";
			case 0x0003: s1="cm";
			case 0x0004: s1="sec";
			case 0x0005: s1="Hz";
			case 0x0006: s1="dB/sec";
			case 0x0007: s1="cm/sec";
			case 0x0008: s1="cm2";
			case 0x0009: s1="cm2/sec";
			case 0x000a: s1="cm3";
			case 0x000b: s1="cm3/sec";
			case 0x000c: s1="deg.";
			default: u=0;
		}
		if(u>0)
		{
			s[1]=addS(s[1],formS(getSA("Units in X dir."),getSA(s1)));			
		}
		//flip angle
		s[1]=addS(s[1],formS(getSA("Flip angle"),getTAf(1,0x00181314)));
		//slice location
		s[1]=addS(s[1],formS(getSA("Slice location (mm)"),getTAf(2,0x00201041)));
		//slice thckn and spacing		
		s[1]=addS(s[1],formS(getSA("Slice thickness (mm)"),getTAf(2,Tag.SliceThickness)));
		s[1]=addS(s[1],formS(getSA("Slice spacing (mm)"),getTAf(2,Tag.SpacingBetweenSlices)));
		s[1]=addS(s[1],formS(getSA("Bit depth"),getTA(Tag.BitsStored)));
			
		xml.AddValue("ii1", s[0]);
		xml.AddValue("ii2", s[1]);
		xml.AddValue("ii3", s[2]);
		xml.AddValue("ii4", s[3]);
		xml.AddValue("ss1", ss[0]);
		xml.AddValue("ss2", ss[1]);
		xml.AddValue("ss3", ss[2]);
		xml.AddValue("ss4", ss[3]);
		return xml;
	}
	/**
	 * Format time in human-readable form from DICOM time
	 * @param dcmTime DICOM time, hhmmss
	 * @return human time hh:mm:ss
	 */
	private String formatTime(String dcmTime)
	{
		try
		{
			if(dcmTime.length()<6) return dcmTime;
			int h=Integer.valueOf(dcmTime.substring(0,2)),
				m=Integer.valueOf(dcmTime.substring(2,4)),
				s=Integer.valueOf(dcmTime.substring(4,6));
			return dcmTime.substring(0,2)+":"+dcmTime.substring(2,4)+":"+dcmTime.substring(4,6);
		}
		catch(Exception e)
		{
			return dcmTime;
		}
	}
	
	/**
	 * 
	 * Format string in human-readable form from DICOM date
	 * @param dcmDate yyyymmdd
	 * @return human-readable string, 01 Jan 2010
	 */
	private String formatDate(String dcmDate)
	{
		try
		{
			if(dcmDate.length()<8) return dcmDate;
			String[] mn={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
			int yr=Integer.valueOf(dcmDate.substring(0,4)),
				mon=Integer.valueOf(dcmDate.substring(4,6)),
				day=Integer.valueOf(dcmDate.substring(6,8));
			return new String(day+" "+mn[mon-1]+" "+yr);
		}
		catch(Exception e)
		{
			return dcmDate;
		}
	}
	/**
	 * Extract string representing floating point number from DICOM tag, 
	 * with specified number of significant digits. 
	 * 
	 * @param tag DICOM tag to extract
	 * @param decDig number of sign. digits
	 * @return formatted string
	 */
	private String formatFloat(int tag, int decDig)
	{
		try
		{
			float f=m_dob.getFloat(tag);
			if(f==0) return null;
			String format="%1."+String.format("%1d", decDig)+"f";
			return String.format(format, f);
		}
		catch(Exception e){return null;}
	}
			
	/**
	 * Short form for concatenating two strings, does nothing when string to add is empty
	 * @param s0
	 * @param toAdd
	 * @return
	 */
	private String addS(String s0,String toAdd)
	{
		if(toAdd.length()<1) return s0;
		return s0+toAdd;
	}
	/**
	 * Get string array made of DICOM tag values in floating point format
	 * @param decDig number of significant digits to display 
	 * @param tag array of DICOM tags
	 * @return
	 */
	private String[] getTAf(int decDig, int ... tags)
	{
		String [] res=new String[tags.length];
		for(int i=0; i<tags.length;i++)
		{			
			res[i]=formatFloat(tags[i],decDig);
		}
		return res;
	}
	/**
	 * Get string array made of DICOM tag values
	 * @param tags DICOM tag codes
	 * @return String array of tag values (String-formatted)
	 */
	private String[] getTA(int ... tags)
	{
		String[] res=new String[tags.length];
		for(int i=0; i<tags.length; i++)
		{
			res[i]=m_dob.getString(tags[i]);
		}
		return res;
	}	
	/**
	 * Get string array
	 * @param str list of strings
	 * @return string array
	 */
	private String[] getSA(String ... str){return str;}
	/**
	 * Form information string enumerating tag values with prefixes
	 * @param pref prefix array
	 * @param tags tag value array
	 * @return
	 */
	private String formS(String[] pref, String[] tags)
	{
		String res="",tmp;
		String[] vals=new String[pref.length];
		
		int nCl=0;
		for(int i=0; i< pref.length; i++)
		{
			vals[i]=null;
			tmp=tags[i];
			if(tmp==null || tmp.length()<1) continue;
			if(pref[i].length()>0)
				vals[i]=pref[i]+": "+tmp;
			else 
				vals[i]=tmp;
			nCl++;
		}
		int ind=0;
		for(int i=0; i<pref.length; i++)
		{
			if(vals[i]!=null) 
				res+=vals[i];
			if(ind<nCl-1) res+=", ";
			ind++;
		}
		if(res.length()>0) res+="\n";
		return res;
	}
	private void getPatientOrientation(String[] sI)
	{
		//todo: extract patient orientation.
	}
}
