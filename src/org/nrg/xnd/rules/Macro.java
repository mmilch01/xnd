package org.nrg.xnd.rules;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.eclipse.core.runtime.IProgressMonitor;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.model.CElement;
import org.nrg.xnd.model.RepositoryViewManager;

public class Macro extends Rule
{
	private LinkedList<Operation> m_operations = new LinkedList<Operation>();
	public Macro(RepositoryViewManager rvm, String uid)
	{
		super(rvm, uid);
	}

	public Collection<String> getOperationList()
	{
		LinkedList<String> ops = new LinkedList<String>();
		for (Operation op : m_operations)
		{
			String s = op.getDescription();
			ops.add(s);
		}
		return ops;
	}
	@Override
	public boolean ApplyRule(Collection<CElement> cce, IProgressMonitor ipm)
	{
		for (Operation op : m_operations)
			for (CElement ce : cce)
				op.applyOperation(ce, ipm);
		return true;
	}

	@Override
	protected boolean ParseRuleDescriptor()
	{
		return Serialize(m_xmldoc.getRootElement(), true);
	}

	public boolean Save()
	{
		if (m_xmldoc != null)
			m_xmldoc.clearContent();
		else
			m_xmldoc = DocumentHelper.createDocument();
		if (!Serialize(m_xmldoc.addElement("rule"), false))
			return false;
		return saveRule();
	}

	public boolean Serialize(Element el, boolean is_loading)
	{
		if (is_loading)
		{
			m_operations.clear();
			Element e;
			Operation op;
			for (Iterator<Element> iel = el.elementIterator(); iel.hasNext();)
			{
				e = iel.next();
				if (e.getName().compareTo("operation") == 0)
				{
					m_operations.add(new Operation(e));
				}
			}
			return true;
		} else
		{
			el.setName("rule");
			el.addAttribute("type", "macro");
			el.addAttribute("id", m_uid);
			for (Operation op : m_operations)
			{
				op.SerializeXML(el.addElement("operation"), false);
			}
		}
		return true;
	}
	public void addOp(int type, Object op)
	{
		m_operations.add(new Operation(type, op));
	}
	public void removeOp(int pos)
	{
		m_operations.remove(pos);
	}
	public void moveOp(int pos, int dir)
	{
		if (dir > 0) // up
		{
			if (pos == m_operations.size() - 1)
				return;
			Operation op = m_operations.get(pos);
			m_operations.remove(pos);
			m_operations.add(pos + 1, op);
		} else
		// down
		{
			if (pos == 0)
				return;
			Operation op = m_operations.get(pos - 1);
			m_operations.remove(pos - 1);
			m_operations.add(pos, op);
		}
	}
	public static String[] getOpTypeList()
	{
		String[] types = {"Manage", "Set tag", "Apply rule"};
		return types;
	}

	public class Operation
	{
		public static final int MANAGE = 0, SETTAG = 1, APPLYRULE = 2;
		public int m_type = -1;
		public Rule m_rule = null;
		public ItemTag m_tag = null;

		public String getDescription()
		{
			switch (m_type)
			{
				case MANAGE :
					return "Manage file or folder";
				case SETTAG :
					return "Set tag \"" + m_tag.GetName() + "\" to \""
							+ m_tag.GetFirstValue() + "\"";
				case APPLYRULE :
					return "Apply \""
							+ m_rule.getuid()
							+ "\" rule (type: "
							+ ((m_rule != null)
									? (m_rule.getTypeName())
									: ("(null)")) + ")";
				default :
					return null;
			}
		}
		public Operation(int type, Object op)
		{
			switch (type)
			{
				case MANAGE :
					m_type = type;
					return;
				case SETTAG :
					m_type = type;
					m_tag = (ItemTag) op;
					return;
				case APPLYRULE :
					m_type = type;
					m_rule = (Rule) op;
					return;
			}
		}
		public Operation(Element el)
		{
			SerializeXML(el, true);
		}
		public boolean SerializeXML(Element el, boolean is_loading)
		{
			if (is_loading)
			{
				if (el.attributeValue("type").compareTo("manage") == 0)
				{
					m_type = MANAGE;
				} else if (el.attributeValue("type").compareTo("applyrule") == 0)
				{
					m_type = APPLYRULE;
					m_rule = RuleManager.getRule(el.attributeValue("ruleID"));
				} else if (el.attributeValue("type").compareTo("settag") == 0)
				{
					m_type = SETTAG;
					m_tag = new ItemTag(el.attributeValue("tagName"), el
							.attributeValue("tagValue"));
				}
				return isValid();
			} else
			{
				if (!isValid())
					return false;
				el.setName("operation");
				switch (m_type)
				{
					case MANAGE :
						el.addAttribute("type", "manage");
						return true;
					case SETTAG :
						el.addAttribute("type", "settag");
						el.addAttribute("tagName", m_tag.GetName());
						el.addAttribute("tagValue", m_tag.GetFirstValue());
						return true;
					case APPLYRULE :
						el.addAttribute("type", "applyrule");
						el.addAttribute("ruleID", m_rule.getuid());
						return true;
					default :
						return false;
				}
			}
		}

		private boolean isValid()
		{
			if (m_type == -1)
				return false;
			switch (m_type)
			{
				case MANAGE :
					return true;
				case SETTAG :
					return m_tag != null && m_tag.GetName() != null
							&& m_tag.GetName().length() > 0;
				case APPLYRULE :
					return m_rule != null;
				default :
					return false;
			}
		}
		public void applyOperation(CElement el, IProgressMonitor pm)
		{
			if (!isValid())
				return;
			switch (m_type)
			{
				case MANAGE :
					el.ApplyOperation(null, CElement.MANAGEALL, pm);
					break;
				case SETTAG :
					ItemTag[] tags = new ItemTag[1];
					tags[0] = m_tag;
					el.ApplyOperation(tags, CElement.SETTAGS, pm);
					break;
				case APPLYRULE :
					if (m_rule.isSpecialRecursion())
						m_rule.ApplyRule(el, pm);
					else
						el.ApplyOperation(m_rule, -1, pm);
					break;
			}
		}
	}
}
