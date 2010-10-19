package org.nrg.fileserver;

import java.io.File;

/**
 * @author mmilch Collection repository manager. Implements basic operations for
 *         multiple collection management.
 * 
 */
public interface CollectionManager
{
	public void AddCollection(FileCollection fc);
	/**
	 * Create a new collection with id starting with specified prefix, and add
	 * it to this Collection Manager.
	 * 
	 * @param prefix
	 *            - collection id prefix
	 * @param bGenerateUID
	 *            - whether to generate a unique ID based on prefix, or just use
	 *            prefix as an ID.
	 * @return
	 */
	public FileCollection CreateCollection(String id, boolean bGenerateUID);
	public boolean Contains(String collection_id);
	/**
	 * Given a specific local file, identify whether its relative URI is
	 * contained within any of managed collections. Implementations should be
	 * fairly efficient.
	 * 
	 * @param f
	 *            Local file to search collections for.
	 * @return corresponding collection if match is found.
	 */
	public FileCollection FindCollection(File f);
	/**
	 * Retrieve file collection by its ID.
	 * 
	 * @param collection_id
	 * @return
	 */
	public FileCollection GetCollection(String collection_id);
	public void Refresh();
	/**
	 * Remove collection from the collection manager.
	 * 
	 * @param collection_id
	 *            id of collection to be removed.
	 */
	public void RemoveCollection(String collection_id);
}
