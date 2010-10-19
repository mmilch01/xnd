package org.nrg.xnd.tools;
import java.util.Collection;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.TagMap;
import org.nrg.fileserver.TagSet;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.VirtualFolder;

public class ClipboardManager
{
	private TagMap m_tags = new TagMap();
	private final Clipboard m_cb = new Clipboard(new Display());
	public void toClipboard(Collection<CElement> src)
	{
		ItemRecord tagRecord;
		m_tags.clear();

		for (CElement ce : src)
		{
			tagRecord = null;
			if (ce instanceof VirtualFolder)
			{
				tagRecord = ((VirtualFolder) ce).getAssociatedTags();
			} else if (ce instanceof DBElement)
			{
				tagRecord = ((DBElement) ce).GetIR();
			}
			if (tagRecord != null)
			{
				m_tags.mergeTags(tagRecord.getTagCollection());
			}
		}
		TextTransfer tt = TextTransfer.getInstance();
		m_cb.clearContents();
		String text = m_tags.toString();
		m_cb.setContents(new Object[]{text}, new Transfer[]{tt});
	}
	public TagSet fromClipboard()
	{
		m_tags.clear();
		String text = (String) m_cb.getContents(TextTransfer.getInstance());
		m_tags.fromString(text);
		return m_tags;
	}
}