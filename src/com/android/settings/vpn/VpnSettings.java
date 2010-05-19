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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.vpn.IVpnService;
import android.net.vpn.L2tpIpsecProfile;
import android.net.vpn.L2tpIpsecPskProfile;
import android.net.vpn.L2tpProfile;
import android.net.vpn.VpnManager;
import android.net.vpn.VpnProfile;
import android.net.vpn.VpnState;
import android.net.vpn.VpnType;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final String PROFILES_ROOT = VpnManager.getProfilePath() + "/";
    private static final String PROFILE_OBJ_FILE = ".pobj";

    private static final int REQUEST_ADD_OR_EDIT_PROFILE = 1;
    private static final int REQUEST_SELECT_VPN_TYPE = 2;

    private static final int CONTEXT_MENU_CONNECT_ID = ContextMenu.FIRST + 0;
    private static final int CONTEXT_MENU_DISCONNECT_ID = ContextMenu.FIRST + 1;
    private static final int CONTEXT_MENU_EDIT_ID = ContextMenu.FIRST + 2;
    private static final int CONTEXT_MENU_DELETE_ID = ContextMenu.FIRST + 3;

    private static final int CONNECT_BUTTON = DialogInterface.BUTTON_POSITIVE;
    private static final int OK_BUTTON = DialogInterface.BUTTON_POSITIVE;

    private static final int DIALOG_CONNECT = VpnManager.VPN_ERROR_LARGEST + 1;
    private static final int DIALOG_SECRET_NOT_SET = DIALOG_CONNECT + 1;

    private static final int NO_ERROR = VpnManager.VPN_ERROR_NO_ERROR;

    private static final String KEY_PREFIX_IPSEC_PSK = Credentials.VPN + 'i';
    private static final String KEY_PREFIX_L2TP_SECRET = Credentials.VPN + 'l';

    private PreferenceScreen mAddVpn;
    private PreferenceCategory mVpnListContainer;

    // profile name --> VpnPreference
    private Map<String, VpnPreference> mVpnPreferenceMap;
    private List<VpnProfile> mVpnProfileList;

    // profile engaged in a connection
    private VpnProfile mActiveProfile;

    // actor engaged in connecting
    private VpnProfileActor mConnectingActor;

    // states saved for unlocking keystore
    private Runnable mUnlockAction;

    private KeyStore mKeyStore = KeyStore.getInstance();

    private VpnManager mVpnManager = new VpnManager(this);

    private ConnectivityReceiver mConnectivityReceiver =
            new ConnectivityReceiver();

    private int mConnectingErrorCode = NO_ERROR;

    private Dialog mShowingDialog;

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

        retrieveVpnListFromStorage();
        checkVpnConnectionStatusInBackground();
    }

    @Override
    public void onResume() {
        super.onResume();

        if ((mUnlockAction != null) && isKeyStoreUnlocked()) {
            Runnable action = mUnlockAction;
            mUnlockAction = null;
            runOnUiThread(action);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterForContextMenu(getListView());
        mVpnManager.unregisterConnectivityReceiver(mConnectivityReceiver);
        if ((mShowingDialog != null) && mShowingDialog.isShowing()) {
            mShowingDialog.dismiss();
        }
    }

    @Override
    protected Dialog onCreateDialog (int id) {
        switch (id) {
            case DIALOG_CONNECT:
                return createConnectDialog();

            case DIALOG_SECRET_NOT_SET:
                return createSecretNotSetDialog();

            case VpnManager.VPN_ERROR_CHALLENGE:
            case VpnManager.VPN_ERROR_UNKNOWN_SERVER:
            case VpnManager.VPN_ERROR_PPP_NEGOTIATION_FAILED:
                return createEditDialog(id);

            default:
                Log.d(TAG, "create reconnect dialog for event " + id);
                return createReconnectDialog(id);
        }
    }

    private Dialog createConnectDialog() {
        return new AlertDialog.Builder(this)
                .setView(mConnectingActor.createConnectView())
                .setTitle(String.format(getString(R.string.vpn_connect_to),
                        mActiveProfile.getName()))
                .setPositiveButton(getString(R.string.vpn_connect_button),
                        this)
                .setNegativeButton(getString(android.R.string.cancel),
                        this)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                removeDialog(DIALOG_CONNECT);
                                changeState(mActiveProfile, VpnState.IDLE);
                            }
                        })
                .create();
    }

    private Dialog createReconnectDialog(int id) {
        int msgId;
        switch (id) {
            case VpnManager.VPN_ERROR_AUTH:
                msgId = R.string.vpn_auth_error_dialog_msg;
                break;

            case VpnManager.VPN_ERROR_REMOTE_HUNG_UP:
                msgId = R.string.vpn_remote_hung_up_error_dialog_msg;
                break;

            case VpnManager.VPN_ERROR_CONNECTION_LOST:
                msgId = R.string.vpn_reconnect_from_lost;
                break;

            case VpnManager.VPN_ERROR_REMOTE_PPP_HUNG_UP:
                msgId = R.string.vpn_remote_ppp_hung_up_error_dialog_msg;
                break;

            default:
                msgId = R.string.vpn_confirm_reconnect;
        }
        return createCommonDialogBuilder().setMessage(msgId).create();
    }

    private Dialog createEditDialog(int id) {
        int msgId;
        switch (id) {
            case VpnManager.VPN_ERROR_CHALLENGE:
                msgId = R.string.vpn_challenge_error_dialog_msg;
                break;

            case VpnManager.VPN_ERROR_UNKNOWN_SERVER:
                msgId = R.string.vpn_unknown_server_dialog_msg;
                break;

            case VpnManager.VPN_ERROR_PPP_NEGOTIATION_FAILED:
                msgId = R.string.vpn_ppp_negotiation_failed_dialog_msg;
                break;

            default:
                return null;
        }
        return createCommonEditDialogBuilder().setMessage(msgId).create();
    }

    private Dialog createSecretNotSetDialog() {
        return createCommonDialogBuilder()
                .setMessage(R.string.vpn_secret_not_set_dialog_msg)
                .setPositiveButton(R.string.vpn_yes_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int w) {
                                startVpnEditor(mActiveProfile);
                            }
                        })
                .create();
    }

    private AlertDialog.Builder createCommonEditDialogBuilder() {
        return createCommonDialogBuilder()
                .setPositiveButton(R.string.vpn_yes_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int w) {
                                VpnProfile p = mActiveProfile;
                                onIdle();
                                startVpnEditor(p);
                            }
                        });
    }

    private AlertDialog.Builder createCommonDialogBuilder() {
        return new AlertDialog.Builder(this)
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.vpn_yes_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int w) {
                                connectOrDisconnect(mActiveProfile);
                            }
                        })
                .setNegativeButton(R.string.vpn_no_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int w) {
                                onIdle();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                onIdle();
                            }
                        });
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
            boolean isNotConnect = (isIdle || (state == VpnState.DISCONNECTING)
                    || (state == VpnState.CANCELLED));
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

            int index = getProfileIndexFromId(p.getId());
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

            if (needKeyStoreToSave(p)) {
                Runnable action = new Runnable() {
                    public void run() {
                        onActivityResult(requestCode, resultCode, data);
                    }
                };
                if (!unlockKeyStore(p, action)) return;
            }

            try {
                if (index < 0) {
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
        if (which == CONNECT_BUTTON) {
            Dialog d = (Dialog) dialog;
            String error = mConnectingActor.validateInputs(d);
            if (error == null) {
                mConnectingActor.connect(d);
                removeDialog(DIALOG_CONNECT);
                return;
            } else {
                dismissDialog(DIALOG_CONNECT);
                // show error dialog
                mShowingDialog = new AlertDialog.Builder(this)
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
                        .create();
                mShowingDialog.show();
            }
        } else {
            removeDialog(DIALOG_CONNECT);
            changeState(mActiveProfile, VpnState.IDLE);
        }
    }

    private int getProfileIndexFromId(String id) {
        int index = 0;
        for (VpnProfile p : mVpnProfileList) {
            if (p.getId().equals(id)) {
                return index;
            } else {
                index++;
            }
        }
        return -1;
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
        mShowingDialog = new AlertDialog.Builder(this)
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.vpn_confirm_profile_deletion)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setNegativeButton(R.string.vpn_no_button, onClickListener)
                .create();
        mShowingDialog.show();
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

    private VpnPreference addPreferenceFor(VpnProfile p) {
        return addPreferenceFor(p, true);
    }

    // Adds a preference in mVpnListContainer
    private VpnPreference addPreferenceFor(
            VpnProfile p, boolean addToContainer) {
        VpnPreference pref = new VpnPreference(this, p);
        mVpnPreferenceMap.put(p.getName(), pref);
        if (addToContainer) mVpnListContainer.addPreference(pref);

        pref.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference pref) {
                        connectOrDisconnect(((VpnPreference) pref).mProfile);
                        return true;
                    }
                });
        return pref;
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

    private boolean isKeyStoreUnlocked() {
        return mKeyStore.test() == KeyStore.NO_ERROR;
    }

    // Returns true if the profile needs to access keystore
    private boolean needKeyStoreToSave(VpnProfile p) {
        switch (p.getType()) {
            case L2TP_IPSEC_PSK:
                L2tpIpsecPskProfile pskProfile = (L2tpIpsecPskProfile) p;
                String presharedKey = pskProfile.getPresharedKey();
                if (!TextUtils.isEmpty(presharedKey)) return true;
                // pass through

            case L2TP:
                L2tpProfile l2tpProfile = (L2tpProfile) p;
                if (l2tpProfile.isSecretEnabled() &&
                        !TextUtils.isEmpty(l2tpProfile.getSecretString())) {
                    return true;
                }
                // pass through

            default:
                return false;
        }
    }

    // Returns true if the profile needs to access keystore
    private boolean needKeyStoreToConnect(VpnProfile p) {
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
    private boolean unlockKeyStore(VpnProfile p, Runnable action) {
        if (isKeyStoreUnlocked()) return true;
        mUnlockAction = action;
        Credentials.getInstance().unlock(this);
        return false;
    }

    private void startVpnEditor(final VpnProfile profile) {
        Intent intent = new Intent(this, VpnEditor.class);
        intent.putExtra(KEY_VPN_PROFILE, (Parcelable) profile);
        startActivityForResult(intent, REQUEST_ADD_OR_EDIT_PROFILE);
    }

    private synchronized void connect(final VpnProfile p) {
        if (needKeyStoreToConnect(p)) {
            Runnable action = new Runnable() {
                public void run() {
                    connect(p);
                }
            };
            if (!unlockKeyStore(p, action)) return;
        }

        if (!checkSecrets(p)) return;
        changeState(p, VpnState.CONNECTING);
        if (mConnectingActor.isConnectDialogNeeded()) {
            showDialog(DIALOG_CONNECT);
        } else {
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
            mActiveProfile = p;
            disableProfilePreferencesIfOneActive();
            break;

        case CONNECTING:
            mConnectingActor = getActor(p);
            // pass through
        case DISCONNECTING:
            mActiveProfile = p;
            disableProfilePreferencesIfOneActive();
            break;

        case CANCELLED:
            changeState(p, VpnState.IDLE);
            break;

        case IDLE:
            assert(mActiveProfile == p);

            if (mConnectingErrorCode == NO_ERROR) {
                onIdle();
            } else {
                showDialog(mConnectingErrorCode);
                mConnectingErrorCode = NO_ERROR;
            }
            break;
        }
    }

    private void onIdle() {
        Log.d(TAG, "   onIdle()");
        mActiveProfile = null;
        mConnectingActor = null;
        enableProfilePreferences();
    }

    private void disableProfilePreferencesIfOneActive() {
        if (mActiveProfile == null) return;

        for (VpnProfile p : mVpnProfileList) {
            switch (p.getState()) {
                case CONNECTING:
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
        for (VpnProfile p : mVpnProfileList) {
            Preference pref = addPreferenceFor(p, false);
        }
        disableProfilePreferencesIfOneActive();
    }

    private void checkVpnConnectionStatusInBackground() {
        new Thread(new Runnable() {
            public void run() {
                mStatusChecker.check(mVpnProfileList);
            }
        }).start();
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

    private boolean checkSecrets(VpnProfile p) {
        boolean secretMissing = false;

        if (p instanceof L2tpIpsecProfile) {
            L2tpIpsecProfile certProfile = (L2tpIpsecProfile) p;

            String cert = certProfile.getCaCertificate();
            if (TextUtils.isEmpty(cert) ||
                    !mKeyStore.contains(Credentials.CA_CERTIFICATE + cert)) {
                certProfile.setCaCertificate(null);
                secretMissing = true;
            }

            cert = certProfile.getUserCertificate();
            if (TextUtils.isEmpty(cert) ||
                    !mKeyStore.contains(Credentials.USER_CERTIFICATE + cert)) {
                certProfile.setUserCertificate(null);
                secretMissing = true;
            }
        }

        if (p instanceof L2tpIpsecPskProfile) {
            L2tpIpsecPskProfile pskProfile = (L2tpIpsecPskProfile) p;
            String presharedKey = pskProfile.getPresharedKey();
            String key = KEY_PREFIX_IPSEC_PSK + p.getId();
            if (TextUtils.isEmpty(presharedKey) || !mKeyStore.contains(key)) {
                pskProfile.setPresharedKey(null);
                secretMissing = true;
            }
        }

        if (p instanceof L2tpProfile) {
            L2tpProfile l2tpProfile = (L2tpProfile) p;
            if (l2tpProfile.isSecretEnabled()) {
                String secret = l2tpProfile.getSecretString();
                String key = KEY_PREFIX_L2TP_SECRET + p.getId();
                if (TextUtils.isEmpty(secret) || !mKeyStore.contains(key)) {
                    l2tpProfile.setSecretString(null);
                    secretMissing = true;
                }
            }
        }

        if (secretMissing) {
            mActiveProfile = p;
            showDialog(DIALOG_SECRET_NOT_SET);
            return false;
        } else {
            return true;
        }
    }

    private void processSecrets(VpnProfile p) {
        switch (p.getType()) {
            case L2TP_IPSEC_PSK:
                L2tpIpsecPskProfile pskProfile = (L2tpIpsecPskProfile) p;
                String presharedKey = pskProfile.getPresharedKey();
                String key = KEY_PREFIX_IPSEC_PSK + p.getId();
                if (!TextUtils.isEmpty(presharedKey) &&
                        !mKeyStore.put(key, presharedKey)) {
                    Log.e(TAG, "keystore write failed: key=" + key);
                }
                pskProfile.setPresharedKey(key);
                // pass through

            case L2TP_IPSEC:
            case L2TP:
                L2tpProfile l2tpProfile = (L2tpProfile) p;
                key = KEY_PREFIX_L2TP_SECRET + p.getId();
                if (l2tpProfile.isSecretEnabled()) {
                    String secret = l2tpProfile.getSecretString();
                    if (!TextUtils.isEmpty(secret) &&
                            !mKeyStore.put(key, secret)) {
                        Log.e(TAG, "keystore write failed: key=" + key);
                    }
                    l2tpProfile.setSecretString(key);
                } else {
                    mKeyStore.delete(key);
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

            mConnectingErrorCode = intent.getIntExtra(
                    VpnManager.BROADCAST_ERROR_CODE, NO_ERROR);

            VpnPreference pref = mVpnPreferenceMap.get(profileName);
            if (pref != null) {
                Log.d(TAG, "received connectivity: " + profileName
                        + ": connected? " + s
                        + "   err=" + mConnectingErrorCode);
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
        private List<VpnProfile> mList;

        synchronized void check(final List<VpnProfile> list) {
            final ConditionVariable cv = new ConditionVariable();
            cv.close();
            mVpnManager.startVpnService();
            ServiceConnection c = new ServiceConnection() {
                public synchronized void onServiceConnected(
                        ComponentName className, IBinder binder) {
                    cv.open();

                    IVpnService service = IVpnService.Stub.asInterface(binder);
                    for (VpnProfile p : list) {
                        try {
                            service.checkStatus(p);
                        } catch (Throwable e) {
                            Log.e(TAG, " --- checkStatus(): " + p.getName(), e);
                            changeState(p, VpnState.IDLE);
                        }
                    }
                    VpnSettings.this.unbindService(this);
                    showPreferences();
                }

                public void onServiceDisconnected(ComponentName className) {
                    cv.open();

                    setDefaultState(list);
                    VpnSettings.this.unbindService(this);
                    showPreferences();
                }
            };
            if (mVpnManager.bindVpnService(c)) {
                if (!cv.block(1000)) {
                    Log.d(TAG, "checkStatus() bindService failed");
                    setDefaultState(list);
                }
            } else {
                setDefaultState(list);
            }
        }

        private void showPreferences() {
            for (VpnProfile p : mVpnProfileList) {
                VpnPreference pref = mVpnPreferenceMap.get(p.getName());
                mVpnListContainer.addPreference(pref);
            }
        }

        private void setDefaultState(List<VpnProfile> list) {
            for (VpnProfile p : list) changeState(p, VpnState.IDLE);
            showPreferences();
        }
    }
}
