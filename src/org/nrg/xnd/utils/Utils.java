package org.nrg.xnd.utils;

import java.awt.Component;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.nrg.fileserver.ItemRecord;
import org.nrg.fileserver.ItemTag;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.model.TagDescr;
import org.nrg.xnd.tools.ImageViewer.ImageViewerManager;

/**
 * An abstract class for performing frequently required tasks.
 * 
 * @author mmilch
 * 
 */
public abstract class Utils
{
	public static final byte SER_OBJ_ITEMRECORD = 0, SER_OBJ_TAGATTR = 1,
			SER_OBJ_ITEMTAG = 2, SER_OBJ_STRING = 3;

	// default settings
	private static final int BLOCK_SZ = 16384;
	static final byte ARR_BYTE = 0, ARR_INT = 1;
	public static final int PORT_FILE_DEFAULT = 8072;
	public static final int PORT_REPOSITORY_DEFAULT = 8081;
	public static final String REMOTE_ADDRESS_DEFAULT = "http://localhost";
	// public static final byte XML_DEFAULT_ONTOLOGY=0;

	public static final int MAX_TABLE_RECORDS = 10000;

	private static final byte[] m_ByteBuf = new byte[10240];
	public static final Logger logger = Logger.getLogger("XNATDesktop");

	private static long m_UIDSeed = 0;
	private static JFrame m_ViewerFrame = null;
	private static ImageViewerManager m_ivm = null;

	public final static NativeFileManager m_nfm = new NativeFileManager();

	public static String SerializeString(Object stream, String s,
			boolean is_loading) throws IOException
	{
		if (is_loading)
		{
			int len;
			ObjectInputStream in = (ObjectInputStream) stream;
			len = in.readInt();
			if (len < 0)
				return null;
			in.readFully(m_ByteBuf, 0, len);
			return new String(m_ByteBuf, 0, len);
		} else
		{
			ObjectOutputStream out = (ObjectOutputStream) stream;
			if (s != null)
				out.writeInt(s.length());
			else
			{
				out.writeInt(-1);
				return null;
			}
			out.writeBytes(s);
			return null;
		}
	}
	/**
	 * Serialization is implemented here and not in ItemTag class, because this
	 * is a high-level file-based repository manager specific function.
	 * 
	 * @param stream
	 * @param t
	 * @param is_loading
	 * @return
	 * @throws IOException
	 */
	public static ItemTag SerializeTag(Object stream, ItemTag t,
			boolean is_loading) throws IOException
	{
		if (is_loading)
		{
			ItemTag tag = new ItemTag(SerializeString(stream, null, true));
			tag.SetValue(SerializeString(stream, null, true));
			return tag;
		} else
		{
			SerializeString(stream, t.GetName(), false);
			SerializeString(stream, t.GetFirstValue(), false);
			return null;
		}
	}
	/**
	 * Item record serialization is implemented here and not in LocalItemRecord
	 * class, since it is not inherent to the low-level implementation.
	 * 
	 * @param stream
	 * @param ir
	 * @param is_loading
	 * @return
	 * @throws IOException
	 */
	public static ItemRecord SerializeItemRecord(Object stream, ItemRecord ir,
			boolean is_loading) throws IOException
	{
		if (is_loading)
		{
			int len;
			ObjectInputStream fis = (ObjectInputStream) stream;
			String abs_path = SerializeString(stream, null, true), rel_path = SerializeString(
					stream, null, true);

			ItemRecord res = new ItemRecord(abs_path, rel_path);
			len = fis.readInt();
			for (int i = 0; i < len; i++)
				res.tagSet(SerializeTag(stream, null, true));
			return res;
		} else
		{
			ObjectOutputStream out = (ObjectOutputStream) stream;
			SerializeString(stream, ir.getAbsolutePath(), false);
			SerializeString(stream, ir.getRelativePath(), false);
			ItemTag[] tags = ir.getAllTags();
			out.writeInt(tags.length);
			for (int i = 0; i < tags.length; i++)
				SerializeTag(stream, tags[i], false);
			// out.flush();
			return null;
		}
	}

