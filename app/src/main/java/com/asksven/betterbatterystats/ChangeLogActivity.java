/*
 * Copyright (C) 2011-2015 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterbatterystats;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.asksven.betterbatterystats.adapters.ChangeLogAdapter;
import com.asksven.betterbatterystats.adapters.CreditsAdapter;

public class ChangeLogActivity extends ActionBarListActivity
{

    private static final String TAG = "ChangeLogActivity";
    
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelog);
        
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.label_changelog));


	    setSupportActionBar(toolbar);
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    getSupportActionBar().setDisplayUseLogoEnabled(false);
	    
        ChangeLogAdapter adapter = new ChangeLogAdapter(this);
        setListAdapter(adapter);

        if (adapter.getReleaseName().equals(""))
        {
            toolbar.setTitle(getString(R.string.label_changelog));
        }
        else
        {
            toolbar.setTitle(getString(R.string.label_changelog) + " (" + adapter.getReleaseName() + ")");
        }

        

    }   
    
}