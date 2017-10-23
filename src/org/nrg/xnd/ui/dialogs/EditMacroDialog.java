package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.rules.Macro;
import org.nrg.xnd.rules.Rule;
import org.nrg.xnd.rules.RuleManager;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.Utils;

public class EditMacroDialog extends Dialog
{
	private List m_OperationList;
	private Group m_AddOperationGroup;
	private Label label1;
	private Combo m_TagName;
	// private MemoCombo m_TagNameMemo;
	private Combo m_TagVal;
	private MemoCombo m_TagValMemo;
	private Text m_Name;
	private Label label5;
	private Button m_RemoveBtn;
	private Button m_AddBtn;
	private Button m_DownBtn;
	private Button m_UpBtn;
	// private MemoCombo m_RuleIDMemo;
	private Combo m_RuleIDCombo;
	private Label label4;
	private Group m_RuleGroup;
	private Label label3;
	private Label label2;
	private Group m_TagOperGroup;
	// private MemoCombo m_operationTypeMemo;
	private Combo m_operationType;
	private Macro m_macro = null;

	public Macro getMacro()
	{
		return m_macro;
	}
	@Override
	protected void okPressed()
	{
		// m_TagNameMemo.Save();
		m_TagValMemo.Save();
		String msg = UpdateData(true);
		if (msg != null)
		{
			Utils.ShowMessageBox("Please correct before saving:", msg,
					Window.OK);
			return;
		}
		msg = Validate();
		if (msg != null)
		{
			Utils.ShowMessageBox("Cannot save macro", msg, Window.OK);
			return;
		}
		RuleManager.updateRule(m_macro);
		super.okPressed();
	}
	public EditMacroDialog(Shell parentShell, Macro macro)
	{
		super(parentShell);
		if (macro == null)
			m_macro = new Macro(XNDApp.app_localVM, "");
		else
			m_macro = macro;
	}

	private String Validate()
	{
		String ts = m_Name.getText();
		if (ts.length() < 1)
			return "Empty or invalid macro name";
		Rule r = RuleManager.getRule(ts);
		if (r != null)
		{
			if (!(r instanceof Macro))
				return "Please choose a different name";
		}
		if (m_OperationList.getItemCount() < 1)
		{
			return "Macro cannot be empty";
		}
		return null;
	}

