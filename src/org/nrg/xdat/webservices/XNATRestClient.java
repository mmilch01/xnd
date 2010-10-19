package org.nrg.xdat.webservices;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.util.URIUtil;

import sun.misc.BASE64Encoder;

public class XNATRestClient extends WSTool
{
	protected static final String METHOD_FLAG = "m";
	protected static final String DEST_FLAG = "remote";
	protected static final String SRC_FLAG = "local";

	private static final int BYTE_BUFFER_LENGTH = 256;

	public XNATRestClient()
	{
		super();
	}

	@Override
	public boolean process()
	{
		// OUTPUT DIRECTORY
		String src = null;
		String dest = null;
		String method = null;

		File srcF = null;

		if ((!arguments.containsKey(METHOD_FLAG)))
		{
			System.out.println("Missing -m tag");
			displayHelp();
			System.exit(4);
		}

		method = (String) arguments.get(METHOD_FLAG);
		if (method.equalsIgnoreCase("GET"))
		{
			method = "GET";
		} else if (method.equalsIgnoreCase("POST"))
		{
			method = "POST";
		} else if (method.equalsIgnoreCase("PUT"))
		{
			method = "PUT";
		} else if (method.equalsIgnoreCase("DELETE"))
		{
			method = "DELETE";
		} else
		{
			System.out
					.println("Missing properly formed -m tag (GET, POST, PUT, DELETE)");
			displayHelp();
			System.exit(4);
		}

		if ((!arguments.containsKey(DEST_FLAG)))
		{
			System.out.println("Missing -dest tag");
			displayHelp();
			System.exit(4);
		}

		try
		{

			if (arguments.containsKey(SRC_FLAG))
				src = (String) arguments.get(SRC_FLAG);
			if (arguments.containsKey(DEST_FLAG))
			{
				dest = (String) arguments.get(DEST_FLAG);
			}

			if (src != null)
			{
				if (!(src.equals("") || src.equals("\"\"")))
				{
					srcF = new File(src);
					if (!srcF.exists()
							&& !(method.equals("GET") || method
									.equals("DELETE")))
					{
						throw new FileNotFoundException(src);
					}
				}
			}

			try
			{
				// REQUEST SESSION ID
				// call.setProperty(Call.CHARACTER_SET_ENCODING,"UTF-8");

				execute(srcF, dest, quiet, method);
			} catch (AxisFault ex2)
			{
				String fault = ex2.getFaultString();
				if (fault == null)
				{
					error(33, "Web Service Exception: " + host + "\n"
							+ ex2.getMessage(), ex2);
				} else if (fault.indexOf("PasswordAuthenticationException") != -1)
				{
					error(99, "Invalid Password.", ex2);
				} else if (fault.indexOf("FailedLoginException") != -1)
				{
					error(98, "Failed Login. Review username and password.",
							ex2);
				} else if (fault.indexOf("UserNotFoundException") != -1)
				{
					error(97, "Failed Login. Review username and password.",
							ex2);
				} else if (fault.indexOf("EnabledException") != -1)
				{
					error(96, "Failed Login. Account disabled.", ex2);
				} else
				{
					error(32, "Web Service Exception @ " + host + "\n" + fault,
							ex2);
				}
			} catch (RemoteException ex)
			{
				error(33, "Web Service Exception: " + host + "\n"
						+ ex.getMessage(), ex);
			} catch (MalformedURLException e)
			{
				error(12, "Web Service Exception: " + host + "\n"
						+ e.getMessage(), e);
			} catch (Throwable e)
			{
				error(13, "Web Service Exception: " + host + "\n"
						+ e.getMessage(), e);
			}
		} catch (FileNotFoundException ex)
		{
			error(34, ex.getMessage(), ex);
		}
		return true;
	}

