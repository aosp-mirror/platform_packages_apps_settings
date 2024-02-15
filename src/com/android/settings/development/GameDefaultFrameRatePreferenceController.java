/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development;


import android.app.IGameManagerService;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.flags.Flags;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class GameDefaultFrameRatePreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin  {
    private static final String TAG = "GameDefFrameRatePrefCtr";
    private static final String DISABLE_GAME_DEFAULT_FRAME_RATE_KEY =
            "disable_game_default_frame_rate";
    private final IGameManagerService mGameManagerService;
    static final String PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED =
            "debug.graphics.game_default_frame_rate.disabled";

    private final DevelopmentSystemPropertiesWrapper mSysProps;
    private int mGameDefaultFrameRateValue;

    @VisibleForTesting
    static class Injector {
        public DevelopmentSystemPropertiesWrapper createSystemPropertiesWrapper() {
            return new DevelopmentSystemPropertiesWrapper() {
                @Override
                public String get(String key, String def) {
                    return SystemProperties.get(key, def);
                }
                @Override
                public boolean getBoolean(String key, boolean def) {
                    return SystemProperties.getBoolean(key, def);
                }

                @Override
                public int getInt(String key, int def) {
                    return SystemProperties.getInt(key, def);
                }

                @Override
                public void set(String key, String val) {
                    SystemProperties.set(key, val);
                }
            };
        }
    }

    public GameDefaultFrameRatePreferenceController(Context context) {
        super(context);
        mGameManagerService = IGameManagerService.Stub.asInterface(
                ServiceManager.getService(Context.GAME_SERVICE));

        mSysProps = new Injector().createSystemPropertiesWrapper();

        mGameDefaultFrameRateValue = mSysProps.getInt(
                "ro.surface_flinger.game_default_frame_rate_override", 60);
    }

    @VisibleForTesting
    GameDefaultFrameRatePreferenceController(Context context,
                                             IGameManagerService gameManagerService,
                                             Injector injector) {
        super(context);
        mGameManagerService = gameManagerService;
        mSysProps = injector.createSystemPropertiesWrapper();
    }

    @Override
    public String getPreferenceKey() {
        return DISABLE_GAME_DEFAULT_FRAME_RATE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isDisabled = (Boolean) newValue;
        try {
            mGameManagerService.toggleGameDefaultFrameRate(!isDisabled);
            updateGameDefaultPreferenceSetting();
        } catch (RemoteException e) {
            // intentional no-op
        }
        return true;
    }

    private void updateGameDefaultPreferenceSetting() {
        final boolean isDisabled =
                mSysProps.getBoolean(PROPERTY_DEBUG_GFX_GAME_DEFAULT_FRAME_RATE_DISABLED,
                        false);
        ((TwoStatePreference) mPreference).setChecked(isDisabled);
        mPreference.setSummary(mContext.getString(
                R.string.disable_game_default_frame_rate_summary,
                mGameDefaultFrameRateValue));
    }
    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateGameDefaultPreferenceSetting();
    }

    @Override
    public boolean isAvailable() {
        return Flags.developmentGameDefaultFrameRate();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        final TwoStatePreference preference = (TwoStatePreference) mPreference;
        if (preference.isChecked()) {
            // When the developer option is disabled, we should set everything
            // to off, that is, enabling game default frame rate.
            try {
                mGameManagerService.toggleGameDefaultFrameRate(true);
            } catch (RemoteException e) {
                // intentional no-op
            }
        }
        preference.setChecked(false);
    }

}
