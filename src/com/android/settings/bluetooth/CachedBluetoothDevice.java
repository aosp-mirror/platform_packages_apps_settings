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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothProfileManager.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CachedBluetoothDevice represents a remote Bluetooth device. It contains
 * attributes of the device (such as the address, name, RSSI, etc.) and
 * functionality that can be performed on the device (connect, pair, disconnect,
 * etc.).
 */
public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {
    private static final String TAG = "CachedBluetoothDevice";
    private static final boolean D = LocalBluetoothManager.D;
    private static final boolean V = LocalBluetoothManager.V;
    private static final boolean DEBUG = false;

    private static final int CONTEXT_ITEM_CONNECT = Menu.FIRST + 1;
    private static final int CONTEXT_ITEM_DISCONNECT = Menu.FIRST + 2;
    private static final int CONTEXT_ITEM_UNPAIR = Menu.FIRST + 3;
    private static final int CONTEXT_ITEM_CONNECT_ADVANCED = Menu.FIRST + 4;

    private final BluetoothDevice mDevice;
    private String mName;
    private short mRssi;
    private BluetoothClass mBtClass;

    private List<Profile> mProfiles = new ArrayList<Profile>();

    private boolean mVisible;

    private final LocalBluetoothManager mLocalManager;

    private List<Callback> mCallbacks = new ArrayList<Callback>();

    /**
     * When we connect to multiple profiles, we only want to display a single
     * error even if they all fail. This tracks that state.
     */
    private boolean mIsConnectingErrorPossible;

    /**
     * Last time a bt profile auto-connect was attempted.
     * If an ACTION_UUID intent comes in within
     * MAX_UUID_DELAY_FOR_AUTO_CONNECT milliseconds, we will try auto-connect
     * again with the new UUIDs
     */
    private long mConnectAttempted;

    // See mConnectAttempted
    private static final long MAX_UUID_DELAY_FOR_AUTO_CONNECT = 5000;

    /** Auto-connect after pairing only if locally initiated. */
    private boolean mConnectAfterPairing;

    /**
     * Describes the current device and profile for logging.
     *
     * @param profile Profile to describe
     * @return Description of the device and profile
     */
    private String describe(CachedBluetoothDevice cachedDevice, Profile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address:").append(cachedDevice.mDevice);
        if (profile != null) {
            sb.append(" Profile:").append(profile.name());
        }

        return sb.toString();
    }

    private String describe(Profile profile) {
        return describe(this, profile);
    }

    public void onProfileStateChanged(Profile profile, int newProfileState) {
        if (D) {
            Log.d(TAG, "onProfileStateChanged: profile " + profile.toString() +
                    " newProfileState " + newProfileState);
        }

        int newState = LocalBluetoothProfileManager.getProfileManager(mLocalManager,
                profile).convertState(newProfileState);

        if (newState == SettingsBtStatus.CONNECTION_STATUS_CONNECTED) {
            if (!mProfiles.contains(profile)) {
                mProfiles.add(profile);
            }
        }
    }

    CachedBluetoothDevice(Context context, BluetoothDevice device) {
        mLocalManager = LocalBluetoothManager.getInstance(context);
        if (mLocalManager == null) {
            throw new IllegalStateException(
                    "Cannot use CachedBluetoothDevice without Bluetooth hardware");
        }

        mDevice = device;

        fillData();
    }

    public void onClicked() {
        int bondState = getBondState();

        if (isConnected()) {
            askDisconnect();
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            connect();
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            pair();
        }
    }

    public void disconnect() {
        for (Profile profile : mProfiles) {
            disconnect(profile);
        }
    }

    public void disconnect(Profile profile) {
        disconnectInt(this, profile);
    }

    private boolean disconnectInt(CachedBluetoothDevice cachedDevice, Profile profile) {
        LocalBluetoothProfileManager profileManager =
                LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
        int status = profileManager.getConnectionStatus(cachedDevice.mDevice);
        if (profileManager.disconnect(cachedDevice.mDevice)) {
            if (D) {
                Log.d(TAG, "Command sent successfully:DISCONNECT " + describe(profile));
            }
            return true;
        }
        return false;
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

        new AlertDialog.Builder(context)
                .setTitle(getName())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, disconnectListener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void connect() {
        if (!ensurePaired()) return;

        mConnectAttempted = SystemClock.elapsedRealtime();

        connectWithoutResettingTimer();
    }

    /*package*/ void onBondingDockConnect() {
        // Attempt to connect if UUIDs are available. Otherwise,
        // we will connect when the ACTION_UUID intent arrives.
        connect();
    }

    private void connectWithoutResettingTimer() {
        // Try to initialize the profiles if there were not.
        if (mProfiles.size() == 0) {
            if (!updateProfiles()) {
                // If UUIDs are not available yet, connect will be happen
                // upon arrival of the ACTION_UUID intent.
                if (DEBUG) Log.d(TAG, "No profiles. Maybe we will connect later");
                return;
            }
        }

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        int preferredProfiles = 0;
        for (Profile profile : mProfiles) {
            if (isConnectableProfile(profile)) {
                LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                        .getProfileManager(mLocalManager, profile);
                if (profileManager.isPreferred(mDevice)) {
                    ++preferredProfiles;
                    disconnectConnected(this, profile);
                    connectInt(this, profile);
                }
            }
        }
        if (DEBUG) Log.d(TAG, "Preferred profiles = " + preferredProfiles);

        if (preferredProfiles == 0) {
            connectAllProfiles();
        }
    }

    private void connectAllProfiles() {
        if (!ensurePaired()) return;

        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;

        for (Profile profile : mProfiles) {
            if (isConnectableProfile(profile)) {
                LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                        .getProfileManager(mLocalManager, profile);
                profileManager.setPreferred(mDevice, false);
                disconnectConnected(this, profile);
                connectInt(this, profile);
            }
        }
    }

    public void connect(Profile profile) {
        mConnectAttempted = SystemClock.elapsedRealtime();
        // Reset the only-show-one-error-dialog tracking variable
        mIsConnectingErrorPossible = true;
        disconnectConnected(this, profile);
        connectInt(this, profile);
    }

    private void disconnectConnected(CachedBluetoothDevice device, Profile profile) {
        LocalBluetoothProfileManager profileManager =
            LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
        CachedBluetoothDeviceManager cachedDeviceManager = mLocalManager.getCachedDeviceManager();
        Set<BluetoothDevice> devices = profileManager.getConnectedDevices();
        if (devices == null) return;
        for (BluetoothDevice btDevice : devices) {
            CachedBluetoothDevice cachedDevice = cachedDeviceManager.findDevice(btDevice);

            if (cachedDevice != null && !cachedDevice.equals(device)) {
                disconnectInt(cachedDevice, profile);
            }
        }
    }

    private boolean connectInt(CachedBluetoothDevice cachedDevice, Profile profile) {
        if (!cachedDevice.ensurePaired()) return false;

        LocalBluetoothProfileManager profileManager =
                LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile);
        int status = profileManager.getConnectionStatus(cachedDevice.mDevice);
        if (profileManager.connect(cachedDevice.mDevice)) {
            if (D) {
                Log.d(TAG, "Command sent successfully:CONNECT " + describe(profile));
            }
            return true;
        }
        Log.i(TAG, "Failed to connect " + profile.toString() + " to " + cachedDevice.mName);

        return false;
    }

    public void showConnectingError() {
        if (!mIsConnectingErrorPossible) return;
        mIsConnectingErrorPossible = false;

        mLocalManager.showError(mDevice, R.string.bluetooth_error_title,
                R.string.bluetooth_connecting_error_message);
    }

    private boolean ensurePaired() {
        if (getBondState() == BluetoothDevice.BOND_NONE) {
            pair();
            return false;
        } else {
            return true;
        }
    }

    public void pair() {
        BluetoothAdapter adapter = mLocalManager.getBluetoothAdapter();

        // Pairing is unreliable while scanning, so cancel discovery
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }

        if (!mDevice.createBond()) {
            mLocalManager.showError(mDevice, R.string.bluetooth_error_title,
                    R.string.bluetooth_pairing_error_message);
            return;
        }

        mConnectAfterPairing = true;  // auto-connect after pairing
    }

    public void unpair() {
        disconnect();

        int state = getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            mDevice.cancelBondProcess();
        }

        if (state != BluetoothDevice.BOND_NONE) {
            final BluetoothDevice dev = getDevice();
            if (dev != null) {
                final boolean successful = dev.removeBond();
                if (successful) {
                    if (D) {
                        Log.d(TAG, "Command sent successfully:REMOVE_BOND " + describe(null));
                    }
                } else if (V) {
                    Log.v(TAG, "Framework rejected command immediately:REMOVE_BOND " +
                            describe(null));
                }
            }
        }
    }

    private void fillData() {
        fetchName();
        fetchBtClass();
        updateProfiles();

        mVisible = false;

        dispatchAttributesChanged();
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        if (!mName.equals(name)) {
            if (TextUtils.isEmpty(name)) {
                mName = mDevice.getAddress();
            } else {
                mName = name;
            }
            dispatchAttributesChanged();
        }
    }

    public void refreshName() {
        fetchName();
        dispatchAttributesChanged();
    }

    private void fetchName() {
        mName = mDevice.getName();

        if (TextUtils.isEmpty(mName)) {
            mName = mDevice.getAddress();
            if (DEBUG) Log.d(TAG, "Default to address. Device has no name (yet) " + mName);
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
        return mDevice.getBondState();
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
                    .getConnectionStatus(mDevice);
            if (SettingsBtStatus.isConnectionStatusConnected(status)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBusy() {
        for (Profile profile : mProfiles) {
            int status = LocalBluetoothProfileManager.getProfileManager(mLocalManager, profile)
                    .getConnectionStatus(mDevice);
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
        if (mBtClass != null) {
            switch (mBtClass.getMajorDeviceClass()) {
            case BluetoothClass.Device.Major.COMPUTER:
                return R.drawable.ic_bt_laptop;

            case BluetoothClass.Device.Major.PHONE:
                return R.drawable.ic_bt_cellphone;
            }
        } else {
            Log.w(TAG, "mBtClass is null");
        }

        if (mProfiles.size() > 0) {
            if (mProfiles.contains(Profile.A2DP)) {
                return R.drawable.ic_bt_headphones_a2dp;
            } else if (mProfiles.contains(Profile.HEADSET)) {
                return R.drawable.ic_bt_headset_hfp;
            }
        } else if (mBtClass != null) {
            if (mBtClass.doesClassMatch(BluetoothClass.PROFILE_A2DP)) {
                return R.drawable.ic_bt_headphones_a2dp;

            }
            if (mBtClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) {
                return R.drawable.ic_bt_headset_hfp;
            }
        }
        return 0;
    }

    /**
     * Fetches a new value for the cached BT class.
     */
    private void fetchBtClass() {
        mBtClass = mDevice.getBluetoothClass();
    }

    private boolean updateProfiles() {
        ParcelUuid[] uuids = mDevice.getUuids();
        if (uuids == null) return false;

        LocalBluetoothProfileManager.updateProfiles(uuids, mProfiles);

        if (DEBUG) {
            Log.e(TAG, "updating profiles for " + mDevice.getName());

            boolean printUuids = true;
            BluetoothClass bluetoothClass = mDevice.getBluetoothClass();

            if (bluetoothClass != null) {
                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET) !=
                    mProfiles.contains(Profile.HEADSET)) {
                    Log.v(TAG, "headset classbits != uuid");
                    printUuids = true;
                }

                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_A2DP) !=
                    mProfiles.contains(Profile.A2DP)) {
                    Log.v(TAG, "a2dp classbits != uuid");
                    printUuids = true;
                }

                if (bluetoothClass.doesClassMatch(BluetoothClass.PROFILE_OPP) !=
                    mProfiles.contains(Profile.OPP)) {
                    Log.v(TAG, "opp classbits != uuid");
                    printUuids = true;
                }
            }

            if (printUuids) {
                if (bluetoothClass != null) Log.v(TAG, "Class: " + bluetoothClass.toString());
                Log.v(TAG, "UUID:");
                for (int i = 0; i < uuids.length; i++) {
                    Log.v(TAG, "  " + uuids[i]);
                }
            }
        }
        return true;
    }

    /**
     * Refreshes the UI for the BT class, including fetching the latest value
     * for the class.
     */
    public void refreshBtClass() {
        fetchBtClass();
        dispatchAttributesChanged();
    }

    /**
     * Refreshes the UI when framework alerts us of a UUID change.
     */
    public void onUuidChanged() {
        updateProfiles();

        if (DEBUG) {
            Log.e(TAG, "onUuidChanged: Time since last connect"
                    + (SystemClock.elapsedRealtime() - mConnectAttempted));
        }

        /*
         * If a connect was attempted earlier without any UUID, we will do the
         * connect now.
         */
        if (mProfiles.size() > 0
                && (mConnectAttempted + MAX_UUID_DELAY_FOR_AUTO_CONNECT) > SystemClock
                        .elapsedRealtime()) {
            connectWithoutResettingTimer();
        }
        dispatchAttributesChanged();
    }

    public void onBondingStateChanged(int bondState) {
        if (bondState == BluetoothDevice.BOND_NONE) {
            mProfiles.clear();
            mConnectAfterPairing = false;  // cancel auto-connect
        }

        refresh();

        if (bondState == BluetoothDevice.BOND_BONDED) {
            if (mDevice.isBluetoothDock()) {
                onBondingDockConnect();
            } else if (mConnectAfterPairing) {
                connect();
            }
            mConnectAfterPairing = false;
        }
    }

    public void setBtClass(BluetoothClass btClass) {
        if (btClass != null && mBtClass != btClass) {
            mBtClass = btClass;
            dispatchAttributesChanged();
        }
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
            int connectionStatus = profileManager.getConnectionStatus(mDevice);

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
            isConnecting = profileManager.getConnectionStatus(mDevice) ==
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            isA2dpConnected = profileManager.isConnected(mDevice);
        }

        if (mProfiles.contains(Profile.HEADSET)) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mLocalManager, Profile.HEADSET);
            isConnecting |= profileManager.getConnectionStatus(mDevice) ==
                    SettingsBtStatus.CONNECTION_STATUS_CONNECTING;
            isHeadsetConnected = profileManager.isConnected(mDevice);
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

    public List<Profile> getConnectableProfiles() {
        ArrayList<Profile> connectableProfiles = new ArrayList<Profile>();
        for (Profile profile : mProfiles) {
            if (isConnectableProfile(profile)) {
                connectableProfiles.add(profile);
            }
        }
        return connectableProfiles;
    }

    private boolean isConnectableProfile(Profile profile) {
        return profile.equals(Profile.HEADSET) || profile.equals(Profile.A2DP);
    }

    public void onCreateContextMenu(ContextMenu menu) {
        // No context menu if it is busy (none of these items are applicable if busy)
        if (mLocalManager.getBluetoothState() != BluetoothAdapter.STATE_ON || isBusy()) {
            return;
        }

        int bondState = getBondState();
        boolean isConnected = isConnected();
        boolean hasConnectableProfiles = false;

        for (Profile profile : mProfiles) {
            if (isConnectableProfile(profile)) {
                hasConnectableProfiles = true;
                break;
            }
        }

        menu.setHeaderTitle(getName());

        if (bondState == BluetoothDevice.BOND_NONE) { // Not paired and not connected
            menu.add(0, CONTEXT_ITEM_CONNECT, 0, R.string.bluetooth_device_context_pair_connect);
        } else { // Paired
            if (isConnected) { // Paired and connected
                menu.add(0, CONTEXT_ITEM_DISCONNECT, 0,
                        R.string.bluetooth_device_context_disconnect);
                menu.add(0, CONTEXT_ITEM_UNPAIR, 0,
                        R.string.bluetooth_device_context_disconnect_unpair);
            } else { // Paired but not connected
                if (hasConnectableProfiles) {
                    menu.add(0, CONTEXT_ITEM_CONNECT, 0, R.string.bluetooth_device_context_connect);
                }
                menu.add(0, CONTEXT_ITEM_UNPAIR, 0, R.string.bluetooth_device_context_unpair);
            }

            // Show the connection options item
            if (hasConnectableProfiles) {
                menu.add(0, CONTEXT_ITEM_CONNECT_ADVANCED, 0,
                        R.string.bluetooth_device_context_connect_advanced);
            }
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
                intent.putExtra(ConnectSpecificProfilesActivity.EXTRA_DEVICE, mDevice);
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
        return mDevice.toString();
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof CachedBluetoothDevice)) {
            throw new ClassCastException();
        }

        return mDevice.equals(((CachedBluetoothDevice) o).mDevice);
    }

    @Override
    public int hashCode() {
        return mDevice.getAddress().hashCode();
    }

    public int compareTo(CachedBluetoothDevice another) {
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
        void onDeviceAttributesChanged(CachedBluetoothDevice cachedDevice);
    }
}
