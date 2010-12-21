package com.noshufou.android.su;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.noshufou.android.su.DBHelper.Apps;
import com.noshufou.android.su.PinnedHeaderListView.PinnedHeaderCache;

public class AppListActivity extends ListActivity implements View.OnClickListener {
    private static final String TAG = "Su.AppListActivity";
    
    private static final int STATUS_BUTTON_ID = 1;
	private final int STATUS_TYPE_DOT = 0;
	private final int STATUS_TYPE_EMOTE = 1;
    
    private DBHelper mDB;
    private Cursor mCursor;
    private AppListAdapter mAdapter;
    private Context mContext;
    private boolean mShowStatusIcons;
    private int mStatusIconType;
    private SharedPreferences mPrefs;
    
	private int mPinnedHeaderBackgroundColor;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_list);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mContext = this;
        mDB = new DBHelper(this);
        setupListView();
    }

    @Override
    public void onStart() {
        super.onStart();
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
        mShowStatusIcons = mPrefs.getBoolean("pref_show_status_icons", true);
		String type = mPrefs.getString("pref_status_icon_type", "dot");
		if (type.equals("dot")) {
			mStatusIconType = STATUS_TYPE_DOT;
		} else if (type.equals("emote")) {
			mStatusIconType = STATUS_TYPE_EMOTE;
		}

		refreshList();
    }

    @Override
    public void onDestroy() {
        mDB.close();
        super.onDestroy();
    }
    
	private void setupListView() {
    	final ListView list = getListView();
    	final LayoutInflater inflater = getLayoutInflater();
    	
    	list.setDividerHeight(0);
    	list.setOnCreateContextMenuListener(this);
    	
    	mAdapter = new AppListAdapter(this, mCursor);
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
    
    public void onClick(View v) {
    	int id = v.getId();
    	if (id == STATUS_BUTTON_ID) {
    		long appId = (Long) v.getTag();
    		mDB.changeState(appId);
    		refreshList();
    	}
    }

    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
    	String action = mPrefs.getString("pref_tap_action", "detail");
    	if (action.equals("detail")) {
    		appDetails(id);
    	} else if (action.equals("forget")) {
    		mDB.deleteById(id);
    		refreshList();
    	} else if (action.equals("toggle")) {
    		mDB.changeState(id);
    		refreshList();
    	}
	}

    private void refreshList() {
        mCursor = mDB.getAllApps();
        startManagingCursor(mCursor);
        mAdapter.changeCursor(mCursor);
    }

    private void appDetails(long id) {
        LayoutInflater inflater = LayoutInflater.from(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alert;

        View layout = inflater.inflate(R.layout.app_details, (ViewGroup) findViewById(R.id.detailLayout));
        
        TextView packageNameView = (TextView) layout.findViewById(R.id.packageName);
        TextView requestView = (TextView)layout.findViewById(R.id.requestDetail);
        TextView commandView = (TextView) layout.findViewById(R.id.command);
        TextView statusView = (TextView) layout.findViewById(R.id.status);
        TextView createdView = (TextView) layout.findViewById(R.id.created);
        TextView lastAccessedView = (TextView) layout.findViewById(R.id.lastAccessed);

        final long appId = id;
        AppDetails appDetails = mDB.getAppDetails(id);
        final int appUid = appDetails.getUid();

        String appName = appDetails.getName();
        String appPackage = appDetails.getPackageName();
        Drawable appIcon = Util.getAppIcon(this, appUid);

        packageNameView.setText(appPackage);

        int execUid = appDetails.getExecUid();
        requestView.setText(Util.getUidName(this, execUid, true));
        commandView.setText(appDetails.getCommand());
        statusView.setText(appDetails.getPermissionBool() ? R.string.allow : R.string.deny);
        createdView.setText(Util.formatDateTime(this, appDetails.getDateCreated()));
        lastAccessedView.setText(Util.formatDateTime(this, appDetails.getDateAccess()));
        
        View customTitle = inflater.inflate(R.layout.app_details_title, (ViewGroup) findViewById(R.id.customTitle));
        
        ImageView titleIcon = (ImageView) customTitle.findViewById(R.id.appIcon);
        titleIcon.setImageDrawable(appIcon);
        TextView titleName = (TextView) customTitle.findViewById(R.id.appName);
        titleName.setText(appName);
        TextView titleUid = (TextView) customTitle.findViewById(R.id.appUid);
        titleUid.setText(Integer.toString(appUid));

        builder.setCustomTitle(customTitle)
               .setView(layout)
               .setPositiveButton(appDetails.getPermissionBool() ? R.string.deny : R.string.allow, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mDB.changeState(appId);
                        refreshList();
                    }
                })
               .setNeutralButton(getString(R.string.forget), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mDB.deleteById(appId);
                        refreshList();
                        dialog.cancel();
                    }
                })
               .setNegativeButton(getString(R.string.cancel), null);
        alert = builder.create();
        alert.show();
    }
    
    protected String getQuantityText(int count, int zeroResourceId, int pluralResourceId) {
    	if (count == 0) {
    		return getString(zeroResourceId);
    	} else {
    		String format = getResources().getQuantityText(pluralResourceId, count).toString();
    		return String.format(format, count);
    	}
    }
    
    private final class AppListAdapter extends CursorAdapter
    		implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter {
    	
    	private final String[] sections = { getString(R.string.allow), getString(R.string.deny) };

   		private CharSequence mUnknownNameText;
   		private CharSequence mNoLogDataText;
   		private boolean mDisplaySectionHeaders = true;

   		public AppListAdapter(Context context, Cursor cursor) {
   			super(context, cursor, false);
   			mUnknownNameText = context.getText(R.string.unknown);
   			mNoLogDataText = context.getText(R.string.no_log_data);
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
   			final AppListItem view = new AppListItem(context, null);
   			view.setOnStatusButtonClickListener(AppListActivity.this);
   			return view;
   		}

   		@Override
   		public void bindView(View itemView, Context context, Cursor cursor) {
   			final AppListItem view = (AppListItem)itemView;
   			
   			final int idColumnIndex = cursor.getColumnIndex(Apps.ID);
   			final int uidColumnIndex = cursor.getColumnIndex(Apps.UID);
   			final int nameColumnIndex = cursor.getColumnIndex(Apps.NAME);
   			final int allowColumnIndex = cursor.getColumnIndex(Apps.ALLOW);
   			
            int id = cursor.getInt(idColumnIndex);
            int uid = cursor.getInt(uidColumnIndex);
            String nameText = cursor.getString(nameColumnIndex);
            int allow = cursor.getInt(allowColumnIndex);

            // Set the app icon
   			Drawable appIcon = Util.getAppIcon(mContext, uid);
   			view.setAppIcon(appIcon);
   			
   			// Set the name
   			if (nameText != null && nameText.length() > 0) {
   				view.setNameText(nameText);
   			} else {
   				view.setNameText(mUnknownNameText);
   			}
   			
   			// Set the log info
   			long dateLong = mDB.getLastLog(id, allow);
   			if (dateLong > 0) {
   				view.setLogText(Util.formatDateTime(context, dateLong));
   			} else {
   				view.setLogText(mNoLogDataText);
   			}
   			
   			// Set the status button, if applicable
   			if (mShowStatusIcons) {
   				Drawable statusButton = getStatusButtonDrawable(allow);
   				view.setStatusButton(statusButton, STATUS_BUTTON_ID, id);
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
   					String title = sections[section];
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

   		public int getPositionForSection(int sectionIndex) {
   			if (sectionIndex == 0) {
   				return 0;
   			} else {
   				return mDB.countPermissions(AppDetails.ALLOW);
   			}
   		}

   		public int getSectionForPosition(int position) {
   			mCursor.moveToPosition(position);
   			int allow = mCursor.getInt(mCursor.getColumnIndex("allow"));
   			if (allow == AppDetails.ALLOW) {
   				return 0;
   			} else {
   				return 1;
   			}
   		}
   		
   		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
   				int totalItemCount) {
   			if (view instanceof PinnedHeaderListView) {
   				((PinnedHeaderListView)view).configureHeaderView(firstVisibleItem);
   			}
   		}

   		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			
		}

		/**
   		 * Computes the state of the pinned header.  It can be invisible, fully
   		 * visible or partially pushed up out of the view.
   		 */
   		public int getPinnedHeaderState(int position) {
   			if (mCursor == null || mCursor.getCount() == 0 || mCursor.isClosed()) {
   				return PINNED_HEADER_GONE;
   			}

   			if (position < 0) {
   				return PINNED_HEADER_GONE;
   			}

   			// The header should get pushed up if the top item shown
   			// is the last item in a section for a particular letter.
   			int section = getSectionForPosition(position);
   			int nextSectionPosition = getPositionForSection(section + 1);
   			if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
   				return PINNED_HEADER_PUSHED_UP;
   			}

   			return PINNED_HEADER_VISIBLE;
   		}

   		/**
   		 * Configures the pinned header by setting the appropriate text label
   		 * and also adjusting color if necessary.  The color needs to be
   		 * adjusted when the pinned header is being pushed up from the view.
   		 */
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

   			String title = sections[section];
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

