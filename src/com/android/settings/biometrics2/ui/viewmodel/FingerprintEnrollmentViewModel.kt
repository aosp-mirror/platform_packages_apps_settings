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
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.AndroidViewModel
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics.fingerprint.FingerprintEnrollFinish.FINGERPRINT_SUGGESTION_ACTIVITY
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollIntroduction
import com.android.settings.biometrics2.data.repository.FingerprintRepository
import com.android.settings.biometrics2.ui.model.EnrollmentRequest
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Fingerprint enrollment view model implementation
 */
class FingerprintEnrollmentViewModel(
    application: Application,
    private val fingerprintRepository: FingerprintRepository,
    val request: EnrollmentRequest
) : AndroidViewModel(application) {

    val isWaitingActivityResult: AtomicBoolean = atomic(false)

    private val _setResultFlow = MutableSharedFlow<ActivityResult>()
    val setResultFlow: SharedFlow<ActivityResult>
        get() = _setResultFlow.asSharedFlow()

    var isNewFingerprintAdded = false
        set(value) {
            // Only allow changing this value from false to true
            if (!field) {
                field = value
            }
        }

    /**
     * Get override activity result as current ViewModel status.
     *
     * FingerprintEnrollmentActivity supports user enrolls 2nd fingerprint or starts a new flow
     * through Deferred-SUW, Portal-SUW, or SUW Suggestion. Use a method to get override activity
     * result instead of putting these if-else on every setResult(), .
     */
    fun getOverrideActivityResult(
        result: ActivityResult,
        generatingChallengeExtras: Bundle?
    ): ActivityResult {
        val newResultCode = if (isNewFingerprintAdded)
            BiometricEnrollBase.RESULT_FINISHED
        else if (request.isAfterSuwOrSuwSuggestedAction)
            BiometricEnrollBase.RESULT_CANCELED
        else
            result.resultCode

        var newData = result.data
        if (newResultCode == BiometricEnrollBase.RESULT_FINISHED
            && generatingChallengeExtras != null
        ) {
            if (newData == null) {
                newData = Intent()
            }
            newData.putExtras(generatingChallengeExtras)
        }
        return ActivityResult(newResultCode, newData)
    }

    /**
     * Activity calls this method during onPause() to finish itself when back to background.
     *
     * @param isActivityFinishing Activity has called finish() or not
     * @param isChangingConfigurations Activity is finished because of configuration changed or not.
     */
    fun checkFinishActivityDuringOnPause(
        isActivityFinishing: Boolean,
        isChangingConfigurations: Boolean,
        scope: CoroutineScope
    ) {
        if (isChangingConfigurations || isActivityFinishing || request.isSuw
            || isWaitingActivityResult.value
        ) {
            return
        }
        scope.launch {
            _setResultFlow.emit(ActivityResult(BiometricEnrollBase.RESULT_TIMEOUT, null))
        }
    }

    /**
     * Get Suw fingerprint count extra for statistics
     */
    fun getSuwFingerprintCountExtra(userId: Int) = Bundle().also {
        it.putInt(
            SetupFingerprintEnrollIntroduction.EXTRA_FINGERPRINT_ENROLLED_COUNT,
            fingerprintRepository.getNumOfEnrolledFingerprintsSize(userId)
        )
    }

    /**
     * Gets the result about fingerprint enrollable
     */
    fun isMaxEnrolledReached(userId: Int): Boolean = with(fingerprintRepository) {
        maxFingerprints <= getNumOfEnrolledFingerprintsSize(userId)
    }

    val canAssumeUdfps: Boolean
        get() = fingerprintRepository.canAssumeUdfps()

    val canAssumeSfps: Boolean
        get() = fingerprintRepository.canAssumeSfps()

    /**
     * Update FINGERPRINT_SUGGESTION_ACTIVITY into package manager
     */
    fun updateFingerprintSuggestionEnableState(userId: Int) {
        // Only show "Add another fingerprint" if the user already enrolled one.
        // "Add fingerprint" will be shown in the main flow if the user hasn't enrolled any
        // fingerprints. If the user already added more than one fingerprint, they already know
        // to add multiple fingerprints so we don't show the suggestion.
        val state = if (fingerprintRepository.getNumOfEnrolledFingerprintsSize(userId) == 1)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        getApplication<Application>().packageManager.setComponentEnabledSetting(
            ComponentName(
                getApplication(),
                FINGERPRINT_SUGGESTION_ACTIVITY
            ),
            state,
            PackageManager.DONT_KILL_APP
        )
        Log.d(TAG, "$FINGERPRINT_SUGGESTION_ACTIVITY enabled state: $state")
    }

    companion object {
        private const val TAG = "FingerprintEnrollmentViewModel"
    }
}
