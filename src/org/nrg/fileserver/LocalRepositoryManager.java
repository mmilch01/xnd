/**
 * Copyright (c) 2008 Washington University
 */
package org.nrg.fileserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.nrg.xnat.filestore.FileStore;
import org.nrg.xnat.filestore.ForbiddenException;
import org.nrg.xnat.filestore.MetadataAssignOp;
import org.nrg.xnat.filestore.MetadataDeleteOp;
import org.nrg.xnat.filestore.MetadataOp;
import org.nrg.xnat.search.Search;
import org.nrg.xnat.tags.InvalidTagLabelException;
import org.nrg.xnat.tags.TagLabel;
import org.nrg.xnat.tags.TagLabelException;
import org.nrg.xnat.tags.TagOps;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 * 
 */
public class LocalRepositoryManager extends RepositoryManager
{
	private static final String JNDI_JDBC_DATASOURCE = "java:comp/env/jdbc/datasource";
	private static final String JNDI_XNAT_STORE = "java:comp/env/xnat/store";

	private final static ItemTag[] EMPTY_TAG_ARRAY = new ItemTag[0];
	private final static Collection<TagLabel> EMPTY_TAGLABEL_COLLECTION = new ArrayList<TagLabel>(
			0);

	private final Logger log = Logger.getLogger(LocalRepositoryManager.class);

	private final Connection c;
	private final FileStore store;

	public LocalRepositoryManager() throws NamingException, SQLException
	{
		final InitialContext ic = new InitialContext();
		final DataSource ds = (DataSource) ic.lookup(JNDI_JDBC_DATASOURCE);
		c = ds.getConnection();
		store = (FileStore) ic.lookup(JNDI_XNAT_STORE);
	}

	private static URI getURI(final ItemRecord item) throws URISyntaxException
	{
		return new URI(item.getRelativePath().replace(File.separatorChar, '/'));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#DBItemAdd(org.nrg.xnat.repository
	 * .ItemRecord)
	 */
	@Override
	public boolean DBItemAdd(final ItemRecord item)
	{
		try
		{
			// Register the resource
			final URI uri;
			try
			{
				uri = getURI(item);
			} catch (URISyntaxException e)
			{
				log.error("relative path " + item.getRelativePath()
						+ " is not a valid URI", e);
				return false;
			}
			final File file = item.getFile();
			store.create(c, uri, file); // use default media type

			// Translate and store the metadata
			final Collection<MetadataOp> ops = new ArrayList<MetadataOp>();
			for (final ItemTag tag : item.getAllTags())
			{
				ops.add(new MetadataAssignOp(c, tag.GetName(), tag
						.GetAllValues()));
			}
			store.updateMetadata(c, uri, ops.toArray(new MetadataOp[0]));
			return true;
		} catch (SQLException e)
		{
			log.error(e);
			return false;
		} catch (ForbiddenException e)
		{
			log.info(e);
			return false;
		} catch (TagLabelException e)
		{
			log.info(e);
			return false;
		} catch (FileNotFoundException e)
		{
			log.info(e);
			return false;
		}
	}

