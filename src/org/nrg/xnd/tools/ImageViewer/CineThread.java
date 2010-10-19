package org.nrg.xnd.tools.ImageViewer;
class CineThread extends Thread
{
	public String m_initXML = null;
	public int m_controlID = 0;
	public int m_fps = 12;
	private Study study;

	public int getM_controlID()
	{
		return m_controlID;
	}

	CineThread(Study study)
	{
		this.study = study;
	}

	public void run()
	{
		int cur_dir = 1;
		try
		{
			do
			{
				if (study.getM_CurrentSeries() != null)
				{
					switch (m_controlID)
					{
						case 0 : // suspended
							break;
						case 1 :
							study.getM_CurrentSeries().Cine(1, false, true,
									false);
							break;
						case 2 :
							if (!study.getM_CurrentSeries().Cine(cur_dir,
									false, false, false))
							{
								cur_dir = -cur_dir;
								study.getM_CurrentSeries().Cine(cur_dir, false,
										false, false);
							}
							break;
					}
					sleep(1000 / m_fps);
				}
			} while (true);// m_controlID!=0);
		} catch (Exception e)
		{
		}
	}
}
