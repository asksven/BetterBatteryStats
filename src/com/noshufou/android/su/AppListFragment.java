package com.noshufou.android.su;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.noshufou.android.su.preferences.Preferences;
import com.noshufou.android.su.provider.PermissionsProvider.Apps;
import com.noshufou.android.su.provider.PermissionsProvider.Logs;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.widget.AppListItem;
import com.noshufou.android.su.widget.PinnedHeaderListView;
import com.noshufou.android.su.widget.PinnedHeaderListView.PinnedHeaderCache;

public class AppListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
    private static final String TAG = "Superuser";
    
    private boolean mShowStatusIcons = true;
    private boolean mShowLogData = true;
    private String mStatusIconType = null;
    private int mPinnedHeaderBackgroundColor;
    private AppListAdapter mAdapter;
    private boolean mDualPane;
    private int mCurCheckPosition = -1;
    private LinearLayout mLoadingLayout = null;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "AppListFragment, onCreateView()");
        View view = inflater.inflate(R.layout.fragment_app_list, container, false);
        mLoadingLayout = (LinearLayout) view.findViewById(R.id.loading_layout);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "AppListFragment, onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        
        setupListView();
        getLoaderManager().initLoader(0, null, this);
        
        // Check for second pane
        View detailsFrame = getActivity().findViewById(R.id.fragment_container);
        mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        
        if (mDualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            if (savedInstanceState != null) {
                Log.d(TAG, "AppListFragment, Restoring savedInstanceState");
                mCurCheckPosition = savedInstanceState.getInt("curChoice", -1);
            } 
        }
        
    }
    
    @Override
    public void onResume() {
        Log.d(TAG, "AppListActivity, onResume()");
        super.onResume();
        
        boolean refresh = false;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean showStatusIcons = prefs.getBoolean(Preferences.SHOW_STATUS_ICONS, true);
        if (mShowStatusIcons != showStatusIcons) {
            refresh = true;
        }
        mShowStatusIcons = showStatusIcons;
        boolean showLogData = prefs.getBoolean(Preferences.APPLIST_SHOW_LOG_DATA, true);
        if (mShowLogData != showLogData) {
            refresh = true;
        }
        mShowLogData = showLogData;
        String statusIconType = prefs.getString(Preferences.STATUS_ICON_TYPE, "emote");
        if (mStatusIconType == null) {
            // If mStatusIconType is null, that means this is a first run and a refresh is not
            // necessary
            refresh = false;
        } else if (!statusIconType.equals(mStatusIconType)) {
            refresh = true;
        }
        mStatusIconType = statusIconType;
        
        if (refresh) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ((AppListActivity)getActivity()).showDetails(id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "Saving instance state");
        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof AppDetailsFragment) {
            outState.putInt("curChoice", mCurCheckPosition);
        }
    }

    private void setupListView() {
        final ListView list = getListView();
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        
        list.setDividerHeight(0);
        
        mAdapter = new AppListAdapter(null, getActivity());
        setListAdapter(mAdapter);
        
        if (list instanceof PinnedHeaderListView &&
                mAdapter.getDisplaySectionHeadersEnabled()) {
            mPinnedHeaderBackgroundColor =
                    getResources().getColor(R.color.pinned_header_background);
            PinnedHeaderListView pinnedHeaderExpandableList =
                    (PinnedHeaderListView) list;
            View pinnedHeader = inflater.inflate(R.layout.list_section, list, false);
            pinnedHeaderExpandableList.setPinnedHeaderView(pinnedHeader);
        }
        
        list.setOnScrollListener(mAdapter);
    }
    

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Apps.CONTENT_URI, null,
                Apps.ALLOW + "!=?", new String[] { String.valueOf(Apps.AllowType.ASK) }, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        Log.d(TAG, "onLoadFinished, data " + (data==null?"is":"is not") + " null");
        mLoadingLayout.setVisibility(View.GONE);
        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof AppDetailsFragment) {
            long shownItem = ((AppDetailsFragment)fragment).getShownIndex();
            getListView().setItemChecked(mAdapter.getPositionForId(shownItem), true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
    public class AppListAdapter extends CursorAdapter
            implements OnScrollListener, SectionIndexer, 
            PinnedHeaderListView.PinnedHeaderAdapter {

        private final String[] mSections = { getString(R.string.allow), getString(R.string.deny) };
        private final int[] mSectionTypes = { Apps.AllowType.ALLOW, Apps.AllowType.DENY };
        private int[] mSectionPositions = { 0, -1 };

        private boolean mDisplaySectionHeaders = true;
        private Cursor mCursor;
        
        private HashMap<Long, Integer> mPositions;
        private int mLastCachedPosition = -1;

        public AppListAdapter(Cursor cursor, Context context) {
            super(context, cursor, false);
            mPositions = new HashMap<Long, Integer>(cursor!=null?
                    cursor.getCount():0);
            mLastCachedPosition = -1;
            mCursor = cursor;
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            mSectionPositions[1] = -1;
            mCursor = newCursor;
            mPositions = new HashMap<Long, Integer>(newCursor != null?
                    newCursor.getCount():0);
            mLastCachedPosition = -1;
            return super.swapCursor(newCursor);
        }

        public boolean getDisplaySectionHeadersEnabled() {
            return mDisplaySectionHeaders;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
        
        public int getPositionForId(long id) {
            if (mPositions.containsKey(id)) {
                return mPositions.get(id);
            } else {
                mCursor.moveToPosition(mLastCachedPosition);
                while (mCursor.moveToNext()) {
                    mLastCachedPosition++;
                    if (mCursor.getLong(mCursor.getColumnIndex(Apps._ID)) == id) {
                        mPositions.put(id, mLastCachedPosition);
                        return mLastCachedPosition;
                    }
                }
            }
            return -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            bindSectionHeader(v, position, true);
            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final AppListItem view = new AppListItem(context, null);
            //    view.setOnStatusButtonClickListener(AppListActivity.this);
            return view;
        }

        @Override
        public void bindView(View itemView, Context context, Cursor cursor) {
            final AppListItem view = (AppListItem) itemView;

            int id = cursor.getInt(cursor.getColumnIndex(Apps._ID));
            int uid = cursor.getInt(cursor.getColumnIndex(Apps.UID));
            String nameText = cursor.getString(cursor.getColumnIndex(Apps.NAME));
            int allow = cursor.getInt(cursor.getColumnIndex(Apps.ALLOW));

            // Set app icon
            view.setAppIcon(Util.getAppIcon(getActivity(), uid));

            // Set name
            if (nameText != null && nameText.length() > 0) {
                view.setNameText(nameText);
            } else {
                view.setNameText(getString(R.string.unknown));
            }

            // Set the status indicator
            if (mShowStatusIcons) {
                view.setStatusButton(Util.getStatusIconDrawable(getActivity(), allow), 0, 0l);
            } else {
                view.setStatusButton(null, 0, 0);
            }

            // Set log data
            long  date = cursor.getLong(cursor.getColumnIndex(Apps.LAST_ACCESS));
            if (mShowLogData && date > 0) {
                int lastLogType = cursor.getInt(cursor.getColumnIndex(Apps.LAST_ACCESS_TYPE));
                int logTextRes = R.string.log_last_accessed;
                switch (lastLogType) {
                case Logs.LogType.ALLOW: logTextRes = R.string.log_last_allowed; break;
                case Logs.LogType.DENY: logTextRes = R.string.log_last_denied; break;
                case Logs.LogType.TOGGLE: logTextRes = R.string.log_last_toggled; break;
                case Logs.LogType.CREATE: logTextRes = R.string.log_created_on; break;
                }
                view.setLogText(getString(logTextRes, Util.formatDate(getActivity(), date),
                        Util.formatTime(getActivity(), date)));
            } else {
                view.setLogText(null);
            }
        }

        private void bindSectionHeader(View itemView, int position, boolean displaySectionHeaders) {
            final AppListItem view = (AppListItem)itemView;
            if (!displaySectionHeaders) {
                view.setSectionHeader(null);
                view.setDividerVisible(true);
            } else {
                final int section = getSectionForPosition(position);
                if (getPositionForSection(section) == position) {
                    String title = mSections[section];
                    view.setSectionHeader(title);
                } else {
                    view.setDividerVisible(false);
                    view.setSectionHeader(null);
                }

                // move the divider for the last item in a section
                if (getPositionForSection(section + 1) - 1 == position) {
                    view.setDividerVisible(false);
                } else {
                    view.setDividerVisible(true);
                }
            }
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            if (sectionIndex > 1) {
                return mCursor.getCount();
            }
            if (mSectionPositions[sectionIndex] > -1) {
                return mSectionPositions[sectionIndex];
            } else {
                Uri uri = Uri.withAppendedPath(Apps.COUNT_CONTENT_URI, String.valueOf(mSectionTypes[sectionIndex-1]));
                Cursor c = getActivity().getContentResolver().query(uri, null, null, null, null);
                int numInSection = 0;
                if (c.moveToFirst()) {
                    numInSection = c.getInt(0);
                } else {
                    numInSection = 0;
                }
                c.close();
                mSectionPositions[sectionIndex] = numInSection;
                return numInSection;
            }
        }

        @Override
        public int getSectionForPosition(int position) {
            if (mCursor == null) {
                return -1;
            }
            mCursor.moveToPosition(position);
            int allow = mCursor.getInt(mCursor.getColumnIndex(Apps.ALLOW));
            if (allow == Apps.AllowType.ALLOW) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public Object[] getSections() {
            return mSections;
        }

        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            if (view instanceof PinnedHeaderListView && visibleItemCount > 0) {
                ((PinnedHeaderListView)view).configureHeaderView(firstVisibleItem);
            }
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void configurePinnedHeader(View header, int position, int alpha) {
            PinnedHeaderCache cache = (PinnedHeaderCache)header.getTag();
            if (cache == null) {
                cache = new PinnedHeaderCache();
                cache.titleView = (TextView)header.findViewById(R.id.header_text);
                cache.textColor = cache.titleView.getTextColors();
                cache.background = header.getBackground();
                header.setTag(cache);
            }

            int section = getSectionForPosition(position);

            String title = mSections[section];
            cache.titleView.setText(title);

            if (alpha == 255) {
                // Opaque, use the default background and original text color
                header.setBackgroundDrawable(cache.background);
                cache.titleView.setTextColor(cache.textColor);
            } else {
                // Faded, use a solid color approximation of the background and
                // a translucent text color
                int red = Color.red(mPinnedHeaderBackgroundColor);
                int green = Color.green(mPinnedHeaderBackgroundColor);
                int blue = Color.blue(mPinnedHeaderBackgroundColor);

                header.setBackgroundColor(Color.rgb(
                        255 - alpha*(255-red)/255,
                        255 - alpha*(255-green)/255,
                        255 - alpha*(255-blue)/255));

                int textColor = cache.textColor.getDefaultColor();
                cache.titleView.setTextColor(Color.argb(alpha,
                        Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
            }
        }

        @Override
        public int getPinnedHeaderState(int position) {
            if (mCursor == null || mCursor.getCount() == 0 || mCursor.isClosed()) {
                return PINNED_HEADER_GONE;
            }

            if (position < 0) {
                return PINNED_HEADER_GONE;
            }

            // The header should get pushed up if the top item shown
            // is the last item in a particular section.
            int section = getSectionForPosition(position);
            int nextSectionPosition = getPositionForSection(section + 1);
            if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
                return PINNED_HEADER_PUSHED_UP;
            }

            return PINNED_HEADER_VISIBLE;
        }
    }
}
