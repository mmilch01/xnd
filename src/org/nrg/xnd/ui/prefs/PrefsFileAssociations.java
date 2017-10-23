package org.nrg.xnd.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefsFileAssociations extends PreferencePage
		implements
			IWorkbenchPreferencePage
{
	private Label m_DefCommandLabel;
	private Combo m_DefCommandCombo;
	private Combo m_CommandCombo;
	private Label m_CommandLabel;
	private Combo m_ExtCombo;
	private Label m_ExtLabel;
	private Group m_AssocGroup;

	@Override
	protected Control createContents(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 2;
		parentLayout.makeColumnsEqualWidth = true;
		parent.setLayout(parentLayout);
		{
			m_DefCommandLabel = new Label(parent, SWT.NONE);
			m_DefCommandLabel.setText("Default command:");
			GridData m_DefCommandLabelLData = new GridData();
			m_DefCommandLabelLData.horizontalSpan = 2;
			m_DefCommandLabel.setLayoutData(m_DefCommandLabelLData);
			m_DefCommandLabel.setAlignment(SWT.RIGHT);
		}
		{
			m_DefCommandCombo = new Combo(parent, SWT.NONE);
			GridData m_DefCommandComboLData = new GridData();
			m_DefCommandComboLData.horizontalAlignment = GridData.FILL;
			m_DefCommandComboLData.horizontalSpan = 2;
			m_DefCommandCombo.setLayoutData(m_DefCommandComboLData);
			m_DefCommandCombo.setText("                                    ");
		}
		{
			m_AssocGroup = new Group(parent, SWT.NONE);
			GridLayout m_AssocGroupLayout = new GridLayout();
			m_AssocGroupLayout.numColumns = 2;
			m_AssocGroup.setLayout(m_AssocGroupLayout);
			GridData m_AssocGroupLData = new GridData();
			m_AssocGroupLData.horizontalSpan = 2;
			m_AssocGroupLData.horizontalAlignment = GridData.FILL;
			m_AssocGroup.setLayoutData(m_AssocGroupLData);
			m_AssocGroup.setText("File associations");
			{
				m_ExtLabel = new Label(m_AssocGroup, SWT.NONE);
				m_ExtLabel.setText("Extension:");
			}
			{
				m_CommandLabel = new Label(m_AssocGroup, SWT.NONE);
				m_CommandLabel.setText("Command:");
			}
			{
				m_ExtCombo = new Combo(m_AssocGroup, SWT.NONE);
				GridData m_ExtComboLData = new GridData();
				m_ExtComboLData.horizontalAlignment = GridData.FILL;
				m_ExtCombo.setLayoutData(m_ExtComboLData);
				m_ExtCombo.setText("                     ");
			}
			{
				m_CommandCombo = new Combo(m_AssocGroup, SWT.NONE);
				GridData m_CommandComboLData = new GridData();
				m_CommandComboLData.horizontalAlignment = GridData.FILL;
				m_CommandCombo.setLayoutData(m_CommandComboLData);
				m_CommandCombo.setText("                              ");
			}
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(IWorkbench workbench)
	{
		// TODO Auto-generated method stub

	}
}
