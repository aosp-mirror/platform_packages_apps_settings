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
package com.android.settings.biometrics2.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.android.settings.biometrics2.data.repository.FingerprintRepository
import com.android.settings.biometrics2.ui.model.EnrollmentRequest
import com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus
import com.android.settings.biometrics2.ui.model.FingerprintEnrollable
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroAction.CONTINUE_ENROLL
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroAction.DONE_AND_FINISH
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroAction.SKIP_OR_CANCEL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Fingerprint intro onboarding page view model implementation */
class FingerprintEnrollIntroViewModel(
    application: Application,
    private val fingerprintRepository: FingerprintRepository,
    val request: EnrollmentRequest,
    private val userId: Int
) : AndroidViewModel(application) {

    /** User's action flow (like clicking Agree, Skip, or Done) */
    private val _actionFlow = MutableSharedFlow<FingerprintEnrollIntroAction>()
    val actionFlow: SharedFlow<FingerprintEnrollIntroAction>
        get() = _actionFlow.asSharedFlow()

    private fun getEnrollableStatus(): FingerprintEnrollable {
        val num = fingerprintRepository.getNumOfEnrolledFingerprintsSize(userId)
        val max =
            if (request.isSuw && !request.isAfterSuwOrSuwSuggestedAction)
                fingerprintRepository.getMaxFingerprintsInSuw(
                    getApplication<Application>().resources
                )
            else
                fingerprintRepository.maxFingerprints
        return if (num >= max)
            FingerprintEnrollable.FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
        else
            FingerprintEnrollable.FINGERPRINT_ENROLLABLE_OK
    }

    private val hasScrolledToBottomFlow = MutableStateFlow(HAS_SCROLLED_TO_BOTTOM_DEFAULT)
    private val enrollableStatusFlow = MutableStateFlow(getEnrollableStatus())

    /** Enrollable status and hasScrollToBottom live data */
    val pageStatusFlow: Flow<FingerprintEnrollIntroStatus> =
        hasScrolledToBottomFlow.combine(enrollableStatusFlow) {
            hasScrolledToBottom: Boolean, enrollableStatus: FingerprintEnrollable ->
            FingerprintEnrollIntroStatus(hasScrolledToBottom, enrollableStatus)
        }

    fun updateEnrollableStatus(scope: CoroutineScope) {
        scope.launch {
            enrollableStatusFlow.emit(getEnrollableStatus())
        }
    }

    /** The first sensor type is UDFPS sensor or not */
    val canAssumeUdfps: Boolean
        get() = fingerprintRepository.canAssumeUdfps()

    /** Update onboarding intro page has scrolled to bottom */
    fun setHasScrolledToBottom(value: Boolean, scope: CoroutineScope) {
        scope.launch {
            hasScrolledToBottomFlow.emit(value)
        }
    }

    /** Get parental consent required or not during enrollment process */
    val isParentalConsentRequired: Boolean
        get() = fingerprintRepository.isParentalConsentRequired(getApplication())

    /** Get fingerprint is disable by admin or not */
    val isBiometricUnlockDisabledByAdmin: Boolean
        get() = fingerprintRepository.isDisabledByAdmin(getApplication(), userId)

    /**
     * User clicks next button
     */
    fun onNextButtonClick(scope: CoroutineScope) {
        scope.launch {
            when (val status = enrollableStatusFlow.value) {
                FingerprintEnrollable.FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX ->
                    _actionFlow.emit(DONE_AND_FINISH)

                FingerprintEnrollable.FINGERPRINT_ENROLLABLE_OK ->
                    _actionFlow.emit(CONTINUE_ENROLL)

                else -> Log.w(TAG, "fail to click next, enrolled:$status")
            }
        }
    }

    /** User clicks skip/cancel button */
    fun onSkipOrCancelButtonClick(scope: CoroutineScope) {
        scope.launch {
            _actionFlow.emit(SKIP_OR_CANCEL)
        }
    }

    companion object {
        private const val TAG = "FingerprintEnrollIntroViewModel"
        private const val HAS_SCROLLED_TO_BOTTOM_DEFAULT = false
        private val ENROLLABLE_STATUS_DEFAULT = FingerprintEnrollable.FINGERPRINT_ENROLLABLE_UNKNOWN
    }
}

enum class FingerprintEnrollIntroAction {
    /** User clicks 'Done' button on this page */
    DONE_AND_FINISH,
    /** User clicks 'Agree' button on this page */
    CONTINUE_ENROLL,
    /** User clicks 'Skip' button on this page */
    SKIP_OR_CANCEL
}
