/**
 * $Id: Utils.java,v 1.2 2009/11/25 21:08:34 misha Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.xnd.rules.dicom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.VR;
import org.dcm4che2.util.TagUtils;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 * 
 */
public final class Utils
{
	private static interface Converter
	{
		// Collection<String> convert(DicomObject o, DicomElement e);
		Collection convert(DicomObject o, DicomElement e);
	}

	private static void logUndefinedConversion(final DicomElement e)
	{
		LoggerFactory.getLogger(Utils.class).warn(
				"no conversion defined for " + TagUtils.toString(e.tag())
						+ " (VR = " + e.vr() + ")");
	}

	// private final static Map<VR,Converter> converters = new
	// HashMap<VR,Converter>();
	private final static Map converters = new HashMap();
	static
	{
		converters.put(VR.SQ, new Converter()
		{
			// public Collection<String> convert(final DicomObject o, final
			// DicomElement e) {
			@Override
			public Collection convert(final DicomObject o, final DicomElement e)
			{
				logUndefinedConversion(e);
				// return new ArrayList<String>();
				return new ArrayList();
			}
		});

		converters.put(VR.UN, new Converter()
		{
			// public Collection<String> convert(final DicomObject o, final
			// DicomElement e) {
			@Override
			public Collection convert(final DicomObject o, final DicomElement e)
			{
				// final Collection<String> r = new ArrayList<String>(1);
				final Collection r = new ArrayList(1);
				r.add(e.getString(o.getSpecificCharacterSet(), false));
				return r;
			}
		});

		converters.put(VR.AT, new Converter()
		{
			// public Collection<String> convert(final DicomObject o, final
			// DicomElement e) {
			@Override
			public Collection convert(final DicomObject o, final DicomElement e)
			{
				final int[] vals = e.getInts(false);
				// final Collection<String> r = new
				// ArrayList<String>(vals.length);
				final Collection r = new ArrayList(vals.length);
				for (int i = 0; i < vals.length; i++)
				{
					r.add(String.valueOf(vals[i]));
				}
				return r;
			}
		});

		converters.put(VR.OB, new Converter()
		{
			// public Collection<String> convert(final DicomObject o, final
			// DicomElement e) {
			@Override
			public Collection convert(final DicomObject o, final DicomElement e)
			{
				// final Collection<String> r = new ArrayList<String>(1);
				final Collection r = new ArrayList(1);
				r.add(VR.OB.toString(e.getBytes(), o.bigEndian(), o
						.getSpecificCharacterSet()));
				return r;
			}
		});
	}

	/**
	 * Returns as Strings all values defined for the given attribute in the
	 * given DICOM object, or an empty Collection if the attribute is missing or
	 * if no conversion to Strings is defined.
	 * 
	 * @param o
	 *            DicomObject to be examined
	 * @param tag
	 *            Attribute identifier
	 * @return Collection of String values
	 */
	// final static Collection<String> getValues(final DicomObject o, final int
	// tag) {
	public final static Collection getValues(final DicomObject o, final int tag)
	{
		final DicomElement de = o.get(tag);
		if (null == de)
		{
			// return new ArrayList<String>(0);
			return new ArrayList(0);
		}
		final VR vr = de.vr();
		if (converters.containsKey(vr))
		{
			return ((Converter) converters.get(vr)).convert(o, de);
		} else
		{
			// all other data types can be treated as simple strings, maybe with
			// multiple values separated by backslashes. Join these.
			try
			{
				return Arrays.asList(de.getStrings(o.getSpecificCharacterSet(),
						false));
			} catch (UnsupportedOperationException e)
			{
				logUndefinedConversion(de);
				// return new ArrayList<String>(0);
				return new ArrayList(0);
			}
		}
	}
}
