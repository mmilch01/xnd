package org.nrg.xnd.ui;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.eclipse.swt.widgets.Shell;
import org.nrg.xnd.tools.ImageViewer.ImageViewerManager;

public class ImageViewerFrame extends JFrame
{
	ImageViewerManager m_ivm;
	private Dimension m_size;
	Shell sh;
	public ImageViewerFrame()
	{
		super("Image viewer");
	}
	public ImageViewerManager GetViewerManager()
	{
		return m_ivm;
	}
	@Override
	protected void frameInit()
	{
		super.frameInit();
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setResizable(true);
		// setLayout(new BorderLayout());
		setLayout(null);
		setSize(900, 700);
		getContentPane().setSize(getWidth() - 10, this.getHeight() - 25);
		m_ivm = new ImageViewerManager(getContentPane(),true);
		// pack();
		m_size = getSize();
	}
	@Override
	public void paint(Graphics g)
	{
		Dimension newSz = getSize();
		if (newSz.height != m_size.height || newSz.width != m_size.width)
		{
			m_size = new Dimension(newSz.width - 10, newSz.height - 25);
			// getContentPane().setSize(new
			// Dimension(newSz.width-10,newSz.height-25));
			// getContentPane().setPreferredSize(newSz);
			getContentPane().setSize(m_size);
			m_ivm.UpdateSize(m_size);
		}
		super.paint(g);
	}
}
