/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context
import android.hardware.fingerprint.FingerprintManager.ENROLL_FIND_SENSOR
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.biometrics2.ui.model.EnrollmentProgress
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage
import com.android.settings.biometrics2.ui.viewmodel.DeviceFoldedViewModel
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollErrorDialogViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel
import com.android.settingslib.widget.LottieColorUtils
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.launch

/**
 * Fragment explaining the side fingerprint sensor location for fingerprint enrollment.
 * It interacts with ProgressViewModel, FoldCallback (for different lottie), and
 * LottieAnimationView.
 * <pre>
 * | Has                 | UDFPS | SFPS | Other (Rear FPS) |
 * |---------------------|-------|------|------------------|
 * | Primary button      | Yes   | No   | No               |
 * | Illustration Lottie | Yes   | Yes  | No               |
 * | Animation           | No    | No   | Depend on layout |
 * | Progress ViewModel  | No    | Yes  | Yes              |
 * | Orientation detect  | No    | Yes  | No               |
 * | Foldable detect     | No    | Yes  | No               |
 * </pre>
 */
class FingerprintEnrollFindSfpsFragment : Fragment() {

    private var _viewModel: FingerprintEnrollFindSensorViewModel? = null
    private val viewModel: FingerprintEnrollFindSensorViewModel
        get() = _viewModel!!

    private var _progressViewModel: FingerprintEnrollProgressViewModel? = null
    private val progressViewModel: FingerprintEnrollProgressViewModel
        get() = _progressViewModel!!

    private var _rotationViewModel: DeviceRotationViewModel? = null
    private val rotationViewModel: DeviceRotationViewModel
        get() = _rotationViewModel!!

    private var _foldedViewModel: DeviceFoldedViewModel? = null
    private val foldedViewModel: DeviceFoldedViewModel
        get() = _foldedViewModel!!

    private var _errorDialogViewModel: FingerprintEnrollErrorDialogViewModel? = null
    private val errorDialogViewModel: FingerprintEnrollErrorDialogViewModel
        get() = _errorDialogViewModel!!

    private var findSfpsView: GlifLayout? = null

    private val onSkipClickListener =
        View.OnClickListener { _: View? -> viewModel.onSkipButtonClick() }

    private val illustrationLottie: LottieAnimationView
        get() = findSfpsView!!.findViewById(R.id.illustration_lottie)!!

    private var enrollingCancelSignal: Any? = null

    @Surface.Rotation
    private var animationRotation = -1

    private val rotationObserver = Observer { rotation: Int? ->
        rotation?.let { onRotationChanged(it) }
    }

    private val progressObserver = Observer { progress: EnrollmentProgress? ->
        if (progress != null && !progress.isInitialStep) {
            cancelEnrollment(true)
        }
    }

    private val errorMessageObserver = Observer{ errorMessage: EnrollmentStatusMessage? ->
        Log.d(TAG, "errorMessageObserver($errorMessage)")
        errorMessage?.let { onEnrollmentError(it) }
    }

