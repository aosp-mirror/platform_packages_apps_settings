/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.location;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

public abstract class LocationModeRadioButtonPreferenceController
        extends LocationBasePreferenceController
        implements RadioButtonPreference.OnClickListener {

    protected RadioButtonPreference mPreference;

    public LocationModeRadioButtonPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (RadioButtonPreference) screen.findPreference(getPreferenceKey());
        mPreference.setOnClickListener(this);
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference emiter) {
        mLocationEnabler.setLocationMode(getLocationMode());
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        mPreference.setChecked(mode == getLocationMode());
        mPreference.setEnabled(mLocationEnabler.isEnabled(mode));
    }

    /** Gets the location mode that this controller monitors. */
    protected abstract int getLocationMode();

}
