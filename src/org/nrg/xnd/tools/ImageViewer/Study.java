package org.nrg.xnd.tools.ImageViewer;

import java.awt.Canvas;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import org.nrg.xnd.utils.LightXML;
import org.nrg.xnd.utils.dicom.DICOMRecord;
import org.nrg.xnd.utils.dicom.SeriesElementRecord;
import org.nrg.xnd.utils.dicom.SeriesRecord;

/**
 * ************************************************************************
 * Class responsible for drawing maintaining image series on its canvas
 * ********************************* ***************************************
 */
public class Study extends Canvas
{
	Color m_bkColor = Color.gray;
	boolean m_bToolRenderingActive = false;
	private boolean m_bShowInfo = false;
	private boolean m_bUpdateBackground = true;
	private boolean m_bStPaintFrameOnly = false;
	private boolean m_bShowCrosslink = false;
	protected boolean m_bPreviewMode = false;
	public boolean m_bSerStack = false;
	public boolean m_bLowMemory = false;
	public boolean m_bLoadingFromWorklist = true;
	private boolean m_bSerBrowse = false;
	private int m_ActiveSeriesIndex = 0, m_nSerScreens = 0,
			m_iSelectedSer = -1;
	public int m_PopupOffset = 0;
	public int m_StLayType = LAYOUT_AUTO, m_SerLayType = LAYOUT_STACK;
	private Vector<Series> m_SeriesVector;
	public DICOMRecord m_StudyDR = new DICOMRecord();
	public String m_StudyNote = "";
	Rectangle m_StClip;
	private Series m_CurrentSeries = null;
	private int m_iPrevScreen = -9999;
	private int m_UpdateInd = -1;
	private Series[] m_Series;
	private CineThread m_CineThread = new CineThread(this);
	static final int LAYOUT_AUTO = 0, LAYOUT_STACK = 1, LAYOUT_ONE_TWO = 2,
			LAYOUT_TWO_ONE = 3, LAYOUT_TWO_TWO = 4, LAYOUT_TWO_THREE = 5,
			LAYOUT_THREE_TWO = 6, LAYOUT_THREE_THREE = 7, LAYOUT_FOUR_FOUR = 8,
			LAYOUT_UNCHANGED = -1;
	
	private ImageViewer dicomViewBase;
	
	public void setPreviewMode(boolean bPreviewMode){m_bPreviewMode=bPreviewMode;}
	
	public int getM_nSerScreens()
	{
		return m_nSerScreens;
	}

	public CineThread getM_CineThread()
	{
		return m_CineThread;
	}

	public Series getM_CurrentSeries()
	{
		return m_CurrentSeries;
	}

	public boolean isM_bStPaintFrameOnly()
	{
		return m_bStPaintFrameOnly;
	}

	public ImageViewer getDicomView()
	{
		return dicomViewBase;
	}

	public boolean isM_bShowInfo()
	{
		return m_bShowInfo;
	}

	public void setM_bShowInfo(boolean m_bShowInfo)
	{
		this.m_bShowInfo = m_bShowInfo;
	}

	public void SetStudyDR(DICOMRecord dr)
	{
		m_StudyDR.SetFromDR(dr);
	}

	public Series[] getM_Series()
	{
		return m_Series;
	}

	public boolean isM_bShowCrosslink()
	{
		return m_bShowCrosslink;
	}

	private boolean BelongsToThisStudy(DICOMRecord dr)
	{
		String id1 = m_StudyDR.GetString(DICOMRecord.iStInstUID);
		String id2 = dr.GetString(DICOMRecord.iStInstUID);
		if (id1 == null || id2 == null)
		{
			return false;
		}
		int ind1 = id1.indexOf('*'), ind2 = id1.indexOf('*');
		if (ind1 >= 0)
		{
			id1 = id1.substring(ind1 + 1);
		}
		if (ind2 >= 0)
		{
			id2 = id2.substring(ind2);
		}
		if (id1.length() > id2.length())
		{
			return id1.endsWith(id2);
		} else if (id2.length() > id1.length())
		{
			return id2.endsWith(id1);
		}
		return (id1.compareTo(id2) == 0);
	}

	// //////////////////////////
	// returns: -3 if it's a different study, -2 if the same study at the
	// same level,
	// -1 if series not found in study, and index of series otherwise
	public int ContainedInStudy(DICOMRecord dr)
	{
		if (!BelongsToThisStudy(dr))
		{
			return -3;
		}
		switch (dr.m_QLevel)
		{
			case 2 : // Study
				return -2;
			case 3 : // Series
				if (m_Series == null)
					return -1;
				for (int i = 0; i < m_Series.length; i++)
				{
					if (m_Series[i].m_SeriesDR.GetEntityIndex() == dr
							.GetEntityIndex())
					{
						return i;
					}
				}
				return -1;
			case 4 : // Image
				if (m_Series == null)
					return -1;
				for (int i = 0; i < m_Series.length; i++)
				{
					if (m_Series[i].m_SeriesDR.GetString(
							DICOMRecord.iSerInstUID).compareTo(
							dr.GetString(DICOMRecord.iSerInstUID)) == 0)
					{
						return i;
					}
				}
			default :
				return -3;
		}
	}

	public void EnableIndexUpdate(boolean bEnable)
	{
		m_UpdateInd = (bEnable) ? -1 : 0;
	}

