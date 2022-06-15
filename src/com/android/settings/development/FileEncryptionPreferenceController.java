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
import android.os.storage.IStorageManager;
import android.text.TextUtils;
import android.sysprop.CryptoProperties;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class FileEncryptionPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_CONVERT_FBE = "convert_to_file_encryption";
    private static final String KEY_STORAGE_MANAGER = "mount";

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
        if (CryptoProperties.type().orElse(CryptoProperties.type_values.NONE) !=
            CryptoProperties.type_values.FILE) {
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