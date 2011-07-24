package com.noshufou.android.su;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.noshufou.android.su.provider.PermissionsProvider.Apps;

public class UninstallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            return;
        }
        
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Apps.CONTENT_URI,
                new String[] { Apps._ID },
                Apps.UID + "=?",
                new String[] { String.valueOf(intent.getIntExtra(Intent.EXTRA_UID, -1)) },
                null);
        if (cursor.moveToFirst()) {
            cr.delete(
                    ContentUris.withAppendedId(Apps.CONTENT_URI,
                            cursor.getLong(cursor.getColumnIndex(Apps._ID))),
                    null, null);
        }
        cursor.close();
    }

}
