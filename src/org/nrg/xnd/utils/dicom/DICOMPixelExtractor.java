package org.nrg.xnd.utils.dicom;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.imageio.plugins.dcm.DicomImageReadParam;
import org.nrg.fileserver.ItemRecord;
import org.nrg.xnd.tools.ImageViewer.PixelExtractor;
import org.nrg.xnd.tools.ImageViewer.ip.AbstractImage;
import org.nrg.xnd.utils.LightXML;

public class DICOMPixelExtractor implements PixelExtractor
{
	private int m_ht, m_wid, m_nFrames, m_nPixels, m_BitsAllocated,
			m_BitsStored, m_HighBit, m_SamplesPerPixel, m_BytesPerSample,
			m_LitEndian = 1, m_maxVal = 0, m_nSamples = 0;
	private static final int MAX_DIM = 1024;

	private DicomObject m_dob;
	private String m_Photometric, m_ImgSOPInstUID;
	private byte[] m_srcPixels;
	private Object m_pixels;

	private final int GetID(int group, int element)
	{
		return (group << 16) & element;
	}
	@Override
	public int GetBpp()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public LightXML GetImageInfo()
	{
		LightXML xml = new LightXML();
		xml.AddValue("wid", m_wid);
		xml.AddValue("ht", m_ht);
		xml.AddValue("ii1", "");
		xml.AddValue("ii2", "");
		xml.AddValue("ii3", "");
		xml.AddValue("ii4", "");
		xml.AddValue("ss1", "");
		xml.AddValue("ss2", "");
		xml.AddValue("ss3", "");
		xml.AddValue("ss4", "");
		xml.AddValue("ql", 100);
		xml.AddValue("bpp", m_BitsAllocated);
		xml.AddValue("spp", m_SamplesPerPixel);
		xml.AddValue("pht", m_Photometric);
		xml.AddValue("end", m_LitEndian);
		xml.AddValue("maxpix", m_maxVal);
		xml.AddValue("ord", m_ImgSOPInstUID);
		return xml;
	}

	@Override
	public Object GetPixels()
	{
		return m_pixels;
	}
	@Override
	public boolean LoadImage(ItemRecord ir)
	{
		return false;
	}

	public AbstractImage LoadImage(DicomObject dob, File f) throws OutOfMemoryError
	{
		m_dob = dob;
		Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");		
		ImageReader reader = iter.next();		
        ImageInputStream iis=null;         
        BufferedImage bi=null;        
        DicomImageReadParam param = 
            (DicomImageReadParam) reader.getDefaultReadParam();
        try {
        	iis = ImageIO.createImageInputStream(f);        	
            reader.setInput(iis, false);
            bi = reader.read(0, param);
            if (bi == null) 
            {
                System.err.println("\nError: " + f + " - couldn't read!");
                return null;
            }
        } 
        catch(Exception ioe)
        {}
        finally 
        {
        	try{iis.close();}catch(Exception e){}
        }
        return createImage(bi);
	}
	
	private AbstractImage createImage(BufferedImage bi)
	{
		if(bi==null) return null;
		return null;
	}
	
	private void CalcStatistics()
	{
		if (m_BytesPerSample == 1)
		{
			byte[] bytes = (byte[]) m_pixels;
			int dp = Math.max(m_nSamples << 15, 1);
			m_maxVal = 0;
			byte b;
			for (int i = 0; i < bytes.length; i += 1)
			{
				b = bytes[i];
				if (m_maxVal < b)
					m_maxVal = b;
			}
		} else
		{
			short[] bytes = (short[]) m_pixels;
			int dp = Math.max(m_nSamples << 15, 1);
			m_maxVal = 0;
			int b;
			for (int i = 0; i < bytes.length; i += 1)
			{
				b = bytes[i];
				if (m_maxVal < b)
					m_maxVal = b;
			}
		}
	}

