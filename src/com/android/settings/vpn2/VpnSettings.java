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

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.IConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static android.app.AppOpsManager.OP_ACTIVATE_VPN;

/**
 * Settings screen listing VPNs. Configured VPNs and networks managed by apps
 * are shown in the same list.
 */
public class VpnSettings extends SettingsPreferenceFragment implements
        Handler.Callback, Preference.OnPreferenceClickListener {
    private static final String LOG_TAG = "VpnSettings";

    private static final int RESCAN_MESSAGE = 0;
    private static final int RESCAN_INTERVAL_MS = 1000;

    private static final String EXTRA_PICK_LOCKDOWN = "android.net.vpn.PICK_LOCKDOWN";
    private static final NetworkRequest VPN_REQUEST = new NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
            .build();

    private final IConnectivityManager mConnectivityService = IConnectivityManager.Stub
            .asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
    private ConnectivityManager mConnectivityManager;
    private UserManager mUserManager;

    private final KeyStore mKeyStore = KeyStore.getInstance();

    private HashMap<String, ConfigPreference> mConfigPreferences = new HashMap<>();
    private HashMap<String, AppPreference> mAppPreferences = new HashMap<>();

    private Handler mUpdater;
    private LegacyVpnInfo mConnectedLegacyVpn;

    private boolean mUnavailable;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.VPN;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_VPN)) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            setHasOptionsMenu(false);
            return;
        }

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.vpn_settings2);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.vpn, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Disable all actions if VPN configuration has been disallowed
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setEnabled(!mUnavailable);
        }

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
                while (mConfigPreferences.containsKey(Long.toHexString(millis))) {
                    ++millis;
                }
                VpnProfile profile = new VpnProfile(Long.toHexString(millis));
                ConfigDialogFragment.show(this, profile, true /* editing */, false /* exists */);
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
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            // Show a message to explain that VPN settings have been disabled
            TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
            getListView().setEmptyView(emptyView);
            if (emptyView != null) {
                emptyView.setText(R.string.vpn_settings_not_available);
            }
            return;
        }

        final boolean pickLockdown = getActivity()
                .getIntent().getBooleanExtra(EXTRA_PICK_LOCKDOWN, false);
        if (pickLockdown) {
            LockdownConfigFragment.show(this);
        }

        // Start monitoring
        mConnectivityManager.registerNetworkCallback(VPN_REQUEST, mNetworkCallback);

        // Trigger a refresh
        if (mUpdater == null) {
            mUpdater = new Handler(this);
        }
        mUpdater.sendEmptyMessage(RESCAN_MESSAGE);
    }

    @Override
    public void onPause() {
        if (mUnavailable) {
            super.onPause();
            return;
        }

        // Stop monitoring
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);

        if (mUpdater != null) {
            mUpdater.removeCallbacksAndMessages(null);
        }

        super.onPause();
    }

    @Override
    public boolean handleMessage(Message message) {
        mUpdater.removeMessages(RESCAN_MESSAGE);

        // Pref group within which to list VPNs
        PreferenceGroup vpnGroup = getPreferenceScreen();
        vpnGroup.removeAll();
        mConfigPreferences.clear();
        mAppPreferences.clear();

        // Fetch configured VPN profiles from KeyStore
        for (VpnProfile profile : loadVpnProfiles(mKeyStore)) {
            final ConfigPreference pref = new ConfigPreference(getActivity(), mManageListener,
                    profile);
            pref.setOnPreferenceClickListener(this);
            mConfigPreferences.put(profile.key, pref);
            vpnGroup.addPreference(pref);
        }

        // 3rd-party VPN apps can change elsewhere. Reload them every time.
        for (AppOpsManager.PackageOps pkg : getVpnApps()) {
            String key = getVpnIdentifier(UserHandle.getUserId(pkg.getUid()), pkg.getPackageName());
            final AppPreference pref = new AppPreference(getActivity(), mManageListener,
                    pkg.getPackageName(), pkg.getUid());
            pref.setOnPreferenceClickListener(this);
            mAppPreferences.put(key, pref);
            vpnGroup.addPreference(pref);
        }

        // Mark out connections with a subtitle
        try {
            // Legacy VPNs
            mConnectedLegacyVpn = null;
            LegacyVpnInfo info = mConnectivityService.getLegacyVpnInfo(UserHandle.myUserId());
            if (info != null) {
                ConfigPreference preference = mConfigPreferences.get(info.key);
                if (preference != null) {
                    preference.setState(info.state);
                    mConnectedLegacyVpn = info;
                }
            }

            // Third-party VPNs
            for (UserHandle profile : mUserManager.getUserProfiles()) {
                VpnConfig cfg = mConnectivityService.getVpnConfig(profile.getIdentifier());
                if (cfg != null) {
                    final String key = getVpnIdentifier(profile.getIdentifier(), cfg.user);
                    final AppPreference preference = mAppPreferences.get(key);
                    if (preference != null) {
                        preference.setState(AppPreference.STATE_CONNECTED);
                    }
                }
            }
        } catch (RemoteException e) {
            // ignore
        }

        mUpdater.sendEmptyMessageDelayed(RESCAN_MESSAGE, RESCAN_INTERVAL_MS);
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof ConfigPreference) {
            VpnProfile profile = ((ConfigPreference) preference).getProfile();
            if (mConnectedLegacyVpn != null && profile.key.equals(mConnectedLegacyVpn.key) &&
                    mConnectedLegacyVpn.state == LegacyVpnInfo.STATE_CONNECTED) {
                try {
                    mConnectedLegacyVpn.intent.send();
                    return true;
                } catch (Exception e) {
                    // ignore
                }
            }
            ConfigDialogFragment.show(this, profile, false /* editing */, true /* exists */);
            return true;
        } else if (preference instanceof AppPreference) {
            AppPreference pref = (AppPreference) preference;
            boolean connected = (pref.getState() == AppPreference.STATE_CONNECTED);

            if (!connected) {
                try {
                    UserHandle user = new UserHandle(UserHandle.getUserId(pref.getUid()));
                    Context userContext = getActivity().createPackageContextAsUser(
                            getActivity().getPackageName(), 0 /* flags */, user);
                    PackageManager pm = userContext.getPackageManager();
                    Intent appIntent = pm.getLaunchIntentForPackage(pref.getPackageName());
                    if (appIntent != null) {
                        userContext.startActivityAsUser(appIntent, user);
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException nnfe) {
                    // Fall through
                }
            }

            // Already onnected or no launch intent available - show an info dialog
            PackageInfo pkgInfo = pref.getPackageInfo();
            AppDialogFragment.show(this, pkgInfo, pref.getLabel(), false /* editing */, connected);
            return true;
        }
        return false;
    }

    private View.OnClickListener mManageListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Object tag = view.getTag();

            if (tag instanceof ConfigPreference) {
                ConfigPreference pref = (ConfigPreference) tag;
                ConfigDialogFragment.show(VpnSettings.this, pref.getProfile(), true /* editing */,
                        true /* exists */);
            } else if (tag instanceof AppPreference) {
                AppPreference pref = (AppPreference) tag;
                boolean connected = (pref.getState() == AppPreference.STATE_CONNECTED);
                AppDialogFragment.show(VpnSettings.this, pref.getPackageInfo(), pref.getLabel(),
                        true /* editing */, connected);
            }
        }
    };

    private static String getVpnIdentifier(int userId, String packageName) {
        return Integer.toString(userId)+ "_" + packageName;
    }

    private NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            if (mUpdater != null) {
                mUpdater.sendEmptyMessage(RESCAN_MESSAGE);
            }
        }

        @Override
        public void onLost(Network network) {
            if (mUpdater != null) {
                mUpdater.sendEmptyMessage(RESCAN_MESSAGE);
            }
        }
    };

    @Override
    protected int getHelpResource() {
        return R.string.help_url_vpn;
    }

    private List<AppOpsManager.PackageOps> getVpnApps() {
        List<AppOpsManager.PackageOps> result = Lists.newArrayList();

        // Build a filter of currently active user profiles.
        SparseArray<Boolean> currentProfileIds = new SparseArray<>();
        for (UserHandle profile : mUserManager.getUserProfiles()) {
            currentProfileIds.put(profile.getIdentifier(), Boolean.TRUE);
        }

        // Fetch VPN-enabled apps from AppOps.
        AppOpsManager aom = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> apps = aom.getPackagesForOps(new int[] {OP_ACTIVATE_VPN});
        if (apps != null) {
            for (AppOpsManager.PackageOps pkg : apps) {
                int userId = UserHandle.getUserId(pkg.getUid());
                if (currentProfileIds.get(userId) == null) {
                    // Skip packages for users outside of our profile group.
                    continue;
                }
                // Look for a MODE_ALLOWED permission to activate VPN.
                boolean allowed = false;
                for (AppOpsManager.OpEntry op : pkg.getOps()) {
                    if (op.getOp() == OP_ACTIVATE_VPN &&
                            op.getMode() == AppOpsManager.MODE_ALLOWED) {
                        allowed = true;
                    }
                }
                if (allowed) {
                    result.add(pkg);
                }
            }
        }
        return result;
    }

    protected static List<VpnProfile> loadVpnProfiles(KeyStore keyStore, int... excludeTypes) {
        final ArrayList<VpnProfile> result = Lists.newArrayList();

        // This might happen if the user does not yet have a keystore. Quietly short-circuit because
        // no keystore means no VPN configs.
        if (!keyStore.isUnlocked()) {
            return result;
        }

        // We are the only user of profiles in KeyStore so no locks are needed.
        for (String key : keyStore.list(Credentials.VPN)) {
            final VpnProfile profile = VpnProfile.decode(key, keyStore.get(Credentials.VPN + key));
            if (profile != null && !ArrayUtils.contains(excludeTypes, profile.type)) {
                result.add(profile);
            }
        }
        return result;
    }
}
