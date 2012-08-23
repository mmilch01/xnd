package org.nrg.fileserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.nrg.xnd.filetransfer.FileTransfer;
import org.nrg.xnd.ontology.DefaultOntologyManager;
import org.nrg.xnd.ontology.XNATTableParser;
import org.nrg.xnd.ontology.XNATThesaurus;

import sun.misc.BASE64Encoder;

public class XNATRestAdapter extends RepositoryManager implements FileTransfer
{
	private final String m_root;
	private final String m_usr;
	private final String m_pass;
	private final String m_RESTCharsProhibited[]={"[","]","@"};
	private final String m_RESTCharsProhibitedCodes[]={"%5B", "%5D", "%40"};
	private boolean m_bReportConnErrors = true;
	public final static int GET = 0, PUT = 1, POST = 2, DELETE = 3;

	@Override
	public boolean SessionInit(Vector params)
	{
		return true;
	}
	public void enableConnErrorReport(boolean bEnable)
	{
		m_bReportConnErrors = bEnable;
	}
	public String GetRoot()
	{
		return m_root;
	}
	public boolean Put(ItemRecord remote, ItemRecord local)
	{
		String s;
		if (local.getTag("Project") == null
				|| local.getTag("Project").GetFirstValue().length() < 1)
			return false;
		s = FormSubQuery(DefaultOntologyManager.GetContext(local));
		s += "/files/" + local.getFile().getName() + "?inbody=true";
		File f = local.getFile();
		if (f == null || !f.exists())
			return false;
		HttpMethodBase method = PerformConnection(PUT, s, f);
		if (method == null)
			return false;
		method.releaseConnection();
		return true;
	}
	public boolean Get(ItemRecord remote, ItemRecord local)
	{
		String path = remote.getRelativePath();
		if (path.startsWith("/REST"))
			path = path.substring(5);
		else if (path.startsWith("REST"))
			path = path.substring(4);
		HttpMethodBase get = PerformConnection(GET, path, "");
		if (get == null)
			return false;

		try
		{
			final InputStream is = get.getResponseBodyAsStream();
			final FileOutputStream fos = new FileOutputStream(local
					.getAbsolutePath());
			try
			{
				IOUtils.copy(is, fos);
			} finally
			{
				get.releaseConnection();
				fos.close();
			}
		} catch (Exception e)
		{
			System.out.println("XNATRestAdapter.Get exception: "+ e.getMessage());
//			Utils.logger.error("XNATRestAdapter.Get exception", e);
			return false;
		}
		return true;
	}
	public boolean CreateSubject(ItemRecord ir)
	{
		String request = "/projects/" + ir.getTagValue("Project")
				+ "/subjects/" + ir.getTagValue("Subject");
		final HttpMethodBase method = PerformConnection(PUT, request, "");
		if (method != null)
		{
			method.releaseConnection();
			return true;
		}
		return false;
	}
	private String AdaptRESTQuery(String q)
	{
		for (int i=0; i<m_RESTCharsProhibited.length; i++)
			q=q.replace(m_RESTCharsProhibited[i], m_RESTCharsProhibitedCodes[i]);
		return q;
	}
	public HttpMethodBase PerformConnection(int type, String request,
			Object body)
	{
		final HttpMethodBase method;
		boolean bParams = request.contains("?");
		String q="";
		InputStream is = null;
		try		
		{
//			URLEncoder.encode(str,"UTF-8")
			String req=AdaptRESTQuery(request);
			q = m_root + ("/REST/" + req + (bParams ? "&" : "?") + (req
							.contains("format=") ? "" : "format=xml")).replace(
							"//", "/").replace("//", "/");
			switch (type)
			{
				case GET :
					method = new GetMethod(q);
					// if(params.length()>0) q+="&"+params;
					break;
				case PUT :
					method = new PutMethod(q);
					if (body != null)
					{
						if (body instanceof File)
						{
							try
							{
								is = new FileInputStream((File) body);
								((PutMethod) method)
										.setRequestEntity(new InputStreamRequestEntity(
												is));
								// setRequestBody(fis);
							} catch (FileNotFoundException fnf)
							{
								if (is != null)
									is.close();
								return null;
							}
						} else if (body instanceof InputStream)
						{
							is = (InputStream) body;
							try
							{
								// ((PutMethod)method).setRequestBody(is);
								((PutMethod) method)
										.setRequestEntity(new InputStreamRequestEntity(
												is));
							} catch (Exception ex)
							{
								is.close();
								return null;
							}
						}
					}
					break;
				case POST :
					method = new PostMethod(q);
					break;
				case DELETE :
					method = new DeleteMethod(q);
					break;
				default :
					return null;
			}
		} catch (Exception e)
		{
			if (is != null)
				try
				{
					is.close();
				} catch (Exception e1)
				{
				};
			return null;
		}
		method.addRequestHeader("Authorization", "Basic "
				+ (new BASE64Encoder()).encode((m_usr + ":" + m_pass)
						.getBytes()));
		try
		{
			HttpClient client = new HttpClient();
			client.getHttpConnectionManager().getParams().setConnectionTimeout(
					5000);

			System.out.println(method.getName() + " " + q);
			int status = client.executeMethod(method);
			if (HttpStatus.SC_OK != status)
			{
				if (m_bReportConnErrors)
					System.err
							.println("XNAT Rest adapter.PerformConnection: HTTP response "
									+ status);
				if (is != null)
					is.close();
				// Utils.logger.error("XNAT Rest adapter.PerformConnection: HTTP response "+status);
				System.err.println(method.getName() + " (error code " + status
						+ "): " + q);
				method.releaseConnection();
				return null;
			}

			// System.out.println(method.getResponseBodyAsString());
		} catch (Exception ex)
		{
//			Utils.logger.error("XNAT Rest adapter: HTTP connection failed", ex);
			System.out.println("REST HTTP connecton failed");
			System.err.println(method.getName() + " (error): " + q);
			if (is != null)
				try
				{
					is.close();
				} catch (Exception e)
				{
				};
			method.releaseConnection();
			return null;
		}
		if (is != null)
			try
			{
				is.close();
			} catch (Exception e)
			{
			};
		System.err.println(method.getName() + " (success): " + q);
		return method;
	}

