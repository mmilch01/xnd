package org.nrg.fileserver;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * @author mmilch
 * 
 */
public abstract class RepositoryManager
{
	public boolean SessionInit(Vector init_params)
	{
		return true;
	}
	public boolean SessionEnd()
	{
		return true;
	}
	public abstract boolean ItemRemove(ItemRecord template);

	/**
	 * @return associated collection manager, if any.
	 */
	public CollectionManager getCM()
	{
		return null;
	}
	public void AttachCollection(ItemRecord ir, String col_id)
	{
		DBTagAttach(ir, new ItemTag("Collection_ID", col_id));
	}

	/**
	 * @param template
	 *            Search template.
	 * @param maxrecords
	 *            Maximum records to return.
	 * @param bRetrieveMetadata
	 *            whether all associated tags need to be returned as well
	 *            (should be false where possible, for performance reasons)
	 * @return array of ItemRecord matching search template.
	 */
	public abstract ItemRecord[] DBItemFind(ItemRecord template,
			int maxrecords, boolean bRetrieveMetadata);
	/**
	 * @param item
	 *            ItemRecord to be added
	 * @param bUpdateAllowed
	 *            if item exists, can it be updated
	 * @return true if item was added; or, if update was allowed and matching
	 *         item was found, item was updated. Otherwise, false.
	 */
	public boolean ItemAdd(ItemRecord item, boolean bUpdateAllowed)
	{
		ItemRecord[] items = DBItemFind(item, 1, false); // ??
		if (items.length < 1)
			return DBItemAdd(item);
		else if (items.length == 1) // item already exists, can we update it?
		{
			if (!bUpdateAllowed)
				return true;
			return DBItemUpdate(items[0], item);
		} else
			// item definition is ambiguous.
			return false;
	}
	// internal item manipulation

	/**
	 * Update item with tags of template item.
	 * 
	 * @param item
	 * @param template
	 * @return
	 */
	public boolean DBItemUpdate(ItemRecord item, ItemRecord template)
	{
		ItemTag[] tags = template.getAllTags();
		for (int i = 0; i < tags.length; i++)
		{
			DBTagAttach(item, tags[i]);
		}
		return true;
	}
	/**
	 * A primitive DB addition function, not to called by the user directly.
	 * 
	 * @param item
	 * @return
	 */
	public abstract boolean DBItemAdd(ItemRecord item);

	// public tag manipulation
	/**
	 * @param name
	 *            Unique tag name
	 * @return Array of matching tag names; empty array if no matches.
	 */

	public abstract String[] DBTagFind(String name);

	/**
	 * Remove tag from DB. All items containing this tag should be updated.
	 * 
	 * @param name
	 *            Tag unique name.
	 * @return true if success; false if label was not found.
	 */

	public abstract boolean DBTagDelete(String name);

	/**
	 * Matches (wildcarded) strings s1 and s2.
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public boolean MatchString(String s1, String s2)
	{
		if (s1.startsWith("*") || s2.startsWith("*"))
			return true;
		if ((s1.length() < 1 && s2.length() > 0)
				|| (s1.length() > 0 && s2.length() < 1))
			return false;
		return (s1.compareTo(s2) == 0);
	}
	/**
	 * Match (wildcarded) template with a given item record.
	 * 
	 * @param ir
	 * @param template
	 * @return
	 */
	public boolean DBMatch(ItemRecord ir, ItemRecord template,
			boolean bMatchTags)
	{
		if (!MatchString(ir.getAbsolutePath(), template.getAbsolutePath()))
			return false;
		if (!bMatchTags)
			return true;
		ItemTag[] tags = template.getAllTags();
		ItemTag it, it1;
		for (int i = 0; i < tags.length; i++)
		{
			it = tags[i];
			if ((it1 = ir.getTag(it.GetName())) == null)
				return false;
			if (!MatchString(it.GetFirstValue(), it1.GetFirstValue()))
				return false;
		}
		return true;
	}
	/**
	 * Create and add a new tag to DB
	 * 
	 * @param name
	 *            Tag name.
	 * @return true if successful, false in case of DB error.
	 */
	public boolean TagAdd(String name)
	{
		if (DBTagFind(name).length > 0)
			return false;
		return DBTagAdd(name);
	}
	// internal tag manipulation
	/**
	 * Add tag definition to repository.
	 * 
	 * @param name
	 *            Tag name to be added to DB.
	 * @return
	 */
	public abstract boolean DBTagAdd(String name);
	/**
	 * Attach tag to an item and save it in DB. If tag.IsMultiValue()==true, and
	 * tag exists: add tag value (should be unique) to values list; otherwise,
	 * if tag.IsMultiValue()==false, replace previous value.
	 * 
	 * @param Item
	 *            to search for.
	 * @param Tag
	 *            to attach.
	 * @return
	 */
	public abstract boolean DBTagAttach(ItemRecord item, ItemTag tag);
	/**
	 * Remove tag from specified item.
	 * 
	 * @param item
	 * @param tag
	 * @return
	 */
	public abstract boolean DBTagDetach(ItemRecord item, ItemTag tag);

