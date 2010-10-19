package org.nrg.xnd.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.nrg.fileserver.ItemTag;
import org.nrg.fileserver.TagSet;

public class TagTable extends Table
{
	private TagSet m_tags;
	public static Listener getEditorListener(final Table t,
			final TableEditor te, final int... cols)
	{
		return new Listener()
		{
			public void handleEvent(Event e)
			{
				t.setFocus();
				Control oldEd = te.getEditor();
				if (oldEd != null)
					oldEd.dispose();
				Point pt = new Point(e.x, e.y);
				TableItem ti = getFullItem(t, pt);
				if (ti == null)
					return;
				int col = -1;

				for (int i = 0; i < cols.length; i++)
				{
					if (ti.getBounds(cols[i]).contains(pt))
					{
						col = cols[i];
						break;
					}
				}
				if (col == -1)
					return;
				Text newEd = new Text(t, SWT.NONE);
				newEd.setText(ti.getText(col));
				final int newCol = col;
				newEd.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent e)
					{
						Text text = (Text) te.getEditor();
						te.getItem().setText(newCol, text.getText());
					}
				});
				newEd.selectAll();
				newEd.setFocus();
				te.setEditor(newEd, ti, col);
			}
		};
	}

	public TagTable(Composite parent, int style, TagSet tags)
	{
		super(parent, style);
		setHeaderVisible(true);
		setLinesVisible(true);
		TableColumn nmCol = new TableColumn(this, SWT.LEFT);
		nmCol.setText("Name");

		TableColumn valCol = new TableColumn(this, SWT.LEFT);
		valCol.setText("Value");
		final TableEditor te = new TableEditor(this);
		te.horizontalAlignment = SWT.LEFT;
		te.grabHorizontal = true;
		te.minimumWidth = 50;
		this.addListener(SWT.MouseDown, getEditorListener(this, te, 0, 1));
		/*
		 * this.addListener(SWT.MouseDown, new Listener() { public void
		 * handleEvent(Event e) { TagTable.this.setFocus(); Control oldEd =
		 * te.getEditor(); if(oldEd != null) oldEd.dispose(); Point pt = new
		 * Point(e.x,e.y); TableItem ti = getFullItem(TagTable.this,pt); if
		 * (ti==null) return; final int col; if (ti.getBounds(0).contains(pt))
		 * col = 0; else if (ti.getBounds(1).contains(pt)) col = 1; else return;
		 * 
		 * Text newEd = new Text(TagTable.this, SWT.NONE);
		 * newEd.setText(ti.getText(col)); newEd.addModifyListener(new
		 * ModifyListener() { public void modifyText(ModifyEvent e) { Text text
		 * = (Text) te.getEditor(); te.getItem().setText(col, text.getText()); }
		 * }); newEd.selectAll(); newEd.setFocus(); te.setEditor(newEd, ti,
		 * col); } });
		 */
		this.addControlListener(new ControlAdapter()
		{
			public void controlResized(ControlEvent evt)
			{
				Point p = getSize();
				getColumn(0).setWidth(p.x / 4);
				getColumn(1).setWidth(3 * (p.x / 4));
			}
		});
		m_tags = tags;
	}
	public TagSet getTags()
	{
		return m_tags;
	}

	private static TableItem getFullItem(Table t, Point pt)
	{
		for (TableItem ti : t.getItems())
		{
			for (int j = 0; j < t.getColumnCount(); j++)
			{
				Rectangle r = ti.getBounds(j);
				if (r.contains(pt))
					return ti;
			}
		}
		return null;
	}
	public void refresh()
	{
		this.removeAll();
		TableItem ti;
		for (ItemTag it : m_tags)
		{
			ti = new TableItem(this, SWT.NONE);
			ti.setText(0, it.GetName());
			ti.setText(1, it.GetFirstValue()); // ?? support for one value only
												// at this point, to be
												// elaborated.
		}
	}
}