	/**
	 * @param host
	 *            Host of the server i.e. 'http://localhost:8080/xnat'
	 * @param service_session
	 *            create from 'createServiceSession()'
	 * @param file
	 *            file to upload
	 * @param quiet
	 *            limits system output.
	 * @throws FileNotFoundException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public void execute(File srcF, String dest, boolean quiet, String method)
			throws FileNotFoundException, MalformedURLException, IOException
	{
		long startTime = System.currentTimeMillis();

		String urlString = null;
		if (host.endsWith("/") && dest.startsWith("/"))
		{
			urlString = host + dest.substring(1);

		} else if (!host.endsWith("/") && !dest.startsWith("/"))
		{
			if (!dest.startsWith("http"))
				urlString = host + "/" + dest;
			else
				urlString = dest;
		} else
		{
			urlString = host + dest;
		}

		if (urlString.indexOf("?") > -1)
		{
			String query = urlString.substring(urlString.indexOf("?") + 1);
			urlString = urlString.substring(0, urlString.indexOf("?") + 1);

			int count = 0;
			while (query.indexOf("&") > -1)
			{
				String pair = query.substring(0, query.indexOf("&"));
				query = query.substring(query.indexOf("&") + 1);
				String key = pair.substring(0, pair.lastIndexOf("="));
				String value = pair.substring(pair.lastIndexOf("=") + 1);

				if (count++ > 0)
					urlString += "&";

				urlString += URIUtil.encodeWithinQuery(key);
				urlString += "=";
				urlString += URIUtil.encodeWithinQuery(value);
			}

			if (query.indexOf("=") > -1)
			{
				String key = query.substring(0, query.lastIndexOf("="));
				String value = query.substring(query.lastIndexOf("=") + 1);

				if (count++ > 0)
					urlString += "&";

				urlString += URIUtil.encodeWithinQuery(key);
				urlString += "=";
				urlString += URIUtil.encodeWithinQuery(value);
			} else
			{
				urlString += query;
			}
		}

		HttpClient client = new HttpClient();
		HttpMethodBase filePost = null;
		if (method.equals("POST"))
		{
			filePost = new PostMethod(urlString);
		} else if (method.equals("GET"))
		{
			filePost = new GetMethod(urlString);
		} else if (method.equals("PUT"))
		{
			filePost = new PutMethod(urlString);
		} else if (method.equals("DELETE"))
		{
			filePost = new DeleteMethod(urlString);
		}

		if (userSessionID == null)
		{
			filePost.addRequestHeader("Authorization", "Basic "
					+ (new BASE64Encoder())
							.encode((this.userName + ":" + this.password)
									.getBytes()));
		} else
		{
			try
			{
				externalSessionID = true;
				String service_session = userSessionID;
				userSessionID = refreshServiceSession(service_session);
				if (!service_session.equals(userSessionID))
				{
					// System.out.println(userSessionID)
				}
				filePost.addRequestHeader("Cookie", "JSESSIONID="
						+ userSessionID);
			} catch (ServiceException e)
			{
				error(11, "Web Service Exception: " + host + "\n"
						+ e.getMessage(), e);
			}
		}

		client.getHttpConnectionManager().getParams()
				.setConnectionTimeout(5000);
		if (srcF != null && (filePost instanceof EntityEnclosingMethod))
		{
			Part[] parts = {new FilePart("img1", srcF)};
			((EntityEnclosingMethod) filePost)
					.setRequestEntity(new MultipartRequestEntity(parts,
							filePost.getParams()));
		} else if (srcF != null)
		{
			if (srcF.exists())
			{
				error(77, "File already Exists " + srcF.getAbsolutePath(), null);
			}
		}
		int statusCode = client.executeMethod(filePost);

		// write output
		InputStream is = filePost.getResponseBodyAsStream();

		OutputStream os = null;

		byte[] buff = new byte[BYTE_BUFFER_LENGTH];
		int bytesRead;
		int loaded = 0;

		if (srcF == null || (filePost instanceof EntityEnclosingMethod)
				|| (statusCode >= 200 || statusCode < 300))
		{
			os = System.out;
		} else
		{
			if (srcF.getParentFile() != null && !srcF.getParentFile().exists())
			{
				srcF.getParentFile().mkdirs();
			}
			os = new FileOutputStream(srcF);
			os = new BufferedOutputStream(os);

		}

		java.text.NumberFormat nf = NumberFormat.getInstance();
		while (-1 != (bytesRead = is.read(buff, 0, buff.length)))
		{
			os.write(buff, 0, bytesRead);
			os.flush();
			loaded = loaded + bytesRead;
		}

		is.close();
		os.flush();
		if (!(srcF == null || (filePost instanceof EntityEnclosingMethod) || (statusCode >= 200 || statusCode < 300)))
		{
			os.close();
			System.out.println();
		}

		if (statusCode >= 200 || statusCode < 300)
			System.exit(0);
		else
			System.exit(statusCode);

	}

	@Override
	public void loadHelpText()
	{
		super.loadHelpText();
		helpText.put(METHOD_FLAG, "HTTP Method (GET,POST,PUT,DELETE)");
		helpText.put(SRC_FLAG, "Local file to upload URL.");
		helpText.put(DEST_FLAG, "Remote URL to GET or POST to.");
	}

	@Override
	public void displayHelp()
	{
		System.out.println("\nXNAT REST Client Web Service\n");
		displayCommonHelp();
		printHelpLine(DEST_FLAG);
		printHelpLine(SRC_FLAG);
		printHelpLine(METHOD_FLAG);
	}

	public static void main(String[] args)
	{
		XNATRestClient arcGet = new XNATRestClient();
		arcGet.perform(args);
		try
		{
			HttpClient ht = new HttpClient();
			ht.getHttpConnectionManager().getParams()
					.setConnectionTimeout(5000);
			HttpMethodBase hmb = new GetMethod(
					"http://central.xnat.org/REST/projects");
			hmb.addRequestHeader("Authorization", "Basic "
					+ (new BASE64Encoder())
							.encode(("mmilch:abc123").getBytes()));
			int status = ht.executeMethod(hmb);
			hmb.releaseConnection();
		} catch (Exception e)
		{
		}

	}

}
