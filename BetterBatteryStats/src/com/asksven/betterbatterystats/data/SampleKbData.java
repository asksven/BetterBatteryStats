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

/**
 * COnstant pattern: Delivers sample KB data
 * @author sven
 *
 */
public class SampleKbData
{
	public static String json = 
	        "{"
	            + "'title': 'BetterBatteryStats KB',"
	            + "'version' : 1,"
	            + "'entries' : ["
	            	+ "{"
	                + "'fqn' : 'AlarmManager',"
	                + "'title' : 'Alarm Manager',"
	                + "'url' : 'http://'"
	                + "},"
	            	+ "{"
	                + "'fqn' : '*network-location*',"
	                + "'title' : 'Location Service',"
	                + "'url' : 'http://'"
	                + "}"
	                
	            + "]"
	        + "}";


	public static String json2 = 
	        "{"
	            + "'title': 'Computing and Information systems',"
	            + "'id' : 1,"
	            + "'children' : 'true',"
	            + "'groups' : [{"
	                + "'title' : 'Level one CIS',"
	                + "'id' : 2,"
	                + "'children' : 'true',"
	                + "'groups' : [{"
	                    + "'title' : 'Intro To Computing and Internet',"
	                    + "'id' : 3,"
	                    + "'children': 'false',"
	                    + "'groups':[]"
	                + "}]" 
	            + "}]"
	        + "}";

}
