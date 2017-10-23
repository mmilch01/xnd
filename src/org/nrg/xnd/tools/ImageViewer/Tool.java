package org.nrg.xnd.tools.ImageViewer;

import java.awt.Canvas;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;

import org.eclipse.jface.window.Window;
import org.nrg.xnd.utils.Utils;

/**
 * ************************************************************************
 * <p/>
 * Generic tool class
 * <p/>
 * ************************************************************************
 */
public class Tool extends Canvas implements ActionListener
{
	private boolean m_IsMouseDown, m_Active, m_IsStickable, m_IsImageDown,
			m_IsEnabled, m_bOption = false;
	private ImageViewer dicomViewBase;

	public int getM_ToolID()
	{
		return m_ToolID;
	}

	private int m_ToolID;
	public int m_wid, m_ht;
	private Rectangle[] m_Dims = new Rectangle[2];
	private Image m_UpImage, m_DownImage, m_DisabledImage;
	public static final int WORKLIST = 0, ZOOM = 1, WL = 2, LAYOUT = 3,
			CINE = 4, INFO = 5, RESET = 6, SIZE = 7, REF = 8, ROI = 9,
			NEXT = 10, PREV = 11, NOTES = 12, FLIP = 13, IMSER = 14;
	private static final int IN_MAIN = 0, IN_SIDE = 1, OUT = 2;
	SidePopupMenu m_SideMenu = null;
	SidePopupMenu m_PopupMenu = null;
	Image m_SideImage = null, m_SideDownImage = null,
			m_SideDisabledImage = null;
	ToolThread m_ToolThread;
	public int m_markerRGB = new Color(192, 192, 192).getRGB() | 0xFF000000;

	public void SetImDown(Image im)
	{
		m_DownImage = im;
	}
	public class SidePopupMenu extends PopupMenu implements ItemListener
	{
		public boolean m_bSide = false;

		public void ShowInContext(Component c, int x, int y)
		{
			c.add(this);
			show(c, x, y);
		}

		public int GetSelectedItem()
		{
			return GetSelectedItem(-1);
		}

		public void SetItemState(int index, boolean bSelect)
		{
			Object item = getItem(index);
			if (!(item instanceof CheckboxMenuItem))
			{
				return;
			}
			((CheckboxMenuItem) item).setState(bSelect);
		}

		public int GetSelectedItem(int subMenuIndex)
		{
			Menu menu;
			if (subMenuIndex == -1)
			{
				menu = this;
			} else
			{
				menu = (Menu) getItem(subMenuIndex);
			}
			if (!(menu instanceof Menu))
			{
				return -1;
			}
			MenuItem mi;
			CheckboxMenuItem cmi;
			for (int i = 0; i < menu.getItemCount(); i++)
			{
				mi = menu.getItem(i);
				if (mi instanceof CheckboxMenuItem)
				{
					cmi = (CheckboxMenuItem) mi;
				} else
				{
					continue;
				}
				if (cmi.getState())
				{
					return i;
				}
			}
			return -1;
		}

		@Override
		public void itemStateChanged(ItemEvent e)
		{
			Object src = e.getSource();
			MenuItem mi, mi1;
			for (int i = 0; i < getItemCount(); i++)
			{
				mi = getItem(i);
				if (mi instanceof Menu) // process submenu items
				{
					for (int j = 0; j < ((Menu) mi).getItemCount(); j++)
					{
						mi1 = ((Menu) mi).getItem(j);
						if (mi1 instanceof CheckboxMenuItem)
						{
							if (mi1 == src)
							{
								((CheckboxMenuItem) mi1).setState(true);
							} else
							{
								((CheckboxMenuItem) mi1).setState(false);
							}
						}
						if (mi1 == src)
						{
							MenuCommand(i, j);
						}
					}
					continue;
				}
				if (mi instanceof CheckboxMenuItem)
				{
					if (mi == src)
					{
						((CheckboxMenuItem) mi).setState(true);
					} else
					{
						((CheckboxMenuItem) mi).setState(false);
					}
				}
				if (mi == src)
				{
					MenuCommand(i, 0);
				}
			}
		}
	} // end of SidePopupMenu class

	class ToolFilter extends RGBImageFilter
	{
		public static final int UP = 0, DOWN = 1, DISABLE = 2;
		private int m_type = 0;
		private static final double dr = 0.85;
		public ToolFilter(int type)
		{
			m_type = type;
			canFilterIndexColorModel = true;
		}

