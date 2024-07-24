/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.backup;

import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class AutoRestorePreferenceController extends TogglePreferenceController {
    private static final String TAG = "AutoRestorePrefCtrler";

    private PrivacySettingsConfigData mPSCD;
    private Preference mPreference;

    public AutoRestorePreferenceController(Context context, String key) {
        super(context, key);
        mPSCD = PrivacySettingsConfigData.getInstance();
    }

    @Override
    public int getAvailabilityStatus() {
        if (!PrivacySettingsUtils.isAdminUser(mContext)) {
            return DISABLED_FOR_USER;
        }
        if (PrivacySettingsUtils.isInvisibleKey(mContext, PrivacySettingsUtils.AUTO_RESTORE)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mPreference = preference;
        preference.setEnabled(mPSCD.isBackupEnabled());
    }

    @Override
    public boolean isChecked() {
        final ContentResolver res = mContext.getContentResolver();

        return Settings.Secure.getInt(res,
                Settings.Secure.BACKUP_AUTO_RESTORE, 1) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final boolean nextValue = isChecked;
        boolean result = false;

        final IBackupManager backupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));

        try {
            backupManager.setAutoRestore(nextValue);
            result = true;
        } catch (RemoteException e) {
            ((TwoStatePreference) mPreference).setChecked(!nextValue);
            Log.e(TAG, "Error can't set setAutoRestore", e);
        }

        return result;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}