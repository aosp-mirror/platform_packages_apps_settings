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

package com.android.settings.deviceinfo.firmwareversion

import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreference.Companion.MODULE_UPDATE_ACTION
import com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreference.Companion.MODULE_UPDATE_ACTION_V2
import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

// LINT.IfChange
@RunWith(RobolectricTestRunner::class)
class MainlineModuleVersionPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockResources: Resources

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getPackageManager(): PackageManager = mockPackageManager

            override fun getResources(): Resources = mockResources
        }

    private val mainlineModuleVersionPreference = MainlineModuleVersionPreference()

    @Test
    fun isAvailable_noMainlineModuleProvider_unavailable() {
        createMocks("", null)
        assertThat(mainlineModuleVersionPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noMainlineModulePackageInfo_unavailable() {
        createMocks("test.provider", PackageManager.NameNotFoundException())
        assertThat(mainlineModuleVersionPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasMainlineModulePackageInfo_available() {
        createMocks("test.provider", "test version 123")
        assertThat(mainlineModuleVersionPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun getSummary_versionIsNull() {
        createMocks("test.provider", PackageInfo())
        assertThat(mainlineModuleVersionPreference.getSummary(context)).isNull()
    }

    @Test
    fun getSummary_versionIsEmpty() {
        createMocks("test.provider", "")
        assertThat(mainlineModuleVersionPreference.getSummary(context)).isNull()
    }

    @Test
    fun getSummary_versionIsNotDate() {
        createMocks("test.provider", "a")
        assertThat(mainlineModuleVersionPreference.getSummary(context)).isEqualTo("a")
    }

    @Test
    fun getSummary_versionIsMonth() {
        createMocks("test.provider", "2019-05")
        assertThat(mainlineModuleVersionPreference.getSummary(context)).isEqualTo("May 1, 2019")
    }

    @Test
    fun getSummary_versionIsDate() {
        createMocks("test.provider", "2019-05-13")
        assertThat(mainlineModuleVersionPreference.getSummary(context)).isEqualTo("May 13, 2019")
    }

    @Test
    @EnableFlags(Flags.FLAG_MAINLINE_MODULE_EXPLICIT_INTENT)
    fun intentV2_preferenceShouldBeSelectable() {
        intent_preferenceShouldBeSelectable(MODULE_UPDATE_ACTION_V2, MODULE_PACKAGE)
    }

    @Test
    @DisableFlags(Flags.FLAG_MAINLINE_MODULE_EXPLICIT_INTENT)
    fun intent_preferenceShouldBeSelectable() {
        intent_preferenceShouldBeSelectable(MODULE_UPDATE_ACTION, null)
    }

    private fun intent_preferenceShouldBeSelectable(action: String, intentPackage: String?) {
        createMocks("test.provider", "test version 123") {
            on { resolveActivity(any(), anyInt()) } doAnswer
                {
                    when {
                        (it.arguments[0] as Intent).action == action -> ResolveInfo()
                        else -> null
                    }
                }
        }

        val preference = Preference(context)
        mainlineModuleVersionPreference.bind(preference, mainlineModuleVersionPreference)

        val intent = preference.intent!!
        assertThat(intent.action).isEqualTo(action)
        assertThat(preference.isSelectable).isTrue()
        assertThat(intent.`package`).isEqualTo(intentPackage)
    }

    @Test
    fun intent_null() {
        createMocks("test.provider", "test version 123")

        val preference = Preference(context)
        mainlineModuleVersionPreference.bind(preference, mainlineModuleVersionPreference)

        assertThat(preference.intent).isNull()
        assertThat(preference.isSelectable).isTrue()
    }

    private fun createMocks(
        pkg: String,
        pkgInfo: Any?,
        stubbing: KStubbing<PackageManager>.() -> Unit = {},
    ) {
        mockResources = mock {
            on { getString(R.string.config_mainline_module_update_package) } doReturn MODULE_PACKAGE
            on {
                getString(com.android.internal.R.string.config_defaultModuleMetadataProvider)
            } doReturn pkg
        }

        mockPackageManager = mock {
            when (pkgInfo) {
                is PackageInfo -> on { getPackageInfo(eq(pkg), anyInt()) } doReturn pkgInfo
                is String ->
                    on { getPackageInfo(eq(pkg), anyInt()) } doReturn
                        PackageInfo().apply { versionName = pkgInfo }
                is Exception -> on { getPackageInfo(eq(pkg), anyInt()) } doThrow pkgInfo
                else -> {}
            }
            stubbing.invoke(this)
        }
    }

    companion object {
        const val MODULE_PACKAGE = "com.android.vending"
    }
}
// LINT.ThenChange(MainlineModuleVersionPreferenceControllerTest.java)
