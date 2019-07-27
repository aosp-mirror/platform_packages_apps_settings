/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.password;

import android.app.KeyguardManager;
import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.IBiometricConfirmDeviceCredentialCallback;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;

public abstract class ConfirmDeviceCredentialBaseActivity extends SettingsActivity {

    private static final String STATE_IS_KEYGUARD_LOCKED = "STATE_IS_KEYGUARD_LOCKED";
    private static final String TAG = "ConfirmDeviceCredentialBaseActivity";

    enum ConfirmCredentialTheme {
        NORMAL,
        DARK,  // TODO(yukl): Clean up DARK theme, as it should no longer be used
        WORK
    }

    private boolean mRestoring;
    private boolean mEnterAnimationPending;
    private boolean mFirstTimeVisible = true;
    private boolean mIsKeyguardLocked = false;
    private ConfirmCredentialTheme mConfirmCredentialTheme;
    private BiometricManager mBiometricManager;

    // TODO(b/123378871): Remove when moved.
    private final IBiometricConfirmDeviceCredentialCallback mCancelCallback
            = new IBiometricConfirmDeviceCredentialCallback.Stub() {
        @Override
        public void cancel() {
            finish();
        }
    };

    private boolean isInternalActivity() {
        return (this instanceof ConfirmLockPassword.InternalActivity)
                || (this instanceof ConfirmLockPattern.InternalActivity);
    }

    @Override
    protected void onCreate(Bundle savedState) {
        final int credentialOwnerUserId;
        try {
            credentialOwnerUserId = Utils.getCredentialOwnerUserId(this,
                    Utils.getUserIdFromBundle(this, getIntent().getExtras(), isInternalActivity()));
        } catch (SecurityException e) {
            Log.e(TAG, "Invalid user Id supplied", e);
            finish();
            return;
        }
        if (UserManager.get(this).isManagedProfile(credentialOwnerUserId)) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsWork);
            mConfirmCredentialTheme = ConfirmCredentialTheme.WORK;
        } else if (getIntent().getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.DARK_THEME, false)) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsDark);
            mConfirmCredentialTheme = ConfirmCredentialTheme.DARK;
        } else {
            setTheme(SetupWizardUtils.getTheme(getIntent()));
            mConfirmCredentialTheme = ConfirmCredentialTheme.NORMAL;
        }
        super.onCreate(savedState);

        mBiometricManager = getSystemService(BiometricManager.class);
        mBiometricManager.registerCancellationCallback(mCancelCallback);

        if (mConfirmCredentialTheme == ConfirmCredentialTheme.NORMAL) {
            // Prevent the content parent from consuming the window insets because GlifLayout uses
            // it to show the status bar background.
            findViewById(R.id.content_parent).setFitsSystemWindows(false);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        mIsKeyguardLocked = savedState == null
                ? getSystemService(KeyguardManager.class).isKeyguardLocked()
                : savedState.getBoolean(STATE_IS_KEYGUARD_LOCKED, false);
        // If the activity is launched, not due to config change, when keyguard is locked and the
        // flag is set, assume it's launched on top of keyguard on purpose.
        // TODO: Don't abuse SHOW_WHEN_LOCKED and don't check isKeyguardLocked.
        // Set extra SHOW_WHEN_LOCKED and WindowManager FLAG_SHOW_WHEN_LOCKED only if it's
        // truly on top of keyguard on purpose
        if (mIsKeyguardLocked && getIntent().getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        CharSequence msg = getIntent().getStringExtra(
                ConfirmDeviceCredentialBaseFragment.TITLE_TEXT);
        setTitle(msg);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        mRestoring = savedState != null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_KEYGUARD_LOCKED, mIsKeyguardLocked);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isChangingConfigurations() && !mRestoring
                && mConfirmCredentialTheme == ConfirmCredentialTheme.DARK && mFirstTimeVisible) {
            mFirstTimeVisible = false;
            prepareEnterAnimation();
            mEnterAnimationPending = true;
        }
    }

    private ConfirmDeviceCredentialBaseFragment getFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_content);
        if (fragment != null && fragment instanceof ConfirmDeviceCredentialBaseFragment) {
            return (ConfirmDeviceCredentialBaseFragment) fragment;
        }
        return null;
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if (mEnterAnimationPending) {
            startEnterAnimation();
            mEnterAnimationPending = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO(b/123378871): Remove when moved.
        if (!isChangingConfigurations()) {
            mBiometricManager.onConfirmDeviceCredentialError(
                    BiometricConstants.BIOMETRIC_ERROR_USER_CANCELED,
                    getString(com.android.internal.R.string.biometric_error_user_canceled));
            if (getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOREGROUND_ONLY, false)) {
                finish();
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (getIntent().getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.USE_FADE_ANIMATION, false)) {
            overridePendingTransition(0, R.anim.confirm_credential_biometric_transition_exit);
        }
    }

    public void prepareEnterAnimation() {
        getFragment().prepareEnterAnimation();
    }

    public void startEnterAnimation() {
        getFragment().startEnterAnimation();
    }

    public ConfirmCredentialTheme getConfirmCredentialTheme() {
        return mConfirmCredentialTheme;
    }
}
