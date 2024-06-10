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

import static com.android.settings.notification.modes.ZenModeFragmentBase.MODE_ID;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.core.SubSettingLauncher;

/**
 * Preference with a link and summary about what apps can break through the mode
 */
public class ZenModeAppsLinkPreferenceController extends AbstractZenModePreferenceController {

    private final ZenModeSummaryHelper mSummaryHelper;

    public ZenModeAppsLinkPreferenceController(Context context, String key,
            ZenModesBackend backend) {
        super(context, key, backend);
        mSummaryHelper = new ZenModeSummaryHelper(mContext, mBackend);
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        Bundle bundle = new Bundle();
        bundle.putString(MODE_ID, zenMode.getId());
        // TODO(b/332937635): Update metrics category
        preference.setIntent(new SubSettingLauncher(mContext)
                .setDestination(ZenModeAppsFragment.class.getName())
                .setSourceMetricsCategory(0)
                .setArguments(bundle)
                .toIntent());
        preference.setSummary(mSummaryHelper.getAppsSummary(zenMode));
    }
}
