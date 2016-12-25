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
package com.asksven.betterbatterystats.data;

import java.util.List;

import com.asksven.android.common.privateapiproxies.StatElement;

/**
 * @author sven
 *
 */
public class KbData
{
    private String title;
    private Long version;
    private List<KbEntry> entries;

    public String getTitle() { return title; }
    public Long getVersion() { return version; }
    public List<KbEntry> getEntries() { return entries; }

    public void setTitle(String title) { this.title = title; }
    public void setVersion(Long version) { this.version = version; }
    public void setEntries(List<KbEntry> entries) { this.entries = entries; }

    public String toString()
    {
        return String.format("title:%s,version:%d,entries:%s", title, version, entries);
    }
    
    public KbEntry findByName(String name)
    {
    	KbEntry ret = null;
    	List<KbEntry> entries = getEntries();
    	
    	for (int i=0; i < entries.size(); i++)
    	{
    		KbEntry comp = (KbEntry) entries.get(i);
    		if (comp.getTitle().equals(name))
    		{
    			ret = comp;
    			break;
    		}
    	}
    	return ret;
    }
    
    public KbEntry findByFqn(String fqn)
    {
    	KbEntry ret = null;

    	for (int i=0; i < entries.size(); i++)
    	{
    		KbEntry comp = (KbEntry) entries.get(i);
	    	if (comp.getFqn().equals(fqn))
			{
				ret = comp;
				break;
			}
    	}
    	return ret;
    }
    
    public KbEntry findByStatElement(String name, String fqn)
    {
    	KbEntry ret = null;

    	for (int i=0; i < entries.size(); i++)
    	{
    		KbEntry comp = (KbEntry) entries.get(i);
    		
    		// first round: search for name + fqn
	    	if ( (comp.getFqn().equals(fqn))
	    			&& (comp.getTitle().equals(name)) )
			{
				ret = comp;
				break;
			}
    		// second round: search for name only
	    	if (comp.getTitle().equals(name))
			{
				ret = comp;
				break;
			}

    	}
    	return ret;
    }

}
