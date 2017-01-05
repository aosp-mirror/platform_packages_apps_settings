/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceController;

/**
 * StorageItemPreferenceController handles the updating of a single storage preference line item.
 */
public class StorageItemPreferenceController extends PreferenceController {
    private static final long NOT_YET_SET = -1;
    private final String mKey;
    private long mStorageSize;

    public StorageItemPreferenceController(Context context, String key) {
        super(context);
        mKey = key;
        mStorageSize = NOT_YET_SET;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null || mStorageSize == NOT_YET_SET) {
            return;
        }

        StorageItemPreferenceAlternate summary = (StorageItemPreferenceAlternate) preference;
        summary.setStorageSize(mStorageSize);
    }

    /**
     * Sets the amount of bytes used by this storage item.
     */
    public void setStorageSize(long size) {
        mStorageSize = size;
    }
}