	public static File GetNewTempFile()
	{
		try
		{
			File f = File.createTempFile("wpimg_", null);
			return f;
		} catch (Exception e)
		{
			return null;
		}
	}
	public static FSObject[] ListFiles(File f, boolean bCheckDirs)
	{
		return m_nfm.ListFiles(f, bCheckDirs);
	}
	public static boolean IsDirectory(File f)
	{

		// return f.isDirectory();
		return m_nfm.IsDirectory(f);

		/*
		 * try { return
		 * VFS.getManager().toFileObject(f).getType()==FileType.FOLDER; }
		 * catch(Exception e) { System.out.println(e.getMessage()); return
		 * false; }
		 */
		// final FileSystemView fsv=FileSystemView.getFileSystemView();
		// JFileChooser jfc=new JFileChooser(f);
		// return fsv.isTraversable(f);

	}
	public static java.awt.Frame FindParentFrame(Component comp)
	{
		if (comp instanceof Frame)
			return (Frame) comp;
		for (Component c = comp; c != null; c = c.getParent())
			if (c instanceof Frame)
				return (Frame) c;
		return null;
	} // end of method FindParentFrame.

	public static boolean SerializeCollection(Object stream, Collection col,
			byte obj_id, boolean is_loading)
	{
		try
		{
			if (is_loading)
			{
				ObjectInputStream in = (ObjectInputStream) stream;
				int num, i;
				num = in.readInt();
				Object obj;
				for (i = 0; i < num; i++)
				{
					switch (obj_id)
					{
						case SER_OBJ_STRING :
							obj = SerializeString(stream, null, true);
							if (obj != null)
								col.add(obj);
							break;
					}
				}
				return true;
			} else
			{
				ObjectOutputStream out = (ObjectOutputStream) stream;
				out.writeInt(col.size());
				for (Object obj : col)
				{
					switch (obj_id)
					{
						case SER_OBJ_STRING :
							SerializeString(stream, (String) (obj), false);
							break;
					}
				}
				return true;
			}
		} catch (Exception e)
		{
			return false;
		}
	}
	public static boolean SerializeTreeMap(Object stream, TreeMap map,
			byte obj_id, boolean is_loading)
	{
		try
		{
			if (is_loading)
			{
				ObjectInputStream in = (ObjectInputStream) stream;
				int num, i;
				num = in.readInt();
				Object obj;
				for (i = 0; i < num; i++)
				{
					switch (obj_id)
					{
						case SER_OBJ_TAGATTR :
							obj = new TagDescr();
							if (!((TagDescr) (obj)).Serialize(in, true))
								return false;
							map.put(((TagDescr) (obj)).GetName(), obj);
							break;
						case SER_OBJ_ITEMTAG :
							if ((obj = SerializeTag(in, null, true)) == null)
								return false;
							map.put(((ItemTag) obj).GetName(), obj);
							break;
						case SER_OBJ_ITEMRECORD :
							if ((obj = SerializeItemRecord(in, null, true)) == null)
								return false;
							map.put(((ItemRecord) obj).getAbsolutePath(), obj);
							break;
					}
				}
				return true;
			} else
			{
				ObjectOutputStream out = (ObjectOutputStream) (stream);
				out.writeInt(map.size());
				for (final Object obj : map.values())
				{
					switch (obj_id)
					{
						case SER_OBJ_TAGATTR :
							if (!((TagDescr) obj).Serialize(out, false))
								return false;
							break;
						case SER_OBJ_ITEMTAG :
							SerializeTag(out, (ItemTag) obj, false);
							break;
						case SER_OBJ_ITEMRECORD :
							SerializeItemRecord(out, (ItemRecord) obj, false);
							break;
					}
				}
				// out.flush();
				return true;
			}
		} catch (Exception e)
		{
			return false;
		}

	}
	private static Object AllocateArray(int len, byte type)
			throws OutOfMemoryError
	{
		switch (type)
		{
			case ARR_BYTE :
				return new byte[len];
			case ARR_INT :
				return new int[len];
		}
		return null;
	}

