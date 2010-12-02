package org.nrg.xnd.ui.wizards;

import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.utils.dicom.AEList.AE;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DateAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.IntegerStringAttribute;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TimeAttribute;
import com.pixelmed.query.QueryTreeModel;
import com.pixelmed.query.QueryTreeRecord;
import com.pixelmed.query.StudyRootQueryInformationModel;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
public class QRWizardPage2 extends WizardPage
{
	private Group group1;
	private Table m_StudyTable;
	private Group group3;
	private Label label2;
	private Text m_PatID;
	private Label label3;
	private Text m_PatName;
	private Label label4;
	private Combo m_StBeginDate;
	private MemoCombo m_StBeginDateM;
	private Label label5;
	private Combo m_StEndDate;
	private MemoCombo m_StEndDateM;
	private Label label6;
	private Text m_AccNumText;
	private Label m_statusLabel;
	private Button m_QueryButton;
	private Label label7;
	private Combo m_ModalityCombo;
	private MemoCombo m_ModalityComboM;

	StudyRootQueryInformationModel m_qim;
	QueryTreeModel m_qtm;

	public Collection<AttributeList> getUniqueKeys()
	{
		QueryTreeRecord root = (QueryTreeRecord) m_qtm.getRoot();
		// int nch=root.getChildCount();
		int i = 0;
		LinkedList<AttributeList> al_res = new LinkedList<AttributeList>();
		for (Enumeration e = root.children(); e.hasMoreElements();)
		{
			Object nxto = e.nextElement();
			if (!(nxto instanceof QueryTreeRecord))
				continue;
			if (!m_StudyTable.getItem(i++).getChecked())
				continue;
			QueryTreeRecord nxt = (QueryTreeRecord) nxto;
			al_res.add(nxt.getUniqueKeys());
		}
		return al_res;
	}

	@Override
	public boolean isPageComplete()
	{
		if (m_StudyTable.getItemCount() < 1)
		{
			m_statusLabel.setText("No studies to import");
			return false;
		}
		int nSel = 0;
		for (TableItem ti : m_StudyTable.getItems())
		{
			if (ti.getChecked())
				nSel++;
		}
		if (nSel == 0)
		{
			m_statusLabel.setText("Select studies to retrieve");
			return false;
		}
		m_statusLabel.setText(nSel + ((nSel == 1) ? " study" : " studies")
				+ " selected");
		return true;
	}

	public QRWizardPage2(String pageName)
	{
		super(pageName);
	}
	public QRWizardPage2()
	{
		super("QWPage2", "Select studies to retrieve", null);
	}
	public QRWizardPage2(String pageName, String title,
			ImageDescriptor titleImage)
	{
		super(pageName, title, titleImage);
	}

	/**
	 * @return filter attributes for the DICOM query, null if error detected.
	 */
	public AttributeList getQueryFilter()
	{
		AttributeList filter = new AttributeList();
		try
		{
			{
				AttributeTag t = TagFromName.PatientName;
				Attribute a = new PersonNameAttribute(t);
				a.setValue(m_PatName.getText());
				filter.put(t, a);
			}
			{
				AttributeTag t = TagFromName.PatientID;
				Attribute a = new LongStringAttribute(t);
				a.setValue(m_PatID.getText());
				filter.put(t, a);
			}
			{
				AttributeTag t = TagFromName.AccessionNumber;
				Attribute a = new ShortStringAttribute(t);
				a.setValue(m_AccNumText.getText());
				filter.put(t, a);
			}
			{
				String dt_int = m_StBeginDate.getText() + "-"
						+ m_StEndDate.getText();
				AttributeTag t = TagFromName.StudyDate;
				Attribute a = new DateAttribute(t);
				if (dt_int.length() == 9 || dt_int.length() == 17)
					a.setValue(dt_int);
				filter.put(t, a);
			}
			{
				AttributeTag t = TagFromName.StudyDescription;
				Attribute a = new LongStringAttribute(t);
				filter.put(t, a);
			}
			{
				AttributeTag t = TagFromName.ModalitiesInStudy;
				Attribute a = new CodeStringAttribute(t);
				a.setValue(m_ModalityCombo.getText());
				filter.put(t, a);
			}
			{
				AttributeTag t = TagFromName.StudyTime;
				Attribute a = new TimeAttribute(t);
				filter.put(t, a);
			}
			{
				AttributeTag t = TagFromName.NumberOfStudyRelatedInstances;
				Attribute a = new IntegerStringAttribute(t);
				filter.put(t, a);
			}
			{
				AttributeTag t = TagFromName.NumberOfStudyRelatedSeries;
				Attribute a = new IntegerStringAttribute(t);
				filter.put(t, a);
			}
			{
				AttributeTag t = TagFromName.NumberOfSeriesRelatedInstances;
				Attribute a = new IntegerStringAttribute(t);
				filter.put(t, a);
			}
		} catch (DicomException e)
		{
			return null;
		}
		return filter;
	}

