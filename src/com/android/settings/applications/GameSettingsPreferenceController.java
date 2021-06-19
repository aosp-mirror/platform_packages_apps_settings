/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications;

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/** Contains logic that deals with showing Game Settings in app settings. */
public class GameSettingsPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    final GameSettingsFeatureProvider mGameSettingsFeatureProvider;

    public GameSettingsPreferenceController(Context context, String key) {
        super(context, key);
        mGameSettingsFeatureProvider =
                FeatureFactory.getFactory(context).getGameSettingsFeatureProvider();
    }

    GameSettingsPreferenceController(Context context, String key,
            GameSettingsFeatureProvider gameSettingsFeatureProvider) {
        super(context, key);
        mGameSettingsFeatureProvider = gameSettingsFeatureProvider;
    }

    @Override
    public int getAvailabilityStatus() {
        return mGameSettingsFeatureProvider.isSupported(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            mGameSettingsFeatureProvider.launchGameSettings(mContext);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }
}
