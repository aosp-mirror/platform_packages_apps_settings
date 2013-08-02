/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.CompoundButton;
import android.widget.Switch;

/**
 * LocationEnabler is a helper to manage the Location on/off master switch
 * preference. It turns on/off Location master switch and ensures the summary
 * of the preference reflects the current state.
 */
public final class LocationEnabler implements CompoundButton.OnCheckedChangeListener {
    private final Context mContext;
    private Switch mSwitch;
    private boolean mValidListener;

    // TODO(lifu): listens to the system configuration change, and modify the switch state whenever
    // necessary.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    };

    public LocationEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mValidListener = false;
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        mValidListener = true;
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
        mValidListener = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO(lifu): modify the actual location settings when the user flip the master switch.
    }

    private void setChecked(boolean isChecked) {
        if (isChecked != mSwitch.isChecked()) {
            // set listener to null so that that code below doesn't trigger onCheckedChanged()
            if (mValidListener) {
                mSwitch.setOnCheckedChangeListener(null);
            }
            mSwitch.setChecked(isChecked);
            if (mValidListener) {
                mSwitch.setOnCheckedChangeListener(this);
            }
        }
    }
}
