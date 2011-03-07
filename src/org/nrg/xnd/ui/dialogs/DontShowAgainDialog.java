package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.nrg.xnd.app.XNDApp;

public class DontShowAgainDialog extends Dialog
{
	public static final int UNMANAGE_FOLDER = 0, UNMANAGE_FILES = 1;
	private Button m_ShowAgainButton;
	private String m_id, m_title, m_msg;
	private Label m_msgLabel;
	public DontShowAgainDialog(String id, String title, String msg)
	{
		super(Display.getDefault().getActiveShell());
		m_id = id;
		m_title = title;
		m_msg = msg;
	}
	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText(m_title);
	}
	public boolean NeedToShow()
	{
		return XNDApp.app_Prefs.getBoolean(m_id, true);
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.marginTop = 10;
		parentLayout.verticalSpacing = 20;
		parent.setLayout(parentLayout);
		{
			GridData m_msgLabelLData = new GridData();
			m_msgLabelLData.horizontalAlignment = GridData.FILL;
			m_msgLabelLData.grabExcessHorizontalSpace = true;
			m_msgLabelLData.grabExcessVerticalSpace = true;
			m_msgLabelLData.verticalAlignment = GridData.FILL;
			m_msgLabel = new Label(parent, SWT.NONE);
			m_msgLabel.setLayoutData(m_msgLabelLData);
			m_msgLabel.setText(m_msg);
		}
		{
			m_ShowAgainButton = new Button(parent, SWT.CHECK | SWT.LEFT);
			GridData m_ShowAgainButtonLData = new GridData();
			m_ShowAgainButtonLData.horizontalAlignment = GridData.CENTER;
			m_ShowAgainButtonLData.verticalAlignment = GridData.END;
			m_ShowAgainButtonLData.verticalSpan = 2;
			m_ShowAgainButton.setLayoutData(m_ShowAgainButtonLData);
			m_ShowAgainButton.setText("Don't show again");
		}
		return super.createDialogArea(parent);
	}
	@Override
	protected void cancelPressed()
	{
		if (m_ShowAgainButton.getSelection())
			XNDApp.app_Prefs.putBoolean(m_id, false);
		super.cancelPressed();
	}
	@Override
	protected void okPressed()
	{
		if (m_ShowAgainButton.getSelection())
			XNDApp.app_Prefs.putBoolean(m_id, false);
		// if(m_ShowAgainButton
		// UpdateData(true);
		// if(!Validate()) return;
		super.okPressed();
	}
	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		super.createButtonsForButtonBar(parent);
		Button cB=getCancelButton();
		if(cB!=null) cB.setVisible(false);
	}
}