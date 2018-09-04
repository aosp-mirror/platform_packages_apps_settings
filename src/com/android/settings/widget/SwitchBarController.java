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

package com.android.settings.widget;

import android.widget.Switch;

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/*
 * The switch controller that is used to update the switch widget in the SwitchBar layout.
 */
public class SwitchBarController extends SwitchWidgetController implements
    SwitchBar.OnSwitchChangeListener {

    private final SwitchBar mSwitchBar;

    public SwitchBarController(SwitchBar switchBar) {
        mSwitchBar = switchBar;
    }

    @Override
    public void setupView() {
        mSwitchBar.show();
    }

    @Override
    public void teardownView() {
        mSwitchBar.hide();
    }

    @Override
    public void updateTitle(boolean isChecked) {
        mSwitchBar.setTextViewLabelAndBackground(isChecked);
    }

    @Override
    public void startListening() {
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void stopListening() {
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public void setChecked(boolean checked) {
        mSwitchBar.setChecked(checked);
    }

    @Override
    public boolean isChecked() {
        return mSwitchBar.isChecked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        mSwitchBar.setEnabled(enabled);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (mListener != null) {
            mListener.onSwitchToggled(isChecked);
        }
    }

    @Override
    public void setDisabledByAdmin(EnforcedAdmin admin) {
        mSwitchBar.setDisabledByAdmin(admin);
    }
}
