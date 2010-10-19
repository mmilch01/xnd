package org.nrg.xdat.webservices;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Aug 29, 2006
 *
 */

/**
 * @author timo
 * 
 */
public abstract class WSTool
{
	protected String userName = null;
	protected String password = null;
	protected String host = null;
	protected String proxy = "";
	protected String proxyPort = "";
	protected String ts = null;
	protected String tsPass = null;
	protected String userSessionID = null;

	protected boolean quiet = false;
	protected Hashtable arguments = null;
	private Service service = new Service();

	private File propFile = null;
	private Properties props = null;
	private String defaultUser = null;
	private String defaultHost = null;
	private String defaultPswd = null;
	protected HashMap helpText = null;
	private boolean usingDefaultHost = false;
	private boolean usingDefaultUser = false;

	protected boolean proxySet = false;
	protected boolean proxyPortSet = false;

	protected boolean externalSessionID = false;

	protected static final String PROP_FILE_NAME = ".xnatPass";
	protected static final String HOST_FLAG = "host";
	protected static final String USER_FLAG = "u";
	protected static final String PASSWORD_FLAG = "p";
	protected static final String UNZIP_FLAG = "z";
	protected static final String README_FLAG = "readme";
	protected static final String DECOMPRESS_FLAG = "di";
	protected static final String PROXY_FLAG = "proxy";
	protected static final String PROXY_PORT_FLAG = "proxyPort";
	protected static final String HELP_FLAG = "h";
	protected static final String TS_FLAG = "ts";
	protected static final String TS_PASS_FLAG = "tsPass";
	protected static final String QUIET_FLAG = "quiet";
	protected static final String DASH = "-";
	protected static final String USER_SESSION_FLAG = "user_session";

	protected static final char DEFAULT_CHAR = '+';
	protected static final int HELP_SPACES = 12;

	protected static final boolean DEBUG = false;

	public WSTool()
	{
		loadHelpText();
	}

	public WSTool(String[] args)
	{
		this();
		perform(args);
	}

	/**
	 * Perform the requested service call.
	 * 
	 * @param args
	 */
	public boolean perform(String[] args)
	{
		try
		{
			convertArguments(args);
			if (arguments.containsKey(HELP_FLAG))
			{
				displayHelp();
				return false;
			}

			try
			{
				manageLogin();
			} catch (MalformedURLException e)
			{
				error(12, "Unknown URL: " + host, e);
				return false;
			} catch (IOException e)
			{
				error(13, "Unable to connect to Web Service Server: " + host, e);
				return false;
			}

			return process();
		} catch (RuntimeException e)
		{
			error(40, "Unknown Exception. Contact technical support", e);
			return false;
		}
	}

	protected void loadCertificateProperties()
	{
		if (props != null)
		{
			if (props.getProperty("javax.net.ssl.trustStore") != null)
			{
				ts = props.getProperty("javax.net.ssl.trustStore");
			}

			if (props.getProperty("javax.net.ssl.trustStorePassword") != null)
			{
				tsPass = props.getProperty("javax.net.ssl.trustStorePassword");
			}
		}

		if (arguments.containsKey(TS_FLAG))
		{
			ts = (String) arguments.get(TS_FLAG);
		}
		if (arguments.containsKey(TS_PASS_FLAG))
		{
			tsPass = (String) arguments.get(TS_PASS_FLAG);
		}

		if (ts != null)
		{
			if (tsPass != null)
			{
				System.setProperty("javax.net.ssl.trustStore", ts);
				System.setProperty("javax.net.ssl.trustStorePassword", tsPass);
				if (!quiet)
					System.out.println("Loaded Certificate: " + ts);
			} else
			{
				error(13, "Missing tsPass for " + ts, new Exception(
						"Missing TrustStore Password"));
			}
		}
	}