	public boolean VerifyConnection()
	{
		HttpMethodBase m = PerformConnection(GET, "/projects?accessible=true",
				"");
		if (m != null)
			m.releaseConnection();
		return (m != null);
	}

	public XNATRestAdapter(final String server, final String usr,
			final String pass)
	{
		m_root = server;
		m_usr = usr;
		m_pass = pass;
	}
	public static String FormSubQuery(Collection<ItemTag> context)
	{
		String name, label;
		String result = "/";
		for (ItemTag it : context)
		{
			name = it.GetName().toLowerCase() + "s";
			label = it.GetFirstValue();
			result += name + "/" + label + XNATThesaurus.GetPostfix(it) + "/";
		}

		return result;
	}
	@Override
	public boolean DBItemAdd(ItemRecord item)
	{
		// so far, this is a read-only adapter.
		return false;
	}

	/*
	 * (non-Javadoc) This function returns corresponding files belonging to the
	 * lowest defined hierarchical level.
	 */
	@Override
	public ItemRecord[] DBItemFind(ItemRecord template, int maxrecords,
			boolean retrieveMetadata)
	{
		ItemRecord[] records = new ItemRecord[1];
		records[0] = template;
		return records;
	}

	/**
	 * Files from the given hierarchical level.
	 * 
	 * @param rel_path
	 *            ignored
	 * @param tagsMatching
	 *            both name and value for tags in this collection should match
	 * @param tagsDefined
	 *            empty array.
	 * @param tagsUndefined
	 *            ignored
	 * @return
	 */
	@Override
	public Collection<ItemRecord> DBItemFindEx(String rel_path,
			TagSet tagsMatching, Collection<String> tagsDefined,
			Collection<String> tagsUndefined)
	{
		// only resources can have files in them.
		if (tagsMatching instanceof Context)
		{
			if (((Context) tagsMatching).getLast().GetName().compareTo(
					"Resource") != 0)
				// if(tagsMatching getLast().GetName().compareTo("Resource")!=0)
				return new LinkedList<ItemRecord>();
		}
		String query = FormSubQuery(tagsMatching);
		LinkedList<TreeMap<String, String>> row_map;
		LinkedList<ItemRecord> res = new LinkedList<ItemRecord>();
		try
		{
			HttpMethodBase get = PerformConnection(GET, query + "/files", "");
			if (get == null)
				System.err.println("DBInteFindEx: GET failed");
//				Utils.logger.error("DBInteFindEx: GET failed");
			row_map = XNATTableParser.GetRows(new SAXReader().read(get
					.getResponseBodyAsStream()));
			get.releaseConnection();
			ItemRecord ir;
			for (TreeMap<String, String> row : row_map)
			{
				ir = new ItemRecord(null, row.get("URI"));
				ir.tagsSet(tagsMatching);
				XNATTableParser.SetAllTags(ir, row);
				res.add(ir);
			}
		} catch (DocumentException e)
		{
			System.err.println("DBTagValues: Document exception when reading input from "
					+ m_root);
//			Utils.logger
//					.error("DBTagValues: Document exception when reading input from "
//							+ m_root);
		} catch (IOException e)
		{
			System.err.println("DBTagValues: IOException when connecting to "
					+ m_root);
//			Utils.logger.error("DBTagValues: IOException when connecting to "
//					+ m_root);
		}
		return res;
	}

