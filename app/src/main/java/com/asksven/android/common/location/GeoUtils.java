/**
 * 
 */
package com.asksven.android.common.location;

import java.util.List;

import com.asksven.android.common.networkutils.DataNetwork;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

/**
 * Helper class for retrieing geo data for a given location
 * As geo data requires data connection the call is only made
 * if proper data connectivity exists
 * @author sven
 *
 */
public class GeoUtils
{
	private static final String TAG = "GeoUtils";

	public static String getNearestAddress(Context ctx, Location loc)
	{
		if (!DataNetwork.hasDataConnection(ctx))
		{
			return "";
		}
		
		Address address = getGeoData(ctx, loc);
		String strRet = "";
		if (address != null)
		{	
			String addr0 = address.getAddressLine(0);
			String addr1 = address.getAddressLine(1);
			if (!addr0.equals(""))
			{
				strRet = addr0;
			}
			if (!addr1.equals(""))
			{
				if (!strRet.equals(""))
				{
					strRet = strRet + ", ";
				}
				strRet = strRet + addr1;
			}
	    }
	    return strRet;

	}
	public static String getNearestCity(Context ctx, Location loc)
	{
		if (!DataNetwork.hasDataConnection(ctx))
		{
			return "";
		}

		Address address = getGeoData(ctx, loc);
		String strRet = "";
		if (address != null)
		{	
			strRet = address.getLocality();
	    }
	    return strRet;
	}

	public static Address getGeoData(Context ctx, Location loc)
	{
		if (!DataNetwork.hasDataConnection(ctx))
		{
			return null;
		}

		Geocoder myGeocoder = new Geocoder(ctx);
		Address address = null;
		try
		{
			List<Address> list = myGeocoder.getFromLocation(
					loc.getLatitude(),
					loc.getLongitude(), 1);
	        if (list != null & list.size() > 0)
	        {
	            address = list.get(0);
	        }
		}
		catch (Exception e)
		{
			Log.e(TAG, "Failed while retrieving nearest city");
		}
	    return address;
	}
}
