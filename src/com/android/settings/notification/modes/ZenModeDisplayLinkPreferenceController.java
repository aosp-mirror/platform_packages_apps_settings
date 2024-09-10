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

import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

class ZenModeDisplayLinkPreferenceController extends AbstractZenModePreferenceController  {

    private final ZenModeSummaryHelper mSummaryHelper;

    public ZenModeDisplayLinkPreferenceController(Context context, String key,
            ZenModesBackend backend, ZenHelperBackend helperBackend) {
        super(context, key, backend);
        mSummaryHelper = new ZenModeSummaryHelper(context, helperBackend);
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_AUTOMATIC_ZEN_RULE_ID, zenMode.getId());
        preference.setIntent(
                ZenSubSettingLauncher.forModeFragment(mContext, ZenModeDisplayFragment.class,
                        zenMode.getId(), SettingsEnums.ZEN_PRIORITY_MODE).toIntent());
        preference.setEnabled(zenMode.isEnabled() && zenMode.canEditPolicy());
    }

    @Override
    public CharSequence getSummary(@NonNull ZenMode zenMode) {
        return mSummaryHelper.getDisplayEffectsSummary(zenMode);
    }
}