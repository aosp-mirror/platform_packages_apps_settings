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
import android.telephony.SubscriptionManager
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle

/** Preference controller for "Phone number" */
class MobileNetworkPhoneNumberPreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(context),
) : TelephonyBasePreferenceController(context, key) {

    private lateinit var preference: Preference

    fun init(subId: Int) {
        mSubId = subId
    }

    override fun getAvailabilityStatus(subId: Int): Int =
        when {
            !Flags.isDualSimOnboardingEnabled() -> CONDITIONALLY_UNAVAILABLE
            SubscriptionManager.isValidSubscriptionId(subId) &&
                SubscriptionUtil.isSimHardwareVisible(mContext) -> AVAILABLE
            else -> CONDITIONALLY_UNAVAILABLE
        }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        subscriptionRepository.phoneNumberFlow(mSubId).collectLatestWithLifecycle(
            viewLifecycleOwner) { phoneNumber ->
                preference.summary = phoneNumber ?: getStringUnknown()
            }
    }

    private fun getStringUnknown(): String {
        return mContext.getString(R.string.device_info_default)
    }
}
