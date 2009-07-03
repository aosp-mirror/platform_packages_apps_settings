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
public class BluetoothDevicePreference extends Preference implements LocalBluetoothDevice.Callback {
    private static final String TAG = "BluetoothDevicePreference";

    private static int sDimAlpha = Integer.MIN_VALUE;
    
    private LocalBluetoothDevice mLocalDevice;
    
    /**
     * Cached local copy of whether the device is busy. This is only updated
     * from {@link #onDeviceAttributesChanged(LocalBluetoothDevice)}.
     */ 
    private boolean mIsBusy;
    
    public BluetoothDevicePreference(Context context, LocalBluetoothDevice localDevice) {
        super(context);

        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255);
        }
            
        mLocalDevice = localDevice;
        
        setLayoutResource(R.layout.preference_bluetooth);
        
        localDevice.registerCallback(this);
        
        onDeviceAttributesChanged(localDevice);
    }
    
    public LocalBluetoothDevice getDevice() {
        return mLocalDevice;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        mLocalDevice.unregisterCallback(this);
    }

    public void onDeviceAttributesChanged(LocalBluetoothDevice device) {

        /*
         * The preference framework takes care of making sure the value has
         * changed before proceeding.
         */
        
        setTitle(mLocalDevice.getName());
        
        /*
         * TODO: Showed "Paired" even though it was "Connected". This may be
         * related to BluetoothHeadset not bound to the actual
         * BluetoothHeadsetService when we got here.
         */
        setSummary(mLocalDevice.getSummary());

        // Used to gray out the item
        mIsBusy = mLocalDevice.isBusy();
        
        // Data has changed
        notifyChanged();
        
        // This could affect ordering, so notify that also
        notifyHierarchyChanged();
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && !mIsBusy;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // Disable this view if the bluetooth enable/disable preference view is off
        setDependency("bt_checkbox");

        ImageView btClass = (ImageView) view.findViewById(R.id.btClass);
        btClass.setImageResource(mLocalDevice.getBtClassDrawable());
        btClass.setAlpha(isEnabled() ? 255 : sDimAlpha);        
    }

    @Override
    public int compareTo(Preference another) {
        if (!(another instanceof BluetoothDevicePreference)) {
            // Put other preference types above us
            return 1;
        }
        
        return mLocalDevice.compareTo(((BluetoothDevicePreference) another).mLocalDevice);
    }
 
}
