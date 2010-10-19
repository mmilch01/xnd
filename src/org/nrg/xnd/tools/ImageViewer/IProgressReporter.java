package org.nrg.xnd.tools.ImageViewer;

public interface IProgressReporter
{
	public void taskName(String name);
	public void startTask(String taskName);
	public void subTaskName(String name);
	public void reportProgress(int progress);
	public void endTask();
	public boolean isCanceled();
}
