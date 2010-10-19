package org.nrg.xnd.app;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class AppWBench extends WorkbenchWindowAdvisor
{

	public static IWorkbenchWindow m_window;

	public AppWBench(IWorkbenchWindowConfigurer configurer)
	{
		super(configurer);
	}
	@Override
	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer)
	{
		return new AppActions(configurer);
	}

	@Override
	public void preWindowOpen()
	{
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		m_window = configurer.getWindow();
		configurer.setInitialSize(new Point(500, 400));
		configurer.setShowCoolBar(true);
		configurer.setShowProgressIndicator(true);
		configurer.setShowStatusLine(true);
		configurer.setTitle("XNAT Desktop");
		// configurer.setShowPerspectiveBar(true);
	}
	@Override
	public void postWindowOpen()
	{
		XNDApp.app_Status = getWindowConfigurer().getActionBarConfigurer()
				.getStatusLineManager();
		XNDApp.SetStatus("Ready");
	}
}
