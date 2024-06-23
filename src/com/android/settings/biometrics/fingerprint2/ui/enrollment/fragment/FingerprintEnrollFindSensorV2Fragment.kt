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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog
import com.android.settings.biometrics.fingerprint.FingerprintFindSensorAnimation
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.launch

private const val TAG = "FingerprintEnrollFindSensorV2Fragment"

/**
 * A fragment that is used to educate the user about the fingerprint sensor on this device.
 *
 * If the sensor is not a udfps sensor, this fragment listens to fingerprint enrollment for
 * proceeding to the enroll enrolling.
 *
 * The main goals of this page are
 * 1. Inform the user where the fingerprint sensor is on their device
 * 2. Explain to the user how the enrollment process shown by [FingerprintEnrollEnrollingV2Fragment]
 *    will work.
 */
class FingerprintEnrollFindSensorV2Fragment(val sensorType: FingerprintSensorType) : Fragment() {
  /** Used for testing purposes */
  private var factory: ViewModelProvider.Factory? = null

  @VisibleForTesting
  constructor(
    sensorType: FingerprintSensorType,
    theFactory: ViewModelProvider.Factory,
  ) : this(sensorType) {
    factory = theFactory
  }

  private val viewModelProvider: ViewModelProvider by lazy {
    if (factory != null) {
      ViewModelProvider(requireActivity(), factory!!)
    } else {
      ViewModelProvider(requireActivity())
    }
  }

  // This is only for non-udfps or non-sfps sensor. For udfps and sfps, we show lottie.
  private var animation: FingerprintFindSensorAnimation? = null

  private var contentLayoutId: Int = -1
  private val viewModel: FingerprintEnrollFindSensorViewModel by lazy {
    viewModelProvider[FingerprintEnrollFindSensorViewModel::class.java]
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {

    contentLayoutId =
      when (sensorType) {
        FingerprintSensorType.UDFPS_OPTICAL,
        FingerprintSensorType.UDFPS_ULTRASONIC -> R.layout.udfps_enroll_find_sensor_layout
        FingerprintSensorType.POWER_BUTTON -> R.layout.sfps_enroll_find_sensor_layout
        else -> R.layout.fingerprint_v2_enroll_find_sensor
      }

    val view = inflater.inflate(contentLayoutId, container, false)!! as GlifLayout
    setTexts(sensorType, view)

    // Set up footer bar
    val footerBarMixin = view.getMixin(FooterBarMixin::class.java)
    setupSecondaryButton(footerBarMixin)
    lifecycleScope.launch {
      viewModel.showPrimaryButton.collect { setupPrimaryButton(footerBarMixin) }
    }

    // Set up lottie or animation
    lifecycleScope.launch {
      viewModel.sfpsLottieInfo.collect { (isFolded, rotation) ->
        setupLottie(view, getSfpsIllustrationLottieAnimation(isFolded, rotation))
      }
    }
    lifecycleScope.launch {
      viewModel.udfpsLottieInfo.collect { isAccessibilityEnabled ->
        val lottieAnimation =
          if (isAccessibilityEnabled) R.raw.udfps_edu_a11y_lottie else R.raw.udfps_edu_lottie
        setupLottie(view, lottieAnimation) { viewModel.proceedToEnrolling() }
      }
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

  private fun setupLottie(
    view: View,
    lottieAnimation: Int,
    lottieClickListener: View.OnClickListener? = null,
  ) {
    val illustrationLottie: LottieAnimationView? = view.findViewById(R.id.illustration_lottie)
    illustrationLottie?.setAnimation(lottieAnimation)
    illustrationLottie?.playAnimation()
    illustrationLottie?.setOnClickListener(lottieClickListener)
    illustrationLottie?.visibility = View.VISIBLE
  }

  private fun setTexts(sensorType: FingerprintSensorType?, view: GlifLayout) {
    when (sensorType) {
      FingerprintSensorType.UDFPS_OPTICAL,
      FingerprintSensorType.UDFPS_ULTRASONIC -> {
        view.setHeaderText(R.string.security_settings_udfps_enroll_find_sensor_title)
        view.setDescriptionText(R.string.security_settings_udfps_enroll_find_sensor_message)
      }
      FingerprintSensorType.POWER_BUTTON -> {
        view.setHeaderText(R.string.security_settings_sfps_enroll_find_sensor_title)
        view.setDescriptionText(R.string.security_settings_sfps_enroll_find_sensor_message)
      }
      else -> {
        view.setHeaderText(R.string.security_settings_fingerprint_enroll_find_sensor_title)
        view.setDescriptionText(R.string.security_settings_fingerprint_enroll_find_sensor_message)
      }
    }
  }

  private fun getSfpsIllustrationLottieAnimation(isFolded: Boolean, rotation: Int): Int {
    val animation: Int
    when (rotation) {
      Surface.ROTATION_90 ->
        animation =
          (if (isFolded) R.raw.fingerprint_edu_lottie_folded_top_left
          else R.raw.fingerprint_edu_lottie_portrait_top_left)
      Surface.ROTATION_180 ->
        animation =
          (if (isFolded) R.raw.fingerprint_edu_lottie_folded_bottom_left
          else R.raw.fingerprint_edu_lottie_landscape_bottom_left)
      Surface.ROTATION_270 ->
        animation =
          (if (isFolded) R.raw.fingerprint_edu_lottie_folded_bottom_right
          else R.raw.fingerprint_edu_lottie_portrait_bottom_right)
      else ->
        animation =
          (if (isFolded) R.raw.fingerprint_edu_lottie_folded_top_right
          else R.raw.fingerprint_edu_lottie_landscape_top_right)
    }
    return animation
  }
}
