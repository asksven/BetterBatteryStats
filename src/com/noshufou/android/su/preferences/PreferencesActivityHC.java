package com.noshufou.android.su.preferences;

import java.util.List;

import android.preference.PreferenceActivity;

import com.noshufou.android.su.R;

public class PreferencesActivityHC extends PreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

}
