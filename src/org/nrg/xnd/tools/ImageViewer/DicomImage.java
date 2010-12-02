package org.nrg.xnd.tools.ImageViewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che2.imageioimpl.plugins.dcm.DicomImageReader;
import org.nrg.xnd.tools.ImageViewer.ip.AbstractImage;
import org.nrg.xnd.tools.ImageViewer.ip.ByteImage;
import org.nrg.xnd.tools.ImageViewer.ip.ColorImage;
import org.nrg.xnd.tools.ImageViewer.ip.ShortImage;
import org.nrg.xnd.utils.LightXML;
import org.nrg.xnd.utils.dicom.SeriesElementRecord;

/**
 * ****************************************************************************
 * This class is responsible for each separate image being displayed
 * ****************************************************************************
 */
public class DicomImage
{
	private float m_currentZoom = 1;
	private ScreenMap scrMap = new ScreenMap(this);
	private Point m_RPoint0 = new Point();
	private Point m_RPoint1 = new Point(); // current tool points
	private Point m_OldRPoint1 = new Point(); // Old endpoint for
	private Component m_Parent = null;

	// ruler
	private Point m_RScrPoint0 = new Point();
	private Point m_RScrPoint1 = new Point();
	private Rectangle m_OldCrosslinkRect = null;
	private boolean m_FirstTool = true, m_bCrosslink = false,
			m_bNeedRedraw = false;
	private double m_PixelDX2, m_PixelDY2;
	private double[] m_Hu = new double[2];

	public int m_iNumInterp = 0;

	private String[] m_Info = new String[4];
	private String[] m_SideStr = new String[4];
	private Vector<Point3D> m_CrosslRects = new Vector<Point3D>();
	private double[] m_CLTransf = new double[9];
	private Point3D m_CLTranslate = new Point3D(0, 0, 0);
	public static final int LEFT = 1, TOP = 2, RIGHT = 4, BOTTOM = 8;
	static final int PREVIEW_DIM = 32;
	private boolean m_bPreview = false;
	private AbstractImage m_Image;

	public int Width()
	{
		return m_Image.getWid();
	}

	public int Height()
	{
		return m_Image.getHt();
	}

	public boolean CanCrossLink()
	{
		return m_bCrosslink;
	}

	public Object CrossLinkRect()
	{
		return m_CrosslRects.toArray(new Point3D[0]);
	}

	public ScreenMap GetScreenMap()
	{
		return scrMap;
	}

	public void SetDefaultLookupTable() throws OutOfMemoryError
	{
		m_Image.SetDefaultWL();
	}

	protected void finalize()
	{
		m_Image.finalize();
	}

	public AbstractImage GetImage()
	{
		return m_Image;
	}

	public void Resample(double factor) throws OutOfMemoryError
	{
		int newW = (int) (((double) m_Image.getWid()) * factor + 0.5), newH = (int) (((double) m_Image
				.getWid())
				* factor + 0.5);
		Resample(newW, newH);
	}

	private void Resample(int newW, int newH) throws OutOfMemoryError
	{
		int oldw = getWid(), oldh = getHt();
		if (m_Image.Interpolate(newW, newH))
		{
			UpdateSize(oldw, oldh, newW, newH);
		}
		// if(m_bpp<=16) Interpolate2D(newW,newH);
		// else DownsampleRGB(newW,newH);
	}

	static Object lock = new Object();

	public void ConvertToPreview()
	{
		synchronized (lock)
		{
			if (m_bPreview)
				return;
			// System.out.print("Converting to preview");
			// Runtime.getRuntime().gc();
			double factor = Math.min(1.0, (double) PREVIEW_DIM
					/ (double) Math.max(m_Image.getWid(), m_Image.getHt()));
			try
			{

				Resample(factor);
			} catch (OutOfMemoryError e)
			{
				return;
			}
			m_bPreview = true;
			m_bNeedRedraw = true;
		}
	}

