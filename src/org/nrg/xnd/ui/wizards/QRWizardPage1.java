package org.nrg.xnd.ui.wizards;

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.ui.dialogs.AEPropertiesDialog;
import org.nrg.xnd.utils.Utils;
import org.nrg.xnd.utils.dicom.AEList.AE;

public class QRWizardPage1 extends WizardPage
{

	private Group group1;
	private Label label1;
	private Text m_AccNumText;
	private Combo m_ModalityCombo;
	private MemoCombo m_ModalityComboM;
	private Label label7;
	private Label label6;
	private Combo m_StEndDate;
	private MemoCombo m_StEndDateM;
	private Combo m_StBeginDate;
	private MemoCombo m_StBeginDateM;
	private Label label5;
	private Button m_BrowseButton;
	private Label m_StatusLabel;
	private Text m_PatID;
	private Label label4;
	private Text m_PatName;
	private Label label3;
	private Label label2;
	private Group group3;
	private Button m_AESetupButton;
	private Combo m_DICOMDestAECombo;
	private MemoCombo m_DICOMDestAEComboM;
	private Group group2;
	private Combo m_FolderCombo;
	private MemoCombo m_FolderComboM;

	@Override
	public boolean isPageComplete()
	{
		String fname = m_FolderCombo.getText();
		if (fname.length() < 1)
		{
			m_StatusLabel.setText("Please select destination folder.");
			return false;
		}
		if (!(new File(fname)).exists())
		{
			m_StatusLabel.setText("Destination folder does not exist.");
			return false;
		}
		m_StatusLabel.setText("");
		return true;
	}

	@Override
	public void createControl(Composite parent)
	{
		Composite topLevel = new Composite(parent, SWT.NONE);
		GridLayout parentLayout = new GridLayout();
		topLevel.setLayout(parentLayout);
		{
			group1 = new Group(topLevel, SWT.NONE);
			GridLayout group1Layout = new GridLayout();
			group1Layout.numColumns = 3;
			group1.setLayout(group1Layout);
			GridData group1LData = new GridData();
			group1LData.horizontalAlignment = GridData.FILL;
			group1.setLayoutData(group1LData);
			group1.setText("Folder to store retrieved DICOM files:");
			{
				label1 = new Label(group1, SWT.NONE);
				GridData label1LData = new GridData();
				label1LData.horizontalAlignment = GridData.END;
				label1.setLayoutData(label1LData);
				label1.setText("Folder:");
			}
			{
				GridData m_FolderComboLData = new GridData();
				m_FolderComboLData.horizontalAlignment = GridData.FILL;
				m_FolderComboLData.grabExcessHorizontalSpace = true;
				m_FolderCombo = new Combo(group1, SWT.NONE);
				m_FolderCombo.setLayoutData(m_FolderComboLData);
				m_FolderCombo.addModifyListener(new ModifyListener()
				{
					@Override
					public void modifyText(ModifyEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});
				m_FolderCombo.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});
				m_FolderComboM = new MemoCombo(m_FolderCombo,
						"QRWizardPage1.FolderCombo", 10);
				if (m_FolderCombo.getText().length() < 1)
				{
					File f = new File(Utils.GetIncomingFolder() + "/DICOM");
					if (!f.exists())
						f.mkdir();
					m_FolderCombo.add(f.getAbsolutePath());
				}
			}
			{
				m_BrowseButton = new Button(group1, SWT.PUSH | SWT.CENTER);
				GridData m_BrowseButtonLData = new GridData();
				m_BrowseButton.setLayoutData(m_BrowseButtonLData);
				m_BrowseButton.setText("Browse...");
				m_BrowseButton.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						String fold = Utils.SelectFolder("Folder selection",
								"Folder for received DICOM files");
						if (fold != null)
							m_FolderCombo.setText(fold);
						setPageComplete(isPageComplete());
					}
				});
			}
		}
		{
			group2 = new Group(topLevel, SWT.NONE);
			GridLayout group2Layout = new GridLayout();
			group2Layout.numColumns = 2;
			group2.setLayout(group2Layout);
			GridData group2LData = new GridData();
			group2LData.horizontalAlignment = GridData.FILL;
			group2LData.grabExcessHorizontalSpace = true;
			group2.setLayoutData(group2LData);
			group2.setText("DICOM destination AE:");
			{
				GridData m_DICOMDestAEComboLData = new GridData();
				m_DICOMDestAEComboLData.horizontalAlignment = GridData.FILL;
				m_DICOMDestAEComboLData.grabExcessHorizontalSpace = true;
				m_DICOMDestAECombo = new Combo(group2, SWT.READ_ONLY);
				m_DICOMDestAECombo.setLayoutData(m_DICOMDestAEComboLData);
				m_DICOMDestAEComboM = new MemoCombo(m_DICOMDestAECombo,
						"QRWizardPage1.aeList", 100);
				m_DICOMDestAEComboM.Sync(XNDApp.app_aeList.getAENames());
			}
			{
				m_AESetupButton = new Button(group2, SWT.PUSH | SWT.CENTER);
				GridData m_AESetupButtonLData = new GridData();
				m_AESetupButton.setLayoutData(m_AESetupButtonLData);
				m_AESetupButton.setText("AE setup...");
				m_AESetupButton.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						AEPropertiesDialog aepd = new AEPropertiesDialog(
								QRWizardPage1.this.getShell(),
								m_DICOMDestAECombo.getText());
						aepd.open();
						m_DICOMDestAEComboM
								.Sync(XNDApp.app_aeList.getAENames());
						m_DICOMDestAEComboM.setSelection(aepd
								.getSelectedAEName());
						setPageComplete(isPageComplete());
					}
				});
			}
		}
		{
			m_StatusLabel = new Label(topLevel, SWT.NONE);
			GridData m_StatusLabelLData = new GridData();
			m_StatusLabelLData.horizontalAlignment = GridData.FILL;
			m_StatusLabelLData.grabExcessHorizontalSpace = true;
			m_StatusLabel.setLayoutData(m_StatusLabelLData);
		}
		setControl(topLevel);
		setPageComplete(isPageComplete());
	}
	public AE getSelectedAE()
	{
		return XNDApp.app_aeList.getAE(m_DICOMDestAECombo.getText());
	}
	public void Save()
	{
		m_DICOMDestAEComboM.Save();
		m_FolderComboM.Save();
	}
	public QRWizardPage1()
	{
		super("QWPage1", "DICOM query", null);
	}
	public QRWizardPage1(String pageName, String title,
			ImageDescriptor titleImage)
	{
		super(pageName, title, titleImage);
		// TODO Auto-generated constructor stub
	}
	public AE getLocalAE()
	{
		return XNDApp.app_aeList.getAE("local");
	}
	public AE getRemoteAE()
	{
		return XNDApp.app_aeList.getAE(m_DICOMDestAECombo.getText());
	}
	public String getStoreFolder()
	{
		return m_FolderCombo.getText();
	}
	public QRWizardPage1(String pageName)
	{
		super(pageName);
	}
}