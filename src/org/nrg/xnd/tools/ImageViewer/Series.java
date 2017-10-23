package org.nrg.xnd.tools.ImageViewer;

import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Vector;

import org.nrg.xnd.tools.ImageViewer.ip.ShortImage;
import org.nrg.xnd.utils.LightXML;
import org.nrg.xnd.utils.dicom.DICOMRecord;
import org.nrg.xnd.utils.dicom.SeriesElementRecord;

/**
 * ******************************************************************** Class
 * for storing cached series
 * ********************************************************************
 */
/*
 * class ImageCache { public boolean m_IsSeries; public DImage[] m_Images;
 * public DICOMRecord m_DR; public long m_Size; public int m_Active; public
 * ImageCache(DImage[] Images, DICOMRecord dr, long Size, int Active, boolean
 * IsSeries) { m_DR = (DICOMRecord)dr.clone(); m_Images = Images; m_Size = Size;
 * m_Active = Active; m_IsSeries = IsSeries; }
 * 
 * public void Free() { m_Images = null; }
 * 
 * public boolean Match(DICOMRecord dr) { return m_DR.MatchQueryDR(dr); } }
 */
public class Series extends Rectangle
{
	private boolean m_bSerPaintActiveOnly;
	private boolean m_bSerPaintFrameOnly;
	public boolean m_bFlipRotated = false;
	public boolean m_bPreview = false;
	public int m_Active = 0, m_ToolCode, m_Start = 0;
	public int m_iScreen = -1;
	private Vector<DicomImage> m_ImageVector = new Vector<DicomImage>();
	public DICOMRecord m_SeriesDR = new DICOMRecord();
	private DicomImage m_CurrentToolImage;
	private Rectangle[] m_Screens;
	private Rectangle m_SerClip = null;
	private boolean m_bOriginalOrientation = true;
	public Study m_study;

	public boolean isM_bSerPaintActiveOnly()
	{
		return m_bSerPaintActiveOnly;
	}

	public boolean isM_bSerPaintFrameOnly()
	{
		return m_bSerPaintFrameOnly;
	}

	public Rectangle[] getM_Screens()
	{
		return m_Screens;
	}

	public Rectangle getM_SerClip()
	{
		return m_SerClip;
	}

	public Vector<DicomImage> getImageVector()
	{
		return m_ImageVector;
	}

	public void SetDR(DICOMRecord dr)
	{
		m_SeriesDR.SetFromDR(dr);
	}

	public int CountImages()
	{
		return m_ImageVector.size();
	}

	public Vector<DicomImage> getM_ImageVector()
	{
		return m_ImageVector;
	}

	public int CountImages(boolean bPreview)
	{
		int res = 0;
		if (bPreview)
			for (DicomImage im : m_ImageVector)
				res += im.IsPreview() ? 1 : 0;
		else
			for (DicomImage im : m_ImageVector)
				res += im.IsPreview() ? 0 : 1;
		return res;
	}

