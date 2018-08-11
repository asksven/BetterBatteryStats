/*
 * Copyright (C) 2012 asksven
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

package com.asksven.betterbatterystats.localeplugin.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.ReferencesAdapter;
import com.asksven.betterbatterystats.localeplugin.Constants;
import com.asksven.betterbatterystats.localeplugin.bundle.PluginBundleManager;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;

/**
 * This is the "Edit" activity for a Locale Plug-in.
 */
public final class EditActivity extends AbstractAppCompatPluginActivity
{

    static final String TAG = "EditActivity";

    private static final String HELP_URL = "https://better.asksven.org/"; //$NON-NLS-1$


    private ReferencesAdapter m_spinnerAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.locale_plugin_main);

        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        CharSequence callingApplicationLabel = null;
        try
        {
            callingApplicationLabel =
                    getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getCallingPackage(),
                            0));
        } catch (final PackageManager.NameNotFoundException e)
        {
            Log.e(TAG, "Calling package couldn't be found %s", e); //$NON-NLS-1$
        }
        if (null != callingApplicationLabel)
        {
            setTitle(callingApplicationLabel);
        }

        getSupportActionBar().setSubtitle(R.string.plugin_name);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // populate the spinner
        Spinner spinner = (Spinner) findViewById(R.id.spinnerStatType);
        m_spinnerAdapter = new ReferencesAdapter(this, android.R.layout.simple_spinner_item);
        m_spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(m_spinnerAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        getMenuInflater().inflate(R.menu.locale_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (android.R.id.home == item.getItemId())
        {
            finish();
        } else if (R.id.menu_discard_changes == item.getItemId())
        {
            // Signal to AbstractAppCompatPluginActivity that the user canceled.
            mIsCancelled = true;
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean isBundleValid(@NonNull Bundle bundle)
    {
        return PluginBundleManager.isBundleValid(bundle);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle, @NonNull final String previousBlurb)
    {
        if (PluginBundleManager.isBundleValid(previousBundle))
        {
            ((CheckBox) findViewById(R.id.CheckBoxSaveRef)).setChecked(previousBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_REF));
            ((CheckBox) findViewById(R.id.CheckBoxSaveStat)).setChecked(previousBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_STAT));

            Spinner mySpinner = (Spinner) findViewById(R.id.spinnerStatType);

            mySpinner.setSelection(getIndex(mySpinner, previousBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_REF_NAME)));

            Log.i(TAG, "Retrieved from Bundle: "
                    + ", " + previousBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_REF)
                    + ", " + previousBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_STAT)
                    + ", " + previousBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_REF_NAME));
        }
    }

    @Override
    @Nullable
    public Bundle getResultBundle()
    {
        final Bundle resultBundle = new Bundle();

        final boolean saveRef = ((CheckBox) findViewById(R.id.CheckBoxSaveRef)).isChecked();
        final boolean saveStat = ((CheckBox) findViewById(R.id.CheckBoxSaveStat)).isChecked();

        int pos = ((Spinner) findViewById(R.id.spinnerStatType)).getSelectedItemPosition();
        final String ref = m_spinnerAdapter.getItemLabel(pos);

		/*
		 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note that anything
		 * placed in this Bundle must be available to Locale's class loader. So storing String, int, and other standard
		 * objects will work just fine. However Parcelable objects must also be Serializable. And Serializable objects
		 * must be standard Java objects (e.g. a private subclass to this plug-in cannot be stored in the Bundle, as
		 * Locale's classloader will not recognize it).
		 */
        resultBundle.putInt(PluginBundleManager.BUNDLE_EXTRA_INT_VERSION_CODE, Constants.getVersionCode(this));
        resultBundle.putBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_REF, saveRef);
        resultBundle.putBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_STAT, saveStat);
        resultBundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_REF_NAME, ref);

        Log.i(TAG, "Saved Bundle: " + resultBundle.toString());

        return resultBundle;
    }

    @Override
    @NonNull
    public String getResultBlurb(@NonNull final Bundle bundle)
    {
        return "";
    }


    //private method of your class
    private int getIndex(Spinner spinner, String myString)
    {
        int index = 0;

        for (int i = 0; i < spinner.getCount(); i++)
        {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString))
            {
                index = i;
                break;
            }
        }
        return index;
    }
}