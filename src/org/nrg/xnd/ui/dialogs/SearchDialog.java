package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

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
public class SearchDialog extends Dialog
{
	public SearchDialog(Shell sh)
	{
		super(sh);
		sh.setText("Search");
	}
	protected void ConfigureShell(Shell sh)
	{
		super.configureShell(sh);
		sh.setText("Search");
	}
	@Override
	protected Control createDialogArea(Composite parent)
	{
		{
			GridLayout parentLayout = new GridLayout();
			parentLayout.makeColumnsEqualWidth = true;
			parentLayout.numColumns = 2;
			parent.setLayout(parentLayout);
			parent.setSize(348, 236);
		}
		{
			Composite composite = (Composite) super.createDialogArea(parent);
			return composite;
		}
	}
}