	public DicomImage (Component parent, SeriesElementRecord ser, boolean bReduceSize)
	throws IOException, InstantiationException
	{
		m_Parent = parent;		
		DICOMImageInfo dim=new DICOMImageInfo(ser.m_dob);
		dim.setImageReadParams();
		
		Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");			
		DicomImageReader r=(DicomImageReader)iter.next();
				
        Raster rr=null;
        ImageInputStream iis=null;
        
        boolean bSuccess=true;
        try {
            if(bReduceSize)
            {
            	rr=ImageIO.read(ser.m_file).getRaster();
            }
            else
            {
	        	iis=ImageIO.createImageInputStream(ser.m_file);        	
	        	r.setInput(iis);
	        	rr=r.readRaster(0, null);
            }
//        	DicomImageReader dir=DicomImageReader
//       	bi=ImageIO.rea read(ser.m_file);
        } 
        catch(Exception ioe)
        {
        	bSuccess=false;
        	try{iis.close();}catch(Exception e){}
        }
        if(!bSuccess) throw new IOException();
        int type=rr.getDataBuffer().getDataType(),imtype;
        
        //create pixel data.
        if(type==DataBuffer.TYPE_USHORT) //BufferedImage.TYPE_USHORT_GRAY)
        {
        	m_Image=new ShortImage(16,parent);
        	imtype=AbstractImage.SHORT;
        }
        else if(type==DataBuffer.TYPE_BYTE) //BufferedImage.TYPE_BYTE_GRAY)
        {
        	m_Image=new ByteImage((byte)8,parent);
        	imtype=AbstractImage.BYTE;
        }
        else if(type==DataBuffer.TYPE_INT) //BufferedImage.TYPE_INT_RGB)
        {
        	m_Image=new ColorImage(parent);
        	imtype=AbstractImage.RGB;
        }
        else throw new InstantiationException();
        
        try
        {
        	final int previewW=128, previewH=128;
        	
        	if(bReduceSize)
        		m_Image.createReducedCopy(rr, -1, -1, previewW, previewH, imtype);
        	else
        		m_Image.CreateImage(rr,-1,-1,imtype);
        }
        catch(Exception e)
        {
        	bSuccess=false;
        }
        if(!bSuccess) throw new InstantiationException();
        
        //now init additional metadata.
        LightXML xml=new DICOMImageInfo(ser.m_dob).getImageInfo();
        
		m_Info[0] = xml.GetStringValue("ii1");
		m_Info[1] = xml.GetStringValue("ii2");
		m_Info[2] = xml.GetStringValue("ii3");
		m_Info[3] = xml.GetStringValue("ii4");
		m_SideStr[0] = xml.GetStringValue("ss1");
		m_SideStr[1] = xml.GetStringValue("ss2");
		m_SideStr[2] = xml.GetStringValue("ss3");
		m_SideStr[3] = xml.GetStringValue("ss4");
                                
		m_currentZoom = 1;
		m_bCrosslink = false;
		
		scrMap.m_rImage.setBounds(0, 0, getWid(), getHt());
		scrMap.m_rScreen.setBounds(0, 0, getWid(), getHt());		
	}
	
// Image came from remote server, so no DICOM object is available.
	public DicomImage(Component parent, Object pic, LightXML xml,
			Rectangle rScr, Series par, byte bpp) throws OutOfMemoryError
	{
		m_Parent = parent;
		double[] cl = new double[9];
		double pdx = 0, pdy = 0;
		m_Hu[0] = 0;
		m_Hu[1] = 0;
		try
		{
			cl[0] = java.lang.Double.valueOf(xml.GetStringValue("lt1"))
					.doubleValue();
			cl[1] = java.lang.Double.valueOf(xml.GetStringValue("lt2"))
					.doubleValue();
			cl[2] = java.lang.Double.valueOf(xml.GetStringValue("lt3"))
					.doubleValue();
			cl[3] = java.lang.Double.valueOf(xml.GetStringValue("rt1"))
					.doubleValue();
			cl[4] = java.lang.Double.valueOf(xml.GetStringValue("rt2"))
					.doubleValue();
			cl[5] = java.lang.Double.valueOf(xml.GetStringValue("rt3"))
					.doubleValue();
			cl[6] = java.lang.Double.valueOf(xml.GetStringValue("lb1"))
					.doubleValue();
			cl[7] = java.lang.Double.valueOf(xml.GetStringValue("lb2"))
					.doubleValue();
			cl[8] = java.lang.Double.valueOf(xml.GetStringValue("lb3"))
					.doubleValue();

			pdx = java.lang.Double.valueOf(xml.GetStringValue("dx"))
					.doubleValue();
			pdy = java.lang.Double.valueOf(xml.GetStringValue("dy"))
					.doubleValue();
			m_Hu[0] = java.lang.Double.valueOf(xml.GetStringValue("Hu0"))
					.doubleValue();
			m_Hu[1] = java.lang.Double.valueOf(xml.GetStringValue("Hu1"))
					.doubleValue();
		} catch (NumberFormatException e)
		{
			System.out.println("Loc3");
		}
		m_PixelDX2 = pdx * pdx;
		m_PixelDY2 = pdy * pdy;

		int wid = (java.lang.Integer.valueOf(xml.GetStringValue("wid")))
				.intValue();
		int ht = (java.lang.Integer.valueOf(xml.GetStringValue("ht")))
				.intValue();

		m_Info[0] = xml.GetStringValue("ii1");
		m_Info[1] = xml.GetStringValue("ii2");
		m_Info[2] = xml.GetStringValue("ii3");
		m_Info[3] = xml.GetStringValue("ii4");
		m_SideStr[0] = xml.GetStringValue("ss1");
		m_SideStr[1] = xml.GetStringValue("ss2");
		m_SideStr[2] = xml.GetStringValue("ss3");
		m_SideStr[3] = xml.GetStringValue("ss4");
		if ((Math.abs(cl[0]) + Math.abs(cl[1]) + Math.abs(cl[2])
				+ Math.abs(cl[3]) + Math.abs(cl[4]) + Math.abs(cl[5])) == 0)
		{
			m_bCrosslink = false;
		} else
		{
			m_CrosslRects.add(new Point3D(cl[0], cl[1], cl[2]));
			m_CrosslRects.add(new Point3D(cl[3], cl[4], cl[5]));
			m_CrosslRects.add(new Point3D(cl[6], cl[7], cl[8]));
			m_bCrosslink = true;
		}
		m_currentZoom = 1;

		int quality = xml.GetIntValue("ql");
		int type = 0;

		if (bpp <= 8)
		{
			type |= AbstractImage.BYTE;
			if (quality < 100)
				type |= AbstractImage.JPEG_LOSSY;
			else
				type |= AbstractImage.ZIP;
			m_Image = new ByteImage(bpp, m_Parent);
		} else if (bpp <= 16)
		{
			type |= AbstractImage.SHORT;
			type |= AbstractImage.ZIP;
			if (quality < 100)
				type |= AbstractImage.JPEG_LOSSY;
			m_Image = new ShortImage(bpp, m_Parent);
		} else if (bpp == 24)
		{
			type |= AbstractImage.RGB | AbstractImage.JPEG_LOSSY;
			m_Image = new ColorImage(m_Parent);
		} else if (bpp == 32)
		{
			type |= AbstractImage.RGB | AbstractImage.ZIP;
			m_Image = new ColorImage(m_Parent);
		}
		m_Image.CreateImage(pic, wid, ht, type);
		scrMap.m_rImage.setBounds(0, 0, getWid(), getHt());
		scrMap.m_rScreen.setBounds(0, 0, getWid(), getHt());
	}

	// after initial rectangle is obtained,
	// calculate the crosslink transform

