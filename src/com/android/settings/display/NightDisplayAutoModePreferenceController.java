/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.location.LocationManager;

import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class NightDisplayAutoModePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final LocationManager mLocationManager;
    private DropDownPreference mPreference;
    private ColorDisplayManager mColorDisplayManager;

    public NightDisplayAutoModePreferenceController(Context context, String key) {
        super(context, key);
        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mLocationManager = context.getSystemService(LocationManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isNightDisplayAvailable(mContext) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());

        mPreference.setEntries(new CharSequence[]{
                mContext.getString(R.string.night_display_auto_mode_never),
                mContext.getString(R.string.night_display_auto_mode_custom),
                mContext.getString(R.string.night_display_auto_mode_twilight)
        });
        mPreference.setEntryValues(new CharSequence[]{
                String.valueOf(ColorDisplayManager.AUTO_MODE_DISABLED),
                String.valueOf(ColorDisplayManager.AUTO_MODE_CUSTOM_TIME),
                String.valueOf(ColorDisplayManager.AUTO_MODE_TWILIGHT)
        });
    }

    @Override
    public final void updateState(Preference preference) {
        mPreference.setValue(String.valueOf(mColorDisplayManager.getNightDisplayAutoMode()));
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        if (String.valueOf(ColorDisplayManager.AUTO_MODE_TWILIGHT).equals(newValue)
                && !mLocationManager.isLocationEnabled()) {
            TwilightLocationDialog.show(mContext);
            return true;
        }
        return mColorDisplayManager.setNightDisplayAutoMode(Integer.parseInt((String) newValue));
    }
}
