package org.nrg.xnd.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.utils.Utils;

public class ManageTagsDialog extends Dialog
{
	private Table m_Table;
	private TableColumn m_NameColumn;
	private Button m_EditButton;
	private Button m_AddButton;
	private Button m_ButtonDelete;
	private boolean m_ViewCheckChanged = false;
	public ManageTagsDialog(Shell parentShell)
	{
		super(parentShell);
	}
	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Manage tags");
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.makeColumnsEqualWidth = true;
		parent.setLayout(parentLayout);
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.numColumns = 3;
		composite.setLayout(compositeLayout);
		parent.setSize(400, 600);
		{
			GridData table1LData = new GridData();
			table1LData.horizontalSpan = 2;
			table1LData.verticalSpan = 10;

			table1LData.horizontalAlignment = GridData.FILL;
			table1LData.verticalAlignment = GridData.FILL;
			table1LData.grabExcessHorizontalSpace = true;
			table1LData.grabExcessVerticalSpace = true;

			m_Table = new Table(composite, SWT.RIGHT | SWT.BORDER | SWT.MULTI
					| SWT.V_SCROLL | SWT.CHECK | SWT.H_SCROLL
					| SWT.FULL_SELECTION);
			m_Table.setLayoutData(table1LData);
			TableColumn chk = new TableColumn(m_Table, SWT.CENTER);
			chk.setText("Show");
			chk.setWidth(50);

			m_NameColumn = new TableColumn(m_Table, SWT.LEFT);
			m_NameColumn.setText("Name");
			m_NameColumn.setWidth(120);
			m_Table.setHeaderVisible(true);
			UpdateTable();
			// m_Table.setSize(chk.getWidth()+m_NameColumn.getWidth(),
			// m_Table.getSize().y);
			// m_Table.setSize(200,500);

		}
		int buttonWidth = 80;
		{
			m_AddButton = new Button(composite, SWT.PUSH | SWT.CENTER);
			GridData m_AddButtonLData = new GridData();
			m_AddButtonLData.widthHint = buttonWidth;
			m_AddButton.setLayoutData(m_AddButtonLData);
			m_AddButton.setText("New");
			m_AddButton.setAlignment(SWT.CENTER);
			m_AddButton.setEnabled(true);
			m_AddButton.addSelectionListener(new SelectionListener()
			{
				public void widgetSelected(SelectionEvent evt)
				{
					m_AddButtonKeyPressed(evt);
				}
				public void widgetDefaultSelected(SelectionEvent evt)
				{
					m_AddButtonKeyPressed(evt);
				}
			});
		}
		{
			m_EditButton = new Button(composite, SWT.PUSH | SWT.CENTER);
			GridData m_EditButtonLData = new GridData();
			m_EditButtonLData.widthHint = buttonWidth;
			m_EditButton.setLayoutData(m_EditButtonLData);
			m_EditButton.setText("Rename");
			m_EditButton.setAlignment(SWT.CENTER);
			m_EditButton.setEnabled(false);
		}
		{
			m_ButtonDelete = new Button(composite, SWT.PUSH | SWT.CENTER);
			GridData m_ButtonDeleteLData = new GridData();
			m_ButtonDeleteLData.widthHint = buttonWidth;
			m_ButtonDelete.setLayoutData(m_ButtonDeleteLData);
			m_ButtonDelete.setText("Delete");
			m_ButtonDelete.setAlignment(SWT.CENTER);
			m_ButtonDelete.setEnabled(true);
			m_ButtonDelete.addSelectionListener(new SelectionListener()
			{
				public void widgetSelected(SelectionEvent evt)
				{
					m_DelButtonPressed(evt);
				}
				public void widgetDefaultSelected(SelectionEvent evt)
				{
					m_DelButtonPressed(evt);
				}
			});
		}
		return composite;
	}
	protected void UpdateTable()
	{
		TagDescr[] tags = XNDApp.app_localVM.GetVisibleTagList();
		TagDescr ta;
		for (int i = 0; i < tags.length; i++)
		{
			ta = tags[i];
			AddRow(ta.IsSet(TagDescr.TABLE_DISPLAY), ta.GetName());
		}
	}
	private void AddRow(boolean disp, String name)
	{
		TableItem ti = new TableItem(m_Table, SWT.CENTER, 0);
		ti.setText(0, "");
		ti.setChecked(disp);
		ti.setText(1, name);
	}
	public boolean IsViewConfigChanged()
	{
		return m_ViewCheckChanged;
	}
	@Override
	protected void okPressed()
	{
		TableItem[] items = m_Table.getItems();
		for (int i = 0; i < items.length; i++)
		{
			if (XNDApp.app_localVM.UpdateTagShow(items[i].getText(1), items[i]
					.getChecked()))
				m_ViewCheckChanged = true;
		}
		super.okPressed();
	}

	private void m_DelButtonPressed(SelectionEvent e)
	{
		TableItem[] tags = m_Table.getSelection();
		if (tags.length < 1)
			return;
		MessageBox mb = new MessageBox(new Shell(), SWT.OK | SWT.CANCEL);
		mb.setText("Confirm delete");
		mb.setMessage("Delete selected tags?");
		if (mb.open() == SWT.OK)
		{
			int[] indices = m_Table.getSelectionIndices();
			int[] ind = new int[indices.length];
			int nRemoved = 0;
			for (int i = 0; i < tags.length; i++)
			{
				if (XNDApp.app_localVM.DBTagDelete(tags[i].getText(1)))
				{
					// m_Table.remove(indices[i]);
					ind[nRemoved++] = indices[i];
				}
			}
			if (nRemoved > 0)
			{
				int[] ind1 = new int[nRemoved];
				for (int i = 0; i < nRemoved; i++)
					ind1[i] = ind[i];
				m_Table.remove(ind1);
				m_Table.redraw();
			}
		}
	}

	private void m_AddButtonKeyPressed(SelectionEvent evt)
	{
		DefineTagDialog d = new DefineTagDialog(new Shell(), XNDApp.app_localVM);
		/*
		 * InputDialog d = new InputDialog(new Shell(),"Define custom tag",
		 * "Tag name:","", new IInputValidator() { public String isValid(String
		 * txt) { String msg="Tag name cannot be empty"; if(txt==null) return
		 * msg; if(txt.length()<1) return msg; return null; } });
		 */
		if (d.open() != Window.OK)
			return;
		TagDescr td = d.GetTagDescr();
		if (!XNDApp.app_localVM.DBTagAdd(td))
		{
			Utils.ShowMessageBox("Database error", "Tag not added", Window.OK);
			return;
		}
		AddRow(td.IsSet(TagDescr.TABLE_DISPLAY), td.GetName());
		m_Table.redraw();
		if (td.IsSet(TagDescr.TABLE_DISPLAY))
			m_ViewCheckChanged = true;
	}
}