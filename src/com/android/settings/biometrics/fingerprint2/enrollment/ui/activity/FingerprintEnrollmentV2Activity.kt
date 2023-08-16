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

package com.android.settings.biometrics.fingerprint2.enrollment.ui.activity

import android.annotation.ColorInt
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.SetupWizardUtils
import com.android.settings.Utils
import com.android.settings.Utils.SETTINGS_PACKAGE_NAME
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics.BiometricEnrollBase.CONFIRM_REQUEST
import com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractorImpl
import com.android.settings.biometrics.fingerprint2.enrollment.ui.fragment.FingerprintEnrollConfirmationV2Fragment
import com.android.settings.biometrics.fingerprint2.enrollment.ui.fragment.FingerprintEnrollEnrollingV2Fragment
import com.android.settings.biometrics.fingerprint2.enrollment.ui.fragment.FingerprintEnrollFindSensorV2Fragment
import com.android.settings.biometrics.fingerprint2.enrollment.ui.fragment.FingerprintEnrollmentIntroV2Fragment
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.Confirmation
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.Education
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.Enrollment
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.FingerprintEnrollmentNavigationViewModel
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.FingerprintGatekeeperViewModel
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.FingerprintScrollViewModel
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.FingerprintViewModel
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.Finish
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.GatekeeperInfo
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.Intro
import com.android.settings.biometrics.fingerprint2.enrollment.ui.viewmodel.LaunchConfirmDeviceCredential
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE
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
  private lateinit var navigationViewModel: FingerprintEnrollmentNavigationViewModel
  private lateinit var gatekeeperViewModel: FingerprintGatekeeperViewModel
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

  override fun onAttachedToWindow() {
    window.statusBarColor = getBackgroundColor()
    super.onAttachedToWindow()
  }

  @ColorInt
  private fun getBackgroundColor(): Int {
    val stateList: ColorStateList? =
      Utils.getColorAttr(applicationContext, android.R.attr.windowBackground)
    return stateList?.defaultColor ?: Color.TRANSPARENT
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

    val interactor =
      FingerprintManagerInteractorImpl(
        context,
        backgroundDispatcher,
        fingerprintManager,
        GatekeeperPasswordProvider(LockPatternUtils(context))
      ) {
        var toReturn: Int =
          Settings.Secure.getIntForUser(
            context.contentResolver,
            Settings.Secure.SFPS_PERFORMANT_AUTH_ENABLED,
            -1,
            context.userId,
          )
        if (toReturn == -1) {
          toReturn =
            if (
              context.resources.getBoolean(com.android.internal.R.bool.config_performantAuthDefault)
            ) {
              1
            } else {
              0
            }
          Settings.Secure.putIntForUser(
            context.contentResolver,
            Settings.Secure.SFPS_PERFORMANT_AUTH_ENABLED,
            toReturn,
            context.userId
          )
        }
        toReturn == 1
      }

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
        FingerprintEnrollmentNavigationViewModel.FingerprintEnrollmentNavigationViewModelFactory(
          backgroundDispatcher,
          interactor,
          gatekeeperViewModel,
          gatekeeperInfo is GatekeeperInfo.GatekeeperPasswordInfo, /* canSkipConfirm */
        )
      )[FingerprintEnrollmentNavigationViewModel::class.java]

    // Initialize FingerprintViewModel
    ViewModelProvider(this, FingerprintViewModel.FingerprintViewModelFactory(interactor))[
      FingerprintViewModel::class.java]

    // Initialize scroll view model
    ViewModelProvider(this, FingerprintScrollViewModel.FingerprintScrollViewModelFactory())[
      FingerprintScrollViewModel::class.java]

    lifecycleScope.launch {
      navigationViewModel.navigationViewModel.filterNotNull().collect {
        Log.d(TAG, "navigationStep $it")
        val isForward = it.forward
        val currStep = it.currStep
        val theClass: Class<Fragment>? =
          when (currStep) {
            Confirmation -> FingerprintEnrollConfirmationV2Fragment::class.java as Class<Fragment>
            Education -> FingerprintEnrollFindSensorV2Fragment::class.java as Class<Fragment>
            Enrollment -> FingerprintEnrollEnrollingV2Fragment::class.java as Class<Fragment>
            Intro -> FingerprintEnrollmentIntroV2Fragment::class.java as Class<Fragment>
            else -> null
          }

        if (theClass != null) {
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
