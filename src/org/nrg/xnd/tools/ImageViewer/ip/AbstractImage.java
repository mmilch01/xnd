package org.nrg.xnd.tools.ImageViewer.ip;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.nrg.xnd.utils.Utils;

/**
 * @author mmilch A base class for all entities that implement buffered images,
 *         and pixel-defined operations on them, that is, various image
 *         transformations.
 */
public abstract class AbstractImage
{
	public final static int BYTE = 1, SHORT = 2, RGB = 4, JPEG_LOSSY = 8,
			JPEG_LOSSLESS = 16, ZIP = 32;

	public final static int RightLeft = 0, TopBottom = 1, Rotate90 = 2,
			Rotate270 = 3, Rotate180 = 4;

	private File m_serFile = null;
	protected SlidingColorIndex m_ci;
	protected IndexColorModel m_cm;
	protected int m_wid = 0, m_ht = 0;
	protected int m_ltRatio = 4;
	protected int m_type = 0;
	protected BufferedImage m_bufImage;
	protected Image m_Image;
	protected final static Interpolate m_interp = new Interpolate();
	protected int m_bpp;
	protected int[] m_maxmin=null;
	protected Component m_Parent = null;
	protected boolean m_bLight = false;
	protected abstract void UpdateLtPixels();

	/**
	 * A basic constructor. Does not initialize actual pixels.
	 * 
	 * @param bpp
	 *            bits per pixel in the image.
	 * @param parent
	 *            Parent container.
	 */
	public AbstractImage(int bpp, Component parent)
	{
		m_bpp = bpp;
		m_Parent = parent;
	}

	protected int getBpp(int[] minmax)
	{
		int mx = minmax[1];
		int bpp = 0;
		while (mx > 1)
		{
			mx = mx / 2 + mx % 2;
			bpp = bpp + 1;
		}
		return bpp;
	}

	/**
	 * Used to set a window/level for this image.
	 * 
	 * @param wl
	 *            two-element double array with window and level settings.
	 * @throws OutOfMemoryError
	 */
	public void SetWL(double[] wl) throws OutOfMemoryError
	{
		m_ci.SetLookupTable((int) (wl[1] - wl[0] + 0.5),
				(int) (wl[1] + wl[0] + 0.5));
		InitColorModel();
		UpdateImageFromBuffer(false);
	}

	/**
	 * Should be called each time pixels are modified, to reflect an update in
	 * encapsulated Java image.
	 * 
	 * @param bLight
	 */
	protected abstract void UpdateImageFromBuffer(boolean bLight);

	/**
	 * Get source intensity at a given point (average in case of multiple
	 * samples per pixel
	 * 
	 * @param x
	 * @param y
	 * @return intensity value.
	 */
	public abstract long GetPix(int x, int y);

	/**
	 * @return maximum allowed intensity for current color index.
	 */
//	public int GetMaxPixelValue()
//	{
//		return m_ci.GetMaxPixelValue();
//	}
	public int getMinPixelValue()
	{
		return m_maxmin[0];
	}
	public int getMaxPixelValue() 
	{
		if(m_maxmin==null) m_maxmin=getPixelMaxMin();
		return m_maxmin[1];
	}
	public int getWid()
	{
		return m_bLight ? getLtWid() : m_wid;
	}

	public int getHt()
	{
		return m_bLight ? getLtHt() : m_ht;
	}

	protected int getLtWid()
	{
		return m_wid / m_ltRatio;
	}// -(m_wid/m_ltRatio)%4;}

	protected int getLtHt()
	{
		return m_ht / m_ltRatio;
	}// -(m_ht/m_ltRatio)%4;}

	public Image GetImage()
	{
		if(m_Image==null) UpdateImageFromBuffer(m_bLight);			
		return m_Image;
	}

	/**
	 * Should be called after constructor, to initialize internal pixel
	 * representation and encapsulated Java image.
	 * 
	 * @param pic
	 * @param wid
	 * @param ht
	 * @param code
	 */
	public abstract void CreateImage(Object pic, int wid, int ht, int code);

	public abstract void createReducedCopy(Object pic, int origW, int origH, int maxW, int maxH, int code);
	
	public boolean IsLyte()
	{
		return m_bLight;
	}

	public int GetLtRatio()
	{
		return m_ltRatio;
	}

	public void SetDefaultWL()
	{
		m_ci.SetDefaultLookupTable();
	}

	protected abstract int[] getPixelMaxMin();

	public boolean SerializeTemp(boolean is_loading) throws OutOfMemoryError,
			IOException
	{
		try
		{
			if (is_loading)
			{
				int oldW = getWid(), oldH = getHt();
				try
				{

					if (m_serFile == null)
						return false;
					if (!Serialize(new FileInputStream(m_serFile), true))
						return false;
					UpdateImageFromBuffer(false);
					return true;
				} catch (OutOfMemoryError ome)
				{
					UnloadFromMemory();
					m_wid = oldW;
					m_ht = oldH;
					throw (ome);
				}
			} else
			{
				if (m_serFile != null)
					m_serFile.deleteOnExit();
				m_serFile = Utils.GetNewTempFile();
				if (m_serFile == null)
					return false;
				if (!Serialize(new FileOutputStream(m_serFile), false))
					return false;
				return true;
			}
		} catch (FileNotFoundException e)
		{
			return false;
		}
	}

	public abstract void UnloadFromMemory();

	public abstract boolean Serialize(Object stream, boolean is_loading);

	public abstract boolean FlipRotate(int how);

	public abstract boolean InMemory();

