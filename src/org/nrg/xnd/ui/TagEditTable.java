package org.nrg.xnd.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.TagSet;

public class TagEditTable
{
	private Table m_table;
	public Table getTable()
	{
		return m_table;
	}
	public void setTags(ItemRecord ir)
	{
		m_table.removeAll();
		TagSet ts = ir.getTagCollection();
		for (ItemTag it : ts)
		{
			createTableItem(it);
		}
	}

	private TableItem createTableItem(ItemTag it)
	{
		TableItem ti = new TableItem(m_table, SWT.NONE);
		ti.setText(0, it.GetName());
		ti.setText(1, it.getAllValuesAsString());
		return ti;
	}

	public TagEditTable(Composite parent, int style)
	{
		m_table = new Table(parent, style);
		m_table.setHeaderVisible(true);
		m_table.setLinesVisible(true);
		TableColumn nmColumn = new TableColumn(m_table, SWT.LEFT);
		nmColumn.setText("Name");
		nmColumn.setWidth(120);
		TableColumn valCol = new TableColumn(m_table, SWT.LEFT);
		valCol.setText("Value");
		valCol.setWidth(240);
		final TableEditor te = new TableEditor(m_table);
		te.horizontalAlignment = SWT.LEFT;
		te.grabHorizontal = true;
		te.minimumWidth = 50;
		// m_table.addListener(SWT.MouseDown,
		// TagTable.getEditorListener(m_table, te, 0,1));
	}
}