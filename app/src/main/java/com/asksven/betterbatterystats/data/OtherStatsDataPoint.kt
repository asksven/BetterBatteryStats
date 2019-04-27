/*
 * Copyright (C) 2012-2018 asksven
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

package com.asksven.betterbatterystats.data


class OtherStatsDataPoint: TimeSeriesDataPoint  {
    var statName: String = "other"
    var valueName: String = ""
    var absoluteValue: Double = 0.0
    var totalValue: Double = 0.0
    var percentValue: Double = 0.0


    constructor(guid: String, valueName: String, absoluteValue: Double, totalValue: Double, percentValue: Double, time: Long): super(guid, time) {
        this.absoluteValue = absoluteValue
        this.totalValue = totalValue
        this.percentValue = percentValue
        this.valueName = valueName
    }
}