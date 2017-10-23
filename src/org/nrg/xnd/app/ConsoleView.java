package org.nrg.xnd.app;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class ConsoleView extends ViewPart
{
	private Text m_Text = null;
	public static final StringBuffer m_Message = new StringBuffer();
	public final static String m_ID = "org.nrg.xnat.desktop.ConsoleView";
	private static boolean m_bNeedRefresh = false;
	private static ConsoleView m_this=null;

	public synchronized static void AppendMessage(final String s)
	{
//		m_Message.append(s + "\n");
//		m_bNeedRefresh = true;
		System.out.println(s);
		Display.getDefault().syncExec(new Runnable()
		{
			@Override
			public void run()
			{
				m_this.Refresh();
			}
		});		
	}
	@Override
	public void createPartControl(Composite parent)
	{
		m_this=this;
		setPartName("Console");
		m_Text = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY
				| SWT.V_SCROLL);
		OutputStream out = new OutputStream()
		{
			private StringBuffer m_buf = new StringBuffer();
			private final Object obj = new Object();

			@Override
			public void write(final int b) throws IOException
			{
				synchronized (obj)
				{
					m_Message.append((char) b);
				}
			}
			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				super.write(b, off, len);
				flush();
			}
			@Override
			public void flush() throws IOException
			{
				synchronized (obj)
				{
					final String newText = m_buf.toString();
					m_Message.append(newText);
				}
			}
		};
		PrintStream newOut = new PrintStream(out);
		System.setOut(newOut);
		System.setErr(newOut);		
		m_Text.setText(m_Message.toString());
	}

	// this has to be called from the UI thread!!!
	public void Refresh()
	{
		m_Text.setText(m_Message.toString());
		m_Text.setSelection(m_Text.getText().length(), m_Text.getText()
				.length());
		m_bNeedRefresh = false;
	}
	public boolean NeedRefresh()
	{
		return m_bNeedRefresh;
	}

	@Override
	public void dispose()
	{
		super.dispose();
	}
	@Override
	public void setFocus()
	{
		XNDPerspective.UpdateActionView(this, null);
	}
}
