package org.nrg.xnd.tools.ImageViewer.ip;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.nrg.xnd.utils.Utils;

public class ByteImage extends AbstractImage
{
	private byte[] m_pix;
	private byte[] m_ltpix = null;

	@Override
	protected void UpdateLtPixels() throws OutOfMemoryError
	{
		int sz = (((m_wid) * (m_ht)) / (m_ltRatio * m_ltRatio));
		m_ltpix = new byte[sz];
		int tmp, tmp1, ind;
		for (int i = 0; i < m_ht; i += m_ltRatio)
		{
			tmp = i * (m_wid);
			tmp1 = (i / m_ltRatio) * ((m_wid) / m_ltRatio);
			for (int j = 0; j < m_wid; j += m_ltRatio)
			{
				ind = tmp1 + j / m_ltRatio;
				if (ind < sz)
				{
					m_ltpix[ind] = m_pix[tmp + j];
				}
			}
		}
	}

	public ByteImage(byte bpp, Component parent)
	{
		super(bpp, parent);
	}

	@Override
	protected int[] getPixelMaxMin()
	{
		int min = 255, max = 0;
		int val;
		for (int i = 0; i < m_pix.length; i += 5)
		{
			val = (int) (m_pix[i] & 0xff);
			if (min > val)
				min = val;
			if (max < val)
				max = val;
		}
		int[] res = {min, max};
		return res;
	}

	@Override
	public boolean Serialize(Object stream, boolean is_loading)
			throws OutOfMemoryError
	{
		if (is_loading)
		{
			try
			{
				ObjectInputStream ois = new ObjectInputStream(
						new BufferedInputStream((InputStream) stream));
				int wid = ois.readInt();
				int ht = ois.readInt();

				// source pixels
				m_pix = (byte[]) Utils.SerializeArray(ois, new byte[0],
						is_loading);
				ois.close();
				m_wid = wid;
				m_ht = ht;
				UpdateImageFromBuffer(false);
				return true;
			} catch (IOException e)
			{
				return false;
			} catch (SecurityException e)
			{
				return false;
			}
		} else
		// !is_loading
		{
			try
			{
				ObjectOutputStream oos = new ObjectOutputStream(
						new BufferedOutputStream((OutputStream) stream));
				// size info
				oos.writeInt(getWid());
				oos.writeInt(getHt());
				Utils.SerializeArray(oos, m_pix, is_loading);
				oos.flush();
				oos.close();
				return true;
			} catch (IOException e)
			{
				return false;
			} catch (SecurityException e)
			{
				return false;
			}
		}
	}

	@Override
	public boolean FlipRotate(int how) throws OutOfMemoryError
	{
		byte[] newBytes = m_pix;
		int ind, ind1;
		byte temp;
		if (how == RightLeft)
		{
			for (int i = 0; i < getHt(); i++)
			{
				ind = i * (getWid());
				for (int j = 0; j < getWid() / 2; j++)
				{
					temp = newBytes[ind + j];
					newBytes[ind + j] = newBytes[ind + getWid() - j - 1];
					newBytes[ind + getWid() - j - 1] = temp;
				}
			}
		} else if (how == TopBottom)
		{
			for (int i = 0; i < getHt() / 2; i++)
			{
				ind = i * (getWid());
				ind1 = (getHt() - i - 1) * (getWid());
				for (int j = 0; j < getWid(); j++)
				{
					temp = newBytes[ind + j];
					newBytes[ind + j] = newBytes[ind1 + j];
					newBytes[ind1 + j] = temp;
				}
			}
		} else
		// rotation
		{
			byte[] rotatedBytes;
			rotatedBytes = new byte[newBytes.length];
			int tmp1, tmp2, tmp3;
			if (how == Rotate90)
			{
				tmp2 = getHt() - 1;
				tmp3 = getWid();
				for (int j = 0; j < getWid(); j++)
				{
					tmp1 = (getHt()) * j;
					for (int i = 0; i < getHt(); i++)
					{
						rotatedBytes[tmp1 + i] = newBytes[(tmp2 - i) * tmp3 + j];
					}
				}
				SwapWH();
				newBytes = rotatedBytes;
			} else if (how == Rotate180)
			{
				for (int i = 0; i < getHt(); i++)
				{
					tmp1 = i * (getWid());
					tmp2 = (getHt() - i - 1) * (getWid());
					tmp3 = getWid() - 1;
					for (int j = 0; j < getWid(); j++)
					{
						rotatedBytes[tmp1 + j] = newBytes[tmp2 + (tmp3 - j)];
					}
				}
				newBytes = rotatedBytes;
			} else if (how == Rotate270)
			{
				for (int j = 0; j < getWid(); j++)
				{
					tmp1 = j * (getHt());
					tmp2 = getWid() - j - 1;
					tmp3 = getWid();
					for (int i = 0; i < getHt(); i++)
					{
						rotatedBytes[tmp1 + i] = newBytes[i * tmp3 + tmp2];
					}
				}
				SwapWH();
				newBytes = rotatedBytes;
			}
			m_pix = newBytes;
		}
		UpdateLtPixels();
		UpdateImageFromBuffer(false);
		return true;
	}

