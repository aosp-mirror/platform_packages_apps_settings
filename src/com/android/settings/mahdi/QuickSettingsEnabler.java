/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mahdi;

import android.provider.Settings;
import android.content.Context;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.mahdi.SwitchWidget;

public class QuickSettingsEnabler extends SwitchWidget {
    public QuickSettingsEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setState(Switch switch_) {
        boolean isEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.EXPANDED_VIEW_WIDGET, 0) == 1;
        mSwitch.setChecked(isEnabled);
        return;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.EXPANDED_VIEW_WIDGET,
                isChecked ? 1 : 0);
        return;
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }
}
