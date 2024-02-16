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
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.StageViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UdfpsEnrollFragment() : Fragment(R.layout.fingerprint_v2_udfps_enroll_enrolling) {

  /** Used for testing purposes */
  private var factory: ViewModelProvider.Factory? = null
  private val viewModel: UdfpsViewModel by lazy { viewModelProvider[UdfpsViewModel::class.java] }

  @VisibleForTesting
  constructor(theFactory: ViewModelProvider.Factory) : this() {
    factory = theFactory
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val layout = view as GlifLayout
    val illustrationLottie: LottieAnimationView = layout.findViewById(R.id.illustration_lottie)!!

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.stageFlow.collect {
            layout.setHeaderText(getHeaderText(it))
            getDescriptionText(it)?.let { descriptionText ->
              layout.setDescriptionText(descriptionText)
            }
            getLottie(it)?.let { lottie ->
              layout.descriptionText = ""
              LottieCompositionFactory.fromRawRes(requireContext().applicationContext, lottie)
                .addListener { comp ->
                  comp?.let { composition ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        illustrationLottie.setComposition(composition)
                        illustrationLottie.visibility = View.VISIBLE
                        illustrationLottie.playAnimation()
                      }
                  }
                }
            }
          }
        }
      }
    }
  }

  private fun getHeaderText(stageViewModel: StageViewModel): Int {
    return when (stageViewModel) {
      StageViewModel.Center,
      StageViewModel.Guided,
      StageViewModel.Fingertip,
      StageViewModel.Unknown -> R.string.security_settings_udfps_enroll_fingertip_title
      StageViewModel.LeftEdge -> R.string.security_settings_udfps_enroll_left_edge_title
      StageViewModel.RightEdge -> R.string.security_settings_udfps_enroll_right_edge_title
    }
  }

  private fun getDescriptionText(stageViewModel: StageViewModel): Int? {
    return when (stageViewModel) {
      StageViewModel.Center,
      StageViewModel.Guided,
      StageViewModel.Fingertip,
      StageViewModel.LeftEdge,
      StageViewModel.RightEdge -> null
      StageViewModel.Unknown -> R.string.security_settings_udfps_enroll_start_message
    }
  }

  private fun getLottie(stageViewModel: StageViewModel): Int? {
    return when (stageViewModel) {
      StageViewModel.Center,
      StageViewModel.Guided -> R.raw.udfps_center_hint_lottie
      StageViewModel.Fingertip -> R.raw.udfps_tip_hint_lottie
      StageViewModel.LeftEdge -> R.raw.udfps_left_edge_hint_lottie
      StageViewModel.RightEdge -> R.raw.udfps_right_edge_hint_lottie
      StageViewModel.Unknown -> null
    }
  }

  private val viewModelProvider: ViewModelProvider by lazy {
    if (factory != null) {
      ViewModelProvider(requireActivity(), factory!!)
    } else {
      ViewModelProvider(requireActivity())
    }
  }

  companion object {
    private const val TAG = "UDFPSEnrollFragment"
    private val navStep = FingerprintNavigationStep.Enrollment::class
  }
}