	public void createControl(Composite parent)
	{
		Composite topLevel = new Composite(parent, SWT.NONE);
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 2;
		topLevel.setLayout(parentLayout);
		{
			group3 = new Group(topLevel, SWT.NONE);
			GridLayout group3Layout = new GridLayout();
			group3Layout.numColumns = 4;
			group3.setLayout(group3Layout);
			GridData group3LData = new GridData();
			group3LData.horizontalAlignment = GridData.FILL;
			group3LData.grabExcessHorizontalSpace = true;
			group3.setLayoutData(group3LData);
			group3.setText("DICOM query parameters");
			{
				label2 = new Label(group3, SWT.NONE);
				GridData label2LData = new GridData();
				label2LData.horizontalAlignment = GridData.END;
				label2.setLayoutData(label2LData);
				label2.setText("Patient ID:");
			}
			{
				GridData m_PatIDLData = new GridData();
				m_PatIDLData.horizontalAlignment = GridData.FILL;
				m_PatIDLData.grabExcessHorizontalSpace = true;
				m_PatID = new Text(group3, SWT.BORDER);
				m_PatID.setLayoutData(m_PatIDLData);
				m_PatID.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});
			}
			{
				label3 = new Label(group3, SWT.NONE);
				GridData label3LData = new GridData();
				label3LData.horizontalAlignment = GridData.END;
				label3.setLayoutData(label3LData);
				label3.setText("Patient name:");
			}
			{
				GridData m_PatNameLData = new GridData();
				m_PatNameLData.horizontalAlignment = GridData.FILL;
				m_PatNameLData.grabExcessHorizontalSpace = true;
				m_PatName = new Text(group3, SWT.BORDER);
				m_PatName.setLayoutData(m_PatNameLData);
				m_PatName.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});
			}
			{
				label4 = new Label(group3, SWT.NONE);
				GridData label4LData = new GridData();
				label4LData.horizontalAlignment = GridData.END;
				label4.setLayoutData(label4LData);
				label4.setText("Study date, between\n (format: yyyymmdd)");
			}
			{
				GridData m_StBeginDateLData = new GridData();
				m_StBeginDateLData.horizontalAlignment = GridData.FILL;
				m_StBeginDateLData.grabExcessHorizontalSpace = true;
				m_StBeginDate = new Combo(group3, SWT.NONE);
				m_StBeginDate.setLayoutData(m_StBeginDateLData);
				m_StBeginDate.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});
				m_StBeginDateM = new MemoCombo(m_StBeginDate,
						"QRWizardPage2.StBeginDate", 10);
			}
			{
				label5 = new Label(group3, SWT.NONE);
				GridData label5LData = new GridData();
				label5LData.horizontalAlignment = GridData.END;
				label5LData.grabExcessHorizontalSpace = true;
				label5.setLayoutData(label5LData);
				label5.setText("and");
			}
			{
				GridData combo1LData = new GridData();
				combo1LData.horizontalAlignment = GridData.FILL;
				combo1LData.grabExcessHorizontalSpace = true;
				m_StEndDate = new Combo(group3, SWT.NONE);
				m_StEndDate.setLayoutData(combo1LData);
				m_StEndDate.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});
				m_StEndDateM = new MemoCombo(m_StEndDate,
						"QRWizardPage2.StEndDate", 10);
			}
			{
				label6 = new Label(group3, SWT.NONE);
				GridData label6LData = new GridData();
				label6LData.horizontalAlignment = GridData.END;
				label6.setLayoutData(label6LData);
				label6.setText("Accession number:");
			}
			{
				GridData m_AccNumTextLData = new GridData();
				m_AccNumTextLData.horizontalAlignment = GridData.FILL;
				m_AccNumTextLData.grabExcessHorizontalSpace = true;
				m_AccNumText = new Text(group3, SWT.BORDER);
				m_AccNumText.setLayoutData(m_AccNumTextLData);
				m_AccNumText.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});
			}
			{
				label7 = new Label(group3, SWT.NONE);
				GridData label7LData = new GridData();
				label7LData.horizontalAlignment = GridData.END;
				label7.setLayoutData(label7LData);
				label7.setText("Modality:");
			}
			{
				GridData m_ModalityComboLData = new GridData();
				m_ModalityComboLData.horizontalAlignment = GridData.FILL;
				m_ModalityComboLData.grabExcessHorizontalSpace = true;
				m_ModalityCombo = new Combo(group3, SWT.NONE);
				m_ModalityCombo.setLayoutData(m_ModalityComboLData);
				m_ModalityCombo.addModifyListener(new ModifyListener()
				{
					public void modifyText(ModifyEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});
				m_ModalityComboM = new MemoCombo(m_ModalityCombo,
						"QRWizardPage2.ModalityCombo", 10);
			}
		}
		{
			m_QueryButton = new Button(topLevel, SWT.PUSH | SWT.CENTER);
			GridData m_QueryButtonLData = new GridData();
			m_QueryButtonLData.verticalAlignment = GridData.END;
			m_QueryButton.setLayoutData(m_QueryButtonLData);
			m_QueryButton.setText("Search for studies");
			m_QueryButton.addSelectionListener(new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent evt)
				{
					performQuery();
					InitTable();
					setPageComplete(isPageComplete());
				}
			});
		}
		{
			group1 = new Group(topLevel, SWT.NONE);
			group1.setLayout(null);
			GridData group1LData = new GridData();
			group1LData.horizontalAlignment = GridData.FILL;
			group1LData.grabExcessHorizontalSpace = true;
			group1LData.grabExcessVerticalSpace = true;
			group1LData.verticalAlignment = GridData.FILL;
			group1LData.horizontalSpan = 2;
			group1.setLayoutData(group1LData);
			group1.setText("Studies found");
			{
				m_StudyTable = new Table(group1, SWT.BORDER | SWT.CHECK
						| SWT.V_SCROLL | SWT.H_SCROLL | SWT.FOCUSED);

				m_StudyTable.setHeaderVisible(true);
				m_StudyTable.setLinesVisible(true);
				m_StudyTable.setBounds(7, 17, 820, 200);
				m_StudyTable.addMouseListener(new MouseAdapter()
				{
					public void mouseUp(MouseEvent evt)
					{
						setPageComplete(isPageComplete());
					}
				});

				addColumn("Patient Name");
				addColumn("Patient ID");
				addColumn("Study date");
				addColumn("Study time");
				addColumn("Descr.");
				addColumn("Modality");
				addColumn("Images");
				addColumn("Accession number");
				InitTable();
			}
		}
		{
			GridData m_statusLabelLData = new GridData();
			m_statusLabelLData.horizontalAlignment = GridData.FILL;
			m_statusLabelLData.horizontalSpan = 2;
			m_statusLabel = new Label(topLevel, SWT.NONE);
			m_statusLabel.setLayoutData(m_statusLabelLData);
		}
		setControl(topLevel);
		topLevel.addFocusListener(new FocusAdapter()
		{
			public void focusGained(FocusEvent evt)
			{
				// InitTable();
			}
		});
		setPageComplete(isPageComplete());
	}
	private void performQuery()
	{
		XNDApp.StartWaitCursor();
		try
		{
			AttributeList filter = getQueryFilter();
			if (filter == null)
			{
				m_qim = null;
				m_qtm = null;
				return;
			}
			AE ae = ((QRWizard) getWizard()).m_page1.getSelectedAE();
			AE loc_ae = XNDApp.app_aeList.getAE("local");
			m_qim = new StudyRootQueryInformationModel(ae.m_netName,
					ae.m_sendPort, ae.m_title, loc_ae.m_title, 1);
			m_qtm = m_qim.performHierarchicalQuery(filter);
		} catch (Exception e)
		{
		} finally
		{
			XNDApp.EndWaitCursor();
		}
	}
	public void InitTable()
	{
		m_StudyTable.removeAll();
		if (m_qtm != null)
		{
			if (!(m_qtm.getRoot() instanceof QueryTreeRecord))
				return;
			QueryTreeRecord root = (QueryTreeRecord) m_qtm.getRoot();
			// int nch=root.getChildCount();
			for (Enumeration e = root.children(); e.hasMoreElements();)
			{
				Object nxto = e.nextElement();
				if (!(nxto instanceof QueryTreeRecord))
					continue;
				QueryTreeRecord nxt = (QueryTreeRecord) nxto;
				AttributeList al = nxt.getAllAttributesReturnedInIdentifier();
				addRow(al);
			}
		}
		/*
		 * else //just add some empty rows { for(int i=0; i<10; i++) new
		 * TableItem(m_StudyTable,SWT.NONE); }
		 */
		// m_StudyTable.pack();
	}
	public void Save()
	{
		m_StBeginDateM.Save();
		m_StEndDateM.Save();
		m_ModalityComboM.Save();
	}
	private void addRow(AttributeList al)
	{
		TableItem ti = new TableItem(m_StudyTable, SWT.NONE);
		setCellValue(al, ti, TagFromName.PatientName, 0);
		setCellValue(al, ti, TagFromName.PatientID, 1);
		setCellValue(al, ti, TagFromName.StudyDate, 2);
		setCellValue(al, ti, TagFromName.StudyTime, 3);
		setCellValue(al, ti, TagFromName.StudyDescription, 4);
		setCellValue(al, ti, TagFromName.ModalitiesInStudy, 5);
		setCellValue(al, ti, TagFromName.NumberOfStudyRelatedInstances, 6);
		setCellValue(al, ti, TagFromName.AccessionNumber, 7);
	}
	private void setCellValue(AttributeList al, TableItem ti, AttributeTag at,
			int ind)
	{
		Attribute a = al.get(at);
		if (a != null)
			ti.setText(ind, a.getDelimitedStringValuesOrEmptyString());
	}
	private void addColumn(String name)
	{
		TableColumn tc = new TableColumn(m_StudyTable, SWT.LEFT);
		tc.setText(name);
		tc.setWidth(100);
	}
}