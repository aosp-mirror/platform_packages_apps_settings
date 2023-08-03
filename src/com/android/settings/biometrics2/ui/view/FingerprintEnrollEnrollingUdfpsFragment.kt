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

import android.annotation.RawRes
import android.content.Context
import android.hardware.biometrics.BiometricFingerprintConstants
import android.hardware.fingerprint.FingerprintManager.ENROLL_ENROLL
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.android.settings.R
import com.android.settings.biometrics2.ui.model.EnrollmentProgress
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollErrorDialogViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel
import com.android.settings.biometrics2.ui.widget.UdfpsEnrollView
import com.android.settingslib.display.DisplayDensityUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Fragment is used to handle enrolling process for udfps
 */
class FingerprintEnrollEnrollingUdfpsFragment : Fragment() {

    private var _enrollingViewModel: FingerprintEnrollEnrollingViewModel? = null
    private val enrollingViewModel: FingerprintEnrollEnrollingViewModel
        get() = _enrollingViewModel!!

    private var _rotationViewModel: DeviceRotationViewModel? = null
    private val rotationViewModel: DeviceRotationViewModel
        get() = _rotationViewModel!!

    private var _progressViewModel: FingerprintEnrollProgressViewModel? = null
    private val progressViewModel: FingerprintEnrollProgressViewModel
        get() = _progressViewModel!!

    private var _errorDialogViewModel: FingerprintEnrollErrorDialogViewModel? = null
    private val errorDialogViewModel: FingerprintEnrollErrorDialogViewModel
        get() = _errorDialogViewModel!!

    private var illustrationLottie: LottieAnimationView? = null

    private var haveShownTipLottie = false
    private var haveShownLeftEdgeLottie = false
    private var haveShownRightEdgeLottie = false
    private var haveShownCenterLottie = false
    private var haveShownGuideLottie = false

    private var enrollingView: RelativeLayout? = null

    private val titleText: TextView
        get() = enrollingView!!.findViewById(R.id.suc_layout_title)!!

    private val subTitleText: TextView
        get() = enrollingView!!.findViewById(R.id.sud_layout_subtitle)!!

    private val udfpsEnrollView: UdfpsEnrollView
        get() = enrollingView!!.findViewById(R.id.udfps_animation_view)!!

    private val skipBtn: Button
        get() = enrollingView!!.findViewById(R.id.skip_btn)!!

    private val icon: ImageView
        get() = enrollingView!!.findViewById(R.id.sud_layout_icon)!!

    private val shouldShowLottie: Boolean
        get() {
            val displayDensity = DisplayDensityUtils(requireContext())
            val currentDensityIndex: Int = displayDensity.currentIndexForDefaultDisplay
            val currentDensity: Int =
                displayDensity.defaultDisplayDensityValues[currentDensityIndex]
            val defaultDensity: Int = displayDensity.defaultDensityForDefaultDisplay
            return defaultDensity == currentDensity
        }

    private val isAccessibilityEnabled
        get() = enrollingViewModel.isAccessibilityEnabled

    private var rotation = -1

    private var enrollingCancelSignal: Any? = null

    private val onSkipClickListener = View.OnClickListener { _: View? ->
        enrollingViewModel.setOnSkipPressed()
        cancelEnrollment(true) // TODO Add test after b/273640000 fixed
    }

    private val progressObserver = Observer { progress: EnrollmentProgress? ->
        if (progress != null && progress.steps >= 0) {
            onEnrollmentProgressChange(progress)
        }
    }

