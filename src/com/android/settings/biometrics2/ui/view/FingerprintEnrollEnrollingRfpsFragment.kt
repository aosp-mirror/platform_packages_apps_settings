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
package com.android.settings.biometrics2.ui.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_CANCELED
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils.loadInterpolator
import android.view.animation.Interpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.settings.R
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog
import com.android.settings.biometrics2.ui.model.EnrollmentProgress
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout

/**
 * Fragment is used to handle enrolling process for rfps
 */
class FingerprintEnrollEnrollingRfpsFragment : Fragment() {

    private var _enrollingViewModel: FingerprintEnrollEnrollingViewModel? = null
    private val enrollingViewModel: FingerprintEnrollEnrollingViewModel
        get() = _enrollingViewModel!!

    private var _progressViewModel: FingerprintEnrollProgressViewModel? = null
    private val progressViewModel: FingerprintEnrollProgressViewModel
        get() = _progressViewModel!!

    private val fastOutSlowInInterpolator: Interpolator
        get() = loadInterpolator(requireActivity(), android.R.interpolator.fast_out_slow_in)

    private val linearOutSlowInInterpolator: Interpolator
        get() = loadInterpolator(requireActivity(), android.R.interpolator.linear_out_slow_in)

    private val fastOutLinearInInterpolator: Interpolator
        get() = loadInterpolator(requireActivity(), android.R.interpolator.fast_out_linear_in)

    private var isAnimationCancelled = false

    private var enrollingRfpsView: GlifLayout? = null
    private val progressBar: ProgressBar
        get() = enrollingRfpsView!!.findViewById<ProgressBar>(R.id.fingerprint_progress_bar)!!

    private var progressAnim: ObjectAnimator? = null

    private val errorText: TextView
        get() = enrollingRfpsView!!.findViewById<TextView>(R.id.error_text)!!

    private val iconAnimationDrawable: AnimatedVectorDrawable?
        get() = (progressBar.background as LayerDrawable)
            .findDrawableByLayerId(R.id.fingerprint_animation) as AnimatedVectorDrawable?

    private val iconBackgroundBlinksDrawable: AnimatedVectorDrawable?
        get() = (progressBar.background as LayerDrawable)
            .findDrawableByLayerId(R.id.fingerprint_background) as AnimatedVectorDrawable?

    private var iconTouchCount = 0

    private val touchAgainRunnable =
        Runnable {
            showError(
                // Use enrollingRfpsView to getString to prevent activity is missing during rotation
                enrollingRfpsView!!.context.getString(
                    R.string.security_settings_fingerprint_enroll_lift_touch_again
                )
            )
        }

    private val onSkipClickListener = View.OnClickListener { _: View? ->
        enrollingViewModel.setOnSkipPressed()
        cancelEnrollment()
    }

    private val progressObserver: Observer<EnrollmentProgress> =
        Observer<EnrollmentProgress> { progress: EnrollmentProgress? ->
            if (DEBUG) {
                Log.d(TAG, "progressObserver($progress)")
            }
            if (progress != null && progress.steps >= 0) {
                onEnrollmentProgressChange(progress)
            }
        }

    private val helpMessageObserver: Observer<EnrollmentStatusMessage> =
        Observer<EnrollmentStatusMessage> { helpMessage: EnrollmentStatusMessage? ->
            if (DEBUG) {
                Log.d(TAG, "helpMessageObserver($helpMessage)")
            }
            helpMessage?.let { onEnrollmentHelp(it) }
        }

