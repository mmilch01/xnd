package org.nrg.xnd.ontology;

import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.utils.Utils;

/**
 * Associates mapping between a tuple <ontology tag+discriminating tag> <-> XNAT tag. 
 * 
 * @author mmilch
 * 
 */
public final class XnatTypeManager
{
	private static TreeMap<String,property_group> m_property_groups=new TreeMap<String,property_group>();
	
	public XnatTypeManager(){};
	public static String GetDefaultLocation()
	{
		return new File(Utils.GetPluginPath()
				+ "/xml_resources/xnat_complex_types.xml").getAbsolutePath();		
	}
	public static void LoadFromXML(File f) throws DocumentException
	{
		Document d=new SAXReader().read(f);
		Element el;
		for (Iterator<Element> it = d.getRootElement().elementIterator(); it
		.hasNext();)
		{
			el = it.next();
			if (el.getName().compareTo("property_group") == 0)
			{
				m_property_groups.put(el.attributeValue("key"), new property_group(el));
			}
		}
	}	
	public static String GetCompexTypeRestClauses(String ontology_key, ItemRecord it)
	{
		//ItemRecord is a set of all tags.
		//ontology_key is a dividing tag name (modality)
		//a) iterate through the all property groups
		//b) iterate through all tags for each property group and find combinations "tag+ontology_key"
		// that give xnat type in this property group.
		// 
		
		ItemTag dividingTag;
//		ItemTag[] tags=it.getAllTags();
		String pr_gr,xtype;
//		LinkedList<String> xnames=new LinkedList<String>();
		String xnames="";
		for (Iterator<String> iter=m_property_groups.keySet().iterator(); iter.hasNext();)
		{
			pr_gr=iter.next();
			if((dividingTag=it.getTag(pr_gr))==null) continue;
			
			try
			{
				m_property_groups.get(pr_gr);
				xtype=property_group.GetXnatName(dividingTag.GetFirstValue(), ontology_key);
				if (xtype!=null)
				{
					if(xnames.length()>0)
						xnames+="&"+"xtype";
					else 
						xnames=xtype;
				}
			}catch(Exception e){}			
		}
		return xnames;
	}	
	private final static class property_group
	{
		public static String m_key;
		public static TreeMap<String,TreeMap<String,String>> m_property_types=new TreeMap<String,TreeMap<String,String>>();
		public property_group(Element el)
		{
			m_key=el.attributeValue("key");
			Element property_type,rest_clause;
			TreeMap<String,String> xsimap;
			for (Iterator<Element> iter = el.elementIterator(); iter.hasNext();)
			{
				property_type=iter.next();
				xsimap=new TreeMap<String,String>();
				for (Iterator<Element> iter1 = property_type.elementIterator(); iter1.hasNext();)
				{
					rest_clause=iter1.next();
					xsimap.put(rest_clause.attributeValue("ontology_key"),rest_clause.attributeValue("text"));					
				}
				m_property_types.put(property_type.attributeValue("name"), xsimap);
			}
		}	
		public static String GetXnatName(String property_type, String ontology_key) throws NullPointerException
		{
			return m_property_types.get(property_type).get(ontology_key);
		}		
	}
}