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

package com.android.settings.network.telephony

import android.content.Context
import android.os.UserManager
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spaprivileged.model.enterprise.Restrictions
import com.android.settingslib.spaprivileged.template.preference.RestrictedSwitchPreference

/** Preference controller for "Roaming" */
class RoamingPreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val mobileDataRepository: MobileDataRepository = MobileDataRepository(context),
) : ComposePreferenceController(context, key) {
    @VisibleForTesting var fragmentManager: FragmentManager? = null
    private var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    private var telephonyManager = context.getSystemService(TelephonyManager::class.java)!!
    private val carrierConfigRepository = CarrierConfigRepository(context)
    private val roamingSearchItem = RoamingSearchItem(context)

    fun init(fragmentManager: FragmentManager, subId: Int) {
        this.fragmentManager = fragmentManager
        this.subId = subId
        telephonyManager = telephonyManager.createForSubscriptionId(subId)
    }

    override fun getAvailabilityStatus() =
        if (roamingSearchItem.isAvailable(subId)) AVAILABLE else CONDITIONALLY_UNAVAILABLE

    @Composable
    override fun Content() {
        val summary = stringResource(R.string.roaming_enable)
        val isDataRoamingEnabled by
            remember { mobileDataRepository.isDataRoamingEnabledFlow(subId) }
                .collectAsStateWithLifecycle(null)
        RestrictedSwitchPreference(
            model =
                object : SwitchPreferenceModel {
                    override val title = stringResource(R.string.roaming)
                    override val summary = { summary }
                    override val checked = { isDataRoamingEnabled }
                    override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                        if (newChecked && isDialogNeeded()) {
                            showDialog()
                        } else {
                            // Update data directly if we don't need dialog
                            telephonyManager.isDataRoamingEnabled = newChecked
                        }
                    }
                },
            restrictions = Restrictions(keys = listOf(UserManager.DISALLOW_DATA_ROAMING)),
        )
    }

    @VisibleForTesting
    fun isDialogNeeded(): Boolean {
        // Need dialog if we need to turn on roaming and the roaming charge indication is allowed
        return !carrierConfigRepository.getBoolean(
            subId, CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL)
    }

    private fun showDialog() {
        fragmentManager?.let { RoamingDialogFragment.newInstance(subId).show(it, DIALOG_TAG) }
    }

    companion object {
        private const val DIALOG_TAG = "MobileDataDialog"

        class RoamingSearchItem(private val context: Context) : MobileNetworkSettingsSearchItem {
            private val carrierConfigRepository = CarrierConfigRepository(context)

            fun isAvailable(subId: Int): Boolean =
                SubscriptionManager.isValidSubscriptionId(subId) &&
                    !carrierConfigRepository.getBoolean(
                        subId, CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL)

            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (!isAvailable(subId)) return null
                return MobileNetworkSettingsSearchResult(
                    key = "button_roaming_key",
                    title = context.getString(R.string.roaming),
                )
            }
        }
    }
}
