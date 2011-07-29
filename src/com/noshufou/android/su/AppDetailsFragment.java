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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.noshufou.android.su.preferences.Preferences;
import com.noshufou.android.su.provider.PermissionsProvider.Apps;
import com.noshufou.android.su.provider.PermissionsProvider.Logs;
import com.noshufou.android.su.service.LogService;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.widget.BetterPopupWindow;
import com.noshufou.android.su.widget.LogAdapter;
import com.noshufou.android.su.widget.PinnedHeaderListView;

public class AppDetailsFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<Cursor>, FragmentWithLog, OnClickListener {
    private static final String TAG = "Su.AppDetailsFragment";

    private TextView mAppName = null;
    private ImageView mAppIcon = null;
    private ImageView mStatusIcon = null;
    private TextView mPackageNameText = null;
    private TextView mAppUidText = null;
    private TextView mRequestDetailText = null;
    private TextView mCommandText = null;
    private TextView mStatusText = null;
    
    private boolean mUseAppSettings = true;
    private boolean mNotificationsEnabled = true;
    private boolean mLoggingEnabled = true;
    
    private Button mToggleButton = null;
    
    private static final int DETAILS_LOADER = 1;
    private static final int LOG_LOADER = 2;
    
    private long mShownIndex = -1;
    
    private boolean mReady = false;
    private boolean mDualPane = false;
    
    private int mAllow = -1;
    
    LogAdapter mAdapter = null;
    
    public static AppDetailsFragment newInstance(long index) {
        Log.d(TAG, "AppDetailsFragment, newInstance()");
        AppDetailsFragment fragment = new AppDetailsFragment();
        
        Bundle args = new Bundle();
        args.putLong("index", index);
        fragment.setArguments(args);
        
        return fragment;
    }
    
    public long getShownIndex() {
        Log.d(TAG, "AppDetailsFragment, getShownIndex()");
        return mShownIndex;
    }
    
    public void setShownIndex(long index) {
        Log.d(TAG, "AppDetailsFragment, setShownIndex()");
        mShownIndex = index;
        getLoaderManager().restartLoader(DETAILS_LOADER, null, this);
        getLoaderManager().restartLoader(LOG_LOADER, null, this);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "AppDetailsFragment, onCreateView()");
        Log.d(TAG, "AppDetailsFragment, mShownIndex = " + mShownIndex);
        if (container == null) {
            return null;
        }
        View view = inflater.inflate(R.layout.fragment_app_details, container, false);

        mAppName = (TextView) view.findViewById(R.id.app_name);
        mAppIcon = (ImageView) view.findViewById(R.id.app_icon);
        mStatusIcon = (ImageView) view.findViewById(R.id.status_icon);
        mPackageNameText = (TextView) view.findViewById(R.id.package_name);
        mAppUidText = (TextView) view.findViewById(R.id.app_uid);
        mRequestDetailText = (TextView) view.findViewById(R.id.request_detail);
        mCommandText = (TextView) view.findViewById(R.id.command);
        mStatusText = (TextView) view.findViewById(R.id.status);
        
