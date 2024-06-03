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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.activity

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.hardware.fingerprint.FingerprintEnrollOptions
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.util.TypedValue
import android.view.accessibility.AccessibilityManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.SetupWizardUtils
import com.android.settings.Utils.SETTINGS_PACKAGE_NAME
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics.BiometricEnrollBase.CONFIRM_REQUEST
import com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED
import com.android.settings.biometrics.BiometricUtils
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.fingerprint2.data.repository.DebuggingRepositoryImpl
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSensorRepositoryImpl
import com.android.settings.biometrics.fingerprint2.data.repository.UdfpsEnrollDebugRepositoryImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.DebuggingInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.DisplayDensityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.DisplayDensityInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrollStageInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrollStageInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintEnrollInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.FoldStateInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FoldStateInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.UdfpsEnrollInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.UdfpsEnrollInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.VibrationInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.VibrationInteractorImpl
import com.android.settings.biometrics.fingerprint2.lib.model.Default
import com.android.settings.biometrics.fingerprint2.lib.model.Settings
import com.android.settings.biometrics.fingerprint2.lib.model.SetupWizard
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollConfirmationV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollEnrollingV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollIntroV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.education.RfpsEnrollFindSensorFragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.education.SfpsEnrollFindSensorFragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.education.UdfpsEnrollFindSensorFragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.common.util.toFingerprintEnrollOptions
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.fragment.RFPSEnrollFragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel.RFPSViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.fragment.UdfpsEnrollFragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsLastStepViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.BackgroundViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintAction
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollConfirmationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollIntroViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintFlowViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintGatekeeperViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.ConfirmDeviceCredential
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Confirmation
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Education
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Enrollment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Init
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Introduction
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.TransitionStep
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintScrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.GatekeeperInfo
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.Transition
import com.android.settings.flags.Flags
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE
import com.android.settingslib.display.DisplayDensityUtils
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val TAG = "FingerprintEnrollmentV2Activity"

/**
 * This is the activity that controls the entire Fingerprint Enrollment experience through its
 * children fragments.
 */
class FingerprintEnrollmentV2Activity : FragmentActivity() {
  private lateinit var fingerprintEnrollEnrollingViewModel: FingerprintEnrollEnrollingViewModel
  private lateinit var navigationViewModel: FingerprintNavigationViewModel
  private lateinit var gatekeeperViewModel: FingerprintGatekeeperViewModel
  private lateinit var fingerprintEnrollViewModel: FingerprintEnrollViewModel
  private lateinit var vibrationInteractor: VibrationInteractor
  private lateinit var foldStateInteractor: FoldStateInteractor
  private lateinit var orientationInteractor: OrientationInteractor
  private lateinit var displayDensityInteractor: DisplayDensityInteractor
  private lateinit var udfpsEnrollInteractor: UdfpsEnrollInteractor
  private lateinit var fingerprintScrollViewModel: FingerprintScrollViewModel
  private lateinit var backgroundViewModel: BackgroundViewModel
  private lateinit var fingerprintFlowViewModel: FingerprintFlowViewModel
  private lateinit var fingerprintEnrollConfirmationViewModel:
    FingerprintEnrollConfirmationViewModel
  private lateinit var udfpsLastStepViewModel: UdfpsLastStepViewModel
  private lateinit var udfpsViewModel: UdfpsViewModel
  private lateinit var enrollStageInteractor: EnrollStageInteractor
  private val coroutineDispatcher = Dispatchers.Default

