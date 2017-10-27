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

package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeCallsPreferenceController extends
        AbstractZenModePreferenceController {

    protected static final String KEY = "zen_mode_calls";
    private final ZenModeBackend mBackend;

    public ZenModeCallsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
        mBackend = ZenModeBackend.getInstance(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                preference.setEnabled(false);
                preference.setSummary(mBackend.getContactsSummary(mBackend.SOURCE_NONE));
                break;
            default:
                preference.setEnabled(true);
                preference.setSummary(mBackend.getContactsSummary(
                        NotificationManager.Policy.PRIORITY_CATEGORY_CALLS));
        }

    }
}
