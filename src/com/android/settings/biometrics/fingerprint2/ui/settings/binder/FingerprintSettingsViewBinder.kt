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

package com.android.settings.biometrics.fingerprint2.ui.settings.binder

import android.hardware.fingerprint.FingerprintManager
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import com.android.settings.biometrics.fingerprint2.ui.settings.binder.FingerprintSettingsViewBinder.FingerprintView
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.EnrollAdditionalFingerprint
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.EnrollFirstFingerprint
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FingerprintSettingsNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FingerprintSettingsViewModel
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FinishSettings
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FinishSettingsWithResult
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.LaunchConfirmDeviceCredential
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.LaunchedActivity
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.PreferenceViewModel
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.ShowSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val TAG = "FingerprintSettingsViewBinder"

/** Binds a [FingerprintSettingsViewModel] to a [FingerprintView] */
object FingerprintSettingsViewBinder {

  interface FingerprintView {
    /**
     * Helper function to launch fingerprint enrollment(This should be the default behavior when a
     * user enters their PIN/PATTERN/PASS and no fingerprints are enrolled).
     */
    fun launchFullFingerprintEnrollment(
      userId: Int,
      gateKeeperPasswordHandle: Long?,
      challenge: Long?,
      challengeToken: ByteArray?,
    )
    /** Helper to launch an add fingerprint request */
    fun launchAddFingerprint(userId: Int, challengeToken: ByteArray?)
    /**
     * Helper function that will try and launch confirm lock, if that fails we will prompt user to
     * choose a PIN/PATTERN/PASS.
     */
    fun launchConfirmOrChooseLock(userId: Int)
    /** Used to indicate that FingerprintSettings is finished. */
    fun finish()
    /** Indicates what result should be set for the returning callee */
    fun setResultExternal(resultCode: Int)
    /** Indicates the settings UI should be shown */
    fun showSettings(enrolledFingerprints: List<FingerprintData>)
    /** Updates the add fingerprints preference */
    fun updateAddFingerprintsPreference(canEnroll: Boolean, maxFingerprints: Int)
    /** Updates the sfps fingerprints preference */
    fun updateSfpsPreference(isSfpsPrefVisible: Boolean)
    /** Indicates that a user has been locked out */
    fun userLockout(authAttemptViewModel: FingerprintAuthAttemptModel.Error)
    /** Indicates a fingerprint preference should be highlighted */
    suspend fun highlightPref(fingerId: Int)
    /** Indicates a user should be prompted to delete a fingerprint */
    suspend fun askUserToDeleteDialog(fingerprintViewModel: FingerprintData): Boolean
    /** Indicates a user should be asked to renae ma dialog */
    suspend fun askUserToRenameDialog(
      fingerprintViewModel: FingerprintData
    ): Pair<FingerprintData, String>?
  }

  fun bind(
    view: FingerprintView,
    viewModel: FingerprintSettingsViewModel,
    navigationViewModel: FingerprintSettingsNavigationViewModel,
    lifecycleScope: LifecycleCoroutineScope,
  ) {

    /** Result listener for launching enrollments **after** a user has reached the settings page. */

    // Settings display flow
    lifecycleScope.launch { viewModel.enrolledFingerprints.collect { view.showSettings(it) } }
    lifecycleScope.launch {
      viewModel.addFingerprintPrefInfo.collect { (enablePref, maxFingerprints) ->
        view.updateAddFingerprintsPreference(enablePref, maxFingerprints)
      }
    }
    lifecycleScope.launch { viewModel.isSfpsPrefVisible.collect { view.updateSfpsPreference(it) } }

    // Dialog flow
    lifecycleScope.launch {
      viewModel.isShowingDialog.collectLatest {
        if (it == null) {
          return@collectLatest
        }
        when (it) {
          is PreferenceViewModel.RenameDialog -> {
            val willRename = view.askUserToRenameDialog(it.fingerprintViewModel)
            if (willRename != null) {
              Log.d(TAG, "renaming fingerprint $it")
              viewModel.renameFingerprint(willRename.first, willRename.second)
            }
            viewModel.onRenameDialogFinished()
          }
          is PreferenceViewModel.DeleteDialog -> {
            if (view.askUserToDeleteDialog(it.fingerprintViewModel)) {
              Log.d(TAG, "deleting fingerprint $it")
              viewModel.deleteFingerprint(it.fingerprintViewModel)
            }
            viewModel.onDeleteDialogFinished()
          }
        }
      }
    }

    // Auth flow
    lifecycleScope.launch {
      viewModel.authFlow.filterNotNull().collect {
        when (it) {
          is FingerprintAuthAttemptModel.Success -> {
            view.highlightPref(it.fingerId)
          }
          is FingerprintAuthAttemptModel.Error -> {
            if (it.error == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
              view.userLockout(it)
            }
          }
        }
      }
    }

    // Launch this on Dispatchers.Default and not main.
    // Otherwise it takes too long for state transitions such as PIN/PATTERN/PASS
    // to enrollment, which makes gives the user a janky experience.
    lifecycleScope.launch(Dispatchers.Default) {
      var settingsShowingJob: Job? = null
      navigationViewModel.nextStep.filterNotNull().collect { nextStep ->
        settingsShowingJob?.cancel()
        settingsShowingJob = null
        Log.d(TAG, "next step = $nextStep")
        when (nextStep) {
          is EnrollFirstFingerprint ->
            view.launchFullFingerprintEnrollment(
              nextStep.userId,
              nextStep.gateKeeperPasswordHandle,
              nextStep.challenge,
              nextStep.challengeToken,
            )
          is EnrollAdditionalFingerprint ->
            view.launchAddFingerprint(nextStep.userId, nextStep.challengeToken)
          is LaunchConfirmDeviceCredential -> view.launchConfirmOrChooseLock(nextStep.userId)
          is FinishSettings -> {
            Log.d(TAG, "Finishing due to ${nextStep.reason}")
            view.finish()
          }
          is FinishSettingsWithResult -> {
            Log.d(TAG, "Finishing with result ${nextStep.result} due to ${nextStep.reason}")
            view.setResultExternal(nextStep.result)
            view.finish()
          }
          is ShowSettings -> Log.d(TAG, "Showing settings")
          is LaunchedActivity -> Log.d(TAG, "Launched activity, awaiting result")
        }
      }
    }
  }
}
