package org.nrg.xdat.webservices;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;

/**
 * @author timo Modified from command line version by Mikhail Milchenko
 * 
 */
public class StoreXMLWS extends WSTool
{
	private String service_session = null;
	/**
	 * @param args
	 */
	public StoreXMLWS()
	{
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see WSTool#displayHelp()
	 */
	@Override
	public void displayHelp()
	{
		System.out.println("\nStore XML Web Service\n");
		System.out.println("");
		System.out.println("Parameters:");
		System.out.println("-u          USERNAME");
		System.out.println("-p          PASSWORD");
		System.out
				.println("-host       URL to XNAT based website.  (i.e. localhost/xnat)");
		System.out.println("-location   location of xml file to insert.");
		System.out.println("-dir        directory containing files to insert.");
		System.out
				.println("-r          in combination with the dir tag, this will cause the app to descend into sub folders looking for xml files.");
		System.out
				.println("-allowDataDeletion    (REQUIRED) (either 'true' or 'false'): Whether or not pre-existing data for this element which has no unique indentifiers specified, should be overwritten.  If 'true' the pre-existing rows will be removed before the new rows are inserted.  If 'false', then the new rows will be added (appended) without affect to the pre-existing rows.");
		System.out
				.println("-stopAtException       stop At Exception (Defaults to true): for batch mode");
		System.out.println("-quiet      Minimize system output.");
		System.out.println("-h          Print help.");
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see WSTool#process()
	 */
	@Override
	public boolean process()
	{
		Hashtable hash = this.arguments;

		String s = (String) hash.get("allowDataDeletion");
		if (s == null)
		{
			System.out
					.println("ERROR CODE 17: Missing required value for -allowDataDeletion.");
			displayHelp();
			return false;
			// System.exit(17);
		}
		if (s.equalsIgnoreCase("true"))
		{
			hash.put("allowItemOverwrite", new Boolean(true));
		} else
		{
			hash.put("allowItemOverwrite", new Boolean(false));
		}

		s = (String) hash.get("stopAtException");
		if (s != null)
		{
			if (s.equalsIgnoreCase("true"))
			{
				hash.put("stopAtException", new Boolean(true));
			} else
			{
				hash.put("stopAtException", new Boolean(false));
			}
		}

		Boolean quarantine = (Boolean) hash.get("quarantine");
		Boolean allowItemOverwrite = (Boolean) hash.get("allowItemOverwrite");

		boolean stopAtException = true;
		if (hash.get("stopAtException") != null)
		{
			stopAtException = ((Boolean) hash.get("stopAtException"))
					.booleanValue();
		}

		String dir = (String) arguments.get("dir");
		String location = (String) arguments.get("location");
		try
		{
			service_session = this.createServiceSession();

			if (dir == null)
			{
				if (location == null)
				{
					System.out
							.println("ERROR CODE 18: Must define a file location or directory location.");
					displayHelp();
					return false;
					// System.exit(18);
				}
				/*
				 * File f = new File((String)arguments.get("location")); if
				 * (!f.exists()) { System.out.println("Unable to find file: " +
				 * f.getAbsolutePath()); return false; // System.exit(1); }
				 */
				sendFile(location, quarantine, allowItemOverwrite);
			} else
			{
				// tool.info((String)hash.get("dir") +"," + hash.get("r")+ "," +
				// quarantine + ","+allowItemOverwrite.booleanValue());
				File d = new File((String) hash.get("dir"));

				if (hash.get("r") == null)
				{
					storeXMLFolderService(d, false, quarantine,
							allowItemOverwrite, stopAtException);
				} else
				{
					storeXMLFolderService(d, true, quarantine,
							allowItemOverwrite, stopAtException);
				}
			}

			closeServiceSession(service_session);
		} catch (AxisFault ex2)
		{
			String fault = ex2.getFaultString();
			if (fault == null)
			{
				System.out.println("ERROR CODE 33: \nWeb Service Exception: "
						+ host + "\n" + ex2.getMessage());
				logError(ex2);
				return false;
				// System.exit(33);
			} else if (fault.indexOf("PasswordAuthenticationException") != -1)
			{
				System.out.println("ERROR CODE 99: \nInvalid Password.");
				logError(ex2);
				return false;
				// System.exit(99);
			} else if (fault.indexOf("FailedLoginException") != -1)
			{
				System.out
						.println("ERROR CODE 98: \nFailed Login. Review username and password.");
				logError(ex2);
				return false;
				// System.exit(98);
			} else if (fault.indexOf("UserNotFoundException") != -1)
			{
				System.out
						.println("ERROR CODE 97: \nFailed Login. Review username and password.");
				logError(ex2);
				return false;
				// System.exit(98);
			} else if (fault.indexOf("EnabledException") != -1)
			{
				System.out
						.println("ERROR CODE 96: \nFailed Login. Account disabled.");
				logError(ex2);
				return false;
				// System.exit(98);
			} else
			{
				System.out.println("ERROR CODE 32: \nWeb Service Exception @ "
						+ host + "\n" + fault);
				logError(ex2);
				return false;
				// System.exit(32);
			}
		} catch (RemoteException ex2)
		{
			System.out.println("Error Storing File.");
			System.out.println(ex2.getMessage());
			logError(ex2);
			return false;
			// System.exit(1);
		} catch (ServiceException e)
		{
			System.out.println("Error Storing File.");
			System.out.println(e.getMessage());
			logError(e);
			return false;
			// System.exit(1);
		} catch (MalformedURLException e)
		{
			System.out.println("ERROR CODE: 12\nUnknown URL: " + host);
			logError(e);
			return false;
			// System.exit(12);
		}
		return true;
		// System.exit(0);
	}
	private void sendFile(File f, Boolean quarantine, Boolean allowItemOverwrite)
			throws RemoteException, ServiceException
	{
		sendFile(GetContents(f), quarantine, allowItemOverwrite);
	}
	private void sendFile(/* File f */String contents, Boolean quarantine,
			Boolean allowItemOverwrite) throws RemoteException,
			ServiceException
	{
		// String contents = GetContents(f);

		Call call = createCall(service_session);
		call.setTargetEndpointAddress(host + "axis/StoreXML.jws");

		call.setOperationName("store");
		Object[] params = {service_session, contents, quarantine,
				allowItemOverwrite};

		// if (!quiet)System.out.println("\nFound Document: " +
		// f.getAbsolutePath());
		if (!quiet)
			System.out.println("Sending Request...");
		long startTime = Calendar.getInstance().getTimeInMillis();
		String o = (String) call.invoke(params);
		long duration = Calendar.getInstance().getTimeInMillis() - startTime;
		if (!quiet)
			System.out.println("Response Received (" + duration + " ms)");

		System.out.println(o);
		call = null;
	}

	private void storeXMLFolderService(File dir, boolean recursive,
			Boolean quarantine, Boolean allowItemOverwrite,
			boolean stopAtException) throws RemoteException, ServiceException
	{
		if (!dir.exists())
		{
		} else
		{
			ArrayList dirs = new ArrayList();
			File[] list = dir.listFiles();
			for (int i = 0; i < list.length; i++)
			{
				if (list[i].getName().endsWith(".xml"))
				{
					try
					{
						sendFile(list[i], quarantine, allowItemOverwrite);
					} catch (AxisFault ex2)
					{
						System.out.println("Error Storing "
								+ list[i].getAbsolutePath());
						System.out.println(ex2.getFaultString());

						if (stopAtException)
						{
							throw ex2;
						} else
						{
							OutputToFile("Error Storing " + list[i].getName()
									+ " " + ex2.getFaultString(),
									"storeXMLexceptions.txt", true);
						}
					} catch (RemoteException ex2)
					{
						System.out.println("Error Storing "
								+ list[i].getAbsolutePath());
						System.out.println(ex2.getMessage());

						if (stopAtException)
						{
							throw ex2;
						} else
						{
							OutputToFile("Error Storing (XNAT - Validation) "
									+ list[i].getName() + " "
									+ ex2.getMessage(),
									"storeXMLexceptions.txt", true);
						}
					}
				} else
				{
					if (list[i].isDirectory() && recursive)
					{
						dirs.add(list[i]);
					}
				}
			}

			if (recursive)
			{
				Iterator iter = dirs.iterator();
				while (iter.hasNext())
				{
					File f = (File) iter.next();

					storeXMLFolderService(f, recursive, quarantine,
							allowItemOverwrite, stopAtException);
				}
			}
		}
	}
	public static void OutputToFile(String content, String filePath,
			boolean append)
	{
		File _outFile;
		FileOutputStream _outFileStream;
		PrintWriter _outPrintWriter;

		_outFile = new File(filePath);

		try
		{
			_outFileStream = new FileOutputStream(_outFile, append);
		} // end try
		catch (IOException except)
		{
			return;
		} // end catch
		// Instantiate and chain the PrintWriter
		_outPrintWriter = new PrintWriter(_outFileStream);

		_outPrintWriter.println(content);
		_outPrintWriter.flush();

		_outPrintWriter.close();

		try
		{
			_outFileStream.close();
		} catch (IOException except)
		{
			return;
		}
	}
	public static String GetContents(File f)
	{
		try
		{
			FileInputStream in = new FileInputStream(f);
			DataInputStream dis = new DataInputStream(in);
			StringBuffer sb = new StringBuffer();
			while (dis.available() != 0)
			{
				// Print file line to screen
				sb.append(dis.readLine()).append("\n");
			}

			dis.close();

			return sb.toString();
		} catch (Exception e)
		{
			return "";
		}
	}
	/*
	 * public static void main(String[] args) { StoreXMLWS storeXML = new
	 * StoreXMLWS(); storeXML.perform(args); }
	 */
}