		@Override
		public int filterRGB(int x, int y, int rgb)
		{			
			if (m_type == UP || m_type == DISABLE)
			{
				if ((rgb | 0xFF000000) == m_markerRGB)
					return 0x00FFFFFF & rgb;
			}
			if (m_type == DOWN)
			{
				int r = (rgb & 0xff0000) >> 16, g = (rgb & 0x00ff00) >> 8, b = (rgb & 0x0000ff);
				r = (int) (r * dr);
				g = (int) (g * dr);
				b = (int) (b * dr);

				return (rgb & 0xff000000) | (r << 16) | (g << 8) | b;

				// | (Math.max((rgb & 0xff0000) - 0x0f0000, 0x00))
				// | (Math.max((rgb & 0x00ff00) - 0x000f00, 0x00))
				// | (Math.max((rgb & 0x0000ff) - 0x0f, 0x00));
			} else if (m_type == DISABLE)
			{
				return (rgb & ~0xff000000) | 0x80000000;
			} else
				return rgb;
		}
	} // end of class ToolDownFilter

	public Tool(ImageViewer dicomViewBase, Image im_up, int ToolID)
	{
		this(dicomViewBase, im_up, true, null, ToolID);
	}

	public Tool(ImageViewer dicomViewBase, Image im_up, boolean isstickable,
			Image iSideImageUp, int ToolID, boolean bOption)
	{
		this(dicomViewBase, im_up, isstickable, iSideImageUp, ToolID);
		m_bOption = bOption;
	}

	public Tool(ImageViewer dicomViewBase, Image im_up, boolean isstickable,
			Image iSideImageUp, int ToolID)
	{
		this.dicomViewBase = dicomViewBase;
		m_IsMouseDown = false;
		m_IsImageDown = false;
		m_IsEnabled = true;
		m_Active = false;
		m_IsStickable = isstickable;
		m_ToolID = ToolID;
		// m_UpImage = im_up;
		// m_DownImage = im_down;
		m_UpImage = createImage(new FilteredImageSource(im_up.getSource(),
				new ToolFilter(ToolFilter.UP)));
		m_DownImage = createImage(new FilteredImageSource(im_up.getSource(),
				new ToolFilter(ToolFilter.DOWN)));
		if (iSideImageUp != null)
		{
			m_SideImage = createImage(new FilteredImageSource(iSideImageUp
					.getSource(), new ToolFilter(ToolFilter.UP)));
			m_SideDownImage = createImage(new FilteredImageSource(iSideImageUp
					.getSource(), new ToolFilter(ToolFilter.DOWN)));
		}
		// if(iSideImageDown!=null)
		// {
		// m_SideDownImage = createImage(new
		// FilteredImageSource(iSideImageUp.getSource(),
		// new ToolDownFilter()));
		// }
		// else m_SideDownImage=null;

		m_DisabledImage = createImage(new FilteredImageSource(
				im_up.getSource(), new ToolFilter(ToolFilter.DISABLE)));

		m_Dims[0] = new Rectangle(new Dimension(m_UpImage.getWidth(this),
				m_UpImage.getHeight(this)));
		if (iSideImageUp != null)
		{
			m_SideDisabledImage = createImage(new FilteredImageSource(
					iSideImageUp.getSource(),
					new ToolFilter(ToolFilter.DISABLE)));
			m_Dims[1] = new Rectangle(new Dimension(m_SideImage.getWidth(this),
					m_SideImage.getHeight(this)));
			m_Dims[1].setLocation(m_Dims[0].width, 0);
			Rectangle rec = m_Dims[0].union(m_Dims[1]);
			setSize(rec.width, rec.height);
		} else
		{
			setSize(m_Dims[0].width, m_Dims[0].height);
		}
		m_SideMenu = InitSideMenu();
		if (iSideImageUp != null)
			this.add(m_SideMenu);

		else
			dicomViewBase.getStudy().add(m_SideMenu);
		// this.add(m_SideMenu);
		// m_PopupMenu=InitSideMenu();
		// m_Study.add(m_PopupMenu);
		m_wid = getSize().width;
		m_ht = getSize().height;
		addMouseListener(new ToolMouseListener());
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (m_SideMenu == null)
		{
			return;
		}
		// System.out.println(e);
		MenuItem srcmi = (MenuItem) e.getSource();

		for (int i = 0; i < m_SideMenu.getItemCount(); i++)
		{
			if (srcmi == m_SideMenu.getItem(i))
			{
				MenuCommand(i, -1);
				return;
			}
		}
	}

