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

package com.android.settings.privatespace;

import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;
import static android.app.admin.DevicePolicyManager.EXTRA_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_LOW;

import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FINGERPRINT_ENROLLMENT_ONLY;
import static com.android.settings.privatespace.PrivateSpaceSetupActivity.ACCOUNT_LOGIN_ACTION;
import static com.android.settings.privatespace.PrivateSpaceSetupActivity.EXTRA_ACTION_TYPE;
import static com.android.settings.privatespace.PrivateSpaceSetupActivity.SET_LOCK_ACTION;

import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Activity that is started as private profile user that helps to set private profile lock or add an
 * account on the private profile.
 */
public class PrivateProfileContextHelperActivity extends FragmentActivity {
    private static final String TAG = "PrivateSpaceHelperAct";
    private final ActivityResultLauncher<Intent> mAddAccountToPrivateProfile =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), this::onAccountAdded);
    private final ActivityResultLauncher<Intent> mSetNewPrivateProfileLock =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onSetNewProfileLockActionCompleted);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!android.os.Flags.allowPrivateProfile()
                || !android.multiuser.Flags.enablePrivateSpaceFeatures()) {
            return;
        }
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            int action = getIntent().getIntExtra(EXTRA_ACTION_TYPE, -1);
            if (action == ACCOUNT_LOGIN_ACTION) {
                setContentView(R.layout.private_space_wait_screen);
                PrivateSpaceLoginFeatureProvider privateSpaceLoginFeatureProvider =
                        FeatureFactory.getFeatureFactory().getPrivateSpaceLoginFeatureProvider();
                UserHandle userHandle =
                        PrivateSpaceMaintainer.getInstance(this).getPrivateProfileHandle();
                if (userHandle != null) {
                    if (!privateSpaceLoginFeatureProvider.initiateAccountLogin(
                            createContextAsUser(userHandle, 0), mAddAccountToPrivateProfile)) {
                        setResult(RESULT_OK);
                        finish();
                    }
                } else {
                    Log.w(TAG, "Private profile user handle is null");
                    setResult(RESULT_CANCELED);
                    finish();
                }
            } else if (action == SET_LOCK_ACTION) {
                createPrivateSpaceLock();
            }
        }
    }

    private void createPrivateSpaceLock() {
        final Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_LOW);
        intent.putExtra(EXTRA_KEY_FINGERPRINT_ENROLLMENT_ONLY, true);
        intent.putExtra(
                EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE, R.string.private_space_lock_setup_title);
        intent.putExtra(
                EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION,
                R.string.private_space_lock_setup_description);
        mSetNewPrivateProfileLock.launch(intent);
    }

    private void onAccountAdded(@Nullable ActivityResult result) {
        if (result == null) {
            Log.i(TAG, "private space account login result null");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        final int resultCode = result.getResultCode();
        if (resultCode == RESULT_OK) {
            Log.i(TAG, "private space account login success");
        } else if (resultCode == RESULT_FIRST_USER) {
            Log.i(TAG, "private space account login skipped");
        } else {
            Log.i(TAG, "private space account login failed");
        }
        setResult(
                resultCode == RESULT_OK || resultCode == RESULT_FIRST_USER
                        ? RESULT_OK
                        : RESULT_CANCELED);
        finish();
    }

    private void onSetNewProfileLockActionCompleted(@Nullable ActivityResult result) {
        LockPatternUtils lockPatternUtils =
                FeatureFactory.getFeatureFactory()
                        .getSecurityFeatureProvider()
                        .getLockPatternUtils(this);
        if (result != null && lockPatternUtils.isSeparateProfileChallengeEnabled(getUserId())) {
            Log.i(TAG, "separate private space lock setup success");
            setResult(RESULT_OK);
        } else {
            Log.i(TAG, "separate private space lock not setup");
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
