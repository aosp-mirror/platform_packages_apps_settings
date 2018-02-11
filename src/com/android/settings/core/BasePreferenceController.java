/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.core;

import android.annotation.IntDef;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.search.ResultPayload;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.core.AbstractPreferenceController;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Abstract class to consolidate utility between preference controllers and act as an interface
 * for Slices. The abstract classes that inherit from this class will act as the direct interfaces
 * for each type when plugging into Slices.
 *
 * TODO (b/73074893) Add Lifecycle Setting method.
 */
public abstract class BasePreferenceController extends AbstractPreferenceController {

    private static final String TAG = "SettingsPrefController";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AVAILABLE, DISABLED_UNSUPPORTED, DISABLED_FOR_USER, DISABLED_DEPENDENT_SETTING,
            UNAVAILABLE_UNKNOWN})
    public @interface AvailabilityStatus {
    }

    /**
     * The setting is available.
     */
    public static final int AVAILABLE = 0;

    /**
     * The setting is not supported by the device.
     */
    public static final int DISABLED_UNSUPPORTED = 1;

    /**
     * The setting cannot be changed by the current user.
     */
    public static final int DISABLED_FOR_USER = 2;

    /**
     * The setting has a dependency in the Settings App which is currently blocking access.
     */
    public static final int DISABLED_DEPENDENT_SETTING = 3;

    /**
     * A catch-all case for internal errors and inexplicable unavailability.
     */
    public static final int UNAVAILABLE_UNKNOWN = 4;

    protected final String mPreferenceKey;

    public BasePreferenceController(Context context, String preferenceKey) {
        super(context);
        mPreferenceKey = preferenceKey;
    }

    /**
     * @return {@AvailabilityStatus} for the Setting. This status is used to determine if the
     * Setting should be shown or disabled in Settings. Further, it can be used to produce
     * appropriate error / warning Slice in the case of unavailability.
     * </p>
     * The status is used for the convenience methods: {@link #isAvailable()},
     * {@link #isSupported()}
     */
    @AvailabilityStatus
    public abstract int getAvailabilityStatus();

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public final boolean isAvailable() {
        return getAvailabilityStatus() == AVAILABLE;
    }

    /**
     * @return {@code false} if the setting is not applicable to the device. This covers both
     * settings which were only introduced in future versions of android, or settings that have
     * hardware dependencies.
     * </p>
     * Note that a return value of {@code true} does not mean that the setting is available.
     */
    public final boolean isSupported() {
        return getAvailabilityStatus() != DISABLED_UNSUPPORTED;
    }

    /**
     * Updates non-indexable keys for search provider.
     *
     * Called by SearchIndexProvider#getNonIndexableKeys
     */
    public void updateNonIndexableKeys(List<String> keys) {
        if (this instanceof AbstractPreferenceController) {
            if (!isAvailable()) {
                final String key = getPreferenceKey();
                if (TextUtils.isEmpty(key)) {
                    Log.w(TAG,
                            "Skipping updateNonIndexableKeys due to empty key " + this.toString());
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
    public void updateRawDataToIndex(List<SearchIndexableRaw> rawData) {
    }

    /**
     * @return the {@link ResultPayload} corresponding to the search result type for the preference.
     * TODO (b/69808376) Remove this method.
     * Do not extend this method. It will not launch with P.
     */
    @Deprecated
    public ResultPayload getResultPayload() {
        return null;
    }
}