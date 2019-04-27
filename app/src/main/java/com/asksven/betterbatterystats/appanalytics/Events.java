/*
 * Copyright (C) 2017 asksven
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
package com.asksven.betterbatterystats.appanalytics;

/**
 * Created by sven on 30/04/2017.
 */

public class Events
{

    public static final String EVENT_LAUNCH_ROOTED = "launch_device_rooted";
    public static final String EVENT_LAUNCH_UNROOTED = "launch_device_unrooted";
    public static final String EVENT_LAUNCH_GPLAY = "launch_edition_gplay";
    public static final String EVENT_LAUNCH_XDA = "launch_edition_xda";
    public static final String EVENT_LAUNCH_DARK_THEME = "launch_theme_dark";
    public static final String EVENT_LAUNCH_LIGHT_THEME = "launch_theme_light";
    public static final String EVENT_LAUNCH_ROUND_GAUGES = "launch_style_roundgauges";
    public static final String EVENT_LAUNCH_LINEAR_GAUGES = "launch_sytle_lineargauges";


    public static final String EVENT_PROCESS_WATCHDOG = "process_watchdog";

    public static final String EVENT_PERFORM_SAVEDUMPFILE = "perform_savedumpfile";
    public static final String EVENT_PERFORM_SAVETIMESERIES = "perform_savetimeseries";


}
