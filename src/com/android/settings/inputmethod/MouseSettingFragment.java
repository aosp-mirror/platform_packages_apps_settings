/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.keyboard.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class MouseSettingFragment extends DashboardFragment {
    private static final String TAG = MouseSettingFragment.class.getSimpleName();

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.mouse_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_KEYBOARD_MOUSE;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mouse_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return Flags.keyboardAndTouchpadA11yNewPageEnabled()
                            && InputPeripheralsSettingsUtils.isMouse();
                }
            };
}
