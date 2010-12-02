package org.nrg.xnd.tools.ImageViewer.ip;

import java.awt.Component;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.nrg.xnd.utils.Utils;

public class ShortImage extends AbstractImage
{

	private char[] m_pix;
	private char[] m_ltpix;

	public ShortImage(int bpp, Component par)
	{
		super(bpp, par);
	}
	public int[] getMaxMinStat(){return m_maxmin;}
	
	@Override
	protected int[] getPixelMaxMin()
	{
		int min = 65535, max = 0;
		for (int i = 0; i < m_pix.length; i += 5)
		{
			if (min > m_pix[i])
				min = m_pix[i];
			if (max < m_pix[i])
				max = m_pix[i];
		}
		int[] res = {min, max};
		return res;
	}

	@Override
	protected void UpdateLtPixels() throws OutOfMemoryError
	{
		int lwid = getLtWid(), lht = getLtHt();
		int sz = getLtWid() * getLtHt(); // (((m_wid ) * (m_ht ))/ (m_ltRatio *
											// m_ltRatio));
		m_ltpix = new char[sz];
		int tmp, tmp1, ind;

		// for (int i1=0; i1 < lht; i1++)
		for (int i = 0, i1 = 0; i < m_ht; i += m_ltRatio, i1++)
		{
			// tmp = i * (m_wid);
			// tmp1 = (i / m_ltRatio) * ((m_wid ) / m_ltRatio);
			// for (int j1=0; j1 < lwid; j1++)
			for (int j = 0, j1 = 0; j < m_wid; j += m_ltRatio, j1++)
			{
				// ind = i1*lwid+j1; //tmp1 + j / m_ltRatio;
				// ind = ((i*lht*lwid)/m_ht)+(j*lwid)/m_wid;
				ind = i1 * lwid + j1;
				if (ind < sz)
				{
					m_ltpix[ind] = m_pix[i * m_wid + j];
					// m_ltpix[ind] = m_pix[(i1*m_wid*m_ht)/lht +
					// (j1*m_wid)/lwid];
				}
			}
		}
	}

	@Override
	public boolean FlipRotate(int how) throws OutOfMemoryError
	{
		char[] newBytes = m_pix;
		int ind, ind1;
		char temp;
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
			char[] rotatedBytes;
			rotatedBytes = new char[newBytes.length];
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
		return (long) (m_pix[m_wid * y + x] & 0xffff);
	}

