/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@Implements(value = BluetoothAdapter.class)
public class ShadowBluetoothAdapter extends org.robolectric.shadows.ShadowBluetoothAdapter {

    private int mState;
    private List<Integer> mSupportedProfiles = new ArrayList<>();
    private List<BluetoothDevice> mMostRecentlyConnectedDevices = new ArrayList<>();

    @Implementation
    protected List<Integer> getSupportedProfiles() {
        return mSupportedProfiles;
    }

    public void addSupportedProfiles(int profile) {
        mSupportedProfiles.add(profile);
    }

    public void clearSupportedProfiles() {
        mSupportedProfiles.clear();
    }

    @Implementation
    protected int getConnectionState() {
        return mState;
    }

    public void setConnectionState(int state) {
        mState = state;
    }

    @Implementation
    protected boolean factoryReset() {
        return true;
    }

    @Implementation
    protected List<BluetoothDevice> getMostRecentlyConnectedDevices() {
        return mMostRecentlyConnectedDevices;
    }

    public void setMostRecentlyConnectedDevices(List<BluetoothDevice> list) {
        mMostRecentlyConnectedDevices = list;
    }
}