	private void CalcCLTransform()
	{
		if (!m_bCrosslink)
		{
			return;
		}
		m_CLTranslate.SetPoint3D(0, 0, 0);
		m_CLTranslate = m_CLTranslate.Minus(m_CrosslRects.get(0));
		double[] R = new double[9];
		ResetMatrix(R);
		ResetMatrix(m_CLTransf);
		Point3D pt1 = new Point3D(m_CrosslRects.get(1)), pt2 = new Point3D(
				m_CrosslRects.get(2));
		// translation to (0,0,0)
		pt1 = pt1.Plus(m_CLTranslate);
		pt2 = pt2.Plus(m_CLTranslate);
		// first rotation

		double nrm = Math.sqrt(pt1.x * pt1.x + pt1.z * pt1.z);
		double cs, sn;
		if (nrm > 0)
		{
			cs = pt1.x / nrm;
			sn = pt1.z / nrm;
			m_CLTransf[0] = cs;
			m_CLTransf[2] = sn;
			m_CLTransf[6] = -sn;
			m_CLTransf[8] = cs;
		}
		pt1 = pt1.MatrixTimesPt(m_CLTransf);
		pt2 = pt2.MatrixTimesPt(m_CLTransf);
		// second rotation
		nrm = Math.sqrt(pt1.x * pt1.x + pt1.y * pt1.y);
		if (nrm > 0)
		{
			cs = pt1.x / nrm;
			sn = pt1.y / nrm;
			R[0] = cs;
			R[1] = sn;
			R[3] = -sn;
			R[4] = cs;
		}
		pt1 = pt1.MatrixTimesPt(R);
		pt2 = pt2.MatrixTimesPt(R);
		m_CLTransf = MatrixTimesMatrix(R, m_CLTransf);
		// third rotation
		nrm = Math.sqrt(pt2.y * pt2.y + pt2.z * pt2.z);
		ResetMatrix(R);
		if (nrm > 0)
		{
			cs = pt2.y / nrm;
			sn = pt2.z / nrm;
			R[4] = cs;
			R[5] = sn;
			R[7] = -sn;
			R[8] = cs;
		}
		/* pt1=pt1.MatrixTimesPt(R); */
		pt2 = pt2.MatrixTimesPt(R);
		m_CLTransf = MatrixTimesMatrix(R, m_CLTransf);
		// scale
		ResetMatrix(R);
		if (pt1.x > 0)
		{
			R[0] = m_Image.getWid() / pt1.x;
		}
		if (pt2.y > 0)
		{
			R[4] = m_Image.getHt() / pt2.y;
		}
		m_CLTransf = MatrixTimesMatrix(R, m_CLTransf);
	}

	public void SetImageRect(DicomImage im)
	{
		int wid = getWid(), ht = getHt(), iwid = im.getWid(), iht = im.getHt();
		if (wid == iwid && ht == iht)
		{
			scrMap.m_rImage = new Rectangle(im.scrMap.m_rImage);
			return;
		}
		double zoom = (double) Math.max(wid, ht) / (double) Math.max(iwid, iht);
		Rectangle r = new Rectangle(im.scrMap.m_rImage);
		r.x = (int) (r.x * zoom + 0.5);
		r.y = (int) (r.y * zoom + 0.5);
		r.width = (int) (r.width * zoom + 0.5);
		r.height = (int) (r.height * zoom + 0.5);
		scrMap.m_rImage = r;
	}

	public int getWid()
	{
		return m_Image.getWid();
	}

	public int getHt()
	{
		return m_Image.getHt();
	}

	public boolean IsPreview()
	{
		return m_bPreview;
	}

	private void UpdateSize(int oldW, int oldH, int newW, int newH)
	{
		double kx = (double) newW / (double) oldW, ky = (double) newH
				/ (double) oldH;
		m_PixelDX2 /= (kx * kx);
		m_PixelDY2 /= (ky * ky);
		Rectangle r = new Rectangle(scrMap.m_rImage);
		scrMap.m_rImage.setBounds((int) (r.x * kx + 0.5),
				(int) (r.y * ky + 0.5), (int) (r.width * kx + 0.5),
				(int) (r.height * ky + 0.5));
		CalcCLTransform();
	}

	public Point3D GetCrossLinkedPt(Point3D src)
	{
		if (!m_bCrosslink)
		{
			return new Point3D(0, 0, 0);
		}
		return src.Plus(m_CLTranslate).MatrixTimesPt(m_CLTransf);
	}

	private boolean IntersectWithCrosslink(Point3D[] st_crl, Point P0, Point P1)
	{
		Point3D pt0, pt1, pt2;
		pt0 = GetCrossLinkedPt(st_crl[0]);
		pt1 = GetCrossLinkedPt(st_crl[1]);
		pt2 = GetCrossLinkedPt(st_crl[2]);
		Point3D normal = pt1.Minus(pt0).VectorProduct(pt2.Minus(pt0));
		double x1 = -1, x2 = -1, y1 = -1, y2 = -1, sp = pt0
				.ScalarProduct(normal);
		if (Math.abs(normal.x) > 0)
		{
			x1 = sp / normal.x; // y=0
			x2 = (sp - getHt() * normal.y) / normal.x; // y=m_ht
		}
		if (Math.abs(normal.y) > 0)
		{
			y1 = sp / normal.y; // x=0
			y2 = (sp - getWid() * normal.x) / normal.y; // x=m_wid
		}
		int nPts = 0;
		if (x1 >= 0 && x1 < getWid())
		{
			P0.x = (int) Math.round(x1);
			P0.y = 0;
			nPts = 1;
		}
		if (x2 >= 0 && x2 < getWid())
		{
			if (nPts == 0)
			{
				P0.x = (int) Math.round(x2);
				P0.y = getHt() - 1;
			} else
			{
				P1.x = (int) Math.round(x2);
				P1.y = getHt() - 1;
			}
			nPts++;
		}
		if (y1 >= 0 && y1 < getHt() && nPts < 2)
		{
			if (nPts == 0)
			{
				P0.x = 0;
				P0.y = (int) Math.round(y1);
			} else
			{
				P1.x = 0;
				P1.y = (int) Math.round(y1);
			}
			nPts++;
		}
		if (y2 >= 0 && y2 < getHt() && nPts < 2)
		{
			if (nPts == 0)
			{
				return false;
			} else
			{
				P1.x = getWid();
				P1.y = (int) Math.round(y2);
			}
			nPts++;
		}
		if (nPts < 2)
		{
			return false;
		}
		Point res0 = scrMap.WindowToScreen(P0);
		Point res1 = scrMap.WindowToScreen(P1);
		P0.setLocation(res0);
		P1.setLocation(res1);
		return true;
	}

