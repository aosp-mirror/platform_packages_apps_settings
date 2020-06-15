/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.password;

import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_LOW;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_MEDIUM;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;

import static com.android.settings.password.ChooseLockPassword.ChooseLockPasswordFragment.RESULT_FINISHED;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CALLER_APP_NAME;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_IS_CALLING_APP_ADMIN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManager.PasswordComplexity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.EncryptionInterstitial;
import com.android.settings.EventLogTags;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollActivity;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import com.google.android.setupcompat.util.WizardManagerHelper;

public class ChooseLockGeneric extends SettingsActivity {
    public static final String CONFIRM_CREDENTIALS = "confirm_credentials";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ChooseLockGenericFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        return ChooseLockGenericFragment.class;
    }

    public static class InternalActivity extends ChooseLockGeneric {
    }

    public static class ChooseLockGenericFragment extends SettingsPreferenceFragment {

        private static final String TAG = "ChooseLockGenericFragment";
        private static final String KEY_SKIP_FINGERPRINT = "unlock_skip_fingerprint";
        private static final String KEY_SKIP_FACE = "unlock_skip_face";
        private static final String PASSWORD_CONFIRMED = "password_confirmed";
        private static final String WAITING_FOR_CONFIRMATION = "waiting_for_confirmation";
        public static final String MINIMUM_QUALITY_KEY = "minimum_quality";
        public static final String HIDE_DISABLED_PREFS = "hide_disabled_prefs";
        public static final String TAG_FRP_WARNING_DIALOG = "frp_warning_dialog";
        public static final String KEY_LOCK_SETTINGS_FOOTER ="lock_settings_footer";

        /**
         * Boolean extra determining whether a "screen lock options" button should be shown. This
         * extra is both sent and received by ChooseLockGeneric.
         *
         * When this extra is false, nothing will be done.
         * When ChooseLockGeneric receives this extra set as true, and if ChooseLockGeneric is
         * starting ChooseLockPassword or ChooseLockPattern automatically without user interaction,
         * ChooseLockGeneric will set this extra to true when starting ChooseLockPassword/Pattern.
         *
         * This gives the user the choice to select a different screen lock type, even if
         * ChooseLockGeneric selected a default.
         */
        public static final String EXTRA_SHOW_OPTIONS_BUTTON = "show_options_button";

        /**
         * Original intent extras used to start this activity. This is passed to ChooseLockPassword
         * when the "screen lock options" button is shown, so that when that button is clicked,
         * ChooseLockGeneric can be relaunched with the same extras.
         */
        public static final String EXTRA_CHOOSE_LOCK_GENERIC_EXTRAS = "choose_lock_generic_extras";

        @VisibleForTesting
        static final int CONFIRM_EXISTING_REQUEST = 100;
        @VisibleForTesting
        static final int ENABLE_ENCRYPTION_REQUEST = 101;
        @VisibleForTesting
        static final int CHOOSE_LOCK_REQUEST = 102;
        @VisibleForTesting
        static final int CHOOSE_LOCK_BEFORE_BIOMETRIC_REQUEST = 103;
        @VisibleForTesting
        static final int SKIP_FINGERPRINT_REQUEST = 104;

        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private DevicePolicyManager mDpm;
        private boolean mHasChallenge = false;
        private long mChallenge;
        private boolean mPasswordConfirmed = false;
        private boolean mWaitingForConfirmation = false;
        private boolean mForChangeCredRequiredForBoot = false;
        private LockscreenCredential mUserPassword;
        private LockPatternUtils mLockPatternUtils;
        private FingerprintManager mFingerprintManager;
        private FaceManager mFaceManager;
        private int mUserId;
        private ManagedLockPasswordProvider mManagedPasswordProvider;
        private boolean mIsSetNewPassword = false;
        private UserManager mUserManager;
        private ChooseLockGenericController mController;
        private int mUnificationProfileId = UserHandle.USER_NULL;
        private LockscreenCredential mUnificationProfileCredential;

        /**
         * From intent extra {@link ChooseLockSettingsHelper#EXTRA_KEY_REQUESTED_MIN_COMPLEXITY}.
         */
        @PasswordComplexity private int mRequestedMinComplexity;

        /** From intent extra {@link ChooseLockSettingsHelper#EXTRA_KEY_CALLER_APP_NAME}. */
        private String mCallerAppName = null;

        /**
         * The value from the intent extra {@link
         * ChooseLockSettingsHelper#EXTRA_KEY_IS_CALLING_APP_ADMIN}.
         */
        private boolean mIsCallingAppAdmin;

        protected boolean mForFingerprint = false;
        protected boolean mForFace = false;

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.CHOOSE_LOCK_GENERIC;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity activity = getActivity();
            final Bundle arguments = getArguments();
            if (!WizardManagerHelper.isDeviceProvisioned(activity)
                    && !canRunBeforeDeviceProvisioned()) {
                Log.i(TAG, "Refusing to start because device is not provisioned");
                activity.finish();
                return;
            }
            final Intent intent = activity.getIntent();
            String chooseLockAction = intent.getAction();
            mFingerprintManager = Utils.getFingerprintManagerOrNull(activity);
            mFaceManager = Utils.getFaceManagerOrNull(activity);
            mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(activity);
            mLockPatternUtils = new LockPatternUtils(activity);
            mIsSetNewPassword = ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(chooseLockAction)
                    || ACTION_SET_NEW_PASSWORD.equals(chooseLockAction);

            // Defaults to needing to confirm credentials
            final boolean confirmCredentials = intent
                .getBooleanExtra(CONFIRM_CREDENTIALS, true);
            if (activity instanceof ChooseLockGeneric.InternalActivity) {
                mPasswordConfirmed = !confirmCredentials;
                mUserPassword = intent.getParcelableExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            }

            mHasChallenge = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
            mChallenge = intent.getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            mForFingerprint = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);
            mForFace = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, false);
            mRequestedMinComplexity = intent
                    .getIntExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE);
            mCallerAppName =
                    intent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME);
            mIsCallingAppAdmin = intent
                    .getBooleanExtra(EXTRA_KEY_IS_CALLING_APP_ADMIN, /* defValue= */ false);
            mForChangeCredRequiredForBoot = arguments != null && arguments.getBoolean(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_CHANGE_CRED_REQUIRED_FOR_BOOT);
            mUserManager = UserManager.get(activity);

            if (arguments != null) {
                mUnificationProfileCredential = (LockscreenCredential) arguments.getParcelable(
                        ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL);
                mUnificationProfileId = arguments.getInt(
                        ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID,
                        UserHandle.USER_NULL);
            }

            if (savedInstanceState != null) {
                mPasswordConfirmed = savedInstanceState.getBoolean(PASSWORD_CONFIRMED);
                mWaitingForConfirmation = savedInstanceState.getBoolean(WAITING_FOR_CONFIRMATION);
                if (mUserPassword == null) {
                    mUserPassword = savedInstanceState.getParcelable(
                            ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                }
            }

            // a) If this is started from other user, use that user id.
            // b) If this is started from the same user, read the extra if this is launched
            //    from Settings app itself.
            // c) Otherwise, use UserHandle.myUserId().
            mUserId = Utils.getSecureTargetUser(
                    activity.getActivityToken(),
                    UserManager.get(activity),
                    arguments,
                    intent.getExtras()).getIdentifier();
            mController = new ChooseLockGenericController(
                    getContext(), mUserId, mRequestedMinComplexity, mLockPatternUtils);
            if (ACTION_SET_NEW_PASSWORD.equals(chooseLockAction)
                    && UserManager.get(activity).isManagedProfile(mUserId)
                    && mLockPatternUtils.isSeparateProfileChallengeEnabled(mUserId)) {
                activity.setTitle(R.string.lock_settings_picker_title_profile);
            }

            mManagedPasswordProvider = ManagedLockPasswordProvider.get(activity, mUserId);

            if (mPasswordConfirmed) {
                updatePreferencesOrFinish(savedInstanceState != null);
                if (mForChangeCredRequiredForBoot) {
                    maybeEnableEncryption(mLockPatternUtils.getKeyguardStoredPasswordQuality(
                            mUserId), false);
                }
            } else if (!mWaitingForConfirmation) {
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(activity, this);
                boolean managedProfileWithUnifiedLock =
                        UserManager.get(activity).isManagedProfile(mUserId)
                        && !mLockPatternUtils.isSeparateProfileChallengeEnabled(mUserId);
                boolean skipConfirmation = managedProfileWithUnifiedLock && !mIsSetNewPassword;
                if (skipConfirmation
                        || !helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST,
                        getString(R.string.unlock_set_unlock_launch_picker_title), true, mUserId)) {
                    mPasswordConfirmed = true; // no password set, so no need to confirm
                    updatePreferencesOrFinish(savedInstanceState != null);
                } else {
                    mWaitingForConfirmation = true;
                }
            }
            addHeaderView();
        }

        protected boolean canRunBeforeDeviceProvisioned() {
            PersistentDataBlockManager pdbm = (PersistentDataBlockManager)
                    getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);

            // Can only run during setup if factory reset protection has already been cleared
            // or if the device does not support FRP.
            return (pdbm == null || pdbm.getDataBlockSize() == 0);
        }

        protected Class<? extends ChooseLockGeneric.InternalActivity> getInternalActivityClass() {
            return ChooseLockGeneric.InternalActivity.class;
        }

        protected void addHeaderView() {
            if (mForFingerprint) {
                setHeaderView(R.layout.choose_lock_generic_fingerprint_header);
                if (mIsSetNewPassword) {
                    ((TextView) getHeaderView().findViewById(R.id.fingerprint_header_description))
                            .setText(R.string.fingerprint_unlock_title);
                }
            } else if (mForFace) {
                setHeaderView(R.layout.choose_lock_generic_face_header);
                if (mIsSetNewPassword) {
                    ((TextView) getHeaderView().findViewById(R.id.face_header_description))
                            .setText(R.string.face_unlock_title);
                }
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            writePreferenceClickMetric(preference);

            final String key = preference.getKey();
            if (!isUnlockMethodSecure(key) && mLockPatternUtils.isSecure(mUserId)) {
                // Show the disabling FRP warning only when the user is switching from a secure
                // unlock method to an insecure one
                showFactoryResetProtectionWarningDialog(key);
                return true;
            } else if (KEY_SKIP_FINGERPRINT.equals(key) || KEY_SKIP_FACE.equals(key)) {
                Intent chooseLockGenericIntent = new Intent(getActivity(),
                    getInternalActivityClass());
                chooseLockGenericIntent.setAction(getIntent().getAction());
                // Forward the target user id to  ChooseLockGeneric.
                chooseLockGenericIntent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                chooseLockGenericIntent.putExtra(CONFIRM_CREDENTIALS, !mPasswordConfirmed);
                chooseLockGenericIntent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY,
                        mRequestedMinComplexity);
                chooseLockGenericIntent.putExtra(EXTRA_KEY_CALLER_APP_NAME, mCallerAppName);
                if (mUserPassword != null) {
                    chooseLockGenericIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                            mUserPassword);
                }
                startActivityForResult(chooseLockGenericIntent, SKIP_FINGERPRINT_REQUEST);
                return true;
            } else {
                return setUnlockMethod(key);
            }
        }

        /**
         * If the device has encryption already enabled, then ask the user if they
         * also want to encrypt the phone with this password.
         *
         * @param quality
         * @param disabled
         */
        // TODO: why does this take disabled, its always called with a quality higher than
        // what makes sense with disabled == true
        private void maybeEnableEncryption(int quality, boolean disabled) {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            if (UserManager.get(getActivity()).isAdminUser()
                    && mUserId == UserHandle.myUserId()
                    && LockPatternUtils.isDeviceEncryptionEnabled()
                    && !LockPatternUtils.isFileEncryptionEnabled()
                    && !dpm.getDoNotAskCredentialsOnBoot()) {
                // Get the intent that the encryption interstitial should start for creating
                // the new unlock method.
                Intent unlockMethodIntent = getIntentForUnlockMethod(quality);
                unlockMethodIntent.putExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_FOR_CHANGE_CRED_REQUIRED_FOR_BOOT,
                        mForChangeCredRequiredForBoot);
                final Context context = getActivity();
                // If accessibility is enabled and the user hasn't seen this dialog before, set the
                // default state to agree with that which is compatible with accessibility
                // (password not required).
                final boolean accEn = AccessibilityManager.getInstance(context).isEnabled();
                final boolean required = mLockPatternUtils.isCredentialRequiredToDecrypt(!accEn);
                Intent intent = getEncryptionInterstitialIntent(context, quality, required,
                        unlockMethodIntent);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT,
                        mForFingerprint);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE,
                        mForFace);
                startActivityForResult(
                        intent,
                        mIsSetNewPassword && mHasChallenge
                                ? CHOOSE_LOCK_BEFORE_BIOMETRIC_REQUEST
                                : ENABLE_ENCRYPTION_REQUEST);
            } else {
                if (mForChangeCredRequiredForBoot) {
                    // Welp, couldn't change it. Oh well.
                    finish();
                    return;
                }
                updateUnlockMethodAndFinish(quality, disabled, false /* chooseLockSkipped */);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            mWaitingForConfirmation = false;
            if (requestCode == CONFIRM_EXISTING_REQUEST && resultCode == Activity.RESULT_OK) {
                mPasswordConfirmed = true;
                mUserPassword = data != null
                    ? data.getParcelableExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD)
                    : null;
                updatePreferencesOrFinish(false /* isRecreatingActivity */);
                if (mForChangeCredRequiredForBoot) {
                    if (mUserPassword != null && !mUserPassword.isNone()) {
                        maybeEnableEncryption(
                                mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId), false);
                    } else {
                        finish();
                    }
                }
            } else if (requestCode == CHOOSE_LOCK_REQUEST
                    || requestCode == ENABLE_ENCRYPTION_REQUEST) {
                if (resultCode != RESULT_CANCELED || mForChangeCredRequiredForBoot) {
                    getActivity().setResult(resultCode, data);
                    finish();
                } else {
                    // If PASSWORD_TYPE_KEY is set, this activity is used as a trampoline to start
                    // the actual password enrollment. If the result is canceled, which means the
                    // user pressed back, finish the activity with result canceled.
                    int quality = getIntent().getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, -1);
                    if (quality != -1) {
                        getActivity().setResult(RESULT_CANCELED, data);
                        finish();
                    }
                }
            } else if (requestCode == CHOOSE_LOCK_BEFORE_BIOMETRIC_REQUEST
                    && resultCode == BiometricEnrollBase.RESULT_FINISHED) {
                Intent intent = getBiometricEnrollIntent(getActivity());
                if (data != null) {
                    intent.putExtras(data.getExtras());
                }
                // Forward the target user id to fingerprint setup page.
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                startActivity(intent);
                finish();
            } else if (requestCode == SKIP_FINGERPRINT_REQUEST) {
                if (resultCode != RESULT_CANCELED) {
                    getActivity().setResult(
                            resultCode == RESULT_FINISHED ? RESULT_OK : resultCode, data);
                    finish();
                }
            } else if (requestCode == SearchFeatureProvider.REQUEST_CODE) {
                return;
            } else {
                getActivity().setResult(Activity.RESULT_CANCELED);
                finish();
            }
            if (requestCode == Activity.RESULT_CANCELED && mForChangeCredRequiredForBoot) {
                finish();
            }
        }

        protected Intent getBiometricEnrollIntent(Context context) {
            final Intent intent =
                    new Intent(context, BiometricEnrollActivity.InternalActivity.class);
            intent.putExtra(BiometricEnrollActivity.EXTRA_SKIP_INTRO, true);
            return intent;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            // Saved so we don't force user to re-enter their password if configuration changes
            outState.putBoolean(PASSWORD_CONFIRMED, mPasswordConfirmed);
            outState.putBoolean(WAITING_FOR_CONFIRMATION, mWaitingForConfirmation);
            if (mUserPassword != null) {
                outState.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, mUserPassword);
            }
        }

        @VisibleForTesting
        void updatePreferencesOrFinish(boolean isRecreatingActivity) {
            Intent intent = getActivity().getIntent();
            int quality = -1;
            if (StorageManager.isFileEncryptedNativeOrEmulated()) {
                quality = intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, -1);
            } else {
                // For non-file encrypted devices we need to show encryption interstitial, so always
                // show the lock type picker and ignore PASSWORD_TYPE_KEY.
                Log.i(TAG, "Ignoring PASSWORD_TYPE_KEY because device is not file encrypted");
            }
            if (quality == -1) {
                // If caller didn't specify password quality, show UI and allow the user to choose.
                quality = intent.getIntExtra(MINIMUM_QUALITY_KEY, -1);
                quality = mController.upgradeQuality(quality);
                final boolean hideDisabledPrefs = intent.getBooleanExtra(
                        HIDE_DISABLED_PREFS, false);
                final PreferenceScreen prefScreen = getPreferenceScreen();
                if (prefScreen != null) {
                    prefScreen.removeAll();
                }
                addPreferences();
                disableUnusablePreferences(quality, hideDisabledPrefs);
                updatePreferenceText();
                updateCurrentPreference();
                updatePreferenceSummaryIfNeeded();
            } else if (!isRecreatingActivity) {
                // Don't start the activity again if we are recreated for configuration change
                updateUnlockMethodAndFinish(quality, false, true /* chooseLockSkipped */);
            }
        }

        protected void addPreferences() {
            addPreferencesFromResource(R.xml.security_settings_picker);

            final Preference footer = findPreference(KEY_LOCK_SETTINGS_FOOTER);
            if (!TextUtils.isEmpty(mCallerAppName) && !mIsCallingAppAdmin) {
                footer.setVisible(true);
                footer.setTitle(getFooterString());
            } else {
                footer.setVisible(false);
            }

            // Used for testing purposes
            findPreference(ScreenLockType.NONE.preferenceKey).setViewId(R.id.lock_none);
            findPreference(KEY_SKIP_FINGERPRINT).setViewId(R.id.lock_none);
            findPreference(KEY_SKIP_FACE).setViewId(R.id.lock_none);
            findPreference(ScreenLockType.PIN.preferenceKey).setViewId(R.id.lock_pin);
            findPreference(ScreenLockType.PASSWORD.preferenceKey).setViewId(R.id.lock_password);
        }

        private String getFooterString() {
            @StringRes int stringId;
            switch (mRequestedMinComplexity) {
                case PASSWORD_COMPLEXITY_HIGH:
                    stringId = R.string.unlock_footer_high_complexity_requested;
                    break;
                case PASSWORD_COMPLEXITY_MEDIUM:
                    stringId = R.string.unlock_footer_medium_complexity_requested;
                    break;
                case PASSWORD_COMPLEXITY_LOW:
                    stringId = R.string.unlock_footer_low_complexity_requested;
                    break;
                case PASSWORD_COMPLEXITY_NONE:
                default:
                    stringId = R.string.unlock_footer_none_complexity_requested;
                    break;
            }

            return getResources().getString(stringId, mCallerAppName);
        }

        private void updatePreferenceText() {
            if (mForFingerprint) {
                setPreferenceTitle(ScreenLockType.PATTERN,
                        R.string.fingerprint_unlock_set_unlock_pattern);
                setPreferenceTitle(ScreenLockType.PIN, R.string.fingerprint_unlock_set_unlock_pin);
                setPreferenceTitle(ScreenLockType.PASSWORD,
                        R.string.fingerprint_unlock_set_unlock_password);
            } else if (mForFace) {
                setPreferenceTitle(ScreenLockType.PATTERN,
                        R.string.face_unlock_set_unlock_pattern);
                setPreferenceTitle(ScreenLockType.PIN, R.string.face_unlock_set_unlock_pin);
                setPreferenceTitle(ScreenLockType.PASSWORD,
                        R.string.face_unlock_set_unlock_password);
            }

            if (mManagedPasswordProvider.isSettingManagedPasswordSupported()) {
                setPreferenceTitle(ScreenLockType.MANAGED,
                        mManagedPasswordProvider.getPickerOptionTitle(mForFingerprint));
            } else {
                removePreference(ScreenLockType.MANAGED.preferenceKey);
            }

            if (!(mForFingerprint && mIsSetNewPassword)) {
                removePreference(KEY_SKIP_FINGERPRINT);
            }
            if (!(mForFace && mIsSetNewPassword)) {
                removePreference(KEY_SKIP_FACE);
            }
        }

        private void setPreferenceTitle(ScreenLockType lock, @StringRes int title) {
            Preference preference = findPreference(lock.preferenceKey);
            if (preference != null) {
                preference.setTitle(title);
            }
        }

        private void setPreferenceTitle(ScreenLockType lock, CharSequence title) {
            Preference preference = findPreference(lock.preferenceKey);
            if (preference != null) {
                preference.setTitle(title);
            }
        }

        private void setPreferenceSummary(ScreenLockType lock, @StringRes int summary) {
            Preference preference = findPreference(lock.preferenceKey);
            if (preference != null) {
                preference.setSummary(summary);
            }
        }

        private void updateCurrentPreference() {
            String currentKey = getKeyForCurrent();
            Preference preference = findPreference(currentKey);
            if (preference != null) {
                preference.setSummary(R.string.current_screen_lock);
            }
        }

        private String getKeyForCurrent() {
            final int credentialOwner = UserManager.get(getContext())
                    .getCredentialOwnerProfile(mUserId);
            if (mLockPatternUtils.isLockScreenDisabled(credentialOwner)) {
                return ScreenLockType.NONE.preferenceKey;
            }
            ScreenLockType lock =
                    ScreenLockType.fromQuality(
                            mLockPatternUtils.getKeyguardStoredPasswordQuality(credentialOwner));
            return lock != null ? lock.preferenceKey : null;
        }

        /***
         * Disables preferences that are less secure than required quality. The actual
         * implementation is in disableUnusablePreferenceImpl.
         *
         * @param quality the requested quality.
         * @param hideDisabledPrefs if false preferences show why they were disabled; otherwise
         * they're not shown at all.
         */
        protected void disableUnusablePreferences(final int quality, boolean hideDisabledPrefs) {
            disableUnusablePreferencesImpl(quality, hideDisabledPrefs);
        }

        /***
         * Disables preferences that are less secure than required quality.
         *
         * @param quality the requested quality.
         * @param hideDisabled whether to hide disable screen lock options.
         */
        protected void disableUnusablePreferencesImpl(final int quality,
                boolean hideDisabled) {
            final PreferenceScreen entries = getPreferenceScreen();

            int adminEnforcedQuality = mDpm.getPasswordQuality(null, mUserId);
            EnforcedAdmin enforcedAdmin = RestrictedLockUtilsInternal.checkIfPasswordQualityIsSet(
                    getActivity(), mUserId);
            // If we are to unify a work challenge at the end of the credential enrollment, manually
            // merge any password policy from that profile here, so we are enrolling a compliant
            // password. This is because once unified, the profile's password policy will
            // be enforced on the new credential.
            if (mUnificationProfileId != UserHandle.USER_NULL) {
                int profileEnforceQuality = mDpm.getPasswordQuality(null, mUnificationProfileId);
                if (profileEnforceQuality > adminEnforcedQuality) {
                    adminEnforcedQuality = profileEnforceQuality;
                    enforcedAdmin = EnforcedAdmin.combine(enforcedAdmin,
                            RestrictedLockUtilsInternal.checkIfPasswordQualityIsSet(
                                    getActivity(), mUnificationProfileId));
                }
            }

            for (ScreenLockType lock : ScreenLockType.values()) {
                String key = lock.preferenceKey;
                Preference pref = findPreference(key);
                if (pref instanceof RestrictedPreference) {
                    boolean visible = mController.isScreenLockVisible(lock);
                    boolean enabled = mController.isScreenLockEnabled(lock, quality);
                    boolean disabledByAdmin =
                            mController.isScreenLockDisabledByAdmin(lock, adminEnforcedQuality);
                    if (hideDisabled) {
                        visible = visible && enabled;
                    }
                    if (!visible) {
                        entries.removePreference(pref);
                    } else if (disabledByAdmin && enforcedAdmin != null) {
                        ((RestrictedPreference) pref).setDisabledByAdmin(enforcedAdmin);
                    } else if (!enabled) {
                        // we need to setDisabledByAdmin to null first to disable the padlock
                        // in case it was set earlier.
                        ((RestrictedPreference) pref).setDisabledByAdmin(null);
                        pref.setSummary(R.string.unlock_set_unlock_disabled_summary);
                        pref.setEnabled(false);
                    } else {
                        ((RestrictedPreference) pref).setDisabledByAdmin(null);
                    }
                }
            }
        }

        private void updatePreferenceSummaryIfNeeded() {
            // On a default block encrypted device with accessibility, add a warning
            // that your data is not credential encrypted
            if (!StorageManager.isBlockEncrypted()) {
                return;
            }

            if (StorageManager.isNonDefaultBlockEncrypted()) {
                return;
            }

            if (AccessibilityManager.getInstance(getActivity()).getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK).isEmpty()) {
                return;
            }

            setPreferenceSummary(ScreenLockType.PATTERN, R.string.secure_lock_encryption_warning);
            setPreferenceSummary(ScreenLockType.PIN, R.string.secure_lock_encryption_warning);
            setPreferenceSummary(ScreenLockType.PASSWORD, R.string.secure_lock_encryption_warning);
            setPreferenceSummary(ScreenLockType.MANAGED, R.string.secure_lock_encryption_warning);
        }

        protected Intent getLockManagedPasswordIntent(LockscreenCredential password) {
            return mManagedPasswordProvider.createIntent(false, password);
        }

        protected Intent getLockPasswordIntent(int quality) {
            ChooseLockPassword.IntentBuilder builder =
                    new ChooseLockPassword.IntentBuilder(getContext())
                            .setPasswordQuality(quality)
                            .setRequestedMinComplexity(mRequestedMinComplexity)
                            .setForFingerprint(mForFingerprint)
                            .setForFace(mForFace)
                            .setUserId(mUserId);
            if (mHasChallenge) {
                builder.setChallenge(mChallenge);
            }
            if (mUserPassword != null) {
                builder.setPassword(mUserPassword);
            }
            if (mUnificationProfileId != UserHandle.USER_NULL) {
                builder.setProfileToUnify(mUnificationProfileId, mUnificationProfileCredential);
            }
            return builder.build();
        }

        protected Intent getLockPatternIntent() {
            ChooseLockPattern.IntentBuilder builder =
                    new ChooseLockPattern.IntentBuilder(getContext())
                            .setForFingerprint(mForFingerprint)
                            .setForFace(mForFace)
                            .setUserId(mUserId);
            if (mHasChallenge) {
                builder.setChallenge(mChallenge);
            }
            if (mUserPassword != null) {
                builder.setPattern(mUserPassword);
            }
            if (mUnificationProfileId != UserHandle.USER_NULL) {
                builder.setProfileToUnify(mUnificationProfileId, mUnificationProfileCredential);
            }
            return builder.build();
        }

        protected Intent getEncryptionInterstitialIntent(Context context, int quality,
                boolean required, Intent unlockMethodIntent) {
            return EncryptionInterstitial.createStartIntent(context, quality, required,
                    unlockMethodIntent);
        }

        /**
         * Invokes an activity to change the user's pattern, password or PIN based on given quality
         * and minimum quality specified by DevicePolicyManager. If quality is
         * {@link DevicePolicyManager#PASSWORD_QUALITY_UNSPECIFIED}, password is cleared.
         *
         * @param quality the desired quality. Ignored if DevicePolicyManager requires more security
         * @param disabled whether or not to show LockScreen at all. Only meaningful when quality is
         * @param chooseLockSkipped whether or not this activity is skipped. This is true when this
         * activity was not shown to the user at all, instead automatically proceeding based on
         * the given intent extras, typically {@link LockPatternUtils#PASSWORD_TYPE_KEY}.
         * {@link DevicePolicyManager#PASSWORD_QUALITY_UNSPECIFIED}
         */
        void updateUnlockMethodAndFinish(int quality, boolean disabled, boolean chooseLockSkipped) {
            // Sanity check. We should never get here without confirming user's existing password.
            if (!mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }

            quality = mController.upgradeQuality(quality);
            Intent intent = getIntentForUnlockMethod(quality);
            if (intent != null) {
                if (getIntent().getBooleanExtra(EXTRA_SHOW_OPTIONS_BUTTON, false)) {
                    intent.putExtra(EXTRA_SHOW_OPTIONS_BUTTON, chooseLockSkipped);
                }
                intent.putExtra(EXTRA_CHOOSE_LOCK_GENERIC_EXTRAS, getIntent().getExtras());
                startActivityForResult(intent,
                        mIsSetNewPassword && mHasChallenge
                                ? CHOOSE_LOCK_BEFORE_BIOMETRIC_REQUEST
                                : CHOOSE_LOCK_REQUEST);
                return;
            }

            if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                // Clearing of user biometrics when screen lock is cleared is done at
                // LockSettingsService.removeBiometricsForUser().
                if (mUserPassword != null) {
                    // No need to call setLockCredential if the user currently doesn't
                    // have a password
                    mChooseLockSettingsHelper.utils().setLockCredential(
                            LockscreenCredential.createNone(), mUserPassword, mUserId);
                }
                mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled, mUserId);
                getActivity().setResult(Activity.RESULT_OK);
                finish();
            }
        }

        private Intent getIntentForUnlockMethod(int quality) {
            Intent intent = null;
            if (quality >= DevicePolicyManager.PASSWORD_QUALITY_MANAGED) {
                intent = getLockManagedPasswordIntent(mUserPassword);
            } else if (quality >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
                intent = getLockPasswordIntent(quality);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                intent = getLockPatternIntent();
            }
            return intent;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mUserPassword != null) {
                mUserPassword.zeroize();
            }
            // Force a garbage collection immediately to remove remnant of user password shards
            // from memory.
            System.gc();
            System.runFinalization();
            System.gc();
        }

        @Override
        public int getHelpResource() {
            return R.string.help_url_choose_lockscreen;
        }

        private int getResIdForFactoryResetProtectionWarningTitle() {
            boolean isProfile = UserManager.get(getActivity()).isManagedProfile(mUserId);
            return isProfile ? R.string.unlock_disable_frp_warning_title_profile
                    : R.string.unlock_disable_frp_warning_title;
        }

        private int getResIdForFactoryResetProtectionWarningMessage() {
            final boolean hasFingerprints;
            if (mFingerprintManager != null && mFingerprintManager.isHardwareDetected()) {
                hasFingerprints = mFingerprintManager.hasEnrolledFingerprints(mUserId);
            } else {
                hasFingerprints = false;
            }
            boolean isProfile = UserManager.get(getActivity()).isManagedProfile(mUserId);
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    if (hasFingerprints && isProfile) {
                        return R.string
                                .unlock_disable_frp_warning_content_pattern_fingerprint_profile;
                    } else if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pattern_fingerprint;
                    } else if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pattern_profile;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_pattern;
                    }
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    if (hasFingerprints && isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_fingerprint_profile;
                    } else if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_fingerprint;
                    } else if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_profile;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_pin;
                    }
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    if (hasFingerprints && isProfile) {
                        return R.string
                                .unlock_disable_frp_warning_content_password_fingerprint_profile;
                    } else if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_password_fingerprint;
                    } else if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_password_profile;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_password;
                    }
                default:
                    if (hasFingerprints && isProfile) {
                        return R.string
                                .unlock_disable_frp_warning_content_unknown_fingerprint_profile;
                    } else if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_unknown_fingerprint;
                    } else if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_unknown_profile;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_unknown;
                    }
            }
        }

        private boolean isUnlockMethodSecure(String unlockMethod) {
            return !(ScreenLockType.SWIPE.preferenceKey.equals(unlockMethod) ||
                    ScreenLockType.NONE.preferenceKey.equals(unlockMethod));
        }

        private boolean setUnlockMethod(String unlockMethod) {
            EventLog.writeEvent(EventLogTags.LOCK_SCREEN_TYPE, unlockMethod);

            ScreenLockType lock = ScreenLockType.fromKey(unlockMethod);
            if (lock != null) {
                switch (lock) {
                    case NONE:
                    case SWIPE:
                        updateUnlockMethodAndFinish(
                                lock.defaultQuality,
                                lock == ScreenLockType.NONE,
                                false /* chooseLockSkipped */);
                        return true;
                    case PATTERN:
                    case PIN:
                    case PASSWORD:
                    case MANAGED:
                        maybeEnableEncryption(lock.defaultQuality, false);
                        return true;
                }
            }
            Log.e(TAG, "Encountered unknown unlock method to set: " + unlockMethod);
            return false;
        }

        private void showFactoryResetProtectionWarningDialog(String unlockMethodToSet) {
            int title = getResIdForFactoryResetProtectionWarningTitle();
            int message = getResIdForFactoryResetProtectionWarningMessage();
            FactoryResetProtectionWarningDialog dialog =
                    FactoryResetProtectionWarningDialog.newInstance(
                            title, message, unlockMethodToSet);
            dialog.show(getChildFragmentManager(), TAG_FRP_WARNING_DIALOG);
        }

        public static class FactoryResetProtectionWarningDialog extends InstrumentedDialogFragment {

            private static final String ARG_TITLE_RES = "titleRes";
            private static final String ARG_MESSAGE_RES = "messageRes";
            private static final String ARG_UNLOCK_METHOD_TO_SET = "unlockMethodToSet";

            public static FactoryResetProtectionWarningDialog newInstance(
                    int titleRes, int messageRes, String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag =
                        new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
                args.putInt(ARG_TITLE_RES, titleRes);
                args.putInt(ARG_MESSAGE_RES, messageRes);
                args.putString(ARG_UNLOCK_METHOD_TO_SET, unlockMethodToSet);
                frag.setArguments(args);
                return frag;
            }

            @Override
            public void show(FragmentManager manager, String tag) {
                if (manager.findFragmentByTag(tag) == null) {
                    // Prevent opening multiple dialogs if tapped on button quickly
                    super.show(manager, tag);
                }
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Bundle args = getArguments();

                return new AlertDialog.Builder(getActivity())
                        .setTitle(args.getInt(ARG_TITLE_RES))
                        .setMessage(args.getInt(ARG_MESSAGE_RES))
                        .setPositiveButton(R.string.unlock_disable_frp_warning_ok,
                                (dialog, whichButton) -> {
                                    String unlockMethod = args.getString(ARG_UNLOCK_METHOD_TO_SET);
                                    ((ChooseLockGenericFragment) getParentFragment())
                                            .setUnlockMethod(unlockMethod);
                                })
                        .setNegativeButton(R.string.cancel, (dialog, whichButton) -> dismiss())
                        .create();
            }

            @Override
            public int getMetricsCategory() {
                return SettingsEnums.DIALOG_FRP;
            }
        }
    }
}
