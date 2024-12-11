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

package com.android.settings.service

import android.content.Context
import android.service.settings.preferences.GetValueRequest
import android.service.settings.preferences.GetValueResult
import android.service.settings.preferences.MetadataResult
import android.service.settings.preferences.SetValueRequest
import android.service.settings.preferences.SetValueResult
import android.service.settings.preferences.SettingsPreferenceMetadata
import android.service.settings.preferences.SettingsPreferenceValue
import com.android.settingslib.graph.PreferenceCoordinate
import com.android.settingslib.graph.PreferenceGetterErrorCode
import com.android.settingslib.graph.PreferenceGetterFlags
import com.android.settingslib.graph.PreferenceGetterRequest
import com.android.settingslib.graph.PreferenceGetterResponse
import com.android.settingslib.graph.PreferenceSetterRequest
import com.android.settingslib.graph.PreferenceSetterResult
import com.android.settingslib.graph.preferenceValueProto
import com.android.settingslib.graph.proto.PreferenceProto
import com.android.settingslib.graph.proto.PreferenceValueProto
import com.android.settingslib.graph.getText
import com.android.settingslib.graph.proto.PreferenceGraphProto
import com.android.settingslib.graph.proto.PreferenceOrGroupProto
import com.android.settingslib.graph.toIntent
import com.android.settingslib.metadata.SensitivityLevel

/** Transform Catalyst Graph result to Framework GET METADATA result */
fun transformCatalystGetMetadataResponse(
    context: Context,
    graph: PreferenceGraphProto
): MetadataResult {
    val preferences = mutableSetOf<PreferenceWithScreen>()
    // recursive function to visit all nodes in preference group
    fun traverseGroupOrPref(
        screenKey: String,
        groupOrPref: PreferenceOrGroupProto,
    ) {
        when (groupOrPref.kindCase) {
            PreferenceOrGroupProto.KindCase.PREFERENCE ->
                preferences.add(
                    PreferenceWithScreen(screenKey, groupOrPref.preference)
                )
            PreferenceOrGroupProto.KindCase.GROUP -> {
                for (child in groupOrPref.group.preferencesList) {
                    traverseGroupOrPref(screenKey, child)
                }
            }
            else -> {}
        }
    }
    // traverse all screens and all preferences on screen
    for ((screenKey, screen) in graph.screensMap) {
        for (groupOrPref in screen.root.preferencesList) {
            traverseGroupOrPref(screenKey, groupOrPref)
        }
    }

    return if (preferences.isNotEmpty()) {
        MetadataResult.Builder(MetadataResult.RESULT_OK)
            .setMetadataList(
                preferences.map {
                    it.preference.toMetadata(context, it.screenKey)
                }
            )
            .build()
    } else {
        MetadataResult.Builder(MetadataResult.RESULT_UNSUPPORTED).build()
    }
}

/** Translate Framework GET VALUE request to Catalyst GET VALUE request */
fun transformFrameworkGetValueRequest(
    request: GetValueRequest,
    flags: Int = PreferenceGetterFlags.ALL
): PreferenceGetterRequest {
    val coord = PreferenceCoordinate(request.screenKey, request.preferenceKey)
    return PreferenceGetterRequest(
        arrayOf(coord),
        flags
    )
}

