package org.nrg.xnd.tools.ImageViewer;

import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.nrg.xnd.utils.dicom.DICOMRecord;
import org.nrg.xnd.utils.dicom.SeriesRecord;

public class ImageViewer
{
	private TextArea m_PatInfo;
	private TextArea m_ExPatInfo;
	public Label m_nImageLabel;
	public Label m_nSerLabel;
	private Color m_Background = Color.lightGray;
	private long m_LastTimeClicked = 0;
	public Label m_StatusLabel, m_ApplInfoLabel;
	private Study m_Study; // = new Study(); //the one and only Study object

	public int m_DragXPos, m_DragYPos;
	private Tool m_WList, m_Zoom, m_Flip, m_WL, m_Layout, m_CurrentTool,
			m_Reset, m_Cine, m_Info, m_Ruler, m_Ref, m_ROI, m_PrevSeries,
			m_NextSeries, m_Notes, m_ImSer;
	private TreeMap<Integer, Tool> m_tools=new TreeMap<Integer,Tool>();

	private PopupMenu m_pm = new PopupMenu();
	private MenuListener m_MenuListener = new MenuListener();
	private UniKey m_UniKey = new UniKey();
	private byte[] m_MemBuf = new byte[32735];
	private Container m_C;
	private WorklistManager m_DBManager;
	private boolean m_bToolbar=true;

	public static final byte TOOL_WLIST = 0;

	public WorklistManager getDBManager()
	{
		return m_DBManager;
	}
	public MenuListener getMenuListener()
	{
		return m_MenuListener;
	}
	public PopupMenu getPopupMenu()
	{
		return m_pm;
	}
	public Tool getTool(int id)
	{
		return m_tools.get(Integer.valueOf(id));
	}
	public void setCurrentTool(Tool t)
	{
		m_CurrentTool = t;
	}
	public ImageViewer(Container container, WorklistManager ilm, boolean bShowToolbar)
	{
		m_C = container;
		m_DBManager = ilm;
		m_bToolbar=bShowToolbar;
		Init();
	}
	public Study getStudy()
	{
		return m_Study;
	}
	public Container getContainer()
	{
		return m_C;
	}
	public Tool getCurrentTool()
	{
		return m_CurrentTool;
	}
	public void destroy()
	{
		m_Study.DisposeStudy();
		try
		{
			File tmp_dir = new File(System.getProperty("java.io.tmpdir"));
			// delete temp files
			FileFilter ff = new FileFilter()
			{
				@Override
				public boolean accept(File f)
				{
					return (f.getName().startsWith("wpimg") && f.getName()
							.endsWith(".tmp"));
				}
			};
			for (File f : tmp_dir.listFiles(ff))
			{
				f.delete();
			}
		} finally
		{
		}
	}
	public void EnableTool(byte toolId, boolean bEnable)
	{
		switch (toolId)
		{
			case TOOL_WLIST :
				m_WList.SetEnabled(bEnable);
		}
	}

