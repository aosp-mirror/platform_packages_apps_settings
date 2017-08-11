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
package com.android.settings.dashboard.suggestions;

import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class SuggestionLogHelper {

    public static Pair<Integer, Object>[] getSuggestionTaggedData(boolean enabled) {
        return new Pair[]{
                Pair.create(
                        MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, enabled ? 1 : 0)};
    }
}
