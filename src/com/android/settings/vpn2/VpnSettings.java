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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VpnSettings extends SettingsPreferenceFragment implements
        Handler.Callback, Preference.OnPreferenceClickListener,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private static final String TAG = "VpnSettings";

    private static final String TAG_LOCKDOWN = "lockdown";

    private static final String EXTRA_PICK_LOCKDOWN = "android.net.vpn.PICK_LOCKDOWN";

    // TODO: migrate to using DialogFragment when editing

    private final IConnectivityManager mService = IConnectivityManager.Stub
            .asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private boolean mUnlocking = false;

    private HashMap<String, VpnPreference> mPreferences = new HashMap<String, VpnPreference>();
    private VpnDialog mDialog;

    private Handler mUpdater;
    private LegacyVpnInfo mInfo;

    // The key of the profile for the current ContextMenu.
    private String mSelectedKey;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.vpn_settings2);

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.vpn, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Hide lockdown VPN on devices that require IMS authentication
        if (SystemProperties.getBoolean("persist.radio.imsregrequired", false)) {
            menu.findItem(R.id.vpn_lockdown).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.vpn_create: {
                // Generate a new key. Here we just use the current time.
                long millis = System.currentTimeMillis();
                while (mPreferences.containsKey(Long.toHexString(millis))) {
                    ++millis;
                }
                mDialog = new VpnDialog(
                        getActivity(), this, new VpnProfile(Long.toHexString(millis)), true);
                mDialog.setOnDismissListener(this);
                mDialog.show();
                return true;
            }
            case R.id.vpn_lockdown: {
                LockdownConfigFragment.show(this);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
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

        final boolean pickLockdown = getActivity()
                .getIntent().getBooleanExtra(EXTRA_PICK_LOCKDOWN, false);
        if (pickLockdown) {
            LockdownConfigFragment.show(this);
        }

        // Check KeyStore here, so others do not need to deal with it.
        if (!mKeyStore.isUnlocked()) {
            if (!mUnlocking) {
                // Let us unlock KeyStore. See you later!
                Credentials.getInstance().unlock(getActivity());
            } else {
                // We already tried, but it is still not working!
                finishFragment();
            }
            mUnlocking = !mUnlocking;
            return;
        }

        // Now KeyStore is always unlocked. Reset the flag.
        mUnlocking = false;

        // Currently we are the only user of profiles in KeyStore.
        // Assuming KeyStore and KeyGuard do the right thing, we can
        // safely cache profiles in the memory.
        if (mPreferences.size() == 0) {
            PreferenceGroup group = getPreferenceScreen();

            final Context context = getActivity();
            final List<VpnProfile> profiles = loadVpnProfiles(mKeyStore);
            for (VpnProfile profile : profiles) {
                final VpnPreference pref = new VpnPreference(context, profile);
                pref.setOnPreferenceClickListener(this);
                mPreferences.put(profile.key, pref);
                group.addPreference(pref);
            }
        }

        // Show the dialog if there is one.
        if (mDialog != null) {
            mDialog.setOnDismissListener(this);
            mDialog.show();
        }

        // Start monitoring.
        if (mUpdater == null) {
            mUpdater = new Handler(this);
        }
        mUpdater.sendEmptyMessage(0);

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
        if (getView() != null) {
            unregisterForContextMenu(getListView());
        }
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
            mKeyStore.put(Credentials.VPN + profile.key, profile.encode(), KeyStore.UID_SELF,
                    KeyStore.FLAG_ENCRYPTED);

            // Update the preference.
            VpnPreference preference = mPreferences.get(profile.key);
            if (preference != null) {
                disconnect(profile.key);
                preference.update(profile);
            } else {
                preference = new VpnPreference(getActivity(), profile);
                preference.setOnPreferenceClickListener(this);
                mPreferences.put(profile.key, preference);
                getPreferenceScreen().addPreference(preference);
            }

            // If we are not editing, connect!
            if (!mDialog.isEditing()) {
                try {
                    connect(profile);
                } catch (Exception e) {
                    Log.e(TAG, "connect", e);
                }
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
            VpnProfile profile = ((VpnPreference) preference).getProfile();
            if (mInfo != null && profile.key.equals(mInfo.key) &&
                    mInfo.state == LegacyVpnInfo.STATE_CONNECTED) {
                try {
                    mInfo.intent.send();
                    return true;
                } catch (Exception e) {
                    // ignore
                }
            }
            mDialog = new VpnDialog(getActivity(), this, profile, false);
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
        mUpdater.removeMessages(0);

        if (isResumed()) {
            try {
                LegacyVpnInfo info = mService.getLegacyVpnInfo();
                if (mInfo != null) {
                    VpnPreference preference = mPreferences.get(mInfo.key);
                    if (preference != null) {
                        preference.update(-1);
                    }
                    mInfo = null;
                }
                if (info != null) {
                    VpnPreference preference = mPreferences.get(info.key);
                    if (preference != null) {
                        preference.update(info.state);
                        mInfo = info;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            mUpdater.sendEmptyMessageDelayed(0, 1000);
        }
        return true;
    }

    private void connect(VpnProfile profile) throws Exception {
        try {
            mService.startLegacyVpn(profile);
        } catch (IllegalStateException e) {
            Toast.makeText(getActivity(), R.string.vpn_no_network, Toast.LENGTH_LONG).show();
        }
    }

    private void disconnect(String key) {
        if (mInfo != null && key.equals(mInfo.key)) {
            try {
                mService.prepareVpn(VpnConfig.LEGACY_VPN, VpnConfig.LEGACY_VPN);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_vpn;
    }

    private static class VpnPreference extends Preference {
        private VpnProfile mProfile;
        private int mState = -1;

        VpnPreference(Context context, VpnProfile profile) {
            super(context);
            setPersistent(false);
            setOrder(0);

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

        void update(int state) {
            mState = state;
            update();
        }

        void update() {
            if (mState < 0) {
                String[] types = getContext().getResources()
                        .getStringArray(R.array.vpn_types_long);
                setSummary(types[mProfile.type]);
            } else {
                String[] states = getContext().getResources()
                        .getStringArray(R.array.vpn_states);
                setSummary(states[mState]);
            }
            setTitle(mProfile.name);
            notifyHierarchyChanged();
        }

        @Override
        public int compareTo(Preference preference) {
            int result = -1;
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

    /**
     * Dialog to configure always-on VPN.
     */
    public static class LockdownConfigFragment extends DialogFragment {
        private List<VpnProfile> mProfiles;
        private List<CharSequence> mTitles;
        private int mCurrentIndex;

        private static class TitleAdapter extends ArrayAdapter<CharSequence> {
            public TitleAdapter(Context context, List<CharSequence> objects) {
                super(context, com.android.internal.R.layout.select_dialog_singlechoice_holo,
                        android.R.id.text1, objects);
            }
        }

        public static void show(VpnSettings parent) {
            if (!parent.isAdded()) return;

            final LockdownConfigFragment dialog = new LockdownConfigFragment();
            dialog.show(parent.getFragmentManager(), TAG_LOCKDOWN);
        }

        private static String getStringOrNull(KeyStore keyStore, String key) {
            final byte[] value = keyStore.get(Credentials.LOCKDOWN_VPN);
            return value == null ? null : new String(value);
        }

        private void initProfiles(KeyStore keyStore, Resources res) {
            final String lockdownKey = getStringOrNull(keyStore, Credentials.LOCKDOWN_VPN);

            mProfiles = loadVpnProfiles(keyStore, VpnProfile.TYPE_PPTP);
            mTitles = Lists.newArrayList();
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

    private static List<VpnProfile> loadVpnProfiles(KeyStore keyStore, int... excludeTypes) {
        final ArrayList<VpnProfile> result = Lists.newArrayList();
        final String[] keys = keyStore.saw(Credentials.VPN);
        if (keys != null) {
            for (String key : keys) {
                final VpnProfile profile = VpnProfile.decode(
                        key, keyStore.get(Credentials.VPN + key));
                if (profile != null && !ArrayUtils.contains(excludeTypes, profile.type)) {
                    result.add(profile);
                }
            }
        }
        return result;
    }
}
