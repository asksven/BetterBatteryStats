/*
 * Copyright (C) 2013 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterbatterystats.fragments;

import com.actionbarsherlock.app.SherlockFragment;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.BbsApplication;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.ReferencesAdapter;
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SelectReferencesFragment extends SherlockFragment implements AdapterView.OnItemSelectedListener
{
	private ReferencesAdapter m_spinnerFromAdapter;
	private ReferencesAdapter m_spinnerToAdapter;
	private static String TAG = "SelectReferencesFragment";
	private String m_refFromName;
	private String m_refToName;

	public static SelectReferencesFragment newInstance()
	{
		SelectReferencesFragment f = new SelectReferencesFragment();
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View root = inflater.inflate(R.layout.fragment_select_references, container, false);
		
		BbsApplication app = (BbsApplication) getActivity().getApplication();
		m_refFromName = app.getRefFromName();
		m_refToName = app.getRefToName();
		
		Spinner spinnerStatType = (Spinner) root.findViewById(R.id.spinnerStatType);
		m_spinnerFromAdapter = new ReferencesAdapter(getActivity(), android.R.layout.simple_spinner_item);
		m_spinnerFromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerStatType.setAdapter(m_spinnerFromAdapter);
		// setSelection MUST be called after setAdapter
		spinnerStatType.setSelection(m_spinnerFromAdapter.getPosition(m_refFromName));
		spinnerStatType.setOnItemSelectedListener(this);

		
		Spinner spinnerStatSampleEnd = (Spinner) root.findViewById(R.id.spinnerStatSampleEnd);
		m_spinnerToAdapter = new ReferencesAdapter(getActivity(), android.R.layout.simple_spinner_item);
		m_spinnerToAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerStatSampleEnd.setAdapter(m_spinnerToAdapter);
		// setSelection must be called after setAdapter
		if ((m_refToName != null) && !m_refToName.equals("") )
		{
			int pos = m_spinnerToAdapter.getPosition(m_refToName);
			spinnerStatSampleEnd.setSelection(pos);
			
		}
		else
		{
			spinnerStatSampleEnd.setSelection(m_spinnerToAdapter.getPosition(Reference.CURRENT_REF_FILENAME));
		}
		spinnerStatSampleEnd.setOnItemSelectedListener(this);


		return root;
	}
	
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id)
	{
		BbsApplication app = (BbsApplication) getActivity().getApplication();

		// id is in the order of the spinners, 0 is stat, 1 is stat_type
		if (parent == (Spinner) getView().findViewById(R.id.spinnerStatType))
		{
			// detect if something changed
			String newStat = (String) ( (ReferencesAdapter) parent.getAdapter()).getItemName(position);

			Log.i(TAG, "Spinner from changed from " + m_refFromName + " to " + newStat);
			m_refFromName = newStat;
			app.setRefFromName(m_refFromName);
			// we need to update the second spinner
			m_spinnerToAdapter.filterToSpinner(newStat, getActivity());
			m_spinnerToAdapter.notifyDataSetChanged();
			
			// select the right element
			Spinner spinnerStatSampleEnd = (Spinner) getView().findViewById(R.id.spinnerStatSampleEnd);
			spinnerStatSampleEnd.setSelection(m_spinnerToAdapter.getPosition(m_refToName));


		}
		else if (parent == (Spinner) getView().findViewById(R.id.spinnerStatSampleEnd))
		{
			String newStat = (String) ( (ReferencesAdapter) parent.getAdapter()).getItemName(position);
			Log.i(TAG, "Spinner to changed from " + m_refToName + " to " + newStat);
			m_refToName = newStat;
			app.setRefFromName(newStat);
		}
		else
		{
    		Log.e(TAG, "ProcessStatsActivity.onItemSelected error. ID could not be resolved");
    		Toast.makeText(getActivity(), "Error: could not resolve what changed", Toast.LENGTH_SHORT).show();

		}
	}

	public void onNothingSelected(AdapterView<?> parent)
	{
	}


}
