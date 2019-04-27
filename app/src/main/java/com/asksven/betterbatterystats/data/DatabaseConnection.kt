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
package com.asksven.betterbatterystats.data;

import android.util.Log
import org.influxdb.InfluxDB
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Point
import java.util.*
import org.influxdb.dto.Point.measurement
import org.influxdb.InfluxDB.ConsistencyLevel
import org.influxdb.dto.BatchPoints
import java.util.concurrent.TimeUnit

class DatabaseConnection {
    //val socket: DatagramSocket = DatagramSocket()
    // val socketAddress: SocketAddress
    val buffer: Queue<OtherStatsDataPoint> = LinkedList<OtherStatsDataPoint>()
    val sendBufferSize = 1
    val maxPossibleBufferSize = 1024
    private var influxDB: InfluxDB? = null
    private val url: String
    private val login: String
    private val password: String
    private val databaseName: String

    constructor(url: String, login: String, password: String, databaseName: String) : super() {
        this.url = url
        this.login = login
        this.password = password
        this.databaseName = databaseName
    }

    fun sendMsg(state: OtherStatsDataPoint) {
        if (buffer.size == maxPossibleBufferSize) {
            buffer.remove()
        }
        buffer.add(state)
    }

    private fun connect() {
        try {
            if (login.isNotEmpty()) {
                influxDB = InfluxDBFactory.connect(url, login, password)
            } else {
                influxDB = InfluxDBFactory.connect(url)
            }
        } catch (e:Exception ) {
            Log.i("db", e.toString())
        }
        // influxDB?.setDatabase(databaseName)
    }

    fun send() {

        connect()

        if (buffer.size >= sendBufferSize) {
            val batchPoints = BatchPoints
                    .database(databaseName)
                    .tag("uuid", buffer.elementAt(0).uuid)
                    .consistency(ConsistencyLevel.ALL)
                    .build()

            for (i in 1..buffer.size) {
                val state = buffer.poll()
                val point = Point.measurement(state.statName + " - " + state.valueName)
                        .time(state.time, TimeUnit.NANOSECONDS)
                        .addField("absoluteValue", state.absoluteValue)
                        .addField("totalValue", state.totalValue)
                        .addField("percentValue", state.percentValue)
                        .build()
                batchPoints.point(point)
            }

            try {
                influxDB?.write(batchPoints)
            } catch (e:Exception ) {
                Log.i("db.write failed", e.toString())
            }
            // Log.i("send", sendBuffer.joinToString(separator = ""))
            // sendUDP(sendBuffer.joinToString(separator = ""))
            // influxDB?.write(sendBuffer)
        }
    }

}