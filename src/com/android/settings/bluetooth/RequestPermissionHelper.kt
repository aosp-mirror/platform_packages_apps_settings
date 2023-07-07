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

package com.android.settings.bluetooth

import android.content.Context
import android.os.UserManager
import androidx.appcompat.app.AlertDialog
import com.android.settings.R
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.framework.common.userManager

object RequestPermissionHelper {
    fun requestEnable(
        context: Context,
        appLabel: CharSequence?,
        timeout: Int,
        onAllow: () -> Unit,
        onDeny: () -> Unit,
    ): AlertDialog? {
        if (context.resources.getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog)) {
            // Don't even show the dialog if configured this way
            onAllow()
            return null
        }
        return AlertDialog.Builder(context).apply {
            setMessage(context.getEnableMessage(timeout, appLabel))
            setPositiveButton(R.string.allow) { _, _ ->
                if (context.isDisallowBluetooth()) onDeny() else onAllow()
            }
            setNegativeButton(R.string.deny) { _, _ -> onDeny() }
            setOnCancelListener { onDeny() }
        }.create()
    }

    fun requestDisable(
        context: Context,
        appLabel: CharSequence?,
        onAllow: () -> Unit,
        onDeny: () -> Unit,
    ): AlertDialog? {
        if (context.resources.getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog)) {
            // Don't even show the dialog if configured this way
            onAllow()
            return null
        }
        return AlertDialog.Builder(context).apply {
            setMessage(context.getDisableMessage(appLabel))
            setPositiveButton(R.string.allow) { _, _ -> onAllow() }
            setNegativeButton(R.string.deny) { _, _ -> onDeny() }
            setOnCancelListener { onDeny() }
        }.create()
    }
}

// If Bluetooth is disallowed, don't try to enable it, show policy transparency message instead.
private fun Context.isDisallowBluetooth() =
    if (userManager.hasUserRestriction(UserManager.DISALLOW_BLUETOOTH)) {
        devicePolicyManager.createAdminSupportIntent(UserManager.DISALLOW_BLUETOOTH)
            ?.let { startActivity(it) }
        true
    } else false

private fun Context.getEnableMessage(timeout: Int, name: CharSequence?): String = when {
    timeout < 0 -> when (name) {
        null -> getString(R.string.bluetooth_ask_enablement_no_name)
        else -> getString(R.string.bluetooth_ask_enablement, name)
    }

    timeout == BluetoothDiscoverableEnabler.DISCOVERABLE_TIMEOUT_NEVER -> when (name) {
        null -> getString(R.string.bluetooth_ask_enablement_and_lasting_discovery_no_name)
        else -> getString(R.string.bluetooth_ask_enablement_and_lasting_discovery, name)
    }

    else -> when (name) {
        null -> getString(R.string.bluetooth_ask_enablement_and_discovery_no_name, timeout)
        else -> getString(R.string.bluetooth_ask_enablement_and_discovery, name, timeout)
    }
}

private fun Context.getDisableMessage(name: CharSequence?): String =
    when (name) {
        null -> getString(R.string.bluetooth_ask_disablement_no_name)
        else -> getString(R.string.bluetooth_ask_disablement, name)
    }
