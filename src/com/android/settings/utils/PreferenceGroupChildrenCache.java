/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.utils;

import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

/**
 * Class that helps track which {@link Preference}s in a {@link PreferenceGroup} are still being
 * used, and remove unused ones.
 */
public class PreferenceGroupChildrenCache {

    private ArrayMap<String, Preference> mPreferenceCache;

    public void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<>();
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
        }
    }

    public void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : mPreferenceCache.values()) {
            group.removePreference(p);
        }
        mPreferenceCache = null;
    }

    public Preference getCachedPreference(String key) {
        return mPreferenceCache != null ? mPreferenceCache.remove(key) : null;
    }

    public int getCachedCount() {
        return mPreferenceCache != null ? mPreferenceCache.size() : 0;
    }
}