	@Override
	protected void InitColorModel()
	{
		byte[] ci = m_ci.GetLookupTable();
		m_cm = new IndexColorModel(m_bpp, getMaxPixelValue()/*m_ci.GetMaxPixelValue() + 1*/, ci, ci,
				ci);
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
				m_pix = (char[]) Utils.SerializeArray(ois, new char[0],
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
	protected void UpdateImageFromBuffer(boolean bLyte) throws OutOfMemoryError
	{
		m_bLight = bLyte;
		if (bLyte && m_ltpix == null)
			UpdateLtPixels();
		byte[] pix8 = create8BitImage();
		byte[] ci = new byte[256];
		for (int i = 0; i < 256; i++)
			ci[i] = (byte) i;

		IndexColorModel cm = new IndexColorModel(m_bpp, 256, ci, ci, ci);
		MemoryImageSource mis = new MemoryImageSource(getWid(), getHt(), cm,
				pix8, 0, getWid());
		// new MemoryImageSource(m_wid,m_ht,m_cm, pix8, 0, m_wid);
		// m_Image = Toolkit.getDefaultToolkit().createImage(mis);
		m_Image = m_Parent.createImage(mis);
	}

	byte[] create8BitImage()
	{
		// int size = getWid()*getHt();

		// if (pixels8==null)
		// pixels8 = new byte[size];
		char[] src = m_bLight ? m_ltpix : m_pix;
		int size = src.length;
		byte[] pixels8 = new byte[size];
		int value;
		byte[] lt = m_ci.GetLookupTable();
		int min = 0, max = m_ci.GetMaxPixelValue();
		// double scale = 256.0/(max-min+1);
		int ind;
		for (int i = 0; i < size; i++)
		{
			ind = src[i];
			if (ind < 0)
				ind = 0;
			if (ind > lt.length)
				ind = lt.length - 1;
			// value = (m_pix[i]&0xffff)-min;
			// if (value<0) value = 0;
			// value = (int)(value*scale+0.5);
			// if (value>255) value = 255;
			value = (int) (lt[ind] & 0xff);
			if (src[i] > max)
				value = 255;
			// if(value>255) value=255;
			pixels8[i] = (byte) value;// (byte)value;
		}
		return pixels8;
	}

	@Override
	public boolean Interpolate(int W, int H) throws OutOfMemoryError
	{

		int[] clut = m_interp.getClut();

		final int sShift = 7, aShift = 10, aShiftD = 2;
		final int aShiftP = aShift + aShiftD, aShiftM = aShift - aShiftD;
		final int sV = 1 << sShift, aV = 1 << aShift;
		final int sV2 = 2 * sV;

		boolean InterpolateFast = false; // always apply
		// cubic
		// interpolation
		// Validate input
		// ??if(pSrcBuffer==0 || pDesBuffer==0) return;
		int SrcX0 = 0, SrcX1 = m_wid - 1, SrcY0 = 0, SrcY1 = m_ht - 1, SrcW = m_wid, SrcH = m_ht;
		if (W < m_wid && H < m_ht)
			InterpolateFast = true;
		// int SrcW0 = SrcX1 - SrcX0, SrcH0 = SrcY1 - SrcY0;

		// Initialize interpolation parameters
		boolean jOut;
		int tx, ty, i, j, iPrev, rx, ry, x, y, k, tp0, tp1;

		int DesW = W;// Math.max(W, H);
		int DesH = H;// DesW;
		// int SrcWs = SrcW0 << sShift, DesWs = DesW << sShift;
		// int SrcHs = SrcH0 << sShift, DesHs = DesH << sShift;
		int w0, w1, w2, w3, iMax = SrcW - 2, jMax = SrcH - 2;
		int dq, dqMax = 1 << aShiftD, pixel;
		int pS = 0, pD = 0, pSline = 0, pDline = 0, pPixel = 0;
		int[] p = new int[4];
		int[] q = new int[4];

		// Set binary interpolation scaling
		int ShiftXY = Math.max(sShift + 1, 14);
		int Mx = (SrcW << ShiftXY) / DesW, My = (SrcH << ShiftXY) / DesH;
		int dS = ShiftXY - sShift;

		char[] pSrcBuffer = m_pix;
		char[] pDesBuffer = new char[DesW * DesH];
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
					tp0 = (pSrcBuffer[pPixel] * (sV - rx) + pSrcBuffer[pPixel + 1]
							* rx) >> sShift;
					pPixel += SrcW;
					tp1 = (pSrcBuffer[pPixel] * (sV - rx) + pSrcBuffer[pPixel + 1]
							* rx) >> sShift;
					pDesBuffer[pDline + x] = (char) ((tp0 * (sV - ry) + tp1
							* ry) >> sShift);
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
					q[3] = (pSrcBuffer[p[0]] * w0 + pSrcBuffer[p[1]] * w1
							+ pSrcBuffer[p[2]] * w2 + pSrcBuffer[p[3]] * w3) >> aShiftM;
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
						q[k] = (pSrcBuffer[p[0]] * w0 + pSrcBuffer[p[1]] * w1
								+ pSrcBuffer[p[2]] * w2 + pSrcBuffer[p[3]] * w3) >> aShiftM;
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
					pixel = q[1] >> aShiftD;
				else
				{
					pixel = (q[0] * clut[sV + rx] + q[1] * clut[rx] + q[2]
							* clut[sV - rx] + q[3] * clut[sV2 - rx]) >> aShiftP;
				}
				if (pixel < 0)
					pDesBuffer[pPixel] = 0;
				else if (pixel > 65535)
					pDesBuffer[pPixel] = (char) 65535;
				else
					pDesBuffer[pPixel] = (char) pixel;
			} // next x
		} // next y
		m_pix = pDesBuffer;
		m_wid = DesW;
		m_ht = DesH;
		UpdateImageFromBuffer(false);
		return true;
	}

	@Override
	public void CreateImage(Object pic, int wid, int ht, int code)
			throws OutOfMemoryError 
	{
		if(pic instanceof Raster)
		{			
			m_type=code;
			Raster r = (Raster) pic;
			m_wid=r.getWidth();
			m_ht=r.getHeight();
			m_type=AbstractImage.SHORT;
			int tmp;
			m_pix = new char[m_wid * m_ht];
			for (int i = 0; i < m_ht; i++)
			{
				tmp = i * (m_wid);
				for (int j = 0, j1 = 0; j < m_wid; j++, j1 += 2)
				{	
					m_pix[tmp + j] = (char)r.getSample(j,i,0);
				}
			}
		}
		else
		{
			m_type = code;
			m_wid = wid;
			m_ht = ht;
			m_pix = new char[m_wid * m_ht];
			byte[] HiByte = (byte[]) (((Vector) pic).elementAt(0)), LoByte = (byte[]) (((Vector) pic)
					.elementAt(1));
			int tmp, tmp1;
			for (int i = 0; i < m_ht; i++)
			{
				tmp = i * (m_wid);
				tmp1 = i * m_wid;
				for (int j = 0, j1 = 0; j < m_wid; j++, j1 += 2)
				{
					m_pix[tmp + j] = (char) ((char) (LoByte[tmp1 + j] & 0xff) + ((char) (HiByte[tmp1
							+ j] & 0xff) << 8));
				}
			}
		}
		m_maxmin = getPixelMaxMin();
		m_ci = new SlidingColorIndex();
		m_ci.SetLookupTable(m_maxmin[0], m_maxmin[1]);
		InitColorModel();
//		UpdateImageFromBuffer(false);
	}
	
	@Override
	public boolean InMemory()
	{
		return m_pix != null;
	}

	@Override
	public void UnloadFromMemory()
	{
		m_pix = null;
		m_Image = null;
		m_bufImage = null;
	}

	@Override
	public void createReducedCopy(Object pic, int origW, int origH, int maxW,
			int maxH, int code)
	{
		// TODO Auto-generated method stub
		
	}
}
