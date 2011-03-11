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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.noshufou.android.su.util.AppDetails;
import com.noshufou.android.su.util.DBHelper;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.util.DBHelper.Apps;
import com.noshufou.android.su.widget.AppListItem;
import com.noshufou.android.su.widget.PinnedHeaderListView;
import com.noshufou.android.su.widget.PinnedHeaderListView.PinnedHeaderCache;

public class AppListActivity extends ListActivity {
    private static final String TAG = "Su.AppListActivity";
    
    private static final int MENU_UPDATE = 1;
    
    private Context mContext;
    private DBHelper mDB;
    private Cursor mCursor;
    private AppListAdapter mAdapter;
    private boolean mShowStatusIcons = true;
    private int mStatusIconType = 1;
    
    private int mPinnedHeaderBackgroundColor;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);
        ((TextView)findViewById(R.id.title_text)).setText(R.string.app_name);
        
        mContext = this;

    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mDB = new DBHelper(this);
        setupListView();
//        refreshList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDB.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_UPDATE, Menu.NONE, R.string.updater_update);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_UPDATE:
            Intent intent = new Intent(this, UpdaterActivity.class);
            startActivity(intent);
            return true;
        default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, AppDetailsActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    private void setupListView() {
        final ListView list = getListView();
        final LayoutInflater inflater = getLayoutInflater();
        
        list.setDividerHeight(0);
        
        mCursor = mDB.getAllApps();
        mAdapter = new AppListAdapter(mCursor, this);
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
    
    private void refreshList() {
        mCursor = mDB.getAllApps();
        startManagingCursor(mCursor);
        mAdapter.changeCursor(mCursor);
    }
    
    private class AppListAdapter extends CursorAdapter
            implements OnScrollListener, SectionIndexer, 
            PinnedHeaderListView.PinnedHeaderAdapter {

        final String[] mSections = { getString(R.string.allow), getString(R.string.deny) };

        private boolean mDisplaySectionHeaders = true;
        private Cursor mCursor;

        public AppListAdapter(Cursor cursor, Context context) {
            super(context, cursor, false);
            mCursor = cursor;
        }
        
        public boolean getDisplaySectionHeadersEnabled() {
            return mDisplaySectionHeaders;
        }
        
        @Override
        public boolean hasStableIds() {
            return true;
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
//            view.setOnStatusButtonClickListener(AppListActivity.this);
            return view;
        }

        @Override
        public void bindView(View itemView, Context context, Cursor cursor) {
            final AppListItem view = (AppListItem) itemView;
            
            int id = cursor.getInt(cursor.getColumnIndex(Apps.ID));
            int uid = cursor.getInt(cursor.getColumnIndex(Apps.UID));
            String nameText = cursor.getString(cursor.getColumnIndex(Apps.NAME));
            int allow = cursor.getInt(cursor.getColumnIndex(Apps.ALLOW));
            
            // Set app icon
            view.setAppIcon(Util.getAppIcon(AppListActivity.this, uid));
            
            // Set name
            if (nameText != null && nameText.length() > 0) {
                view.setNameText(nameText);
            } else {
                view.setNameText(getString(R.string.unknown));
            }
            
            // Set the status indicator
            if (mShowStatusIcons) {
                view.setStatusButton(getStatusButtonDrawable(allow), 0, 0l);
            }
            
            // Set log data
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
            if (sectionIndex == 0) {
                return 0;
            } else {
                return mDB.countPermissions(AppDetails.ALLOW);
            }
        }

        @Override
        public int getSectionForPosition(int position) {
            mCursor.moveToPosition(position);
            int allow = mCursor.getInt(mCursor.getColumnIndex(Apps.ALLOW));
            if (allow == AppDetails.ALLOW) {
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
            Log.d(TAG, "firstVisibleItem=" + firstVisibleItem + ", visibleItemCount=" + visibleItemCount + ", totalItemCount=" + totalItemCount);
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

        private Drawable getStatusButtonDrawable(int allow) {
            int[][] statusButtons = {
                    { R.drawable.perm_deny_dot, R.drawable.perm_allow_dot },
                    { R.drawable.perm_deny_emo, R.drawable.perm_allow_emo }
            };
            
            if (allow < 0 || allow > 1) {
                Log.e(TAG, "Bad value given to getStatusButtonDrawable(int). Expecting 0 or 1, got " + allow);
                return null;
            }

            Drawable drawable = mContext.getResources().getDrawable(statusButtons[mStatusIconType][allow]);
            return drawable;
        }
    }
}
