package org.nrg.xnd.app;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public final class IImageKeys
{
	public static final int FOLDER_CLOSED = 0;
	private static final String FC_P = "icons/fold_closed.gif";

	public static final int FOLDER_OPEN = 1;
	public static final String FO_P = "icons/fold_open.gif";

	public static final int FILE_BLANK = 2;
	public static final String FB_P = "icons/file_blank.gif";

	public static final int PLUS = 3;
	public static final String PL_P = "icons/addfolder.gif";

	public static final int ROOT = 4;
	public static final String RT_P = "icons/root.gif";

	public static final int LABEL = 5;
	public static final String LB_P = "icons/manag_tags.gif";

	public static final int CONNECT = 6;
	public static final String CN_P = "icons/connect.gif";

	public static final int TAGVIEW = 7;
	public static final String TV_P = "icons/tagview.gif";

	public static final int REMOTE = 8;
	public static final String RM_P = "icons/remote.gif";

	public static final int LOCAL = 9;
	public static final String LC_P = "icons/local.gif";

	public static final int REFRESH = 10;
	public static final String RF_P = "icons/refresh.gif";

	public static final int UPLOAD = 11;
	public static final String UP_P = "icons/upload.gif";

	public static final int DOWNLOAD = 12;
	public static final String DW_P = "icons/download.gif";

	public static final int IMVIEWER = 13;
	public static final String IV_P = "icons/imviewer.gif";

	public static final int FILTER = 14;
	public static final String FT_P = "icons/filter.gif";

	public static final int DB = 15;
	public static final String DB_P = "icons/db.gif";

	public static final int DR = 16;
	public static final String DR_P = "icons/dr.gif";

	public static final int FRAGMENT = 17;
	public static final String FR_P = "icons/frag.gif";

	public static final int COLLECTION = 18;
	public static final String CL_P = "icons/collection.gif";

	public static final int DOTS = 19;
	public static final String DT_P = "icons/dots.gif";

	public static final int XNAT16 = 20;
	public static final String XN16 = "icons/xnat16.gif";

	public static final int WIZARD = 21;
	public static final String WIZ = "icons/wizard.gif";

	private static final int[] keys = {FOLDER_CLOSED, FOLDER_OPEN, FILE_BLANK,
			PLUS, ROOT, LABEL, CONNECT, TAGVIEW, REMOTE, LOCAL, REFRESH,
			UPLOAD, DOWNLOAD, IMVIEWER, FILTER, DB, DR, FRAGMENT, COLLECTION,
			DOTS, XNAT16, WIZARD};
	private static final String[] keyPaths = {FC_P, FO_P, FB_P, PL_P, RT_P,
			LB_P, CN_P, TV_P, RM_P, LC_P, RF_P, UP_P, DW_P, IV_P, FT_P, DB_P,
			DR_P, FR_P, CL_P, DT_P, XN16, WIZ};
	public static ImageDescriptor[] descriptors;
	private static ImAttr[] m_attrs;

	public static void Init()
	{
		m_attrs = new ImAttr[keys.length];
		for (int i = 0; i < keys.length; i++)
			m_attrs[i] = new IImageKeys().new ImAttr(keyPaths[i]);
	}
	public static Image GetImage(int id)
	{
		return m_attrs[id].m_im;
	}
	public static ImageDescriptor GetImDescr(int id)
	{
		return m_attrs[id].m_id;
	}

	class ImAttr
	{
		public ImageDescriptor m_id;
		public Image m_im;

		public ImageDescriptor GetID()
		{
			return m_id;
		}
		public Image GetImage()
		{
			return m_im;
		}
		public ImAttr(String path)
		{
			m_id = AbstractUIPlugin.imageDescriptorFromPlugin(
					"org.nrg.xnat.desktop", path);
			m_im = (Image) m_id.createResource(Display.findDisplay(Thread
					.currentThread()));
		}
	}
}