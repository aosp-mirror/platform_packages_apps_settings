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

import com.android.settings.search.ResultPayload;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

/**
 * A controller mixin that adds mobile settings specific functionality
 */
public interface PreferenceControllerMixin {

    /**
     * Updates non-indexable keys for search provider.
     *
     * Called by SearchIndexProvider#getNonIndexableKeys
     */
    default void updateNonIndexableKeys(List<String> keys) {
        if (this instanceof AbstractPreferenceController) {
            if (!((AbstractPreferenceController) this).isAvailable()) {
                keys.add(((AbstractPreferenceController) this).getPreferenceKey());
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
     * @return the {@link ResultPayload} corresponding to the search result type for the preference.
     */
    default ResultPayload getResultPayload() {
        return null;
    }
}
