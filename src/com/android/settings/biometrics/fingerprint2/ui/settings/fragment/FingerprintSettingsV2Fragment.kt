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

package com.android.settings.biometrics.fingerprint2.ui.settings.fragment

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyResources.Strings.Settings.FINGERPRINT_UNLOCK_DISABLED_EXPLANATION
import android.app.settings.SettingsEnums
import android.content.Context.FINGERPRINT_SERVICE
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.provider.Settings.Secure
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.Utils.SETTINGS_PACKAGE_NAME
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics.BiometricEnrollBase.CONFIRM_REQUEST
import com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY
import com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroductionInternal
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractorImpl
import com.android.settings.biometrics.fingerprint2.repository.PressToAuthProviderImpl
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintData
import com.android.settings.biometrics.fingerprint2.shared.model.Settings
import com.android.settings.biometrics.fingerprint2.ui.settings.binder.FingerprintSettingsViewBinder
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FingerprintSettingsNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel.FingerprintSettingsViewModel
import com.android.settings.core.SettingsBaseActivity
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.password.ChooseLockGeneric
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE
import com.android.settingslib.HelpUtils
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.transition.SettingsTransitionHelper
import com.android.settingslib.widget.FooterPreference
import com.google.android.setupdesign.util.DeviceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "FingerprintSettingsV2Fragment"
private const val KEY_FINGERPRINTS_ENROLLED_CATEGORY = "security_settings_fingerprints_enrolled"
private const val KEY_FINGERPRINT_SIDE_FPS_CATEGORY =
  "security_settings_fingerprint_unlock_category"
private const val KEY_FINGERPRINT_ADD = "key_fingerprint_add"
private const val KEY_FINGERPRINT_SIDE_FPS_SCREEN_ON_TO_AUTH =
  "security_settings_require_screen_on_to_auth"
