package org.nrg.xdat.webservices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jan 11, 2008
 * Modified from command line version by Mikhail Milchenko
 *
 */
public class StoreXARWS extends WSTool
{
	protected static final String FILE_FLAG = "f";

	public StoreXARWS()
	{
		super();
	}

	public boolean process()
	{
		// OUTPUT DIRECTORY
		String f = null;

		if (arguments.get(FILE_FLAG) == null)
		{
			displayHelp();
			return false;
			// System.exit(4);
		}

		try
		{

			f = (String) arguments.get(FILE_FLAG);
			File file = new File(f);
			if (!file.exists())
			{
				throw new FileNotFoundException(f);
			}

			String service_session = createServiceSession();
			boolean bRes = false;
			try
			{
				// REQUEST SESSION ID
				// call.setProperty(Call.CHARACTER_SET_ENCODING,"UTF-8");

				bRes = execute(host, service_session, file, quiet);
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
			closeServiceSession(service_session);
			return bRes;
		} catch (MalformedURLException e)
		{
			error(12, "Web Service Exception: " + host + "\n" + e.getMessage(),
					e);
			return false;
		} catch (AxisFault ex2)
		{
			String fault = ex2.getFaultString();
			if (fault == null)
			{
				error(33, "Web Service Exception: " + host + "\n"
						+ ex2.getMessage(), ex2);
				return false;
			} else if (fault.indexOf("PasswordAuthenticationException") != -1)
			{
				error(99, "Invalid Password.", ex2);
				return false;
			} else if (fault.indexOf("FailedLoginException") != -1)
			{
				error(98, "Failed Login. Review username and password.", ex2);
				return false;
			} else if (fault.indexOf("UserNotFoundException") != -1)
			{
				error(97, "Failed Login. Review username and password.", ex2);
				return false;
			} else if (fault.indexOf("EnabledException") != -1)
			{
				error(96, "Failed Login. Account disabled.", ex2);
				return false;
			} else
			{
				error(32, "Web Service Exception @ " + host + "\n" + fault, ex2);
				return false;
			}
		} catch (RemoteException ex)
		{
			error(33,
					"Web Service Exception: " + host + "\n" + ex.getMessage(),
					ex);
			return false;
		} catch (ServiceException ex)
		{
			error(11,
					"Web Service Exception: " + host + "\n" + ex.getMessage(),
					ex);
			return false;
		} catch (FileNotFoundException ex)
		{
			error(34, "File Not Found: " + f, ex);
			return false;
		}
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
	public boolean execute(String host, String service_session, File file,
			boolean quiet) throws FileNotFoundException, MalformedURLException,
			IOException
	{
		int counter = 0;

		long startTime = System.currentTimeMillis();
		String urlString = host + "app/template/StoreXAR.vm";

		URL url = new URL(urlString);

		PostMethod filePost = new PostMethod(urlString);
		filePost.setRequestHeader("Cookie", "JSESSIONID=" + service_session);

		Part[] parts = {new FilePart("archive", file)};
		filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost
				.getParams()));

		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams()
				.setConnectionTimeout(5000);

		int status = client.executeMethod(filePost);

		UploadResponse ur = new UploadResponse(filePost
				.getResponseBodyAsString());

		if (ur.getStatus().equals("COMPLETE"))
		{
			System.out.println("Upload Completed.");
			return true;
			// System.exit(0);
		} else
		{
			System.out.println("Error: " + ur.getMessage());
			return false;
			// System.exit(ur.getCode());

		}

		// // create a boundary string
		// String boundary = MultiPartFormOutputStream.createBoundary();
		// URLConnection urlConn =
		// MultiPartFormOutputStream.createConnection(url);
		// urlConn.setRequestProperty("Cookie", "JSESSIONID="+service_session);
		// urlConn.setRequestProperty("Accept", "*/*");
		// urlConn.setRequestProperty("Content-Type",
		// MultiPartFormOutputStream.getContentType(boundary));
		// // set some other request headers...
		// urlConn.setRequestProperty("Connection", "Keep-Alive");
		// urlConn.setRequestProperty("Cache-Control", "no-cache");
		// // no need to connect cuz getOutputStream() does it
		// MultiPartFormOutputStream out =
		// new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
		// // write a text field element
		// out.writeField("session", service_session);
		// // upload a file
		// out.writeFile("archive", "application/zip", file);
		// // can also write bytes directly
		// // out.writeFile("myFile", "text/plain", "C:\\test.txt",
		// // "This is some file text.".getBytes("ASCII"));
		// out.close();
		// // read response from server
		// BufferedReader in = new BufferedReader(
		// new InputStreamReader(urlConn.getInputStream()));
		// StringBuffer response = new StringBuffer();
		// String line = null;
		// while((line = in.readLine()) != null) {
		// response.append(line).append("\n");
		// }
		//             
		// UploadResponse ur = new UploadResponse(response.toString());
		//        
		// if(ur.getStatus().equals("COMPLETE")){
		// System.out.println("Upload Completed.");
		// System.exit(0);
		// }else{
		// System.out.println("Error: " + ur.getMessage());
		// System.exit(ur.getCode());
		//                 
		// }
		// Use Buffered Stream for reading/writing.

	}

	public void loadHelpText()
	{
		super.loadHelpText();
		helpText.put(FILE_FLAG, "File to upload.");
	}

	public void displayHelp()
	{
		System.out.println("\nStoreXAR Web Service\n");
		displayCommonHelp();
		printHelpLine(FILE_FLAG);
	}

	/*
	 * public static void main(String[] args) { StoreXARWS arcGet = new
	 * StoreXARWS(); arcGet.perform(args); }
	 */
	public class UploadResponse
	{
		private int code = 0;
		private String message = "";
		private String status = "";

		public UploadResponse(String content)
		{
			if (content != null)
			{
				if (content.indexOf("status=\"") != -1)
				{
					int start = content.indexOf("status=\"") + 8;
					int end = content.indexOf("\"", start);
					setStatus(content.substring(start, end));
				}
				if (content.indexOf("CODE=\"") != -1)
				{
					int start = content.indexOf("code=\"") + 6;
					int end = content.indexOf("\"", start);
					String _code = content.substring(start, end);
					if (_code != "")
					{
						try
						{
							int i = Integer.parseInt(_code);
							setCode(i);
						} catch (NumberFormatException e)
						{
							setCode(99);
						}
					}
				}
				if (content.indexOf("<message>") != -1)
				{
					int start = content.indexOf("<message>") + 9;
					int end = content.indexOf("</message>", start);
					setMessage(content.substring(start, end));
				}
			}
		}

		/**
		 * @return the code
		 */
		public int getCode()
		{
			return code;
		}
		/**
		 * @param code
		 *            the code to set
		 */
		public void setCode(int code)
		{
			this.code = code;
		}
		/**
		 * @return the message
		 */
		public String getMessage()
		{
			return message;
		}
		/**
		 * @param message
		 *            the message to set
		 */
		public void setMessage(String message)
		{
			this.message = message;
		}
		/**
		 * @return the status
		 */
		public String getStatus()
		{
			return status;
		}
		/**
		 * @param status
		 *            the status to set
		 */
		public void setStatus(String status)
		{
			this.status = status;
		}

	}
}