	/**
	 * Find a tag with name parentTagName which is parent to a given tag
	 * 
	 * @param tag
	 * @param parentTagName
	 * @return
	 */
	public ItemTag DBFindParentTag(ItemTag tag, String parentTagName)
	{
		ItemRecord template = new ItemRecord("*", "*");
		template.tagSet(tag);
		template.tagSet(new ItemTag(parentTagName, "*"));
		ItemRecord[] found = DBItemFind(template, 1, true);
		if (found.length < 1)
			return null;
		return found[0].getTag(parentTagName);
	}

	/**
	 * Extended item find
	 * 
	 * @param rel_path
	 *            item relative path
	 * @param tagsMatching
	 *            both name and value for tags in this collection should match
	 * @param tagsDefined
	 *            tags from this collection should be defined.
	 * @param tagsUndefined
	 *            tags from this collection should be undefined.
	 * @return
	 */
	public Collection<ItemRecord> DBItemFindEx(String rel_path,
			TagSet tagsMatching, Collection<String> tagsDefined,
			Collection<String> tagsUndefined)
	{
		ItemRecord template = new ItemRecord("*", rel_path);
		template.tagsSet(tagsMatching);
		LinkedList<ItemRecord> llir = new LinkedList<ItemRecord>();
		// find all matching records
		ItemRecord[] irs = DBItemFind(template, -1, true);
		// filter out records that don't match.
		boolean bMatch = true;
		for (ItemRecord ir : irs)
		{
			for (String s : tagsDefined)
			{
				if (ir.getTag(s) == null)
				{
					bMatch = false;
					break;
				}
			}
			if (!bMatch)
				continue;
			for (String s : tagsUndefined)
			{
				if (ir.getTag(s) != null)
				{
					bMatch = false;
					break;
				}
			}
			if (bMatch)
				llir.add(ir);
			bMatch = true;
		}
		return llir;
	}

