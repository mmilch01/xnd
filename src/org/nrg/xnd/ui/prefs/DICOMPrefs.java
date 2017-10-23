package org.nrg.xnd.ui.prefs;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.dicom.AEList.AE;

public class DICOMPrefs extends PreferencePage
		implements
			IWorkbenchPreferencePage
{
	private Group group1;
	private Label label1;
	private Button m_applyChanges;
	private Button m_delAE;
	private Text m_AETitle;
	private Label label5;
	private Text m_NetwAddr;
	private Label label4;
	private Text m_RecvPort;
	private Label label3;
	private Text m_sendPort;
	private Label label2;
	private Button m_newAEButton;
	private Combo m_AENameCombo;
	private MemoCombo m_AENameComboM;

	@Override
	protected Control createContents(Composite parent)
	{
		noDefaultAndApplyButton();

		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 3;
		parent.setLayout(parentLayout);
		{
			label1 = new Label(parent, SWT.NONE);
			GridData label1LData = new GridData();
			label1LData.horizontalAlignment = GridData.END;
			label1.setLayoutData(label1LData);
			label1.setText("Name:");
		}
		{
			GridData m_AENameComboLData = new GridData();
			m_AENameComboLData.horizontalAlignment = GridData.FILL;
			m_AENameComboLData.grabExcessHorizontalSpace = true;
			m_AENameCombo = new Combo(parent, SWT.READ_ONLY);
			m_AENameCombo.setLayoutData(m_AENameComboLData);
			m_AENameComboM = new MemoCombo(m_AENameCombo,
					"DICOMPrefs.NameCombo", 100);
			m_AENameComboM.Sync(XNDApp.app_aeList.getAENames());
			m_AENameCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					updateData(false);
				}
			});
		}
		{
			m_delAE = new Button(parent, SWT.PUSH | SWT.CENTER);
			GridData m_delAELData = new GridData();
			m_delAELData.horizontalAlignment = GridData.CENTER;
			m_delAE.setLayoutData(m_delAELData);
			m_delAE.setText("Delete");
			m_delAE.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					if (m_AENameCombo.getText().compareTo("localhost") == 0)
						return;
					XNDApp.app_aeList.delAE(m_AENameCombo.getText());
					m_AENameCombo.remove(m_AENameCombo.getText());
					m_AENameCombo.select(0);
				}
			});
		}
		{
			group1 = new Group(parent, SWT.NONE);
			GridLayout group1Layout = new GridLayout();
			group1Layout.numColumns = 4;
			group1.setLayout(group1Layout);
			GridData group1LData = new GridData();
			group1LData.grabExcessHorizontalSpace = true;
			group1LData.horizontalAlignment = GridData.FILL;
			group1LData.horizontalSpan = 3;
			group1.setLayoutData(group1LData);
			group1.setText("DICOM Application Entity (AE) parameters");
			{
				label5 = new Label(group1, SWT.NONE);
				GridData label5LData = new GridData();
				label5LData.horizontalAlignment = GridData.END;
				label5.setLayoutData(label5LData);
				label5.setText("AE Title:");
			}
			{
				GridData m_AETitleLData = new GridData();
				m_AETitleLData.horizontalAlignment = GridData.FILL;
				m_AETitleLData.grabExcessHorizontalSpace = true;
				m_AETitleLData.horizontalSpan = 3;
				m_AETitle = new Text(group1, SWT.BORDER);
				m_AETitle.setLayoutData(m_AETitleLData);
			}
			{
				label4 = new Label(group1, SWT.NONE);
				GridData label4LData = new GridData();
				label4.setLayoutData(label4LData);
				label4.setText("Network address:");
			}
			{
				GridData m_NetwAddrLData = new GridData();
				m_NetwAddrLData.horizontalAlignment = GridData.FILL;
				m_NetwAddrLData.horizontalSpan = 3;
				m_NetwAddr = new Text(group1, SWT.BORDER);
				m_NetwAddr.setLayoutData(m_NetwAddrLData);
			}
			{
				label2 = new Label(group1, SWT.NONE);
				GridData label2LData = new GridData();
				label2LData.horizontalAlignment = GridData.END;
				label2.setLayoutData(label2LData);
				label2.setText("Send port:");
			}
			{
				m_sendPort = new Text(group1, SWT.BORDER);
				GridData m_sendPortLData = new GridData();
				m_sendPortLData.horizontalAlignment = GridData.FILL;
				m_sendPortLData.grabExcessHorizontalSpace = true;
				m_sendPort.setLayoutData(m_sendPortLData);
			}
			{
				label3 = new Label(group1, SWT.NONE);
				GridData label3LData = new GridData();
				label3.setLayoutData(label3LData);
				label3.setText("Receive port:");
			}
			{
				GridData m_RecvPortLData = new GridData();
				m_RecvPortLData.horizontalAlignment = GridData.FILL;
				m_RecvPortLData.grabExcessHorizontalSpace = true;
				m_RecvPort = new Text(group1, SWT.BORDER);
				m_RecvPort.setLayoutData(m_RecvPortLData);
			}
		}
		{
			m_newAEButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			GridData m_newAEButtonLData = new GridData();
			m_newAEButtonLData.horizontalAlignment = GridData.CENTER;
			m_newAEButton.setLayoutData(m_newAEButtonLData);
			m_newAEButton.setText("New AE...");
			m_newAEButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					InputDialog id = new InputDialog(getShell(),
							"Remote destination name", "Enter remote AE name:",
							"", new IInputValidator()
							{
								@Override
								public String isValid(String newText)
								{
									if (newText.length() < 1)
										return "Enter non-empty string";
									if (XNDApp.app_aeList.getAE(newText) != null)
									{
										return "AE with such name already exists";
									}
									return null;
								}
							});
					if (id.open() != Window.OK)
						return;
					AE newAE = XNDApp.app_aeList.new AE(id.getValue());
					XNDApp.app_aeList.addAE(newAE);
					m_AENameCombo.add(id.getValue(), 0);
					m_AENameCombo.select(0);
					updateData(false);
				}
			});
		}
		{
			m_applyChanges = new Button(parent, SWT.PUSH | SWT.CENTER);
			GridData m_applyChangesLData = new GridData();
			m_applyChangesLData.horizontalAlignment = GridData.END;
			m_applyChangesLData.horizontalSpan = 2;
			m_applyChanges.setLayoutData(m_applyChangesLData);
			m_applyChanges.setText("Apply changes");
			m_applyChanges.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					updateData(true);
				}
			});
		}
		updateData(false);
		return null;
	}
	private void updateData(boolean bFromUI)
	{
		if (bFromUI)
		{
			AE cur_ae = XNDApp.app_aeList.getAE(m_AENameCombo.getText());
			cur_ae.m_netName = m_NetwAddr.getText();
			cur_ae.m_title = m_AETitle.getText();
			cur_ae.m_recvPort = Integer.valueOf(m_RecvPort.getText());
			cur_ae.m_sendPort = Integer.valueOf(m_sendPort.getText());
		} else
		{
			AE cur_ae = XNDApp.app_aeList.getAE(m_AENameCombo.getText());
			m_NetwAddr.setText(cur_ae.m_netName);
			m_AETitle.setText(cur_ae.m_title);
			m_RecvPort.setText(String.valueOf(cur_ae.m_recvPort));
			m_sendPort.setText(String.valueOf(cur_ae.m_sendPort));
		}
	}
	@Override
	public boolean performOk()
	{
		updateData(true);
		XNDApp.app_aeList.Save();
		return super.performOk();
	}

	@Override
	public void init(IWorkbench workbench)
	{
		// TODO Auto-generated method stub
	}
}