	public Color GetBackground()
	{
		return m_Background;
	}
	public void Init()
	{
		m_Study = new Study(this);
		m_C.setBackground(m_Background);
		m_C.setLayout(null);
		Rectangle r = m_C.getBounds();
		m_C.setSize(Math.max(700, r.width), Math.max(500, r.height));

		m_PatInfo = new java.awt.TextArea("", 0, 3,
				TextArea.SCROLLBARS_NONE);
		m_ExPatInfo = new java.awt.TextArea("", 0, 3,
				TextArea.SCROLLBARS_HORIZONTAL_ONLY);
		m_nImageLabel = new java.awt.Label();
		m_nSerLabel = new java.awt.Label();
						
		m_PatInfo.setEditable(false);
		m_ExPatInfo.setEditable(false);
		m_PatInfo.setFont(new Font("Dialog", Font.PLAIN, 10));
		m_ExPatInfo.setFont(new Font("Dialog", Font.PLAIN, 12));
		if(m_bToolbar)
		{			
			m_C.add(m_PatInfo);
			m_C.add(m_ExPatInfo);
		}
		m_nImageLabel.setText("");
		m_nSerLabel.setText("");
		m_nImageLabel.setAlignment(java.awt.Label.RIGHT);
		m_nSerLabel.setAlignment(java.awt.Label.RIGHT);
		if(m_bToolbar)
		{			
			m_C.add(m_nImageLabel);
			m_C.add(m_nSerLabel);
		}
		m_nSerLabel.setForeground(new java.awt.Color(30, 30, 115));
		m_nSerLabel.setFont(new Font("Dialog", Font.BOLD, 12));
		m_nImageLabel.setForeground(new java.awt.Color(30, 30, 115));
		m_nImageLabel.setFont(new Font("Dialog", Font.BOLD, 12));
		m_StatusLabel = new Label();
		ClassLoader cl = this.getClass().getClassLoader();
		String is = "/../icons/imview/";
			//"icons/imview/";//Util.m_Lang > 0 ? "img_ru/" : "img_en/";
		URL res=cl.getResource(is + "sb0.gif");
		try
		{
			Image sI0 = ImageIO.read(cl.getResourceAsStream(is + "sb0.gif")), wrk0 = ImageIO
					.read(cl.getResourceAsStream(is + "wrk0.gif")), zm0 = ImageIO
					.read(cl.getResourceAsStream(is + "zm0.gif")), wl0 = ImageIO
					.read(cl.getResourceAsStream(is + "wl0.gif")), inf0 = ImageIO
					.read(cl.getResourceAsStream(is + "info0.gif")), all0 = ImageIO
					.read(cl.getResourceAsStream(is + "all0.gif")), cin0 = ImageIO
					.read(cl.getResourceAsStream(is + "cin0.gif")), rst0 = ImageIO
					.read(cl.getResourceAsStream(is + "rst0.gif")), sz0 = ImageIO
					.read(cl.getResourceAsStream(is + "sz0.gif")), ref0 = ImageIO
					.read(cl.getResourceAsStream(is + "ref0.gif")), prv0 = ImageIO
					.read(cl.getResourceAsStream(is + "prv0.gif")), roi0 = ImageIO
					.read(cl.getResourceAsStream(is + "roi0.gif")), nxt0 = ImageIO
					.read(cl.getResourceAsStream(is + "nxt0.gif")), not0 = ImageIO
					.read(cl.getResourceAsStream(is + "not0.gif")), flp0 = ImageIO
					.read(cl.getResourceAsStream(is + "flp0.gif")), imser1 = ImageIO
					.read(cl.getResourceAsStream(is + "im.gif")), imser2 = ImageIO
					.read(cl.getResourceAsStream(is + "ser.gif")), sideSmall = ImageIO
					.read(cl.getResourceAsStream(is + "ss.gif"));
			
			m_WList = new Tool(this, wrk0, false, null, Tool.WORKLIST);
			m_tools.put(new Integer(Tool.WORKLIST), m_WList);
			m_Zoom = new Tool(this, zm0, true, null, Tool.ZOOM);
			m_tools.put(new Integer(Tool.ZOOM), m_Zoom);
			m_WL = new Tool(this, wl0, true, null, Tool.WL);
			m_tools.put(new Integer(Tool.WL), m_WL);
			m_Layout = new Tool(this, all0, true, sI0, Tool.LAYOUT);
			m_tools.put(new Integer(Tool.LAYOUT), m_Layout);
			m_Cine = new Tool(this, cin0, true, null, Tool.CINE);
			m_tools.put(new Integer(Tool.CINE), m_Cine);
			m_Info = new Tool(this, inf0, Tool.INFO);
			m_tools.put(new Integer(Tool.INFO), m_Info);
			m_Reset = new Tool(this, rst0, false, null, Tool.RESET);
			m_tools.put(new Integer(Tool.RESET), m_Reset);
			m_Ruler = new Tool(this, sz0, Tool.SIZE);
			m_tools.put(new Integer(Tool.SIZE), m_Ruler);
			m_Ref = new Tool(this, ref0, Tool.REF);
			m_tools.put(new Integer(Tool.REF), m_Ref);
			m_ROI = new Tool(this, roi0, Tool.ROI);
			m_tools.put(new Integer(Tool.ROI), m_ROI);
			m_PrevSeries = new Tool(this, prv0, false, null, Tool.PREV);
			m_tools.put(new Integer(Tool.PREV), m_PrevSeries);
			m_NextSeries = new Tool(this, nxt0, false, null, Tool.NEXT);
			m_tools.put(new Integer(Tool.NEXT), m_NextSeries);
			m_Notes = new Tool(this, not0, false, null, Tool.NOTES);
			m_tools.put(new Integer(Tool.NOTES), m_Notes);
			m_Flip = new Tool(this, flp0, false, sI0, Tool.FLIP);
			m_tools.put(new Integer(Tool.FLIP), m_Flip);
			m_ImSer = new Tool(this, imser1, false, sideSmall, Tool.IMSER, true);
			m_tools.put(new Integer(Tool.IMSER), m_ImSer);
			m_ImSer.SetImDown(imser2);

			if(m_bToolbar)
			{
				m_C.add(m_WList);
				m_C.add(m_Zoom);
				m_C.add(m_Flip);
				m_C.add(m_WL);
				m_C.add(m_Layout);
				m_C.add(m_PrevSeries);
				m_C.add(m_NextSeries);
				m_C.add(m_Notes);
				m_C.add(m_Cine);
				m_C.add(m_Info);
				m_C.add(m_Reset);
				m_C.add(m_Ruler);
				m_C.add(m_Ref);
				m_C.add(m_ROI);
				m_C.add(m_ImSer);
			}
			m_C.add(m_StatusLabel);			
			m_StatusLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			m_StatusLabel.setText(Util.Trans("Ready"));
			m_StatusLabel.setAlignment(Label.LEFT);

//			m_ApplInfoLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
// 			m_ApplInfoLabel.setText("AlgoM TeleradWS, ver. " + m_version);
//			m_ApplInfoLabel.setAlignment(Label.RIGHT);

			m_Zoom.SetEnabled(false);
			m_Ruler.SetEnabled(false);
			m_Ref.SetEnabled(false);
			m_ROI.SetEnabled(false);
			m_Flip.SetEnabled(false); // m_Pan.SetEnabled(false);
			m_WL.SetEnabled(false);
			m_Reset.SetEnabled(false);
			m_Notes.SetEnabled(false);
			m_Layout.SetEnabled(false);
			m_Cine.SetEnabled(false);
			m_Info.SetEnabled(false);
			m_PrevSeries.SetEnabled(false);
			m_NextSeries.SetEnabled(false);
			m_ImSer.SetEnabled(false);
		} catch (Exception e)
		{
			Util.LogError("Error while initializing controls, Code 1", e);
		}
		m_C.add(m_Study);
		// m_MenuListener = new MenuListener();
		UniMouseMotion aSymMouseMotion = new UniMouseMotion();
		m_Study.addMouseMotionListener(aSymMouseMotion);
		m_C.addKeyListener(m_UniKey);
		m_Study.addKeyListener(m_UniKey);
		m_PatInfo.addKeyListener(m_UniKey);
		m_ExPatInfo.addKeyListener(m_UniKey);
		UniMouse aUniMouse = new UniMouse();
		m_Study.addMouseListener(aUniMouse);
		m_PatInfo.addMouseListener(aUniMouse);
		m_ExPatInfo.addMouseListener(aUniMouse);
		if(m_bToolbar)
		{				
			m_Study.add(m_pm);
		}
		m_ExPatInfo.setVisible(false);
		UpdateSize(m_C.getSize());
	}

