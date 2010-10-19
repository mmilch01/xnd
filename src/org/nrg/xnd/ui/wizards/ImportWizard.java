package org.nrg.xnd.ui.wizards;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.FSFile;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.model.RootElement;
import org.nrg.xnd.rules.FileExtensionRule;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.rules.RuleManager;
import org.nrg.xnd.rules.dicom.DICOMRule;
import org.nrg.xnd.utils.FSObject;
import org.nrg.xnd.utils.Utils;

public class ImportWizard extends Wizard implements IRunnableWithProgress
{
	ImportWizardMainPage m_mainP;
	ImportWizardAutoPage m_aP;
	ImportWizardManualPage m_manualP;
	public ImportWizard()
	{
		super();
	}
	public boolean AllowedExtraSystemTag(String tag)
	{
		if (tag.compareTo("Project") == 0 || tag.compareTo("Subject") == 0
				|| tag.compareTo("Modality") == 0)
			return false;
		return true;
	}
	@Override
	public void addPages()
	{
		addPage(m_mainP = new ImportWizardMainPage());
		addPage(m_aP = new ImportWizardAutoPage());
		addPage(m_manualP = new ImportWizardManualPage());
	}
	@Override
	public IWizardPage getNextPage(IWizardPage iwp)
	{
		if (iwp instanceof ImportWizardMainPage)
		{
			m_aP.updateControls();
			return m_aP;
		}
		return super.getNextPage(iwp);
	}
	@Override
	public boolean performFinish()
	{
		m_mainP.Save();
		m_aP.Save();
		m_manualP.Save();
		try
		{
			XNDApp.app_Prefs.flush();
			new ProgressMonitorDialog(new Shell()).run(true, false, this);
		} catch (Exception e)
		{
			return false;
		}
		return true;
	}
	private class RuntimeData implements Runnable
	{
		String[] ffs;
		LinkedList<ItemTag> tagsToSet;
		String modality = "";
		LinkedList<CElement> els;
		FileExtensionRule fer;
		boolean bDicomFormat = false, bNameRule = false, bCol = false,
				bExtractModFromDicom = true;

		public void run()
		{
			ffs = m_mainP.getSelectedFolders();
			// LinkedList<CElement> els=new LinkedList<CElement>();
			// FSObject fso;
			// get all tags to set.
			tagsToSet = new LinkedList<ItemTag>();
			tagsToSet.add(new ItemTag("Project", m_mainP.getProject()));
			// tagsToSet.add(new ItemTag("Modality",m_mainP.getModality()));
			modality = m_mainP.getModality();
			bExtractModFromDicom = m_mainP.getExtractModality();
			if (m_mainP.isSingleSubject())
				tagsToSet.add(new ItemTag("Subject", m_mainP.getSubject()));
			tagsToSet.addAll(m_manualP.getDefinedSystemTags());
			Collection<ItemTag> uTags = m_manualP.getDefinedUserTags();

			for (ItemTag it : uTags)
				XNDApp.app_localVM.TagAdd(it.GetName());

			tagsToSet.addAll(uTags);

			int type = 0;
			if (m_aP.isAnalyzeFormat())
				type |= FileExtensionRule.FORMAT_ANALYZE;
			if (m_aP.is4dfpFormat())
				type |= FileExtensionRule.FORMAT_IFH;
			if (m_aP.isNRRDFormat())
				type |= FileExtensionRule.FORMAT_NRRD;

			fer = null;
			if (type != 0)
				fer = (FileExtensionRule) RuleManager
						.getDefaultRule(Rule.RULE_FILEEXT); // new
			// FileExtensionRule(XNDApp.app_localVM,type);
			bDicomFormat = m_aP.isDicomFormat();
			bNameRule = m_aP.isRunNameRule();
			bCol = m_aP.isCollection();
		}
	}
	protected void ProcessData(IProgressMonitor monitor)
	{
		// monitor.subTask("test subtask");
		RuntimeData rt = new RuntimeData();
		Display.getDefault().syncExec(rt);
		// try{Thread.sleep(1000);}catch(Exception e){}
		// monitor.subTask("subtask after running gui access");
		// try{Thread.sleep(1000);}catch(Exception e){}
		FSObject fso;
		try
		{
			for (String f : rt.ffs)
			{
				monitor.setTaskName("Adding to database: " + f);
				fso = new FSObject(f);
				CElement el;
				if (fso.isDirectory())
					el = new FSFolder(new File(f), XNDApp.app_localVM, null);
				else
					el = new FSFile(new File(f), XNDApp.app_localVM, null);
				// els.add(el);
				// manage
				el.ApplyOperation(null, CElement.MANAGEALL, monitor);
				// apply DICOM rule and collection rule
				if (rt.bDicomFormat)
				{
					monitor.setTaskName("Extracting DICOM tags: " + f);
					// monitor.subTask("test subtask");
					DICOMRule dr = (DICOMRule) RuleManager
							.getDefaultRule(Rule.RULE_DICOM);
					dr.setGenerateCollections(rt.bCol);
					if (el instanceof FSFile)
						el.ApplyOperation(dr, -1, monitor);
					else
					{
						Collection<CElement> cce = new LinkedList<CElement>();
						cce.add(el);
						RootElement re = new RootElement(cce,
								XNDApp.app_localVM);
						dr.ApplyRule(re, monitor);
					}
					el.Invalidate();
				}
				// apply naming rule
				if (rt.bNameRule)
				{
					monitor
							.setTaskName("Extracting tags from directory structure: "
									+ f);
					el.ApplyOperation(RuleManager
							.getDefaultRule(Rule.RULE_NAMING), -1, monitor);
				}
				if (rt.fer != null)
				{
					monitor.setTaskName("Determining data formats: " + f);
					el.ApplyOperation(rt.fer, -1, null);
				}
				// set common tags.
				monitor.setTaskName("Applying all extracted tags: " + f);
				LinkedList<ItemTag> tagsToSet = new LinkedList<ItemTag>();
				tagsToSet.addAll(rt.tagsToSet);
				if (!rt.bDicomFormat || !rt.bExtractModFromDicom)
				{
					tagsToSet.add(new ItemTag("Modality", rt.modality));
				}
				el.ApplyOperation(tagsToSet.toArray(new ItemTag[0]),
						CElement.SETTAGS, monitor);
			}
		} catch (final Exception e)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					Utils.ShowMessageBox("Import failed",
							"There was an error when importing data:\n"
									+ e.toString(), Window.OK);
				}
			});

		}
	}
	public void run(IProgressMonitor monitor)
	{
		// final IProgressMonitor mon = monitor;
		ProcessData(monitor);
		// mon.beginTask("Extracting tags", 0);
		/*
		 * Display.getDefault().syncExec(new Runnable() { public void run() {
		 * ProcessData(mon); } });
		 */
		monitor.done();
	}
	@Override
	public boolean performCancel()
	{
		return true;
	}
}