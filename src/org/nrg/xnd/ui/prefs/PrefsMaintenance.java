package org.nrg.xnd.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefsMaintenance extends PreferencePage
		implements
			IWorkbenchPreferencePage
{

	@Override
	protected Control createContents(Composite parent)
	{
		{
			GridLayout parentLayout = new GridLayout();
			parentLayout.makeColumnsEqualWidth = true;
			parent.setLayout(parentLayout);
			parent.setSize(304, 209);
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
