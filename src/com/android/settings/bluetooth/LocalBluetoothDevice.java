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
import com.android.settings.bluetooth.LocalBluetoothProfileManager.Profile;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothClass;
import android.bluetooth.IBluetoothDeviceCallback;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalBluetoothDevice represents a remote Bluetooth device. It contains
 * attributes of the device (such as the address, name, RSSI, etc.) and
 * functionality that can be performed on the device (connect, pair, disconnect,
 * etc.).
 */
public class LocalBluetoothDevice implements Comparable<LocalBluetoothDevice> {
    private static final String TAG = "LocalBluetoothDevice";

    private static final int CONTEXT_ITEM_CONNECT = Menu.FIRST + 1;
    private static final int CONTEXT_ITEM_DISCONNECT = Menu.FIRST + 2;
    private static final int CONTEXT_ITEM_UNPAIR = Menu.FIRST + 3;
    private static final int CONTEXT_ITEM_CONNECT_ADVANCED = Menu.FIRST + 4;

    private final String mAddress;
    private String mName;
    private short mRssi;
    private int mBtClass = BluetoothClass.ERROR;

    private List<Profile> mProfiles = new ArrayList<Profile>();

    private boolean mVisible;

    private final LocalBluetoothManager mLocalManager;

    private List<Callback> mCallbacks = new ArrayList<Callback>();

    /**
     * When we connect to multiple profiles, we only want to display a single
     * error even if they all fail. This tracks that state.
     */
    private boolean mIsConnectingErrorPossible;

    LocalBluetoothDevice(Context context, String address) {
        mLocalManager = LocalBluetoothManager.getInstance(context);
        if (mLocalManager == null) {
            throw new IllegalStateException(
                    "Cannot use LocalBluetoothDevice without Bluetooth hardware");
        }

        mAddress = address;

        fillData();
    }

    public void onClicked() {
        int bondState = getBondState();

        if (isConnected()) {
            askDisconnect();
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            connect();
        } else if (bondState == BluetoothDevice.BOND_NOT_BONDED) {
            pair();
        }
    }

    public void disconnect() {
        for (Profile profile : mProfiles) {
            disconnect(profile);
        }
    }

    public void disconnect(Profile profile) {
        LocalBluetoothProfileManager profileManager =
                LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
        int status = profileManager.getConnectionStatus(mAddress);
        if (SettingsBtStatus.isConnectionStatusConnected(status)) {
            profileManager.disconnect(mAddress);
        }
    }

