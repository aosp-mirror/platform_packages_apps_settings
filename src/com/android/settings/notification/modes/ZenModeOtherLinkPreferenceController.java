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

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settingslib.notification.modes.ZenMode;

/**
 * Preference with a link and summary about what other sounds can break through the mode
 */
class ZenModeOtherLinkPreferenceController extends AbstractZenModePreferenceController {

    private final ZenModeSummaryHelper mSummaryHelper;

    public ZenModeOtherLinkPreferenceController(Context context, String key,
            ZenHelperBackend helperBackend) {
        super(context, key);
        mSummaryHelper = new ZenModeSummaryHelper(mContext, helperBackend);
    }

    @Override
    public boolean isAvailable(ZenMode zenMode) {
        return zenMode.getRule().getInterruptionFilter() != INTERRUPTION_FILTER_ALL;
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        // TODO: b/332937635 - Update metrics category
        preference.setIntent(
                ZenSubSettingLauncher.forModeFragment(mContext, ZenModeOtherFragment.class,
                        zenMode.getId(), 0).toIntent());

        preference.setSummary(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode));
        // TODO: b/346551087 - Show media icons
        ((CircularIconsPreference) preference).displayIcons(CircularIconSet.EMPTY);
    }
}