	/**
	 * Infer tag values from a given set of defined tags. Implementations are
	 * recommended to override this method for efficiency reasons.
	 * 
	 * @param path
	 *            Contains defined tag hierarchy: Tag_1:
	 *            (Name_1=Value_1),...,Tag_N:(Name_N=Value_N)
	 * @param query_tags
	 *            Tags to be populated with values: qTag_1:
	 *            (qName_1=?),...,(qName_M=?)
	 * @return set values in query_tags (multiple values for each qTag_i,
	 *         i=1,..,M are allowed), using the algorithm: A. find all
	 *         ItemRecords (ir_1,...ir_K) such that value(Tag_i(ir_j))=value_i,
	 *         i=1,..,N, j=1,..,K, denoting the set of these ItemRecords for S.
	 *         B. for each k, k=1,..,M, find all different values of qTag_k for
	 *         records from S, and store them in values of qTag_k.
	 */
	public TreeMap<ItemTag, TagMap> DBTagValues(final Context path,
			final TagMap query_tags)
	{
		ItemRecord template = new ItemRecord("*", "*");
		TreeMap<ItemTag, TagMap> aTags = new TreeMap<ItemTag, TagMap>();
		if (path.size() > 0)
		{
			template.tagsSet(path);
			ItemRecord[] context_recs = DBItemFind(template, -1, true);
			// String val;
			// TagMap llit;
			// ItemRecord qir;
			// String qtname;
			// TreeMap<String,ItemRecord> tmsir;
			boolean bNew = false;
			for (ItemRecord ir : context_recs)
			{
				for (ItemTag it : query_tags)
				{
					ItemTag rTag;
					if ((rTag = ir.getTag(it.GetName())) != null)
					{
						TagMap mp = aTags.get(rTag);
						if (mp == null)
						{
							mp = new TagMap();
							mp.add(rTag);
							bNew = true;
						}
						mp.mergeTags(ir.getTagCollection());
						if (bNew)
						{
							aTags.put(rTag, mp);
							bNew = false;
						}
					}
				}
			}
		} else
		// optimized processing in case when context is empty.
		{
			boolean bNew = false;
			for (ItemTag it : query_tags)
			{
				// template.TagSet(new ItemTag(it.GetName(),"*"));
				template.tagSet(it);
				ItemRecord[] recs = DBItemFind(template, -1, true);
				ItemTag rTag;
				for (ItemRecord ir : recs)
				{
					rTag = ir.getTag(it.GetName());
					if (rTag == null)
						continue;
					TagMap mp = aTags.get(rTag);
					if (mp == null)
					{
						mp = new TagMap();
						mp.add(rTag);
						bNew = true;
					}
					mp.mergeTags(ir.getTagCollection());
					if (bNew)
					{
						aTags.put(rTag, mp);
						bNew = false;
					}
				}
				template.removeAllTags();
			}
		}
		/*
		 * for(ItemTag qr:query_tags) { template.TagSet(new
		 * ItemTag(qr.GetName(),"*")); ItemRecord[]
		 * r=DBItemFind(template,-1,true); template.TagRemove(qr.GetName());
		 * qr.ClearAllValues(); qtname=qr.GetName(); tmsir=new
		 * TreeMap<String,ItemRecord>(); //determine valid values for this tag.
		 * for(ItemRecord ir:r) { if(null!=(val=ir.GetTagValue(qtname))) {
		 * if(!tmsir.containsKey(val)) { qir=new ItemRecord(null,null);
		 * qir.TagsSet(ir.GetAllTags()); tmsir.put(val,qir); } else {
		 * qir=tmsir.get(val); qir.TagsMerge(ir.GetTagCollection()); }
		 * qr.AddValue(val); } } //and populate extra tags for(String
		 * s:tmsir.keySet()) { llit=new TagMap(); llit.add(new
		 * ItemTag(qr.GetName(),s));
		 * llit.addAll(tmsir.get(s).GetTagCollection()); aTags.merge(llit); } }
		 */
		return aTags;
	}

	/**
	 * 
	 * Find all values for tag childTagName for items with tags specified in
	 * template
	 * 
	 * @param tag
	 * @param childTagName
	 * @return tag values of found items with tag name=childTagName
	 */
	public String[] DBFindMatchingTagValues(ItemRecord template,
			String childTagName)
	{
		template.tagSet(new ItemTag(childTagName, "*"));
		ItemRecord[] found = DBItemFind(template, -1, true);
		if (found.length < 1)
			return new String[0];
		TreeSet<String> ts = new TreeSet<String>();
		ItemTag it;
		for (ItemRecord ir : found)
		{
			if ((it = ir.getTag(childTagName)) != null)
				;
			ts.add(it.GetFirstValue());
		}
		return ts.toArray(new String[0]);
	}
	/**
	 * Attach specified tag to items matching specified item template.
	 * 
	 * @param item
	 * @param tag
	 * @return
	 */
	public boolean TagAttach(ItemRecord item, ItemTag tag)
	{
		ItemRecord[] items = DBItemFind(item, -1, true);
		String[] dbtag = DBTagFind(tag.GetName());
		if (dbtag.length != 1)
			return false;

		if (items.length < 1)
			return false;
		boolean res = true;
		for (int i = 0; i < items.length; i++)
		{
			res |= DBTagAttach(items[i], new ItemTag(dbtag[0], tag
					.GetFirstValue()));
		}
		return res;
	}
}