	private void ResetMatrix(double[] m)
	{
		m[0] = 1;
		m[1] = 0;
		m[2] = 0;
		m[3] = 0;
		m[4] = 1;
		m[5] = 0;
		m[6] = 0;
		m[7] = 0;
		m[8] = 1;
	}

	private double[] MatrixTimesMatrix(double[] lft, double[] rt)
	{
		int i, j, k, ind, ind1;
		double[] res = new double[9];
		for (i = 0; i < 3; i++)
		{
			ind = i * 3;
			for (j = 0; j < 3; j++)
			{
				ind1 = ind + j;
				res[ind1] = 0;
				for (k = 0; k < 3; k++)
				{
					res[ind1] += lft[i * 3 + k] * rt[k * 3 + j];
				}
			}
		}
		return res;
	}

	public boolean IsHU()
	{
		return (m_Hu[0] != 0 || m_Hu[1] != 0);
	}

	public double[] GetWLColor(double[] huwinlev)
	{
		if (m_Hu[0] == 0)
		{
			return null;
		}
		double[] res = new double[2];
		res[0] = huwinlev[0] / m_Hu[0];
		res[1] = (huwinlev[1] - m_Hu[1]) / m_Hu[0];
		return res;
	}

	public int GetHU(double val)
	{
		return (int) (val * m_Hu[0] + m_Hu[1] + 0.5);
	}

	public String GetToolString(int ToolCode)
	{
		String temp_string = new String("");
		double fX = (m_PixelDX2 > 0) ? Math.sqrt(m_PixelDX2) : 1;
		double fY = (m_PixelDY2 > 0) ? Math.sqrt(m_PixelDY2) : 1;
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(1);
		nf.setGroupingUsed(false);
		String units = (m_PixelDX2 > 0 && m_PixelDY2 > 0) ? " "
				+ Util.Trans("mm") : " " + Util.Trans("pix") + ".";
		boolean bHu = IsHU();
		switch (ToolCode)
		{
			case Tool.WL :
				nf.setMaximumFractionDigits(0);
				Point wl = GetImage().GetWL();
				double i0 = bHu ? GetHU(wl.x) : wl.x,
				i1 = bHu ? GetHU(wl.y) : wl.y;

				double lev = (i0 + i1) / 2;
				double win = i1 - lev;
				temp_string = Util.Trans("Intensity range") + ": ["
						+ nf.format(wl.x) + " " + nf.format(wl.y) + "], "
						+ Util.Trans("win") + "=" + nf.format(win)
						+ (bHu ? " HU, " : ", ") + Util.Trans("lev") + "="
						+ nf.format(lev) + (bHu ? " HU" : "");

				return temp_string;
			case Tool.SIZE :
				return nf.format(RulerDistance()).replace(',', '.') + units;
			case Tool.ROI :
				Rectangle r = GetRect(m_RPoint0, m_RPoint1);
				r.setLocation(r.x + 1, r.y + 1);
				temp_string = Util.Trans("Size") + " "
						+ nf.format(r.width * fX).replace(',', '.') + "x"
						+ nf.format(r.height * fY).replace(',', '.') + units
						+ ", ";
				temp_string += Util.Trans("Area")
						+ " "
						+ nf.format(r.width * r.height * fX * fY).replace(',',
								'.') + " " + Util.Trans("sq") + "." + units;
				long[] stats = GetStatistics(r, false);
				if (stats == null)
				{
					return temp_string;
				}
				long min = stats[0],
				max = stats[1],
				m = stats[2],
				m2 = stats[3],
				cnt = stats[4];
				double d = Math.sqrt((double) m2 / (double) cnt
						- ((double) m / (double) cnt)
						* ((double) m / (double) cnt));
				if (!bHu)
				{
					temp_string += ", " + Util.Trans("Min") + " "
							+ nf.format(min) + ", " + Util.Trans("Max") + " "
							+ nf.format(max);
					temp_string += ", "
							+ Util.Trans("Average")
							+ " "
							+ nf.format((double) m / (double) cnt).replace(',',
									'.');
					temp_string += ", " + Util.Trans("Deviation") + " "
							+ nf.format(d).replace(',', '.');
				} else
				{
					temp_string += ", [" + nf.format(GetHU(min)) + " HU, "
							+ nf.format(GetHU(max)) + " HU], "
							+ Util.Trans("Average") + ": "
							+ nf.format(GetHU((double) m / (double) cnt))
							+ " HU";
				}
				return temp_string;
		}
		return temp_string;
	}

	/**
	 * ************************************************************************
	 * Get ROI statistics
	 * ************************************************************************
	 */
	public long[] GetStatistics(Rectangle roiRect, boolean bFilter)
	{
		Rectangle r;
		if (roiRect != null)
		{
			r = roiRect;
		} else
		{
			r = new Rectangle(getWid() - 1, getHt() - 1);
		}
		if (r.width < 1 || r.height < 1)
		{
			return null;
		}
		r.setLocation(r.x + 1, r.y + 1);

		// r=scrMap.ScreenToWindow(r);

		int infsup = m_Image.getMaxPixelValue() + 1;
		long m = 0, m2 = 0, cnt = 0;
		long tmp1, max = -infsup, min = infsup;
		long min1 = min, max1 = max;
		for (int i = r.y; i < r.y + r.height; i += Math.max(r.height / 100, 1))
		{
			for (int j = r.x; j < r.x + r.width; j += Math
					.max(r.width / 100, 1))
			{
				tmp1 = m_Image.GetPix(i, j);
				if (tmp1 > max1)
				{
					max = max1;
					max1 = tmp1;
				} else if (tmp1 > max && tmp1 < max1)
				{
					max = tmp1;
				}
				if (tmp1 < min1)
				{
					min = min1;
					min1 = tmp1;
				} else if (tmp1 < min && tmp1 > min1)
				{
					min = tmp1;
				}
				m += tmp1;
				m2 += tmp1 * tmp1;
				cnt++;
			}
		}

		if (min == infsup)
		{
			min = min1;
		}
		if (max == infsup)
		{
			max = max1;
		}
		long[] stats = new long[5];
		if (!bFilter)
		{
			stats[0] = min1;
			stats[1] = max1;
		} else
		{
			stats[0] = min;
			stats[1] = max;
		}
		stats[2] = m;
		stats[3] = m2;
		stats[4] = cnt;
		return stats;
	}

