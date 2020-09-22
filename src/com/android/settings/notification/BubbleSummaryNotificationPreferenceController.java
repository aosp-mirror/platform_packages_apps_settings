/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.notification;

import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Summary of the feature setting for bubbles, available through notification menu.
 */
public class BubbleSummaryNotificationPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    static final int ON = 1;

    public BubbleSummaryNotificationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(
                areBubblesEnabled()
                        ? R.string.notifications_bubble_setting_on_summary
                        : R.string.switch_off_text);
    }

    @Override
    public int getAvailabilityStatus() {
        ActivityManager am = mContext.getSystemService(ActivityManager.class);
        return am.isLowRamDevice() ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }

    private boolean areBubblesEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, ON) == ON;
    }
}
