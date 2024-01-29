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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settings.R
import com.android.settings.biometrics.fingerprint.FingerprintFindSensorAnimation
import com.android.settings.biometrics2.ui.model.EnrollmentProgress
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollErrorDialogViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.launch

/**
 * Fragment explaining the side fingerprint sensor location for fingerprint enrollment.
 * It interacts with ProgressViewModel, and FingerprintFindSensorAnimation.
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
class FingerprintEnrollFindRfpsFragment : Fragment() {

    private var _viewModel: FingerprintEnrollFindSensorViewModel? = null
    private val viewModel: FingerprintEnrollFindSensorViewModel
        get() = _viewModel!!

    private var _progressViewModel: FingerprintEnrollProgressViewModel? = null
    private val progressViewModel: FingerprintEnrollProgressViewModel
        get() = _progressViewModel!!

    private var _rotationViewModel: DeviceRotationViewModel? = null
    private val rotationViewModel: DeviceRotationViewModel
        get() = _rotationViewModel!!

    private var _errorDialogViewModel: FingerprintEnrollErrorDialogViewModel? = null
    private val errorDialogViewModel: FingerprintEnrollErrorDialogViewModel
        get() = _errorDialogViewModel!!

    private var findRfpsView: GlifLayout? = null

    private val onSkipClickListener =
        View.OnClickListener { _: View? -> viewModel.onSkipButtonClick() }

    private var animation: FingerprintFindSensorAnimation? = null

    private var enrollingCancelSignal: Any? = null

    @Surface.Rotation
    private var lastRotation = -1

    private val progressObserver = Observer { progress: EnrollmentProgress? ->
        if (progress != null && !progress.isInitialStep) {
            cancelEnrollment(true)
        }
    }

    private val errorMessageObserver = Observer { errorMessage: EnrollmentStatusMessage? ->
        Log.d(TAG, "errorMessageObserver($errorMessage)")
        errorMessage?.let { onEnrollmentError(it) }
    }

    private val canceledSignalObserver = Observer { canceledSignal: Any? ->
        canceledSignal?.let { onEnrollmentCanceled(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        findRfpsView = inflater.inflate(
            R.layout.fingerprint_enroll_find_sensor,
            container,
            false
        ) as GlifLayout

        val animationView = findRfpsView!!.findViewById<View>(
            R.id.fingerprint_sensor_location_animation
        )
        if (animationView is FingerprintFindSensorAnimation) {
            animation = animationView
        }

        return findRfpsView!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().bindFingerprintEnrollFindRfpsView(
            view = findRfpsView!!,
            onSkipClickListener = onSkipClickListener
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorDialogViewModel.triggerRetryFlow.collect { retryLookingForFingerprint() }
            }
        }
    }

    private fun retryLookingForFingerprint() {
        startEnrollment()
        animation?.let {
            Log.d(TAG, "retry, start animation")
            it.startAnimation()
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
        val rotationLiveData: LiveData<Int> = rotationViewModel.liveData
        lastRotation = rotationLiveData.value!!
        if (!errorDialogViewModel.isDialogShown) {
            animation?.let {
                Log.d(TAG, "onResume(), start animation")
                it.startAnimation()
            }
        }
        super.onResume()
    }

    override fun onPause() {
        animation?.let {
            if (DEBUG) {
                Log.d(TAG, "onPause(), pause animation")
            }
            it.pauseAnimation()
        }
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        removeEnrollmentObservers()
        val isEnrolling = progressViewModel.isEnrolling
        val isConfigChange = requireActivity().isChangingConfigurations
        Log.d(TAG, "onStop(), enrolling:$isEnrolling isConfigChange:$isConfigChange")
        if (isEnrolling && !isConfigChange) {
            cancelEnrollment(false)
        }
    }

    private fun removeEnrollmentObservers() {
        progressViewModel.progressLiveData.removeObserver(progressObserver)
        progressViewModel.helpMessageLiveData.removeObserver(errorMessageObserver)
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

    private fun onEnrollmentError(errorMessage: EnrollmentStatusMessage) {
        cancelEnrollment(false)
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

    override fun onDestroy() {
        animation?.let {
            if (DEBUG) {
                Log.d(TAG, "onDestroy(), stop animation")
            }
            it.stopAnimation()
        }
        super.onDestroy()
    }

    override fun onAttach(context: Context) {
        ViewModelProvider(requireActivity()).let { provider ->
            _viewModel = provider[FingerprintEnrollFindSensorViewModel::class.java]
            _progressViewModel = provider[FingerprintEnrollProgressViewModel::class.java]
            _rotationViewModel = provider[DeviceRotationViewModel::class.java]
            _errorDialogViewModel = provider[FingerprintEnrollErrorDialogViewModel::class.java]
        }
        super.onAttach(context)
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "FingerprintEnrollFindRfpsFragment"
    }
}

fun FragmentActivity.bindFingerprintEnrollFindRfpsView(
    view: GlifLayout,
    onSkipClickListener: View.OnClickListener,
) {
    GlifLayoutHelper(this, view).let {
        it.setHeaderText(
            R.string.security_settings_fingerprint_enroll_find_sensor_title
        )
        it.setDescriptionText(
            getText(R.string.security_settings_fingerprint_enroll_find_sensor_message)
        )
    }

    view.getMixin(FooterBarMixin::class.java).secondaryButton =
        FooterButton.Builder(this)
            .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
            .setButtonType(FooterButton.ButtonType.SKIP)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
            .build()
            .also {
                it.setOnClickListener(onSkipClickListener)
            }
}