/** Translate Catalyst GET VALUE result to Framework GET VALUE result */
fun transformCatalystGetValueResponse(
    context: Context,
    request: GetValueRequest,
    response: PreferenceGetterResponse
): GetValueResult? {
    val coord = PreferenceCoordinate(request.screenKey, request.preferenceKey)
    val errorResponse = response.errors[coord]
    val valueResponse = response.preferences[coord]
    when {
        errorResponse != null -> {
            val errorCode = when (errorResponse) {
                PreferenceGetterErrorCode.NOT_FOUND -> GetValueResult.RESULT_UNSUPPORTED
                PreferenceGetterErrorCode.DISALLOW -> GetValueResult.RESULT_DISALLOW
                else -> GetValueResult.RESULT_INTERNAL_ERROR
            }
            return GetValueResult.Builder(errorCode).build()
        }
        valueResponse != null -> {
            val resultBuilder = GetValueResult.Builder(GetValueResult.RESULT_OK)
            resultBuilder.setMetadata(valueResponse.toMetadata(context, coord.screenKey))
            val prefValue = valueResponse.value
            when (prefValue.valueCase.number) {
                PreferenceValueProto.BOOLEAN_VALUE_FIELD_NUMBER -> {
                    resultBuilder.setValue(
                        SettingsPreferenceValue.Builder(
                            SettingsPreferenceValue.TYPE_BOOLEAN
                        ).setBooleanValue(prefValue.booleanValue)
                            .build()
                    )
                    return resultBuilder.build()
                }
                PreferenceValueProto.INT_VALUE_FIELD_NUMBER -> {
                    resultBuilder.setValue(
                        SettingsPreferenceValue.Builder(
                            SettingsPreferenceValue.TYPE_INT
                        ).setIntValue(prefValue.intValue)
                            .build()
                    )
                    return resultBuilder.build()
                }
            }
            return GetValueResult.Builder(
                GetValueResult.RESULT_UNSUPPORTED
            ).build()
        }
        else -> return null
    }
}

/** Translate Framework SET VALUE request to Catalyst SET VALUE request */
fun transformFrameworkSetValueRequest(request: SetValueRequest): PreferenceSetterRequest? {
    val valueProto = when (request.preferenceValue.type) {
        SettingsPreferenceValue.TYPE_BOOLEAN -> preferenceValueProto {
            booleanValue = request.preferenceValue.booleanValue
        }
        SettingsPreferenceValue.TYPE_INT -> preferenceValueProto {
            intValue = request.preferenceValue.intValue
        }
        else -> null
    }
    return valueProto?.let {
        PreferenceSetterRequest(request.screenKey, request.preferenceKey, it)
    }
}

/** Translate Catalyst SET VALUE result to Framework SET VALUE result */
fun transformCatalystSetValueResponse(@PreferenceSetterResult response: Int): SetValueResult {
   val resultCode = when (response) {
        PreferenceSetterResult.OK -> SetValueResult.RESULT_OK
        PreferenceSetterResult.UNAVAILABLE -> SetValueResult.RESULT_UNAVAILABLE
        PreferenceSetterResult.DISABLED -> SetValueResult.RESULT_DISABLED
        PreferenceSetterResult.UNSUPPORTED -> SetValueResult.RESULT_UNSUPPORTED
        PreferenceSetterResult.DISALLOW -> SetValueResult.RESULT_DISALLOW
        PreferenceSetterResult.REQUIRE_APP_PERMISSION ->
            SetValueResult.RESULT_REQUIRE_APP_PERMISSION
        PreferenceSetterResult.REQUIRE_USER_AGREEMENT -> SetValueResult.RESULT_REQUIRE_USER_CONSENT
        PreferenceSetterResult.RESTRICTED -> SetValueResult.RESULT_RESTRICTED
       PreferenceSetterResult.INVALID_REQUEST -> SetValueResult.RESULT_INVALID_REQUEST
        else -> SetValueResult.RESULT_INTERNAL_ERROR
    }
    return SetValueResult.Builder(resultCode).build()
}

private data class PreferenceWithScreen(
    val screenKey: String,
    val preference: PreferenceProto,
)

private fun PreferenceProto.toMetadata(
    context: Context,
    screenKey: String
): SettingsPreferenceMetadata {
    val sensitivity = when (sensitivityLevel) {
        SensitivityLevel.NO_SENSITIVITY -> SettingsPreferenceMetadata.NO_SENSITIVITY
        SensitivityLevel.LOW_SENSITIVITY -> SettingsPreferenceMetadata.EXPECT_POST_CONFIRMATION
        SensitivityLevel.MEDIUM_SENSITIVITY -> SettingsPreferenceMetadata.EXPECT_PRE_CONFIRMATION
        else -> SettingsPreferenceMetadata.NO_DIRECT_ACCESS
    }
    return SettingsPreferenceMetadata.Builder(screenKey, key)
        .setTitle(title.getText(context))
        .setSummary(summary.getText(context))
        .setEnabled(enabled)
        .setAvailable(available)
        .setRestricted(restricted)
        .setWritable(persistent)
        .setLaunchIntent(launchIntent.toIntent())
        .setWriteSensitivity(sensitivity)
        .build()
}
