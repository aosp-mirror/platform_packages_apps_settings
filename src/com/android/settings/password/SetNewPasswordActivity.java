/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.Manifest.permission.REQUEST_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;
import static android.app.admin.DevicePolicyManager.EXTRA_DEVICE_PASSWORD_REQUIREMENT_ONLY;
import static android.app.admin.DevicePolicyManager.EXTRA_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;

import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CALLER_APP_NAME;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_IS_CALLING_APP_ADMIN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import android.app.Activity;
import android.app.RemoteServiceException.MissingRequestPasswordComplexityPermissionException;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManager.PasswordComplexity;
import android.app.admin.PasswordMetrics;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.List;

/**
 * Trampolines {@link DevicePolicyManager#ACTION_SET_NEW_PASSWORD} and
 * {@link DevicePolicyManager#ACTION_SET_NEW_PARENT_PROFILE_PASSWORD} intent to the appropriate UI
 * activity for handling set new password.
 */
public class SetNewPasswordActivity extends Activity implements SetNewPasswordController.Ui {
    private static final String TAG = "SetNewPasswordActivity";
    private String mNewPasswordAction;
    private SetNewPasswordController mSetNewPasswordController;

    /**
     * From intent extra {@link DevicePolicyManager#EXTRA_PASSWORD_COMPLEXITY}.
     *
     * <p>This is used only if caller has the required permission and activity is launched by
     * {@link DevicePolicyManager#ACTION_SET_NEW_PASSWORD}.
     */
    private @PasswordComplexity int mRequestedMinComplexity = PASSWORD_COMPLEXITY_NONE;

    private boolean mDevicePasswordRequirementOnly = false;

    /**
     * Label of the app which launches this activity.
     *
     * <p>Value would be {@code null} if launched from settings app.
     */
    private String mCallerAppName = null;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        final Intent intent = getIntent();

        mNewPasswordAction = intent.getAction();
        if (!ACTION_SET_NEW_PASSWORD.equals(mNewPasswordAction)
                && !ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(mNewPasswordAction)) {
            Log.e(TAG, "Unexpected action to launch this activity");
            finish();
            return;
        }
        logSetNewPasswordIntent();

        final IBinder activityToken = getActivityToken();
        mCallerAppName = (String) PasswordUtils.getCallingAppLabel(this, activityToken);
        if (ACTION_SET_NEW_PASSWORD.equals(mNewPasswordAction)
                && intent.hasExtra(EXTRA_PASSWORD_COMPLEXITY)) {
            final boolean hasPermission = PasswordUtils.isCallingAppPermitted(
                    this, activityToken, REQUEST_PASSWORD_COMPLEXITY);
            if (hasPermission) {
                mRequestedMinComplexity =
                        PasswordMetrics.sanitizeComplexityLevel(intent.getIntExtra(
                                EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_NONE));
            } else {
                PasswordUtils.crashCallingApplication(activityToken,
                        "Must have permission "
                                + REQUEST_PASSWORD_COMPLEXITY + " to use extra "
                                + EXTRA_PASSWORD_COMPLEXITY,
                        MissingRequestPasswordComplexityPermissionException.TYPE_ID);
                finish();
                return;
            }
        }
        if (ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(mNewPasswordAction)) {
            mDevicePasswordRequirementOnly = intent.getBooleanExtra(
                    EXTRA_DEVICE_PASSWORD_REQUIREMENT_ONLY, false);
            Log.i(TAG, String.format("DEVICE_PASSWORD_REQUIREMENT_ONLY: %b",
                    mDevicePasswordRequirementOnly));
        }
        mSetNewPasswordController = SetNewPasswordController.create(
                this, this, intent, activityToken);
        mSetNewPasswordController.dispatchSetNewPasswordIntent();
    }

    @Override
    public void launchChooseLock(Bundle chooseLockFingerprintExtras) {
        final boolean isInSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
        Intent intent = isInSetupWizard ? new Intent(this, SetupChooseLockGeneric.class)
                : new Intent(this, ChooseLockGeneric.class);
        intent.setAction(mNewPasswordAction);
        intent.putExtras(chooseLockFingerprintExtras);
        intent.putExtra(EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE,
                getIntent().getIntExtra(EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE, -1));
        intent.putExtra(EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION,
                getIntent().getIntExtra(EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION, -1));
        if (mCallerAppName != null) {
            intent.putExtra(EXTRA_KEY_CALLER_APP_NAME, mCallerAppName);
        }
        if (mRequestedMinComplexity != PASSWORD_COMPLEXITY_NONE) {
            intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, mRequestedMinComplexity);
        }
        if (isCallingAppAdmin()) {
            intent.putExtra(EXTRA_KEY_IS_CALLING_APP_ADMIN, true);
        }
        intent.putExtra(EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY, mDevicePasswordRequirementOnly);
        // Copy the setup wizard intent extra to the intent.
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
        startActivity(intent);
        finish();
    }

    private boolean isCallingAppAdmin() {
        DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);
        String callingAppPackageName = PasswordUtils.getCallingAppPackageName(getActivityToken());
        List<ComponentName> admins = devicePolicyManager.getActiveAdmins();
        if (admins == null) {
            return false;
        }
        for (ComponentName componentName : admins) {
            if (componentName.getPackageName().equals(callingAppPackageName)) {
                return true;
            }
        }
        return false;
    }

    private void logSetNewPasswordIntent() {
        final String callingAppPackageName =
                PasswordUtils.getCallingAppPackageName(getActivityToken());

        // use int min value to denote absence of EXTRA_PASSWORD_COMPLEXITY
        final int extraPasswordComplexity = getIntent().hasExtra(EXTRA_PASSWORD_COMPLEXITY)
                ? getIntent().getIntExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_NONE)
                : Integer.MIN_VALUE;

        final boolean extraDevicePasswordRequirementOnly = getIntent().getBooleanExtra(
                EXTRA_DEVICE_PASSWORD_REQUIREMENT_ONLY, false);

        // Use 30th bit to encode extraDevicePasswordRequirementOnly, since the top bit (31th bit)
        // encodes whether EXTRA_PASSWORD_COMPLEXITY has been absent.
        final int logValue = extraPasswordComplexity
                | (extraDevicePasswordRequirementOnly ? 1 << 30 : 0);
        // this activity is launched by either ACTION_SET_NEW_PASSWORD or
        // ACTION_SET_NEW_PARENT_PROFILE_PASSWORD
        final int action = ACTION_SET_NEW_PASSWORD.equals(mNewPasswordAction)
                ? SettingsEnums.ACTION_SET_NEW_PASSWORD
                : SettingsEnums.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD;

        final MetricsFeatureProvider metricsProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        metricsProvider.action(
                metricsProvider.getAttribution(this),
                action,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                callingAppPackageName,
                logValue);
    }
}