	public abstract boolean Interpolate(int W, int H);

	public void SetDefaultLookupTable()
	{
		m_ci.SetDefaultLookupTable();
	}

	@Override
	public void finalize()
	{
		if (m_serFile != null)
			m_serFile.delete();
	}

	public void SwapWH()
	{
		int tmp;
		tmp = m_wid;
		m_wid = m_ht;
		m_ht = tmp;
	}

	public void WindowLevel(double dx, double dy, int scrSize, boolean bLight)
			throws OutOfMemoryError
	{
		Point w = m_ci.GetWL();
		double cx = Math.abs(dx), cy = Math.abs(dy);
		if (cx > cy)
			dy = 0;
		else
			dx = 0;
		if (!(!bLight && m_bLight))
		{
			if (cx + cy < scrSize / 100)
				return;
			double scale = getMaxPixelValue()/*m_ci.GetMaxPixelValue()*/ / (double) scrSize;
			int wmin = w.x + (int) (0.5 + scale * (dx + dy));
			int wmax = w.y + (int) (0.5 + scale * (dy - dx));
			m_ci.SetLookupTable(wmin, wmax);
		}
		InitColorModel();
		UpdateImageFromBuffer(bLight);
	}

	protected abstract void InitColorModel();

	public Point GetWL()
	{
		return m_ci.GetWL();
	}

	protected class SlidingColorIndex
	{
		Point m_WL = new Point();
		int m_StartIndex = 0, m_EndIndex = 0;
		int m_MaxPixelValue = 0;
		byte[] m_LookupTable = new byte[0];

		public boolean Serialize(Object stream, boolean is_loading)
				throws IOException
		{

			if (is_loading)
			{
				/*
				 * ObjectInputStream ois = (ObjectInputStream) stream; m_WL =
				 * Utils.SerializePoint(ois, null, is_loading); m_StartIndex =
				 * ois.readInt(); m_EndIndex = ois.readInt(); m_MaxPixelValue =
				 * ois.readInt(); m_LookupTable = (byte[])
				 * Utils.SerializeArray(ois, new byte[0], is_loading);
				 */
				return true;
			} else
			{
				/*
				 * ObjectOutputStream oos = (ObjectOutputStream) stream;
				 * Utils.SerializePoint(oos, m_WL, is_loading);
				 * oos.writeInt(m_StartIndex); oos.writeInt(m_EndIndex);
				 * oos.writeInt(m_MaxPixelValue); Utils.SerializeArray(oos,
				 * m_LookupTable, is_loading);
				 */
				return true;
			}
		}

		public SlidingColorIndex()
		{
			int nBits = Math.min(16, m_bpp);
			m_MaxPixelValue = PowerOf2(nBits) - 1;
			// m_MaxPixelValue = m_maxmin[1];
			m_WL.x = 0;
			// m_WL.x = m_maxmin[0];
			m_WL.y = m_MaxPixelValue;
			m_StartIndex = 0;// m_maxmin[0];
			m_EndIndex = m_MaxPixelValue;
			m_LookupTable = new byte[m_MaxPixelValue + 1];
			SetLookupTable(m_maxmin[0], m_MaxPixelValue, true);
		}

		public void SetDefaultLookupTable()
		{
			SetLookupTable(0, m_MaxPixelValue, false);
		}

		public int GetMaxPixelValue()
		{
			return m_MaxPixelValue;
		}

		public byte[] GetLookupTable()
		{
			return m_LookupTable;
		}

		public Point GetWL()
		{
			return m_WL;
		}

		public boolean SetLookupTable(int wmin, int wmax)
		{
			return SetLookupTable(wmin, wmax, false);
		}

		private boolean SetLookupTable(int wmin, int wmax, boolean first_time)
		{
			int w0 = m_WL.x, w1 = m_WL.y;
			if (wmin >= wmax)
			{
				int lev = (wmin + wmax + 1) / 2;
				wmin = lev - 1;
				wmax = lev + 1;
			}
			m_WL.x = Math.min(m_MaxPixelValue - 2, wmin);
			m_WL.y = Math.max(2, Math.max(m_WL.x + 2, wmax));

			if (w0 == m_WL.x && w1 == m_WL.y && !first_time)
			{
				return false; // return false if unchanged
			}

			int oldStartIndex = m_StartIndex, oldEndIndex = m_EndIndex;
			m_StartIndex = Math.max(m_WL.x, 0);
			m_EndIndex = Math.min(m_WL.y, m_MaxPixelValue);

			int start, end;
			if (first_time) // fill entire table
			{
				start = 0;
				end = m_MaxPixelValue;
			} else
			// fill up only to limiting indices
			{
				start = oldStartIndex;
				end = oldEndIndex;
			}
			int i, c;
			for (i = start; i < m_StartIndex; i++)
			{
				m_LookupTable[i] = 0;

			}
			int startci = (m_WL.x < 0) ? -m_WL.x * 255 : 0;
			int sz = m_WL.y - m_WL.x;

			for (i = m_StartIndex, c = startci; i <= m_EndIndex; i++, c += 255)
			{
				m_LookupTable[i] = (byte) (c / sz);

			}
			for (i = m_EndIndex + 1; i <= end; i++)
			{
				m_LookupTable[i] = (byte) 255;
			}
			return true;
		}

		private int PowerOf2(int power)
		{
			if (power > 32)
			{
				return 0;
			}
			int res = 1;
			for (int i = 0; i < power; i++)
			{
				res *= 2;
			}
			return res;
		}
	}// end of class SlidingColorIndex.
}