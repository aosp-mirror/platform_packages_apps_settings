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

package com.android.settings.tests.screenshot.biometrics.fingerprint.fragment

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollConfirmationV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import com.android.settings.tests.screenshot.biometrics.fingerprint.Injector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.screenshot.FragmentScreenshotTestRule
import platform.test.screenshot.ViewScreenshotTestRule.Mode

@RunWith(AndroidJUnit4::class)
class RfpsEnrollConfirmationScreenshotTest {
  private val injector: Injector = Injector(FingerprintNavigationStep.Confirmation)

  @Rule @JvmField var rule: FragmentScreenshotTestRule = Injector.BiometricFragmentScreenShotRule()

  @Test
  fun testConfirmation() {
    rule.screenshotTest(
      "rfps_enroll_confirmation",
      Mode.MatchSize,
      FingerprintEnrollConfirmationV2Fragment(injector.factory),
    )
  }
}