	public String UpdateData(boolean bFromGUI)
	{
		if (bFromGUI)
		{
			String msg = Validate();
			if (msg != null)
			{
				// Utils.ShowMessageBox("Correct input", msg, Window.OK);
				return msg;
			}
			m_macro.setuid(m_Name.getText());
		} else
		{
			if (m_macro == null)
				return null;
			// m_Name.setText(m_macro.getuid());
			m_OperationList.removeAll();
			for (String s : m_macro.getOperationList())
			{
				m_OperationList.add(s);
			}
		}
		return null;
	}
	protected void EnableAddControls()
	{
		boolean bSettag = m_operationType.getSelectionIndex() == Macro.Operation.SETTAG, bRule = m_operationType
				.getSelectionIndex() == Macro.Operation.APPLYRULE;
		for (Control c : m_TagOperGroup.getChildren())
			c.setEnabled(bSettag);
		for (Control c : m_RuleGroup.getChildren())
			c.setEnabled(bRule);
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 4;
		parent.setLayout(parentLayout);
		{
			label5 = new Label(parent, SWT.NONE);
			GridData label5LData = new GridData();
			label5LData.horizontalAlignment = GridData.FILL;
			label5.setLayoutData(label5LData);
			label5.setText("Macro name:");
		}
		{
			GridData m_NameLData = new GridData();
			m_NameLData.horizontalAlignment = GridData.FILL;
			m_NameLData.horizontalSpan = 4;
			m_NameLData.heightHint = 13;
			m_Name = new Text(parent, SWT.BORDER);
			m_Name.setLayoutData(m_NameLData);
			m_Name.setText(m_macro.getuid());
		}
		{
			GridData m_OperationListLData = new GridData();
			m_OperationListLData.horizontalAlignment = GridData.FILL;
			m_OperationListLData.grabExcessHorizontalSpace = true;
			m_OperationListLData.verticalSpan = 3;
			m_OperationListLData.verticalAlignment = GridData.FILL;
			m_OperationListLData.horizontalSpan = 3;
			m_OperationList = new List(parent, SWT.BORDER);
			m_OperationList.setLayoutData(m_OperationListLData);
		}
		{
			m_AddOperationGroup = new Group(parent, SWT.NONE);
			GridLayout m_AddOperationGroupLayout = new GridLayout();
			m_AddOperationGroupLayout.numColumns = 2;
			m_AddOperationGroup.setLayout(m_AddOperationGroupLayout);
			GridData m_AddOperationGroupLData = new GridData();
			m_AddOperationGroupLData.horizontalAlignment = GridData.FILL;
			m_AddOperationGroupLData.heightHint = 174;
			m_AddOperationGroupLData.verticalSpan = 3;
			m_AddOperationGroup.setLayoutData(m_AddOperationGroupLData);
			m_AddOperationGroup.setText("Add operation");
			{
				label1 = new Label(m_AddOperationGroup, SWT.NONE);
				GridData label1LData = new GridData();
				label1.setLayoutData(label1LData);
				label1.setText("Operation:");
			}
			{
				GridData m_operationTypeLData = new GridData();
				m_operationType = new Combo(m_AddOperationGroup, SWT.READ_ONLY
						| SWT.DROP_DOWN);
				m_operationType.setLayoutData(m_operationTypeLData);
				m_operationType.addModifyListener(new ModifyListener()
				{
					@Override
					public void modifyText(ModifyEvent evt)
					{
						EnableAddControls();
					}
				});
				for (String type : Macro.getOpTypeList())
					m_operationType.add(type);
				try
				{
					m_operationType.select(0);
				} catch (Exception e)
				{
				}
			}
			{
				m_TagOperGroup = new Group(m_AddOperationGroup, SWT.NONE);
				GridLayout m_TagOperGroupLayout = new GridLayout();
				m_TagOperGroupLayout.makeColumnsEqualWidth = true;
				m_TagOperGroupLayout.numColumns = 2;
				m_TagOperGroup.setLayout(m_TagOperGroupLayout);
				GridData m_TagOperGroupLData = new GridData();
				m_TagOperGroupLData.horizontalAlignment = GridData.FILL;
				m_TagOperGroupLData.horizontalSpan = 2;
				m_TagOperGroup.setLayoutData(m_TagOperGroupLData);
				{
					label2 = new Label(m_TagOperGroup, SWT.NONE);
					GridData label2LData = new GridData();
					label2.setLayoutData(label2LData);
					label2.setText("Tag name:");
				}
				{
					label3 = new Label(m_TagOperGroup, SWT.NONE);
					GridData label3LData = new GridData();
					label3.setLayoutData(label3LData);
					label3.setText("Tag value:");
				}
				{
					GridData m_TagNameLData = new GridData();
					m_TagName = new Combo(m_TagOperGroup, SWT.NONE);
					m_TagName.setLayoutData(m_TagNameLData);

					for (TagDescr td : XNDApp.app_localVM.GetVisibleTagList())
						m_TagName.add(td.GetName());

					// m_TagNameMemo=new
					// MemoCombo(m_TagName,"EditMacroDialog.TagName",10);
				}
				{
					GridData m_TagValLData = new GridData();
					m_TagVal = new Combo(m_TagOperGroup, SWT.NONE);
					m_TagVal.setLayoutData(m_TagValLData);
					m_TagValMemo = new MemoCombo(m_TagVal,
							"EditMacroDialog.TagVal", 10);
				}
			}
			{
				GridData m_RuleGroupLData = new GridData();
				m_RuleGroupLData.horizontalAlignment = GridData.FILL;
				m_RuleGroupLData.horizontalSpan = 2;
				m_RuleGroupLData.verticalSpan = -1;
				m_RuleGroup = new Group(m_AddOperationGroup, SWT.NONE);
				GridLayout m_RuleGroupLayout = new GridLayout();
				m_RuleGroupLayout.makeColumnsEqualWidth = true;
				m_RuleGroupLayout.numColumns = 2;
				m_RuleGroup.setLayout(m_RuleGroupLayout);
				m_RuleGroup.setLayoutData(m_RuleGroupLData);
				{
					label4 = new Label(m_RuleGroup, SWT.NONE);
					GridData label4LData = new GridData();
					label4LData.horizontalAlignment = GridData.END;
					label4.setLayoutData(label4LData);
					label4.setText("Rule ID:");
				}
				{
					GridData m_RuleIDComboLData = new GridData();
					m_RuleIDCombo = new Combo(m_RuleGroup, SWT.READ_ONLY
							| SWT.DROP_DOWN);
					m_RuleIDCombo.setLayoutData(m_RuleIDComboLData);
					for (Rule r : RuleManager.getRuleCollection())
						m_RuleIDCombo.add(r.getuid());
					m_RuleIDCombo.select(0);
				}
			}
			{
				m_AddBtn = new Button(m_AddOperationGroup, SWT.PUSH
						| SWT.CENTER);
				GridData m_AddBtnLData = new GridData();
				m_AddBtnLData.horizontalAlignment = GridData.END;
				m_AddBtnLData.horizontalSpan = 3;
				m_AddBtn.setLayoutData(m_AddBtnLData);
				m_AddBtn.setText("Add");
				m_AddBtn.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						int ind = m_operationType.getSelectionIndex();
						switch (ind)
						{
							case Macro.Operation.MANAGE :
								m_macro.addOp(ind, null);
								UpdateData(false);
								return;
							case Macro.Operation.SETTAG :
								if (m_TagName.getText().length() < 1)
								{
									Utils.ShowMessageBox("Empty tag name",
											"Tag name cannot be empty",
											Window.OK);
									return;
								}
								m_macro.addOp(ind, new ItemTag(m_TagName
										.getText(), m_TagVal.getText()));
								UpdateData(false);
								return;
							case Macro.Operation.APPLYRULE :
								Rule r = RuleManager.getRule(m_RuleIDCombo
										.getText());
								if (r == null)
								{
									Utils.ShowMessageBox("Unknown rule",
											"Invalid rule ID", Window.OK);
								}
								m_macro.addOp(ind, r);
								UpdateData(false);
								return;
						}
					}
				});
			}
		}
		{
			m_UpBtn = new Button(parent, SWT.PUSH | SWT.CENTER);
			GridData m_UpBtnLData = new GridData();
			m_UpBtnLData.horizontalAlignment = GridData.END;
			m_UpBtn.setLayoutData(m_UpBtnLData);
			m_UpBtn.setText("Up");
			m_UpBtn.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					int ind = m_OperationList.getSelectionIndex();
					m_macro.moveOp(m_OperationList.getSelectionIndex(), -1);
					ind = Math.max(ind - 1, 0);
					UpdateData(false);
					m_OperationList.setSelection(ind);
				}
			});
		}
		{
			m_DownBtn = new Button(parent, SWT.PUSH | SWT.CENTER);
			GridData m_DownBtnLData = new GridData();
			m_DownBtn.setLayoutData(m_DownBtnLData);
			m_DownBtn.setText("Down");
			m_DownBtn.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					int ind = m_OperationList.getSelectionIndex();
					m_macro.moveOp(m_OperationList.getSelectionIndex(), 1);
					UpdateData(false);
					ind = Math.min(m_OperationList.getItemCount(), ind + 1);
					m_OperationList.setSelection(ind);
				}
			});
		}
		{
			m_RemoveBtn = new Button(parent, SWT.PUSH | SWT.CENTER);
			GridData m_RemoveBtnLData = new GridData();
			m_RemoveBtnLData.horizontalAlignment = GridData.END;
			m_RemoveBtn.setLayoutData(m_RemoveBtnLData);
			m_RemoveBtn.setText("Remove");
			m_RemoveBtn.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					int ind = m_OperationList.getSelectionIndex();
					if (ind < 0)
						return;
					m_macro.removeOp(ind);
					UpdateData(false);
				}
			});
		}
		Composite composite = (Composite) super.createDialogArea(parent);
		UpdateData(false);
		EnableAddControls();
		// parent.setSize(385, 241);
		return composite;
	}

}