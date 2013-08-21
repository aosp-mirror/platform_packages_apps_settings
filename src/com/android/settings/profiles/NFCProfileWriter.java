/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import java.util.UUID;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Profile;
import android.app.ProfileManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;

/**
 * Activity to support writing a profile to an NFC tag.
 * The mime type is "cm/profile" and the payload is the raw bytes of the profile's
 * UUID. The payload was intentionally kept small to support writing on 46-byte tags.
 */
public class NFCProfileWriter extends Activity {

    private static final String TAG = "NFCProfileWriter";

    static final String EXTRA_PROFILE_UUID = "PROFILE_UUID";

    private NfcAdapter mNfcAdapter;

    private IntentFilter[] mWriteTagFilters;

    private Profile mProfile;

    private ProfileManager mProfileManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mProfileManager = (ProfileManager) getSystemService(Context.PROFILE_SERVICE);

        setContentView(R.layout.nfc_writer);
        setTitle(R.string.profile_write_nfc_tag);
    }

    @Override
    public void onResume() {
        super.onResume();
        String profileUuid = getIntent().getStringExtra(EXTRA_PROFILE_UUID);
        if (profileUuid != null) {
            mProfile = mProfileManager.getProfile(UUID.fromString(profileUuid));
            Log.d(TAG, "Profile to write: " + mProfile.getName());
            enableTagWriteMode();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        disableTagWriteMode();
    }

    private PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    private void disableTagWriteMode() {
        mNfcAdapter.disableForegroundDispatch(this);
    }

    private void enableTagWriteMode() {
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[] {
            tagDetected
        };
        mNfcAdapter.enableForegroundDispatch(this, getPendingIntent(), mWriteTagFilters, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Tag writing mode
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (NFCProfileUtils.writeTag(NFCProfileUtils.getProfileAsNdef(mProfile), detectedTag)) {
                Toast.makeText(this, R.string.profile_write_success, Toast.LENGTH_LONG).show();
                NFCProfileUtils.vibrate(this);
            } else {
                Toast.makeText(this, R.string.profile_write_failed, Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }
}
