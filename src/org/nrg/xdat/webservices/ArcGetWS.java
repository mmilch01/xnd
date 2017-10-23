package org.nrg.xdat.webservices;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;

//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Aug 18, 2006
 *
 */

/**
 * @author timo
 * 
 */
public class ArcGetWS extends WSTool
{
	protected static final String SESSION_FLAG = "s";
	protected static final String FILE_FLAG = "f";
	protected static final String OUTPUT_FLAG = "o";
	protected static final String RAW_FLAG = "r";
	protected static final String TYPE_FLAG = "t";
	protected static final String PROCESSED_FLAG = "proc";
	protected static final String QUALITY_FLAG = "quality";

	public ArcGetWS()
	{
		super();
	}

	@Override
	public boolean process()
	{
		// OUTPUT DIRECTORY
		String dir = null;
		Object o = arguments.get(OUTPUT_FLAG);
		boolean bRes = false;
		if (o == null)
		{
			dir = "." + File.separator;
		} else
		{
			if (o instanceof ArrayList)
			{
				dir = (String) ((ArrayList) o).get(0);
				System.out.println("Exporting to " + dir
						+ ".\n Ignoring other -" + OUTPUT_FLAG + " tags.");
			} else
			{
				dir = (String) o;
			}
			if (!dir.endsWith(File.separator))
			{
				dir += File.separator;
			}
		}

		String zipped = (String) arguments.get(UNZIP_FLAG);
		boolean unzip = false;
		if (zipped != null)
		{
			if (zipped.equalsIgnoreCase("true"))
			{
				unzip = true;
			}
		}

		String decompressS = (String) arguments.get(DECOMPRESS_FLAG);
		boolean decompress = false;
		if (decompressS != null)
		{
			if (decompressS.equalsIgnoreCase("true"))
			{
				decompress = true;
			}
		}

		// IDENTIFY SESSIONS
		ArrayList sessions = null;
		o = null;
		o = arguments.get(SESSION_FLAG);

		if (o == null && arguments.get(FILE_FLAG) == null)
		{
			System.out.println("Missing parameter: -" + SESSION_FLAG
					+ " Session ID");
			displayHelp();
			return false;
		}
		if (o == null)
		{
			o = arguments.get(FILE_FLAG);
			if (o instanceof ArrayList)
			{
				ArrayList files = (ArrayList) o;
				sessions = new ArrayList();
				for (int i = 0; i < files.size(); i++)
				{
					o = files.get(i);
					File sessionFile = new File((String) o);
					if (sessionFile.exists())
					{
						try
						{
							sessions.addAll(FileLinesToArrayList(sessionFile));
						} catch (FileNotFoundException e)
						{
							System.out.println("File Not Found: " + o);
							return false;
						} catch (IOException e)
						{
							System.out.println("Unable to load file: " + o);
							return false;
						}
						if (sessions.size() == 0)
						{
							System.out
									.println("Unable to load session ids from file: "
											+ o);
							return false;
						}
					} else
					{
						System.out.println("Unable to access file: " + o);
						return false;
					}
				}
			} else
			{
				sessions = new ArrayList();
				File sessionFile = new File((String) o);
				if (sessionFile.exists())
				{
					try
					{
						sessions = FileLinesToArrayList(sessionFile);
					} catch (FileNotFoundException e)
					{
						System.out.println("File Not Found: " + o);
						return false;
					} catch (IOException e)
					{
						System.out.println("Unable to load file: " + o);
						return false;
					}
					if (sessions.size() == 0)
					{
						System.out
								.println("Unable to load session ids from file: "
										+ o);
						return false;
					}
				} else
				{
					System.out.println("Unable to access file: " + o);
					return false;
				}
			}
		} else
		{
			if (o instanceof ArrayList)
			{
				sessions = (ArrayList) o;
			} else
			{
				sessions = new ArrayList();
				sessions.add(o);
			}
		}
		try
		{
			String service_session = createServiceSession();
			for (int i = 0; i < sessions.size(); i++)
			{
				String session_id = (String) sessions.get(i);
				try
				{
					// REQUEST SESSION ID
					// call.setProperty(Call.CHARACTER_SET_ENCODING,"UTF-8");
					if (!quiet)
						System.out.println("Validating MR Session ID: "
								+ session_id);
					Call call = createCall(service_session);
					URL url = new URL(host + "axis/VelocitySearch.jws");
					call.setTargetEndpointAddress(url);
					call.setOperationName("search");
					Object[] params = new Object[]{service_session,
							"xnat:mrSessionData.ID", "=", session_id,
							"xnat:mrSessionData", "xnat_mrSessionData_brief.vm"};

					if (!quiet)
						System.out.println("Sending Request...");
					long startTime = System.currentTimeMillis();
					String s = (String) call.invoke(params);
					long duration = System.currentTimeMillis() - startTime;
					if (!quiet)
						System.out.println("Response Received (" + duration
								+ " ms)\n\n");
					if (!quiet)
						System.out.println(s);

					if (arguments.containsKey(README_FLAG)
							|| arguments.containsKey(README_FLAG.toUpperCase()))
					{
						int counter = 0;
						File outFile = new File(dir + session_id
								+ "_README.txt");
						while (outFile.exists())
						{
							outFile = new File(dir + session_id + "_"
									+ counter++ + "_README.txt");
						}
						FileWriter fw = new FileWriter(outFile);
						fw.write(s);
						fw.close();
					}

					boolean found = false;
					if (s.indexOf(session_id) != -1)
					{
						found = true;
					}
					if (found)
					{
						ArrayList r = null;
						if (arguments.get(RAW_FLAG) != null)
						{
							Object raw = arguments.get(RAW_FLAG);
							if (raw instanceof ArrayList)
							{
								r = (ArrayList) raw;
							} else
							{
								if (raw.equals("true"))
								{
									r = new ArrayList();
									r.add("ALL");
								} else
								{
									r = new ArrayList();
									r.add(raw);
								}
							}
						}

						ArrayList tScans = null;
						if (arguments.get(PROCESSED_FLAG) != null)
						{
							tScans = new ArrayList();
							tScans.add("ALL");
						} else
						{
							Object t = arguments.get(TYPE_FLAG);
							if (t != null)
							{
								if (t instanceof ArrayList)
								{
									tScans = (ArrayList) t;
								} else
								{
									if (t.equals("true"))
									{
										tScans = new ArrayList();
										tScans.add("ALL");
									} else
									{
										tScans = new ArrayList();
										tScans.add(t);
									}
								}
							}
						}

						ArrayList qualities = null;
						if (arguments.get(QUALITY_FLAG) == null)
						{
							qualities = new ArrayList();
							qualities.add("ALL");
						} else
						{
							Object q = arguments.get(QUALITY_FLAG);
							if (q != null)
							{
								if (q instanceof ArrayList)
								{
									qualities = (ArrayList) q;
								} else
								{
									if (q.equals("true"))
									{
										qualities = new ArrayList();
										qualities.add("ALL");
									} else
									{
										qualities = new ArrayList();
										qualities.add(q);
									}
								}
							}
						}

						bRes = execute(host, service_session, session_id, r,
								tScans, dir, unzip, quiet, decompress,
								qualities);
					} else
					{
						error(90, "INVALID SESSION ID '" + session_id + "'.",
								null);
						return false;
					}
				} catch (AxisFault ex2)
				{
					String fault = ex2.getFaultString();
					if (fault == null)
					{
						error(33, "Web Service Exception: " + host + "\n"
								+ ex2.getMessage(), ex2);
						bRes = false;
					} else if (fault.indexOf("PasswordAuthenticationException") != -1)
					{
						error(99, "Invalid Password.", ex2);
						bRes = false;
					} else if (fault.indexOf("FailedLoginException") != -1)
					{
						error(98,
								"Failed Login. Review username and password.",
								ex2);
						bRes = false;
					} else if (fault.indexOf("UserNotFoundException") != -1)
					{
						error(97,
								"Failed Login. Review username and password.",
								ex2);
						bRes = false;
					} else if (fault.indexOf("EnabledException") != -1)
					{
						error(96, "Failed Login. Account disabled.", ex2);
						bRes = false;
					} else
					{
						error(32, "Web Service Exception @ " + host + "\n"
								+ fault, ex2);
						bRes = false;
					}
				} catch (RemoteException ex)
				{
					error(33, "Web Service Exception: " + host + "\n"
							+ ex.getMessage(), ex);
					bRes = false;
				} catch (MalformedURLException e)
				{
					error(12, "Web Service Exception: " + host + "\n"
							+ e.getMessage(), e);
					bRes = false;
				} catch (IOException e)
				{
					error(13, "Web Service Exception: " + host + "\n"
							+ e.getMessage(), e);
					bRes = false;
				}
			}
			closeServiceSession(service_session);
		} catch (MalformedURLException e)
		{
			error(12, "Web Service Exception: " + host + "\n" + e.getMessage(),
					e);
			bRes = false;
		} catch (AxisFault ex2)
		{
			String fault = ex2.getFaultString();
			if (fault == null)
			{
				error(33, "Web Service Exception: " + host + "\n"
						+ ex2.getMessage(), ex2);
				bRes = false;
			} else if (fault.indexOf("PasswordAuthenticationException") != -1)
			{
				error(99, "Invalid Password.", ex2);
				bRes = false;
			} else if (fault.indexOf("FailedLoginException") != -1)
			{
				error(98, "Failed Login. Review username and password.", ex2);
				bRes = false;
			} else if (fault.indexOf("UserNotFoundException") != -1)
			{
				error(97, "Failed Login. Review username and password.", ex2);
				bRes = false;
			} else if (fault.indexOf("EnabledException") != -1)
			{
				error(96, "Failed Login. Account disabled.", ex2);
				bRes = false;
			} else
			{
				error(32, "Web Service Exception @ " + host + "\n" + fault, ex2);
				bRes = false;
			}
		} catch (RemoteException ex)
		{
			error(33,
					"Web Service Exception: " + host + "\n" + ex.getMessage(),
					ex);
			bRes = false;
		} catch (ServiceException ex)
		{
			error(11,
					"Web Service Exception: " + host + "\n" + ex.getMessage(),
					ex);
			bRes = false;
		}
		return bRes;
	}

