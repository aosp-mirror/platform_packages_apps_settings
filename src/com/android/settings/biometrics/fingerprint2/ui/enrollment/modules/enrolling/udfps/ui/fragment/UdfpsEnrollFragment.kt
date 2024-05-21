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
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_HOVER_MOVE
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
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.StageViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.common.widget.FingerprintErrorDialog
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.DescriptionText
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.EducationAnimationModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.HeaderText
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
  private lateinit var lottie: LottieAnimationView

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
    val fragment = this
    lottie = view.findViewById(R.id.illustration_lottie)!!
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
        launch {
          viewModel.sensorLocation.collect { sensor ->
            udfpsEnrollView.setSensorRect(sensor.sensorBounds, sensor.sensorType)
          }
        }
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
          viewModel.shouldShowLottie.collect {
            lottie.visibility = if (it) View.VISIBLE else View.GONE
          }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.lottie.collect { lottieModel ->
            if (lottie.visibility == View.GONE) {
              return@collect
            }
            val resource = lottieModel.toResource()
            if (resource != null) {
              LottieCompositionFactory.fromRawRes(requireContext(), resource).addListener { comp ->
                comp?.let { composition ->
                  lottie.setComposition(composition)
                  lottie.visibility = View.VISIBLE
                  lottie.playAnimation()
                }
              }
            } else {
              lottie.visibility = View.INVISIBLE
            }
          }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          repeatOnLifecycle(Lifecycle.State.DESTROYED) { viewModel.stopEnrollment() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.accessibilityEnabled.collect { enabled ->
            udfpsEnrollView.setAccessibilityEnabled(enabled)
          }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.enrollState.collect {
            Log.d(TAG, "EnrollEvent $it")
            if (it is FingerEnrollState.EnrollError) {
              try {
                FingerprintErrorDialog.showInstance(it, fragment)
              } catch (exception: Exception) {
                Log.e(TAG, "Exception occurred $exception")
              }
            } else {
              udfpsEnrollView.onUdfpsEvent(it)
            }
          }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.progressSaved.collect { udfpsEnrollView.onEnrollProgressSaved(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.enrollStage.collect { udfpsEnrollView.updateStage(it) }
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.touchExplorationDebug.collect {
        udfpsEnrollView.sendDebugTouchExplorationEvent(
          MotionEvent.obtain(100, 100, ACTION_HOVER_MOVE, it.x.toFloat(), it.y.toFloat(), 0)
        )
      }
    }
    viewModel.readyForEnrollment()
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
