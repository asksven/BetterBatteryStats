package com.asksven.betterbatterystats;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.Toast;

import com.asksven.android.common.CommonLogSettings;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.services.EventWatcherService;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
{

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value)
        {
            String key = value.toString();
            final Context ctx = preference.getContext();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);


            if (key.equals("ref_for_screen_off"))
            {
                boolean serviceShouldBeRunning = sharedPreferences.getBoolean(key, false);

                if (serviceShouldBeRunning)
                {
                    if (!EventWatcherService.isServiceRunning(preference.getContext()))
                    {
                        Intent i = new Intent(preference.getContext(), EventWatcherService.class);
                        preference.getContext().startService(i);
                    }
                } else
                {
                    if (EventWatcherService.isServiceRunning(preference.getContext()))
                    {
                        Intent i = new Intent(ctx, EventWatcherService.class);
                        ctx.stopService(i);
                    }
                }
            }

            if (key.equals("debug_logging"))
            {
                boolean enabled = sharedPreferences.getBoolean(key, false);
                if (enabled)
                {
                    LogSettings.DEBUG = true;
                    CommonLogSettings.DEBUG = true;
                } else
                {
                    LogSettings.DEBUG = true;
                    CommonLogSettings.DEBUG = true;
                }
            }

            if (key.equals("active_mon_enabled"))
            {
                boolean enabled = sharedPreferences.getBoolean(key, false);

                if (enabled)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext());
                    builder.setMessage(ctx.getString(R.string.message_enable_active_mon))
                            .setCancelable(false)
                            .setPositiveButton(ctx.getString(R.string.label_button_yes),
                                    new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            // Fire the alarms
                                            StatsProvider.scheduleActiveMonAlarm(ctx);
                                            dialog.cancel();
                                        }
                                    })
                            .setNegativeButton(ctx.getString(R.string.label_button_no),
                                    new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            SharedPreferences sharedPrefs = PreferenceManager
                                                    .getDefaultSharedPreferences(ctx);
                                            SharedPreferences.Editor editor = sharedPrefs.edit();
                                            editor.putBoolean("active_mon_enabled", false);
                                            editor.commit();
                                            dialog.cancel();
                                        }
                                    });
                    builder.create().show();

                }

                else
                {
                    // cancel any existing alarms
                    StatsProvider.cancelActiveMonAlarm(ctx);

                }

            }
            if (key.equals("theme"))
            {
                if (ctx != null)
                {
                    Intent i = ctx.getPackageManager()
                            .getLaunchIntentForPackage(ctx.getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    ctx.startActivity(i);
                }
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context)
    {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference)
    {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String theme = sharedPrefs.getString("theme", "0");
        if (theme.equals("0"))
        {
            this.setTheme(R.style.Theme_Bbs);
        } else
        {
            this.setTheme(R.style.Theme_Bbs_Dark);
        }
        super.onCreate(savedInstanceState);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar()
    {
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
        {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            if (!super.onMenuItemSelected(featureId, item))
            {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane()
    {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName)
    {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || DisplayPreferenceFragment.class.getName().equals(fragmentName)
                || ViewsPreferenceFragment.class.getName().equals(fragmentName)
                || WidgetsPreferenceFragment.class.getName().equals(fragmentName)
                || WatchdogPreferenceFragment.class.getName().equals(fragmentName)
                || ActiveMonPreferenceFragment.class.getName().equals(fragmentName)
                || AdvancedPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DisplayPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_display);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ViewsPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_views);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WidgetsPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_widgets);
            setHasOptionsMenu(true);

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WatchdogPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_watchdog);
            setHasOptionsMenu(true);

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ActiveMonPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_activemon);
            setHasOptionsMenu(true);

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AdvancedPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_advanced);
            setHasOptionsMenu(true);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }



}
