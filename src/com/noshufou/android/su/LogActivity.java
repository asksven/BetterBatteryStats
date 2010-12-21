package com.noshufou.android.su;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
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

import com.noshufou.android.su.DBHelper.Apps;
import com.noshufou.android.su.DBHelper.LogType;
import com.noshufou.android.su.DBHelper.Logs;
import com.noshufou.android.su.PinnedHeaderListView.PinnedHeaderCache;

public class LogActivity extends ListActivity {
//	private static final String TAG = "Su.LogActivity";
    
    private static final int MENU_CLEAR_LOG = 1;
	
	private DBHelper mDB;
	private Cursor mCursor;
	private LogAdapter mAdapter;

	private int mPinnedHeaderBackgroundColor;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_list);
        
        mDB = new DBHelper(this);
        setupListView();
    }

    @Override
	protected void onPause() {
            mDB.close();
            super.onPause();
	}

	@Override
    public void onResume() {
        super.onResume();
        mDB = new DBHelper(this);
        refreshList();
    }

    @Override
    public void onDestroy() {
        mDB.close();
        super.onDestroy();
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_CLEAR_LOG, Menu.NONE, R.string.pref_clear_log)
                .setIcon(R.drawable.ic_menu_clear);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_CLEAR_LOG) {
            mDB.clearLog();
            refreshList();
            return true;
        }
        return false;
    }

    private void setupListView() {
    	final ListView list = getListView();
    	final LayoutInflater inflater = getLayoutInflater();
    	
    	list.setDividerHeight(0);
    	
    	mAdapter = new LogAdapter(this, mCursor);
    	setListAdapter(mAdapter);
    	
    	if (list instanceof PinnedHeaderListView && mAdapter.getDisplaySectionHeadersEnabled()) {
   			mPinnedHeaderBackgroundColor =
					getResources().getColor(R.color.pinned_header_background);
   			PinnedHeaderListView pinnedHeaderList = (PinnedHeaderListView)list;
   			View pinnedHeader = inflater.inflate(R.layout.list_section, list, false);
   			pinnedHeaderList.setPinnedHeaderView(pinnedHeader);
    	}
    	
        list.setOnScrollListener(mAdapter);
    }
    
    private void refreshList() {
    	mCursor = mDB.getAllLogs();
    	startManagingCursor(mCursor);
    	mAdapter.changeCursor(mCursor);
    }
    
    private final class LogAdapter extends CursorAdapter
    		implements SectionIndexer, OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter {
    	private SectionIndexer mIndexer;
    	private boolean mDisplaySectionHeaders = true;
    	private Context mContext;
    	
    	LogAdapter(Context context, Cursor cursor) {
    		super(context, cursor, false);
    		mContext = context;
    	}
    	
    	public boolean getDisplaySectionHeadersEnabled() {
    		return mDisplaySectionHeaders;
    	}
    	
    	@Override
   		public View getView(int position, View convertView, ViewGroup parent) {
   			View v = super.getView(position, convertView, parent);
   			bindSectionHeader(v, position, mDisplaySectionHeaders);
   			return v;
   		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LogItem view = new LogItem(context, null);
			return view;
		}
    	
		@Override
		public void bindView(View itemView, Context context, Cursor cursor) {
			final LogItem view = (LogItem)itemView;
			
			final int dateColumnIndex = cursor.getColumnIndex(Logs.DATE);
			final int typeColumnIndex = cursor.getColumnIndex(Logs.TYPE);
			final int nameColumnIndex = cursor.getColumnIndex(Apps.NAME);
			
			// Set the time
			long time = cursor.getLong(dateColumnIndex);
			if (time > 0) {
				view.setTimeText(Util.formatTime(context, time));
			} else {
				view.setTimeText("no time");
			}
			
			// Set the app name
			String name = cursor.getString(nameColumnIndex);
			if (name != null && name.length() > 0) {
				view.setNameText(name);
			} else {
				view.setNameText("no name");
			}
			
			// Set the type
			int type = cursor.getInt(typeColumnIndex);
			switch (type) {
			case LogType.CREATE:
				view.setTypeText(mContext.getString(R.string.created));
				break;
			case LogType.ALLOW:
				view.setTypeText(mContext.getString(R.string.allowed));
				break;
			case LogType.DENY:
				view.setTypeText(mContext.getString(R.string.denied));
				break;
			case LogType.TOGGLE:
				view.setTypeText(mContext.getString(R.string.toggled));
				break;
			default:
				view.setTypeText("unknown");
			}
		}

		private void bindSectionHeader(View itemView, int position, boolean displaySectionHeaders) {
   			final LogItem view = (LogItem)itemView;
   			if (!displaySectionHeaders) {
   				view.setSectionHeader(null);
   				view.setDividerVisible(true);
   			} else {
   				final int section = getSectionForPosition(position);
   				if (getPositionForSection(section) == position) {
   					String title = (String)mIndexer.getSections()[section];;
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
		public void changeCursor(Cursor cursor) {
			super.changeCursor(cursor);
			updateIndexer(cursor);
		}

		private void updateIndexer(Cursor cursor) {
			if (cursor == null || cursor.getCount() == 0) {
				mIndexer = null;
				return;
			}
			
			mIndexer = new DateIndexer(mContext, cursor, cursor.getColumnIndex(Logs.DATE));
		}
		
		@Override
		public Object [] getSections() {
			if (mIndexer == null) {
				return new String[] { " " };
			} else {
				return mIndexer.getSections();
			}
		}

		@Override
		public int getPositionForSection(int section) {
			if (mIndexer == null) {
				return -1;
			} else {
				int position = mIndexer.getPositionForSection(section);
				return position;
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
   				// Opaque: use the default background, and the original text color
   				header.setBackgroundDrawable(cache.background);
   				cache.titleView.setTextColor(cache.textColor);
   			} else {
   				// Faded: use a solid color approximation of the background, and
   				// a translucent text color
   				header.setBackgroundColor(Color.rgb(
   						Color.red(mPinnedHeaderBackgroundColor) * alpha / 255,
   						Color.green(mPinnedHeaderBackgroundColor) * alpha / 255,
   						Color.blue(mPinnedHeaderBackgroundColor) * alpha / 255));

   				int textColor = cache.textColor.getDefaultColor();
   				cache.titleView.setTextColor(Color.argb(alpha,
   						Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
   			}
   		}
    }
}

