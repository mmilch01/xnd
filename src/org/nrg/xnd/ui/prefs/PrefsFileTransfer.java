package org.nrg.xnd.ui.prefs;

import java.io.File;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.ui.MemoCheckBox;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.Utils;

public class PrefsFileTransfer extends PreferencePage
		implements
			IWorkbenchPreferencePage
{
	private Button m_BrowseButton;
	private Combo m_IncomingFolderText;
	private MemoCombo m_IncomingFolderTextM;
	private Label m_IncomingFolderLabel;
	private MemoCheckBox m_ExtractDataFromDICOMHeadersM;
	private Button m_ExtractDataFromDICOMHeaders;
	private Button m_RunPipelinesCheck;
	private MemoCheckBox m_RunPipelinesCheckM;
	private Group group2;
	private Group group1;
	private Button m_FilteredDownloadCheck;
	private String m_IncomingFolder;

	@Override
	protected Control createContents(Composite parent)
	{
		noDefaultAndApplyButton();
		GridLayout parentLayout = new GridLayout();
		parent.setLayout(parentLayout);
		{
			group1 = new Group(parent, SWT.NONE);
			GridLayout group1Layout = new GridLayout();
			group1Layout.numColumns = 3;
			group1.setLayout(group1Layout);
			group1.setText("Download options");
		}
		{
			group2 = new Group(parent, SWT.NONE);
			GridLayout group2Layout = new GridLayout();
			group2.setLayout(group2Layout);
			group2.setText("Upload to XNAT options");
			{
				m_RunPipelinesCheck = new Button(group2, SWT.CHECK | SWT.LEFT);
				m_RunPipelinesCheckM = new MemoCheckBox(m_RunPipelinesCheck,
						"PrefsFileTransfer.RunPipelinesCheck", true);
				GridData m_RunPipelinesCheckLData = new GridData();
				m_RunPipelinesCheck.setLayoutData(m_RunPipelinesCheckLData);
				m_RunPipelinesCheck
						.setText("Execute default XNAT pipelines on uploaded data");
			}
			{
				m_ExtractDataFromDICOMHeaders = new Button(group2, SWT.CHECK
						| SWT.LEFT);
				m_ExtractDataFromDICOMHeadersM = new MemoCheckBox(
						m_ExtractDataFromDICOMHeaders,
						"PrefsFileTransfer.ExtractDataFromDICOMHeaders", true);
				GridData m_ExtractDataFromDICOMHeadersLData = new GridData();
				m_ExtractDataFromDICOMHeaders
						.setLayoutData(m_ExtractDataFromDICOMHeadersLData);
				m_ExtractDataFromDICOMHeaders
						.setText("Request XNAT server to extract additional metadata (DICOM only)");
			}
		}
		{
			m_IncomingFolderLabel = new Label(group1, SWT.NONE);
			m_IncomingFolderLabel.setText("Incoming folder:");
		}
		{
			GridData m_IncomingFolderTextLData = new GridData();
			m_IncomingFolderTextLData.horizontalAlignment = GridData.FILL;
			m_IncomingFolderTextLData.grabExcessHorizontalSpace = true;
			m_IncomingFolderText = new Combo(group1, SWT.BORDER);
			m_IncomingFolderText.setLayoutData(m_IncomingFolderTextLData);
			m_IncomingFolderTextM = new MemoCombo(m_IncomingFolderText,
					"PrefsFileTransfer.IncomingFolder", 10);
		}
		{
			m_BrowseButton = new Button(group1, SWT.PUSH | SWT.CENTER);
			m_BrowseButton.setText("Browse...");
			m_BrowseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					Button b = ((Button) (evt.getSource()));

					DirectoryDialog d = new DirectoryDialog(new Shell());
					d.setText("Browse for a directory");
					d.setMessage("Select default incoming directory");
					if (d.open() != null)
					{
						m_IncomingFolder = d.getFilterPath();
						UpdateData(false);
					}
				}
			});
		}
		{
			m_FilteredDownloadCheck = new Button(group1, SWT.CHECK | SWT.LEFT);
			m_FilteredDownloadCheck.setText("Filtered download");
			m_FilteredDownloadCheck.setSelection(XNDApp.app_Prefs.getBoolean(
					"PrefsFileTransferFilterDownload", true));
		}

		UpdateData(false);
		return null;
	}
	private void UpdateData(boolean bStore)
	{
		if (!bStore)
		{
			m_IncomingFolderText.setText(m_IncomingFolder);
		} else
		{
			m_IncomingFolder = m_IncomingFolderText.getText();
		}
	}
	@Override
	public boolean performOk()
	{
		UpdateData(true);
		if (!(new File(m_IncomingFolder)).exists())
		{
			if (Utils.ShowMessageBox("",
					"Incoming folder does not exist, proceed?", SWT.OK
							| SWT.CANCEL) == SWT.CANCEL)
				return false;
		}
		XNDApp.app_Prefs.put("IncomingFolder", m_IncomingFolder);
		try
		{
			XNDApp.app_Prefs.flush();
		} catch (Exception e)
		{
		}
		m_ExtractDataFromDICOMHeadersM.Save();
		m_RunPipelinesCheckM.Save();
		m_IncomingFolderTextM.Save();
		return true;
	}
	public void init(IWorkbench workbench)
	{
		m_IncomingFolder = Utils.GetIncomingFolder();
	}
}