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
import android.annotation.RawRes
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.fingerprint.FingerprintManager.ENROLL_ENROLL
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.android.settings.R
import com.android.settings.biometrics2.ui.model.EnrollmentProgress
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollErrorDialogViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.template.DescriptionMixin
import com.google.android.setupdesign.template.HeaderMixin
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Fragment is used to handle enrolling process for sfps
 */
class FingerprintEnrollEnrollingSfpsFragment : Fragment() {

    private var _enrollingViewModel: FingerprintEnrollEnrollingViewModel? = null
    private val enrollingViewModel: FingerprintEnrollEnrollingViewModel
        get() = _enrollingViewModel!!

    private var _progressViewModel: FingerprintEnrollProgressViewModel? = null
    private val progressViewModel: FingerprintEnrollProgressViewModel
        get() = _progressViewModel!!

    private var _errorDialogViewModel: FingerprintEnrollErrorDialogViewModel? = null
    private val errorDialogViewModel: FingerprintEnrollErrorDialogViewModel
        get() = _errorDialogViewModel!!

    private val fastOutSlowInInterpolator: Interpolator
        get() = AnimationUtils.loadInterpolator(
            activity,
            androidx.appcompat.R.interpolator.fast_out_slow_in,
        )

    private var enrollingView: GlifLayout? = null

    private val progressBar: ProgressBar
        get() = enrollingView!!.findViewById(R.id.fingerprint_progress_bar)!!

    private var progressAnim: ObjectAnimator? = null

    private val progressAnimationListener: Animator.AnimatorListener =
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                if (progressBar.progress >= PROGRESS_BAR_MAX) {
                    progressBar.postDelayed(delayedFinishRunnable, PROGRESS_ANIMATION_DURATION)
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
        }

    private val illustrationLottie: LottieAnimationView
        get() = enrollingView!!.findViewById(R.id.illustration_lottie)!!

    private var haveShownSfpsNoAnimationLottie = false
    private var haveShownSfpsCenterLottie = false
    private var haveShownSfpsTipLottie = false
    private var haveShownSfpsLeftEdgeLottie = false
    private var haveShownSfpsRightEdgeLottie = false

    private var helpAnimation: ObjectAnimator? = null

    private var iconTouchCount = 0

    private val showIconTouchDialogRunnable = Runnable { showIconTouchDialog() }

    private var enrollingCancelSignal: Any? = null

    // Give the user a chance to see progress completed before jumping to the next stage.
    private val delayedFinishRunnable = Runnable { enrollingViewModel.onEnrollingDone() }

    private val onSkipClickListener = View.OnClickListener { _: View? ->
        enrollingViewModel.setOnSkipPressed()
        cancelEnrollment(true)
    }

    private val progressObserver = Observer { progress: EnrollmentProgress? ->
        if (progress != null && progress.steps >= 0) {
            onEnrollmentProgressChange(progress)
        }
    }

    private val helpMessageObserver = Observer { helpMessage: EnrollmentStatusMessage? ->
        helpMessage?.let { onEnrollmentHelp(it) }
    }

    private val errorMessageObserver = Observer { errorMessage: EnrollmentStatusMessage? ->
        Log.d(TAG, "errorMessageObserver($errorMessage)")
        errorMessage?.let { onEnrollmentError(it) }
    }

