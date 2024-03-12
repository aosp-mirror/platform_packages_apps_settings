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
import static android.app.admin.DevicePolicyResources.Strings.Settings.LOCK_SETTINGS_NEW_PROFILE_LOCK_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.LOCK_SETTINGS_UPDATE_PROFILE_LOCK_TITLE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_IT_ADMIN_CANT_RESET_SCREEN_LOCK;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_IT_ADMIN_CANT_RESET_SCREEN_LOCK_ACTION;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_SCREEN_LOCK_SETUP_MESSAGE;

import static com.android.settings.password.ChooseLockPassword.ChooseLockPasswordFragment.RESULT_FINISHED;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CALLER_APP_NAME;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_IS_CALLING_APP_ADMIN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_WRITE_REPAIR_MODE_PW;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.EventLogTags;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollActivity;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.safetycenter.LockScreenSafetySource;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.widget.FooterPreference;

import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * Activity class that provides a generic implementation for displaying options to choose a lock
 * type, either for setting up a new lock or updating an existing lock.
 */
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
        private static final String KEY_SKIP_BIOMETRICS = "unlock_skip_biometrics";
        private static final String PASSWORD_CONFIRMED = "password_confirmed";
        private static final String WAITING_FOR_CONFIRMATION = "waiting_for_confirmation";
        public static final String HIDE_INSECURE_OPTIONS = "hide_insecure_options";
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
        static final int CHOOSE_LOCK_REQUEST = 102;
        @VisibleForTesting
        static final int CHOOSE_LOCK_BEFORE_BIOMETRIC_REQUEST = 103;
        @VisibleForTesting
        static final int SKIP_FINGERPRINT_REQUEST = 104;

        private LockPatternUtils mLockPatternUtils;
        private DevicePolicyManager mDpm;
        private boolean mRequestGatekeeperPasswordHandle = false;
        private boolean mPasswordConfirmed = false;
        private boolean mWaitingForConfirmation = false;
        private LockscreenCredential mUserPassword;
        private FingerprintManager mFingerprintManager;
        private FaceManager mFaceManager;
        private int mUserId;
        private boolean mIsManagedProfile;
        private ManagedLockPasswordProvider mManagedPasswordProvider;
        /**
         * Whether the activity is launched by admins via
         * {@link DevicePolicyManager#ACTION_SET_NEW_PASSWORD} or
         * {@link DevicePolicyManager#ACTION_SET_NEW_PARENT_PROFILE_PASSWORD}
         */
        private boolean mIsSetNewPassword = false;
        private UserManager mUserManager;
        private ChooseLockGenericController mController;
        private int mUnificationProfileId = UserHandle.USER_NULL;
        private LockscreenCredential mUnificationProfileCredential;

        /**
         * From intent extra {@link ChooseLockSettingsHelper#EXTRA_KEY_REQUESTED_MIN_COMPLEXITY}.
         * Only contains complexity requested by calling app, not complexity enforced by device
         * admins.
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
        protected boolean mForBiometrics = false;

        private boolean mOnlyEnforceDevicePasswordRequirement = false;
        private int mExtraLockScreenTitleResId;
        private int mExtraLockScreenDescriptionResId;

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
            } else if (arguments != null) {
                mUserPassword = (LockscreenCredential) arguments.getParcelable(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                mPasswordConfirmed = mUserPassword != null;
            }

            mRequestGatekeeperPasswordHandle = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, false);
            mForFingerprint = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);
            mForFace = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, false);
            mForBiometrics = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, false);

            mExtraLockScreenTitleResId = intent.getIntExtra(EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE, -1);
            mExtraLockScreenDescriptionResId =
                    intent.getIntExtra(EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION, -1);

            mRequestedMinComplexity = intent.getIntExtra(
                    EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE);
            mOnlyEnforceDevicePasswordRequirement = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY, false);

            mIsCallingAppAdmin = intent
                    .getBooleanExtra(EXTRA_KEY_IS_CALLING_APP_ADMIN, /* defValue= */ false);
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
                mUserPassword = savedInstanceState.getParcelable(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
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
            mIsManagedProfile = UserManager.get(getActivity()).isManagedProfile(mUserId);
            mController = new ChooseLockGenericController.Builder(
                    getContext(), mUserId, mLockPatternUtils)
                    .setAppRequestedMinComplexity(mRequestedMinComplexity)
                    .setEnforceDevicePasswordRequirementOnly(mOnlyEnforceDevicePasswordRequirement)
                    .setProfileToUnify(mUnificationProfileId)
                    .setHideInsecureScreenLockTypes(alwaysHideInsecureScreenLockTypes()
                            || intent.getBooleanExtra(HIDE_INSECURE_OPTIONS, false))
                    .build();

            // If the complexity is provided by the admin, do not get the caller app's name.
            // If the app requires, for example, low complexity, and the admin requires high
            // complexity, it does not make sense to show a footer telling the user it's the app
            // requesting a particular complexity because the admin-set complexity will override it.
            mCallerAppName = mController.isComplexityProvidedByAdmin() ? null :
                    intent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME);

            mManagedPasswordProvider = ManagedLockPasswordProvider.get(activity, mUserId);

            if (mPasswordConfirmed) {
                updatePreferencesOrFinish(savedInstanceState != null);
            } else if (!mWaitingForConfirmation) {
                final ChooseLockSettingsHelper.Builder builder =
                        new ChooseLockSettingsHelper.Builder(activity, this);
                builder.setRequestCode(CONFIRM_EXISTING_REQUEST)
                        .setTitle(getString(R.string.unlock_set_unlock_launch_picker_title))
                        .setReturnCredentials(true)
                        .setUserId(mUserId);
                boolean managedProfileWithUnifiedLock =
                        mIsManagedProfile
                        && !mLockPatternUtils.isSeparateProfileChallengeEnabled(mUserId);
                boolean skipConfirmation = managedProfileWithUnifiedLock && !mIsSetNewPassword;
                if (skipConfirmation || !builder.show()) {
                    mPasswordConfirmed = true; // no password set, so no need to confirm
                    updatePreferencesOrFinish(savedInstanceState != null);
                } else {
                    mWaitingForConfirmation = true;
                }
            }
            addHeaderView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            updateActivityTitle();
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        protected boolean alwaysHideInsecureScreenLockTypes() {
            return false;
        }

        private void updateActivityTitle() {
            if (mLockPatternUtils == null) {
                // mLockPatternUtils will be uninitialized if ChooseLockGenericFragment.onCreate()
                // finishes early.
                return;
            }
            final boolean updateExistingLock;
            if (mIsManagedProfile) {
                // Going from unified challenge -> separate challenge is considered as adding
                // a new lock to the profile, while if the profile already has a separate challenge
                // it's an update.
                updateExistingLock = mLockPatternUtils.isSeparateProfileChallengeEnabled(mUserId);
                if (updateExistingLock) {
                    getActivity().setTitle(mDpm.getResources().getString(
                            LOCK_SETTINGS_UPDATE_PROFILE_LOCK_TITLE,
                            () -> getString(mExtraLockScreenTitleResId != -1
                                    ? mExtraLockScreenTitleResId
                                    : R.string.lock_settings_picker_update_profile_lock_title)));
                } else {
                    getActivity().setTitle(mDpm.getResources().getString(
                            LOCK_SETTINGS_NEW_PROFILE_LOCK_TITLE,
                            () -> getString(mExtraLockScreenTitleResId != -1
                                    ? mExtraLockScreenTitleResId
                                    : R.string.lock_settings_picker_new_profile_lock_title)));
                }
            } else if (mExtraLockScreenTitleResId != -1) {
                // Show customized screen lock title if it is passed as an extra in the intent.
                getActivity().setTitle(mExtraLockScreenTitleResId);
            } else {
                updateExistingLock = mLockPatternUtils.isSecure(mUserId);
                if (updateExistingLock) {
                    getActivity().setTitle(R.string.lock_settings_picker_update_lock_title);
                } else {
                    getActivity().setTitle(R.string.lock_settings_picker_new_lock_title);
                }
            }
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
            setHeaderView(R.layout.choose_lock_generic_biometric_header);
            TextView textView = getHeaderView().findViewById(R.id.biometric_header_description);

            if (mIsManagedProfile) {
                textView.setText(mDpm.getResources().getString(
                        WORK_PROFILE_SCREEN_LOCK_SETUP_MESSAGE,
                        () -> getString(mExtraLockScreenDescriptionResId != -1
                                ? mExtraLockScreenDescriptionResId
                                : R.string.lock_settings_picker_profile_message)));
            } else if (mExtraLockScreenDescriptionResId != -1) {
                // Show customized description in screen lock if passed as an extra in the intent.
                textView.setText(mExtraLockScreenDescriptionResId);
            } else if (mForFingerprint) {
                if (mIsSetNewPassword) {
                    textView.setText(R.string.fingerprint_unlock_title);
                } else {
                    textView.setText(R.string.lock_settings_picker_biometric_message);
                }
            } else if (mForFace) {
                if (mIsSetNewPassword) {
                    textView.setText(R.string.face_unlock_title);
                } else {
                    textView.setText(R.string.lock_settings_picker_biometric_message);
                }
            } else if (mForBiometrics) {
                if (mIsSetNewPassword) {
                    textView.setText(R.string.biometrics_unlock_title);
                } else {
                    textView.setText(R.string.lock_settings_picker_biometric_message);
                }
            } else {
                textView.setText("");
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
            } else if (KEY_SKIP_FINGERPRINT.equals(key) || KEY_SKIP_FACE.equals(key)
                    || KEY_SKIP_BIOMETRICS.equals(key)) {
                Intent chooseLockGenericIntent = new Intent(getActivity(),
                    getInternalActivityClass());
                chooseLockGenericIntent.setAction(getIntent().getAction());
                if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
                    SetupWizardUtils.copySetupExtras(getIntent(), chooseLockGenericIntent);
                }
                // Forward the target user id to  ChooseLockGeneric.
                chooseLockGenericIntent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                chooseLockGenericIntent.putExtra(
                        EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE, mExtraLockScreenTitleResId);
                chooseLockGenericIntent.putExtra(CONFIRM_CREDENTIALS, !mPasswordConfirmed);
                chooseLockGenericIntent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY,
                        mRequestedMinComplexity);
                chooseLockGenericIntent.putExtra(EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY,
                        mOnlyEnforceDevicePasswordRequirement);
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
            } else if (requestCode == CHOOSE_LOCK_REQUEST) {
                if (resultCode != RESULT_CANCELED) {
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
                    // ChooseLockGeneric should have requested for a Gatekeeper Password Handle to
                    // be returned, so that biometric enrollment(s) can subsequently request
                    // Gatekeeper to create HardwareAuthToken(s) wrapping biometric-specific
                    // challenges. Send the extras (including the GK Password) to the enrollment
                    // activity.
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
                outState.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                        mUserPassword.duplicate());
            }
        }

        @VisibleForTesting
        void updatePreferencesOrFinish(boolean isRecreatingActivity) {
            Intent intent = getActivity().getIntent();
            int quality = -1;
            if (StorageManager.isFileEncrypted()) {
                quality = intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, -1);
            } else {
                // For unencrypted devices, always show the lock type picker and ignore
                // PASSWORD_TYPE_KEY.
                Log.i(TAG, "Ignoring PASSWORD_TYPE_KEY because device is not file encrypted");
            }
            if (quality == -1) {
                // If caller didn't specify password quality, show UI and allow the user to choose.
                final PreferenceScreen prefScreen = getPreferenceScreen();
                if (prefScreen != null) {
                    prefScreen.removeAll();
                }
                addPreferences();
                disableUnusablePreferences();
                updatePreferenceText();
                updateCurrentPreference();
            } else if (!isRecreatingActivity) {
                // Don't start the activity again if we are recreated for configuration change
                updateUnlockMethodAndFinish(quality, false, true /* chooseLockSkipped */);
            }
        }

        protected void addPreferences() {
            addPreferencesFromResource(R.xml.security_settings_picker);

            int profileUserId = Utils.getManagedProfileId(mUserManager, mUserId);
            final FooterPreference footer = findPreference(KEY_LOCK_SETTINGS_FOOTER);
            if (!TextUtils.isEmpty(mCallerAppName) && !mIsCallingAppAdmin) {
                footer.setVisible(true);
                footer.setTitle(getFooterString());
            } else if (!mForFace && !mForBiometrics && !mForFingerprint && !mIsManagedProfile
                    && mController.isScreenLockRestrictedByAdmin()
                    && profileUserId != UserHandle.USER_NULL) {
                final StringBuilder description = new StringBuilder(
                        mDpm.getResources().getString(
                                WORK_PROFILE_IT_ADMIN_CANT_RESET_SCREEN_LOCK,
                                () -> getString(
                                R.string.lock_settings_picker_admin_restricted_personal_message)));
                footer.setVisible(true);
                footer.setTitle(description);

                final StringBuilder setLockText = new StringBuilder(
                        mDpm.getResources().getString(
                                WORK_PROFILE_IT_ADMIN_CANT_RESET_SCREEN_LOCK_ACTION,
                                () -> getString(
                          R.string.lock_settings_picker_admin_restricted_personal_message_action)));
                View.OnClickListener setLockClickListener = (v) -> {
                    final Bundle extras = new Bundle();
                    extras.putInt(Intent.EXTRA_USER_ID, profileUserId);
                    if (mUserPassword != null) {
                        extras.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                                mUserPassword);
                    }
                    new SubSettingLauncher(getActivity())
                            .setDestination(ChooseLockGenericFragment.class.getName())
                            .setSourceMetricsCategory(getMetricsCategory())
                            .setArguments(extras)
                            .launch();
                    finish();
                };
                footer.setLearnMoreText(setLockText);
                footer.setLearnMoreAction(setLockClickListener);
            } else {
                footer.setVisible(false);
            }

            // Used for testing purposes
            findPreference(ScreenLockType.NONE.preferenceKey).setViewId(R.id.lock_none);
            findPreference(KEY_SKIP_FINGERPRINT).setViewId(R.id.lock_none);
            findPreference(KEY_SKIP_FACE).setViewId(R.id.lock_none);
            findPreference(KEY_SKIP_BIOMETRICS).setViewId(R.id.lock_none);
            findPreference(ScreenLockType.PIN.preferenceKey).setViewId(R.id.lock_pin);
            findPreference(ScreenLockType.PASSWORD.preferenceKey).setViewId(R.id.lock_password);
        }

        private String getFooterString() {
            @StringRes int stringId;
            switch (mController.getAggregatedPasswordComplexity()) {
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
            } else if (mForBiometrics) {
                setPreferenceTitle(ScreenLockType.PATTERN,
                        getBiometricsPreferenceTitle(ScreenLockType.PATTERN));
                setPreferenceTitle(ScreenLockType.PIN,
                        getBiometricsPreferenceTitle(ScreenLockType.PIN));
                setPreferenceTitle(ScreenLockType.PASSWORD,
                        getBiometricsPreferenceTitle(ScreenLockType.PASSWORD));
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
            if (!(mForBiometrics && mIsSetNewPassword)) {
                removePreference(KEY_SKIP_BIOMETRICS);
            }
        }

        @VisibleForTesting
        String getBiometricsPreferenceTitle(@NonNull ScreenLockType secureType) {
            final boolean hasFingerprint = Utils.hasFingerprintHardware(getContext());
            final boolean hasFace = Utils.hasFaceHardware(getContext());
            final boolean isSuw = WizardManagerHelper.isAnySetupWizard(getIntent());
            final boolean isFaceSupported =
                    hasFace && (!isSuw || BiometricUtils.isFaceSupportedInSuw(getContext()));

            // Assume the flow is "Screen Lock" + "Face" + "Fingerprint"
            if (mController != null) {
                return BiometricUtils.getCombinedScreenLockOptions(getContext(),
                        mController.getTitle(secureType), hasFingerprint, isFaceSupported);
            } else {
                Log.e(TAG, "ChooseLockGenericController is null!");
                return getResources().getString(R.string.error_title);
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
         * Disables preferences that are less secure than required quality.
         *
         */
        private void disableUnusablePreferences() {
            final PreferenceScreen entries = getPreferenceScreen();

            for (ScreenLockType lock : ScreenLockType.values()) {
                String key = lock.preferenceKey;
                Preference pref = findPreference(key);
                if (pref instanceof RestrictedPreference) {
                    boolean visible = mController.isScreenLockVisible(lock);
                    boolean enabled = mController.isScreenLockEnabled(lock);
                    if (!visible) {
                        entries.removePreference(pref);
                    } else if (!enabled) {
                        pref.setEnabled(false);
                    }
                }
            }
        }

        protected Intent getLockManagedPasswordIntent(LockscreenCredential password) {
            return mManagedPasswordProvider.createIntent(false, password);
        }

        protected Intent getLockPasswordIntent(int quality) {
            ChooseLockPassword.IntentBuilder builder =
                    new ChooseLockPassword.IntentBuilder(getContext())
                            .setPasswordType(quality)
                            .setPasswordRequirement(
                                    mController.getAggregatedPasswordComplexity(),
                                    mController.getAggregatedPasswordMetrics())
                            .setForFingerprint(mForFingerprint)
                            .setForFace(mForFace)
                            .setForBiometrics(mForBiometrics)
                            .setUserId(mUserId)
                            .setRequestGatekeeperPasswordHandle(mRequestGatekeeperPasswordHandle);
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
                            .setForBiometrics(mForBiometrics)
                            .setUserId(mUserId)
                            .setRequestGatekeeperPasswordHandle(mRequestGatekeeperPasswordHandle);
            if (mUserPassword != null) {
                builder.setPattern(mUserPassword);
            }
            if (mUnificationProfileId != UserHandle.USER_NULL) {
                builder.setProfileToUnify(mUnificationProfileId, mUnificationProfileCredential);
            }
            return builder.build();
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
            // We should never get here without confirming user's existing password.
            if (!mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }

            quality = mController.upgradeQuality(quality);
            Intent intent = getIntentForUnlockMethod(quality);
            if (intent != null) {
                if (getIntent().getBooleanExtra(EXTRA_SHOW_OPTIONS_BUTTON, false)) {
                    intent.putExtra(EXTRA_SHOW_OPTIONS_BUTTON, chooseLockSkipped);
                }
                if (getIntent().getBooleanExtra(EXTRA_KEY_REQUEST_WRITE_REPAIR_MODE_PW, false)) {
                    intent.putExtra(EXTRA_KEY_REQUEST_WRITE_REPAIR_MODE_PW, true);
                }
                intent.putExtra(EXTRA_CHOOSE_LOCK_GENERIC_EXTRAS, getIntent().getExtras());
                // If the caller requested Gatekeeper Password Handle to be returned, we assume it
                // came from biometric enrollment. onActivityResult will put the LockSettingsService
                // into the extras and launch biometric enrollment. This should be cleaned up,
                // since requesting a Gatekeeper Password Handle should not imply it came from
                // biometric setup/settings.
                startActivityForResult(intent,
                        mIsSetNewPassword && mRequestGatekeeperPasswordHandle
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
                    mLockPatternUtils.setLockCredential(
                            LockscreenCredential.createNone(), mUserPassword, mUserId);
                }
                mLockPatternUtils.setLockScreenDisabled(disabled, mUserId);
                getActivity().setResult(Activity.RESULT_OK);
                LockScreenSafetySource.onLockScreenChange(getContext());
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
        public void onStop() {
            super.onStop();
            // hasCredential checks to see if user chooses a password for screen lock. If the
            // screen lock is None or Swipe, we do not want to call getActivity().finish().
            // Otherwise, bugs would be caused. (e.g. b/278488549, b/278530059)
            final boolean hasCredential = mLockPatternUtils.isSecure(mUserId);
            if (!getActivity().isChangingConfigurations()
                    && !mWaitingForConfirmation && hasCredential) {
                getActivity().finish();
            }
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
            return mIsManagedProfile ? R.string.unlock_disable_frp_warning_title_profile
                    : R.string.unlock_disable_frp_warning_title;
        }

        private int getResIdForFactoryResetProtectionWarningMessage() {
            final boolean hasFingerprints;
            final boolean hasFace;
            if (mFingerprintManager != null && mFingerprintManager.isHardwareDetected()) {
                hasFingerprints = mFingerprintManager.hasEnrolledFingerprints(mUserId);
            } else {
                hasFingerprints = false;
            }

            if (mFaceManager != null && mFaceManager.isHardwareDetected()) {
                hasFace = mFaceManager.hasEnrolledTemplates(mUserId);
            } else {
                hasFace = false;
            }

            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    if (hasFingerprints && hasFace) {
                        return R.string.unlock_disable_frp_warning_content_pattern_face_fingerprint;
                    } else if (hasFingerprints) {
                        return R.string.unlock_disable_frp_warning_content_pattern_fingerprint;
                    } else if (hasFace) {
                        return R.string.unlock_disable_frp_warning_content_pattern_face;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_pattern;
                    }
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    if (hasFingerprints && hasFace) {
                        return R.string.unlock_disable_frp_warning_content_pin_face_fingerprint;
                    } else if (hasFingerprints) {
                        return R.string.unlock_disable_frp_warning_content_pin_fingerprint;
                    } else if (hasFace) {
                        return R.string.unlock_disable_frp_warning_content_pin_face;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_pin;
                    }
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    if (hasFingerprints && hasFace) {
                        return R.string
                                .unlock_disable_frp_warning_content_password_face_fingerprint;
                    } else if (hasFingerprints) {
                        return R.string.unlock_disable_frp_warning_content_password_fingerprint;
                    } else if (hasFace) {
                        return R.string.unlock_disable_frp_warning_content_password_face;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_password;
                    }
                default:
                    if (hasFingerprints && hasFace) {
                        return R.string.unlock_disable_frp_warning_content_unknown_face_fingerprint;
                    } else if (hasFingerprints) {
                        return R.string.unlock_disable_frp_warning_content_unknown_fingerprint;
                    } else if (hasFace) {
                        return R.string.unlock_disable_frp_warning_content_unknown_face;
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
                    case PATTERN:
                    case PIN:
                    case PASSWORD:
                    case MANAGED:
                        updateUnlockMethodAndFinish(
                                lock.defaultQuality,
                                lock == ScreenLockType.NONE,
                                false /* chooseLockSkipped */);
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
