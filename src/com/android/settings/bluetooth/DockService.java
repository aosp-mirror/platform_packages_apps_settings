/*
 * Copyright (C) 2009 The Android Open Source Project
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
import com.android.settings.bluetooth.LocalBluetoothProfileManager.ServiceListener;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.List;
import java.util.Set;

public class DockService extends Service implements AlertDialog.OnMultiChoiceClickListener,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
        CompoundButton.OnCheckedChangeListener, ServiceListener {

    private static final String TAG = "DockService";

    static final boolean DEBUG = false;

    // Time allowed for the device to be undocked and redocked without severing
    // the bluetooth connection
    private static final long UNDOCKED_GRACE_PERIOD = 1000;

    // Time allowed for the device to be undocked and redocked without turning
    // off Bluetooth
    private static final long DISABLE_BT_GRACE_PERIOD = 2000;

    // Msg for user wanting the UI to setup the dock
    private static final int MSG_TYPE_SHOW_UI = 111;

    // Msg for device docked event
    private static final int MSG_TYPE_DOCKED = 222;

    // Msg for device undocked event
    private static final int MSG_TYPE_UNDOCKED_TEMPORARY = 333;

    // Msg for undocked command to be process after UNDOCKED_GRACE_PERIOD millis
    // since MSG_TYPE_UNDOCKED_TEMPORARY
    private static final int MSG_TYPE_UNDOCKED_PERMANENT = 444;

    // Msg for disabling bt after DISABLE_BT_GRACE_PERIOD millis since
    // MSG_TYPE_UNDOCKED_PERMANENT
    private static final int MSG_TYPE_DISABLE_BT = 555;

    private static final String SHARED_PREFERENCES_NAME = "dock_settings";

    private static final String SHARED_PREFERENCES_KEY_DISABLE_BT_WHEN_UNDOCKED =
        "disable_bt_when_undock";

    private static final String SHARED_PREFERENCES_KEY_DISABLE_BT =
        "disable_bt";

    private static final String SHARED_PREFERENCES_KEY_CONNECT_RETRY_COUNT =
        "connect_retry_count";

    /*
     * If disconnected unexpectedly, reconnect up to 6 times. Each profile counts
     * as one time so it's only 3 times for both profiles on the car dock.
     */
    private static final int MAX_CONNECT_RETRY = 6;

    private static final int INVALID_STARTID = -100;

    // Created in OnCreate()
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private Runnable mRunnable;
    private DockService mContext;
    private LocalBluetoothManager mBtManager;

    // Normally set after getting a docked event and unset when the connection
    // is severed. One exception is that mDevice could be null if the service
    // was started after the docked event.
    private BluetoothDevice mDevice;

    // Created and used for the duration of the dialog
    private AlertDialog mDialog;
    private Profile[] mProfiles;
    private boolean[] mCheckedItems;
    private int mStartIdAssociatedWithDialog;

    // Set while BT is being enabled.
    private BluetoothDevice mPendingDevice;
    private int mPendingStartId;
    private int mPendingTurnOnStartId = INVALID_STARTID;
    private int mPendingTurnOffStartId = INVALID_STARTID;

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate");

        mBtManager = LocalBluetoothManager.getInstance(this);
        mContext = this;

        HandlerThread thread = new HandlerThread("DockService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        mRunnable = null;
        LocalBluetoothProfileManager.removeServiceListener(this);
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // not supported
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand startId:" + startId + " flags: " + flags);

        if (intent == null) {
            // Nothing to process, stop.
            if (DEBUG) Log.d(TAG, "START_NOT_STICKY - intent is null.");

            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by the Receiver is released.
            DockEventReceiver.finishStartingService(this, startId);
            return START_NOT_STICKY;
        }

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            handleBtStateChange(intent, startId);
            return START_NOT_STICKY;
        }

        /*
         * This assumes that the intent sender has checked that this is a dock
         * and that the intent is for a disconnect
         */
        if (BluetoothHeadset.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            BluetoothDevice disconnectedDevice = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            int retryCount = getSettingInt(SHARED_PREFERENCES_KEY_CONNECT_RETRY_COUNT, 0);
            if (retryCount < MAX_CONNECT_RETRY) {
                setSettingInt(SHARED_PREFERENCES_KEY_CONNECT_RETRY_COUNT, retryCount + 1);
                handleUnexpectedDisconnect(disconnectedDevice, Profile.HEADSET, startId);
            }
            return START_NOT_STICKY;
        } else if (BluetoothA2dp.ACTION_SINK_STATE_CHANGED.equals(intent.getAction())) {
            BluetoothDevice disconnectedDevice = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            int retryCount = getSettingInt(SHARED_PREFERENCES_KEY_CONNECT_RETRY_COUNT, 0);
            if (retryCount < MAX_CONNECT_RETRY) {
                setSettingInt(SHARED_PREFERENCES_KEY_CONNECT_RETRY_COUNT, retryCount + 1);
                handleUnexpectedDisconnect(disconnectedDevice, Profile.A2DP, startId);
            }
            return START_NOT_STICKY;
        }

        Message msg = parseIntent(intent);
        if (msg == null) {
            // Bad intent
            if (DEBUG) Log.d(TAG, "START_NOT_STICKY - Bad intent.");
            DockEventReceiver.finishStartingService(this, startId);
            return START_NOT_STICKY;
        }

        if (msg.what == MSG_TYPE_DOCKED) {
            removeSetting(SHARED_PREFERENCES_KEY_CONNECT_RETRY_COUNT);
        }

        msg.arg2 = startId;
        processMessage(msg);

        return START_NOT_STICKY;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            processMessage(msg);
        }
    }

    // This method gets messages from both onStartCommand and mServiceHandler/mServiceLooper
    private synchronized void processMessage(Message msg) {
        int msgType = msg.what;
        final int state = msg.arg1;
        final int startId = msg.arg2;
        boolean deferFinishCall = false;
        BluetoothDevice device = null;
        if (msg.obj != null) {
            device = (BluetoothDevice) msg.obj;
        }

        if(DEBUG) Log.d(TAG, "processMessage: " + msgType + " state: " + state + " device = "
                + (device == null ? "null" : device.toString()));

        switch (msgType) {
            case MSG_TYPE_SHOW_UI:
                if (mDialog != null) {
                    // Shouldn't normally happen
                    mDialog.dismiss();
                    mDialog = null;
                }
                mDevice = device;
                createDialog(mContext, mDevice, state, startId);
                break;

            case MSG_TYPE_DOCKED:
                if (DEBUG) {
                    // TODO figure out why hasMsg always returns false if device
                    // is supplied
                    Log.d(TAG, "1 Has undock perm msg = "
                            + mServiceHandler.hasMessages(MSG_TYPE_UNDOCKED_PERMANENT, mDevice));
                    Log.d(TAG, "2 Has undock perm msg = "
                            + mServiceHandler.hasMessages(MSG_TYPE_UNDOCKED_PERMANENT, device));
                }

                mServiceHandler.removeMessages(MSG_TYPE_UNDOCKED_PERMANENT);
                mServiceHandler.removeMessages(MSG_TYPE_DISABLE_BT);
                removeSetting(SHARED_PREFERENCES_KEY_DISABLE_BT);

                if (!device.equals(mDevice)) {
                    if (mDevice != null) {
                        // Not expected. Cleanup/undock existing
                        handleUndocked(mContext, mBtManager, mDevice);
                    }

                    mDevice = device;

                    // Register first in case LocalBluetoothProfileManager
                    // becomes ready after isManagerReady is called and it
                    // would be too late to register a service listener.
                    LocalBluetoothProfileManager.addServiceListener(this);
                    if (LocalBluetoothProfileManager.isManagerReady()) {
                        handleDocked(device, state, startId);
                        // Not needed after all
                        LocalBluetoothProfileManager.removeServiceListener(this);
                    } else {
                        final BluetoothDevice d = device;
                        mRunnable = new Runnable() {
                            public void run() {
                                handleDocked(d, state, startId);
                            }
                        };
                        deferFinishCall = true;
                    }
                }
                break;

            case MSG_TYPE_UNDOCKED_PERMANENT:
                // Grace period passed. Disconnect.
                handleUndocked(mContext, mBtManager, device);

                if (DEBUG) {
                    Log.d(TAG, "DISABLE_BT_WHEN_UNDOCKED = "
                            + getSettingBool(SHARED_PREFERENCES_KEY_DISABLE_BT_WHEN_UNDOCKED));
                }

                if (getSettingBool(SHARED_PREFERENCES_KEY_DISABLE_BT_WHEN_UNDOCKED)) {
                    // BT was disabled when we first docked
                    if (!hasOtherConnectedDevices(device)) {
                        if(DEBUG) Log.d(TAG, "QUEUED BT DISABLE");
                        // Queue a delayed msg to disable BT
                        Message newMsg = mServiceHandler.obtainMessage(MSG_TYPE_DISABLE_BT, 0,
                                startId, null);
                        mServiceHandler.sendMessageDelayed(newMsg, DISABLE_BT_GRACE_PERIOD);
                        deferFinishCall = true;
                    } else {
                        // Don't disable BT if something is connected
                        removeSetting(SHARED_PREFERENCES_KEY_DISABLE_BT_WHEN_UNDOCKED);
                    }
                }
                break;

            case MSG_TYPE_UNDOCKED_TEMPORARY:
                // Undocked event received. Queue a delayed msg to sever connection
                Message newMsg = mServiceHandler.obtainMessage(MSG_TYPE_UNDOCKED_PERMANENT, state,
                        startId, device);
                mServiceHandler.sendMessageDelayed(newMsg, UNDOCKED_GRACE_PERIOD);
                break;

            case MSG_TYPE_DISABLE_BT:
                if(DEBUG) Log.d(TAG, "BT DISABLE");
                if (mBtManager.getBluetoothAdapter().disable()) {
                    removeSetting(SHARED_PREFERENCES_KEY_DISABLE_BT_WHEN_UNDOCKED);
                } else {
                    // disable() returned an error. Persist a flag to disable BT later
                    setSettingBool(SHARED_PREFERENCES_KEY_DISABLE_BT, true);
                    mPendingTurnOffStartId = startId;
                    deferFinishCall = true;
                    if(DEBUG) Log.d(TAG, "disable failed. try again later " + startId);
                }
                break;
        }

        if (mDialog == null && mPendingDevice == null && msgType != MSG_TYPE_UNDOCKED_TEMPORARY
                && !deferFinishCall) {
            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by the Receiver is released.
            DockEventReceiver.finishStartingService(DockService.this, startId);
        }
    }

    public synchronized boolean hasOtherConnectedDevices(BluetoothDevice dock) {
        List<CachedBluetoothDevice> cachedDevices = mBtManager.getCachedDeviceManager()
                .getCachedDevicesCopy();
        Set<BluetoothDevice> btDevices = mBtManager.getBluetoothAdapter().getBondedDevices();
        if (btDevices == null || cachedDevices == null || btDevices.size() == 0) {
            return false;
        }
        if(DEBUG) Log.d(TAG, "btDevices = " + btDevices.size());
        if(DEBUG) Log.d(TAG, "cachedDevices = " + cachedDevices.size());

        for (CachedBluetoothDevice device : cachedDevices) {
            BluetoothDevice btDevice = device.getDevice();
            if (!btDevice.equals(dock) && btDevices.contains(btDevice) && device.isConnected()) {
                if(DEBUG) Log.d(TAG, "connected device = " + device.getName());
                return true;
            }
        }
        return false;
    }

    private Message parseIntent(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1234);

        if (DEBUG) {
            Log.d(TAG, "Action: " + intent.getAction() + " State:" + state
                    + " Device: " + (device == null ? "null" : device.getName()));
        }

        if (device == null) {
            Log.w(TAG, "device is null");
            return null;
        }

        int msgType;
        switch (state) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                msgType = MSG_TYPE_UNDOCKED_TEMPORARY;
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_CAR:
                if (DockEventReceiver.ACTION_DOCK_SHOW_UI.equals(intent.getAction())) {
                    msgType = MSG_TYPE_SHOW_UI;
                } else {
                    msgType = MSG_TYPE_DOCKED;
                }
                break;
            default:
                return null;
        }

        return mServiceHandler.obtainMessage(msgType, state, 0, device);
    }

    private boolean createDialog(DockService service, BluetoothDevice device, int state,
            int startId) {
        switch (state) {
            case Intent.EXTRA_DOCK_STATE_CAR:
            case Intent.EXTRA_DOCK_STATE_DESK:
                break;
            default:
                return false;
        }

        startForeground(0, new Notification());

        // Device in a new dock.
        boolean firstTime = !mBtManager.hasDockAutoConnectSetting(device.getAddress());

        CharSequence[] items = initBtSettings(service, device, state, firstTime);

        final AlertDialog.Builder ab = new AlertDialog.Builder(service);
        ab.setTitle(service.getString(R.string.bluetooth_dock_settings_title));

        // Profiles
        ab.setMultiChoiceItems(items, mCheckedItems, service);

        // Remember this settings
        LayoutInflater inflater = (LayoutInflater) service
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        float pixelScaleFactor = service.getResources().getDisplayMetrics().density;
        View view = inflater.inflate(R.layout.remember_dock_setting, null);
        CheckBox rememberCheckbox = (CheckBox) view.findViewById(R.id.remember);

        // check "Remember setting" by default if no value was saved
        boolean checked = firstTime || mBtManager.getDockAutoConnectSetting(device.getAddress());
        rememberCheckbox.setChecked(checked);
        rememberCheckbox.setOnCheckedChangeListener(this);
        int viewSpacingLeft = (int) (14 * pixelScaleFactor);
        int viewSpacingRight = (int) (14 * pixelScaleFactor);
        ab.setView(view, viewSpacingLeft, 0 /* top */, viewSpacingRight, 0 /* bottom */);
        if (DEBUG) {
            Log.d(TAG, "Auto connect = "
                    + mBtManager.getDockAutoConnectSetting(device.getAddress()));
        }

        // Ok Button
        ab.setPositiveButton(service.getString(android.R.string.ok), service);

        mStartIdAssociatedWithDialog = startId;
        mDialog = ab.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        mDialog.setOnDismissListener(service);
        mDialog.show();
        return true;
    }

    // Called when the individual bt profiles are clicked.
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (DEBUG) Log.d(TAG, "Item " + which + " changed to " + isChecked);
        mCheckedItems[which] = isChecked;
    }

    // Called when the "Remember" Checkbox is clicked
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (DEBUG) Log.d(TAG, "onCheckedChanged: Remember Settings = " + isChecked);
        if (mDevice != null) {
            mBtManager.saveDockAutoConnectSetting(mDevice.getAddress(), isChecked);
        }
    }

    // Called when the dialog is dismissed
    public void onDismiss(DialogInterface dialog) {
        // NOTE: We MUST not call stopSelf() directly, since we need to
        // make sure the wake lock acquired by the Receiver is released.
        if (mPendingDevice == null) {
            DockEventReceiver.finishStartingService(mContext, mStartIdAssociatedWithDialog);
        }
        mContext.stopForeground(true);
    }

    // Called when clicked on the OK button
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE && mDevice != null) {
            if (!mBtManager.hasDockAutoConnectSetting(mDevice.getAddress())) {
                mBtManager.saveDockAutoConnectSetting(mDevice.getAddress(), true);
            }

            applyBtSettings(mDevice, mStartIdAssociatedWithDialog);
        }
    }

    private CharSequence[] initBtSettings(DockService service, BluetoothDevice device, int state,
            boolean firstTime) {
        // TODO Avoid hardcoding dock and profiles. Read from system properties
        int numOfProfiles = 0;
        switch (state) {
            case Intent.EXTRA_DOCK_STATE_DESK:
                numOfProfiles = 1;
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                numOfProfiles = 2;
                break;
            default:
                return null;
        }

        mProfiles = new Profile[numOfProfiles];
        mCheckedItems = new boolean[numOfProfiles];
        CharSequence[] items = new CharSequence[numOfProfiles];

        switch (state) {
            case Intent.EXTRA_DOCK_STATE_CAR:
                items[0] = service.getString(R.string.bluetooth_dock_settings_headset);
                items[1] = service.getString(R.string.bluetooth_dock_settings_a2dp);
                mProfiles[0] = Profile.HEADSET;
                mProfiles[1] = Profile.A2DP;
                if (firstTime) {
                    // Enable by default for car dock
                    mCheckedItems[0] = true;
                    mCheckedItems[1] = true;
                } else {
                    mCheckedItems[0] = LocalBluetoothProfileManager.getProfileManager(mBtManager,
                            Profile.HEADSET).isPreferred(device);
                    mCheckedItems[1] = LocalBluetoothProfileManager.getProfileManager(mBtManager,
                            Profile.A2DP).isPreferred(device);
                }
                break;

            case Intent.EXTRA_DOCK_STATE_DESK:
                items[0] = service.getString(R.string.bluetooth_dock_settings_a2dp);
                mProfiles[0] = Profile.A2DP;
                if (firstTime) {
                    // Disable by default for desk dock
                    mCheckedItems[0] = false;
                } else {
                    mCheckedItems[0] = LocalBluetoothProfileManager.getProfileManager(mBtManager,
                            Profile.A2DP).isPreferred(device);
                }
                break;
        }
        return items;
    }

    private void handleBtStateChange(Intent intent, int startId) {
        int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        synchronized (this) {
            if(DEBUG) Log.d(TAG, "BtState = " + btState + " mPendingDevice = " + mPendingDevice);
            if (btState == BluetoothAdapter.STATE_ON) {
                if (mPendingDevice != null) {
                    if (mPendingDevice.equals(mDevice)) {
                        if(DEBUG) Log.d(TAG, "applying settings");
                        applyBtSettings(mPendingDevice, mPendingStartId);
                    } else if(DEBUG) {
                        Log.d(TAG, "mPendingDevice  (" + mPendingDevice + ") != mDevice ("
                                + mDevice + ")");
                    }

                    mPendingDevice = null;
                    DockEventReceiver.finishStartingService(mContext, mPendingStartId);
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "A DISABLE_BT_WHEN_UNDOCKED = "
                                + getSettingBool(SHARED_PREFERENCES_KEY_DISABLE_BT_WHEN_UNDOCKED));
                    }
                    // Reconnect if docked and bluetooth was enabled by user.
                    Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_DOCK_EVENT));
                    if (i != null) {
                        int state = i.getIntExtra(Intent.EXTRA_DOCK_STATE,
                                Intent.EXTRA_DOCK_STATE_UNDOCKED);
                        if (state != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                            BluetoothDevice device = i
                                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (device != null) {
                                connectIfEnabled(device);
                            }
                        } else if (getSettingBool(SHARED_PREFERENCES_KEY_DISABLE_BT)
                                && mBtManager.getBluetoothAdapter().disable()) {
                            mPendingTurnOffStartId = startId;
                            removeSetting(SHARED_PREFERENCES_KEY_DISABLE_BT);
                            return;
                        }
                    }
                }

                if (mPendingTurnOnStartId != INVALID_STARTID) {
                    DockEventReceiver.finishStartingService(this, mPendingTurnOnStartId);
                    mPendingTurnOnStartId = INVALID_STARTID;
                }

                DockEventReceiver.finishStartingService(this, startId);
            } else if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
                // Remove the flag to disable BT if someone is turning off bt.
                // The rational is that:
                // a) if BT is off at undock time, no work needs to be done
                // b) if BT is on at undock time, the user wants it on.
                removeSetting(SHARED_PREFERENCES_KEY_DISABLE_BT_WHEN_UNDOCKED);
                DockEventReceiver.finishStartingService(this, startId);
            } else if (btState == BluetoothAdapter.STATE_OFF) {
                // Bluetooth was turning off as we were trying to turn it on.
                // Let's try again
                if(DEBUG) Log.d(TAG, "Bluetooth = OFF mPendingDevice = " + mPendingDevice);

                if (mPendingTurnOffStartId != INVALID_STARTID) {
                    DockEventReceiver.finishStartingService(this, mPendingTurnOffStartId);
                    removeSetting(SHARED_PREFERENCES_KEY_DISABLE_BT);
                    mPendingTurnOffStartId = INVALID_STARTID;
                }

                if (mPendingDevice != null) {
                    mBtManager.getBluetoothAdapter().enable();
                    mPendingTurnOnStartId = startId;
                } else {
                    DockEventReceiver.finishStartingService(this, startId);
                }
            }
        }
    }

    private void handleUnexpectedDisconnect(BluetoothDevice disconnectedDevice, Profile profile,
            int startId) {
        synchronized (this) {
            if (DEBUG) Log.d(TAG, "handling failed connect for " + disconnectedDevice);

            // Reconnect if docked.
            if (disconnectedDevice != null) {
                // registerReceiver can't be called from a BroadcastReceiver
                Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_DOCK_EVENT));
                if (i != null) {
                    int state = i.getIntExtra(Intent.EXTRA_DOCK_STATE,
                            Intent.EXTRA_DOCK_STATE_UNDOCKED);
                    if (state != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                        BluetoothDevice dockedDevice = i
                                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (dockedDevice != null && dockedDevice.equals(disconnectedDevice)) {
                            CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(mContext,
                                    mBtManager, dockedDevice);
                            cachedDevice.connect(profile);
                        }
                    }
                }
            }

            DockEventReceiver.finishStartingService(this, startId);
        }
    }

    private synchronized void connectIfEnabled(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(mContext, mBtManager, device);
        List<Profile> profiles = cachedDevice.getConnectableProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mBtManager, profiles.get(i));
            int auto;
            if (Profile.A2DP == profiles.get(i)) {
                auto = BluetoothA2dp.PRIORITY_AUTO_CONNECT;
            } else if (Profile.HEADSET == profiles.get(i)) {
                auto = BluetoothHeadset.PRIORITY_AUTO_CONNECT;
            } else {
                continue;
            }

            if (profileManager.getPreferred(device) == auto) {
                cachedDevice.connect();
                break;
            }
        }
    }

    private synchronized void applyBtSettings(final BluetoothDevice device, int startId) {
        if (device == null || mProfiles == null || mCheckedItems == null)
            return;

        // Turn on BT if something is enabled
        synchronized (this) {
            for (boolean enable : mCheckedItems) {
                if (enable) {
                    int btState = mBtManager.getBluetoothState();
                    if(DEBUG) Log.d(TAG, "BtState = " + btState);
                    // May have race condition as the phone comes in and out and in the dock.
                    // Always turn on BT
                    mBtManager.getBluetoothAdapter().enable();

                    switch (btState) {
                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_TURNING_ON:
                            if (mPendingDevice != null && mPendingDevice.equals(mDevice)) {
                                return;
                            }

                            mPendingDevice = device;
                            mPendingStartId = startId;
                            if (btState != BluetoothAdapter.STATE_TURNING_ON) {
                                setSettingBool(SHARED_PREFERENCES_KEY_DISABLE_BT_WHEN_UNDOCKED,
                                        true);
                            }
                            return;
                    }
                }
            }
        }

        mPendingDevice = null;

        boolean callConnect = false;
        CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(mContext, mBtManager,
                device);
        for (int i = 0; i < mProfiles.length; i++) {
            LocalBluetoothProfileManager profileManager = LocalBluetoothProfileManager
                    .getProfileManager(mBtManager, mProfiles[i]);

            if (DEBUG) Log.d(TAG, mProfiles[i].toString() + " = " + mCheckedItems[i]);

            if (mCheckedItems[i]) {
                // Checked but not connected
                callConnect = true;
            } else if (!mCheckedItems[i]) {
                // Unchecked, may or may not be connected.
                int status = profileManager.getConnectionStatus(cachedDevice.getDevice());
                if (SettingsBtStatus.isConnectionStatusConnected(status)) {
                    if (DEBUG) Log.d(TAG, "applyBtSettings - Disconnecting");
                    cachedDevice.disconnect(mProfiles[i]);
                }
            }
            profileManager.setPreferred(device, mCheckedItems[i]);
            if (DEBUG) {
                if (mCheckedItems[i] != profileManager.isPreferred(device)) {
                    Log.e(TAG, "Can't save prefered value");
                }
            }
        }

        if (callConnect) {
            if (DEBUG) Log.d(TAG, "applyBtSettings - Connecting");
            cachedDevice.connect();
        }
    }

    private synchronized void handleDocked(final BluetoothDevice device, final int state,
            final int startId) {
        if (mBtManager.getDockAutoConnectSetting(device.getAddress())) {
            // Setting == auto connect
            initBtSettings(mContext, device, state, false);
            applyBtSettings(mDevice, startId);
        } else {
            createDialog(mContext, device, state, startId);
        }
    }

    private synchronized void handleUndocked(Context context, LocalBluetoothManager localManager,
            BluetoothDevice device) {
        mRunnable = null;
        LocalBluetoothProfileManager.removeServiceListener(this);
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mDevice = null;
        mPendingDevice = null;
        CachedBluetoothDevice cachedBluetoothDevice = getCachedBluetoothDevice(context,
                localManager, device);
        cachedBluetoothDevice.disconnect();
    }

    private static CachedBluetoothDevice getCachedBluetoothDevice(Context context,
            LocalBluetoothManager localManager, BluetoothDevice device) {
        CachedBluetoothDeviceManager cachedDeviceManager = localManager.getCachedDeviceManager();
        CachedBluetoothDevice cachedBluetoothDevice = cachedDeviceManager.findDevice(device);
        if (cachedBluetoothDevice == null) {
            cachedBluetoothDevice = new CachedBluetoothDevice(context, device);
        }
        return cachedBluetoothDevice;
    }

    private boolean getSettingBool(String key) {
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, false);
    }

    private int getSettingInt(String key, int defaultValue) {
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return sharedPref.getInt(key, defaultValue);
    }

    private void setSettingBool(String key, boolean bool) {
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE).edit();
        editor.putBoolean(key, bool);
        editor.apply();
    }

    private void setSettingInt(String key, int value) {
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void removeSetting(String key) {
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(key);
        editor.apply();
    }

    public synchronized void onServiceConnected() {
        if (mRunnable != null) {
            mRunnable.run();
            mRunnable = null;
            LocalBluetoothProfileManager.removeServiceListener(this);
        }
    }

    public void onServiceDisconnected() {
    }
}
