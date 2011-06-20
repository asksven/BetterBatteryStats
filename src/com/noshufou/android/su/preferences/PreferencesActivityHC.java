package com.noshufou.android.su.preferences;

import java.util.List;

import com.noshufou.android.su.R;
import com.noshufou.android.su.R.xml;

import android.preference.PreferenceActivity;

public class PreferencesActivityHC extends PreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

}