	public void UpdateSize(Dimension sz)
	{
		Rectangle r = m_C.getBounds();
		int y0 = 2, gapWid = 1, gapHt = 2;
		int x = 10, infoWid = 228;
		if (r.width <= 800)
		{
			gapWid = 1;
			x = 5;
			infoWid = 200;
		}
		m_WList.setLocation(x, y0);
		x += m_WList.m_wid + gapWid;
		m_WL.setLocation(x, y0);
		x += m_WL.m_wid + gapWid;
		m_Cine.setLocation(x, y0);
		x += m_Cine.m_wid + gapWid;
		m_Info.setLocation(x, y0);
		x += m_Info.m_wid + gapWid;
		m_Layout.setLocation(x, y0);
		x += m_Layout.m_wid + gapWid;
		m_Zoom.setLocation(x, y0);
		x += m_Zoom.m_wid + gapWid;
		m_Flip.setLocation(x, y0);
		x += m_Flip.m_wid + gapWid;
		m_ROI.setLocation(x, y0);
		x += m_ROI.m_wid + gapWid;
		m_Ruler.setLocation(x, y0);
		x += m_Ruler.m_wid + gapWid;
		m_Ref.setLocation(x, y0);
		x += m_Ruler.m_wid + gapWid;

		if (r.width <= 700)
		{
			x = 5;
			y0 += m_Notes.m_ht + gapHt;
		}
		m_Notes.setLocation(x, y0);
		x += m_Notes.m_wid + gapWid;
		m_Reset.setLocation(x, y0);
		x += m_Reset.m_wid + 20;
		m_PrevSeries.setLocation(x, y0);
		m_ImSer.setLocation(x, y0 + m_PrevSeries.m_ht);
		x += m_PrevSeries.m_wid + gapWid;
		m_NextSeries.setLocation(x, y0);

		x = r.width - infoWid;
		y0 = 2;
		m_PatInfo.setBounds(x, y0 - 1, infoWid, m_WL.m_ht + 2);
		m_ExPatInfo.setBounds(x + infoWid - 322, y0 + m_WL.m_ht + gapHt, 322,
				298);
		x -= 80 + 5;
		m_nSerLabel.setBounds(x, 2, 80, 22);
		m_nImageLabel.setBounds(x - 5, 23, 85, 22);

		// m_LoginLabel.setBounds(r.width - 200, r.height - 19, 195, 15);
		m_StatusLabel.setBounds(5, r.height - 20, r.width - 205, 20);
//		m_ApplInfoLabel.setBounds(r.width - 200, r.height - 20, 200, 20);
		if(m_bToolbar)
		{
			if (r.width <= 640)
			{
				m_Study.setBounds(0, 2 * (m_WL.m_ht + y0 + gapHt), r.width,
						r.height - 20 - 2 * (m_WL.m_ht + y0 + gapHt));
			} else
			{
				m_Study.setBounds(0, m_WL.m_ht + y0 + gapHt, r.width, r.height - 20
						- (m_WL.m_ht + y0 + gapHt));
	
			}
		}
		else
		{
			m_Study.setBounds(0,gapHt+y0,r.width,r.height-20-y0-gapHt);
		}
		m_Study.SetStudyLayout(Study.LAYOUT_UNCHANGED, Study.LAYOUT_UNCHANGED);
	}

