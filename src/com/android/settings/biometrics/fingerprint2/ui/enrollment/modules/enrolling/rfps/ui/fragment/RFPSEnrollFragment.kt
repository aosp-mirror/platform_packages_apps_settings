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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.shared.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel.RFPSIconTouchViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel.RFPSViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.widget.FingerprintErrorDialog
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.widget.IconTouchDialog
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.widget.RFPSProgressBar
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.BackgroundViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.OrientationStateViewModel
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val TAG = "RFPSEnrollFragment"

/** This fragment is responsible for taking care of rear fingerprint enrollment. */
class RFPSEnrollFragment : Fragment(R.layout.fingerprint_v2_rfps_enroll_enrolling) {

  private lateinit var linearOutSlowInInterpolator: Interpolator
  private lateinit var fastOutLinearInInterpolator: Interpolator
  private lateinit var textView: TextView
  private lateinit var progressBar: RFPSProgressBar

  private val iconTouchViewModel: RFPSIconTouchViewModel by lazy {
    ViewModelProvider(requireActivity())[RFPSIconTouchViewModel::class.java]
  }

  private val orientationViewModel: OrientationStateViewModel by lazy {
    ViewModelProvider(requireActivity())[OrientationStateViewModel::class.java]
  }

  private val rfpsViewModel: RFPSViewModel by lazy {
    ViewModelProvider(requireActivity())[RFPSViewModel::class.java]
  }

  private val backgroundViewModel: BackgroundViewModel by lazy {
    ViewModelProvider(requireActivity())[BackgroundViewModel::class.java]
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = super.onCreateView(inflater, container, savedInstanceState)!!
    val fragment = this
    val context = requireContext()
    val glifLayout = view.requireViewById(R.id.setup_wizard_layout) as GlifLayout
    glifLayout.setDescriptionText(R.string.security_settings_fingerprint_enroll_start_message)
    glifLayout.setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title)

    fastOutLinearInInterpolator =
      AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in)
    linearOutSlowInInterpolator =
      AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in)

    textView = view.requireViewById(R.id.text) as TextView
    progressBar = view.requireViewById(R.id.fingerprint_progress_bar) as RFPSProgressBar

    val footerBarMixin = glifLayout.getMixin(FooterBarMixin::class.java)
    footerBarMixin.secondaryButton =
      FooterButton.Builder(context)
        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
        .setListener { Log.e(TAG, "skip enrollment!") }
        .setButtonType(FooterButton.ButtonType.SKIP)
        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
        .build()
    footerBarMixin.buttonContainer.setBackgroundColor(Color.TRANSPARENT)

    progressBar.setOnTouchListener { _, motionEvent ->
      if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
        iconTouchViewModel.userTouchedFingerprintIcon()
      }
      true
    }

    // On any orientation event, dismiss dialogs.
    viewLifecycleOwner.lifecycleScope.launch {
      orientationViewModel.orientation.collect { dismissDialogs() }
    }

    // Signal we are ready for enrollment.
    rfpsViewModel.readyForEnrollment()

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        // Icon animation  update
        viewLifecycleOwner.lifecycleScope.launch {
          rfpsViewModel.shouldAnimateIcon.collect { animate ->
            progressBar.updateIconAnimation(animate)
          }
        }

        // Flow to show a dialog.
        viewLifecycleOwner.lifecycleScope.launch {
          iconTouchViewModel.shouldShowDialog.collectLatest { showDialog ->
            if (showDialog) {
              try {
                IconTouchDialog.showInstance(fragment)
              } catch (exception: Exception) {
                Log.d(TAG, "Dialog dismissed due to $exception")
              }
            }
          }
        }
      }
    }

    // If we go to the background, then finish enrollment. This should be permanent finish,
    // and shouldn't be reset until we explicitly tell the view model we want to retry
    // enrollment.
    viewLifecycleOwner.lifecycleScope.launch {
      backgroundViewModel.background
        .filter { inBackground -> inBackground }
        .collect { rfpsViewModel.stopEnrollment() }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      rfpsViewModel.progress.filterNotNull().collect { progress -> handleEnrollProgress(progress) }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      rfpsViewModel.helpMessage.filterNotNull().collect { help ->
        textView.text = help.helpString
        textView.visibility = View.VISIBLE
        textView.translationY =
          resources.getDimensionPixelSize(R.dimen.fingerprint_error_text_appear_distance).toFloat()
        textView.alpha = 0f
        textView
          .animate()
          .alpha(1f)
          .translationY(0f)
          .setDuration(200)
          .setInterpolator(linearOutSlowInInterpolator)
          .start()

      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      rfpsViewModel.errorMessage.filterNotNull().collect { error -> handleEnrollError(error) }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      rfpsViewModel.textViewIsVisible.collect {
        textView.visibility = if (it) View.VISIBLE else View.INVISIBLE
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      rfpsViewModel.clearHelpMessage.collect {
        textView
          .animate()
          .alpha(0f)
          .translationY(
            resources
              .getDimensionPixelSize(R.dimen.fingerprint_error_text_disappear_distance)
              .toFloat()
          )
          .setDuration(100)
          .setInterpolator(fastOutLinearInInterpolator)
          .withEndAction { rfpsViewModel.setVisibility(false) }
          .start()
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.DESTROYED) {
        rfpsViewModel.stopEnrollment()
        dismissDialogs()
      }
    }
    return view
  }

  private fun handleEnrollError(error: FingerEnrollState.EnrollError) {
    val fragment = this
    viewLifecycleOwner.lifecycleScope.launch {
      try {
        val shouldRestartEnrollment = FingerprintErrorDialog.showInstance(error, fragment)
      } catch (exception: Exception) {
        Log.e(TAG, "Exception occurred $exception")
      }
      onEnrollmentFailed()
    }
  }

  private fun onEnrollmentFailed() {
    rfpsViewModel.stopEnrollment()
  }

  private fun handleEnrollProgress(progress: FingerEnrollState.EnrollProgress) {
    progressBar.updateProgress(
      progress.remainingSteps.toFloat() / progress.totalStepsRequired.toFloat()
    )

    if (progress.remainingSteps == 0) {
      performNextStepSuccess()
    }
  }

  private fun performNextStepSuccess() {}

  private fun dismissDialogs() {
    val transaction = parentFragmentManager.beginTransaction()
    for (frag in parentFragmentManager.fragments) {
      if (frag is InstrumentedDialogFragment) {
        Log.d(TAG, "removing dialog settings fragment $frag")
        frag.dismiss()
        transaction.remove(frag)
      }
    }
    transaction.commitAllowingStateLoss()
  }
}