    private val canceledSignalObserver = Observer { canceledSignal: Any? ->
        canceledSignal?.let { onEnrollmentCanceled(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = (inflater.inflate(
        R.layout.sfps_enroll_find_sensor_layout,
        container,
        false
    ) as GlifLayout).also {
        findSfpsView = it
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().bindFingerprintEnrollFindSfpsView(
            view = findSfpsView!!,
            onSkipClickListener = onSkipClickListener
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorDialogViewModel.triggerRetryFlow.collect { startEnrollment() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val isErrorDialogShown = errorDialogViewModel.isDialogShown
        Log.d(TAG, "onStart(), isEnrolling:${progressViewModel.isEnrolling}"
                + ", isErrorDialog:$isErrorDialogShown")
        if (!isErrorDialogShown) {
            startEnrollment()
        }
    }

    override fun onResume() {
        super.onResume()
        val rotationLiveData: LiveData<Int> = rotationViewModel.liveData
        playLottieAnimation(rotationLiveData.value!!)
        rotationLiveData.observe(this, rotationObserver)
    }

    override fun onPause() {
        rotationViewModel.liveData.removeObserver(rotationObserver)
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        val isEnrolling = progressViewModel.isEnrolling
        val isConfigChange = requireActivity().isChangingConfigurations
        Log.d(TAG, "onStop(), enrolling:$isEnrolling isConfigChange:$isConfigChange")
        if (isEnrolling && !isConfigChange) {
            cancelEnrollment(false)
        }
    }

    private fun removeEnrollmentObservers() {
        progressViewModel.errorMessageLiveData.removeObserver(errorMessageObserver)
        progressViewModel.progressLiveData.removeObserver(progressObserver)
    }

    private fun startEnrollment() {
        enrollingCancelSignal = progressViewModel.startEnrollment(ENROLL_FIND_SENSOR)
        if (enrollingCancelSignal == null) {
            Log.e(TAG, "startEnrollment(), failed to start enrollment")
        } else {
            Log.d(TAG, "startEnrollment(), success")
        }
        progressViewModel.progressLiveData.observe(this, progressObserver)
        progressViewModel.errorMessageLiveData.observe(this, errorMessageObserver)
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

    private fun onRotationChanged(@Surface.Rotation newRotation: Int) {
        if (DEBUG) {
            Log.d(TAG, "onRotationChanged() from $animationRotation to $newRotation")
        }
        if ((newRotation + 2) % 4 == animationRotation) {
            // Fragment not changed, we just need to play correct rotation animation
            playLottieAnimation(newRotation)
        }
    }

    private fun onEnrollmentError(errorMessage: EnrollmentStatusMessage) {
        progressViewModel.cancelEnrollment()
        lifecycleScope.launch {
            Log.d(TAG, "newDialogFlow as $errorMessage")
            errorDialogViewModel.newDialog(errorMessage.msgId)
        }
    }

    private fun onEnrollmentCanceled(canceledSignal: Any) {
        Log.d(
            TAG,
            "onEnrollmentCanceled enrolling:$enrollingCancelSignal, canceled:$canceledSignal"
        )
        if (enrollingCancelSignal === canceledSignal) {
            val progress: EnrollmentProgress? = progressViewModel.progressLiveData.value
            progressViewModel.canceledSignalLiveData.removeObserver(canceledSignalObserver)
            progressViewModel.clearProgressLiveData()
            if (progress != null && !progress.isInitialStep) {
                viewModel.onStartButtonClick()
            }
        }
    }

    private fun playLottieAnimation(@Surface.Rotation rotation: Int) {
        @RawRes val animationRawRes = getSfpsLottieAnimationRawRes(rotation)
        Log.d(
            TAG,
            "play lottie animation $animationRawRes, previous rotation:$animationRotation"
                    + ", new rotation:" + rotation
        )
        animationRotation = rotation
        illustrationLottie.setAnimation(animationRawRes)
        LottieColorUtils.applyDynamicColors(activity, illustrationLottie)
        illustrationLottie.visibility = View.VISIBLE
        illustrationLottie.playAnimation()
    }

    @RawRes
    private fun getSfpsLottieAnimationRawRes(@Surface.Rotation rotation: Int): Int {
        val isFolded = java.lang.Boolean.FALSE != foldedViewModel.liveData.value
        return when (rotation) {
            Surface.ROTATION_90 ->
                if (isFolded)
                    R.raw.fingerprint_edu_lottie_folded_top_left
                else
                    R.raw.fingerprint_edu_lottie_portrait_top_left
            Surface.ROTATION_180 ->
                if (isFolded)
                    R.raw.fingerprint_edu_lottie_folded_bottom_left
                else
                    R.raw.fingerprint_edu_lottie_landscape_bottom_left
            Surface.ROTATION_270 ->
                if (isFolded)
                    R.raw.fingerprint_edu_lottie_folded_bottom_right
                else
                    R.raw.fingerprint_edu_lottie_portrait_bottom_right
            else ->
                if (isFolded)
                    R.raw.fingerprint_edu_lottie_folded_top_right
                else
                    R.raw.fingerprint_edu_lottie_landscape_top_right
        }
    }

    override fun onAttach(context: Context) {
        ViewModelProvider(requireActivity()).let { provider ->
            _viewModel = provider[FingerprintEnrollFindSensorViewModel::class.java]
            _progressViewModel = provider[FingerprintEnrollProgressViewModel::class.java]
            _rotationViewModel = provider[DeviceRotationViewModel::class.java]
            _foldedViewModel = provider[DeviceFoldedViewModel::class.java]
            _errorDialogViewModel = provider[FingerprintEnrollErrorDialogViewModel::class.java]
        }
        super.onAttach(context)
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "FingerprintEnrollFindSfpsFragment"
    }
}

fun FragmentActivity.bindFingerprintEnrollFindSfpsView(
    view: GlifLayout,
    onSkipClickListener: View.OnClickListener
) {
    view.getMixin(FooterBarMixin::class.java).let {
        it.secondaryButton = FooterButton.Builder(this)
            .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
            .setButtonType(FooterButton.ButtonType.SKIP)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
            .build()
        it.secondaryButton.setOnClickListener(onSkipClickListener)
    }

    GlifLayoutHelper(this, view).let {
        it.setHeaderText(R.string.security_settings_sfps_enroll_find_sensor_title)
        it.setDescriptionText(
            getText(R.string.security_settings_sfps_enroll_find_sensor_message)
        )
    }
}