    private val errorMessageObserver: Observer<EnrollmentStatusMessage> =
        Observer<EnrollmentStatusMessage> { errorMessage: EnrollmentStatusMessage? ->
            if (DEBUG) {
                Log.d(TAG, "errorMessageObserver($errorMessage)")
            }
            errorMessage?.let { onEnrollmentError(it) }
       }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                enrollingViewModel.setOnBackPressed()
                cancelEnrollment()
            }
        }

    override fun onAttach(context: Context) {
        ViewModelProvider(requireActivity()).let { provider ->
            _enrollingViewModel = provider[FingerprintEnrollEnrollingViewModel::class.java]
            _progressViewModel = provider[FingerprintEnrollProgressViewModel::class.java]
        }
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    override fun onDetach() {
        onBackPressedCallback.isEnabled = false
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        enrollingRfpsView = inflater.inflate(
                R.layout.fingerprint_enroll_enrolling, container, false
        ) as GlifLayout
        return enrollingRfpsView!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iconAnimationDrawable!!.registerAnimationCallback(iconAnimationCallback)

        progressBar.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                iconTouchCount++
                if (iconTouchCount == ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN) {
                    showIconTouchDialog()
                } else {
                    progressBar.postDelayed(
                        showDialogRunnable,
                        ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN
                    )
                }
            } else if (event.actionMasked == MotionEvent.ACTION_CANCEL
                || event.actionMasked == MotionEvent.ACTION_UP
            ) {
                progressBar.removeCallbacks(showDialogRunnable)
            }
            true
        }

        requireActivity().bindFingerprintEnrollEnrollingRfpsView(
            view = enrollingRfpsView!!,
            onSkipClickListener = onSkipClickListener
        )
    }

    override fun onStart() {
        super.onStart()
        isAnimationCancelled = false
        startIconAnimation()
        startEnrollment()
        updateProgress(false /* animate */, progressViewModel.progressLiveData.value!!)
        updateTitleAndDescription()
    }

    private fun startIconAnimation() {
        iconAnimationDrawable?.start()
    }

    private fun stopIconAnimation() {
        isAnimationCancelled = true
        iconAnimationDrawable?.stop()
    }

    override fun onStop() {
        stopIconAnimation()
        removeEnrollmentObservers()
        if (!activity!!.isChangingConfigurations && progressViewModel.isEnrolling) {
            progressViewModel.cancelEnrollment()
        }
        super.onStop()
    }

    private fun removeEnrollmentObservers() {
        preRemoveEnrollmentObservers()
        progressViewModel.errorMessageLiveData.removeObserver(errorMessageObserver)
    }

    private fun preRemoveEnrollmentObservers() {
        progressViewModel.progressLiveData.removeObserver(progressObserver)
        progressViewModel.helpMessageLiveData.removeObserver(helpMessageObserver)
    }

    private fun cancelEnrollment() {
        preRemoveEnrollmentObservers()
        progressViewModel.cancelEnrollment()
    }

    private fun startEnrollment() {
        val startResult: Boolean =
            progressViewModel.startEnrollment(FingerprintManager.ENROLL_ENROLL)
        if (!startResult) {
            Log.e(TAG, "startEnrollment(), failed")
        }
        progressViewModel.progressLiveData.observe(this, progressObserver)
        progressViewModel.helpMessageLiveData.observe(this, helpMessageObserver)
        progressViewModel.errorMessageLiveData.observe(this, errorMessageObserver)
    }

    private fun onEnrollmentHelp(helpMessage: EnrollmentStatusMessage) {
        val helpStr: CharSequence = helpMessage.str
        if (!TextUtils.isEmpty(helpStr)) {
            errorText.removeCallbacks(touchAgainRunnable)
            showError(helpStr)
        }
    }

    private fun onEnrollmentError(errorMessage: EnrollmentStatusMessage) {
        stopIconAnimation()
        removeEnrollmentObservers()
        if (enrollingViewModel.onBackPressed
            && errorMessage.msgId == FINGERPRINT_ERROR_CANCELED
        ) {
            enrollingViewModel.onCancelledDueToOnBackPressed()
        } else if (enrollingViewModel.onSkipPressed
            && errorMessage.msgId == FINGERPRINT_ERROR_CANCELED
        ) {
            enrollingViewModel.onCancelledDueToOnSkipPressed()
        } else {
            val errMsgId: Int = errorMessage.msgId
            enrollingViewModel.showErrorDialog(
                FingerprintEnrollEnrollingViewModel.ErrorDialogData(
                    enrollingRfpsView!!.context.getString(
                        FingerprintErrorDialog.getErrorMessage(errMsgId)
                    ),
                    enrollingRfpsView!!.context.getString(
                        FingerprintErrorDialog.getErrorTitle(errMsgId)
                    ),
                    errMsgId
                )
            )
            progressViewModel.cancelEnrollment()
        }
    }

    private fun onEnrollmentProgressChange(progress: EnrollmentProgress) {
        updateProgress(true /* animate */, progress)
        updateTitleAndDescription()
        animateFlash()
        errorText.removeCallbacks(touchAgainRunnable)
        errorText.postDelayed(touchAgainRunnable, HINT_TIMEOUT_DURATION.toLong())
    }

    private fun updateProgress(animate: Boolean, enrollmentProgress: EnrollmentProgress) {
        if (!progressViewModel.isEnrolling) {
            Log.d(TAG, "Enrollment not started yet")
            return
        }
        val progress = getProgress(enrollmentProgress)
        // Only clear the error when progress has been made.
        // TODO (b/234772728) Add tests.
        if (progressBar.progress < progress) {
            clearError()
        }
        if (animate) {
            animateProgress(progress)
        } else {
            progressBar.progress = progress
            if (progress >= PROGRESS_BAR_MAX) {
                delayedFinishRunnable.run()
            }
        }
    }

    private fun getProgress(progress: EnrollmentProgress): Int {
        if (progress.steps == -1) {
            return 0
        }
        val displayProgress = 0.coerceAtLeast(progress.steps + 1 - progress.remaining)
        return PROGRESS_BAR_MAX * displayProgress / (progress.steps + 1)
    }

    private fun showError(error: CharSequence) {
        errorText.text = error
        if (errorText.visibility == View.INVISIBLE) {
            errorText.visibility = View.VISIBLE
            errorText.translationY = enrollingRfpsView!!.context.resources.getDimensionPixelSize(
                R.dimen.fingerprint_error_text_appear_distance
            ).toFloat()
            errorText.alpha = 0f
            errorText.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(linearOutSlowInInterpolator)
                .start()
        } else {
            errorText.animate().cancel()
            errorText.alpha = 1f
            errorText.translationY = 0f
        }
        if (isResumed && enrollingViewModel.isAccessibilityEnabled) {
            enrollingViewModel.vibrateError(javaClass.simpleName + "::showError")
        }
    }

    private fun clearError() {
        if (errorText.visibility == View.VISIBLE) {
            errorText.animate()
                .alpha(0f)
                .translationY(
                    resources.getDimensionPixelSize(
                        R.dimen.fingerprint_error_text_disappear_distance
                    ).toFloat()
                )
                .setDuration(100)
                .setInterpolator(fastOutLinearInInterpolator)
                .withEndAction { errorText!!.visibility = View.INVISIBLE }
                .start()
        }
    }

    private fun animateProgress(progress: Int) {
        progressAnim?.cancel()
        val anim = ObjectAnimator.ofInt(
            progressBar /* target */,
            "progress" /* propertyName */,
            progressBar.progress /* values[0] */,
            progress /* values[1] */
        )
        anim.addListener(progressAnimationListener)
        anim.interpolator = fastOutSlowInInterpolator
        anim.setDuration(ANIMATION_DURATION)
        anim.start()
        progressAnim = anim
    }

    private fun animateFlash() {
        iconBackgroundBlinksDrawable?.start()
    }

    private fun updateTitleAndDescription() {
        val progressLiveData: EnrollmentProgress = progressViewModel.progressLiveData.value!!
        GlifLayoutHelper(activity!!, enrollingRfpsView!!).setDescriptionText(
            enrollingRfpsView!!.context.getString(
                if (progressLiveData.steps == -1)
                    R.string.security_settings_fingerprint_enroll_start_message
                else
                    R.string.security_settings_fingerprint_enroll_repeat_message
            )
        )
    }

    private fun showIconTouchDialog() {
        iconTouchCount = 0
        enrollingViewModel.showIconTouchDialog()
    }

    private val showDialogRunnable = Runnable { showIconTouchDialog() }

    private val progressAnimationListener: Animator.AnimatorListener =
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                startIconAnimation()
            }

            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                stopIconAnimation()
                if (progressBar.progress >= PROGRESS_BAR_MAX) {
                    progressBar.postDelayed(delayedFinishRunnable, ANIMATION_DURATION)
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
        }

    // Give the user a chance to see progress completed before jumping to the next stage.
    private val delayedFinishRunnable = Runnable { enrollingViewModel.onEnrollingDone() }

    private val iconAnimationCallback: Animatable2.AnimationCallback =
        object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(d: Drawable) {
                if (isAnimationCancelled) {
                    return
                }

                // Start animation after it has ended.
                progressBar.post { startIconAnimation() }
            }
        }

    companion object {
        private const val DEBUG = false
        private const val TAG = "FingerprintEnrollEnrollingRfpsFragment"
        private const val PROGRESS_BAR_MAX = 10000
        private const val ANIMATION_DURATION = 250L
        private const val ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN: Long = 500
        private const val ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3

        /**
         * If we don't see progress during this time, we show an error message to remind the users that
         * they need to lift the finger and touch again.
         */
        private const val HINT_TIMEOUT_DURATION = 2500
    }
}

fun FragmentActivity.bindFingerprintEnrollEnrollingRfpsView(
    view: GlifLayout,
    onSkipClickListener: View.OnClickListener
) {
    GlifLayoutHelper(this, view).let {
        it.setDescriptionText(
            getString(
                R.string.security_settings_fingerprint_enroll_start_message
            )
        )
        it.setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title)
    }

    view.findViewById<ProgressBar>(R.id.fingerprint_progress_bar)!!
        .progressBackgroundTintMode = PorterDuff.Mode.SRC

    view.getMixin(FooterBarMixin::class.java).secondaryButton =
        FooterButton.Builder(this)
            .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
            .setListener(onSkipClickListener)
            .setButtonType(FooterButton.ButtonType.SKIP)
            .setTheme(R.style.SudGlifButton_Secondary)
            .build()
}
