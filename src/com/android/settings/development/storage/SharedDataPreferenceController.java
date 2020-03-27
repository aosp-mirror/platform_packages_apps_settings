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

import androidx.preference.Preference;

import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class SharedDataPreferenceController extends DeveloperOptionsPreferenceController {

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
    public boolean isAvailable() {
        return mBlobStoreManager != null;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setEnabled(mBlobStoreManager != null);
        // TODO: update summary to indicate why this preference isn't available
    }
}
