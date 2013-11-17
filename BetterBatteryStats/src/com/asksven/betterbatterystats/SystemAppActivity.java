package com.asksven.betterbatterystats;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.asksven.android.common.utils.SystemAppInstaller;
import com.asksven.android.common.utils.SystemAppInstaller.Status;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
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
	final static String APK = "com.asksven.betterbatterystats";
	
	Object m_stats = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_systemapp);

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
					if (!SystemAppInstaller.isSystemApp(APK))
					{
						status = SystemAppInstaller.install(APK);
					}
					else
					{
						status = SystemAppInstaller.uninstall(APK);
					}
						
					setButtonText(buttonRemount);
				
					if (status.getSuccess())
					{
						Toast.makeText(SystemAppActivity.this, "Succeeded", Toast.LENGTH_LONG).show();
						// prepare the alert box
			            AlertDialog.Builder alertbox = new AlertDialog.Builder(SystemAppActivity.this);
			 
			            // set the message to display
			            alertbox.setMessage("Installed as system app. Please reboot to activate.");
			 
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
		if (SystemAppInstaller.isSystemApp(APK))
		{
			button.setText("Installed as system app");
		}
		else
		{
			button.setText("Not installed as system app");
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

}