	/**
	 * @param host
	 *            Host of the server i.e. 'http://localhost:8080/xnat'
	 * @param service_session
	 *            create from 'createServiceSession()'
	 * @param session_id
	 *            MR Session Identifier
	 * @param raw
	 *            Strings representing the names of the Scan Types for raw
	 *            images to aquire ('ALL' to receive all).
	 * @param tScans
	 *            Strings representing the names of the Scan Types for processed
	 *            images to aquire ('ALL' to receive all).
	 * @param dir
	 *            String directory to place files.
	 * @param unzip
	 *            whether or not to unzip the downloaded file.
	 * @param quiet
	 *            limits system output.
	 * @throws FileNotFoundException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static boolean execute(String host, String service_session,
			String session_id, ArrayList raw, ArrayList tScans, String dir,
			boolean unzip, boolean quiet, boolean decompress, ArrayList quality)
			throws FileNotFoundException, MalformedURLException, IOException
	{
		if (!quiet)
			System.out.println("Requesting archived data for " + session_id
					+ "");

		int counter = 0;

		File outFile = new File(dir + session_id + ".zip");
		if (!outFile.getParentFile().exists())
		{
			outFile.getParentFile().mkdirs();
		}

		while (outFile.exists())
		{
			outFile = new File(dir + session_id + "_" + counter++ + ".zip");
		}

		long startTime = System.currentTimeMillis();
		String urlString = host + "app/template/ArcGet.vm/session/"
				+ service_session + "/id/" + session_id;
		if (raw != null)
		{
			if (raw.indexOf("ALL") != -1)
			{
				urlString += "/raw/ALL";
			} else
			{
				urlString += "/raw/";
				Iterator iter = raw.iterator();
				int counter2 = 0;
				while (iter.hasNext())
				{
					String rawS = (String) iter.next();
					if (counter2++ == 0)
					{
						urlString += rawS;
					} else
					{
						urlString += "," + rawS;
					}
				}
			}
		}

		if (tScans != null)
		{
			if (tScans.indexOf("ALL") != -1)
			{
				urlString += "/proc/ALL";
			} else
			{
				urlString += "/proc/";
				Iterator iter = tScans.iterator();
				int counter2 = 0;
				while (iter.hasNext())
				{
					String rawS = (String) iter.next();
					if (counter2++ == 0)
					{
						urlString += rawS;
					} else
					{
						urlString += "," + rawS;
					}
				}
			}
		}

		if (decompress)
		{
			urlString += "/unzip/true";
		}

		if (quality != null)
		{
			if (quality.indexOf("ALL") != -1)
			{
				urlString += "/quality/ALL";
			} else
			{
				urlString += "/quality/";
				Iterator iter = quality.iterator();
				int counter2 = 0;
				while (iter.hasNext())
				{
					String qualityS = (String) iter.next();
					if (counter2++ == 0)
					{
						urlString += qualityS;
					} else
					{
						urlString += "," + qualityS;
					}
				}
			}
		}

		URLConnection url = new URL(urlString).openConnection();
		url.setRequestProperty("Cookie", "JSESSIONID=" + service_session);
		// Use Buffered Stream for reading/writing.
		InputStream bis = null;
		BufferedOutputStream bos = null;

		FileOutputStream out = new FileOutputStream(outFile);

		bis = url.getInputStream();
		bos = new BufferedOutputStream(out);

		byte[] buff = new byte[256];
		int bytesRead;
		int loaded = 0;
		if (!quiet)
			System.out.print("0 KB");

		java.text.NumberFormat nf = NumberFormat.getInstance();
		while (-1 != (bytesRead = bis.read(buff, 0, buff.length)))
		{
			bos.write(buff, 0, bytesRead);
			bos.flush();
			loaded = loaded + bytesRead;
			if (!quiet)
				System.out.print('\r');
			if (!quiet)
				System.out.print(nf.format((loaded / 1024)) + " KB");
			if (!quiet)
				System.out.flush();
		}
		bis.close();
		bos.flush();
		bos.close();
		out.close();
		if (!quiet)
			System.out.print('\r');
		if (!quiet)
			System.out.println(nf.format((loaded / 1024)) + " KB Downloaded.");
		if (!quiet)
			System.out.flush();

		boolean bRes = true;
		// is the following necessasry? GH
		// yes, it validates that the returned file is a valid zip file (not
		// corrupt). TO
		try
		{
			ZipFile file = new ZipFile(outFile);
			file.close(); // added this to turn off warning GH
		} catch (ZipException e1)
		{
			System.out
					.println("Download Failed.\nZipped File appears to be incomplete.\nContact "
							+ host + " Administrator for more information.");
			return false;
		}

		if (unzip)
		{
			long serviceDuration = System.currentTimeMillis() - startTime;
			if (!quiet)
				System.out.println("Zipped Archive Received ("
						+ serviceDuration + " ms)");

			if (!quiet)
				System.out.println("Unzipping Archive\n");
			Unzip(outFile, quiet);
			outFile.delete();
			serviceDuration = System.currentTimeMillis() - startTime;
			System.out.println("Archive Loaded (" + serviceDuration + " ms): "
					+ dir + session_id);
			File readme = new File(dir + "README.txt");
			if ((!quiet) && readme.exists())
			{
				FileInputStream in = new FileInputStream(readme);
				DataInputStream dis = new DataInputStream(in);
				while (dis.available() != 0)
				{
					String line = dis.readLine();
					if (line == null)
					{
						break;
					} else
					{
						System.out.println(line);
					}
				}

				dis.close();
				if (!quiet)
					System.out.println("Summary available in README.txt.");
			}
		} else
		{
			long serviceDuration = System.currentTimeMillis() - startTime;
			System.out.println("Zipped Archive Received (" + serviceDuration
					+ " ms) : " + outFile.getCanonicalPath() + "\n");
			if (!quiet)
			{
				try
				{
					ZipFile file = new ZipFile(outFile);
					ZipEntry entry = file.getEntry("README.txt");

					if (entry == null)
					{
						System.out.println("ERROR: Failed to load README.txt");
					} else
					{
						DataInputStream dis = new DataInputStream(file
								.getInputStream(entry));
						while (dis.available() != 0)
						{
							String line = dis.readLine();
							if (line == null)
							{
								break;
							} else
							{
								System.out.println(line);
							}
						}

						dis.close();
					}
				} catch (RuntimeException e2)
				{
					System.out.println("ERROR: Failed to load README.txt");
					bRes = false;
				}
			}
		}
		return bRes;
	}

	@Override
	public void loadHelpText()
	{
		super.loadHelpText();
		helpText
				.put(
						SESSION_FLAG,
						"Session id of the desired session(s).  For multiple sessions, use multiple -s tags.");
		helpText.put(FILE_FLAG, "file containing session ids to search for.");
		helpText.put(OUTPUT_FLAG, "Output directory.");
		helpText
				.put(
						RAW_FLAG,
						"Retrieves raw image data of the specified type or id. (use 'ALL' to get all raw data).");
		helpText
				.put(
						TYPE_FLAG,
						"Retrieves processed images of the specified type or id. (use 'ALL' to get all raw data).");
		helpText.put(PROCESSED_FLAG,
				"Retrieves processed image data for the specified sessions.");
		helpText
				.put(
						QUALITY_FLAG,
						"Qualities of scans to include (usable,questionable, unusable) defaults to all.");
	}

	@Override
	public void displayHelp()
	{
		System.out.println("\nArc-Get Web Service\n");
		displayCommonHelp();
		printHelpLine(SESSION_FLAG);
		printHelpLine(FILE_FLAG);
		printHelpLine(OUTPUT_FLAG);
		printHelpLine(RAW_FLAG);
		printHelpLine(TYPE_FLAG);
		printHelpLine(PROCESSED_FLAG);
		printHelpLine(UNZIP_FLAG);
		printHelpLine(README_FLAG);
		printHelpLine(DECOMPRESS_FLAG);
		printHelpLine(QUALITY_FLAG);
	}
	/*
	 * public static void main(String[] args) { ArcGetWS arcGet = new
	 * ArcGetWS(); arcGet.perform(args); }
	 */

