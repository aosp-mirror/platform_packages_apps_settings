/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_ALARMS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_EVENTS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_MEDIA;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_REMINDERS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_SYSTEM;

import android.content.Context;
import android.service.notification.ZenPolicy;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

public class ZenModeOtherPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public ZenModeOtherPreferenceController(Context context, String key,
            ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        TwoStatePreference pref = (TwoStatePreference) preference;
        pref.setChecked(getMode().getPolicy().isCategoryAllowed(getCategory(), true));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allow = (Boolean) newValue;

        ZenPolicy diffPolicy = new ZenPolicy.Builder()
                .allowCategory(getCategory(), allow)
                .build();
        getMode().setPolicy(diffPolicy);
        mBackend.updateMode(getMode());

        return true;
    }

    private int getCategory() {
        switch (getPreferenceKey()) {
            case "modes_category_alarm":
                return PRIORITY_CATEGORY_ALARMS;
            case "modes_category_media":
                return PRIORITY_CATEGORY_MEDIA;
            case "modes_category_system":
                return PRIORITY_CATEGORY_SYSTEM;
            case "modes_category_reminders":
                return PRIORITY_CATEGORY_REMINDERS;
            case "modes_category_events":
                return PRIORITY_CATEGORY_EVENTS;
        }
        return -1;
    }
}
