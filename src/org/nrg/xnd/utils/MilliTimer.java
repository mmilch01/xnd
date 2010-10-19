package org.nrg.xnd.utils;

import org.eclipse.core.runtime.IProgressMonitor;

public class MilliTimer
{
	private long m_CurTime = 0, m_LastTime = 0, m_interval = 100;
	private IProgressMonitor m_ipm;
	public MilliTimer(IProgressMonitor ipm)
	{
		this(100, ipm);
	}
	public MilliTimer(long interval, IProgressMonitor ipm)
	{
		Reset();
		m_interval = interval;
		m_ipm = ipm;
	}
	public boolean Check(String msg, String sub_msg)
	{
		if (m_ipm == null)
			return true;
		if (m_ipm != null && m_ipm.isCanceled())
			return false;
		m_LastTime = System.currentTimeMillis();
		if (m_LastTime - m_CurTime > m_interval)
		{
			m_CurTime = m_LastTime;
			if (msg != null)
				m_ipm.setTaskName(msg);
			if (sub_msg != null)
				m_ipm.subTask(sub_msg);
		}
		return true;
	}
	public void Reset()
	{
		m_CurTime = m_LastTime = System.currentTimeMillis();
	}
}
