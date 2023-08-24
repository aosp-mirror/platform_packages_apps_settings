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

import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
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
    private static final int RESOURCE_ID_ICON =
            com.android.settingslib.R.drawable.settings_input_antenna;

    private BluetoothLeBroadcastMetadata mBluetoothLeBroadcastMetadata;
    private BluetoothLeBroadcastReceiveState mBluetoothLeBroadcastReceiveState;
    private ImageView mFrictionImageView;
    private String mTitle;
    private boolean mStatus;
    private boolean mIsEncrypted;

    BluetoothBroadcastSourcePreference(@NonNull Context context) {
        super(context);
        initUi();
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        view.findViewById(com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider)
                .setVisibility(View.INVISIBLE);
        final ImageButton imageButton =
                (ImageButton) view.findViewById(com.android.settingslib.R.id.icon_button);
        imageButton.setVisibility(View.GONE);
        mFrictionImageView =
                (ImageView) view.findViewById(com.android.settingslib.R.id.friction_icon);
        updateStatusButton();
    }

    private void initUi() {
        setLayoutResource(com.android.settingslib.R.layout.preference_access_point);
        setWidgetLayoutResource(com.android.settingslib.R.layout.access_point_friction_widget);
        mTitle = getContext().getString(RESOURCE_ID_UNKNOWN_PROGRAM_INFO);
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
        mTitle = getProgramInfo();
        mIsEncrypted = mBluetoothLeBroadcastMetadata.isEncrypted();
        mStatus = status || mBluetoothLeBroadcastReceiveState != null;

        refresh();
    }

    /**
     * Updates the title and status from BluetoothLeBroadcastReceiveState.
     */
    public void updateReceiveStateAndRefreshUi(BluetoothLeBroadcastReceiveState receiveState) {
        mBluetoothLeBroadcastReceiveState = receiveState;
        mTitle = getProgramInfo();
        mStatus = true;

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

    private String getProgramInfo() {
        if (mBluetoothLeBroadcastReceiveState != null) {
            List<BluetoothLeAudioContentMetadata> bluetoothLeAudioContentMetadata =
                    mBluetoothLeBroadcastReceiveState.getSubgroupMetadata();
            if (!bluetoothLeAudioContentMetadata.isEmpty()) {
                return bluetoothLeAudioContentMetadata.stream()
                        .map(i -> i.getProgramInfo())
                        .findFirst().orElse(
                                getContext().getString(RESOURCE_ID_UNKNOWN_PROGRAM_INFO));
            }
        }
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

    /**
     * Whether the broadcast source is encrypted or not.
     * @return If true, the broadcast source needs the broadcast code. If false, the broadcast
     * source does not need the broadcast code.
     */
    public boolean isEncrypted() {
        return mIsEncrypted;
    }

    /**
     * Whether the broadcast source is connected at the beginging. We will get the
     * BluetoothLeBroadcastReceiveState from the broadcast source.
     * See {@link BluetoothFindBroadcastsFragment#addConnectedSourcePreference}
     * @return If true, the broadcast source is already connected by the broadcast sink.
     */
    public boolean isCreatedByReceiveState() {
        return mBluetoothLeBroadcastReceiveState != null;
    }

    /**
     * Clear the BluetoothLeBroadcastReceiveState and reset the state when the user clicks the
     * "leave broadcast" button.
     */
    public void clearReceiveState() {
        mBluetoothLeBroadcastReceiveState = null;
        mTitle = getProgramInfo();
        mStatus = false;
        refresh();
    }
}