	public void OptimizeMemory(Throwable e)
	{
		m_Study.OptimizeMemory(e);
	}
	public void SetPatientInfo(DICOMRecord dr)
	{
		String tmp;
		boolean bHidePHI = m_DBManager.IsGuestMode();
		m_PatInfo.setText("");
		m_PatInfo.append(Util.Trans("PatID") + " : " + dr.GetString(DICOMRecord.iPatID)
				+ "\n");
		if (bHidePHI)
		{
			m_PatInfo.append(Util.Trans("PatName") + ": <<Hidden>>\n");
		} else
		{
			tmp = Util.Trans("PatName") + " : " + dr.GetString(DICOMRecord.iPatName)
					+ "\n";
			tmp = tmp.replace('^', ' ');
			m_PatInfo.append(tmp);
		}
		tmp = "";
		if (dr.m_StudyDate.length() == 8)
		{
			tmp = dr.m_StudyDate.substring(4, 6) + "/"
					+ dr.m_StudyDate.substring(6, 8) + "/"
					+ dr.m_StudyDate.substring(0, 4);
		} else
		{
			tmp = dr.m_StudyDate;
		}
		m_PatInfo.append(Util.Trans("Study Date") + " : " + tmp);
		try
		{
			m_PatInfo.setCaretPosition(0);
		} catch (Exception e)
		{
		}
		m_Study.m_StudyNote = dr.GetString(DICOMRecord.iStDescr);
		m_ExPatInfo.setText("");
		tmp = dr.GetString(DICOMRecord.iPatID);
		tmp = tmp.replace('^', ' ');
		m_ExPatInfo.append(Util.Trans("PatID") + " : " + tmp);
		if (bHidePHI)
		{
			m_ExPatInfo.append("\n" + Util.Trans("PatName") + ": <<"
					+ Util.Trans("Hidden") + ">>");
		} else
		{
			tmp = dr.GetString(DICOMRecord.iPatName);
			tmp = tmp.replace('^', ' ');
			m_ExPatInfo.append("\n" + Util.Trans("PatName") + " : " + tmp);
		}
		if (dr.m_StudyDate.length() == 8)
		{
			tmp = dr.m_StudyDate.substring(4, 6) + "/"
					+ dr.m_StudyDate.substring(6, 8) + "/"
					+ dr.m_StudyDate.substring(0, 4);
		} else
		{
			tmp = dr.m_StudyDate;
		}
		m_ExPatInfo.append("\n" + Util.Trans("Study Date") + " : " + tmp);
		if (dr.m_StudyTime.length() == 6)
		{
			tmp = dr.m_StudyTime.substring(0, 2) + ":"
					+ dr.m_StudyTime.substring(2, 4) + ":"
					+ dr.m_StudyTime.substring(4, 6);
		} else
		{
			tmp = dr.m_StudyTime;
		}
		m_ExPatInfo.append("\n" + Util.Trans("Study Time") + " : " + tmp);
		if (dr.m_BirthDate.length() == 8)
		{
			tmp = dr.m_BirthDate.substring(0, 4);
		} else
		{
			tmp = dr.m_BirthDate;
		}
		m_ExPatInfo.append("\n" + Util.Trans("Year of birth") + " : " + tmp);
		m_ExPatInfo.append("\n" + Util.Trans("PatSex") + " : "
				+ dr.GetString(DICOMRecord.iPatSex));
		if (dr.GetString(DICOMRecord.iPatComments) != null)
			m_ExPatInfo.append("\n" + Util.Trans("Patient comments") + ": "
					+ dr.GetString(DICOMRecord.iPatComments));
		m_ExPatInfo.append("\n\nStInstUID : " + dr.GetString(DICOMRecord.iStInstUID));
		m_ExPatInfo.append("\nStID : " + dr.GetString(DICOMRecord.iStID));
		m_ExPatInfo.append("\n" + Util.Trans("StModalities") + " : "
				+ dr.GetString(DICOMRecord.iStModalities));
		m_ExPatInfo.append("\n\nSerInstUID : " + dr.GetString(DICOMRecord.iSerInstUID));
		m_ExPatInfo.append("\n" + Util.Trans("SerModality") + " : "
				+ dr.GetString(DICOMRecord.iSerModality));
		m_ExPatInfo.append("\n" + Util.Trans("SerNumber") + " : "
				+ dr.GetString(DICOMRecord.iSerNumber));
		m_ExPatInfo.append("\n\nImgSOPInstUID : "
				+ dr.GetString(DICOMRecord.iImgSOPInstUID));
		m_ExPatInfo.append("\n" + Util.Trans("ImgNumber") + " : "
				+ dr.GetString(DICOMRecord.iImgNumber));
		if (!bHidePHI)
		{
			m_ExPatInfo.append("\n" + Util.Trans("ImgInstit") + " : "
					+ dr.GetString(DICOMRecord.iImgInstit));
		}
		try
		{
			m_ExPatInfo.setCaretPosition(0);
		} catch (Exception e)
		{
		}
		// ?? m_Study.SetStudyDR(dr);
	}
	public Container GetContainer()
	{
		return m_C;
	}
	public void EnableImageButtons()
	{
		m_Ruler.SetEnabled(true);
		m_Ref.SetEnabled(m_Study.CanCrosslink());
		m_ROI.SetEnabled(true);
		m_Zoom.SetEnabled(true);
		m_ImSer.SetEnabled(true);
		m_WL.SetEnabled(true);
		// ??
		m_WList.SetEnabled(false);
		m_Reset.SetEnabled(true);
		m_Flip.SetEnabled(true);
		m_Info.SetEnabled(true);

		// m_Notes.SetEnabled(m_DBWindow.m_iUserRights >= DBWindow.RightsModify
		// || m_Usr.compareTo("guest") == 0);

		if (m_Study.IsMultiImage() || m_Study.IsMultiSeries())
		{
			m_Layout.SetEnabled(true);
			m_Layout.SetActive(true);
			m_Cine.SetEnabled(true);
		} else
		{
			m_Layout.SetActive(false);
			m_Layout.SetEnabled(false);
			m_Cine.SetActive(false);
			m_Cine.SetEnabled(false);
		}
		m_Study.SetBrowseMode();
	}

