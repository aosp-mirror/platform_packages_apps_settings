/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settingslib.search.SearchIndexableRaw;

import java.util.List;

/**
 * Provider for Accessibility Search related features.
 */
public interface AccessibilitySearchFeatureProvider {

    /**
     * Returns accessibility features to be searched where the accessibility features are always on
     * the device and their feature names won't change.
     *
     * @param context a valid context {@link Context} instance
     * @return a list of {@link SearchIndexableRaw} references
     */
    @Nullable
    List<SearchIndexableRaw> getSearchIndexableRawData(Context context);

    /**
     * Returns synonyms of the Accessibility component that is used for search.
     *
     * @param context the context that is used for grabbing resources
     * @param componentName the ComponentName of the accessibility feature
     * @return a comma separated synonyms e.g. "wifi, wi-fi, network connection"
     */
    @NonNull
    String getSynonymsForComponent(@NonNull Context context, @NonNull ComponentName componentName);
}
