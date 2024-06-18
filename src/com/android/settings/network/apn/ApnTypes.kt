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

package com.android.settings.network.apn

import android.content.Context
import android.telephony.data.ApnSetting
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.android.settings.R
import com.android.settingslib.spa.widget.editor.SettingsDropdownCheckOption

object ApnTypes {
    private const val TAG = "ApnTypes"

    private val APN_TYPES = arrayOf(
        ApnSetting.TYPE_DEFAULT_STRING,
        ApnSetting.TYPE_MMS_STRING,
        ApnSetting.TYPE_SUPL_STRING,
        ApnSetting.TYPE_DUN_STRING,
        ApnSetting.TYPE_HIPRI_STRING,
        ApnSetting.TYPE_FOTA_STRING,
        ApnSetting.TYPE_IMS_STRING,
        ApnSetting.TYPE_CBS_STRING,
        ApnSetting.TYPE_IA_STRING,
        ApnSetting.TYPE_EMERGENCY_STRING,
        ApnSetting.TYPE_MCX_STRING,
        ApnSetting.TYPE_XCAP_STRING,
        ApnSetting.TYPE_VSIM_STRING,
        ApnSetting.TYPE_BIP_STRING,
        ApnSetting.TYPE_ENTERPRISE_STRING,
    )

    private fun splitToList(apnType: String): List<String> {
        val types = apnType.split(',').map { it.trim().toLowerCase(Locale.current) }
        if (ApnSetting.TYPE_ALL_STRING in types || APN_TYPES.all { it in types }) {
            return listOf(ApnSetting.TYPE_ALL_STRING)
        }
        return APN_TYPES.filter { it in types }
    }

    fun isApnTypeReadOnly(apnType: String, readOnlyTypes: List<String>): Boolean {
        val apnTypes = splitToList(apnType)
        return ApnSetting.TYPE_ALL_STRING in readOnlyTypes ||
            ApnSetting.TYPE_ALL_STRING in apnTypes && readOnlyTypes.isNotEmpty() ||
            apnTypes.any { it in readOnlyTypes }
    }

    fun getOptions(context: Context, apnType: String, readOnlyTypes: List<String>) = buildList {
        val apnTypes = splitToList(apnType)
        add(
            context.createSettingsDropdownCheckOption(
                text = ApnSetting.TYPE_ALL_STRING,
                isSelectAll = true,
                changeable = readOnlyTypes.isEmpty(),
                selected = ApnSetting.TYPE_ALL_STRING in apnTypes,
            )
        )
        for (type in APN_TYPES) {
            add(
                context.createSettingsDropdownCheckOption(
                    text = type,
                    changeable = ApnSetting.TYPE_ALL_STRING !in readOnlyTypes &&
                        type !in readOnlyTypes,
                    selected = ApnSetting.TYPE_ALL_STRING in apnTypes || type in apnTypes,
                )
            )
        }
    }.also { Log.d(TAG, "APN Type options: $it") }

    private fun Context.createSettingsDropdownCheckOption(
        text: String,
        isSelectAll: Boolean = false,
        changeable: Boolean,
        selected: Boolean,
    ) = SettingsDropdownCheckOption(
        text = text,
        isSelectAll = isSelectAll,
        changeable = changeable,
        selected = mutableStateOf(selected),
    ) {
        if (!changeable) {
            val message = resources.getString(R.string.error_adding_apn_type, text)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun List<SettingsDropdownCheckOption>.toApnType(): String {
        val (selectAllOptions, regularOptions) = partition { it.isSelectAll }
        for (selectAllOption in selectAllOptions) {
            if (selectAllOption.selected.value) return ApnSetting.TYPE_ALL_STRING
        }
        return regularOptions.filter { it.selected.value }.joinToString(",") { it.text }
    }

    private val NotPreSelectedTypes = setOf(
        ApnSetting.TYPE_IMS_STRING,
        ApnSetting.TYPE_IA_STRING,
        ApnSetting.TYPE_EMERGENCY_STRING,
        ApnSetting.TYPE_MCX_STRING,
    )

    fun getPreSelectedApnType(customizedConfig: CustomizedConfig): String =
        (customizedConfig.defaultApnTypes
            ?: defaultPreSelectedApnTypes(customizedConfig.readOnlyApnTypes))
            .joinToString(",")

    private fun defaultPreSelectedApnTypes(readOnlyApnTypes: List<String>) =
        if (ApnSetting.TYPE_ALL_STRING in readOnlyApnTypes) emptyList()
        else APN_TYPES.filter { it !in readOnlyApnTypes + NotPreSelectedTypes }
}
