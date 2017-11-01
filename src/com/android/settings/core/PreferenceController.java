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

import android.content.Context;

import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

/**
 * A controller that manages event for preference.
 */
public abstract class PreferenceController extends AbstractPreferenceController {

    public PreferenceController(Context context) {
        super(context);
    }

    /**
     * Updates raw data for search provider.
     *
     * Called by SearchIndexProvider#getRawDataToIndex
     */
    public void updateRawDataToIndex(List<SearchIndexableRaw> rawData) {
    }

    /**
     * @return the {@link ResultPayload} corresponding to the search result type for the preference.
     */
    public ResultPayload getResultPayload() {
        return null;
    }
}
