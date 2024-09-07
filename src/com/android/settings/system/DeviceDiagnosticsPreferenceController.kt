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

package com.android.settings.system

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

import androidx.preference.Preference

import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags

open class DeviceDiagnosticsPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int {
        if (!Flags.enableDeviceDiagnosticsInSettings()) {
            return UNSUPPORTED_ON_DEVICE
        }
        if (getIntent() == null) {
            return UNSUPPORTED_ON_DEVICE
        }
        return AVAILABLE
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preferenceKey != preference.key) {
            return false
        }

        val intent = getIntent()
        if (intent == null) {
            return false
        }

        preference.getContext().startActivity(intent)
        return true
    }

    private fun getIntent(): Intent? {
        val intent = Intent(Intent.ACTION_MAIN)

        val packageName = mContext.getResources().getString(
                R.string.config_device_diagnostics_package_name)
        intent.setPackage(packageName)

        val info = mContext.getPackageManager().resolveActivity(intent, 0)
        if (info == null) {
            return null
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }
}
