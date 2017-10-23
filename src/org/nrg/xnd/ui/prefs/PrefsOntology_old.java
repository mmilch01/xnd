package org.nrg.xnd.ui.prefs;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.Utils;

public class PrefsOntology_old extends PreferencePage
		implements
			IWorkbenchPreferencePage
{
	private Label m_oLabel;
	private Combo m_oText;
	private MemoCombo m_oTextM;
	private String m_oFile;
	private Button m_oTestButton;
	private Button m_DICOMCollectionsCheck;
	private Button m_oBrowseButton;

	private Combo m_drText;
	private MemoCombo m_drTextM;
	private String m_drFile;
	private Button m_drTestButton;
	private Button m_drBrowseButton;
	private Label m_drLabel;

	private Combo m_trText;
	private MemoCombo m_trTextM;
	private String m_trFile;
	private Button m_trTestButton;
	private Button m_trBrowseButton;
	private Label m_trLabel;

	private Button m_nrBrowseButton;
	private Button m_nrTestButton;
	private Combo m_nrText;
	private MemoCombo m_nrTextM;
	private String m_nrFile;
	private Label m_nrLabel;

	@Override
	protected Control createContents(Composite parent)
	{
		noDefaultAndApplyButton();
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 4;
		parent.setLayout(parentLayout);
		parent.setSize(424, 142);
		{
			m_oLabel = new Label(parent, SWT.NONE);
			m_oLabel.setText("Default ontology XML:");
		}
		{
			GridData m_oTextLData = new GridData();
			m_oTextLData.horizontalAlignment = GridData.FILL;
			m_oTextLData.grabExcessHorizontalSpace = true;
			m_oTextLData.heightHint = 13;
			m_oText = new Combo(parent, SWT.NONE);
			m_oText.setLayoutData(m_oTextLData);
			m_oTextM = new MemoCombo(m_oText, "PrefsOntology.OntologyXML", 10);
		}
		{
			m_oBrowseButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			m_oBrowseButton.setText("Browse...");
			m_oBrowseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					m_oFile = Utils.SelectFile("Select ontology XML file",
							m_oFile);
					UpdateData(false);
				}
			});
		}
		{
			m_oTestButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			m_oTestButton.setText("Test");
			/*
			 * m_oTestButton.addSelectionListener(new SelectionAdapter() {
			 * public void widgetSelected(SelectionEvent evt) {
			 * if(LoadXML(Utils.XML_DEFAULT_ONTOLOGY))
			 * Utils.ShowMessageBox("Ontology XML test", "Success", SWT.OK);
			 * else Utils.ShowMessageBox("Ontology XML test", "Failed", SWT.OK);
			 * } });
			 */
		}
		// DICOM rule
		{
			m_drLabel = new Label(parent, SWT.NONE);
			m_drLabel.setText("DICOM rule XML:");
		}
		{
			GridData m_drTextLData = new GridData();
			m_drTextLData.horizontalAlignment = GridData.FILL;
			m_drTextLData.grabExcessHorizontalSpace = true;
			m_drText = new Combo(parent, SWT.BORDER);
			m_drText.setLayoutData(m_drTextLData);
			m_drTextM = new MemoCombo(m_drText, "PrefsOntology.DICOMRuleXML",
					10);
		}
		{
			m_drBrowseButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			m_drBrowseButton.setText("Browse...");
			m_drBrowseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					m_drFile = Utils.SelectFile("Select DICOM rule XML file",
							m_drFile);
					UpdateData(false);
				}
			});
		}
		{
			m_drTestButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			m_drTestButton.setText("Test");
			/*
			 * m_drTestButton.addSelectionListener(new SelectionAdapter() {
			 * public void widgetSelected(SelectionEvent evt) {
			 * if(LoadXML(Utils.XML_DEFAULT_RULE_DICOM))
			 * Utils.ShowMessageBox("DICOM rule test", "Success", SWT.OK); else
			 * Utils.ShowMessageBox("DICOM rule test", "Failed", SWT.OK); } });
			 */
		}
		{
			m_DICOMCollectionsCheck = new Button(parent, SWT.CHECK | SWT.LEFT);
			GridData m_DICOMCollectionsCheckLData = new GridData();
			m_DICOMCollectionsCheckLData.horizontalSpan = 4;
			m_DICOMCollectionsCheck.setLayoutData(m_DICOMCollectionsCheckLData);
			m_DICOMCollectionsCheck
					.setText("Generate collections when using DICOM rule (recommended for large datasets)");
			m_DICOMCollectionsCheck.setSelection(XNDApp.app_Prefs.getBoolean(
					"DICOMRuleUseCollections", true));
		}

		// tag pattern rule
		{
			m_trLabel = new Label(parent, SWT.NONE);
			m_trLabel.setText("Tag pattern rule XML:");
		}
		{
			GridData m_trTextLData = new GridData();
			m_trTextLData.horizontalAlignment = GridData.FILL;
			m_trTextLData.grabExcessHorizontalSpace = true;
			m_trText = new Combo(parent, SWT.BORDER);
			m_trText.setLayoutData(m_trTextLData);
			m_trTextM = new MemoCombo(m_trText,
					"PrefsOntology.TagPatternRuleXML", 10);

		}
		{
			m_trBrowseButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			m_trBrowseButton.setText("Browse...");
			m_trBrowseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					m_trFile = Utils.SelectFile(
							"Select tag pattern rule XML file", m_trFile);
					UpdateData(false);
				}
			});
		}
		{
			m_trTestButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			m_trTestButton.setText("Test");
			/*
			 * m_trTestButton.addSelectionListener(new SelectionAdapter() {
			 * public void widgetSelected(SelectionEvent evt) {
			 * if(LoadXML(Utils.XML_DEFAULT_RULE_TAG))
			 * Utils.ShowMessageBox("Tag pattern rule XML test", "Success",
			 * SWT.OK); else Utils.ShowMessageBox("Tag pattern rule XML test",
			 * "Failed", SWT.OK); } });
			 */
		}
		// name rule
		{
			m_nrLabel = new Label(parent, SWT.NONE);
			m_nrLabel.setText("Naming rule XML:");
		}
		{
			GridData m_nrTextLData = new GridData();
			m_nrTextLData.horizontalAlignment = GridData.FILL;
			m_nrTextLData.grabExcessHorizontalSpace = true;
			m_nrText = new Combo(parent, SWT.BORDER);
			m_nrText.setLayoutData(m_nrTextLData);
			m_nrTextM = new MemoCombo(m_nrText, "PrefsOntology.NameRuleXML", 10);
		}
		{
			m_nrBrowseButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			m_nrBrowseButton.setText("Browse...");
			m_nrBrowseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					m_nrFile = Utils.SelectFile("Select naming rule XML file",
							m_nrFile);
					UpdateData(false);
				}
			});
		}
		{
			m_nrTestButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			m_nrTestButton.setText("Test");
			/*
			 * m_nrTestButton.addSelectionListener(new SelectionAdapter() {
			 * public void widgetSelected(SelectionEvent evt) {
			 * if(LoadXML(Utils.XML_DEFAULT_RULE_NAMING))
			 * Utils.ShowMessageBox("Name rule XML test", "Success", SWT.OK);
			 * else Utils.ShowMessageBox("Name rule XML test", "Failed",
			 * SWT.OK); } });
			 */
		}

		// TODO Auto-generated method stub
		UpdateData(false);
		return null;
	}
	private boolean LoadXML(byte type)
	{
		/*
		 * UpdateData(true); switch(type) { case Utils.XML_DEFAULT_ONTOLOGY:
		 * return DefaultOntologyManager.LoadTagDescriptorXML(new
		 * File(m_oFile)); case Utils.XML_DEFAULT_RULE_DICOM: return
		 * DICOMRule.GetInstance(XNDApp.app_localVM,
		 * XNDApp.app_Prefs.getBoolean("DICOMRuleUseCollections", true))
		 * .LoadRuleDescriptor(new File(m_drFile)); case
		 * Utils.XML_DEFAULT_RULE_NAMING: return
		 * NameRule.GetInstance(XNDApp.app_localVM).LoadRuleDescriptor(new
		 * File(m_nrFile)); case Utils.XML_DEFAULT_RULE_TAG: return
		 * ModifyTagValueRule
		 * .GetInstance(XNDApp.app_localVM).LoadRuleDescriptor(new
		 * File(m_trFile)); }
		 */
		return false;
	}
	private void UpdateData(boolean bStore)
	{
		if (!bStore)
		{
			m_oText.setText(m_oFile);
			m_nrText.setText(m_nrFile);
			m_drText.setText(m_drFile);
			m_trText.setText(m_trFile);
		} else
		{
			m_oFile = m_oText.getText();
			m_nrFile = m_nrText.getText();
			m_drFile = m_drText.getText();
			m_trFile = m_trText.getText();
		}
	}
	@Override
	public boolean performOk()
	{
		/*
		 * if(!LoadXML(Utils.XML_DEFAULT_ONTOLOGY)) {
		 * if(Utils.ShowMessageBox("XML parsing error",
		 * "Ontology XML file either does not exist or contains invalid XML. Ignore?"
		 * , SWT.OK|SWT.CANCEL)==SWT.CANCEL) return false; }
		 * if(!LoadXML(Utils.XML_DEFAULT_RULE_DICOM)) {
		 * if(Utils.ShowMessageBox("XML parsing error",
		 * "DICOM rule XML file either does not exist or contains invalid XML. Ignore?"
		 * , SWT.OK|SWT.CANCEL)==SWT.CANCEL) return false; }
		 * if(!LoadXML(Utils.XML_DEFAULT_RULE_NAMING)) {
		 * if(Utils.ShowMessageBox("XML parsing error",
		 * "Naming rule XML file either does not exist or contains invalid XML. Ignore?"
		 * , SWT.OK|SWT.CANCEL)==SWT.CANCEL) return false; }
		 * if(!LoadXML(Utils.XML_DEFAULT_RULE_TAG)) {
		 * if(Utils.ShowMessageBox("XML parsing error",
		 * "Tag pattern rule XML file either does not exist or contains invalid XML. Ignore?"
		 * , SWT.OK|SWT.CANCEL)==SWT.CANCEL) return false; }
		 */
		XNDApp.app_Prefs.putBoolean("DICOMRuleUseCollections",
				m_DICOMCollectionsCheck.getSelection());
		XNDApp.app_Prefs.put("XMLDefaultOntology", m_oFile);
		XNDApp.app_Prefs.put("XMLDefaultDICOMRule", m_drFile);
		XNDApp.app_Prefs.put("XMLDefaultNamingRule", m_nrFile);
		XNDApp.app_Prefs.put("XMLDefaultTagRule", m_trFile);
		m_drTextM.Save();
		m_nrTextM.Save();
		m_oTextM.Save();
		m_trTextM.Save();
		return true;
	}
	@Override
	public void init(IWorkbench workbench)
	{
		/*
		 * m_oFile=XNDApp.app_Prefs.get("XMLDefaultOntology",
		 * Utils.GetDefaultLocation(Utils.XML_DEFAULT_ONTOLOGY));
		 * m_drFile=XNDApp.app_Prefs.get("XMLDefaultDICOMRule",
		 * Utils.GetDefaultLocation(Utils.XML_DEFAULT_RULE_DICOM));
		 * m_nrFile=XNDApp.app_Prefs.get("XMLDefaultNamingRule",
		 * Utils.GetDefaultLocation(Utils.XML_DEFAULT_RULE_NAMING));
		 * m_trFile=XNDApp.app_Prefs.get("XMLDefaultTagRule",
		 * Utils.GetDefaultLocation(Utils.XML_DEFAULT_RULE_TAG));
		 */
	}
}