	public double RulerDistance()
	{
		return Math.sqrt((double) Dist2(m_RPoint0, m_RPoint1));
	}

	private double Dist2(Point p0, Point p1)
	{
		double res;
		if (m_PixelDX2 > 0 && m_PixelDY2 > 0)
		{
			return ((p0.x - p1.x) * (p0.x - p1.x) * m_PixelDX2 + (p0.y - p1.y)
					* (p0.y - p1.y) * m_PixelDY2);
		} else
		{
			res = ((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y)
					* (p0.y - p1.y));
			int i = m_iNumInterp;
			while (i > 0)
			{
				res /= 4.0;
				i--;
			}
			return res;
		}
	}

	public Vector GetToolClip(int ToolCode)
	{
		Rectangle r, temp_r;
		Vector v;
		switch (ToolCode)
		{
			case Tool.SIZE :
				r = GetRect(m_RScrPoint0, m_RScrPoint1);
				int nPartitions = 1; // Math.max(Math.min(r.width/30,r.height/30),1);
				v = new Vector(nPartitions);
				Point p0 = new Point(m_RScrPoint0);
				Point p1 = new Point();
				double stepX = (double) (m_RScrPoint1.x - m_RScrPoint0.x)
						/ (double) nPartitions;
				double stepY = (double) (m_RScrPoint1.y - m_RScrPoint0.y)
						/ (double) nPartitions;
				double dx = 0,
				dy = 0;
				for (int i = 0; i < nPartitions; i++)
				{
					dx += stepX;
					dy += stepY;
					if (i < nPartitions - 1)
					{
						p1.x = (int) (m_RScrPoint0.x + dx);
						p1.y = (int) (m_RScrPoint0.y + dy);
					} else
					{
						p1.x = m_RScrPoint1.x;
						p1.y = m_RScrPoint1.y;
					}
					temp_r = GetRect(p0, p1);
					temp_r.grow(4, 4);
					v.addElement(temp_r);
					p0.x = p1.x;
					p0.y = p1.y;
				}
				return v;
			case Tool.ROI :
				r = GetRect(m_RScrPoint0, m_RScrPoint1);
				v = new Vector(1);
				temp_r = new Rectangle(r.x, r.y, r.width, r.height);
				temp_r.grow(4, 4);
				v.addElement(temp_r);
				return v;
		}
		return null;
	}

	public Rectangle InitTool(Point pt, int ToolCode)
	{
		// Point ptmp=scrMap.ScreenToWindow(new
		// Point(pt.x-scrMap.m_rScreen.x, pt.y-scrMap.m_rScreen.y));
		Point ptmp = scrMap.ScreenToWindow(pt);
		Point pClip = m_OldRPoint1;
		Rectangle clip = new Rectangle(pClip.x - 2, pClip.y + 2, 4, 4);
		if (m_FirstTool)
		{
			m_RPoint0 = ptmp;
			m_RPoint1 = m_RPoint0;
			m_OldRPoint1 = pt;
			m_RScrPoint0 = pt;
			m_RScrPoint1 = pt;
			m_FirstTool = false;
		} else
		{
			double dist0 = Dist2(m_RPoint0, ptmp);
			double dist1 = Dist2(m_RPoint1, ptmp);
			if (dist0 < dist1)
			{
				m_RPoint0 = m_RPoint1;
				m_RPoint1 = ptmp;
				ptmp = m_RScrPoint0;
				m_RScrPoint0 = m_RScrPoint1;
				m_RScrPoint1 = ptmp;
			} else
			{
				m_RPoint1 = ptmp;
			}
			m_OldRPoint1 = m_RScrPoint1;
		}
		return clip;
	}

	public Rectangle PrepareTool(Point pt, int ToolCode)
	{
		Point topLeft = new Point(Math.min(m_OldRPoint1.x, m_RScrPoint0.x),
				Math.min(m_OldRPoint1.y, m_RScrPoint0.y));
		Rectangle clipRect = new Rectangle(topLeft.x, topLeft.y, Math
				.abs(m_OldRPoint1.x - m_RScrPoint0.x), Math.abs(m_OldRPoint1.y
				- m_RScrPoint0.y));
		clipRect.grow(4, 4);
		m_OldRPoint1 = m_RScrPoint1;
		m_RScrPoint1 = pt;
		m_RPoint1 = scrMap.ScreenToWindow(pt);
		return clipRect;
	}

	public void ResetTool()
	{
		m_FirstTool = true;
	}

	private void DrawDoubleLine(Graphics g, Point x0, Point x1)
	{
		if (g == null)
		{
			return;
		}
		double dx = Math.abs(x0.x - x1.x);
		double dy = Math.abs(x0.y - x1.y);
		double tg;
		if (dx > 0)
		{
			tg = dy / dx;
		} else
		{
			tg = 100;
		}
		if (tg < 1)
		{
			g.drawLine(x0.x, x0.y + 1, x1.x, x1.y + 1);
		} else
		{
			g.drawLine(x0.x + 1, x0.y, x1.x + 1, x1.y);
		}
	}

	private Rectangle GetRect(Point x1, Point x2)
	{
		Point x0 = new Point(x1);
		if (x0.x > x2.x)
		{
			x0.x = x2.x;
		}
		if (x0.y > x2.y)
		{
			x0.y = x2.y;
		}
		int wid = Math.abs(x2.x - x1.x);
		int ht = Math.abs(x2.y - x1.y);
		return new Rectangle(x0.x, x0.y, wid, ht);
	}

