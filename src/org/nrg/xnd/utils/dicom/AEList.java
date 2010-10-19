package org.nrg.xnd.utils.dicom;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.nrg.xnd.utils.Utils;

public class AEList
{
	private TreeMap<String, AE> m_AEs = new TreeMap<String, AE>();
	public AEList()
	{
		File aelFile = getFile();
		try
		{
			Document d = new SAXReader().read(aelFile);
			Serialize(d, true);
		} catch (Exception e)
		{
		}
		if (!m_AEs.containsKey("local"))
			m_AEs.put("local", new AE());
	}
	public Collection<String> getAENames()
	{
		LinkedList<String> names = new LinkedList<String>();
		for (AE ae : m_AEs.values())
			names.add(ae.m_name);
		return names;
	}
	public AE getAE(String name)
	{
		return m_AEs.get(name);
	}
	public void delAE(String name)
	{
		m_AEs.remove(name);
	}
	public Collection<AE> getAEs()
	{
		return m_AEs.values();
	}
	public void addAE(AE ae)
	{
		m_AEs.put(ae.m_name, ae);
	}

	private File getFile()
	{
		return new File(Utils.GetUserFolder() + "/" + "aelist.xml");
	}
	public void Save()
	{
		File aelFile = new File(Utils.GetUserFolder() + "/" + "aelist.xml");
		OutputFormat format = OutputFormat.createPrettyPrint();
		try
		{
			XMLWriter w = new XMLWriter(new FileWriter(aelFile), format);
			Document d = DocumentHelper.createDocument();
			Serialize(d, false);
			w.write(d);
			w.close();
		} catch (Exception e)
		{
		}
	}
	private void Serialize(Document d, boolean is_loading)
	{
		if (is_loading)
		{
			for (Iterator it = d.getRootElement().elementIterator(); it
					.hasNext();)
			{
				Element el = (Element) it.next();
				if (el.getName().toLowerCase().compareTo("ae") != 0)
					continue;
				AE newAE = new AE(el);
				m_AEs.put(newAE.m_name, newAE);
			}
		} else
		{
			Element root = d.addElement("ApplicationEntities");
			for (AE ae : m_AEs.values())
			{
				Element aeEl = root.addElement("AE");
				ae.Serialize(aeEl, false);
			}
		}
	}

	public class AE implements Comparable<AE>
	{
		public String m_name, m_netName, m_title;
		public int m_sendPort, m_recvPort;

		public AE()
		{
			String name = "local";
			init(name, "localhost", "LOCAL_AE", 104, 104);
		}

		public AE(String name)
		{
			init(name, "localhost", "LOCAL_AE", 104, 104);
		}

		public AE(AE from)
		{
			init(from.m_name, from.m_netName, from.m_title, from.m_sendPort,
					from.m_recvPort);
		}
		public int compareTo(AE ae)
		{
			int res = ae.m_name.compareTo(m_name);
			if (res != 0)
				return res;
			boolean bSame = (m_netName.compareTo(ae.m_netName) == 0)
					&& (m_title.compareTo(ae.m_title) == 0)
					&& (m_sendPort == ae.m_sendPort)
					&& (m_recvPort == ae.m_recvPort);
			return bSame ? 0 : -1;
		}

		public void init(String name, String netName, String title,
				int sendPort, int recvPort)
		{
			m_name = name;
			m_netName = netName;
			m_title = title;
			m_sendPort = sendPort;
			m_recvPort = recvPort;
		}
		public AE(Element el)
		{
			try
			{
				Serialize(el, true);
			} catch (Exception e)
			{
			}
		}
		public void Serialize(Element el, boolean is_loading)
		{
			if (is_loading)
			{
				m_name = el.attributeValue("name");
				m_netName = el.attributeValue("netname");
				m_title = el.attributeValue("aetitle");
				m_sendPort = Integer.parseInt(el.attributeValue("sendport"));
				m_recvPort = Integer.parseInt(el.attributeValue("recvport"));
			} else
			{
				el.addAttribute("name", m_name);
				el.addAttribute("netname", m_netName);
				el.addAttribute("aetitle", m_title);
				el.addAttribute("sendport", String.valueOf(m_sendPort));
				el.addAttribute("recvport", String.valueOf(m_recvPort));
			}
		}
	}
}