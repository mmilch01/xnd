package org.nrg.xdat.webservices;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;

public class CheckUserSession extends WSTool
{

	public CheckUserSession()
	{
		super();
	}

	@Override
	public void displayHelp()
	{
	}

	@Override
	public boolean process()
	{

		try
		{
			String service_session = createServiceSession();
			System.out.println(service_session);
			closeServiceSession(service_session);
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
		}
		return true;
	}
}
