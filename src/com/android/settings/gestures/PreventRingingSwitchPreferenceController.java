/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class PreventRingingSwitchPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, SwitchBar.OnSwitchChangeListener {

    private static final String KEY = "gesture_prevent_ringing_switch";
    private final Context mContext;
    private SettingObserver mSettingObserver;

    @VisibleForTesting
    SwitchBar mSwitch;

    public PreventRingingSwitchPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            LayoutPreference pref = screen.findPreference(getPreferenceKey());
            if (pref != null) {
                mSettingObserver = new SettingObserver(pref);
                pref.setOnPreferenceClickListener(preference -> {
                    int preventRinging = Settings.Secure.getInt(mContext.getContentResolver(),
                            Settings.Secure.VOLUME_HUSH_GESTURE,
                            Settings.Secure.VOLUME_HUSH_VIBRATE);
                    boolean isChecked = preventRinging != Settings.Secure.VOLUME_HUSH_OFF;
                    Settings.Secure.putInt(mContext.getContentResolver(),
                            Settings.Secure.VOLUME_HUSH_GESTURE, isChecked
                                    ? Settings.Secure.VOLUME_HUSH_OFF
                                    : Settings.Secure.VOLUME_HUSH_VIBRATE);
                    return true;
                });
                mSwitch = pref.findViewById(R.id.switch_bar);
                if (mSwitch != null) {
                    mSwitch.addOnSwitchChangeListener(this);
                    mSwitch.show();
                }
            }
        }
    }

    public void setChecked(boolean isChecked) {
        if (mSwitch != null) {
            mSwitch.setChecked(isChecked);
        }
    }

    @Override
    public void updateState(Preference preference) {
        int preventRingingSetting = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_VIBRATE);
        setChecked(preventRingingSetting != Settings.Secure.VOLUME_HUSH_OFF);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        final int preventRingingSetting = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_VIBRATE);
        final int newRingingSetting = preventRingingSetting == Settings.Secure.VOLUME_HUSH_OFF
                ? Settings.Secure.VOLUME_HUSH_VIBRATE
                : preventRingingSetting;

        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, isChecked
                        ? newRingingSetting
                        : Settings.Secure.VOLUME_HUSH_OFF);
    }

    private class SettingObserver extends ContentObserver {
        private final Uri VOLUME_HUSH_GESTURE = Settings.Secure.getUriFor(
                Settings.Secure.VOLUME_HUSH_GESTURE);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(VOLUME_HUSH_GESTURE, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || VOLUME_HUSH_GESTURE.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
