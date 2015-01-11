/*
 * Copyright (C) 2011 asksven
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
package com.asksven.betterbatterystats;

import java.util.ArrayList;

import com.asksven.betterbatterystats.data.Reading;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;

/**
 * @author sven
 * 
 */
public class ShareDialogFragment extends DialogFragment
{
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the Builder class for convenient dialog construction
		//ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_Light_Dialog);
		ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), BaseActivity.getTheme(getActivity()));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
		final ArrayList<Integer> selectedSaveActions = new ArrayList<Integer>();
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		boolean saveAsText = sharedPrefs.getBoolean("save_as_text", true);
		boolean saveAsJson = sharedPrefs.getBoolean("save_as_json", false);
		boolean saveLogcat = sharedPrefs.getBoolean("save_logcat", false);
		boolean saveDmesg = sharedPrefs.getBoolean("save_dmesg", false);
		
		final String m_refFromName = "";
		final String m_refToName = "";

		if (saveAsText)
		{
			selectedSaveActions.add(0);
		}
		if (saveAsJson)
		{
			selectedSaveActions.add(1);
		}
		if (saveLogcat)
		{
			selectedSaveActions.add(2);
		}
		if (saveDmesg)
		{
			selectedSaveActions.add(3);
		}
		
		builder.setTitle("Title");
		
		builder.setMultiChoiceItems(R.array.saveAsLabels, new boolean[]{saveAsText, saveAsJson, saveLogcat, saveDmesg}, new DialogInterface.OnMultiChoiceClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked)
			{
				if (isChecked)
				{
					// If the user checked the item, add it to the
					// selected items
					selectedSaveActions.add(which);
				} else if (selectedSaveActions.contains(which))
				{
					// Else, if the item is already in the array,
					// remove it
					selectedSaveActions.remove(Integer.valueOf(which));
				}
			}
		})
		// Set the action buttons
		.setPositiveButton(R.string.label_button_share, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int id)
			{
            	ArrayList<Uri> attachements = new ArrayList<Uri>();

            	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, getActivity());
	    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, getActivity());

	    		Reading reading = new Reading(getActivity(), myReferenceFrom, myReferenceTo);

				// save as text is selected
				if (selectedSaveActions.contains(0))
				{
					attachements.add(reading.writeToFileText(getActivity()));
				}
				// save as JSON if selected
				if (selectedSaveActions.contains(1))
				{
					attachements.add(reading.writeToFileJson(getActivity()));
				}
				// save logcat if selected
				if (selectedSaveActions.contains(2))
				{
					attachements.add(StatsProvider.getInstance(getActivity()).writeLogcatToFile());
				}
				// save dmesg if selected
				if (selectedSaveActions.contains(3))
				{
					attachements.add(StatsProvider.getInstance(getActivity()).writeDmesgToFile());
				}


				if (!attachements.isEmpty())
				{
					Intent shareIntent = new Intent();
					shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
					shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachements);
					shareIntent.setType("text/*");
					startActivity(Intent.createChooser(shareIntent, "Share info to.."));
				}
			}
		})
		.setNeutralButton(R.string.label_button_save, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int id)
			{

            	Reference myReferenceFrom 	= ReferenceStore.getReferenceByName(m_refFromName, getActivity());
	    		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(m_refToName, getActivity());

	    		Reading reading = new Reading(getActivity(), myReferenceFrom, myReferenceTo);

				// save as text is selected
				// save as text is selected
				if (selectedSaveActions.contains(0))
				{
					reading.writeToFileText(getActivity());
				}
				// save as JSON if selected
				if (selectedSaveActions.contains(1))
				{
					reading.writeToFileJson(getActivity());
				}
				// save logcat if selected
				if (selectedSaveActions.contains(2))
				{
					StatsProvider.getInstance(getActivity()).writeLogcatToFile();
				}
				// save dmesg if selected
				if (selectedSaveActions.contains(3))
				{
					StatsProvider.getInstance(getActivity()).writeDmesgToFile();
				}
				
			}
		}).setNegativeButton(R.string.label_button_cancel, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int id)
				{
					// do nothing
				}
			});

		// Create the AlertDialog object and return it
		return builder.create();
	}
}
