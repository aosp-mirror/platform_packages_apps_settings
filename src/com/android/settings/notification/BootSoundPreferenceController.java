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

package com.android.settings.notification;

import android.content.Context;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class BootSoundPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    // Boot Sounds needs to be a system property so it can be accessed during boot.
    private static final String KEY_BOOT_SOUNDS = "boot_sounds";
    @VisibleForTesting
    static final String PROPERTY_BOOT_SOUNDS = "persist.sys.bootanim.play_sound";

    public BootSoundPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            SwitchPreference preference = screen.findPreference(KEY_BOOT_SOUNDS);
            preference.setChecked(SystemProperties.getBoolean(PROPERTY_BOOT_SOUNDS, true));
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_BOOT_SOUNDS.equals(preference.getKey())) {
            SwitchPreference switchPreference = (SwitchPreference) preference;
            SystemProperties.set(PROPERTY_BOOT_SOUNDS, switchPreference.isChecked() ? "1" : "0");
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BOOT_SOUNDS;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(com.android.settings.R.bool.has_boot_sounds);
    }

}