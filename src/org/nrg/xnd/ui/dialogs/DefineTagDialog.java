package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.utils.Utils;
public class DefineTagDialog extends Dialog
{
	private Group m_NameProps;
	private Text m_NameText;
	private Button m_AnCheck;
	private Button m_Predef_vals;
	private Button m_ShowInTableButton;
	private Button m_RemoveVal;
	private Button m_addVal;
	private List list1;
	private Button m_MultiValueCheck;
	private Label m_NameLabel;
	private Group m_ValProps;
	private TagDescr m_TagDescr = null;
	private RepositoryViewManager m_rvm;
	public DefineTagDialog(Shell parentShell, RepositoryViewManager rvm)
	{
		super(parentShell);
		m_rvm = rvm;
	}
	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("New tag properties");
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.makeColumnsEqualWidth = true;
		parentLayout.numColumns = 2;
		parent.setLayout(parentLayout);
		{
			m_NameProps = new Group(parent, SWT.NONE);
			GridLayout m_NamePropsLayout = new GridLayout();
			m_NamePropsLayout.numColumns = 2;
			m_NameProps.setLayout(m_NamePropsLayout);
			GridData m_NamePropsLData = new GridData();
			m_NamePropsLData.horizontalAlignment = GridData.FILL;
			m_NamePropsLData.verticalAlignment = GridData.FILL;
			m_NameProps.setLayoutData(m_NamePropsLData);
			m_NameProps.setText("General");
			{
				m_NameLabel = new Label(m_NameProps, SWT.NONE);
				GridData m_NameLabelLData = new GridData();
				m_NameLabelLData.horizontalAlignment = GridData.END;
				m_NameLabel.setLayoutData(m_NameLabelLData);
				m_NameLabel.setText("Tag name (unique):");
			}
			{
				m_NameText = new Text(m_NameProps, SWT.BORDER);
			}
			{
				m_ShowInTableButton = new Button(m_NameProps, SWT.CHECK
						| SWT.LEFT);
				GridData m_ShowInTableButtonLData = new GridData();
				m_ShowInTableButtonLData.horizontalSpan = 2;
				m_ShowInTableButton.setLayoutData(m_ShowInTableButtonLData);
				m_ShowInTableButton.setText("Display as table column");
			}
		}
		{
			m_ValProps = new Group(parent, SWT.NONE);
			GridLayout m_ValPropsLayout = new GridLayout();
			m_ValPropsLayout.numColumns = 2;
			m_ValProps.setLayout(m_ValPropsLayout);
			GridData m_ValPropsLData = new GridData();
			m_ValPropsLData.horizontalAlignment = GridData.FILL;
			m_ValPropsLData.verticalAlignment = GridData.FILL;
			m_ValProps.setLayoutData(m_ValPropsLData);
			m_ValProps.setText("Value");
			{
				m_AnCheck = new Button(m_ValProps, SWT.CHECK | SWT.LEFT);
				m_AnCheck.setText("Force alphanumeric");
				m_AnCheck.setEnabled(true);
			}
			{
				m_MultiValueCheck = new Button(m_ValProps, SWT.CHECK | SWT.LEFT);
				m_MultiValueCheck.setText("Multi-value");
				m_MultiValueCheck.setEnabled(false);
			}
			{
				m_Predef_vals = new Button(m_ValProps, SWT.CHECK | SWT.LEFT);
				GridData m_Predef_valsLData = new GridData();
				m_Predef_valsLData.horizontalSpan = 2;
				m_Predef_vals.setLayoutData(m_Predef_valsLData);
				m_Predef_vals.setText("Predefined values:");
				m_Predef_vals.setEnabled(true);
				m_Predef_vals.addSelectionListener(new SelectionListener()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						boolean bSel = m_Predef_vals.getSelection();
						list1.setEnabled(bSel);
						m_addVal.setEnabled(bSel);
						m_RemoveVal.setEnabled(bSel);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
			}
			{
				GridData list1LData = new GridData();
				list1LData.verticalSpan = 3;
				list1LData.horizontalAlignment = GridData.FILL;
				list1 = new List(m_ValProps, SWT.BORDER);
				list1.setLayoutData(list1LData);
				list1.setEnabled(true);
			}
			{
				m_addVal = new Button(m_ValProps, SWT.PUSH | SWT.CENTER);
				GridData m_addValLData = new GridData();
				m_addValLData.horizontalAlignment = GridData.FILL;
				m_addVal.setLayoutData(m_addValLData);
				m_addVal.setText("Add");
				m_addVal.addSelectionListener(new SelectionListener()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						InputDialog d = new InputDialog(new Shell(),
								"Add tag value", "Value:", "",
								new IInputValidator()
								{
									@Override
									public String isValid(String txt)
									{
										if (txt == null || txt.length() < 1)
											return "Value cannot be empty string";
										String[] vals = list1.getItems();
										if (vals == null || vals.length < 1)
											return null;
										for (String s : vals)
											if (txt.compareTo(s) == 0)
												return "Value already exists";
										return null;
									}
								});
						if (d.open() == Window.OK)
							list1.add(d.getValue());
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
				m_addVal.setEnabled(true);
			}
			{
				m_RemoveVal = new Button(m_ValProps, SWT.PUSH | SWT.CENTER);
				GridData m_RemoveValLData = new GridData();
				m_RemoveValLData.horizontalAlignment = GridData.FILL;
				m_RemoveVal.setLayoutData(m_RemoveValLData);
				m_RemoveVal.setText("Remove");
				m_RemoveVal.addSelectionListener(new SelectionListener()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						list1.remove(list1.getSelectionIndices());
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
				m_RemoveVal.setEnabled(true);
			}
		}
		UpdateData(false);
		return super.createDialogArea(parent);
	}
	public TagDescr GetTagDescr()
	{
		return m_TagDescr;
	}
	private boolean Validate()
	{
		if (m_TagDescr == null)
			return false;
		if (m_TagDescr.GetName() == null || m_TagDescr.GetName().length() < 1)
		{
			Utils.ShowMessageBox("Invalid input", "Tag name cannot be empty",
					Window.OK);
			return false;
		}
		if (m_rvm.DBTagFind(m_TagDescr.GetName()).length > 0)
		{
			Utils.ShowMessageBox("Invalid input",
					"Tag with this name already exists", Window.OK);
			return false;
		}
		return true;
	}
	private void UpdateData(boolean bFromUI)
	{
		if (bFromUI)
		{
			m_TagDescr = new TagDescr(m_NameText.getText(), (m_MultiValueCheck
					.getSelection() ? TagDescr.MULTI_VALUE : 0)
					| (m_AnCheck.getSelection()
							? TagDescr.VALUE_ALPHANUMERIC
							: 0)
					| (m_ShowInTableButton.getSelection()
							? TagDescr.TABLE_DISPLAY
							: 0));
			if (m_Predef_vals.getSelection())
			{
				for (String s : list1.getItems())
					m_TagDescr.AddValue(s);
				m_TagDescr.SetAttr(TagDescr.PREDEF_VALUES);
			}
		} else
		{
			boolean bSel = false;
			if (m_TagDescr != null)
			{
				m_NameText.setText(m_TagDescr.GetName());
				m_MultiValueCheck.setSelection(m_TagDescr
						.IsSet(TagDescr.MULTI_VALUE));
				m_AnCheck.setSelection(m_TagDescr
						.IsSet(TagDescr.VALUE_ALPHANUMERIC));
				m_ShowInTableButton.setSelection(m_TagDescr
						.IsSet(TagDescr.TABLE_DISPLAY));
				String[] vals = m_TagDescr.GetValues();
				if (vals == null)
					bSel = false;
				else
				{
					m_Predef_vals.setSelection(true);
					list1.setItems(vals);
					bSel = true;
				}
			}
			list1.setEnabled(bSel);
			m_addVal.setEnabled(bSel);
			m_RemoveVal.setEnabled(bSel);
		}
	}

	@Override
	protected void okPressed()
	{
		UpdateData(true);
		if (!Validate())
			return;
		super.okPressed();
	}
}