	public static void WriteIntArrayBuffered(ObjectOutputStream oos, int[] arr)
			throws IOException, OutOfMemoryError
	{
		final byte[] buf = new byte[BLOCK_SZ];
		int off = 0;
		oos.writeInt(arr.length);
		while (off < arr.length)
		{
			off = WriteArrayBlock(oos, arr, buf, off);
		}
	}
	public static int[] ReadIntArrayBuffered(ObjectInputStream ois)
			throws IOException, OutOfMemoryError
	{
		int len = ois.readInt();
		int[] arr = new int[len];
		final byte[] buf = new byte[BLOCK_SZ];
		int off = 0;
		while (off < len)
			off = ReadArrayBlock(ois, arr, buf, off);
		return arr;
	}
	private static int WriteArrayBlock(ObjectOutputStream oos, int[] arr,
			byte[] block, int off) throws IOException, OutOfMemoryError
	{
		final int int_block_sz = block.length / 4;
		final int last_index = Math.min(off + int_block_sz, arr.length);
		int t, byte_chunk_len = 0;
		for (int i = off, j = 0; i < last_index; i++, j += 4)
		{
			t = arr[i];
			block[j] = (byte) ((t >> 24) & 0xff);
			block[j + 1] = (byte) ((t >> 16) & 0xff);
			block[j + 2] = (byte) ((t >> 8) & 0xff);
			block[j + 3] = (byte) (t & 0xff);
			byte_chunk_len += 4;
		}
		oos.write(block, 0, byte_chunk_len);
		return last_index;
	}
	/**
	 * @param ois
	 *            Input stream
	 * @param arr
	 *            Destination to write to
	 * @param block
	 *            Source buffer to read from
	 * @param off
	 *            offset in destination
	 * @throws IOException
	 * @throws OutOfMemoryError
	 */
	private static int ReadArrayBlock(ObjectInputStream ois, int[] arr,
			byte[] block, int off) throws IOException, OutOfMemoryError
	{

		final int int_block_sz = block.length / 4;
		final int byte_block_sz = block.length;
		final int last_index = Math.min(off + int_block_sz, arr.length);
		final int read_byte_len = Math.min((arr.length - off) * 4,
				byte_block_sz);
		ois.readFully(block, 0, read_byte_len);
		for (int i = off, j = 0; i < last_index; i++, j += 4)
		{
			arr[i] = (((block[j]) << 24) & 0xff000000)
					| (((block[j + 1]) << 16) & 0xff0000)
					| (((block[j + 2]) << 8) & 0xff00)
					| (((block[j + 3])) & 0xff);
		}
		return last_index;
	}

