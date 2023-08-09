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

import android.annotation.StyleRes
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources.Theme
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics2.factory.BiometricsViewModelFactory
import com.android.settings.biometrics2.factory.BiometricsViewModelFactory.CHALLENGE_GENERATOR_KEY
import com.android.settings.biometrics2.factory.BiometricsViewModelFactory.CREDENTIAL_MODEL_KEY
import com.android.settings.biometrics2.factory.BiometricsViewModelFactory.ENROLLMENT_REQUEST_KEY
import com.android.settings.biometrics2.ui.model.CredentialModel
import com.android.settings.biometrics2.ui.model.EnrollmentRequest
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.FingerprintChallengeGenerator
import com.android.settings.biometrics2.ui.viewmodel.CredentialAction
import com.android.settings.biometrics2.ui.viewmodel.DeviceFoldedViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FingerprintEnrollEnrollingAction
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollErrorDialogViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FingerprintEnrollFindSensorAction
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel.FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel.FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel.FingerprintEnrollFinishAction
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroAction
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintErrorDialogSetResultAction.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH
import com.android.settings.biometrics2.ui.viewmodel.FingerprintErrorDialogSetResultAction.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.launch

/**
 * Fingerprint enrollment activity implementation
 */
open class FingerprintEnrollmentActivity : FragmentActivity() {
    /** SetupWizard activity*/
    class SetupActivity : FingerprintEnrollmentActivity()

    /** Internal activity for FingerprintSettings */
    class InternalActivity : FingerprintEnrollmentActivity()

    private val viewModelProvider: ViewModelProvider by lazy {
        ViewModelProvider(this)
    }

    private val viewModel: FingerprintEnrollmentViewModel by lazy {
        viewModelProvider[FingerprintEnrollmentViewModel::class.java]
    }

    private val autoCredentialViewModel: AutoCredentialViewModel by lazy {
        viewModelProvider[AutoCredentialViewModel::class.java]
    }

    private val introViewModel: FingerprintEnrollIntroViewModel by lazy {
        viewModelProvider[FingerprintEnrollIntroViewModel::class.java]
    }

    private val findSensorViewModel: FingerprintEnrollFindSensorViewModel by lazy {
        viewModelProvider[FingerprintEnrollFindSensorViewModel::class.java]
    }

    private val progressViewModel: FingerprintEnrollProgressViewModel by lazy {
        viewModelProvider[FingerprintEnrollProgressViewModel::class.java]
    }

    private val enrollingViewModel: FingerprintEnrollEnrollingViewModel by lazy {
        viewModelProvider[FingerprintEnrollEnrollingViewModel::class.java]
    }

    private val finishViewModel: FingerprintEnrollFinishViewModel by lazy {
        viewModelProvider[FingerprintEnrollFinishViewModel::class.java]
    }

    private val errorDialogViewModel: FingerprintEnrollErrorDialogViewModel by lazy {
        viewModelProvider[FingerprintEnrollErrorDialogViewModel::class.java]
    }

    private var isFirstFragmentAdded = false

    private val findSensorActionObserver = Observer<Int?> { action ->
        if (DEBUG) {
            Log.d(TAG, "findSensorActionObserver($action)")
        }
        action?.let { onFindSensorAction(it) }
    }

    private val enrollingActionObserver = Observer<Int?> { action ->
        if (DEBUG) {
            Log.d(TAG, "enrollingActionObserver($action)")
        }
        action?.let { onEnrollingAction(it) }
    }

    private val finishActionObserver = Observer<Int?> { action ->
        if (DEBUG) {
            Log.d(TAG, "finishActionObserver($action)")
        }
        action?.let { onFinishAction(it) }
    }

    private val chooseLockResultCallback: ActivityResultCallback<ActivityResult> =
        ActivityResultCallback { result ->
            onChooseOrConfirmLockResult(true /* isChooseLock */, result)
        }