	public void DrawTool(Graphics g, int ToolCode)
	{
		g.setXORMode(Color.black);
		g.setColor(Color.white);
		switch (ToolCode)
		{
			case Tool.SIZE :
				g.drawLine(m_RScrPoint0.x, m_RScrPoint0.y, m_RScrPoint1.x,
						m_RScrPoint1.y);
				DrawDoubleLine(g, m_RScrPoint0, m_RScrPoint1);
				break;
			case Tool.ROI :
				Rectangle r = GetRect(m_RScrPoint0, m_RScrPoint1);
				g.drawRect(r.x, r.y, r.width, r.height);
				r.translate(1, 1);
				g.drawRect(r.x, r.y, r.width, r.height);
		}
		g.setPaintMode();
		g.setColor(Color.yellow);
		g.fillOval(m_RScrPoint0.x - 3, m_RScrPoint0.y - 3, 7, 7);
		g.fillOval(m_RScrPoint1.x - 3, m_RScrPoint1.y - 3, 7, 7);
		g.setColor(Color.black);
		g.fillOval(m_RScrPoint0.x - 1, m_RScrPoint0.y - 1, 3, 3);
		g.fillOval(m_RScrPoint1.x - 1, m_RScrPoint1.y - 1, 3, 3);
	}

	public void EraseTool(Graphics g, int ToolCode)
	{
		if (g == null)
		{
			return;
		}
		g.setXORMode(Color.black);
		g.setColor(Color.white);
		g.setPaintMode();
		m_OldRPoint1 = m_RScrPoint1;
	}

	public void ResetImage()
	{
		scrMap.m_rImage.setBounds(0, 0, getWid(), getHt());
		m_bNeedRedraw = true;
		/*
		 * if(m_ci == null) return; m_ci.SetDefaultLookupTable(); m_cm = null;
		 * m_DisplayingImage = null; System.gc(); InitColorModel(null, false);
		 */
	}

	/**
	 * ************************************************************************
	 * Get or set current screen output rectangle
	 * ************************************************************************
	 */
	public Rectangle GetScreenRect()
	{
		return scrMap.m_rScreen;
	}

	public boolean IsRGB()
	{
		return (m_Image instanceof ColorImage);
	}

	public void SetScreenRect(Rectangle r)
	{
		scrMap.m_rScreen = r;
	}

	public void UpdateScreenRect(Rectangle r)
	{
		scrMap.FitInsideScreenRect(r, (float) 1.0);
	}

	public boolean Contains(int x, int y, boolean isShrinked)
	{
		if (!isShrinked)
		{
			return scrMap.m_rScreen.contains(x, y);
		} else
		{
			Rectangle r = new Rectangle(scrMap.m_rScreen);
			r.grow(-3, -3);
			return r.contains(x, y);
		}
	}

	public double[] GetWLHU()
	{
		double[] res = new double[2];
		Point p = GetImage().GetWL();
		double i0 = GetHU(p.x);
		double i1 = GetHU(p.y);
		res[1] = (i0 + i1) / 2;
		res[0] = i1 - res[1];
		return res;
	}

	public double[] GetWLColor()
	{
		double[] res = new double[2];
		Point p = GetImage().GetWL();
		res[1] = (p.x + p.y) / 2;
		res[0] = p.y - res[1];
		return res;
	}

	public void SetWLColor(double[] wlColor) throws OutOfMemoryError
	{
		m_Image.SetWL(wlColor);
	}

	public void SetWLHU(double[] wlHU) throws OutOfMemoryError
	{
		double[] wlColor = GetWLColor(wlHU);
		m_Image.SetWL(wlColor);
	}

	public void WindowLevel(int shiftX, int shiftY, boolean bLight)
			throws OutOfMemoryError
	{
		GetImage().WindowLevel((double) shiftX, (double) shiftY,
				Math.max(scrMap.m_rScreen.width, scrMap.m_rScreen.height),
				bLight);
		m_bNeedRedraw = true;
	}

	/**
	 * ************************************************************************
	 * Shifts displayed part of zoomed image in the specified direction
	 * ************************************************************************
	 */
	public boolean ShiftImage(int shiftX, int shiftY)
	{
		boolean bRes = scrMap.ShiftImageRect(shiftX, shiftY);
		if (bRes)
			m_bNeedRedraw = true;
		return bRes;
	}

	public void UpdateZoomedImage() throws OutOfMemoryError
	{
		double zoom = (double) scrMap.m_rScreen.width
				/ (double) scrMap.m_rImage.width;
		int w = getWid(), h = getHt();
		if (zoom < 1.7 || m_iNumInterp > 0 || w + h > 1024
				|| (w <= PREVIEW_DIM && h <= PREVIEW_DIM))
			return;
		while (zoom > 2.0)
			zoom /= 2.0;
		Resample(zoom);
		m_iNumInterp++;
	}

	/**
	 * ************************************************************************
	 * Increases or decreases image zoom, calling resizing routines
	 * ************************************************************************
	 */
	public void ChangeZoom(boolean IsIncrease)
	{
		float ZoomStep, tmp;
		ZoomStep = (float) 0.05;
		if (IsIncrease)
		{
			tmp = m_currentZoom * (1 + ZoomStep);
			if (!scrMap.IsZoomValid(tmp, true))
			{
				return;
			}
			m_currentZoom = tmp;
			scrMap.ZoomImageRect(1 + ZoomStep);
		} else
		{
			tmp = m_currentZoom / (1 + ZoomStep);
			if (!scrMap.IsZoomValid(tmp, false))
			{
				return;
			}
			m_currentZoom = tmp;
			if (tmp < 1)
			{
				m_currentZoom = 1;
			}
			scrMap.ZoomImageRect(1 / (1 + ZoomStep));
		}
		m_bNeedRedraw = true;
	}

