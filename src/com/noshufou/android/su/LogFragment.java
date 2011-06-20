package com.noshufou.android.su;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.noshufou.android.su.provider.PermissionsProvider.Logs;
import com.noshufou.android.su.widget.LogAdapter;
import com.noshufou.android.su.widget.PinnedHeaderListView;

public class LogFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, FragmentWithLog, OnClickListener {
    
    private LogAdapter mAdapter = null;
    private TextView mLogCountTextView = null;

    public static LogFragment newInstance() {
        return new LogFragment();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);
        
        mLogCountTextView = (TextView) view.findViewById(R.id.log_count);
        view.findViewById(R.id.clear_log_button).setOnClickListener(this);
        
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        ListFragment appList = (ListFragment) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.app_list);
        if (appList != null) {
            appList.getListView().clearChoices();
        }

        setupListView();
        getLoaderManager().initLoader(0, null, this);
    }

    private void setupListView() {
        final ListView list = getListView();
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        
        list.setDividerHeight(0);
        
        mAdapter = new LogAdapter(null, getActivity());
        setListAdapter(mAdapter);
        
        if (list instanceof PinnedHeaderListView &&
                mAdapter.getDisplaySectionHeadersEnabled()) {
            PinnedHeaderListView pinnedHeaderListView =
                (PinnedHeaderListView) list;
            View pinnedHeader = inflater.inflate(R.layout.log_list_section, list, false);
            pinnedHeaderListView.setPinnedHeaderView(pinnedHeader);
        }
        
        list.setOnScrollListener(mAdapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.clear_log_button:
            clearLog();
            break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Logs.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
        mLogCountTextView.setText(getString(R.string.log_count, cursor!=null?cursor.getCount():0));
        mLogCountTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public void clearLog(View view) {
        clearLog();
    }
    
    @Override
    public void clearLog() {
        getActivity().getContentResolver().delete(Logs.CONTENT_URI, null, null);
    }

}