	private void MenuCommand(int commandID, int commandID1)
	{
		double cur_zoom = 0;
		switch (m_ToolID)
		{
			case IMSER :
				dicomViewBase.getStudy().SetBrowseMode(commandID);
				paint(this.getGraphics());
				break;
			case ZOOM :
				if (dicomViewBase.getStudy().GetCurrentSeries() != null)
				{
					cur_zoom = dicomViewBase.getStudy().GetCurrentSeries()
							.GetRelativeZoom();
				}
				if (m_SideMenu.GetSelectedItem() == 1 && cur_zoom < 1.05)
				{
					m_SideMenu.SetItemState(1, false);
					m_SideMenu.SetItemState(0, true);
				}
				break;
			case WL :
				if (dicomViewBase.getStudy().GetCurrentSeries() != null)
				{
					dicomViewBase.getStudy().GetCurrentSeries().SetPresetWL(
							commandID, commandID1);
				}
				break;
			case CINE :
				dicomViewBase.getStudy().ControlCineThread(commandID);
				break;
			case LAYOUT :

				// m_Study.EnableIndexUpdate(false);
				if (commandID == 1)
				{
					dicomViewBase.getStudy().SetStudyLayout(
							Study.LAYOUT_UNCHANGED, commandID1);
				} else if (commandID == 0)
				{
					dicomViewBase.getStudy().SetStudyLayout(commandID1,
							Study.LAYOUT_UNCHANGED);
				}
				// m_Study.EnableIndexUpdate(true);
		}
	}

	private void AddCheckboxMenuItem(String txt, boolean state,
			SidePopupMenu menu, Menu submenu)
	{
		CheckboxMenuItem cmi = new CheckboxMenuItem(Util.Trans(txt), state);
		cmi.addItemListener(menu);
		submenu.add(cmi);
	}

	private void AddCheckboxMenuItem(String txt, boolean state,
			SidePopupMenu menu)
	{
		AddCheckboxMenuItem(txt, state, menu, menu);
	}

