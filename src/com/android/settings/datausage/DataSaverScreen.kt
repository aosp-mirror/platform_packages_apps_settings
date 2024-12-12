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
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import com.android.settings.R
import com.android.settings.Settings.DataSaverSummaryActivity
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator
import com.android.settings.datausage.DataSaverMainSwitchPreference.Companion.KEY as DATA_SAVER_KEY

@ProvidePreferenceScreen
class DataSaverScreen(context: Context) :
    PreferenceScreenCreator,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    private val dataSaverStore = DataSaverMainSwitchPreference.createDataStore(context)
    private lateinit var keyedObserver: KeyedObserver<String>

    override val key
        get() = KEY

    override val title
        get() = R.string.data_saver_title

    override val icon: Int
        get() = R.drawable.ic_settings_data_usage

    override fun isIndexable(context: Context) =
        DataUsageUtils.hasMobileData(context) &&
            DataUsageUtils.getDefaultSubscriptionId(context) != INVALID_SUBSCRIPTION_ID

    override fun getSummary(context: Context): CharSequence? =
        when {
            dataSaverStore.getBoolean(DATA_SAVER_KEY) == true ->
                context.getString(R.string.data_saver_on)
            else -> context.getString(R.string.data_saver_off)
        }

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_data_saver)

    override fun isFlagEnabled(context: Context) = Flags.catalystRestrictBackgroundParentEntry()

    override fun fragmentClass() = DataSaverSummary::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, DataSaverSummaryActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(this) { +DataSaverMainSwitchPreference() }

    override fun hasCompleteHierarchy() = false

    override fun onCreate(context: PreferenceLifecycleContext) {
        keyedObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(KEY) }
        dataSaverStore.addObserver(DATA_SAVER_KEY, keyedObserver, HandlerExecutor.main)
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        dataSaverStore.removeObserver(DATA_SAVER_KEY, keyedObserver)
    }

    companion object {
        const val KEY = "restrict_background_parent_entry"
    }
}
