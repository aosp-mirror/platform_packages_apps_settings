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

package com.android.settings

import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.Settings.FactoryResetActivity
import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test [MainClear]. */
@RunWith(AndroidJUnit4::class)
class MainClearTest {
    @get:Rule
    val mSetFlagsRule = SetFlagsRule()

    @Test
    fun factoryResetCancelButton_flagDisabled_noCancelButton() {
        mSetFlagsRule.disableFlags(Flags.FLAG_SHOW_FACTORY_RESET_CANCEL_BUTTON)
        ActivityScenario.launch(FactoryResetActivity::class.java).use {
            ensurePrimaryButton()
            onView(withText(android.R.string.cancel)).check(doesNotExist())
            it.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }
        }
    }

    @Test
    fun factoryResetCancelButton_flagEnabled_showCancelButton() {
        mSetFlagsRule.enableFlags(Flags.FLAG_SHOW_FACTORY_RESET_CANCEL_BUTTON)
        ActivityScenario.launch(FactoryResetActivity::class.java).use {
            ensurePrimaryButton()
            it.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }

            // Note: onView CANNOT be called within onActivity block, which runs in the main thread
            onView(withText(android.R.string.cancel)).check(matches(isDisplayed())).perform(click())

            it.onActivity { activity -> assertThat(activity.isFinishing).isTrue() }
        }
    }

    private fun ensurePrimaryButton() {
        onView(withText(R.string.main_clear_button_text)).check(matches(isDisplayed()))
    }
}