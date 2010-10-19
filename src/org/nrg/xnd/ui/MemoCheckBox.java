package org.nrg.xnd.ui;

import org.eclipse.swt.widgets.Button;
import org.nrg.xnd.app.XNDApp;

public class MemoCheckBox
{
	private String m_uid;
	public Button m_cb;

	public MemoCheckBox(Button cb, String UID)
	{
		init(cb, UID, false);
	}
	private void init(Button cb, String UID, boolean bDefState)
	{
		m_cb = cb;
		m_uid = UID;
		try
		{
			m_cb.setSelection(XNDApp.app_Prefs.getBoolean(m_uid, bDefState));
		} catch (Exception e)
		{
		}
	}
	// NOTE: add constructor for this class before defining any event listeners;
	// otherwise may lead to exceptions.
	public MemoCheckBox(Button cb, String UID, boolean bDefState)
	{
		init(cb, UID, bDefState);
	}
	public void Save()
	{
		XNDApp.app_Prefs.putBoolean(m_uid, m_cb.getSelection());
	}
}