package com.noshufou.android.su.preferences;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class PreferencesFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int res = getActivity()
                .getResources()
                .getIdentifier(getArguments().getString("resource"),
                        "xml",
                        getActivity().getPackageName());
        
        addPreferencesFromResource(res);
        
        PreferenceScreen screen = getPreferenceScreen();
//        Preferences.setupScreen(getActivity(), screen);
    }

}