	private SidePopupMenu InitSideMenu()
	{
		SidePopupMenu menu = new SidePopupMenu();
		/*
		 * if (m_SideMenu == null) { m_SideMenu = new SidePopupMenu(); }
		 * Tool.this.add(m_SideMenu); m_Study.add(m_SideMenu);
		 */

		MenuItem mi;
		CheckboxMenuItem cmi;
		switch (m_ToolID)
		{
			case IMSER :
				AddCheckboxMenuItem("Image", true, menu);
				AddCheckboxMenuItem("Series", false, menu);
				break;
			case FLIP :
				AddCheckboxMenuItem("Flip Left - Right", true, menu);
				AddCheckboxMenuItem("Flip Top - Bottom", false, menu);
				AddCheckboxMenuItem("Rotate 90 Clockwise", false, menu);
				AddCheckboxMenuItem("Rotate 90 Counter-Clockwise", false, menu);
				AddCheckboxMenuItem("Rotate 180", false, menu);
				break;
			case ZOOM :
				AddCheckboxMenuItem("Zoom", true, menu);
				AddCheckboxMenuItem("Pan", false, menu);
				break;
			case CINE :
				AddCheckboxMenuItem("Manual", true, menu);
				AddCheckboxMenuItem("Auto loop", false, menu);
				AddCheckboxMenuItem("Auto sweep", false, menu);
				menu.addSeparator();
				AddCheckboxMenuItem("3 frames/sec", false, menu);
				AddCheckboxMenuItem("9 frames/sec", false, menu);
				AddCheckboxMenuItem("12 frames/sec", false, menu);
				AddCheckboxMenuItem("24 frames/sec", false, menu);
				AddCheckboxMenuItem("30 frames/sec", false, menu);
				break;
			case WL :
				AddCheckboxMenuItem("0   " + Util.Trans("Lung"), false, menu);
				AddCheckboxMenuItem("1   " + Util.Trans("Bone"), false, menu);
				AddCheckboxMenuItem("2   " + Util.Trans("Brain"), false, menu);
				AddCheckboxMenuItem("3   " + Util.Trans("Chest"), false, menu);
				AddCheckboxMenuItem("4   " + Util.Trans("Heart"), false, menu);
				AddCheckboxMenuItem("5   " + Util.Trans("Head"), false, menu);
				AddCheckboxMenuItem("6   " + Util.Trans("Abdomen"), false, menu);
				AddCheckboxMenuItem("7   " + Util.Trans("Spine"), false, menu);
				AddCheckboxMenuItem("8   " + Util.Trans("Liver"), false, menu);
				AddCheckboxMenuItem("9   " + Util.Trans("Kidney"), false, menu);
				menu.addSeparator();
				Menu mUS = new Menu(Util.Trans("UltraSound"));
				AddCheckboxMenuItem("Low", false, menu, mUS);
				AddCheckboxMenuItem("Med", false, menu, mUS);
				AddCheckboxMenuItem("High", false, menu, mUS);
				menu.add(mUS);
				Menu mMammo = new Menu(Util.Trans("Mammogram"));
				AddCheckboxMenuItem(Util.Trans("Low") + " (900)", false, menu,
						mMammo);
				AddCheckboxMenuItem(Util.Trans("Med") + " (750)", false, menu,
						mMammo);
				AddCheckboxMenuItem(Util.Trans("High") + " (600)", false, menu,
						mMammo);
				menu.add(mMammo);
				break;
			case LAYOUT :
				menu.m_bSide = true;
				Menu mfSub = new Menu(Util.Trans("Monitor frames"));
				AddCheckboxMenuItem("Auto", true, menu, mfSub);
				AddCheckboxMenuItem("1x1", false, menu, mfSub);
				AddCheckboxMenuItem("1x2", false, menu, mfSub);
				AddCheckboxMenuItem("2x1", false, menu, mfSub);
				AddCheckboxMenuItem("2x2", false, menu, mfSub);
				AddCheckboxMenuItem("2x3", false, menu, mfSub);
				AddCheckboxMenuItem("3x2", false, menu, mfSub);
				AddCheckboxMenuItem("3x3", false, menu, mfSub);
				AddCheckboxMenuItem("4x4", false, menu, mfSub);
				menu.add(mfSub);

				Menu isSub = new Menu(Util.Trans("Images in series"));
				AddCheckboxMenuItem("Auto", false, menu, isSub);
				AddCheckboxMenuItem("1x1", true, menu, isSub);
				AddCheckboxMenuItem("1x2", false, menu, isSub);
				AddCheckboxMenuItem("2x1", false, menu, isSub);
				AddCheckboxMenuItem("2x2", false, menu, isSub);
				AddCheckboxMenuItem("2x3", false, menu, isSub);
				AddCheckboxMenuItem("3x2", false, menu, isSub);
				AddCheckboxMenuItem("3x3", false, menu, isSub);
				AddCheckboxMenuItem("4x4", false, menu, isSub);
				menu.add(isSub);
		}
		return menu;
	}

	@Override
	public void paint(Graphics g)
	{
		if (g == null)
		{
			return;
		}
		if (m_bOption) // image depends on option selected
		{
			if (m_SideMenu.GetSelectedItem() == 0)
			{
				g.drawImage(m_UpImage, 0, 0, this);
			} else
			{
				g.drawImage(m_DownImage, 0, 0, this);
			}
			if (m_SideImage != null)
			{
				g.drawImage(m_SideImage, m_Dims[0].width, 0, this);
			}
			return;
		}
		if (m_IsImageDown)
		{
			g.drawImage(m_DownImage, 0, 0, this);
			if (m_SideImage != null)
			{
				g.drawImage(m_SideImage, m_Dims[0].width, 0, this);
			}
		} else if (m_IsEnabled)
		{
			g.drawImage(m_UpImage, 0, 0, this);
			if (m_SideImage != null)
			{
				g.drawImage(m_SideImage, m_Dims[0].width, 0, this);
			}
		} else
		{
			g.drawImage(m_DisabledImage, 0, 0, this);
			if (m_SideDisabledImage != null)
			{
				g.drawImage(m_SideDisabledImage, m_Dims[0].width, 0, this);
			}
		}
	}

