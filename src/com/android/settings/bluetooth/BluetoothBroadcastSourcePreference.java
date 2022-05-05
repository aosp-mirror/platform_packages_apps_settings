/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastSubgroup;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.Utils;

import java.util.List;

/**
 * Preference to display a broadcast source in the Broadcast Source List.
 */
class BluetoothBroadcastSourcePreference extends Preference {

    private static final int RESOURCE_ID_UNKNOWN_PROGRAM_INFO = R.string.device_info_default;
    private static final int RESOURCE_ID_ICON = R.drawable.settings_input_antenna;

    private BluetoothLeBroadcastMetadata mBluetoothLeBroadcastMetadata;
    private ImageView mFrictionImageView;
    private String mTitle;
    private boolean mStatus;
    private boolean mIsEncrypted;

    BluetoothBroadcastSourcePreference(@NonNull Context context,
            @NonNull BluetoothLeBroadcastMetadata source) {
        super(context);
        initUi();
        updateMetadataAndRefreshUi(source, false);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        view.findViewById(R.id.two_target_divider).setVisibility(View.INVISIBLE);
        final ImageButton imageButton = (ImageButton) view.findViewById(R.id.icon_button);
        imageButton.setVisibility(View.GONE);
        mFrictionImageView = (ImageView) view.findViewById(R.id.friction_icon);
        updateStatusButton();
    }

    private void initUi() {
        setLayoutResource(R.layout.preference_access_point);
        setWidgetLayoutResource(R.layout.access_point_friction_widget);

        mStatus = false;
        final Drawable drawable = getContext().getDrawable(RESOURCE_ID_ICON);
        if (drawable != null) {
            drawable.setTint(Utils.getColorAttrDefaultColor(getContext(),
                    android.R.attr.colorControlNormal));
            setIcon(drawable);
        }
    }

    private void updateStatusButton() {
        if (mFrictionImageView == null) {
            return;
        }
        if (mStatus || mIsEncrypted) {
            Drawable drawable;
            if (mStatus) {
                drawable = getContext().getDrawable(R.drawable.bluetooth_broadcast_dialog_done);
            } else {
                drawable = getContext().getDrawable(R.drawable.ic_friction_lock_closed);
            }
            if (drawable != null) {
                drawable.setTint(Utils.getColorAttrDefaultColor(getContext(),
                        android.R.attr.colorControlNormal));
                mFrictionImageView.setImageDrawable(drawable);
            }
            mFrictionImageView.setVisibility(View.VISIBLE);
        } else {
            mFrictionImageView.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the title and status from BluetoothLeBroadcastMetadata.
     */
    public void updateMetadataAndRefreshUi(BluetoothLeBroadcastMetadata source, boolean status) {
        mBluetoothLeBroadcastMetadata = source;
        mTitle = getBroadcastMetadataProgramInfo();
        mIsEncrypted = mBluetoothLeBroadcastMetadata.isEncrypted();
        mStatus = status;

        refresh();
    }

    /**
     * Gets the BluetoothLeBroadcastMetadata.
     */
    public BluetoothLeBroadcastMetadata getBluetoothLeBroadcastMetadata() {
        return mBluetoothLeBroadcastMetadata;
    }

    private void refresh() {
        setTitle(mTitle);
        updateStatusButton();
    }

    private String getBroadcastMetadataProgramInfo() {
        if (mBluetoothLeBroadcastMetadata == null) {
            return getContext().getString(RESOURCE_ID_UNKNOWN_PROGRAM_INFO);
        }
        final List<BluetoothLeBroadcastSubgroup> subgroups =
                mBluetoothLeBroadcastMetadata.getSubgroups();
        if (subgroups.isEmpty()) {
            return getContext().getString(RESOURCE_ID_UNKNOWN_PROGRAM_INFO);
        }
        return subgroups.stream()
                .map(i -> i.getContentMetadata().getProgramInfo())
                .filter(i -> !TextUtils.isEmpty(i))
                .findFirst().orElse(getContext().getString(RESOURCE_ID_UNKNOWN_PROGRAM_INFO));
    }
}
