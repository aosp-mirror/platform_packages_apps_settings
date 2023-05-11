/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.deviceinfo.simstatus

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.deviceinfo.PhoneNumberUtil
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.TelephonyPreferenceDialog
import com.android.settingslib.Utils
import com.android.settingslib.qrcode.QrCodeGenerator
import com.android.settingslib.spaprivileged.framework.common.userManager

/**
 * This is to show a preference regarding EID of SIM card.
 *
 * @param preferenceKey is the key for Preference
 */
class SimEidPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {
    private var slotSimStatus: SlotSimStatus? = null
    private var eidStatus: EidStatus? = null
    private lateinit var preference: TelephonyPreferenceDialog
    private lateinit var eid: String

    fun init(slotSimStatus: SlotSimStatus?, eidStatus: EidStatus?) {
        this.slotSimStatus = slotSimStatus
        this.eidStatus = eidStatus
    }

    override fun getAvailabilityStatus(): Int {
        if (!SubscriptionUtil.isSimHardwareVisible(mContext)) return UNSUPPORTED_ON_DEVICE
        eid = eidStatus?.eid ?: ""
        val isAvailable = mContext.userManager.isAdminUser &&
            !Utils.isWifiOnly(mContext) &&
            eid.isNotEmpty()
        return if (isAvailable) AVAILABLE else CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
        val title = getTitle()
        preference.title = title
        preference.dialogTitle = title
    }

    /** Constructs title string. */
    private fun getTitle(): String {
        val slotSize = slotSimStatus?.size() ?: 0
        if (slotSize <= 1) {
            return mContext.getString(R.string.status_eid)
        }
        // Only append slot index to title when more than 1 is available
        for (idxSlot in 0 until slotSize) {
            val subInfo = slotSimStatus?.getSubscriptionInfo(idxSlot)
            if (subInfo != null && subInfo.isEmbedded) {
                return mContext.getString(R.string.eid_multi_sim, idxSlot + 1)
            }
        }
        return mContext.getString(R.string.status_eid)
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)

        updateDialog()
    }

    private fun updateDialog() {
        val dialog = preference.dialog ?: return
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        dialog.setCanceledOnTouchOutside(false)
        val textView = dialog.findViewById<TextView>(R.id.esim_id_value)
        textView.text = PhoneNumberUtil.expandByTts(eid)

        val qrCodeView = dialog.findViewById<ImageView>(R.id.esim_id_qrcode)
        qrCodeView.setImageBitmap(getEidQrCode(eid))

        // After "Tap to show", eid is displayed on preference.
        preference.summary = textView.text
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == preferenceKey) {
            this.preference.setOnShowListener { updateDialog() }
            return true
        }
        return super.handlePreferenceTreeClick(preference)
    }

    companion object {
        private const val TAG = "SimEidPreferenceController"
        private const val QR_CODE_SIZE = 600

        /**
         * Gets the QR code for EID
         * @param eid is the EID string
         * @return a Bitmap of QR code
         */
        private fun getEidQrCode(eid: String): Bitmap? = try {
            QrCodeGenerator.encodeQrCode(eid, QR_CODE_SIZE)
        } catch (exception: Exception) {
            Log.w(TAG, "Error when creating QR code width $QR_CODE_SIZE", exception)
            null
        }
    }
}
