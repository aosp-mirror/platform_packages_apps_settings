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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.fragment

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.DescriptionText
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.HeaderText
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.EducationAnimationModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.StageViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.widget.UdfpsEnrollViewV2
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.launch

/** This fragment is responsible for showing the udfps Enrollment UI. */
class UdfpsEnrollFragment() : Fragment(R.layout.fingerprint_v2_udfps_enroll_enrolling) {

  /** Used for testing purposes */
  private var factory: ViewModelProvider.Factory? = null
  private val viewModel: UdfpsViewModel by lazy { viewModelProvider[UdfpsViewModel::class.java] }
  private lateinit var udfpsEnrollView: UdfpsEnrollViewV2

  private val viewModelProvider: ViewModelProvider by lazy {
    if (factory != null) {
      ViewModelProvider(requireActivity(), factory!!)
    } else {
      ViewModelProvider(requireActivity())
    }
  }

  @VisibleForTesting
  constructor(theFactory: ViewModelProvider.Factory) : this() {
    factory = theFactory
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val illustrationLottie: LottieAnimationView = view.findViewById(R.id.illustration_lottie)!!
    udfpsEnrollView = view.findViewById(R.id.udfps_animation_view)!!
    val titleTextView = view.findViewById<TextView>(R.id.title)!!
    val descriptionTextView = view.findViewById<TextView>(R.id.description)!!

    val glifLayout = view.findViewById<GlifLayout>(R.id.dummy_glif_layout)!!
    val backgroundColor = glifLayout.backgroundBaseColor
    val window = requireActivity().window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    val color = backgroundColor?.defaultColor ?: glifLayout.primaryColor.defaultColor
    window.statusBarColor = color
    view.setBackgroundColor(color)

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.headerText.collect { titleTextView.setText(it.toResource()) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.descriptionText.collect {
            if (it != null) {
              it.toResource()?.let { text -> descriptionTextView.setText(text) }
            } else {
              descriptionTextView.text = ""
            }
          }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.sensorLocation.collect { rect -> udfpsEnrollView.setSensorRect(rect) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.accessibilityEnabled.collect { isEnabled -> udfpsEnrollView.setAccessibilityEnabled(isEnabled) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.lottie.collect { lottieModel ->
            val resource = lottieModel.toResource()
            if (resource != null) {
              LottieCompositionFactory.fromRawRes(requireContext(), resource).addListener { comp ->
                comp?.let { composition ->
                  illustrationLottie.setComposition(composition)
                  illustrationLottie.visibility = View.VISIBLE
                  illustrationLottie.playAnimation()
                }
              }
            } else {
              illustrationLottie.visibility = View.INVISIBLE
            }
          }
        }
        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.udfpsEvent.collect { udfpsEnrollView.onUdfpsEvent(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.enrollStage.collect { udfpsEnrollView.updateStage(it) }
        }
      }
    }
  }

  private fun HeaderText.toResource(): Int {
    return when (this.stageViewModel) {
      StageViewModel.Center,
      StageViewModel.Guided,
      StageViewModel.Fingertip,
      StageViewModel.Unknown -> R.string.security_settings_udfps_enroll_fingertip_title
      StageViewModel.LeftEdge -> R.string.security_settings_udfps_enroll_left_edge_title
      StageViewModel.RightEdge -> R.string.security_settings_udfps_enroll_right_edge_title
    }
  }

  private fun DescriptionText.toResource(): Int? {
    return when (this.stageViewModel) {
      StageViewModel.Center,
      StageViewModel.Guided,
      StageViewModel.Fingertip,
      StageViewModel.LeftEdge,
      StageViewModel.RightEdge -> null
      StageViewModel.Unknown -> R.string.security_settings_udfps_enroll_start_message
    }
  }

  private fun EducationAnimationModel.toResource(): Int? {
    return when (this.stageViewModel) {
      StageViewModel.Center,
      StageViewModel.Guided -> R.raw.udfps_center_hint_lottie
      StageViewModel.Fingertip -> R.raw.udfps_tip_hint_lottie
      StageViewModel.LeftEdge -> R.raw.udfps_left_edge_hint_lottie
      StageViewModel.RightEdge -> R.raw.udfps_right_edge_hint_lottie
      StageViewModel.Unknown -> null
    }
  }

  companion object {
    private const val TAG = "UDFPSEnrollFragment"
    private val navStep = FingerprintNavigationStep.Enrollment::class
  }
}