	public static void Unzip(File f, boolean quiet) throws IOException
	{
		int BUFFER = 2048;
		InputStream in = new FileInputStream(f);
		String s = f.getAbsolutePath();
		int index = s.lastIndexOf('/');
		if (index == -1)
		{
			index = s.lastIndexOf('\\');
		}
		if (index == -1)
		{
			throw new IOException("Unknown Zip File.");
		}
		BufferedOutputStream dest = null;
		String dir = s.substring(0, index + 1);
		ZipInputStream zis = new ZipInputStream(in);
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null)
		{
			int count;
			byte data[] = new byte[BUFFER];
			// write the files to the disk
			File newF = (new File(dir + entry.getName()));
			newF.getParentFile().mkdirs();
			if (entry.getName().endsWith(".gz"))
			{
				newF = new File(dir
						+ entry.getName().substring(0,
								entry.getName().lastIndexOf(".gz")));
				OutputStream fos = new FileOutputStream(newF);
				fos = new GZIPOutputStream(fos);
				dest = new BufferedOutputStream(fos, BUFFER);

				if (!quiet)
					System.out.print('\r');
				if (!quiet)
					System.out.print("Extracting: " + newF.getName());
				if (!quiet)
					System.out.flush();
			} else
			{
				OutputStream fos = new FileOutputStream(newF);
				dest = new BufferedOutputStream(fos, BUFFER);
				if (!quiet)
					System.out.print('\r');
				if (!quiet)
					System.out.print("Extracting: " + newF.getName());
				if (!quiet)
					System.out.flush();
			}
			while ((count = zis.read(data, 0, BUFFER)) != -1)
			{
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();
		}
		zis.close();
		if (!quiet)
			System.out.print('\r');
		if (!quiet)
			System.out.flush();
		if (!quiet)
			System.out.println("File Extraction Complete.\n");
	}
}
