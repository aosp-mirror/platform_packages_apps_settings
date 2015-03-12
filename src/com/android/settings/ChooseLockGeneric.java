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
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.security.KeyStore;
import android.service.fingerprint.FingerprintManager;
import android.service.fingerprint.FingerprintManagerReceiver;
import android.util.EventLog;
import android.util.Log;
import android.util.MutableBoolean;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.ListView;

import com.android.internal.widget.LockPatternUtils;

import java.util.List;

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
        private boolean mPasswordConfirmed = false;
        private boolean mWaitingForConfirmation = false;
        private int mEncryptionRequestQuality;
        private boolean mEncryptionRequestDisabled;
        private boolean mRequirePassword;
        private LockPatternUtils mLockPatternUtils;
        private FingerprintManager mFingerprintManager;

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
                if (!helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST, null, null)) {
                    mPasswordConfirmed = true; // no password set, so no need to confirm
                    updatePreferencesOrFinish();
                } else {
                    mWaitingForConfirmation = true;
                }
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                Preference preference) {
            final String key = preference.getKey();

            if (!isUnlockMethodSecure(key) && mLockPatternUtils.isSecure()) {
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
        private void maybeEnableEncryption(int quality, boolean disabled) {
            if (Process.myUserHandle().isOwner() && LockPatternUtils.isDeviceEncryptionEnabled()) {
                mEncryptionRequestQuality = quality;
                mEncryptionRequestDisabled = disabled;
                final Context context = getActivity();
                // If accessibility is enabled and the user hasn't seen this dialog before, set the
                // default state to agree with that which is compatible with accessibility
                // (password not required).
                final boolean accEn = AccessibilityManager.getInstance(context).isEnabled();
                final boolean required = mLockPatternUtils.isCredentialRequiredToDecrypt(!accEn);
                Intent intent = getEncryptionInterstitialIntent(context, quality, required);
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
                updatePreferenceSummaryIfNeeded();
            } else {
                updateUnlockMethodAndFinish(quality, false);
            }
        }

        /** increases the quality if necessary */
        private int upgradeQuality(int quality) {
            quality = upgradeQualityForDPM(quality);
            quality = upgradeQualityForKeyStore(quality);
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

        private int upgradeQualityForKeyStore(int quality) {
            if (!mKeyStore.isEmpty()) {
                if (quality < CredentialStorage.MIN_PASSWORD_QUALITY) {
                    quality = CredentialStorage.MIN_PASSWORD_QUALITY;
                }
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
            final Intent intent = getActivity().getIntent();

            // if there are multiple users, disable "None" setting
            UserManager mUm = (UserManager) getSystemService(Context.USER_SERVICE);
            List<UserInfo> users = mUm.getUsers(true);
            final boolean singleUser = users.size() == 1;

            for (int i = entries.getPreferenceCount() - 1; i >= 0; --i) {
                Preference pref = entries.getPreference(i);
                if (pref instanceof PreferenceScreen) {
                    final String key = ((PreferenceScreen) pref).getKey();
                    boolean enabled = true;
                    boolean visible = true;
                    if (KEY_UNLOCK_SET_OFF.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                        visible = singleUser; // don't show when there's more than 1 user
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
                        visible = visible && enabled;
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

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final boolean confirmCredentials) {
            return ChooseLockPattern.createIntent(context, requirePassword,
                    confirmCredentials);
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
                Intent intent = getLockPasswordIntent(context, quality, minLength,
                        maxLength, mRequirePassword,  /* confirm credentials */false);
                startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                Intent intent = getLockPatternIntent(context, mRequirePassword,
                        /* confirm credentials */false);
                startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                mChooseLockSettingsHelper.utils().clearLock();
                mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled);
                removeAllFingerprintTemplates();
                getActivity().setResult(Activity.RESULT_OK);
                finish();
            } else {
                removeAllFingerprintTemplates();
                finish();
            }
        }

        // TODO: This is only required because we used to enforce clients have a listener,
        // which is no longer required in the new API.  Remove when that happens.
        FingerprintManagerReceiver mReceiver = new FingerprintManagerReceiver() {
            public void onRemoved(int fingerprintId) {
                Log.v(TAG, "onRemoved(id=" + fingerprintId + ")");
            }
        };

        private void removeAllFingerprintTemplates() {
            if (mFingerprintManager != null && mFingerprintManager.isHardwareDetected()) {
                mFingerprintManager.startListening(mReceiver);
                mFingerprintManager.remove(0 /* all fingerprint templates */);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mFingerprintManager.stopListening();
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_choose_lockscreen;
        }

        private int getResIdForFactoryResetProtectionWarningTitle() {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return R.string.unlock_disable_lock_pattern_summary;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return R.string.unlock_disable_lock_pin_summary;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    return R.string.unlock_disable_lock_password_summary;
                default:
                    return R.string.unlock_disable_lock_unknown_summary;
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
            int title = getResIdForFactoryResetProtectionWarningTitle();
            FactoryResetProtectionWarningDialog dialog =
                    FactoryResetProtectionWarningDialog.newInstance(title, unlockMethodToSet);
            dialog.show(getChildFragmentManager(), TAG_FRP_WARNING_DIALOG);
        }

        public static class FactoryResetProtectionWarningDialog extends DialogFragment {

            private static final String ARG_TITLE_RES = "titleRes";
            private static final String ARG_UNLOCK_METHOD_TO_SET = "unlockMethodToSet";

            public static FactoryResetProtectionWarningDialog newInstance(int title,
                    String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag =
                        new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
                args.putInt(ARG_TITLE_RES, title);
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
                        .setMessage(R.string.unlock_disable_frp_warning_content)
                        .setPositiveButton(R.string.okay,
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
