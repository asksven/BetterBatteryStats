package com.noshufou.android.su.widget;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.noshufou.android.su.R;
import com.noshufou.android.su.provider.PermissionsProvider.Logs;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.widget.PinnedHeaderListView.PinnedHeaderCache;

public class LogAdapter extends CursorAdapter
        implements OnScrollListener, SectionIndexer,
        PinnedHeaderListView.PinnedHeaderAdapter {

    private SectionIndexer mIndexer;
    private boolean mDisplaySectionHeaders = true;
    private boolean mShowName = true;
    private Cursor mCursor;
    private Context mContext;

    public LogAdapter(Cursor cursor, Context context) {
        super(context, cursor, false);
        mCursor = cursor;
        mContext = context;
    }
    
    public LogAdapter(Cursor cursor, Context context, boolean showName) {
        this(cursor, context);
        mShowName = showName;
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        updateIndexer(newCursor);
        return super.swapCursor(newCursor);
    }

    private void updateIndexer(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            mIndexer = null;
            return;
        }

        mIndexer = new DateIndexer(mContext, cursor, cursor.getColumnIndex(Logs.DATE));
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
        final LogListItem view = new LogListItem(context, null);
        return view;
    }
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        LogListItem item = (LogListItem) view;
        item.setTimeText(Util.formatTime(context,
                cursor.getLong(cursor.getColumnIndex(Logs.DATE))));
        if (mShowName) {
            item.setNameText(cursor.getString(cursor.getColumnIndex(Logs.NAME)));
        }
        int logType = R.string.unknown;
        switch (cursor.getInt(cursor.getColumnIndex(Logs.TYPE))) {
        case Logs.LogType.ALLOW:
            logType = R.string.allowed;
            break;
        case Logs.LogType.DENY:
            logType = R.string.denied;
            break;
        case Logs.LogType.CREATE:
            logType = R.string.created;
            break;
        case Logs.LogType.TOGGLE:
            logType = R.string.toggled;
            break;
        }
        item.setTypeText(mContext.getString(logType));
    }

    private void bindSectionHeader(View itemView, int position, boolean displaySectionHeaders) {
        final LogListItem view = (LogListItem) itemView;
        if (!displaySectionHeaders) {
            view.setSectionHeader(null);
            view.setDividerVisible(true);
        } else {
            final int section = getSectionForPosition(position);
            if (getPositionForSection(section) == position) {
                String title = (String)mIndexer.getSections()[section];
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
    public Object[] getSections() {
        if (mIndexer == null) {
            return new String[] { " " } ;
        } else {
            return mIndexer.getSections();
        }
    }

    @Override
    public int getPositionForSection(int section) {
        if (mIndexer == null) {
            return -1;
        } else {
            return mIndexer.getPositionForSection(section);
        }
    }

    @Override
    public int getSectionForPosition(int position) {
        if (mIndexer == null) {
            return -1;
        } else {
            return mIndexer.getSectionForPosition(position);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        if (view instanceof PinnedHeaderListView) {
            ((PinnedHeaderListView)view).configureHeaderView(firstVisibleItem);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Don't need to do anything here, but we must override it anyway
    }

    @Override
    public int getPinnedHeaderState(int position) {
        if (mIndexer == null || mCursor == null || mCursor.getCount() == 0
                || mCursor.isClosed()) {
            return PINNED_HEADER_GONE;
        }

        if (position < 0) {
            return PINNED_HEADER_GONE;
        }

        int section = getSectionForPosition(position);
        int nextSectionPosition = getPositionForSection(section + 1);
        if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
            return PINNED_HEADER_PUSHED_UP;
        }

        return PINNED_HEADER_VISIBLE;
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

        String title = (String)mIndexer.getSections()[section];
        cache.titleView.setText(title);

        if (alpha == 255) {
            // Opaque, use the default background and original text color
            header.setBackgroundDrawable(cache.background);
            cache.titleView.setTextColor(cache.textColor);
        } else {
            // Faded, use a solid color approximation of the background and
            // a translucent text color

            header.setBackgroundColor(0x00ffffff);

            int textColor = cache.textColor.getDefaultColor();
            cache.titleView.setTextColor(Color.argb(alpha,
                    Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
        }
    }

}
