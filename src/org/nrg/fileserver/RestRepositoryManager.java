/**
 * Copyright (c) 2008 Washington University
 */
package org.nrg.fileserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.datatype.DatatypeDocumentFactory;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.nrg.xnd.filetransfer.FileTransfer;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * RepositoryManager that uses the REST web services API to access the
 * underlying file server and database.
 * 
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 * 
 */
public final class RestRepositoryManager extends RepositoryManager
		implements
			FileTransfer
{
	private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	private static final String CONTENT_TYPE_METADATA = "application/x-xnat-metadata+xml";
	private static final String CONTENT_TYPE_DATA = "application/octet-stream";
	private static final String CONTENT_DISPOSITION_METADATA = "x-xnat-metadata";

	private static final String SERVICE_SEARCH = "/search";
	private static final String SERVICE_STORE = "/data";
	private static final String SERVICE_LABELS = "/tags";

	private static final Namespace NAMESPACE_XE = new Namespace(null,
			"http://nrg.wustl.edu/xe");
	private static final QName XML_LABELS = new QName("Labels", NAMESPACE_XE);
	private static final QName XML_LABELS_LABEL = new QName("Label",
			NAMESPACE_XE);
	private static final QName XML_SEARCH_RESPONSE_ROOT = new QName(
			"SearchResults", NAMESPACE_XE);
	private static final QName XML_SEARCH_RESPONSE_RESOURCE = new QName(
			"Resource", NAMESPACE_XE);
	private static final QName XML_SEARCH_RESPONSE_RESOURCE_URI = new QName(
			"URI", NAMESPACE_XE);
	private static final QName XML_RESPONSE_TAGS = new QName("Metadata",
			NAMESPACE_XE);
	private static final QName XML_RESPONSE_TAG = new QName("Tag", NAMESPACE_XE);
	private static final String XML_RESPONSE_TAG_ATTR_LABEL = "Label";
	private static final QName XML_RESPONSE_TAG_VALUE = new QName("Value",
			NAMESPACE_XE);
	private static final QName XML_REQUEST_TAGS = new QName("Metadata",
			NAMESPACE_XE);
	private static final QName XML_REQUEST_TAGS_DELETE = new QName("Delete",
			NAMESPACE_XE);
	private static final String XML_REQUEST_TAGS_DELETE_ATTR_LABEL = "Label";
	private static final QName XML_REQUEST_TAGS_TAG = new QName("Tag",
			NAMESPACE_XE);
	private static final String XML_REQUEST_TAGS_TAG_ATTR_LABEL = "Label";
	private static final QName XML_REQUEST_TAGS_TAG_VALUE = new QName("Value",
			NAMESPACE_XE);

	private static final Collection<Integer> HTTP_STATUS_SUCCESS = new HashSet<Integer>();
	static
	{
		HTTP_STATUS_SUCCESS.add(HttpStatus.SC_OK);
		HTTP_STATUS_SUCCESS.add(HttpStatus.SC_CREATED);
		HTTP_STATUS_SUCCESS.add(HttpStatus.SC_NO_CONTENT);
	}

	private static final DocumentFactory documentFactory = DatatypeDocumentFactory
			.getInstance();

	private final Logger log = Logger.getLogger(RestRepositoryManager.class);

	private final URI searchService;
	private final String storeService;
	private final URI labelsService;
	private final String m_root;

	public RestRepositoryManager(final URL server) throws URISyntaxException
	{
		final String root = server.toString();
		this.searchService = new URI(root + SERVICE_SEARCH);
		this.storeService = root + SERVICE_STORE;
		new URI(storeService); // verify that this is a valid URI
		this.labelsService = new URI(root + SERVICE_LABELS);
		m_root = root;
	}

	public String GetRoot()
	{
		return m_root;
	}

	private final URI getURI(final ItemRecord item, final Collection<?> args)
			throws URISyntaxException
	{
		final StringBuilder sb = new StringBuilder(storeService);
		sb.append("/");
		sb.append(item.getRelativePath());
		if (!args.isEmpty())
		{
			sb.append("?");
			join(args, "&", sb);
		}
		return new URI(sb.toString());
	}

	private final <T> URI getURI(final ItemRecord item, final T... args)
			throws URISyntaxException
	{
		return getURI(item, Arrays.asList(args));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#ItemRemove(org.nrg.xnat.repository
	 * .ItemRecord)
	 */
	@Override
	public boolean ItemRemove(final ItemRecord item)
	{
		final HttpDelete delete;
		try
		{
			delete = new HttpDelete(item.getRelativePath());
		} catch (URISyntaxException e)
		{
			log.error(e);
			return false;
		}
		try
		{
			return null != doRequest(delete);
		} catch (HttpException e)
		{
			log.error(e);
			return false;
		} catch (IOException e)
		{
			log.error(e);
			return false;
		}
	}

	private void reportInvalidElement(final QName expected, final Element e)
	{
		log.error("Received invalid element " + e.getQName() + ", expected "
				+ expected);
	}

	private final static ItemRecord[] EMPTY_RECORD_ARRAY = new ItemRecord[0];

	private ItemRecord[] doMetadataSearch(final ItemTag[] tags,
			final int maxrecords, final boolean getMetadata)
	{
		final StringBuilder search = new StringBuilder(searchService.toString());
		boolean hasOpts = false;
		if (0 <= maxrecords)
		{
			search.append(hasOpts ? "&" : "?");
			search.append("max-resources=");
			search.append(maxrecords);
			hasOpts = true;
		}
		if (!getMetadata)
		{
			search.append(hasOpts ? "&" : "?");
			search.append("no-metadata");
			hasOpts = true;
		}

		final Collection<String> constraints = new ArrayList<String>();
		for (final ItemTag tag : tags)
		{
			final Collection<String> values = new HashSet<String>(Arrays
					.asList(tag.GetAllValues()));
			if (values.isEmpty())
			{
				constraints.add("?" + tag.GetName());
			} else
			{
				if (values.contains("*"))
				{
					constraints.add(tag.GetName());
				} else
				{
					for (final String v : values)
					{
						final StringBuilder sb = new StringBuilder(tag
								.GetName());
						sb.append("=");
						sb.append(v);
						constraints.add(sb.toString());
					}
				}
			}
		}
		search.append(hasOpts ? "&" : "?");
		join(constraints, "&", search);

		final HttpURLConnection get;
		try
		{
			final URL url = new URL(search.toString());
			get = (HttpURLConnection) url.openConnection();
		} catch (MalformedURLException e)
		{
			log.error(e);
			return EMPTY_RECORD_ARRAY;
		} catch (IOException e)
		{
			log.error(e);
			return EMPTY_RECORD_ARRAY;
		}

		final Document d;
		try
		{
			if (HttpURLConnection.HTTP_OK != get.getResponseCode())
			{
				log.error("Error " + get.getResponseCode()
						+ " from GET request");
				log.error(get.getResponseMessage());
				return EMPTY_RECORD_ARRAY;
			}

			final SAXReader saxReader = new SAXReader(XMLReaderFactory
					.createXMLReader());
			d = saxReader.read(get.getInputStream());
		} catch (IllegalStateException e)
		{
			log.error(e);
			return EMPTY_RECORD_ARRAY;
		} catch (SAXException e)
		{
			log.error(e);
			return EMPTY_RECORD_ARRAY;
		} catch (DocumentException e)
		{
			log.error(e);
			return EMPTY_RECORD_ARRAY;
		} catch (IOException e)
		{
			log.error(e);
			return EMPTY_RECORD_ARRAY;
		} finally
		{
			get.disconnect();
		}

		final Element root = d.getRootElement();
		if (!XML_SEARCH_RESPONSE_ROOT.equals(root.getQName()))
		{
			reportInvalidElement(XML_SEARCH_RESPONSE_ROOT, root);
			return EMPTY_RECORD_ARRAY;
		}

		final String prefix = storeService + "/";
		final int prefixLen = prefix.length();

		final Collection<ItemRecord> items = new ArrayList<ItemRecord>();
		for (final Iterator<?> root_ei = root.elementIterator(); root_ei
				.hasNext();)
		{
			final Element resource = (Element) root_ei.next();
			if (!XML_SEARCH_RESPONSE_RESOURCE.equals(resource.getQName()))
			{
				reportInvalidElement(XML_SEARCH_RESPONSE_RESOURCE, resource);
				return EMPTY_RECORD_ARRAY;
			}
			final Element urie = resource
					.element(XML_SEARCH_RESPONSE_RESOURCE_URI);
			final String itemURI = urie.getText();
			final String relPath;
			if (itemURI.startsWith(prefix))
			{
				relPath = itemURI.substring(prefixLen);
			} else
			{
				log.warn("Unable to strip data service prefix " + prefix
						+ " from URI " + itemURI);
				relPath = itemURI;
			}
			final ItemRecord item = new ItemRecord(itemURI, relPath);
			for (final Object tageo : resource.elements(XML_RESPONSE_TAG))
			{
				final Element tage = (Element) tageo;
				final ItemTag tag = new ItemTag(tage
						.attributeValue(XML_RESPONSE_TAG_ATTR_LABEL));
				for (final Object valeo : tage.elements(XML_RESPONSE_TAG_VALUE))
				{
					tag.AddValue(((Element) valeo).getText());
				}
				item.tagSet(tag);
			}
			items.add(item);
		}

		// TODO: doesn't do anything with the <tag> return elements (yet)

		return items.toArray(EMPTY_RECORD_ARRAY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#DBItemFind(org.nrg.xnat.repository
	 * .ItemRecord, int, boolean)
	 */
	@Override
	public ItemRecord[] DBItemFind(final ItemRecord template,
			final int maxrecords, final boolean getMetadata)
	{
		if ("*".equals(template.getRelativePath()))
		{
			return doMetadataSearch(template.getAllTags(), maxrecords,
					getMetadata);
		} else
		{
			// If template URI is set and concrete, just compare the metadata
			// for the named resource
			final ItemRecord remote = new ItemRecord(template);
			try
			{
				if (getMetadata)
				{
					loadItemTags(remote);
				}
			} catch (IllegalStateException e)
			{
				log.error(e);
				return EMPTY_RECORD_ARRAY;
			} catch (URISyntaxException e)
			{
				log.error(e);
				return EMPTY_RECORD_ARRAY;
			} catch (HttpException e)
			{
				log.error(e);
				return EMPTY_RECORD_ARRAY;
			} catch (IOException e)
			{
				log.error(e);
				return EMPTY_RECORD_ARRAY;
			} catch (SAXException e)
			{
				log.error(e);
				return EMPTY_RECORD_ARRAY;
			} catch (DocumentException e)
			{
				log.error(e);
				return EMPTY_RECORD_ARRAY;
			}

			if (DBMatch(remote, template, getMetadata) && maxrecords > 0)
			{
				return new ItemRecord[]{remote};
			} else
			{
				return EMPTY_RECORD_ARRAY;
			}
		}
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
		throw new UnsupportedOperationException(
				"DBItemAdd not supported for remote repositories");
	}

	private final String[] EMPTY_STRING_ARRAY = {};

	public final boolean VerifyConnection()
	{
		try
		{
			final StringBuilder sb = new StringBuilder(searchService.toString());
			sb.append("??Project");
			final HttpURLConnection get;
			get = (HttpURLConnection) (new URL(sb.toString()).openConnection());
			get.disconnect();
			return HttpURLConnection.HTTP_OK == get.getResponseCode();
		} catch (Exception e)
		{
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.repository.RepositoryManager#DBTagFind(java.lang.String)
	 */
	@Override
	public String[] DBTagFind(final String name)
	{
		final StringBuilder surl = new StringBuilder(labelsService.toString());
		if (null != name && !"*".equals(name))
		{
			if (isValidTagLabel(name))
			{
				surl.append("?");
				surl.append(name);
			} else
			{
				log.error("invalid tag search " + name);
			}
		}

		final HttpURLConnection get;
		try
		{
			final URL url = new URL(surl.toString());
			get = (HttpURLConnection) url.openConnection();
		} catch (MalformedURLException e)
		{
			log.error(e);
			return EMPTY_STRING_ARRAY;
		} catch (IOException e)
		{
			log.error(e);
			return EMPTY_STRING_ARRAY;
		}

		// TODO: validating?
		final Document d;
		try
		{
			if (HttpURLConnection.HTTP_OK != get.getResponseCode())
			{
				log.error("Error " + get.getResponseCode()
						+ " from GET request");
				log.error(get.getResponseMessage());
				return EMPTY_STRING_ARRAY;
			}

			final SAXReader saxReader = new SAXReader(XMLReaderFactory
					.createXMLReader());
			d = saxReader.read(get.getInputStream());
		} catch (SAXException e)
		{
			log.error("Error parsing GET response entity", e);
			return EMPTY_STRING_ARRAY;
		} catch (DocumentException e)
		{
			log.error("Error parsing GET response entity", e);
			return EMPTY_STRING_ARRAY;
		} catch (IOException e)
		{
			log.error("Error parsing GET response entity", e);
			return EMPTY_STRING_ARRAY;
		} finally
		{
			get.disconnect();
		}

		final Element e = d.getRootElement();
		if (XML_LABELS.equals(e.getQName()))
		{
			final Collection<String> labels = new LinkedList<String>();
			final Iterator<?> ei = e.elementIterator();
			while (ei.hasNext())
			{
				final Element le = (Element) ei.next();
				if (XML_LABELS_LABEL.equals(le.getQName()))
				{
					labels.add(le.getText());
				} else
				{
					log.error("unexpected label element " + e.getQName());
					return EMPTY_STRING_ARRAY;
				}
			}
			return labels.toArray(new String[0]);
		} else
		{
			log.error("unexpected root element " + e.getQName());
			return EMPTY_STRING_ARRAY;
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
		if (!isValidTagLabel(name))
			return false;
		final HttpDelete delete;
		try
		{
			delete = new HttpDelete(labelsService + "?" + name);
		} catch (URISyntaxException e1)
		{
			return false;
		}

		final HttpClient client = new DefaultHttpClient();
		final HttpResponse response;
		try
		{
			response = client.execute(delete);
		} catch (HttpException e)
		{
			log.error("DELETE request failed", e);
			return false;
		} catch (IOException e)
		{
			log.error("DELETE request failed", e);
			return false;
		}

		final StatusLine status = response.getStatusLine();
		if (!HTTP_STATUS_SUCCESS.contains(status.getStatusCode()))
		{
			log.error("Error " + status.getStatusCode()
					+ " from DELETE request");
			log.error(status.getReasonPhrase());
			return false;
		}

		final HttpEntity re = response.getEntity();
		// TODO: validating?
		final Document d;
		try
		{
			final SAXReader saxReader = new SAXReader(XMLReaderFactory
					.createXMLReader());
			d = saxReader.read(re.getContent());
		} catch (SAXException e)
		{
			log.error("Error parsing DELETE response entity", e);
			return false;
		} catch (DocumentException e)
		{
			log.error("Error parsing DELETE response entity", e);
			return false;
		} catch (IOException e)
		{
			log.error("Error parsing DELETE response entity", e);
			return false;
		}

		final Element e = d.getRootElement();
		// TODO: verify tag
		final Iterator<?> ei = e.elementIterator();
		if (!ei.hasNext())
		{
			return false;
		}
		final Element le = (Element) ei.next(); // TODO: verify tag
		return (name.equalsIgnoreCase(le.getText()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.nrg.xnat.repository.RepositoryManager#DBTagAdd(java.lang.String)
	 */
	@Override
	public boolean DBTagAdd(final String name)
	{
		if (!isValidTagLabel(name))
			return false;
		final HttpPost post;
		try
		{
			post = new HttpPost(labelsService + "?" + name);
		} catch (URISyntaxException e1)
		{
			return false;
		}
		log.debug("POST: " + post.getURI());

		final HttpClient client = new DefaultHttpClient();
		final HttpResponse response;
		try
		{
			response = client.execute(post);
		} catch (HttpException e)
		{
			log.error("POST request failed", e);
			return false;
		} catch (IOException e)
		{
			log.error("POST request failed", e);
			return false;
		}

		final StatusLine status = response.getStatusLine();
		if (HTTP_STATUS_SUCCESS.contains(status.getStatusCode()))
		{
			return true;
		} else
		{
			log.error("Error " + status.getStatusCode() + " from POST request");
			log.error(status.getReasonPhrase());
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
		final Document d = DocumentFactory.getInstance().createDocument();
		addTagsElement(d, tag);

		final HttpEntity entity = makeDocumentEntity(d, CONTENT_TYPE_METADATA);
		try
		{
			final boolean success = null != doPut(getURI(item), entity,
					CONTENT_DISPOSITION_METADATA);
			if (success)
			{
				item.tagSet(tag);
			}
			return success;
		} catch (HttpException e)
		{
			log.error(e);
			return false;
		} catch (IOException e)
		{
			log.error(e);
			return false;
		} catch (URISyntaxException e)
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
		final String name = tag.GetName();
		final Document d = DocumentFactory.getInstance().createDocument();
		final Element root = addTagsElement(d);
		final Element delete = root.addElement(XML_REQUEST_TAGS_DELETE);
		delete.addAttribute(XML_REQUEST_TAGS_DELETE_ATTR_LABEL, name);

		final HttpEntity entity = makeDocumentEntity(d, CONTENT_TYPE_METADATA);
		try
		{
			final boolean success = null != doPut(getURI(item), entity,
					CONTENT_DISPOSITION_METADATA);
			if (success)
			{
				item.tagRemove(name);
			}
			return success;
		} catch (HttpException e)
		{
			log.error(e);
			return false;
		} catch (IOException e)
		{
			log.error(e);
			return false;
		} catch (URISyntaxException e)
		{
			log.error(e);
			return false;
		}
	}

	private static Element addTagsElement(final Branch root,
			final ItemTag... tags)
	{
		final Element tagse = root.addElement(XML_REQUEST_TAGS);
		for (final ItemTag tag : tags)
		{
			final Element tage = tagse.addElement(XML_REQUEST_TAGS_TAG);
			tage.addAttribute(XML_REQUEST_TAGS_TAG_ATTR_LABEL, tag.GetName());
			for (final String val : tag.GetAllValues())
			{
				final Element vale = tage
						.addElement(XML_REQUEST_TAGS_TAG_VALUE);
				vale.setText(val);
			}
		}
		return tagse;
	}

	private static HttpEntity makeDocumentEntity(final Document d,
			final String mediaType)
	{
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final XMLWriter writer;
		try
		{
			writer = new XMLWriter(bos);
		} catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException("default encoding is unsupported?", e);
		}
		try
		{
			writer.write(d);
			writer.close();
		} catch (IOException e)
		{
			throw new RuntimeException("IO error on ByteArrayOutputStream?", e);
		}

		final ByteArrayEntity entity = new ByteArrayEntity(bos.toByteArray());
		if (null != mediaType)
		{
			entity.setContentType(mediaType);
		}
		return entity;
	}

	private static HttpEntity makeMetadataEntity(final ItemRecord item)
	{
		// Extract metadata for transport
		final ItemTag[] tags = item.getAllTags();
		if (0 == tags.length)
		{
			return null;
		}

		final Document metadoc = documentFactory.createDocument();
		addTagsElement(metadoc, tags);
		return makeDocumentEntity(metadoc, CONTENT_TYPE_METADATA);
	}

	private HttpResponse doRequest(final HttpUriRequest request)
			throws HttpException, IOException
	{
		final HttpClient client = new DefaultHttpClient();
		final HttpResponse response = client.execute(request);
		final StatusLine status = response.getStatusLine();
		if (HTTP_STATUS_SUCCESS.contains(status.getStatusCode()))
		{
			return response;
		} else
		{
			log.error(status);
			return null;
		}
	}

	// private HttpResponse doPost(final URI uri, final HttpEntity entity, final
	// String disposition)
	// throws HttpException,IOException {
	// final HttpPost post = new HttpPost(uri);
	// post.setEntity(entity);
	// if (null != disposition) {
	// post.setHeader(HEADER_CONTENT_DISPOSITION, disposition);
	// }
	// return doRequest(post);
	// }

	private HttpResponse doPut(final URI uri, final HttpEntity entity,
			final String disposition) throws HttpException, IOException
	{
		final HttpPut put = new HttpPut(uri);
		put.setEntity(entity);
		if (null != disposition)
		{
			put.setHeader(HEADER_CONTENT_DISPOSITION, disposition);
		}
		return doRequest(put);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.fileserver.ItemManager#Put(org.nrg.xnat.repository.ItemRecord
	 * , org.nrg.xnat.repository.ItemRecord)
	 */
	// public boolean Put(final ItemRecord dest, final ItemRecord src, boolean
	// isClient)
	@Override
	public boolean Put(final ItemRecord dest, final ItemRecord src)
	{
		final URI uri;
		try
		{
			uri = getURI(dest);
		} catch (URISyntaxException e)
		{
			log.error(e);
			return false;
		}

		final HttpEntity dataEntity = new FileEntity(src.getFile(),
				CONTENT_TYPE_DATA);
		final HttpEntity metadataEntity = makeMetadataEntity(src);

		try
		{
			if (null != metadataEntity)
			{
				return null != doPut(uri, metadataEntity,
						CONTENT_DISPOSITION_METADATA)
						&& null != doPut(uri, dataEntity, null);
			} else
			{
				return null != doPut(uri, dataEntity, null);
			}
		} catch (HttpException e)
		{
			log.error(e);
			return false;
		} catch (IOException e)
		{
			log.error(e);
			return false;
		}
	}

	private final void loadItemTags(final ItemRecord dest)
			throws URISyntaxException, HttpException, IOException,
			SAXException, IllegalStateException, DocumentException
	{
		final URI metadataURI = getURI(dest, "part=metadata");
		final URL url = metadataURI.toURL();
		final HttpURLConnection get = (HttpURLConnection) url.openConnection();

		final Document d;
		try
		{
			final SAXReader saxReader = new SAXReader(XMLReaderFactory
					.createXMLReader()); // TODO: validating?
			d = saxReader.read(get.getInputStream());
		} finally
		{
			get.disconnect();
		}

		final Element root = d.getRootElement();
		if (!XML_RESPONSE_TAGS.equals(root.getQName()))
		{
			throw new DocumentException("Invalid response root element "
					+ root.getQName() + ", expected " + XML_RESPONSE_TAGS);
		}

		// Looks like a reasonable response from the server; replace the
		// metadata.
		dest.m_Tags.clear(); // TODO: do something cleaner

		for (final Object tageo : root.elements(XML_RESPONSE_TAG))
		{
			final Element tage = (Element) tageo;
			final String name = tage
					.attributeValue(XML_RESPONSE_TAG_ATTR_LABEL);
			// TODO: this supports single-value-only tags.
			final Element vale = tage.element(XML_RESPONSE_TAG_VALUE);
			final ItemTag tag = new ItemTag(name, vale.getText());
			dest.tagSet(tag);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.nrg.xnat.fileserver.ItemManager#Get(org.nrg.xnat.repository.ItemRecord
	 * , org.nrg.xnat.repository.ItemRecord)
	 */
	@Override
	public boolean Get(final ItemRecord src, final ItemRecord dest)
	{

		final File resource = dest.getFile();
		resource.getParentFile().mkdirs();

		try
		{
			final URL url = getURI(src).toURL();
			final HttpURLConnection get = (HttpURLConnection) url
					.openConnection();
			get.connect();
			final InputStream is = get.getInputStream();
			final FileOutputStream fos = new FileOutputStream(resource);
			try
			{
				IOUtils.copy(is, fos);
			} finally
			{
				get.disconnect();
				fos.close();
			}

			loadItemTags(src);
			dest.tagsSetFromArray(src.getAllTags());
			return true;
		} catch (HttpException e)
		{
			log.error(e);
		} catch (IOException e)
		{
			log.error(e);
		} catch (IllegalStateException e)
		{
			log.error(e);
		} catch (URISyntaxException e)
		{
			log.error(e);
		} catch (SAXException e)
		{
			log.error(e);
		} catch (DocumentException e)
		{
			log.error(e);
		}
		return false;
	}

	private static final Pattern VALID_LABEL_PATTERN = Pattern.compile("\\w+");

	/**
	 * Is the given String a valid tag label?
	 * 
	 * @param label
	 *            String to be tested
	 * @return true if a valid label, false otherwise.
	 */
	public static boolean isValidTagLabel(final String label)
	{
		return null != label && VALID_LABEL_PATTERN.matcher(label).matches();
	}

	private static void join(final Iterable<?> a, final String separator,
			final StringBuilder sb)
	{
		final Iterator<?> ai = a.iterator();
		if (ai.hasNext())
		{
			sb.append(ai.next());
			while (ai.hasNext())
			{
				sb.append(separator);
				sb.append(ai.next());
			}
		}
	}

	// private static String join(final Iterable<?> a, final String separator) {
	// final StringBuilder sb = new StringBuilder();
	// join(a, separator, sb);
	// return sb.toString();
	// }
}
