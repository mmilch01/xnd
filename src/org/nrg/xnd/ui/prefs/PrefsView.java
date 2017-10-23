package org.nrg.xnd.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.utils.Utils;

public class PrefsView extends PreferencePage
		implements
			IWorkbenchPreferencePage
{
	private Button m_FixedViewCheck;
	private Button m_LimitCheck;
	private Group group1;
	private Group m_LayoutGroup;
	private RepositoryViewManager m_rvm;

	@Override
	protected Control createContents(Composite parent)
	{
		m_rvm = XNDApp.app_localVM;

		noDefaultAndApplyButton();
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 2;
		parent.setLayout(parentLayout);
		{
			m_LayoutGroup = new Group(parent, SWT.NONE);
			GridLayout m_LayoutGroupLayout = new GridLayout();
			m_LayoutGroupLayout.makeColumnsEqualWidth = true;
			m_LayoutGroup.setLayout(m_LayoutGroupLayout);
			m_LayoutGroup.setText("Layout Properties");
			m_FixedViewCheck = new Button(m_LayoutGroup, SWT.CHECK | SWT.LEFT);
			m_FixedViewCheck.setText("Fixed layout (requires restart)");
			m_FixedViewCheck.setEnabled(false);
			{
				group1 = new Group(parent, SWT.NONE);
				GridLayout group1Layout = new GridLayout();
				group1Layout.numColumns = 2;
				group1.setLayout(group1Layout);
				group1.setText("Optimizations");
				{
					m_LimitCheck = new Button(group1, SWT.CHECK | SWT.LEFT);
					m_LimitCheck
							.setText("Limit the number of displayed records");
				}
			}
			m_FixedViewCheck.setSelection(XNDApp.app_Prefs.getBoolean(
					"LayoutFixed", true));
			m_LimitCheck.setSelection(XNDApp.app_Prefs.getBoolean(
					"LimitRecords", true));
		}
		return null;
	}
	@Override
	public boolean performOk()
	{
		boolean tst = m_FixedViewCheck.getSelection();
		XNDApp.app_Prefs.putBoolean("LayoutFixed", tst);
		boolean lim = m_LimitCheck.getSelection();
		XNDApp.app_Prefs.putBoolean("LimitRecords", lim);
		XNDApp.app_maxRecords = (lim) ? Utils.MAX_TABLE_RECORDS : -1;
		return true;
	}
	@Override
	public void init(IWorkbench workbench)
	{
	}
}