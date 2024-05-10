/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.data.ApnSetting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import com.android.settings.network.MobileDataContentObserver

/**
 * Preference controller for "MMS messages"
 */
class MmsMessagePreferenceController(context: Context, key: String) :
    TelephonyTogglePreferenceController(context, key), DefaultLifecycleObserver {

    private lateinit var telephonyManager: TelephonyManager

    private var preferenceScreen: PreferenceScreen? = null

    private val mobileDataContentObserver =
        MobileDataContentObserver(Handler(Looper.getMainLooper())).apply {
            setOnMobileDataChangedListener {
                preferenceScreen?.let { super.displayPreference(it) }
            }
        }

    fun init(subId: Int) {
        mSubId = subId
        telephonyManager = mContext.getSystemService(TelephonyManager::class.java)!!
            .createForSubscriptionId(subId)
    }

    override fun getAvailabilityStatus(subId: Int) =
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
            !telephonyManager.isDataEnabled &&
            telephonyManager.isApnMetered(ApnSetting.TYPE_MMS)
        ) AVAILABLE else CONDITIONALLY_UNAVAILABLE

    override fun onStart(owner: LifecycleOwner) {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mobileDataContentObserver.register(mContext, mSubId)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mobileDataContentObserver.unRegister(mContext)
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preferenceScreen = screen
    }

    override fun isChecked(): Boolean = telephonyManager.isMobileDataPolicyEnabled(
        TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED
    )

    override fun setChecked(isChecked: Boolean): Boolean {
        telephonyManager.setMobileDataPolicyEnabled(
            TelephonyManager.MOBILE_DATA_POLICY_MMS_ALWAYS_ALLOWED,
            isChecked,
        )
        return true
    }
}
