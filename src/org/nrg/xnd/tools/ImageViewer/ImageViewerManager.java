package org.nrg.xnd.tools.ImageViewer;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.imageioimpl.plugins.dcm.DicomImageReader;
import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.ItemRecord;
import org.nrg.xnd.rules.dicom.DICOMReader;
import org.nrg.xnd.utils.LightXML;
import org.nrg.xnd.utils.dicom.DICOMPixelExtractor;
import org.nrg.xnd.utils.dicom.DICOMRecord;
import org.nrg.xnd.utils.dicom.SeriesRecord;
import org.nrg.xnd.utils.dicom.StudyList;
import org.nrg.xnd.utils.dicom.StudyRecord;

public class ImageViewerManager implements WorklistManager
{
	ImageViewer m_IV;
	public ImageViewerManager(Container c, boolean bShowToolbar)
	{
		m_IV = new ImageViewer(c, this,bShowToolbar);
	}
	public void UpdateSize(Dimension sz)
	{
		m_IV.UpdateSize(sz);
	}
	public void updateStatus(String status)
	{
		m_IV.SetStatus(status);
	}
	public ImageViewer getImView(){return m_IV;}
	public boolean OpenImages(StudyRecord sr, IProgressReporter r, boolean bPreview)
	{
		m_IV.GetContainer().setCursor(
				Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		r.taskName("Loading study...");
		try
		{
			DICOMRecord dr=new DICOMRecord();
			dr.initFromDOB(sr.m_dob);
			m_IV.getStudy().setPreviewMode(bPreview);
			m_IV.getStudy().StartStudyLoading(dr);
			int nSer=1;
			boolean bRes=true;
			for(SeriesRecord serR:sr.getSeries())
			{
				serR.sortSRs();
				r.taskName("Loading series "+nSer);
				if(!m_IV.addSeriesToStudy(serR,r)){ bRes=false; break;}
				nSer++;
				if (r.isCanceled())	return false;
			}
			m_IV.getStudy().EndStudyLoading();
			return bRes;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return false;
		}
		finally
		{
			m_IV.GetContainer().setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));			
		}
/*		
		LinkedList<DICOMRecord> drs = new LinkedList<DICOMRecord>();
		DicomObject dob;
		DICOMRecord dr = null;
		DICOMPixelExtractor dpe = new DICOMPixelExtractor();
		LightXML image_info;

		
		int i = 0;
		int nAttempts = 0;
		for (int ind = 0; ind < images.length; ind++)
		{
			try
			{
				dob = DICOMReader.read(images[ind], Tag.PixelData);
				if (dob == null)
					continue;
				dr = new DICOMRecord();
				dr.InitFromDOB(dob);
				if (i == 0)
				{
					m_IV.SendEvent("start study loading", dr);
				}
				drs.add(dr);
				if (!dpe.LoadImage(dob))
					continue;
				image_info = dpe.GetImageInfo();
				if (m_IV.AddImageToStudy(dpe.GetPixels(), image_info, dr, true))
					i++;
				nAttempts = 0;
				m_IV.SetStatus("Loading image " + ind + " out of "
						+ images.length);
			} catch (OutOfMemoryError e)
			{
				System.out.println("OutOfMemoryError while loading image");
				if (nAttempts < 3)
				{
					m_IV.OptimizeMemory(e);
					ind--;
				}
				nAttempts++;
			} finally
			{
			}
		}
		if (i > 0)
		{
			// m_IV.SendEvent("end series loading", dr);
			m_IV.SendEvent("end study loading", dr);
			m_IV.SetStatus("Images loaded");
		}
		m_IV.GetContainer().setCursor(
				Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		return (i > 0);
*/		
	}

	public String GetUser()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean IsGuestMode()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean IsPreviewMode()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean LoadSeries(DICOMRecord dr)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void SetPreviewMode(boolean set)
	{
		// TODO Auto-generated method stub

	}

	public void ShowDBWindow(boolean show)
	{
		// TODO Auto-generated method stub

	}

	public boolean UpdateStudyNote(String note, DICOMRecord studyDR)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
