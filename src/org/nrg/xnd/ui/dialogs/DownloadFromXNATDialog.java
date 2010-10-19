package org.nrg.xnd.ui.dialogs;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.tools.ArcGetManager;

public class DownloadFromXNATDialog extends Dialog
{
	private Combo m_TaglistCombo;
	private ItemTag m_SelectedTag = null;
	private Label m_TagNameLabel;
	private Combo m_Value;
	private RepositoryViewManager m_rvm;
	private Label m_LabelValue;
	private int m_DlgType = -1;
	private Object[] m_tags;
	private Label m_PasswordLabel;
	private Text m_PasswordText;
	private Text m_ServerText;
	private Text m_SessionText;
	private Label m_ServerLabel;
	private Label m_UserLabel;
	private Label m_SessionLabel;
	private Text m_UserText;
	private File m_root;

	public static final int ADD = 0, REMOVE = 1, EDIT = 2;

	public DownloadFromXNATDialog(Shell parentShell, File root)
	{
		super(parentShell);
		m_root = root;
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("XNAT 1.4 connection params");
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 2;
		parent.setLayout(parentLayout);
		{
			m_ServerLabel = new Label(parent, SWT.NONE);
			m_ServerLabel.setText("Server:");
		}
		{
			m_ServerText = new Text(parent, SWT.BORDER);
			m_ServerText.setText("http://localhost:8080/xnat");
		}
		{
			m_UserLabel = new Label(parent, SWT.NONE);
			m_UserLabel.setText("User:");
		}
		{
			m_UserText = new Text(parent, SWT.BORDER);
		}
		{
			m_PasswordLabel = new Label(parent, SWT.NONE);
			m_PasswordLabel.setText("Pass:");
		}
		{
			m_PasswordText = new Text(parent, SWT.BORDER | SWT.PASSWORD);
		}
		{
			m_SessionLabel = new Label(parent, SWT.NONE);
			m_SessionLabel.setText("XNAT Session:");
		}
		{
			m_SessionText = new Text(parent, SWT.BORDER);
		}
		Composite composite = (Composite) super.createDialogArea(parent);
		return composite;
	}
	public ItemTag GetSelectedTag()
	{
		return m_SelectedTag;
	}
	@Override
	protected void okPressed()
	{
		ArcGetManager agm = new ArcGetManager(m_ServerText.getText(),
				m_UserText.getText(), m_PasswordText.getText(), m_SessionText
						.getText(), m_root);
		if (!agm.IsValid())
		{
			MessageBox mb = new MessageBox(new Shell());
			mb.setText("Invalid input");
			mb
					.setMessage("Could not validate connection parameters. Please see console for details.");
		} else
		{
			agm.start();
		}
		super.okPressed();
	}
}
