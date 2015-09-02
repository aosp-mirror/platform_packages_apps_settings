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
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.security.KeyStore;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;

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
        private static final int MIN_PASSWORD_LENGTH = 4;
        private static final String KEY_UNLOCK_SET_OFF = "unlock_set_off";
        private static final String KEY_UNLOCK_SET_NONE = "unlock_set_none";
        private static final String KEY_UNLOCK_SET_PIN = "unlock_set_pin";
        private static final String KEY_UNLOCK_SET_PASSWORD = "unlock_set_password";
        private static final String KEY_UNLOCK_SET_PATTERN = "unlock_set_pattern";
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
        private boolean mForFingerprint = false;
        private String mUserPassword;
        private LockPatternUtils mLockPatternUtils;
        private FingerprintManager mFingerprintManager;
        private RemovalCallback mRemovalCallback = new RemovalCallback() {

            @Override
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                Log.v(TAG, "Fingerprint removed: " + fingerprint.getFingerId());
                if (mFingerprintManager.getEnrolledFingerprints().size() == 0) {
                    finish();
                }
            }

            @Override
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(getActivity(), errString, Toast.LENGTH_SHORT);
                }
                finish();
            }
        };

        @Override
        protected int getMetricsCategory() {
            return MetricsLogger.CHOOSE_LOCK_GENERIC;
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

            mHasChallenge = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
            mChallenge = getActivity().getIntent().getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            mForFingerprint = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);

            if (savedInstanceState != null) {
                mPasswordConfirmed = savedInstanceState.getBoolean(PASSWORD_CONFIRMED);
                mWaitingForConfirmation = savedInstanceState.getBoolean(WAITING_FOR_CONFIRMATION);
                mEncryptionRequestQuality = savedInstanceState.getInt(ENCRYPT_REQUESTED_QUALITY);
                mEncryptionRequestDisabled = savedInstanceState.getBoolean(
                        ENCRYPT_REQUESTED_DISABLED);
            }

            if (mPasswordConfirmed) {
                updatePreferencesOrFinish();
            } else if (!mWaitingForConfirmation) {
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                if (!helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST,
                        getString(R.string.unlock_set_unlock_launch_picker_title), true)) {
                    mPasswordConfirmed = true; // no password set, so no need to confirm
                    updatePreferencesOrFinish();
                } else {
                    mWaitingForConfirmation = true;
                }
            }
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (mForFingerprint) {
                final LayoutInflater inflater = LayoutInflater.from(getContext());
                final ListView listView = getListView();
                final View fingerprintHeader = inflater.inflate(
                        R.layout.choose_lock_generic_fingerprint_header, listView, false);
                listView.addHeaderView(fingerprintHeader, null, false);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                Preference preference) {
            final String key = preference.getKey();

            if (!isUnlockMethodSecure(key) && mLockPatternUtils.isSecure(UserHandle.myUserId())) {
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
            if (Process.myUserHandle().isOwner() && LockPatternUtils.isDeviceEncryptionEnabled()
                    && !dpm.getDoNotAskCredentialsOnBoot()) {
                mEncryptionRequestQuality = quality;
                mEncryptionRequestDisabled = disabled;
                final Context context = getActivity();
                // If accessibility is enabled and the user hasn't seen this dialog before, set the
                // default state to agree with that which is compatible with accessibility
                // (password not required).
                final boolean accEn = AccessibilityManager.getInstance(context).isEnabled();
                final boolean required = mLockPatternUtils.isCredentialRequiredToDecrypt(!accEn);
                Intent intent = getEncryptionInterstitialIntent(context, quality, required);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT,
                        mForFingerprint);
                startActivityForResult(intent, ENABLE_ENCRYPTION_REQUEST);
            } else {
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
            } else if (requestCode == ENABLE_ENCRYPTION_REQUEST
                    && resultCode == Activity.RESULT_OK) {
                mRequirePassword = data.getBooleanExtra(
                        EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true);
                updateUnlockMethodAndFinish(mEncryptionRequestQuality, mEncryptionRequestDisabled);
            } else if (requestCode == CHOOSE_LOCK_REQUEST) {
                getActivity().setResult(resultCode, data);
                finish();
            } else {
                getActivity().setResult(Activity.RESULT_CANCELED);
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
                addPreferencesFromResource(R.xml.security_settings_picker);
                disableUnusablePreferences(quality, hideDisabledPrefs);
                updateCurrentPreference();
                updatePreferenceSummaryIfNeeded();
            } else {
                updateUnlockMethodAndFinish(quality, false);
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
            if (mLockPatternUtils.isLockScreenDisabled(UserHandle.myUserId())) {
                return KEY_UNLOCK_SET_OFF;
            }
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return KEY_UNLOCK_SET_PATTERN;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return KEY_UNLOCK_SET_PIN;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                    return KEY_UNLOCK_SET_PASSWORD;
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
            int minQuality = mDPM.getPasswordQuality(null);
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

            for (int i = entries.getPreferenceCount() - 1; i >= 0; --i) {
                Preference pref = entries.getPreference(i);
                if (pref instanceof PreferenceScreen) {
                    final String key = pref.getKey();
                    boolean enabled = true;
                    boolean visible = true;
                    if (KEY_UNLOCK_SET_OFF.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                        if (getResources().getBoolean(R.bool.config_hide_none_security_option)) {
                            enabled = false;
                            visible = false;
                        }
                    } else if (KEY_UNLOCK_SET_NONE.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                    } else if (KEY_UNLOCK_SET_PATTERN.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
                    } else if (KEY_UNLOCK_SET_PIN.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
                    } else if (KEY_UNLOCK_SET_PASSWORD.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
                    }
                    if (hideDisabled) {
                        visible = enabled;
                    }
                    if (!visible) {
                        entries.removePreference(pref);
                    } else if (!enabled) {
                        pref.setSummary(R.string.unlock_set_unlock_disabled_summary);
                        pref.setEnabled(false);
                    }
                }
            }
        }

        private void updatePreferenceSummaryIfNeeded() {
            if (LockPatternUtils.isDeviceEncrypted()) {
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
                    case KEY_UNLOCK_SET_PASSWORD: {
                        preference.setSummary(summary);
                    } break;
                }
            }
        }

        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, boolean confirmCredentials) {
            return ChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, confirmCredentials);
        }

        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, long challenge) {
            return ChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, challenge);
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength,
                final int maxLength, boolean requirePasswordToDecrypt, String password) {
            return ChooseLockPassword.createIntent(context, quality, minLength, maxLength,
                    requirePasswordToDecrypt, password);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final boolean confirmCredentials) {
            return ChooseLockPattern.createIntent(context, requirePassword,
                    confirmCredentials);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
               long challenge) {
            return ChooseLockPattern.createIntent(context, requirePassword, challenge);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final String pattern) {
            return ChooseLockPattern.createIntent(context, requirePassword, pattern);
        }

        protected Intent getEncryptionInterstitialIntent(Context context, int quality,
                boolean required) {
            return EncryptionInterstitial.createStartIntent(context, quality, required);
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

            final Context context = getActivity();
            if (quality >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
                int minLength = mDPM.getPasswordMinimumLength(null);
                if (minLength < MIN_PASSWORD_LENGTH) {
                    minLength = MIN_PASSWORD_LENGTH;
                }
                final int maxLength = mDPM.getPasswordMaximumLength(quality);
                Intent intent;
                if (mHasChallenge) {
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mChallenge);
                } else {
                    intent = getLockPasswordIntent(context, quality, minLength,
                        maxLength, mRequirePassword, mUserPassword);
                }
                startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                Intent intent;
                if (mHasChallenge) {
                    intent = getLockPatternIntent(context, mRequirePassword,
                        mChallenge);
                } else {
                    intent = getLockPatternIntent(context, mRequirePassword,
                        mUserPassword);
                }
                startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                mChooseLockSettingsHelper.utils().clearLock(UserHandle.myUserId());
                mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled,
                        UserHandle.myUserId());
                removeAllFingerprintTemplatesAndFinish();
                getActivity().setResult(Activity.RESULT_OK);
            } else {
                removeAllFingerprintTemplatesAndFinish();
            }
        }

        private void removeAllFingerprintTemplatesAndFinish() {
            if (mFingerprintManager != null && mFingerprintManager.isHardwareDetected()
                    && mFingerprintManager.getEnrolledFingerprints().size() > 0) {
                mFingerprintManager.remove(new Fingerprint(null, 0, 0, 0), mRemovalCallback);
            } else {
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

        private int getResIdForFactoryResetProtectionWarningMessage() {
            boolean hasFingerprints = mFingerprintManager.hasEnrolledFingerprints();
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return hasFingerprints
                            ? R.string.unlock_disable_frp_warning_content_pattern_fingerprint
                            : R.string.unlock_disable_frp_warning_content_pattern;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return hasFingerprints
                            ? R.string.unlock_disable_frp_warning_content_pin_fingerprint
                            : R.string.unlock_disable_frp_warning_content_pin;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    return hasFingerprints
                            ? R.string.unlock_disable_frp_warning_content_password_fingerprint
                            : R.string.unlock_disable_frp_warning_content_password;
                default:
                    return hasFingerprints
                            ? R.string.unlock_disable_frp_warning_content_unknown_fingerprint
                            : R.string.unlock_disable_frp_warning_content_unknown;
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
            int message = getResIdForFactoryResetProtectionWarningMessage();
            FactoryResetProtectionWarningDialog dialog =
                    FactoryResetProtectionWarningDialog.newInstance(message, unlockMethodToSet);
            dialog.show(getChildFragmentManager(), TAG_FRP_WARNING_DIALOG);
        }

        public static class FactoryResetProtectionWarningDialog extends DialogFragment {

            private static final String ARG_MESSAGE_RES = "messageRes";
            private static final String ARG_UNLOCK_METHOD_TO_SET = "unlockMethodToSet";

            public static FactoryResetProtectionWarningDialog newInstance(int messageRes,
                    String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag =
                        new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
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
                        .setTitle(R.string.unlock_disable_frp_warning_title)
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
