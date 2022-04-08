
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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback;
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

    public static Intent createIntent(CharSequence title, CharSequence details, long challenge) {
        Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME,
                ConfirmDeviceCredentialActivity.class.getName());
        intent.putExtra(KeyguardManager.EXTRA_TITLE, title);
        intent.putExtra(KeyguardManager.EXTRA_DESCRIPTION, details);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        return intent;
    }

    private BiometricManager mBiometricManager;
    private BiometricFragment mBiometricFragment;
    private DevicePolicyManager mDevicePolicyManager;
    private LockPatternUtils mLockPatternUtils;
    private UserManager mUserManager;
    private TrustManager mTrustManager;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Context mContext;
    private boolean mCheckDevicePolicyManager;

    private String mTitle;
    private String mDetails;
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

    private String getStringForError(int errorCode) {
        switch (errorCode) {
            case BiometricConstants.BIOMETRIC_ERROR_USER_CANCELED:
                return getString(com.android.internal.R.string.biometric_error_user_canceled);
            case BiometricConstants.BIOMETRIC_ERROR_CANCELED:
                return getString(com.android.internal.R.string.biometric_error_canceled);
            default:
                return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        mBiometricManager = getSystemService(BiometricManager.class);
        mDevicePolicyManager = getSystemService(DevicePolicyManager.class);
        mUserManager = UserManager.get(this);
        mTrustManager = getSystemService(TrustManager.class);
        mLockPatternUtils = new LockPatternUtils(this);

        Intent intent = getIntent();
        mContext = this;
        mCheckDevicePolicyManager = intent
                .getBooleanExtra(BiometricPrompt.EXTRA_DISALLOW_BIOMETRICS_IF_POLICY_EXISTS, false);
        mTitle = intent.getStringExtra(KeyguardManager.EXTRA_TITLE);
        mDetails = intent.getStringExtra(KeyguardManager.EXTRA_DESCRIPTION);
        String alternateButton = intent.getStringExtra(
                KeyguardManager.EXTRA_ALTERNATE_BUTTON_LABEL);
        boolean frp = KeyguardManager.ACTION_CONFIRM_FRP_CREDENTIAL.equals(intent.getAction());

        mUserId = UserHandle.myUserId();
        if (isInternalActivity()) {
            try {
                mUserId = Utils.getUserIdFromBundle(this, intent.getExtras());
            } catch (SecurityException se) {
                Log.e(TAG, "Invalid intent extra", se);
            }
        }
        final int effectiveUserId = mUserManager.getCredentialOwnerProfile(mUserId);
        final boolean isManagedProfile = UserManager.get(this).isManagedProfile(mUserId);
        // if the client app did not hand in a title and we are about to show the work challenge,
        // check whether there is a policy setting the organization name and use that as title
        if ((mTitle == null) && isManagedProfile) {
            mTitle = getTitleFromOrganizationName(mUserId);
        }


        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this);
        final LockPatternUtils lockPatternUtils = new LockPatternUtils(this);

        final Bundle bpBundle = new Bundle();
        bpBundle.putString(BiometricPrompt.KEY_TITLE, mTitle);
        bpBundle.putString(BiometricPrompt.KEY_DESCRIPTION, mDetails);
        bpBundle.putBoolean(BiometricPrompt.EXTRA_DISALLOW_BIOMETRICS_IF_POLICY_EXISTS,
                mCheckDevicePolicyManager);

        final @LockPatternUtils.CredentialType int credentialType = Utils.getCredentialType(
                mContext, effectiveUserId);
        if (mTitle == null) {
            bpBundle.putString(BiometricPrompt.KEY_DEVICE_CREDENTIAL_TITLE,
                    getTitleFromCredentialType(credentialType, isManagedProfile));
        }
        if (mDetails == null) {
            bpBundle.putString(BiometricPrompt.KEY_DEVICE_CREDENTIAL_SUBTITLE,
                    getDetailsFromCredentialType(credentialType, isManagedProfile));
        }

        boolean launchedBiometric = false;
        boolean launchedCDC = false;
        // If the target is a managed user and user key not unlocked yet, we will force unlock
        // tied profile so it will enable work mode and unlock managed profile, when personal
        // challenge is unlocked.
        if (frp) {
            launchedCDC = mChooseLockSettingsHelper.launchFrpConfirmationActivity(
                    0, mTitle, mDetails, alternateButton);
        } else if (isManagedProfile && isInternalActivity()
                && !lockPatternUtils.isSeparateProfileChallengeEnabled(mUserId)) {
            mCredentialMode = CREDENTIAL_MANAGED;
            if (isBiometricAllowed(effectiveUserId, mUserId)) {
                showBiometricPrompt(bpBundle);
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
                showBiometricPrompt(bpBundle);
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
            boolean isManagedProfile) {
        switch (credentialType) {
            case LockPatternUtils.CREDENTIAL_TYPE_PIN:
                return isManagedProfile
                        ? getString(R.string.lockpassword_confirm_your_work_pin_header)
                        : getString(R.string.lockpassword_confirm_your_pin_header);
            case LockPatternUtils.CREDENTIAL_TYPE_PATTERN:
                return isManagedProfile
                        ? getString(R.string.lockpassword_confirm_your_work_pattern_header)
                        : getString(R.string.lockpassword_confirm_your_pattern_header);
            case LockPatternUtils.CREDENTIAL_TYPE_PASSWORD:
                return isManagedProfile
                        ? getString(R.string.lockpassword_confirm_your_work_password_header)
                        : getString(R.string.lockpassword_confirm_your_password_header);
        }
        return null;
    }

    private String getDetailsFromCredentialType(@LockPatternUtils.CredentialType int credentialType,
            boolean isManagedProfile) {
        switch (credentialType) {
            case LockPatternUtils.CREDENTIAL_TYPE_PIN:
                return isManagedProfile
                        ? getString(R.string.lockpassword_confirm_your_pin_generic_profile)
                        : getString(R.string.lockpassword_confirm_your_pin_generic);
            case LockPatternUtils.CREDENTIAL_TYPE_PATTERN:
                return isManagedProfile
                        ? getString(R.string.lockpassword_confirm_your_pattern_generic_profile)
                        : getString(R.string.lockpassword_confirm_your_pattern_generic);
            case LockPatternUtils.CREDENTIAL_TYPE_PASSWORD:
                return isManagedProfile
                        ? getString(R.string.lockpassword_confirm_your_password_generic_profile)
                        : getString(R.string.lockpassword_confirm_your_password_generic);
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

    private void showBiometricPrompt(Bundle bundle) {
        mBiometricManager.setActiveUser(mUserId);

        mBiometricFragment = (BiometricFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_BIOMETRIC_FRAGMENT);
        boolean newFragment = false;

        if (mBiometricFragment == null) {
            mBiometricFragment = BiometricFragment.newInstance(bundle);
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
        // but dummy challenge value (0L). This will result in ConfirmLockPassword calling
        // verifyTiedProfileChallenge() (if it's a profile with unified challenge), due to the
        // difference between ConfirmLockPassword.startVerifyPassword() and
        // ConfirmLockPassword.startCheckPassword(). Calling verifyTiedProfileChallenge() here is
        // necessary when this is part of the turning on work profile flow, because it forces
        // unlocking the work profile even before the profile is running.
        // TODO: Remove the duplication of checkPassword and verifyPassword in ConfirmLockPassword,
        // LockPatternChecker and LockPatternUtils. verifyPassword should be the only API to use,
        // which optionally accepts a challenge.
        if (mCredentialMode == CREDENTIAL_MANAGED) {
            launched = mChooseLockSettingsHelper
                    .launchConfirmationActivityWithExternalAndChallenge(
                            0 /* request code */, null /* title */, mTitle, mDetails,
                            true /* isExternal */, 0L /* challenge */, mUserId);
        } else if (mCredentialMode == CREDENTIAL_NORMAL) {
            launched = mChooseLockSettingsHelper.launchConfirmationActivity(
                    0 /* request code */, null /* title */,
                    mTitle, mDetails, false /* returnCredentials */, true /* isExternal */,
                    mUserId);
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
