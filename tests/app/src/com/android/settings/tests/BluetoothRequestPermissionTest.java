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

package com.android.settings.tests;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class BluetoothRequestPermissionTest extends Activity {
    private static final String TAG = "BluetoothRequestPermissionTest";
    BluetoothAdapter mAdapter;
    private ArrayAdapter<String> mMsgAdapter;

    // Discoverable button alternates between 20 second timeout and no timeout.
    private boolean mDiscoveryWithTimeout = true;

    private class BtOnClickListener implements OnClickListener {
        final boolean mEnableOnly; // enable or enable + discoverable

        public BtOnClickListener(boolean enableOnly) {
            mEnableOnly = enableOnly;
        }

        public void onClick(View v) {
            requestPermission(mEnableOnly);
        }
    }

    private class BtScanOnClickListener implements OnClickListener {
        public void onClick(View v) {
            Button scanButton = (Button) v;
            if (mAdapter.isDiscovering()) {
                mAdapter.cancelDiscovery();
                scanButton.setText(R.string.start_scan);
            } else {
                mAdapter.startDiscovery();
                scanButton.setText(R.string.stop_scan);
            }
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.bluetooth_request_permission_test);
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        Button enable = (Button) findViewById(R.id.enable);
        enable.setOnClickListener(new BtOnClickListener(true /* enable */));

        Button discoverable = (Button) findViewById(R.id.discoverable);
        discoverable.setOnClickListener(new BtOnClickListener(false /* enable & discoverable */));

        Button scanButton = (Button) findViewById(R.id.scan);
        scanButton.setOnClickListener(new BtScanOnClickListener());
        if (mAdapter.isDiscovering()) {
            scanButton.setText(R.string.stop_scan);
        } else {
            scanButton.setText(R.string.start_scan);
        }

        mMsgAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        ListView listView = (ListView) findViewById(R.id.msg_container);
        listView.setAdapter(mMsgAdapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        addMsg("Initialized");
    }

    void requestPermission(boolean enableOnly) {
        Intent i = new Intent();
        if (enableOnly) {
            addMsg("Starting activity to enable bt");
            i.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        } else {
            addMsg("Starting activity to enable bt + discovery");
            i.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            // Discoverability duration toggles between 20 seconds and no timeout.
            int timeout = (mDiscoveryWithTimeout ? 20 : 0);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, timeout);
            mDiscoveryWithTimeout = !mDiscoveryWithTimeout;
        }
        startActivityForResult(i, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 1) {
            Log.e(TAG, "Unexpected onActivityResult " + requestCode + " " + resultCode);
            return;
        }

        if (resultCode == Activity.RESULT_CANCELED) {
            addMsg("Result = RESULT_CANCELED");
        } else if (resultCode == Activity.RESULT_OK) {
            addMsg("Result = RESULT_OK (not expected for discovery)");
        } else {
            addMsg("Result = " + resultCode);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void addMsg(String msg) {
        mMsgAdapter.add(msg);
        Log.d(TAG, "msg");
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null)
                return;
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                String stateStr = "???";
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothDevice.ERROR)) {
                    case BluetoothAdapter.STATE_OFF:
                        stateStr = "off";
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        stateStr = "turning on";
                        break;
                    case BluetoothAdapter.STATE_ON:
                        stateStr = "on";
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        stateStr = "turning off";
                        break;
                }
                addMsg("Bluetooth status = " + stateStr);
            } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                addMsg("Found: " + name);
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                addMsg("Scan started...");
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                addMsg("Scan ended");
            }
        }
    };
}