	public void RestoreFromPreview()
	{
		for (DicomImage im : m_ImageVector)
		{
			if (im.IsPreview())
				for (int k = 0; k < 3; k++)
				{
					// DICOMViewBase.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try
					{
						im.SerializeTemp(true);
						break;
					} catch (OutOfMemoryError e)
					{
						System.out
								.println("RestoreFromPreview.OutOfMemoryError");
						m_study.OptimizeMemory(e);
					} finally
					{
						// DICOMViewBase.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				}
		}
		PaintSeries(m_study.getGraphics(), false);
	}

	public void ReduceMemoryLoad(double ratio)
	{
		int sz = m_ImageVector.size();
		int maxIm = Math.max(1, (int) Math.round(ratio * sz));

		if (maxIm > sz)
		{
			ConvertToPreview(true, true);
			return;
		}
		int first = Math.max(m_Active - maxIm, 0), last = Math.min(m_Active
				+ maxIm, sz - 1);

		DicomImage im;
		for (int i = 0; i < sz; i++)
		{
			if (i >= first || i <= last)
				continue;
			im = m_ImageVector.get(i);
			im.ConvertToPreview();
		}
	}

	public void ConvertToPreview(boolean bKeepCurrent, boolean bKeepVisible)
	{
		DicomImage active = m_ImageVector.get(m_Active);
		boolean[] bVis = GetVisibleImages();
		DicomImage im;
		for (int i = 0; i < m_ImageVector.size(); i++)
		{
			im = m_ImageVector.get(i);
			if ((bKeepCurrent && (im == active)) || (bKeepVisible && bVis[i]))
				continue;
			im.ConvertToPreview();
		}
	}

	public boolean IsVisible()
	{
		return (m_iScreen >= 0 && m_iScreen < m_study.getM_nSerScreens());
	}

	private boolean[] GetVisibleImages()
	{
		boolean[] res = new boolean[m_ImageVector.size()];
		for (int i = 0; i < m_ImageVector.size(); i++)
		{
			if (m_Screens != null)
			{
				if (i < m_Start || i >= m_Start + m_Screens.length)
					res[i] = false;
				else
					res[i] = true;
			} else
				res[i] = false;
		}
		return res;
	}

	public boolean IsMultiImage()
	{
		return m_ImageVector.size() > 1;
	}

	public Series(Study m_study, boolean bPreview)
	{
		this.m_study = m_study;
		m_ImageVector = new Vector<DicomImage>(20, 50);
		m_study.setBackground(Color.gray);
		m_bPreview = bPreview;
		m_bSerPaintActiveOnly = false;
		m_bSerPaintFrameOnly = false;
	}
	public boolean addImageToSeries(SeriesElementRecord ser)
	throws OutOfMemoryError, IOException, InstantiationException
	{
		DicomImage dIm = new DicomImage(m_study,ser,m_study.m_bPreviewMode);
//		dIm.SerializeTemp(false);
		m_ImageVector.addElement(dIm);
//		dIm.SerializeTemp(false);
		return true;
	}
	
	public boolean AddImageToSeries(Object im, LightXML xml, byte bpp,
			boolean IsSeries) throws OutOfMemoryError
	{
		// Utils.ShowProgressWindow(GetParentFrame(), "test");
		DicomImage dIm = new DicomImage(m_study, im, xml, null, this, bpp);
		dIm.SerializeTemp(false);
		// dIm.ConvertToPreview();
		// dIm.SerializeTemp(true);

		// dIm.ConvertToPreview();
		// dIm.SerializeTemp(true);
		// By default, just dump image to disk and not load it into memory.
		// dIm.CleanMemory();
		// dIm.ConvertToPreview();
		m_ImageVector.addElement(dIm);
		String str = m_SeriesDR.GetString(DICOMRecord.iPatComments); // ??temp
		// fix
		// for
		// pat
		// comment
		if (str == null || str.length() < 1)
		{
			m_SeriesDR.SetString(DICOMRecord.iPatComments, xml
					.GetStringValue("PatComm"));
		}
		return true;		
	}

	public void RemoveAll()
	{
		m_ImageVector.removeAllElements();
	}

	public void EnableInfo(boolean bEnable)
	{
		m_study.setM_bShowInfo(bEnable);
		PaintSeries(m_study.getGraphics(), false);
	}

	public boolean Cine(int dir, boolean bActiveOnly, boolean bLoop,
			boolean skipDraw)
	{
		int newStart = m_Start;
		if (bLoop)
		{
			if ((m_Start + dir) >= m_ImageVector.size()
					|| (m_Start + dir) <= -m_Screens.length)
			{
				if (dir > 0)
					newStart = 0;
				else
					newStart = m_ImageVector.size() - 1;
			} else
			{
				newStart += dir;
			}
			if (newStart == m_Start)
				return false;

			m_Start = newStart;
			m_Active = m_Active + dir;
			if (m_Active < 0)
				m_Active = m_ImageVector.size() - 1;
			else if (m_Active > m_ImageVector.size() - 1)
				m_Active = 0;
		} else
		{
			if ((m_Start + dir) >= m_ImageVector.size()
					|| (m_Start + dir) <= -m_Screens.length)
			{
				if (dir > 0)
					newStart = m_ImageVector.size() - 1;
				else
					newStart = -m_Screens.length + 1;
			} else
			{
				newStart += dir;
			}
			if (newStart == m_Start)
				return false;
			m_Start = newStart;
			m_Active = Math.min(Math.max(0, m_Active + dir), m_ImageVector
					.size() - 1);
		}

		UpdateScreenRects();
		m_bSerPaintActiveOnly = bActiveOnly;
		PaintSeries(m_study.getGraphics(), skipDraw);
		return true;
	}

	private void UpdateScreenRects()
	{
		if (m_Screens == null)
		{
			return;
		}
		for (int i = 0; i < m_Screens.length; i++)
		{
			if (i + m_Start >= 0 && i + m_Start < m_ImageVector.size())
			{
				m_ImageVector.get(i + m_Start).SetScreenRect(m_Screens[i]);
			}
		}
	}

	public void ChangeFrame(boolean dir)
	{
		int size = m_ImageVector.size();
		int prev_active = m_Active;
		if (size < 2)
		{
			return;
		}

		if (!dir)
		{
			m_Active = Math.min(m_Active + 1, size - 1);
		} else
		{
			m_Active = Math.max(m_Active - 1, 0);
		}
		if (prev_active != m_Active)
		{
			PaintSeries(m_study.getGraphics(), false);
		}
	}

	public void SelectActiveImage(int x, int y)
	{
		if (m_Screens == null)
		{
			return;
		}
		int size = m_Screens.length;
		if (size == 0)
		{
			return;
		}
		int sel = 0;
		for (int i = 0; i < size; i++)
		{
			if (m_Screens[i].contains(x, y))
			{
				sel = i;
				break;
			}
		}
		sel += m_Start;
		if (sel >= 0 && sel < m_ImageVector.size())
		{
			if (m_Active != sel)
			{
				m_Active = sel;
				m_bSerPaintFrameOnly = true;
				try
				{
					PaintSeries(m_study.getGraphics(), false);
				} catch (OutOfMemoryError e)
				{
					System.out.println("SelectActiveImage.OutOfMemoryError");
					m_study.OptimizeMemory(e);
					return;
				}
			}
		}
	}

	public double GetRelativeZoom()
	{
		if (m_ImageVector.size() < 1)
			return 0;
		return (double) m_ImageVector.get(0).Width()
				/ (double) m_ImageVector.get(0).GetScreenMap().getM_rImage().width;
	}

	public int GetSize()
	{
		return m_ImageVector.size();
	}

	public DicomImage GetActiveImage()
	{
		return m_ImageVector.get(m_Active);
	}

	public int GetScreenHeight()
	{
		try
		{
			return m_Screens[0].height;
		}

		catch (Exception e)
		{
			return -1;
		}
	}

	public Object GetSeriesCrosslink()
	{
		if (GetSize() < 1)
			return null;
		if (m_Active > m_ImageVector.size() - 1 || m_bFlipRotated)
		{
			return null;
		}
		if (!m_ImageVector.get(m_Active).CanCrossLink())
		{
			return null;
		}
		return m_ImageVector.get(m_Active).CrossLinkRect();
	}

	public void SetStatusString(String s)
	{
		m_study.getDicomView().m_StatusLabel.setText(s);
	}

	/**
	 * **************************************************************** This
	 * method is to be called when image output area (m_rScreen rectangle in
	 * class ScreenMap) is changed, to repaint the entire background.
	 * ****************************************************************
	 */
	public void RepaintSerBackground(Rectangle r)
	{
		Graphics g = m_study.getGraphics();
		if (g == null)
		{
			return;
		}
		if (r == null)
		{
			r = new Rectangle(this);
		}
		g.setColor(m_study.getBackground()); // set the drawing color to the
		// background color
		g.fillRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
		g.setColor(m_study.getForeground()); // reset the foreground color
	}

	public boolean CanWL()
	{
		return true;
		/*
		 * if(m_Images==null) return false; for(int i=0; i<m_Images.length; i++)
		 * { if(m_Images[i].IsRGB()) return false; } return true;
		 */
	}

	public void EndLoading()
	{
		if (m_ImageVector.size() < 1)
			return;
		ResetSeries();
	}

	public synchronized void SetLayout(int code)
	{
		if (code != -1)
		{
			m_study.m_SerLayType = code;
		}
		if (m_ImageVector.size() == 0)
			return;
		m_study.getM_CineThread().m_controlID = 0;
		double count = GetSize();
		double icount;
		double shift = 5;
		Rectangle rect = m_ImageVector.get(0).GetScreenRect();
		double picRatio = (rect.width / rect.height);
		double width = this.width;
		double height = this.height;

		m_SerClip = new Rectangle(this);
		double zoom;
		double best_zoom = 0.0;
		int best_nc = 1;
		int nr;
		icount = count;
		switch (m_study.m_SerLayType)
		{
			case Study.LAYOUT_STACK :
				best_nc = 1;
				nr = 1;
				break;
			case Study.LAYOUT_ONE_TWO :
				nr = 1;
				best_nc = 2;
				break;
			case Study.LAYOUT_TWO_ONE :
				nr = 2;
				best_nc = 1;
				break;
			case Study.LAYOUT_TWO_TWO :
				nr = 2;
				best_nc = 2;
				break;
			case Study.LAYOUT_TWO_THREE :
				nr = 2;
				best_nc = 3;
				break;
			case Study.LAYOUT_THREE_TWO :
				nr = 3;
				best_nc = 2;
				break;
			case Study.LAYOUT_THREE_THREE :
				nr = 3;
				best_nc = 3;
				break;
			case Study.LAYOUT_FOUR_FOUR :
				nr = 4;
				best_nc = 4;
				break;
			default :
				for (int nc = 1; nc <= 1 + icount / 2; nc++)
				{
					nr = (int) ((icount + nc - 1) / nc);
					zoom = Math.min((width - shift * (nc + 1.0))
							/ (rect.width * nc), (height - shift * (nr + 1.0))
							/ (rect.height * nr));
					if (zoom > best_zoom)
					{
						best_zoom = zoom;
						best_nc = nc;
					}
				}
				nr = (int) ((icount + best_nc - 1) / best_nc);

		}
		best_zoom = Math.min((width - shift * (best_nc + 1.0))
				/ (rect.width * best_nc), (height - shift * (nr + 1.0))
				/ (rect.height * nr));
		int w = Math.max(4, (int) (rect.width * best_zoom));
		int h = Math.max(4, (int) (rect.height * best_zoom));
		int dx = (int) Math.max(1, (width - w * best_nc) / (best_nc + 1));
		int dy = (int) Math.max(1, (height - h * nr) / (nr + 1));
		int current = 0;
		int old_size = (m_Screens != null) ? m_Screens.length : 0;
		m_Screens = new Rectangle[nr * best_nc];
		m_Start = m_Active;
		/*
		 * if (nr * best_nc <= old_size) { m_Start = m_Active; } else { m_Start
		 * = 0; }
		 */
		for (int i = 0; i < nr; i++)
		{
			for (int j = 0; j < best_nc; j++)
			{
				m_Screens[current] = new Rectangle(dx + j * (dx + w) + this.x,
						dy + i * (dy + h) + this.y, w, h);
				if (current + m_Start < icount)
				{
					m_ImageVector.get(current + m_Start).SetScreenRect(
							m_Screens[current]);
				}
				current++;
			}
		}
	}

	public void Flip(int dir)
	{
		if (dir < 0)
		{
			return;
		}
		m_bFlipRotated = true;
		if (dir > 1)
		{
			m_bOriginalOrientation = !m_bOriginalOrientation;
			RepaintSerBackground(null);
		}
		for (DicomImage im : m_ImageVector)
			im.FlipRotate(dir);
		if (dir == 3 || dir == 4)
		{
			SetLayout(m_study.m_SerLayType);
		}
		try
		{
			PaintSeries(m_study.getGraphics(), false);
		} catch (OutOfMemoryError e)
		{
			System.out.println("Flip.OutOfMemoryError");
			m_study.OptimizeMemory(e);
			return;
		}
	}

	public void ResetSeries()
	{
		for (DicomImage im : m_ImageVector)
			im.ResetImage();
		ResetTool();
		for (int nTries = 1; nTries <= 3; nTries++)
			try
			{
				SetWLDefault();
				PaintSeries(m_study.getGraphics(), false);
				return;
			} catch (OutOfMemoryError e)
			{
				System.out.println("ResetSeries.OutOfMemoryError");
				m_study.OptimizeMemory(e);
			}
	}

	/**
	 * **************************************************************** Shifts
	 * displayed part of zoomed image in the specified direction
	 * ****************************************************************
	 */
	public boolean ShiftImage(int shiftX, int shiftY, boolean bAll)
	{
		DicomImage currentImage = m_ImageVector.get(m_Active);
		boolean bRes;
		if (!bAll)
		{
			bRes = currentImage.ShiftImage(shiftX, shiftY);
			m_bSerPaintActiveOnly = true;
		} else
		{
			for (DicomImage im : m_ImageVector)
				im.SetImageRect(currentImage);
			bRes = true;
		}
		try
		{
			PaintSeries(m_study.getGraphics(), false);
		} catch (OutOfMemoryError e)
		{
			System.out.println("ShiftImage.OutOfMemoryError");
			m_study.OptimizeMemory(e);
		}
		return bRes;
	}

	public void ResetTool()
	{
		if (m_CurrentToolImage != null)
		{
			m_study.RepaintClipRegion(m_study.getGraphics(), m_CurrentToolImage
					.GetToolClip(m_ToolCode));
			m_CurrentToolImage.ResetTool();
		}

		SetStatusString(Util.Trans("Ready"));
		m_CurrentToolImage = null;
		m_ToolCode = -1;
	}

	synchronized private void DrawTool(DicomImage im, Point pt)
	{
		if (!m_study.m_bToolRenderingActive)
		{
			return;
		}
		Rectangle temp_clip;
		Graphics g = m_study.getGraphics();
		if (g == null)
		{
			return;
		}
		temp_clip = im.PrepareTool(pt, m_ToolCode);
		im.EraseTool(g, m_ToolCode);
		g.setClip(temp_clip.x, temp_clip.y, temp_clip.width, temp_clip.height);
		m_bSerPaintActiveOnly = true;
		try
		{
			PaintSeries(g, false);
		} catch (OutOfMemoryError e)
		{
			System.out.println("DrawTool.OutOfMemoryError");
			m_study.OptimizeMemory(e);
			return;
		}
		g.setClip(m_SerClip.x, m_SerClip.y, m_SerClip.width, m_SerClip.height);
		im.DrawTool(g, m_ToolCode);
		g.setClip(m_study.m_StClip);
		SetStatusString(im.GetToolString(m_ToolCode));
	}

	synchronized public void InitTool(Point pt, int ToolCode)
	{
		if (!m_study.m_bToolRenderingActive)
		{
			return;
		}
		DicomImage im;
		Graphics g = m_study.getGraphics();
		if (g == null)
		{
			return;
		}
		im = m_ImageVector.get(m_Active);
		if (im.Contains(pt.x, pt.y, true))
		{
			if (m_CurrentToolImage != im)
			{
				ResetTool();
			} else if (m_CurrentToolImage != null)
			{
				m_study.RepaintClipRegion(g, m_CurrentToolImage
						.GetToolClip(ToolCode));

			}
			m_ToolCode = ToolCode;
			im.InitTool(pt, ToolCode);
			DrawTool(im, pt);
			m_CurrentToolImage = im;
		}
	}

	synchronized public void DrawTool(Point pt)
	{
		DicomImage im = m_ImageVector.get(m_Active);
		if (im == m_CurrentToolImage && im.Contains(pt.x, pt.y, true))
		{
			DrawTool(im, pt);
		}
	}

	private int GetMedian(int[] data)
	{
		int sz = data.length;
		if (sz == 0)
		{
			return 0;
		}
		if (sz < 2)
		{
			return data[0];
		}
		int temp;
		for (int i = 0; i < sz - 1; i++)
		{
			for (int j = i + 1; j < sz; j++)
			{
				if (data[i] > data[j])
				{
					temp = data[j];
					data[j] = data[i];
					data[i] = temp;
				}
			}
		}
		if (sz % 2 == 0)
		{
			return (data[sz / 2 - 1] + data[sz / 2]) / 2;
		} else
		{
			return data[(sz - 1) / 2];
		}
	}

	public void SetWLDefault() throws OutOfMemoryError
	{
		if (GetSize() < 1)
			return;
		// long[] stats;
		// int minPix,maxPix;
		// stats=m_Images[0].GetStatistics(null,true);
		int[] wins = new int[GetSize()];
		int[] levs = new int[GetSize()];
		// double avg=0;
		// int minMed, maxMed;
		double[] wl = new double[2];
		if (m_ImageVector.get(0).IsHU())
		{
			// minPix = m_Images[0].GetHU(stats[0]); maxPix =
			// m_Images[0].GetHU(stats[1]);
			for (int i = 0; i < GetSize(); i++)
			{
				m_ImageVector.get(i).SetDefaultLookupTable();
				wl = m_ImageVector.get(i).GetWLHU();
				wins[i] = (int) wl[0];
				levs[i] = (int) wl[1];
				// stats=m_Images[i].GetStatistics(null,true);
				// mins[i]=m_Images[i].GetHU(stats[0]);
				// maxs[i]=m_Images[i].GetHU(stats[1]);
				// avg+=m_Images[i].GetHU(stats[2])/stats[4];
				// minPix=Math.min(m_Images[i].GetHU(stats[0]),minPix);
				// maxPix=Math.max(m_Images[i].GetHU(stats[1]),maxPix);
			}
			/*
			 * minMed=GetMedian(mins); maxMed=GetMedian(maxs);
			 * if(minMed>=maxMed) {minMed=minPix;maxMed=maxPix;}
			 * avg/=m_Images.length; if(avg<=minMed || avg>=maxMed)
			 * avg=(minMed+maxMed)*0.5; wl[0]=(int)avg;//(minMed+maxMed)*0.5;
			 * wl[1]=Math.max(maxMed-avg,avg-minMed);//(maxMed-minMed)*0.5;
			 */
			wl[0] = GetMedian(wins);
			wl[1] = GetMedian(levs);
			for (DicomImage im : m_ImageVector)
				im.SetWLHU(wl);
		} else
		{
			double mn=65535,mx=0;
			if(m_ImageVector.get(0).GetImage() instanceof ShortImage)
			{
				double[] winlev=new double[2];
				int[] imMaxMin;
				for(DicomImage im: m_ImageVector)
				{
					imMaxMin=((ShortImage)im.GetImage()).getMaxMinStat();
					mx=Math.max(mx, imMaxMin[1]);
					mn=Math.min(mn,imMaxMin[0]);
					winlev[0]=(mx-mn)/2; winlev[1]=(mx+mn)/2;
					im.GetImage().SetWL(winlev);
				}				
			}
			else
			{
				for (DicomImage im : m_ImageVector)
					im.SetDefaultLookupTable();
			}
		}			
	}

	/**
	 * **************************************************************** 
	 * Changing contrast and brightness of displaying images
	 * ****************************************************************
	 */
	public void SetPresetWL(int commandID, int commandID1)
	{
		DicomImage currentImage = m_ImageVector.get(m_Active);
		if (currentImage == null)
		{
			return;
		}
		if (!currentImage.IsHU())
		{
			return;
		}
		double[] wl = new double[2];
		wl[0] = 0;
		wl[1] = 0;
		switch (commandID)
		{
			case 0 :
				wl[0] = 750;
				wl[1] = -600;
				break; // lung
			case 1 :
				wl[0] = 1250;
				wl[1] = 480;
				break; // bone
			case 2 :
				wl[0] = 40;
				wl[1] = 40;
				break; // brain
			case 3 :
				wl[0] = 175;
				wl[1] = 40;
				break; // chest
			case 4 :
				wl[0] = 175;
				wl[1] = 50;
				break; // heart
			case 5 :
				wl[0] = 175;
				wl[1] = 90;
				break; // head
			case 6 :
				wl[0] = 175;
				wl[1] = 40;
				break; // abdomen
			case 7 :
				wl[0] = 150;
				wl[1] = 25;
				break; // spine
			case 8 :
				wl[0] = 75;
				wl[1] = 50;
				break; // liver
			case 9 :
				wl[0] = 100;
				wl[1] = 30;
				break; // kidney
			case 11 : // ultrasound
				switch (commandID1)
				{
					case 0 :
						wl[0] = 95;
						wl[1] = 80;
						break; // low
					case 1 :
						wl[0] = 80;
						wl[1] = 70;
						break; // med
					case 2 :
						wl[0] = 60;
						wl[1] = 60;
						break; // high
				}
				break;
			case 12 : // mammogram
				switch (commandID1)
				{
					case 0 :
						wl[0] = 900;
						wl[1] = 2350;
						break; // low
					case 1 :
						wl[0] = 750;
						wl[1] = 2350;
						break; // med
					case 2 :
						wl[0] = 600;
						wl[1] = 2350;
						break; // high
				}
				break;
		}
		if (wl[0] == 0 && wl[1] == 0)
		{
			return;
		}
		// reverse hu transform
		currentImage.SetWLHU(wl);
		SetWindowLevel(currentImage, true);
		SetStatusString(currentImage.GetToolString(Tool.WL));
	}

	public void WindowLevel(int shiftX, int shiftY, boolean bAll)
	{
		int size = GetSize();
		if (size < 1)
			return;
		DicomImage currentImage = m_ImageVector.get(m_Active);
		currentImage.WindowLevel(shiftX, shiftY, !bAll);
		SetStatusString(currentImage.GetToolString(Tool.WL));
		SetWindowLevel(currentImage, bAll);
	}

	public void InitWLMenu(Tool.SidePopupMenu popupMenu)
	{
		boolean bEn = false;
		DicomImage currentImage = m_ImageVector.get(m_Active);
		if (currentImage != null)
		{
			bEn = currentImage.IsHU();
		}

		MenuItem mi, mi1;
		for (int i = 0; i < popupMenu.getItemCount(); i++)
		{
			mi = popupMenu.getItem(i);
			if (mi instanceof Menu) // process submenu items
			{
				for (int j = 0; j < ((Menu) mi).getItemCount(); j++)
				{
					mi1 = ((Menu) mi).getItem(j);
					if (mi1 instanceof CheckboxMenuItem)
					{
						((CheckboxMenuItem) mi1).setState(false);
						((CheckboxMenuItem) mi1).setEnabled(bEn);
					}
				}
				continue;
			} else if (mi instanceof CheckboxMenuItem)
			{
				((CheckboxMenuItem) mi).setState(false);
				((CheckboxMenuItem) mi).setEnabled(bEn);
			}
		}
	}

	private void SetWindowLevel(DicomImage currentImage, boolean bAll)
	{
		m_bSerPaintActiveOnly = true;
		int size = GetSize();
		if (bAll)
		{
			double[] wl;
			boolean bHu = currentImage.IsHU();
			if (bHu)
			{
				wl = currentImage.GetWLHU();
			} else
			{
				wl = currentImage.GetWLColor();
			}
			DicomImage im;
			for (int i = 1; i <= size; i++)
			{
				if (i == m_Active + 1)
				{
					continue;
				}
				im = m_ImageVector.get(i - 1);
				if (bHu)
				{
					im.SetWLHU(wl);
				} else
				{
					im.SetWLColor(wl);
				}
			}
			m_bSerPaintActiveOnly = false;
		}
		try
		{
			PaintSeries(m_study.getGraphics(), false);
		} catch (OutOfMemoryError e)
		{
			System.out.println("SetWindowLevel.OutOfMemoryError");
			m_study.OptimizeMemory(e);
			return;
		}
		// SetStatusString(Util.Trans("Ready"));
	}

	/**
	 * ****************************************************************
	 * Increases or decreases image zoom, calling resizing routines
	 * ****************************************************************
	 */
	public void ChangeZoom(boolean IsIncrease, boolean bAll)
	{
		DicomImage currentImage = m_ImageVector.get(m_Active);
		if (!bAll)
		{
			currentImage.ChangeZoom(IsIncrease);
			m_bSerPaintActiveOnly = true;
		} else
		{
			for (DicomImage im : m_ImageVector)
				im.SetImageRect(currentImage);
		}
		try
		{
			PaintSeries(m_study.getGraphics(), false);
		} catch (OutOfMemoryError e)
		{
			System.out.println("ChangeZoom.OutOfMemoryError");
			m_study.OptimizeMemory(e);
			return;
		}
	}
	private void performDraw(Graphics g)
	{
		boolean bCurActive;
		for (int i = 0; i < getM_Screens().length; i++)
		{
			if ((i + m_Start < 0 || i + m_Start >= GetSize()))
			{
				RepaintSerBackground(getM_Screens()[i]);
				continue;
			}
			DicomImage im = getImageVector().get(i + m_Start);
			bCurActive = (i + m_Start == m_Active)
					&& (this == m_study.getM_CurrentSeries());
			if (bCurActive || !isM_bSerPaintActiveOnly()
					|| m_study.isM_bShowCrosslink())
			{
				boolean bPainted = false;
				for (int k = 0; k < 3; k++)
				{
					try
					{
						// try to restore to full resolution
						if (bCurActive && im.IsPreview())
						{
							try
							{
								im.SerializeTemp(true);
							} finally
							{
							}
						}
						if (k == 0)
						{
							bPainted = im.paint(g, getM_SerClip(), bCurActive,
									isM_bSerPaintFrameOnly()
											|| m_study.isM_bStPaintFrameOnly(),
									m_bFlipRotated, m_study
											.isM_bShowCrosslink()
											&& (this != m_study
													.getM_CurrentSeries()),
									m_study.isM_bShowInfo(), m_bPreview,
									m_study.GetStudyCrosslink());
						} else
						{
							if (!im.paint(g, getM_SerClip(), bCurActive,
									isM_bSerPaintFrameOnly()
											|| m_study.isM_bStPaintFrameOnly(),
									m_bFlipRotated, m_study
											.isM_bShowCrosslink()
											&& (this != m_study
													.getM_CurrentSeries()),
									m_study.isM_bShowInfo(), m_bPreview,
									m_study.GetStudyCrosslink()))
							{
								im.ConvertToPreview();
								continue;
							}
						}
						break;
					} catch (OutOfMemoryError e)
					{
						try
						{
							/*
							 * m_study.getDicomView().m_MemBuf = null;
							 * System.gc(); m_study.OptimizeMemory(e);
							 * m_study.getDicomView().m_MemBuf = new
							 * byte[32735];
							 */
						} catch (OutOfMemoryError e1)
						{
							System.out
									.println("PaintSeries.OutOfMemoryError loc 2:");
							System.out.println("Unrecoverable memory error");
						}
					}
				}
			}
		}
	}

	/**
	 * **************************************************************** Painting
	 * rectangle from image to rectangle from canvas, as well as borders and
	 * adequate frames.
	 * ****************************************************************
	 */
	public void PaintSeries(Graphics g, boolean skipPaint)
	{
		if (m_Screens == null || GetSize() == 0 || g == null)
			return;
		if (m_Screens.length < 1)
			return;

		// seriesDrawThread.(g, this, this.m_study);
		if (skipPaint == false)
		{
			performDraw(g);
		}

		if (m_Screens.length > 0)
		{
			if (this == m_study.getM_CurrentSeries())
			{
				if (m_study.GetActiveSerIndex() >= 0)
				{
					m_study.getDicomView().m_nSerLabel.setText(Util
							.Trans("Ser")
							+ " "
							+ (m_study.GetActiveSerIndex() + 1)
							+ "/"
							+ m_study.getM_Series().length);
				} else
				{
					m_study.getDicomView().m_nSerLabel.setText("");
				}
				m_study.getDicomView().m_nImageLabel.setText(Util.Trans("Img")
						+ " " + Integer.toString(m_Active + 1) + "/"
						+ GetSize());
			}
		} else
		{
			m_study.getDicomView().m_nImageLabel.setText("");
		}
		if (this == m_study.getM_CurrentSeries())
		{
			g.setColor(Color.white);
		} else
		{
			g.setColor(Color.lightGray);
		}
		g.drawRect(x, y, width - 1, height - 1);
		g.setColor(Color.black);
		g.drawRect(x + 1, y + 1, width - 3, height - 3);
		m_bSerPaintActiveOnly = false;
		m_bSerPaintFrameOnly = false;
	}

} // end of class Series
