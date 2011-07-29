/*******************************************************************************
 * Copyright (c) 2011 Adam Shanks (ChainsDD)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.noshufou.android.su;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.noshufou.android.su.preferences.PreferencesActivity;
import com.noshufou.android.su.util.Util;

public class AppListActivity extends FragmentActivity {
    private static final String TAG = "Su.AppListActivity";
    
    TransitionDrawable mTitleLogo = null;
    
    List<Intent> mMaliciousAppsIntents;
    
    private boolean mDualPane = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "AppListActivity, onCreate()");
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_app_list);
        ImageView titleLogo = (ImageView) findViewById(Build.VERSION.SDK_INT<11?R.id.title_logo:android.R.id.home);
        mTitleLogo = (TransitionDrawable) titleLogo.getDrawable();
        
        if (findViewById(R.id.fragment_container) != null) {
            mDualPane = true;
        }
        
        if (savedInstanceState == null && mDualPane) {
            Log.d(TAG, "AppListActivity, savedInstanceState was null");
            gotoLog(null);
        }
        
        new EliteCheck().execute();
        new MaliciousAppCheck().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_prefs:
            gotoPrefs(null);
            return true;
        case R.id.menu_extras:
            gotoExtras(null);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int nextIntent = requestCode + 1;
        if (nextIntent < mMaliciousAppsIntents.size()) {
            startActivityForResult(mMaliciousAppsIntents.get(nextIntent), nextIntent);
        }
    }

    public void clearLog(View view) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof FragmentWithLog) {
            ((FragmentWithLog)fragment).clearLog();
        }
    }
    
    public void showDetails(long id) {
        if (mDualPane) {
            AppDetailsFragment detailsFragment = AppDetailsFragment.newInstance(id);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.fragment_container, detailsFragment);
            transaction.commit();
        } else {
            Intent intent = new Intent();
            intent.setClass(this, AppDetailsActivity.class);
            intent.putExtra("index", id);
            startActivity(intent);
        }
    }
    
    public void gotoLog(View view) {
        if (mDualPane) {
            Fragment fragment = new LogFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        } else {
            final Intent intent = new Intent(this, LogActivity.class);
            startActivity(intent);
        }
    }
    
    public void gotoPrefs(View view) {
//        if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB) {
            startActivity(new Intent(this, PreferencesActivity.class));
//        } else {
//            startActivity(new Intent(this, PreferencesActivityHC.class));
//        }
    }
    
    public void gotoExtras(View view) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.noshufou.android.su.elite",
                "com.noshufou.android.su.elite.FeaturedAppsActivity"));
        startActivity(intent);
    }
    
    private class EliteCheck extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            return Util.elitePresent(AppListActivity.this, false, 0);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mTitleLogo.startTransition(1000);
            }
        }
        
    }
    
    private class MaliciousAppCheck extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... params) {
            return Util.findMaliciousPackages(getApplicationContext());
        }

        @Override
        protected void onPostExecute(final List<String> result) {
            if (!result.isEmpty()) {
                LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.dialog_malicious_apps, null);
                ((TextView)layout.findViewById(R.id.message1)).setText(
                        getResources().getQuantityText(R.plurals.malicious_apps_message1,
                                result.size()));
                LinearLayout apps = (LinearLayout) layout.findViewById(R.id.apps_layout);
                mMaliciousAppsIntents = new ArrayList<Intent>();
                PackageManager pm = getPackageManager();
                for (String s : result) {
                    Log.d(TAG, s);
                    TextView nameText = new TextView(AppListActivity.this);
                    nameText.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
                    String[] parts = s.split(":");
                    try {
                        String appName = pm.getApplicationLabel(
                                pm.getApplicationInfo(parts[0], 0)).toString();
                        nameText.setText(appName + " (" + parts[0] + ")");
                    } catch (NameNotFoundException e) {
                        nameText.setText(s);
                    }
                    TextView probText = new TextView(AppListActivity.this);
                    probText.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
                    probText.setPadding(10, 0, 0, 0);
                    switch (Integer.parseInt(parts[1])) {
                    case Util.MALICIOUS_UID:
                        probText.setText(R.string.malicious_app_uid);
                        break;
                    case Util.MALICIOUS_RESPOND:
                        probText.setText(R.string.malicious_app_respond);
                        break;
                    case Util.MALICIOUS_PROVIDER_WRITE:
                        probText.setText(R.string.malicious_app_write);
                        break;
                    }
                    apps.addView(nameText, new LayoutParams(
                            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    apps.addView(probText, new LayoutParams(
                            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    mMaliciousAppsIntents.add(new Intent(Intent.ACTION_DELETE,
                            Uri.parse("package:" + parts[0])));
                }

                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT > 10) {
                    builder = new AlertDialog.Builder(AppListActivity.this,
                            AlertDialog.THEME_HOLO_DARK);
                } else {
                    builder = new AlertDialog.Builder(AppListActivity.this);
                }
                
                builder.setTitle(R.string.malicious_apps_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setView(layout)
                    .setCancelable(false)
                    .setPositiveButton(R.string.malicious_apps_uninstall,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivityForResult(mMaliciousAppsIntents.get(0), 0);
                                }
                            })
                    .setNegativeButton(R.string.malicious_apps_ignore, null) 
                    .show();
            }
        }
        
    }
}