	/**
	 * Find all child tags for a given hierarchy.
	 * 
	 * @param path
	 *            Contains defined tag hierarchy: Tag_1:
	 *            (Name_1=Value_1),...,Tag_N:(Name_N=Value_N)
	 * @param query_tags
	 *            child tags to be populated qTag_1: (qName_1=?),...,(qName_M=?)
	 * @return set values in query_tags; map of associated tags for each query
	 *         tag
	 */
	@Override
	public TreeMap<ItemTag, TagMap> DBTagValues(final Context path,
			final TagMap query_tags)
	{
		String query = FormSubQuery(path);
		String q1;
		LinkedList<TreeMap<String, String>> row_map;
		TreeMap<ItemTag, TagMap> aTags = new TreeMap<ItemTag, TagMap>();
		TagMap llit;
		Context nqt = new Context();

		try
		{
			ItemRecord ir;
			String qp;
			for (ItemTag it : query_tags)
			{
				q1 = query + "/" + it.GetName().toLowerCase() + "s";
				qp = XNATThesaurus.getQueryParams(it);
				final HttpMethodBase get = PerformConnection(GET, q1
						+ (qp.length() > 0 ? ("?" + XNATThesaurus
								.getQueryParams(it)) : ""), "");
				if (get != null)
				{
					row_map = XNATTableParser.GetRows(new SAXReader().read(get
							.getResponseBodyAsStream()));
					get.releaseConnection();
					ItemTag qTag;
					for (TreeMap<String, String> row : row_map)
					{
						String val = row.get(XNATThesaurus.GetIDVarname(it));
						if (val != null)
						{
							qTag = new ItemTag(it.GetName(), val);
							ir = new ItemRecord(null, null);
							XNATTableParser.SetAllTags(ir, row);
							llit = new TagMap();
							llit.add(new ItemTag(qTag.GetName(), val));
							llit.addAll(ir.getTagCollection());
							aTags.put(qTag, llit);
						}

					}
					nqt.add(new ItemTag(it.GetName()));
				}
			}
			query_tags.clear();
			query_tags.addAll(nqt);
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());			
		}
/*		
		catch (DocumentException e)
		{
			Utils.logger
					.error("DBTagValues: Document exception when reading input from "
							+ m_root);
		} catch (MalformedURLException e)
		{
			Utils.logger
					.error("DBTagValues: URL exception when trying to connect to "
							+ m_root);
		} catch (IOException e)
		{
			Utils.logger.error("DBTagValues: IOException when reading from "
					+ m_root);
		}
*/		
		return aTags;
	}

	@Override
	public boolean DBTagAdd(String name)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean DBTagAttach(ItemRecord item, ItemTag tag)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean DBTagDelete(String name)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean DBTagDetach(ItemRecord item, ItemTag tag)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String[] DBTagFind(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean ItemRemove(ItemRecord template)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
