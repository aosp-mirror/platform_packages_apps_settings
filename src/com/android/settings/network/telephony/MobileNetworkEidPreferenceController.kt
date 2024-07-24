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
import android.graphics.Bitmap
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.deviceinfo.PhoneNumberUtil
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionInfoListViewModel
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.CustomDialogPreferenceCompat
import com.android.settingslib.Utils
import com.android.settingslib.qrcode.QrCodeGenerator
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.framework.common.userManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preference controller for "EID"
 */
open class MobileNetworkEidPreferenceController(context: Context, key: String) :
    TelephonyBasePreferenceController(context, key) {

    private lateinit var lazyViewModel: Lazy<SubscriptionInfoListViewModel>
    private lateinit var preference: CustomDialogPreferenceCompat
    private lateinit var fragment: Fragment
    private var coroutineScope: CoroutineScope? = null
    private var title = String()
    private var eid = String()

    fun init(fragment: Fragment, subId: Int) {
        this.fragment = fragment
        lazyViewModel = fragment.viewModels()
        mSubId = subId
    }

    override fun getAvailabilityStatus(subId: Int): Int = when {
        !Flags.isDualSimOnboardingEnabled() -> CONDITIONALLY_UNAVAILABLE
        SubscriptionManager.isValidSubscriptionId(subId)
                && eid.isNotEmpty()
                && mContext.userManager.isAdminUser -> AVAILABLE

        else -> CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        preference.isVisible = false

        val viewModel by lazyViewModel
        coroutineScope = viewLifecycleOwner.lifecycleScope
        viewModel.subscriptionInfoListFlow
            .map { subscriptionInfoList ->
                subscriptionInfoList
                    .firstOrNull { subInfo ->
                        subInfo.subscriptionId == mSubId && subInfo.isEmbedded
                    }
            }
            .collectLatestWithLifecycle(viewLifecycleOwner) { subscriptionInfo ->
                subscriptionInfo?.let {
                    coroutineScope?.launch {
                        refreshData(it)
                    }
                }
            }
    }

    @VisibleForTesting
    suspend fun refreshData(subscriptionInfo: SubscriptionInfo) {
        withContext(Dispatchers.Default) {
            eid = getEid(subscriptionInfo)
            if (eid.isEmpty()) {
                Log.d(TAG, "EID is empty.")
            }
            title = getTitle()
        }
        refreshUi()
    }

    fun refreshUi() {
        preference.title = title
        preference.dialogTitle = title
        preference.summary = eid
        preference.isVisible = eid.isNotEmpty()
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != preferenceKey) return false
        this.preference.setOnShowListener {
            coroutineScope?.launch { updateDialog() }
        }
        return true
    }

    private fun getTitle(): String {
        return mContext.getString(R.string.status_eid)
    }

    private suspend fun updateDialog() {
        val dialog = preference.dialog ?: return
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        dialog.setCanceledOnTouchOutside(false)
        val textView = dialog.requireViewById<TextView>(R.id.esim_id_value)
        textView.text = PhoneNumberUtil.expandByTts(eid)

        val qrCodeView = dialog.requireViewById<ImageView>(R.id.esim_id_qrcode)

        qrCodeView.setImageBitmap(getEidQrCode(eid))
    }

    protected fun getTelephonyManager(context: Context): TelephonyManager? {
        return context.getSystemService(TelephonyManager::class.java)
    }

    protected fun getEuiccManager(context: Context): EuiccManager? {
        return context.getSystemService(EuiccManager::class.java)
    }

    @VisibleForTesting
    fun getEid(subscriptionInfo: SubscriptionInfo): String {
        val euiccMgr = getEuiccManager(mContext)
        val telMgr = getTelephonyManager(mContext)
        if(euiccMgr==null || telMgr==null) return String()

        var eid = getEidPerSlot(telMgr, euiccMgr, subscriptionInfo)
        return eid.ifEmpty {
            getDefaultEid(euiccMgr)
        }
    }

    private fun getEidPerSlot(
        telMgr: TelephonyManager,
        euiccMgr: EuiccManager,
        subscriptionInfo: SubscriptionInfo
    ): String {
        val uiccCardInfoList = telMgr.uiccCardsInfo
        val cardId = subscriptionInfo.cardId

        /**
         * Find EID from first slot which contains an eSIM and with card ID within
         * the eSIM card ID provided by SubscriptionManager.
         */
        return uiccCardInfoList.firstOrNull { cardInfo -> cardInfo.isEuicc && cardInfo.cardId == cardId }
            ?.let { cardInfo ->
                var eid = cardInfo.getEid()
                if (TextUtils.isEmpty(eid)) {
                    eid = euiccMgr.createForCardId(cardInfo.cardId).getEid()
                }
                eid
            } ?: String()
    }

    private fun getDefaultEid(euiccMgr: EuiccManager?): String {
        return if (euiccMgr == null || !euiccMgr.isEnabled) {
            String()
        } else euiccMgr.getEid() ?: String()
    }

    companion object {
        private const val TAG = "MobileNetworkEidPreferenceController"
        private const val QR_CODE_SIZE = 600

        /**
         * Gets the QR code for EID
         * @param eid is the EID string
         * @return a Bitmap of QR code
         */
        private suspend fun getEidQrCode(eid: String): Bitmap? = withContext(Dispatchers.Default) {
            try {
                Log.d(TAG, "updateDialog. getEidQrCode $eid")
                QrCodeGenerator.encodeQrCode(contents = eid, size = QR_CODE_SIZE)
            } catch (exception: Exception) {
                Log.w(TAG, "Error when creating QR code width $QR_CODE_SIZE", exception)
                null
            }
        }
    }
}
