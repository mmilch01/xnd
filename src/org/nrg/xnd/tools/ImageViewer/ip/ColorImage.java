package org.nrg.xnd.tools.ImageViewer.ip;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.nrg.xnd.utils.Utils;

public class ColorImage extends AbstractImage
{
	private int[] m_pix;
	private int[] m_ltpix;

	public ColorImage(Component par)
	{
		super(8, par);
	}

	@Override
	public boolean FlipRotate(int how) throws OutOfMemoryError
	{
		int[] newBytes = m_pix;
		int ind, ind1, temp;
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
			int[] rotatedBytes;
			rotatedBytes = new int[newBytes.length];
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

	private int GetGrayValue(int rgb)
	{
		return ((rgb & 0xff) + ((rgb & 0xff00) >> 8) + ((rgb & 0xff0000) >> 16)) / 3;
	}

	@Override
	public long GetPix(int x, int y)
	{
		return GetGrayValue(m_pix[m_wid * y + x]);
	}

	@Override
	protected int[] getPixelMaxMin()
	{
		int min = 256, max = 0;
		int val;
		for (int i = 0; i < m_pix.length; i += 5)
		{
			val = GetGrayValue(m_pix[i]);
			if (min > val)
				min = val;
			if (max < val)
				max = val;
		}
		int[] res = {min, max};
		return res;
	}

	private int[] GetWLPixels(int[] src) throws OutOfMemoryError
	{
		int[] newPixels = new int[src.length];
		byte[] lookup = m_ci.GetLookupTable();
		int oldPixel;
		int byte1, byte2, byte3;
		for (int i = 0; i < src.length; i++) // explicitly
		// tranform
		// source bytes
		{
			oldPixel = src[i];
			byte1 = (oldPixel & 0x0000ff);
			byte1 = lookup[byte1 & 0xff];
			byte2 = ((oldPixel & 0x00ff00) >> 8);
			byte2 = lookup[byte2 & 0xff];
			byte3 = ((oldPixel & 0xff0000) >> 16);
			byte3 = lookup[byte3 & 0xff];
			newPixels[i] = 0xff000000 | (byte1 & 0xff)
					| ((byte2 << 8) & 0xff00)
					| ((byte3 << 16) & 0xff0000);
		}
		return newPixels;
	}

	@Override
	protected void InitColorModel()
	{
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
				m_pix = (int[]) Utils.SerializeArray(ois, new int[0],
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
	protected void UpdateImageFromBuffer(boolean light) throws OutOfMemoryError
	{
		// if(!light)
		{
			MemoryImageSource mis = new MemoryImageSource(m_wid, m_ht,
					GetWLPixels(m_pix), 0, m_wid);
			m_Image = m_Parent.createImage(mis);
			m_bLight = false;
		}
		/*
		 * else { if(m_ltpix==null) UpdateLtPixels(); MemoryImageSource mis=new
		 * MemoryImageSource(getWid(),getHt(), GetWLPixels(m_ltpix), 0,
		 * getWid()); m_Image = m_Parent.createImage(mis); m_bLight=true; }
		 */
	}

	@Override
	protected void UpdateLtPixels() throws OutOfMemoryError
	{
		int sz = (((m_wid) * (m_ht)) / (m_ltRatio * m_ltRatio));
		m_ltpix = new int[sz];
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

	@Override
	public boolean Interpolate(int W, int H) throws OutOfMemoryError
	{
		if (W > m_wid || H > m_ht)
			return false;
		DownsampleRGB(W, H);
		m_wid = W;
		m_ht = H;
		UpdateImageFromBuffer(false);
		return true;
	}

	private void DownsampleRGB(int newW, int newH) throws OutOfMemoryError
	{
		int[] newSrc = new int[newW * newH];
		int oldX, oldY;
		double rX = (double) m_wid / (double) newW, rY = (double) m_ht
				/ (double) newH;
		int[] oldSrc = m_pix;
		for (int y = 0; y < newH; y++)
		{
			oldY = Math.min(m_ht - 1, (int) (y * rY + 0.5));
			for (int x = 0; x < newW; x++)
			{
				oldX = Math.min(m_wid - 1, (int) (x * rX + 0.5));
				newSrc[newW * y + x] = oldSrc[m_wid * oldY + oldX];
			}
		}
		m_pix = newSrc;
	}

	@Override
	public void CreateImage(Object pic, int wid, int ht, int code)
			throws OutOfMemoryError
	{
		m_type = code;
		m_wid = wid;
		m_ht = ht;
		// m_pix=(int[])pic;
		// m_Image = new BufferedImage(m_wid,m_ht,BufferedImage.TYPE_BYTE_GRAY);
		// zip-compressed buffer.
		int hdiff = m_ht + (m_ht % 16) > 0 ? (16 - m_ht % 16) : 0;

		if ((m_type & ZIP) != 0)
		{
			try
			{
				byte[] pixels = Utils.UnzipBuf((byte[]) pic);
				m_pix = new int[(m_wid) * (m_ht + hdiff)];
				int start_pix = 0xff000000 | (pixels[2] & 0xff)
						| ((pixels[1] << 8) & 0xff00)
						| ((pixels[0] << 16) & 0xff0000);

				int tmp, tmp1;
				for (int i = 0; i < m_ht + hdiff; i++)
				{
					tmp = i * (m_wid);
					tmp1 = i * 3 * m_wid;
					for (int j = 0; j < m_wid; j++)
					{
						if (i < m_ht)
							m_pix[tmp + j] = 0xff000000
									| (pixels[tmp1 + 3 * j + 2] & 0xff)
									| ((pixels[tmp1 + 3 * j + 1] << 8) & 0xff00)
									| ((pixels[tmp1 + 3 * j] << 16) & 0xff0000);
						else
							m_pix[tmp + j] = start_pix;
					}
				}
				m_ht += hdiff;
			} catch (IOException ex)
			{
				System.err.println("Exception when unzipping pixel buffer.");
				ex.printStackTrace();
				return;
			}
		}
		// we have lossy JPEG compressed buffer
		else if ((m_type & JPEG_LOSSY) != 0 && (pic instanceof Image))
		{
			PixelGrabber pg = new PixelGrabber((Image) pic, 0, 0, m_wid, m_ht,
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
			int[] src = (int[]) pg.getPixels();
			if (hdiff == 0)
			{
				m_pix = src;
				for (int i = 0; i < m_pix.length; i++)
					m_pix[i] = (m_pix[i] | 0xff000000);
			} else
			{
				m_pix = new int[m_wid * (m_ht + hdiff)];
				int tmp;
				int start_pix = 0xff000000 | (src[2] & 0xff)
						| ((src[1] << 8) & 0xff00)
						| ((src[0] << 16) & 0xff0000);
				for (int i = 0; i < m_ht + hdiff; i++)
				{
					tmp = i * (m_wid);
					for (int j = 0; j < m_wid; j++)
					{
						if (i < m_ht)
							m_pix[tmp + j] = 0xff000000 | src[tmp + j];
						else
							m_pix[tmp + j] = start_pix;
					}
				}
			}
		}
		m_maxmin = getPixelMaxMin();
		m_ci = new SlidingColorIndex();
		// InitColorModel();
		UpdateImageFromBuffer(false);
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
		m_ltpix = null;
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