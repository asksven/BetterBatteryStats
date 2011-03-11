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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.noshufou.android.su.util.AppDetails;
import com.noshufou.android.su.util.DBHelper;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.util.DBHelper.LogType;
import com.noshufou.android.su.util.DBHelper.Logs;

public class AppDetailsActivity extends ListActivity implements OnClickListener {
    
    private Button mToggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_details);
        
        Intent callingIntent = this.getIntent();
        long appId = callingIntent.getLongExtra("id", 0);
        
        DBHelper db = new DBHelper(this);
        AppDetails appDetails = db.getAppDetails(appId);
        int appUid = appDetails.getUid();
        int reqUid = appDetails.getExecUid();

        // Title bar
        ((TextView)findViewById(R.id.title_text)).setText(appDetails.getName());
        ((ImageButton)findViewById(R.id.home_button)).setOnClickListener(this);
        
        ((ImageView)findViewById(R.id.app_icon)).setImageDrawable(Util.getAppIcon(this, appUid));
        ((TextView)findViewById(R.id.package_name)).setText(appDetails.getPackageName());
        ((TextView)findViewById(R.id.app_uid)).setText(Integer.toString(appUid));
        ((TextView)findViewById(R.id.request_detail))
                .setText(Util.getUidName(this, reqUid, true));
        ((TextView)findViewById(R.id.command)).setText(appDetails.getCommand());
        ((TextView)findViewById(R.id.status)).setText(
                appDetails.getPermissionBool() ? R.string.allow : R.string.deny);
        
        setListAdapter(new LogAdapter(this, R.layout.log_list_item,
                appDetails.getRecentUsage()));
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.home_button:
            final Intent intent = new Intent(this, AppListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;
        }
    }
    
    private class LogAdapter extends ResourceCursorAdapter {

        public LogAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            long date = cursor.getLong(cursor.getColumnIndex(Logs.DATE));
            ((TextView)view.findViewById(R.id.log_date))
                    .setText(Util.formatDate(context, date));
            ((TextView)view.findViewById(R.id.log_time))
                    .setText(Util.formatTime(context, date));
            int logType = R.string.unknown;
            switch (cursor.getInt(cursor.getColumnIndex(Logs.TYPE))) {
            case LogType.ALLOW:
                logType = R.string.allowed;
                break;
            case LogType.DENY:
                logType = R.string.denied;
                break;
            case LogType.CREATE:
                logType = R.string.created;
                break;
            case LogType.TOGGLE:
                logType = R.string.toggled;
                break;
            }
            ((TextView)view.findViewById(R.id.log_type)).setText(logType);
        }

    }

}
