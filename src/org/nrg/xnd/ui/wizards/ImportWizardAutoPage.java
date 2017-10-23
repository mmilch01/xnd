package org.nrg.xnd.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.nrg.xnd.ui.MemoCheckBox;

public class ImportWizardAutoPage extends WizardPage
{
	private Group group1;
	private Label m_StatusLabel;
	private Button m_OtherFormat, m_Collection, m_RunNameRule, m_4dfpFormat,
			m_NRRDFormat, m_AnalyzeFormat, m_DicomFormat;
	private MemoCheckBox m_OtherFormatM, m_4dfpFormatM, m_NRRDFormatM,
			m_AnalyzeFormatM, m_DicomFormatM, m_CollectionM, m_RunNameRuleM;
	private Group group2;
	public ImportWizardAutoPage()
	{
		super("IWAutoTagging", "Automatic metadata extraction", null);
	}
	@Override
	public boolean isPageComplete()
	{
		if (!m_DicomFormat.getSelection() && !m_AnalyzeFormat.getSelection()
				&& !m_NRRDFormat.getSelection() && !m_4dfpFormat.getSelection()
				&& !m_OtherFormat.getSelection())
		{
			m_StatusLabel.setText("Choose at least one data format.");
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
		parentLayout.numColumns = 2;
		topLevel.setLayout(parentLayout);
		{
			GridData m_StatusLabelLData = new GridData();
			m_StatusLabelLData.horizontalAlignment = GridData.FILL;
			m_StatusLabelLData.horizontalSpan = 2;
			m_StatusLabel = new Label(topLevel, SWT.NONE);
			m_StatusLabel.setLayoutData(m_StatusLabelLData);
		}
		{
			group1 = new Group(topLevel, SWT.NONE);
			GridLayout group1Layout = new GridLayout();
			group1.setLayout(group1Layout);
			GridData group1LData = new GridData();
			group1LData.verticalAlignment = GridData.FILL;
			group1.setLayoutData(group1LData);
			group1.setText("Data formats (check all that apply)");
			{
				m_DicomFormat = new Button(group1, SWT.CHECK | SWT.LEFT);
				GridData m_DicomFormatCheckLData = new GridData();
				m_DicomFormat.setLayoutData(m_DicomFormatCheckLData);
				m_DicomFormat.setText("DICOM");
				m_DicomFormatM = new MemoCheckBox(m_DicomFormat,
						"ImportWizardAutoPage.DicomFormat");
				if (((ImportWizardMainPage) getPreviousPage())
						.getExtractModality())
				{
					m_DicomFormat.setSelection(true);
					m_DicomFormat.setEnabled(false);
				} else
				{
					m_DicomFormat.addSelectionListener(new SelectionAdapter()
					{
						@Override
						public void widgetSelected(SelectionEvent evt)
						{
							getWizard().getContainer().updateButtons();
						}
					});
					m_DicomFormat.setEnabled(true);
				}
			}
			{
				m_AnalyzeFormat = new Button(group1, SWT.CHECK | SWT.LEFT);
				GridData m_AnalyzeFormatLData = new GridData();
				m_AnalyzeFormat.setLayoutData(m_AnalyzeFormatLData);
				m_AnalyzeFormat.setText("Analyze (.hdr, .img)");
				m_AnalyzeFormatM = new MemoCheckBox(m_AnalyzeFormat,
						"ImportWizardAutoPage.AnalyzeFormat");
				m_AnalyzeFormat.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						getWizard().getContainer().updateButtons();
					}
				});
			}
			{
				m_NRRDFormat = new Button(group1, SWT.CHECK | SWT.LEFT);
				GridData m_NRRDFormatCheckLData = new GridData();
				m_NRRDFormat.setLayoutData(m_NRRDFormatCheckLData);
				m_NRRDFormat.setText("NRRD");
				m_NRRDFormatM = new MemoCheckBox(m_NRRDFormat,
						"ImportWizardAutoPage.NRRDFormat");
				m_NRRDFormat.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						getWizard().getContainer().updateButtons();
					}
				});
			}
			{
				m_4dfpFormat = new Button(group1, SWT.CHECK | SWT.LEFT);
				GridData m_IFHLData = new GridData();
				m_4dfpFormat.setLayoutData(m_IFHLData);
				m_4dfpFormat.setText("4dfp (.ifh, .img)");
				m_4dfpFormatM = new MemoCheckBox(m_4dfpFormat,
						"ImportWizardAutoPage.4dfpFormat");
				m_4dfpFormat.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						getWizard().getContainer().updateButtons();
					}
				});
			}
			{
				m_OtherFormat = new Button(group1, SWT.CHECK | SWT.LEFT);
				GridData m_OtherFormatLData = new GridData();
				m_OtherFormat.setLayoutData(m_OtherFormatLData);
				m_OtherFormat.setText("Other");
				m_OtherFormatM = new MemoCheckBox(m_OtherFormat,
						"ImportWizardAutoPage.OtherFormat");
				m_OtherFormat.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						getWizard().getContainer().updateButtons();
					}
				});
			}
		}
		{
			group2 = new Group(topLevel, SWT.NONE);
			GridLayout group2Layout = new GridLayout();
			group2.setLayout(group2Layout);
			GridData group2LData = new GridData();
			group2LData.verticalAlignment = GridData.FILL;
			group2.setLayoutData(group2LData);
			group2.setText("Metadata generation options");
			{
				m_RunNameRule = new Button(group2, SWT.CHECK | SWT.LEFT);
				GridData m_RunNameRuleLData = new GridData();
				m_RunNameRule.setLayoutData(m_RunNameRuleLData);
				m_RunNameRule
						.setText("Capture metadata from pre-defined folder structure");
				m_RunNameRuleM = new MemoCheckBox(m_RunNameRule,
						"ImportWizardAutoPage.NameRule");
			}
			{
				m_Collection = new Button(group2, SWT.CHECK | SWT.LEFT);
				GridData m_CollectionCheckboxLData = new GridData();
				m_Collection.setLayoutData(m_CollectionCheckboxLData);
				m_Collection
						.setText("Generate collections for multi-file entities (recommended)");
				m_CollectionM = new MemoCheckBox(m_Collection,
						"ImportWizardAutoPage.Collections");
			}
		}
		GridData topLevelLData = new GridData();
		topLevel.setLayoutData(topLevelLData);
		// GridLayout parentLayout1 = new GridLayout();
		// parentLayout1.makeColumnsEqualWidth = true;
		// parent.setLayout(parentLayout1);
		setControl(topLevel);
		setPageComplete(isPageComplete());
	}
	public void updateControls()
	{
		if (((ImportWizardMainPage) getPreviousPage()).getExtractModality())
		{
			m_DicomFormat.setSelection(true);
			m_DicomFormat.setEnabled(false);
		} else
		{
			m_DicomFormat.setEnabled(true);
		}
	}
	public void Save()
	{
		m_OtherFormatM.Save();
		m_4dfpFormatM.Save();
		m_NRRDFormatM.Save();
		m_AnalyzeFormatM.Save();
		m_DicomFormatM.Save();
		m_RunNameRuleM.Save();
		m_CollectionM.Save();
	}

	public boolean isAnalyzeFormat()
	{
		return m_AnalyzeFormat.getSelection();
	}

	public boolean isNRRDFormat()
	{
		return m_NRRDFormat.getSelection();
	}

	public boolean is4dfpFormat()
	{
		return m_4dfpFormat.getSelection();
	}

	public boolean isOtherFormat()
	{
		return m_OtherFormat.getSelection();
	}

	public boolean isDicomFormat()
	{
		return m_DicomFormat.getSelection();
	}

	public boolean isRunNameRule()
	{
		return m_RunNameRule.getSelection();
	}

	public boolean isCollection()
	{
		return m_Collection.getSelection();
	}

}