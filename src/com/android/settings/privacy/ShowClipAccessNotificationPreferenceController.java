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

package com.android.settings.privacy;

import android.content.ClipboardManager;
import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for preference to toggle whether clipboard access notifications should be shown.
 */
public class ShowClipAccessNotificationPreferenceController
        extends TogglePreferenceController implements LifecycleObserver {

    private static final String KEY_SHOW_CLIP_ACCESS_NOTIFICATION = "show_clip_access_notification";

    private final DeviceConfig.OnPropertiesChangedListener mDeviceConfigListener =
            properties -> updateConfig();
    private boolean mDefault;
    private Preference mPreference;

    public ShowClipAccessNotificationPreferenceController(Context context) {
        super(context, KEY_SHOW_CLIP_ACCESS_NOTIFICATION);
        updateConfig();
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS, (mDefault ? 1 : 0)) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS, (isChecked ? 1 : 0));
        return true;
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
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_privacy;
    }

    /**
     * Registers a DeviceConfig listener on start.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        DeviceConfig.addOnPropertiesChangedListener(DeviceConfig.NAMESPACE_CLIPBOARD,
                mContext.getMainExecutor(), mDeviceConfigListener);
    }

    /**
     * Removes the DeviceConfig listener on stop.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        DeviceConfig.removeOnPropertiesChangedListener(mDeviceConfigListener);
    }

    private void updateConfig() {
        mDefault = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_CLIPBOARD,
                ClipboardManager.DEVICE_CONFIG_SHOW_ACCESS_NOTIFICATIONS,
                ClipboardManager.DEVICE_CONFIG_DEFAULT_SHOW_ACCESS_NOTIFICATIONS);
        updateState(mPreference);
    }

}