	private boolean GeneratePixels() throws OutOfMemoryError
	{
		if (m_BytesPerSample > 2)
			return false;
		// first, downsample if image is too big
		int pstep = 1;
		if (m_wid > MAX_DIM || m_ht > MAX_DIM)
		{
			pstep = Math.max(m_wid / MAX_DIM, m_ht / MAX_DIM);
			while (pstep > 1)
				if ((m_wid / pstep) < 8 || (m_ht / pstep) < 8)
					pstep--;
				else
					break;
			if (pstep < 1)
				pstep = 1;
		}
		m_maxVal = 0;

		// downsample if needed and calculate max
		{
			int newW = m_wid / pstep;
			int newH = m_ht / pstep;
			int ind1, ind2;
			Object newPixels;
			if (m_BytesPerSample == 1)
			{
				byte[] newBytes;
				if (pstep > 1)
					newBytes = new byte[newW * newH];
				else
					newBytes = m_srcPixels;

				for (int y = 0, y0 = 0; y < newH; y++, y0 += pstep)
				{
					ind1 = y * newW;
					ind2 = y0 * m_wid;
					for (int x = 0, x0 = 0; x < newW; x++, x0 += pstep)
					{
						if (pstep > 1)
							newBytes[ind1 + x] = m_srcPixels[ind2 + x0];
						// m_maxVal=Math.max(newBytes[ind1+x],m_maxVal);
					}
				}
				newPixels = newBytes;
			} else
			{
				short[] newBytes = new short[newW * newH];

				if (m_LitEndian == 1)
				{
					for (int y = 0, y0 = 0; y < newH; y++, y0 += pstep)
					{
						ind1 = y * newW;
						ind2 = y0 * m_wid;
						for (int x = 0, x0 = 0; x < newW; x++, x0 += pstep)
						{
							newBytes[ind1 + x] = (short) ((((m_srcPixels[2 * (ind2 + x0) + 1]) << 8) & 0xff00) | ((short) (m_srcPixels[2 * (ind2 + x0)] & 0xff)));
							// m_maxVal=Math.max(newBytes[ind1+x],m_maxVal);
						}
					}
				} else
				{
					for (int y = 0, y0 = 0; y < newH; y++, y0 += pstep)
					{
						ind1 = y * newW;
						ind2 = y0 * m_wid;
						for (int x = 0, x0 = 0; x < newW; x++, x0 += pstep)
						{
							newBytes[ind1 + x] = (short) ((((m_srcPixels[2 * (ind2 + x0)]) << 8) & 0xff00) | ((m_srcPixels[2 * (ind2 + x0) + 1] & 0xff)));
							// m_maxVal=Math.max(newBytes[ind1+x],m_maxVal);
						}
					}
				}
				newPixels = newBytes;
			}
			m_wid = newW;
			m_ht = newH;
			m_pixels = newPixels;
			CalcStatistics();
			m_srcPixels = null;
		}
		return true;
	}
	private boolean NormalizePixels()
	{
		if (m_BytesPerSample > 2)
			return false;
		if (m_BytesPerSample == 1)
		{
			byte[] pixels = (byte[]) m_pixels;
			for (int i = 0; i < pixels.length; i++)
			{
				pixels[i] = (byte) (m_maxVal - pixels[i]);
			}
		} else if (m_BytesPerSample == 2)
		{
			short[] pixels = (short[]) m_pixels;
			for (int i = 0; i < pixels.length; i++)
				pixels[i] = (short) (m_maxVal - pixels[i]);
		}
		return true;
	}

	private boolean FormatPixelData() throws OutOfMemoryError
	{
		// for now, only grayscale 1 or 2 bytes per pixel are supported
		if (!m_Photometric.startsWith("MONOCHROME"))
			return false;
		boolean bMon = m_Photometric.compareTo("MONOCHROME1") == 0;

		if (!GeneratePixels())
			return false;
		// planar configuration
		// int planar=m_dob.getInt(GetID(0x0028,0x0006));
		// int offset=0;

		int offset = 0;
		if (m_dob.containsValue(GetID(0x7fe0, 0x000f)))
		{
			offset = m_dob.getInt(GetID(0x7fe0, 0x000f));
		}
		if (bMon)
			NormalizePixels();
		return true;
	}
	private boolean ExtractPixelParameters() throws OutOfMemoryError
	{
		m_wid = m_dob.getInt(Tag.Columns);
		m_ht = m_dob.getInt(Tag.Rows);
		if (m_wid < 2 || m_ht < 2)
			return false;

		m_nFrames = m_dob.getInt(Tag.NumberOfFrames);
		if (m_nFrames < 1)
			m_nFrames = 1;

		m_nPixels = m_wid * m_ht * m_nFrames;

		m_BitsAllocated = m_dob.getInt(Tag.BitsAllocated);
		if (m_BitsAllocated < 1)
			m_BitsAllocated = 8;

		m_BitsStored = m_dob.getInt(Tag.BitsStored);
		if (m_BitsStored < 8)
			m_BitsStored = 8;

		m_HighBit = m_dob.getInt(Tag.HighBit);
		if (m_HighBit < 1)
			m_HighBit = 7;

		m_SamplesPerPixel = m_dob.getInt(Tag.SamplesPerPixel);
		if (m_SamplesPerPixel < 1)
			m_SamplesPerPixel = 1;

		if (m_SamplesPerPixel > 3)
			return false;

		m_BytesPerSample = (m_BitsAllocated + 7) / 8;

		m_ImgSOPInstUID = m_dob.getString(Tag.SOPInstanceUID);
		m_Photometric = m_dob.getString(Tag.PhotometricInterpretation);

		if ((m_srcPixels = m_dob.getBytes(Tag.PixelData)) == null)
			return false;

		if (m_srcPixels.length != m_nPixels * m_SamplesPerPixel
				* m_BytesPerSample)
			return false;

		m_nSamples = m_nPixels * m_SamplesPerPixel;
		m_LitEndian = m_dob.bigEndian() ? 0 : 1;
		return true;
	}
}
