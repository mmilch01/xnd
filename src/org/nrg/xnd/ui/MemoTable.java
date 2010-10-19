package org.nrg.xnd.ui;

import java.util.LinkedList;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.nrg.xnd.utils.Utils;

public class MemoTable
{
	private String m_uid;
	public Table m_table;
	private int m_nCols = -1;
	public MemoTable(Table table, String uid)
	{
		m_table = table;
		m_nCols = m_table.getColumnCount();
		m_uid = uid;
		LinkedList<String> rows = new LinkedList<String>();
		TableColumn tc;
		for (int i = 0; i < table.getColumnCount(); i++)
		{
			tc = table.getColumn(i);
			// serialize rows in a given column.
			Utils.SerializeListOfValues(m_uid + "." + tc.getText(), rows, true);
			// set value for each row
			for (int j = 0; j < rows.size(); j++)
				table.getItem(j).setText(i, rows.get(j));
			rows.clear();
		}
	}
	public void Save()
	{
		LinkedList<String> rows = new LinkedList<String>();
		for (int i = 0; i < m_nCols; i++)
		{
			// save each column as an array of values.
			for (TableItem ti : m_table.getItems())
			{
				if (!isEmpty(ti))
					rows.add(ti.getText(i));
			}
			// return in case when table is empty.
			// if(i==0 && rows.size()<1) return;
			Utils.SerializeListOfValues(m_uid + "."
					+ m_table.getColumn(i).getText(), rows, false);
			rows.clear();
		}
	}
	private boolean isEmpty(TableItem ti)
	{
		for (int i = 0; i < m_nCols; i++)
		{
			if (ti.getText(i).length() > 0)
				return false;
		}
		return true;
	}
}
