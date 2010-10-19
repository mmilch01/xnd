package org.nrg.xnd.tools.ImageViewer;

public class RadWSProgress implements IProgressReporter
{
	private ImageViewer m_iv;
	public RadWSProgress(ImageViewer par)
	{
		m_iv=par;
	}
	
	@Override
	public void endTask()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isCanceled()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reportProgress(int progress)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void startTask(String taskName)
	{
		m_iv.SetStatus(taskName);
	}

	@Override
	public void subTaskName(String name)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void taskName(String name)
	{
		m_iv.SetStatus(name);
	}

}