	public void SetActive(boolean b)
	{
		m_Active = b;
		if (b)
		{
			m_IsImageDown = true;
		} else
		{
			m_IsImageDown = false;
			if (m_ToolID == CINE)
				dicomViewBase.getStudy().ControlCineThread(0);
		}
		repaint();
	}

	public void SetEnabled(boolean b)
	{
		m_IsEnabled = b;
		repaint();
	}

	public boolean IsSticked()
	{
		return m_Active;
	}

	public boolean IsEnabled()
	{
		return m_IsEnabled;
	}

	private int Contains(Point x)
	{

		if (m_Dims[0].contains(x))
		{
			return IN_MAIN;
		}
		if (m_Dims[1] != null)
		{
			if (m_Dims[1].contains(x))
			{
				return IN_SIDE;
			}
		}
		return OUT;
	}

	public void ChangeStatus()
	{
		if (m_IsImageDown)
		{
			if (!m_IsStickable)
			{
				m_IsImageDown = false;
			} else
			{
				if (!m_Active)
				{
					m_IsImageDown = true;
					m_Active = true;
				} else
				{
					m_IsImageDown = false;
					m_Active = false;
				}
			}
			repaint();
		}
	}

	void InitTool(Point pt)
	{
		if (!m_Active)
		{
			return;
		}
		switch (m_ToolID)
		{
			case SIZE :
			case ROI :
				dicomViewBase.getStudy().InitTool(pt, m_ToolID);
		}
	}

	void MouseReleased()
	{
		if (!m_Active)
		{
			return;
		}
		switch (m_ToolID)
		{
			case WL :
				dicomViewBase.getStudy().WindowLevel(0, 0, true);
				break;
			case ZOOM :
				dicomViewBase.getStudy().ChangeZoom(false, true);
				break;
			case CINE :
				// m_Study.Cine(0, false);
				break;
		}
	}

	void MouseDragged(java.awt.event.MouseEvent event)
	{
		double cur_zoom;
		if (!m_Active)
		{
			return;
		}
		int x = event.getX(), y = event.getY();
		int diffX = dicomViewBase.m_DragXPos - x, diffY = dicomViewBase.m_DragYPos
				- y;
		double sh = (dicomViewBase.getStudy().GetCurrentSeries()
				.GetScreenHeight());
		int MinDiff = (int) Math.max(1, sh / 100.0);
		switch (m_ToolID)
		{
			case SIZE :
			case ROI :
				dicomViewBase.getStudy().DrawTool(event.getPoint());
				break;
			case ZOOM :
				if (m_SideMenu.GetSelectedItem() < 0)
				{
					return;
				}
				cur_zoom = 0;
				if (dicomViewBase.getStudy().GetCurrentSeries() != null)
				{
					cur_zoom = dicomViewBase.getStudy().GetCurrentSeries()
							.GetRelativeZoom();
				}
				if (m_SideMenu.GetSelectedItem() == 1 && cur_zoom < 1.05)
				{
					m_SideMenu.SetItemState(1, false);
					m_SideMenu.SetItemState(0, true);
				}
				if (m_SideMenu.GetSelectedItem() == 0) // zoom
				{
					if (Math.abs(diffY) < MinDiff)
					{
						return;
					}
					if (diffY > 0)
					{
						dicomViewBase.getStudy().ChangeZoom(false, false);
					} else
					{
						dicomViewBase.getStudy().ChangeZoom(true, false);
					}
				} else
				// pan
				{
					if (!dicomViewBase.getStudy().ShiftImage(diffX, diffY,
							false))
					{
						return;
					}
				}
				break;
			case WL :
				if (Math.abs(diffX) < MinDiff && Math.abs(diffY) < MinDiff)
				{
					return;
				}
				dicomViewBase.getStudy().WindowLevel(diffX, diffY, false);
				break;
			case CINE :
				if (Math.abs(diffY) < MinDiff)
					return;
				// MinDiff =
				// (int)Math.max(1,(double)(m_Study.GetCurrentSeries().GetScreenHeight()/(double)m_Study.GetCurrentSeries().GetSize()));
				// if(Math.abs(diffY)>1 && Math.abs(diffY)<MinDiff) return;
				double r = Math.abs(diffY / MinDiff);
				if (r < 1)
					r = 1;
				double acc = Math.floor(r);
				if (acc > 2)
					r = r * Math.sqrt(acc);
				int step = (int) ((r + 0.5) * Math.signum(diffY));
				dicomViewBase.getStudy().Cine(-step, false, false);
		}
		dicomViewBase.m_DragYPos = y;
		dicomViewBase.m_DragXPos = x;
	}

