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

package com.android.settings.location;

import android.content.Context;
import android.text.Html;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.FooterPreference;

/**
 * Preference controller for Location Settings footer.
 */
public class LocationSettingsFooterPreferenceController extends LocationBasePreferenceController {
    FooterPreference mFooterPreference;

    public LocationSettingsFooterPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mFooterPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        boolean enabled = mLocationEnabler.isEnabled(mode);
        mFooterPreference.setTitle(Html.fromHtml(mContext.getString(
                enabled ? R.string.location_settings_footer_location_on
                        : R.string.location_settings_footer_location_off)));
    }
}
