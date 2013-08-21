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
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.android.settings.R;

/**
 * Activity to support attaching a unknown NFC tag to an existing profile.
 */
public class NFCProfileSelect extends Activity {

    private static final String TAG = "NFCProfileSelect";

    static final String EXTRA_PROFILE_UUID = "PROFILE_UUID";

    private ProfileManager mProfileManager;

    private UUID mProfileUuid;

    final static int defaultChoice = -1;

    private int currentChoice = defaultChoice;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProfileManager = (ProfileManager) getSystemService(Context.PROFILE_SERVICE);

        setContentView(R.layout.nfc_select);
        setTitle(R.string.profile_unknown_nfc_tag);

        findViewById(R.id.add_tag).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showProfileSelectionDialog();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        String profileUuid = getIntent().getStringExtra(EXTRA_PROFILE_UUID);
        if (profileUuid != null) {
            mProfileUuid = UUID.fromString(profileUuid);
        } else {
            finish();
        }
    }

    void showProfileSelectionDialog() {
        final Profile[] profiles = mProfileManager.getProfiles();
        final String[] profileNames = new String[profiles.length];
        for (int i = 0; i < profiles.length; i++) {
            profileNames[i] = profiles[i].getName();
        }

        Builder builder = new Builder(this);
        builder.setTitle(R.string.profile_settings_title);
        builder.setSingleChoiceItems(profileNames, currentChoice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentChoice = which;
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (currentChoice != defaultChoice) {
                    Profile profile = profiles[currentChoice];
                    profile.addSecondaryUuid(mProfileUuid);
                    mProfileManager.updateProfile(profile);
                    Toast.makeText(NFCProfileSelect.this, R.string.profile_write_success, Toast.LENGTH_LONG).show();
                }
                finish();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }
}
