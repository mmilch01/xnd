/**
 * $Id: FormattedTagExtractor.java,v 1.2 2009/11/25 21:08:33 misha Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.xnd.rules.dicom;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.dcm4che2.data.DicomObject;

/**
 * Generates a formatted output using the first value for each given attribute.
 * 
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public class FormattedTagExtractor implements TagExtractor
{
	private final String name, format;
	private final int[] attrs;
	private final int maxAttr;

	public FormattedTagExtractor(final String name, final String format,
			final int[] attrs)
	{
		this.name = name;
		this.format = format;
		if (attrs.length <= 0)
			throw new IllegalArgumentException(
					"FormattedTagExtractor must use at least one DICOM attribute");
		this.attrs = new int[attrs.length];
		System.arraycopy(attrs, 0, this.attrs, 0, attrs.length);

		int max = attrs[0];
		for (int i = 1; i < attrs.length; i++)
		{
			if (attrs[i] > max)
				max = attrs[i];
		}
		maxAttr = max;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nrg.xnat.importer.dicom.TagExtractor#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nrg.xnat.importer.dicom.TagExtractor#getMaxAttr()
	 */
	@Override
	public int getMaxAttr()
	{
		return maxAttr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.importer.dicom.TagExtractor#getValues(org.dcm4che2.data.
	 * DicomObject)
	 * 
	 * First pass: just return formatted string from first value for each
	 * attribute
	 */
	@Override
	public Collection getValues(final DicomObject o)
	{
		final Object[] vals = new Object[attrs.length];
		for (int i = 0; i < attrs.length; i++)
		{
			final Iterator vi = Utils.getValues(o, attrs[i]).iterator();
			vals[i] = vi.hasNext() ? vi.next() : null;
		}

		final Collection r = new ArrayList(1);
		r.add(MessageFormat.format(format, vals));

		return r;
	}
}
