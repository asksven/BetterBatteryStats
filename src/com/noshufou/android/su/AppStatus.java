package com.noshufou.android.su;

public class AppStatus {
    public static final int ASK = 0;
    public static final int ALLOW = 1;
    public static final int DENY = 2;

	public static final String ALLOW_CODE = "ALLOW";
	public static final String DENY_CODE = "DENY";

    public int permission;
    public int callerUid;
    public long dateAccess;

    AppStatus(int permissionIn, int callerUidIn, long dateAccessIn) {
        permission = permissionIn;
        dateAccess = dateAccessIn;
        callerUid = callerUidIn;
    }
    
    public String getPermissionCode() {
        return (permission == ALLOW) ? ALLOW_CODE : DENY_CODE;
    }
}