	public void EnableDirectionButtons(boolean bEnable)
	{
		if (m_NextSeries != null)
			m_NextSeries.SetEnabled(bEnable);
		if (m_PrevSeries != null)
			m_PrevSeries.SetEnabled(bEnable);
	}

	public void UpdateDirectionButtons(int EntityIndex, int EntitySize)
	{
		EnableDirectionButtons(false);
		if (EntityIndex < 1 || EntityIndex > EntitySize || EntitySize == 1)
		{
			return;
		}
		boolean l = false, r = false;
		if (EntityIndex > 1)
		{
			l = true;
		}
		if (EntityIndex < EntitySize)
		{
			r = true;
		}
		UpdateDirectionButtons(l, r);
	}

	public void UpdateDirectionButtons(boolean l, boolean r)
	{
		m_PrevSeries.SetEnabled(l);
		m_NextSeries.SetEnabled(r);
	}

	public void UpdateStudyNote(String str)
	{
		String usr = m_DBManager.GetUser();
		if (str.length() < 1)
		{
			m_Study.SetStatusString(Util.Trans("No update necessary"));
			return;
		}
		if (m_Study.m_StudyNote.compareTo("[Empty]") != 0)
		{
			str = m_Study.m_StudyNote + "\n[" + usr + "]: " + str;
		} else
		{
			str = "[" + usr + "]: " + str;
		}
		if (str.length() > 2040)
		{
			int index = str.indexOf("[", str.length() - 2040);
			str = str.substring(index);
		}
		if (m_DBManager.UpdateStudyNote(str, m_Study.m_StudyDR))
		{
			m_Study.m_StudyNote = str;
		}
	}

