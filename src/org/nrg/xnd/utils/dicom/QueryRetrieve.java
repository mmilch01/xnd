package org.nrg.xnd.utils.dicom;

import java.io.File;
import java.io.IOException;

import org.nrg.xnd.app.ConsoleView;
import org.nrg.xnd.app.XNDApp;
import org.nrg.xnd.utils.Utils;
import org.nrg.xnd.utils.dicom.AEList.AE;

import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.StoredFilePathStrategyHashSubFolders;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;

public final class QueryRetrieve
{
	private static StorageSOPClassSCPDispatcher m_sscd;
	private static AE m_ae;
	private static String m_fold;

	public static boolean isRunning()
	{
		return m_sscd != null;
	}
	public static boolean isConfigChanged(String fold)
	{
		if (m_ae == null || m_fold == null)
			return true;
		if (m_ae.compareTo(XNDApp.app_aeList.getAE("local")) != 0)
			return true;
		return (m_fold.compareTo(fold) != 0);
	}
	public static String start(String store_fold, ReceivedObjectHandler roh)
	{
		int debug_level = 1;
		m_fold = store_fold;
		if (m_sscd != null)
			return null;
		// m_sscd.shutdown();

		try
		{
			AE local = XNDApp.app_aeList.getAE("local");
			m_ae = XNDApp.app_aeList.new AE(local);

			System.err
					.println("StorageSOPClassSCPDispatcher.main(): listening on port "
							+ m_ae.m_recvPort
							+ " AE "
							+ m_ae.m_title
							+ " storing into "
							+ store_fold
							+ " debugging level " + debug_level);

			m_sscd = new StorageSOPClassSCPDispatcher(m_ae.m_recvPort,
					m_ae.m_title, new File(store_fold),
					new StoredFilePathStrategyHashSubFolders(), roh == null
							? getDefaultReceiveObjectHandler()
							: roh, debug_level);
			new Thread(m_sscd).start();
		} catch (Exception e)
		{
			String msg = "Exception creating SCP service: " + e.getMessage();
			Utils.logger.error("Exception creating SCP service", e);
			return msg;
		}
		return null;
	}
	private static ReceivedObjectHandler getDefaultReceiveObjectHandler()
	{
		return new ReceivedObjectHandler()
		{
			@Override
			public void sendReceivedObjectIndication(String fileName,
					String transferSyntax, String callingAETitle)
					throws DicomNetworkException, DicomException, IOException
			{
				ConsoleView.AppendMessage("\nDICOM file received: " + fileName);
			}
		};
	}
	public static void stop()
	{
		if (m_sscd != null)
			m_sscd.shutdown();
		m_sscd = null;
		m_ae = null;
	}
}
