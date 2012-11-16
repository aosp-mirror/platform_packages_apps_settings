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
import com.android.settings.bluetooth.LocalBluetoothProfileManager.ServiceListener;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class DockService extends Service implements ServiceListener {

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

    private static final String KEY_DISABLE_BT_WHEN_UNDOCKED = "disable_bt_when_undock";

    private static final String KEY_DISABLE_BT = "disable_bt";

    private static final String KEY_CONNECT_RETRY_COUNT = "connect_retry_count";

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
    private LocalBluetoothAdapter mLocalAdapter;
    private CachedBluetoothDeviceManager mDeviceManager;
    private LocalBluetoothProfileManager mProfileManager;

    // Normally set after getting a docked event and unset when the connection
    // is severed. One exception is that mDevice could be null if the service
    // was started after the docked event.
    private BluetoothDevice mDevice;

    // Created and used for the duration of the dialog
    private AlertDialog mDialog;
    private LocalBluetoothProfile[] mProfiles;
    private boolean[] mCheckedItems;
    private int mStartIdAssociatedWithDialog;

    // Set while BT is being enabled.
    private BluetoothDevice mPendingDevice;
    private int mPendingStartId;
    private int mPendingTurnOnStartId = INVALID_STARTID;
    private int mPendingTurnOffStartId = INVALID_STARTID;

    private CheckBox mAudioMediaCheckbox;

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate");

        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(this);
        if (manager == null) {
            Log.e(TAG, "Can't get LocalBluetoothManager: exiting");
            return;
        }

        mLocalAdapter = manager.getBluetoothAdapter();
        mDeviceManager = manager.getCachedDeviceManager();
        mProfileManager = manager.getProfileManager();
        if (mProfileManager == null) {
            Log.e(TAG, "Can't get LocalBluetoothProfileManager: exiting");
            return;
        }

        HandlerThread thread = new HandlerThread("DockService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        mRunnable = null;
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mProfileManager != null) {
            mProfileManager.removeServiceListener(this);
        }
        if (mServiceLooper != null) {
            mServiceLooper.quit();
        }

        mLocalAdapter = null;
        mDeviceManager = null;
        mProfileManager = null;
        mServiceLooper = null;
        mServiceHandler = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // not supported
        return null;
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand startId: " + startId + " flags: " + flags);

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
        final SharedPreferences prefs = getPrefs();
        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
            BluetoothDevice disconnectedDevice = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int retryCount = prefs.getInt(KEY_CONNECT_RETRY_COUNT, 0);
            if (retryCount < MAX_CONNECT_RETRY) {
                prefs.edit().putInt(KEY_CONNECT_RETRY_COUNT, retryCount + 1).apply();
                handleUnexpectedDisconnect(disconnectedDevice, mProfileManager.getHeadsetProfile(), startId);
            }
            return START_NOT_STICKY;
        } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
            BluetoothDevice disconnectedDevice = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            int retryCount = prefs.getInt(KEY_CONNECT_RETRY_COUNT, 0);
            if (retryCount < MAX_CONNECT_RETRY) {
                prefs.edit().putInt(KEY_CONNECT_RETRY_COUNT, retryCount + 1).apply();
                handleUnexpectedDisconnect(disconnectedDevice, mProfileManager.getA2dpProfile(), startId);
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
            prefs.edit().remove(KEY_CONNECT_RETRY_COUNT).apply();
        }

        msg.arg2 = startId;
        processMessage(msg);

        return START_NOT_STICKY;
    }

    private final class ServiceHandler extends Handler {
        private ServiceHandler(Looper looper) {
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
        BluetoothDevice device = null;
        if (msg.obj != null) {
            device = (BluetoothDevice) msg.obj;
        }

        if(DEBUG) Log.d(TAG, "processMessage: " + msgType + " state: " + state + " device = "
                + (device == null ? "null" : device.toString()));

        boolean deferFinishCall = false;

        switch (msgType) {
            case MSG_TYPE_SHOW_UI:
                if (device != null) {
                    createDialog(device, state, startId);
                }
                break;

            case MSG_TYPE_DOCKED:
                deferFinishCall = msgTypeDocked(device, state, startId);
                break;

            case MSG_TYPE_UNDOCKED_PERMANENT:
                deferFinishCall = msgTypeUndockedPermanent(device, startId);
                break;

            case MSG_TYPE_UNDOCKED_TEMPORARY:
                msgTypeUndockedTemporary(device, state, startId);
                break;

            case MSG_TYPE_DISABLE_BT:
                deferFinishCall = msgTypeDisableBluetooth(startId);
                break;
        }

        if (mDialog == null && mPendingDevice == null && msgType != MSG_TYPE_UNDOCKED_TEMPORARY
                && !deferFinishCall) {
            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by the Receiver is released.
            DockEventReceiver.finishStartingService(this, startId);
        }
    }

    private boolean msgTypeDisableBluetooth(int startId) {
        if (DEBUG) {
            Log.d(TAG, "BT DISABLE");
        }
        final SharedPreferences prefs = getPrefs();
        if (mLocalAdapter.disable()) {
            prefs.edit().remove(KEY_DISABLE_BT_WHEN_UNDOCKED).apply();
            return false;
        } else {
            // disable() returned an error. Persist a flag to disable BT later
            prefs.edit().putBoolean(KEY_DISABLE_BT, true).apply();
            mPendingTurnOffStartId = startId;
            if(DEBUG) {
                Log.d(TAG, "disable failed. try again later " + startId);
            }
            return true;
        }
    }

    private void msgTypeUndockedTemporary(BluetoothDevice device, int state,
            int startId) {
        // Undocked event received. Queue a delayed msg to sever connection
        Message newMsg = mServiceHandler.obtainMessage(MSG_TYPE_UNDOCKED_PERMANENT, state,
                startId, device);
        mServiceHandler.sendMessageDelayed(newMsg, UNDOCKED_GRACE_PERIOD);
    }

    private boolean msgTypeUndockedPermanent(BluetoothDevice device, int startId) {
        // Grace period passed. Disconnect.
        handleUndocked(device);
        if (device != null) {
            final SharedPreferences prefs = getPrefs();

            if (DEBUG) {
                Log.d(TAG, "DISABLE_BT_WHEN_UNDOCKED = "
                        + prefs.getBoolean(KEY_DISABLE_BT_WHEN_UNDOCKED, false));
            }

            if (prefs.getBoolean(KEY_DISABLE_BT_WHEN_UNDOCKED, false)) {
                if (hasOtherConnectedDevices(device)) {
                    // Don't disable BT if something is connected
                    prefs.edit().remove(KEY_DISABLE_BT_WHEN_UNDOCKED).apply();
                } else {
                    // BT was disabled when we first docked
                    if (DEBUG) {
                        Log.d(TAG, "QUEUED BT DISABLE");
                    }
                    // Queue a delayed msg to disable BT
                    Message newMsg = mServiceHandler.obtainMessage(
                            MSG_TYPE_DISABLE_BT, 0, startId, null);
                    mServiceHandler.sendMessageDelayed(newMsg,
                            DISABLE_BT_GRACE_PERIOD);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean msgTypeDocked(BluetoothDevice device, final int state,
            final int startId) {
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
        getPrefs().edit().remove(KEY_DISABLE_BT).apply();

        if (device != null) {
            if (!device.equals(mDevice)) {
                if (mDevice != null) {
                    // Not expected. Cleanup/undock existing
                    handleUndocked(mDevice);
                }

                mDevice = device;

                // Register first in case LocalBluetoothProfileManager
                // becomes ready after isManagerReady is called and it
                // would be too late to register a service listener.
                mProfileManager.addServiceListener(this);
                if (mProfileManager.isManagerReady()) {
                    handleDocked(device, state, startId);
                    // Not needed after all
                    mProfileManager.removeServiceListener(this);
                } else {
                    final BluetoothDevice d = device;
                    mRunnable = new Runnable() {
                        public void run() {
                            handleDocked(d, state, startId);  // FIXME: WTF runnable here?
                        }
                    };
                    return true;
                }
            }
        } else {
            // display dialog to enable dock for media audio only in the case of low end docks and
            // if not already selected by user
            int dockAudioMediaEnabled = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, -1);
            if (dockAudioMediaEnabled == -1 &&
                    state == Intent.EXTRA_DOCK_STATE_LE_DESK) {
                handleDocked(null, state, startId);
                return true;
            }
        }
        return false;
    }

    synchronized boolean hasOtherConnectedDevices(BluetoothDevice dock) {
        Collection<CachedBluetoothDevice> cachedDevices = mDeviceManager.getCachedDevicesCopy();
        Set<BluetoothDevice> btDevices = mLocalAdapter.getBondedDevices();
        if (btDevices == null || cachedDevices == null || btDevices.isEmpty()) {
            return false;
        }
        if(DEBUG) {
            Log.d(TAG, "btDevices = " + btDevices.size());
            Log.d(TAG, "cachedDeviceUIs = " + cachedDevices.size());
        }

        for (CachedBluetoothDevice deviceUI : cachedDevices) {
            BluetoothDevice btDevice = deviceUI.getDevice();
            if (!btDevice.equals(dock) && btDevices.contains(btDevice) && deviceUI
                    .isConnected()) {
                if(DEBUG) Log.d(TAG, "connected deviceUI = " + deviceUI.getName());
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
                    + " Device: " + (device == null ? "null" : device.getAliasName()));
        }

        int msgType;
        switch (state) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                msgType = MSG_TYPE_UNDOCKED_TEMPORARY;
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
            case Intent.EXTRA_DOCK_STATE_CAR:
                if (device == null) {
                    Log.w(TAG, "device is null");
                    return null;
                }
                /// Fall Through ///
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
                if (DockEventReceiver.ACTION_DOCK_SHOW_UI.equals(intent.getAction())) {
                    if (device == null) {
                        Log.w(TAG, "device is null");
                        return null;
                    }
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

    private void createDialog(BluetoothDevice device,
            int state, int startId) {
        if (mDialog != null) {
            // Shouldn't normally happen
            mDialog.dismiss();
            mDialog = null;
        }
        mDevice = device;
        switch (state) {
            case Intent.EXTRA_DOCK_STATE_CAR:
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                break;
            default:
                return;
        }

        startForeground(0, new Notification());

        final AlertDialog.Builder ab = new AlertDialog.Builder(this);
        View view;
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

        mAudioMediaCheckbox = null;

        if (device != null) {
            // Device in a new dock.
            boolean firstTime =
                    !LocalBluetoothPreferences.hasDockAutoConnectSetting(this, device.getAddress());

            CharSequence[] items = initBtSettings(device, state, firstTime);

            ab.setTitle(getString(R.string.bluetooth_dock_settings_title));

            // Profiles
            ab.setMultiChoiceItems(items, mCheckedItems, mMultiClickListener);

            // Remember this settings
            view = inflater.inflate(R.layout.remember_dock_setting, null);
            CheckBox rememberCheckbox = (CheckBox) view.findViewById(R.id.remember);

            // check "Remember setting" by default if no value was saved
            boolean checked = firstTime ||
                    LocalBluetoothPreferences.getDockAutoConnectSetting(this, device.getAddress());
            rememberCheckbox.setChecked(checked);
            rememberCheckbox.setOnCheckedChangeListener(mCheckedChangeListener);
            if (DEBUG) {
                Log.d(TAG, "Auto connect = "
                  + LocalBluetoothPreferences.getDockAutoConnectSetting(this, device.getAddress()));
            }
        } else {
            ab.setTitle(getString(R.string.bluetooth_dock_settings_title));

            view = inflater.inflate(R.layout.dock_audio_media_enable_dialog, null);
            mAudioMediaCheckbox =
                    (CheckBox) view.findViewById(R.id.dock_audio_media_enable_cb);

            boolean checked = Settings.Global.getInt(getContentResolver(),
                                    Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, 0) == 1;

            mAudioMediaCheckbox.setChecked(checked);
            mAudioMediaCheckbox.setOnCheckedChangeListener(mCheckedChangeListener);
        }

        float pixelScaleFactor = getResources().getDisplayMetrics().density;
        int viewSpacingLeft = (int) (14 * pixelScaleFactor);
        int viewSpacingRight = (int) (14 * pixelScaleFactor);
        ab.setView(view, viewSpacingLeft, 0 /* top */, viewSpacingRight, 0 /* bottom */);

        // Ok Button
        ab.setPositiveButton(getString(android.R.string.ok), mClickListener);

        mStartIdAssociatedWithDialog = startId;
        mDialog = ab.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        mDialog.setOnDismissListener(mDismissListener);
        mDialog.show();
    }

    // Called when the individual bt profiles are clicked.
    private final DialogInterface.OnMultiChoiceClickListener mMultiClickListener =
            new DialogInterface.OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if (DEBUG) {
                        Log.d(TAG, "Item " + which + " changed to " + isChecked);
                    }
                    mCheckedItems[which] = isChecked;
                }
            };


    // Called when the "Remember" Checkbox is clicked
    private final CompoundButton.OnCheckedChangeListener mCheckedChangeListener =
            new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (DEBUG) {
                        Log.d(TAG, "onCheckedChanged: Remember Settings = " + isChecked);
                    }
                    if (mDevice != null) {
                        LocalBluetoothPreferences.saveDockAutoConnectSetting(
                                DockService.this, mDevice.getAddress(), isChecked);
                    } else {
                        Settings.Global.putInt(getContentResolver(),
                                Settings.Global.DOCK_AUDIO_MEDIA_ENABLED, isChecked ? 1 : 0);
                    }
                }
            };


    // Called when the dialog is dismissed
    private final DialogInterface.OnDismissListener mDismissListener =
            new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    // NOTE: We MUST not call stopSelf() directly, since we need to
                    // make sure the wake lock acquired by the Receiver is released.
                    if (mPendingDevice == null) {
                        DockEventReceiver.finishStartingService(
                                DockService.this, mStartIdAssociatedWithDialog);
                    }
                    stopForeground(true);
                }
            };

    // Called when clicked on the OK button
    private final DialogInterface.OnClickListener mClickListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        if (mDevice != null) {
                            if (!LocalBluetoothPreferences
                                    .hasDockAutoConnectSetting(
                                            DockService.this,
                                            mDevice.getAddress())) {
                                LocalBluetoothPreferences
                                        .saveDockAutoConnectSetting(
                                                DockService.this,
                                                mDevice.getAddress(), true);
                            }

                            applyBtSettings(mDevice, mStartIdAssociatedWithDialog);
                        } else if (mAudioMediaCheckbox != null) {
                            Settings.Global.putInt(getContentResolver(),
                                    Settings.Global.DOCK_AUDIO_MEDIA_ENABLED,
                                    mAudioMediaCheckbox.isChecked() ? 1 : 0);
                        }
                    }
                }
            };

    private CharSequence[] initBtSettings(BluetoothDevice device,
            int state, boolean firstTime) {
        // TODO Avoid hardcoding dock and profiles. Read from system properties
        int numOfProfiles;
        switch (state) {
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                numOfProfiles = 1;
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                numOfProfiles = 2;
                break;
            default:
                return null;
        }

        mProfiles = new LocalBluetoothProfile[numOfProfiles];
        mCheckedItems = new boolean[numOfProfiles];
        CharSequence[] items = new CharSequence[numOfProfiles];

        // FIXME: convert switch to something else
        switch (state) {
            case Intent.EXTRA_DOCK_STATE_CAR:
                items[0] = getString(R.string.bluetooth_dock_settings_headset);
                items[1] = getString(R.string.bluetooth_dock_settings_a2dp);
                mProfiles[0] = mProfileManager.getHeadsetProfile();
                mProfiles[1] = mProfileManager.getA2dpProfile();
                if (firstTime) {
                    // Enable by default for car dock
                    mCheckedItems[0] = true;
                    mCheckedItems[1] = true;
                } else {
                    mCheckedItems[0] = mProfiles[0].isPreferred(device);
                    mCheckedItems[1] = mProfiles[1].isPreferred(device);
                }
                break;

            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                items[0] = getString(R.string.bluetooth_dock_settings_a2dp);
                mProfiles[0] = mProfileManager.getA2dpProfile();
                if (firstTime) {
                    // Disable by default for desk dock
                    mCheckedItems[0] = false;
                } else {
                    mCheckedItems[0] = mProfiles[0].isPreferred(device);
                }
                break;
        }
        return items;
    }

    // TODO: move to background thread to fix strict mode warnings
    private void handleBtStateChange(Intent intent, int startId) {
        int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        synchronized (this) {
            if(DEBUG) Log.d(TAG, "BtState = " + btState + " mPendingDevice = " + mPendingDevice);
            if (btState == BluetoothAdapter.STATE_ON) {
                handleBluetoothStateOn(startId);
            } else if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
                // Remove the flag to disable BT if someone is turning off bt.
                // The rational is that:
                // a) if BT is off at undock time, no work needs to be done
                // b) if BT is on at undock time, the user wants it on.
                getPrefs().edit().remove(KEY_DISABLE_BT_WHEN_UNDOCKED).apply();
                DockEventReceiver.finishStartingService(this, startId);
            } else if (btState == BluetoothAdapter.STATE_OFF) {
                // Bluetooth was turning off as we were trying to turn it on.
                // Let's try again
                if(DEBUG) Log.d(TAG, "Bluetooth = OFF mPendingDevice = " + mPendingDevice);

                if (mPendingTurnOffStartId != INVALID_STARTID) {
                    DockEventReceiver.finishStartingService(this, mPendingTurnOffStartId);
                    getPrefs().edit().remove(KEY_DISABLE_BT).apply();
                    mPendingTurnOffStartId = INVALID_STARTID;
                }

                if (mPendingDevice != null) {
                    mLocalAdapter.enable();
                    mPendingTurnOnStartId = startId;
                } else {
                    DockEventReceiver.finishStartingService(this, startId);
                }
            }
        }
    }

    private void handleBluetoothStateOn(int startId) {
        if (mPendingDevice != null) {
            if (mPendingDevice.equals(mDevice)) {
                if(DEBUG) {
                    Log.d(TAG, "applying settings");
                }
                applyBtSettings(mPendingDevice, mPendingStartId);
            } else if(DEBUG) {
                Log.d(TAG, "mPendingDevice  (" + mPendingDevice + ") != mDevice ("
                        + mDevice + ')');
            }

            mPendingDevice = null;
            DockEventReceiver.finishStartingService(this, mPendingStartId);
        } else {
            final SharedPreferences prefs = getPrefs();
            if (DEBUG) {
                Log.d(TAG, "A DISABLE_BT_WHEN_UNDOCKED = "
                        + prefs.getBoolean(KEY_DISABLE_BT_WHEN_UNDOCKED, false));
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
                } else if (prefs.getBoolean(KEY_DISABLE_BT, false)
                        && mLocalAdapter.disable()) {
                    mPendingTurnOffStartId = startId;
                    prefs.edit().remove(KEY_DISABLE_BT).apply();
                    return;
                }
            }
        }

        if (mPendingTurnOnStartId != INVALID_STARTID) {
            DockEventReceiver.finishStartingService(this, mPendingTurnOnStartId);
            mPendingTurnOnStartId = INVALID_STARTID;
        }

        DockEventReceiver.finishStartingService(this, startId);
    }

    private synchronized void handleUnexpectedDisconnect(BluetoothDevice disconnectedDevice,
            LocalBluetoothProfile profile, int startId) {
        if (DEBUG) {
            Log.d(TAG, "handling failed connect for " + disconnectedDevice);
        }

            // Reconnect if docked.
            if (disconnectedDevice != null) {
                // registerReceiver can't be called from a BroadcastReceiver
                Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_DOCK_EVENT));
                if (intent != null) {
                    int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                            Intent.EXTRA_DOCK_STATE_UNDOCKED);
                    if (state != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                        BluetoothDevice dockedDevice = intent
                                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (dockedDevice != null && dockedDevice.equals(disconnectedDevice)) {
                            CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(
                                    dockedDevice);
                            cachedDevice.connectProfile(profile);
                        }
                    }
                }
            }

            DockEventReceiver.finishStartingService(this, startId);
    }

    private synchronized void connectIfEnabled(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(
                device);
        List<LocalBluetoothProfile> profiles = cachedDevice.getConnectableProfiles();
        for (LocalBluetoothProfile profile : profiles) {
            if (profile.getPreferred(device) == BluetoothProfile.PRIORITY_AUTO_CONNECT) {
                cachedDevice.connect(false);
                return;
            }
        }
    }

    private synchronized void applyBtSettings(BluetoothDevice device, int startId) {
        if (device == null || mProfiles == null || mCheckedItems == null
                || mLocalAdapter == null) {
            return;
        }

        // Turn on BT if something is enabled
        for (boolean enable : mCheckedItems) {
            if (enable) {
                int btState = mLocalAdapter.getBluetoothState();
                if (DEBUG) {
                    Log.d(TAG, "BtState = " + btState);
                }
                // May have race condition as the phone comes in and out and in the dock.
                // Always turn on BT
                mLocalAdapter.enable();

                // if adapter was previously OFF, TURNING_OFF, or TURNING_ON
                if (btState != BluetoothAdapter.STATE_ON) {
                    if (mPendingDevice != null && mPendingDevice.equals(mDevice)) {
                        return;
                    }

                    mPendingDevice = device;
                    mPendingStartId = startId;
                    if (btState != BluetoothAdapter.STATE_TURNING_ON) {
                        getPrefs().edit().putBoolean(
                                KEY_DISABLE_BT_WHEN_UNDOCKED, true).apply();
                    }
                    return;
                }
            }
        }

        mPendingDevice = null;

        boolean callConnect = false;
        CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(
                device);
        for (int i = 0; i < mProfiles.length; i++) {
            LocalBluetoothProfile profile = mProfiles[i];
            if (DEBUG) Log.d(TAG, profile.toString() + " = " + mCheckedItems[i]);

            if (mCheckedItems[i]) {
                // Checked but not connected
                callConnect = true;
            } else if (!mCheckedItems[i]) {
                // Unchecked, may or may not be connected.
                int status = profile.getConnectionStatus(cachedDevice.getDevice());
                if (status == BluetoothProfile.STATE_CONNECTED) {
                    if (DEBUG) Log.d(TAG, "applyBtSettings - Disconnecting");
                    cachedDevice.disconnect(mProfiles[i]);
                }
            }
            profile.setPreferred(device, mCheckedItems[i]);
            if (DEBUG) {
                if (mCheckedItems[i] != profile.isPreferred(device)) {
                    Log.e(TAG, "Can't save preferred value");
                }
            }
        }

        if (callConnect) {
            if (DEBUG) Log.d(TAG, "applyBtSettings - Connecting");
            cachedDevice.connect(false);
        }
    }

    private synchronized void handleDocked(BluetoothDevice device, int state,
            int startId) {
        if (device != null &&
                LocalBluetoothPreferences.getDockAutoConnectSetting(this, device.getAddress())) {
            // Setting == auto connect
            initBtSettings(device, state, false);
            applyBtSettings(mDevice, startId);
        } else {
            createDialog(device, state, startId);
        }
    }

    private synchronized void handleUndocked(BluetoothDevice device) {
        mRunnable = null;
        mProfileManager.removeServiceListener(this);
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mDevice = null;
        mPendingDevice = null;
        if (device != null) {
            CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(device);
            cachedDevice.disconnect();
        }
    }

    private CachedBluetoothDevice getCachedBluetoothDevice(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
        if (cachedDevice == null) {
            cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);
        }
        return cachedDevice;
    }

    public synchronized void onServiceConnected() {
        if (mRunnable != null) {
            mRunnable.run();
            mRunnable = null;
            mProfileManager.removeServiceListener(this);
        }
    }

    public void onServiceDisconnected() {
        // FIXME: shouldn't I do something on service disconnected too?
    }
}
