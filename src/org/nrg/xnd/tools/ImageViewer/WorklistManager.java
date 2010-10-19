package org.nrg.xnd.tools.ImageViewer;
import org.nrg.xnd.utils.dicom.DICOMRecord;

public interface WorklistManager
{
	public boolean IsPreviewMode();
	public void SetPreviewMode(boolean bSet);
	public boolean UpdateStudyNote(String note, DICOMRecord studyDR);
	public String GetUser();
	public void ShowDBWindow(boolean bShow);
	public boolean LoadSeries(DICOMRecord dr);
	public boolean IsGuestMode();
}
