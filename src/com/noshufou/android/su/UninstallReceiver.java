package com.noshufou.android.su;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UninstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DBHelper db = new DBHelper(context);
        int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
        if (uid != 1 && !(intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))) {
            db.deleteByUid(uid);
        }
        db.close();
    }
}
