package org.nrg.fileserver;

import java.util.Collection;

/**
 * FileCollection is an interface abstracting the representation of an arbitrary
 * collection of relative resource URI's. It defines a unique ID and resource
 * collection access methods.
 * 
 * @author mmilch
 * 
 */
public interface FileCollection
{
	/**
	 * Return a unique ID for this collection. The ID is used for
	 * referencing/identifying this collection from within the global
	 * application scope.
	 * 
	 * @return ID string
	 */
	public String GetID();
	/**
	 * Save/restore to/from persistent representation. Current existing
	 * subclasses use collection ID for uniquely identifying storage location.
	 * Thus, after the collection ID is initialized, it can be populated at any
	 * moment by calling Serialize. Call Serialize(false) when collection
	 * content has been changed, to synchronize with persistent representation.
	 * 
	 * @param is_loading
	 *            true: load from persistent storage; false: save to persistent
	 *            storage
	 * @return id string
	 */
	public boolean Serialize(boolean is_loading);
	/**
	 * Answers whether a particular resource URI is contained in the collection.
	 * 
	 * @param rel_path
	 *            resourse path in question
	 * @return whether resource is found in this collection.
	 */
	public boolean ContainsFile(String rel_path);
	/**
	 * @return all contained resource URI's in string collection.
	 */
	public Collection<String> GetAllFiles();
	/**
	 * Add resource to collection
	 * 
	 * @param rel_path
	 */
	public void AddFile(String rel_path);
	/**
	 * Delete this collection representation (but not actual resources) from
	 * persistent storage.
	 */
	public void Delete();
}
