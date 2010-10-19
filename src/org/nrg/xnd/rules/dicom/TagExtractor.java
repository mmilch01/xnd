/**
 * $Id: TagExtractor.java,v 1.2 2009/11/25 21:08:34 misha Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.xnd.rules.dicom;

import java.util.Collection;

import org.dcm4che2.data.DicomObject;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 * 
 */
public interface TagExtractor
{
	/**
	 * @return The name of the tag to be extracted
	 */
	public String getName();

	/**
	 * @return The largest index of an attribute used by this extractor
	 */
	public int getMaxAttr();

	/**
	 * All values of the tag for the given DICOM object
	 * 
	 * @param o
	 *            DICOM object for which tags should be derived
	 * @return tag values for that object
	 */
	// public Collection<String> getValues(DicomObject o);
	public Collection getValues(DicomObject o);
}
