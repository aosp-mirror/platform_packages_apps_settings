/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.metrics

import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.PreferenceActionMetricsProvider
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settingslib.graph.PreferenceGetterErrorCode
import com.android.settingslib.graph.PreferenceSetterResult
import com.android.settingslib.metadata.PreferenceCoordinate
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceRemoteOpMetricsLogger
import com.android.settingslib.metadata.PreferenceScreenMetadata

/** Metrics logger for settings remote operations. */
class SettingsRemoteOpMetricsLogger : PreferenceRemoteOpMetricsLogger {

    override fun logGetterApi(
        context: Context,
        callingUid: Int,
        preferenceCoordinate: PreferenceCoordinate,
        screen: PreferenceScreenMetadata?,
        preference: PreferenceMetadata?,
        errorCode: Int,
        latencyMs: Long,
    ) =
        SettingsStatsLog.SETTINGS_EXT_API_REPORTED__TYPE__ACTION_READ.log(
            context,
            callingUid,
            preferenceCoordinate,
            preference,
            errorCode,
            latencyMs,
            Int::convertGetterErrorCode,
        )

    override fun logSetterApi(
        context: Context,
        callingUid: Int,
        preferenceCoordinate: PreferenceCoordinate,
        screen: PreferenceScreenMetadata?,
        preference: PreferenceMetadata?,
        errorCode: Int,
        latencyMs: Long,
    ) =
        SettingsStatsLog.SETTINGS_EXT_API_REPORTED__TYPE__ACTION_WRITE.log(
            context,
            callingUid,
            preferenceCoordinate,
            preference,
            errorCode,
            latencyMs,
            Int::convertSetterErrorCode,
        )

    private fun Int.log(
        context: Context,
        callingUid: Int,
        preferenceCoordinate: PreferenceCoordinate,
        preference: PreferenceMetadata?,
        errorCode: Int,
        latencyMs: Long,
        errorCodeToMetricsResult: (Int) -> Int,
    ) {
        if (preference is PreferenceActionMetricsProvider) {
            SettingsStatsLog.write(
                SettingsStatsLog.SETTINGS_EXTAPI_REPORTED,
                context.packageNameOfUid(callingUid),
                "",
                this,
                errorCodeToMetricsResult(errorCode),
                latencyMs,
                preference.preferenceActionMetrics,
            )
        } else {
            SettingsStatsLog.write(
                SettingsStatsLog.SETTINGS_EXTAPI_REPORTED,
                context.packageNameOfUid(callingUid),
                preferenceCoordinate.settingsId,
                this,
                errorCodeToMetricsResult(errorCode),
                latencyMs,
                SettingsEnums.ACTION_UNKNOWN,
            )
        }
    }

    override fun logGraphApi(context: Context, callingUid: Int, success: Boolean, latencyMs: Long) {
        val result =
            if (success) {
                SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_OK
            } else {
                SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_INTERNAL_ERROR
            }
        SettingsStatsLog.write(
            SettingsStatsLog.SETTINGS_EXTAPI_REPORTED,
            context.packageNameOfUid(callingUid),
            "",
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__TYPE__ACTION_GET_METADATA,
            result,
            latencyMs,
            SettingsEnums.ACTION_UNKNOWN,
        )
    }
}

private fun Context.packageNameOfUid(uid: Int) = packageManager.getNameForUid(uid) ?: ""

private val PreferenceCoordinate.settingsId: String
    get() = "$screenKey/$key"

private fun Int.convertGetterErrorCode() =
    when (this) {
        PreferenceGetterErrorCode.OK ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_OK
        PreferenceGetterErrorCode.NOT_FOUND ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_UNSUPPORTED
        PreferenceGetterErrorCode.DISALLOW ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_DISALLOW
        else -> SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_INTERNAL_ERROR
    }

private fun Int.convertSetterErrorCode() =
    when (this) {
        PreferenceSetterResult.OK -> SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_OK
        PreferenceSetterResult.UNSUPPORTED ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_UNSUPPORTED
        PreferenceSetterResult.DISABLED ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_DISABLED
        PreferenceSetterResult.RESTRICTED ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_RESTRICTED
        PreferenceSetterResult.UNAVAILABLE ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_UNAVAILABLE
        PreferenceSetterResult.REQUIRE_APP_PERMISSION ->
            SettingsStatsLog
                .SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_REQUIRE_APP_PERMISSION
        PreferenceSetterResult.REQUIRE_USER_AGREEMENT ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_REQUIRE_USER_CONSENT
        PreferenceSetterResult.DISALLOW ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_DISALLOW
        PreferenceSetterResult.INVALID_REQUEST ->
            SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_INVALID_REQUEST
        else -> SettingsStatsLog.SETTINGS_EXT_API_REPORTED__RESULT__RESULT_FAILURE_INTERNAL_ERROR
    }
