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

import static com.android.internal.app.SetScreenLockDialogActivity.LAUNCH_REASON_PRIVATE_SPACE_SETTINGS_ACCESS;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.PendingIntent;
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
import com.android.internal.app.SetScreenLockDialogActivity;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.transition.SettingsTransitionHelper;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * This class represents an activity responsible for user authentication before starting the private
 * space setup flow or accessing the private space settings page if already created. Also prompts
 * user to set a device lock if not set with an alert dialog. This can be launched using the intent
 * com.android.settings.action.PRIVATE_SPACE_SETUP_FLOW.
 */
public class PrivateSpaceAuthenticationActivity extends FragmentActivity {
    private static final String TAG = "PrivateSpaceAuthCheck";
    public static final String EXTRA_SHOW_PRIVATE_SPACE_UNLOCKED =
            "extra_show_private_space_unlocked";
    private PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    private KeyguardManager mKeyguardManager;

    private final ActivityResultLauncher<Intent> mSetDeviceLock =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onSetDeviceLockResult);
    private final ActivityResultLauncher<Intent> mVerifyDeviceLock =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), this::onVerifyDeviceLock);

    static class Injector {
        PrivateSpaceMaintainer injectPrivateSpaceMaintainer(Context context) {
            return PrivateSpaceMaintainer.getInstance(context);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Flags.allowPrivateProfile()) {
            ThemeHelper.trySetDynamicColor(this);
            mPrivateSpaceMaintainer =
                    new Injector().injectPrivateSpaceMaintainer(getApplicationContext());
            if (getKeyguardManager().isDeviceSecure()) {
                if (savedInstanceState == null) {
                    if (mPrivateSpaceMaintainer.doesPrivateSpaceExist()) {
                        unlockAndLaunchPrivateSpaceSettings(this);
                    } else {
                        authenticatePrivateSpaceEntry();
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
            unlockAndLaunchPrivateSpaceSettings(context);
        } else {
            startActivity(new Intent(context, PrivateSpaceSetupActivity.class));
            finish();
        }
    }

    @VisibleForTesting
    public void setPrivateSpaceMaintainer(Injector injector) {
        mPrivateSpaceMaintainer = injector.injectPrivateSpaceMaintainer(this);
    }

    private void promptToSetDeviceLock() {
        Log.d(TAG, "Show prompt to set device lock before using private space feature");
        if (android.multiuser.Flags.showSetScreenLockDialog()) {
            Intent setScreenLockPromptIntent =
                    SetScreenLockDialogActivity
                            .createBaseIntent(LAUNCH_REASON_PRIVATE_SPACE_SETTINGS_ACCESS);
            startActivity(setScreenLockPromptIntent);
            finish();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.no_device_lock_title)
                    .setMessage(R.string.no_device_lock_summary)
                    .setPositiveButton(
                            R.string.no_device_lock_action_label,
                            (DialogInterface dialog, int which) -> {
                                Log.d(TAG, "Start activity to set new device lock");
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
    }

    private KeyguardManager getKeyguardManager() {
        if (mKeyguardManager == null) {
            mKeyguardManager = getSystemService(KeyguardManager.class);
        }
        return mKeyguardManager;
    }

    private void onSetDeviceLockResult(@Nullable ActivityResult result) {
        if (result != null) {
            if (getKeyguardManager().isDeviceSecure()) {
                onLockAuthentication(this);
            } else {
                finish();
            }
        }
    }

    private void onVerifyDeviceLock(@Nullable ActivityResult result) {
        if (result != null && result.getResultCode() == RESULT_OK) {
            onLockAuthentication(this);
        } else {
            finish();
        }
    }

    private void unlockAndLaunchPrivateSpaceSettings(Context context) {
        SubSettingLauncher privateSpaceSettings =
                new SubSettingLauncher(context)
                        .setDestination(PrivateSpaceDashboardFragment.class.getName())
                        .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                        .setSourceMetricsCategory(SettingsEnums.PRIVATE_SPACE_SETTINGS);
        if (mPrivateSpaceMaintainer.isPrivateSpaceLocked()) {
            ActivityOptions options =
                    ActivityOptions.makeBasic()
                            .setPendingIntentCreatorBackgroundActivityStartMode(
                                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            mPrivateSpaceMaintainer.unlockPrivateSpace(
                    PendingIntent.getActivity(
                                    context, /* requestCode */
                                    0,
                                    privateSpaceSettings
                                            .toIntent()
                                            .putExtra(EXTRA_SHOW_PRIVATE_SPACE_UNLOCKED, true),
                                    PendingIntent.FLAG_IMMUTABLE,
                                    options.toBundle())
                            .getIntentSender());
        } else {
            Log.i(TAG, "Launch private space settings");
            privateSpaceSettings.launch();
        }
        finish();
    }

    private void authenticatePrivateSpaceEntry() {
        Intent credentialIntent = mPrivateSpaceMaintainer.getPrivateProfileLockCredentialIntent();
        if (credentialIntent != null) {
            mVerifyDeviceLock.launch(credentialIntent);
        } else {
            Log.e(TAG, "verifyCredentialIntent is null even though device lock is set");
            finish();
        }
    }
}