	private int getCurToolID()
	{
		Tool t = dicomViewBase.getCurrentTool();
		if (t == null)
			return -1;
		return t.getM_ToolID();
	}
	public void ImplementAction()
	{
		boolean res = false;
		double cur_zoom = 0;
		if (getCurToolID() == Tool.SIZE || getCurToolID() == Tool.ROI)
		{
			dicomViewBase.getStudy().ResetTool();
		}
		switch (m_ToolID)
		{
			case WORKLIST :
				dicomViewBase.getDBManager().ShowDBWindow(true);
				break;
			case INFO :
				dicomViewBase.getStudy().EnableInfo(!m_Active);
				break;
			case REF :
				dicomViewBase.getStudy().EnableCrosslink(!m_Active);
				break;
			case ZOOM :
				if (dicomViewBase.getStudy().GetCurrentSeries() != null)
				{
					cur_zoom = dicomViewBase.getStudy().GetCurrentSeries()
							.GetRelativeZoom();
				}
				if (m_SideMenu.GetSelectedItem() == 1 && cur_zoom < 1.05)
				{
					m_SideMenu.SetItemState(1, false);
					m_SideMenu.SetItemState(0, true);
				}
			case CINE :
				if (m_Active)
					dicomViewBase.getStudy().ControlCineThread(0);

			case SIZE :
			case ROI :
			case WL :
				if (dicomViewBase.getCurrentTool() != null
						&& dicomViewBase.getCurrentTool() != this)
				{
					dicomViewBase.getCurrentTool().SetActive(false);
				}
				dicomViewBase.setCurrentTool(this);
				break;
			case FLIP :
				if (Utils.ShowMessageBox("Warning", "$orient_modify",
						Window.CANCEL) == Window.OK)
				{
					dicomViewBase.getStudy().Flip(m_SideMenu.GetSelectedItem());
				}
				Release();
				break;
			case LAYOUT :

				// m_Study.EnableIndexUpdate(false);
				if (!m_Active)
				{
					dicomViewBase.getStudy().m_bSerStack = false;
					dicomViewBase.getStudy().SetStudyLayout(
							Study.LAYOUT_UNCHANGED,
							m_SideMenu.GetSelectedItem());
				} else
				{
					dicomViewBase.getStudy().m_bSerStack = true;
					dicomViewBase.getStudy().SetStudyLayout(
							Study.LAYOUT_UNCHANGED,
							m_SideMenu.GetSelectedItem());
				}

				// m_Study.EnableIndexUpdate(true);
				break;
			case RESET :
				if (dicomViewBase.getStudy() != null)
				{
					dicomViewBase.getStudy().ResetStudy();
				}
				break;
			case PREV :
			case NEXT :
				if (m_ToolThread == null)
				{
					return;
				}
				try
				{
					m_ToolThread.start();
				} catch (Exception e)
				{
				}

				if (dicomViewBase.getTool(Tool.IMSER).m_SideMenu
						.GetSelectedItem() == 1)
				{
					Release();
				}
				break;
			case NOTES :
				dicomViewBase.StudyNote();
				ChangeStatus();
				m_IsMouseDown = false;
				break;
		}
	}

	public boolean ShowPopup(int x, int y)
	{
		if (!m_Active || m_SideMenu == null)
		{
			return false;
		}
		if (!m_SideMenu.m_bSide)
		{
			if (m_SideMenu.getItemCount() < 1)
			{
				return false;
			}
			if (m_ToolID == CINE)
				if (dicomViewBase.getStudy().getM_CineThread().m_controlID == 0)
				{
					m_SideMenu.SetItemState(0, true);
					m_SideMenu.SetItemState(1, false);
					m_SideMenu.SetItemState(2, false);
				}

			if (m_ToolID == WL)
			{
				dicomViewBase.getStudy().GetCurrentSeries().InitWLMenu(
						m_SideMenu);
			}
			// m_Study.add(m_SideMenu);
			m_SideMenu.ShowInContext(dicomViewBase.getStudy(), x, y);
			// m_Study.remove(m_SideMenu);
			// this.add(m_SideMenu);
			return true;
		}
		return false;
	}

