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
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractorImpl
import com.android.settings.biometrics.fingerprint2.shared.model.Default
import com.android.settings.biometrics.fingerprint2.shared.model.SetupWizard
import com.android.settings.biometrics.fingerprint2.repository.PressToAuthProviderImpl
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollConfirmationV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollEnrollingV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollFindSensorV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment.FingerprintEnrollIntroV2Fragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.fragment.RFPSEnrollFragment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel.RFPSViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.AccessibilityViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.BackgroundViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.Confirmation
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.Education
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.Enrollment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintGatekeeperViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintScrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.Finish
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FoldStateViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.GatekeeperInfo
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.Intro
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.LaunchConfirmDeviceCredential
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.OrientationStateViewModel
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val TAG = "FingerprintEnrollmentV2Activity"

/**
 * This is the activity that controls the entire Fingerprint Enrollment experience through its
 * children fragments.
 */
class FingerprintEnrollmentV2Activity : FragmentActivity() {
  private lateinit var fingerprintEnrollEnrollingViewModel: FingerprintEnrollEnrollingViewModel
  private lateinit var navigationViewModel: FingerprintEnrollNavigationViewModel
  private lateinit var gatekeeperViewModel: FingerprintGatekeeperViewModel
  private lateinit var fingerprintEnrollViewModel: FingerprintEnrollViewModel
  private lateinit var accessibilityViewModel: AccessibilityViewModel
  private lateinit var foldStateViewModel: FoldStateViewModel
  private lateinit var orientationStateViewModel: OrientationStateViewModel
  private lateinit var fingerprintScrollViewModel: FingerprintScrollViewModel
  private lateinit var backgroundViewModel: BackgroundViewModel
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
    foldStateViewModel.onConfigurationChange(newConfig)
  }

  private fun onConfirmDevice(resultCode: Int, data: Intent?) {
    val wasSuccessful = resultCode == RESULT_FINISHED || resultCode == Activity.RESULT_OK
    val gateKeeperPasswordHandle = data?.getExtra(EXTRA_KEY_GK_PW_HANDLE) as Long?
    lifecycleScope.launch {
      gatekeeperViewModel.onConfirmDevice(wasSuccessful, gateKeeperPasswordHandle)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.fingerprint_v2_enroll_main)

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


    val interactor =
      FingerprintManagerInteractorImpl(
        context,
        backgroundDispatcher,
        fingerprintManager,
        GatekeeperPasswordProvider(LockPatternUtils(context)),
        PressToAuthProviderImpl(context),
        enrollType,
      )

    var challenge: Long? = intent.getExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE) as Long?
    val token = intent.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN)
    val gatekeeperInfo = FingerprintGatekeeperViewModel.toGateKeeperInfo(challenge, token)

    gatekeeperViewModel =
      ViewModelProvider(
        this,
        FingerprintGatekeeperViewModel.FingerprintGatekeeperViewModelFactory(
          gatekeeperInfo,
          interactor,
        )
      )[FingerprintGatekeeperViewModel::class.java]

    navigationViewModel =
      ViewModelProvider(
        this,
        FingerprintEnrollNavigationViewModel.FingerprintEnrollNavigationViewModelFactory(
          backgroundDispatcher,
          interactor,
          gatekeeperViewModel,
          gatekeeperInfo is GatekeeperInfo.GatekeeperPasswordInfo,
          enrollType,
        )
      )[FingerprintEnrollNavigationViewModel::class.java]

    // Initialize FoldStateViewModel
    foldStateViewModel =
      ViewModelProvider(this, FoldStateViewModel.FoldStateViewModelFactory(context))[
        FoldStateViewModel::class.java]
    foldStateViewModel.onConfigurationChange(resources.configuration)

    // Initialize FingerprintViewModel
    fingerprintEnrollViewModel =
      ViewModelProvider(
        this,
        FingerprintEnrollViewModel.FingerprintEnrollViewModelFactory(
          interactor,
          gatekeeperViewModel,
          navigationViewModel,
        )
      )[FingerprintEnrollViewModel::class.java]

    // Initialize scroll view model
    fingerprintScrollViewModel =
      ViewModelProvider(this, FingerprintScrollViewModel.FingerprintScrollViewModelFactory())[
        FingerprintScrollViewModel::class.java]

    // Initialize AccessibilityViewModel
    accessibilityViewModel =
      ViewModelProvider(
        this,
        AccessibilityViewModel.AccessibilityViewModelFactory(
          getSystemService(AccessibilityManager::class.java)!!
        )
      )[AccessibilityViewModel::class.java]

    // Initialize OrientationViewModel
    orientationStateViewModel =
      ViewModelProvider(this, OrientationStateViewModel.OrientationViewModelFactory(context))[
        OrientationStateViewModel::class.java]

    // Initialize FingerprintEnrollEnrollingViewModel
    fingerprintEnrollEnrollingViewModel =
      ViewModelProvider(
        this,
        FingerprintEnrollEnrollingViewModel.FingerprintEnrollEnrollingViewModelFactory(
          fingerprintEnrollViewModel,
          backgroundViewModel
        )
      )[FingerprintEnrollEnrollingViewModel::class.java]

    // Initialize FingerprintEnrollFindSensorViewModel
    ViewModelProvider(
      this,
      FingerprintEnrollFindSensorViewModel.FingerprintEnrollFindSensorViewModelFactory(
        navigationViewModel,
        fingerprintEnrollViewModel,
        gatekeeperViewModel,
        backgroundViewModel,
        accessibilityViewModel,
        foldStateViewModel,
        orientationStateViewModel
      )
    )[FingerprintEnrollFindSensorViewModel::class.java]

    // Initialize RFPS View Model
    ViewModelProvider(
      this,
      RFPSViewModel.RFPSViewModelFactory(fingerprintEnrollEnrollingViewModel)
    )[RFPSViewModel::class.java]

    lifecycleScope.launch {
      navigationViewModel.navigationViewModel
        .filterNotNull()
        .combine(fingerprintEnrollViewModel.sensorType) { nav, sensorType -> Pair(nav, sensorType) }
        .collect { (nav, sensorType) ->
          Log.d(TAG, "navigationStep $nav")
          fingerprintEnrollViewModel.sensorTypeCached = sensorType
          val isForward = nav.forward
          val currStep = nav.currStep
          val theClass: Class<Fragment>? =
            when (currStep) {
              Confirmation -> FingerprintEnrollConfirmationV2Fragment::class.java as Class<Fragment>
              Education -> FingerprintEnrollFindSensorV2Fragment::class.java as Class<Fragment>
              is Enrollment -> {
                when (sensorType) {
                  FingerprintSensorType.REAR -> RFPSEnrollFragment::class.java as Class<Fragment>
                  else -> FingerprintEnrollEnrollingV2Fragment::class.java as Class<Fragment>
                }
              }
              Intro -> FingerprintEnrollIntroV2Fragment::class.java as Class<Fragment>
              else -> null
            }

          if (theClass != null) {
            supportFragmentManager.fragments.onEach { fragment ->
              supportFragmentManager.beginTransaction().remove(fragment).commit()
            }

            supportFragmentManager
              .beginTransaction()
              .setReorderingAllowed(true)
              .add(R.id.fragment_container_view, theClass, null)
              .commit()
          } else {

            if (currStep is Finish) {
              if (currStep.resultCode != null) {
                finishActivity(currStep.resultCode)
              } else {
                finish()
              }
            } else if (currStep == LaunchConfirmDeviceCredential) {
              launchConfirmOrChooseLock(userId)
            }
          }
        }
    }

    val fromSettingsSummary =
      intent.getBooleanExtra(BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY, false)
    if (
      fromSettingsSummary && GatekeeperPasswordProvider.containsGatekeeperPasswordHandle(intent)
    ) {
      overridePendingTransition(
        com.google.android.setupdesign.R.anim.sud_slide_next_in,
        com.google.android.setupdesign.R.anim.sud_slide_next_out
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
