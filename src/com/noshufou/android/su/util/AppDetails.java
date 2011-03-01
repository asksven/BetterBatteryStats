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
package com.noshufou.android.su.util;

import android.database.Cursor;
import android.util.Log;

import com.noshufou.android.su.util.DBHelper.LogType;

public class AppDetails {
    private static final String TAG = "Su.AppDetails";
    public static final int ASK = -1;
    public static final int DENY = 0;
    public static final int ALLOW = 1;

    public static final String ALLOW_CODE = "ALLOW";
    public static final String DENY_CODE = "DENY";

    private int mId;
    private int mUid;
    private String mPackageName;
    private String mName;
    private int mExecUid;
    private String mCommand;
    private int mAllow;
    private long mDateAccess;
    private int mAccessType;
    private long mDateCreated;
    private Cursor mRecentUsage;
    
    AppDetails() {
        
    }

    AppDetails(int uid, int allow, long dateAccess) {
        mUid = uid;
        mAllow = allow;
        mDateAccess = dateAccess;
    }
    
    public String getPermissionCode() {
        return (mAllow == ALLOW) ? ALLOW_CODE : DENY_CODE;
    }
    
    public boolean getPermissionBool() {
        return (mAllow == ALLOW);
    }
    
    public int getId() {
        return mId;
    }
    
    public void setId(int id) {
        mId = id;
    }
    
    public int getUid() {
        return mUid;
    }
    
    public void setUid(int uid) {
        mUid = uid;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getExecUid() {
        return mExecUid;
    }

    public void setExecUid(int execUid) {
        this.mExecUid = execUid;
    }

    public String getCommand() {
        return mCommand;
    }

    public void setCommand(String command) {
        this.mCommand = command;
    }

    public int getAllow() {
        return mAllow;
    }

    public void setAllow(int allow) {
        if (allow == ALLOW || allow == DENY || allow == ASK) {
        this.mAllow = allow;
        } else {
            Log.e(TAG, "AppDetails.setAllow(int): accessType should be 1, 2, or 3. allow=" + allow);
        }
    }

    public Cursor getRecentUsage() {
        return mRecentUsage;
    }
    
    public void setRecentUsage(Cursor cursor) {
        mRecentUsage = cursor;
    }
}
