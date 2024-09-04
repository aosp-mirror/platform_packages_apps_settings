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
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_ALARMS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_EVENTS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_MEDIA;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_REMINDERS;
import static android.service.notification.ZenPolicy.PRIORITY_CATEGORY_SYSTEM;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.service.notification.ZenPolicy;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Preference with a link and summary about what other sounds can break through the mode
 */
class ZenModeOtherLinkPreferenceController extends AbstractZenModePreferenceController {

    private static final ImmutableMap</* @PriorityCategory */ Integer, /* @DrawableRes */ Integer>
            PRIORITIES_TO_ICONS = ImmutableMap.of(
                    PRIORITY_CATEGORY_ALARMS, R.drawable.ic_zen_mode_sound_alarms,
                    PRIORITY_CATEGORY_MEDIA, R.drawable.ic_zen_mode_sound_media,
                    PRIORITY_CATEGORY_SYSTEM, R.drawable.ic_zen_mode_sound_system,
                    PRIORITY_CATEGORY_REMINDERS, R.drawable.ic_zen_mode_sound_reminders,
                    PRIORITY_CATEGORY_EVENTS, R.drawable.ic_zen_mode_sound_events);

    private final ZenModeSummaryHelper mSummaryHelper;

    public ZenModeOtherLinkPreferenceController(Context context, String key,
            ZenHelperBackend helperBackend) {
        super(context, key);
        mSummaryHelper = new ZenModeSummaryHelper(mContext, helperBackend);
    }

    @Override
    public boolean isAvailable(ZenMode zenMode) {
        return zenMode.getInterruptionFilter() != INTERRUPTION_FILTER_ALL;
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        preference.setIntent(
                ZenSubSettingLauncher.forModeFragment(mContext, ZenModeOtherFragment.class,
                        zenMode.getId(), SettingsEnums.ZEN_PRIORITY_MODE).toIntent());

        preference.setEnabled(zenMode.isEnabled() && zenMode.canEditPolicy());
        preference.setSummary(mSummaryHelper.getOtherSoundCategoriesSummary(zenMode));
        ((CircularIconsPreference) preference).setIcons(getSoundIcons(zenMode.getPolicy()));
    }

    private CircularIconSet<Integer> getSoundIcons(ZenPolicy policy) {
        ImmutableList.Builder<Integer> icons = new ImmutableList.Builder<>();
        for (Map.Entry<Integer, Integer> entry : PRIORITIES_TO_ICONS.entrySet()) {
            if (policy.isCategoryAllowed(entry.getKey(), false)) {
                icons.add(entry.getValue());
            }
        }
        return new CircularIconSet<>(icons.build(),
                iconResId -> IconUtil.makeCircularIconPreferenceItem(mContext, iconResId));
    }
}
