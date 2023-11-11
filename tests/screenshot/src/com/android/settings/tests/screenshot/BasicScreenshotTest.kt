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

package com.android.settings.tests.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.shared.model.Default
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollIntroV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintGatekeeperViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintScrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.GatekeeperInfo
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.NavState
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.Start
import com.android.settings.testutils2.FakeFingerprintManagerInteractor
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.screenshot.GoldenImagePathManager
import platform.test.screenshot.ScreenshotTestRule
import platform.test.screenshot.matchers.MSSIMMatcher

@RunWith(AndroidJUnit4::class)
class BasicScreenshotTest {
  @Rule
  @JvmField
  var rule: ScreenshotTestRule =
    ScreenshotTestRule(
      GoldenImagePathManager(
        InstrumentationRegistry.getInstrumentation().getContext(),
        InstrumentationRegistry.getInstrumentation()
          .getTargetContext()
          .getFilesDir()
          .getAbsolutePath() + "/settings_screenshots"
      )
    )

  private var context: Context = ApplicationProvider.getApplicationContext()
  private var interactor = FakeFingerprintManagerInteractor()

  private val gatekeeperViewModel =
    FingerprintGatekeeperViewModel(
      GatekeeperInfo.GatekeeperPasswordInfo(byteArrayOf(1, 2, 3), 100L),
      interactor
    )

  private val backgroundDispatcher = StandardTestDispatcher()
  private lateinit var fragmentScenario: FragmentScenario<FingerprintEnrollIntroV2Fragment>
  val navState = NavState(true)

  private val navigationViewModel = FingerprintEnrollNavigationViewModel(
      backgroundDispatcher,
      interactor,
      gatekeeperViewModel,
      Start.next(navState),
      navState,
      Default,
    )
  private var fingerprintViewModel = FingerprintEnrollViewModel(
      interactor, gatekeeperViewModel, navigationViewModel,
    )
  private var fingerprintScrollViewModel = FingerprintScrollViewModel()

  @Before
  fun setup() {
    val factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
          modelClass: Class<T>,
        ): T {
          return when (modelClass) {
            FingerprintEnrollViewModel::class.java -> fingerprintViewModel
            FingerprintScrollViewModel::class.java -> fingerprintScrollViewModel
            FingerprintEnrollNavigationViewModel::class.java -> navigationViewModel
            FingerprintGatekeeperViewModel::class.java -> gatekeeperViewModel
            else -> null
          }
            as T
        }
      }

    fragmentScenario =
      launchFragmentInContainer(Bundle(), R.style.SudThemeGlif) {
        FingerprintEnrollIntroV2Fragment(factory)
      }
  }

  /** Renders a [view] into a [Bitmap]. */
  private fun viewToBitmap(view: View): Bitmap {
    val bitmap =
      Bitmap.createBitmap(
        view.measuredWidth,
        view.measuredHeight,
        Bitmap.Config.ARGB_8888,
      )
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
  }

  @Test
  fun testEnrollIntro() {
    fragmentScenario.onFragment { fragment ->
      val view = fragment.requireView().findViewById<View>(R.id.enroll_intro_content_view)!!
      view.setBackgroundColor(Color.BLACK)
    }
    fragmentScenario.onFragment { fragment ->
      val view = fragment.requireView().findViewById<View>(R.id.enroll_intro_content_view)!!
      rule.assertBitmapAgainstGolden(
        viewToBitmap(view),
        "fp_enroll_intro",
        MSSIMMatcher()
      )
    }

  }
}
