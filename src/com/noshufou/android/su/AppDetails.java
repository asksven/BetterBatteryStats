package com.noshufou.android.su;

import android.util.Log;

import com.noshufou.android.su.DBHelper.LogType;

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

	public long getDateAccess() {
		return mDateAccess;
	}

	public void setDateAccess(long mDateAccess) {
		this.mDateAccess = mDateAccess;
	}
	
	public int getAccessType() {
		return mAccessType;
	}
	
	public void setAccessType(int accessType) {
		if (accessType == LogType.ALLOW || accessType == LogType.DENY) {
			mAccessType = accessType;
		} else {
			Log.e(TAG, "AppDetails.setAccessType(int): accessType should be 1 or 3. accessType=" + accessType);
			mAccessType = -1;
		}
	}

	public long getDateCreated() {
		return mDateCreated;
	}

	public void setDateCreated(long mDateCreated) {
		this.mDateCreated = mDateCreated;
	}
}
