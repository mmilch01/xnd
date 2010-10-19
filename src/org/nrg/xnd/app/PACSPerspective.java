package org.nrg.xnd.app;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

public class PACSPerspective implements IPerspectiveFactory
{
	public void createInitialLayout(IPageLayout layout)
	{
		layout.setEditorAreaVisible(false);
		if (XNDApp.app_Prefs.getBoolean("LayoutFixed", true))
		{
			layout.setFixed(true);
		}
		String fvid = FileView.m_ID + ":local";
		layout.addStandaloneView(PACSView.m_ID, true, IPageLayout.LEFT, 1.0f,
				layout.getEditorArea());
		// layout.addView(ConsoleView.m_ID, IPageLayout.BOTTOM, 0.8f, fvid);
		// layout.addStandaloneViewPlaceholder(FileView.m_ID + ":XNAT",
		// IPageLayout.BOTTOM, 0.5f, fvid, true);
		// layout.addStandaloneViewPlaceholder(FileView.m_ID + ":remote",
		// IPageLayout.BOTTOM, 0.5f, fvid, true);
	}
	public static void SelectionChanged(IWorkbenchPart part, ISelection sel)
	{
		if (part instanceof IViewPart)
			UpdateActionView((IViewPart) part, sel);
	}

	public static void UpdateActionView(IViewPart view, ISelection sel)
	{
		IViewPart ivp;
		/*
		 * try { if ((ivp = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
		 * .getActivePage().findView(ActionView.m_ID)) != null) ((ActionView)
		 * ivp).UpdateActionList( (view != null) ? view : ivp, sel); } catch
		 * (Exception e) { }
		 */
	}
}
