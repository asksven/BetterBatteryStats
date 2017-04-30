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
package com.asksven.android.common.kernelutils;


/**
 * This class retrieves time in state info from sysfs
 * Adapted from
 *   https://github.com/project-voodoo/android_oc-uv_stability_test
 * and
 *   https://github.com/storm717/cpuspy
 * @author sven
 *
 */
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;

import com.asksven.android.common.privateapiproxies.StatElement;

import android.util.Log;
import android.os.SystemClock;

public class CpuStates
{
	private static final String TAG = "CpuStates";
	
    // path to sysfs
    public static final String TIME_IN_STATE_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";
    public static final String VERSION_PATH = "/proc/version";


    public static ArrayList<State> getTimesInStates()
    {
    	ArrayList<State> states = new ArrayList<State>();
    	long totalTime = 0;
        try
        {
            // create a buffered reader to read in the time-in-states log
            InputStream is = new FileInputStream (TIME_IN_STATE_PATH);
            InputStreamReader ir = new InputStreamReader (is);
            BufferedReader br = new BufferedReader (ir);

            String line;
            while ( (line = br.readLine ()) != null )
            {
                // split open line and convert to Integers
                String[] nums = line.split (" ");
                
                // duration x 10 to store ms
                State myState = new State(Integer.parseInt(nums[0]), Long.parseLong(nums[1])*10);
                totalTime += myState.m_duration;
                states.add(myState);
            }

            is.close ();

        }
        catch (Exception e)
        {
            Log.e (TAG, e.getMessage() );
            return null;
        }

        // add in sleep state
        long sleepTime = SystemClock.elapsedRealtime() - SystemClock.uptimeMillis();
        states.add( new State(0, sleepTime));
        totalTime += sleepTime;
        
        // store the total time in order to be able to calculate ratio
        for (int i = 0; i < states.size(); i++ )
        {
        	states.get(i).setTotal(totalTime);
        }
        
        return states;
    }
}