    private val helpMessageObserver = Observer { helpMessage: EnrollmentStatusMessage? ->
        Log.d(TAG, "helpMessageObserver($helpMessage)")
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

    private val acquireObserver =
        Observer { isAcquiredGood: Boolean? -> isAcquiredGood?.let { onAcquired(it) } }

    private val pointerDownObserver =
        Observer { sensorId: Int? -> sensorId?.let { onPointerDown(it) } }

    private val pointerUpObserver =
        Observer { sensorId: Int? -> sensorId?.let { onPointerUp(it) } }

    private val rotationObserver =
        Observer { rotation: Int? -> rotation?.let { onRotationChanged(it) } }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                enrollingViewModel.setOnBackPressed()
                cancelEnrollment(true)
            }
        }

    // Give the user a chance to see progress completed before jumping to the next stage.
    private val delayedFinishRunnable = Runnable { enrollingViewModel.onEnrollingDone() }

    override fun onAttach(context: Context) {
        ViewModelProvider(requireActivity()).let { provider ->
            _enrollingViewModel = provider[FingerprintEnrollEnrollingViewModel::class.java]
            _rotationViewModel = provider[DeviceRotationViewModel::class.java]
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
    ): View = (inflater.inflate(
        R.layout.udfps_enroll_enrolling_v2, container, false
    ) as RelativeLayout).also {
        enrollingView = it
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rotation = rotationViewModel.liveData.value!!
        updateIllustrationLottie(rotation)

        requireActivity().bindFingerprintEnrollEnrollingUdfpsView(
            view = enrollingView!!,
            sensorProperties = enrollingViewModel.firstFingerprintSensorPropertiesInternal!!,
            rotation = rotation,
            onSkipClickListener = onSkipClickListener,
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorDialogViewModel.triggerRetryFlow.collect { retryEnrollment() }
            }
        }
    }

    private fun retryEnrollment() {
        reattachUdfpsEnrollView()

        startEnrollment()

        updateProgress(false /* animate */, progressViewModel.progressLiveData.value!!)
        progressViewModel.helpMessageLiveData.value.let {
            if (it != null) {
                onEnrollmentHelp(it)
            } else {
                updateTitleAndDescription()
            }
        }
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
                updateTitleAndDescription()
            }
        }
    }

    private fun reattachUdfpsEnrollView() {
        enrollingView!!.let {
            val newUdfpsView = LayoutInflater.from(requireActivity()).inflate(
                R.layout.udfps_enroll_enrolling_v2_udfps_view,
                null
            )
            val index = it.indexOfChild(udfpsEnrollView)
            val lp = udfpsEnrollView.layoutParams

            it.removeView(udfpsEnrollView)
            it.addView(newUdfpsView, index, lp)
            udfpsEnrollView.setSensorProperties(
                enrollingViewModel.firstFingerprintSensorPropertiesInternal
            )
        }

        // Clear lottie status
        haveShownTipLottie = false
        haveShownLeftEdgeLottie = false
        haveShownRightEdgeLottie = false
        haveShownCenterLottie = false
        haveShownGuideLottie = false
        illustrationLottie?.let {
            it.contentDescription = ""
            it.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        rotationViewModel.liveData.observe(this, rotationObserver)
    }

    override fun onPause() {
        rotationViewModel.liveData.removeObserver(rotationObserver)
        super.onPause()
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
        progressViewModel.acquireLiveData.removeObserver(acquireObserver)
        progressViewModel.pointerDownLiveData.removeObserver(pointerDownObserver)
        progressViewModel.pointerUpLiveData.removeObserver(pointerUpObserver)
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
        progressViewModel.acquireLiveData.observe(this, acquireObserver)
        progressViewModel.pointerDownLiveData.observe(this, pointerDownObserver)
        progressViewModel.pointerUpLiveData.observe(this, pointerUpObserver)
    }

    private fun updateProgress(animate: Boolean, enrollmentProgress: EnrollmentProgress) {
        if (!progressViewModel.isEnrolling) {
            Log.d(TAG, "Enrollment not started yet")
            return
        }

        val progress = getProgress(enrollmentProgress)
        Log.d(TAG, "updateProgress($animate, $enrollmentProgress), progress:$progress")

        if (enrollmentProgress.steps != -1) {
            udfpsEnrollView.onEnrollmentProgress(
                enrollmentProgress.remaining,
                enrollmentProgress.steps
            )
        }

        if (progress >= PROGRESS_BAR_MAX) {
            if (animate) {
                // Wait animations to finish, then proceed to next page
                activity!!.mainThreadHandler.postDelayed(delayedFinishRunnable, 400L)
            } else {
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

    private fun updateTitleAndDescription() {
        Log.d(TAG, "updateTitleAndDescription($currentStage)")
        when (currentStage) {
            STAGE_CENTER -> {
                titleText.setText(R.string.security_settings_fingerprint_enroll_repeat_title)
                if (isAccessibilityEnabled || illustrationLottie == null) {
                    subTitleText.setText(R.string.security_settings_udfps_enroll_start_message)
                } else if (!haveShownCenterLottie) {
                    haveShownCenterLottie = true
                    // Note: Update string reference when differentiate in between udfps & sfps
                    illustrationLottie!!.contentDescription = getString(R.string.security_settings_sfps_enroll_finger_center_title)
                    configureEnrollmentStage(R.raw.udfps_center_hint_lottie)
                }
            }

            STAGE_GUIDED -> {
                titleText.setText(R.string.security_settings_fingerprint_enroll_repeat_title)
                if (isAccessibilityEnabled || illustrationLottie == null) {
                    subTitleText.setText(
                        R.string.security_settings_udfps_enroll_repeat_a11y_message
                    )
                } else if (!haveShownGuideLottie) {
                    haveShownGuideLottie = true
                    illustrationLottie!!.contentDescription =
                        getString(R.string.security_settings_fingerprint_enroll_repeat_message)
                    // TODO(b/228100413) Could customize guided lottie animation
                    configureEnrollmentStage(R.raw.udfps_center_hint_lottie)
                }
            }

            STAGE_FINGERTIP -> {
                titleText.setText(R.string.security_settings_udfps_enroll_fingertip_title)
                if (!haveShownTipLottie && illustrationLottie != null) {
                    haveShownTipLottie = true
                    illustrationLottie!!.contentDescription =
                        getString(R.string.security_settings_udfps_tip_fingerprint_help)
                    configureEnrollmentStage(R.raw.udfps_tip_hint_lottie)
                }
            }

            STAGE_LEFT_EDGE -> {
                titleText.setText(R.string.security_settings_udfps_enroll_left_edge_title)
                if (!haveShownLeftEdgeLottie && illustrationLottie != null) {
                    haveShownLeftEdgeLottie = true
                    illustrationLottie!!.contentDescription =
                        getString(R.string.security_settings_udfps_side_fingerprint_help)
                    configureEnrollmentStage(R.raw.udfps_left_edge_hint_lottie)
                } else if (illustrationLottie == null) {
                    if (isStageHalfCompleted) {
                        subTitleText.setText(
                            R.string.security_settings_fingerprint_enroll_repeat_message
                        )
                    } else {
                        subTitleText.setText(R.string.security_settings_udfps_enroll_edge_message)
                    }
                }
            }

            STAGE_RIGHT_EDGE -> {
                titleText.setText(R.string.security_settings_udfps_enroll_right_edge_title)
                if (!haveShownRightEdgeLottie && illustrationLottie != null) {
                    haveShownRightEdgeLottie = true
                    illustrationLottie!!.contentDescription =
                        getString(R.string.security_settings_udfps_side_fingerprint_help)
                    configureEnrollmentStage(R.raw.udfps_right_edge_hint_lottie)
                } else if (illustrationLottie == null) {
                    if (isStageHalfCompleted) {
                        subTitleText.setText(
                            R.string.security_settings_fingerprint_enroll_repeat_message
                        )
                    } else {
                        subTitleText.setText(R.string.security_settings_udfps_enroll_edge_message)
                    }
                }
            }

            STAGE_UNKNOWN -> {
                titleText.setText(R.string.security_settings_fingerprint_enroll_udfps_title)
                subTitleText.setText(R.string.security_settings_udfps_enroll_start_message)
                val description: CharSequence = getString(
                    R.string.security_settings_udfps_enroll_a11y
                )
                requireActivity().title = description
            }

            else -> {
                titleText.setText(R.string.security_settings_fingerprint_enroll_udfps_title)
                subTitleText.setText(R.string.security_settings_udfps_enroll_start_message)
                val description: CharSequence = getString(
                    R.string.security_settings_udfps_enroll_a11y
                )
                requireActivity().title = description
            }
        }
    }

    private fun updateIllustrationLottie(@Surface.Rotation rotation: Int) {
        if (rotation == ROTATION_90 || rotation == ROTATION_270) {
            illustrationLottie = null
        } else if (shouldShowLottie) {
            illustrationLottie =
                enrollingView!!.findViewById(R.id.illustration_lottie)
        }
    }

    private val currentStage: Int
        get() {
            val progress = progressViewModel.progressLiveData.value!!
            if (progress.steps == -1) {
                return STAGE_UNKNOWN
            }
            val progressSteps: Int = progress.steps - progress.remaining
            return if (progressSteps < getStageThresholdSteps(0)) {
                STAGE_CENTER
            } else if (progressSteps < getStageThresholdSteps(1)) {
                STAGE_GUIDED
            } else if (progressSteps < getStageThresholdSteps(2)) {
                STAGE_FINGERTIP
            } else if (progressSteps < getStageThresholdSteps(3)) {
                STAGE_LEFT_EDGE
            } else {
                STAGE_RIGHT_EDGE
            }
        }

    private val isStageHalfCompleted: Boolean
        get() {
            val progress: EnrollmentProgress = progressViewModel.progressLiveData.value!!
            if (progress.steps == -1) {
                return false
            }
            val progressSteps: Int = progress.steps - progress.remaining
            var prevThresholdSteps = 0
            for (i in 0 until enrollingViewModel.getEnrollStageCount()) {
                val thresholdSteps = getStageThresholdSteps(i)
                if (progressSteps in prevThresholdSteps until thresholdSteps) {
                    val adjustedProgress = progressSteps - prevThresholdSteps
                    val adjustedThreshold = thresholdSteps - prevThresholdSteps
                    return adjustedProgress >= adjustedThreshold / 2
                }
                prevThresholdSteps = thresholdSteps
            }

            // After last enrollment step.
            return true
        }

    private fun getStageThresholdSteps(index: Int): Int {
        val progress: EnrollmentProgress = progressViewModel.progressLiveData.value!!
        if (progress.steps == -1) {
            Log.w(TAG, "getStageThresholdSteps: Enrollment not started yet")
            return 1
        }
        return (progress.steps * enrollingViewModel.getEnrollStageThreshold(index)).roundToInt()
    }

    private fun configureEnrollmentStage(@RawRes lottie: Int) {
        subTitleText.text = ""
        LottieCompositionFactory.fromRawRes(activity, lottie)
            .addListener { c: LottieComposition ->
                illustrationLottie?.let {
                    it.setComposition(c)
                    it.visibility = View.VISIBLE
                    it.playAnimation()
                }
            }
    }

    private fun onEnrollmentProgressChange(progress: EnrollmentProgress) {
        updateProgress(true /* animate */, progress)
        updateTitleAndDescription()
        if (isAccessibilityEnabled) {
            val steps: Int = progress.steps
            val remaining: Int = progress.remaining
            val percent = ((steps - remaining).toFloat() / steps.toFloat() * 100).toInt()
            val announcement: CharSequence = activity!!.getString(
                R.string.security_settings_udfps_enroll_progress_a11y_message, percent
            )
            enrollingViewModel.sendAccessibilityEvent(announcement)
        }
    }

    private fun onEnrollmentHelp(helpMessage: EnrollmentStatusMessage) {
        Log.d(TAG, "onEnrollmentHelp($helpMessage)")
        val helpStr: CharSequence = helpMessage.str
        if (helpStr.isNotEmpty()) {
            showError(helpStr)
            udfpsEnrollView.onEnrollmentHelp()
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

    private fun onAcquired(isAcquiredGood: Boolean) {
        udfpsEnrollView.onAcquired(isAcquiredGood)
    }

    private fun onPointerDown(sensorId: Int) {
        udfpsEnrollView.onPointerDown(sensorId)
    }

    private fun onPointerUp(sensorId: Int) {
        udfpsEnrollView.onPointerUp(sensorId)
    }

    private fun showError(error: CharSequence) {
        titleText.text = error
        titleText.contentDescription = error
        subTitleText.contentDescription = ""
    }

    private fun onRotationChanged(newRotation: Int) {
        if ((newRotation + 2) % 4 == rotation) {
            rotation = newRotation
            requireContext().configLayout(newRotation, titleText, subTitleText, icon, skipBtn)
        }
    }

    companion object {
        private val TAG = "FingerprintEnrollEnrollingUdfpsFragment"
        private const val PROGRESS_BAR_MAX = 10000
        private const val STAGE_UNKNOWN = -1
        private const val STAGE_CENTER = 0
        private const val STAGE_GUIDED = 1
        private const val STAGE_FINGERTIP = 2
        private const val STAGE_LEFT_EDGE = 3
        private const val STAGE_RIGHT_EDGE = 4
    }
}


fun FragmentActivity.bindFingerprintEnrollEnrollingUdfpsView(
    view: RelativeLayout,
    sensorProperties: FingerprintSensorPropertiesInternal,
    @Surface.Rotation rotation: Int,
    onSkipClickListener: View.OnClickListener
) {
    view.findViewById<UdfpsEnrollView>(R.id.udfps_animation_view)!!.setSensorProperties(
        sensorProperties
    )

    val titleText = view.findViewById<TextView>(R.id.suc_layout_title)!!
    val subTitleText = view.findViewById<TextView>(R.id.sud_layout_subtitle)!!
    val icon = view.findViewById<ImageView>(R.id.sud_layout_icon)!!
    val skipBtn = view.findViewById<Button>(R.id.skip_btn)!!.also {
        it.setOnClickListener(onSkipClickListener)
    }
    configLayout(rotation, titleText, subTitleText, icon, skipBtn)
}

private fun Context.configLayout(
    @Surface.Rotation newRotation: Int,
    titleText: TextView,
    subTitleText: TextView,
    icon: ImageView,
    skipBtn: Button
) {
    if (newRotation == ROTATION_270) {
        val iconLP = RelativeLayout.LayoutParams(-2, -2)
        iconLP.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        iconLP.addRule(RelativeLayout.END_OF, R.id.udfps_animation_view)
        iconLP.topMargin = convertDpToPixel(76.64f)
        iconLP.leftMargin = convertDpToPixel(151.54f)
        icon.layoutParams = iconLP
        val titleLP = RelativeLayout.LayoutParams(-1, -2)
        titleLP.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        titleLP.addRule(RelativeLayout.END_OF, R.id.udfps_animation_view)
        titleLP.topMargin = convertDpToPixel(138f)
        titleLP.leftMargin = convertDpToPixel(144f)
        titleText.layoutParams = titleLP
        val subtitleLP = RelativeLayout.LayoutParams(-1, -2)
        subtitleLP.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        subtitleLP.addRule(RelativeLayout.END_OF, R.id.udfps_animation_view)
        subtitleLP.topMargin = convertDpToPixel(198f)
        subtitleLP.leftMargin = convertDpToPixel(144f)
        subTitleText.layoutParams = subtitleLP
    } else if (newRotation == ROTATION_90) {
        val metrics = resources.displayMetrics
        val iconLP = RelativeLayout.LayoutParams(-2, -2)
        iconLP.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        iconLP.addRule(RelativeLayout.ALIGN_PARENT_START)
        iconLP.topMargin = convertDpToPixel(76.64f)
        iconLP.leftMargin = convertDpToPixel(71.99f)
        icon.layoutParams = iconLP
        val titleLP = RelativeLayout.LayoutParams(
            metrics.widthPixels / 2, -2
        )
        titleLP.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        titleLP.addRule(RelativeLayout.ALIGN_PARENT_START, R.id.udfps_animation_view)
        titleLP.topMargin = convertDpToPixel(138f)
        titleLP.leftMargin = convertDpToPixel(66f)
        titleText.layoutParams = titleLP
        val subtitleLP = RelativeLayout.LayoutParams(
            metrics.widthPixels / 2, -2
        )
        subtitleLP.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        subtitleLP.addRule(RelativeLayout.ALIGN_PARENT_START)
        subtitleLP.topMargin = convertDpToPixel(198f)
        subtitleLP.leftMargin = convertDpToPixel(66f)
        subTitleText.layoutParams = subtitleLP
    }
    if (newRotation == ROTATION_90 || newRotation == ROTATION_270) {
        val skipBtnLP = skipBtn.layoutParams as RelativeLayout.LayoutParams
        skipBtnLP.topMargin = convertDpToPixel(26f)
        skipBtnLP.leftMargin = convertDpToPixel(54f)
        skipBtn.requestLayout()
    }
}

fun Context.convertDpToPixel(dp: Float): Int {
    return (dp * resources.displayMetrics.density).toInt()
}
