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

package com.android.settings.wifi

import android.annotation.StyleRes
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.OpenForTesting
import androidx.appcompat.app.AlertDialog
import com.android.settings.R
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.wifitrackerlib.WifiEntry

/**
 * Dialog for users to edit a Wi-Fi network.
 */
@OpenForTesting
open class WifiDialog2 @JvmOverloads constructor(
    context: Context,
    private val listener: WifiDialog2Listener,
    val wifiEntry: WifiEntry?,
    private val mode: Int,
    @StyleRes style: Int = 0,
    private val hideSubmitButton: Boolean = mode == WifiConfigUiBase2.MODE_VIEW,
    private val hideMeteredAndPrivacy: Boolean = false,
) : AlertDialog(context, style), WifiConfigUiBase2, DialogInterface.OnClickListener {
    /**
     * Host UI component of WifiDialog2 can receive callbacks by this interface.
     */
    interface WifiDialog2Listener {
        /**
         * To forget the Wi-Fi network.
         */
        fun onForget(dialog: WifiDialog2) {}

        /**
         * To save the Wi-Fi network.
         */
        fun onSubmit(dialog: WifiDialog2) {}

        /**
         * To trigger Wi-Fi QR code scanner.
         */
        fun onScan(dialog: WifiDialog2, ssid: String) {}
    }

    private lateinit var view: View
    private lateinit var controller: WifiConfigController2

    override fun getController(): WifiConfigController2 = controller

    override fun onCreate(savedInstanceState: Bundle?) {
        setWindowsOverlay()
        view = layoutInflater.inflate(R.layout.wifi_dialog, null)
        setView(view)
        controller = WifiConfigController2(this, view, wifiEntry, mode, hideMeteredAndPrivacy)
        super.onCreate(savedInstanceState)
        if (hideSubmitButton) {
            controller.hideSubmitButton()
        } else {
            // During creation, the submit button can be unavailable to determine visibility.
            // Right after creation, update button visibility
            controller.enableSubmitIfAppropriate()
        }
        if (wifiEntry == null) {
            controller.hideForgetButton()
        }
    }

    private fun setWindowsOverlay() {
        window?.apply {
            val lp = attributes
            setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG)
            attributes = lp
        }
    }

    override fun onStart() {
        super.onStart()
        val ssidScannerButton = requireViewById<ImageButton>(R.id.ssid_scanner_button)
        if (hideSubmitButton) {
            ssidScannerButton.visibility = View.GONE
        } else {
            ssidScannerButton.setOnClickListener {
                val ssidEditText = requireViewById<TextView>(R.id.ssid)
                val ssid = ssidEditText.text.toString()
                listener.onScan(this, ssid)
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        controller.updatePassword()
    }

    override fun dispatchSubmit() {
        listener.onSubmit(this)
        dismiss()
    }

    override fun onClick(dialogInterface: DialogInterface, id: Int) {
        when (id) {
            BUTTON_SUBMIT -> listener.onSubmit(this)
            BUTTON_FORGET -> {
                if (WifiUtils.isNetworkLockedDown(context, wifiEntry!!.wifiConfiguration)) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                        context,
                        RestrictedLockUtilsInternal.getDeviceOwner(context)
                    )
                    return
                }
                listener.onForget(this)
            }
        }
    }

    override fun getMode(): Int = mode

    override fun getSubmitButton(): Button? = getButton(BUTTON_SUBMIT)

    override fun getForgetButton(): Button? = getButton(BUTTON_FORGET)

    override fun getCancelButton(): Button? = getButton(BUTTON_NEGATIVE)

    override fun setSubmitButton(text: CharSequence) {
        setButton(BUTTON_SUBMIT, text, this)
    }

    override fun setForgetButton(text: CharSequence) {
        setButton(BUTTON_FORGET, text, this)
    }

    override fun setCancelButton(text: CharSequence) {
        setButton(BUTTON_NEGATIVE, text, this)
    }

    companion object {
        private const val BUTTON_SUBMIT = BUTTON_POSITIVE
        private const val BUTTON_FORGET = BUTTON_NEUTRAL
    }
}
