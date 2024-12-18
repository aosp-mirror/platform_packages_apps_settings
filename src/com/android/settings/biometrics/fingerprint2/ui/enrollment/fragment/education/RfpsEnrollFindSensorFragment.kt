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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.education

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settings.R
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog
import com.android.settings.biometrics.fingerprint.FingerprintFindSensorAnimation
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.common.util.toFingerprintEnrollOptions
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollFindSensorViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.launch

/**
 * A fragment that is used to educate the user about the rear fingerprint sensor on this device.
 *
 * The main goals of this page are
 * 1. Inform the user where the fingerprint sensor is on their device
 * 2. Explain to the user how the enrollment process shown by [FingerprintEnrollEnrollingV2Fragment]
 *    will work.
 */
class RfpsEnrollFindSensorFragment() : Fragment() {
  /** Used for testing purposes */
  private var factory: ViewModelProvider.Factory? = null

  @VisibleForTesting
  constructor(theFactory: ViewModelProvider.Factory) : this() {
    factory = theFactory
  }

  private var animation: FingerprintFindSensorAnimation? = null

  private val viewModel: FingerprintEnrollFindSensorViewModel by activityViewModels {
    factory ?: FingerprintEnrollFindSensorViewModel.Factory
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view =
      inflater.inflate(R.layout.fingerprint_v2_enroll_find_sensor, container, false)!! as GlifLayout
    view.setHeaderText(R.string.security_settings_fingerprint_enroll_find_sensor_title)
    view.setDescriptionText(R.string.security_settings_fingerprint_enroll_find_sensor_message)

    // Set up footer bar
    val footerBarMixin = view.getMixin(FooterBarMixin::class.java)
    setupSecondaryButton(footerBarMixin)

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.enroll(requireActivity().intent.toFingerprintEnrollOptions())
      }
    }
    lifecycleScope.launch {
      viewModel.showPrimaryButton.collect { setupPrimaryButton(footerBarMixin) }
    }

    lifecycleScope.launch {
      viewModel.showRfpsAnimation.collect {
        animation = view.findViewById(R.id.fingerprint_sensor_location_animation)
        animation!!.startAnimation()
      }
    }

    lifecycleScope.launch {
      viewModel.showErrorDialog.collect { (errMsgId, isSetup) ->
        // TODO: Covert error dialog kotlin as well
        FingerprintErrorDialog.showErrorDialog(requireActivity(), errMsgId, isSetup)
      }
    }
    return view
  }

  override fun onDestroy() {
    animation?.stopAnimation()
    super.onDestroy()
  }

  private fun setupSecondaryButton(footerBarMixin: FooterBarMixin) {
    footerBarMixin.secondaryButton =
      FooterButton.Builder(requireActivity())
        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
        .setListener { viewModel.secondaryButtonClicked() }
        .setButtonType(FooterButton.ButtonType.SKIP)
        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
        .build()
  }

  private fun setupPrimaryButton(footerBarMixin: FooterBarMixin) {
    footerBarMixin.primaryButton =
      FooterButton.Builder(requireActivity())
        .setText(R.string.security_settings_udfps_enroll_find_sensor_start_button)
        .setListener {
          Log.d(TAG, "onStartButtonClick")
          viewModel.proceedToEnrolling()
        }
        .setButtonType(FooterButton.ButtonType.NEXT)
        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
        .build()
  }

  companion object {
    private const val TAG = "RfpsEnrollFindSensor"
  }
}