    private val chooseLockLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult(), chooseLockResultCallback)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Theme
        setTheme(viewModel.request.theme)
        ThemeHelper.trySetDynamicColor(this)
        window.statusBarColor = Color.TRANSPARENT

        // fragment
        setContentView(R.layout.biometric_enrollment_container)
        val fragment: Fragment? = supportFragmentManager.findFragmentById(
            R.id.fragment_container_view
        )
        Log.d(
            TAG,
            "onCreate() has savedInstance:$(savedInstanceState != null), fragment:$fragment"
        )

        isFirstFragmentAdded = (savedInstanceState != null)
        if (fragment == null) {
            checkCredential()
            if (viewModel.request.isSkipFindSensor) {
                startEnrollingFragment()
            } else if (viewModel.request.isSkipIntro) {
                startFindSensorFragment()
            } else {
                startIntroFragment()
            }
        } else {
            val tag: String? = fragment.tag
            if (INTRO_TAG == tag) {
                attachIntroViewModel()
            } else if (FIND_SENSOR_TAG == tag) {
                attachFindSensorViewModel()
                attachIntroViewModel()
            } else if (ENROLLING_TAG == tag) {
                attachEnrollingViewModel()
                attachFindSensorViewModel()
                attachIntroViewModel()
            } else if (FINISH_TAG == tag) {
                attachFinishViewModel()
                attachFindSensorViewModel()
                attachIntroViewModel()
            } else {
                Log.e(TAG, "fragment tag $tag not found")
                finish()
                return
            }
        }

        collectFlows()
    }

    private fun collectFlows() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setResultFlow.collect {
                    Log.d(TAG, "setResultLiveData($it)")
                    onSetActivityResult(it)
                }
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                autoCredentialViewModel.generateChallengeFailedFlow.collect {
                    Log.d(TAG, "generateChallengeFailedFlow($it)")
                    onSetActivityResult(ActivityResult(RESULT_CANCELED, null))
                }
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorDialogViewModel.newDialogFlow.collect {
                    Log.d(TAG, "newErrorDialogFlow($it)")
                    FingerprintEnrollErrorDialog.newInstance(it).show(
                        supportFragmentManager,
                        ERROR_DIALOG_TAG
                    )
                }
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorDialogViewModel.setResultFlow.collect {
                    Log.d(TAG, "errorDialogSetResultFlow($it)")
                    when (it) {
                        FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH -> onSetActivityResult(
                            ActivityResult(BiometricEnrollBase.RESULT_FINISHED, null)
                        )

                        FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT -> onSetActivityResult(
                            ActivityResult(BiometricEnrollBase.RESULT_TIMEOUT, null)
                        )
                    }
                }
            }
        }
    }

    private fun startFragment(fragmentClass: Class<out Fragment>, tag: String) {
        if (!isFirstFragmentAdded) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container_view, fragmentClass, null, tag)
                .commit()
            isFirstFragmentAdded = true
        } else {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_open_enter_dynamic_color,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_open_exit,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_close_enter_dynamic_color,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_close_exit
                )
                .replace(R.id.fragment_container_view, fragmentClass, null, tag)
                .addToBackStack(tag)
                .commit()
        }
    }

    private fun startIntroFragment() {
        attachIntroViewModel()
        startFragment(FingerprintEnrollIntroFragment::class.java, INTRO_TAG)
    }

    private fun attachIntroViewModel() {
        val request: EnrollmentRequest = viewModel.request
        if (request.isSkipIntro || request.isSkipFindSensor) {
            return
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                introViewModel.actionFlow.collect(this@FingerprintEnrollmentActivity::onIntroAction)
            }
        }
    }

    // We need to make sure token is valid before entering find sensor page
    private fun startFindSensorFragment() {
        // Always setToken into progressViewModel even it is not necessary action for UDFPS
        progressViewModel.setToken(autoCredentialViewModel.token)
        attachFindSensorViewModel()
        val fragmentClass: Class<out Fragment> = if (viewModel.canAssumeUdfps) {
            FingerprintEnrollFindUdfpsFragment::class.java
        } else if (viewModel.canAssumeSfps) {
            FingerprintEnrollFindSfpsFragment::class.java
        } else {
            FingerprintEnrollFindRfpsFragment::class.java
        }
        startFragment(fragmentClass, FIND_SENSOR_TAG)
    }

    private fun attachFindSensorViewModel() {
        if (viewModel.request.isSkipFindSensor) {
            return
        }
        findSensorViewModel.let {
            // Clear ActionLiveData in FragmentViewModel to prevent getting previous action during
            // recreate, like press 'Start' then press 'back' in FingerprintEnrollEnrolling
            // activity.
            it.clearActionLiveData()
            it.actionLiveData.observe(this, findSensorActionObserver)
        }
    }

    private fun startEnrollingFragment() {
        // Always setToken into progressViewModel even it is not necessary action for SFPS or RFPS
        progressViewModel.setToken(autoCredentialViewModel.token)
        attachEnrollingViewModel()
        val fragmentClass: Class<out Fragment> = if (viewModel.canAssumeUdfps) {
            FingerprintEnrollEnrollingUdfpsFragment::class.java
        } else if (viewModel.canAssumeSfps) {
            FingerprintEnrollEnrollingSfpsFragment::class.java
        } else {
            FingerprintEnrollEnrollingRfpsFragment::class.java
        }
        startFragment(fragmentClass, ENROLLING_TAG)
    }

    private fun attachEnrollingViewModel() {
        enrollingViewModel.let {
            it.clearActionLiveData()
            it.actionLiveData.observe(this, enrollingActionObserver)
        }
    }

    private fun startFinishFragment() {
        viewModel.isNewFingerprintAdded = true
        attachFinishViewModel()
        if (viewModel.request.isSkipFindSensor) {
            // Set page to Finish
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_open_enter_dynamic_color,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_open_exit,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_close_enter_dynamic_color,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_close_exit
                )
                .replace(
                    R.id.fragment_container_view,
                    FingerprintEnrollFinishFragment::class.java,
                    null,
                    FINISH_TAG
                )
                .commit()
        } else {
            // Remove Enrolling page
            supportFragmentManager.popBackStack()

            // Remove old Finish page if any
            if (supportFragmentManager.findFragmentByTag(FINISH_TAG) != null) {
                supportFragmentManager.popBackStack(FINISH_TAG, POP_BACK_STACK_INCLUSIVE)
            }

            // Remove FindSensor page if maxEnrolled
            if (viewModel.isMaxEnrolledReached(autoCredentialViewModel.userId)
                && supportFragmentManager.findFragmentByTag(FIND_SENSOR_TAG) != null
            ) {
                supportFragmentManager.popBackStack(FIND_SENSOR_TAG, POP_BACK_STACK_INCLUSIVE)
            }

            // Add Finish page
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_open_enter_dynamic_color,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_open_exit,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_close_enter_dynamic_color,
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_close_exit
                )
                .replace(
                    R.id.fragment_container_view,
                    FingerprintEnrollFinishFragment::class.java,
                    null,
                    FINISH_TAG
                )
                .addToBackStack(FINISH_TAG)
                .commit()
        }
    }

    private fun attachFinishViewModel() {
        finishViewModel.let {
            it.clearActionLiveData()
            it.actionLiveData.observe(this, finishActionObserver)
        }
    }

    private fun onSetActivityResult(result: ActivityResult) {
        val challengeExtras: Bundle? = autoCredentialViewModel.createGeneratingChallengeExtras()
        val overrideResult: ActivityResult = viewModel.getOverrideActivityResult(
            result, challengeExtras
        )
        if (DEBUG) {
            Log.d(
                TAG, "onSetActivityResult(" + result + "), override:" + overrideResult
                        + ") challengeExtras:" + challengeExtras
            )
        }
        setResult(overrideResult.resultCode, overrideResult.data)
        finish()
    }

    private fun checkCredential() {
        when (autoCredentialViewModel.checkCredential(lifecycleScope)) {
            CredentialAction.FAIL_NEED_TO_CHOOSE_LOCK -> {
                val intent: Intent = autoCredentialViewModel.createChooseLockIntent(
                    this,
                    viewModel.request.isSuw,
                    viewModel.request.suwExtras
                )
                if (!viewModel.isWaitingActivityResult.compareAndSet(false, true)) {
                    Log.w(TAG, "chooseLock, fail to set isWaiting flag to true")
                }
                chooseLockLauncher.launch(intent)
                return
            }

            CredentialAction.FAIL_NEED_TO_CONFIRM_LOCK -> {
                val launched: Boolean = autoCredentialViewModel.createConfirmLockLauncher(
                    this,
                    LAUNCH_CONFIRM_LOCK_ACTIVITY,
                    getString(R.string.security_settings_fingerprint_preference_title)
                ).launch()
                if (!launched) {
                    // This shouldn't happen, as we should only end up at this step if a lock thingy
                    // is already set.
                    Log.e(TAG, "confirmLock, launched is true")
                    finish()
                } else if (!viewModel.isWaitingActivityResult.compareAndSet(false, true)) {
                    Log.w(TAG, "confirmLock, fail to set isWaiting flag to true")
                }
                return
            }

            CredentialAction.CREDENTIAL_VALID,
            CredentialAction.IS_GENERATING_CHALLENGE -> {}
        }
    }

    private fun onChooseOrConfirmLockResult(
        isChooseLock: Boolean,
        activityResult: ActivityResult
    ) {
        if (!viewModel.isWaitingActivityResult.compareAndSet(true, false)) {
            Log.w(TAG, "isChooseLock:$isChooseLock, fail to unset waiting flag")
        }
        if (!autoCredentialViewModel.generateChallengeAsCredentialActivityResult(
                isChooseLock,
                activityResult,
                lifecycleScope
            )
        ) {
            onSetActivityResult(activityResult)
        }
    }

    private fun onIntroAction(action: FingerprintEnrollIntroAction) {
        Log.d(TAG, "onIntroAction($action)")
        when (action) {
            FingerprintEnrollIntroAction.DONE_AND_FINISH -> {
                onSetActivityResult(ActivityResult(BiometricEnrollBase.RESULT_FINISHED, null))
                return
            }

            FingerprintEnrollIntroAction.SKIP_OR_CANCEL -> {
                onSetActivityResult(ActivityResult(BiometricEnrollBase.RESULT_SKIP, null))
                return
            }

            FingerprintEnrollIntroAction.CONTINUE_ENROLL -> {
                startFindSensorFragment()
            }
        }
    }

    private fun onFindSensorAction(@FingerprintEnrollFindSensorAction action: Int) {
        when (action) {
            FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP -> {
                onSetActivityResult(ActivityResult(BiometricEnrollBase.RESULT_SKIP, null))
                return
            }

            FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG -> {
                SkipSetupFindFpsDialog().show(
                    supportFragmentManager,
                    SKIP_SETUP_FIND_FPS_DIALOG_TAG
                )
                return
            }

            FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START -> {
                startEnrollingFragment()
            }
        }
    }

    private fun onEnrollingAction(@FingerprintEnrollEnrollingAction action: Int) {
        when (action) {
            FINGERPRINT_ENROLL_ENROLLING_ACTION_DONE -> {
                startFinishFragment()
            }

            FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_USER_SKIP -> {
                onSetActivityResult(ActivityResult(BiometricEnrollBase.RESULT_SKIP, null))
            }

            FINGERPRINT_ENROLL_ENROLLING_ACTION_SHOW_ICON_TOUCH_DIALOG -> {
                FingerprintEnrollEnrollingIconTouchDialog().show(
                    supportFragmentManager,
                    SKIP_SETUP_FIND_FPS_DIALOG_TAG
                )
            }

            FINGERPRINT_ENROLL_ENROLLING_CANCELED_BECAUSE_BACK_PRESSED -> {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    onSetActivityResult(ActivityResult(RESULT_CANCELED, null))
                }
            }
        }
    }

    private fun onFinishAction(@FingerprintEnrollFinishAction action: Int) {
        when (action) {
            FINGERPRINT_ENROLL_FINISH_ACTION_ADD_BUTTON_CLICK -> {
                startEnrollingFragment()
            }

            FINGERPRINT_ENROLL_FINISH_ACTION_NEXT_BUTTON_CLICK -> {
                val data: Intent? = if (viewModel.request.isSuw) {
                    Intent().also {
                        it.putExtras(
                            viewModel.getSuwFingerprintCountExtra(
                                autoCredentialViewModel.userId
                            )
                        )
                    }
                } else {
                    null
                }
                onSetActivityResult(ActivityResult(BiometricEnrollBase.RESULT_FINISHED, data))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.checkFinishActivityDuringOnPause(
            isFinishing,
            isChangingConfigurations,
            lifecycleScope
        )
    }

    override fun onDestroy() {
        viewModel.updateFingerprintSuggestionEnableState(autoCredentialViewModel.userId)
        super.onDestroy()
    }

    override fun onApplyThemeResource(theme: Theme, @StyleRes resid: Int, first: Boolean) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true)
        super.onApplyThemeResource(theme, resid, first)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LAUNCH_CONFIRM_LOCK_ACTIVITY) {
            onChooseOrConfirmLockResult(false, ActivityResult(resultCode, data))
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras(super.defaultViewModelCreationExtras).also {
            it[CHALLENGE_GENERATOR_KEY] = FingerprintChallengeGenerator(
                featureFactory.biometricsRepositoryProvider.getFingerprintRepository(application)!!
            )
            it[ENROLLMENT_REQUEST_KEY] =
                EnrollmentRequest(intent, applicationContext, this is SetupActivity)
            it[CREDENTIAL_MODEL_KEY] =
                CredentialModel(intent.extras, SystemClock.elapsedRealtimeClock())
        }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = BiometricsViewModelFactory()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.statusBarColor = backgroundColor
    }

    @get:ColorInt
    private val backgroundColor: Int
        get() {
            val stateList: ColorStateList? =
                Utils.getColorAttr(this, android.R.attr.windowBackground)
            return stateList?.defaultColor ?: Color.TRANSPARENT
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        viewModelProvider[DeviceFoldedViewModel::class.java].onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "FingerprintEnrollmentActivity"
        protected const val LAUNCH_CONFIRM_LOCK_ACTIVITY = 1

        private const val INTRO_TAG = "intro"
        private const val FIND_SENSOR_TAG = "find-sensor"
        private const val ENROLLING_TAG = "enrolling"
        private const val FINISH_TAG = "finish"
        private const val SKIP_SETUP_FIND_FPS_DIALOG_TAG = "skip-setup-dialog"
        private const val ERROR_DIALOG_TAG = "error-dialog"
    }
}
