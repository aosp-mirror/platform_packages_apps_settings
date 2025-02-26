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

import android.content.Context
import android.os.Bundle
import android.os.IUserRestrictionsListener
import android.os.UserManager
import com.android.settingslib.datastore.KeyedDataObservable
import com.android.settingslib.datastore.KeyedObserver
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/** Helper class to monitor user restriction changes. */
object UserRestrictions {
    private val observable = KeyedDataObservable<String>()

    private val userRestrictionsListener =
        object : IUserRestrictionsListener.Stub() {
            override fun onUserRestrictionsChanged(
                userId: Int,
                newRestrictions: Bundle,
                prevRestrictions: Bundle,
            ) {
                // there is no API to remove listener, do a quick check to avoid unnecessary work
                if (!observable.hasAnyObserver()) return

                val changedKeys = mutableSetOf<String>()
                val keys = newRestrictions.keySet() + prevRestrictions.keySet()
                for (key in keys) {
                    if (newRestrictions.getBoolean(key) != prevRestrictions.getBoolean(key)) {
                        changedKeys.add(key)
                    }
                }

                for (key in changedKeys) observable.notifyChange(key, 0)
            }
        }

    private val listenerAdded = AtomicBoolean()

    fun addObserver(context: Context, observer: KeyedObserver<String?>, executor: Executor) {
        context.addUserRestrictionsListener()
        observable.addObserver(observer, executor)
    }

    fun addObserver(
        context: Context,
        key: String,
        observer: KeyedObserver<String>,
        executor: Executor,
    ) {
        context.addUserRestrictionsListener()
        observable.addObserver(key, observer, executor)
    }

    private fun Context.addUserRestrictionsListener() {
        if (listenerAdded.getAndSet(true)) return
        // surprisingly, there is no way to remove the listener
        applicationContext
            .getSystemService(UserManager::class.java)
            .addUserRestrictionsListener(userRestrictionsListener)
    }

    fun removeObserver(observer: KeyedObserver<String?>) = observable.removeObserver(observer)

    fun removeObserver(key: String, observer: KeyedObserver<String>) =
        observable.removeObserver(key, observer)
}
