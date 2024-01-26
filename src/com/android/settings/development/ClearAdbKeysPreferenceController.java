/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.debug.IAdbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.sysprop.AdbProperties;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ClearAdbKeysPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "ClearAdbPrefCtrl";
    private static final String CLEAR_ADB_KEYS = "clear_adb_keys";

    private final IAdbManager mAdbManager;
    private final DevelopmentSettingsDashboardFragment mFragment;

    public ClearAdbKeysPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);

        mFragment = fragment;
        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(Context.ADB_SERVICE));
    }

    @Override
    public boolean isAvailable() {
        // If the build is insecure (any -user build, 'ro.adb.secure=0'), adbd does not
        // requests/store authorizations. There is no need for a "revoke authorizations"
        // button.
        return AdbProperties.secure().orElse(false);
    }

    @Override
    public String getPreferenceKey() {
        return CLEAR_ADB_KEYS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        if (mPreference != null && !isAdminUser()) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            ClearAdbKeysWarningDialog.show(mFragment);
            return true;
        }
        return false;
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isAdminUser()) {
            mPreference.setEnabled(true);
        }
    }

    public void onClearAdbKeysConfirmed() {
        try {
            mAdbManager.clearDebuggingKeys();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to clear adb keys", e);
        }
    }

    @VisibleForTesting
    boolean isAdminUser() {
        return ((UserManager) mContext.getSystemService(Context.USER_SERVICE)).isAdminUser();
    }
}