private const val KEY_FINGERPRINT_FOOTER = "security_settings_fingerprint_footer"

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
class FingerprintSettingsV2Fragment :
  DashboardFragment(), FingerprintSettingsViewBinder.FingerprintView {
  private lateinit var settingsViewModel: FingerprintSettingsViewModel
  private lateinit var navigationViewModel: FingerprintSettingsNavigationViewModel

  /** Result listener for ChooseLock activity flow. */
  private val confirmDeviceResultListener =
    registerForActivityResult(StartActivityForResult()) { result ->
      val resultCode = result.resultCode
      val data = result.data
      onConfirmDevice(resultCode, data)
    }

  /** Result listener for launching enrollments **after** a user has reached the settings page. */
  private val launchAdditionalFingerprintListener: ActivityResultLauncher<Intent> =
    registerForActivityResult(StartActivityForResult()) { result ->
      lifecycleScope.launch {
        val resultCode = result.resultCode
        Log.d(TAG, "onEnrollAdditionalFingerprint($resultCode)")

        if (resultCode == BiometricEnrollBase.RESULT_TIMEOUT) {
          navigationViewModel.onEnrollAdditionalFailure()
        } else {
          navigationViewModel.onEnrollSuccess()
        }
      }
    }

  /** Initial listener for the first enrollment request */
  private val launchFirstEnrollmentListener: ActivityResultLauncher<Intent> =
    registerForActivityResult(StartActivityForResult()) { result ->
      lifecycleScope.launch {
        val resultCode = result.resultCode
        val data = result.data

        Log.d(TAG, "onEnrollFirstFingerprint($resultCode, $data)")
        if (resultCode != RESULT_FINISHED || data == null) {
          if (resultCode == BiometricEnrollBase.RESULT_TIMEOUT) {
            navigationViewModel.onEnrollFirstFailure(
              "Received RESULT_TIMEOUT when enrolling",
              resultCode
            )
          } else {
            navigationViewModel.onEnrollFirstFailure(
              "Incorrect resultCode or data was null",
              resultCode
            )
          }
        } else {
          val token = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN)
          val challenge = data.getExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE) as Long?
          navigationViewModel.onEnrollFirst(token, challenge)
        }
      }
    }

  override fun userLockout(authAttemptViewModel: FingerprintAuthAttemptModel.Error) {
    Toast.makeText(activity, authAttemptViewModel.message, Toast.LENGTH_SHORT).show()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // This is needed to support ChooseLockSettingBuilder...show(). All other activity
    // calls should use the registerForActivity method call.
    super.onActivityResult(requestCode, resultCode, data)
    onConfirmDevice(resultCode, data)
  }

  override fun onCreate(icicle: Bundle?) {
    super.onCreate(icicle)

    if (icicle != null) {
      Log.d(TAG, "onCreateWithSavedState")
    } else {
      Log.d(TAG, "onCreate()")
    }

    /*
    if (
      !FeatureFlagUtils.isEnabled(
        context,
        FeatureFlagUtils.SETTINGS_BIOMETRICS2_FINGERPRINT_SETTINGS
      )
    ) {
      Log.d(TAG, "Finishing due to feature not being enabled")
      finish()
      return
    }

     */

    val context = requireContext()
    val userId = context.userId

    preferenceScreen.isVisible = false

    val fingerprintManager = context.getSystemService(FINGERPRINT_SERVICE) as FingerprintManager

    val backgroundDispatcher = Dispatchers.IO
    val activity = requireActivity()
    val userHandle = activity.user.identifier
    // Note that SUW should not be launching FingerprintSettings
    val isAnySuw = Settings

    val pressToAuthProvider = {
      var toReturn: Int =
        Secure.getIntForUser(
          context.contentResolver,
          Secure.SFPS_PERFORMANT_AUTH_ENABLED,
          -1,
          userHandle,
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
        Secure.putIntForUser(
          context.contentResolver,
          Secure.SFPS_PERFORMANT_AUTH_ENABLED,
          toReturn,
          userHandle
        )
      }

      toReturn == 1
    }

    val interactor =
      FingerprintManagerInteractorImpl(
        context.applicationContext,
        backgroundDispatcher,
        fingerprintManager,
        GatekeeperPasswordProvider(LockPatternUtils(context.applicationContext)),
        PressToAuthProviderImpl(context),
        isAnySuw
      )

    val token = intent.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN)
    val challenge = intent.getLongExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, -1L)

    navigationViewModel =
      ViewModelProvider(
        this,
        FingerprintSettingsNavigationViewModel.FingerprintSettingsNavigationModelFactory(
          userId,
          interactor,
          backgroundDispatcher,
          token,
          challenge
        )
      )[FingerprintSettingsNavigationViewModel::class.java]

    settingsViewModel =
      ViewModelProvider(
        this,
        FingerprintSettingsViewModel.FingerprintSettingsViewModelFactory(
          userId,
          interactor,
          backgroundDispatcher,
          navigationViewModel,
        )
      )[FingerprintSettingsViewModel::class.java]

    FingerprintSettingsViewBinder.bind(
      this,
      settingsViewModel,
      navigationViewModel,
      lifecycleScope,
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

  override fun onStop() {
    super.onStop()
    navigationViewModel.maybeFinishActivity(requireActivity().isChangingConfigurations)
  }

  override fun onPause() {
    super.onPause()
    settingsViewModel.shouldAuthenticate(false)
    val transaction = parentFragmentManager.beginTransaction()
    for (frag in parentFragmentManager.fragments) {
      if (frag is InstrumentedDialogFragment) {
        Log.d(TAG, "removing dialog settings fragment $frag")
        frag.dismiss()
        transaction.remove(frag)
      }
    }
    transaction.commit()
  }

  override fun onResume() {
    super.onResume()
    settingsViewModel.shouldAuthenticate(true)
  }

  /** Used to indicate that preference has been clicked */
  fun onPrefClicked(fingerprintViewModel: FingerprintData) {
    Log.d(TAG, "onPrefClicked(${fingerprintViewModel})")
    settingsViewModel.onPrefClicked(fingerprintViewModel)
  }

  /** Used to indicate that a delete pref has been clicked */
  fun onDeletePrefClicked(fingerprintViewModel: FingerprintData) {
    Log.d(TAG, "onDeletePrefClicked(${fingerprintViewModel})")
    settingsViewModel.onDeleteClicked(fingerprintViewModel)
  }

  override fun showSettings(enrolledFingerprints: List<FingerprintData>) {
    val category =
      this@FingerprintSettingsV2Fragment.findPreference(KEY_FINGERPRINTS_ENROLLED_CATEGORY)
        as PreferenceCategory?

    category?.removeAll()

    enrolledFingerprints.forEach { fingerprint ->
      category?.addPreference(
        FingerprintSettingsPreference(
          requireContext(),
          fingerprint,
          this@FingerprintSettingsV2Fragment,
          enrolledFingerprints.size == 1,
        )
      )
    }
    category?.isVisible = true
    preferenceScreen.isVisible = true
    addFooter()
  }

  override fun updateAddFingerprintsPreference(canEnroll: Boolean, maxFingerprints: Int) {
    val pref = this@FingerprintSettingsV2Fragment.findPreference<Preference>(KEY_FINGERPRINT_ADD)
    val maxSummary = context?.getString(R.string.fingerprint_add_max, maxFingerprints) ?: ""
    pref?.summary = maxSummary
    pref?.isEnabled = canEnroll
    pref?.setOnPreferenceClickListener {
      navigationViewModel.onAddFingerprintClicked()
      true
    }
    pref?.isVisible = true
  }

  override fun updateSfpsPreference(isSfpsPrefVisible: Boolean) {
    val sideFpsPref =
      this@FingerprintSettingsV2Fragment.findPreference(KEY_FINGERPRINT_SIDE_FPS_CATEGORY)
        as PreferenceCategory?
    sideFpsPref?.isVisible = isSfpsPrefVisible
    val otherPref =
      this@FingerprintSettingsV2Fragment.findPreference(KEY_FINGERPRINT_SIDE_FPS_SCREEN_ON_TO_AUTH)
        as Preference?
    otherPref?.isVisible = isSfpsPrefVisible
  }

  private fun addFooter() {
    val footer =
      this@FingerprintSettingsV2Fragment.findPreference(KEY_FINGERPRINT_FOOTER)
        as PreferenceCategory?
    val admin =
      RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
        activity,
        DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT,
        requireActivity().userId
      )
    val activity = requireActivity()
    val helpIntent =
      HelpUtils.getHelpIntent(activity, getString(helpResource), activity::class.java.name)
    val learnMoreClickListener =
      View.OnClickListener { v: View? -> activity.startActivityForResult(helpIntent, 0) }

    class FooterColumn {
      var title: CharSequence? = null
      var learnMoreOverrideText: CharSequence? = null
      var learnMoreOnClickListener: View.OnClickListener? = null
    }

    var footerColumns = mutableListOf<FooterColumn>()
    if (admin != null) {
      val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
      val column1 = FooterColumn()
      column1.title =
        devicePolicyManager.resources.getString(FINGERPRINT_UNLOCK_DISABLED_EXPLANATION) {
          getString(R.string.security_fingerprint_disclaimer_lockscreen_disabled_1)
        }

      column1.learnMoreOnClickListener =
        View.OnClickListener { _ ->
          RestrictedLockUtils.sendShowAdminSupportDetailsIntent(activity, admin)
        }
      column1.learnMoreOverrideText = getText(R.string.admin_support_more_info)
      footerColumns.add(column1)
      val column2 = FooterColumn()
      column2.title = getText(R.string.security_fingerprint_disclaimer_lockscreen_disabled_2)
      column2.learnMoreOverrideText =
        getText(R.string.security_settings_fingerprint_settings_footer_learn_more)
      column2.learnMoreOnClickListener = learnMoreClickListener
      footerColumns.add(column2)
    } else {
      val column = FooterColumn()
      column.title =
        getString(
          R.string.security_settings_fingerprint_enroll_introduction_v3_message,
          DeviceHelper.getDeviceName(requireActivity())
        )
      column.learnMoreOnClickListener = learnMoreClickListener
      column.learnMoreOverrideText =
        getText(R.string.security_settings_fingerprint_settings_footer_learn_more)
      footerColumns.add(column)
    }

    footer?.removeAll()
    for (i in 0 until footerColumns.size) {
      val column = footerColumns[i]
      val footerPrefToAdd: FooterPreference =
        FooterPreference.Builder(requireContext()).setTitle(column.title).build()
      if (i > 0) {
        footerPrefToAdd.setIconVisibility(View.GONE)
      }
      if (column.learnMoreOnClickListener != null) {
        footerPrefToAdd.setLearnMoreAction(column.learnMoreOnClickListener)
        if (!TextUtils.isEmpty(column.learnMoreOverrideText)) {
          footerPrefToAdd.setLearnMoreText(column.learnMoreOverrideText)
        }
      }
      footer?.addPreference(footerPrefToAdd)
    }
  }

  override suspend fun askUserToDeleteDialog(fingerprintViewModel: FingerprintData): Boolean {
    Log.d(TAG, "showing delete dialog for (${fingerprintViewModel})")

    try {
      val willDelete =
        fingerprintPreferences()
          .first { it?.fingerprintViewModel == fingerprintViewModel }
          ?.askUserToDeleteDialog()
          ?: false
      if (willDelete) {
        mMetricsFeatureProvider.action(
          context,
          SettingsEnums.ACTION_FINGERPRINT_DELETE,
          fingerprintViewModel.fingerId
        )
      }
      return willDelete
    } catch (exception: Exception) {
      Log.d(TAG, "askUserToDeleteDialog exception $exception")
      return false
    }
  }

  override suspend fun askUserToRenameDialog(
    fingerprintViewModel: FingerprintData
  ): Pair<FingerprintData, String>? {
    Log.d(TAG, "showing rename dialog for (${fingerprintViewModel})")
    try {
      val toReturn =
        fingerprintPreferences()
          .first { it?.fingerprintViewModel == fingerprintViewModel }
          ?.askUserToRenameDialog()
      if (toReturn != null) {
        mMetricsFeatureProvider.action(
          context,
          SettingsEnums.ACTION_FINGERPRINT_RENAME,
          toReturn.first.fingerId
        )
      }
      return toReturn
    } catch (exception: Exception) {
      Log.d(TAG, "askUserToRenameDialog exception $exception")
      return null
    }
  }

  override suspend fun highlightPref(fingerId: Int) {
    fingerprintPreferences()
      .first { pref -> pref?.fingerprintViewModel?.fingerId == fingerId }
      ?.highlight()
  }

  override fun launchConfirmOrChooseLock(userId: Int) {
    lifecycleScope.launch(Dispatchers.Default) {
      navigationViewModel.setStepToLaunched()
      val intent = Intent()
      val builder =
        ChooseLockSettingsHelper.Builder(requireActivity(), this@FingerprintSettingsV2Fragment)
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

  override fun launchFullFingerprintEnrollment(
    userId: Int,
    gateKeeperPasswordHandle: Long?,
    challenge: Long?,
    challengeToken: ByteArray?,
  ) {
    navigationViewModel.setStepToLaunched()
    Log.d(TAG, "launchFullFingerprintEnrollment")
    val intent = Intent()
    intent.setClassName(
      SETTINGS_PACKAGE_NAME,
      FingerprintEnrollIntroductionInternal::class.java.name
    )
    intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true)
    intent.putExtra(
      SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
      SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE
    )

    intent.putExtra(Intent.EXTRA_USER_ID, userId)

    if (gateKeeperPasswordHandle != null) {
      intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, gateKeeperPasswordHandle)
    } else {
      intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, challengeToken)
      intent.putExtra(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, challenge)
    }
    launchFirstEnrollmentListener.launch(intent)
  }

  override fun setResultExternal(resultCode: Int) {
    setResult(resultCode)
  }

  override fun launchAddFingerprint(userId: Int, challengeToken: ByteArray?) {
    navigationViewModel.setStepToLaunched()
    val intent = Intent()
    intent.setClassName(
      SETTINGS_PACKAGE_NAME,
      FingerprintEnrollEnrolling::class.qualifiedName.toString()
    )
    intent.putExtra(Intent.EXTRA_USER_ID, userId)
    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, challengeToken)
    launchAdditionalFingerprintListener.launch(intent)
  }

  private fun onConfirmDevice(resultCode: Int, data: Intent?) {
    val wasSuccessful = resultCode == RESULT_FINISHED || resultCode == Activity.RESULT_OK
    val gateKeeperPasswordHandle = data?.getExtra(EXTRA_KEY_GK_PW_HANDLE) as Long?
    lifecycleScope.launch {
      navigationViewModel.onConfirmDevice(wasSuccessful, gateKeeperPasswordHandle)
    }
  }

  private fun fingerprintPreferences(): List<FingerprintSettingsPreference?> {
    val category =
      this@FingerprintSettingsV2Fragment.findPreference(KEY_FINGERPRINTS_ENROLLED_CATEGORY)
        as PreferenceCategory?

    return category?.let { cat ->
      cat.childrenToList().map { it as FingerprintSettingsPreference? }
    }
      ?: emptyList()
  }

  private fun PreferenceCategory.childrenToList(): List<Preference> {
    val mutable: MutableList<Preference> = mutableListOf()
    for (i in 0 until this.preferenceCount) {
      mutable.add(this.getPreference(i))
    }
    return mutable.toList()
  }
}
