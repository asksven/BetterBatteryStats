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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.noshufou.android.su.util.Util;

public class AppDetailsActivity extends FragmentActivity {
    private static final String TAG = "Su.AppDetailsActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE
                && config.screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_app_details);
        if (savedInstanceState == null) {
            AppDetailsFragment detailsFragment = new AppDetailsFragment();
            detailsFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, detailsFragment).commit();
        }
    }

    public void goHome(View view) {
        Log.d(TAG, "Home button pressed");
        Util.goHome(this);
    }
}
