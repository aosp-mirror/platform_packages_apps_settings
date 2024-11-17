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

package com.android.settings.datausage

import android.content.Context
import com.android.settings.R
import com.android.settings.widget.MainSwitchBarMetadata
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit

class DataSaverMainSwitchPreference(context: Context) :
    MainSwitchBarMetadata, PreferenceLifecycleProvider {

    private val dataSaverBackend = DataSaverBackend(context)
    private var dataSaverBackendListener: DataSaverBackend.Listener? = null

    override val key
        get() = "use_data_saver"

    override val title
        get() = R.string.data_saver_switch_title

    override fun storage(context: Context): KeyValueStore = DataSaverStore(dataSaverBackend)

    override fun getReadPermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, value: Boolean?, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun onStart(context: PreferenceLifecycleContext) {
        val listener = DataSaverBackend.Listener { context.notifyPreferenceChange(this) }
        dataSaverBackendListener = listener
        dataSaverBackend.addListener(listener)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        dataSaverBackendListener?.let {
            dataSaverBackend.remListener(it)
            dataSaverBackendListener = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class DataSaverStore(private val dataSaverBackend: DataSaverBackend) :
        NoOpKeyedObservable<String>(), KeyValueStore {

        override fun contains(key: String) = true // just assume the datastore contains the value

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
            dataSaverBackend.isDataSaverEnabled as T?

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            dataSaverBackend.isDataSaverEnabled = value as Boolean
        }
    }
}
