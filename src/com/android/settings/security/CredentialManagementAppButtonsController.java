/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.security;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyEventLogger;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.stats.devicepolicy.DevicePolicyEnums;
import android.util.Log;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.ActionButtonsPreference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller that shows the remove button of the credential management app, which allows the user
 * to remove the credential management app and its certificates.
 */
public class CredentialManagementAppButtonsController extends BasePreferenceController {

    private static final String TAG = "CredentialManagementApp";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final PackageManager mPackageManager;
    private final AppOpsManager mAppOpsManager;
    private boolean mHasCredentialManagerPackage;
    private String mCredentialManagerPackageName;

    public CredentialManagementAppButtonsController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mExecutor.execute(() -> {
            try {
                IKeyChainService service = KeyChain.bind(mContext).getService();
                mHasCredentialManagerPackage = service.hasCredentialManagementApp();
                mCredentialManagerPackageName = service.getCredentialManagementAppPackageName();
            } catch (InterruptedException | RemoteException e) {
                Log.e(TAG, "Unable to display credential management app buttons");
            }
            mHandler.post(() -> displayButtons(screen));
        });
    }

    private void displayButtons(PreferenceScreen screen) {
        if (mHasCredentialManagerPackage) {
            ((ActionButtonsPreference) screen.findPreference(getPreferenceKey()))
                    .setButton1Text(R.string.remove_credential_management_app)
                    .setButton1Icon(R.drawable.ic_undo_24)
                    .setButton1OnClickListener(view -> removeCredentialManagementApp());
        }
    }

    private void removeCredentialManagementApp() {
        try {
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(
                    mCredentialManagerPackageName, 0);
            mAppOpsManager.setMode(AppOpsManager.OP_MANAGE_CREDENTIALS,
                    appInfo.uid, mCredentialManagerPackageName, AppOpsManager.MODE_DEFAULT);
            mExecutor.execute(() -> {
                try {
                    IKeyChainService service = KeyChain.bind(mContext).getService();
                    service.removeCredentialManagementApp();
                    DevicePolicyEventLogger
                            .createEvent(DevicePolicyEnums.CREDENTIAL_MANAGEMENT_APP_REMOVED)
                            .write();
                } catch (InterruptedException | RemoteException e) {
                    Log.e(TAG, "Unable to remove the credential management app");
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to remove the credential management app");
        }
    }
}
