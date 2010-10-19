package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.ui.MemoCombo;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
public class OpenConnectionDialog extends Dialog
{
	private Text m_XNATPassword;
	private Combo m_XNATServer, m_XNATUser;
	private MemoCombo m_XNATServerM, m_XNATUserM;
	private Label label1, label2, label3;
	private String m_serv, m_usr, m_pass;

	public String getServer()
	{
		return m_serv;
	}
	public String getUser()
	{
		return m_usr;
	}
	public String getPass()
	{
		return m_pass;
	}

	public OpenConnectionDialog(Shell sh)
	{
		super(sh);
	}
	@Override
	protected void configureShell(Shell sh)
	{
		super.configureShell(sh);
		sh.setText("Specify connection parameters");
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 2;
		parent.setLayout(parentLayout);
		parent.setSize(289, 147);
		{
			label1 = new Label(parent, SWT.NONE);
			GridData label1LData = new GridData();
			label1LData.horizontalAlignment = GridData.END;
			label1.setLayoutData(label1LData);
			label1.setText("XNAT server:");
		}
		{
			GridData m_XNATServerLData = new GridData();
			m_XNATServerLData.horizontalAlignment = GridData.FILL;
			m_XNATServerLData.grabExcessHorizontalSpace = true;
			m_XNATServer = new Combo(parent, SWT.DROP_DOWN);
			m_XNATServer.setLayoutData(m_XNATServerLData);
		}
		m_XNATServerM = new MemoCombo(m_XNATServer,
				"OpenConnectionDialog.XNATServerList", 10);
		{
			label2 = new Label(parent, SWT.NONE);
			GridData label2LData = new GridData();
			label2LData.horizontalAlignment = GridData.END;
			label2.setLayoutData(label2LData);
			label2.setText("User:");
		}
		{
			GridData m_XNATUserLData = new GridData();
			m_XNATUserLData.grabExcessHorizontalSpace = true;
			m_XNATUserLData.horizontalAlignment = GridData.FILL;
			m_XNATUser = new Combo(parent, 0);
			m_XNATUser.setLayoutData(m_XNATUserLData);
		}
		m_XNATUserM = new MemoCombo(m_XNATUser,
				"OpenConnectionDialog.XNATUserList", 5);
		{
			label3 = new Label(parent, SWT.NONE);
			GridData label3LData = new GridData();
			label3LData.horizontalAlignment = GridData.END;
			label3.setLayoutData(label3LData);
			label3.setText("Password:");
		}
		{
			GridData m_XNATPasswordLData = new GridData();
			m_XNATPasswordLData.grabExcessHorizontalSpace = true;
			m_XNATPasswordLData.horizontalAlignment = GridData.FILL;
			m_XNATPassword = new Text(parent, SWT.SINGLE | SWT.WRAP
					| SWT.BORDER | SWT.PASSWORD);
			m_XNATPassword.setLayoutData(m_XNATPasswordLData);
		}
		UpdateData(false);
		Composite c = (Composite) super.createDialogArea(parent);
		return c;
	}
	private void UpdateData(boolean bFromUI)
	{
		if (!bFromUI)
		{
			m_XNATServer.select(0);
			m_XNATUser.select(0);
			// m_XNATServer.setText(XNDApp.app_Prefs.get("defaultXNATUploadServer","http://central.xnat.org"));
			// m_XNATUser.setText(XNDApp.app_Prefs.get("defaultXNATUploadUser",
			// ""));
			m_XNATPassword.setText(XNDApp.app_Prefs.get(
					"OpenConnectionDialog.Pass", ""));
		} else
		{
			m_XNATServerM.Save();
			m_XNATUserM.Save();
			m_serv = m_XNATServer.getText();
			m_usr = m_XNATUser.getText();
			m_pass = m_XNATPassword.getText();
			XNDApp.app_Prefs.put("OpenConnectionDialog.XNATServer", m_serv);
			XNDApp.app_Prefs.put("OpenConnectionDialog.XNATUser", m_usr);
			XNDApp.app_Prefs.put("OpenConnectionDialog.Pass", m_pass);
		}
	}
	@Override
	protected void okPressed()
	{
		UpdateData(true);
		super.okPressed();
	}
}