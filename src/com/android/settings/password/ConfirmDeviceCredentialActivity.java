
/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PASSWORD_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PATTERN_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PIN_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_CONFIRM_PASSWORD;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_CONFIRM_PATTERN;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_CONFIRM_PIN;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.RemoteLockscreenValidationSession;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback;
import android.hardware.biometrics.PromptInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.concurrent.Executor;

/**
 * Launch this when you want to confirm the user is present by asking them to enter their
 * PIN/password/pattern.
 */
public class ConfirmDeviceCredentialActivity extends FragmentActivity {
    public static final String TAG = ConfirmDeviceCredentialActivity.class.getSimpleName();

    /**
     * If the intent is sent from {@link com.android.systemui.keyguard.WorkLockActivityController}
     * then check for device policy management flags.
     */
    public static final String EXTRA_FROM_WORK_LOCK_ACTIVITY_CONTROLLER =
            "from_work_lock_activity_controller";

    // The normal flow that apps go through
    private static final int CREDENTIAL_NORMAL = 1;
    // Unlocks the managed profile when the primary profile is unlocked
    private static final int CREDENTIAL_MANAGED = 2;

    private static final String TAG_BIOMETRIC_FRAGMENT = "fragment";

    public static class InternalActivity extends ConfirmDeviceCredentialActivity {
    }

