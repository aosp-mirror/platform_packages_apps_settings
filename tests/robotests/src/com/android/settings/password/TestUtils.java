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

package com.android.settings.password;

import android.app.KeyguardManager;
import android.app.RemoteLockscreenValidationResult;
import android.app.RemoteLockscreenValidationSession;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;

import com.android.security.SecureBox;
import com.android.settings.R;

import org.robolectric.Robolectric;

import java.security.NoSuchAlgorithmException;

public final class TestUtils {

    public static final String SERVICE_NAME = "SERVICE_NAME";
    public static final String PACKAGE_NAME = "PACKAGE_NAME";
    public static final ComponentName COMPONENT_NAME =
            new ComponentName(PACKAGE_NAME, SERVICE_NAME);
    public static final int VALID_REMAINING_ATTEMPTS = 5;
    public static final int NO_MORE_REMAINING_ATTEMPTS = 0;
    public static final int TIMEOUT_MS = 10000;
    public static final RemoteLockscreenValidationResult GUESS_VALID_RESULT =
            new RemoteLockscreenValidationResult.Builder()
                    .setResultCode(RemoteLockscreenValidationResult.RESULT_GUESS_VALID)
                    .build();
    public static final RemoteLockscreenValidationResult GUESS_INVALID_RESULT =
            new RemoteLockscreenValidationResult.Builder()
                    .setResultCode(RemoteLockscreenValidationResult.RESULT_GUESS_INVALID)
                    .build();
    public static final RemoteLockscreenValidationResult LOCKOUT_RESULT =
            new RemoteLockscreenValidationResult.Builder()
                    .setResultCode(RemoteLockscreenValidationResult.RESULT_LOCKOUT)
                    .setTimeoutMillis(TIMEOUT_MS)
                    .build();
    public static final RemoteLockscreenValidationResult NO_REMAINING_ATTEMPTS_RESULT =
            new RemoteLockscreenValidationResult.Builder()
                    .setResultCode(RemoteLockscreenValidationResult.RESULT_NO_REMAINING_ATTEMPTS)
                    .build();

    private TestUtils() {
    }

    public static PackageInfo createPackageInfoWithService(
            String packageName, String serviceName, String requiredServicePermission) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.name = serviceName;
        serviceInfo.applicationInfo = new ApplicationInfo();
        serviceInfo.permission = requiredServicePermission;

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.services = new ServiceInfo[]{serviceInfo};
        return packageInfo;
    }

    public static Intent createRemoteLockscreenValidationIntent(
            int lockscreenType, int remainingAttempts) throws Exception {
        return new Intent()
                .putExtra(ConfirmDeviceCredentialBaseFragment.IS_REMOTE_LOCKSCREEN_VALIDATION, true)
                .putExtra(KeyguardManager.EXTRA_REMOTE_LOCKSCREEN_VALIDATION_SESSION,
                        createRemoteLockscreenValidationSession(lockscreenType, remainingAttempts))
                .putExtra(Intent.EXTRA_COMPONENT_NAME, COMPONENT_NAME);
    }

    public static RemoteLockscreenValidationSession createRemoteLockscreenValidationSession(
            int lockscreenType, int remainingAttempts) throws NoSuchAlgorithmException {
        return new RemoteLockscreenValidationSession.Builder()
                .setLockType(lockscreenType)
                .setRemainingAttempts(remainingAttempts)
                .setSourcePublicKey(SecureBox.genKeyPair().getPublic().getEncoded())
                .build();
    }

    public static ConfirmDeviceCredentialBaseActivity buildConfirmDeviceCredentialBaseActivity(
            Class<? extends ConfirmDeviceCredentialBaseActivity> impl, Intent intent) {
        return Robolectric.buildActivity(impl, intent).setup().get();
    }

    public static ConfirmDeviceCredentialBaseFragment getConfirmDeviceCredentialBaseFragment(
            ConfirmDeviceCredentialBaseActivity activity) {
        return (ConfirmDeviceCredentialBaseFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.main_content);
    }

}