    private val canceledSignalObserver = Observer { canceledSignal: Any? ->
        Log.d(TAG, "canceledSignalObserver($canceledSignal)")
        canceledSignal?.let { onEnrollmentCanceled(it) }
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                enrollingViewModel.setOnBackPressed()
                cancelEnrollment(true)
            }
        }

    override fun onAttach(context: Context) {
        ViewModelProvider(requireActivity()).let { provider ->
            _enrollingViewModel = provider[FingerprintEnrollEnrollingViewModel::class.java]
            _progressViewModel = provider[FingerprintEnrollProgressViewModel::class.java]
            _errorDialogViewModel = provider[FingerprintEnrollErrorDialogViewModel::class.java]
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
    ): View? {
        enrollingView = inflater.inflate(
            R.layout.sfps_enroll_enrolling,
            container, false
        ) as GlifLayout
        return enrollingView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().bindFingerprintEnrollEnrollingSfpsView(
            view = enrollingView!!,
            onSkipClickListener = onSkipClickListener
        )

        // setHelpAnimation()
        helpAnimation = ObjectAnimator.ofFloat(
            enrollingView!!.findViewById<RelativeLayout>(R.id.progress_lottie)!!,
            "translationX" /* propertyName */,
            0f,
            HELP_ANIMATION_TRANSLATION_X,
            -1 * HELP_ANIMATION_TRANSLATION_X,
            HELP_ANIMATION_TRANSLATION_X,
            0f
        ).also {
            it.interpolator = AccelerateDecelerateInterpolator()
            it.setDuration(HELP_ANIMATION_DURATION)
            it.setAutoCancel(false)
        }

        progressBar.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                iconTouchCount++
                if (iconTouchCount == ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN) {
                    showIconTouchDialog()
                } else {
                    progressBar.postDelayed(
                        showIconTouchDialogRunnable,
                        ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN
                    )
                }
            } else if (event.actionMasked == MotionEvent.ACTION_CANCEL
                || event.actionMasked == MotionEvent.ACTION_UP
            ) {
                progressBar.removeCallbacks(showIconTouchDialogRunnable)
            }
            true
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorDialogViewModel.triggerRetryFlow.collect { retryEnrollment() }
            }
        }
    }

    private fun retryEnrollment() {
        startEnrollment()
        updateProgress(false /* animate */, progressViewModel.progressLiveData.value!!)
    }

    override fun onStart() {
        super.onStart()
        val isEnrolling = progressViewModel.isEnrolling
        val isErrorDialogShown = errorDialogViewModel.isDialogShown
        Log.d(TAG, "onStart(), isEnrolling:$isEnrolling, isErrorDialog:$isErrorDialogShown")
        if (!isErrorDialogShown) {
            startEnrollment()
        }

        updateProgress(false /* animate */, progressViewModel.progressLiveData.value!!)
        progressViewModel.helpMessageLiveData.value.let {
            if (it != null) {
                onEnrollmentHelp(it)
            } else {
                clearError()
                updateTitleAndDescription()
            }
        }
    }

    override fun onStop() {
        removeEnrollmentObservers()
        val isEnrolling = progressViewModel.isEnrolling
        val isConfigChange = requireActivity().isChangingConfigurations
        Log.d(TAG, "onStop(), enrolling:$isEnrolling isConfigChange:$isConfigChange")
        if (isEnrolling && !isConfigChange) {
            cancelEnrollment(false)
        }
        super.onStop()
    }

    private fun removeEnrollmentObservers() {
        progressViewModel.errorMessageLiveData.removeObserver(errorMessageObserver)
        progressViewModel.progressLiveData.removeObserver(progressObserver)
        progressViewModel.helpMessageLiveData.removeObserver(helpMessageObserver)
    }

    private fun cancelEnrollment(waitForLastCancelErrMsg: Boolean) {
        if (!progressViewModel.isEnrolling) {
            Log.d(TAG, "cancelEnrollment(), failed because isEnrolling is false")
            return
        }
        removeEnrollmentObservers()
        if (waitForLastCancelErrMsg) {
            progressViewModel.canceledSignalLiveData.observe(this, canceledSignalObserver)
        } else {
            enrollingCancelSignal = null
        }
        val cancelResult: Boolean = progressViewModel.cancelEnrollment()
        if (!cancelResult) {
            Log.e(TAG, "cancelEnrollment(), failed to cancel enrollment")
        }
    }

    private fun startEnrollment() {
        enrollingCancelSignal = progressViewModel.startEnrollment(ENROLL_ENROLL)
        if (enrollingCancelSignal == null) {
            Log.e(TAG, "startEnrollment(), failed")
        } else {
            Log.d(TAG, "startEnrollment(), success")
        }
        progressViewModel.progressLiveData.observe(this, progressObserver)
        progressViewModel.helpMessageLiveData.observe(this, helpMessageObserver)
        progressViewModel.errorMessageLiveData.observe(this, errorMessageObserver)
    }

    private fun configureEnrollmentStage(description: CharSequence, @RawRes lottie: Int) {
        GlifLayoutHelper(requireActivity(), enrollingView!!).setDescriptionText(description)
        LottieCompositionFactory.fromRawRes(activity, lottie)
            .addListener { c: LottieComposition ->
                illustrationLottie.setComposition(c)
                illustrationLottie.visibility = View.VISIBLE
                illustrationLottie.playAnimation()
            }
    }

    private val currentSfpsStage: Int
        get() {
            val progressLiveData: EnrollmentProgress =
                progressViewModel.progressLiveData.value
                    ?: return STAGE_UNKNOWN
            val progressSteps: Int = progressLiveData.steps - progressLiveData.remaining
            return if (progressSteps < getStageThresholdSteps(0)) {
                SFPS_STAGE_NO_ANIMATION
            } else if (progressSteps < getStageThresholdSteps(1)) {
                SFPS_STAGE_CENTER
            } else if (progressSteps < getStageThresholdSteps(2)) {
                SFPS_STAGE_FINGERTIP
            } else if (progressSteps < getStageThresholdSteps(3)) {
                SFPS_STAGE_LEFT_EDGE
            } else {
                SFPS_STAGE_RIGHT_EDGE
            }
        }

    private fun onEnrollmentHelp(helpMessage: EnrollmentStatusMessage) {
        Log.d(TAG, "onEnrollmentHelp($helpMessage)")
        val helpStr: CharSequence = helpMessage.str
        if (helpStr.isNotEmpty()) {
            showError(helpStr)
        }
    }

    private fun onEnrollmentError(errorMessage: EnrollmentStatusMessage) {
        cancelEnrollment(true)
        lifecycleScope.launch {
            Log.d(TAG, "newDialog $errorMessage")
            errorDialogViewModel.newDialog(errorMessage.msgId)
        }
    }

    private fun onEnrollmentCanceled(canceledSignal: Any) {
        Log.d(
            TAG,
            "onEnrollmentCanceled enrolling:$enrollingCancelSignal, canceled:$canceledSignal"
        )
        if (enrollingCancelSignal === canceledSignal) {
            progressViewModel.canceledSignalLiveData.removeObserver(canceledSignalObserver)
            progressViewModel.clearProgressLiveData()
            if (enrollingViewModel.onBackPressed) {
                enrollingViewModel.onCancelledDueToOnBackPressed()
            } else if (enrollingViewModel.onSkipPressed) {
                enrollingViewModel.onCancelledDueToOnSkipPressed()
            }
        }
    }

    private fun announceEnrollmentProgress(announcement: CharSequence) {
        enrollingViewModel.sendAccessibilityEvent(announcement)
    }

    private fun onEnrollmentProgressChange(progress: EnrollmentProgress) {
        updateProgress(true /* animate */, progress)
        if (enrollingViewModel.isAccessibilityEnabled) {
            val percent: Int =
                ((progress.steps - progress.remaining).toFloat() / progress.steps.toFloat() * 100).toInt()
            val announcement: CharSequence = getString(
                R.string.security_settings_sfps_enroll_progress_a11y_message, percent
            )
            announceEnrollmentProgress(announcement)
            illustrationLottie.contentDescription =
                getString(R.string.security_settings_sfps_animation_a11y_label, percent)
        }
        updateTitleAndDescription()
    }

    private fun updateProgress(animate: Boolean, enrollmentProgress: EnrollmentProgress) {
        if (!progressViewModel.isEnrolling) {
            Log.d(TAG, "Enrollment not started yet")
            return
        }

        val progress = getProgress(enrollmentProgress)
        Log.d(TAG, "updateProgress($animate, $enrollmentProgress), old:${progressBar.progress}"
                + ", new:$progress")

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
        enrollingView!!.let {
            it.headerText = error
            it.headerTextView.contentDescription = error
            GlifLayoutHelper(requireActivity(), it).setDescriptionText("")
        }

        if (isResumed && !helpAnimation!!.isRunning) {
            helpAnimation!!.start()
        }
        applySfpsErrorDynamicColors(true)
        if (isResumed && enrollingViewModel.isAccessibilityEnabled) {
            enrollingViewModel.vibrateError(javaClass.simpleName + "::showError")
        }
    }

    private fun clearError() {
        applySfpsErrorDynamicColors(false)
    }

    private fun animateProgress(progress: Int) {
        progressAnim?.cancel()
        progressAnim = ObjectAnimator.ofInt(
            progressBar,
            "progress",
            progressBar.progress,
            progress
        ).also {
            it.addListener(progressAnimationListener)
            it.interpolator = fastOutSlowInInterpolator
            it.setDuration(PROGRESS_ANIMATION_DURATION)
            it.start()
        }
    }

    /**
     * Applies dynamic colors corresponding to showing or clearing errors on the progress bar
     * and finger lottie for SFPS
     */
    private fun applySfpsErrorDynamicColors(isError: Boolean) {
        progressBar.applyProgressBarDynamicColor(requireContext(), isError)
        illustrationLottie.applyLottieDynamicColor(requireContext(), isError)
    }

    private fun getStageThresholdSteps(index: Int): Int {
        val progressLiveData: EnrollmentProgress? =
            progressViewModel.progressLiveData.value
        if (progressLiveData == null || progressLiveData.steps == -1) {
            Log.w(TAG, "getStageThresholdSteps: Enrollment not started yet")
            return 1
        }
        return (progressLiveData.steps
                * enrollingViewModel.getEnrollStageThreshold(index)).roundToInt()
    }

    private fun updateTitleAndDescription() {
        val helper = GlifLayoutHelper(requireActivity(), enrollingView!!)
        if (enrollingViewModel.isAccessibilityEnabled) {
            enrollingViewModel.clearTalkback()
            helper.glifLayout.descriptionTextView.accessibilityLiveRegion =
                View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val stage = currentSfpsStage
        if (DEBUG) {
            Log.d(
                TAG, "updateTitleAndDescription, stage:" + stage
                        + ", noAnimation:" + haveShownSfpsNoAnimationLottie
                        + ", center:" + haveShownSfpsCenterLottie
                        + ", tip:" + haveShownSfpsTipLottie
                        + ", leftEdge:" + haveShownSfpsLeftEdgeLottie
                        + ", rightEdge:" + haveShownSfpsRightEdgeLottie
            )
        }
        when (stage) {
            SFPS_STAGE_NO_ANIMATION -> {
                helper.setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title)
                if (!haveShownSfpsNoAnimationLottie) {
                    haveShownSfpsNoAnimationLottie = true
                    illustrationLottie.contentDescription =
                        getString(R.string.security_settings_sfps_animation_a11y_label, 0)
                    configureEnrollmentStage(
                        getString(R.string.security_settings_sfps_enroll_start_message),
                        R.raw.sfps_lottie_no_animation
                    )
                }
            }

            SFPS_STAGE_CENTER -> {
                helper.setHeaderText(R.string.security_settings_sfps_enroll_finger_center_title)
                if (!haveShownSfpsCenterLottie) {
                    haveShownSfpsCenterLottie = true
                    configureEnrollmentStage(
                        getString(R.string.security_settings_sfps_enroll_start_message),
                        R.raw.sfps_lottie_pad_center
                    )
                }
            }

            SFPS_STAGE_FINGERTIP -> {
                helper.setHeaderText(R.string.security_settings_sfps_enroll_fingertip_title)
                if (!haveShownSfpsTipLottie) {
                    haveShownSfpsTipLottie = true
                    configureEnrollmentStage("", R.raw.sfps_lottie_tip)
                }
            }

            SFPS_STAGE_LEFT_EDGE -> {
                helper.setHeaderText(R.string.security_settings_sfps_enroll_left_edge_title)
                if (!haveShownSfpsLeftEdgeLottie) {
                    haveShownSfpsLeftEdgeLottie = true
                    configureEnrollmentStage("", R.raw.sfps_lottie_left_edge)
                }
            }

            SFPS_STAGE_RIGHT_EDGE -> {
                helper.setHeaderText(R.string.security_settings_sfps_enroll_right_edge_title)
                if (!haveShownSfpsRightEdgeLottie) {
                    haveShownSfpsRightEdgeLottie = true
                    configureEnrollmentStage("", R.raw.sfps_lottie_right_edge)
                }
            }

            STAGE_UNKNOWN -> {
                // Don't use BiometricEnrollBase#setHeaderText, since that invokes setTitle,
                // which gets announced for a11y upon entering the page. For SFPS, we want to
                // announce a different string for a11y upon entering the page.
                helper.setHeaderText(R.string.security_settings_sfps_enroll_find_sensor_title)
                helper.setDescriptionText(
                    getString(R.string.security_settings_sfps_enroll_start_message)
                )
                val description: CharSequence = getString(
                    R.string.security_settings_sfps_enroll_find_sensor_message
                )
                helper.glifLayout.headerTextView.contentDescription = description
                helper.activity.title = description
            }

            else -> {
                helper.setHeaderText(R.string.security_settings_sfps_enroll_find_sensor_title)
                helper.setDescriptionText(
                    getString(R.string.security_settings_sfps_enroll_start_message)
                )
                val description: CharSequence = getString(
                    R.string.security_settings_sfps_enroll_find_sensor_message
                )
                helper.glifLayout.headerTextView.contentDescription = description
                helper.activity.title = description
            }
        }
    }

    private fun showIconTouchDialog() {
        iconTouchCount = 0
        enrollingViewModel.showIconTouchDialog()
    }

    companion object {
        private val TAG = FingerprintEnrollEnrollingSfpsFragment::class.java.simpleName
        private const val DEBUG = false
        private const val PROGRESS_BAR_MAX = 10000
        private const val HELP_ANIMATION_DURATION = 550L
        private const val HELP_ANIMATION_TRANSLATION_X = 40f
        private const val PROGRESS_ANIMATION_DURATION = 250L
        private const val ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN: Long = 500
        private const val ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3
        private const val STAGE_UNKNOWN = -1
        private const val SFPS_STAGE_NO_ANIMATION = 0
        private const val SFPS_STAGE_CENTER = 1
        private const val SFPS_STAGE_FINGERTIP = 2
        private const val SFPS_STAGE_LEFT_EDGE = 3
        private const val SFPS_STAGE_RIGHT_EDGE = 4
    }
}