	public void UpdatePopupMenu(int code)
	{
		if (m_SideMenu == null)
		{
			return;
		}
		CheckboxMenuItem cmi;
		int sel;
		switch (m_ToolID)
		{
			case LAYOUT :
				if (code < 0 || code > m_SideMenu.getItemCount() - 1)
				{
					return;
				}
				sel = m_SideMenu.GetSelectedItem();
				if (sel < 0)
				{
					return;
				}
				cmi = (CheckboxMenuItem) m_SideMenu.getItem(sel);
				cmi.setState(false);
				cmi = (CheckboxMenuItem) m_SideMenu.getItem(code);
				cmi.setState(true);
				break;
		}
	}

	public void ChangeState()
	{
		if (m_IsEnabled == true)
		{
			m_IsMouseDown = true;
			m_IsImageDown = true;
			paint(getGraphics());
			ImplementAction();
			ChangeStatus();
			m_IsMouseDown = true;
		}
	}

	public void RunTool()
	{
		if (m_IsEnabled == true && !m_bOption)
		{
			m_IsMouseDown = true;
			m_IsImageDown = true;
			paint(getGraphics());
			ImplementAction();
		}
	}

	public void Release()
	{
		ChangeStatus();
		m_IsMouseDown = false;
		if (m_ToolThread != null)
		{
			m_ToolThread.SetStopCommand();
		}
	}

	public class ToolMouseListener extends MouseAdapter
	{
		@Override
		public void mousePressed(MouseEvent e)
		{
			Point p = e.getPoint();
			int pos = Contains(p);

			if (m_ToolThread == null)
			{
				m_ToolThread = new ToolThread(dicomViewBase, Tool.this);
			}

			m_ToolThread.SetStartCommand();

			if (pos == IN_MAIN)
			{
				RunTool();
			} else if ((pos == IN_SIDE) && (m_IsEnabled == true))
			{
				if (m_SideMenu == null)
				{
					return;
				}
				switch (m_ToolID)
				{
					case WL :
						if (dicomViewBase.getStudy().getM_CurrentSeries() != null)
						{
							dicomViewBase.getStudy().getM_CurrentSeries()
									.InitWLMenu(m_SideMenu);
						}
					case CINE :
					case FLIP :
					case ZOOM :
					case IMSER :
						m_SideMenu.ShowInContext(Tool.this, 0, m_ht);
						break;
					case LAYOUT :
						try
						{
							m_SideMenu.ShowInContext(Tool.this, 0, m_ht);
						} catch (Exception ex)
						{
							Util
									.LogError(
											"Could not show popup menu: exception occured, Code 2",
											ex);
						}
				}
			}
		}
		@Override
		public void mouseReleased(MouseEvent e)
		{
			if (Contains(e.getPoint()) == IN_MAIN)
			{
				Release();
			}
		}
	}
	class ToolThread extends Thread
	{
		boolean m_bRunning = false;
		int iterationNumber = 0;
		private Tool tool;
		private ImageViewer dicomDICOMView;

		ToolThread(ImageViewer dicomDICOMView, Tool tool)
		{
			this.dicomDICOMView = dicomDICOMView;
			this.tool = tool;
			SetStopCommand();
		}

		public void SetStopCommand()
		{
			m_bRunning = false;
			iterationNumber = 0;
			try
			{
				dicomDICOMView.getStudy().ShowNext(0, true);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		public void SetStartCommand()
		{
			m_bRunning = true;
			iterationNumber = 0;
			if (!isAlive())
			{
				start();
			} else
			{
				interrupt();
			}
		}

		@Override
		public void run()
		{
			m_bRunning = true;
			while (true)
			{
				if (m_bRunning && this.tool.IsEnabled())
				{
					try
					{
						switch (tool.getM_ToolID())
						{
							case Tool.PREV :
								dicomDICOMView.getStudy().ShowNext(-1, true);
								break;
							case Tool.NEXT :
								dicomDICOMView.getStudy().ShowNext(1, true);
								break;
						}
						iterationNumber++;
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
				try
				{
					Thread.sleep(calcDelayTime());
				} catch (InterruptedException e)
				{
	}
}
		}

		private int calcDelayTime()
		{
			return 1000 / (iterationNumber + 10);
		}
	} // end of class ToolThread
} //end of class Tool
