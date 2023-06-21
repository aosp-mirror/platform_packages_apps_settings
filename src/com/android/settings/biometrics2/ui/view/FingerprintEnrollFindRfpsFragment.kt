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
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintManager.ENROLL_FIND_SENSOR
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.settings.R
import com.android.settings.biometrics.fingerprint.FingerprintFindSensorAnimation
import com.android.settings.biometrics2.ui.model.EnrollmentProgress
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout

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

    private var findRfpsView: GlifLayout? = null

    private val onSkipClickListener =
        View.OnClickListener { _: View? -> viewModel.onSkipButtonClick() }

    private var animation: FingerprintFindSensorAnimation? = null

    @Surface.Rotation
    private var lastRotation = -1

    private val rotationObserver = Observer { rotation: Int? ->
        if (DEBUG) {
            Log.d(TAG, "rotationObserver $rotation")
        }
        rotation?.let { onRotationChanged(it) }
    }

    private val progressObserver: Observer<EnrollmentProgress> =
        Observer<EnrollmentProgress> { progress: EnrollmentProgress? ->
            if (DEBUG) {
                Log.d(TAG, "progressObserver($progress)")
            }
            if (progress != null && !progress.isInitialStep) {
                stopLookingForFingerprint(true)
            }
        }

    private val lastCancelMessageObserver: Observer<EnrollmentStatusMessage> =
        Observer<EnrollmentStatusMessage> { errorMessage: EnrollmentStatusMessage? ->
            if (DEBUG) {
                Log.d(TAG, "lastCancelMessageObserver($errorMessage)")
            }
            errorMessage?.let { onLastCancelMessage(it) }
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
    }

    override fun onStart() {
        super.onStart()
        if (DEBUG) {
            Log.d(
                TAG,
                "onStart(), start looking for fingerprint, animation exist:${animation != null}"
            )
        }
        startLookingForFingerprint()
    }

    override fun onResume() {
        val rotationLiveData: LiveData<Int> = rotationViewModel.liveData
        lastRotation = rotationLiveData.value!!
        rotationLiveData.observe(this, rotationObserver)
        animation?.let {
            if (DEBUG) {
                Log.d(TAG, "onResume(), start animation")
            }
            it.startAnimation()
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
        val isEnrolling: Boolean = progressViewModel.isEnrolling
        if (DEBUG) {
            Log.d(
                TAG,
                "onStop(), current enrolling: ${isEnrolling}, animation exist:${animation != null}"
            )
        }
        if (isEnrolling) {
            stopLookingForFingerprint(false)
        }
    }

    private fun startLookingForFingerprint() {
        if (progressViewModel.isEnrolling) {
            Log.d(
                TAG,
                "startLookingForFingerprint(), failed because isEnrolling is true before starting"
            )
            return
        }
        val startResult: Boolean = progressViewModel.startEnrollment(ENROLL_FIND_SENSOR)
        if (!startResult) {
            Log.e(TAG, "startLookingForFingerprint(), failed to start enrollment")
        }
        progressViewModel.progressLiveData.observe(this, progressObserver)
    }

    private fun stopLookingForFingerprint(waitForLastCancelErrMsg: Boolean) {
        if (!progressViewModel.isEnrolling) {
            Log.d(
                TAG,
                "stopLookingForFingerprint(), failed because isEnrolling is false before stopping"
            )
            return
        }
        if (waitForLastCancelErrMsg) {
            progressViewModel.clearErrorMessageLiveData() // Prevent got previous error message
            progressViewModel.errorMessageLiveData.observe(this, lastCancelMessageObserver)
        }
        progressViewModel.progressLiveData.removeObserver(progressObserver)
        val cancelResult: Boolean = progressViewModel.cancelEnrollment()
        if (!cancelResult) {
            Log.e(TAG, "stopLookingForFingerprint(), failed to cancel enrollment")
        }
    }

    private fun onRotationChanged(@Surface.Rotation newRotation: Int) {
        if (DEBUG) {
            Log.d(TAG, "onRotationChanged() from $lastRotation to $newRotation")
        }
        if (newRotation % 2 != lastRotation % 2) {
            // Fragment is going to be recreated, just stopLookingForFingerprint() here.
            stopLookingForFingerprint(true)
        }
    }

    private fun onLastCancelMessage(errorMessage: EnrollmentStatusMessage) {
        if (errorMessage.msgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
            val progress: EnrollmentProgress? = progressViewModel.progressLiveData.value
            progressViewModel.clearProgressLiveData()
            progressViewModel.errorMessageLiveData.removeObserver(lastCancelMessageObserver)
            if (progress != null && !progress.isInitialStep) {
                viewModel.onStartButtonClick()
            }
        } else {
            Log.e(TAG, "errorMessageObserver($errorMessage)")
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
            .setTheme(R.style.SudGlifButton_Secondary)
            .build()
            .also {
                it.setOnClickListener(onSkipClickListener)
            }
}
