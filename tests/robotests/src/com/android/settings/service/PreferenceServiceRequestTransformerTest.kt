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
import android.content.Intent
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.service.settings.preferences.GetValueRequest
import android.service.settings.preferences.GetValueResult
import android.service.settings.preferences.MetadataResult
import android.service.settings.preferences.SetValueRequest
import android.service.settings.preferences.SetValueResult
import android.service.settings.preferences.SettingsPreferenceMetadata
import android.service.settings.preferences.SettingsPreferenceValue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.homepage.SettingsHomepageActivity
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.flags.Flags.FLAG_SETTINGS_CATALYST
import com.android.settingslib.graph.PreferenceCoordinate
import com.android.settingslib.graph.PreferenceGetterErrorCode
import com.android.settingslib.graph.PreferenceGetterFlags
import com.android.settingslib.graph.PreferenceGetterResponse
import com.android.settingslib.graph.PreferenceSetterResult
import com.android.settingslib.graph.preferenceGroupProto
import com.android.settingslib.graph.preferenceOrGroupProto
import com.android.settingslib.graph.preferenceProto
import com.android.settingslib.graph.preferenceScreenProto
import com.android.settingslib.graph.preferenceValueDescriptorProto
import com.android.settingslib.graph.preferenceValueProto
import com.android.settingslib.graph.proto.PreferenceGraphProto
import com.android.settingslib.graph.rangeValueProto
import com.android.settingslib.graph.textProto
import com.android.settingslib.graph.toProto
import com.android.settingslib.metadata.SensitivityLevel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@RequiresFlagsEnabled(FLAG_SETTINGS_CATALYST)
class PreferenceServiceRequestTransformerTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun transformCatalystGetMetadataResponse_emptyGraph_returnsFrameworkResponseWithError() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val graphProto = PreferenceGraphProto.newBuilder().build()
        val fResult = transformCatalystGetMetadataResponse(context, graphProto)
        with(fResult) {
            assertThat(resultCode).isEqualTo(MetadataResult.RESULT_UNSUPPORTED)
            assertThat(metadataList).isEmpty()
        }
    }

    @Test
    fun transformCatalystGetMetadataResponse_populatedGraph_returnsFrameworkResponseWithSuccess() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val screen = preferenceScreenProto {
            root = preferenceGroupProto {
                addAllPreferences(
                    listOf(
                        preferenceOrGroupProto {
                            group = preferenceGroupProto {
                                addPreferences(
                                    preferenceOrGroupProto {
                                        preference = preferenceProto {
                                            key = "key1"
                                            title = textProto { string = "title1" }
                                            enabled = true
                                        }
                                    }
                                )
                            }
                        },
                        preferenceOrGroupProto {
                            preference = preferenceProto {
                                key = "key2"
                                title = textProto { string = "title2" }
                                enabled = false
                            }
                        },
                    )
                )
            }
        }
        val graphProto = PreferenceGraphProto.newBuilder().putScreens("screen", screen).build()

        val fResult = transformCatalystGetMetadataResponse(context, graphProto)
        with(fResult) {
            assertThat(resultCode).isEqualTo(MetadataResult.RESULT_OK)
            assertThat(metadataList.size).isEqualTo(2)
        }
        assertThat(
                fResult.metadataList.any {
                    it.key == "key1" &&
                        it.screenKey == "screen" &&
                        it.title == "title1" &&
                        it.isEnabled
                }
            )
            .isTrue()
        assertThat(
                fResult.metadataList.any {
                    it.key == "key2" &&
                        it.screenKey == "screen" &&
                        it.title == "title2" &&
                        !it.isEnabled
                }
            )
            .isTrue()
    }

    @Test
    fun transformFrameworkGetValueRequest_returnsValidCatalystRequest() {
        val fRequest = GetValueRequest.Builder("screen", "pref").build()
        val cRequest = transformFrameworkGetValueRequest(fRequest)
        with(cRequest) {
            assertThat(preferences).hasLength(1)
            assertThat(preferences.first().screenKey).isEqualTo(fRequest.screenKey)
            assertThat(preferences.first().key).isEqualTo(fRequest.preferenceKey)
            assertThat(flags).isEqualTo(PreferenceGetterFlags.ALL)
        }
    }

    @Test
    fun transformCatalystGetValueResponse_success_returnsValidFrameworkResponse() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val fRequest = GetValueRequest.Builder("screen", "key").build()
        val cResult =
            PreferenceGetterResponse(
                emptyMap(),
                mapOf(
                    PreferenceCoordinate(fRequest.screenKey, fRequest.preferenceKey) to
                        preferenceProto {
                            key = "key"
                            title = textProto { string = "title" }
                            summary = textProto { string = "summary" }
                            enabled = true
                            available = true
                            restricted = true
                            persistent = true
                            valueDescriptor = preferenceValueDescriptorProto {
                                rangeValue = rangeValueProto {
                                    min = 0
                                    max = 10
                                    step = 2
                                }
                            }
                            sensitivityLevel = SensitivityLevel.LOW_SENSITIVITY
                            readPermissions = Permissions.allOf("read_permission").toProto()
                            writePermissions = Permissions.anyOf("write_permission").toProto()
                            launchIntent =
                                Intent(context, SettingsHomepageActivity::class.java).toProto()
                            value = preferenceValueProto { booleanValue = true }
                        }
                ),
            )
        val fResult = transformCatalystGetValueResponse(context, fRequest, cResult)
        assertThat(fResult!!.resultCode).isEqualTo(GetValueResult.RESULT_OK)
        with(fResult.metadata!!) {
            assertThat(title).isEqualTo("title")
            assertThat(summary).isEqualTo("summary")
            assertThat(isEnabled).isTrue()
            assertThat(isAvailable).isTrue()
            assertThat(isRestricted).isTrue()
            assertThat(isWritable).isTrue()
            assertThat(writeSensitivity)
                .isEqualTo(SettingsPreferenceMetadata.EXPECT_POST_CONFIRMATION)
            assertThat(readPermissions).containsExactly("read_permission")
            assertThat(writePermissions).containsExactly("write_permission")
            assertThat(launchIntent).isNotNull()
            assertThat(launchIntent!!.component!!.className)
                .isEqualTo(SettingsHomepageActivity::class.java.name)
            val intRange = extras.getBundle("key_int_range")
            assertThat(intRange).isNotNull()
            assertThat(intRange!!.getInt("key_min", -1)).isEqualTo(0)
            assertThat(intRange.getInt("key_max", -1)).isEqualTo(10)
            assertThat(intRange.getInt("key_step", -1)).isEqualTo(2)
        }
        with(fResult.value!!) {
            assertThat(type).isEqualTo(SettingsPreferenceValue.TYPE_BOOLEAN)
            assertThat(booleanValue).isTrue()
        }
    }

    @Test
    fun transformCatalystGetValueResponse_failure_returnsValidFrameworkResponse() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val fRequest = GetValueRequest.Builder("screen", "key").build()
        val cResult =
            PreferenceGetterResponse(
                mapOf(
                    PreferenceCoordinate(fRequest.screenKey, fRequest.preferenceKey) to
                        PreferenceGetterErrorCode.NOT_FOUND
                ),
                emptyMap(),
            )
        val fResult = transformCatalystGetValueResponse(context, fRequest, cResult)
        with(fResult!!) {
            assertThat(resultCode).isEqualTo(GetValueResult.RESULT_UNSUPPORTED)
            assertThat(metadata).isNull()
            assertThat(value).isNull()
        }
    }

    @Test
    fun transformCatalystGetValueResponse_invalidResponse_returnsNull() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val fRequest = GetValueRequest.Builder("screen", "key").build()
        val cResult = PreferenceGetterResponse(emptyMap(), emptyMap())
        val fResult = transformCatalystGetValueResponse(context, fRequest, cResult)
        assertThat(fResult).isNull()
    }

    @Test
    fun transformFrameworkSetValueRequest_typeBoolean_returnsValidCatalystRequest() {
        val fRequest =
            SetValueRequest.Builder(
                    "screen",
                    "pref",
                    SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_BOOLEAN)
                        .setBooleanValue(true)
                        .build(),
                )
                .build()
        val cRequest = transformFrameworkSetValueRequest(fRequest)
        with(cRequest!!) {
            assertThat(screenKey).isEqualTo(fRequest.screenKey)
            assertThat(key).isEqualTo(fRequest.preferenceKey)
            assertThat(value.hasBooleanValue()).isTrue()
            assertThat(value.booleanValue).isTrue()
        }
    }

    @Test
    fun transformFrameworkSetValueRequest_typeInt_returnsValidCatalystRequest() {
        val fRequest =
            SetValueRequest.Builder(
                    "screen",
                    "pref",
                    SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_INT)
                        .setIntValue(5)
                        .build(),
                )
                .build()
        val cRequest = transformFrameworkSetValueRequest(fRequest)
        with(cRequest!!) {
            assertThat(screenKey).isEqualTo(fRequest.screenKey)
            assertThat(key).isEqualTo(fRequest.preferenceKey)
            assertThat(value.hasIntValue()).isTrue()
            assertThat(value.intValue).isEqualTo(5)
        }
    }

    @Test
    fun transformFrameworkSetValueRequest_typeString_returnsNull() {
        val fRequest =
            SetValueRequest.Builder(
                    "screen",
                    "pref",
                    SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_STRING)
                        .setStringValue("value")
                        .build(),
                )
                .build()
        val cRequest = transformFrameworkSetValueRequest(fRequest)
        assertThat(cRequest).isNull()
    }

    @Test
    fun transformCatalystSetValueResponse_returnsValidFrameworkResponse() {
        assertThat(transformCatalystSetValueResponse(PreferenceSetterResult.OK).resultCode)
            .isEqualTo(SetValueResult.RESULT_OK)

        assertThat(transformCatalystSetValueResponse(PreferenceSetterResult.UNAVAILABLE).resultCode)
            .isEqualTo(SetValueResult.RESULT_UNAVAILABLE)

        assertThat(transformCatalystSetValueResponse(PreferenceSetterResult.DISABLED).resultCode)
            .isEqualTo(SetValueResult.RESULT_DISABLED)

        assertThat(transformCatalystSetValueResponse(PreferenceSetterResult.UNSUPPORTED).resultCode)
            .isEqualTo(SetValueResult.RESULT_UNSUPPORTED)

        assertThat(transformCatalystSetValueResponse(PreferenceSetterResult.DISALLOW).resultCode)
            .isEqualTo(SetValueResult.RESULT_DISALLOW)

        assertThat(
                transformCatalystSetValueResponse(PreferenceSetterResult.REQUIRE_APP_PERMISSION)
                    .resultCode
            )
            .isEqualTo(SetValueResult.RESULT_REQUIRE_APP_PERMISSION)

        assertThat(
                transformCatalystSetValueResponse(PreferenceSetterResult.REQUIRE_USER_AGREEMENT)
                    .resultCode
            )
            .isEqualTo(SetValueResult.RESULT_REQUIRE_USER_CONSENT)

        assertThat(transformCatalystSetValueResponse(PreferenceSetterResult.RESTRICTED).resultCode)
            .isEqualTo(SetValueResult.RESULT_RESTRICTED)

        assertThat(
                transformCatalystSetValueResponse(PreferenceSetterResult.INVALID_REQUEST).resultCode
            )
            .isEqualTo(SetValueResult.RESULT_INVALID_REQUEST)

        assertThat(
                transformCatalystSetValueResponse(PreferenceSetterResult.INTERNAL_ERROR).resultCode
            )
            .isEqualTo(SetValueResult.RESULT_INTERNAL_ERROR)
    }
}
