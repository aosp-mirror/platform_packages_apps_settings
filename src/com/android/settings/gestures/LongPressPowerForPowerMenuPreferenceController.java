/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * Configures the behaviour of the radio selector to configure long press power button to Power
 * Menu.
 */
public class LongPressPowerForPowerMenuPreferenceController extends BasePreferenceController
        implements PowerMenuSettingsUtils.SettingsStateCallback,
                SelectorWithWidgetPreference.OnClickListener,
                LifecycleObserver {

    private SelectorWithWidgetPreference mPreference;
    private final PowerMenuSettingsUtils mUtils;

    public LongPressPowerForPowerMenuPreferenceController(Context context, String key) {
        super(context, key);
        mUtils = new PowerMenuSettingsUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setOnClickListener(this);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference)
                    .setChecked(
                            !PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext));
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(mContext);
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void onChange(Uri uri) {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_START) */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mUtils.registerObserver(this);
    }

    /** @OnLifecycleEvent(Lifecycle.Event.ON_STOP) */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mUtils.unregisterObserver();
    }
}
