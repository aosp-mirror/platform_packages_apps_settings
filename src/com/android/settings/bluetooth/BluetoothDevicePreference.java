/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.AlertDialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.settings.R;

import java.util.List;

/**
 * BluetoothDevicePreference is the preference type used to display each remote
 * Bluetooth device in the Bluetooth Settings screen.
 */
public final class BluetoothDevicePreference extends Preference implements
        CachedBluetoothDevice.Callback, OnClickListener {
    private static final String TAG = "BluetoothDevicePreference";

    private static int sDimAlpha = Integer.MIN_VALUE;

    private final CachedBluetoothDevice mCachedDevice;

    private ImageView mDeviceSettings;

    private OnClickListener mOnSettingsClickListener;

    private AlertDialog mDisconnectDialog;

    public BluetoothDevicePreference(Context context, CachedBluetoothDevice cachedDevice) {
        super(context);

        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255);
        }

        mCachedDevice = cachedDevice;

        setWidgetLayoutResource(R.layout.preference_bluetooth);

        mCachedDevice.registerCallback(this);

        onDeviceAttributesChanged();
    }

    CachedBluetoothDevice getCachedDevice() {
        return mCachedDevice;
    }

    public void setOnSettingsClickListener(OnClickListener listener) {
        mOnSettingsClickListener = listener;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        mCachedDevice.unregisterCallback(this);
        if (mDisconnectDialog != null) {
            mDisconnectDialog.dismiss();
            mDisconnectDialog = null;
        }
    }

    public void onDeviceAttributesChanged() {
        /*
         * The preference framework takes care of making sure the value has
         * changed before proceeding. It will also call notifyChanged() if
         * any preference info has changed from the previous value.
         */
        setTitle(mCachedDevice.getName());

        setSummary(getConnectionSummary());

        // Used to gray out the item
        setEnabled(!mCachedDevice.isBusy());

        // This could affect ordering, so notify that
        notifyHierarchyChanged();
    }

    @Override
    protected void onBindView(View view) {
        // Disable this view if the bluetooth enable/disable preference view is off
        if (null != findPreferenceInHierarchy("bt_checkbox")) {
            setDependency("bt_checkbox");
        }

        super.onBindView(view);

        ImageView btClass = (ImageView) view.findViewById(android.R.id.icon);
        btClass.setImageResource(getBtClassDrawable());
        btClass.setAlpha(isEnabled() ? 255 : sDimAlpha);
        btClass.setVisibility(View.VISIBLE);
        mDeviceSettings = (ImageView) view.findViewById(R.id.deviceDetails);
        if (mOnSettingsClickListener != null) {
            mDeviceSettings.setOnClickListener(this);
            mDeviceSettings.setTag(mCachedDevice);
            mDeviceSettings.setAlpha(isEnabled() ? 255 : sDimAlpha);
        } else { // Hide the settings icon and divider
            mDeviceSettings.setVisibility(View.GONE);
            View divider = view.findViewById(R.id.divider);
            if (divider != null) {
                divider.setVisibility(View.GONE);
            }
        }

        LayoutInflater inflater = (LayoutInflater)
                getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup profilesGroup = (ViewGroup) view.findViewById(R.id.profileIcons);
        for (LocalBluetoothProfile profile : mCachedDevice.getProfiles()) {
            int iconResource = profile.getDrawableResource(mCachedDevice.getBtClass());
            if (iconResource != 0) {
                Drawable icon = getContext().getResources().getDrawable(iconResource);
                inflater.inflate(R.layout.profile_icon_small, profilesGroup, true);
                ImageView imageView =
                        (ImageView) profilesGroup.getChildAt(profilesGroup.getChildCount() - 1);
                imageView.setImageDrawable(icon);
                boolean profileEnabled = mCachedDevice.isConnectedProfile(profile);
                imageView.setAlpha(profileEnabled ? 255 : sDimAlpha);
            }
        }
    }

    public void onClick(View v) {
        if (v == mDeviceSettings) {
            if (mOnSettingsClickListener != null) {
                mOnSettingsClickListener.onClick(v);
            }
        }
    }

    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof BluetoothDevicePreference)) {
            return false;
        }
        return mCachedDevice.equals(
                ((BluetoothDevicePreference) o).mCachedDevice);
    }

    public int hashCode() {
        return mCachedDevice.hashCode();
    }

    @Override
    public int compareTo(Preference another) {
        if (!(another instanceof BluetoothDevicePreference)) {
            // Put other preference types above us
            return 1;
        }

        return mCachedDevice
                .compareTo(((BluetoothDevicePreference) another).mCachedDevice);
    }

    void onClicked() {
        int bondState = mCachedDevice.getBondState();

        if (mCachedDevice.isConnected()) {
            askDisconnect();
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            mCachedDevice.connect(true);
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            pair();
        }
    }

    // Show disconnect confirmation dialog for a device.
    private void askDisconnect() {
        Context context = getContext();
        String name = mCachedDevice.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }
        String message = context.getString(R.string.bluetooth_disconnect_blank, name);

        DialogInterface.OnClickListener disconnectListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mCachedDevice.disconnect();
            }
        };

        mDisconnectDialog = Utils.showDisconnectDialog(context,
                mDisconnectDialog, disconnectListener, name, message);
    }

    private void pair() {
        if (!mCachedDevice.startPairing()) {
            Utils.showError(getContext(), mCachedDevice.getName(),
                    R.string.bluetooth_pairing_error_message);
        }
    }

    private int getConnectionSummary() {
        final CachedBluetoothDevice cachedDevice = mCachedDevice;
        final BluetoothDevice device = cachedDevice.getDevice();

        // if any profiles are connected or busy, return that status
        for (LocalBluetoothProfile profile : cachedDevice.getProfiles()) {
            int connectionStatus = cachedDevice.getProfileConnectionState(profile);

            if (connectionStatus != BluetoothProfile.STATE_DISCONNECTED) {
                return Utils.getConnectionStateSummary(connectionStatus);
            }
        }

        switch (cachedDevice.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                return R.string.bluetooth_paired;
            case BluetoothDevice.BOND_BONDING:
                return R.string.bluetooth_pairing;
            case BluetoothDevice.BOND_NONE:
                return R.string.bluetooth_not_connected;
            default:
                return 0;
        }
    }

    private int getBtClassDrawable() {
        BluetoothClass btClass = mCachedDevice.getBtClass();
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                    return R.drawable.ic_bt_laptop;

                case BluetoothClass.Device.Major.PHONE:
                    return R.drawable.ic_bt_cellphone;

                case BluetoothClass.Device.Major.PERIPHERAL:
                    return HidProfile.getHidClassDrawable(btClass);

                case BluetoothClass.Device.Major.IMAGING:
                    return R.drawable.ic_bt_imaging;

                default:
                    // unrecognized device class; continue
            }
        } else {
            Log.w(TAG, "mBtClass is null");
        }

        List<LocalBluetoothProfile> profiles = mCachedDevice.getProfiles();
        for (LocalBluetoothProfile profile : profiles) {
            int resId = profile.getDrawableResource(btClass);
            if (resId != 0) {
                return resId;
            }
        }
        if (btClass != null) {
            if (btClass.doesClassMatch(BluetoothClass.PROFILE_A2DP)) {
                return R.drawable.ic_bt_headphones_a2dp;

            }
            if (btClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) {
                return R.drawable.ic_bt_headset_hfp;
            }
        }
        return 0;
    }
}
