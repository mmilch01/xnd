package org.nrg.xnd.ui.dialogs;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TreeIterator;
import org.nrg.xnd.model.TypeFilter;
import org.nrg.xnd.model.VirtualFolder;
import org.nrg.xnd.tools.StoreXARManager;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.Utils;
public class UploadToXNATDialog extends Dialog
{
	private Combo m_TaglistCombo;
	private ItemTag m_SelectedTag = null;
	private Label m_TagNameLabel;
	private Combo m_Value;
	private RepositoryViewManager m_rvm;
	private Label m_LabelValue;
	private int m_DlgType = -1;
	private Object[] m_tags;
	private Group m_ConnParamGroup;
	private Label m_PasswordLabel;
	private Text m_PasswordText;
	private Combo m_ServerText;
	private MemoCombo m_ServerTextM;
	private Label m_ServerLabel;
	private Label m_UserLabel;
	private TableViewer m_TableViewer;
	private Label m_TotalSizeLabel;
	private long m_TotalSize = 0;
	private Button m_CreateSubject;
	private Button m_ClearAllButton;
	private Button m_SelectAllButton;
	private Group group1;
	private Table m_ExpTable;
	private Combo m_UserText;
	private MemoCombo m_UserTextM;

	private Collection<CElement> m_elements;
	private LinkedList<VirtualFolder> m_Experiments = new LinkedList<VirtualFolder>();
	private TreeMap<String, Collection<DBElement>> m_RecordsMap = new TreeMap<String, Collection<DBElement>>();
	public static final int ADD = 0, REMOVE = 1, EDIT = 2;
	private static final int FIELD_EXPERIMENT = 0, FIELD_MODALITY = 1,
			FIELD_FILES = 2, FIELD_SIZE = 3;

