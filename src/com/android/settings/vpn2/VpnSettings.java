/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.settings.R;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.settings.SettingsPreferenceFragment;

import java.util.HashMap;

public class VpnSettings extends SettingsPreferenceFragment implements
        Handler.Callback, Preference.OnPreferenceClickListener,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final String TAG = "VpnSettings";

    // Match these constants with R.array.vpn_states.
    private static final int STATE_NONE = -1;
    private static final int STATE_CONNECTING = 0;
    private static final int STATE_CONNECTED = 1;
    private static final int STATE_DISCONNECTED = 2;
    private static final int STATE_FAILED = 3;

    private final KeyStore mKeyStore = KeyStore.getInstance();
    private boolean mUnlocking = false;

    private HashMap<String, VpnPreference> mPreferences;
    private VpnDialog mDialog;
    private String mSelectedKey;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        addPreferencesFromResource(R.xml.vpn_settings2);
        PreferenceGroup group = getPreferenceScreen();
        group.setOrderingAsAdded(false);
        group.findPreference("add_network").setOnPreferenceClickListener(this);

        if (savedState != null) {
            VpnProfile profile = VpnProfile.decode(savedState.getString("VpnKey"),
                    savedState.getByteArray("VpnProfile"));
            if (profile != null) {
                mDialog = new VpnDialog(getActivity(), this, profile,
                        savedState.getBoolean("VpnEditing"));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        // We do not save view hierarchy, as they are just profiles.
        if (mDialog != null) {
            VpnProfile profile = mDialog.getProfile();
            savedState.putString("VpnKey", profile.key);
            savedState.putByteArray("VpnProfile", profile.encode());
            savedState.putBoolean("VpnEditing", mDialog.isEditing());
        }
        // else?
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check KeyStore here, so others do not need to deal with it.
        if (mKeyStore.state() != KeyStore.State.UNLOCKED) {
            if (!mUnlocking) {
                // Let us unlock KeyStore. See you later!
                Credentials.getInstance().unlock(getActivity());
            } else {
                // We already tried, but it is still not working!
                getActivity().getFragmentManager().popBackStack();
            }
            mUnlocking = !mUnlocking;
            return;
        }

        // Now KeyStore is always unlocked. Reset the flag.
        mUnlocking = false;

        // Currently we are the only user of profiles in KeyStore.
        // Assuming KeyStore and KeyGuard do the right thing, we can
        // safely cache profiles in the memory.
        if (mPreferences == null) {
            mPreferences = new HashMap<String, VpnPreference>();

            String[] keys = mKeyStore.saw(Credentials.VPN);
            if (keys != null && keys.length > 0) {
                Context context = getActivity();

                for (String key : keys) {
                    VpnProfile profile = VpnProfile.decode(key,
                            mKeyStore.get(Credentials.VPN + key));
                    if (profile == null) {
                        Log.w(TAG, "bad profile: key = " + key);
                        mKeyStore.delete(Credentials.VPN + key);
                    } else {
                        VpnPreference preference = new VpnPreference(context, profile);
                        mPreferences.put(key, preference);
                    }
                }
            }
        }
        PreferenceGroup group = getPreferenceScreen();
        for (VpnPreference preference : mPreferences.values()) {
            group.addPreference(preference);
        }

        // Show the dialog if there is one.
        if (mDialog != null) {
            mDialog.setOnDismissListener(this);
            mDialog.show();
        }

        // Start monitoring.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }
        mHandler.sendEmptyMessage(0);

        // Register for context menu. Hmmm, getListView() is hidden?
        registerForContextMenu(getListView());
    }

    @Override
    public void onPause() {
        super.onPause();

        // Hide the dialog if there is one.
        if (mDialog != null) {
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
        }

        // Unregister for context menu.
        unregisterForContextMenu(getListView());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Here is the exit of a dialog.
        mDialog = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            // Always save the profile.
            VpnProfile profile = mDialog.getProfile();
            mKeyStore.put(Credentials.VPN + profile.key, profile.encode());

            // Update the preference.
            VpnPreference preference = mPreferences.get(profile.key);
            if (preference != null) {
                disconnect(profile.key);
                preference.update(profile);
            } else {
                preference = new VpnPreference(getActivity(), profile);
                mPreferences.put(profile.key, preference);
                getPreferenceScreen().addPreference(preference);
            }

            // If we are not editing, connect!
            if (!mDialog.isEditing()) {
                connect(profile.key);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
        if (mDialog != null) {
            Log.v(TAG, "onCreateContextMenu() is called when mDialog != null");
            return;
        }

        if (info instanceof AdapterContextMenuInfo) {
            Preference preference = (Preference) getListView().getItemAtPosition(
                    ((AdapterContextMenuInfo) info).position);
            if (preference instanceof VpnPreference) {
                VpnProfile profile = ((VpnPreference) preference).getProfile();
                mSelectedKey = profile.key;
                menu.setHeaderTitle(profile.name);
                menu.add(Menu.NONE, R.string.vpn_menu_edit, 0, R.string.vpn_menu_edit);
                menu.add(Menu.NONE, R.string.vpn_menu_delete, 0, R.string.vpn_menu_delete);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mDialog != null) {
            Log.v(TAG, "onContextItemSelected() is called when mDialog != null");
            return false;
        }

        VpnPreference preference = mPreferences.get(mSelectedKey);
        if (preference == null) {
            Log.v(TAG, "onContextItemSelected() is called but no preference is found");
            return false;
        }

        switch (item.getItemId()) {
            case R.string.vpn_menu_edit:
                mDialog = new VpnDialog(getActivity(), this, preference.getProfile(), true);
                mDialog.setOnDismissListener(this);
                mDialog.show();
                return true;
            case R.string.vpn_menu_delete:
                disconnect(mSelectedKey);
                getPreferenceScreen().removePreference(preference);
                mPreferences.remove(mSelectedKey);
                mKeyStore.delete(Credentials.VPN + mSelectedKey);
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mDialog != null) {
            Log.v(TAG, "onPreferenceClick() is called when mDialog != null");
            return true;
        }

        if (preference instanceof VpnPreference) {
            mDialog = new VpnDialog(getActivity(), this,
                    ((VpnPreference) preference).getProfile(), false);
        } else {
            // Generate a new key. Here we just use the current time.
            long millis = System.currentTimeMillis();
            while (mPreferences.containsKey(Long.toHexString(millis))) {
                ++millis;
            }
            mDialog = new VpnDialog(getActivity(), this,
                    new VpnProfile(Long.toHexString(millis)), true);
        }
        mDialog.setOnDismissListener(this);
        mDialog.show();
        return true;
    }

    @Override
    public boolean handleMessage(Message message) {
        mHandler.removeMessages(0);

        if (isResumed()) {




            mHandler.sendEmptyMessageDelayed(0, 1000);
        }
        return true;
    }

    private void connect(String key) {
    }

    private void disconnect(String key) {
    }


    private class VpnPreference extends Preference {
        private VpnProfile mProfile;
        private int mState = STATE_NONE;

        VpnPreference(Context context, VpnProfile profile) {
            super(context);
            setPersistent(false);
            setOnPreferenceClickListener(VpnSettings.this);

            mProfile = profile;
            update();
        }

        VpnProfile getProfile() {
            return mProfile;
        }

        void update(VpnProfile profile) {
            mProfile = profile;
            update();
        }

        void update() {
            if (mState != STATE_NONE) {
                String[] states = getContext().getResources()
                        .getStringArray(R.array.vpn_states);
                setSummary(states[mState]);
            } else {
                String[] types = getContext().getResources()
                        .getStringArray(R.array.vpn_types_long);
                setSummary(types[mProfile.type]);
            }
            setTitle(mProfile.name);
            notifyChanged();
        }

        @Override
        public int compareTo(Preference preference) {
            int result = 1;
            if (preference instanceof VpnPreference) {
                VpnPreference another = (VpnPreference) preference;

                if ((result = another.mState - mState) == 0 &&
                        (result = mProfile.name.compareTo(another.mProfile.name)) == 0 &&
                        (result = mProfile.type - another.mProfile.type) == 0) {
                    result = mProfile.key.compareTo(another.mProfile.key);
                }
            }
            return result;
        }
    }
}
