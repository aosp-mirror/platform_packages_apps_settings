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

import android.annotation.StringRes;
import android.content.Context;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController;
import com.android.settingslib.dream.DreamBackend;

public class WhenToDreamPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin {

    private static final String DEFAULT_PREF_KEY = "when_to_start";
    private final DreamBackend mBackend;
    private final boolean mDreamsDisabledByAmbientModeSuppression;
    private final boolean mDreamsEnabledOnBattery;

    public WhenToDreamPreferenceController(Context context) {
        this(context, DEFAULT_PREF_KEY);
    }

    public WhenToDreamPreferenceController(Context context, String preferenceKey) {
        this(context, preferenceKey, context.getResources().getBoolean(
                com.android.internal.R.bool.config_dreamsDisabledByAmbientModeSuppressionConfig),
                context.getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsEnabledOnBattery));
    }

    @VisibleForTesting
    WhenToDreamPreferenceController(
            Context context,
            String preferenceKey,
            boolean dreamsDisabledByAmbientModeSuppression,
            boolean dreamsEnabledOnBattery) {
        super(context, preferenceKey);

        mBackend = DreamBackend.getInstance(context);
        mDreamsDisabledByAmbientModeSuppression = dreamsDisabledByAmbientModeSuppression;
        mDreamsEnabledOnBattery = dreamsEnabledOnBattery;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        preference.setSummary(getSummaryResId());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(getSummaryResId());
    }

    private @StringRes int getSummaryResId() {
        if (mDreamsDisabledByAmbientModeSuppression
                && AmbientDisplayAlwaysOnPreferenceController.isAodSuppressedByBedtime(mContext)) {
            return R.string.screensaver_settings_when_to_dream_bedtime;
        } else {
            final int resId = DreamSettings.getDreamSettingDescriptionResId(
                    mBackend.getWhenToDreamSetting(), mDreamsEnabledOnBattery);
            return resId;
        }
    }
}