fun FragmentActivity.bindFingerprintEnrollEnrollingSfpsView(
    view: GlifLayout,
    onSkipClickListener: View.OnClickListener
) {
    GlifLayoutHelper(this, view).setDescriptionText(
        getString(R.string.security_settings_fingerprint_enroll_start_message)
    )

    view.getMixin(FooterBarMixin::class.java).secondaryButton = FooterButton.Builder(this)
        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
        .setListener(onSkipClickListener)
        .setButtonType(FooterButton.ButtonType.SKIP)
        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
        .build()

    view.findViewById<ProgressBar>(R.id.fingerprint_progress_bar)!!.progressBackgroundTintMode =
        PorterDuff.Mode.SRC

    view.findViewById<ProgressBar>(R.id.fingerprint_progress_bar)!!
        .applyProgressBarDynamicColor(this, false)

    view.findViewById<LottieAnimationView>(R.id.illustration_lottie)!!
        .applyLottieDynamicColor(this, false)

    view.maybeHideSfpsText(resources.configuration.orientation)
}

private fun ProgressBar.applyProgressBarDynamicColor(context: Context, isError: Boolean) {
    progressTintList = ColorStateList.valueOf(
        context.getColor(
            if (isError)
                R.color.sfps_enrollment_progress_bar_error_color
            else
                R.color.sfps_enrollment_progress_bar_fill_color
        )
    )
    progressTintMode = PorterDuff.Mode.SRC
    invalidate()
}