	private final Collection<ItemRecord> doTagsSearch(final ItemTag[] tags,
			final int maxrecords, final boolean getMetadata)
	{
		// Extract metadata
		final Map<TagLabel, Collection<String>> values = new LinkedHashMap<TagLabel, Collection<String>>();
		final Collection<TagLabel> set = new ArrayList<TagLabel>(), unset = new ArrayList<TagLabel>();
		for (final ItemTag tag : tags)
		{
			final TagLabel label;
			try
			{
				label = new TagLabel(tag.GetName());
			} catch (InvalidTagLabelException e)
			{
				log.error(e);
				return null;
			}

			final Collection<String> v = new LinkedHashSet<String>(Arrays
					.asList(tag.GetAllValues()));
			if (v.contains("*"))
			{
				set.add(label); // any value will do, so we don't need to
				// enumerate them in the search
			} else
			{
				if (values.containsKey(label))
				{
					log.error("ItemRecord contains duplicate label " + label);
					values.get(tag).addAll(v);
				} else
				{
					values.put(label, v);
				}
			}
		}

		// Search for matches
		final Search results;
		try
		{
			synchronized (c)
			{
				results = Search.get(c, values, set, unset, (maxrecords < 0)
						? null
						: maxrecords);
			}
		} catch (TagLabelException e)
		{
			log.error(e);
			return null;
		} catch (SQLException e)
		{
			log.error(e);
			return null;
		}

		// Construct ItemRecords for matches (also testing URIs)
		final Collection<ItemRecord> matches = new ArrayList<ItemRecord>();
		for (final Iterator<Map.Entry<URI, File>> mei = results
				.resourceEntryIterator(); mei.hasNext();)
		{
			try
			{
				matches.add(makeItemRecord(mei.next(), getMetadata));
			} catch (FileNotFoundException e)
			{
				log.error("database inconsistency", e);
				// continue with next match
			} catch (SQLException e)
			{
				log.error(e);
				return null;
			}
		}

		return matches;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#DBItemFind(org.nrg.xnat.repository
	 * .ItemRecord, int)
	 */
	@Override
	public ItemRecord[] DBItemFind(final ItemRecord template,
			final int maxrecords, final boolean getMetadata)
	{
		// This is a reimplementation of DBMatch/MatchString for use on the
		// underlying database
		// First check the template: only do a search if the resource is
		// wildcarded.
		final ItemRecord[] empty = new ItemRecord[0];
		if ("*".equals(template.getAbsolutePath()))
		{
			final Collection<ItemRecord> r = doTagsSearch(
					template.getAllTags(), maxrecords, getMetadata);
			return null == r ? empty : r.toArray(empty);
		} else
		{
			final URI uri;
			try
			{
				uri = getURI(template);
			} catch (URISyntaxException e)
			{
				log.error(e);
				return empty;
			}
			final ItemRecord ir;
			try
			{
				ir = makeItemRecord(uri, store.getFile(c, uri), getMetadata);
			} catch (FileNotFoundException e)
			{
				log.error(e);
				return empty;
			} catch (SQLException e)
			{
				log.error(e);
				return empty;
			}

			if (DBMatch(ir, template, getMetadata) && 0 != maxrecords)
			{
				final ItemRecord[] r = new ItemRecord[1];
				r[0] = ir;
				return r;
			} else
			{
				return empty;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nrg.xnat.repository.RepositoryManager#DBTagAdd(java.lang.String)
	 */
	@Override
	public boolean DBTagAdd(final String name)
	{
		try
		{
			TagOps.add(c, new TagLabel(name));
			return true;
		} catch (InvalidTagLabelException e)
		{
			log.info(e);
			return false;
		} catch (SQLException e)
		{
			log.error(e);
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#DBTagAttach(org.nrg.xnat.repository
	 * .ItemRecord, org.nrg.xnat.repository.ItemTag)
	 */
	@Override
	public boolean DBTagAttach(final ItemRecord item, final ItemTag tag)
	{
		final URI uri;
		try
		{
			uri = getURI(item);
		} catch (URISyntaxException e)
		{
			log.error(e);
			return false;
		}
		final String name = tag.GetName();
		try
		{
			if (tag.IsMultiValue())
			{
				store.updateMetadata(c, uri, new MetadataAssignOp(c, name, tag
						.GetAllValues()));
			} else
			{
				store.updateMetadata(c, uri, new MetadataDeleteOp(c, name));
				store.updateMetadata(c, uri, new MetadataAssignOp(c, name, tag
						.GetFirstValue()));
			}
			return true;
		} catch (SQLException e)
		{
			log.error(e);
			return false;
		} catch (TagLabelException e)
		{
			log.info(e);
			return false;
		} catch (FileNotFoundException e)
		{
			log.info(e);
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#DBTagDelete(java.lang.String)
	 */
	@Override
	public boolean DBTagDelete(final String name)
	{
		try
		{
			TagOps.delete(c, new TagLabel(name));
			return true;
		} catch (InvalidTagLabelException e)
		{
			log.info(e);
			return false;
		} catch (SQLException e)
		{
			log.error(e);
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#DBTagDetach(org.nrg.xnat.repository
	 * .ItemRecord, org.nrg.xnat.repository.ItemTag)
	 */
	@Override
	public boolean DBTagDetach(final ItemRecord item, final ItemTag tag)
	{
		try
		{
			store.updateMetadata(c, getURI(item), new MetadataDeleteOp(c, tag
					.GetName()));
			return true;
		} catch (SQLException e)
		{
			log.error(e);
			return false;
		} catch (URISyntaxException e)
		{
			log.error(e);
			return false;
		} catch (TagLabelException e)
		{
			log.error(e);
			return false;
		} catch (FileNotFoundException e)
		{
			log.info(e);
			return false;
		}
	}

	private final static String[] EMPTY_STRING_ARRAY = {};

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#DBTagFind(java.lang.String)
	 */
	@Override
	public String[] DBTagFind(final String name)
	{
		final Collection<TagLabel> found;
		try
		{
			found = TagOps.getMatching(c, new TagLabel(name));
		} catch (InvalidTagLabelException e)
		{
			log.info(e);
			return EMPTY_STRING_ARRAY;
		} catch (SQLException e)
		{
			log.error(e);
			return EMPTY_STRING_ARRAY;
		}
		return toStringArray(found);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#ItemRemove(org.nrg.xnat.repository
	 * .ItemRecord)
	 */
	@Override
	public boolean ItemRemove(final ItemRecord template)
	{
		try
		{
			store.delete(c, false, getURI(template));
			return true;
		} catch (FileNotFoundException e)
		{
			log.debug(e);
			return false;
		} catch (SQLException e)
		{
			log.error(e);
			return false;
		} catch (URISyntaxException e)
		{
			log.error(e);
			return false;
		}
	}

	/**
	 * Find a tag with name parentTagName which is parent to a given tag
	 * 
	 * @param tag
	 * @param parentTagName
	 * @return
	 */
	@Override
	public ItemTag DBFindParentTag(final ItemTag tag, final String parentTagName)
	{
		try
		{
			final Map<TagLabel, Collection<String>> constraints = new HashMap<TagLabel, Collection<String>>();
			constraints.put(new TagLabel(tag.GetName()), Arrays.asList(tag
					.GetAllValues()));

			final Collection<TagLabel> requested = new ArrayList<TagLabel>(1);
			final TagLabel parent = new TagLabel(parentTagName);
			requested.add(parent);
			return makeItemTag(parent, Search
					.getTags(c, constraints, requested).get(parent));
		} catch (SQLException e)
		{
			log.error(e);
			return new ItemTag(null);
		} catch (TagLabelException e)
		{
			log.error(e);
			return new ItemTag(null);
		}
	}

	/**
	 * 
	 * Find values for tag childTagName, given the tag values in template
	 * 
	 * @param tag
	 * @param childTagName
	 * @return tag values of found items with tag name=childTagName
	 */
	public ItemTag[] DBFindMatchingTags(final ItemRecord template,
			final String childTagName)
	{
		try
		{
			final Map<TagLabel, Collection<String>> constraints = new HashMap<TagLabel, Collection<String>>();
			for (final ItemTag tag : template.getAllTags())
			{
				constraints.put(new TagLabel(tag.GetName()), Arrays.asList(tag
						.GetAllValues()));
			}

			final Collection<TagLabel> requested = new ArrayList<TagLabel>(1);
			final TagLabel child = new TagLabel(childTagName);
			requested.add(child);

			final Collection<String> values = Search.getTags(c, constraints,
					requested).get(child);

			final ItemTag[] itemTags = new ItemTag[values.size()];
			int i = 0;
			for (final String value : values)
			{
				itemTags[i++] = new ItemTag(childTagName, value);
			}
			return itemTags;
		} catch (SQLException e)
		{
			log.error(e);
			return EMPTY_TAG_ARRAY;
		} catch (TagLabelException e)
		{
			log.error(e);
			return EMPTY_TAG_ARRAY;
		}
	}

	/*
	 * public void DBTagValues (final Collection<ItemTag> path, final
	 * Collection<ItemTag> query_tags) { final Map<TagLabel,Collection<String>>
	 * values = new HashMap<TagLabel,Collection<String>>(); final
	 * Map<ItemTag,TagLabel> queryLabels = new
	 * LinkedHashMap<ItemTag,TagLabel>();
	 * 
	 * final Search search; try { for (final ItemTag it : path) { values.put(new
	 * TagLabel(it.GetName()), Arrays.asList(it.GetAllValues())); } for (final
	 * ItemTag it : query_tags) { queryLabels.put(it, new
	 * TagLabel(it.GetName())); } synchronized (c) { search = Search.get(c,
	 * values, queryLabels.values(), EMPTY_TAGLABEL_COLLECTION, 0); } } catch
	 * (Exception e) { log.error(e); return; }
	 * 
	 * for (final Map.Entry<ItemTag,TagLabel> me : queryLabels.entrySet()) {
	 * final ItemTag tag = me.getKey(); for (final String value :
	 * search.getTagValues(me.getValue())) { tag.AddValue(value); } } }
	 */

	private final ItemTag makeItemTag(final TagLabel label,
			final Iterable<String> values)
	{
		final ItemTag tag = new ItemTag(label.toString());
		for (final String value : values)
		{
			tag.AddValue(value);
		}
		return tag;
	}

	private final ItemRecord makeItemRecord(final URI uri, final File f,
			final boolean getMetadata) throws FileNotFoundException,
			SQLException
	{
		final ItemRecord item = new ItemRecord(f.getAbsolutePath(), uri
				.toString());

		if (getMetadata)
		{
			for (final Map.Entry<TagLabel, Collection<String>> me : store
					.getMetadata(c, uri).entrySet())
			{
				final ItemTag tag = makeItemTag(me.getKey(), me.getValue());
				item.tagSet(tag);
			}
		}

		return item;
	}

	private final ItemRecord makeItemRecord(final Map.Entry<URI, File> entry,
			final boolean getMetadata) throws FileNotFoundException,
			SQLException
	{
		return makeItemRecord(entry.getKey(), entry.getValue(), getMetadata);
	}

	private final static <T> String[] toStringArray(
			final Collection<? extends T> os)
	{
		final String[] v = new String[os.size()];
		int i = 0;
		for (final T o : os)
		{
			v[i++] = o.toString();
		}
		return v;
	}
}
