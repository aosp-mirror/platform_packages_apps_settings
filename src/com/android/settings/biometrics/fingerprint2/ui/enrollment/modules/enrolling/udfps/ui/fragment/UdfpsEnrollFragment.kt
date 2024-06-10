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
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.data.model.EnrollStageModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.common.util.toFingerprintEnrollOptions
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.common.widget.FingerprintErrorDialog
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.model.DescriptionText
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.model.HeaderText
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.EducationAnimationModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.widget.UdfpsEnrollViewV2
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.launch

/** This fragment is responsible for showing the udfps Enrollment UI. */
class UdfpsEnrollFragment() : Fragment(R.layout.fingerprint_v2_udfps_enroll_enrolling) {

  /** Used for testing purposes */
  private var factory: ViewModelProvider.Factory? = null
  private lateinit var udfpsEnrollView: UdfpsEnrollViewV2
  private lateinit var lottie: LottieAnimationView

  private val viewModel: UdfpsViewModel by activityViewModels { factory ?: UdfpsViewModel.Factory }

  @VisibleForTesting
  constructor(theFactory: ViewModelProvider.Factory) : this() {
    factory = theFactory
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val fragment = this
    lottie = view.findViewById(R.id.illustration_lottie)!!
    udfpsEnrollView = view.findViewById(R.id.udfps_animation_view)!!
    val glifLayout: GlifLayout = view.findViewById(R.id.glif_layout)!!

    val backgroundColor = glifLayout.backgroundBaseColor
    val window = requireActivity().window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    val color = backgroundColor?.defaultColor ?: glifLayout.primaryColor.defaultColor
    window.statusBarColor = color
    view.setBackgroundColor(color)

    view.findViewById<Button>(R.id.skip)?.apply {
      setOnClickListener { viewModel.negativeButtonClicked() }
    }

    udfpsEnrollView.setFinishAnimationCompleted { viewModel.finishedSuccessfully() }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.enroll(requireActivity().intent.toFingerprintEnrollOptions())
        launch {
          viewModel.sensorLocation.collect { sensor ->
            udfpsEnrollView.setSensorRect(sensor.sensorBounds, sensor.sensorType)
          }
        }

        launch { viewModel.overlayShown.collect { udfpsEnrollView.overlayShown() } }
        launch { viewModel.headerText.collect { glifLayout.setHeaderText(it.toResource()) } }
        launch {
          viewModel.userInteractedWithSensor.collect {
            if (!it) {
              glifLayout.setHeaderText(R.string.security_settings_fingerprint_enroll_udfps_title)
              glifLayout.setDescriptionText(R.string.security_settings_udfps_enroll_start_message)
            }
          }
        }

        launch {
          viewModel.descriptionText.collect {
            if (it != null) {
              it.toResource()?.let { text -> glifLayout.setDescriptionText(text) }
            } else {
              glifLayout.descriptionText = ""
            }
          }
        }
        launch {
          viewModel.shouldShowLottie.collect {
            lottie.visibility = if (it) View.VISIBLE else View.GONE
          }
        }

        launch {
          viewModel.lottie.collect { lottieModel ->
            if (lottie.visibility == View.GONE) {
              return@collect
            }
            val resource = lottieModel.toResource()
            if (resource != null) {
              glifLayout.descriptionTextView.visibility = View.GONE
              LottieCompositionFactory.fromRawRes(requireContext(), resource).addListener { comp ->
                comp?.let { composition ->
                  lottie.setComposition(composition)
                  lottie.visibility = View.VISIBLE
                  lottie.playAnimation()
                }
              }
            } else {
              glifLayout.descriptionTextView.visibility = View.VISIBLE
              lottie.visibility = View.INVISIBLE
            }
          }
        }
        launch {
          viewModel.accessibilityEnabled.collect { enabled ->
            udfpsEnrollView.setAccessibilityEnabled(enabled)
          }
        }

        launch {
          viewModel.enrollState.collect {
            Log.d(TAG, "EnrollEvent $it")
            if (it is FingerEnrollState.EnrollError) {
              try {
                FingerprintErrorDialog.showInstance(it, fragment)
                viewModel.errorDialogShown(it)
              } catch (exception: Exception) {
                Log.e(TAG, "Exception occurred $exception")
              }
            } else {
              udfpsEnrollView.onUdfpsEvent(it)
            }
          }
        }

        launch { viewModel.progressSaved.collect { udfpsEnrollView.onEnrollProgressSaved(it) } }

        launch { viewModel.guidedEnrollment.collect { udfpsEnrollView.updateGuidedEnrollment(it) } }
        launch {
          viewModel.guidedEnrollmentSaved.collect { udfpsEnrollView.onGuidedPointSaved(it) }
        }

        launch { viewModel.shouldDrawIcon.collect { udfpsEnrollView.shouldDrawIcon(it) } }

        // Hack to get the final step of enroll progress to animate.
        launch {
          viewModel.udfpsLastStepViewModel.shouldAnimateCompletion.collect {
            Log.d(TAG, "Sending fake last enroll event $it")
            udfpsEnrollView.onUdfpsEvent(it)
          }
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.DESTROYED) { viewModel.stopEnrollment() }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.isLandscape.collect {
        if (it) {
          changeViewToLandscape()
        }
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.isReverseLandscape.collect {
        if (it) {
          changeViewToReverseLandscape()
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      view.setOnTouchListener { _, motionEvent ->
        viewModel.onTouchEvent(motionEvent)
        false
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.touchEvent.collect { udfpsEnrollView.onTouchEvent(it) }
    }

    viewModel.readyForEnrollment()
  }

  private fun changeViewToReverseLandscape() {
    Log.d(TAG, "changeViewToReverseLandscape")
    val glifContainer = requireView().findViewById<GlifLayout>(R.id.glif_layout)!!
    val headerView =
      glifContainer.findViewById<View>(
        com.google.android.setupdesign.R.id.sud_landscape_header_area
      )
    // The landscape_header_area nad landscape_content_area should have the same parent
    val parent = headerView!!.parent as ViewGroup
    val sudContentFrame =
      glifContainer.findViewById<View>(
        com.google.android.setupdesign.R.id.sud_landscape_content_area
      )!!
    val udfpsContainer = requireView().findViewById<FrameLayout>(R.id.layout_container)!!

    parent.removeView(headerView)
    parent.removeView(sudContentFrame)
    parent.addView(sudContentFrame)
    parent.addView(headerView)

    unclipSubviewsFromParent(udfpsContainer)
    udfpsEnrollView.requestLayout()
  }

  private fun changeViewToLandscape() {
    Log.d(TAG, "changeViewToLandscape")

    val glifContainer = requireView().findViewById<GlifLayout>(R.id.glif_layout)!!
    val headerView =
      glifContainer.findViewById<View>(
        com.google.android.setupdesign.R.id.sud_landscape_header_area
      )
    // The landscape_header_area nad landscape_content_area should have the same parent
    val parent = headerView!!.parent as ViewGroup
    val sudContentFrame =
      glifContainer.findViewById<View>(
        com.google.android.setupdesign.R.id.sud_landscape_content_area
      )!!

    parent.removeView(headerView)
    parent.removeView(sudContentFrame)
    parent.addView(headerView)
    parent.addView(sudContentFrame)

    val udfpsContainer = requireView().findViewById<FrameLayout>(R.id.layout_container)!!
    unclipSubviewsFromParent(udfpsContainer)
    udfpsEnrollView.requestLayout()
  }

  private fun unclipSubviewsFromParent(view: View) {
    var currParent = view.parent
    while (currParent is ViewGroup) {
      currParent.clipChildren = false
      currParent.clipToPadding = false
      currParent = currParent.parent
    }
  }

  private fun HeaderText.toResource(): Int {
    return when (this.enrollStageModel) {
      EnrollStageModel.Center,
      EnrollStageModel.Guided -> R.string.security_settings_fingerprint_enroll_repeat_title
      EnrollStageModel.Fingertip -> R.string.security_settings_udfps_enroll_fingertip_title
      EnrollStageModel.Unknown -> R.string.security_settings_fingerprint_enroll_udfps_title
      EnrollStageModel.LeftEdge -> R.string.security_settings_udfps_enroll_left_edge_title
      EnrollStageModel.RightEdge -> R.string.security_settings_udfps_enroll_right_edge_title
    }
  }

  private fun DescriptionText.toResource(): Int? {
    return when (this.enrollStageModel) {
      EnrollStageModel.Center,
      EnrollStageModel.Guided,
      EnrollStageModel.Fingertip,
      EnrollStageModel.LeftEdge,
      EnrollStageModel.RightEdge -> null
      EnrollStageModel.Unknown -> R.string.security_settings_udfps_enroll_start_message
    }
  }

  private fun EducationAnimationModel.toResource(): Int? {
    return when (this.enrollStageModel) {
      EnrollStageModel.Center,
      EnrollStageModel.Guided -> R.raw.udfps_center_hint_lottie
      EnrollStageModel.Fingertip -> R.raw.udfps_tip_hint_lottie
      EnrollStageModel.LeftEdge -> R.raw.udfps_left_edge_hint_lottie
      EnrollStageModel.RightEdge -> R.raw.udfps_right_edge_hint_lottie
      EnrollStageModel.Unknown -> null
    }
  }

  companion object {
    private const val TAG = "UDFPSEnrollFragment"
  }
}
