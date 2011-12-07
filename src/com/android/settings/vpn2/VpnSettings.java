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
import android.net.IConnectivityManager;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
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

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.settings.SettingsPreferenceFragment;

import java.net.Inet4Address;
import java.nio.charset.Charsets;
import java.util.Arrays;
import java.util.HashMap;

public class VpnSettings extends SettingsPreferenceFragment implements
        Handler.Callback, Preference.OnPreferenceClickListener,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final String TAG = "VpnSettings";

    private final IConnectivityManager mService = IConnectivityManager.Stub
            .asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private boolean mUnlocking = false;

    private HashMap<String, VpnPreference> mPreferences;
    private VpnDialog mDialog;

    private Handler mUpdater;
    private LegacyVpnInfo mInfo;

    // The key of the profile for the current ContextMenu.
    private String mSelectedKey;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        addPreferencesFromResource(R.xml.vpn_settings2);
        getPreferenceScreen().setOrderingAsAdded(false);

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
        if (mPreferences == null) {
            mPreferences = new HashMap<String, VpnPreference>();
            PreferenceGroup group = getPreferenceScreen();

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
                        group.addPreference(preference);
                    }
                }
            }
            group.findPreference("add_network").setOnPreferenceClickListener(this);
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

    private String[] getDefaultNetwork() throws Exception {
        LinkProperties network = mService.getActiveLinkProperties();
        if (network == null) {
            throw new IllegalStateException("Network is not available");
        }
        String interfaze = network.getInterfaceName();
        if (interfaze == null) {
            throw new IllegalStateException("Cannot get the default interface");
        }
        String gateway = null;
        for (RouteInfo route : network.getRoutes()) {
            // Currently legacy VPN only works on IPv4.
            if (route.isDefaultRoute() && route.getGateway() instanceof Inet4Address) {
                gateway = route.getGateway().getHostAddress();
                break;
            }
        }
        if (gateway == null) {
            throw new IllegalStateException("Cannot get the default gateway");
        }
        return new String[] {interfaze, gateway};
    }

    private void connect(VpnProfile profile) throws Exception {
        // Get the default interface and the default gateway.
        String[] network = getDefaultNetwork();
        String interfaze = network[0];
        String gateway = network[1];

        // Load certificates.
        String privateKey = "";
        String userCert = "";
        String caCert = "";
        String serverCert = "";
        if (!profile.ipsecUserCert.isEmpty()) {
            byte[] value = mKeyStore.get(Credentials.USER_PRIVATE_KEY + profile.ipsecUserCert);
            privateKey = (value == null) ? null : new String(value, Charsets.UTF_8);
            value = mKeyStore.get(Credentials.USER_CERTIFICATE + profile.ipsecUserCert);
            userCert = (value == null) ? null : new String(value, Charsets.UTF_8);
        }
        if (!profile.ipsecCaCert.isEmpty()) {
            byte[] value = mKeyStore.get(Credentials.CA_CERTIFICATE + profile.ipsecCaCert);
            caCert = (value == null) ? null : new String(value, Charsets.UTF_8);
        }
        if (!profile.ipsecServerCert.isEmpty()) {
            byte[] value = mKeyStore.get(Credentials.USER_CERTIFICATE + profile.ipsecServerCert);
            serverCert = (value == null) ? null : new String(value, Charsets.UTF_8);
        }
        if (privateKey == null || userCert == null || caCert == null || serverCert == null) {
            // TODO: find out a proper way to handle this. Delete these keys?
            throw new IllegalStateException("Cannot load credentials");
        }

        // Prepare arguments for racoon.
        String[] racoon = null;
        switch (profile.type) {
            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
                racoon = new String[] {
                    interfaze, profile.server, "udppsk", profile.ipsecIdentifier,
                    profile.ipsecSecret, "1701",
                };
                break;
            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
                racoon = new String[] {
                    interfaze, profile.server, "udprsa", privateKey, userCert,
                    caCert, serverCert, "1701",
                };
                break;
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                racoon = new String[] {
                    interfaze, profile.server, "xauthpsk", profile.ipsecIdentifier,
                    profile.ipsecSecret, profile.username, profile.password, "", gateway,
                };
                break;
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                racoon = new String[] {
                    interfaze, profile.server, "xauthrsa", privateKey, userCert,
                    caCert, serverCert, profile.username, profile.password, "", gateway,
                };
                break;
            case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
                racoon = new String[] {
                    interfaze, profile.server, "hybridrsa",
                    caCert, serverCert, profile.username, profile.password, "", gateway,
                };
                break;
        }

        // Prepare arguments for mtpd.
        String[] mtpd = null;
        switch (profile.type) {
            case VpnProfile.TYPE_PPTP:
                mtpd = new String[] {
                    interfaze, "pptp", profile.server, "1723",
                    "name", profile.username, "password", profile.password,
                    "linkname", "vpn", "refuse-eap", "nodefaultroute",
                    "usepeerdns", "idle", "1800", "mtu", "1400", "mru", "1400",
                    (profile.mppe ? "+mppe" : "nomppe"),
                };
                break;
            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
                mtpd = new String[] {
                    interfaze, "l2tp", profile.server, "1701", profile.l2tpSecret,
                    "name", profile.username, "password", profile.password,
                    "linkname", "vpn", "refuse-eap", "nodefaultroute",
                    "usepeerdns", "idle", "1800", "mtu", "1400", "mru", "1400",
                };
                break;
        }

        VpnConfig config = new VpnConfig();
        config.user = profile.key;
        config.interfaze = interfaze;
        config.session = profile.name;
        config.routes = profile.routes;
        if (!profile.dnsServers.isEmpty()) {
            config.dnsServers = Arrays.asList(profile.dnsServers.split(" +"));
        }
        if (!profile.searchDomains.isEmpty()) {
            config.searchDomains = Arrays.asList(profile.searchDomains.split(" +"));
        }

        mService.startLegacyVpn(config, racoon, mtpd);
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

    private class VpnPreference extends Preference {
        private VpnProfile mProfile;
        private int mState = -1;

        VpnPreference(Context context, VpnProfile profile) {
            super(context);
            setPersistent(false);
            setOrder(0);
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
}
