/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.virtual;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.ActionButtonsPreference;

import java.util.Objects;

/** This class adds one button to "forget" (ie unpair) the device. */
public class VirtualDeviceDetailsButtonsController extends BasePreferenceController {

    private static final String KEY_VIRTUAL_DEVICE_ACTION_BUTTONS = "virtual_device_action_buttons";

    @Nullable
    private PreferenceFragmentCompat mFragment;
    @Nullable
    private VirtualDeviceWrapper mDevice;

    public VirtualDeviceDetailsButtonsController(@NonNull Context context) {
        super(context, KEY_VIRTUAL_DEVICE_ACTION_BUTTONS);
    }

    /** One-time initialization when the controller is first created. */
    void init(@NonNull PreferenceFragmentCompat fragment, @NonNull VirtualDeviceWrapper device) {
        mFragment = fragment;
        mDevice = device;
    }

    private void onForgetButtonPressed() {
        ForgetDeviceDialogFragment fragment =
                ForgetDeviceDialogFragment.newInstance(Objects.requireNonNull(mDevice));
        fragment.show(Objects.requireNonNull(mFragment).getParentFragmentManager(),
                ForgetDeviceDialogFragment.TAG);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        ((ActionButtonsPreference) screen.findPreference(getPreferenceKey()))
                .setButton1Text(R.string.forget)
                .setButton1Icon(R.drawable.ic_settings_delete)
                .setButton1OnClickListener((view) -> onForgetButtonPressed())
                .setButton1Enabled(true);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
