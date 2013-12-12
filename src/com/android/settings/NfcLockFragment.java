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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.nfc.NfcAdapter;
import android.nfc.NfcUnlock;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import android.preference.PreferenceCategory;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;

import java.text.DateFormat;
import java.util.Date;

import static android.preference.Preference.OnPreferenceClickListener;

public class NfcLockFragment extends SettingsPreferenceFragment {

    private static final String NFC_PAIRING = "nfc_pairing";
    private static final String NFC_UNLOCK_ENABLED = "nfc_unlock_enabled";
    private static final String TAGS_CATEGORY = "nfc_unlock_tags_category";
    private static final String TAG_FORMAT = "Tag # %d";

    private NfcUnlock mNfcUnlock;
    private LockPatternUtils mLockPatternUtils;
    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcUnlock = NfcUnlock.getInstance(NfcAdapter.getDefaultAdapter(getActivity()));
        mLockPatternUtils = new LockPatternUtils(getActivity());
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        addPreferencesFromResource(R.xml.security_settings_nfc_unlock);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean prefsEnabled = (mLockPatternUtils.isLockPasswordEnabled() ||
                mLockPatternUtils.isLockPatternEnabled()) && mNfcAdapter.isEnabled();
        CheckBoxPreference unlockPref = (CheckBoxPreference) findPreference(NFC_UNLOCK_ENABLED);
        unlockPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mNfcUnlock.setNfcUnlockEnabled((Boolean) newValue);
                return true;
            }
        });
        Preference pairingPref = findPreference(NFC_PAIRING);
        unlockPref.setEnabled(prefsEnabled);
        pairingPref.setEnabled(prefsEnabled);

        long[] tagRegistryTimes = mNfcUnlock.getTagRegistryTimes();
        unlockPref.setChecked(mNfcUnlock.getNfcUnlockEnabled());

        final PreferenceCategory pairedTags = (PreferenceCategory) findPreference(TAGS_CATEGORY);
        pairedTags.setEnabled(prefsEnabled);

        loadTagList(tagRegistryTimes, pairedTags);
    }

    private void loadTagList(long[] tagRegistryTimes, final PreferenceCategory pairedTags) {
        pairedTags.removeAll();

        for (int i = 0; i < tagRegistryTimes.length; i++) {

            final Preference thisPreference = new Preference(getActivity());
            final long timestamp = tagRegistryTimes[i];

            thisPreference.setTitle(String.format(TAG_FORMAT, i));
            thisPreference.setSummary(
                    DateFormat.getDateTimeInstance().format(new Date(tagRegistryTimes[i])));
            thisPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(getActivity());

                    deleteDialogBuilder.setTitle(thisPreference.getTitle());
                    deleteDialogBuilder.setItems(new String[] {"Delete"},
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0) {
                                        if (mNfcUnlock.deregisterTag(timestamp)) {
                                            loadTagList(mNfcUnlock.getTagRegistryTimes(),
                                                    pairedTags);
                                        }
                                    }
                                }
                            });


                    deleteDialogBuilder.show();

                    return true;
                }
            });


            pairedTags.addPreference(thisPreference);
        }
    }
}