	public boolean FlipRotate(int how) throws OutOfMemoryError
	{
		int oldW = GetImage().getWid(), oldH = GetImage().getHt();
		GetImage().FlipRotate(how);

		int newW = GetImage().getWid(), newH = GetImage().getHt();

		if (newW != oldW || newH != oldH)
			UpdateSize(oldW, oldH, newW, newH);
		m_bNeedRedraw = true;
		return true;
	}

	private void DrawText(Graphics g, Color c, String text, Rectangle r,
			int align)
	{
		if (g == null)
		{
			return;
		}
		int len = text.length();
		if (len < 1)
		{
			return;
		}
		int occ = 0, occ0 = 0, i;
		FontMetrics fm = g.getFontMetrics();
		int x = r.x, y = r.y, desc = fm.getMaxDescent(), max_len = 0, ht = 0;
		int font_ht = fm.getHeight(), str_wid;
		String[] strings = new String[20];
		int[] lens = new int[20];
		for (i = 0; i < 20; i++)
		{
			if (i > 0)
			{
				occ0 = occ + 1;
			}
			occ = text.indexOf('\n', occ0);
			if (occ < 0)
			{
				occ = len;
			}
			strings[i] = text.substring(occ0, occ);
			lens[i] = fm.stringWidth(strings[i]);
			if (max_len < lens[i])
			{
				max_len = lens[i];
			}
			ht = ht + font_ht;
			if (occ == len)
			{
				break;
			}
		}
		switch (align)
		{
			case 0 : // topleft
				break;
			case 1 : // topright
				if (max_len < r.width)
				{
					x = r.x + (r.width - max_len);
				}
				break;
			case 3 : // bottom right
				if (max_len < r.width)
				{
					x = r.x + (r.width - max_len);
				}
			case 2 : // bottom left
				if (ht < r.height)
				{
					y = r.y + (r.height - ht);
				}
				break;

		}
		for (int j = 0; j <= i; j++)
		{
			g.setColor(Color.black);
			g.fillRect(x, y, lens[j], font_ht);
			g.setColor(c);
			g.drawString(strings[j], x, y + font_ht - desc);
			y = y + font_ht;
		}
	}

	public Rectangle GetRect(Graphics g, String s, int pos)
	{
		if (g == null)
		{
			return null;
		}
		Rectangle r = new Rectangle();
		FontMetrics fm = g.getFontMetrics();
		r.setBounds(0, 0, fm.stringWidth(s), fm.getHeight());
		int x = ((pos & RIGHT) != 0) ? 5 : scrMap.m_rScreen.width - r.width - 5, y = ((pos & TOP) != 0)
				? 5
				: scrMap.m_rScreen.height - r.height - 5;

		r.setLocation(scrMap.m_rScreen.x + x, scrMap.m_rScreen.y + y);
		r.grow(2, 2);
		return r.intersection(scrMap.m_rScreen);
	}

	private void DrawTextInRect(Graphics g, Color c, String s, int pos)
	{
		Rectangle r;
		if ((r = GetRect(g, s, pos)) == null)
			return;
		g.setColor(c);
		g.drawRect(r.x, r.y, r.width, r.height);
		r.grow(-2, -2);
		Shape clip = g.getClip();
		g.setClip(r);
		DrawText(g, c, s, r, 0);
		g.setClip(clip);
	}

