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

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.noshufou.android.su.preferences.PreferencesActivity;
import com.noshufou.android.su.util.Util;

public class AppListActivity extends FragmentActivity {
    private static final String TAG = "Superuser";
    
    private static final int MENU_PREFERENCES = 1;
    TransitionDrawable mTitleLogo = null;
    
    private boolean mDualPane = false;
    private Handler mHandler;

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
        default:
            return super.onOptionsItemSelected(item);
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

}
