package org.nrg.xnd.tools.ImageViewer;

public final class Util
{
	public static int m_Lang = 0;
	public static final int MESSAGE = 1, WARNING = 2, ERROR = 3;
	public static void LogMessage(String msg)
	{
		LogGeneralError(msg, MESSAGE, null);
	}

	public static void LogWarning(String msg, Throwable e)
	{
		LogGeneralError(msg, WARNING, e);
	}

	public static void LogError(String msg, Throwable e)
	{
		LogGeneralError(msg, ERROR, e);
	}

	public static void LogGeneralError(String msg, int type, Throwable e)
	{
		System.err.print("*************** ");
		switch (type)
		{
			case 1 :
				System.err.print("Message: ");
				break;
			case 2 :
				System.err.print("Warning: ");
				break;
			case 3 :
				System.err.print("Error: **");
		}
		System.err.println("******************");
		System.err.println(msg);
		if (e != null)
		{
			e.printStackTrace();
		}
	}
	public static String UTF(String slocal)
	{
		return slocal;
		/*
		 * byte[] rs=new byte[slocal.length()]; int code; for (int i=0;
		 * i<slocal.length(); i++) { code=slocal.charAt(i);
		 * rs[i]=(code>0xff)?(byte)(code-'\u0410'+0xC0):(byte)code; } // try{
		 * return new String(rs,"Cp1251");} catch(Exception e){return null;}
		 * try{ return new String(rs);} catch(Exception e){return null;}
		 */
	}