  /** Result listener for ChooseLock activity flow. */
  private val confirmDeviceResultListener =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val resultCode = result.resultCode
      val data = result.data
      onConfirmDevice(resultCode, data)
    }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == CONFIRM_REQUEST) {
      onConfirmDevice(resultCode, data)
    }
  }

  override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations) {
      backgroundViewModel.wentToBackground()
    }
  }

  override fun onResume() {
    super.onResume()
    backgroundViewModel.inForeground()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    foldStateInteractor.onConfigurationChange(newConfig)
    val displayDensityUtils = DisplayDensityUtils(applicationContext)
    val currIndex = displayDensityUtils.currentIndexForDefaultDisplay
    displayDensityInteractor.updateFontScale(resources.configuration.fontScale)
    displayDensityInteractor.updateDisplayDensity(
      displayDensityUtils.defaultDisplayDensityValues[currIndex]
    )
  }

  private fun onConfirmDevice(resultCode: Int, data: Intent?) {
    val wasSuccessful = resultCode == RESULT_FINISHED || resultCode == Activity.RESULT_OK
    val gateKeeperPasswordHandle = data?.getExtra(EXTRA_KEY_GK_PW_HANDLE) as Long?

    lifecycleScope.launch {
      val confirmDeviceResult =
        if (wasSuccessful) {
          FingerprintAction.CONFIRM_DEVICE_SUCCESS
        } else {
          FingerprintAction.CONFIRM_DEVICE_FAIL
        }
      gatekeeperViewModel.onConfirmDevice(wasSuccessful, gateKeeperPasswordHandle)
      navigationViewModel.update(
        confirmDeviceResult,
        ConfirmDeviceCredential::class,
        "$TAG#onConfirmDevice",
      )
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // TODO(b/299573056): Show split screen dialog when it's in multi window mode.
    setContentView(R.layout.fingerprint_v2_enroll_main)

    if (!Flags.fingerprintV2Enrollment()) {
      check(false) {
        "fingerprint enrollment v2 is not enabled, " +
          "please run adb shell device_config put " +
          "biometrics_framework com.android.settings.flags.fingerprint_v2_enrollment true"
      }
      finish()
    }

    setTheme(SetupWizardUtils.getTheme(applicationContext, intent))
    ThemeHelper.trySetDynamicColor(applicationContext)

    val backgroundDispatcher = Dispatchers.IO

    val context = applicationContext
    val fingerprintManager = context.getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
    val isAnySuw = WizardManagerHelper.isAnySetupWizard(intent)
    val enrollType =
      if (isAnySuw) {
        SetupWizard
      } else {
        Default
      }

    backgroundViewModel =
      ViewModelProvider(this, BackgroundViewModel.BackgroundViewModelFactory())[
        BackgroundViewModel::class.java]

    fingerprintFlowViewModel =
      ViewModelProvider(this, FingerprintFlowViewModel.FingerprintFlowViewModelFactory(enrollType))[
        FingerprintFlowViewModel::class.java]
    val displayDensityUtils = DisplayDensityUtils(context)
    val currIndex = displayDensityUtils.currentIndexForDefaultDisplay
    val defaultDisplayDensity = displayDensityUtils.defaultDensityForDefaultDisplay
    displayDensityInteractor =
      DisplayDensityInteractorImpl(
        resources.configuration.fontScale,
        displayDensityUtils.defaultDisplayDensityValues[currIndex],
        defaultDisplayDensity,
        lifecycleScope,
      )

    val debuggingRepo = DebuggingRepositoryImpl()
    val debuggingInteractor = DebuggingInteractorImpl(debuggingRepo)
    val udfpsEnrollDebugRepositoryImpl = UdfpsEnrollDebugRepositoryImpl()

    val fingerprintSensorRepo =
      if (debuggingRepo.isUdfpsEnrollmentDebuggingEnabled()) udfpsEnrollDebugRepositoryImpl
      else FingerprintSensorRepositoryImpl(fingerprintManager, backgroundDispatcher, lifecycleScope)

    if (intent.getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1) === -1) {
      val isSuw: Boolean = WizardManagerHelper.isAnySetupWizard(intent)
      intent.putExtra(
        BiometricUtils.EXTRA_ENROLL_REASON,
        if (isSuw) FingerprintEnrollOptions.ENROLL_REASON_SUW
        else FingerprintEnrollOptions.ENROLL_REASON_SETTINGS,
      )
    }

    val fingerprintEnrollStateRepository =
      if (debuggingRepo.isUdfpsEnrollmentDebuggingEnabled()) udfpsEnrollDebugRepositoryImpl
      else
        FingerprintEnrollInteractorImpl(
          context.applicationContext,
          intent.toFingerprintEnrollOptions(),
          fingerprintManager,
          Settings,
        )
    val accessibilityInteractor =
      AccessibilityInteractorImpl(
        getSystemService(AccessibilityManager::class.java)!!,
        lifecycleScope,
      )

    val pixelsPerMillimeter =
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1f, context.resources.displayMetrics)
    udfpsEnrollInteractor = UdfpsEnrollInteractorImpl(pixelsPerMillimeter, accessibilityInteractor)

    val fingerprintManagerInteractor =
      FingerprintManagerInteractorImpl(
        context,
        backgroundDispatcher,
        fingerprintManager,
        fingerprintSensorRepo,
        GatekeeperPasswordProvider(LockPatternUtils(context)),
        fingerprintEnrollStateRepository,
      )

    var challenge = intent.getExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE) as Long?
    val token = intent.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN)
    val gatekeeperInfo = FingerprintGatekeeperViewModel.toGateKeeperInfo(challenge, token)

    val hasConfirmedDeviceCredential = gatekeeperInfo is GatekeeperInfo.GatekeeperPasswordInfo

    navigationViewModel =
      ViewModelProvider(
        this,
        FingerprintNavigationViewModel.FingerprintNavigationViewModelFactory(
          Init,
          hasConfirmedDeviceCredential,
          fingerprintFlowViewModel,
          fingerprintManagerInteractor,
        ),
      )[FingerprintNavigationViewModel::class.java]
    // Initialize FingerprintEnrollIntroViewModel
    ViewModelProvider(
      this,
      FingerprintEnrollIntroViewModel.FingerprintEnrollIntoViewModelFactory(
        navigationViewModel,
        fingerprintFlowViewModel,
        fingerprintManagerInteractor,
      ),
    )[FingerprintEnrollIntroViewModel::class.java]

    gatekeeperViewModel =
      ViewModelProvider(
        this,
        FingerprintGatekeeperViewModel.FingerprintGatekeeperViewModelFactory(
          gatekeeperInfo,
          fingerprintManagerInteractor,
        ),
      )[FingerprintGatekeeperViewModel::class.java]

    // Initialize FoldStateViewModel
    foldStateInteractor = FoldStateInteractorImpl(context)
    foldStateInteractor.onConfigurationChange(resources.configuration)

    orientationInteractor = OrientationInteractorImpl(context)
    vibrationInteractor =
      VibrationInteractorImpl(context.getSystemService(Vibrator::class.java)!!, context)

    // Initialize FingerprintViewModel
    fingerprintEnrollViewModel =
      ViewModelProvider(
        this,
        FingerprintEnrollViewModel.FingerprintEnrollViewModelFactory(
          fingerprintManagerInteractor,
          gatekeeperViewModel,
          navigationViewModel,
        ),
      )[FingerprintEnrollViewModel::class.java]

    // Initialize scroll view model
    fingerprintScrollViewModel =
      ViewModelProvider(this, FingerprintScrollViewModel.FingerprintScrollViewModelFactory())[
        FingerprintScrollViewModel::class.java]

    // Initialize FingerprintEnrollEnrollingViewModel
    fingerprintEnrollEnrollingViewModel =
      ViewModelProvider(
        this,
        FingerprintEnrollEnrollingViewModel.FingerprintEnrollEnrollingViewModelFactory(
          fingerprintEnrollViewModel,
          backgroundViewModel,
        ),
      )[FingerprintEnrollEnrollingViewModel::class.java]

    // Initialize FingerprintEnrollFindSensorViewModel
    ViewModelProvider(
      this,
      FingerprintEnrollFindSensorViewModel.FingerprintEnrollFindSensorViewModelFactory(
        navigationViewModel,
        fingerprintEnrollViewModel,
        gatekeeperViewModel,
        backgroundViewModel,
        accessibilityInteractor,
        foldStateInteractor,
        orientationInteractor,
        fingerprintFlowViewModel,
        fingerprintManagerInteractor,
      ),
    )[FingerprintEnrollFindSensorViewModel::class.java]

    // Initialize RFPS View Model
    ViewModelProvider(
      this,
      RFPSViewModel.RFPSViewModelFactory(
        fingerprintEnrollEnrollingViewModel,
        navigationViewModel,
        orientationInteractor,
        fingerprintManagerInteractor,
      ),
    )[RFPSViewModel::class.java]

    enrollStageInteractor = EnrollStageInteractorImpl()

    udfpsLastStepViewModel =
      UdfpsLastStepViewModel(fingerprintEnrollEnrollingViewModel, vibrationInteractor)

    udfpsViewModel =
      ViewModelProvider(
        this,
        UdfpsViewModel.UdfpsEnrollmentFactory(
          vibrationInteractor,
          displayDensityInteractor,
          navigationViewModel,
          debuggingInteractor,
          fingerprintEnrollEnrollingViewModel,
          udfpsEnrollDebugRepositoryImpl,
          enrollStageInteractor,
          orientationInteractor,
          backgroundViewModel,
          fingerprintSensorRepo,
          udfpsEnrollInteractor,
          fingerprintManagerInteractor,
          udfpsLastStepViewModel,
          accessibilityInteractor,
        ),
      )[UdfpsViewModel::class.java]

    fingerprintEnrollConfirmationViewModel =
      ViewModelProvider(
        this,
        FingerprintEnrollConfirmationViewModel.FingerprintEnrollConfirmationViewModelFactory(
          navigationViewModel,
          fingerprintManagerInteractor,
        ),
      )[FingerprintEnrollConfirmationViewModel::class.java]

    lifecycleScope.launch {
      navigationViewModel.currentStep.collect { step ->
        if (step is Init) {
          Log.d(TAG, "FingerprintNav.init($step)")
          navigationViewModel.update(FingerprintAction.ACTIVITY_CREATED, Init::class, "$TAG#init")
        }
      }
    }

    lifecycleScope.launch {
      navigationViewModel.navigateTo.filterNotNull().collect { step ->
        Log.d(TAG, "navigateTo: $step")
        if (step is ConfirmDeviceCredential) {
          launchConfirmOrChooseLock(userId)
          navigationViewModel.update(
            FingerprintAction.TRANSITION_FINISHED,
            TransitionStep::class,
            "$TAG#launchConfirmOrChooseLock",
          )
        } else {
          val theClass: Fragment? =
            when (step) {
              Confirmation -> FingerprintEnrollConfirmationV2Fragment()
              is Education -> {
                when (step.sensor.sensorType) {
                  FingerprintSensorType.REAR -> RfpsEnrollFindSensorFragment()
                  FingerprintSensorType.UDFPS_OPTICAL,
                  FingerprintSensorType.UDFPS_ULTRASONIC -> UdfpsEnrollFindSensorFragment()
                  else -> SfpsEnrollFindSensorFragment()
                }
              }
              is Enrollment -> {
                when (step.sensor.sensorType) {
                  FingerprintSensorType.REAR -> RFPSEnrollFragment()
                  FingerprintSensorType.UDFPS_OPTICAL,
                  FingerprintSensorType.UDFPS_ULTRASONIC -> UdfpsEnrollFragment()
                  else -> FingerprintEnrollEnrollingV2Fragment()
                }
              }
              is Introduction -> FingerprintEnrollIntroV2Fragment()
              else -> null
            }

          if (theClass != null) {
            supportFragmentManager
              .beginTransaction()
              .setCustomAnimations(
                step.enterTransition.toAnimation(),
                step.exitTransition.toAnimation(),
              )
              .setReorderingAllowed(true)
              .replace(R.id.fragment_container_view, theClass::class.java, null)
              .commit()
            navigationViewModel.update(
              FingerprintAction.TRANSITION_FINISHED,
              TransitionStep::class,
              "$TAG#fragmentManager.add($theClass)",
            )
          }
        }
      }
    }

    lifecycleScope.launch {
      navigationViewModel.shouldFinish.filterNotNull().collect {
        Log.d(TAG, "FingerprintSettingsNav.finishing($it)")
        if (it.result != null) {
          finishActivity(it.result)
        } else {
          finish()
        }
      }
    }

    lifecycleScope.launch {
      navigationViewModel.currentScreen.filterNotNull().collect { screen ->
        Log.d(TAG, "FingerprintSettingsNav.currentScreen($screen)")
      }
    }

    val fromSettingsSummary =
      intent.getBooleanExtra(BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY, false)
    if (
      fromSettingsSummary && GatekeeperPasswordProvider.containsGatekeeperPasswordHandle(intent)
    ) {
      overridePendingTransition(
        com.google.android.setupdesign.R.anim.sud_slide_next_in,
        com.google.android.setupdesign.R.anim.sud_slide_next_out,
      )
    }
  }

  private fun launchConfirmOrChooseLock(userId: Int) {
    val activity = this
    lifecycleScope.launch(coroutineDispatcher) {
      val intent = Intent()
      val builder = ChooseLockSettingsHelper.Builder(activity)
      val launched =
        builder
          .setRequestCode(CONFIRM_REQUEST)
          .setTitle(getString(R.string.security_settings_fingerprint_preference_title))
          .setRequestGatekeeperPasswordHandle(true)
          .setUserId(userId)
          .setForegroundOnly(true)
          .setReturnCredentials(true)
          .show()
      if (!launched) {
        intent.setClassName(SETTINGS_PACKAGE_NAME, ChooseLockGeneric::class.java.name)
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS, true)
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true)
        intent.putExtra(Intent.EXTRA_USER_ID, userId)
        confirmDeviceResultListener.launch(intent)
      }
    }
  }
}

private fun Transition.toAnimation(): Int {
  return when (this) {
    Transition.EnterFromLeft -> com.google.android.setupdesign.R.anim.sud_slide_back_in
    Transition.EnterFromRight -> com.google.android.setupdesign.R.anim.sud_slide_next_in
    Transition.ExitToLeft -> com.google.android.setupdesign.R.anim.sud_slide_next_out
    Transition.ExitToRight -> com.google.android.setupdesign.R.anim.sud_slide_back_out
  }
}
