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

package com.android.settings.gestures;

import android.content.Context;
import android.net.Uri;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * The controller to handle one-handed mode pull screen into reach preference.
 **/
public class OneHandedActionPullDownPrefController extends BasePreferenceController
        implements OneHandedSettingsUtils.TogglesCallback, LifecycleObserver, OnStart, OnStop {

    private final OneHandedSettingsUtils mUtils;

    private Preference mPreference;

    public OneHandedActionPullDownPrefController(Context context, String key) {
        super(context, key);
        mUtils = new OneHandedSettingsUtils(context);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference).setChecked(
                    !OneHandedSettingsUtils.isSwipeDownNotificationEnabled(mContext));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return  (OneHandedSettingsUtils.isSupportOneHandedMode()
                && OneHandedSettingsUtils.canEnableController(mContext))
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        OneHandedSettingsUtils.setSwipeDownNotificationEnabled(mContext, false);
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference).setChecked(true);
        }
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mUtils.registerToggleAwareObserver(this);
    }

    @Override
    public void onStop() {
        mUtils.unregisterToggleAwareObserver();
    }

    @Override
    public void onChange(Uri uri) {
        if (mPreference == null) {
            return;
        }
        if (uri.equals(OneHandedSettingsUtils.ONE_HANDED_MODE_ENABLED_URI)
                || uri.equals(OneHandedSettingsUtils.SOFTWARE_SHORTCUT_ENABLED_URI)
                || uri.equals(OneHandedSettingsUtils.HARDWARE_SHORTCUT_ENABLED_URI)) {
            mPreference.setEnabled(OneHandedSettingsUtils.canEnableController(mContext));
        } else if (uri.equals(OneHandedSettingsUtils.SHOW_NOTIFICATION_ENABLED_URI)) {
            updateState(mPreference);
        }
    }
}
