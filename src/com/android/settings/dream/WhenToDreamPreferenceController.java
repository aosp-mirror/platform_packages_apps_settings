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

package com.android.settings.dream;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;

public class WhenToDreamPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String WHEN_TO_START = "when_to_start";
    private final DreamBackend mBackend;

    WhenToDreamPreferenceController(Context context) {
        super(context);

        mBackend = DreamBackend.getInstance(context);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        int resId = DreamSettings.getDreamSettingDescriptionResId(mBackend.getWhenToDreamSetting());
        preference.setSummary(preference.getContext().getString(resId));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return WHEN_TO_START;
    }
}