	public void StudyNote()
	{
		/*
		 * if (m_StudyNote == null) { m_StudyNote = new
		 * StudyNoteDialog(Utils.FindParentFrame(m_C), this,
		 * Util.Trans("Study note"), true); }
		 * m_StudyNote.SetText(m_Study.m_StudyNote);
		 * m_StudyNote.setVisible(true);
		 */
	}
	public boolean addSeriesToStudy(SeriesRecord sr, IProgressReporter pr)
	{
		return m_Study.loadSeries(sr, pr);
	}
	
	public void SendEvent(String event, DICOMRecord dr)
	{
		// System.out.println(event);
		if (event.compareTo("start study loading") == 0)
		{
			m_Study.StartStudyLoading(dr);
		} else if (event.compareTo("start series loading") == 0)
		{
			m_Study.StartSeriesLoading(dr);
		} else if (event.compareTo("end study loading") == 0)
		{
			m_Study.EndStudyLoading();
		} else if (event.compareTo("end series loading") == 0)
		{
			// m_Study.EndSeriesLoading();
		} else if (event.compareTo("set study dr") == 0)
		{
			m_Study.SetStudyDR(dr);
		}
	}
	void MousePressed(java.awt.event.MouseEvent event)
	{
		m_Study.SetToolActive(true);
		m_DragXPos = event.getX();
		m_DragYPos = event.getY();
		m_Study.SelectActiveImage(m_DragXPos, m_DragYPos);
		if (m_CurrentTool != null)
		{
			m_CurrentTool.InitTool(event.getPoint());
		}
	}
	public void SetStatus(String s)
	{
		m_Study.SetStatusString(s);
	}

