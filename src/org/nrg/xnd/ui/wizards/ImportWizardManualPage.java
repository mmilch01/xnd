package org.nrg.xnd.ui.wizards;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.ui.MemoCheckBox;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.ui.MemoTable;

public class ImportWizardManualPage extends WizardPage
{
	private Group group1;
	private Table m_TagsTable;
	private MemoTable m_TagsTableM;
	private Group group2;
	private LinkedList<Button> m_SystemTagNames = new LinkedList<Button>();
	private Button m_ClearUserTagsButton;
	private Label m_StatusLabel;
	private LinkedList<Combo> m_SystemTagVals = new LinkedList<Combo>();
	private LinkedList<MemoCombo> m_SystemTagMems = new LinkedList<MemoCombo>();
	private LinkedList<MemoCheckBox> m_SystemTagCheckMems = new LinkedList<MemoCheckBox>();

	public ImportWizardManualPage()
	{
		super("IWManualTagging", "Manual tagging", null);
	}
	private Collection<TagDescr> getSysTags()
	{
		TagDescr[] tgs = DefaultOntologyManager.GetDefaultTagDescrs();
		ImportWizard iw = (ImportWizard) getWizard();
		LinkedList<TagDescr> ltd = new LinkedList<TagDescr>();
		for (TagDescr td : tgs)
		{
			if (td.IsSet(TagDescr.INVISIBLE))
				continue;
			if (!iw.AllowedExtraSystemTag(td.GetName()))
				continue;
			ltd.add(td);
		}
		return ltd;
	}
	@Override
	public boolean isPageComplete()
	{
		for (int i = 0; i < m_SystemTagNames.size(); i++)
		{
			if (m_SystemTagNames.get(i).getSelection()
					&& m_SystemTagVals.get(i).getText().length() < 1)
			{
				m_StatusLabel
						.setText("Please set non-empty value for system tag "
								+ m_SystemTagNames.get(i).getText());
				return false;
			}
		}
		m_StatusLabel.setText("");
		return true;
	}
	@Override
	public void createControl(Composite parent)
	{
		Composite topLevel = new Composite(parent, SWT.NONE);
		GridLayout parentLayout = new GridLayout();
		parentLayout.makeColumnsEqualWidth = true;
		topLevel.setLayout(parentLayout);
		{
			GridData m_StatusLabelLData = new GridData();
			m_StatusLabelLData.horizontalAlignment = GridData.FILL;
			m_StatusLabel = new Label(topLevel, SWT.NONE);
			m_StatusLabel.setLayoutData(m_StatusLabelLData);
		}
		{
			group1 = new Group(topLevel, SWT.NONE);
			GridLayout group1Layout = new GridLayout();
			group1Layout.numColumns = 4;
			group1.setLayout(group1Layout);
			GridData group1LData = new GridData();
			group1.setLayoutData(group1LData);
			group1.setText("For all files, set the following system tags:");
			Collection<TagDescr> tds = getSysTags();
			for (TagDescr td : tds)
			{
				Button cb = new Button(group1, SWT.CHECK | SWT.LEFT);
				cb.setText(td.GetName());
				MemoCheckBox mcb = new MemoCheckBox(cb,
						"ImportWizardManualPage." + td.GetName() + ".check");
				cb.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent evt)
					{
						getWizard().getContainer().updateButtons();
					}
				});
				Combo c = new Combo(group1, SWT.NONE);
				MemoCombo mc = new MemoCombo(c, "ImportWizardManualPage."
						+ td.GetName(), 10);
				c.addModifyListener(new ModifyListener()
				{
					@Override
					public void modifyText(ModifyEvent evt)
					{
						getWizard().getContainer().updateButtons();
					}
				});
				m_SystemTagNames.add(cb);
				m_SystemTagVals.add(c);
				m_SystemTagMems.add(mc);
				m_SystemTagCheckMems.add(mcb);
			}
		}
		{
			group2 = new Group(topLevel, SWT.NONE);
			GridLayout group2Layout = new GridLayout();
			group2Layout.makeColumnsEqualWidth = true;
			group2.setLayout(group2Layout);
			GridData group2LData = new GridData();
			group2LData.grabExcessHorizontalSpace = true;
			group2LData.horizontalAlignment = GridData.FILL;
			group2.setLayoutData(group2LData);
			group2.setText("Additional tags:");
			{
				GridData m_TagsTableLData = new GridData();
				m_TagsTable = new Table(group2, SWT.BORDER | SWT.V_SCROLL
						| SWT.H_SCROLL | SWT.FOCUSED);
				m_TagsTable.setLayoutData(m_TagsTableLData);
				InitTable();
				m_TagsTableM = new MemoTable(m_TagsTable,
						"ImportWizardManualPage.TagsTable");
				m_TagsTable.addControlListener(new ControlAdapter()
				{
					@Override
					public void controlResized(ControlEvent evt)
					{
						Point p = m_TagsTable.getSize();
						m_TagsTable.getColumn(0).setWidth(p.x / 4);
						m_TagsTable.getColumn(1).setWidth(3 * (p.x / 4));
					}
				});

			}
			{
				m_ClearUserTagsButton = new Button(group2, SWT.PUSH
						| SWT.CENTER);
				GridData m_ClearUserTagsButtonLData = new GridData();
				m_ClearUserTagsButtonLData.horizontalAlignment = GridData.END;
				m_ClearUserTagsButton.setLayoutData(m_ClearUserTagsButtonLData);
				m_ClearUserTagsButton.setText("Clear All");
				m_ClearUserTagsButton
						.addSelectionListener(new SelectionAdapter()
						{
							@Override
							public void widgetSelected(SelectionEvent evt)
							{
								m_TagsTable.clearAll();
							}
						});
			}
		}
		setControl(topLevel);
		setPageComplete(isPageComplete());
	}
	private void InitTable()
	{
		m_TagsTable.setHeaderVisible(true);
		m_TagsTable.setLinesVisible(true);

		TableColumn nmCol = new TableColumn(m_TagsTable, SWT.LEFT);
		nmCol.setText("Name");
		nmCol.setWidth(120);
		TableColumn valCol = new TableColumn(m_TagsTable, SWT.LEFT);
		valCol.setText("Value (optional)");
		valCol.setWidth(240);
		for (int i = 0; i < 10; i++)
		{
			(new TableItem(m_TagsTable, SWT.NONE)).setText("");
		}
		final TableEditor te = new TableEditor(m_TagsTable);
		te.horizontalAlignment = SWT.LEFT;
		te.grabHorizontal = true;
		te.minimumWidth = 50;
		m_TagsTable.addListener(SWT.MouseDown, new Listener()
		{
			@Override
			public void handleEvent(Event e)
			{
				m_TagsTable.setFocus();
				Control oldEd = te.getEditor();
				if (oldEd != null)
					oldEd.dispose();
				Point pt = new Point(e.x, e.y);
				TableItem ti = getFullItem(pt);// m_TagsTable.getItem(pt);
				if (ti == null)
					return;
				final int col;
				if (ti.getBounds(0).contains(pt))
					col = 0;
				else if (ti.getBounds(1).contains(pt))
					col = 1;
				else
					return;
				Text newEd = new Text(m_TagsTable, SWT.NONE);
				newEd.setText(ti.getText(col));
				newEd.addModifyListener(new ModifyListener()
				{
					@Override
					public void modifyText(ModifyEvent e)
					{
						Text text = (Text) te.getEditor();
						te.getItem().setText(col, text.getText());
					}
				});
				newEd.selectAll();
				newEd.setFocus();
				te.setEditor(newEd, ti, col);
			}
		});
	}
	protected TableItem getFullItem(Point pt)
	{
		for (TableItem ti : m_TagsTable.getItems())
		{
			Rectangle b0 = ti.getBounds(0);
			Rectangle b1 = ti.getBounds(1);
			if (b0.contains(pt) || b1.contains(pt))
				return ti;
		}
		return null;
	}
	public void Save()
	{
		for (MemoCheckBox mcb : m_SystemTagCheckMems)
			mcb.Save();
		for (MemoCombo mc : m_SystemTagMems)
			mc.Save();
		m_TagsTableM.Save();
	}
	public Collection<ItemTag> getDefinedUserTags()
	{
		LinkedList<ItemTag> llit = new LinkedList<ItemTag>();
		String nm, val;
		for (TableItem ti : m_TagsTable.getItems())
		{
			nm = ti.getText(0);
			val = ti.getText(1);
			if (nm == null || nm.length() < 1)
				continue;
			llit.add(new ItemTag(nm, val));
		}
		return llit;
	}
	public Collection<ItemTag> getDefinedSystemTags()
	{
		LinkedList<ItemTag> llit = new LinkedList<ItemTag>();
		for (int i = 0; i < m_SystemTagNames.size(); i++)
		{
			if (m_SystemTagNames.get(i).getSelection())
				llit.add(new ItemTag(m_SystemTagNames.get(i).getText(),
						m_SystemTagVals.get(i).getText()));
		}
		return llit;
	}
}