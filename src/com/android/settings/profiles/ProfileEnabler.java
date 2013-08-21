/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import android.app.ProfileManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

public class ProfileEnabler implements CompoundButton.OnCheckedChangeListener  {
    private final Context mContext;
    private Switch mSwitch;
    private boolean mStateMachineEvent;

    public ProfileEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchState();
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchState();
    }

    private void setSwitchState() {
        boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;
        mStateMachineEvent = true;
        mSwitch.setChecked(enabled);
        mStateMachineEvent = false;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent) {
            return;
        }
        // Handle a switch change
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, isChecked ? 1 : 0);

        // Send a broadcast intent to the world
        // TODO Enabling or disabling profiles should be at ProfileManager, not here
        Intent intent=new Intent(ProfileManager.PROFILES_STATE_CHANGED_ACTION);
        intent.putExtra(
                ProfileManager.EXTRA_PROFILES_STATE,
                isChecked ?
                          ProfileManager.PROFILES_STATE_ENABLED :
                          ProfileManager.PROFILES_STATE_DISABLED);
        mContext.sendBroadcast(intent);

    }

}