fun LottieAnimationView.applyLottieDynamicColor(context: Context, isError: Boolean) {
    addValueCallback(
        KeyPath(".blue100", "**"),
        LottieProperty.COLOR_FILTER
    ) {
        PorterDuffColorFilter(
            context.getColor(
                if (isError)
                    R.color.sfps_enrollment_fp_error_color
                else
                    R.color.sfps_enrollment_fp_captured_color
            ),
            PorterDuff.Mode.SRC_ATOP
        )
    }
    invalidate()
}

fun GlifLayout.maybeHideSfpsText(@Configuration.Orientation orientation: Int) {
    val headerMixin: HeaderMixin = getMixin(HeaderMixin::class.java)
    val descriptionMixin: DescriptionMixin = getMixin(DescriptionMixin::class.java)

    val isLandscape = (orientation == Configuration.ORIENTATION_LANDSCAPE)
    headerMixin.setAutoTextSizeEnabled(isLandscape)
    if (isLandscape) {
        headerMixin.textView.minLines = 0
        headerMixin.textView.maxLines = 10
        descriptionMixin.textView.minLines = 0
        descriptionMixin.textView.maxLines = 10
    } else {
        headerMixin.textView.setLines(4)
        // hide the description
        descriptionMixin.textView.setLines(0)
    }
}
