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
import android.text.TextUtils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Configures the behaviour of long press power footer. */
public class LongPressPowerFooterPreferenceController extends BasePreferenceController
        implements PowerMenuSettingsUtils.SettingsStateCallback, LifecycleObserver {

    private Preference mPreference;
    private final PowerMenuSettingsUtils mUtils;

    public LongPressPowerFooterPreferenceController(Context context, String key) {
        super(context, key);
        mUtils = new PowerMenuSettingsUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        CharSequence footerHintText = mContext.getString(R.string.power_menu_power_volume_up_hint);
        // If the device supports hush gesture, we need to tell the user where to find the setting.
        if (mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_volumeHushGestureEnabled)) {
            footerHintText =
                    TextUtils.concat(
                            footerHintText,
                            "\n\n",
                            mContext.getString(R.string.power_menu_power_prevent_ringing_hint));
        }

        preference.setSummary(footerHintText);
        preference.setVisible(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext));
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
