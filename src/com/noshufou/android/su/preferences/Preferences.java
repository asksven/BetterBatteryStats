package com.noshufou.android.su.preferences;

public class Preferences {
    
    public static final String PIN = "pref_pin";
    public static final String CHANGE_PIN = "pref_change_pin";
    public static final String TIMEOUT = "pref_timeout";
    public static final String AUTOMATIC_ACTION = "pref_automatic_action";
    public static final String GHOST_MODE = "pref_ghost_mode";
    public static final String SECRET_CODE = "pref_secret_code";
    public static final String SHOW_STATUS_ICONS = "pref_show_status_icons";
    public static final String STATUS_ICON_TYPE = "pref_status_icon_type";
    public static final String APPLIST_SHOW_LOG_DATA = "pref_applist_show_log_data";
    public static final String LOGGING = "pref_logging";
    public static final String DELETE_OLD_LOGS = "pref_delete_old_logs";
    public static final String LOG_ENTRY_LIMIT = "pref_log_entry_limit";
    public static final String HOUR_FORMAT = "pref_24_hour_format";
    public static final String SHOW_SECONDS = "pref_show_seconds";
    public static final String DATE_FORMAT = "pref_date_format";
    public static final String CLEAR_LOG = "pref_clear_log";
    public static final String NOTIFICATIONS = "pref_notifications";
    public static final String NOTIFICATION_TYPE = "pref_notification_type";
    public static final String TOAST_LOCATION = "pref_toast_location";
    public static final String USE_ALLOW_TAG = "pref_use_allow_tag";
    public static final String WRITE_ALLOW_TAG = "pref_write_allow_tag";
    public static final String VERSION = "pref_version";
    public static final String BIN_VERSION = "pref_bin_version";
    public static final String CHANGELOG = "pref_changelog";
    public static final String GET_ELITE = "pref_get_elite";
    
    public static final String CATEGORY_SECURITY = "pref_category_security";
    public static final String CATEGORY_APPLIST = "pref_category_applist";
    public static final String CATEGORY_LOG = "pref_category_log";
    public static final String CATEGORY_NOTIFICATION = "pref_category_notification";
    public static final String CATEGORY_NFC = "pref_category_nfc";
    public static final String CATEGORY_INFO = "pref_category_info";
    
    public static final String ELITE_PREFS[] = new String[] {
        CATEGORY_SECURITY + ":" + PIN,
        CATEGORY_SECURITY + ":" + CHANGE_PIN,
        CATEGORY_SECURITY + ":" + TIMEOUT,
        CATEGORY_SECURITY + ":" + GHOST_MODE,
        CATEGORY_SECURITY + ":" + SECRET_CODE,
        CATEGORY_LOG + ":" + LOG_ENTRY_LIMIT ,
        CATEGORY_NOTIFICATION + ":" + TOAST_LOCATION,
        CATEGORY_NFC + ":"
    };

}
