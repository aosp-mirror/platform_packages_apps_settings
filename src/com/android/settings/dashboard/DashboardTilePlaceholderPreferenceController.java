/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * PreferenceController for a dashboard_tile_placeholder, a special preference marking where
 * dynamic dashboard tiles should be injected in a screen. It is optional when building
 * preference screen in xml. If not present, all dynamic dashboard tiles will be added to the
 * bottom of page.
 */
class DashboardTilePlaceholderPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_PLACEHOLDER = "dashboard_tile_placeholder";

    private int mOrder = Preference.DEFAULT_ORDER;

    public DashboardTilePlaceholderPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        final Preference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            mOrder = pref.getOrder();
            screen.removePreference(pref);
        }
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PLACEHOLDER;
    }

    public int getOrder() {
        return mOrder;
    }
}