	/**
	 * Set properties if a properties file is found
	 */
	protected void loadProperties()
	{
		String fileName = "." + File.separator + PROP_FILE_NAME;
		propFile = new File(fileName);
		if (!propFile.exists())
		{
			String home = addFinalChar(System.getProperty("user.home"),
					File.separator);
			propFile = new File(home + PROP_FILE_NAME);
		}
		if (!propFile.exists())
		{ // create file to store current password

		} else
		{ // read properties from file
			try
			{
				InputStream f = new FileInputStream(propFile);
				props = new Properties();
				props.load(f);
				f.close();
				setDefaults();
			} catch (Exception e)
			{
				logError(e);
			}
		}
	}

	/**
	 * Look for default user, host and password in the properites object and set
	 * appropriate fields if found
	 */
	protected void setDefaults()
	{
		Enumeration names = props.propertyNames();
		while (names.hasMoreElements())
		{
			String key = (String) names.nextElement();
			char firstCh = key.charAt(0);
			if (firstCh == DEFAULT_CHAR)
			{
				String[] hostUser = key.split("@");
				defaultUser = hostUser[0].substring(1);
				defaultHost = addFinalChar(hostUser[1], "/");
				defaultPswd = props.getProperty(key);
			}
		}
	}

	protected void error(int errNo, String msg, Throwable e)
	{
		System.out.println("ERROR CODE " + errNo + ": " + msg + ".");
		if (e == null)
		{
			displayHelp();
		} else
		{
			logError(e);
		}
		// System.exit(errNo);
	}

	/**
	 * Obtains a value for the host from all possible sources
	 */
	protected void findHost()
	{
		if (arguments.containsKey(HOST_FLAG))
		{
			Object o = arguments.get(HOST_FLAG);
			if (o instanceof ArrayList)
			{
				host = (String) ((ArrayList) o).get(((ArrayList) o).size() - 1);
				System.out.println("Using host: " + host);
			} else
			{
				host = (String) o;
			}
		} else
		{
			host = defaultHost;
			usingDefaultHost = true;
			if (!quiet)
				System.out.println("Connecting to " + host);
		}
		if (host == null)
		{
			System.out.println("Missing Host.");
			displayHelp();
			// System.exit(1);
		}
		if (!host.startsWith("http://") && !host.startsWith("https://")) // LGV'08
			// Added
			// https
			host = "http://" + host;
		host = addFinalChar(host, "/");
	}

	protected void findProxy()
	{
		if (arguments.containsKey(PROXY_FLAG))
		{
			proxy = (String) arguments.get(PROXY_FLAG);
			proxySet = true;
		}
	}

	protected void findProxyPort()
	{
		if (arguments.containsKey(PROXY_PORT_FLAG))
		{
			proxyPort = (String) arguments.get(PROXY_PORT_FLAG);
			proxyPortSet = true;
		}
	}

	protected String addFinalChar(String s, String terminal)
	{
		if (!s.endsWith(terminal))
		{
			s += terminal;
		}
		return s;
	}

	/**
	 * Obtains a value for the user from all possible sources
	 * 
	 * @throws IOException
	 */
	protected void findUser() throws IOException
	{
		if (arguments.containsKey(USER_FLAG))
		{
			Object o = arguments.get(USER_FLAG);
			if (o instanceof ArrayList)
			{
				error(8, "Multiple usernames defined", null);
			}
			userName = (String) o;
		} else
		{
			if (usingDefaultHost)
			{
				userName = defaultUser;
				usingDefaultUser = true;
			}
		}
		if (userName == null && userSessionID == null)
		{
			// System.out.println("USER SESSION NOT FOUND: " +
			// arguments.get(USER_SESSION_FLAG));
			System.out.println("Enter username for " + host + ":");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			userName = in.readLine();
			if (userName == null || userName.equals(""))
			{
				error(2, "Missing Username", null);
			}
		}
	}

	public boolean manageServiceSessionID()
	{
		if (arguments.containsKey(USER_SESSION_FLAG))
		{
			userSessionID = (String) arguments.get(USER_SESSION_FLAG);
			return true;
		}

		return false;
	}

