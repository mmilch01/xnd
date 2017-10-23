package org.nrg.xnd.tools.ImageViewer;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;

/**
 * ************************************************************************ This
 * class is responsible for transformations from image rectangle to screen
 * rectangle (fitting, zooming, etc)
 * ************************************************************************
 */
public class ScreenMap
{
	Rectangle m_rScreen;
	Rectangle m_rImage;
	private DicomImage dicomImage;

	public Rectangle getM_rImage()
	{
		return m_rImage;
	}

	public ScreenMap(DicomImage dicomImage)
	{
		this.dicomImage = dicomImage;
		m_rScreen = new Rectangle();
		m_rImage = new Rectangle();
	}

	public boolean Serialize(Object stream, boolean is_loading)
			throws IOException
	{
		return false;
		/*
		 * if (is_loading) { m_rScreen = Utils.SerializeRectangle(stream, null,
		 * is_loading); m_rImage = Utils.SerializeRectangle(stream, null,
		 * is_loading); return true; } else { Utils.SerializeRectangle(stream,
		 * m_rScreen, is_loading); Utils.SerializeRectangle(stream, m_rImage,
		 * is_loading); return true; }
		 */
	}

	public Rectangle ScreenToWindow(Rectangle r)
	{
		Point p0 = ScreenToWindow(new Point(r.x, r.y));
		Point p1 = ScreenToWindow(new Point(r.x + r.width, r.y + r.height));
		return new Rectangle(p0.x, p0.y, p1.x - p0.x, p1.y - p0.y);
	}

	public Point ScreenToWindow(Point pt)
	{
		int Vcx = m_rScreen.x, Vcy = m_rScreen.y;
		int Vsx = m_rScreen.width, Vsy = m_rScreen.height;
		int Wcx = m_rImage.x, Wcy = m_rImage.y;
		int Wsx = m_rImage.width, Wsy = m_rImage.height;
		double Scale = (double) Wsx / (double) Vsx; // Uniform
		// scale
		double xx, yy;
		xx = (pt.x - Vcx) * Scale + Wcx;
		yy = (pt.y - Vcy) * Scale + Wcy;
		return new Point((int) (xx + 0.5), (int) (yy + 0.5));
	}

	public Point WindowToScreen(Point pt)
	{
		int Vcx = m_rScreen.x, Vcy = m_rScreen.y;
		int Vsx = m_rScreen.width, Vsy = m_rScreen.height;
		int Wcx = m_rImage.x, Wcy = m_rImage.y;
		int Wsx = m_rImage.width, Wsy = m_rImage.height;
		double Scale = (double) Vsx / (double) Wsx;
		double xx, yy;
		xx = (pt.x - Wcx) * Scale + Vcx;
		yy = (pt.y - Wcy) * Scale + Vcy;
		return new Point((int) (xx + 0.5), (int) (yy + 0.5));
	}

	/**
	 * ********************************************************************
	 * Changes rectangle r proportionally according to ratio factor. Minimum
	 * change when ratio!=1 is 1 pixel.
	 * ********************************************************************
	 */
	public void ZoomRect(Rectangle r, float ratio)
	{
		int shiftX, shiftY;
		if (r == null || ratio == 1.0)
		{
			return;
		}
		if (ratio > 1)
		{
			shiftX = Math.max(1, Math.round(r.width * (ratio - 1)));
			shiftY = Math.max(1, Math.round(r.height * (ratio - 1)));
		} else
		{
			shiftX = Math.min(-1, Math.round(r.width * (ratio - 1)));
			shiftY = Math.min(-1, Math.round(r.height * (ratio - 1)));
		}
		r.x += shiftX;
		r.y += shiftY;
		r.width -= 2 * shiftX;
		r.height -= 2 * shiftY;
	}

	/**
	 * ********************************************************************
	 * Returns false if further increasing/decreasing of image zoom is not
	 * allowed.
	 * ********************************************************************
	 */
	public boolean IsZoomValid(float zoom, boolean isIncreasing)
	{
		if (zoom > 10)
		{
			return false;
		}
		// double wid=getWid()*zoom, ht=getHt()*zoom;
		// if (wid<m_rScreen.width || ht<m_rScreen.height)
		// return true;
		// return false;

		if (zoom > 10)
		{
			return false;
		}
		if (zoom < 1)
		{
			if (dicomImage.getWid() > m_rImage.width
					|| dicomImage.getHt() > m_rImage.height)
			{
				return true;
			}
		}
		if (zoom < 1 && !isIncreasing)
		{
			return false;
		}
		zoom = Math.min(m_rImage.height / 8, m_rImage.width / 8);
		if (zoom < 1 && isIncreasing)
		{
			return false;
		} else
		{
			return true;
		}
	}

	/**
	 * ********************************************************************
	 * changes region of image to be displayed according to ratio and checking
	 * bounds.
	 * ********************************************************************
	 */
	public void ZoomImageRect(float ratio)
	{
		ZoomRect(m_rImage, ratio);
		if (m_rImage.x < 0)
		{
			m_rImage.x = 0;
		}
		if (m_rImage.y < 0)
		{
			m_rImage.y = 0;
		}
		if (m_rImage.width > dicomImage.getWid())
		{
			m_rImage.width = dicomImage.getWid();
		}
		if (m_rImage.height > dicomImage.getHt())
		{
			m_rImage.height = dicomImage.getHt();
		}
		if (m_rImage.x + m_rImage.width > dicomImage.getWid())
		{
			m_rImage.x = dicomImage.getWid() - m_rImage.width;
		}
		if (m_rImage.y + m_rImage.height > dicomImage.getHt())
		{
			m_rImage.y = dicomImage.getHt() - m_rImage.height;
		}
	}

	/**
	 * ********************************************************************
	 * Moves image displaying rectangle according to move vector. Checks for
	 * border integrity.
	 * ********************************************************************
	 */
	boolean ShiftImageRect(int shiftX, int shiftY)
	{
		Point pt0 = ScreenToWindow(new Point(0, 0));
		Point pt1 = ScreenToWindow(new Point(Math.abs(shiftX), Math.abs(shiftY)));
		int dx = (shiftX > 0) ? pt1.x - pt0.x : pt0.x - pt1.x, dy = (shiftY > 0)
				? pt1.y - pt0.y
				: pt0.y - pt1.y;
		if (dx == 0 && dy == 0)
		{
			return false;
		}
		m_rImage.x = Math.min(dicomImage.getWid() - m_rImage.width, Math.max(
				m_rImage.x + dx, 0));
		m_rImage.y = Math.min(dicomImage.getHt() - m_rImage.height, Math.max(
				m_rImage.y + dy, 0));
		return true;
	}

	/**
	 * ********************************************************************
	 * Changes screen displaying area to fit (proportionally) to given rectangle
	 * and be less then that by ratio.
	 * ********************************************************************
	 */
	void FitInsideScreenRect(Rectangle screen, float ratio)
	{
		double c = Math.min(screen.width / (1.0 + m_rScreen.width),
				screen.height / (1.0 + m_rScreen.height));
		int w = (int) Math.max(4, c * m_rScreen.width);
		int h = (int) Math.max(4, c * m_rScreen.height);
		int x = (2 * screen.x + screen.width) / 2;
		int y = (2 * screen.y + screen.height) / 2;
		m_rScreen = new Rectangle(x - w / 2, y - h / 2, w, h);
		ZoomRect(m_rScreen, ratio);
	}
} // end of class ScreenMap
