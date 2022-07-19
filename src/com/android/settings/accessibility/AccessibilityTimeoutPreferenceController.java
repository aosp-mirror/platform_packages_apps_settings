/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.google.common.primitives.Ints;

/** Preference controller for accessibility timeout. */
public class AccessibilityTimeoutPreferenceController extends BasePreferenceController {

    private final ContentResolver mContentResolver;
    private final Resources mResources;

    public AccessibilityTimeoutPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = mContext.getContentResolver();
        mResources = mContext.getResources();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final String[] timeoutSummaries = mResources.getStringArray(
                R.array.accessibility_timeout_summaries);
        final int[] timeoutValues = mResources.getIntArray(
                R.array.accessibility_timeout_selector_values);
        final int timeoutValue = AccessibilityTimeoutUtils.getSecureAccessibilityTimeoutValue(
                mContentResolver);
        final int idx = Ints.indexOf(timeoutValues, timeoutValue);
        return timeoutSummaries[idx == -1 ? 0 : idx];
    }
}
