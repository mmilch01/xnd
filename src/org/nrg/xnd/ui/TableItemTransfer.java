package org.nrg.xnd.ui;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.LinkedList;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.nrg.fileserver.ItemRecord;
import org.nrg.xnd.utils.Utils;

public class TableItemTransfer extends ByteArrayTransfer
{
	private static final String TYPE = "local_item_record";
	private static final int TYPEID = registerType(TYPE);
	private static TableItemTransfer m_instance = new TableItemTransfer();

	@Override
	protected int[] getTypeIds()
	{
		return new int[]{TYPEID};
	}
	@Override
	protected String[] getTypeNames()
	{
		return new String[]{TYPE};
	}
	public static TableItemTransfer getInstance()
	{
		return m_instance;
	}
	@Override
	public void javaToNative(Object o, TransferData td)
	{
		// ??
		/*
		 * if(o==null || !(o instanceof TableItem[])) return; //
		 * if(!(((TableItem)o).getData() instanceof FileItem)) return;
		 * TableItem[] tis=(TableItem[])o; ItemRecord[] irs=new
		 * ItemRecord[tis.length]; for(int i=0; i<irs.length; i++)
		 * irs[i]=((TableElement)(tis[i].getData())).GetItemRecord();
		 * if(!isSupportedType(td)) return; try { ByteArrayOutputStream baos=new
		 * ByteArrayOutputStream(); ObjectOutputStream oos=new
		 * ObjectOutputStream(baos); for(int i=0; i<irs.length; i++) {
		 * Utils.SerializeItemRecord(oos, irs[i], false); } oos.flush(); byte []
		 * buf=baos.toByteArray(); oos.close(); super.javaToNative(buf, td); }
		 * catch(Exception e){}
		 */
	}
	@Override
	public Object nativeToJava(TransferData td)
	{
		if (!isSupportedType(td))
			return null;
		byte[] buf = (byte[]) super.nativeToJava(td);
		if (buf == null)
			return null;

		LinkedList<ItemRecord> irs = new LinkedList<ItemRecord>();
		ItemRecord ir;
		try
		{
			ObjectInputStream oin = new ObjectInputStream(
					new ByteArrayInputStream(buf));
			while (oin.available() > 20)
			{
				ir = Utils.SerializeItemRecord(oin, null, true);
				if (ir != null)
					irs.add(ir);
			}
		} catch (Exception e)
		{
			return null;
		}
		return irs.toArray(new ItemRecord[0]);
	}
}
