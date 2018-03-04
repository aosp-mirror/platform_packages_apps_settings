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
import android.hardware.usb.IUsbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ClearAdbKeysPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "ClearAdbPrefCtrl";
    private static final String CLEAR_ADB_KEYS = "clear_adb_keys";

    @VisibleForTesting
    static final String RO_ADB_SECURE_PROPERTY_KEY = "ro.adb.secure";

    private final IUsbManager mUsbManager;
    private final DevelopmentSettingsDashboardFragment mFragment;

    public ClearAdbKeysPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);

        mFragment = fragment;
        mUsbManager = IUsbManager.Stub.asInterface(ServiceManager.getService(Context.USB_SERVICE));
    }

    @Override
    public boolean isAvailable() {
        return SystemProperties.getBoolean(RO_ADB_SECURE_PROPERTY_KEY, false /* default */);
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
            mUsbManager.clearUsbDebuggingKeys();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to clear adb keys", e);
        }
    }

    @VisibleForTesting
    boolean isAdminUser() {
        return ((UserManager) mContext.getSystemService(Context.USER_SERVICE)).isAdminUser();
    }
}
