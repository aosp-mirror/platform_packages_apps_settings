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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Preference controller for autoclick (dwell timing). */
public class AutoclickPreferenceController extends BasePreferenceController {

    /**
     * Resource ids from which autoclick preference summaries should be derived. The strings have
     * placeholder for integer delay value.
     */
    private static final int[] AUTOCLICK_PREFERENCE_SUMMARIES = {
            R.string.accessibilty_autoclick_preference_subtitle_short_delay,
            R.string.accessibilty_autoclick_preference_subtitle_medium_delay,
            R.string.accessibilty_autoclick_preference_subtitle_long_delay
    };

    public AutoclickPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, OFF) == ON;
        if (!enabled) {
            return mContext.getResources().getText(R.string.autoclick_disabled);
        }
        final int delayMillis = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_DEFAULT);
        final int summaryIndex = getAutoclickPreferenceSummaryIndex(delayMillis);
        return AutoclickUtils.getAutoclickDelaySummary(mContext,
                AUTOCLICK_PREFERENCE_SUMMARIES[summaryIndex], delayMillis);
    }

    /** Finds index of the summary that should be used for the provided autoclick delay. */
    private int getAutoclickPreferenceSummaryIndex(int delay) {
        if (delay <= AutoclickUtils.MIN_AUTOCLICK_DELAY_MS) {
            return 0;
        }
        if (delay >= AutoclickUtils.MAX_AUTOCLICK_DELAY_MS) {
            return AUTOCLICK_PREFERENCE_SUMMARIES.length - 1;
        }
        int delayRange =
                AutoclickUtils.MAX_AUTOCLICK_DELAY_MS - AutoclickUtils.MIN_AUTOCLICK_DELAY_MS;
        int rangeSize = (delayRange) / (AUTOCLICK_PREFERENCE_SUMMARIES.length - 1);
        return (delay - AutoclickUtils.MIN_AUTOCLICK_DELAY_MS) / rangeSize;
    }
}
