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

package com.android.settings.vpn;

import com.android.settings.R;
import com.android.settings.SecuritySettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.vpn.L2tpIpsecPskProfile;
import android.net.vpn.L2tpProfile;
import android.net.vpn.VpnManager;
import android.net.vpn.VpnProfile;
import android.net.vpn.VpnState;
import android.net.vpn.VpnType;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.security.Keystore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The preference activity for configuring VPN settings.
 */
public class VpnSettings extends PreferenceActivity implements
        DialogInterface.OnClickListener {
    // Key to the field exchanged for profile editing.
    static final String KEY_VPN_PROFILE = "vpn_profile";

    // Key to the field exchanged for VPN type selection.
    static final String KEY_VPN_TYPE = "vpn_type";

    private static final String TAG = VpnSettings.class.getSimpleName();

    private static final String PREF_ADD_VPN = "add_new_vpn";
    private static final String PREF_VPN_LIST = "vpn_list";

    private static final String PROFILES_ROOT = VpnManager.PROFILES_PATH + "/";
    private static final String PROFILE_OBJ_FILE = ".pobj";

    private static final String STATE_ACTIVE_ACTOR = "active_actor";

    private static final int REQUEST_ADD_OR_EDIT_PROFILE = 1;
    private static final int REQUEST_SELECT_VPN_TYPE = 2;

    private static final int CONTEXT_MENU_CONNECT_ID = ContextMenu.FIRST + 0;
    private static final int CONTEXT_MENU_DISCONNECT_ID = ContextMenu.FIRST + 1;
    private static final int CONTEXT_MENU_EDIT_ID = ContextMenu.FIRST + 2;
    private static final int CONTEXT_MENU_DELETE_ID = ContextMenu.FIRST + 3;

    private static final int CONNECT_BUTTON = DialogInterface.BUTTON1;
    private static final int OK_BUTTON = DialogInterface.BUTTON1;

    private static final int DIALOG_CONNECT = 0;

    private PreferenceScreen mAddVpn;
    private PreferenceCategory mVpnListContainer;

    // profile name --> VpnPreference
    private Map<String, VpnPreference> mVpnPreferenceMap;
    private List<VpnProfile> mVpnProfileList;

    private int mIndexOfEditedProfile = -1;

    // profile engaged in a connection
    private VpnProfile mActiveProfile;

    // actor engaged in connecting
    private VpnProfileActor mConnectingActor;
    private boolean mStateSaved = false;

    // states saved for unlocking keystore
    private Runnable mUnlockAction;

    private VpnManager mVpnManager = new VpnManager(this);

    private ConnectivityReceiver mConnectivityReceiver =
            new ConnectivityReceiver();

    private boolean mConnectingError;

    private StatusChecker mStatusChecker = new StatusChecker();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.vpn_settings);

        // restore VpnProfile list and construct VpnPreference map
        mVpnListContainer = (PreferenceCategory) findPreference(PREF_VPN_LIST);

        // set up the "add vpn" preference
        mAddVpn = (PreferenceScreen) findPreference(PREF_ADD_VPN);
        mAddVpn.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        startVpnTypeSelection();
                        return true;
                    }
                });

        // for long-press gesture on a profile preference
        registerForContextMenu(getListView());

        // listen to vpn connectivity event
        mVpnManager.registerConnectivityReceiver(mConnectivityReceiver);

        String profileName = (savedInstanceState == null)
                ? null
                : savedInstanceState.getString(STATE_ACTIVE_ACTOR);
        mStateSaved = !TextUtils.isEmpty(profileName);
        retrieveVpnListFromStorage();
        if (mStateSaved) {
            mConnectingActor =
                    getActor(mVpnPreferenceMap.get(profileName).mProfile);
        } else {
            checkVpnConnectionStatusInBackground();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mStatusChecker.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusChecker.onResume();

        if ((mUnlockAction != null) && isKeystoreUnlocked()) {
            Runnable action = mUnlockAction;
            mUnlockAction = null;
            runOnUiThread(action);
        }
    }

    @Override
    protected synchronized void onSaveInstanceState(Bundle outState) {
        if (mConnectingActor == null) return;

        outState.putString(STATE_ACTIVE_ACTOR,
                mConnectingActor.getProfile().getName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterForContextMenu(getListView());
        mVpnManager.unregisterConnectivityReceiver(mConnectivityReceiver);
    }

    @Override
    protected Dialog onCreateDialog (int id) {
        switch (id) {
            case DIALOG_CONNECT:
                return createConnectDialog();

            default:
                return null;
        }
    }

    private Dialog createConnectDialog() {
        if (mConnectingActor == null) {
            Log.e(TAG, "no connecting actor to create the dialog");
            return null;
        }
        String name = (mConnectingActor == null)
                ? getString(R.string.vpn_default_profile_name)
                : mConnectingActor.getProfile().getName();
        Dialog d = new AlertDialog.Builder(this)
                .setView(mConnectingActor.createConnectView())
                .setTitle(String.format(getString(R.string.vpn_connect_to),
                        name))
                .setPositiveButton(getString(R.string.vpn_connect_button),
                        this)
                .setNegativeButton(getString(android.R.string.cancel),
                        this)
                .create();
        return d;
    }

    @Override
    protected void onPrepareDialog (int id, Dialog dialog) {
        if (mStateSaved) {
            mStateSaved = false;
            super.onPrepareDialog(id, dialog);
        } else if (mConnectingActor != null) {
            mConnectingActor.updateConnectView(dialog);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        VpnProfile p = getProfile(getProfilePositionFrom(
                    (AdapterContextMenuInfo) menuInfo));
        if (p != null) {
            VpnState state = p.getState();
            menu.setHeaderTitle(p.getName());

            boolean isIdle = (state == VpnState.IDLE);
            boolean isNotConnect =
                    (isIdle || (state == VpnState.DISCONNECTING));
            menu.add(0, CONTEXT_MENU_CONNECT_ID, 0, R.string.vpn_menu_connect)
                    .setEnabled(isIdle && (mActiveProfile == null));
            menu.add(0, CONTEXT_MENU_DISCONNECT_ID, 0,
                    R.string.vpn_menu_disconnect)
                    .setEnabled(state == VpnState.CONNECTED);
            menu.add(0, CONTEXT_MENU_EDIT_ID, 0, R.string.vpn_menu_edit)
                    .setEnabled(isNotConnect);
            menu.add(0, CONTEXT_MENU_DELETE_ID, 0, R.string.vpn_menu_delete)
                    .setEnabled(isNotConnect);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = getProfilePositionFrom(
                (AdapterContextMenuInfo) item.getMenuInfo());
        VpnProfile p = getProfile(position);

        switch(item.getItemId()) {
        case CONTEXT_MENU_CONNECT_ID:
        case CONTEXT_MENU_DISCONNECT_ID:
            connectOrDisconnect(p);
            return true;

        case CONTEXT_MENU_EDIT_ID:
            mIndexOfEditedProfile = position;
            startVpnEditor(p);
            return true;

        case CONTEXT_MENU_DELETE_ID:
            deleteProfile(position);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        final int index = mIndexOfEditedProfile;
        mIndexOfEditedProfile = -1;

        if ((resultCode == RESULT_CANCELED) || (data == null)) {
            Log.d(TAG, "no result returned by editor");
            return;
        }

        if (requestCode == REQUEST_SELECT_VPN_TYPE) {
            String typeName = data.getStringExtra(KEY_VPN_TYPE);
            startVpnEditor(createVpnProfile(typeName));
        } else if (requestCode == REQUEST_ADD_OR_EDIT_PROFILE) {
            VpnProfile p = data.getParcelableExtra(KEY_VPN_PROFILE);
            if (p == null) {
                Log.e(TAG, "null object returned by editor");
                return;
            }

            if (checkDuplicateName(p, index)) {
                final VpnProfile profile = p;
                Util.showErrorMessage(this, String.format(
                        getString(R.string.vpn_error_duplicate_name),
                        p.getName()),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int w) {
                                startVpnEditor(profile);
                            }
                        });
                return;
            }

            if (needKeystoreToSave(p)) {
                Runnable action = new Runnable() {
                    public void run() {
                        mIndexOfEditedProfile = index;
                        onActivityResult(requestCode, resultCode, data);
                    }
                };
                if (!unlockKeystore(p, action)) return;
            }

            try {
                if ((index < 0) || (index >= mVpnProfileList.size())) {
                    addProfile(p);
                    Util.showShortToastMessage(this, String.format(
                            getString(R.string.vpn_profile_added), p.getName()));
                } else {
                    replaceProfile(index, p);
                    Util.showShortToastMessage(this, String.format(
                            getString(R.string.vpn_profile_replaced),
                            p.getName()));
                }
            } catch (IOException e) {
                final VpnProfile profile = p;
                Util.showErrorMessage(this, e + ": " + e.getMessage(),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int w) {
                                startVpnEditor(profile);
                            }
                        });
            }
        } else {
            throw new RuntimeException("unknown request code: " + requestCode);
        }
    }

    // Called when the buttons on the connect dialog are clicked.
    //@Override
    public synchronized void onClick(DialogInterface dialog, int which) {
        dismissDialog(DIALOG_CONNECT);
        if (which == CONNECT_BUTTON) {
            Dialog d = (Dialog) dialog;
            String error = mConnectingActor.validateInputs(d);
            if (error == null) {
                changeState(mConnectingActor.getProfile(), VpnState.CONNECTING);
                mConnectingActor.connect(d);
                return;
            } else {
                // show error dialog
                new AlertDialog.Builder(this)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(String.format(getString(
                                R.string.vpn_error_miss_entering), error))
                        .setPositiveButton(R.string.vpn_back_button,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        showDialog(DIALOG_CONNECT);
                                    }
                                })
                        .show();
            }
        }
    }

    // Replaces the profile at index in mVpnProfileList with p.
    // Returns true if p's name is a duplicate.
    private boolean checkDuplicateName(VpnProfile p, int index) {
        List<VpnProfile> list = mVpnProfileList;
        VpnPreference pref = mVpnPreferenceMap.get(p.getName());
        if ((pref != null) && (index >= 0) && (index < list.size())) {
            // not a duplicate if p is to replace the profile at index
            if (pref.mProfile == list.get(index)) pref = null;
        }
        return (pref != null);
    }

    private int getProfilePositionFrom(AdapterContextMenuInfo menuInfo) {
        // excludes mVpnListContainer and the preferences above it
        return menuInfo.position - mVpnListContainer.getOrder() - 1;
    }

    // position: position in mVpnProfileList
    private VpnProfile getProfile(int position) {
        return ((position >= 0) ? mVpnProfileList.get(position) : null);
    }

    // position: position in mVpnProfileList
    private void deleteProfile(final int position) {
        if ((position < 0) || (position >= mVpnProfileList.size())) return;
        DialogInterface.OnClickListener onClickListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which == OK_BUTTON) {
                            VpnProfile p = mVpnProfileList.remove(position);
                            VpnPreference pref =
                                    mVpnPreferenceMap.remove(p.getName());
                            mVpnListContainer.removePreference(pref);
                            removeProfileFromStorage(p);
                        }
                    }
                };
        new AlertDialog.Builder(this)
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.vpn_confirm_profile_deletion)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setNegativeButton(R.string.vpn_no_button, onClickListener)
                .show();
    }

    // Randomly generates an ID for the profile.
    // The ID is unique and only set once when the profile is created.
    private void setProfileId(VpnProfile profile) {
        String id;

        while (true) {
            id = String.valueOf(Math.abs(
                    Double.doubleToLongBits(Math.random())));
            if (id.length() >= 8) break;
        }
        for (VpnProfile p : mVpnProfileList) {
            if (p.getId().equals(id)) {
                setProfileId(profile);
                return;
            }
        }
        profile.setId(id);
    }

    private void addProfile(VpnProfile p) throws IOException {
        setProfileId(p);
        processSecrets(p);
        saveProfileToStorage(p);

        mVpnProfileList.add(p);
        addPreferenceFor(p);
        disableProfilePreferencesIfOneActive();
    }

    // Adds a preference in mVpnListContainer
    private void addPreferenceFor(VpnProfile p) {
        VpnPreference pref = new VpnPreference(this, p);
        mVpnPreferenceMap.put(p.getName(), pref);
        mVpnListContainer.addPreference(pref);

        pref.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference pref) {
                        connectOrDisconnect(((VpnPreference) pref).mProfile);
                        return true;
                    }
                });
    }

    // index: index to mVpnProfileList
    private void replaceProfile(int index, VpnProfile p) throws IOException {
        Map<String, VpnPreference> map = mVpnPreferenceMap;
        VpnProfile oldProfile = mVpnProfileList.set(index, p);
        VpnPreference pref = map.remove(oldProfile.getName());
        if (pref.mProfile != oldProfile) {
            throw new RuntimeException("inconsistent state!");
        }

        p.setId(oldProfile.getId());

        processSecrets(p);

        // TODO: remove copyFiles once the setId() code propagates.
        // Copy config files and remove the old ones if they are in different
        // directories.
        if (Util.copyFiles(getProfileDir(oldProfile), getProfileDir(p))) {
            removeProfileFromStorage(oldProfile);
        }
        saveProfileToStorage(p);

        pref.setProfile(p);
        map.put(p.getName(), pref);
    }

    private void startVpnTypeSelection() {
        Intent intent = new Intent(this, VpnTypeSelection.class);
        startActivityForResult(intent, REQUEST_SELECT_VPN_TYPE);
    }

    private boolean isKeystoreUnlocked() {
        return (Keystore.getInstance().getState() == Keystore.UNLOCKED);
    }


    // Returns true if the profile needs to access keystore
    private boolean needKeystoreToSave(VpnProfile p) {
        return needKeystoreToConnect(p);
    }

    // Returns true if the profile needs to access keystore
    private boolean needKeystoreToEdit(VpnProfile p) {
        switch (p.getType()) {
            case L2TP_IPSEC:
            case L2TP_IPSEC_PSK:
                return true;

            default:
                return false;
        }
    }

    // Returns true if the profile needs to access keystore
    private boolean needKeystoreToConnect(VpnProfile p) {
        switch (p.getType()) {
            case L2TP_IPSEC:
            case L2TP_IPSEC_PSK:
                return true;

            case L2TP:
                return ((L2tpProfile) p).isSecretEnabled();

            default:
                return false;
        }
    }

    // Returns true if keystore is unlocked or keystore is not a concern
    private boolean unlockKeystore(VpnProfile p, Runnable action) {
        if (isKeystoreUnlocked()) return true;
        mUnlockAction = action;
        startActivity(
                new Intent(SecuritySettings.ACTION_UNLOCK_CREDENTIAL_STORAGE));
        return false;
    }

    private void startVpnEditor(final VpnProfile profile) {
        if (needKeystoreToEdit(profile)) {
            Runnable action = new Runnable() {
                public void run() {
                    startVpnEditor(profile);
                }
            };
            if (!unlockKeystore(profile, action)) return;
        }

        Intent intent = new Intent(this, VpnEditor.class);
        intent.putExtra(KEY_VPN_PROFILE, (Parcelable) profile);
        startActivityForResult(intent, REQUEST_ADD_OR_EDIT_PROFILE);
    }

    private synchronized void connect(final VpnProfile p) {
        if (needKeystoreToConnect(p)) {
            Runnable action = new Runnable() {
                public void run() {
                    connect(p);
                }
            };
            if (!unlockKeystore(p, action)) return;
        }

        mConnectingActor = getActor(p);
        if (mConnectingActor.isConnectDialogNeeded()) {
            removeDialog(DIALOG_CONNECT);
            showDialog(DIALOG_CONNECT);
        } else {
            changeState(p, VpnState.CONNECTING);
            mConnectingActor.connect(null);
        }
    }

    // Do connect or disconnect based on the current state.
    private synchronized void connectOrDisconnect(VpnProfile p) {
        VpnPreference pref = mVpnPreferenceMap.get(p.getName());
        switch (p.getState()) {
            case IDLE:
                connect(p);
                break;

            case CONNECTING:
                // do nothing
                break;

            case CONNECTED:
                mConnectingError = false;
                // pass through
            case DISCONNECTING:
                changeState(p, VpnState.DISCONNECTING);
                getActor(p).disconnect();
                break;
        }
    }

    private void changeState(VpnProfile p, VpnState state) {
        VpnState oldState = p.getState();
        if (oldState == state) return;

        p.setState(state);
        mVpnPreferenceMap.get(p.getName()).setSummary(
                getProfileSummaryString(p));

        switch (state) {
        case CONNECTED:
            mConnectingActor = null;
            // pass through
        case CONNECTING:
            mActiveProfile = p;
            disableProfilePreferencesIfOneActive();
            break;

        case DISCONNECTING:
            if (oldState == VpnState.CONNECTING) {
                mConnectingError = true;
            }
            break;

        case CANCELLED:
            changeState(p, VpnState.IDLE);
            break;

        case IDLE:
            assert(mActiveProfile != p);
            mActiveProfile = null;
            mConnectingActor = null;
            enableProfilePreferences();

            if (oldState == VpnState.CONNECTING) mConnectingError = true;
            if (mConnectingError) showReconnectDialog(p);
            break;
        }
    }

    private void showReconnectDialog(final VpnProfile p) {
        new AlertDialog.Builder(this)
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.vpn_confirm_reconnect)
                .setPositiveButton(R.string.vpn_yes_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int w) {
                                dialog.dismiss();
                                connectOrDisconnect(p);
                            }
                        })
                .setNegativeButton(R.string.vpn_no_button, null)
                .show();
    }

    private void disableProfilePreferencesIfOneActive() {
        if (mActiveProfile == null) return;

        for (VpnProfile p : mVpnProfileList) {
            switch (p.getState()) {
                case DISCONNECTING:
                case IDLE:
                    mVpnPreferenceMap.get(p.getName()).setEnabled(false);
                    break;

                default:
                    mVpnPreferenceMap.get(p.getName()).setEnabled(true);
            }
        }
    }

    private void enableProfilePreferences() {
        for (VpnProfile p : mVpnProfileList) {
            mVpnPreferenceMap.get(p.getName()).setEnabled(true);
        }
    }

    static String getProfileDir(VpnProfile p) {
        return PROFILES_ROOT + p.getId();
    }

    static void saveProfileToStorage(VpnProfile p) throws IOException {
        File f = new File(getProfileDir(p));
        if (!f.exists()) f.mkdirs();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                new File(f, PROFILE_OBJ_FILE)));
        oos.writeObject(p);
        oos.close();
    }

    private void removeProfileFromStorage(VpnProfile p) {
        Util.deleteFile(getProfileDir(p));
    }

    private void retrieveVpnListFromStorage() {
        mVpnPreferenceMap = new LinkedHashMap<String, VpnPreference>();
        mVpnProfileList = Collections.synchronizedList(
                new ArrayList<VpnProfile>());
        mVpnListContainer.removeAll();

        File root = new File(PROFILES_ROOT);
        String[] dirs = root.list();
        if (dirs == null) return;
        for (String dir : dirs) {
            File f = new File(new File(root, dir), PROFILE_OBJ_FILE);
            if (!f.exists()) continue;
            try {
                VpnProfile p = deserialize(f);
                if (p == null) continue;
                if (!checkIdConsistency(dir, p)) continue;

                mVpnProfileList.add(p);
            } catch (IOException e) {
                Log.e(TAG, "retrieveVpnListFromStorage()", e);
            }
        }
        Collections.sort(mVpnProfileList, new Comparator<VpnProfile>() {
            public int compare(VpnProfile p1, VpnProfile p2) {
                return p1.getName().compareTo(p2.getName());
            }

            public boolean equals(VpnProfile p) {
                // not used
                return false;
            }
        });
        for (VpnProfile p : mVpnProfileList) addPreferenceFor(p);
        disableProfilePreferencesIfOneActive();
    }

    private void checkVpnConnectionStatusInBackground() {
        mStatusChecker.check(mVpnProfileList);
    }

    // A sanity check. Returns true if the profile directory name and profile ID
    // are consistent.
    private boolean checkIdConsistency(String dirName, VpnProfile p) {
        if (!dirName.equals(p.getId())) {
            Log.d(TAG, "ID inconsistent: " + dirName + " vs " + p.getId());
            return false;
        } else {
            return true;
        }
    }

    private VpnProfile deserialize(File profileObjectFile) throws IOException {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
                    profileObjectFile));
            VpnProfile p = (VpnProfile) ois.readObject();
            ois.close();
            return p;
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "deserialize a profile", e);
            return null;
        }
    }

    private String getProfileSummaryString(VpnProfile p) {
        switch (p.getState()) {
        case CONNECTING:
            return getString(R.string.vpn_connecting);
        case DISCONNECTING:
            return getString(R.string.vpn_disconnecting);
        case CONNECTED:
            return getString(R.string.vpn_connected);
        default:
            return getString(R.string.vpn_connect_hint);
        }
    }

    private VpnProfileActor getActor(VpnProfile p) {
        return new AuthenticationActor(this, p);
    }

    private VpnProfile createVpnProfile(String type) {
        return mVpnManager.createVpnProfile(Enum.valueOf(VpnType.class, type));
    }

    private static final String NAMESPACE_VPN = "vpn";
    private static final String KEY_PREFIX_IPSEC_PSK = "ipsk000";
    private static final String KEY_PREFIX_L2TP_SECRET = "lscrt000";

    private void processSecrets(VpnProfile p) {
        Keystore ks = Keystore.getInstance();
        switch (p.getType()) {
            case L2TP_IPSEC_PSK:
                L2tpIpsecPskProfile pskProfile = (L2tpIpsecPskProfile) p;
                String keyName = KEY_PREFIX_IPSEC_PSK + p.getId();
                String keyNameForDaemon = NAMESPACE_VPN + "_" + keyName;
                String presharedKey = pskProfile.getPresharedKey();
                if (!presharedKey.equals(keyNameForDaemon)) {
                    int ret = ks.put(NAMESPACE_VPN, keyName, presharedKey);
                    if (ret < 0) Log.e(TAG, "keystore write failed: key=" + keyName);
                    pskProfile.setPresharedKey(keyNameForDaemon);
                }
                // pass through

            case L2TP:
                L2tpProfile l2tpProfile = (L2tpProfile) p;
                keyName = KEY_PREFIX_L2TP_SECRET + p.getId();
                if (l2tpProfile.isSecretEnabled()) {
                    keyNameForDaemon = NAMESPACE_VPN + "_" + keyName;
                    String secret = l2tpProfile.getSecretString();
                    if (!secret.equals(keyNameForDaemon)) {
                        int ret = ks.put(NAMESPACE_VPN, keyName, secret);
                        if (ret < 0) {
                            Log.e(TAG, "keystore write failed: key=" + keyName);
                        }
                        l2tpProfile.setSecretString(keyNameForDaemon);
                    }
                } else {
                    ks.remove(NAMESPACE_VPN, keyName);
                }
                break;
        }
    }

    private class VpnPreference extends Preference {
        VpnProfile mProfile;
        VpnPreference(Context c, VpnProfile p) {
            super(c);
            setProfile(p);
        }

        void setProfile(VpnProfile p) {
            mProfile = p;
            setTitle(p.getName());
            setSummary(getProfileSummaryString(p));
        }
    }

    // to receive vpn connectivity events broadcast by VpnService
    private class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String profileName = intent.getStringExtra(
                    VpnManager.BROADCAST_PROFILE_NAME);
            if (profileName == null) return;

            VpnState s = (VpnState) intent.getSerializableExtra(
                    VpnManager.BROADCAST_CONNECTION_STATE);
            if (s == null) {
                Log.e(TAG, "received null connectivity state");
                return;
            }
            VpnPreference pref = mVpnPreferenceMap.get(profileName);
            if (pref != null) {
                Log.d(TAG, "received connectivity: " + profileName
                        + ": connected? " + s);
                changeState(pref.mProfile, s);
            } else {
                Log.e(TAG, "received connectivity: " + profileName
                        + ": connected? " + s + ", but profile does not exist;"
                        + " just ignore it");
            }
        }
    }

    // managing status check in a background thread
    private class StatusChecker {
        private Set<VpnProfile> mQueue = new HashSet<VpnProfile>();
        private boolean mPaused;
        private ConditionVariable mThreadCv = new ConditionVariable();

        void onPause() {
            mPaused = true;
            mThreadCv.block(); // until the checking thread is over
        }

        synchronized void onResume() {
            mPaused = false;
            start();
        }

        synchronized void check(List<VpnProfile> list) {
            boolean started = !mQueue.isEmpty();
            for (VpnProfile p : list) {
                if (!mQueue.contains(p)) mQueue.add(p);
            }
            if (!started) start();
        }

        private synchronized VpnProfile next() {
            if (mPaused || mQueue.isEmpty()) return null;
            Iterator<VpnProfile> i = mQueue.iterator();
            VpnProfile p = i.next();
            i.remove();
            return p;
        }

        private void start() {
            mThreadCv.close();
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        VpnProfile p = next();
                        if (p == null) break;
                        getActor(p).checkStatus();
                    }
                    mThreadCv.open();
                }
            }).start();
        }
    }
}
