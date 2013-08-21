/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import android.app.Profile;
import android.app.ProfileManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.widget.Toast;

import com.android.settings.R;

/**
 * This activity handles NDEF_DISCOVERED intents with the cm/profile mime type.
 * Tags should be encoded with the 16-byte UUID of the profile to be activated.
 * Tapping a tag while that profile is already active will select the previously
 * active profile.
 */
public class NFCProfile extends Activity {

    private static final String PREFS_NAME = "NFCProfile";

    private static final String PREFS_PREVIOUS_PROFILE = "previous-profile";

    static final String PROFILE_MIME_TYPE = "cm/profile";

    private ProfileManager mProfileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProfileManager = (ProfileManager) getSystemService(Context.PROFILE_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    for (NdefRecord record : msgs[i].getRecords()) {
                        String type = new String(record.getType());
                        byte[] payload = record.getPayload();
                        if (PROFILE_MIME_TYPE.equals(type) && payload != null
                                && payload.length == 16) {
                            handleProfileMimeType(payload);
                        }
                    }
                }
            }
        }
        finish();
    }

    private void handleProfileMimeType(byte[] payload) {
        UUID profileUuid = NFCProfileUtils.toUUID(payload);

        boolean enabled = Settings.System.getInt(getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;

        if (enabled) {
            // Only do NFC profile changing if System Profile support is enabled
            Profile currentProfile = mProfileManager.getActiveProfile();
            Profile targetProfile = mProfileManager.getProfile(profileUuid);

            if (targetProfile == null) {
                // show profile selection for unknown tag
                Intent i = new Intent(this, NFCProfileSelect.class);
                i.putExtra(NFCProfileSelect.EXTRA_PROFILE_UUID, profileUuid.toString());
                i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                this.startActivity(i);
            } else {
                // switch to profile
                if (currentProfile == null || !currentProfile.getUuid().equals(profileUuid)) {
                    saveCurrentProfile();
                    switchTo(profileUuid);
                } else {
                    Profile lastProfile = getPreviouslySelectedProfile();
                    if (lastProfile != null) {
                        switchTo(lastProfile.getUuid());
                        clearPreviouslySelectedProfile();
                    }
                }
            }
        }
    }

    private void switchTo(UUID uuid) {
        Profile p = mProfileManager.getProfile(uuid);
        if (p != null) {
            mProfileManager.setActiveProfile(uuid);

            Toast.makeText(
                    this,
                    String.format(getResources().getString(R.string.profile_selected), p.getName()),
                    Toast.LENGTH_LONG).show();
            NFCProfileUtils.vibrate(this);
        }
    }

    private Profile getPreviouslySelectedProfile() {
        Profile previous = null;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
        String uuid = prefs.getString(PREFS_PREVIOUS_PROFILE, null);
        if (uuid != null) {
            previous = mProfileManager.getProfile(UUID.fromString(uuid));
        }
        return previous;
    }

    private void clearPreviouslySelectedProfile() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
        editor.remove(PREFS_PREVIOUS_PROFILE);
        editor.commit();
    }

    private void saveCurrentProfile() {
        Profile currentProfile = mProfileManager.getActiveProfile();
        if (currentProfile != null) {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
            editor.putString(PREFS_PREVIOUS_PROFILE, currentProfile.getUuid().toString());
            editor.commit();
        }
    }
}
