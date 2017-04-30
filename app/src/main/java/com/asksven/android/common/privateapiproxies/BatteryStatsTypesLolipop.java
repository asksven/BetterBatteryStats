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
package com.asksven.android.common.privateapiproxies;




/**
 * This class holds the required constants from BatteryStats
 * Copyright (C) 2008 The Android Open Source Project applies
 * @see android.os.BatteryStats
 * @author sven
 *
 */
public class BatteryStatsTypesLolipop extends BatteryStatsTypes
{

    /** Include all of the data in the stats, including previously saved data */
    public static final int STATS_SINCE_CHARGED = 0;

    /** Include only the current run in the stats */
    public static final int STATS_CURRENT = 1;

    /** Include only the run since the last time the device was unplugged in the stats */
    public static final int STATS_SINCE_UNPLUGGED = 2;
    
}