	public void manageLogin() throws MalformedURLException, IOException
	{
		loadProperties(); // gets user, host and password from properties
		loadCertificateProperties();
		Object q = (String) arguments.get(QUIET_FLAG);
		if (q != null)
		{
			if (q instanceof ArrayList)
			{

			} else
			{
				if (((String) q).equalsIgnoreCase("true"))
				{
					quiet = true;
				}
			}
		}
		manageServiceSessionID();

		findHost();
		findUser();
		findPassword();
		findProxy();
		findProxyPort();
		if (proxySet)
		{
			Properties systemSettings = System.getProperties();
			systemSettings.put("http.proxyHost", proxy);
			if (proxyPortSet)
			{
				systemSettings.put("http.proxyPort", proxyPort);
			}
			System.setProperties(systemSettings);
		}

		// the following does not appear to be needed
		// this is needed. The getContent() method tests the connection to the
		// host. If it fails, it throws the MalformedURLException. TO
		URL url = new URL(host + "axis/XMLSearch.jws");
		url.getContent();
	}

	public void logError(String message)
	{
		File outFile = null;
		FileOutputStream outFileStream = null;
		PrintWriter outPrintWriter = null;
		outFile = new File("error.log");
		try
		{
			outFileStream = new FileOutputStream(outFile, true);
		} catch (IOException except)
		{
			System.out.println(except.getMessage());
		}
		// Instantiate and chain the PrintWriter
		outPrintWriter = new PrintWriter(outFileStream);
		outPrintWriter.print(Calendar.getInstance().getTime() + " " + userName
				+ "@" + host + ": ");
		outPrintWriter.println(message);
		outPrintWriter.flush();
		outPrintWriter.close();
		System.out.println("Error logged in " + outFile.getAbsolutePath());
		try
		{
			outFileStream.close();
		} catch (IOException except)
		{
			System.out.println(except.getMessage());
		}
	}

	public void logError(Throwable e)
	{
		File outFile = null;
		FileOutputStream outFileStream = null;
		PrintWriter outPrintWriter = null;

		outFile = new File("error.log");

		try
		{
			outFileStream = new FileOutputStream(outFile, true);
		} catch (IOException except)
		{
			System.out.println(except.getMessage());
		}
		// Instantiate and chain the PrintWriter
		outPrintWriter = new PrintWriter(outFileStream);
		outPrintWriter.print(Calendar.getInstance().getTime() + " " + userName
				+ "@" + host + ":");
		e.printStackTrace(outPrintWriter);
		outPrintWriter.flush();
		outPrintWriter.close();
		System.out.println("Error logged in " + outFile.getAbsolutePath());
		try
		{
			outFileStream.close();
		} catch (IOException except)
		{
			System.out.println(except.getMessage());
		}
	}

	public static void outputToFile(String content, String filePath,
			boolean append)
	{
		File outFile = null;
		FileOutputStream outFileStream = null;
		PrintWriter outPrintWriter = null;

		outFile = new File(filePath);

		try
		{
			outFileStream = new FileOutputStream(outFile, append);
		} // end try
		catch (IOException except)
		{
			System.out.println(except.getMessage());
		} // end catch
		// Instantiate and chain the PrintWriter
		outPrintWriter = new PrintWriter(outFileStream);
		outPrintWriter.println(content);
		outPrintWriter.flush();
		outPrintWriter.close();
		try
		{
			outFileStream.close();
		} catch (IOException except)
		{
			System.out.println(except.getMessage());
		}
	}

	public abstract void displayHelp();

	public abstract boolean process();

