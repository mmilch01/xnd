package org.nrg.xnd.model;

import java.util.Collection;

/**
 * @author mmilch Prototype class for all validators of tagged files.
 */
public abstract class Validator
{
	protected Collection<CElement> m_elements;
	public Validator(Collection<CElement> els)
	{
		m_elements = els;
	}
	/**
	 * validate the tags in container collection given at the time of
	 * construction.
	 * 
	 * @return problem list if validation failed; null otherwise.
	 */
	public abstract String validate(boolean bAutoFix);
	/**
	 * fix found problems.
	 * 
	 * @return false if problems still exist.
	 */
	public abstract boolean fix();
}