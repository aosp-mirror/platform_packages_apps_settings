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

package com.android.settings.remoteauth.enrolling

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
class RemoteAuthEnrollEnrollingTest {
    @Before
    fun setup() {
        launchFragmentInContainer<RemoteAuthEnrollEnrolling>(Bundle(), R.style.SudThemeGlif)
    }

    @Test
    fun testRemoteAuthenticatorEnrollEnrolling_headerVisible() {
        onView(withText(R.string.security_settings_remoteauth_enroll_enrolling_title)).check(
            matches(
                withEffectiveVisibility(Visibility.VISIBLE)
            )
        )
    }

    @Test
    fun testRemoteAuthenticatorEnrollEnrolling_primaryButtonDisabled() {
        onView(withText(R.string.security_settings_remoteauth_enroll_enrolling_agree)).check(
            matches(
                isNotEnabled()
            )
        )
    }

    @Test
    fun testRemoteAuthenticatorEnrollEnrolling_progressBarNotVisible() {
        onView(withId(R.id.enrolling_list_progress_bar)).check(
            matches(
                withEffectiveVisibility(
                    Visibility.INVISIBLE
                )
            )
        )
    }

    @Test
    fun testRemoteAuthenticatorEnrollEnrolling_errorTextNotVisible() {
        onView(withId(R.id.error_text)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
    }
}