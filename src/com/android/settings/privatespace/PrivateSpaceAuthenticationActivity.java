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

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Flags;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.transition.SettingsTransitionHelper;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Prompts user to set a device lock if not set with an alert dialog.
 * If a lock is already set then first authenticates user before displaying private space settings
 * page.
 */
public class PrivateSpaceAuthenticationActivity extends FragmentActivity {
    private static final String TAG = "PrivateSpaceAuthCheck";
    private PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    private KeyguardManager mKeyguardManager;

    private final ActivityResultLauncher<Intent> mSetDeviceLock =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::onSetDeviceLockResult);
    private final ActivityResultLauncher<Intent> mVerifyDeviceLock =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::onVerifyDeviceLock);

    static class Injector {
        PrivateSpaceMaintainer injectPrivateSpaceMaintainer(Context context) {
            return PrivateSpaceMaintainer.getInstance(context);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Flags.allowPrivateProfile()) {
            super.onCreate(savedInstanceState);
            ThemeHelper.trySetDynamicColor(this);
            mPrivateSpaceMaintainer = new Injector().injectPrivateSpaceMaintainer(
                    getApplicationContext());
            if (getKeyguardManager().isDeviceSecure()) {
                if (savedInstanceState == null) {
                    Intent credentialIntent =
                            mPrivateSpaceMaintainer.getPrivateProfileLockCredentialIntent();
                    if (credentialIntent != null) {
                        mVerifyDeviceLock.launch(credentialIntent);
                    } else {
                        Log.e(TAG, "verifyCredentialIntent is null even though device lock is set");
                        finish();
                    }
                }
            } else {
                promptToSetDeviceLock();
            }
        } else {
            finish();
        }
    }

    /** Starts private space setup flow or the PS settings page on device lock authentication */
    @VisibleForTesting
    public void onLockAuthentication(Context context) {
        if (mPrivateSpaceMaintainer.doesPrivateSpaceExist()) {
            new SubSettingLauncher(context)
                    .setDestination(PrivateSpaceDashboardFragment.class.getName())
                    .setTransitionType(
                            SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                    .setSourceMetricsCategory(SettingsEnums.PRIVATE_SPACE_SETTINGS)
                    .launch();
        } else {
            startActivity(new Intent(context, PrivateSpaceSetupActivity.class));
        }
    }

    @VisibleForTesting
    public void setPrivateSpaceMaintainer(Injector injector) {
        mPrivateSpaceMaintainer = injector.injectPrivateSpaceMaintainer(this);
    }

    private void promptToSetDeviceLock() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_device_lock_title)
                .setMessage(R.string.no_device_lock_summary)
                .setPositiveButton(
                        R.string.no_device_lock_action_label,
                        (DialogInterface dialog, int which) -> {
                            mSetDeviceLock.launch(new Intent(ACTION_SET_NEW_PASSWORD));
                        })
                .setNegativeButton(
                        R.string.no_device_lock_cancel,
                        (DialogInterface dialog, int which) -> finish())
                .setOnCancelListener(
                        (DialogInterface dialog) -> {
                            finish();
                        })
                .show();
    }

    private KeyguardManager getKeyguardManager() {
        if (mKeyguardManager == null) {
            mKeyguardManager = getSystemService(KeyguardManager.class);
        }
        return  mKeyguardManager;
    }

    private void onSetDeviceLockResult(@Nullable ActivityResult result) {
        if (result != null) {
            if (getKeyguardManager().isDeviceSecure()) {
                onLockAuthentication(this);
            }
            finish();
        }
    }

    private void onVerifyDeviceLock(@Nullable ActivityResult result) {
        if (result != null && result.getResultCode() == RESULT_OK) {
            onLockAuthentication(this);
        }
        finish();
    }
}
