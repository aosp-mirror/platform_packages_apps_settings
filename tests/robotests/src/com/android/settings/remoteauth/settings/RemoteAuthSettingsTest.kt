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

package com.android.settings.remoteauth.settings

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.android.settings.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteAuthSettingsTest {
    @Before
    fun setUp() {
        launchFragmentInContainer<RemoteAuthSettings>(Bundle(), R.style.SudThemeGlif)
    }

    @Test
    fun testRemoteAuthenticatorSettings_headerVisible() {
        onView(withText(R.string.security_settings_remoteauth_settings_title)).check(
            matches(
                withEffectiveVisibility(Visibility.VISIBLE)
            )
        )
    }

    @Test
    fun testRemoteAuthenticatorSettings_descriptionVisible() {
        onView(withText(R.string.security_settings_remoteauth_settings_description)).check(
            matches(
                withEffectiveVisibility(Visibility.VISIBLE)
            )
        )
    }
}