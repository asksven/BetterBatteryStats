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

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.List;

import android.util.Log;

import com.asksven.android.common.dto.StateDto;
import com.asksven.android.common.nameutils.UidInfo;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.google.gson.annotations.SerializedName;


/**
 * Value holder for CpuState
 * @author sven
 *
 */

/** simple struct for states/time */
public class State extends StatElement implements Comparable<State>, Serializable
{
	/** 
	 * the tag for logging
	 */
	private transient static final String TAG = "Process";

	private static final long serialVersionUID = 1L;
	
	
	@SerializedName("freq")
	public int m_freq = 0;
	
	@SerializedName("duration_ms")
    public long m_duration = 0;

	public State()
	{
	
	}
	
    public State(int freq, long duration)
    {
    	m_freq 		= freq;
    	m_duration 	= duration;
    	
    }

    public State(StateDto source)
    {
		this.setUid(source.m_uid);
		this.m_duration			= source.m_duration;
		this.m_freq				= source.m_freq;
		this.setTotal(source.m_total);
    }

    public State clone()
    {
    	State clone = new State(m_freq, m_duration);
    	clone.setTotal(this.getTotal());
    	return clone;
    }
    
    public StateDto toDto()
    {
    	StateDto ret = new StateDto();
		ret.m_uid 				= this.getuid();
		ret.m_duration			= this.m_duration;
		ret.m_freq				= this.m_freq;
		ret.m_total				= this.getTotal();
		
    	return ret;
    }
    
    public String getName()
    {
    	String ret = formatFreq(m_freq);
    	if (ret.equals("0 kHz"))
    	{
    		ret = "Deep Sleep";
    	}
    	return ret;
    }
    
    public String toString()
    {
    	return getName() + " " + m_duration;
    }

    public String getData(long totalTime)
    {
    	return formatDuration(m_duration) + " " + this.formatRatio(m_duration, totalTime);
    }

	/**
	 * returns a string representation of the data
	 */
	public String getVals()
	{
		
		return getName() + " " + this.formatDuration(m_duration) + " (" + m_duration/1000 + " s)"
				+ " in " + this.formatDuration(getTotal()) + " (" + getTotal()/1000 + " s)"
		+ " Ratio: " + formatRatio(m_duration, getTotal());
	}

	/** 
	 * returns the values of the data
	 */	
	public double[] getValues()
	{
		double[] retVal = new double[2];
		retVal[0] = m_duration;
		return retVal;
	}

    /**
     * Formats a freq in Hz in a readable form
     * @param freqHz
     * @return
     */
    String formatFreq(int freqkHz)
    {
    	double freq = freqkHz;
    	double freqMHz = freq / 1000;
    	double freqGHz = freq / 1000 / 1000;

    	String formatedFreq = "";
    	DecimalFormat df = new DecimalFormat("#.##");
        
    	if (freqGHz >= 1)
    	{
    		formatedFreq = df.format(freqGHz) + " GHz";
    	}
    	else if (freqMHz >= 1)
    	{
    		formatedFreq = df.format(freqMHz) + " MHz";
    	}

    	else
    	{
    		formatedFreq = df.format(freqkHz) + " kHz";
    	}
    	
    	return formatedFreq;

    }
    
	/**
	 * Substracts the values from a previous object
	 * found in myList from the current Process
	 * in order to obtain an object containing only the data since a referenc
	 * @param myList
	 */
	public void substractFromRef(List<StatElement> myList )
	{
		if (myList != null)
		{
			for (int i = 0; i < myList.size(); i++)
			{
				try
				{
					State myRef = (State) myList.get(i);
					if ( (this.getName().equals(myRef.getName())) && (this.getuid() == myRef.getuid()) )
					{
						this.m_duration 	-= myRef.m_duration;
						this.setTotal( this.getTotal() - myRef.getTotal() );
						if (m_duration < 0)
						{
							Log.e(TAG, "substractFromRef generated negative values (" + this.m_duration + " - " + myRef.m_duration + ")");
						}
						break;
					}
				}
				catch (ClassCastException e)
				{
					// just log as it is no error not to change the process
					// being substracted from to do nothing
					Log.e(TAG, "substractFromRef was called with a wrong list type");
				}
				
			}
		}
	}
	
	/**
     * Compare a given Wakelock with this object.
     * If the duration of this object is 
     * greater than the received object,
     * then this object is greater than the other.
     */
	public int compareTo(State o)
	{
		// we want to sort in descending order
		return ((int)( (o.m_freq) - (this.m_freq) ));
	}


}
