package org.nrg.xnd.rules;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.jface.window.Window;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.utils.Utils;

public abstract class RuleManager
{
	private static TreeMap<String, Rule> m_rules = new TreeMap<String, Rule>();

	public static void LoadRules()
	{
		m_rules.clear();
		loadRulesEx(false);
		loadRulesEx(true);
	}
	private static void loadRulesEx(boolean bMacro)
	{
		File rf = new File(getRuleFolder());
		File[] files = rf.listFiles();
		for (File f : files)
			loadRule(f, bMacro, false);

		// pre-existing custom rules.
		rf = new File(Utils.GetPluginPath() + "/xml_resources/customRules");
		files = rf.listFiles();
		for (File f : files)
			loadRule(f, bMacro, false);

		// pre-existing default rules.
		for (int i = 0; i < Rule.DEFAULT_RULES.length; i++)
		{
			if ((i == Rule.RULE_MACRO && !bMacro)
					|| ((i != Rule.RULE_MACRO) && bMacro))
				continue;

			if (!m_rules.containsKey(Rule.DEFAULT_RULES[i]))
				LoadDefaultRule(i);
		}
	}
	public static void updateRule(Rule r)
	{
		m_rules.remove(r.getuid());
		m_rules.put(r.getuid(), r);
	}
	public static String getRuleFolder()
	{
		return Utils.GetUserFolder() + "/rules";
	}

	public static Rule getRule(String id)
	{
		return m_rules.get(id);
	}
	public static void saveRules()
	{
		for (Rule r : m_rules.values())
		{
			if (r instanceof Macro)
				((Macro) r).Save();
			else
				r.saveRule();
		}
	}

	public static void deleteRule(String name)
	{
		Rule r = m_rules.get(name);
		File f = r.getFile();
		if (f != null)
			f.delete();
		m_rules.remove(name);
	}
	public static TreeMap<String, Rule> getRules()
	{
		return m_rules;
	}
	public static Collection<Rule> getCustomRulesOnly()
	{
		LinkedList<Rule> llr = new LinkedList<Rule>();
		for (Rule r : m_rules.values())
		{
			if (r.isDefault())
				continue;
			llr.add(r);
		}
		return llr;
	}
	public static Collection<Rule> getRuleCollection()
	{
		return m_rules.values();
	}
	public static Rule getDefaultRule(int id)
	{
		return m_rules.get(Rule.DEFAULT_RULES[id]);
	}
	public static Rule loadRule(File f)
	{
		return loadRule(f, false, true);
	}
	public static Rule loadRule(File f, boolean bMacro, boolean bAll)
	{
		try
		{
			Document d = new SAXReader().read(f);
			Element el = d.getRootElement();
			if (el.getName().toLowerCase().compareTo("rule") != 0)
				return null;
			String type = el.attributeValue("type").toLowerCase(), uid = el
					.attributeValue("id").toLowerCase();
			boolean bisM = type.compareTo(Rule.TYPES[Rule.RULE_MACRO]) == 0;
			if (!bAll)
			{
				if ((bMacro && !bisM) || (!bMacro && bisM))
					return null;
			}
			int itype = -1;
			for (int i = 0; i < Rule.TYPES.length; i++)
			{
				if (Rule.TYPES[i].compareTo(type) == 0)
				{
					itype = i;
					break;
				}
			}
			if (itype < 0 || uid == null || m_rules.containsKey(uid))
				return null;
			Rule r = Rule.getInstance(itype, uid, XNDApp.app_localVM);
			r.LoadRuleDescriptor(f);
			if (!m_rules.containsKey(r.getuid()))
			{
				m_rules.put(r.getuid(), r);
				return r;
			} else
			{
				Utils.ShowMessageBox("Rule not added", "Duplicate rule: "
						+ "\"" + r.getuid() + "\"", Window.OK);
				return null;
			}
		} catch (Exception e)
		{
			Utils.ShowMessageBox("Exception adding rule:", e.getMessage(), Window.OK);
			return null;
		}
	}
	private static boolean LoadDefaultRule(int rule_id)
	{
		Rule r = Rule.getInstance(rule_id, Rule.defaultName(rule_id),
				XNDApp.app_localVM);
		if (r == null)
			return false;
		if (r.loadDefaultDescriptor())
		{
			m_rules.put(r.getuid(), r);
			return true;
		}
		return false;
	}
}
