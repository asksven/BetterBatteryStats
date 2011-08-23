package com.noshufou.android.su;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.noshufou.android.su.provider.PermissionsProvider.Logs;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.widget.ChangeLog;
import com.noshufou.android.su.widget.PagerHeader;

public class HomeActivity extends FragmentActivity {
    private static final String TAG = "Su.HomeActivity";
    
    private static final int MENU_EXTRAS = 0;
    private static final int MENU_CLEAR_LOG = 1;
    private static final int MENU_PREFERENCES = 2;
    
    private boolean mDualPane = false;
    
    private ViewPager mPager;
    private TransitionDrawable mTitleLogo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_home);
        
        if (findViewById(R.id.fragment_container) != null) {
            mDualPane = true;
            showLog();
        } else {
            mPager = (ViewPager)findViewById(R.id.pager);
            PagerAdapter pagerAdapter = new PagerAdapter(this,
                    mPager,
                    (PagerHeader)findViewById(R.id.pager_header));

            pagerAdapter.addPage(AppListFragment.class, null, "apps");
            pagerAdapter.addPage(LogFragment.class, null, "log");
        }
        
        mTitleLogo = 
                (TransitionDrawable) ((ImageView)findViewById(android.R.id.home)).getDrawable();
        new EliteCheck().execute();

        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun())
            cl.getLogDialog().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.noshufou.android.su.elite",
                "com.noshufou.android.su.elite.FeaturedAppsActivity"));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.NONE, MENU_EXTRAS, MENU_EXTRAS,
                this.getComponentName(), null, intent, 0, null);

        MenuItem item = menu.add(Menu.NONE, MENU_CLEAR_LOG,
                MENU_CLEAR_LOG, R.string.menu_clear_log);
        item.setIcon(android.R.drawable.ic_menu_delete);

        item = menu.add(Menu.NONE, MENU_PREFERENCES,
                MENU_PREFERENCES, R.string.menu_preferences);
        item.setIcon(android.R.drawable.ic_menu_preferences);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_CLEAR_LOG:
            getContentResolver().delete(Logs.CONTENT_URI, null, null);
            break;
        case MENU_PREFERENCES:
            Util.launchPreferences(this);
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void showDetails(long id) {
        if (mDualPane) {
            Bundle bundle = new Bundle();
            bundle.putLong("index", id);
            Fragment detailsFragment = 
                    Fragment.instantiate(this, AppDetailsFragment.class.getName(), bundle);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.fragment_container, detailsFragment);
            transaction.commit();
        } else {
            Intent intent = new Intent(this, AppDetailsActivity.class);
            intent.putExtra("index", id);
            startActivity(intent);
        }
    }
    
    public void showLog() {
        if (mDualPane) {
            Fragment logFragment = Fragment.instantiate(this, LogFragment.class.getName());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.fragment_container, logFragment);
            transaction.commit();
        }
    }

    public static class PagerAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener, PagerHeader.OnHeaderChangeListener {
        
        private final Context mContext;
        private final ViewPager mPager;
        private final PagerHeader mHeader;
        private final ArrayList<PageInfo> mPages = new ArrayList<PageInfo>();
        
        static final class PageInfo {
            private final Class<?> clss;
            private final Bundle args;
            
            PageInfo(Class<?> _clss, Bundle _args) {
                clss = _clss;
                args = _args;
            }
        }

        public PagerAdapter(FragmentActivity activity, ViewPager pager,
                PagerHeader header) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mPager = pager;
            mHeader = header;
            mHeader.setOnHeaderChangeListener(this);
            mPager.setAdapter(this);
            mPager.setOnPageChangeListener(this);
        }
        
        public void addPage(Class<?> clss, Bundle args, String title) {
            PageInfo info = new PageInfo(clss, args);
            mPages.add(info);
            mHeader.add(0, title);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mPages.size();
        }
        
        @Override
        public Fragment getItem(int position) {
            PageInfo info = mPages.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            mHeader.setPosition(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            mHeader.setDisplayedPage(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onHeaderSelected(int position) {
            mPager.setCurrentItem(position);
        }

    }
    
    private class EliteCheck extends AsyncTask<Void, Void, Boolean> {
        
        @Override
        protected Boolean doInBackground(Void... params) {
            return Util.elitePresent(HomeActivity.this, false, 0);
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mTitleLogo.startTransition(1000);
            }
        }
    }
}
