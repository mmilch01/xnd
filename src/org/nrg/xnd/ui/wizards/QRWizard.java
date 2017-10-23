package org.nrg.xnd.ui.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import org.nrg.xnd.app.AppActions;
import org.nrg.xnd.app.ConsoleView;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.FSFile;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.rules.RuleManager;
import org.nrg.xnd.utils.Utils;
import org.nrg.xnd.utils.dicom.QueryRetrieve;
import org.nrg.xnd.utils.dicom.AEList.AE;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.query.StudyRootQueryInformationModel;

public class QRWizard extends Wizard implements IRunnableWithProgress
{
	QRWizardPage1 m_page1;
	QRWizardPage2 m_page2;
	boolean bPACSView = false;
	@Override
	public boolean performFinish()
	{
		m_page1.Save();
		m_page2.Save();
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
		StudyRootQueryInformationModel m_qim;
		Collection<AttributeList> al_res;
		String m_storeFolder;
		AE m_localAE;
		AE m_remoteAE;
		@Override
		public void run()
		{
			m_qim = m_page2.m_qim;
			al_res = m_page2.getUniqueKeys();
			m_storeFolder = m_page1.getStoreFolder();
			m_localAE = m_page1.getLocalAE();
			m_remoteAE = m_page1.getRemoteAE();
		}
		public String isValid()
		{
			if (m_qim == null)
				return "Cannot read query results";
			if (al_res.size() < 1)
				return "No items to retrieve";
			if (m_storeFolder == null || !(new File(m_storeFolder)).exists())
				return "Storage folder does not exist";
			if (m_localAE == null || m_remoteAE == null)
				return "Remote AE could not be determined";
			return null;
		}
	}

	private void performRetrieve(final IProgressMonitor mon)
	{
		if (AppActions.GetActivePACSView() != null)
			bPACSView = true;

		mon.setTaskName("Sending C-Move request...");
		final RuntimeData rd = new RuntimeData();
		Display.getDefault().syncExec(rd);
		final String err = rd.isValid();
		if (err != null)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				@Override
				public void run()
				{
					Utils.ShowMessageBox("Import failed",
							"There was a problem with parameters:\n" + err,
							Window.OK);
				}
			});
			return;
		}

		boolean bManaged = false, bMatchName = false;
		for (String fold : XNDApp.app_localVM.GetManagedFolders())
		{
			if (Utils.CrossCheckDirs(rd.m_storeFolder, fold) != 0)
			{
				bManaged = true;
				break;
			}
			if (Utils.MatchFolderNames(rd.m_storeFolder, fold))
			{
				bMatchName = true;
				break;
			}
		}
		if (!bManaged)
			XNDApp.app_localVM.AddManagedFolder(rd.m_storeFolder);
		final FSFolder fl = new FSFolder(new File(rd.m_storeFolder),
				XNDApp.app_localVM, null);

		final LinkedList<String> recvFiles = new LinkedList<String>();
		// try
		{
			ReceivedObjectHandler roh = new ReceivedObjectHandler()
			{
				@Override
				public void sendReceivedObjectIndication(String fileName,
						String transferSyntax, String callingAETitle)
						throws DicomNetworkException, DicomException,
						IOException
				{
					mon.subTask("File received: " + fileName);
					final String fn = fileName;
					Display.getDefault().syncExec(new Runnable()
					{
						@Override
						public void run()
						{
							ConsoleView.AppendMessage("DICOM file received: "
									+ fn);
						}
					});
					Rule r;
					r = RuleManager.getDefaultRule(Rule.RULE_DICOM);
					if (bPACSView)
						r = RuleManager.getRule("dicom_rule_pacs");

					mon.subTask("Extracting tags from " + fileName);
					FSFile fsf = new FSFile(new File(fileName),
							XNDApp.app_localVM, fl);
					fsf.ApplyOperation(null, CElement.MANAGEALL, mon);
					DBElement dbe = DBElement.CreateDBE(fsf.GetFSObject(),
							XNDApp.app_localVM, fl);
					if (dbe != null)
						dbe.ApplyOperation(r, -1, mon);
					recvFiles.add(fileName);
				}
			};
			if (QueryRetrieve.isConfigChanged(rd.m_storeFolder))
			{
				QueryRetrieve.stop();
				final String res = QueryRetrieve.start(rd.m_storeFolder, roh);
				if (res != null)
				{
					Display.getDefault().syncExec(new Runnable()
					{
						@Override
						public void run()
						{
							Utils.ShowMessageBox("Import failed",
									"Could not start StorageSOPClass SCP:\n"
											+ res, Window.OK);
						}
					});
				}
			}
		}
		// catch(Exception e)
		// {
		// Utils.logger.error("Exception creating SCP service",e);
		// }
		AttributeTag at = TagFromName.QueryRetrieveLevel;
		Attribute a = new ShortStringAttribute(at);
		try
		{
			a.setValue("STUDY");
		} catch (Exception ce)
		{
		}
		Attribute b;

		for (AttributeList al : rd.al_res)
		{
			try
			{
				mon.setTaskName("Performing C-Move for study "
						+ al.get(TagFromName.StudyInstanceUID)
								.getDelimitedStringValuesOrEmptyString());
				b = al.get(at);
				if (b == null)
					al.put(a);
				rd.m_qim.performHierarchicalMove(al);
			} catch (Exception e)
			{
				Utils.logger.error("Exception executing C-Move request", e);
			}
		}
		/*
		 * Display.getDefault().syncExec(new Runnable() { public void run() {
		 * ConfirmDialog cd=new ConfirmDialog("QRWizard.DurationWarning","Note",
		 * "Request to retrieve remote studies is sent. See the import progress in the console."
		 * ); if(cd.NeedToShow()) cd.open(); } });
		 */

		/*
		 * if(recvFiles.size()>0) {
		 * mon.setTaskName("Generating collections...");
		 * fl.ApplyOperation(RuleManager.getDefaultRule(Rule.RULE_COL), -1,
		 * mon); } fl.Invalidate();
		 */
	}
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException
	{
		performRetrieve(monitor);
		monitor.done();
	}
	@Override
	public void addPages()
	{
		addPage(m_page1 = new QRWizardPage1());
		addPage(m_page2 = new QRWizardPage2());
	}
	@Override
	public IWizardPage getNextPage(IWizardPage page)
	{
		return super.getNextPage(page);
	}
}