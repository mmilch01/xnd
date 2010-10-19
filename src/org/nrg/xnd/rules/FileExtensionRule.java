package org.nrg.xnd.rules;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.DBElement;
import org.nrg.xnd.model.RepositoryViewManager;

public class FileExtensionRule extends Rule
{
	@Override
	protected boolean ParseRuleDescriptor()
	{
		return true;
	}
	public static final int FORMAT_ANALYZE = 1, FORMAT_IFH = 2,
			FORMAT_NRRD = 4;
	private int m_format = 0;

	public FileExtensionRule(RepositoryViewManager rvm, String uid)
	{
		super(rvm, uid);
		m_format = FORMAT_ANALYZE | FORMAT_IFH | FORMAT_NRRD;
	}
	public FileExtensionRule(RepositoryViewManager rvm, String uid, int formats)
	{
		super(rvm, uid);
		m_format = formats;
	}
	@Override
	public boolean ApplyRule(Collection<CElement> cce, IProgressMonitor ipm)
	{
		DBElement dbe;
		File f;
		String nm, ex;
		ItemTag tgAn = new ItemTag("coll_format", "ANALYZE"), tg4dfp = new ItemTag(
				"coll_format", "IFH"), tgNrrd = new ItemTag("coll_format",
				"NRRD");

		for (CElement ce : cce)
		{
			if (!(ce instanceof DBElement))
				continue;
			dbe = (DBElement) ce;
			f = dbe.GetIR().getFile();
			if (f == null || !f.exists())
				continue;
			ex = getFileExtension(f);
			nm = getFileName(f);
			if (ex == null || ex.length() < 1 || nm == null || nm.length() < 1)
				continue;
			if (ex.toLowerCase().compareTo("nrrd") == 0)
			{
				ItemTag[] tags = {tgNrrd};
				dbe.ApplyOperation(tags, CElement.SETTAGS, ipm);
			} else if ((m_format | FORMAT_IFH) != 0)
			{
				if (new File(f.getParent() + "/" + nm + ".ifh").exists()
						&& new File(f.getParent() + "/" + nm + ".img").exists())
				{
					if (ex.compareTo("ifh") == 0 || ex.compareTo("hdr") == 0
							|| ex.compareTo("img") == 0
							|| ex.compareTo("rec") == 0)
					{
						ItemTag[] tags = {tg4dfp};
						dbe.ApplyOperation(tags, CElement.SETTAGS, ipm);
					}
				}
			} else if ((m_format | FORMAT_ANALYZE) != 0)
			{
				if (new File(f.getParent() + "/" + nm + ".hdr").exists()
						&& new File(f.getParent() + "/" + nm + ".img").exists())
				{
					if (ex.compareTo("hdr") == 0 || ex.compareTo("img") == 0)
					{
						ItemTag[] tags = {tgAn};
						dbe.ApplyOperation(tags, CElement.SETTAGS, ipm);
					}
				}
			}
		}
		return true;
	}
	public static String getFileExtension(File f)
	{
		int ind = f.getName().lastIndexOf(".");
		if (ind < 0)
			return null;
		return f.getName().substring(ind + 1);
	}
	public String getFileName(File f)
	{
		String nm = f.getName();
		int ind = f.getName().lastIndexOf(".");
		if (ind < 0)
			return nm;
		return nm.substring(0, ind - 1);
	}
}