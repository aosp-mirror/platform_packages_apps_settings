/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.net.VpnProfile;
import com.android.settings.R;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog to configure always-on VPN.
 */
public class LockdownConfigFragment extends DialogFragment {
    private List<VpnProfile> mProfiles;
    private List<CharSequence> mTitles;
    private int mCurrentIndex;

    private static final String TAG_LOCKDOWN = "lockdown";

    private static class TitleAdapter extends ArrayAdapter<CharSequence> {
        public TitleAdapter(Context context, List<CharSequence> objects) {
            super(context, com.android.internal.R.layout.select_dialog_singlechoice_material,
                    android.R.id.text1, objects);
        }
    }

    public static void show(VpnSettings parent) {
        if (!parent.isAdded()) return;

        final LockdownConfigFragment dialog = new LockdownConfigFragment();
        dialog.show(parent.getFragmentManager(), TAG_LOCKDOWN);
    }

    private static String getStringOrNull(KeyStore keyStore, String key) {
        if (!keyStore.isUnlocked()) {
            return null;
        }
        final byte[] value = keyStore.get(key);
        return value == null ? null : new String(value);
    }

    private void initProfiles(KeyStore keyStore, Resources res) {
        final String lockdownKey = getStringOrNull(keyStore, Credentials.LOCKDOWN_VPN);

        mProfiles = VpnSettings.loadVpnProfiles(keyStore, VpnProfile.TYPE_PPTP);
        mTitles = new ArrayList<>(1 + mProfiles.size());
        mTitles.add(res.getText(R.string.vpn_lockdown_none));

        mCurrentIndex = 0;
        for (VpnProfile profile : mProfiles) {
            if (TextUtils.equals(profile.key, lockdownKey)) {
                mCurrentIndex = mTitles.size();
            }
            mTitles.add(profile.name);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final KeyStore keyStore = KeyStore.getInstance();

        initProfiles(keyStore, context.getResources());

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

        builder.setTitle(R.string.vpn_menu_lockdown);

        final View view = dialogInflater.inflate(R.layout.vpn_lockdown_editor, null, false);
        final ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(new TitleAdapter(context, mTitles));
        listView.setItemChecked(mCurrentIndex, true);
        builder.setView(view);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int newIndex = listView.getCheckedItemPosition();
                if (mCurrentIndex == newIndex) return;

                if (newIndex == 0) {
                    keyStore.delete(Credentials.LOCKDOWN_VPN);
                } else {
                    final VpnProfile profile = mProfiles.get(newIndex - 1);
                    if (!profile.isValidLockdownProfile()) {
                        Toast.makeText(context, R.string.vpn_lockdown_config_error,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    keyStore.put(Credentials.LOCKDOWN_VPN, profile.key.getBytes(),
                            KeyStore.UID_SELF, KeyStore.FLAG_ENCRYPTED);
                }

                // kick profiles since we changed them
                ConnectivityManager.from(getActivity()).updateLockdownVpn();
            }
        });

        return builder.create();
    }
}

