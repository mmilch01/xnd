package org.nrg.xnd.utils;

import java.util.Vector;
public class LightXML implements Cloneable
{
	Vector<String> m_Data = new Vector<String>();
	public void SetFromXML(LightXML xml)
	{
		FreeParamArray();
		int sz = xml.m_Data.size();
		if (sz < 1)
			return;
		for (int i = 0; i < sz; i++)
		{
			m_Data.addElement(xml.m_Data.elementAt(i));
		}
	}
	public LightXML()
	{
		FreeParamArray();
	}
	/*****************************************************
	 * 
	 * Set object to initial (empty) state
	 * 
	 ******************************************************/
	public void Reset()
	{
		FreeParamArray();
		System.gc();
	}
	public void FreeParamArray()
	{
		m_Data.removeAllElements();
	}
	public static String StringFromByteArray(byte[] buf)
	{
		String tmp = "", aa;
		for (int j = 0; j < buf.length; j++)
		{
			aa = Integer.toHexString(((buf[j]) + 256) % 256);
			if (aa.length() == 1)
				aa = "0" + aa;
			tmp += aa;
		}
		return tmp;
	}
	public static byte[] ByteArrayFromString(String str)
	{
		if ((str.length() % 2) != 0)
			return null;
		byte[] res = new byte[str.length() / 2];
		try
		{
			for (int i = 0; i < str.length() / 2; i++)
			{
				res[i] = (byte) (Integer.parseInt(str.substring(2 * i,
						2 * i + 2), 16));
			}
		} catch (Exception e)
		{
			return null;
		}
		return res;
	}
	public String GetStringValue(String name)
	{
		if (name.length() < 1)
			return null;
		for (int i = 0; i < m_Data.size(); i += 2)
		{
			if ((m_Data.elementAt(i)).compareTo(AdjustValue(name)) == 0)
				return (m_Data.elementAt(i + 1));
		}
		return null;
	}
	public String GetNameByIndex(int ind)
	{
		if (ind * 2 >= m_Data.size())
			return null;
		return (m_Data.elementAt(ind * 2));
	}
	public int GetIntValue(String name)
	{
		String s = GetStringValue(name);
		if (s == null)
			return 0;
		int res;
		try
		{
			res = Integer.valueOf(s).intValue();
		} catch (Exception e)
		{
			res = 0;
		}
		return res;
	}
	public double GetDoubleValue(String name)
	{
		String s = GetStringValue(name);
		if (s == null)
			return 0;
		double res;
		try
		{
			res = Double.valueOf(s).doubleValue();
		} catch (Exception e)
		{
			res = 0;
		}
		return res;
	}
	private String AdjustValue(String str)
	{
		str = str.replace('<', '[');
		return str.replace('>', ']');
	}
	public void AddValue(String name, String value)
	{
		m_Data.addElement(AdjustValue(name));
		m_Data.addElement(AdjustValue(value));
	}
	public void AddValue(String name, int value)
	{
		AddValue(name, Integer.toString(value));
	}
	public void AddValue(String name, double value)
	{
		AddValue(name, Double.toString(value));
	}
	public void AttachParamString(String str)
	{
		FreeParamArray();
		if (str.length() < 7)
			return;
		int len = str.length(), namelen, valuelen, index = 0, nValues = 0, occurence, occurence1;
		boolean bShortSyntax;
		String tmp_name, tmp_value;
		do
		{
			if ((occurence = str.indexOf("<", index)) < 0)
				break;
			index += (occurence - index) + 1;
			namelen = 0;
			occurence = str.indexOf("\">", index);
			occurence1 = str.indexOf("=\"", index);

			if (occurence < 0)
				break; // xml ended
			if (occurence1 < 0)
				bShortSyntax = false;
			else if (occurence < occurence1)
				bShortSyntax = false;
			else
				bShortSyntax = true;

			if (bShortSyntax)
				occurence = occurence1;
			namelen = occurence - index;
			index += namelen;
			if (namelen < 1)
				continue;
			tmp_name = str.substring(index - namelen, index);
			valuelen = 0;
			index++;

			if (bShortSyntax)
			{
				if ((occurence = str.indexOf("\"", index)) < 0)
					break;
				index += (occurence - index) + 1;
				if ((occurence = str.indexOf("\">", index)) < 0)
					break;
				valuelen = occurence - index;
				tmp_value = "";
				if (valuelen > 0)
					tmp_value = str.substring(index, index + valuelen);
				index += (valuelen + 1);
			} else
			{
				if ((occurence = str.indexOf('<', index)) < 0)
					break;
				valuelen = occurence - index;
				if ((occurence = str.indexOf('/', index)) < 0)
					break;
				index += valuelen;
				tmp_value = "";
				if (valuelen > 0)
					tmp_value = str.substring(index - valuelen, index);
			}
			if ((occurence = str.indexOf('>', index)) < 0)
				break;
			index += (occurence - index) + 1;
			m_Data.addElement(AdjustValue(tmp_name));
			m_Data.addElement(AdjustValue(tmp_value));
		} while (index < len);
	}
	public void AttachParamString(byte[] src)
	{
		AttachParamString(new String(src));
	}
	byte[] GetStream()
	{
		if (m_Data.size() < 2)
			return null;
		String buf = "";
		for (int i = 0; i < m_Data.size(); i += 2)
			buf += "<" + (m_Data.elementAt(i)) + "=\""
					+ (m_Data.elementAt(i + 1)) + "\">";
		return buf.getBytes();
	}
}// end of LightXML class.
