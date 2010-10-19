package org.nrg.xnd.ui.dialogs;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.ui.MemoCheckBox;
import org.nrg.xnd.ui.MemoCombo;
import org.nrg.xnd.ui.wizards.ImportWizard;

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
public class EditTagSetDialog extends Dialog
{
	private ItemTag[] m_tags;
	private RepositoryViewManager m_rvm;
	private LinkedList<Button> m_SystemTagNames = new LinkedList<Button>();
	private LinkedList<Combo> m_SystemTagVals = new LinkedList<Combo>();
	private LinkedList<MemoCombo> m_SystemTagMems = new LinkedList<MemoCombo>();
	private LinkedList<MemoCheckBox> m_SystemTagCheckMems = new LinkedList<MemoCheckBox>();
	public Collection<ItemTag> m_definedTags = null;

	public EditTagSetDialog(Shell parentShell, ItemTag[] tags,
			RepositoryViewManager rvm)
	{
		super(parentShell);
		m_tags = tags;
		m_rvm = rvm;
	}
	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Set predefined tags");
	}
	public Collection<ItemTag> getDefinedSystemTags()
	{
		LinkedList<ItemTag> llit = new LinkedList<ItemTag>();
		for (int i = 0; i < m_SystemTagNames.size(); i++)
		{
			if (m_SystemTagNames.get(i).getSelection())
				llit.add(new ItemTag(m_SystemTagNames.get(i).getText(),
						m_SystemTagVals.get(i).getText()));
		}
		return llit;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 4;
		parent.setLayout(parentLayout);
		// Composite composite = (Composite) super.createDialogArea(parent);
		// GridLayout compositeLayout = new GridLayout();
		// compositeLayout.numColumns = 4;
		// composite.setLayout(compositeLayout);
		// m_TagLabels = new Label[m_tags.length];
		// m_TagText=new Text[m_tags.length];
		// m_TagInput = new Combo[m_tags.length];
		// if (m_tags.length < 1)
		// return composite;
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.verticalIndent = 5;
		if (XNDApp.app_Platform == XNDApp.PLATFORM_MAC)
			gd.widthHint = 134;

		Collection<TagDescr> tds = getSysTags();
		for (TagDescr td : tds)
		{
			Button cb = new Button(parent, SWT.CHECK | SWT.LEFT);
			cb.setText(td.GetName());
			MemoCheckBox mcb = new MemoCheckBox(cb, "EditTagSetDialog."
					+ td.GetName() + ".check", false);
			Combo c = new Combo(parent, SWT.NONE);
			MemoCombo mc = new MemoCombo(c, "EditTagSetDialog." + td.GetName(),
					10);
			m_SystemTagNames.add(cb);
			m_SystemTagVals.add(c);
			m_SystemTagMems.add(mc);
			m_SystemTagCheckMems.add(mcb);
		}

		/*
		 * // gd.heightHint = 16; for (int i = 0; i < m_tags.length; i++) {
		 * TagDescr td = m_rvm.GetTagDescr(m_tags[i].GetName()); if (td != null
		 * && td.IsSet(TagDescr.INVISIBLE)) continue; m_TagLabels[i] = new
		 * Label(composite, SWT.NONE);
		 * m_TagLabels[i].setText(m_tags[i].GetName());
		 * m_TagLabels[i].setLayoutData(gd); if
		 * (!m_rvm.CheckTagProperty(m_tags[i].GetName(),
		 * TagDescr.PREDEF_VALUES)) { m_TagInput[i] = new Combo(composite,
		 * SWT.SINGLE | SWT.WRAP | SWT.BORDER);
		 * m_TagInput[i].setText(m_tags[i].GetFirstValue()); } else {
		 * m_TagInput[i] = new Combo(composite, SWT.SINGLE | SWT.WRAP |
		 * SWT.BORDER | SWT.READ_ONLY);
		 * m_TagInput[i].setItems(m_rvm.GetTagDescr(m_tags[i].GetName())
		 * .GetValues()); } m_TagInput[i].setLayoutData(gd); }
		 */
		// composite.setSize(223, 104);
		return parent;
	}
	private Collection<TagDescr> getSysTags()
	{
		TagDescr[] tgs = DefaultOntologyManager.GetDefaultTagDescrs();
		LinkedList<TagDescr> ltd = new LinkedList<TagDescr>();
		for (TagDescr td : tgs)
		{
			if (!td.IsSet(TagDescr.SYSTEM))
				continue;
			ltd.add(td);
		}
		return ltd;
	}
	/*
	 * public ItemTag[] GetTags() { Vector v = new Vector(); for (int i = 0; i <
	 * m_tags.length; i++) { if (m_tags[i].GetFirstValue().length() > 0)
	 * v.add(m_tags[i]); } ItemTag[] neTags = new ItemTag[v.size()]; for (int i
	 * = 0; i < v.size(); i++) neTags[i] = (ItemTag) v.get(i); return neTags; }
	 */
	@Override
	protected void okPressed()
	{
		m_definedTags = getDefinedSystemTags();
		super.okPressed();
	}
}