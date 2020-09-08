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
package com.android.settings.core;

import android.text.TextUtils;
import android.util.Log;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.List;

/**
 * A controller mixin that adds mobile settings specific functionality
 * TODO (b/69808530) Replace with BasePreferenceController.
 */
public interface PreferenceControllerMixin {

    String TAG = "PrefControllerMixin";

    /**
     * Updates non-indexable keys for search provider.
     *
     * Called by SearchIndexProvider#getNonIndexableKeys
     */
    default void updateNonIndexableKeys(List<String> keys) {
        if (this instanceof AbstractPreferenceController) {
            if (!((AbstractPreferenceController) this).isAvailable()) {
                final String key = ((AbstractPreferenceController) this).getPreferenceKey();
                if (TextUtils.isEmpty(key)) {
                    Log.w(TAG,
                            "Skipping updateNonIndexableKeys due to empty key " + toString());
                    return;
                }
                if (keys.contains(key)) {
                    Log.w(TAG, "Skipping updateNonIndexableKeys, key already in list. "
                            + toString());
                    return;
                }
                keys.add(key);
            }
        }
    }

    /**
     * Updates raw data for search provider.
     *
     * Called by SearchIndexProvider#getRawDataToIndex
     */
    default void updateRawDataToIndex(List<SearchIndexableRaw> rawData) {
    }

    /**
     * Updates dynamic raw data for search provider.
     *
     * Called by SearchIndexProvider#getDynamicRawDataToIndex
     */
    default void updateDynamicRawDataToIndex(List<SearchIndexableRaw> rawData) {
    }
}
