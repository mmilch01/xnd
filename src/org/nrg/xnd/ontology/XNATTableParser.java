package org.nrg.xnd.ontology;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.Element;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;

public abstract class XNATTableParser
{
	public static boolean SetTag(ItemRecord ir, String col_name, String name,
			TreeMap<String, String> row)
	{
		String val = row.get(col_name);
		if (val == null)
			return false;
		ir.tagSet(new ItemTag(name != null ? name : col_name, val));
		return true;
	}
	public static void SetAllTags(ItemRecord ir, TreeMap<String, String> row)
	{
		for (String col_name : row.keySet())
			SetTag(ir, col_name, null, row);
	}
	public static LinkedList<TreeMap<String, String>> GetRows(Document d)
	{
		return GetRows(d, true, null);
	}
	public static LinkedList<TreeMap<String, String>> GetRows(Document d,
			boolean indexByText, String indexAttr)
	{
		Vector<String> m_columns = new Vector<String>();
		LinkedList<TreeMap<String, String>> m_rows = new LinkedList<TreeMap<String, String>>();
		Iterator<TreeMap<String, String>> m_iterator = null;
		TreeMap<String, String> m_currentRow = null;
		Element root = d.getRootElement();

		Element columns = root.element("results").element("columns");
		Element rows = root.element("results").element("rows");

		Element el;

		String val;
		for (Iterator<Element> it = columns.elementIterator(); it.hasNext();)
		{
			el = it.next();
			if (indexByText)
				m_columns.add(el.getText());
			else
			{
				val = el.attributeValue(indexAttr);
				if (val != null)
					m_columns.add(val);
				else
					m_columns.add("null_" + el.getText());
			}
		}
		Element cell;
		int ind;
		TreeMap<String, String> row;
		for (Iterator<Element> itr = rows.elementIterator(); itr.hasNext();)
		{
			el = itr.next();
			ind = 0;
			row = new TreeMap<String, String>();
			for (Iterator<Element> it = el.elementIterator(); it.hasNext();)
			{
				cell = it.next();
				row.put(m_columns.elementAt(ind++), cell.getText());
			}
			m_rows.add(row);
		}
		return m_rows;
	}
}