	protected void findPassword() throws IOException
	{
		String formattedHost = host;
		if (arguments.containsKey(PASSWORD_FLAG))
		{
			Object o = arguments.get(PASSWORD_FLAG);
			if (o instanceof ArrayList)
			{
				password = (String) ((ArrayList) o)
						.get(((ArrayList) o).size() - 1);
				System.out.println("Using host: " + host);
			} else
			{
				password = (String) o;
			}
		} else if (password == null)
		{ // search through properties entries
			if (props != null)
			{
				password = props.getProperty(userName + "@" + formattedHost);
				if (password == null)
				{
					password = props.getProperty("+" + userName + "@"
							+ formattedHost);
				}
				if (password == null)
				{
					password = props.getProperty("*@" + formattedHost);
				}
				if (password == null)
				{
					if (host.startsWith("http://")
							|| host.startsWith("https://"))
					{ // LGV'08 Added https
						formattedHost = formattedHost.substring(7);
						password = props.getProperty(userName + "@"
								+ formattedHost);
					}
				}
				if (password == null)
				{
					password = props.getProperty("+" + userName + "@"
							+ formattedHost);
				}
				if (password == null)
				{
					password = props.getProperty("*@" + formattedHost);
				}
				if (password == null)
				{
					formattedHost = formattedHost.replace(':', '.');
					password = props
							.getProperty(userName + "@" + formattedHost);
				}
				if (password == null)
				{
					password = props.getProperty("+" + userName + "@"
							+ formattedHost);
				}
				if (password == null)
				{
					password = props.getProperty(userName + "@*");
				}
			}
			if (password == null)
			{
				if (usingDefaultUser)
					password = defaultPswd;
			}
			if (password == null && userSessionID == null)
			{
				char[] inputPassword = readPassword(System.in, "Enter "
						+ userName + "'s Password\n");
				if (inputPassword == null)
				{
					error(3, "Missing Password", null);
				} else
				{
					password = String.valueOf(inputPassword);
				}
			}
		}
	}