	public static String Trans(String s)
	{
		if (s.length() < 1)
		{
			return "";
		}
		if (m_Lang == 0)
		{
			if (s.charAt(0) != '$')
			{
				return s;
			} else
			{
				switch (s.charAt(1))
				{
					case 'a' :
						if (s.compareTo("$allow") == 0)
						{
							return "This will allow everyone to view this record. Continue?";
						}
						if (s.compareTo("$arch_verify") == 0)
						{
							return "Verifying archive availability, please wait";
						}
						break;
					case 'c' :
						if (s.compareTo("$cant_connect") == 0)
						{
							return "Cannot connect to server.\nPlease make sure that your local firewall\nor proxy is not blocking our site.";
						}
						if (s.compareTo("$conn_fail") == 0)
						{
							return "Connection to server failed";
						}
						break;
					case 'd' :
						if (s.compareTo("$download_next_series") == 0)
						{
							return "Are you sure you want to download the next series in this study?";
						}
						if (s.compareTo("$delete") == 0)
						{
							return "Are you sure you want to permanently delete this record and all related images?";
						}
						if (s.compareTo("$date_sel") == 0)
						{
							return "Please select a valid date";
						}
						break;
					case 'f' :
						if (s.compareTo("$from_cache") == 0)
						{
							return "Images extracted from cache";
						}
						break;
					case 'h' :
						if (s.compareTo("$hdr_fail") == 0)
						{
							return "Sending request header failed";
						}
						break;
					case 'i' :
						if (s.compareTo("$inval_resp") == 0)
						{
							return "Invalid response from server";
						}
						if (s.compareTo("$im_not_rcvd") == 0)
						{
							return "Could not receive image";
						}
						break;
					case 'n' :
						if (s.compareTo("$note_exceed") == 0)
						{
							return "Your note exceeds 255 characters";
						}
						if (s.compareTo("$not_found") == 0)
						{
							return "No matching records found";
						}
						if (s.compareTo("$no_memory") == 0)
						{
							return "There is not enough memory available to for Java. The current study will be loaded in preview mode. You can reload separate series in full mode later. To fit all series/images into the memory: 1) increase Java VM memory; 2) try to select a higher compression option from the worklist.";
						}
						if (s.compareTo("$no_memory_to_continue_operation") == 0)
						{
							return "Current operation cannot be completed due to lack of Java memory. Some images will be unloaded";
						}
						break;
					case 'o' :
						if (s.compareTo("$orient_modify") == 0)
						{
							return "Are you sure you want to modify the orientation of all images in current series?";
						}
						break;
					case 'p' :
						if (s.compareTo("$prevent") == 0)
						{
							return "This will prevent others from viewing this record.Continue?";
						}
						break;
					case 'q' :
						if (s.compareTo("$query_incorrect") == 0)
						{
							return "Incorrect query, closing connection";
						}
						break;
					case 'r' :
						if (s.compareTo("$req_fail") == 0)
						{
							return "Sending request failed";
						}
						if (s.compareTo("$rec_fail") == 0)
						{
							return "Receiving records failed";
						}
						if (s.compareTo("$reload_full") == 0)
						{
							return "Load this series fully?";
						}
						break;
					case 's' :
						if (s.compareTo("$send_fail") == 0)
						{
							return "Could not send data";
						}
						if (s.compareTo("$sock_err") == 0)
						{
							return "could not read from socket";
						}
						break;
					case 'u' :
						if (s.compareTo("$upd_rec") == 0)
						{
							return "Number of updated records";
						}
						break;
					case 'w' :
						if (s.compareTo("$web_url") == 0)
						{
							// ?? return "Web URL (Click to select, Shift+Click
							// to email, Ctrl+Click to open in a new window)";
							return "Web URL (Click to select, Shift+Click to email)";
						}
						break;
					case 'y' :
						if (s.compareTo("$your_note") == 0)
						{
							return "Your note (255 characters max)";
						}
				}
				return s;
			}
		}
		char ch;
		ch = s.charAt(0);
		if (ch == '$')
		{
			ch = s.charAt(1);
		}
		switch (ch)
		{
			case 'a' :
				if (s.compareTo("and") == 0)
				{
					return UTF("\u0438");
				}
				if (s.compareTo("$allow") == 0)
				{
					return UTF("\u041E\u0442\u043A\u0440\u044B\u0442\u044C \u0434\u043E\u0441\u0442\u0443\u043F \u043A \u044D\u0442\u043E\u0439 \u0437\u0430\u043F\u0438\u0441\u0438 \u0432\u0441\u0435\u043C \u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u0442\u0435\u043B\u044F\u043C?");
				}
				if (s.compareTo("$arch_verify") == 0)
				{
					return UTF("\u041F\u0440\u043E\u0432\u0435\u0440\u043A\u0430 \u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E\u0441\u0442\u0438 \u0430\u0440\u0445\u0438\u0432\u0430, \u043F\u043E\u0434\u043E\u0436\u0434\u0438\u0442\u0435");
				}
				break;
			case 'A' :
				if (s.compareTo("Auto") == 0)
				{
					return UTF("\u0410\u0432\u0442\u043E");
				}
				if (s.compareTo("Area") == 0)
				{
					return UTF("\u043F\u043B\u043E\u0449\u0430\u0434\u044C");
				}
				if (s.compareTo("Average") == 0)
				{
					return UTF("\u0441\u0440\u0435\u0434\u043D\u0435\u0435");
				}
				if (s.compareTo("Access #") == 0)
				{
					return UTF("\u041D\u043E\u043C. \u0434\u043E\u0441\u0442\u0443\u043F\u0430");
				}
				if (s.compareTo("Any") == 0)
				{
					return UTF("\u041B\u044E\u0431\u0430\u044F");
				}
				if (s.compareTo("After") == 0)
				{
					return UTF("\u041F\u043E\u0441\u043B\u0435");
				}
				if (s.compareTo("Append note") == 0)
				{
					return UTF("\u0414\u043E\u0431\u0430\u0432\u0438\u0442\u044C");
				}
				if (s.compareTo("Archive location") == 0)
				{
					return UTF("\u0410\u0440\u0445\u0438\u0432");
				}
				if (s.compareTo("Archive change successful") == 0)
				{
					return UTF("\u0421\u043E\u0435\u0434\u0438\u043D\u0435\u043D\u0438\u0435 \u0441 \u0430\u0440\u0445\u0438\u0432\u043E\u043C \u0443\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D\u043E");
				}
				break;
			case 'B' :
				if (s.compareTo("Body part") == 0)
				{
					return UTF("\u0427\u0430\u0441\u0442\u044C \u0442\u0435\u043B\u0430");
				}
				if (s.compareTo("Back to") == 0)
				{
					return UTF("\u041D\u0430\u0437\u0430\u0434 \u043D\u0430 \u0443\u0440\u043E\u0432\u0435\u043D\u044C");
				}
				if (s.compareTo("Before") == 0)
				{
					return UTF("\u0414\u043E");
				}
				if (s.compareTo("Between") == 0)
				{
					return UTF("\u041C\u0435\u0436\u0434\u0443");
				}
				break;
			case 'C' :
				if (s.compareTo("Confirm") == 0)
				{
					return UTF("\u041F\u043E\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043D\u0438\u0435");
				}
				if (s.compareTo("Connected to server") == 0)
				{
					return UTF("\u0421\u043E\u0435\u0434\u0438\u043D\u0435\u043D\u0438\u0435 \u0443\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D\u043E");
				}
				if (s.compareTo("Connection closed") == 0)
				{
					return UTF("\u0421\u043E\u0435\u0434\u0438\u043D\u0435\u043D\u0438\u0435 \u0437\u0430\u043A\u0440\u044B\u0442\u043E");
				}
				if (s.compareTo("Compression level") == 0)
				{
					return UTF("\u0421\u0436\u0430\u0442\u0438\u0435");
				}
				if (s.compareTo("Communication error") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430 \u0441\u043E\u0435\u0434\u0438\u043D\u0435\u043D\u0438\u044F");
				}
				if (s.compareTo("Cancel") == 0)
				{
					return UTF("\u041E\u0442\u043C\u0435\u043D\u0430");
				}

				break;
			case 'c' :
				if (s.compareTo("chars") == 0)
				{
					return UTF("\u0441\u0438\u043C\u0432\u043E\u043B\u043E\u0432");
				}
				if (s.compareTo("$cant_connect") == 0)
				{
					return UTF("\u041D\u0435\u0442 \u0441\u0432\u044F\u0437\u0438 \u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u043E\u043C. \u0423\u0431\u0435\u0434\u0438\u0442\u0435\u0441\u044C, \u0447\u0442\u043E \u0432\u0430\u0448 firewall \u0438\u043B\u0438 \u043F\u0440\u043E\u043A\u0441\u0438 \u043D\u0435 \u0431\u043B\u043E\u043A\u0438\u0440\u0443\u0435\u0442 \u043D\u0430\u0448 \u0441\u0430\u0439\u0442.");
				}
				if (s.compareTo("$conn_fail") == 0)
				{
					return UTF("\u0421\u0432\u044F\u0437\u044C \u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u043E\u043C \u043F\u043E\u0442\u0435\u0440\u044F\u043D\u0430");
				}
				break;
			case 'd' :
				if (s.compareTo("$download_next_series") == 0)
				{
					return UTF("\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u0441\u043B\u0435\u0434\u0443\u044E\u0449\u0443\u044E \u0441\u0435\u0440\u0438\u044E \u044D\u0442\u043E\u0433\u043E \u0438\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u044F?");
				}
				if (s.compareTo("$delete") == 0)
				{
					return UTF("\u0423\u0434\u0430\u043B\u0438\u0442\u044C \u0442\u0435\u043A\u0443\u0449\u0443\u044E \u0437\u0430\u043F\u0438\u0441\u044C \u0438 \u0441\u043E\u043E\u0442\u0432\u0435\u0442\u0441\u0442\u0432\u0443\u044E\u0449\u0438\u0435 \u0435\u0439 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F?");
				}
				if (s.compareTo("$date_sel") == 0)
				{
					return UTF("\u0412\u0432\u0435\u0434\u0435\u043D\u0430 \u043D\u0435\u0434\u043E\u043F\u0443\u0441\u0442\u0438\u043C\u0430\u044F \u0434\u0430\u0442\u0430");
				}
				break;
			case 'D' :
				if (s.compareTo("Delete record") == 0)
				{
					return UTF("\u0423\u0434\u0430\u043B\u0438\u0442\u044C \u0437\u0430\u043F\u0438\u0441\u044C");
				}
				if (s.compareTo("DICOM QUERY") == 0)
				{
					return UTF("\u0417\u0430\u043F\u043E\u0441 DICOM");
				}
				if (s.compareTo("Description") == 0)
				{
					return UTF("\u041E\u043F\u0438\u0441\u0430\u043D\u0438\u0435");
				}
				if (s.compareTo("Descr") == 0)
				{
					return UTF("\u041E\u043F\u0438\u0441");
				}
				if (s.compareTo("Distance") == 0)
				{
					return UTF("\u0414\u043B\u0438\u043D\u0430");
				}
				if (s.compareTo("Deviation") == 0)
				{
					return UTF("\u043E\u0442\u043A\u043B\u043E\u043D\u0435\u043D\u0438\u0435");
				}
				break;
			case 'E' :
				if (s.compareTo("Error message") == 0
						|| s.compareTo("Error") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430");
				}
				break;
			case 'f' :
				if (s.compareTo("$from_cache") == 0)
				{
					return UTF("\u0418\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F \u0438\u0437\u0432\u043B\u0435\u0447\u0435\u043D\u044B \u0438\u0437 \u0440\u0435\u0437\u0435\u0440\u0432\u0430");
				}
				break;
			case 'F' :
				if (s.compareTo("Flip Left - Right") == 0)
				{
					return UTF("\u041E\u0442\u0440\u0430\u0437\u0438\u0442\u044C \u043E\u0442\u043D. \u0432\u0435\u0440\u0442\u0438\u043A\u0430\u043B\u0438");
				}
				if (s.compareTo("Flip Top - Bottom") == 0)
				{
					return UTF("\u041E\u0442\u0440\u0430\u0437\u0438\u0442\u044C \u043E\u0442\u043D. \u0433\u043E\u0440\u0438\u0437\u043E\u043D\u0442\u0430\u043B\u0438");
				}
				if (s.compareTo("Format") == 0)
				{
					return UTF("\u0424\u043E\u0440\u043C\u0430\u0442");
				}
				break;
			case 'h' :
				if (s.compareTo("$hdr_fail") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430 \u043F\u0440\u0438 \u043F\u043E\u0441\u044B\u043B\u043A\u0435 \u0437\u0430\u0433\u043E\u043B\u043E\u0432\u043A\u0430 \u0437\u0430\u043F\u0440\u043E\u0441\u0430");
				}
				break;
			case 'H' :
				if (s.compareTo("Hide reviewed studies") == 0)
				{
					return UTF("\u041D\u0435 \u043F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0442\u044C \u0438\u0437\u0443\u0447\u0435\u043D\u043D\u044B\u0435");
				}
				break;
			case 'i' :
				if (s.compareTo("image records") == 0)
				{
					return UTF("\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0439");
				}
				if (s.compareTo("images received") == 0)
				{
					return UTF("\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0439 \u043F\u043E\u043B\u0443\u0447\u0435\u043D\u043E");
				}
				if (s.compareTo("images") == 0)
				{
					return UTF("\u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0439");
				}
				if (s.compareTo("image") == 0)
				{
					return UTF("\u0438\u0437\u043E\u0431\u0440.");
				}
				if (s.compareTo("$inval_resp") == 0)
				{
					return UTF("\u041D\u0435\u043A\u043E\u0440\u0440\u0435\u043A\u0442\u043D\u044B\u0439 \u043E\u0442\u0432\u0435\u0442 \u0441\u0435\u0440\u0432\u0435\u0440\u0430");
				}
				if (s.compareTo("$im_not_rcvd") == 0)
				{
					return UTF("\u0418\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u043D\u0435 \u043F\u043E\u043B\u0443\u0447\u0435\u043D\u043E");
				}
				break;
			case 'I' :
				if (s.compareTo("Intensity range") == 0)
				{
					return UTF("\u0414\u0438\u0430\u043F\u0430\u0437\u043E\u043D \u044F\u0440\u043A\u043E\u0441\u0442\u0435\u0439");
				}
				if (s.compareTo("ImgNumber") == 0)
				{
					return UTF("\u041D\u043E\u043C\u0435\u0440 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F");
				}
				if (s.compareTo("Images in series") == 0)
				{
					return UTF("\u0418\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0439 \u0432 \u0441\u0435\u0440\u0438\u0438");
				}
				if (s.compareTo("IMAGE level") == 0)
				{
					return UTF("\u0443\u0440\u043E\u0432\u043D\u0435 \u0418\u0417\u041E\u0411\u0420\u0410\u0416\u0415\u041D\u0418\u0415");
				}
				if (s.compareTo("Img") == 0)
				{
					return UTF("\u0418\u0437\u0431\u0440"); // "\u1048\u1079\u1073\u1088;"
				}
				if (s.compareTo("Image") == 0 || s.compareTo("Images") == 0)
				{
					return UTF("\u0418\u0437\u043E\u0431\u0440.");
				}
				if (s.compareTo("Images not found") == 0)
				{
					return UTF("\u0418\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u044B");
				}
				if (s.compareTo("Invert access rights") == 0)
				{
					return UTF("\u0418\u0437\u043C\u0435\u043D\u0438\u0442\u044C \u0434\u043E\u0441\u0442\u0443\u043F");
				}
				if (s.compareTo("ImgInstit") == 0)
				{
					return UTF("\u041C\u0435\u0434\u0438\u0446\u0438\u043D\u0441\u043A\u043E\u0435 \u0443\u0447\u0440\u0435\u0436\u0434\u0435\u043D\u0438\u0435");
				}
				break;
			case 'l' :
				if (s.compareTo("lev") == 0)
				{
					return UTF("\u0443\u0440\u043E\u0432\u0435\u043D\u044C");
				}
				if (s.compareTo("loaded") == 0)
				{
					return UTF("\u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u0430");
				}
			case 'L' :
				if (s.compareTo("Lossless") == 0)
				{
					return UTF("\u0411\u0435\u0437 \u043F\u043E\u0442\u0435\u0440\u044C");
				}
				if (s.compareTo("Local archive") == 0)
				{
					return UTF("\u041B\u043E\u043A\u0430\u043B\u044C\u043D\u044B\u0439 \u0430\u0440\u0445\u0438\u0432");
				}
				if (s.compareTo("Loading series") == 0)
				{
					return UTF("\u0417\u0430\u0433\u0440\u0443\u0437\u043A\u0430 \u0441\u0435\u0440\u0438\u0438");
				}
				if (s.compareTo("Loading") == 0)
				{
					return UTF("\u0417\u0430\u0433\u0440\u0443\u0437\u043A\u0430");
				}
				if (s.compareTo("Loading records") == 0)
				{
					return UTF("\u0417\u0430\u0433\u0440\u0443\u0437\u043A\u0430 \u0437\u0430\u043F\u0438\u0441\u0435\u0439");
				}
				if (s.compareTo("Loading image") == 0)
				{
					return UTF("\u0417\u0430\u0433\u0440\u0443\u0437\u043A\u0430 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F");
				}
				if (s.compareTo("Last 2 days") == 0)
				{
					return UTF("\u0417\u0430 \u043F\u0440\u0435\u0434. 2 \u0434\u043D\u044F");
				}
				if (s.compareTo("Last week") == 0)
				{
					return UTF("\u0417\u0430 \u043F\u0440\u0435\u0434. \u043D\u0435\u0434\u0435\u043B\u044E");
				}
				if (s.compareTo("Last 30 days") == 0)
				{
					return UTF("\u0417\u0430 \u043F\u0440\u0435\u0434. 30 \u0434\u043D\u0435\u0439");
				}
				break;
			case 'm' :
				if (s.compareTo("mm") == 0)
				{
					return UTF("\u043C\u043C");
				}
				if (s.compareTo("match found at") == 0)
				{
					return UTF("\u0437\u0430\u043F\u0438\u0441\u044C \u043D\u0430");
				}
				if (s.compareTo("matches found at") == 0)
				{
					return UTF("\u0437\u0430\u043F\u0438\u0441\u0435\u0439 \u043D\u0430");
				}
				if (s.compareTo("max") == 0)
				{
					return UTF("\u043C\u0430\u043A\u0441\u0438\u043C\u0443\u043C");
				}
				break;
			case 'M' :
				if (s.compareTo("Make public") == 0)
				{
					return UTF("\u041E\u0431\u0449. \u0434\u043E\u0441\u0442\u0443\u043F");
				}
				if (s.compareTo("Make private") == 0)
				{
					return UTF("\u0427\u0430\u0441\u0442\u043D. \u0434\u043E\u0441\u0442\u0443\u043F");
				}
				if (s.compareTo("Modality") == 0)
				{
					return UTF("\u041C\u043E\u0434\u0430\u043B\u044C\u043D.");
				}
				if (s.compareTo("Monitor frames") == 0)
				{
					return UTF("\u0412\u0438\u0440\u0442. \u043C\u043E\u043D\u0438\u0442\u043E\u0440\u044B");
				}
				if (s.compareTo("Message") == 0)
				{
					return UTF("\u0421\u043E\u043E\u0431\u0449\u0435\u043D\u0438\u0435");
				}
				if (s.compareTo("Min") == 0)
				{
					return UTF("\u043C\u0438\u043D");
				}
				if (s.compareTo("Max") == 0)
				{
					return UTF("\u043C\u0430\u043A\u0441");
				}
				if (s.compareTo("Minimum") == 0)
				{
					return UTF("\u041C\u0438\u043D\u0438\u043C\u0430\u043B\u044C\u043D\u043E\u0435");
				}
				if (s.compareTo("Medium") == 0)
				{
					return UTF("\u0421\u0440\u0435\u0434\u043D\u0435\u0435");
				}
				if (s.compareTo("Maximum") == 0)
				{
					return UTF("\u041C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u043E\u0435");
				}
				if (s.compareTo("My Patients") == 0)
				{
					return UTF("\u041C\u043E\u0438 \u043F\u0430\u0446\u0438\u0435\u043D\u0442\u044B");
				}
			case 'n' :
				if (s.compareTo("$not_found") == 0)
				{
					return UTF("\u0417\u0430\u043F\u0438\u043F\u0438\u0441\u0435\u0439 \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u043E");
				}
				if (s.compareTo("$note_exceed") == 0)
				{
					return UTF("\u0412\u0430\u0448\u0430 \u0437\u0430\u043F\u0438\u0441\u044C \u043F\u0440\u0435\u0432\u044B\u0448\u0430\u0435\u0442 255 \u0441\u0438\u043C\u0432\u043E\u043B\u043E\u0432");
				}
				if (s.compareTo("$no_memory") == 0)
				{
					return UTF("\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u043F\u0430\u043C\u044F\u0442\u0438 \u0434\u043B\u044F \u0437\u0430\u0433\u0440\u0443\u0437\u043A\u0438 \u0438\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u044F. \u0412\u0441\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043D\u044B\u0435 \u0441\u0435\u0440\u0438\u0438 \u0431\u0443\u0434\u0443\u0442 \u0434\u043E\u0441\u0442\u0443\u043F\u043D\u044B \u0432 \u0440\u0435\u0436\u0438\u043C\u0435 \u043F\u0440\u0435\u0434\u043F\u0440\u043E\u0441\u043C\u043E\u0442\u0440\u0430. \u0412\u044B \u043C\u043E\u0436\u0435\u0442\u0435 \u043F\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u0432\u0441\u0451 \u0438\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u0435 \u0441 \u0431\u041E\u043B\u044C\u0448\u0438\u043C \u0441\u0436\u0430\u0442\u0438\u0435\u043C \u0438\u043B\u0438 \u0441\u043C\u043E\u0436\u0435\u0442\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0430\u0442\u044C \u0441\u0435\u0440\u0438\u0438 \u043F\u043E \u043E\u0442\u0434\u0435\u043B\u044C\u043D\u043E\u0441\u0442\u0438 \u0438\u0437 \u0440\u0435\u0436\u0438\u043C\u0430 \u043F\u0440\u0435\u0434\u043F\u0440\u043E\u0441\u043C\u043E\u0442\u0440\u0430.");
				}
				if (s.compareTo("$no_memory_to_continue_operation") == 0)
				{
					return UTF("\u041D\u0435\u0434\u043E\u0441\u0442\u0430\u0442\u043E\u0447\u043D\u043E \u043F\u0430\u043C\u044F\u0442\u0438 Java \u0434\u043B\u044F \u0442\u0435\u043A\u0443\u0449\u0435\u0439 \u043E\u043F\u0435\u0440\u0430\u0446\u0438\u0438. \u041D\u0435\u043A\u043E\u0442\u043E\u0440\u044B\u0435 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F \u0431\u0443\u0434\u0443\u0442 \u0432\u044B\u0433\u0440\u0443\u0436\u0435\u043D\u044B \u0438\u0437 \u043F\u0430\u043C\u044F\u0442\u0438.");
				}
				break;
			case 'N' :
				if (s.compareTo("No matches found") == 0)
				{
					return UTF("\u0421\u043E\u043E\u0442\u0432\u0435\u0442\u0441\u0442\u0432\u0438\u0439 \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u043E");
				}
				if (s.compareTo("No item selected") == 0)
				{
					return UTF("\u0417\u0430\u043F\u0438\u0441\u044C \u043D\u0435 \u0432\u044B\u0431\u0440\u0430\u043D\u0430");
				}
				if (s.compareTo("Notes history") == 0)
				{
					return UTF("\u041F\u0440\u0435\u0434\u044B\u0434\u0443\u0449\u0438\u0435 \u0437\u0430\u043F\u0438\u0441\u0438");
				}
				if (s.compareTo("No update necessary") == 0)
				{
					return UTF("\u041E\u0431\u043D\u043E\u0432\u043B\u0435\u043D\u0438\u0435 \u043D\u0435 \u0442\u0440\u0435\u0431\u0443\u0435\u0442\u0441\u044F");
				}
				break;
			case 'O' :
				if (s.compareTo("Out of memory") == 0)
				{
					return UTF("\u041D\u0435\u0445\u0432\u0430\u0442\u043A\u0430 \u043F\u0430\u043C\u044F\u0442\u0438");
				}
				if (s.compareTo("OK") == 0)
				{
					return UTF("\u0414\u0430");
				}
				break;
			case 'o' :
				if (s.compareTo("of total") == 0)
				{
					return UTF("\u0438\u0437");
				}
				if (s.compareTo("$orient_modify") == 0)
				{
					return UTF("\u0418\u0437\u043C\u0435\u043D\u0438\u0442\u044C \u043E\u0440\u0438\u0435\u043D\u0442\u0430\u0446\u0438\u044E \u0432\u0441\u0435\u0445 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0439 \u0432 \u0442\u0435\u043A\u0443\u0449\u0435\u0439 \u0441\u0435\u0440\u0438\u0438?");
				}
				break;
			case 'p' :
				if (s.compareTo("pix") == 0)
				{
					return UTF("\u043F\u0438\u043A\u0441");
				}
				if (s.compareTo("patient records") == 0)
				{
					return UTF("\u043F\u0430\u0446\u0438\u0435\u043D\u0442\u043E\u0432");
				}
				if (s.compareTo("$prevent") == 0)
				{
					return UTF("\u0417\u0430\u043A\u0440\u044B\u0442\u044C \u0434\u043E\u0441\u0442\u0443\u043F \u043A \u044D\u0442\u043E\u0439 \u0437\u0430\u043F\u0438\u0441\u0438 \u0434\u043B\u044F \u0434\u0440\u0443\u0433\u0438\u0445 \u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u0442\u0435\u043B\u0435\u0439?");
				}
				break;
			case 'P' :
				if (s.compareTo("PatID") == 0 || s.compareTo("Patient ID") == 0)
				{
					return UTF("ID \u043F\u0430\u0446\u0438\u0435\u043D\u0442\u0430");
				}
				if (s.compareTo("PatName") == 0
						|| s.compareTo("Patient Name") == 0)
				{
					return UTF("\u0418\u043C\u044F \u043F\u0430\u0446\u0438\u0435\u043D\u0442\u0430");
				}
				if (s.compareTo("PATIENT level") == 0)
				{
					return UTF("\u0443\u0440\u043E\u0432\u043D\u0435 \u041F\u0410\u0426\u0418\u0415\u041D\u0422");
				}
				if (s.compareTo("Pan") == 0)
				{
					return UTF("\u041F\u0435\u0440\u0435\u043C\u0435\u0449\u0430\u0442\u044C");
				}
				if (s.compareTo("Processing image") == 0)
				{
					return UTF("\u041E\u0431\u0440\u0430\u0431\u043E\u0442\u043A\u0430 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F");
				}
				if (s.compareTo("PatSex") == 0)
				{
					return UTF("\u041F\u043E\u043B");
				}
				if (s.compareTo("Preview") == 0)
				{
					return UTF("\u041F\u0440\u0435\u0434\u0432. \u043F\u0440\u043E\u0441\u043C\u043E\u0442\u0440");
				}
				if (s.compareTo("Patient") == 0)
				{
					return UTF("\u041F\u0430\u0446\u0438\u0435\u043D\u0442");
				}
				break;
			case 'q' :
				if (s.compareTo("$query_incorrect") == 0)
				{
					return UTF("\u0421\u0431\u043E\u0439\u043D\u044B\u0439 \u0437\u0430\u043F\u0440\u043E\u0441, \u0441\u043E\u0435\u0434\u0438\u043D\u0438\u0442\u0435\u0441\u044C \u0437\u0430\u043D\u043E\u0432\u043E");
				}
				break;
			case 'r' :
				if (s.compareTo("records received") == 0)
				{
					return UTF("\u0437\u0430\u043F\u0438\u0441\u0435\u0439 \u043F\u043E\u043B\u0443\u0447\u0435\u043D\u043E");
				}
				if (s.compareTo("record(s) removed") == 0)
				{
					return UTF("\u0437\u0430\u043F\u0438\u0441\u0435\u0439 \u0443\u0434\u0430\u043B\u0435\u043D\u043E");
				}
				if (s.compareTo("$req_fail") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430 \u043F\u0440\u0438 \u043F\u043E\u0441\u044B\u043B\u043A\u0435 \u0437\u0430\u043F\u0440\u043E\u0441\u0430");
				}
				if (s.compareTo("$rec_fail") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430 \u043F\u0440\u0438 \u043F\u043E\u043B\u0443\u0447\u0435\u043D\u0438\u0438 \u0437\u0430\u043F\u0438\u0441\u0435\u0439");
				}
				if (s.compareTo("$reload_full") == 0)
				{
					return UTF("\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u044D\u0442\u0443 \u0441\u0435\u0440\u0438\u044E \u0446\u0435\u043B\u0438\u043A\u043E\u043C?");
				}
				break;
			case 'R' :
				if (s.compareTo("Ready") == 0)
				{
					return UTF("\u0413\u043E\u0442\u043E\u0432");
				}
				if (s.compareTo("Receiving records") == 0)
				{
					return UTF("\u041F\u043E\u043B\u0443\u0447\u0435\u043D\u0438\u0435 \u0437\u0430\u043F\u0438\u0441\u0435\u0439");
				}
				if (s.compareTo("Reading physician") == 0)
				{
					return UTF("\u041B\u0435\u0447\u0430\u0449\u0438\u0439 \u0432\u0440\u0430\u0447");
				}
				if (s.compareTo("Referring physician") == 0)
				{
					return UTF("\u041D\u0430\u043F\u0440\u0430\u0432\u0438\u0432\u0448\u0438\u0439 \u0432\u0440\u0430\u0447");
				}
				if (s.compareTo("Reload lossless") == 0)
				{
					return UTF("\u041F\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u0441\u043E \u0441\u0436\u0430\u0442. \u0431\u0435\u0437 \u043F\u043E\u0442\u0435\u0440\u044C");
				}
				if (s.compareTo("Rotate 90 Clockwise") == 0)
				{
					return UTF("\u0412\u0440\u0430\u0449. \u043D\u0430 90 \u043F\u043E \u0447\u0430\u0441\u043E\u0432\u043E\u0439");
				}
				if (s.compareTo("Rotate 90 Counter-Clockwise") == 0)
				{
					return UTF("\u0412\u0440\u0430\u0449. \u043D\u0430 90 \u043F\u0440\u043E\u0442\u0438\u0432 \u0447\u0430\u0441\u043E\u0432\u043E\u0439");
				}
				if (s.compareTo("Rotate 180") == 0)
				{
					return UTF("\u0412\u0440\u0430\u0449. \u043D\u0430 180");
				}
				if (s.compareTo("Receiving records failed") == 0)
				{
					return UTF("\u041D\u0435 \u0443\u0434\u0430\u043B\u043E\u0441\u044C \u043F\u043E\u043B\u0443\u0447\u0438\u0442\u044C \u0437\u0430\u043F\u0438\u0441\u0438");
				}
				break;
			case 's' :
				if (s.compareTo("sq") == 0)
				{
					return UTF("\u043A\u0432");
				}
				if (s.compareTo("study records") == 0)
				{
					return UTF("\u0438\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u0439");
				}
				if (s.compareTo("series records") == 0)
				{
					return UTF("\u0441\u0435\u0440\u0438\u0439");
				}
				if (s.compareTo("$send_fail") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430 \u043F\u0440\u0438 \u043E\u0442\u0441\u044B\u043B\u043A\u0435 \u0434\u0430\u043D\u043D\u044B\u0445");
				}
				if (s.compareTo("$sock_err") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430 \u0441\u043E\u043A\u0435\u0442\u0430");
				}
				break;
			case 'S' :
				if (s.compareTo("Study Date") == 0)
				{
					return UTF("\u0414\u0430\u0442\u0430 \u0438\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u044F");
				}
				if (s.compareTo("Study Time") == 0)
				{
					return UTF("\u0412\u0440\u0435\u043C\u044F \u0438\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u044F");
				}
				if (s.compareTo("Study Date & Time") == 0)
				{
					return UTF("\u0414\u0430\u0442\u0430, \u0432\u0440\u0435\u043C\u044F \u0438\u0441\u0441\u043B-\u044F");
				}
				if (s.compareTo("Study loaded") == 0)
				{
					return UTF("\u0418\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E");
				}
				if (s.compareTo("Study") == 0)
				{
					return UTF("\u0418\u0441\u0441\u043B\u0435\u0434-\u0435");
				}
				if (s.compareTo("STUDY level") == 0)
				{
					return UTF("\u0443\u0440\u043E\u0432\u043D\u0435 \u0418\u0421\u0421\u041B\u0415\u0414\u041E\u0412\u0410\u041D\u0418\u0415");
				}
				if (s.compareTo("Start Search") == 0)
				{
					return UTF("\u041F\u043E\u0438\u0441\u043A");
				}
				if (s.compareTo("StModalities") == 0)
				{
					return UTF("\u041C\u043E\u0434\u0430\u043B\u044C\u043D\u043E\u0441\u0442\u0438 \u0438\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u044F");
				}
				if (s.compareTo("SerModality") == 0)
				{
					return UTF("\u041C\u043E\u0434\u0430\u043B\u044C\u043D\u043E\u0441\u0442\u044C \u0441\u0435\u0440\u0438\u0438");
				}
				if (s.compareTo("Study note") == 0)
				{
					return UTF("\u0417\u0430\u043C\u0435\u0442\u043A\u0438");
				}
				if (s.compareTo("Sending request") == 0)
				{
					return UTF("\u041F\u043E\u0441\u044B\u043B\u0430\u0435\u0442\u0441\u044F \u0437\u0430\u043F\u0440\u043E\u0441");
				}
				if (s.compareTo("Series") == 0)
				{
					return UTF("\u0421\u0435\u0440\u0438\u044F");
				}
				if (s.compareTo("Study notes") == 0)
				{
					return UTF("\u041A\u043E\u043C\u043C\u0435\u043D\u0442\u0430\u0440\u0438\u0438 \u043A \u0438\u0441\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u043D\u0438\u044E");
				}
				if (s.compareTo("SERIES level") == 0)
				{
					return UTF("\u0443\u0440\u043E\u0432\u043D\u0435 \u0421\u0415\u0420\u0418\u042F");
				}
				if (s.compareTo("Series loaded") == 0)
				{
					return UTF("\u0421\u0435\u0440\u0438\u044F \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u0430");
				}
				if (s.compareTo("Series prepared") == 0)
				{
					return UTF("\u0421\u0435\u0440\u0438\u044F \u043F\u0440\u0438\u0433\u043E\u0442\u043E\u0432\u043B\u0435\u043D\u0430");
				}
				if (s.compareTo("Show series") == 0)
				{
					return UTF("\u041F\u043E\u043A\u0430\u0437\u0430\u0442\u044C \u0441\u0435\u0440\u0438\u044E");
				}
				if (s.compareTo("Ser") == 0)
				{
					return UTF("\u0421\u0435\u0440");
				}
				if (s.compareTo("Size") == 0)
				{
					return UTF("\u0420\u0430\u0437\u043C\u0435\u0440");
				}
				if (s.compareTo("SerNumber") == 0)
				{
					return UTF("\u041D\u043E\u043C\u0435\u0440 \u0441\u0435\u0440\u0438\u0438");
				}
				if (s.compareTo("System memory") == 0)
				{
					return UTF("\u0421\u0438\u0441\u0442\u0435\u043C\u043D\u0430\u044F \u043F\u0430\u043C\u044F\u0442\u044C");
				}
				if (s.compareTo("Server error") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430 \u043D\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0435");
				}
				if (s.compareTo("Series%20from%20Web%20PACS") == 0)
				{
					return UTF("%D1%E5%F0%E8%E8%20%F1%20%C2%E5%E1-PACS");
				}
				break;
			case 'T' :
				if (s.compareTo("Today") == 0)
				{
					return UTF("\u0421\u0435\u0433\u043E\u0434\u043D\u044F");
				}
				if (s.compareTo("Transfer error") == 0)
				{
					return UTF("\u041E\u0448\u0438\u0431\u043A\u0430 \u043F\u0435\u0440\u0435\u0434\u0430\u0447\u0438");
				}
				break;
			case 'u' :
				if (s.compareTo("$upd_rec") == 0)
				{
					return UTF("\u041A\u043E\u043B\u0438\u0447\u0435\u0441\u0442\u0432\u043E \u043E\u0431\u043D\u043E\u0432\u043B\u0451\u043D\u043D\u044B\u0445 \u0437\u0430\u043F\u0438\u0441\u0435\u0439");
				}
				break;
			case 'V' :
				if (s.compareTo("View first study") == 0)
				{
					return UTF("\u0421\u043C\u043E\u0442\u0440. 1-\u0435 \u0438\u0441\u0441\u043B-\u0435");
				}
				if (s.compareTo("View study") == 0)
				{
					return UTF("\u0421\u043C\u043E\u0442\u0440\u0435\u0442\u044C \u0438\u0441\u0441\u043B\u0435\u0434-\u0435");
				}
				if (s.compareTo("View series") == 0)
				{
					return UTF("\u0421\u043C\u043E\u0442\u0440\u0435\u0442\u044C \u0441\u0435\u0440\u0438\u044E");
				}
				if (s.compareTo("View image") == 0)
				{
					return UTF("\u0421\u043C\u043E\u0442\u0440\u0435\u0442\u044C \u0438\u0437\u043E\u0431\u0440-\u0435");
				}
				break;
			case 'w' :
				if (s.compareTo("win") == 0)
				{
					return UTF("\u043E\u043A\u043D\u043E");
				}
				if (s.compareTo("$web_url") == 0)
				{
					return UTF("\u0412\u0435\u0431-\u0441\u0441\u044B\u043B\u043A\u0430 (\u0432\u044B\u0431\u0440\u0430\u0442\u044C - \u043A\u043B\u0438\u043A, \u043F\u043E\u0441\u043B\u0430\u0442\u044C \u043F\u043E \u044D\u043B. \u043F\u043E\u0447\u0442\u0435 - shift+\u043A\u043B\u0438\u043A, \u043E\u0442\u043A\u0440\u044B\u0442\u044C \u0432 \u043D\u043E\u0432\u043E\u043C \u043E\u043A\u043D\u0435 - ctrl+\u043A\u043B\u0438\u043A)");
				}
				break;
			case 'W' :
				if (s.compareTo("Worklist") == 0)
				{
					return UTF("\u0420\u0430\u0431\u043E\u0447\u0438\u0439 \u0441\u043F\u0438\u0441\u043E\u043A");
				}
				if (s.compareTo("Warning") == 0)
				{
					return UTF("\u041F\u0440\u0435\u0434\u0443\u043F\u0440\u0435\u0436\u0434\u0435\u043D\u0438\u0435");
				}
				if (s.compareTo("Web URL") == 0)
				{
					return UTF("\u0412\u0435\u0431-\u0441\u0441\u044B\u043B\u043A\u0430");
				}
				if (s.compareTo("Waiting for server") == 0)
				{
					return UTF("\u041E\u0436\u0438\u0434\u0430\u043D\u0438\u0435 \u0441\u0435\u0440\u0432\u0435\u0440\u0430");
				}
				if (s.compareTo("Waiting for image") == 0)
				{
					return UTF("\u041E\u0436\u0438\u0434\u0430\u043D\u0438\u0435 \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F");
				}
				break;
			case 'Y' :
				if (s.compareTo("Year of birth") == 0)
				{
					return UTF("\u0413\u043E\u0434 \u0440\u043E\u0436\u0434\u0435\u043D\u0438\u044F");
				}
				break;
			case 'y' :
				if (s.compareTo("$your_note") == 0)
				{
					return UTF("\u0412\u0430\u0448 \u043A\u043E\u043C\u043C\u0435\u043D\u0442\u0430\u0440\u0438\u0439, \u043C\u0430\u043A\u0441. 255 \u0441\u0438\u043C\u0432\u043E\u043B\u043E\u0432");
				}
				if (s.compareTo("yyyymmdd") == 0)
				{
					return UTF("\u0433\u0433\u0433\u0433\u043C\u043C\u0434\u0434");
				}
				break;
			case 'z' :
			case 'Z' :
				if (s.compareTo("Zoom") == 0)
				{
					return UTF("\u0423\u0432\u0435\u043B\u0438\u0447\u0438\u0432\u0430\u0442\u044C");
				}
				break;
			default :
				return s;
		}
		return s;
	}
}