	/**
	 * ************************************************************************
	 * Painting rectangle from image to rectangle from canvas, as well as
	 * borders and adequate frames.
	 * ************************************************************************
	 */
	public boolean paint(Graphics g, Shape clip, boolean bActive,
			boolean bFrameOnly, boolean bFlipRotated, boolean bShowCrosslink,
			boolean bShowInfo, boolean bPreview, Object crossLink)
			throws OutOfMemoryError
	{
		if (clip == null || m_Image.GetImage() == null || g == null)
			return false;
		g.setClip(clip);
		int x, y, wid, ht;
		UpdateZoomedImage();
		x = scrMap.m_rImage.x;
		y = scrMap.m_rImage.y;
		wid = scrMap.m_rImage.width;
		ht = scrMap.m_rImage.height;
		if (GetImage().IsLyte())
		{
			int lr = GetImage().GetLtRatio();
			x = Math.round(x / lr);
			y = Math.round(y / lr);
			wid = Math.round(wid / lr);
			ht = Math.round(ht / lr);
		}
		boolean bCrosslink = !bFlipRotated && bShowCrosslink && m_bCrosslink;
		if (!bFrameOnly || (bFrameOnly && bCrosslink) || m_bNeedRedraw)
		{
			if (bFrameOnly && bCrosslink)
			{
				g.setClip(m_OldCrosslinkRect);
			}
			boolean bRes;
			{
				bRes = g.drawImage(m_Image.GetImage(), scrMap.m_rScreen.x,
						scrMap.m_rScreen.y, scrMap.m_rScreen.x
								+ scrMap.m_rScreen.width, scrMap.m_rScreen.y
								+ scrMap.m_rScreen.height, x, y, x + wid, y
								+ ht, null);
				g.setClip(clip);
				// System.err.println("drawImage hit, bActive="+bActive+" bFrameOnly="+bFrameOnly+" bPreview="+bPreview);
				if (!bRes)
					return false;
				m_bNeedRedraw = false;
			}
		}
		if (bCrosslink)
		{
			Point3D[] st_crl = (Point3D[]) crossLink;
			if (st_crl != null)
			{
				Point pt0 = new Point(0, 0), pt1 = new Point(0, 0);
				if (IntersectWithCrosslink(st_crl, pt0, pt1))
				{
					m_OldCrosslinkRect = new Rectangle(pt0);
					m_OldCrosslinkRect.add(pt1);
					g.setColor(Color.red);
					g.setClip(scrMap.m_rScreen);
					g.drawLine(pt0.x, pt0.y, pt1.x, pt1.y);
					g.setClip(clip);
				}
			}
		}
		Rectangle r = new Rectangle();
		g.setFont(new Font("Dialog", Font.PLAIN, 10));
		Color c;
		if (bShowInfo)
		{
			// g.setFont(new Font("Dialog", Font.PLAIN, 10));
			c = Color.white;
			// g.setColor(Color.white);
			r = new Rectangle(0, 0, (2 * scrMap.m_rScreen.width) / 5,
					(2 * scrMap.m_rScreen.height) / 5);
			r.setLocation(scrMap.m_rScreen.x + 3, scrMap.m_rScreen.y + 3);
			g.setClip(r);
			if (m_Info[0] != null)
			{
				DrawText(g, c, m_Info[0], r, 0);
			}
			r.setLocation(scrMap.m_rScreen.x + scrMap.m_rScreen.width
					- (2 * scrMap.m_rScreen.width / 5), scrMap.m_rScreen.y + 3);
			g.setClip(r);
			if (m_Info[1] != null)
			{
				DrawText(g, c, m_Info[1], r, 1);
			}
			r.setLocation(scrMap.m_rScreen.x + 3, scrMap.m_rScreen.y
					+ scrMap.m_rScreen.height
					- (2 * scrMap.m_rScreen.height / 5));
			g.setClip(r);
			if (m_Info[2] != null)
			{
				DrawText(g, c, m_Info[2], r, 2);
			}
			r.setLocation(scrMap.m_rScreen.x + scrMap.m_rScreen.width
					- (2 * scrMap.m_rScreen.width / 5), scrMap.m_rScreen.y
					+ scrMap.m_rScreen.height
					- (2 * scrMap.m_rScreen.height / 5));
			g.setClip(r);
			if (m_Info[3] != null)
			{
				DrawText(g, c, m_Info[3], r, 3);
			}
			g.setClip(clip);
		}
		// draw side info & signs
		x = scrMap.m_rScreen.x;
		y = scrMap.m_rScreen.y;
		wid = scrMap.m_rScreen.width;
		ht = scrMap.m_rScreen.height;
		c = Color.green;
		if (m_SideStr[0] != null)
		{
			r.setBounds(x + 3, y + ht / 2 - 3, wid / 3, ht / 3);
			g.setClip(r);
			DrawText(g, c, m_SideStr[0], r, 0);
		}
		if (m_SideStr[1] != null)
		{
			r.setBounds(x + wid / 2 - 3, y + 3, wid / 3, ht / 3);
			g.setClip(r);
			DrawText(g, c, m_SideStr[1], r, 0);
		}
		if (m_SideStr[2] != null)
		{
			r.setBounds(x + wid - wid / 3, y + ht / 2 - 3, wid / 3 - 3, ht / 3);
			g.setClip(r);
			DrawText(g, c, m_SideStr[2], r, 1);
		}
		if (m_SideStr[3] != null)
		{
			r.setBounds(x + wid / 2 - 3, y + ht - ht / 3, wid / 3, ht / 3 - 3);
			g.setClip(r);
			DrawText(g, c, m_SideStr[3], r, 2);
			g.setClip(clip);
		}
		if (bPreview)
			DrawTextInRect(g, Color.green, "Load All", LEFT | BOTTOM);
		if (m_bPreview)
			DrawTextInRect(g, Color.red, "!", RIGHT | BOTTOM);
		if (!bActive)
		{
			g.setColor(Color.lightGray);
		} else
		{
			g.setColor(Color.green);
		}
		x = scrMap.m_rScreen.x;
		y = scrMap.m_rScreen.y;
		wid = scrMap.m_rScreen.width;
		ht = scrMap.m_rScreen.height;
		g.drawRect(x - 1, y - 1, wid + 1, ht + 1);
		g.setColor(Color.black);
		g.drawRect(x, y, wid - 1, ht - 1);
		m_bNeedRedraw = false;
		return true;
	}

	class Point3D
	{
		double x = 0, y = 0, z = 0;

		public boolean Serialize(Object stream, boolean is_loading)
				throws IOException
		{
			if (is_loading)
			{
				ObjectInputStream ois = (ObjectInputStream) stream;
				x = ois.readDouble();
				y = ois.readDouble();
				z = ois.readDouble();
			} else
			{
				ObjectOutputStream oos = (ObjectOutputStream) stream;
				oos.writeDouble(x);
				oos.writeDouble(y);
				oos.writeDouble(z);
			}
			return true;
		}

		public Point3D(double xx, double yy, double zz)
		{
			x = xx;
			y = yy;
			z = zz;
		}

		public Point3D(Point3D pt)
		{
			SetPoint3D(pt);
		}

		public void SetPoint3D(double xx, double yy, double zz)
		{
			x = xx;
			y = yy;
			z = zz;
		}

		public void SetPoint3D(Point3D pt)
		{
			x = pt.x;
			y = pt.y;
			z = pt.z;
		}

		public Point3D Minus(Point3D pt)
		{
			return new Point3D(x - pt.x, y - pt.y, z - pt.z);
		}

		public Point3D Plus(Point3D pt)
		{
			return new Point3D(x + pt.x, y + pt.y, z + pt.z);
		}

		public Point3D VectorProduct(Point3D pt)
		{
			return new Point3D(y * pt.z - z * pt.y, z * pt.x - x * pt.z, x
					* pt.y - y * pt.x);
		}

		public double ScalarProduct(Point3D pt)
		{
			return x * pt.x + y * pt.y + z * pt.z;
		}

		public Point3D MatrixTimesPt(double[] a)
		{
			return new Point3D(a[0] * x + a[1] * y + a[2] * z, a[3] * x + a[4]
					* y + a[5] * z, a[6] * x + a[7] * y + a[8] * z);
		}
	}

	public boolean SerializeTemp(boolean is_loading) throws OutOfMemoryError
	{
		try
		{
			boolean bRes;
			if (is_loading)
			{
				int oldW = getWid(), oldH = getHt();
				if (m_Image.SerializeTemp(is_loading))
				{
					m_bNeedRedraw = true;
					m_bPreview = false;
					UpdateSize(oldW, oldH, getWid(), getHt());
					return true;
				}
				return false;
			} else
			{
				return m_Image.SerializeTemp(is_loading);
			}
		} catch (FileNotFoundException e)
		{
			return false;
		} catch (IOException e)
		{
			return false;
		}
	}
}