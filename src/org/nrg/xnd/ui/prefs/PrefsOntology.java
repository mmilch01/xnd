package org.nrg.xnd.ui.prefs;

import java.io.File;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.nrg.xnd.app.ConsoleView;
import org.nrg.xnd.app.FileView;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.rules.FileExtensionRule;
import org.nrg.xnd.rules.Macro;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.rules.RuleManager;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.ui.dialogs.EditMacroDialog;
import org.nrg.xnd.utils.Utils;

public class PrefsOntology extends PreferencePage
		implements
			IWorkbenchPreferencePage
{
	private Group group1;
	private Table m_RuleTable;
	private Button m_RemoveButton;
	private Button m_ViewModifyMacroButton;
	private Group group3;
	private Button m_addMacro;
	private Button m_AddFromFolder;
	private Label label1;
	private Text m_OntName;
	private Text m_ontoDescr;
	private Button m_BrowseOntFile;
	private Combo m_ontCombo;
	private MemoCombo m_ontComboM;
	private Label label3;
	private Label label2;
	private Group group2;
	private Button m_ModifyButton;
	private Button m_AddButton;
	private boolean m_bUpdOntology = false;

	@Override
	protected Control createContents(Composite parent)
	{
		this.noDefaultAndApplyButton();

		GridLayout parentLayout = new GridLayout();
		parent.setLayout(parentLayout);
		parent.setSize(400, 300);
		{
			group1 = new Group(parent, SWT.NONE);
			GridLayout group1Layout = new GridLayout();
			group1Layout.numColumns = 2;
			group1.setLayout(group1Layout);
			GridData group1LData = new GridData();
			group1LData.horizontalAlignment = GridData.FILL;
			group1LData.grabExcessHorizontalSpace = true;
			group1.setLayoutData(group1LData);
			group1.setText("Tag generation rules");
			{
				GridData m_RuleTableLData = new GridData();
				m_RuleTableLData.verticalAlignment = GridData.FILL;
				m_RuleTableLData.horizontalAlignment = GridData.FILL;
				m_RuleTableLData.verticalSpan = 5;
				m_RuleTableLData.grabExcessVerticalSpace = true;
				m_RuleTableLData.grabExcessHorizontalSpace = true;
				m_RuleTable = new Table(group1, SWT.DRAW_DELIMITER | SWT.RIGHT
						| SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
						| SWT.FULL_SELECTION);

				m_RuleTable.setLayoutData(m_RuleTableLData);
				m_RuleTable.addControlListener(new ControlAdapter()
				{
					@Override
					public void controlResized(ControlEvent evt)
					{
						Point p = m_RuleTable.getSize();
						m_RuleTable.getColumn(0).setWidth(p.x / 2);
						m_RuleTable.getColumn(1).setWidth(p.x / 2 - 15);
					}
				});
				m_RuleTable.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						int[] ind = m_RuleTable.getSelectionIndices();
						m_ViewModifyMacroButton.setEnabled(false);
						if (ind.length != 1)
							return;
						Rule r = null;
						try
						{
							r = RuleManager.getRule(m_RuleTable.getItem(ind[0])
									.getText(0));
						} catch (Exception e)
						{
							return;
						}
						if (r == null || !(r instanceof Macro) || r.isDefault())
							return;
						m_ViewModifyMacroButton.setEnabled(true);
					}
				});
				InitTable();
			}
			{
				m_AddButton = new Button(group1, SWT.PUSH | SWT.CENTER);
				GridData m_AddButtonLData = new GridData();
				m_AddButtonLData.grabExcessHorizontalSpace = true;
				m_AddButtonLData.verticalAlignment = GridData.BEGINNING;
				m_AddButtonLData.horizontalAlignment = GridData.FILL;
				m_AddButton.setLayoutData(m_AddButtonLData);
				m_AddButton.setText("Add rule from file");
				m_AddButton.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						String f = Utils.SelectFile(
								"Select rule descriptor xml file for import",
								"");
						if (f == null)
							return;
						Rule r = RuleManager.loadRule(new File(f));
						if (r == null)
						{
							ConsoleView
									.AppendMessage("Error parsing rule descriptor for file "
											+ f);
							return;
						}
						addRuleToTable(r);
					}
				});
			}
			{
				m_AddFromFolder = new Button(group1, SWT.PUSH | SWT.CENTER);
				GridData m_AddFromFolderLData = new GridData();
				m_AddFromFolderLData.horizontalAlignment = GridData.FILL;
				m_AddFromFolder.setLayoutData(m_AddFromFolderLData);
				m_AddFromFolder.setText("Add rules from folder");
				m_AddFromFolder.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						String f = Utils.SelectFolder(
								"Select folder with rule descriptor files", "");
						// Utils.SelectFile("Select rule descriptor xml file for import",
						// "");
						if (f == null)
							return;
						File fold = new File(f);
						if (!fold.exists())
							return;
						File[] files = fold.listFiles();
						String ext;
						for (File fl : files)
						{
							ext = FileExtensionRule.getFileExtension(fl);
							if (ext == null)
								continue;
							if (ext.toLowerCase().compareTo("xml") != 0)
								continue;
							Rule r = RuleManager.loadRule(fl);
							if (r == null)
								continue;
							addRuleToTable(r);
						}
					}
				});
			}
			{
				m_RemoveButton = new Button(group1, SWT.PUSH | SWT.CENTER);
				GridData m_RemoveButtonLData = new GridData();
				m_RemoveButtonLData.verticalAlignment = GridData.BEGINNING;
				m_RemoveButtonLData.horizontalAlignment = GridData.FILL;
				m_RemoveButton.setLayoutData(m_RemoveButtonLData);
				m_RemoveButton.setText("Delete");
				m_RemoveButton.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						TableItem[] ti = m_RuleTable.getSelection();
						int[] ind = m_RuleTable.getSelectionIndices();
						if (ti.length < 1)
							return;
						boolean bDef = false;
						for (TableItem t : ti)
						{
							if (RuleManager.getRule(t.getText(0)).isDefault())
							{
								bDef = true;
								continue;
							}
							RuleManager.deleteRule(t.getText(0));
						}
						if (bDef)
							Utils.ShowMessageBox("Warning",
									"Default rules are not deleted", Window.OK);
						/*
						 * for(int i:ind) {
						 * if(RuleManager.getRule(m_RuleTable.getItem
						 * (i).getText(0)).isDefault()) continue;
						 * m_RuleTable.remove(ind); }
						 */
						updateRuleList();
					}
				});
			}
			{
				m_ModifyButton = new Button(group1, SWT.PUSH | SWT.CENTER);
				GridData m_ModifyButtonLData = new GridData();
				m_ModifyButton.setLayoutData(m_ModifyButtonLData);
				m_ModifyButton.setText("Modify");
				m_ModifyButton.setVisible(false);
			}
			{
				group3 = new Group(group1, SWT.NONE);
				GridLayout group3Layout = new GridLayout();
				group3Layout.makeColumnsEqualWidth = true;
				group3Layout.numColumns = 2;
				group3.setLayout(group3Layout);
				GridData group3LData = new GridData();
				group3LData.horizontalAlignment = GridData.FILL;
				group3.setLayoutData(group3LData);
				group3.setText("Macros");
				{
					m_addMacro = new Button(group3, SWT.PUSH | SWT.CENTER);
					GridData m_addMacroLData = new GridData();
					m_addMacroLData.horizontalAlignment = GridData.FILL;
					m_addMacroLData.grabExcessHorizontalSpace = true;
					m_addMacro.setLayoutData(m_addMacroLData);
					m_addMacro.setText("Add");
					m_addMacro.addSelectionListener(new SelectionAdapter()
					{
						@Override
						public void widgetSelected(SelectionEvent evt)
						{
							EditMacroDialog emd = new EditMacroDialog(
									new Shell(), null);
							if (emd.open() == Window.OK)
							{
								Rule r = emd.getMacro();
								RuleManager.updateRule(r);
								addRuleToTable(r);
							}
						}
					});
				}
				{
					m_ViewModifyMacroButton = new Button(group3, SWT.PUSH
							| SWT.CENTER);
					GridData m_ViewModifyMacroButtonLData = new GridData();
					m_ViewModifyMacroButtonLData.horizontalAlignment = GridData.FILL;
					m_ViewModifyMacroButtonLData.grabExcessHorizontalSpace = true;
					m_ViewModifyMacroButton
							.setLayoutData(m_ViewModifyMacroButtonLData);
					m_ViewModifyMacroButton.setText("View/Edit");
					m_ViewModifyMacroButton.setEnabled(false);
					m_ViewModifyMacroButton
							.addSelectionListener(new SelectionAdapter()
							{
								@Override
								public void widgetSelected(SelectionEvent evt)
								{
									TableItem[] tis = m_RuleTable
											.getSelection();
									if (tis.length != 1)
										return;
									Rule r = RuleManager.getRule(tis[0]
											.getText(0));
									if (r == null || !(r instanceof Macro))
										return;
									EditMacroDialog emd = new EditMacroDialog(
											new Shell(), (Macro) r);
									if (emd.open() == Window.OK)
									{
										RuleManager.updateRule(emd.getMacro());
									}
								}
							});
				}
			}
		}
		{
			group2 = new Group(parent, SWT.NONE);
			GridLayout group2Layout = new GridLayout();
			group2Layout.numColumns = 2;
			group2.setLayout(group2Layout);
			GridData group2LData = new GridData();
			group2LData.horizontalAlignment = GridData.FILL;
			group2.setLayoutData(group2LData);
			group2.setText("Current ontology");
			{
				label1 = new Label(group2, SWT.NONE);
				GridData label1LData = new GridData();
				label1.setLayoutData(label1LData);
				label1.setText("Name:");
			}
			{
				GridData m_OntNameLData = new GridData();
				m_OntName = new Text(group2, SWT.BORDER);
				m_OntName.setEditable(false);
				m_OntName.setLayoutData(m_OntNameLData);
			}
			{
				label2 = new Label(group2, SWT.NONE);
				GridData label2LData = new GridData();
				label2LData.horizontalSpan = 2;
				label2.setLayoutData(label2LData);
				label2.setText("Description:");
			}
			{
				GridData m_ontoDescrLData = new GridData();
				m_ontoDescrLData.horizontalAlignment = GridData.FILL;
				m_ontoDescrLData.horizontalSpan = 2;
				m_ontoDescrLData.verticalSpan = 2;
				m_ontoDescrLData.grabExcessHorizontalSpace = true;
				m_ontoDescrLData.grabExcessVerticalSpace = true;
				m_ontoDescr = new Text(group2, SWT.MULTI | SWT.WRAP
						| SWT.BORDER);
				m_ontoDescr.setLayoutData(m_ontoDescrLData);
				m_ontoDescr.setText("\n\n");
				m_ontoDescr.setEditable(false);
			}
			{
				label3 = new Label(group2, SWT.NONE);
				GridData label3LData = new GridData();
				label3LData.horizontalAlignment = GridData.END;
				label3.setLayoutData(label3LData);
				label3.setText("File:");
			}
			{
				GridData m_ontComboLData = new GridData();
				m_ontComboLData.horizontalAlignment = GridData.FILL;
				m_ontCombo = new Combo(group2, SWT.READ_ONLY);
				m_ontComboM = new MemoCombo(m_ontCombo,
						"PrefsOntology.OntologyFile", 10);
				m_ontCombo.setLayoutData(m_ontComboLData);
				addComboEntry(XNDApp.app_Prefs.get("XMLDefaultOntology",
						DefaultOntologyManager.getDefaultLocation()));
				UpdateOntologyFields();

				m_ontCombo.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent evt)
					{
						String file = m_ontCombo.getText();
						if (DefaultOntologyManager
								.LoadTagDescriptorXML(new File(file)))
						{
							// addComboEntry(file);
							UpdateOntologyFields();
							m_bUpdOntology = true;
						} else
						{
							Utils.ShowMessageBox("Error",
									"The file does not contain valid ontology",
									Window.OK);
						}
					}
				});
			}
			{
				m_BrowseOntFile = new Button(group2, SWT.PUSH | SWT.CENTER);
				GridData m_BrowseOntFileLData = new GridData();
				m_BrowseOntFileLData.horizontalAlignment = GridData.END;
				m_BrowseOntFileLData.horizontalSpan = 2;
				m_BrowseOntFile.setLayoutData(m_BrowseOntFileLData);
				m_BrowseOntFile.setText("Browse...");
				m_BrowseOntFile.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						String file = Utils.SelectFile(
								"Select ontology XML file", m_ontCombo
										.getText());
						if (DefaultOntologyManager
								.LoadTagDescriptorXML(new File(file)))
						{
							addComboEntry(file);
							UpdateOntologyFields();
						} else
						{
							Utils.ShowMessageBox("Error",
									"The file does not contain valid ontology",
									Window.OK);
						}
					}
				});
			}
		}
		return null;
	}
	private void UpdateOntologyFields()
	{
		m_OntName.setText(DefaultOntologyManager.getID());
		m_ontoDescr.setText(DefaultOntologyManager.getDescr());
		XNDApp.app_Prefs.put("XMLDefaultOntology", m_ontCombo.getText());
	}
	public void InitTable()
	{
		TableColumn nmCol = new TableColumn(m_RuleTable, SWT.LEFT | SWT.BORDER);
		nmCol.setText("Name");
		nmCol.setWidth(50);
		TableColumn typeCol = new TableColumn(m_RuleTable, SWT.LEFT
				| SWT.BORDER);
		typeCol.setText("Type");
		typeCol.setWidth(50);
		m_RuleTable.setHeaderVisible(true);
		updateRuleList();
		// m_RuleTable.setLayoutData(new RowData(250,150));
		// m_RuleTable.setSize(250,150);
	}
	private void updateRuleList()
	{
		m_RuleTable.removeAll();
		for (Rule r : RuleManager.getRuleCollection())
			addRuleToTable(r);
	}

	private void addComboEntry(String s)
	{
		int i = 0, ind = -1;

		for (String it : m_ontCombo.getItems())
		{
			if (it.compareTo(s) == 0)
			{
				ind = i;
				break;
			}
			i++;
		}
		if (ind >= 0)
		{
			m_ontCombo.select(ind);
			return;
		} else
		{
			m_ontCombo.add(s, 0);
			m_ontCombo.select(0);
		}
	}
	private void addRuleToTable(Rule r)
	{
		TableItem ti = new TableItem(m_RuleTable, SWT.NONE);
		ti.setText(0, r.getuid());
		ti.setText(1, r.getTypeName());
	}
	@Override
	public boolean performOk()
	{
		m_ontComboM.Save();
		try
		{
			XNDApp.app_Prefs.flush();
		} catch (Exception e)
		{
		}
		FileView.UpdateMenus();
		if (m_bUpdOntology)
		{
			Utils.ShowMessageBox("Application restart",
					"Changing default ontology requires restart of XND.",
					Window.OK);
			XNDApp.app_localVM.InsertSystemTags();
			XNDApp.theApp.SerializeApp(false);
			PlatformUI.getWorkbench().restart();
		}
		return super.performOk();
	}
	/*
	 * public boolean performOK() { m_ontComboM.Save();
	 * try{XNDApp.app_Prefs.flush();}catch(Exception e){}
	 * FileView.UpdateMenus(); return true; }
	 */
	public void init(IWorkbench workbench)
	{
		// TODO Auto-generated method stub
	}
}
