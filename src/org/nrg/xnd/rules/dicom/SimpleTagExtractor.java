/**
 * $Id: SimpleTagExtractor.java,v 1.2 2009/11/25 21:08:33 misha Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.xnd.rules.dicom;

import java.util.Collection;
import java.util.LinkedList;

import org.dcm4che2.data.DicomObject;

/**
 * Generates String representations of all values of a single attribute.
 * 
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public final class SimpleTagExtractor implements TagExtractor
{
	private final String name;
	private final int attr;

	public SimpleTagExtractor(final String name, final int attr)
	{
		this.name = name;
		this.attr = attr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nrg.xnat.importer.dicom.TagExtractor#getName()
	 */
	public String getName()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nrg.xnat.importer.dicom.TagExtractor#getMaxAttr()
	 */
	public int getMaxAttr()
	{
		return attr;
	}
	public String GetFirstValue(final DicomObject o)
	{
		LinkedList l = new LinkedList(getValues(o));
		if (l.size() < 1)
			return null;
		return (String) l.get(0);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.importer.dicom.TagExtractor#getValues(org.dcm4che2.data.
	 * DicomObject)
	 */
	// public Collection<String> getValues(final DicomObject o) {
	public Collection getValues(final DicomObject o)
	{
		return Utils.getValues(o, attr);
	}
}
