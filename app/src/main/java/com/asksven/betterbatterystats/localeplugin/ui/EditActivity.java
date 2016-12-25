///*
// * Copyright (C) 2012 asksven
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// * This file was contributed by two forty four a.m. LLC <http://www.twofortyfouram.com>
// * unter the terms of the Apache License, Version 2.0
// */
//
//package com.asksven.betterbatterystats.localeplugin.ui;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.content.Intent;
//import android.content.pm.PackageManager.NameNotFoundException;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.widget.CheckBox;
//import android.widget.Spinner;
//import android.widget.Toast;
//
//import com.asksven.betterbatterystats.adapters.ReferencesAdapter;
//import com.asksven.betterbatterystats.localeplugin.Constants;
//import com.asksven.betterbatterystats.localeplugin.bundle.BundleScrubber;
//import com.asksven.betterbatterystats.localeplugin.bundle.PluginBundleManager;
//import com.twofortyfouram.locale.BreadCrumber;
//import com.asksven.betterbatterystats.R;
//
///**
// * This is the "Edit" activity for a Locale Plug-in.
// */
//public final class EditActivity extends Activity
//{
//
//	static final String TAG = "EditActivity";
//    /**
//     * Help URL, used for the {@link com.twofortyfouram.locale.platform.R.id#twofortyfouram_locale_menu_help} menu item.
//     */
//    private static final String HELP_URL = "http://blog.asksven.org"; //$NON-NLS-1$
//
//    /**
//     * Flag boolean that can only be set to true via the "Don't Save"
//     * {@link com.twofortyfouram.locale.platform.R.id#twofortyfouram_locale_menu_dontsave} menu item in
//     * {@link #onMenuItemSelected(int, MenuItem)}.
//     * <p>
//     * If true, then this {@code Activity} should return {@link Activity#RESULT_CANCELED} in {@link #finish()}.
//     * <p>
//     * If false, then this {@code Activity} should generally return {@link Activity#RESULT_OK} with extras
//     * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} and {@link com.twofortyfouram.locale.Intent#EXTRA_STRING_BLURB}.
//     * <p>
//     * There is no need to save/restore this field's state when the {@code Activity} is paused.
//     */
//    private boolean mIsCancelled = false;
//
//    private ReferencesAdapter m_spinnerAdapter;
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    protected void onCreate(final Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//
//        /*
//         * A hack to prevent a private serializable classloader attack
//         */
//        BundleScrubber.scrub(getIntent());
//        BundleScrubber.scrub(getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE));
//
//        setContentView(R.layout.locale_plugin_main);
//
//        if (Build.VERSION.SDK_INT >= 11)
//        {
//            CharSequence callingApplicationLabel = null;
//            try
//            {
//                callingApplicationLabel = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getCallingPackage(), 0));
//            }
//            catch (final NameNotFoundException e)
//            {
//                if (Constants.IS_LOGGABLE)
//                {
//                    Log.e(Constants.LOG_TAG, "Calling package couldn't be found", e); //$NON-NLS-1$
//                }
//            }
//            if (null != callingApplicationLabel)
//            {
//                setTitle(callingApplicationLabel);
//            }
//        }
//        else
//        {
//            setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(), getString(R.string.plugin_name)));
//        }
//
//        // populate the spinner
//		Spinner spinner = (Spinner) findViewById(R.id.spinnerStatType);
//		m_spinnerAdapter = new ReferencesAdapter(this, android.R.layout.simple_spinner_item);
//		m_spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//		spinner.setAdapter(m_spinnerAdapter);
//
//        /*
//         * if savedInstanceState is null, then then this is a new Activity instance and a check for EXTRA_BUNDLE is needed
//         */
//        if (null == savedInstanceState)
//        {
//            final Bundle forwardedBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
//
//            if (PluginBundleManager.isBundleValid(forwardedBundle))
//            {
//            	// PluginBundleManager.isBundleValid must be changed if elements are added to the bundle
//            	//
//                ((CheckBox) findViewById(R.id.CheckBoxSaveRef)).setChecked(forwardedBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_REF));
//                ((CheckBox) findViewById(R.id.CheckBoxSaveStat)).setChecked(forwardedBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_STAT));
//                ((CheckBox) findViewById(R.id.CheckBoxSaveStatJson)).setChecked(forwardedBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_JSON));
//
////                ((Spinner) findViewById(R.id.spinnerStatType)).setSelection(forwardedBundle.getInt(PluginBundleManager.BUNDLE_EXTRA_INT_POSITION));
//                Log.i(TAG, "Retrieved from Bundle: "
//                		+", " + forwardedBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_REF)
//                		+", " + forwardedBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_STAT)
//                		+ ", " + forwardedBundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_REF_NAME));
//            }
//        }
//
//
//        /*
//         * if savedInstanceState isn't null, there is no need to restore any Activity state directly via onSaveInstanceState(), as
//         * the EditText object handles that automatically
//         */
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void finish()
//    {
//        if (mIsCancelled)
//        {
//            setResult(RESULT_CANCELED);
//        }
//        else
//        {
//            final boolean saveRef = ((CheckBox) findViewById(R.id.CheckBoxSaveRef)).isChecked();
//            final boolean saveStat = ((CheckBox) findViewById(R.id.CheckBoxSaveStat)).isChecked();
//            final boolean saveStatJson = ((CheckBox) findViewById(R.id.CheckBoxSaveStatJson)).isChecked();
//
//            int pos = ((Spinner) findViewById(R.id.spinnerStatType)).getSelectedItemPosition();
//            final String ref = m_spinnerAdapter.getItemName(pos);
//
//            /*
//             * This is the result Intent to Locale
//             */
//            final Intent resultIntent = new Intent();
//
//            /*
//             * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note that anything
//             * placed in this Bundle must be available to Locale's class loader. So storing String, int, and other standard
//             * objects will work just fine. However Parcelable objects must also be Serializable. And Serializable objects
//             * must be standard Java objects (e.g. a private subclass to this plug-in cannot be stored in the Bundle, as
//             * Locale's classloader will not recognize it).
//             */
//            final Bundle resultBundle = new Bundle();
//            resultBundle.putInt(PluginBundleManager.BUNDLE_EXTRA_INT_VERSION_CODE, Constants.getVersionCode(this));
//            resultBundle.putBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_REF, saveRef);
//            resultBundle.putBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_STAT, saveStat);
//            resultBundle.putBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_JSON, saveStatJson);
//            resultBundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_REF_NAME, ref);
//
//            Log.i(TAG, "Saved Bundle: " + resultBundle.toString());
//
//            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
//
//            // add text for display in tasker
//            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, "Stat Type: " + ref);
//
//            setResult(RESULT_OK, resultIntent);
//        }
//
//        super.finish();
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public boolean onCreateOptionsMenu(final Menu menu)
//    {
//        super.onCreateOptionsMenu(menu);
//
//        /*
//         * inflate the default menu layout from XML
//         */
//        getMenuInflater().inflate(com.twofortyfouram.locale.platform.R.menu.twofortyfouram_locale_help_save_dontsave, menu);
//
//        /*
//         * Set up the breadcrumbs for the ActionBar
//         */
//        if (Build.VERSION.SDK_INT >= 11)
//        {
//            /*
//             * Lazily instantiated class ensures compatibility with Dalvik on Android 1.6 devices
//             */
//            new Runnable()
//            {
//                @SuppressLint("NewApi")
//				public void run()
//                {
//                    getActionBar().setSubtitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(), getString(R.string.plugin_name)));
//                }
//            }.run();
//        }
//        /*
//         * Dynamically load the home icon from the host package for Ice Cream Sandwich or later. Note that this leaves Honeycomb
//         * devices without the host's icon in the ActionBar, but eventually all Honeycomb devices should receive an OTA to Ice
//         * Cream Sandwich so this problem will go away.
//         */
//        if (Build.VERSION.SDK_INT >= 14)
//        {
//            /*
//             * Lazily instantiated class ensures compatibility with Dalvik on Android 1.6 devices
//             */
//            new Runnable()
//            {
//                @SuppressLint("NewApi")
//				public void run()
//                {
//                    getActionBar().setDisplayHomeAsUpEnabled(true);
//
//                    /*
//                     * Note: There is a small TOCTOU error here, in that the host could be uninstalled right after launching the
//                     * plug-in. That would cause getApplicationIcon() to return the default application icon. It won't fail, but
//                     * it will return an incorrect icon.
//                     *
//                     * In practice, the chances that the host will be uninstalled while the plug-in UI is running are very slim.
//                     */
//                    try
//                    {
//                        getActionBar().setIcon(getPackageManager().getApplicationIcon(getCallingPackage()));
//                    }
//                    catch (final NameNotFoundException e)
//                    {
//                        if (Constants.IS_LOGGABLE)
//                        {
//                            Log.w(Constants.LOG_TAG, "An error occurred loading the host's icon", e); //$NON-NLS-1$
//                        }
//                    }
//                }
//            }.run();
//        }
//
//        return true;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public boolean onMenuItemSelected(final int featureId, final MenuItem item)
//    {
//        final int id = item.getItemId();
//
//        /*
//         * Royal pain in the butt to support the home button in SDK 11's ActionBar
//         */
//        if (Build.VERSION.SDK_INT >= 11)
//        {
//            try
//            {
//                if (id == android.R.id.class.getField("home").getInt(null)) //$NON-NLS-1$
//                {
//                    finish();
//                    return true;
//                }
//            }
//            catch (final NoSuchFieldException e)
//            {
//                // this should never happen under API 11 or greater
//                throw new RuntimeException(e);
//            }
//            catch (final IllegalAccessException e)
//            {
//                // this should never happen under API 11 or greater
//                throw new RuntimeException(e);
//            }
//        }
//
//        if (id == com.twofortyfouram.locale.platform.R.id.twofortyfouram_locale_menu_help)
//        {
//            try
//            {
//                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(HELP_URL)));
//            }
//            catch (final Exception e)
//            {
//                Toast.makeText(getApplicationContext(), com.twofortyfouram.locale.platform.R.string.twofortyfouram_locale_application_not_available, Toast.LENGTH_LONG).show();
//            }
//
//            return true;
//        }
//        else if (id == com.twofortyfouram.locale.platform.R.id.twofortyfouram_locale_menu_dontsave)
//        {
//            mIsCancelled = true;
//            finish();
//            return true;
//        }
//        else if (id == com.twofortyfouram.locale.platform.R.id.twofortyfouram_locale_menu_save)
//        {
//            finish();
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//
//
//}