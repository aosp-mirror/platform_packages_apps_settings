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

package com.android.settings;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.security.KeyStore;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import java.util.List;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class ChooseLockGeneric extends SettingsActivity {
    public static final String CONFIRM_CREDENTIALS = "confirm_credentials";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());

        String action = modIntent.getAction();
        if (DevicePolicyManager.ACTION_SET_NEW_PASSWORD.equals(action)
                || DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(action)) {
            modIntent.putExtra(EXTRA_HIDE_DRAWER, true);
        }
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
        private static final int MIN_PASSWORD_LENGTH = 4;
        private static final String KEY_UNLOCK_SET_OFF = "unlock_set_off";
        private static final String KEY_UNLOCK_SET_NONE = "unlock_set_none";
        private static final String KEY_UNLOCK_SET_PIN = "unlock_set_pin";
        private static final String KEY_UNLOCK_SET_PASSWORD = "unlock_set_password";
        private static final String KEY_UNLOCK_SET_PATTERN = "unlock_set_pattern";
        private static final String KEY_UNLOCK_SET_MANAGED = "unlock_set_managed";
        private static final String PASSWORD_CONFIRMED = "password_confirmed";
        private static final String WAITING_FOR_CONFIRMATION = "waiting_for_confirmation";
        public static final String MINIMUM_QUALITY_KEY = "minimum_quality";
        public static final String HIDE_DISABLED_PREFS = "hide_disabled_prefs";
        public static final String ENCRYPT_REQUESTED_QUALITY = "encrypt_requested_quality";
        public static final String ENCRYPT_REQUESTED_DISABLED = "encrypt_requested_disabled";
        public static final String TAG_FRP_WARNING_DIALOG = "frp_warning_dialog";

        private static final int CONFIRM_EXISTING_REQUEST = 100;
        private static final int ENABLE_ENCRYPTION_REQUEST = 101;
        private static final int CHOOSE_LOCK_REQUEST = 102;

        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private DevicePolicyManager mDPM;
        private KeyStore mKeyStore;
        private boolean mHasChallenge = false;
        private long mChallenge;
        private boolean mPasswordConfirmed = false;
        private boolean mWaitingForConfirmation = false;
        private int mEncryptionRequestQuality;
        private boolean mEncryptionRequestDisabled;
        private boolean mRequirePassword;
        private boolean mForChangeCredRequiredForBoot = false;
        private String mUserPassword;
        private LockPatternUtils mLockPatternUtils;
        private FingerprintManager mFingerprintManager;
        private int mUserId;
        private boolean mHideDrawer = false;
        private ManagedLockPasswordProvider mManagedPasswordProvider;

        protected boolean mForFingerprint = false;

        @Override
        protected int getMetricsCategory() {
            return MetricsEvent.CHOOSE_LOCK_GENERIC;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mFingerprintManager =
                (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
            mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mKeyStore = KeyStore.getInstance();
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this.getActivity());
            mLockPatternUtils = new LockPatternUtils(getActivity());

            // Defaults to needing to confirm credentials
            final boolean confirmCredentials = getActivity().getIntent()
                .getBooleanExtra(CONFIRM_CREDENTIALS, true);
            if (getActivity() instanceof ChooseLockGeneric.InternalActivity) {
                mPasswordConfirmed = !confirmCredentials;
            }
            mHideDrawer = getActivity().getIntent().getBooleanExtra(EXTRA_HIDE_DRAWER, false);

            mHasChallenge = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
            mChallenge = getActivity().getIntent().getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            mForFingerprint = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);
            mForChangeCredRequiredForBoot = getArguments() != null && getArguments().getBoolean(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_CHANGE_CRED_REQUIRED_FOR_BOOT);

            if (savedInstanceState != null) {
                mPasswordConfirmed = savedInstanceState.getBoolean(PASSWORD_CONFIRMED);
                mWaitingForConfirmation = savedInstanceState.getBoolean(WAITING_FOR_CONFIRMATION);
                mEncryptionRequestQuality = savedInstanceState.getInt(ENCRYPT_REQUESTED_QUALITY);
                mEncryptionRequestDisabled = savedInstanceState.getBoolean(
                        ENCRYPT_REQUESTED_DISABLED);
            }

            int targetUser = Utils.getSecureTargetUser(
                    getActivity().getActivityToken(),
                    UserManager.get(getActivity()),
                    null,
                    getActivity().getIntent().getExtras()).getIdentifier();
            if (DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(
                    getActivity().getIntent().getAction()) ||
                    !mLockPatternUtils.isSeparateProfileChallengeAllowed(targetUser)) {
                // Always use parent if explicitely requested or if profile challenge is not
                // supported
                Bundle arguments = getArguments();
                mUserId = Utils.getUserIdFromBundle(getContext(), arguments != null ? arguments
                        : getActivity().getIntent().getExtras());
            } else {
                mUserId = targetUser;
            }

            if (DevicePolicyManager.ACTION_SET_NEW_PASSWORD
                    .equals(getActivity().getIntent().getAction())
                    && Utils.isManagedProfile(UserManager.get(getActivity()), mUserId)
                    && mLockPatternUtils.isSeparateProfileChallengeEnabled(mUserId)) {
                getActivity().setTitle(R.string.lock_settings_picker_title_profile);
            }

            mManagedPasswordProvider = ManagedLockPasswordProvider.get(getActivity(), mUserId);

            if (mPasswordConfirmed) {
                updatePreferencesOrFinish();
                if (mForChangeCredRequiredForBoot) {
                    maybeEnableEncryption(mLockPatternUtils.getKeyguardStoredPasswordQuality(
                            mUserId), false);
                }
            } else if (!mWaitingForConfirmation) {
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                boolean managedProfileWithUnifiedLock = Utils
                        .isManagedProfile(UserManager.get(getActivity()), mUserId)
                        && !mLockPatternUtils.isSeparateProfileChallengeEnabled(mUserId);
                if (managedProfileWithUnifiedLock
                        || !helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST,
                        getString(R.string.unlock_set_unlock_launch_picker_title), true, mUserId)) {
                    mPasswordConfirmed = true; // no password set, so no need to confirm
                    updatePreferencesOrFinish();
                } else {
                    mWaitingForConfirmation = true;
                }
            }
            addHeaderView();
        }

        protected void addHeaderView() {
            if (mForFingerprint) {
                setHeaderView(R.layout.choose_lock_generic_fingerprint_header);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();

            if (!isUnlockMethodSecure(key) && mLockPatternUtils.isSecure(mUserId)) {
                // Show the disabling FRP warning only when the user is switching from a secure
                // unlock method to an insecure one
                showFactoryResetProtectionWarningDialog(key);
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
                mEncryptionRequestQuality = quality;
                mEncryptionRequestDisabled = disabled;
                // Get the intent that the encryption interstitial should start for creating
                // the new unlock method.
                Intent unlockMethodIntent = getIntentForUnlockMethod(quality, disabled);
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
                intent.putExtra(EXTRA_HIDE_DRAWER, mHideDrawer);
                startActivityForResult(intent, ENABLE_ENCRYPTION_REQUEST);
            } else {
                if (mForChangeCredRequiredForBoot) {
                    // Welp, couldn't change it. Oh well.
                    finish();
                    return;
                }
                mRequirePassword = false; // device encryption not enabled or not device owner.
                updateUnlockMethodAndFinish(quality, disabled);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            mWaitingForConfirmation = false;
            if (requestCode == CONFIRM_EXISTING_REQUEST && resultCode == Activity.RESULT_OK) {
                mPasswordConfirmed = true;
                mUserPassword = data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                updatePreferencesOrFinish();
                if (mForChangeCredRequiredForBoot) {
                    if (!TextUtils.isEmpty(mUserPassword)) {
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
                }
            } else {
                getActivity().setResult(Activity.RESULT_CANCELED);
                finish();
            }
            if (requestCode == Activity.RESULT_CANCELED && mForChangeCredRequiredForBoot) {
                finish();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            // Saved so we don't force user to re-enter their password if configuration changes
            outState.putBoolean(PASSWORD_CONFIRMED, mPasswordConfirmed);
            outState.putBoolean(WAITING_FOR_CONFIRMATION, mWaitingForConfirmation);
            outState.putInt(ENCRYPT_REQUESTED_QUALITY, mEncryptionRequestQuality);
            outState.putBoolean(ENCRYPT_REQUESTED_DISABLED, mEncryptionRequestDisabled);
        }

        private void updatePreferencesOrFinish() {
            Intent intent = getActivity().getIntent();
            int quality = intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, -1);
            if (quality == -1) {
                // If caller didn't specify password quality, show UI and allow the user to choose.
                quality = intent.getIntExtra(MINIMUM_QUALITY_KEY, -1);
                quality = upgradeQuality(quality);
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
            } else {
                updateUnlockMethodAndFinish(quality, false);
            }
        }

        protected void addPreferences() {
            addPreferencesFromResource(R.xml.security_settings_picker);

            // Used for testing purposes
            findPreference(KEY_UNLOCK_SET_NONE).setViewId(R.id.lock_none);
            findPreference(KEY_UNLOCK_SET_PIN).setViewId(R.id.lock_pin);
            findPreference(KEY_UNLOCK_SET_PASSWORD).setViewId(R.id.lock_password);
        }

        private void updatePreferenceText() {
            if (mForFingerprint) {
                Preference pattern = findPreference(KEY_UNLOCK_SET_PATTERN);
                pattern.setTitle(R.string.fingerprint_unlock_set_unlock_pattern);

                Preference pin = findPreference(KEY_UNLOCK_SET_PIN);
                pin.setTitle(R.string.fingerprint_unlock_set_unlock_pin);

                Preference password = findPreference(KEY_UNLOCK_SET_PASSWORD);
                password.setTitle(R.string.fingerprint_unlock_set_unlock_password);
            }

            if (mManagedPasswordProvider.isSettingManagedPasswordSupported()) {
                Preference managed = findPreference(KEY_UNLOCK_SET_MANAGED);
                managed.setTitle(mManagedPasswordProvider.getPickerOptionTitle(mForFingerprint));
            } else {
                removePreference(KEY_UNLOCK_SET_MANAGED);
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
                return KEY_UNLOCK_SET_OFF;
            }
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(credentialOwner)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return KEY_UNLOCK_SET_PATTERN;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return KEY_UNLOCK_SET_PIN;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    return KEY_UNLOCK_SET_PASSWORD;
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    return KEY_UNLOCK_SET_MANAGED;
                case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                    return KEY_UNLOCK_SET_NONE;
            }
            return null;
        }

        /** increases the quality if necessary */
        private int upgradeQuality(int quality) {
            quality = upgradeQualityForDPM(quality);
            return quality;
        }

        private int upgradeQualityForDPM(int quality) {
            // Compare min allowed password quality
            int minQuality = mDPM.getPasswordQuality(null, mUserId);
            if (quality < minQuality) {
                quality = minQuality;
            }
            return quality;
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

            int adminEnforcedQuality = mDPM.getPasswordQuality(null, mUserId);
            EnforcedAdmin enforcedAdmin = RestrictedLockUtils.checkIfPasswordQualityIsSet(
                    getActivity(), mUserId);
            for (int i = entries.getPreferenceCount() - 1; i >= 0; --i) {
                Preference pref = entries.getPreference(i);
                if (pref instanceof RestrictedPreference) {
                    final String key = pref.getKey();
                    boolean enabled = true;
                    boolean visible = true;
                    boolean disabledByAdmin = false;
                    if (KEY_UNLOCK_SET_OFF.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                        if (getResources().getBoolean(R.bool.config_hide_none_security_option)) {
                            enabled = false;
                            visible = false;
                        }
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                    } else if (KEY_UNLOCK_SET_NONE.equals(key)) {
                        if (mUserId != UserHandle.myUserId()) {
                            // Swipe doesn't make sense for profiles.
                            visible = false;
                        }
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                    } else if (KEY_UNLOCK_SET_PATTERN.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
                    } else if (KEY_UNLOCK_SET_PIN.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
                    } else if (KEY_UNLOCK_SET_PASSWORD.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
                    } else if (KEY_UNLOCK_SET_MANAGED.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_MANAGED
                                && mManagedPasswordProvider.isManagedPasswordChoosable();
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_MANAGED;
                    }
                    if (hideDisabled) {
                        visible = enabled;
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

            CharSequence summary = getString(R.string.secure_lock_encryption_warning);

            PreferenceScreen screen = getPreferenceScreen();
            final int preferenceCount = screen.getPreferenceCount();
            for (int i = 0; i < preferenceCount; i++) {
                Preference preference = screen.getPreference(i);
                switch (preference.getKey()) {
                    case KEY_UNLOCK_SET_PATTERN:
                    case KEY_UNLOCK_SET_PIN:
                    case KEY_UNLOCK_SET_PASSWORD:
                    case KEY_UNLOCK_SET_MANAGED: {
                        preference.setSummary(summary);
                    } break;
                }
            }
        }

        protected Intent getLockManagedPasswordIntent(boolean requirePassword, String password) {
            return mManagedPasswordProvider.createIntent(requirePassword, password);
        }

        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, boolean confirmCredentials, int userId) {
            return ChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, confirmCredentials, userId);
        }

        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, long challenge, int userId) {
            return ChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, challenge, userId);
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength,
                int maxLength, boolean requirePasswordToDecrypt, String password, int userId) {
            return ChooseLockPassword.createIntent(context, quality, minLength, maxLength,
                    requirePasswordToDecrypt, password, userId);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final boolean confirmCredentials, int userId) {
            return ChooseLockPattern.createIntent(context, requirePassword,
                    confirmCredentials, userId);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
               long challenge, int userId) {
            return ChooseLockPattern.createIntent(context, requirePassword, challenge, userId);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final String pattern, int userId) {
            return ChooseLockPattern.createIntent(context, requirePassword, pattern, userId);
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
         * {@link DevicePolicyManager#PASSWORD_QUALITY_UNSPECIFIED}
         */
        void updateUnlockMethodAndFinish(int quality, boolean disabled) {
            // Sanity check. We should never get here without confirming user's existing password.
            if (!mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }

            quality = upgradeQuality(quality);
            Intent intent = getIntentForUnlockMethod(quality, disabled);
            if (intent != null) {
                startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
                return;
            }

            if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                mLockPatternUtils.setSeparateProfileChallengeEnabled(mUserId, true, mUserPassword);
                mChooseLockSettingsHelper.utils().clearLock(mUserId);
                mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled, mUserId);
                removeAllFingerprintForUserAndFinish(mUserId);
                getActivity().setResult(Activity.RESULT_OK);
            } else {
                removeAllFingerprintForUserAndFinish(mUserId);
            }
        }

        private Intent getIntentForUnlockMethod(int quality, boolean disabled) {
            Intent intent = null;
            final Context context = getActivity();
            if (quality >= DevicePolicyManager.PASSWORD_QUALITY_MANAGED) {
                intent = getLockManagedPasswordIntent(mRequirePassword, mUserPassword);
            } else if (quality >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
                int minLength = mDPM.getPasswordMinimumLength(null, mUserId);
                if (minLength < MIN_PASSWORD_LENGTH) {
                    minLength = MIN_PASSWORD_LENGTH;
                }
                final int maxLength = mDPM.getPasswordMaximumLength(quality);
                if (mHasChallenge) {
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mChallenge, mUserId);
                } else {
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mUserPassword, mUserId);
                }
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                if (mHasChallenge) {
                    intent = getLockPatternIntent(context, mRequirePassword,
                            mChallenge, mUserId);
                } else {
                    intent = getLockPatternIntent(context, mRequirePassword,
                            mUserPassword, mUserId);
                }
            }
            if (intent != null) {
                intent.putExtra(EXTRA_HIDE_DRAWER, mHideDrawer);
            }
            return intent;
        }

        private void removeAllFingerprintForUserAndFinish(final int userId) {
            if (mFingerprintManager != null && mFingerprintManager.isHardwareDetected()) {
                if (mFingerprintManager.hasEnrolledFingerprints(userId)) {
                    mFingerprintManager.setActiveUser(userId);
                    // For the purposes of M and N, groupId is the same as userId.
                    final int groupId = userId;
                    Fingerprint finger = new Fingerprint(null, groupId, 0, 0);
                    mFingerprintManager.remove(finger, userId,
                            new RemovalCallback() {
                                @Override
                                public void onRemovalError(Fingerprint fp, int errMsgId,
                                        CharSequence errString) {
                                    Log.v(TAG, "Fingerprint removed: " + fp.getFingerId());
                                    if (fp.getFingerId() == 0) {
                                        removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                                    }
                                }

                                @Override
                                public void onRemovalSucceeded(Fingerprint fingerprint) {
                                    if (fingerprint.getFingerId() == 0) {
                                        removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                                    }
                                }
                            });
                } else {
                    // No fingerprints in this user, we may also want to delete managed profile
                    // fingerprints
                    removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                }
            } else {
                // The removal callback will call finish, once all fingerprints are removed.
                // We need to wait for that to occur, otherwise, the UI will still show that
                // fingerprints exist even though they are (about to) be removed depending on
                // the race condition.
                finish();
            }
        }

        private void removeManagedProfileFingerprintsAndFinishIfNecessary(final int parentUserId) {
            mFingerprintManager.setActiveUser(UserHandle.myUserId());
            final UserManager um = UserManager.get(getActivity());
            boolean hasChildProfile = false;
            if (!um.getUserInfo(parentUserId).isManagedProfile()) {
                // Current user is primary profile, remove work profile fingerprints if necessary
                final List<UserInfo> profiles = um.getProfiles(parentUserId);
                final int profilesSize = profiles.size();
                for (int i = 0; i < profilesSize; i++) {
                    final UserInfo userInfo = profiles.get(i);
                    if (userInfo.isManagedProfile() && !mLockPatternUtils
                            .isSeparateProfileChallengeEnabled(userInfo.id)) {
                        removeAllFingerprintForUserAndFinish(userInfo.id);
                        hasChildProfile = true;
                        break;
                    }
                }
            }
            if (!hasChildProfile) {
                finish();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_choose_lockscreen;
        }

        private int getResIdForFactoryResetProtectionWarningTitle() {
            boolean isProfile = Utils.isManagedProfile(UserManager.get(getActivity()), mUserId);
            return isProfile ? R.string.unlock_disable_frp_warning_title_profile
                    : R.string.unlock_disable_frp_warning_title;
        }

        private int getResIdForFactoryResetProtectionWarningMessage() {
            boolean hasFingerprints = mFingerprintManager.hasEnrolledFingerprints(mUserId);
            boolean isProfile = Utils.isManagedProfile(UserManager.get(getActivity()), mUserId);
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
            return !(KEY_UNLOCK_SET_OFF.equals(unlockMethod) ||
                    KEY_UNLOCK_SET_NONE.equals(unlockMethod));
        }

        private boolean setUnlockMethod(String unlockMethod) {
            EventLog.writeEvent(EventLogTags.LOCK_SCREEN_TYPE, unlockMethod);

            if (KEY_UNLOCK_SET_OFF.equals(unlockMethod)) {
                updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, true /* disabled */ );
            } else if (KEY_UNLOCK_SET_NONE.equals(unlockMethod)) {
                updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, false /* disabled */ );
            } else if (KEY_UNLOCK_SET_MANAGED.equals(unlockMethod)) {
                maybeEnableEncryption(DevicePolicyManager.PASSWORD_QUALITY_MANAGED, false);
            } else if (KEY_UNLOCK_SET_PATTERN.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, false);
            } else if (KEY_UNLOCK_SET_PIN.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC, false);
            } else if (KEY_UNLOCK_SET_PASSWORD.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC, false);
            } else {
                Log.e(TAG, "Encountered unknown unlock method to set: " + unlockMethod);
                return false;
            }
            return true;
        }

        private void showFactoryResetProtectionWarningDialog(String unlockMethodToSet) {
            int title = getResIdForFactoryResetProtectionWarningTitle();
            int message = getResIdForFactoryResetProtectionWarningMessage();
            FactoryResetProtectionWarningDialog dialog =
                    FactoryResetProtectionWarningDialog.newInstance(
                            title, message, unlockMethodToSet);
            dialog.show(getChildFragmentManager(), TAG_FRP_WARNING_DIALOG);
        }

        public static class FactoryResetProtectionWarningDialog extends DialogFragment {

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
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ((ChooseLockGenericFragment) getParentFragment())
                                                .setUnlockMethod(
                                                        args.getString(ARG_UNLOCK_METHOD_TO_SET));
                                    }
                                }
                        )
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dismiss();
                                    }
                                }
                        )
                        .create();
            }
        }
    }
}
