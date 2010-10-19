package org.nrg.xnd.app;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.nrg.xnd.utils.Utils;

public class AppWorkbenchAdvisor extends WorkbenchAdvisor
{

	private static final String PERSPECTIVE_ID = "org.nrg.xnat.desktop.xndperspective";
	// "org.nrg.xnat.desktop.pacsperspective";

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer)
	{
		return new AppWBench(configurer);
	}

	@Override
	public String getInitialWindowPerspectiveId()
	{
		return PERSPECTIVE_ID;
	}
	@Override
	public void initialize(IWorkbenchConfigurer configurer)
	{
		super.initialize(configurer);
		configurer.setSaveAndRestore(true);
	}

	@Override
	public void eventLoopIdle(Display display)
	{
		final IViewReference[] ivr;
		boolean b = false;
		if ((ivr = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getViewReferences()) != null)
		{
			for (int i = 0; i < ivr.length; i++)
			{
				if (ivr[i].getView(false) instanceof ConsoleView)
				{
					b = true;
					if (((ConsoleView) (ivr[i].getView(false))).NeedRefresh())
						((ConsoleView) (ivr[i].getView(false))).Refresh();
					break;
				}
			}
			Utils.CheckForThreadMessages();
			// ((ConsoleView)(ivr.getView(false))).Refresh();
		}
		// if(b)
		// display.sleep();
		// else
		super.eventLoopIdle(display);

	}

}
