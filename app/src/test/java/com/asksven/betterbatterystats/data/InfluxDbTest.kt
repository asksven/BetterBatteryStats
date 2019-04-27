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


import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    var database: DatabaseConnection? = null

//    @Test
//    fun testConnection() {
//        this.connect()
//        assert(database != null)
//    }
//
//    @Test
//    fun testWrite() {
//        this.connect()
//        assert(database != null)
//
//        for (i in 0..20) {
//            val now = System.currentTimeMillis()
//            database?.sendMsg(OtherStatsDataPoint("tes-uuid", "test-value", 125.0 + i, 30000.0+(100*i), 20.0 + (2 * i), now * 1000 * 1000))
//        }
//
//        database?.send()
//    }
//
//
//    private fun connect() {
//        var address = "https://influxdb.gke-dev.asksven.io"
//        var login = "admin"
//        var password = "qQZqEtD5yK"
//        var dbName = "tests"
//
//        database = DatabaseConnection(address, login, password, dbName)
//
//        database?.start()
//
//    }
}