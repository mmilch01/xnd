package org.nrg.xnd.ui.dialogs;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TagDescr;

public class EditTagValueDialog extends Dialog
{
	private Combo m_TaglistCombo;
	private ItemTag m_SelectedTag = null;
	private Label m_TagNameLabel;
	private Combo m_Value;
	private RepositoryViewManager m_rvm;
	private Label m_LabelValue;
	private int m_DlgType = -1;
	private TagDescr[] m_tags;
	private String m_defaultTagName=null;
	public static final int ADD = 0, REMOVE = 1, EDIT = 2;

	public EditTagValueDialog(Shell parentShell, int type,
			RepositoryViewManager rvm, String defaultTagName)
	{
		super(parentShell);
		m_DlgType = type;
		m_rvm = rvm;
		m_defaultTagName=defaultTagName;
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		if (m_DlgType == ADD)
			newShell.setText("Set tag");
		else if (m_DlgType == REMOVE)
			newShell.setText("Remove tag");
		else if (m_DlgType == EDIT)
			newShell.setText("Patterned tag value");
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.numColumns = 2;
		composite.setLayout(compositeLayout);
		{
			m_TagNameLabel = new Label(composite, SWT.NONE);
			GridData m_TagNameLabelLData = new GridData();
			m_TagNameLabelLData.horizontalAlignment = GridData.END;
			m_TagNameLabel.setLayoutData(m_TagNameLabelLData);
			m_TagNameLabel.setText("Tag name:");
		}
		// {
		GridData m_TaglistComboLData = new GridData();
		m_TaglistCombo = new Combo(composite, SWT.READ_ONLY);
		m_TaglistCombo.setLayoutData(m_TaglistComboLData);
		m_tags = m_rvm.GetTagList(0);
		String t;
		int selection=0;
		for (int i = 0; i < m_tags.length; i++)
		{
			t=m_tags[i].GetName();
			m_TaglistCombo.add(t);
			if(m_defaultTagName!=null)
			{
				if(t.compareTo(m_defaultTagName)==0) selection=i;
			}
		}
		try
		{
			m_TaglistCombo.select(selection);
		} catch (Exception e)
		{
		}
		// }
		{
			m_LabelValue = new Label(composite, SWT.NONE);
			if (m_DlgType != EDIT)
				m_LabelValue.setText("Tag value:");
			else
				m_LabelValue.setText("Pattern:");
			if (m_DlgType == REMOVE)
				m_LabelValue.setVisible(false);
			GridData m_ValueTextAreaLData = new GridData();
			m_ValueTextAreaLData.grabExcessHorizontalSpace = true;
			m_ValueTextAreaLData.grabExcessVerticalSpace = true;
			if (XNDApp.app_Platform == XNDApp.PLATFORM_MAC)
				m_ValueTextAreaLData.widthHint = 134;
			m_Value = new Combo(composite, SWT.SINGLE | SWT.BORDER);
			m_Value.setLayoutData(m_ValueTextAreaLData);
			if (m_DlgType == REMOVE)
				m_Value.setEnabled(false);
			else
				m_Value.setFocus();
		}
		m_TaglistCombo.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				if (m_DlgType == EDIT)
					return;
				int sel;
				if ((sel = m_TaglistCombo.getSelectionIndex()) < 0)
					return;
				ItemTag tag = new ItemTag(m_tags[sel].GetName());
				TagDescr td = m_rvm.GetTagDescr(tag.GetName());
				if (td.IsSet(TagDescr.PREDEF_VALUES))
				{
					m_Value.setItems(td.GetValues());
					m_Value.select(0);
				} else
					m_Value.removeAll();
			}
			public void widgetDefaultSelected(SelectionEvent e)
			{
			}
		});
		return composite;
	}
	public ItemTag GetSelectedTag()
	{
		return m_SelectedTag;
	}
	@Override
	protected void okPressed()
	{
		m_SelectedTag = new ItemTag(m_TaglistCombo.getItem(m_TaglistCombo
				.getSelectionIndex()), m_Value.getText());
		super.okPressed();
	}
}