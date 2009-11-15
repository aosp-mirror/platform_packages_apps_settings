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

import com.android.settings.R;

import android.content.Context;
import android.preference.Preference;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

/**
 * BluetoothDevicePreference is the preference type used to display each remote
 * Bluetooth device in the Bluetooth Settings screen.
 */
public class BluetoothDevicePreference extends Preference implements CachedBluetoothDevice.Callback {
    private static final String TAG = "BluetoothDevicePreference";

    private static int sDimAlpha = Integer.MIN_VALUE;

    private CachedBluetoothDevice mCachedDevice;

    /**
     * Cached local copy of whether the device is busy. This is only updated
     * from {@link #onDeviceAttributesChanged(CachedBluetoothDevice)}.
     */
    private boolean mIsBusy;

    public BluetoothDevicePreference(Context context, CachedBluetoothDevice cachedDevice) {
        super(context);

        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255);
        }

        mCachedDevice = cachedDevice;

        setLayoutResource(R.layout.preference_bluetooth);

        cachedDevice.registerCallback(this);

        onDeviceAttributesChanged(cachedDevice);
    }

    public CachedBluetoothDevice getCachedDevice() {
        return mCachedDevice;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        mCachedDevice.unregisterCallback(this);
    }

    public void onDeviceAttributesChanged(CachedBluetoothDevice cachedDevice) {

        /*
         * The preference framework takes care of making sure the value has
         * changed before proceeding.
         */

        setTitle(mCachedDevice.getName());

        /*
         * TODO: Showed "Paired" even though it was "Connected". This may be
         * related to BluetoothHeadset not bound to the actual
         * BluetoothHeadsetService when we got here.
         */
        setSummary(mCachedDevice.getSummary());

        // Used to gray out the item
        mIsBusy = mCachedDevice.isBusy();

        // Data has changed
        notifyChanged();

        // This could affect ordering, so notify that also
        notifyHierarchyChanged();
    }

    @Override
    public boolean isEnabled() {
        // Temp fix until we have 2053751 fixed in the framework
        setEnabled(true);
        return super.isEnabled() && !mIsBusy;
    }

    @Override
    protected void onBindView(View view) {
        // Disable this view if the bluetooth enable/disable preference view is off
        if (null != findPreferenceInHierarchy("bt_checkbox")){
            setDependency("bt_checkbox");
        }

        super.onBindView(view);

        ImageView btClass = (ImageView) view.findViewById(R.id.btClass);
        btClass.setImageResource(mCachedDevice.getBtClassDrawable());
        btClass.setAlpha(isEnabled() ? 255 : sDimAlpha);
    }

    @Override
    public int compareTo(Preference another) {
        if (!(another instanceof BluetoothDevicePreference)) {
            // Put other preference types above us
            return 1;
        }

        return mCachedDevice.compareTo(((BluetoothDevicePreference) another).mCachedDevice);
    }

}
