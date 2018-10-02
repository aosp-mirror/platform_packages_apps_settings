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

package com.android.settings.development.featureflags;

import android.content.Context;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import com.android.settings.core.FeatureFlags;

import java.util.HashSet;

/**
 * Helper class to get feature persistent flag information.
 */
public class FeatureFlagPersistent {
    private static final HashSet<String> PERSISTENT_FLAGS;
    static {
        PERSISTENT_FLAGS = new HashSet<>();
        PERSISTENT_FLAGS.add(FeatureFlags.HEARING_AID_SETTINGS);
    }

    public static boolean isEnabled(Context context, String feature) {
        String value = SystemProperties.get(FeatureFlagUtils.PERSIST_PREFIX + feature);
        if (!TextUtils.isEmpty(value)) {
            return Boolean.parseBoolean(value);
        } else {
            return FeatureFlagUtils.isEnabled(context, feature);
        }
    }

    public static void setEnabled(Context context, String feature, boolean enabled) {
        SystemProperties.set(FeatureFlagUtils.PERSIST_PREFIX + feature, enabled ? "true" : "false");
        FeatureFlagUtils.setEnabled(context, feature, enabled);
    }

    public static boolean isPersistent(String feature) {
        return PERSISTENT_FLAGS.contains(feature);
    }

    /**
     * Returns all persistent flags in their raw form.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    static HashSet<String> getAllPersistentFlags() {
        return PERSISTENT_FLAGS;
    }
}

