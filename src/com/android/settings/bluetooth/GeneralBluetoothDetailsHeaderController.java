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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

/** This class adds a header with device name and status (connected/disconnected, etc.). */
public class GeneralBluetoothDetailsHeaderController extends BluetoothDetailsController {
    private static final String KEY_GENERAL_DEVICE_HEADER = "general_bluetooth_device_header";

    @Nullable
    private LayoutPreference mLayoutPreference;

    public GeneralBluetoothDetailsHeaderController(
            Context context,
            PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        if (!Flags.enableBluetoothDeviceDetailsPolish()) {
            return false;
        }
        boolean hasLeAudio =
                mCachedDevice.getUiAccessibleProfiles().stream()
                        .anyMatch(profile -> profile.getProfileId() == BluetoothProfile.LE_AUDIO);
        return !BluetoothUtils.isAdvancedDetailsHeader(mCachedDevice.getDevice()) && !hasLeAudio;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mLayoutPreference = screen.findPreference(KEY_GENERAL_DEVICE_HEADER);
    }

    @Override
    protected void refresh() {
        if (!isAvailable() || mLayoutPreference == null) {
            return;
        }
        ImageView imageView = mLayoutPreference.findViewById(R.id.bt_header_icon);
        if (imageView != null) {
            final Pair<Drawable, String> pair =
                    BluetoothUtils.getBtRainbowDrawableWithDescription(mContext, mCachedDevice);
            imageView.setImageDrawable(pair.first);
            imageView.setContentDescription(pair.second);
        }

        TextView title = mLayoutPreference.findViewById(R.id.bt_header_device_name);
        if (title != null) {
            title.setText(mCachedDevice.getName());
        }
        TextView summary = mLayoutPreference.findViewById(R.id.bt_header_connection_summary);
        if (summary != null) {
            summary.setText(mCachedDevice.getConnectionSummary());
        }
        ImageButton renameButton = mLayoutPreference.findViewById(R.id.rename_button);
        renameButton.setVisibility(View.VISIBLE);
        renameButton.setOnClickListener(
                view -> {
                    RemoteDeviceNameDialogFragment.newInstance(mCachedDevice)
                            .show(
                                    mFragment.getFragmentManager(),
                                    RemoteDeviceNameDialogFragment.TAG);
                });
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return KEY_GENERAL_DEVICE_HEADER;
    }
}
