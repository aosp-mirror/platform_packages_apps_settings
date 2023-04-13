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

package com.android.settings.biometrics.fingerprint2.ui.fragment

import android.app.Activity
import android.app.settings.SettingsEnums
import android.content.Context.FINGERPRINT_SERVICE
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.util.FeatureFlagUtils
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroductionInternal
import com.android.settings.biometrics.fingerprint2.ui.binder.FingerprintViewBinder
import com.android.settings.biometrics.fingerprint2.ui.viewmodel.FingerprintSettingsViewModel
import com.android.settings.core.SettingsBaseActivity
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.settingslib.transition.SettingsTransitionHelper

const val TAG = "FingerprintSettingsV2Fragment"

/**
 * A class responsible for showing FingerprintSettings. Typical activity Flows are
 * 1. Settings > FingerprintSettings > PIN/PATTERN/PASS -> FingerprintSettings
 * 2. FingerprintSettings -> FingerprintEnrollment fow
 *
 * This page typically allows for
 * 1. Fingerprint deletion
 * 2. Fingerprint enrollment
 * 3. Renaming a fingerprint
 * 4. Enabling/Disabling a feature
 */
class FingerprintSettingsV2Fragment : DashboardFragment() {
    private lateinit var binding: FingerprintViewBinder.Binding

    private val launchFirstEnrollmentListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val resultCode = result.resultCode
            val data = result.data

            Log.d(
                TAG, "onEnrollFirstFingerprint($resultCode, $data)"
            )
            if (resultCode != BiometricEnrollBase.RESULT_FINISHED || data == null) {
                if (resultCode == BiometricEnrollBase.RESULT_TIMEOUT) {
                    binding.onEnrollFirstFailure(
                        "Received RESULT_TIMEOUT when enrolling", resultCode
                    )
                } else {
                    binding.onEnrollFirstFailure("Incorrect resultCode or data was null")
                }
            } else {
                val token =
                    data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN)
                val keyChallenge = data.getExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE) as Long?
                binding.onEnrollFirst(token, keyChallenge)
            }
        }

    /** Result listener for launching enrollments **after** a user has reached the settings page. */
    private val launchAdditionalFingerprintListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultCode = result.resultCode
            Log.d(
                TAG, "onEnrollAdditionalFingerprint($resultCode)"
            )

            if (resultCode == BiometricEnrollBase.RESULT_TIMEOUT) {
                binding.onEnrollAdditionalFailure()
            } else {
                binding.onEnrollSuccess()
            }
        }

    /** Result listener for ChooseLock activity flow. */
    private val confirmDeviceResultListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultCode = result.resultCode
            val data = result.data
            onConfirmDevice(resultCode, data)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // This is needed to support ChooseLockSettingBuilder...show(). All other activity
        // calls should use the registerForActivity method call.
        super.onActivityResult(requestCode, resultCode, data)
        val wasSuccessful =
            resultCode == BiometricEnrollBase.RESULT_FINISHED || resultCode == Activity.RESULT_OK
        val gateKeeperPasswordHandle =
            data?.getExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE) as Long?
        binding.onConfirmDevice(wasSuccessful, gateKeeperPasswordHandle)
    }


    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        if (!FeatureFlagUtils.isEnabled(
                context, FeatureFlagUtils.SETTINGS_BIOMETRICS2_FINGERPRINT_SETTINGS
            )
        ) {
            Log.d(
                TAG, "Finishing due to feature not being enabled"
            )
            finish()
            return
        }
        val viewModel = ViewModelProvider(
            this, FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
                requireContext().applicationContext.userId, requireContext().getSystemService(
                    FINGERPRINT_SERVICE
                ) as FingerprintManager
            )
        )[FingerprintSettingsViewModel::class.java]

        val token = intent.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN)
        val challenge = intent.getLongExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, -1L)

        binding = FingerprintViewBinder.bind(
            viewModel,
            lifecycleScope,
            token,
            challenge,
            ::launchFullFingerprintEnrollment,
            ::launchAddFingerprint,
            ::launchConfirmOrChooseLock,
            ::finish,
            ::setResultExternal,
        )
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.FINGERPRINT
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.security_settings_fingerprint_limbo
    }

    override fun getLogTag(): String {
        return TAG
    }

    /**
     * Helper function that will try and launch confirm lock, if that fails we will prompt user
     * to choose a PIN/PATTERN/PASS.
     */
    private fun launchConfirmOrChooseLock(userId: Int) {
        val intent = Intent()
        val builder = ChooseLockSettingsHelper.Builder(requireActivity(), this)
        val launched = builder.setRequestCode(BiometricEnrollBase.CONFIRM_REQUEST)
            .setTitle(getString(R.string.security_settings_fingerprint_preference_title))
            .setRequestGatekeeperPasswordHandle(true).setUserId(userId).setForegroundOnly(true)
            .setReturnCredentials(true).show()
        if (!launched) {
            intent.setClassName(
                Utils.SETTINGS_PACKAGE_NAME, ChooseLockGeneric::class.java.name
            )
            intent.putExtra(
                ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS, true
            )
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true)
            intent.putExtra(Intent.EXTRA_USER_ID, userId)
            confirmDeviceResultListener.launch(intent)
        }
    }

    /**
     * Helper for confirming a PIN/PATTERN/PASS
     */
    private fun onConfirmDevice(resultCode: Int, data: Intent?) {
        val wasSuccessful =
            resultCode == BiometricEnrollBase.RESULT_FINISHED || resultCode == Activity.RESULT_OK
        val gateKeeperPasswordHandle =
            data?.getExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE) as Long?
        binding.onConfirmDevice(wasSuccessful, gateKeeperPasswordHandle)
    }

    /**
     * Helper function to launch fingerprint enrollment(This should be the default behavior
     * when a user enters their PIN/PATTERN/PASS and no fingerprints are enrolled.
     */
    private fun launchFullFingerprintEnrollment(
        userId: Int,
        gateKeeperPasswordHandle: Long?,
        challenge: Long?,
        challengeToken: ByteArray?,
    ) {
        val intent = Intent()
        intent.setClassName(
            Utils.SETTINGS_PACKAGE_NAME, FingerprintEnrollIntroductionInternal::class.java.name
        )
        intent.putExtra(BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY, true)
        intent.putExtra(
            SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
            SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE
        )

        intent.putExtra(Intent.EXTRA_USER_ID, userId)

        if (gateKeeperPasswordHandle != null) {
            intent.putExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gateKeeperPasswordHandle
            )
        } else {
            intent.putExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, challengeToken
            )
            intent.putExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, challenge)
        }
        launchFirstEnrollmentListener.launch(intent)
    }

    private fun setResultExternal(resultCode: Int) {
        setResult(resultCode)
    }

    /** Helper to launch an add fingerprint request */
    private fun launchAddFingerprint(userId: Int, challengeToken: ByteArray?) {
        val intent = Intent()
        intent.setClassName(
            Utils.SETTINGS_PACKAGE_NAME, FingerprintEnrollEnrolling::class.qualifiedName.toString()
        )
        intent.putExtra(Intent.EXTRA_USER_ID, userId)
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, challengeToken)
        launchAdditionalFingerprintListener.launch(intent)
    }

}