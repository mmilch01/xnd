package org.nrg.xnd.tools.ImageViewer;

import org.nrg.fileserver.ItemRecord;
import org.nrg.xnd.utils.LightXML;

public interface PixelExtractor
{
	public boolean LoadImage(ItemRecord ir);
	public int GetBpp();
	public LightXML GetImageInfo();
	public Object GetPixels();
}
