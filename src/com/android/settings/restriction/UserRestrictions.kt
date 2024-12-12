/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.restriction

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.KeyedObserver
import java.util.concurrent.Executor

/** Helper class to monitor user restriction changes. */
class UserRestrictions private constructor(private val applicationContext: Context) {

    private val observable =
        object : AbstractKeyedDataObservable<String>() {
            override fun onFirstObserverAdded() {
                val intentFilter = IntentFilter()
                intentFilter.addAction(UserManager.ACTION_USER_RESTRICTIONS_CHANGED)
                applicationContext.registerReceiver(broadcastReceiver, intentFilter)
            }

            override fun onLastObserverRemoved() {
                applicationContext.unregisterReceiver(broadcastReceiver)
            }
        }

    private val broadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // there is no way to get the changed keys, just notify all observers
                observable.notifyChange(DataChangeReason.UPDATE)
            }
        }

    fun addObserver(observer: KeyedObserver<String?>, executor: Executor) =
        observable.addObserver(observer, executor)

    fun addObserver(key: String, observer: KeyedObserver<String>, executor: Executor) =
        observable.addObserver(key, observer, executor)

    fun removeObserver(observer: KeyedObserver<String?>) = observable.removeObserver(observer)

    fun removeObserver(key: String, observer: KeyedObserver<String>) =
        observable.removeObserver(key, observer)

    companion object {
        @Volatile private var instance: UserRestrictions? = null

        fun get(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: UserRestrictions(context.applicationContext).also { instance = it }
                }
    }
}
