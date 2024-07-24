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
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.deviceinfo.imei.ImeiInfoDialogFragment
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.Utils
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.framework.common.userManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preference controller for "IMEI"
 */
class MobileNetworkImeiPreferenceController(context: Context, key: String) :
    TelephonyBasePreferenceController(context, key) {

    private lateinit var lazyViewModel: Lazy<SubscriptionInfoListViewModel>
    private lateinit var preference: Preference
    private lateinit var fragment: Fragment
    private lateinit var mTelephonyManager: TelephonyManager
    private var simSlot = -1
    private var imei = String()
    private var title = String()

    fun init(fragment: Fragment, subId: Int) {
        this.fragment = fragment
        lazyViewModel = fragment.viewModels()
        mSubId = subId
        mTelephonyManager = mContext.getSystemService(TelephonyManager::class.java)
            ?.createForSubscriptionId(mSubId)!!
    }

    override fun getAvailabilityStatus(subId: Int): Int = when {
        !Flags.isDualSimOnboardingEnabled() -> CONDITIONALLY_UNAVAILABLE
        SubscriptionManager.isValidSubscriptionId(subId)
                && SubscriptionUtil.isSimHardwareVisible(mContext)
                && mContext.userManager.isAdminUser
                && !Utils.isWifiOnly(mContext) -> AVAILABLE
        else -> CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        val viewModel by lazyViewModel
        val coroutineScope = viewLifecycleOwner.lifecycleScope

        viewModel.subscriptionInfoListFlow
                .collectLatestWithLifecycle(viewLifecycleOwner) { subscriptionInfoList ->
                    subscriptionInfoList
                            .firstOrNull { subInfo -> subInfo.subscriptionId == mSubId }
                            ?.let {
                                coroutineScope.launch {
                                    refreshData(it)
                                }
                            }
                }
    }

    @VisibleForTesting
    suspend fun refreshData(subscription:SubscriptionInfo){
        withContext(Dispatchers.Default) {
            title = getTitle()
            imei = getImei()
            simSlot = subscription.simSlotIndex
        }
        refreshUi()
    }

    private fun refreshUi(){
        preference.title = title
        preference.summary = imei
        preference.isVisible = true
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != preferenceKey) return false

        Log.d(TAG, "handlePreferenceTreeClick:")
        ImeiInfoDialogFragment.show(fragment, simSlot, preference.title.toString())
        return true
    }
    private fun getImei(): String {
        val phoneType = getPhoneType()
        return if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) mTelephonyManager.meid?: String()
                else mTelephonyManager.imei?: String()
    }
    private fun getTitleForGsmPhone(): String {
        return mContext.getString(R.string.status_imei)
    }

    private fun getTitleForCdmaPhone(): String {
        return mContext.getString(R.string.status_meid_number)
    }

    private fun getTitle(): String {
        val phoneType = getPhoneType()
        return if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) getTitleForCdmaPhone()
                else getTitleForGsmPhone()
    }

    fun getPhoneType(): Int {
        return mTelephonyManager.currentPhoneType
    }

    companion object {
        private const val TAG = "MobileNetworkImeiPreferenceController"
    }
}