	public void OptimizeMemory(Throwable e)
	{
		// Util.LogError("OutOfMemory: optimizing...", e);
		System.out.println("Optimizing memory...");
		/*
		 * ErrorMessage confirmDialog = new ErrorMessage(DBWindow
		 * .FindParentFrame(ImageViewer.this.getParent()),
		 * Util.Trans("$no_memory_to_continue_operation"),
		 * Util.Trans("Out of memory"), true, false);
		 * confirmDialog.setVisible(true);
		 */
		m_bLoadingFromWorklist = false;
		// m_UpdateInd = 0;
		dicomViewBase.getContainer().setCursor(
				Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try
		{
			OptimizeStudy();
		} finally
		{
			dicomViewBase.getContainer().setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		System.out.println("Study optimized");
		// m_UpdateInd = -1;
		// ResetAll();
	}

	public boolean IsMultiImage()
	{
		if (m_Series == null)
		{
			return false;
		}
		for (int i = 0; i < m_Series.length; i++)
		{
			if (m_Series[i].IsMultiImage())
			{
				return true;
			}
		}
		return false;
	}

	public boolean IsMultiSeries()
	{
		if (m_Series == null)
		{
			return false;
		}
		return (m_Series.length > 1);
	}

	public void SetToolActive(boolean bActive)
	{
		m_bToolRenderingActive = bActive;
	}

	public void SetBrowseMode()
	{
		SetBrowseMode(m_bSerBrowse ? 1 : 0);
	}

	private boolean IsStudyComplete()
	{
		if (m_Series == null)
		{
			return false;
		}
		if (m_Series.length < 1)
		{
			return false;
		}
		return (m_Series.length == m_Series[0].m_SeriesDR.GetEntitySize());
	}

	public void UpdateArrows()
	{
		if (m_bSerBrowse)
		{
			boolean l, r;
			int vd = GetValidDirections();
			l = (vd & 2) > 0;
			r = (vd & 1) > 0;
			if (!l && !r)
			{
				if (!IsStudyComplete())
				{
					l = r = true;
				}
			}
			dicomViewBase.UpdateDirectionButtons(l, r);
		} else
		{
			dicomViewBase.EnableDirectionButtons(true);
		}
	}

	public void SetBrowseMode(int ind)
	{
		m_bSerBrowse = (ind == 0) ? false : true;
		UpdateArrows();
	}

	public boolean IsBrowseSeries()
	{
		return m_bSerBrowse;
	}

	public void ResetStudy()
	{
		if (m_CurrentSeries == null)
		{
			return;
		}
		m_CurrentSeries.ResetSeries();
		Graphics g = Study.this.getGraphics();
		if (g != null)
		{
			g.setClip(null);
		}
	}

	public void ResetAll()
	{
		// RepaintBackground(null);
		for (int i = 0; i < m_Series.length; i++)
		{
			if (m_Series[i].IsVisible())
			{
				m_Series[i].ResetSeries();
			}
		}
		Graphics g = Study.this.getGraphics();
		if (g != null)
		{
			g.setClip(null);
		}
	}

	public Study(ImageViewer dicomViewBase)
	{
		this.dicomViewBase = dicomViewBase;
		m_SeriesVector = new Vector<Series>(20, 50);
		setBackground(m_bkColor);
		// setBackground(Color.gray);
	}

	public void SetSeriesComplete(boolean bComplete)
	{
		if (m_CurrentSeries != null)
		{
			m_CurrentSeries.m_bPreview = !bComplete;
		}
	}

	public boolean loadSeries(SeriesRecord sr, IProgressReporter pr)
	{
		DICOMRecord dr=new DICOMRecord();
		dr.initFromDOB(sr.getDOB());
		StartSeriesLoading(dr);
		int iRes, nIm=1;
		
		Collection<SeriesElementRecord> cser=sr.getElements();
		
		final int maxImages=10,nImages=cser.size();
		int accum=0;
		
		for(SeriesElementRecord ser:cser)
		{
			pr.subTaskName("Loading image "+nIm);
			if(m_bPreviewMode)
			{
				if(maxImages<cser.size())
				{
					accum+=maxImages;
					if(accum<nImages) continue;
					accum-=nImages;
				}
			}
			if((iRes=addImageToStudy(ser))==-1) return false;
			if(iRes==0) 
			{
				if(addImageToStudy(ser)!=1) return false;
			}
			nIm++;
			if(pr.isCanceled()) return false;
		}
		EndSeriesLoading();
		return true;
	}
	/**
	 * @param ser
	 * @return -1: unrecoverable error, 0: can try to repeat the call to recover the error, 1: success 
	 */
	public int addImageToStudy(SeriesElementRecord ser)
	{
		if(m_CurrentSeries==null) return -1;
		boolean bRes;
		try
		{
			bRes=m_CurrentSeries.addImageToSeries(ser);
		}
		catch(IOException ioe)
		{
			System.err.println("Study.loadSeries.IOException");
			return -1;
		}
		catch(InstantiationException ie)
		{
			System.err.println("Study.loadSeries.InstantiationException");
			return -1;
		}
		catch(OutOfMemoryError oome)
		{
			System.err.println("Study.loadSeries.OutOfMemoryError");
			OptimizeStudy();
			return 0;
		}
		return (bRes)?1:-1;
		
	}
	
	public boolean AddImageToStudy(Object im, LightXML xml, byte bpp,
			boolean IsSeries)
	{
		if (m_CurrentSeries == null)
			return false;
		boolean bRes;
		for (int i = 0; i < 2; i++)
		{
			try
			{
				bRes = m_CurrentSeries.AddImageToSeries(im, xml, bpp, IsSeries);
				return bRes;
			} catch (OutOfMemoryError e)
			{
				// System.out.println("AddImageToStudy.OutOfMemoryError");
				OptimizeStudy();
			}
		}
		return false;
	}

	public void EnableInfo(boolean bEnable)
	{
		m_bShowInfo = bEnable;
		paint(getGraphics());
	}

	private boolean IsStack()
	{
		return (m_StLayType == LAYOUT_STACK || m_bSerStack);
	}

	public synchronized void ControlCineThread(int commandID)
	{
		switch (commandID)
		{
			case 0 :
				m_CineThread.m_controlID = 0;
				break;
			case 1 :
				m_CineThread.m_controlID = 1;
				try
				{
					if (!m_CineThread.isAlive())
						m_CineThread.start();
				} catch (Exception e)
				{
					/*
					 * CineThread oldThread=m_CineThread; m_CineThread=new
					 * CineThread(); m_CineThread.m_controlID=1;
					 * m_CineThread.m_fps=oldThread.m_fps; m_CineThread.start();
					 */
				}
				break;
			case 2 :
				m_CineThread.m_controlID = 2;
				try
				{
					if (!m_CineThread.isAlive())
						m_CineThread.start();
				} catch (Exception e)
				{
					/*
					 * CineThread oldThread=m_CineThread; m_CineThread=new
					 * CineThread(); m_CineThread.m_controlID=1;
					 * m_CineThread.m_fps=oldThread.m_fps; m_CineThread.start();
					 */
				}
				break;
			case 4 :
				m_CineThread.m_fps = 3;
				break;
			case 5 :
				m_CineThread.m_fps = 9;
				break;
			case 6 :
				m_CineThread.m_fps = 12;
				break;
			case 7 :
				m_CineThread.m_fps = 24;
				break;
			case 8 :
				m_CineThread.m_fps = 30;
				break;
		}
	}

	public boolean Cine(int dir, boolean bActiveOnly, boolean skipDraw)
	{
		if (m_CurrentSeries != null)
		{
			m_CurrentSeries.Cine(dir, bActiveOnly, false, skipDraw);
		}

		if (m_bShowCrosslink && !IsStack())
		{
			for (int i = 0; i < m_Series.length; i++)
			{
				if (m_Series[i] != m_CurrentSeries && m_Series[i].IsVisible())
				{
					m_Series[i].PaintSeries(getGraphics(), false);
				}
			}
		}
		return true;

	}

	public void ForceDraw()
	{
		if (m_CurrentSeries != null)
		{
			m_CurrentSeries.PaintSeries(getGraphics(), false);
		}
	}

	public void ChangeFrame(boolean dir)
	{
		if (m_CurrentSeries != null)
		{
			m_CurrentSeries.ChangeFrame(dir);
		}
	}

	public void SelectActiveImage(int x, int y)
	{
		if (m_Series == null)
		{
			return;
		}
		if (!IsStack())
		{
			int size = m_Series.length;
			if (size == 0)
			{
				return;
			}
			int sel = -1;
			for (int i = 0; i < m_Series.length; i++)
			{
				if (!m_Series[i].IsVisible())
				{
					continue; // series should be visible
				}
				if (m_Series[i].contains(x, y)) // Detect series rect that
				// contains (x,y)
				{
					sel = i;
					break;
				}
			}
			if (sel < 0)
			{
				return;
			}
			if (m_ActiveSeriesIndex != sel)
			{
				SetCurrentSeries(m_Series[sel]);
			}
		}
		if (m_CurrentSeries != null)
		{
			m_CurrentSeries.SelectActiveImage(x, y);
		}
		if (!IsStack())
		{
			m_bStPaintFrameOnly = !m_bShowCrosslink;
			paint(getGraphics());
		}
	}

	public Series GetCurrentSeries()
	{
		int i = GetActiveSerIndex();
		return (i >= 0) ? m_Series[i] : null;
	}

	public int GetActiveSerIndex()
	{
		if (m_CurrentSeries == null || m_Series == null)
		{
			return -1;
		}
		if (m_Series.length < 1)
		{
			return -1;
		}
		for (int i = 0; i < m_Series.length; i++)
		{
			if (m_Series[i] == m_CurrentSeries)
			{
				return i;
			}
		}
		return -1;
	}
	public void SetStatusString(String s)
	{
		dicomViewBase.m_StatusLabel.setText(s);
	}

	/**
	 * ******************************************************************** This
	 * method is to be called when image output area (m_rScreen rectangle in
	 * class ScreenMap) is changed, to repaint the entire background.
	 * ********************************************************************
	 */
	private void RepaintBackground(Rectangle r)
	{
		Graphics g = getGraphics();
		if (g == null)
		{
			return;
		}
		if (r == null)
		{
			r = new Rectangle(0, 0, getSize().width, getSize().height);
		}
		try
		{
			g.setColor(m_bkColor);
			// getBackground()); // set the drawing color to the
			// background color
			g.fillRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
			g.setColor(getForeground()); // reset the foreground color
		} catch (Exception e)
		{
		}
	}

	public void StartStudyLoading(DICOMRecord dr)
	{
		// cache previous study
		DisposeStudy();
		SetStudyDR(dr);
	}

	public void DisposeStudy()
	{
		m_ActiveSeriesIndex = 0;
		m_bToolRenderingActive = false;
		m_CurrentSeries = null;
		m_Series = null;
		m_SeriesVector.removeAllElements();
		dicomViewBase.EnableDirectionButtons(false);
		m_StudyDR.Clear();
		m_StudyNote = "";
		System.gc();
	}

	public void EndStudyLoading()
	{
		if (!m_bLoadingFromWorklist && m_UpdateInd >= 0) // current
		// series was
		// reloaded,
		// don't change
		// study layout
		{
			// SetCurrentSeries(m_CurrentSeries);
			UpdateStudyLayout(0);
			m_bLoadingFromWorklist = true;
			m_UpdateInd = -1;
			return;
		}
		m_bLoadingFromWorklist = true;
		m_UpdateInd = -1;
		if (m_SeriesVector.size() > 0)
		{
			m_Series = null;
			System.gc();
			m_Series = new Series[m_SeriesVector.size()];
			Series tmp;
			Series lastSeries = m_CurrentSeries;
			SortSeriesVector();
			{
				for (int i = 0; i < m_SeriesVector.size(); i++)
				{
					m_Series[i] = (Series) m_SeriesVector.elementAt(i);
				}
			}
			if (lastSeries != null)
			{
				SetCurrentSeries(lastSeries);
			} else
			{
				SetCurrentSeries(m_Series[0]);
			}
			// try
			{
				SetStudyLayout(LAYOUT_UNCHANGED, LAYOUT_UNCHANGED);
			}
			/*
			 * catch (OutOfMemoryError e) { int nDel=ProcessMemoryError();
			 * UpdateStudyLayout(nDel); ResetStudy(); }
			 */
			dicomViewBase.EnableImageButtons();
		}
		m_iPrevScreen = -9999;
		boolean cl = CanCrosslink();
		m_iSelectedSer = -1;
		dicomViewBase.getTool(Tool.REF).SetActive(cl);
		EnableCrosslink(cl);
	}

	private void SortSeriesVector() // sorting of series based on database
	// order
	{
		int sz = m_SeriesVector.size();
		if (sz < 2)
		{
			return;
		}
		int[] ind = new int[sz];
		int[] perm = new int[sz];
		int i, tmp;
		for (i = 0; i < sz; i++)
		{
			ind[i] = m_SeriesVector.elementAt(i).m_SeriesDR.GetEntityIndex();
			perm[i] = i;
		}
		for (i = 0; i < sz; i++)
		{
			for (int j = i + 1; j < sz; j++)
			{
				if (ind[perm[i]] > ind[perm[j]])
				{
					tmp = perm[i];
					perm[i] = perm[j];
					perm[j] = tmp;
				}
			}
		}
		Vector newSerVect = new Vector(sz);
		for (i = 0; i < sz; i++)
		{
			newSerVect.addElement((Series) m_SeriesVector.elementAt(perm[i]));
		}
		m_SeriesVector.removeAllElements();
		m_SeriesVector = newSerVect;
	}

	public void MarkSeriesForUpdate(int ind)
	{
		m_UpdateInd = ind;
		/*
		 * m_SeriesVector.removeElementAt(ind); Series[] new_vect = new
		 * Series[m_Series.length - 1]; m_Series[ind] = null; System.gc();
		 * for(int i = 0; i < ind; i++) { new_vect[i] = m_Series[i]; } for(int i
		 * = ind + 1; i < m_Series.length; i++) { new_vect[i - 1] = m_Series[i];
		 * } m_Series = new_vect;
		 */
	}

	public void OnSeriesUpdate()
	{
		if (m_CurrentSeries != null)
		{
			m_iPrevScreen = m_CurrentSeries.m_iScreen;
		}
	}

	/**
	 * ********************************************************************
	 * Series are loaded from the study interface. EntityIndex is an index in
	 * DICOMRecord used for identifying series number in study, default quality
	 * -1 means that it is left the same
	 */
	/*
	 * boolean LoadSeries(int EntityIndex, int quality) { DICOMRecord dr = new
	 * DICOMRecord(); dr.SetFromDR(m_CurrentSeries.m_SeriesDR); int es =
	 * dr.GetEntitySize(); dr.ClearBelowLevel(DICOMRecord.LevelStudy);
	 * dr.SetExtraQueryInfo(DICOMRecord.LevelSeries, EntityIndex); dr.m_QLevel =
	 * DICOMRecord.LevelSeries; dr.m_Quality = quality;
	 * m_DBManager.SetPreviewMode(false); // m_DBWindow.m_bPreview = false;
	 * 
	 * boolean res = m_DBManager.LoadSeries(dr); // m_DBWindow.ShowSeries(dr);
	 * m_DBManager.SetPreviewMode(true); // m_DBWindow.m_bPreview = true;
	 * EnableImageButtons(); return res; }
	 */
	public boolean ShowNext(int direction, boolean skipPaint)
	{
		if (m_bSerBrowse)
		{
			return ShowNextSeries(direction);
		} else
		{
			if (direction == 0)
			{
				ForceDraw();
			} else
			{
				Cine(direction, false, skipPaint);
			}
			dicomViewBase.EnableDirectionButtons(true);
			return true;
		}
	}

	/**
	 * ******************************************************************** Load
	 * series into active screen using direction buttons.
	 */
	public boolean ShowNextSeries(int direction) // based on virtual
	// screen index
	{
		int vd = GetValidDirections();
		if (((direction < 0) && ((vd & 2) == 0))
				|| ((direction > 0) && ((vd & 1) == 0)))
		{
			/*
			 * Utils.ShowMessageBox("", "Load next sermsg, type) ErrorMessage
			 * confirmDialog = new ErrorMessage(DBWindow
			 * .FindParentFrame(dicomViewBase.getParent()), Util
			 * .Trans("$download_next_series"), Util.Trans("Confirm"), true,
			 * true); confirmDialog.setVisible(true); if
			 * (confirmDialog.isM_bCancel() == true) { dicomViewBase.repaint();
			 * return false; }
			 */
			// dicomViewBase.repaint();
			// return LoadSeries(GetNextMissingEntity(direction), -1); // return
			// false;
		}
		if (m_CurrentSeries == null
				|| (m_StLayType == LAYOUT_AUTO && !m_bSerStack))
		{
			return true;
		}
		if (m_Series.length < 2)
		{
			return true;
		}
		int ind = m_ActiveSeriesIndex, n = 0;
		boolean res = true;
		do
		{
			ind = (ind + direction + m_Series.length) % m_Series.length;
			n++;
			res = m_Series[ind].IsVisible();
		} while (res && n < m_Series.length);
		if (res)
		{
			return true;
		}
		SwapSeries(m_ActiveSeriesIndex, ind);
		SetCurrentSeries(m_Series[ind]);
		m_Series[ind].SetLayout(LAYOUT_UNCHANGED);
		RepaintBackground(m_CurrentSeries);
		if (m_bShowCrosslink)
		{
			paint(getGraphics());
		} else
		{
			m_Series[ind].PaintSeries(getGraphics(), false);
		}
		return true;
	}

	private void SetVisibleCurrentSeries()
	{
		if (m_CurrentSeries.IsVisible())
		{
			return;
		}
		Series firstVisible = null;
		for (int i = 0; i < m_Series.length; i++)
		{
			if (m_Series[i].IsVisible())
			{
				firstVisible = m_Series[i];
				break;
			}
		}
		if (firstVisible != null)
		{
			SetCurrentSeries(firstVisible);
		}
	}

	private void SetCurrentSeries(Series ser)
	{
		m_CurrentSeries = ser;
		m_ActiveSeriesIndex = GetActiveSerIndex();
		boolean l = false, r = false;
		if (m_Series == null)
		{
			return;
		}
		if (m_Series.length < 1)
		{
			return;
		}
		int vd = GetValidDirections();
		l = (vd & 2) > 0;
		r = (vd & 1) > 0;
		dicomViewBase.SetPatientInfo(m_CurrentSeries.m_SeriesDR);
		if (m_Series.length < m_CurrentSeries.m_SeriesDR.GetEntitySize())
		{
			l = r = true;
		}
		if (m_bSerBrowse)
		{
			dicomViewBase.UpdateDirectionButtons(l, r);
		}
	}

	private int GetNextMissingEntity(int iDir)
	{
		int ei = m_CurrentSeries.m_SeriesDR.GetEntityIndex() - 1, cur_ei, es = m_CurrentSeries.m_SeriesDR
				.GetEntitySize(), old_ei, ind = 0;
		if (es <= 1)
		{
			return 0;
		}
		do
		{
			old_ei = ei;
			for (int i = 0; i < m_Series.length; i++)
			{
				if (m_Series[i] == m_CurrentSeries)
				{
					continue;
				}
				cur_ei = m_Series[i].m_SeriesDR.GetEntityIndex() - 1;
				if (((ei + iDir + es) % es) == cur_ei)
				{
					ei = cur_ei;
				}
			}
			ind++;
		} while ((old_ei != ei) && (ind < m_Series.length));
		return (old_ei == ei) ? ((ei + iDir + es) % es) + 1 : 0;
	}

	private int GetValidDirections()
	{
		if (m_Series == null)
		{
			return 0;
		}
		int vd = 0;
		for (int i = 0; i < m_Series.length; i++)
		{
			if (i < m_ActiveSeriesIndex && !m_Series[i].IsVisible())
			{
				vd = vd | 2;
			} else if (i > m_ActiveSeriesIndex && !m_Series[i].IsVisible())
			{
				vd = vd | 1;
			}
		}
		return vd;
	}

	public boolean IsPreviewMode()
	{
		if (m_bLoadingFromWorklist)
		{
			// if (dicomViewBase.m_DBWindow.getCheckPreview().isSelected())
			{
				return true;
			}
			// return false;
		} else
		{
			return false;
		}
	}

	/**
	 * ********************************************************************
	 * Frees some memory by deleting selected images or series from the study.
	 * returns nSerOld-nSerNew.
	 */

	public int OptimizeStudy()
	{
		if (m_SeriesVector.size() < 1)
			return 0;
		// Utils.ShowProgressWindow(GetParentFrame(),
		// "Freeing up some memory...");
		SetStatusString("Freeing some memory...");
		int nImagesInitial = CountImages(false);

		// first, convert invisible series
		for (Series s : m_SeriesVector)
		{
			if (s != m_CurrentSeries && !s.IsVisible())
				s.ConvertToPreview(false, false);
		}

		// second, convert invisible images for all series except for current
		if (nImagesInitial == CountImages(false))
		{
			for (Series s : m_SeriesVector)
				if (s != m_CurrentSeries)
					s.ConvertToPreview(false, true);
		} else
		{
			SetStatusString("Memory optimization complete");
			return nImagesInitial - CountImages(false);
		}

		// third, convert visible images in all series except for current
		if (nImagesInitial == CountImages(false))
		{
			for (Series s : m_SeriesVector)
				if (s != m_CurrentSeries)
					s.ConvertToPreview(false, false);
		} else
		{
			SetStatusString("Memory optimization complete");
			return nImagesInitial - CountImages(false);
		}

		// Fourth, convert some invisible images on current series.
		if (nImagesInitial == CountImages(false))
		{
			m_CurrentSeries.ReduceMemoryLoad(0.25);
		} else
		{
			SetStatusString("Memory optimization complete");
			return nImagesInitial - CountImages(false);
		}

		// Fifth, convert visible images current series
		if (nImagesInitial == CountImages(false))
		{
			m_CurrentSeries.ConvertToPreview(true, false);
		} else
		{
			SetStatusString("Memory optimization complete");
			return nImagesInitial - CountImages(false);
		}

		// finally, convert current image of current series
		if (nImagesInitial == CountImages(false))
		{
			m_CurrentSeries.ConvertToPreview(true, true);
		}
		if (nImagesInitial == CountImages(false))
			SetStatusString("Memory optimization failed");
		// Utils.HideProgressWindow();
		SetStatusString("Memory optimization complete");
		return nImagesInitial - CountImages(false);
	}

	private int CountImages(boolean bPreview)
	{
		int res = 0;
		for (Series s : m_SeriesVector)
			res += s.CountImages(bPreview);
		return res;
	}

	private void SwapSeries(int ind0, int ind1)
	{
		SwapSeries(m_Series[ind0], m_Series[ind1]);
	}

	private void SwapSeries(Series ser0, Series ser1)
	{
		int tmp = ser0.m_iScreen;
		ser0.m_iScreen = ser1.m_iScreen;
		ser1.m_iScreen = tmp;
		Rectangle tmp_rect = new Rectangle(ser0);
		ser0.setBounds(ser1);
		ser1.setBounds(tmp_rect);
	}

	public void StartSeriesLoading(DICOMRecord dr)
	{
		Series ser;
		if (m_UpdateInd < 0)
		{
			ser = new Series(this, false);
		} else
		{
			ser = m_SeriesVector.elementAt(m_UpdateInd);
		}
		ser.RemoveAll();
		ser.SetDR(dr);
		m_StudyDR.SetFromDR(dr);
		m_StudyDR.ClearBelowLevel(DICOMRecord.LevelStudy);
		SetCurrentSeries(ser);
		if (m_UpdateInd < 0)
		{
			m_SeriesVector.addElement(ser);
		}
	}

	public void EndSeriesLoading()
	{
		if (m_CurrentSeries != null)
		{
			m_CurrentSeries.EndLoading();
		}
		// if(m_DBWindow.m_bPreview == false)m_CurrentSeries.m_bPreview =
		// false;
		// else m_CurrentSeries.m_bPreview =
		// m_DBWindow.m_checkPreview.getState();
	}

	private void UpdateStudyLayout(int nDeleted)
	{
		if (nDeleted > 0)
		{
			SetStudyLayout(LAYOUT_UNCHANGED, LAYOUT_UNCHANGED);
			return;
		}
		for (int i = 0; i < m_Series.length; i++)
		{
			m_Series[i].SetLayout(LAYOUT_UNCHANGED);
		}
		{
			RepaintBackground(null);
			paint(getGraphics());
		}
	}

	public void SetStudyLayout(int codeStudy, int codeSeries)
	{
		SetStudyLayoutEx(codeStudy, codeSeries);
	}

	private void SetStudyLayoutEx(int codeStudy, int codeSeries)
	{
		if (m_Series==null || m_Series.length == 0)
		{
			return;
		}
		if (codeStudy != LAYOUT_UNCHANGED)
		{
			m_StLayType = codeStudy;
		}
		double count = (double) m_Series.length;
		double icount;
		double shift = 5;
		Dimension thisSize = getSize();
		double width = (double) thisSize.width;
		double height = (double) thisSize.height;
		if (m_StClip == null)
		{
			m_StClip = new Rectangle(thisSize);
		}
		int nc = 1, nr = 1;
		icount = count;
		switch (m_StLayType)
		{
			case LAYOUT_AUTO :
				nc = (int) Math.ceil(Math.sqrt(count));
				nr = (int) Math.ceil(count / nc);
				break;
			case LAYOUT_STACK :
				nr = 1;
				nc = 1;
				break;
			case LAYOUT_ONE_TWO :
				nr = 1;
				nc = 2;
				break;
			case LAYOUT_TWO_ONE :
				nr = 2;
				nc = 1;
				break;
			case LAYOUT_TWO_TWO :
				nr = 2;
				nc = 2;
				break;
			case LAYOUT_TWO_THREE :
				nr = 2;
				nc = 3;
				break;
			case LAYOUT_THREE_TWO :
				nr = 3;
				nc = 2;
				break;
			case LAYOUT_THREE_THREE :
				nr = 3;
				nc = 3;
				break;
			case LAYOUT_FOUR_FOUR :
				nr = 4;
				nc = 4;
				break;
		}
		if (IsStack())
		{
			nr = 1;
			nc = 1;
		}

		m_nSerScreens = nc * nr;
		int w = (int) Math.max(4, width / nc) - (int) (2 * shift);
		int h = (int) Math.max(4, height / nr) - (int) (2 * shift);
		int dx = (int) Math.max(1, (width - w * nc) / (nc + 1));
		int dy = (int) Math.max(1, (height - h * nr) / (nr + 1));
		int current = 0;
		for (int i = 0; i < nr; i++)
		{
			for (int j = 0; j < nc; j++)
			{
				if (current < count)
				{
					if (!IsStack())
					{
						m_Series[current].setBounds(dx + j * (dx + w), dy + i
								* (dy + h), w, h);
					} else
					{
						m_Series[current].setBounds(dx, dy, w, h);
					}
					m_Series[current].SetLayout(codeSeries);
				} else
				{
					break;
				}
				current++;
			}
		}
		for (int i = 0; i < m_Series.length; i++)
		{
			m_Series[i].m_iScreen = i;
		}
		Series ser = null;
		if (!IsStack())
		{
			if (m_iPrevScreen >= 0
					&& m_iPrevScreen < m_nSerScreens
					&& ((m_CurrentSeries.m_iScreen >= m_nSerScreens) || (m_CurrentSeries.m_iScreen < 0))
					&& ((ser = GetSeriesFromScreen(m_iPrevScreen)) != null))
			{
				SwapSeries(m_CurrentSeries, ser);
				SetCurrentSeries(m_CurrentSeries);
				m_CurrentSeries.SetLayout(codeSeries);
				ser.SetLayout(codeSeries);
			} else if (m_UpdateInd < 0)
			{
				SetVisibleCurrentSeries();
			}
		} else
		{
			SwapSeries(0, m_ActiveSeriesIndex);
			SetCurrentSeries(m_CurrentSeries);
			m_CurrentSeries.SetLayout(codeSeries);
		}
		UpdateArrows();
		RepaintBackground(null);
		paint(Study.this.getGraphics());
	}

	private Series GetSeriesFromScreen(int screen)
	{
		for (int i = 0; i < m_Series.length; i++)
		{
			if (m_Series[i].m_iScreen == screen)
			{
				return m_Series[i];
			}
		}
		return null;
	}

	public void EnableCrosslink(boolean bEnable)
	{
		m_bShowCrosslink = bEnable;
		update(this.getGraphics());
	}

	public boolean CanCrosslink()
	{
		if (m_Series == null)
		{
			return false;
		}
		if (m_Series.length < 2)
		{
			return false;
		}
		return (GetStudyCrosslink() != null);
	}

	public Object GetStudyCrosslink()
	{
		if (null == m_CurrentSeries)
		{
			return null;
		}
		return m_CurrentSeries.GetSeriesCrosslink();
	}

	public void Flip(int dir)
	{
		if (m_CurrentSeries != null)
		{
			m_CurrentSeries.Flip(dir);
		}
	}

	/**
	 * ********************************************************************
	 * Shifts displayed part of zoomed image in the specified direction
	 * ********************************************************************
	 */
	public boolean ShiftImage(int shiftX, int shiftY, boolean bAll)
	{
		return m_CurrentSeries.ShiftImage(shiftX, shiftY, bAll);
	}

	public void ResetTool()
	{
		m_CurrentSeries.ResetTool();
	}

	public void RepaintClipRegion(Graphics g, Vector clip)
	{
		if (g == null)
		{
			return;
		}
		Rectangle r;
		for (int i = 0; i < clip.size(); i++)
		{
			r = (Rectangle) (clip.elementAt(i));
			g.setClip(r.x, r.y, r.width, r.height);
			paint(g);
		}
		g.setClip(m_StClip);
	}

	public void InitPopupMenu(int x, int y)
	{
		m_iSelectedSer = -1;
		for (int i = 0; i < m_Series.length; i++)
		{
			if (!m_Series[i].IsVisible())
			{
				continue;
			}
			if (m_Series[i].contains(x, y))
			{
				m_iSelectedSer = i;
				break;
			}
		}
		if (m_iSelectedSer < 0)
		{
			return;
		}
		CheckboxMenuItem cmi;
		dicomViewBase.getPopupMenu().removeAll();
		if (m_Series[m_iSelectedSer].m_SeriesDR.m_Quality > 0)
		{
			cmi = new CheckboxMenuItem(Util.Trans("Reload lossless"), false);
			cmi.addItemListener(dicomViewBase.getMenuListener());
			dicomViewBase.getPopupMenu().add(cmi);
			m_PopupOffset = 1;
		} else
		{
			m_PopupOffset = 0;
		}
		String str;
		dicomViewBase.getPopupMenu().add(Util.Trans("Show series") + ":");
		dicomViewBase.getPopupMenu().addSeparator();
		for (int i = 0; i < m_Series.length; i++)
		{
			str = m_Series[i].m_SeriesDR.GetString(DICOMRecord.iSerDescr);
			if (str.length() < 2)
			{
				str = Util.Trans("Series") + " " + i;
			}
			cmi = new CheckboxMenuItem(str, m_iSelectedSer == i);
			cmi.addItemListener(dicomViewBase.getMenuListener());
			dicomViewBase.getPopupMenu().add(cmi);
		}
		dicomViewBase.getPopupMenu().show(this, x, y);
	}

	public boolean ProcessMouseClick(MouseEvent me)
	{
		if (m_Series == null)
		{
			return false;
		}
		Point pt = me.getPoint();
		Series ser = null;
		boolean bRestoreSeries = false;
		for (int i = 0; i < m_Series.length; i++)
		{
			if (m_Series[i].contains(pt) && m_Series[i].IsVisible())
			{
				ser = m_Series[i];
				for (DicomImage im : ser.getM_ImageVector())
				{
					if ((me.getModifiers() & InputEvent.ALT_MASK) != 0)
					{
						bRestoreSeries = true;
						break;
					}
				}
				break;
			}
		}
		if (ser == null)
		{
			return false;
		}
		if (bRestoreSeries)
		{
			ser.RestoreFromPreview();
			return true;
		}
		if (ser.getM_ImageVector().size() < 1 || !ser.m_bPreview)
		{
			return false;
		}
		if (!ser.getM_ImageVector().get(0).GetRect(getGraphics(), "Load All",
				DicomImage.LEFT | DicomImage.BOTTOM).contains(pt))
		{
			return false;
		}
		/*
		 * ErrorMessage confirmDialog = new ErrorMessage(DBWindow
		 * .FindParentFrame(dicomViewBase.getParent()), Util
		 * .Trans("$reload_full") + " (" +
		 * ser.m_SeriesDR.GetString(DICOMRecord.iSerNumImages) + " " +
		 * Util.Trans("images") + ")", Util.Trans("Confirm"), true, true);
		 * confirmDialog.setVisible(true); if (confirmDialog.isM_bCancel() ==
		 * true) { dicomViewBase.repaint(); return false; }
		 */
		// LoadSeries(ser.m_SeriesDR.GetEntityIndex(), -1);
		return true;
	}

	public void ReloadSeries()
	{
		if (m_iSelectedSer >= 0)
		{
			// LoadSeries(m_Series[m_iSelectedSer].m_SeriesDR.GetEntityIndex(),
			// 0);
		}
	}

	public void SwapSeries(int ind)
	{
		if (m_iSelectedSer < 0 || (m_Series.length < 2) || ind < 2)
		{
			return;
		}
		int ind0 = m_iSelectedSer, ind1 = ind - 2;
		if (ind0 == ind1)
		{
			return;
		}
		SwapSeries(ind0, ind1);
		SetCurrentSeries(m_Series[ind1]);
		m_Series[ind0].SetLayout(LAYOUT_UNCHANGED);
		m_Series[ind1].SetLayout(LAYOUT_UNCHANGED);
		{
			if (m_Series[ind0].IsVisible())
			{
				RepaintBackground(m_Series[ind0]);
				m_Series[ind0].PaintSeries(getGraphics(), false);
			}
			if (m_Series[ind1].IsVisible())
			{
				RepaintBackground(m_Series[ind1]);
				m_Series[ind1].PaintSeries(getGraphics(), false);
			}
		}
		m_iSelectedSer = -1;
	}

	synchronized public void InitTool(Point pt, int ToolCode)
	{
		m_CurrentSeries.InitTool(pt, ToolCode);
	}

	public void DrawTool(Point pt)
	{
		m_CurrentSeries.DrawTool(pt);
	}

	/**
	 * ********************************************************************
	 * Changing contrast and brightness of displaying images
	 * ********************************************************************
	 */
	public void WindowLevel(int shiftX, int shiftY, boolean bAll)
	{
		m_CurrentSeries.WindowLevel(shiftX, shiftY, bAll);
	}

	/**
	 * ********************************************************************
	 * Increases or decreases image zoom, calling resizing routines
	 * ********************************************************************
	 */
	public void ChangeZoom(boolean IsIncrease, boolean bAll)
	{
		if (m_CurrentSeries != null)
		{
			m_CurrentSeries.ChangeZoom(IsIncrease, bAll);
		}
	}

	/**
	 * ******************************************************************** This
	 * is called whenever canvas needs to be repainted. Does not repaint
	 * background by default.
	 * ********************************************************************
	 */
	public void update(Graphics g)
	{
		if (m_bUpdateBackground)
		{
			RepaintBackground(null);
		}
		paint(g);
	}

	/**
	 * ********************************************************************
	 * Painting rectangle from image to rectangle from canvas, as well as
	 * borders and adequate frames.
	 * ********************************************************************
	 */
	public void paint(Graphics g)
	{
		if (m_Series == null || g == null)
		{
			return;
		}
		// try
		{
			for (int i = 0; i < m_Series.length; i++)
			{
				if (m_Series[i].IsVisible())
				{
					m_Series[i].PaintSeries(g, false);
				}
			}
		}
		/*
		 * catch(OutOfMemoryError e) { int nDel=ProcessMemoryError();
		 * UpdateStudyLayout(nDel); return; }
		 */
		Dimension d;
		d = getSize();
		int ScrWid, ScrHt;
		ScrWid = d.width;
		ScrHt = d.height;
		g.setColor(Color.black);
		g.drawLine(0, 1, ScrWid, 1);
		g.setColor(Color.white);
		g.drawLine(0, ScrHt - 1, ScrWid, ScrHt - 1);
		m_bStPaintFrameOnly = false;
	}

} // end of class Study
