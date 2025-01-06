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

import android.Manifest
import android.content.Context
import com.android.settings.R
import com.android.settings.widget.MainSwitchBarMetadata
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel

class DataSaverMainSwitchPreference : MainSwitchBarMetadata, PreferenceLifecycleProvider {

    override val key
        get() = KEY

    override val title
        get() = R.string.data_saver_switch_title

    override fun storage(context: Context) = createDataStore(context)

    override fun getReadPermissions(context: Context) =
        Permissions.allOf(Manifest.permission.MANAGE_NETWORK_POLICY)

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(Manifest.permission.MANAGE_NETWORK_POLICY)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    @Suppress("UNCHECKED_CAST")
    private class DataSaverStore(private val dataSaverBackend: DataSaverBackend) :
        AbstractKeyedDataObservable<String>(), KeyValueStore, DataSaverBackend.Listener {

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
            dataSaverBackend.isDataSaverEnabled as T?

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            dataSaverBackend.isDataSaverEnabled = value as Boolean
        }

        override fun onFirstObserverAdded() = dataSaverBackend.addListener(this)

        override fun onLastObserverRemoved() = dataSaverBackend.remListener(this)

        override fun onDataSaverChanged(isDataSaving: Boolean) =
            notifyChange(KEY, DataChangeReason.UPDATE)
    }

    companion object {
        const val KEY = "use_data_saver"

        /** Creates [KeyValueStore] for data saver preference. */
        fun createDataStore(context: Context): KeyValueStore =
            DataSaverStore(DataSaverBackend(context))
    }
}
