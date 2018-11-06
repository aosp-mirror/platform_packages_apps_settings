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
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;
import java.util.Optional;

public class CurrentDreamPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {
    private final DreamBackend mBackend;
    private final static String TAG = "CurrentDreamPreferenceController";
    private final static String CURRENT_SCREENSAVER = "current_screensaver";

    public CurrentDreamPreferenceController(Context context) {
        super(context);
        mBackend = DreamBackend.getInstance(context);
    }

    @Override
    public boolean isAvailable() {
        return mBackend.getDreamInfos().size() > 0;
    }

    @Override
    public String getPreferenceKey() {
        return CURRENT_SCREENSAVER;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        preference.setSummary(mBackend.getActiveDreamName());
        setGearClickListenerForPreference(preference);
    }

    private void setGearClickListenerForPreference(Preference preference) {
        if (!(preference instanceof GearPreference)) return;

        GearPreference gearPreference = (GearPreference)preference;
        Optional<DreamInfo> info = getActiveDreamInfo();
        if (!info.isPresent() || info.get().settingsComponentName == null) {
            gearPreference.setOnGearClickListener(null);
            return;
        }
        gearPreference.setOnGearClickListener(gearPref -> launchScreenSaverSettings());
    }

    private void launchScreenSaverSettings() {
        Optional<DreamInfo> info = getActiveDreamInfo();
        if (!info.isPresent()) return;
        mBackend.launchSettings(mContext, info.get());
    }

    private Optional<DreamInfo> getActiveDreamInfo() {
        return mBackend.getDreamInfos()
                .stream()
                .filter((info) -> info.isActive)
                .findFirst();
    }
}