	public UploadToXNATDialog(Shell parentShell, Collection<CElement> cce,
			RepositoryViewManager rvm)
	{
		super(parentShell);
		m_elements = cce;
		m_rvm = rvm;
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Upload experiments to XNAT 1.4");
		try
		{
			this.getButton(IDialogConstants.OK_ID).setText("Upload");
		} catch (Exception e)
		{
		}
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 2;
		parent.setLayout(parentLayout);
		XNDApp.StartWaitCursor();
		// parent.setSize(593, 354);
		{
			group1 = new Group(parent, SWT.NONE);
			GridLayout group1Layout = new GridLayout();
			group1Layout.numColumns = 3;
			group1Layout.makeColumnsEqualWidth = false;
			group1.setLayout(group1Layout);
			GridData group1LData = new GridData();
			group1LData.verticalAlignment = GridData.FILL;
			group1.setLayoutData(group1LData);
			group1.setText("Experiments to upload");
			{
				GridData m_ExperimentTableLData = new GridData();
				m_ExpTable = new Table(group1, SWT.CHECK | SWT.SINGLE
						| SWT.BORDER);

				/*
				 * m_ExpTable.addSelectionListener(new SelectionListener() {
				 * public void widgetSelected(SelectionEvent e) {
				 * if(e.detail!=SWT.CHECK) return;
				 * UpdateTotalSize((TableItem)e.item,
				 * ((TableItem)(e.item)).getChecked()); } public void
				 * widgetDefaultSelected(SelectionEvent e){} });
				 */
				m_TableViewer = new CheckboxTableViewer(m_ExpTable);
				m_ExpTable.setLayoutData(m_ExperimentTableLData);
				m_ExpTable.setHeaderVisible(true);
				m_ExpTable.setLinesVisible(true);
				GridData gd = new GridData();
				// gd.verticalSpan=10;
				gd.verticalAlignment = GridData.FILL;
				gd.horizontalAlignment = GridData.FILL;
				gd.grabExcessHorizontalSpace = true;
				gd.grabExcessVerticalSpace = true;
				gd.horizontalSpan = 2;
				gd.widthHint = 450;
				gd.heightHint = 150;
				m_ExpTable.setLayoutData(gd);
				// m_ExpTable.setSize(376, 23);
				String[] titles = {"Project/Subject/Experiment"};// ,"Modality"};//,"Files","Size"};
				TableColumn tc;
				for (int i = 0; i < titles.length; i++)
				{
					tc = new TableColumn(m_ExpTable, SWT.NONE);
					tc.setText(titles[i]);
				}
				String[] tags = {"Project", "Subject", "Experiment"};
				// m_RecordsMap=DistributeByTagPath(tags);
				// m_Experiments=m_RecordsMap.keySet().toArray(new String[0]);

				TableItem ti;
				long[] fileStats;
				// for(int i=0; i<m_Experiments.length; i++)
				FindExperiments(m_elements);
				for (VirtualFolder vf : m_Experiments)
				{
					ti = new TableItem(m_ExpTable, SWT.NONE);
					ti.setText(FIELD_EXPERIMENT, vf.VirtualPath());
					// fileStats=FileStats(m_Experiments[i]);
					// LinkedList<DBElement>
					// col=(LinkedList<DBElement>)(m_RecordsMap.get(exp));
					// ti.setText(FIELD_MODALITY,col.get(0).GetIR().GetTagValue("Modality"));
					// ti.setText(FIELD_FILES,Long.toString(fileStats[0]));
					// m_TotalSize+=fileStats[1];
					// ti.setText(FIELD_SIZE,Utils.GetFormattedSize(fileStats[1]));
					ti.setChecked(true);
					// tc.setWidth(200);
				}
				for (int i = 0; i < titles.length; i++)
					m_ExpTable.getColumn(i).pack();
			}
			{
				m_TotalSizeLabel = new Label(group1, SWT.NONE);
				GridData m_TotalSizeLabelLData = new GridData();
				m_TotalSizeLabelLData.horizontalAlignment = GridData.FILL;
				m_TotalSizeLabelLData.grabExcessHorizontalSpace = true;
				m_TotalSizeLabel.setLayoutData(m_TotalSizeLabelLData);
				// UpdateLabels();
			}
			{
				m_SelectAllButton = new Button(group1, SWT.PUSH | SWT.CENTER);
				m_SelectAllButton.setText("Select all");
				m_SelectAllButton.addSelectionListener(new SelectionListener()
				{
					public void widgetSelected(SelectionEvent evt)
					{
						// m_ExpTable.setRedraw(false);
						TableItem[] items = m_ExpTable.getItems();
						// long total_sz=0;
						for (int i = 0; i < items.length; i++)
						{
							if (!items[i].getChecked())
							{
								items[i].setChecked(true);
								// UpdateTotalSize(items[i],true);
							}
						}
						// m_ExpTable.setRedraw(true);
					}
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
			}
			{
				m_ClearAllButton = new Button(group1, SWT.PUSH | SWT.CENTER);
				m_ClearAllButton.setText("Clear all");
				m_ClearAllButton.addSelectionListener(new SelectionListener()
				{
					public void widgetSelected(SelectionEvent evt)
					{
						// m_ExpTable.setRedraw(false);
						TableItem[] items = m_ExpTable.getItems();
						for (int i = 0; i < items.length; i++)
						{
							if (items[i].getChecked())
								items[i].setChecked(false);
						}
						// m_TotalSize=0;
						// UpdateLabels();
						// m_ExpTable.setRedraw(true);
						// m_ExpTable.redraw();
					}
					public void widgetDefaultSelected(SelectionEvent evt)
					{
					}
				});
			}
		}
		{
			m_ConnParamGroup = new Group(parent, SWT.NONE);
			GridLayout m_ConnParamGroupLayout = new GridLayout();
			m_ConnParamGroupLayout.numColumns = 2;
			m_ConnParamGroup.setLayout(m_ConnParamGroupLayout);
			GridData m_ConnParamGroupLData = new GridData();
			// m_ConnParamGroupLData.widthHint = 328;
			m_ConnParamGroupLData.verticalAlignment = GridData.FILL;
			m_ConnParamGroup.setLayoutData(m_ConnParamGroupLData);
			m_ConnParamGroup.setText("Session parameters");
			{
				m_ServerLabel = new Label(m_ConnParamGroup, SWT.NONE);
				GridData m_ServerLabelLData = new GridData();
				m_ServerLabelLData.horizontalAlignment = GridData.CENTER;
				m_ServerLabel.setLayoutData(m_ServerLabelLData);
				m_ServerLabel.setText("Server:");
			}
			{
				GridData m_ServerTextLData = new GridData();
				m_ServerTextLData.widthHint = 120;
				m_ServerText = new Combo(m_ConnParamGroup, SWT.BORDER);
				m_ServerText.setLayoutData(m_ServerTextLData);
				// m_ServerText.setText(XNDApp.app_Prefs.get("defaultXNATUploadServer","http://central.xnat.org"));
				m_ServerTextM = new MemoCombo(m_ServerText,
						"UploadToXNATDialog.ServerText", 10);
				// m_ServerText.setText("http://central.xnat.org");
				m_ServerText.select(0);
			}
			{
				m_UserLabel = new Label(m_ConnParamGroup, SWT.NONE);
				GridData m_UserLabelLData = new GridData();
				m_UserLabelLData.horizontalAlignment = GridData.CENTER;
				m_UserLabel.setLayoutData(m_UserLabelLData);
				m_UserLabel.setText("User:");
			}
			{
				GridData m_UserTextLData = new GridData();
				m_UserTextLData.widthHint = 80;
				m_UserText = new Combo(m_ConnParamGroup, SWT.BORDER);
				m_UserText.setLayoutData(m_UserTextLData);
				// m_UserText.setText(XNDApp.app_Prefs.get("defaultXNATUploadUser",
				// "guest"));
				m_UserTextM = new MemoCombo(m_UserText,
						"UploadToXNATDialog.UserText", 10);
				m_UserText.select(0);
			}
			{
				m_PasswordLabel = new Label(m_ConnParamGroup, SWT.NONE);
				GridData m_PasswordLabelLData = new GridData();
				m_PasswordLabelLData.horizontalAlignment = GridData.CENTER;
				m_PasswordLabel.setLayoutData(m_PasswordLabelLData);
				m_PasswordLabel.setText("Pass:");
			}
			{
				GridData m_PasswordTextLData = new GridData();
				m_PasswordTextLData.widthHint = 80;
				m_PasswordText = new Text(m_ConnParamGroup, SWT.BORDER
						| SWT.PASSWORD);
				// m_PasswordText.setText(XNDApp.app_Prefs.get("defaultXNATPass",
				// "guest"));
				m_PasswordText.setLayoutData(m_PasswordTextLData);
			}
			{
				m_CreateSubject = new Button(m_ConnParamGroup, SWT.CHECK
						| SWT.LEFT);
				GridData m_CreateSubjectLData = new GridData();
				m_CreateSubjectLData.horizontalAlignment = GridData.FILL;
				m_CreateSubjectLData.horizontalSpan = 2;
				m_CreateSubject.setLayoutData(m_CreateSubjectLData);
				m_CreateSubject.setText("Create nonexistent subjects");
				m_CreateSubject.setSelection(XNDApp.app_Prefs.getBoolean(
						"defaultXNATUploadCreateSubject", true));
			}
		}
		Composite composite = (Composite) super.createDialogArea(parent);
		XNDApp.EndWaitCursor();
		return composite;
	}
	private void FindExperiments(Collection<CElement> cce)
	{
		VirtualFolder vf;
		String lbl;
		for (CElement ce : cce)
		{
			if (ce instanceof VirtualFolder)
			{
				vf = (VirtualFolder) ce;
				lbl = vf.GetLabel();
				if (lbl.startsWith("Project:") || lbl.startsWith("Subject:"))
				{
					Collection<CElement> che = vf.GetChildren(new TypeFilter(
							TypeFilter.VFOLDER, false), null);
					FindExperiments(che);
				} else if (lbl.startsWith("Experiment:"))
				{
					m_Experiments.add(vf);
				}
			}
		}
	}

	private TreeMap<String, Collection<DBElement>> DistributeByTagPath(
			String[] tags)
	{
		TreeMap<String, Collection<DBElement>> res = new TreeMap<String, Collection<DBElement>>();
		Collection<DBElement> col;
		String val;
		TreeIterator ti = new TreeIterator(m_elements, new TypeFilter(
				TypeFilter.FSFOLDER | TypeFilter.DBITEM | TypeFilter.VFOLDER,
				false));
		CElement ce;
		DBElement dbe;
		while ((ce = ti.Next()) != null)
		{
			if (ce instanceof DBElement)
			{
				dbe = (DBElement) ce;
				if ((val = Utils.MakePath(dbe.GetIR(), tags)) == null)
					continue;
				if (res.containsKey(val))
				{
					col = res.get(val);
					col.add(dbe);
				} else
				{
					col = new LinkedList<DBElement>();
					col.add(dbe);
					res.put(val, col);
				}
			}
		}
		return res;
	}

	private long[] ElementStats(DBElement el)
	{

		File f0 = el.GetIR().getFile();

		try
		{
			if (el.IsCollection())
			{
				Collection<String> files = m_rvm.GetAllFiles(el.GetIR());
				long llen = 0;
				for (String p : files)
					llen += new File(m_rvm.GetAbsolutePath(p)).length();
				return new long[]{files.size(), llen};
			} else
			{
				return new long[]{1, f0.length()};
			}
		} catch (Exception e)
		{
			return null;
		}
	}

	private long[] FileStats(String tagPath)
	{
		long[] info = {0, 0};
		Collection<DBElement> recs = m_RecordsMap.get(tagPath);
		File f;

		long[] stats;
		for (final DBElement dbe_next : recs)
		{
			stats = ElementStats(dbe_next);
			info[0] += stats[0];
			info[1] += stats[1];
		}
		return info;
	}

	private void UpdateTotalSize(TableItem item, boolean bAdd)
	{
		long sz = Utils.SizeFromFormattedStr(item.getText(FIELD_SIZE));
		m_TotalSize = (bAdd) ? (m_TotalSize + sz) : Math.max(0,
				(m_TotalSize - sz));
		UpdateLabels();
	}
	private void UpdateLabels()
	{
		// m_TotalSizeLabel.setText("Estimated compressed upload size: "+Utils.GetFormattedSize(m_TotalSize/2));
	}
	public ItemTag GetSelectedTag()
	{
		return m_SelectedTag;
	}
	@Override
	protected void okPressed()
	{
		// XNDApp.app_Prefs.put("defaultXNATUploadServer",
		// m_ServerText.getText());
		// XNDApp.app_Prefs.put("defaultXNATUploadUser", m_UserText.getText());
		// XNDApp.app_Prefs.put("defaultXNATPass", m_PasswordText.getText());
		m_UserTextM.Save();
		m_ServerTextM.Save();
		XNDApp.app_Prefs.putBoolean("defaultXNATUploadCreateSubject",
				m_CreateSubject.getSelection());
		try
		{
			XNDApp.app_Prefs.flush();
		} catch (Exception e)
		{
		}
		// TreeMap<String,Collection<DBElement>> selected = new
		// TreeMap<String,Collection<DBElement>>();
		LinkedList<VirtualFolder> selected = new LinkedList<VirtualFolder>();
		TableItem[] ti = m_ExpTable.getItems();
		String tagPath;
		for (int i = 0; i < ti.length; i++)
		{
			if (ti[i].getChecked())
				selected.add(m_Experiments.get(i));
		}

		StoreXARManager sxm = new StoreXARManager(m_ServerText.getText(),
				m_UserText.getText(), m_PasswordText.getText(), m_rvm,
				selected, m_CreateSubject.getSelection());
		if (!sxm.IsValid())
		{
			MessageBox mb = new MessageBox(new Shell());
			mb.setText("Invalid input");
			mb
					.setMessage("Could not validate connection parameters. Please see console for details.");
		} else
		{
			try
			{
				sxm.setUser(true);
				sxm.schedule();
			} catch (Exception e)
			{
			}

		}
		super.okPressed();
	}
}
