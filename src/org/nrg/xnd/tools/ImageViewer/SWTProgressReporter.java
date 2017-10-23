package org.nrg.xnd.tools.ImageViewer;

import org.eclipse.core.runtime.IProgressMonitor;

public class SWTProgressReporter implements IProgressReporter
{
	private IProgressMonitor m_mon;
	public SWTProgressReporter(IProgressMonitor mon)
	{
		m_mon=mon;
	}
	@Override
	public void endTask()
	{
		m_mon.done();
	}
	@Override
	public boolean isCanceled()
	{
		return m_mon.isCanceled();
	}
	@Override
	public void reportProgress(int progress)
	{
		m_mon.worked(progress);
	}

	@Override
	public void startTask(String taskName)
	{
		m_mon.beginTask(taskName, 0);
	}

	@Override
	public void subTaskName(String name)
	{
		m_mon.subTask(name);
	}
	@Override
	public void taskName(String name)
	{
		m_mon.setTaskName(name);		
	}

}
