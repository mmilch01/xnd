package org.nrg.xnd.tools;

import java.io.FileWriter;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nrg.fileserver.ItemRecord;
import org.nrg.xdat.webservices.StoreXMLWS;

public class StoreXMLManager
{
	private String m_usr;
	private String m_pass;
	private String m_host;
	private String m_xml;

	public StoreXMLManager(String host, String usr, String pass, String xml)
	{
		m_usr = usr;
		m_pass = pass;
		m_host = host;
		m_xml = xml;
	}
	public boolean StoreSubject(ItemRecord ir)
	{
		Element root = StoreXARManager.AddDefaultNamespaces(DocumentHelper
				.createElement("xnat:Subject"));
		String label, project;
		project = ir.getTagValue("Project");
		label = ir.getTagValue("Subject");
		if (project == null || label == null)
			return false;
		root.addAttribute("ID", project + "_" + label);
		root.addAttribute("project", project);
		root.addAttribute("label", label);
		Document d = DocumentHelper.createDocument(root);
		OutputFormat format = OutputFormat.createPrettyPrint();
		StringWriter sw = new StringWriter();
		XMLWriter xw = new XMLWriter(sw, format);
		try
		{
			XMLWriter testw = new XMLWriter(new FileWriter(
					"D:/test/upd_subj.xml"), format);
			testw.write(d);
			testw.close();

			xw.write(d);
			xw.close();
		} catch (Exception e)
		{
			return false;
		}
		String contents = sw.toString();

		String[] args = {"-u", m_usr, "-p", m_pass, "-host", m_host,
				"-location", contents, "allowDataDeletion", "false"};
		StoreXMLWS storeXML = new StoreXMLWS();
		return storeXML.perform(args);
	}
}