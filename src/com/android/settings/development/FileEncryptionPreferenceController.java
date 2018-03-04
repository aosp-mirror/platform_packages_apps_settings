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
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.IStorageManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class FileEncryptionPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_CONVERT_FBE = "convert_to_file_encryption";
    private static final String KEY_STORAGE_MANAGER = "mount";

    @VisibleForTesting
    static final String FILE_ENCRYPTION_PROPERTY_KEY = "ro.crypto.type";

    private final IStorageManager mStorageManager;

    public FileEncryptionPreferenceController(Context context) {
        super(context);

        mStorageManager = getStorageManager();
    }

    @Override
    public boolean isAvailable() {
        if (mStorageManager == null) {
            return false;
        }

        try {
            return mStorageManager.isConvertibleToFBE();
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CONVERT_FBE;
    }

    @Override
    public void updateState(Preference preference) {
        if (!TextUtils.equals("file",
                SystemProperties.get(FILE_ENCRYPTION_PROPERTY_KEY, "none" /* default */))) {
            return;
        }

        mPreference.setEnabled(false);
        mPreference.setSummary(
                mContext.getResources().getString(R.string.convert_to_file_encryption_done));
    }

    private IStorageManager getStorageManager() {
        try {
            return IStorageManager.Stub.asInterface(
                    ServiceManager.getService(KEY_STORAGE_MANAGER));
        } catch (VerifyError e) {
            // Used for tests since Robolectric cannot initialize this class.
            return null;
        }
    }
}