	class UniKey extends java.awt.event.KeyAdapter
	{
		@Override
		public void keyPressed(java.awt.event.KeyEvent event)
		{
			int c = event.getKeyCode();
			if (c == KeyEvent.VK_F1 && m_WList.IsEnabled())
			{
				m_WList.RunTool();
				m_WList.Release();
			} else if (c == KeyEvent.VK_F2 && m_WL.IsEnabled())
			{
				m_WL.RunTool();
				m_WL.Release();
			} else if (c == KeyEvent.VK_F3 && m_Cine.IsEnabled())
			{
				m_Cine.RunTool();
				m_Cine.Release();
			} else if (c == KeyEvent.VK_F4 && m_Info.IsEnabled())
			{
				m_Info.RunTool();
				m_Info.Release();
			} else if (c == KeyEvent.VK_F5 && m_Layout.IsEnabled())
			{
				m_Layout.RunTool();
				m_Layout.Release();
			} else if (c == KeyEvent.VK_F6 && m_Zoom.IsEnabled())
			{
				m_Zoom.RunTool();
				m_Zoom.Release();
			} else if (c == KeyEvent.VK_F7 && m_Flip.IsEnabled())
			{
				m_Flip.RunTool();
				m_Flip.Release();
			} else if (c == KeyEvent.VK_F8 && m_ROI.IsEnabled())
			{
				m_ROI.RunTool();
				m_ROI.Release();
			} else if (c == KeyEvent.VK_F9 && m_Ruler.IsEnabled())
			{
				m_Ruler.RunTool();
				m_Ruler.Release();
			} else if (c == KeyEvent.VK_F10 && m_Ref.IsEnabled())
			{
				m_Ref.RunTool();
				m_Ref.Release();
			} else if (c == KeyEvent.VK_F11 && m_Notes.IsEnabled())
			{
				m_Notes.RunTool();
				m_Notes.Release();
			} else if (c == KeyEvent.VK_F12 && m_Reset.IsEnabled())
			{
				m_Reset.RunTool();
				m_Reset.Release();
			}
		}
	}

	class MenuListener implements ItemListener // ActionListener
	{
		/*
		 * public void actionPerformed(ActionEvent e) { Object o =
		 * e.getSource(); if(o instanceof CheckboxMenuItem) { for(int i = 0; i <
		 * m_pm.getItemCount(); i++) { if(o == m_pm.getItem(i)) {
		 * m_Study.SwapSeries(i-m_Study.m_PopupOffset); break; } } } else if (o
		 * instanceof MenuItem) { if(((MenuItem)o).getLabel().compareTo("Reload
		 * lossless")==0) m_Study.ReloadSeries(); } }
		 */
		@Override
		public void itemStateChanged(ItemEvent e)
		{
			Object o = e.getSource();
			if (o instanceof CheckboxMenuItem)
			{
				for (int i = 0; i < m_pm.getItemCount(); i++)
				{
					if (((MenuItem) o).getLabel().compareTo(
							Util.Trans("Reload lossless")) == 0)
					{
						m_Study.ReloadSeries();
						return;
					}
					if (o == m_pm.getItem(i))
					{
						m_Study.SwapSeries(i - m_Study.m_PopupOffset);
						break;
					}
				}
			}
		}
	} // end of class MenuListener

	class UniMouseMotion extends java.awt.event.MouseMotionAdapter
	{
		@Override
		public void mouseDragged(java.awt.event.MouseEvent event)
		{
			Object object = event.getSource();
			if (object == m_Study)
			{
				if (m_CurrentTool != null)
				{
					m_CurrentTool.MouseDragged(event);
				}
			}
		}
	} // end of class UniMouseMotion