	public static Object SerializeArray(Object stream, Object arr,
			boolean is_loading) throws IOException, OutOfMemoryError
	{
		byte type;
		if (arr instanceof byte[])
			type = ARR_BYTE;
		else if (arr instanceof int[])
			type = ARR_INT;
		else
			return null;

		if (is_loading)
		{
			int len;
			ObjectInputStream in = (ObjectInputStream) stream;
			switch (type)
			{
				case ARR_BYTE :
					len = in.readInt();
					if (len < 1)
						return AllocateArray(0, type);
					Object res = AllocateArray(len, type);
					in.readFully((byte[]) res, 0, len);
					return res;
				case ARR_INT :
					int[] i_res = ReadIntArrayBuffered(in);
					// for(int i=0; i<len; i++)
					// i_res[i]=in.readInt();
					return i_res;
			}
			return null;
		} else
		{
			ObjectOutputStream out = (ObjectOutputStream) stream;
			int len;
			switch (type)
			{
				case ARR_BYTE :
					len = ((byte[]) arr).length;
					out.writeInt(len);
					out.write((byte[]) arr);
					return null;
				case ARR_INT :
					int[] arr_i = (int[]) arr;
					WriteIntArrayBuffered(out, arr_i);
					// len=arr_i.length;
					// out.writeInt(len);
					// for(int i=0; i<len; i++)
					// out.writeInt(arr_i[i]);
					return null;
			}
			return null;
		}
	}
	private static byte[] SerializeByteArray(Object stream, byte[] arr,
			int len, boolean is_loading) throws IOException
	{
		if (is_loading)
		{
			InputStream in = (InputStream) stream;
			int r, off = 0, bytes_read = 0;
			byte[] buf = new byte[len];
			do
			{
				r = in.read(buf, off, len - bytes_read);
				bytes_read += r;
				off += r;
			} while (bytes_read < len);
			return buf;
		} else
		{
			OutputStream out = (OutputStream) stream;
			out.write(arr);
			return null;
		}
	}
	public static char NextValidChar(char c)
	{
		char res = (char) (c + 1);
		if (!ValidChar(res))
			return '~';
		return res;
	}
	public static String MaskPath(String path)
	{
		return path.replace('/', '_').replace('\\', '_');
	}
	public static boolean ValidChar(char c)
	{
		return ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
				|| (c >= '0' && c <= '9') || c == '_' || c == '.');
	}
	public static String StrFormatURI(String s)
	{
		if (s == null)
			return s;
		char ch;
		String res = s;
		for (int i = 0; i < s.length(); i++)
		{
			if (!ValidChar(ch = s.charAt(i)))
			{
				char[] syms = {ch};
				res = res.replace(new String(syms), "%"
						+ String.format("%1$.02h", ch));
			}
		}
		return res;
	}
	public static String StrFormat(String s)
	{
		char ch;
		String res = s;
		for (int i = 0; i < s.length(); i++)
		{
			if (!ValidChar(ch = s.charAt(i)))
				res = res.replace(ch, '_');
		}
		return res;
	}
	public static String GetPluginPath()
	{
		try
		{
			String s = new Path(FileLocator.resolve(
					FileLocator.find(
							Platform.getBundle("org.nrg.xnat.desktop"),
							new Path("/"), null)).getFile()).toFile()
					.toString();
			if (s == null)
				s = "";
			s = (!s.endsWith("/")) ? s += "/" : s;
			return s;
		} catch (Exception e)
		{
		}
		return "";
	}
	public static int ShowMessageBox(String caption, String msg, int type)
	{
		MessageBox mb = new MessageBox(new Shell(), type);
		mb.setText(caption);
		mb.setMessage(msg);
		return mb.open();
	}
	public static String NameFromPath(String path)
	{
		String res = path;
		while (res.endsWith("/"))
			res = res.substring(0, res.lastIndexOf("/") - 1);
		while (res.endsWith("\\"))
			res = res.substring(0, res.lastIndexOf("\\") - 1);
		int sep = Math.max(res.lastIndexOf('\\'), res.lastIndexOf('/'));
		if (sep < 0)
			return res;
		return res.substring(sep + 1);
	}
	/**
	 * Check to see if immediate folder names of two different paths are the
	 * same.
	 * 
	 * @param path1
	 * @param path2
	 * @return
	 */
	public static boolean MatchFolderNames(String path1, String path2)
	{
		return NameFromPath(path1).compareTo(NameFromPath(path2)) == 0;
	}
	public static String GetUserFolder()
	{
		return System.getProperty("user.home") + "/.xnd";
	}
	public static String GetIncomingFolder()
	{
		String def_fold = GetUserFolder() + "/Incoming";
//		def_fold = XNDApp.app_Prefs.get("IncomingFolder", def_fold);
		return new File(def_fold).getAbsolutePath();
	}
	public static File GetCollectionFolder()
	{
		String def_fold = GetUserFolder() + "/collections";
		File f = new File(def_fold);
		if (!f.exists())
		{
			f.mkdir();
		}
		return f;
	}
	public static File SuggestLocalPath(ItemRecord remoteItem)
	{
		String[] mf = XNDApp.app_localVM.GetManagedFolders();
		if (mf == null || mf.length < 1)
			return null;
		return new File(mf[0] + "//" + remoteItem.getFileName());
	}
	public static String SelectFolder(String title, String msg)
	{
		if (XNDApp.app_Platform == XNDApp.PLATFORM_WIN32
				|| XNDApp.app_Platform == XNDApp.PLATFORM_MAC)
		{
			String fold = XNDApp.app_Prefs.get("LastFolder", null);
			DirectoryDialog d = new DirectoryDialog(new Shell());
			d.setText(title);
			d.setMessage(msg);
			d.setFilterPath(fold);
			if (d.open() != null)
			{
				XNDApp.app_Prefs.put("LastFolder", d.getFilterPath());
				return d.getFilterPath();
			} else
				return null;
		} else
		{
			InputDialog d = new InputDialog(new Shell(), title, msg, "~/",
					new IInputValidator()
					{
						public String isValid(String newText)
						{
							File f = new File(newText);
							if (!f.exists())
								return "Direcotory does not exist";
							if (!Utils.IsDirectory(f))
								return "Not a directory";
							return null;
						}
					});
			if (d.open() == Window.OK)
				return d.getValue();
			return null;
		}
	}
	public static String SelectFile(String msg, String def_file)
	{
		FileDialog fd = new FileDialog(new Shell());
		fd.setFileName(def_file);
		fd.setText(msg);
		String res;
		if (fd.open() != null)
		{
			return new File(fd.getFilterPath() + "/" + fd.getFileName())
					.getAbsolutePath();
		} else
			return def_file;
	}
	public static Collection<String> SelectFiles(String msg)
	{
		FileDialog fd = new FileDialog(new Shell(), SWT.MULTI);
		fd.setText(msg);
		LinkedList<String> ll = new LinkedList<String>();
		if (fd.open() != null)
		{
			String[] files = fd.getFileNames();
			for (String s : files)
			{
				ll
						.add(new File(fd.getFilterPath() + "/" + s)
								.getAbsolutePath());
			}
		}
		return ll;
	}
	public static String MakePath(ItemRecord ir, String... tagNames)
	{
		String[] tagPath = new String[tagNames.length];
		for (int i = 0; i < tagNames.length; i++)
		{
			if ((tagPath[i] = ir.getTagValue(tagNames[i])) == null)
				return null;
		}
		return MakePath(tagPath);
	}
	public static String MakePath(String... tags)
	{
		String res = "";
		for (int i = 0; i < tags.length; i++)
		{
			res += "/" + tags[i];
		}
		return res;
	}
	public static String[] ParsePath(String tagPath)
	{
		LinkedList<String> tags = new LinkedList<String>();
		int st = 1, en;
		while ((en = tagPath.indexOf('/', st)) > 0)
		{
			tags.add(tagPath.substring(st, en - 1));
			st = en + 1;
		}
		return tags.toArray(new String[0]);
	}
	public static String[] GetTagValues(String name, ItemRecord[] records)
	{
		TreeMap<String, String> tm = new TreeMap<String, String>();
		ItemTag it;
		String s;
		for (int i = 0; i < records.length; i++)
		{
			if ((it = records[i].getTag(name)) == null)
				continue;
			s = it.GetFirstValue();
			if (s.length() < 1 || tm.containsKey(s))
				continue;
			else
				tm.put(s, s);
		}
		return tm.values().toArray(new String[0]);
	}
	public static String GetFormattedSize(long sz)
	{
		double dsz = sz;
		if (sz < 1024)
			return String.format("%1$d bytes", sz);
		else if (sz < 1048576)
			return String.format("%1$.1f KB", dsz / 1024.0);
		else if (sz < 1073741824)
			return String.format("%1$.1f MB", dsz / 1048576.0);
		else
			return String.format("%1$.1f GB", dsz / 1073741824.0);
	}
	public static long SizeFromFormattedStr(String s)
	{
		String sub = s.substring(0, s.indexOf(' ') - 1);
		double res = new Double(sub).doubleValue();
		if (s.contains("bytes"))
			return (long) res;
		else if (s.contains("KB"))
			return (long) (res * 1024.0);
		else if (s.contains("MB"))
			return (long) (res * 1048576.0);
		else
			return (long) (res * 1073741824.0);
	}
	public static String PseudoUID(String end)
	{
		return new Long(Calendar.getInstance().getTimeInMillis()).toString()
				+ new Integer(Calendar.getInstance().get(Calendar.DATE))
						.toString()
				+ "."
				+ new Integer(Calendar.getInstance().get(Calendar.YEAR))
						.toString() + "." + new Long(m_UIDSeed++).toString()
				+ ((end.length() > 0) ? ("." + end) : "");
	}
	/**
	 * check if neither of two paths is the beginning of the other.
	 * 
	 * @param path1
	 * @param path2
	 * @return
	 */
	public static int CrossCheckDirs(String path1, String path2)
	{
		if (path1.startsWith(path2))
			return -1;
		if (path2.startsWith(path1))
			return 1;
		return 0;
	}
	public static boolean CopyFile(File src, File dest)
	{
		try
		{
			String command = "";

			if (XNDApp.app_Platform == XNDApp.PLATFORM_WIN32)
				command = "cmd /c copy " + src.getAbsolutePath() + " "
						+ dest.getAbsolutePath();
			else
				command = "cp " + src.getAbsolutePath() + " "
						+ dest.getAbsolutePath();

			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
			return true;
		} catch (Exception e)
		{
			return false;
		}
	}
	public static void SerializeListOfValues(String ID,
			Collection<String> vals, boolean is_loading)
	{
		final String token = "%&%";
		if (is_loading)
		{
			String val = XNDApp.app_Prefs.get(ID, "");
			// if(val.length()<1) return;
			String[] strings = val.split(token);
			for (String s : strings)
				vals.add(s);
		} else
		{
			String res = "";
			int i = 0;
			for (String s : vals)
			{
				if (i == 0)
					res = s;
				else
					res += token + s;
				i++;
			}
			XNDApp.app_Prefs.put(ID, res);
		}
	}
	public static byte[] UnzipBuf(byte[] buf) throws IOException
	{
		ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(buf));
		try
		{
			ZipEntry entry = zin.getNextEntry();
			// System.out.println("Zip entry size: "+entry.getSize());
			byte[] res = readZipBuf(zin);
			return res;
		} catch (Exception e)
		{
			return null;
		} finally
		{
			if (zin != null)
				zin.close();
		}
	}
	private static byte[] readZipBuf(ZipInputStream zin)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			int len = 0, total = 0;
			byte[] buf = new byte[2048];
			while ((len = zin.read(buf)) > 0)
			{
				baos.write(buf, 0, len);
				// total+=len;
				// System.out.println(total);
			}
			return baos.toByteArray();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
			return null;
		} finally
		{
			if (baos != null)
				try
				{
					baos.close();
				} catch (Exception e)
				{
				}
		}
	}

	public static void CheckForThreadMessages()
	{
		String msg;
		/*
		 * if((msg=StoreXARManager.GetInterfaceMessage())!=null)
		 * ShowMessageBox("Upload message", msg, Window.OK);
		 */
	}
}