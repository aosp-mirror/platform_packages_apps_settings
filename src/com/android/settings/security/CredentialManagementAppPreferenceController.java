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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller that shows and updates the credential management app summary.
 */
public class CredentialManagementAppPreferenceController extends BasePreferenceController {

    private static final String TAG = "CredentialManagementApp";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final PackageManager mPackageManager;
    private boolean mHasCredentialManagerPackage;
    private String mCredentialManagerPackageName;

    public CredentialManagementAppPreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        mExecutor.execute(() -> {
            try {
                IKeyChainService service = KeyChain.bind(mContext).getService();
                mHasCredentialManagerPackage = service.hasCredentialManagementApp();
                mCredentialManagerPackageName = service.getCredentialManagementAppPackageName();
            } catch (InterruptedException | RemoteException e) {
                Log.e(TAG, "Unable to display credential management app preference");
            }
            mHandler.post(() -> displayPreference(preference));
        });
    }

    @VisibleForTesting
    void displayPreference(Preference preference) {
        if (mHasCredentialManagerPackage) {
            preference.setEnabled(true);
            try {
                ApplicationInfo applicationInfo =
                        mPackageManager.getApplicationInfo(mCredentialManagerPackageName, 0);
                preference.setSummary(applicationInfo.loadLabel(mPackageManager));
            } catch (PackageManager.NameNotFoundException e) {
                preference.setSummary(mCredentialManagerPackageName);
            }
        } else {
            preference.setEnabled(false);
            preference.setSummary(R.string.no_certificate_management_app);
        }
    }
}
