/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcUnlock;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

public class PairNfcDevice extends Activity {
    private static String TAG = PairNfcDevice.class.getName();

    private TextView mStatusText;
    private ImageView mStatusImage;

    private PendingIntent mPendingIntent;
    private NfcAdapter mAdapter;

    private Handler mHandler = new Handler();
    private PowerManager.WakeLock mWakeLock;

    private NfcUnlock mNfcUnlock;

    // If pairing fails, we immediately get a new intent that would not leave time for the user to
    // read the error message.  So we'll just drop it and the user has to try again.
    // TEST
    private boolean mWaitingForDeviceDelayed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_pairing);

        mStatusText = (TextView) findViewById(R.id.status_text);
        mStatusImage = (ImageView) findViewById(R.id.status_image);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        mNfcUnlock = NfcUnlock.getInstance(mAdapter);

        setWaitingForDeviceMode();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    public void onPause() {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    public void onResume() {
        super.onResume();

        if (!mAdapter.isEnabled()) {
            // We need the user to start NFC.
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage(R.string.enable_nfc);
            dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                }
            });
            dialogBuilder.show();
        }

        mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);

        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        }
        mWakeLock.acquire();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (!mWaitingForDeviceDelayed) {
            processTag(tag);
        }
    }

    private void processTag(Tag tag) {
        if (mNfcUnlock.registerTag(tag)) {
            setPairingSucceededMode();
        } else {
            setPairingFailedMode();
        }
    }

    private void setWaitingForDeviceModeDelayed(int delayInMs) {
        mWaitingForDeviceDelayed = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWaitingForDeviceDelayed = false;
                setWaitingForDeviceMode();
            }
        }, delayInMs);
    }

    private void setWaitingForDeviceMode() {
        mStatusImage.setImageResource(R.drawable.no_ring_detected);
        mStatusText.setText(R.string.status_no_ring_detected);
    }

    private void setPairingFailedMode() {
        setErrorMode(R.string.status_error_pairing_failed);
    }

    private void setPairingSucceededMode() {
        mStatusImage.setImageResource(R.drawable.ring_paired);
        mStatusText.setText(R.string.status_device_paired);

        // Automatically quit.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 4000);
    }

    private void setErrorMode(int errorMsgResourceId) {
        mStatusText.setText(errorMsgResourceId);
        setWaitingForDeviceModeDelayed(2500);
    }
}
