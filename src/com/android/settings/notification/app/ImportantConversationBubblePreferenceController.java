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

package com.android.settings.notification.app;

import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;
import static android.provider.Settings.Secure.BUBBLE_IMPORTANT_CONVERSATIONS;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;

public class ImportantConversationBubblePreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "ImpConvBubPrefContr";
    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;


    public ImportantConversationBubblePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    private boolean isGloballyEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, OFF) == ON;
    }

    @Override
    public int getAvailabilityStatus() {
        return isGloballyEnabled() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                BUBBLE_IMPORTANT_CONVERSATIONS, ON) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                BUBBLE_IMPORTANT_CONVERSATIONS, isChecked ? ON : OFF);
    }
}