	@Override
	public long GetPix(int x, int y)
	{
		return (long) (m_pix[m_wid * y + x] & 0xff);
	}

	@Override
	protected void InitColorModel()
	{
		byte[] ci = m_ci.GetLookupTable();
		m_cm = new IndexColorModel(m_bpp, getMaxPixelValue()+1/*m_ci.GetMaxPixelValue() + 1*/, ci, ci,
				ci);
	}

	@Override
	public boolean InMemory()
	{
		return m_pix != null;
	}

	@Override
	public boolean Interpolate(int W, int H) throws OutOfMemoryError
	{
		int[] clut = m_interp.getClut();

		final int sShift = 7, aShift = 10, aShiftD = 2;
		final int aShiftP = aShift + aShiftD, aShiftM = aShift - aShiftD;
		final int sV = 1 << sShift, aV = 1 << aShift;
		final int sV2 = 2 * sV;

		final boolean InterpolateFast = false; // always apply

		int SrcX0 = 0, SrcX1 = m_wid - 1, SrcY0 = 0, SrcY1 = m_ht - 1, SrcW = m_wid, SrcH = m_ht;

		// Initialize interpolation parameters
		boolean jOut;
		int tx, ty, i, j, iPrev, rx, ry, x, y, k, tp0, tp1;

		int DesW = W;// Math.max(W, H);
		int DesH = H;// DesW;
		int w0, w1, w2, w3, iMax = SrcW - 2, jMax = SrcH - 2;
		int dq, dqMax = 1 << aShiftD, pixel;
		int pS = 0, pD = 0, pSline = 0, pDline = 0, pPixel = 0;
		int[] p = new int[4];
		int[] q = new int[4];

		// Set binary interpolation scaling
		int ShiftXY = Math.max(sShift + 1, 14);
		int Mx = (SrcW << ShiftXY) / DesW, My = (SrcH << ShiftXY) / DesH;
		int dS = ShiftXY - sShift;

		byte[] pSrcBuffer = m_pix;
		byte[] pDesBuffer = new byte[DesW * DesH];
		int res;
		for (y = 0, ty = 0; y < DesH; y++, ty += My)
		{
			j = ty >> ShiftXY;
			ry = (ty - (j << ShiftXY)) >> dS;
			j += SrcY0;
			if (j > jMax)
			{
				j = jMax;
				ry = sV;
			}
			pSline = pS + j * SrcW;
			pDline = pD + y * DesW;
			w0 = clut[sV + ry];
			w1 = clut[ry];
			w2 = clut[sV - ry];
			w3 = clut[sV2 - ry];
			jOut = j < 1 || j >= jMax || InterpolateFast;
			iPrev = -100;
			dq = 1000000;

			for (x = 0, tx = 0; x < DesW; x++, tx += Mx)
			{
				i = tx >> ShiftXY;
				rx = (tx - (i << ShiftXY)) >> dS;
				i += SrcX0;
				if (i > iMax)
				{
					i = iMax;
					rx = sV;
				}
				if (jOut || i < 1 || i >= iMax) // linear
				// interpolation
				{
					pPixel = pSline + i;
					tp0 = ((pSrcBuffer[pPixel] & 0xff) * (sV - rx) + (pSrcBuffer[pPixel + 1] & 0xff)
							* rx) >> sShift;
					pPixel += SrcW;
					tp1 = ((pSrcBuffer[pPixel] & 0xff) * (sV - rx) + (pSrcBuffer[pPixel + 1] & 0xff)
							* rx) >> sShift;
					res = (tp0 * (sV - ry) + tp1 * ry) >> sShift;
					pDesBuffer[pDline + x] = (byte) Math.max(0, Math.min(res,
							255));
					continue;
				}

				// Compute interpolation in y-direction
				if (i == iPrev)
				{
					; // We can use old q-values
				} else if (i == iPrev + 1) // Step to the right
				{
					q[0] = q[1];
					q[1] = q[2];
					q[2] = q[3];
					p[1] = pSline + i + 2;
					p[0] = p[1] - SrcW;
					p[2] = p[1] + SrcW;
					p[3] = p[2] + SrcW;
					q[3] = ((pSrcBuffer[p[0]] & 0xff) * w0
							+ (pSrcBuffer[p[1]] & 0xff) * w1
							+ (pSrcBuffer[p[2]] & 0xff) * w2 + (pSrcBuffer[p[3]] & 0xff)
							* w3) >> aShiftM;
					dq = q[1] - q[2];
					if (dq < 0)
					{
						dq = -dq;
					}
				} else
				// Compute everything from scratch
				{
					p[1] = pSline + i - 1;
					p[0] = p[1] - SrcW;
					p[2] = p[1] + SrcW;
					p[3] = p[2] + SrcW;
					for (k = 0; k < 4; k++)
					{
						q[k] = ((pSrcBuffer[p[0]] & 0xff) * w0
								+ (pSrcBuffer[p[1]] & 0xff) * w1
								+ (pSrcBuffer[p[2]] & 0xff) * w2 + (pSrcBuffer[p[3]] & 0xff)
								* w3) >> aShiftM;
						p[0]++;
						p[1]++;
						p[2]++;
						p[3]++;
					}
					dq = q[1] - q[2];
					if (dq < 0)
						dq = -dq;
				}
				iPrev = i;

				// Compute interpolation in x-direction
				pPixel = pDline + x;
				if (dq < dqMax)
				{
					pixel = q[1] >> aShiftD;
				} else
				{
					pixel = (q[0] * clut[sV + rx] + q[1] * clut[rx] + q[2]
							* clut[sV - rx] + q[3] * clut[sV2 - rx]) >> aShiftP;
				}
				pDesBuffer[pPixel] = (byte) Math.max(0, Math.min(pixel, 255));
			} // next x
		} // next y
		m_pix = pDesBuffer;
		m_wid = DesW;
		m_ht = DesH;
		UpdateImageFromBuffer(false);
		return true;
	}

