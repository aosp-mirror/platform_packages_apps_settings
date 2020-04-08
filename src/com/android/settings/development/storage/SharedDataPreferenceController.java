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

package com.android.settings.development.storage;

import android.app.blob.BlobStoreManager;
import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.io.IOException;

public class SharedDataPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {
    private static final String TAG = "SharedDataPrefCtrl";
    private static final String SHARED_DATA = "shared_data";

    private BlobStoreManager mBlobStoreManager;

    public SharedDataPreferenceController(Context context) {
        super(context);
        mBlobStoreManager = (BlobStoreManager) context.getSystemService(BlobStoreManager.class);
    }

    @Override
    public String getPreferenceKey() {
        return SHARED_DATA;
    }

    @Override
    public void updateState(Preference preference) {
        try {
            final boolean showPref = mBlobStoreManager != null
                    && !mBlobStoreManager.queryBlobsForUser(UserHandle.CURRENT).isEmpty();
            preference.setEnabled(showPref);
            preference.setSummary(showPref ? R.string.shared_data_summary
                                           : R.string.shared_data_no_blobs_text);
        } catch (IOException e) {
            Log.e(TAG, "Unable to fetch blobs for current user: " + e.getMessage());
            preference.setEnabled(false);
            preference.setSummary(R.string.shared_data_no_blobs_text);
        }
    }
}