	public void ShowPopupMenu(int x, int y)
	{
		m_Study.InitPopupMenu(x, y);
	}

	class UniMouse extends java.awt.event.MouseAdapter
	{
		@Override
		public void mousePressed(java.awt.event.MouseEvent event)
		{
			Object object = event.getSource();
			if (object == m_ExPatInfo)
			{
				return;
			}
			if (object == m_PatInfo)
			{
				m_ExPatInfo.setVisible(!m_ExPatInfo.isVisible());
				return;
			}
			if ((event.getModifiers() & InputEvent.CTRL_MASK) != 0)
			{
				if (m_CurrentTool != null)
				{
					if (!m_CurrentTool.ShowPopup(event.getX(), event.getY()))
					{
						ShowPopupMenu(event.getX(), event.getY());

					}
				} else
				{
					ShowPopupMenu(event.getX(), event.getY());
				}
				return;
			}
			long curPeriod = System.currentTimeMillis() - m_LastTimeClicked;
			if (m_LastTimeClicked == 0)
			{
				m_LastTimeClicked = System.currentTimeMillis();
			} else
			{
				m_LastTimeClicked += curPeriod;
			}
			if (object == m_Study)
			{
				if (curPeriod < 300 && m_Study.IsMultiImage())
				{
					m_LastTimeClicked = 0;
					m_Layout.ChangeState();
				} else
				{
					MousePressed(event);
				}
			}
		}

		@Override
		public void mouseExited(java.awt.event.MouseEvent event)
		{
			Object object = event.getSource();

			if (object == m_Study)
			{
				m_Study.SetToolActive(false);
			}
		}

		@Override
		public void mouseReleased(java.awt.event.MouseEvent event)
		{
			if (event.getSource() == m_ExPatInfo)
			{
				int start = m_ExPatInfo.getText().indexOf("\n") + 1, end = m_ExPatInfo
						.getText().indexOf("\n", start);
				if (m_ExPatInfo.getCaretPosition() < end
						&& m_ExPatInfo.getCaretPosition() >= start)
				{
					m_ExPatInfo.select(start, end);
					// this code seems to be obsolete
					if ((event.getModifiers() & InputEvent.CTRL_MASK) != 0)
					{
						/*
						 * try { getAppletContext().showDocument( new
						 * URL(m_ExPatInfo.getSelectedText()), "_blank"); }
						 * catch (Exception e) { }
						 */
					} else if ((event.getModifiers() & InputEvent.SHIFT_MASK) != 0)
					{
						// the following code is obsolete, too
						/*
						 * try { String msg = m_ExPatInfo.getSelectedText(); int
						 * ch = msg.indexOf('?', 0); msg = msg.substring(0, ch)
						 * + "%3F" + msg.substring(ch + 1); msg =
						 * "mailto:?subject=" +
						 * Util.Trans("Series%20from%20Web%20PACS") + "&Body=" +
						 * msg; getAppletContext().showDocument(new URL(msg),
						 * "_blank"); } catch (Exception e) { }
						 */
					}
				}
				return;
			}
			if (event.getSource() == m_Study)
			{
				if (m_Study.ProcessMouseClick(event))
				{
					return;
				}
			}
			m_Study.SetToolActive(false);
			if (m_CurrentTool != null)
			{
				m_CurrentTool.MouseReleased();
			}
		}
	} // end of class UniMouse

	public int ContainedInStudy(DICOMRecord dr)
	{
		return m_Study.ContainedInStudy(dr);
	}
	public void OnSeriesUpdate()
	{
		m_Study.OnSeriesUpdate();
	}
	public void MarkSeriesForUpdate(int ind)
	{
		m_Study.MarkSeriesForUpdate(ind);
	}
	public boolean IsBrowseSeries()
	{
		return m_Study.IsBrowseSeries();
	}
	public void SetSeriesComplete(boolean bComplete)
	{
		m_Study.SetSeriesComplete(bComplete);
	}
} // end of class ImageViewer
