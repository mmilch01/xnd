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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.Utils;

public class PrefsClientServer extends PreferencePage
		implements
			IWorkbenchPreferencePage
{
	private Label m_remoteLabel;
	private Combo m_RemoteText;
	private MemoCombo m_RemoteTextM;
	private Text m_ClentPortText;
	private Group group1;
	private Group m_ClientGroup;
	private Label m_ClientPortLabel;
	private String m_RemoteAddr;
	private int m_ClientPort;

	private Label m_ServerPortLabel;
	private Text m_ServerPortText;
	private Button m_ControlButton;
	private boolean m_ServerRunning;
	private int m_ServerPort;

	@Override
	public void init(IWorkbench workbench)
	{
		// server
		m_ServerRunning = XNDApp.app_Prefs.getBoolean("ServerRunning", false);
		m_ServerPort = XNDApp.app_Prefs.getInt("ServerPort",
				Utils.PORT_REPOSITORY_DEFAULT);

		// client
		m_RemoteAddr = XNDApp.app_Prefs.get("RemoteAddress",
				Utils.REMOTE_ADDRESS_DEFAULT);
		m_ClientPort = XNDApp.app_Prefs.getInt("ClientPort",
				Utils.PORT_REPOSITORY_DEFAULT);
	}
	private void UpdateData(boolean bStore)
	{
		if (!bStore)
		{
			if (!m_ServerRunning)
				m_ControlButton.setText("Start server");
			else
				m_ControlButton.setText("Stop server");
			m_ServerPortText.setText(new Integer(m_ServerPort).toString());
			m_RemoteText.setText(m_RemoteAddr);
			m_ClentPortText.setText(new Integer(m_ClientPort).toString());
		} else
		{
			m_ServerPort = new Integer(m_ServerPortText.getText()).intValue();
			m_RemoteAddr = m_RemoteText.getText();
			m_ClientPort = new Integer(m_ClentPortText.getText()).intValue();
		}
	}
	@Override
	public boolean performOk()
	{
		UpdateData(true);

		XNDApp.app_Prefs.putBoolean("ServerRunning", m_ServerRunning);
		XNDApp.app_Prefs.putInt("ServerPort", m_ServerPort);

		XNDApp.app_Prefs.put("RemoteAddress", m_RemoteAddr);
		XNDApp.app_Prefs.putInt("ClientPort", m_ClientPort);

		try
		{
			XNDApp.app_Prefs.flush();
		} catch (Exception e)
		{
		}
		m_RemoteTextM.Save();
		return true;
	}
	@Override
	protected Control createContents(Composite parent)
	{
		{
			noDefaultAndApplyButton();
			GridLayout parentLayout = new GridLayout();
			parentLayout.numColumns = 2;
			parent.setLayout(parentLayout);
			parent.setSize(433, 165);

			{
				m_ClientGroup = new Group(parent, SWT.NONE);
				GridLayout m_ClientGroupLayout = new GridLayout();
				m_ClientGroupLayout.numColumns = 2;
				m_ClientGroup.setLayout(m_ClientGroupLayout);
				GridData m_ClientGroupLData = new GridData();
				m_ClientGroupLData.horizontalAlignment = GridData.FILL;
				m_ClientGroup.setLayoutData(m_ClientGroupLData);
				m_ClientGroup.setText("Server");
				{
					m_ServerPortLabel = new Label(m_ClientGroup, SWT.NONE);
					GridData m_PortLabelLData = new GridData();
					m_PortLabelLData.horizontalAlignment = GridData.END;
					m_ServerPortLabel.setLayoutData(m_PortLabelLData);
					m_ServerPortLabel.setText("Listen port:");
				}
				{
					m_ServerPortText = new Text(m_ClientGroup, SWT.SINGLE
							| SWT.BORDER);
					GridData m_PortTextLData = new GridData();
					m_ServerPortText.setLayoutData(m_PortTextLData);
				}
				{
					GridData m_ControlButtonLData = new GridData();
					m_ControlButtonLData.horizontalSpan = 2;
					m_ControlButtonLData.horizontalAlignment = GridData.END;
					m_ControlButton = new Button(m_ClientGroup, SWT.PUSH);
					m_ControlButton.setLayoutData(m_ControlButtonLData);
					m_ControlButton.addSelectionListener(new SelectionAdapter()
					{
						@Override
						public void widgetSelected(SelectionEvent evt)
						{
							Button b = ((Button) (evt.getSource()));
							if (!m_ServerRunning)
							{
								if (XNDApp.ControlServer(
										XNDApp.SERVER_REPOSITORY, true))
								{
									b.setText("Stop server");
									m_ServerRunning = !m_ServerRunning;
								}
							} else
							{
								XNDApp.ControlServer(XNDApp.SERVER_REPOSITORY,
										false);
								m_ServerRunning = !m_ServerRunning;
								b.setText("Start server");
							}
						}
					});
				}
			}

			{
				group1 = new Group(parent, SWT.NONE);
				GridLayout group1Layout = new GridLayout();
				group1Layout.numColumns = 2;
				group1.setLayout(group1Layout);
				GridData group1LData = new GridData();
				group1LData.horizontalAlignment = GridData.FILL;
				group1LData.grabExcessHorizontalSpace = true;
				group1.setLayoutData(group1LData);
				group1.setText("XNAT Desktop Client");
				{
					m_remoteLabel = new Label(group1, SWT.NONE);
					GridData m_remoteLabelLData = new GridData();
					m_remoteLabelLData.horizontalAlignment = GridData.END;
					m_remoteLabel.setLayoutData(m_remoteLabelLData);
					m_remoteLabel.setText("Remote address:");
				}
				{
					m_RemoteText = new Combo(group1, SWT.BORDER);
					GridData gd = new GridData();
					gd.horizontalAlignment = GridData.FILL;
					gd.grabExcessHorizontalSpace = true;
					m_RemoteText.setLayoutData(gd);
					m_RemoteTextM = new MemoCombo(m_RemoteText,
							"PrefsClientServer.ClientRemoteAddress", 10);
				}
				{
					m_ClientPortLabel = new Label(group1, SWT.NONE);
					GridData m_PortLabelLData = new GridData();
					m_PortLabelLData.horizontalAlignment = GridData.END;
					m_ClientPortLabel.setLayoutData(m_PortLabelLData);
					m_ClientPortLabel.setText("Port:");
				}
				{
					m_ClentPortText = new Text(group1, SWT.SINGLE | SWT.WRAP
							| SWT.BORDER);
				}
			}
			UpdateData(false);
		}
		// TODO Auto-generated method stub
		return null;
	}
}
