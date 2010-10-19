package org.nrg.xnd.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.ViewFilter;
import org.nrg.xnd.ui.dialogs.ModifyFilterDialog;

public class PrefsFilter extends PreferencePage
		implements
			IWorkbenchPreferencePage
{
	private Group group1;
	private Button m_AddButton;
	private Button m_DeleteButton;
	private Button m_EditButton;
	private Table m_filters;
	private TableColumn m_nmCol, m_DescrCol;

	@Override
	protected Control createContents(Composite parent)
	{
		noDefaultAndApplyButton();
		{
			parent.setSize(372, 256);
			{
				group1 = new Group(parent, SWT.NONE);
				GridLayout group1Layout = new GridLayout();
				group1Layout.numColumns = 2;
				group1.setLayout(group1Layout);
				group1.setText("Stored filters");
				{
					GridData m_filtersLData = new GridData();
					m_filtersLData.widthHint = 257;
					m_filtersLData.heightHint = 212;
					m_filtersLData.verticalSpan = 4;
					m_filters = new Table(group1, SWT.DRAW_DELIMITER
							| SWT.RIGHT | SWT.SINGLE | SWT.BORDER | SWT.MULTI
							| SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

					m_filters.setLayoutData(m_filtersLData);
					m_nmCol = new TableColumn(m_filters, SWT.LEFT | SWT.BORDER);
					m_nmCol.setText("Name");
					m_nmCol.setWidth(50);
					m_DescrCol = new TableColumn(m_filters, SWT.LEFT
							| SWT.BORDER);
					m_DescrCol.setText("Description");
					m_DescrCol.setWidth(200);
					m_filters.setSize(250, 100);
					m_filters.setHeaderVisible(true);
					UpdateTable();
					m_filters.pack(true);
				}
				{
					m_AddButton = new Button(group1, SWT.PUSH | SWT.CENTER);
					GridData m_AddButtonLData = new GridData();
					m_AddButtonLData.widthHint = 71;
					m_AddButtonLData.heightHint = 23;
					m_AddButton.setLayoutData(m_AddButtonLData);
					m_AddButton.setText("Add");
					m_AddButton.addSelectionListener(new SelectionListener()
					{
						public void widgetSelected(SelectionEvent evt)
						{
							ViewFilter vf = new ViewFilter();
							ModifyFilterDialog mfd = new ModifyFilterDialog(
									new Shell(), "", vf, XNDApp.app_localVM);
							if (mfd.open() == Window.OK)
							{
								XNDApp.app_filters.put(mfd.GetName(), vf);
								AddRow(mfd.GetName(), vf);
							}
						}
						public void widgetDefaultSelected(SelectionEvent evt)
						{
						}
					});
				}
				{
					m_EditButton = new Button(group1, SWT.PUSH | SWT.CENTER);
					GridData m_EditButtonLData = new GridData();
					m_EditButtonLData.widthHint = 71;
					m_EditButtonLData.heightHint = 23;
					m_EditButton.setLayoutData(m_EditButtonLData);
					m_EditButton.setText("Edit");
					m_EditButton.addSelectionListener(new SelectionListener()
					{
						public void widgetSelected(SelectionEvent evt)
						{
							TableItem[] ti = m_filters.getSelection();
							if (ti.length < 1)
								return;
							ViewFilter vf = XNDApp.app_filters.get(ti[0]
									.getText(0));
							ModifyFilterDialog mfd = new ModifyFilterDialog(
									new Shell(), ti[0].getText(0), vf,
									XNDApp.app_localVM);
							if (mfd.open() == Window.OK)
							{
								XNDApp.app_filters.put(mfd.GetName(), vf);
								ti[0].setText(mfd.GetStrings());
								m_filters.redraw();
							}
						}
						public void widgetDefaultSelected(SelectionEvent evt)
						{
						}

					});
				}
				{
					m_DeleteButton = new Button(group1, SWT.PUSH | SWT.CENTER);
					GridData m_DeleteButtonLData = new GridData();
					m_DeleteButtonLData.widthHint = 71;
					m_DeleteButtonLData.heightHint = 23;
					m_DeleteButton.setLayoutData(m_DeleteButtonLData);
					m_DeleteButton.setText("Delete");
					m_DeleteButton.addSelectionListener(new SelectionListener()
					{
						public void widgetSelected(SelectionEvent evt)
						{
							TableItem[] items = m_filters.getSelection();
							if (items.length < 1)
								return;

							MessageBox mb = new MessageBox(new Shell(), SWT.OK
									| SWT.CANCEL);
							mb.setText("Confirm delete");
							mb.setMessage("Delete selected filters?");
							if (mb.open() == SWT.OK)
							{
								int[] indices = m_filters.getSelectionIndices();
								TableItem[] ti = m_filters.getSelection();
								XNDApp.app_filters.remove(ti[0].getText(0));
								m_filters.remove(indices[0]);
								m_filters.redraw();
							}
						}
						public void widgetDefaultSelected(SelectionEvent evt)
						{
						}

					});
				}
			}
		}

		return null;
	}
	protected void UpdateTable()
	{
		for (String s : XNDApp.app_filters.keySet())
			AddRow(s, XNDApp.app_filters.get(s));
		m_filters.pack(true);
	}

	private void AddRow(String nm, ViewFilter vf)
	{
		TableItem ti = new TableItem(m_filters, SWT.CENTER, 0);
		ti.setText(0, nm);
		ti.setText(1, vf.GetDescription(false));
	}

	public void init(IWorkbench workbench)
	{

	}

}
