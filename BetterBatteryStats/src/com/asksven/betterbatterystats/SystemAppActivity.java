package com.asksven.betterbatterystats;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import com.asksven.android.common.utils.SysUtils;
import com.asksven.android.common.utils.SystemAppInstaller;
import com.asksven.android.common.utils.SystemAppInstaller.Status;
import com.asksven.betterbatterystats.R;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SystemAppActivity extends Activity
{

	final static String TAG = "BetteryInfoTest.MainActivity";
//	final static String APK = "com.asksven.betterbatterystats_debug.apk";
	final static String BBS_SIGNED_APK 	= "com.asksven.betterbatterystats_signed.apk"; 
	final static String BBS_DEBUG_APK 	= "com.asksven.betterbatterystats_debug.apk";
	final static String BBS_XDA_APK		= "com.asksven.betterbatterystats_xdaedition.apk";

	String systemAPKName = "";

	final static String PACKAGE = "com.asksven.betterbatterystats";

	
	Object m_stats = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_systemapp);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);

		if ( !rootEnabled )
		{
			// show message that data is not available
			// prepare the alert box
            AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
 
            // set the message to display
            alertbox.setMessage("In order to use this feature 'Root Features must be enabled (Advanced Preferences)");
 
            // add a neutral button to the alert box and assign a click listener
            alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener()
            {
 
                // click listener on the alert box
                public void onClick(DialogInterface arg0, int arg1)
                {
                	setResult(RESULT_OK);
                	finish();
                }
            });
 
            // show it
            alertbox.show();

		}

		// package name is either com.asksven.betterbatterystats or com.asksven.betterbatterystats_xdaedition 
		String packageName = getPackageName();
		
		// now we also need to find out if this build was signed with the debug key
		boolean debug = isDebuggable(this);
		
		// determine the name of the APK to install from assets
		// if package name does not contain xdaedition
		//   if package is signed with a debug key
		//     use BBS_SIGNED_APK
		//   else
		//     use BBS_DEBUG_APK
		// else
		//    use BBS_XDA_APK
		if (!packageName.contains("xdaedition"))
		{
			if (!debug)
			{
				systemAPKName = BBS_SIGNED_APK;
			}
			else
			{
				systemAPKName = BBS_DEBUG_APK;
			}
		}
		else
		{
			systemAPKName = BBS_XDA_APK;
		}
		
		Log.i(TAG, "SystemAPKName = " + systemAPKName);
		
		final TextView permBattery = (TextView) findViewById(R.id.textViewPermBATTERY_STATS);
		if (hasBatteryStatsPermission(this))
		{
			permBattery.setText("BATTERY_STATS Granted");
		}
		else
		{
			permBattery.setText("BATTERY_STATS  not granted");
		}

		final TextView permDump = (TextView) findViewById(R.id.textViewPermDUMP);
		if (hasDumpPermission(this))
		{
			permDump.setText("DUMP Granted");
		}
		else
		{
			permDump.setText("DUMP  not granted");
		}

		final Button buttonRemount = (Button) findViewById(R.id.button2);
		setButtonText(buttonRemount);

		buttonRemount.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Status status;
				try
				{
					boolean install = !SystemAppInstaller.isSystemApp(systemAPKName); 
					if (install)
					{
						status = SystemAppInstaller.install(SystemAppActivity.this, systemAPKName);
					}
					else
					{
						status = SystemAppInstaller.uninstall(PACKAGE);
					}
						
					setButtonText(buttonRemount);
				
					if (status.getSuccess())
					{
						Toast.makeText(SystemAppActivity.this, "Succeeded", Toast.LENGTH_LONG).show();
						// prepare the alert box
			            AlertDialog.Builder alertbox = new AlertDialog.Builder(SystemAppActivity.this);
			 
			            // set the message to display
			            if (install)
			            {
			            	alertbox.setMessage("Installed as system app. Please reboot to activate.\n\nMake sure to uninstall the system app before uninstalling BetterBatteryStats. ");
			            }
			            else
			            {
			            	alertbox.setMessage("Uninstalled as system app. Please reboot to clean up.");
			   			 
			            }
			            // add a neutral button to the alert box and assign a click listener
			            alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener()
			            {
			 
			                // click listener on the alert box
			                public void onClick(DialogInterface arg0, int arg1)
			                {
			                }
			            });
			 
			            // show it
			            alertbox.show();

					}
					else
					{
						Toast.makeText(SystemAppActivity.this, "Failed", Toast.LENGTH_LONG).show();
						Log.e(TAG,"History: " + status.toString());
					}						
				}
				catch (Exception e)
				{
					Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
					Toast.makeText(SystemAppActivity.this, "Failed", Toast.LENGTH_LONG).show();
				}
				
				// refresh status of button
				setButtonText(buttonRemount);

			}
		});

	}

	void setButtonText(Button button)
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
		if (!rootEnabled) return;

		if (SystemAppInstaller.isSystemApp(systemAPKName))
		{
			button.setText("Uninstall system app");
		}
		else
		{
			button.setText("Install as system app");
		}
	}
		
	private boolean hasBatteryStatsPermission(Context context)
	{
		return wasPermissionGranted(context, android.Manifest.permission.BATTERY_STATS);
	}

	private boolean hasDumpPermission(Context context)
	{
		return wasPermissionGranted(context, android.Manifest.permission.DUMP);
	}

	private boolean wasPermissionGranted(Context context, String permission)
	{
		PackageManager pm = context.getPackageManager();
		int hasPerm = pm.checkPermission(
		    permission, 
		    context.getPackageName());
		return (hasPerm == PackageManager.PERMISSION_GRANTED);
	}
	
	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
	private boolean isDebuggable(Context ctx)
	{
	    boolean debuggable = false;

	    try
	    {
	        PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),PackageManager.GET_SIGNATURES);
	        Signature signatures[] = pinfo.signatures;

	        CertificateFactory cf = CertificateFactory.getInstance("X.509");

	        for ( int i = 0; i < signatures.length;i++)
	        {   
	            ByteArrayInputStream stream = new ByteArrayInputStream(signatures[i].toByteArray());
	            X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);       
	            debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
	            if (debuggable)
	                break;
	        }
	    }
	    catch (NameNotFoundException e)
	    {
	        //debuggable variable will remain false
	    }
	    catch (CertificateException e)
	    {
	        //debuggable variable will remain false
	    }
	    return debuggable;
	}

}
