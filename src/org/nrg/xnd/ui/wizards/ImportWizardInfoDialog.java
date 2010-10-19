package org.nrg.xnd.ui.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.nrg.xnd.app.AppActions;

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
public class ImportWizardInfoDialog extends Dialog
{
	private Text m_Instructions;
	private Button m_AddManagedDirButton;
	public ImportWizardInfoDialog(Shell sh)
	{
		super(sh);
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.makeColumnsEqualWidth = true;
		parent.setLayout(parentLayout);
		{
			m_Instructions = new Text(parent, SWT.MULTI | SWT.WRAP);
			GridData m_InstructionsLData = new GridData();
			m_Instructions.setLayoutData(m_InstructionsLData);
			m_Instructions
					.setText("To import data:\n1. Add a directory root for your data;\n2. Select files or folders under this root in the main view;\n3. Run this wizard.");

			m_Instructions.setEditable(false);
		}
		{
			m_AddManagedDirButton = new Button(parent, SWT.PUSH | SWT.CENTER);
			GridData m_AddManagedDirButtonLData = new GridData();
			m_AddManagedDirButton.setLayoutData(m_AddManagedDirButtonLData);
			m_AddManagedDirButton.setText("Select data root...");
			m_AddManagedDirButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent evt)
				{
					AppActions.GetAction(AppActions.ID_ADD_MANAGED_DIR).run();
				}
			});
		}
		Composite comp = (Composite) super.createDialogArea(parent);
		return comp;
	}
}
