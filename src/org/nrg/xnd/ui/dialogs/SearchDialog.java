package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

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
