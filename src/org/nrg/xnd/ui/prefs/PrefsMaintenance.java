package org.nrg.xnd.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

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

	public void init(IWorkbench workbench)
	{
		// TODO Auto-generated method stub

	}

}