        view.findViewById(R.id.toggle_button).setOnClickListener(this);
        view.findViewById(R.id.forget_button).setOnClickListener(this);
        view.findViewById(R.id.clear_log_button).setOnClickListener(this);
        if (Util.elitePresent(getActivity(), true, 2)) {
            View moreButton = view.findViewById(R.id.more_button);
            moreButton.setOnClickListener(this);
            moreButton.setVisibility(View.VISIBLE);
        }
        
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "AppDetailsFragment, onActivityCreated()");
        Log.d(TAG, "AppDetailsFragment, mShownIndex = " + mShownIndex);
        super.onActivityCreated(savedInstanceState);

        FrameLayout fragmentContainer = (FrameLayout) getActivity()
                .findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            mDualPane = true;
        }
        
        if (savedInstanceState != null && 
                savedInstanceState.containsKey("mShownIndex")) {
            Log.d(TAG, "AppDetailsFragment, Restoring savedInstanceState");
            Log.d(TAG, "AppDetailsFragment, mShownIndex = " + mShownIndex);
            mShownIndex = savedInstanceState.getLong("mShownIndex");
        } else {
            mShownIndex = getArguments().getLong("index", 0);
        }
        
        setupListView();
        getLoaderManager().initLoader(DETAILS_LOADER, null, this);
        getLoaderManager().initLoader(LOG_LOADER, null, this);
    }
    
    private void setupListView() {
        final ListView list = getListView();
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        
        list.setDividerHeight(0);
        
        mAdapter = new LogAdapter(null, getActivity(), false);
        setListAdapter(mAdapter);
        
        if (list instanceof PinnedHeaderListView &&
                mAdapter.getDisplaySectionHeadersEnabled()) {
            PinnedHeaderListView pinnedHeaderListView =
                (PinnedHeaderListView) list;
            View pinnedHeader = inflater.inflate(R.layout.recent_list_section, list, false);
            pinnedHeaderListView.setPinnedHeaderView(pinnedHeader);
        }
        
        list.setOnScrollListener(mAdapter);
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "AppDetailsFragment, onSaveIntanceState()");
        Log.d(TAG, "AppDetailsFragment, mShownIndex = " + mShownIndex);
        
        if (mShownIndex != -1) {
            outState.putLong("mShownIndex", mShownIndex);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
        case R.id.toggle_button:
                toggle(null);
            break;
        case R.id.forget_button:
                forget(null);
            break;
        case R.id.clear_log_button:
                clearLog();
            break;
        case R.id.more_button:
            Log.d(TAG, "more button clicked");
            MoreOptionsPopup popup = new MoreOptionsPopup(view);
            popup.show();
            break;
        }
    }
    
    public void toggle(View view) {
        if (!mReady) {
            return;
        }
        
        if (PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(Preferences.PIN, false)) {
            Intent intent = new Intent(getActivity(), PinActivity.class);
            intent.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_CHECK);
            startActivityForResult(intent, 0);
        } else {
            doToggle();
        }

    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (resultCode == Activity.RESULT_OK) {
            doToggle();
        }
    }

    private void doToggle() {
        ContentResolver cr = getActivity().getContentResolver();
        Uri uri = Uri.withAppendedPath(Apps.CONTENT_URI, String.valueOf(mShownIndex));

        ContentValues values = new ContentValues();
        values.put(Apps.ALLOW, mAllow == 1?0:1);
        cr.update(uri, values, null, null);
        
        // Update the log
        Intent intent = new Intent(getActivity(), LogService.class);
        intent.putExtra(LogService.EXTRA_ACTION, LogService.ADD_LOG);
        intent.putExtra(LogService.EXTRA_APP_ID, mShownIndex);
        intent.putExtra(LogService.EXTRA_ALLOW, Logs.LogType.TOGGLE);
        getActivity().startService(intent);
    }
    
    public void forget(View view) {
        if (!mReady) {
            return;
        }

        ContentResolver cr = getActivity().getContentResolver();
        Uri uri = Uri.withAppendedPath(Apps.CONTENT_URI, String.valueOf(mShownIndex));

        cr.delete(uri, null, null);
        closeDetails();
    }
    
    public void clearLog(View view) {
        clearLog();
    }
    
    @Override
    public void clearLog() {
        if (mShownIndex != -1) {
            getActivity().getContentResolver().delete(ContentUris.withAppendedId(Logs.CONTENT_URI, mShownIndex), null, null);
        }
    }

    public void closeDetails() {
        if (mDualPane) {
            Fragment logFragment = LogFragment.newInstance();
            FragmentTransaction transaction = ((FragmentActivity)getActivity())
                    .getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, logFragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.commit();
        } else {
            Util.goHome(getActivity());
        }
    }
    

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "AppDetailsFragment, onCreateLoader()");
        Log.d(TAG, "AppDetailsFragment, mShownIndex = " + mShownIndex);
        switch (id) {
        case DETAILS_LOADER:
            return new CursorLoader(getActivity(),
                    ContentUris.withAppendedId(Apps.CONTENT_URI, mShownIndex),
                    null, null, null, null);
        case LOG_LOADER:
            return new CursorLoader(getActivity(),
                    ContentUris.withAppendedId(Logs.CONTENT_URI, mShownIndex),
                    null, null, null, null);
        default:
            throw new IllegalArgumentException("Unknown Loader: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "AppDetailsFragment, onLoadFinished()");
        switch (loader.getId()) {
        case DETAILS_LOADER:
            Log.d(TAG, "AppDetailsFragment, DETAILS_LOADER finished");
            if (data.moveToFirst()) {
                Log.d(TAG, "AppDetailsFragment, details found, loading them");
                mAppName.setText(data.getString(data.getColumnIndex(Apps.NAME)));
                mAppIcon.setImageDrawable(
                        Util.getAppIcon(getActivity(), data.getInt(data.getColumnIndex(Apps.UID))));
                int allow = data.getInt(data.getColumnIndex(Apps.ALLOW));
                mStatusIcon.setImageDrawable(Util.getStatusIconDrawable(getActivity(), allow));
                mPackageNameText.setText(data.getString(data.getColumnIndex(Apps.PACKAGE)));
                mAppUidText.setText(data.getString(data.getColumnIndex(Apps.UID)));
                mRequestDetailText.setText(
                        Util.getUidName(getActivity(), data.getInt(data.getColumnIndex(Apps.EXEC_UID)), true));
                mCommandText.setText(data.getString(data.getColumnIndex(Apps.EXEC_CMD)));
                mStatusText.setText(allow==1?
                        R.string.allowed:R.string.denied);
                if (mToggleButton != null) {
                    mToggleButton.setText(allow==1?R.string.deny:R.string.allow);
                }
                mAllow = allow;
                
                String notificationsStr = data.getString(data.getColumnIndex(Apps.NOTIFICATIONS));
                String loggingStr = data.getString(data.getColumnIndex(Apps.LOGGING));
                if (notificationsStr == null && loggingStr == null) {
                    mUseAppSettings = true;
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    mNotificationsEnabled = prefs.getBoolean(Preferences.NOTIFICATIONS, true);
                    mLoggingEnabled = prefs.getBoolean(Preferences.LOGGING, true);
                } else {
                    mUseAppSettings = false;
                    mNotificationsEnabled = notificationsStr.equals("1")?true:false;
                    mLoggingEnabled = loggingStr.equals("1")?true:false;
                }
            }
            mReady = true;
            break;
        case LOG_LOADER:
            mAdapter.swapCursor(data);
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "AppDetailsFragment, onLoaderReset()");
        if (loader.getId() == LOG_LOADER) {
            mAdapter.swapCursor(null);
        }
    }
    
    private class MoreOptionsPopup extends BetterPopupWindow implements OnCheckedChangeListener {
        
        private CheckBox mUseAppCheckBox;
        private CheckBox mNotificationsCheckBox;
        private CheckBox mLoggingCheckBox;
        
        public MoreOptionsPopup(View anchor) {
            super(anchor);
        }

        @Override
        protected void onCreate() {
            LayoutInflater inflater =
                    (LayoutInflater) this.anchor.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            ViewGroup root = (ViewGroup) inflater.inflate(R.layout.more_options, null);

            mUseAppCheckBox = (CheckBox) root.findViewById(R.id.check_use_app);
            mUseAppCheckBox.setChecked(mUseAppSettings);
            
            mNotificationsCheckBox = (CheckBox)root.findViewById(R.id.check_notifications);
            mNotificationsCheckBox.setChecked(mNotificationsEnabled);
            mNotificationsCheckBox.setEnabled(!mUseAppSettings);
            
            mLoggingCheckBox = (CheckBox)root.findViewById(R.id.check_logging);
            mLoggingCheckBox.setChecked(mLoggingEnabled);
            mLoggingCheckBox.setEnabled(!mUseAppSettings);

            mUseAppCheckBox.setOnCheckedChangeListener(this);
            mNotificationsCheckBox.setOnCheckedChangeListener(this);
            mLoggingCheckBox.setOnCheckedChangeListener(this);

            this.setContentView(root);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            ContentResolver cr = getActivity().getContentResolver();
            if (buttonView.getId() == R.id.check_use_app) {
                mUseAppSettings = isChecked;
                mNotificationsCheckBox.setEnabled(!mUseAppSettings);
                mLoggingCheckBox.setEnabled(!mUseAppSettings);
                ContentValues values = new ContentValues();
                values.put(Apps.NOTIFICATIONS, isChecked?null:mNotificationsEnabled);
                values.put(Apps.LOGGING, isChecked?null:mLoggingEnabled);
                cr.update(ContentUris.withAppendedId(Apps.CONTENT_URI, mShownIndex),
                        values, null, null);
                if (isChecked) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    mNotificationsEnabled = prefs.getBoolean(Preferences.NOTIFICATIONS, true);
                    mNotificationsCheckBox.setChecked(mNotificationsEnabled);
                    mLoggingEnabled = prefs.getBoolean(Preferences.LOGGING, true);
                    mLoggingCheckBox.setChecked(mLoggingEnabled);
                }
            } else {
                ContentValues values = new ContentValues();
                values.put(Apps.NOTIFICATIONS, mNotificationsCheckBox.isChecked());
                values.put(Apps.LOGGING, mLoggingCheckBox.isChecked());
                cr.update(ContentUris.withAppendedId(Apps.CONTENT_URI, mShownIndex),
                        values, null, null);
            }
        }
    }

}
