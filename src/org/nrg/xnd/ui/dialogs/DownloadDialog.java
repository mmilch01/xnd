package org.nrg.xnd.ui.dialogs;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.ViewFilter;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.Utils;

public class DownloadDialog extends Dialog
{
	private Label label1;
	private Text m_custFilterInstr;

	private Combo m_custFilterText;
	private MemoCombo m_custFilterTextM;

	private Combo m_filterCombo;
	private Label label2;
	private Button m_chkDownManual;
	private Button m_downBtn;
	private Combo m_downFold;
	private MemoCombo m_downFoldM;
	private ViewFilter m_vf = null;
	private String m_downFolder = null;
	private final static String m_cfInstr = "Custom filter. Use expression of "
			+ "type:\n<tag_name><=|!=>[\"<expression>\"] [AND ...]\ne.g. "
			+ "Project=\"test.*\" AND Subject!=";

	public DownloadDialog(Shell sh)
	{
		super(sh);
	}
	@Override
	protected void configureShell(Shell sh)
	{
		super.configureShell(sh);
		sh.setText("Download options");
	}
	private void UpdateControls(boolean bLoad)
	{
		if (bLoad)
		{
			// download folder
			m_downFold.setText(XNDApp.app_Prefs.get("DownFolder", ""));
			m_chkDownManual.setSelection(!XNDApp.app_Prefs.getBoolean(
					"AutoSelectDownloadFolder", true));

			// filter selection
			m_filterCombo.removeAll();
			int nsel = 0;
			String sel = XNDApp.app_Prefs.get("DownFilterSelection", "<none>");
			m_filterCombo.add("<none>");
			m_filterCombo.add("custom...");
			if (sel.compareTo("custom...") == 0)
				nsel = 1;
			int ind = 2;
			for (String s : XNDApp.app_filters.keySet())
			{
				if (sel.compareTo(s) == 0)
					nsel = ind;
				m_filterCombo.add(s);
				ind++;
			}
			m_filterCombo.select(nsel);
			if (nsel == 1)
			{
				m_custFilterInstr.setText(m_cfInstr);
			} else if (nsel > 0)
			{
				m_custFilterInstr.setText(XNDApp.app_filters.get(
						m_filterCombo.getText()).GetDescription(true));
			}
		} else
		{
			XNDApp.app_Prefs.putBoolean("AutoSelectDownloadFolder",
					!m_chkDownManual.getSelection());

			if (m_chkDownManual.getSelection())
				XNDApp.app_Prefs.put("DownFolder", m_downFold.getText());
			XNDApp.app_Prefs.put("DownFilterSelection", m_filterCombo
					.getItem(m_filterCombo.getSelectionIndex())
					+ "\n\n\n");
		}
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		{
			GridLayout parentLayout = new GridLayout();
			parentLayout.numColumns = 2;
			parent.setLayout(parentLayout);
			parent.setSize(313, 230);
			{
				label1 = new Label(parent, SWT.NONE);
				GridData label1LData = new GridData();
				label1LData.horizontalSpan = 2;
				label1.setLayoutData(label1LData);
				label1.setText("Download folder:");
			}
			{
				GridData m_downFoldLData = new GridData();
				m_downFoldLData.grabExcessHorizontalSpace = true;
				m_downFoldLData.horizontalAlignment = GridData.FILL;
				m_downFold = new Combo(parent, SWT.BORDER);
				m_downFold.setLayoutData(m_downFoldLData);
				m_downFoldM = new MemoCombo(m_downFold,
						"DownloadDialog.DownloadFolder", 10);
			}
			{
				m_downBtn = new Button(parent, SWT.PUSH | SWT.CENTER);
				GridData m_downBtnLData = new GridData();
				m_downBtn.setLayoutData(m_downBtnLData);
				m_downBtn.setText("...");
				m_downBtn.addSelectionListener(new SelectionListener()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						m_downFolder = Utils.SelectFolder("Download folder",
								"Select download folder:");
						if (m_downFolder != null)
							m_downFold.setText(m_downFolder);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
			}
			{
				m_chkDownManual = new Button(parent, SWT.CHECK | SWT.LEFT);
				GridData m_chkDownManualLData = new GridData();
				m_chkDownManualLData.horizontalSpan = 2;
				m_chkDownManual.setLayoutData(m_chkDownManualLData);
				m_chkDownManual.setText("Select download folder manually");
				m_chkDownManual.setSelection(XNDApp.app_Prefs.getBoolean(
						"PrefsFileTransferAutoDownload", false));
				m_chkDownManual.addSelectionListener(new SelectionListener()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						UpdateBtns();
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
			}
			{
				label2 = new Label(parent, SWT.NONE);
				GridData label2LData = new GridData();
				label2LData.horizontalSpan = 2;
				label2.setLayoutData(label2LData);
				label2.setText("Data filter:");
			}
			{
				GridData m_filterComboLData = new GridData();
				m_filterComboLData.horizontalSpan = 2;
				m_filterCombo = new Combo(parent, SWT.READ_ONLY);
				m_filterCombo.setLayoutData(m_filterComboLData);
				m_filterCombo.addSelectionListener(new SelectionListener()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						UpdateBtns();
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
			}
			{
				m_custFilterInstr = new Text(parent, SWT.MULTI | SWT.WRAP);
				m_custFilterInstr.setText(m_cfInstr);
				GridData m_custFilterInstrLData = new GridData();
				m_custFilterInstrLData.horizontalAlignment = GridData.FILL;
				m_custFilterInstrLData.grabExcessHorizontalSpace = true;
				m_custFilterInstr.setLayoutData(m_custFilterInstrLData);
				m_custFilterInstr.setEditable(false);
			}
			{
				GridData m_custFilterTextLData = new GridData();
				m_custFilterTextLData.horizontalAlignment = GridData.FILL;
				m_custFilterTextLData.grabExcessHorizontalSpace = true;
				m_custFilterTextLData.horizontalSpan = 2;
				m_custFilterText = new Combo(parent, SWT.BORDER);
				m_custFilterText.setLayoutData(m_custFilterTextLData);
				m_custFilterTextM = new MemoCombo(m_custFilterText,
						"DownloadDialog.CustomFilterExpression", 20);
			}
		}
		UpdateControls(true);
		UpdateBtns();
		Composite composite = (Composite) super.createDialogArea(parent);
		return composite;
	}
	private void UpdateBtns()
	{
		boolean b = m_chkDownManual.getSelection();
		m_downBtn.setEnabled(b);
		m_downFold.setEnabled(b);
		if (!b)
			m_downFold.setText(Utils.GetIncomingFolder());
		if (m_filterCombo.getSelectionIndex() == 1)
		{
			m_custFilterInstr.setVisible(true);
			m_custFilterInstr.setText(m_cfInstr);
			m_custFilterText.setVisible(true);
		} else if (m_filterCombo.getSelectionIndex() > 1)
		{
			m_custFilterInstr.setVisible(true);
			m_custFilterText.setVisible(false);
			m_custFilterInstr.setText(XNDApp.app_filters.get(
					m_filterCombo.getText()).GetDescription(true)
					+ "\n\n\n");
		} else
		{
			m_custFilterText.setVisible(false);
			m_custFilterInstr.setVisible(false);
		}
	}

	public ViewFilter GetFilter()
	{
		return m_vf;
	}
	public String GetDownFolder()
	{
		return m_downFolder;
	}
	@Override
	protected void okPressed()
	{
		if (m_filterCombo.getText().compareTo("custom...") == 0)
		{
			m_vf = new ViewFilter();
			if (!m_vf.FromString(m_custFilterText.getText()))
			{
				Utils.ShowMessageBox("Error",
						"Custom filter syntax is incorrect", Window.OK);
				return;
			}
		} else if (m_filterCombo.getSelectionIndex() == 0)
			m_vf = null;
		else
		{
			m_vf = XNDApp.app_filters.get(m_filterCombo.getText());
		}
		if (m_downBtn.getEnabled())
		{
			m_downFolder = m_downFold.getText();
			if (!new File(m_downFolder).exists())
			{
				Utils.ShowMessageBox("Error",
						"Select existing download folder first", Window.OK);
				return;
			}
		} else
			m_downFolder = null;
		UpdateControls(false);
		m_custFilterTextM.Save();
		m_downFoldM.Save();
		super.okPressed();
	}
}
