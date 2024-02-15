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

package com.android.settings.network

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Looper
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.ResetNetworkRequest
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.enterprise.Restrictions
import com.android.settingslib.spaprivileged.template.preference.RestrictedPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This is to show a preference regarding resetting Bluetooth and Wi-Fi.
 */
class BluetoothWiFiResetPreferenceController(context: Context, preferenceKey: String) :
    ComposePreferenceController(context, preferenceKey) {

    private val restrictionChecker = NetworkResetRestrictionChecker(context)

    override fun getAvailabilityStatus() =
        if (restrictionChecker.hasUserRestriction()) CONDITIONALLY_UNAVAILABLE else AVAILABLE

    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        val dialogPresenter = rememberAlertDialogPresenter(
            confirmButton = AlertDialogButton(
                text = stringResource(R.string.reset_bluetooth_wifi_button_text),
            ) { reset(coroutineScope) },
            dismissButton = AlertDialogButton(text = stringResource(R.string.cancel)),
            title = stringResource(R.string.reset_bluetooth_wifi_title),
        ) {
            Text(stringResource(R.string.reset_bluetooth_wifi_desc))
        }

        RestrictedPreference(
            model = object : PreferenceModel {
                override val title = stringResource(R.string.reset_bluetooth_wifi_title)
                override val onClick = dialogPresenter::open
            },
            restrictions = Restrictions(keys = listOf(UserManager.DISALLOW_NETWORK_RESET)),
        )
    }

    /**
     * User pressed confirmation button, for starting reset operation.
     */
    private fun reset(coroutineScope: CoroutineScope) {
        // User confirm the reset operation
        featureFactory.metricsFeatureProvider
            .action(mContext, SettingsEnums.RESET_BLUETOOTH_WIFI_CONFIRM, true)

        // Run reset in background thread
        coroutineScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    resetOperation().run()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during reset", e)
                return@launch
            }
            Toast.makeText(
                mContext,
                R.string.reset_bluetooth_wifi_complete_toast,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    @VisibleForTesting
    fun resetOperation(): Runnable = if (SubscriptionUtil.isSimHardwareVisible(mContext)) {
        ResetNetworkRequest(
            ResetNetworkRequest.RESET_WIFI_MANAGER or
                ResetNetworkRequest.RESET_WIFI_P2P_MANAGER or
                ResetNetworkRequest.RESET_BLUETOOTH_MANAGER
        )
            .toResetNetworkOperationBuilder(mContext, Looper.getMainLooper())
    } else {  // For device without SIMs visible to the user
        ResetNetworkRequest(
            ResetNetworkRequest.RESET_CONNECTIVITY_MANAGER or
                ResetNetworkRequest.RESET_VPN_MANAGER or
                ResetNetworkRequest.RESET_WIFI_MANAGER or
                ResetNetworkRequest.RESET_WIFI_P2P_MANAGER or
                ResetNetworkRequest.RESET_BLUETOOTH_MANAGER
        )
            .toResetNetworkOperationBuilder(mContext, Looper.getMainLooper())
            .resetTelephonyAndNetworkPolicyManager(ResetNetworkRequest.ALL_SUBSCRIPTION_ID)
    }.build()

    private companion object {
        private const val TAG = "BluetoothWiFiResetPref"
    }
}
