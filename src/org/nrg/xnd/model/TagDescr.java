package org.nrg.xnd.model;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import org.dom4j.Element;
import org.nrg.xnd.utils.Utils;

public class TagDescr implements Comparable
{
	public static final int VALUE_ALPHANUMERIC = 1, ROOT = 2, DEFAULT = 4,
			TABLE_DISPLAY = 8, MULTI_VALUE = 16, PREDEF_VALUES = 32,
			SYSTEM = 64, INVISIBLE = 128;
	private String m_TagName = "";
	private Collection<String> m_Values = null;
	private int m_Attribs = 0;

	public TagDescr()
	{
	}
	public TagDescr(String s)
	{
		m_TagName = s;
	}
	public TagDescr(String s, int attrs)
	{
		m_TagName = s;
		m_Attribs = attrs;
	}

	public String GetName()
	{
		return m_TagName;
	}
	public void SetName(String s)
	{
		m_TagName = s;
	}

	public void SetAttr(int properties)
	{
		m_Attribs |= properties;
	}
	public void RemoveAttr(int attribs)
	{
		m_Attribs = m_Attribs & (~attribs);
	}
	public void ToggleAttr(int attribs)
	{
		m_Attribs = (m_Attribs & (~attribs)) | (~m_Attribs & attribs);
	}
	public boolean IsSet(int attribs)
	{
		return ((m_Attribs & attribs) == attribs);
	}
	public boolean AttribsDefined(int attribs)
	{
		return ((m_Attribs & attribs) != 0 );
	}
	public void AddValue(String s)
	{
		if (m_Values == null)
			m_Values = new TreeSet<String>();
		m_Values.add(s);
	}
	public String[] GetValues()
	{
		if (m_Values == null || m_Values.size() < 1)
			return null;
		return m_Values.toArray(new String[0]);
	}
	public boolean Serialize(Element element, boolean is_loading)
	{
		if (is_loading)
		{
			m_TagName = element.attribute("Name").getValue();
			m_Attribs = new Integer(element.attribute("Code").getValue())
					.intValue();
			m_Values = new TreeSet<String>();
			for (Iterator<Element> it = element.elementIterator(); it.hasNext();)
			{
				if (element.getName().compareTo("Value") == 0)
					m_Values.add(element.getText());
			}
			if (m_Values.size() < 1)
				m_Values = null;
			return true;
		} else
		{
			element.addAttribute("Name", m_TagName);
			element.addAttribute("Code", new Integer(m_Attribs).toString());
			if (m_Values != null)
			{
				for (String val : m_Values)
					element.addElement("Value").setText(val);
			}
			return true;
		}
	}
	public boolean Serialize(Object stream, boolean is_loading)
	{
		try
		{
			if (is_loading)
			{
				ObjectInputStream in = (ObjectInputStream) stream;
				m_Attribs = in.readInt();
				m_TagName = Utils.SerializeString(stream, null, true);
				m_Values = new TreeSet<String>();
				Utils.SerializeCollection(stream, m_Values,
						Utils.SER_OBJ_STRING, is_loading);
				return true;
			} else
			{
				ObjectOutputStream out = (ObjectOutputStream) stream;
				out.writeInt(m_Attribs);
				Utils.SerializeString(stream, m_TagName, false);
				if (m_Values == null)
					m_Values = new TreeSet<String>();
				Utils.SerializeCollection(stream, m_Values,
						Utils.SER_OBJ_STRING, is_loading);
				return true;
			}
		} catch (Exception e)
		{
			return false;
		}
	}
	public int compareTo(Object o)
	{
		return m_TagName.compareTo(((TagDescr) o).m_TagName);
	}
}