    public void askDisconnect() {
        Context context = mLocalManager.getForegroundActivity();
        if (context == null) {
            // Cannot ask, since we need an activity context
            disconnect();
            return;
        }

        Resources res = context.getResources();

        String name = getName();
        if (TextUtils.isEmpty(name)) {
            name = res.getString(R.string.bluetooth_device);
        }
        String message = res.getString(R.string.bluetooth_disconnect_blank, name);

        DialogInterface.OnClickListener disconnectListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                disconnect();
            }
        };

        AlertDialog ad = new AlertDialog.Builder(context)
                .setTitle(getName())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, disconnectListener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void connect() {
        if (!ensurePaired()) return;

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        Context context = mLocalManager.getContext();
        boolean hasAtLeastOnePreferredProfile = false;
        for (Profile profile : mProfiles) {
            LocalBluetoothProfileManager profileManager =
                    LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
            if (profileManager.isPreferred(mAddress)) {
                hasAtLeastOnePreferredProfile = true;
                connectInt(profile);
            }
        }

        if (!hasAtLeastOnePreferredProfile) {
            connectAndPreferAllProfiles();
        }
    }

    private void connectAndPreferAllProfiles() {
        if (!ensurePaired()) return;

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        Context context = mLocalManager.getContext();
        for (Profile profile : mProfiles) {
            LocalBluetoothProfileManager profileManager =
                    LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
            profileManager.setPreferred(mAddress, true);
            connectInt(profile);
        }
    }

    public void connect(Profile profile) {
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;
        connectInt(profile);
    }

    public void connectInt(Profile profile) {
        if (!ensurePaired()) return;

        LocalBluetoothProfileManager profileManager =
                LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
        int status = profileManager.getConnectionStatus(mAddress);
        if (!SettingsBtStatus.isConnectionStatusConnected(status)) {
            if (profileManager.connect(mAddress) != BluetoothDevice.RESULT_SUCCESS) {
                Log.i(TAG, "Failed to connect " + profile.toString() + " to " + mName);
            }
        }
    }

    public void showConnectingError() {
        if (!mIsConnectingErrorPossible) return;
        mIsConnectingErrorPossible = false;

        mLocalManager.showError(mAddress, R.string.bluetooth_error_title,
                R.string.bluetooth_connecting_error_message);
    }

    private boolean ensurePaired() {
        if (getBondState() == BluetoothDevice.BOND_NOT_BONDED) {
            pair();
            return false;
        } else {
            return true;
        }
    }

    public void pair() {
        BluetoothDevice manager = mLocalManager.getBluetoothManager();

        // Pairing is unreliable while scanning, so cancel discovery
        if (manager.isDiscovering()) {
            manager.cancelDiscovery();
        }

        if (!mLocalManager.getBluetoothManager().createBond(mAddress)) {
            mLocalManager.showError(mAddress, R.string.bluetooth_error_title,
                    R.string.bluetooth_pairing_error_message);
        }
    }

    public void unpair() {
        BluetoothDevice manager = mLocalManager.getBluetoothManager();

        switch (getBondState()) {
        case BluetoothDevice.BOND_BONDED:
            manager.removeBond(mAddress);
            break;

        case BluetoothDevice.BOND_BONDING:
            manager.cancelBondProcess(mAddress);
            break;
        }
    }

    private void fillData() {
        BluetoothDevice manager = mLocalManager.getBluetoothManager();

        fetchName();
        fetchBtClass();

        mVisible = false;

        dispatchAttributesChanged();
    }

    public String getAddress() {
        return mAddress;
    }

    public String getName() {
        return mName;
    }

    public void refreshName() {
        fetchName();
        dispatchAttributesChanged();
    }

    private void fetchName() {
        mName = mLocalManager.getBluetoothManager().getRemoteName(mAddress);

        if (TextUtils.isEmpty(mName)) {
            mName = mAddress;
        }
    }

    public void refresh() {
        dispatchAttributesChanged();
    }

    public boolean isVisible() {
        return mVisible;
    }

    void setVisible(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            dispatchAttributesChanged();
        }
    }

    public int getBondState() {
        return mLocalManager.getBluetoothManager().getBondState(mAddress);
    }

    void setRssi(short rssi) {
        if (mRssi != rssi) {
            mRssi = rssi;
            dispatchAttributesChanged();
        }
    }

    /**
     * Checks whether we are connected to this device (any profile counts).
     *
     * @return Whether it is connected.
     */
    public boolean isConnected() {
        for (Profile profile : mProfiles) {
            int status = LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile)
                    .getConnectionStatus(mAddress);
            if (SettingsBtStatus.isConnectionStatusConnected(status)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBusy() {
        for (Profile profile : mProfiles) {
            int status = LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile)
                    .getConnectionStatus(mAddress);
            if (SettingsBtStatus.isConnectionStatusBusy(status)) {
                return true;
            }
        }

        if (getBondState() == BluetoothDevice.BOND_BONDING) {
            return true;
        }

        return false;
    }

    public int getBtClassDrawable() {

        // First try looking at profiles
        if (mProfiles.contains(Profile.A2DP)) {
            return R.drawable.ic_bt_headphones_a2dp;
        } else if (mProfiles.contains(Profile.HEADSET)) {
            return R.drawable.ic_bt_headset_hfp;
        }

        // Fallback on class
        switch (BluetoothClass.Device.Major.getDeviceMajor(mBtClass)) {
        case BluetoothClass.Device.Major.COMPUTER:
            return R.drawable.ic_bt_laptop;

        case BluetoothClass.Device.Major.PHONE:
            return R.drawable.ic_bt_cellphone;

        default:
            return 0;
        }
    }

    /**
     * Fetches a new value for the cached BT class.
     */
    private void fetchBtClass() {
        mBtClass = mLocalManager.getBluetoothManager().getRemoteClass(mAddress);
        mProfiles.clear();
        LocalBluetoothProfileManager.fill(mBtClass, mProfiles);
    }

    /**
     * Refreshes the UI for the BT class, including fetching the latest value
     * for the class.
     */
    public void refreshBtClass() {
        fetchBtClass();
        dispatchAttributesChanged();
    }

    public int getSummary() {
        // TODO: clean up
        int oneOffSummary = getOneOffSummary();
        if (oneOffSummary != 0) {
            return oneOffSummary;
        }

        for (Profile profile : mProfiles) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mLocalManager, profile);
            int connectionStatus = profileManager.getConnectionStatus(mAddress);

            if (SettingsBtStatus.isConnectionStatusConnected(connectionStatus) ||
                    connectionStatus == SettingsBtStatus.CONNECTION_STATUS_CONNECTING ||
                    connectionStatus == SettingsBtStatus.CONNECTION_STATUS_DISCONNECTING) {
                return SettingsBtStatus.getConnectionStatusSummary(connectionStatus);
            }
        }

        return SettingsBtStatus.getPairingStatusSummary(getBondState());
    }

    /**
     * We have special summaries when particular profiles are connected. This
     * checks for those states and returns an applicable summary.
     *
     * @return A one-off summary that is applicable for the current state, or 0.
     */
    private int getOneOffSummary() {
        boolean isA2dpConnected = false, isHeadsetConnected = false, isConnecting = false;

        if (mProfiles.contains(Profile.A2DP)) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mLocalManager, Profile.A2DP);
            isConnecting = profileManager.getConnectionStatus(mAddress) ==
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            isA2dpConnected = profileManager.isConnected(mAddress);
        }

        if (mProfiles.contains(Profile.HEADSET)) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mLocalManager, Profile.HEADSET);
            isConnecting |= profileManager.getConnectionStatus(mAddress) ==
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            isHeadsetConnected = profileManager.isConnected(mAddress);
        }

        if (isConnecting) {
            // If any of these important profiles is connecting, prefer that
            return SettingsBtStatus.getConnectionStatusSummary(
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING);
        } else if (isA2dpConnected && isHeadsetConnected) {
            return R.string.bluetooth_summary_connected_to_a2dp_headset;
        } else if (isA2dpConnected) {
            return R.string.bluetooth_summary_connected_to_a2dp;
        } else if (isHeadsetConnected) {
            return R.string.bluetooth_summary_connected_to_headset;
        } else {
            return 0;
        }
    }

    public List<Profile> getProfiles() {
        return new ArrayList<Profile>(mProfiles);
    }

    public void onCreateContextMenu(ContextMenu menu) {
        // No context menu if it is busy (none of these items are applicable if busy)
        if (isBusy()) return;

        int bondState = getBondState();
        boolean isConnected = isConnected();
        boolean hasProfiles = mProfiles.size() > 0;

        menu.setHeaderTitle(getName());

        if (isConnected) {
            menu.add(0, CONTEXT_ITEM_DISCONNECT, 0, R.string.bluetooth_device_context_disconnect);
        } else if (hasProfiles) {
            // For connection action, show either "Connect" or "Pair & connect"
            int connectString = (bondState == BluetoothDevice.BOND_NOT_BONDED)
                    ? R.string.bluetooth_device_context_pair_connect
                    : R.string.bluetooth_device_context_connect;
            menu.add(0, CONTEXT_ITEM_CONNECT, 0, connectString);
        }

        if (bondState == BluetoothDevice.BOND_BONDED) {
            // For unpair action, show either "Unpair" or "Disconnect & unpair"
            int unpairString = isConnected
                    ? R.string.bluetooth_device_context_disconnect_unpair
                    : R.string.bluetooth_device_context_unpair;
            menu.add(0, CONTEXT_ITEM_UNPAIR, 0, unpairString);

            // Show the connection options item
            menu.add(0, CONTEXT_ITEM_CONNECT_ADVANCED, 0,
                    R.string.bluetooth_device_context_connect_advanced);
        }
    }

    /**
     * Called when a context menu item is clicked.
     *
     * @param item The item that was clicked.
     */
    public void onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CONTEXT_ITEM_DISCONNECT:
                disconnect();
                break;

            case CONTEXT_ITEM_CONNECT:
                connect();
                break;

            case CONTEXT_ITEM_UNPAIR:
                mLocalManager.getBluetoothManager().disconnectRemoteDeviceAcl(mAddress);
                unpair();
                break;

            case CONTEXT_ITEM_CONNECT_ADVANCED:
                Intent intent = new Intent();
                // Need an activity context to open this in our task
                Context context = mLocalManager.getForegroundActivity();
                if (context == null) {
                    // Fallback on application context, and open in a new task
                    context = mLocalManager.getContext();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                intent.setClass(context, ConnectSpecificProfilesActivity.class);
                intent.putExtra(ConnectSpecificProfilesActivity.EXTRA_ADDRESS, mAddress);
                context.startActivity(intent);
                break;
        }
    }

    public void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    private void dispatchAttributesChanged() {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onDeviceAttributesChanged(this);
            }
        }
    }

    @Override
    public String toString() {
        return mAddress;
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof LocalBluetoothDevice)) {
            throw new ClassCastException();
        }

        return mAddress.equals(((LocalBluetoothDevice) o).mAddress);
    }

    @Override
    public int hashCode() {
        return mAddress.hashCode();
    }

    public int compareTo(LocalBluetoothDevice another) {
        int comparison;

        // Connected above not connected
        comparison = (another.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (comparison != 0) return comparison;

        // Paired above not paired
        comparison = (another.getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0) -
            (getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0);
        if (comparison != 0) return comparison;

        // Visible above not visible
        comparison = (another.mVisible ? 1 : 0) - (mVisible ? 1 : 0);
        if (comparison != 0) return comparison;

        // Stronger signal above weaker signal
        comparison = another.mRssi - mRssi;
        if (comparison != 0) return comparison;

        // Fallback on name
        return getName().compareTo(another.getName());
    }

    public interface Callback {
        void onDeviceAttributesChanged(LocalBluetoothDevice device);
    }
}
