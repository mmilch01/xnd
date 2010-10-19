package org.nrg.xnd.rules;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.ItemRecord;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.RepositoryViewManager;
import org.nrg.xnd.rules.dicom.DICOMRule;
import org.nrg.xnd.utils.Utils;

public abstract class Rule
{
	public static final int RULE_DICOM = 0, RULE_NAMING = 1, RULE_COL = 2,
			RULE_FILEEXT = 3, RULE_MODTAG = 4, RULE_MACRO = 5;
	public static final String[] DEFAULT_RULES = {"defaultdicom",
			"defaultname", "defaultcol", "defaultfileext", "defaultmodifytag",
			"defaultmacro"};
	public static final String[] TYPES = {"dicom", "naming", "collection",
			"file_ext", "modtag", "macro"};

	protected RepositoryViewManager m_rvm;
	protected String m_uid;
	protected boolean m_bSpecialRecursion = false;
	protected Document m_xmldoc = null;
	protected int m_type = -1;
	public static String defaultName(int id)
	{
		return DEFAULT_RULES[id];
	}
	public String getuid()
	{
		return m_uid;
	}
	public void setuid(String uid)
	{
		m_uid = uid;
	}

	public boolean isDefault()
	{
		if (m_type < 0)
			return false;
		return m_uid.compareTo(DEFAULT_RULES[m_type]) == 0;
	}
	public static Rule getInstance(int id, String uid, RepositoryViewManager rm)
	{
		switch (id)
		{
			case RULE_DICOM :
				return new DICOMRule(uid, rm);
			case RULE_NAMING :
				return new NameRule(rm, uid);
			case RULE_FILEEXT :
				return new FileExtensionRule(rm, uid);
			case RULE_COL :
				return CollectionRule.DefaultCollectionRule(rm);
			case RULE_MODTAG :
				return new ModifyTagValueRule(rm, uid);
			case RULE_MACRO :
				return new Macro(rm, uid);
		}
		return null;
	}
	public String getTypeName()
	{
		if (m_type == -1)
			return "undefined";
		return TYPES[m_type];
	}
	public Rule(RepositoryViewManager rm, String uid)
	{
		m_uid = uid;
		m_rvm = rm;
		determineType();
	}
	public Rule(RepositoryViewManager rm)
	{
		m_rvm = rm;
		determineType();
		m_uid = getDefaultName();
	}
	public int getType()
	{
		return m_type;
	}
	public File getFile()
	{
		return new File(RuleManager.getRuleFolder() + "/" + m_uid + ".xml");
	}
	public boolean isSpecialRecursion()
	{
		return m_bSpecialRecursion;
	}
	private void determineType()
	{
		if (this instanceof CollectionRule)
		{
			m_type = RULE_COL;
			m_bSpecialRecursion = true;
		} else if (this instanceof FileExtensionRule)
			m_type = RULE_FILEEXT;
		else if (this instanceof ModifyTagValueRule)
			m_type = RULE_MODTAG;
		else if (this instanceof NameRule)
		{
			m_type = RULE_NAMING;
			// m_bSpecialRecursion=true;
		} else if (this instanceof DICOMRule)
		{
			m_type = RULE_DICOM;
			m_bSpecialRecursion = true;
		} else if (this instanceof Macro)
		{
			m_type = RULE_MACRO;
			m_bSpecialRecursion = true;
		} else
			m_type = -1; // should not happen.
	}
	public String getDefaultName()
	{
		return DEFAULT_RULES[m_type];
	}
	public String getDefaultFile()
	{
		return new File(Utils.GetPluginPath() + "/xml_resources/defaultRules/"
				+ getDefaultName() + "_rule.xml").getAbsolutePath();
	}

	public abstract boolean ApplyRule(Collection<CElement> cce,
			IProgressMonitor ipm);
	public boolean ApplyRule(CElement ce, IProgressMonitor ipm)
	{
		Collection<CElement> cce = new LinkedList<CElement>();
		cce.add(ce);
		return ApplyRule(cce, ipm);
	}
	public boolean loadDefaultDescriptor()
	{
		return LoadRuleDescriptor(new File(getDefaultFile()));
	}
	public boolean LoadRuleDescriptor(File f)
	{
		try
		{
			m_xmldoc = new SAXReader().read(f);
			return ParseRuleDescriptor();
		} catch (Exception e)
		{
			return false;
		}
	}
	protected boolean LoadRuleDescriptor(InputStream is)
	{
		try
		{
			m_xmldoc = new SAXReader().read(is);
			return ParseRuleDescriptor();
		} catch (Exception e)
		{
			return false;
		}
	}
	protected abstract boolean ParseRuleDescriptor();
	public boolean GetUnaffectedRecords(Collection<ItemRecord> c)
	{
		return false;
	}
	public boolean saveRule()
	{
		if (m_xmldoc == null)
			return true;
		OutputFormat format = OutputFormat.createPrettyPrint();
		try
		{
			XMLWriter w = new XMLWriter(new FileWriter(getFile()), format);
			w.write(m_xmldoc);
			w.close();
			return true;
		} catch (Exception e)
		{
			return false;
		}
	}
}