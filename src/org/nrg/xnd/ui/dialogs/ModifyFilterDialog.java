package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.ViewFilter;
import org.nrg.xnd.utils.Utils;

public class ModifyFilterDialog extends Dialog
{
	private Label label0, label1;
	private String m_nm = "";
	private Text m_Name;
	private List m_Clauses;
	private ViewFilter m_vf;
	private Button m_RemoveButton;
	private Button m_AndButton;
	private RepositoryViewManager m_rvm;
	public ModifyFilterDialog(Shell sh, String name, ViewFilter vf,
			RepositoryViewManager rvm)
	{
		super(sh);
		m_vf = vf;
		m_rvm = rvm;
		sh.setText("Add/edit filter");
		m_nm = name;
	}
	protected void ConfigureShell(Shell sh)
	{
		super.configureShell(sh);
		sh.setText("Add/edit filter");
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		{
			GridLayout parentLayout = new GridLayout();
			parentLayout.numColumns = 3;
			parent.setLayout(parentLayout);
			parent.setSize(325, 203);
			{
				label0 = new Label(parent, SWT.NONE);
				label0.setText("Name:");
			}
			{
				GridData m_NameLData = new GridData();
				m_NameLData.horizontalSpan = 2;
				m_NameLData.horizontalAlignment = GridData.FILL;
				m_Name = new Text(parent, SWT.BORDER);
				m_Name.setLayoutData(m_NameLData);
				m_Name.setText(m_nm);
				if (m_nm.length() > 0)
					m_Name.setEditable(false);
			}
			{
				label1 = new Label(parent, SWT.NONE);
				GridData label1LData = new GridData();
				label1LData.heightHint = 13;
				label0.setLayoutData(label1LData);
				label1LData.verticalAlignment = GridData.BEGINNING;
				label1LData.horizontalSpan = 3;
				label1.setLayoutData(label1LData);
				label1.setText("Filter conditions:");
			}
			{
				GridData m_ClausesLData = new GridData();
				m_ClausesLData.horizontalAlignment = GridData.FILL;
				m_ClausesLData.verticalAlignment = GridData.FILL;
				m_ClausesLData.verticalSpan = 3;
				m_ClausesLData.horizontalSpan = 2;
				m_ClausesLData.grabExcessHorizontalSpace = true;
				m_Clauses = new List(parent, SWT.BORDER);
				m_Clauses.setLayoutData(m_ClausesLData);
				m_Clauses.setItems(m_vf.GetClauseList(false));
				m_Clauses.addSelectionListener(new SelectionListener()
				{
					public void widgetSelected(SelectionEvent evt)
					{
						UpdateButtons();
					}
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
			}
			{
				m_AndButton = new Button(parent, SWT.PUSH | SWT.CENTER);
				GridData m_AndButtonLData = new GridData();
				m_AndButtonLData.widthHint = 59;
				m_AndButtonLData.heightHint = 23;
				m_AndButton.setLayoutData(m_AndButtonLData);
				m_AndButton.setText("Add");
				m_AndButton.addSelectionListener(new SelectionListener()
				{
					public void widgetSelected(SelectionEvent evt)
					{
						AddClause(true);
						UpdateButtons();
					}
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});

			}
			{
				m_RemoveButton = new Button(parent, SWT.PUSH | SWT.CENTER);
				GridData m_RemoveButtonLData = new GridData();
				m_RemoveButtonLData.widthHint = 59;
				m_RemoveButtonLData.heightHint = 23;
				m_RemoveButton.setLayoutData(m_RemoveButtonLData);
				m_RemoveButton.setText("Remove");
				m_RemoveButton.addSelectionListener(new SelectionListener()
				{
					public void widgetSelected(SelectionEvent evt)
					{
						RemoveClause();
						UpdateButtons();
					}
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
			}
		}
		Composite c = (Composite) super.createDialogArea(parent);
		return c;
	}
	private void UpdateButtons()
	{
		m_AndButton.setEnabled(true);
		m_RemoveButton.setEnabled(m_Clauses.getSelectionIndex() >= 0);
	}
	private void AddClause(boolean bAnd)
	{
		FilterClauseDialog fcd = new FilterClauseDialog(new Shell(), m_rvm);
		if (fcd.open() == Window.OK)
		{
			m_vf.AddClause(Utils.PseudoUID(""), fcd.m_Tag, fcd.m_code
					| (bAnd ? ViewFilter.AND : ViewFilter.OR), fcd.m_match);
			m_Clauses.setItems(m_vf.GetClauseList(false));
			UpdateButtons();
		}
	}
	public String GetName()
	{
		return m_nm;
	}
	public String[] GetStrings()
	{
		String[] ar = new String[2];
		ar[0] = m_nm;
		ar[1] = m_vf.GetDescription(false);
		return ar;
	}
	@Override
	protected void okPressed()
	{
		m_nm = m_Name.getText();
		if (m_nm.length() < 1)
		{
			Utils.ShowMessageBox("Warning", "Name cannot be empty", SWT.OK);
			return;
		}
		super.okPressed();
	}
	private void RemoveClause()
	{
		if (Utils.ShowMessageBox("Confirm delete", "Remove this condition?",
				SWT.OK | SWT.CANCEL) == SWT.OK)
		{
			m_vf.RemoveClause(m_Clauses.getSelectionIndex());
			m_Clauses.setItems(m_vf.GetClauseList(false));
			UpdateButtons();
		}
	}
}
