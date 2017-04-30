/**
 * 
 */
package com.asksven.android.common;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asksven.betterbatterystats.R;

/**
 * @author sven From
 *         http://www.androidsnippets.com/prompt-engaged-users-to-rate-your-app-in-the-android-market-appirater
 * Make sure to add following to strings.xml
 *    <string name="app_name">AndroidCommon</string>
 *    <string name="app_pname">com.asksven.androidcommon</string>
 *    <string name="label_button_remind">Remind me later</string>
 *    <string name="label_button_rate">Rate</string>
 *    <string name="label_button_no<_thanks">No, thanks</string>
 *    <string name="text_dialog_rate">If you enjoy using %s, please take a moment to rate it. Thanks for your support!</string>
 * 
 * To test it and to tweak the dialog appearence, you can call AppRater.showRateDialog(this, null)
 * from your Activity.
 * 
 * Normal use is to invoke AppRater.app_launched(this) each time your activity is invoked
 * (eg. from within the onCreate method). If all conditions are met, the dialog appears.
 */
public class AppRater
{
	private final static int DAYS_UNTIL_PROMPT = 3;
	private final static int LAUNCHES_UNTIL_PROMPT = 7;

	public static void app_launched(Context ctx)
	{
		SharedPreferences prefs = ctx.getSharedPreferences("apprater", 0);
		
		SharedPreferences.Editor editor = prefs.edit();
		
		if (prefs.getBoolean("dontshowagain", false))
		{
			return;
		}

		

		// Increment launch counter
		long launch_count = prefs.getLong("launch_count", 0) + 1;
		editor.putLong("launch_count", launch_count);

		// Get date of first launch
		Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
		if (date_firstLaunch == 0)
		{
			date_firstLaunch = System.currentTimeMillis();
			editor.putLong("date_firstlaunch", date_firstLaunch);
		}

		// Wait at least n days before opening
		if (launch_count >= LAUNCHES_UNTIL_PROMPT)
		{
			if (System.currentTimeMillis() >= date_firstLaunch + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000))
			{
				showRateDialog(ctx, editor);
			}
		}

		editor.commit();
	}

	public static void showRateDialog(final Context ctx, final SharedPreferences.Editor editor)
	{
		
		final Dialog dialog = new Dialog(ctx);
    	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	dialog.setContentView(R.layout.dialog_rate);
    	
    	TextView dialogTitle = (TextView) dialog.findViewById(R.id.dialog_title);
    	dialogTitle.setText(ctx.getString(R.string.label_button_rate) + " " + ctx.getString(R.string.app_name));
    	
    	Button buttonRate = (Button) dialog.findViewById(R.id.buttonRate);
    	buttonRate.setText(ctx.getString(R.string.label_button_rate));
    	buttonRate.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View v)
			{
				ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ctx.getString(R.string.app_pname))));
				if (editor != null)
				{
					editor.putBoolean("dontshowagain", true);
					editor.commit();
				}
				dialog.dismiss();
			}
		});
    	
    	Button buttonRemind = (Button) dialog.findViewById(R.id.buttonRemind);
    	buttonRemind.setText(R.string.label_button_remind);
    	buttonRemind.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View v)
			{
				dialog.dismiss();
			}
		});
    	
    	Button buttonNoThanks = (Button) dialog.findViewById(R.id.buttonNoThanks);
    	buttonNoThanks.setText(R.string.label_button_no_thanks);
    	buttonNoThanks.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View v)
			{
				if (editor != null)
				{
					editor.putBoolean("dontshowagain", true);
					editor.commit();
				}
				dialog.dismiss();
			}
		});
    	
		dialog.show();
	}
}
