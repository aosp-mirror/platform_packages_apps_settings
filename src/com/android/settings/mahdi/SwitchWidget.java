/*
 * Copyright (C) 2011 The CyanogenMod Project
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

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SwitchWidget implements CompoundButton.OnCheckedChangeListener {
    public static Context mContext;
    public Switch mSwitch;
    public AtomicBoolean mConnected = new AtomicBoolean(false);

    public boolean mStateMachineEvent;

    /*
     * public SwitchWidget(Context context, Switch switch_) { super(context,
     * switch_); mContext = context; mSwitch = switch_; }
     */
    public SwitchWidget() {
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        /* Stub! */
        if (mSwitch == switch_)
            return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);

        setState(switch_);
    }

    public void setState(Switch switch_) {
        /* Stub */
        return;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        /* Stub! */
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