	@Override
	public void UnloadFromMemory()
	{
		m_pix = null;
		m_Image = null;
		m_bufImage = null;
	}
	@Override
	public void createReducedCopy(Object px, int origW, int origH, int maxW,
			int maxH, int code)
	{
		m_type=AbstractImage.BYTE;		
		if(px instanceof Raster)
		{
			Raster rast=(Raster)px;
			if(origW<1) origW=rast.getWidth();
			if(origH<1) origH=rast.getHeight();
			
			double rW=(double)maxW/(double)origW,
				   rH=(double)maxH/(double)origH;
			if(rW>=1 && rH >=1)
			{
				CreateImage(px,origW,origH,code);
				return;
			}
			double r=Math.min(rW,rH), ri=1.0/r;
			int newW=(int)(r*origW), newH=(int)(r*origH);
			m_wid=newW-(newW%4); 
			m_ht=newH-(newH%4);
			
			int tmp;
			m_pix = new byte[m_wid * m_ht];
			int x,y;
			double rix=(double)origW/(double)m_wid, riy=(double)origH/(double)m_ht;
			
			for (int i = 0; i < m_ht; i++)
			{
				y=Math.min(origH,(int)(i*riy+0.5));
				for (int j = 0; j < m_wid; j++)
				{
					x=Math.min(origW,(int)(j*rix+0.5));
					m_pix[m_wid*i + j] = (byte)(((byte)rast.getSample(x,y,0)) & 0xff);
				}
			}
		}
		else return;
		m_maxmin = getPixelMaxMin();
		m_ci = new SlidingColorIndex();
		InitColorModel();
		UpdateImageFromBuffer(false);
	}
	@Override
	public void CreateImage(Object px, int wid, int ht, int code)
			throws OutOfMemoryError
	{
		m_type = code;
		if(px instanceof Raster)
		{
			Raster r = (Raster)px ;
			m_wid=r.getWidth();
			m_ht=r.getHeight();
			m_type=AbstractImage.BYTE;
			int tmp;
			m_pix = new byte[m_wid * m_ht];
			for (int i = 0; i < m_ht; i++)
			{
				tmp = i * (m_wid);
				for (int j = 0; j < m_wid; j++)
				{	
					m_pix[tmp + j] = (byte)(((byte)r.getSample(j,i,0)) & 0xff);
				}
			}
			
		}
		else
		{
			m_wid = wid;
			m_ht = ht;
			// zip-compressed buffer.
			if ((m_type & ZIP) != 0)
			{
				try
				{
					m_pix = Utils.UnzipBuf((byte[]) px);
				} catch (IOException e)
				{
					System.err.println("Exception while unzipping pixel buffer");
					e.printStackTrace();
					return;
				}
	
			}
			// we have lossy JPEG compressed buffer
			else if ((m_type & AbstractImage.JPEG_LOSSY) != 0
					&& (px instanceof Image))
			{
				PixelGrabber pg = new PixelGrabber((Image) px, 0, 0, m_wid, m_ht,
						false);
				try
				{
					pg.grabPixels();
				} catch (InterruptedException e)
				{
					System.err.println("Exception when reading pixel buffer.");
					e.printStackTrace();
					return;
				}
				m_pix = (byte[]) pg.getPixels();
			}
		}		
		m_maxmin = getPixelMaxMin();
		m_ci = new SlidingColorIndex();
		InitColorModel();
		UpdateImageFromBuffer(false);
	}

	protected void UpdateImageFromBuffer(boolean bLight)
			throws OutOfMemoryError
	{
		// DataBuffer db=new DataBufferByte(m_pix,m_pix.length);
		if (!bLight)
		{
			MemoryImageSource mis = new MemoryImageSource(m_wid, m_ht, m_cm,
					m_pix, 0, m_wid);
			m_Image = m_Parent.createImage(mis);
			// Toolkit.getDefaultToolkit().createImage(mis);
			m_bLight = false;
		} else
		{
			if (m_ltpix == null)
				UpdateLtPixels();
			MemoryImageSource mis = new MemoryImageSource(getWid(), getHt(),
					m_cm, m_ltpix, 0, getWid());
			m_Image = Toolkit.getDefaultToolkit().createImage(mis);
			m_bLight = true;
		}
		// m_Image = new BufferedImage(m_cm,
		// Raster.createPackedRaster(db, m_wid, m_ht, 8, new Point(0,0)),
		// false,null);
	}
}