	/**
	 * Parses the command line string into a hash of flag value pairs
	 * 
	 * @param args
	 * @return
	 */
	protected Hashtable convertArguments(String[] args)
	{
		arguments = new Hashtable();
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.startsWith(DASH))
			{
				arg = arg.substring(1);
				if ((i + 1) < args.length)
				{
					String value = args[i + 1];
					if (arguments.get(arg) == null)
					{
						if (value.startsWith(DASH))
						{
							arguments.put(arg, "true");
						} else
						{
							arguments.put(arg, value);
							i++;
						}
					} else
					{
						Object o = arguments.get(arg);
						ArrayList al = null;
						if (o instanceof ArrayList)
						{
							al = ((ArrayList) o);
						} else
						{
							al = new ArrayList();
							al.add(o);
						}
						if (value.startsWith(DASH))
						{
							al.add("true");
						} else
						{
							al.add(value);
							i++;
						}
						arguments.put(arg, al);
					}
				} else
				{
					if (arguments.get(arg) == null)
					{
						arguments.put(arg, "true");
					} else
					{
						Object o = arguments.get(arg);
						ArrayList al = null;
						if (o instanceof ArrayList)
						{
							al = ((ArrayList) o);
						} else
						{
							al = new ArrayList();
							al.add(o);
						}
						al.add("true");
						arguments.put(arg, al);
					}
				}
			} else
			{
				arguments.put(arg, arg);
			}
		}
		return arguments;
	}

	protected Service getService()
	{
		service.setMaintainSession(true);
		return service;
	}

	protected Call createCall(String session) throws ServiceException
	{
		// if(session!=null)((javax.xml.rpc.Stub)service)._setProperty("Cookie","JSESSIONID="
		// + session);

		Call call = (Call) getService().createCall();
		call.setMaintainSession(true);
		if (userName != null)
		{
			call.setUsername(this.userName);
		}
		if (password != null)
		{
			call.setPassword(this.password);
		}
		if (session != null)
			call.setProperty("Cookie", "JSESSIONID=" + session);
		return call;
	}

	/**
	 * @return Session id for use in subsequent requests.
	 * @throws ServiceException
	 * @throws MalformedURLException
	 * @throws RemoteException
	 */
	public String createServiceSession() throws ServiceException,
			MalformedURLException, RemoteException
	{
		if (userSessionID == null)
		{
			Call call = createCall(null);

			// REQUEST SESSION ID
			URL requestSessionURL = new URL(host
					+ "axis/CreateServiceSession.jws");
			call.setTargetEndpointAddress(requestSessionURL);
			call.setOperationName("execute");
			Object[] params = {};
			userSessionID = (String) call.invoke(params);

			// userSessionID=getJSessionID(call.getResponseMessage());
			return userSessionID;
		} else
		{
			externalSessionID = true;
			String service_session = userSessionID;
			userSessionID = refreshServiceSession(service_session);
			if (!service_session.equals(userSessionID))
			{
				// System.out.println(userSessionID)
			}
			return userSessionID;
		}
	}

	public String getJSessionID(org.apache.axis.Message message)
	{
		String[] header = null;
		message.getMimeHeaders().getHeader("set-cookie");
		if (header != null)
		{
			String value = header[0];
			int start = value.indexOf("=");
			int end = value.indexOf(";");
			if (start == -1)
			{
				return value;
			} else if (end == -1)
			{
				return value.substring(start + 1);
			} else
			{
				return value.substring(start + 1, end);
			}
		}

		return null;
	}

	/**
	 * @return Session id for use in subsequent requests.
	 * @throws ServiceException
	 * @throws MalformedURLException
	 * @throws RemoteException
	 */
	public String refreshServiceSession(String service_session)
			throws ServiceException, MalformedURLException, RemoteException
	{
		Call call = createCall(this.userSessionID);
		call.setMaintainSession(true);
		URL requestSessionURL = new URL(host + "axis/RefreshServiceSession.jws");
		call.setTargetEndpointAddress(requestSessionURL);
		call.setOperationName("execute");
		Object[] params = new Object[]{service_session};
		userSessionID = (String) call.invoke(params);
		// userSessionID=getJSessionID(call.getResponseMessage());
		return userSessionID;
	}

	/**
	 * @return Session id for use in subsequent requests.
	 * @throws ServiceException
	 * @throws MalformedURLException
	 * @throws RemoteException
	 */
	public void closeServiceSession(String service_session)
			throws ServiceException, MalformedURLException, RemoteException
	{
		if (service_session != null && !externalSessionID)
		{
			Call call = createCall(userSessionID);
			URL requestSessionURL = new URL(host
					+ "axis/CloseServiceSession.jws");
			call.setTargetEndpointAddress(requestSessionURL);
			call.setOperationName("execute");
			Object[] params = new Object[]{service_session};
			call.invoke(params);
		}
	}

	/**
	 * @param host
	 * @param service_session
	 * @param id
	 * @param dataType
	 * @param dir
	 * @param quiet
	 * @param outputStream
	 *            to write content to
	 * @throws FileNotFoundException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 *             Retrieves XML from specified host and writes it to passed
	 *             output stream.
	 */
	public void writeXMLtoOS(String host, String service_session, Object id,
			String dataType, String dir, boolean quiet, OutputStream out)
			throws FileNotFoundException, MalformedURLException, IOException,
			SAXException, ParserConfigurationException
	{

		if (!quiet)
			System.out.println("Requesting xml for " + id + "");
		long startTime = Calendar.getInstance().getTimeInMillis();
		String urlString = host + "app/template/XMLSearch.vm/session/"
				+ service_session + "/id/" + id + "/data_type/" + dataType;

		URLConnection url = new URL(urlString).openConnection();
		url.setRequestProperty("Cookie", "JSESSIONID=" + service_session);
		// Use Buffered Stream for reading/writing.
		InputStream bis = null;
		BufferedOutputStream bos = null;

		try
		{
			bis = new BufferedInputStream(url.getInputStream());
		} catch (FileNotFoundException e)
		{
			error(
					39,
					"File not found on server.  Please review the search parameters",
					e);
		}

		bos = new BufferedOutputStream(out);

		byte[] buff = new byte[2048];
		int bytesRead;

		while (-1 != (bytesRead = bis.read(buff, 0, buff.length)))
		{
			bos.write(buff, 0, bytesRead);

		}

		bos.flush();

		if (!quiet)
			System.out.println("Response Received ("
					+ (Calendar.getInstance().getTimeInMillis() - startTime)
					+ " ms)");

	}

	/**
	 * @param data
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public boolean isValidXMLFile(java.io.File data)
	{
		try
		{
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);

			// get a new instance of parser
			SAXParser sp = spf.newSAXParser();
			// parse the file and also register this class for call backs
			sp.parse(data, new DefaultHandler());
			return true;
		} catch (Exception e)
		{
			this.logError(e);
			return false;
		}

	}

	/**
	 * @param service_session
	 * @param field
	 * @param comparison
	 * @param value
	 * @param dataType
	 * @return Object [] of Identifiers
	 * @throws ServiceException
	 * @throws MalformedURLException
	 * @throws RemoteException
	 */
	public Object[] getIdentifiers(String service_session, String field,
			String comparison, String value, String dataType)
			throws ServiceException, MalformedURLException, RemoteException
	{
		URL url = new URL(host + "axis/GetIdentifiers.jws");
		Call call = createCall(userSessionID);
		call.setTargetEndpointAddress(url);

		call.setOperationName("search");

		Object[] params = new Object[]{service_session, field, comparison,
				value, dataType};

		if (!quiet)
			System.out.println("Requesting matching IDs...");
		long startTime = Calendar.getInstance().getTimeInMillis();
		Object[] o = (Object[]) call.invoke(params);
		long duration = Calendar.getInstance().getTimeInMillis() - startTime;
		if (!quiet)
			System.out.println("Response Received (" + duration + " ms)");
		return o;
	}

	public final char[] readPassword(InputStream in, String prompt)
			throws IOException
	{
		MaskingThread maskingthread = new MaskingThread(prompt);
		Thread thread = new Thread(maskingthread);
		thread.start();

		char[] lineBuffer;
		char[] buf;

		buf = lineBuffer = new char[128];

		int room = buf.length;
		int offset = 0;
		int c;

		loop : while (true)
		{
			switch (c = in.read())
			{
				case -1 :
				case '\n' :
					break loop;

				case '\r' :
					int c2 = in.read();
					if ((c2 != '\n') && (c2 != -1))
					{
						if (!(in instanceof PushbackInputStream))
						{
							in = new PushbackInputStream(in);
						}
						((PushbackInputStream) in).unread(c2);
					} else
					{
						break loop;
					}

				default :
					if (--room < 0)
					{
						buf = new char[offset + 128];
						room = buf.length - offset - 1;
						System.arraycopy(lineBuffer, 0, buf, 0, offset);
						Arrays.fill(lineBuffer, ' ');
						lineBuffer = buf;
					}
					buf[offset++] = (char) c;
					break;
			}
		}
		maskingthread.stopMasking();
		if (offset == 0)
		{
			return null;
		}
		char[] ret = new char[offset];
		System.arraycopy(buf, 0, ret, 0, offset);
		Arrays.fill(buf, ' ');
		return ret;
	}

	/**
	 * Prints a line of help text for use in displayHelp.
	 * 
	 * @param flag
	 *            - what follows the "-"
	 */
	public void printHelpLine(String flag)
	{
		int spaces = HELP_SPACES - flag.length();
		String spacing = "";
		for (int i = 0; i < spaces; i++)
		{
			spacing += " ";
		}
		System.out.println(DASH + flag + spacing + helpText.get(flag));
	}

	/**
	 * Enters the help text assoicated with each command line option
	 */
	protected void loadHelpText()
	{
		helpText = new HashMap();
		helpText.put(USER_FLAG, "USERNAME");
		helpText.put(PASSWORD_FLAG, "PASSWORD");
		helpText
				.put(USER_SESSION_FLAG,
						"User Session ID: replaces username/password, available from CreateUserSession");
		helpText.put(HOST_FLAG,
				"URL to XNAT based website.  (i.e. http://localhost/xnat).");
		helpText.put(UNZIP_FLAG,
				"Unzip directory VALUES(true,false) (defaults to false).");
		helpText.put(README_FLAG,
				"Whether or not to download the readme file for this session.");
		helpText
				.put(DECOMPRESS_FLAG,
						"Decompress images. (By default images within the archive are compressed).");
		helpText.put(TS_FLAG, "Trust Store (for trusted certificates).");
		helpText.put(TS_PASS_FLAG, "Trust Store Password.");
		helpText.put(PROXY_FLAG, "Proxy server.");
		helpText.put(PROXY_PORT_FLAG, "Proxy server port. (defaults to 80).");
		helpText.put(HELP_FLAG, "Print help.");
		helpText.put(QUIET_FLAG, "Suppress messages.");
	}

	protected void displayCommonHelp()
	{
		System.out.println("");
		System.out.println("Parameters:");
		printHelpLine(USER_FLAG);
		printHelpLine(PASSWORD_FLAG);
		printHelpLine(USER_SESSION_FLAG);
		printHelpLine(HOST_FLAG);
		printHelpLine(TS_FLAG);
		printHelpLine(TS_PASS_FLAG);
		printHelpLine(PROXY_FLAG);
		printHelpLine(PROXY_PORT_FLAG);
		printHelpLine(HELP_FLAG);
		printHelpLine(QUIET_FLAG);
	}

	public void debug(String msg)
	{
		if (DEBUG)
			System.out.println("DEBUG\t" + msg);
	}

	public void debug(String method, String msg)
	{
		debug(method + ":: " + msg);
	}

	public class MaskingThread extends Thread
	{
		private volatile boolean stop;

		private char echochar = '*';

		/**
		 *@param prompt
		 *            The prompt displayed to the user
		 */
		public MaskingThread(String prompt)
		{
			System.out.print(prompt);
		}

		/**
		 * Begin masking until asked to stop.
		 */
		public void run()
		{

			int priority = Thread.currentThread().getPriority();
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

			try
			{
				stop = true;
				while (stop)
				{
					System.out.print("\010" + echochar);
					try
					{
						// attempt masking at this rate
						Thread.sleep(1);
					} catch (InterruptedException iex)
					{
						Thread.currentThread().interrupt();
						return;
					}
				}
			} finally
			{ // restore the original priority
				Thread.currentThread().setPriority(priority);
			}
		}

		/**
		 * Instruct the thread to stop masking.
		 */
		public void stopMasking()
		{
			this.stop = false;
		}

	}

	/**
	 * @param f
	 * @return ArrayList of Strings (one string for each line in the file)
	 */
	public static ArrayList FileLinesToArrayList(File f)
			throws FileNotFoundException, IOException
	{
		ArrayList al = new ArrayList();

		FileInputStream in = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(in);
		StringBuffer sb = new StringBuffer();
		while (dis.available() != 0)
		{
			String s = dis.readLine();
			if (s.indexOf(',') == -1)
			{
				al.add(s.trim());
			} else
			{
				al.addAll(DelimitedStringToArrayList(s, ","));
			}
		}

		dis.close();
		return al;
	}

	public static ArrayList DelimitedStringToArrayList(String s,
			String delimiter)
	{
		ArrayList al = new ArrayList();

		while (s.indexOf(delimiter) != -1)
		{
			al.add(s.substring(0, s.indexOf(delimiter)).trim());
			s = s.substring(s.indexOf(delimiter) + 1);
		}

		if (s.length() > 0)
		{
			al.add(s.trim());
		}

		return al;
	}

}
