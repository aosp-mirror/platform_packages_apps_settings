/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.content.Context;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

public class AudioSharingSwitchBarController
        implements DefaultLifecycleObserver, OnMainSwitchChangeListener {

    private static final String TAG = "AudioSharingSwitchBarCtl";

    private final Context mContext;
    private final SettingsMainSwitchBar mSwitchBar;
    private final Switch mSwitch;

    AudioSharingSwitchBarController(Context context, SettingsMainSwitchBar switchBar) {
        mContext = context;
        mSwitchBar = switchBar;
        mSwitch = mSwitchBar.getSwitch();
        mSwitch.setChecked(false);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        // Filter out unnecessary callbacks when switch is disabled.
        if (!switchView.isEnabled()) return;

        if (isChecked) {
            // TODO: start sharing
        } else {
            // TODO: stop sharing
        }
    }
}
