package org.nrg.xnd.utils.dicom;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.CollectionManager;
import org.nrg.fileserver.FileCollection;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.FSFile;
import org.nrg.xnd.model.Resource;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.rules.dicom.DICOMReader;
import org.nrg.xnd.tools.ImageViewer.IProgressReporter;

public class StudyList
{	
	private IProgressReporter m_mon=null;
	private TreeMap<String,StudyRecord> m_studies=new TreeMap<String,StudyRecord>();
	
	public StudyList(Collection<CElement> cce, IProgressReporter monitor)
	{
		Collection<CElement> files=new LinkedList<CElement>();
		TypeFilter tf=new TypeFilter();
		
		//get all files
		for(CElement ce: cce)
		{
			ce.getLeaves(files,  tf, null);
		}
		m_mon=monitor;
		monitor.taskName("Analyzing DICOM files...");
		for(CElement ce:files)
		{
			if(ce instanceof DBElement)
				analyzeDBElement((DBElement)ce);
			else if (ce instanceof Resource)
			{
				analyzeFile(((Resource)ce).getFile());
			}
			else if(ce instanceof FSFile)
				analyzeFile(((FSFile)ce).GetFSObject());
			if(m_mon.isCanceled()) return;
		}
	}
	public Collection<StudyRecord> getStudies()
	{
		return m_studies.values();
	}
	private void analyzeDBElement(DBElement dbe)
	{
		if(dbe.IsCollection())
		{
			CollectionManager cm=XNDApp.app_localVM.getCM();
			FileCollection fc=cm.GetCollection(dbe.GetIR().getColID());
			for(String s:fc.GetAllFiles())
			{
				analyzeFile(new File(XNDApp.app_localVM.GetAbsolutePath(s)));
			}
		}
		else
			analyzeFile(dbe.GetIR().getFile());
	}
	private void analyzeFile(File f)
	{
		if(f==null) return;
		if(m_mon!=null) m_mon.subTaskName(f.getName());
		DicomObject dob=DICOMReader.read(f);
		if(dob==null) return;
		String s=dob.getString(Tag.StudyInstanceUID);
		if(s==null) return;
		StudyRecord sr=m_studies.get(s);
		if(sr==null)
		{
			sr=new StudyRecord(dob);
			m_studies.put(s, sr);
		}
		sr.addFileToStudy(dob, f);
	}
}
