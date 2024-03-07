/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.widget;

import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settingslib.RestrictedLockUtils;

/**
 * The switch controller that is used to update the switch widget in the SettingsMainSwitchBar.
 */
public class MainSwitchBarController extends SwitchWidgetController implements
        OnCheckedChangeListener {

    private final SettingsMainSwitchBar mMainSwitch;

    public MainSwitchBarController(SettingsMainSwitchBar mainSwitch) {
        mMainSwitch = mainSwitch;
    }

    @Override
    public void setupView() {
        mMainSwitch.show();
    }

    @Override
    public void teardownView() {
        mMainSwitch.hide();
    }

    @Override
    public void setTitle(String title) {
        mMainSwitch.setTitle(title);
    }

    @Override
    public void startListening() {
        mMainSwitch.addOnSwitchChangeListener(this);
    }

    @Override
    public void stopListening() {
        mMainSwitch.removeOnSwitchChangeListener(this);
    }

    @Override
    public void setChecked(boolean checked) {
        mMainSwitch.setChecked(checked);
    }

    @Override
    public boolean isChecked() {
        return mMainSwitch.isChecked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        mMainSwitch.setEnabled(enabled);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mListener != null) {
            mListener.onSwitchToggled(isChecked);
        }
    }

    @Override
    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        mMainSwitch.setDisabledByAdmin(admin);
    }
}
