package org.nrg.xnd.filetransfer;

import org.nrg.fileserver.ItemRecord;

public interface FileTransfer
{
	public boolean Put(ItemRecord dest, ItemRecord src);
	public boolean Get(ItemRecord dest, ItemRecord src);
}