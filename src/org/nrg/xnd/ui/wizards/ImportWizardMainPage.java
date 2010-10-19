/**
 * 
 */
package org.nrg.xnd.ui.wizards;

import java.util.Collection;

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
import org.eclipse.swt.widgets.List;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.AppActions;
import org.nrg.xnd.app.FileView;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.FSFile;
import org.nrg.xnd.model.FSFolder;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.ui.MemoCheckBox;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.Utils;

/**
 * This code was edited or generated using CloudGarden's Jigloo
 * SWT/Swing GUI Builder, which is free for non-commercial
 * use. If Jigloo is being used commercially (ie, by a corporation,
 * company or business for any purpose whatever) then you
 * should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details.
 * Use of Jigloo implies acceptance of these licensing terms.
 * A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
 * THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
 * LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
/**
 * @author mmilch
 * 
 */
public class ImportWizardMainPage extends WizardPage
{
	private Label label1;
	private Label m_StatusLabel;
	private Combo m_Project;
	private Button m_AddDirButton;
	private Button m_AddFiles;
	private Group group2;
	private Group group1;
	private Button m_RemoveDirButton;
	private List m_FolderList;
	private Combo m_ModalityCombo;
	private Label label3;
	private Combo m_subjLabel;
	private Button m_subjData;
	private MemoCombo m_ProjectM, m_subjLabelM, m_ModalityComboM;
	private MemoCheckBox m_subjCheckM, m_extractModalityM;
	private Button m_ExtractModality;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public boolean isPageComplete()
	{
		if (m_FolderList.getItemCount() < 1)
		{
			m_StatusLabel.setText("Add folders or files to import.");
			return false;
		}
		if (m_Project.getText().length() < 1)
		{
			m_StatusLabel.setText("Please set project label.");
			return false;
		}
		if (m_subjData.getSelection() && m_subjLabel.getText().length() < 1)
		{
			m_StatusLabel.setText("Please set subject name.");
			return false;
		}
		if (m_ModalityCombo.getText().length() < 1)
		{
			m_StatusLabel.setText("Please set modality.");
			return false;
		}
		m_StatusLabel.setText("");
		return true;
		// return super.isPageComplete();
	}
	public ImportWizardMainPage()
	{
		super("IWMainPage", "Data sources", null);
	}
	public void createControl(Composite parent)
	{
		Composite topLevel = new Composite(parent, SWT.NONE);
		GridLayout parentLayout = new GridLayout();
		topLevel.setLayout(parentLayout);
		{
			m_StatusLabel = new Label(topLevel, SWT.NONE);
			GridData m_StatusLabelLData = new GridData();
			m_StatusLabelLData.horizontalAlignment = GridData.FILL;
			m_StatusLabel.setLayoutData(m_StatusLabelLData);
		}
		{
			group1 = new Group(topLevel, SWT.NONE);
			GridLayout group1Layout = new GridLayout();
			group1Layout.makeColumnsEqualWidth = true;
			group1Layout.numColumns = 3;
			group1.setLayout(group1Layout);
			GridData group1LData = new GridData();
			group1LData.horizontalAlignment = GridData.FILL;
			group1.setLayoutData(group1LData);
			group1.setText("Files/folders to import (add at least one)");
		}
		{
			GridData m_FolderListLData = new GridData();
			m_FolderListLData.verticalAlignment = GridData.FILL;
			m_FolderListLData.horizontalAlignment = GridData.FILL;
			m_FolderListLData.verticalSpan = 5;
			m_FolderListLData.grabExcessHorizontalSpace = true;
			m_FolderListLData.grabExcessVerticalSpace = true;
			m_FolderListLData.horizontalSpan = 2;
			m_FolderList = new List(group1, SWT.BORDER | SWT.V_SCROLL
					| SWT.H_SCROLL | SWT.MULTI);
			m_FolderList.setLayoutData(m_FolderListLData);
			FileView fv = AppActions.GetLocalFileView();
			if (fv != null)
			{
				Collection<CElement> cce = fv.GetSelectedElements();
				for (CElement ce : cce)
				{
					if (ce instanceof FSFile)// || ce instanceof FSFolder)
					{
						m_FolderList.add(((FSFile) ce).GetFSObject()
								.getAbsolutePath());
					}
					if (ce instanceof FSFolder)// || ce instanceof FSFolder)
					{
						m_FolderList.add(((FSFolder) ce).GetFSObject()
								.getAbsolutePath());
					}
				}

			}
		}
		{
			m_AddDirButton = new Button(group1, SWT.PUSH | SWT.CENTER);
			m_AddDirButton.setText("Add directory");
			m_AddDirButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetDefaultSelected(SelectionEvent evt)
				{
				}
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					String fold = Utils.SelectFolder("Folder selection",
							"Add directory");
					m_FolderList.add(fold);
					getWizard().getContainer().updateButtons();
				}
			});
			m_AddDirButton.setEnabled(false);
		}
		{
			m_AddFiles = new Button(group1, SWT.PUSH | SWT.CENTER);
			GridData m_AddFilesLData = new GridData();
			m_AddFiles.setLayoutData(m_AddFilesLData);
			m_AddFiles.setText("Add files...");
			m_AddFiles.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					Collection<String> files = Utils
							.SelectFiles("Select files/folders to import:");
					for (String f : files)
						m_FolderList.add(f);
					getWizard().getContainer().updateButtons();
				}
			});
			m_AddFiles.setEnabled(false);
		}
		{
			m_RemoveDirButton = new Button(group1, SWT.PUSH | SWT.CENTER);
			GridData m_RemoveDirButtonLData = new GridData();
			m_RemoveDirButtonLData.verticalSpan = 1;
			m_RemoveDirButton.setLayoutData(m_RemoveDirButtonLData);
			m_RemoveDirButton.setText("Remove");
			m_RemoveDirButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					m_FolderList.remove(m_FolderList.getSelectionIndices());
					getWizard().getContainer().updateButtons();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent evt)
				{
				}
			});
			m_RemoveDirButton.setEnabled(false);
		}
		{
			group2 = new Group(topLevel, SWT.NONE);
			GridLayout group2Layout = new GridLayout();
			group2Layout.numColumns = 2;
			group2.setLayout(group2Layout);
			GridData group2LData = new GridData();
			group2LData.horizontalAlignment = GridData.FILL;
			group2.setLayoutData(group2LData);
			group2.setText("Data essentials");
		}

		{
			label1 = new Label(group2, SWT.NONE);
			GridData label1LData = new GridData();
			label1LData.horizontalAlignment = GridData.END;
			label1.setLayoutData(label1LData);
			label1.setText("Project for these data (required):");
		}
		{
			m_Project = new Combo(group2, SWT.NONE);
			m_ProjectM = new MemoCombo(m_Project,
					"ImportWizardMainPage.Project", 10);
			m_Project.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent evt)
				{
					getWizard().getContainer().updateButtons();
				}
			});
		}
		{
			m_subjData = new Button(group2, SWT.CHECK | SWT.LEFT);
			GridData m_subjDataLData = new GridData();
			m_subjDataLData.horizontalAlignment = GridData.END;
			m_subjData.setLayoutData(m_subjDataLData);
			m_subjData.setText("All data belong to a single subject:");
			m_subjCheckM = new MemoCheckBox(m_subjData,
					"ImportWizardMainPage.SingleSubject");
			m_subjData.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					getWizard().getContainer().updateButtons();
				}
			});
		}
		{
			m_subjLabel = new Combo(group2, SWT.NONE);
			m_subjLabelM = new MemoCombo(m_subjLabel,
					"ImportWizardMainPage.Subject", 10);
			m_subjLabel.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent evt)
				{
					getWizard().getContainer().updateButtons();
				}
			});
		}
		{
			label3 = new Label(group2, SWT.NONE);
			GridData label3LData = new GridData();
			label3LData.horizontalAlignment = GridData.END;
			label3.setLayoutData(label3LData);
			label3.setText("Primary imaging modality (required):");
		}
		{
			m_ModalityCombo = new Combo(group2, SWT.READ_ONLY);
			m_ModalityComboM = new MemoCombo(m_ModalityCombo,
					"ImportWizardMainPage.Modality", 10);
			String[] mods = (DefaultOntologyManager.GetTagValues(new ItemTag(
					"Modality")));
			if (m_ModalityCombo.getItemCount() != mods.length)
			{
				m_ModalityCombo.removeAll();
				for (String m : mods)
					m_ModalityCombo.add(m);
				m_ModalityComboM.SetValues(m_ModalityCombo.getItems());
				m_ModalityCombo.select(0);
			}
			m_ModalityCombo.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent evt)
				{
					getWizard().getContainer().updateButtons();
				}
			});
		}
		{
			m_ExtractModality = new Button(group2, SWT.CHECK | SWT.LEFT);
			GridData m_ExtractModalityLData = new GridData();
			m_ExtractModality.setLayoutData(m_ExtractModalityLData);
			m_ExtractModality.setText("Extract modality from DICOM");
			m_extractModalityM = new MemoCheckBox(m_ExtractModality,
					"ImportWizardMainPage.ExtractModalityFromDcm");
		}
		setControl(topLevel);
		setPageComplete(isPageComplete());
	}
	public String[] getSelectedFolders()
	{
		return m_FolderList.getItems();
	}
	public void Save()
	{
		m_ProjectM.Save();
		m_subjLabelM.Save();
		m_ModalityComboM.Save();
		m_subjCheckM.Save();
		m_extractModalityM.Save();
	}

	public String getProject()
	{
		return m_Project.getText();
	}

	public String getModality()
	{
		return m_ModalityCombo.getText();
	}

	public boolean isSingleSubject()
	{
		return m_subjData.getSelection();
	}

	public String getSubject()
	{
		return m_subjLabel.getText();
	}

	public boolean getExtractModality()
	{
		return m_ExtractModality.getSelection();
	}
}
