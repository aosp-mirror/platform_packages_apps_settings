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

package com.android.settings.spa.search

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.SettingsActivity
import com.android.settings.spa.SpaSearchLanding.BundleValue
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingFragment
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingKey
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingSpaPage
import com.android.settingslib.spa.framework.util.KEY_DESTINATION
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SpaSearchLandingActivityTest {

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            doNothing().whenever(mock).startActivity(any())
        }

    @Test
    fun tryLaunch_spaPage() {
        val key =
            SpaSearchLandingKey.newBuilder()
                .setSpaPage(SpaSearchLandingSpaPage.newBuilder().setDestination(DESTINATION))
                .build()

        SpaSearchLandingActivity.tryLaunch(context, key.encodeToString())

        verify(context).startActivity(argThat { getStringExtra(KEY_DESTINATION) == DESTINATION })
    }

    @Test
    fun tryLaunch_fragment() {
        val key =
            SpaSearchLandingKey.newBuilder()
                .setFragment(
                    SpaSearchLandingFragment.newBuilder()
                        .setFragmentName(DESTINATION)
                        .setPreferenceKey(PREFERENCE_KEY)
                        .putArguments(
                            ARGUMENT_KEY,
                            BundleValue.newBuilder().setIntValue(ARGUMENT_VALUE).build()))
                .build()

        SpaSearchLandingActivity.tryLaunch(context, key.encodeToString())

        val intent = argumentCaptor<Intent> { verify(context).startActivity(capture()) }.firstValue
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(DESTINATION)
        val fragmentArguments =
            intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)!!
        assertThat(fragmentArguments.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY))
            .isEqualTo(PREFERENCE_KEY)
        assertThat(fragmentArguments.getInt(ARGUMENT_KEY)).isEqualTo(ARGUMENT_VALUE)
    }

    private companion object {
        const val DESTINATION = "Destination"
        const val PREFERENCE_KEY = "preference_key"
        const val ARGUMENT_KEY = "argument_key"
        const val ARGUMENT_VALUE = 123
    }
}