    public static Intent createIntent(CharSequence title, CharSequence details) {
        Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME,
                ConfirmDeviceCredentialActivity.class.getName());
        intent.putExtra(KeyguardManager.EXTRA_TITLE, title);
        intent.putExtra(KeyguardManager.EXTRA_DESCRIPTION, details);
        return intent;
    }

    private BiometricFragment mBiometricFragment;
    private DevicePolicyManager mDevicePolicyManager;
    private LockPatternUtils mLockPatternUtils;
    private UserManager mUserManager;
    private TrustManager mTrustManager;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Context mContext;
    private boolean mCheckDevicePolicyManager;

    private String mTitle;
    private CharSequence mDetails;
    private int mUserId;
    private int mCredentialMode;
    private boolean mGoingToBackground;
    private boolean mWaitingForBiometricCallback;

    private Executor mExecutor = (runnable -> {
        mHandler.post(runnable);
    });

    private AuthenticationCallback mAuthenticationCallback = new AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            if (!mGoingToBackground) {
                mWaitingForBiometricCallback = false;
                if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                        || errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                    finish();
                } else if (mUserManager.getUserInfo(mUserId) == null) {
                    // This can happen when profile gets wiped due to too many failed auth attempts.
                    Log.i(TAG, "Finishing, user no longer valid: " + mUserId);
                    finish();
                } else {
                    // All other errors go to some version of CC
                    showConfirmCredentials();
                }
            } else if (mWaitingForBiometricCallback) { // mGoingToBackground is true
                mWaitingForBiometricCallback = false;
                finish();
            }
        }

        @Override
        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
            mWaitingForBiometricCallback = false;
            mTrustManager.setDeviceLockedForUser(mUserId, false);
            final boolean isStrongAuth = result.getAuthenticationType()
                    == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL;
            ConfirmDeviceCredentialUtils.reportSuccessfulAttempt(mLockPatternUtils, mUserManager,
                    mDevicePolicyManager, mUserId, isStrongAuth);
            ConfirmDeviceCredentialUtils.checkForPendingIntent(
                    ConfirmDeviceCredentialActivity.this);

            setResult(Activity.RESULT_OK);
            finish();
        }

        @Override
        public void onAuthenticationFailed() {
            mWaitingForBiometricCallback = false;
            mDevicePolicyManager.reportFailedBiometricAttempt(mUserId);
        }

        @Override
        public void onSystemEvent(int event) {
            Log.d(TAG, "SystemEvent: " + event);
            switch (event) {
                case BiometricConstants.BIOMETRIC_SYSTEM_EVENT_EARLY_USER_CANCEL:
                    finish();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        mDevicePolicyManager = getSystemService(DevicePolicyManager.class);
        mUserManager = UserManager.get(this);
        mTrustManager = getSystemService(TrustManager.class);
        mLockPatternUtils = new LockPatternUtils(this);

        Intent intent = getIntent();
        mContext = this;
        mCheckDevicePolicyManager = intent
                .getBooleanExtra(KeyguardManager.EXTRA_DISALLOW_BIOMETRICS_IF_POLICY_EXISTS, false);
        mTitle = intent.getStringExtra(KeyguardManager.EXTRA_TITLE);
        mDetails = intent.getCharSequenceExtra(KeyguardManager.EXTRA_DESCRIPTION);
        String alternateButton = intent.getStringExtra(
                KeyguardManager.EXTRA_ALTERNATE_BUTTON_LABEL);
        boolean frp = KeyguardManager.ACTION_CONFIRM_FRP_CREDENTIAL.equals(intent.getAction());
        boolean remoteValidation =
                KeyguardManager.ACTION_CONFIRM_REMOTE_DEVICE_CREDENTIAL.equals(intent.getAction());

        mUserId = UserHandle.myUserId();
        if (isInternalActivity()) {
            try {
                mUserId = Utils.getUserIdFromBundle(this, intent.getExtras());
            } catch (SecurityException se) {
                Log.e(TAG, "Invalid intent extra", se);
            }
        }
        final int effectiveUserId = mUserManager.getCredentialOwnerProfile(mUserId);
        final boolean isEffectiveUserManagedProfile =
                UserManager.get(this).isManagedProfile(effectiveUserId);
        // if the client app did not hand in a title and we are about to show the work challenge,
        // check whether there is a policy setting the organization name and use that as title
        if ((mTitle == null) && isEffectiveUserManagedProfile) {
            mTitle = getTitleFromOrganizationName(mUserId);
        }

        final PromptInfo promptInfo = new PromptInfo();
        promptInfo.setTitle(mTitle);
        promptInfo.setDescription(mDetails);
        promptInfo.setDisallowBiometricsIfPolicyExists(mCheckDevicePolicyManager);

        final @LockPatternUtils.CredentialType int credentialType = Utils.getCredentialType(
                mContext, effectiveUserId);
        if (mTitle == null) {
            promptInfo.setDeviceCredentialTitle(
                    getTitleFromCredentialType(credentialType, isEffectiveUserManagedProfile));
        }
        if (mDetails == null) {
            promptInfo.setDeviceCredentialSubtitle(
                    getDetailsFromCredentialType(credentialType, isEffectiveUserManagedProfile));
        }

        boolean launchedBiometric = false;
        boolean launchedCDC = false;
        // If the target is a managed user and user key not unlocked yet, we will force unlock
        // tied profile so it will enable work mode and unlock managed profile, when personal
        // challenge is unlocked.
        if (frp) {
            final ChooseLockSettingsHelper.Builder builder =
                    new ChooseLockSettingsHelper.Builder(this);
            launchedCDC = builder.setHeader(mTitle) // Show the title in the header location
                    .setDescription(mDetails)
                    .setAlternateButton(alternateButton)
                    .setExternal(true)
                    .setUserId(LockPatternUtils.USER_FRP)
                    .show();
        } else if (remoteValidation) {
            RemoteLockscreenValidationSession remoteLockscreenValidationSession =
                    intent.getParcelableExtra(
                            KeyguardManager.EXTRA_REMOTE_LOCKSCREEN_VALIDATION_SESSION,
                            RemoteLockscreenValidationSession.class);
            ComponentName remoteLockscreenValidationServiceComponent =
                    intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName.class);

            String checkboxLabel = intent.getStringExtra(KeyguardManager.EXTRA_CHECKBOX_LABEL);
            final ChooseLockSettingsHelper.Builder builder =
                    new ChooseLockSettingsHelper.Builder(this);
            launchedCDC = builder
                    .setRemoteLockscreenValidation(true)
                    .setRemoteLockscreenValidationSession(remoteLockscreenValidationSession)
                    .setRemoteLockscreenValidationServiceComponent(
                            remoteLockscreenValidationServiceComponent)
                    .setHeader(mTitle) // Show the title in the header location
                    .setDescription(mDetails)
                    .setCheckboxLabel(checkboxLabel)
                    .setAlternateButton(alternateButton)
                    .setExternal(true)
                    .show();
        } else if (isEffectiveUserManagedProfile && isInternalActivity()) {
            mCredentialMode = CREDENTIAL_MANAGED;
            if (isBiometricAllowed(effectiveUserId, mUserId)) {
                showBiometricPrompt(promptInfo);
                launchedBiometric = true;
            } else {
                showConfirmCredentials();
                launchedCDC = true;
            }
        } else {
            mCredentialMode = CREDENTIAL_NORMAL;
            if (isBiometricAllowed(effectiveUserId, mUserId)) {
                // Don't need to check if biometrics / pin/pattern/pass are enrolled. It will go to
                // onAuthenticationError and do the right thing automatically.
                showBiometricPrompt(promptInfo);
                launchedBiometric = true;
            } else {
                showConfirmCredentials();
                launchedCDC = true;
            }
        }

        if (launchedCDC) {
            finish();
        } else if (launchedBiometric) {
            // Keep this activity alive until BiometricPrompt goes away
            mWaitingForBiometricCallback = true;
        } else {
            Log.d(TAG, "No pattern, password or PIN set.");
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private String getTitleFromCredentialType(@LockPatternUtils.CredentialType int credentialType,
            boolean isEffectiveUserManagedProfile) {
        int overrideStringId;
        int defaultStringId;
        switch (credentialType) {
            case LockPatternUtils.CREDENTIAL_TYPE_PIN:

                if (isEffectiveUserManagedProfile) {
                    return mDevicePolicyManager.getResources().getString(
                            CONFIRM_WORK_PROFILE_PIN_HEADER,
                            () -> getString(R.string.lockpassword_confirm_your_work_pin_header));
                }

                return getString(R.string.lockpassword_confirm_your_pin_header);
            case LockPatternUtils.CREDENTIAL_TYPE_PATTERN:
                if (isEffectiveUserManagedProfile) {
                    return mDevicePolicyManager.getResources().getString(
                            CONFIRM_WORK_PROFILE_PATTERN_HEADER,
                            () -> getString(
                                    R.string.lockpassword_confirm_your_work_pattern_header));
                }

                return getString(R.string.lockpassword_confirm_your_pattern_header);
            case LockPatternUtils.CREDENTIAL_TYPE_PASSWORD:
                if (isEffectiveUserManagedProfile) {
                    return mDevicePolicyManager.getResources().getString(
                            CONFIRM_WORK_PROFILE_PASSWORD_HEADER,
                            () -> getString(
                                    R.string.lockpassword_confirm_your_work_password_header));
                }

                return getString(R.string.lockpassword_confirm_your_password_header);
        }
        return null;
    }

    private String getDetailsFromCredentialType(@LockPatternUtils.CredentialType int credentialType,
            boolean isEffectiveUserManagedProfile) {
        switch (credentialType) {
            case LockPatternUtils.CREDENTIAL_TYPE_PIN:
                if (isEffectiveUserManagedProfile) {
                    return mDevicePolicyManager.getResources().getString(WORK_PROFILE_CONFIRM_PIN,
                            () -> getString(
                                    R.string.lockpassword_confirm_your_pin_generic_profile));
                }

                return getString(R.string.lockpassword_confirm_your_pin_generic);
            case LockPatternUtils.CREDENTIAL_TYPE_PATTERN:
                if (isEffectiveUserManagedProfile) {
                    return mDevicePolicyManager.getResources().getString(
                            WORK_PROFILE_CONFIRM_PATTERN,
                            () -> getString(
                                    R.string.lockpassword_confirm_your_pattern_generic_profile));
                }

                return getString(R.string.lockpassword_confirm_your_pattern_generic);
            case LockPatternUtils.CREDENTIAL_TYPE_PASSWORD:
                if (isEffectiveUserManagedProfile) {
                    return mDevicePolicyManager.getResources().getString(
                            WORK_PROFILE_CONFIRM_PASSWORD,
                            () -> getString(
                                    R.string.lockpassword_confirm_your_password_generic_profile));
                }

                return getString(R.string.lockpassword_confirm_your_password_generic);
        }
        return null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Translucent activity that is "visible", so it doesn't complain about finish()
        // not being called before onResume().
        setVisible(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isChangingConfigurations()) {
            mGoingToBackground = true;
            if (!mWaitingForBiometricCallback) {
                finish();
            }
        } else {
            mGoingToBackground = false;
        }
    }

    // User could be locked while Effective user is unlocked even though the effective owns the
    // credential. Otherwise, biometric can't unlock fbe/keystore through
    // verifyTiedProfileChallenge. In such case, we also wanna show the user message that
    // biometric is disabled due to device restart.
    private boolean isStrongAuthRequired(int effectiveUserId) {
        return !mLockPatternUtils.isBiometricAllowedForUser(effectiveUserId)
                || !mUserManager.isUserUnlocked(mUserId);
    }

    private boolean isBiometricAllowed(int effectiveUserId, int realUserId) {
        return !isStrongAuthRequired(effectiveUserId) && !mLockPatternUtils
                .hasPendingEscrowToken(realUserId);
    }

    private void showBiometricPrompt(PromptInfo promptInfo) {
        mBiometricFragment = (BiometricFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_BIOMETRIC_FRAGMENT);
        boolean newFragment = false;

        if (mBiometricFragment == null) {
            mBiometricFragment = BiometricFragment.newInstance(promptInfo);
            newFragment = true;
        }
        mBiometricFragment.setCallbacks(mExecutor, mAuthenticationCallback);
        mBiometricFragment.setUser(mUserId);

        if (newFragment) {
            getSupportFragmentManager().beginTransaction()
                    .add(mBiometricFragment, TAG_BIOMETRIC_FRAGMENT).commit();
        }
    }

    /**
     * Shows ConfirmDeviceCredentials for normal apps.
     */
    private void showConfirmCredentials() {
        boolean launched = false;
        // The only difference between CREDENTIAL_MANAGED and CREDENTIAL_NORMAL is that for
        // CREDENTIAL_MANAGED, we launch the real confirm credential activity with an explicit
        // but fake challenge value (0L). This will result in ConfirmLockPassword calling
        // verifyTiedProfileChallenge() (if it's a profile with unified challenge), due to the
        // difference between ConfirmLockPassword.startVerifyPassword() and
        // ConfirmLockPassword.startCheckPassword(). Calling verifyTiedProfileChallenge() here is
        // necessary when this is part of the turning on work profile flow, because it forces
        // unlocking the work profile even before the profile is running.
        // TODO: Remove the duplication of checkPassword and verifyPassword in ConfirmLockPassword,
        // LockPatternChecker and LockPatternUtils. verifyPassword should be the only API to use,
        // which optionally accepts a challenge.
        if (mCredentialMode == CREDENTIAL_MANAGED) {
            final ChooseLockSettingsHelper.Builder builder =
                    new ChooseLockSettingsHelper.Builder(this);
            launched = builder.setHeader(mTitle)
                    .setDescription(mDetails)
                    .setExternal(true)
                    .setUserId(mUserId)
                    .setForceVerifyPath(true)
                    .show();
        } else if (mCredentialMode == CREDENTIAL_NORMAL) {
            final ChooseLockSettingsHelper.Builder builder =
                    new ChooseLockSettingsHelper.Builder(this);
            launched = builder.setHeader(mTitle) // Show the title string in the header area
                    .setDescription(mDetails)
                    .setExternal(true)
                    .setUserId(mUserId)
                    .show();
        }
        if (!launched) {
            Log.d(TAG, "No pin/pattern/pass set");
            setResult(Activity.RESULT_OK);
        }
        finish();
    }

    private boolean isInternalActivity() {
        return this instanceof ConfirmDeviceCredentialActivity.InternalActivity;
    }

    private String getTitleFromOrganizationName(int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        CharSequence organizationNameForUser = (dpm != null)
                ? dpm.getOrganizationNameForUser(userId) : null;
        return organizationNameForUser != null ? organizationNameForUser.toString() : null;
    }
}
