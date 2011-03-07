package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.model.ViewFilter;
import org.nrg.xnd.ui.MemoCombo;

public class FilterClauseDialog extends Dialog
{
	private RepositoryViewManager m_rvm = null;
	private Label label1;
	private Combo m_TagNameCombo;
	private Combo m_RegexpText;
	private MemoCombo m_RegexpTextM;
	private Combo m_FilterCombo;
	private Combo m_ExistCombo;
	private TagDescr[] m_Tags;

	// internal data representation
	public TagDescr m_Tag = null;
	public int m_code;
	public String m_match;

	private void UpdateData(boolean bFromGUI)
	{
		if (!bFromGUI)
		{
			for (int i = 0; i < m_Tags.length; i++)
			{
				if (m_Tags[i].GetName().compareTo(m_Tag.GetName()) == 0)
				{
					m_TagNameCombo.select(i);
					break;
				}
			}
			m_ExistCombo.select(((m_code & ViewFilter.IS) != 0) ? 0 : 1);
			if ((m_code & ViewFilter.EQUAL_TO) != 0)
			{
				m_FilterCombo.select(0);
				m_RegexpText.setEnabled(true);
				m_RegexpText.setText(m_match);
			} else
			{
				m_FilterCombo.select(1);
				m_RegexpText.setEnabled(false);
			}
		} else
		{
			m_Tag = m_Tags[m_TagNameCombo.getSelectionIndex()];
			m_code = ((m_ExistCombo.getSelectionIndex() == 0)
					? ViewFilter.IS
					: ViewFilter.IS_NOT)
					| ((m_FilterCombo.getSelectionIndex() == 0)
							? ViewFilter.EQUAL_TO
							: ViewFilter.DEFINED);
			m_match = m_RegexpText.getText();
		}
	}
	public FilterClauseDialog(Shell parentShell, RepositoryViewManager rvm)
	{
		super(parentShell);
		m_rvm = rvm;
		m_Tags = rvm.GetVisibleTagList();
	}
	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Define filter condition");
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.marginTop = 5;
		parentLayout.numColumns = 2;
		parent.setLayout(parentLayout);
		{
			label1 = new Label(parent, SWT.LEFT);
			GridData label1LData = new GridData();
			label1LData.horizontalAlignment = GridData.CENTER;
			label1.setLayoutData(label1LData);
			label1.setText("Tag");
		}
		{
			GridData m_TagNameComboLData = new GridData();
			m_TagNameComboLData.horizontalAlignment = GridData.FILL;
			m_TagNameComboLData.grabExcessHorizontalSpace = true;
			m_TagNameCombo = new Combo(parent, SWT.READ_ONLY);
			m_TagNameCombo.setLayoutData(m_TagNameComboLData);
			for (TagDescr td : m_Tags)
				m_TagNameCombo.add(td.GetName());
			m_TagNameCombo.select(0);
		}
		{
			GridData m_ExistComboLData = new GridData();
			m_ExistComboLData.horizontalAlignment = GridData.FILL;
			m_ExistCombo = new Combo(parent, SWT.READ_ONLY);
			m_ExistCombo.setLayoutData(m_ExistComboLData);
			m_ExistCombo.add("is");
			m_ExistCombo.add("is not");
			m_ExistCombo.select(0);
		}
		{
			GridData m_FilterComboLData = new GridData();
			m_FilterComboLData.horizontalAlignment = GridData.FILL;
			m_FilterCombo = new Combo(parent, SWT.READ_ONLY);
			m_FilterCombo.setLayoutData(m_FilterComboLData);
			m_FilterCombo.add("equal to");
			m_FilterCombo.add("defined");
			m_FilterCombo.select(0);
			m_FilterCombo.addSelectionListener(new SelectionListener()
			{
				public void widgetSelected(SelectionEvent e)
				{
					int sel;
					if ((sel = m_FilterCombo.getSelectionIndex()) < 0)
						return;
					m_RegexpText.setEnabled(sel == 0);
				}
				public void widgetDefaultSelected(SelectionEvent e)
				{
				}
			});
		}
		{
			m_RegexpText = new Combo(parent, SWT.BORDER);
			m_RegexpText.setText(".*");
			GridData m_RegexpTextLData = new GridData();
			m_RegexpTextLData.horizontalAlignment = GridData.FILL;
			m_RegexpTextLData.horizontalSpan = 2;
			m_RegexpTextLData.grabExcessHorizontalSpace = true;
			m_RegexpText.setLayoutData(m_RegexpTextLData);
			m_RegexpTextM = new MemoCombo(m_RegexpText,
					"FilterClauseDialog.RegexpText", 20);
		}
		UpdateData(true);
		return super.createDialogArea(parent);
	}
	@Override
	protected void okPressed()
	{
		UpdateData(true);
		m_RegexpTextM.Save();
		super.okPressed();
	}
}