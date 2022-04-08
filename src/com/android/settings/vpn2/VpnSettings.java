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

import static android.app.AppOpsManager.OP_ACTIVATE_PLATFORM_VPN;
import static android.app.AppOpsManager.OP_ACTIVATE_VPN;

import android.annotation.UiThread;
import android.annotation.WorkerThread;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
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
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.widget.GearPreference;
import com.android.settings.widget.GearPreference.OnGearClickListener;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Settings screen listing VPNs. Configured VPNs and networks managed by apps
 * are shown in the same list.
 */
public class VpnSettings extends RestrictedSettingsFragment implements
        Handler.Callback, Preference.OnPreferenceClickListener {
    private static final String LOG_TAG = "VpnSettings";

    private static final int RESCAN_MESSAGE = 0;
    private static final int RESCAN_INTERVAL_MS = 1000;

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

    private Map<String, LegacyVpnPreference> mLegacyVpnPreferences = new ArrayMap<>();
    private Map<AppVpnInfo, AppPreference> mAppPreferences = new ArrayMap<>();

    @GuardedBy("this")
    private Handler mUpdater;
    private HandlerThread mUpdaterThread;
    private LegacyVpnInfo mConnectedLegacyVpn;

    private boolean mUnavailable;

    public VpnSettings() {
        super(UserManager.DISALLOW_CONFIG_VPN);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.VPN;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mUnavailable = isUiRestricted();
        setHasOptionsMenu(!mUnavailable);

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
            if (isUiRestrictedByOnlyAdmin()) {
                RestrictedLockUtilsInternal.setMenuItemAsDisabledByAdmin(getPrefContext(),
                        menu.getItem(i), getRestrictionEnforcedAdmin());
            } else {
                menu.getItem(i).setEnabled(!mUnavailable);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Generate a new key. Here we just use the current time.
        if (item.getItemId() == R.id.vpn_create) {
            long millis = System.currentTimeMillis();
            while (mLegacyVpnPreferences.containsKey(Long.toHexString(millis))) {
                ++millis;
            }
            VpnProfile profile = new VpnProfile(Long.toHexString(millis));
            ConfigDialogFragment.show(this, profile, true /* editing */, false /* exists */);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        mUnavailable = mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_VPN);
        if (mUnavailable) {
            // Show a message to explain that VPN settings have been disabled
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.vpn_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        } else {
            setEmptyView(getEmptyTextView());
            getEmptyTextView().setText(R.string.vpn_no_vpns_added);
        }

        // Start monitoring
        mConnectivityManager.registerNetworkCallback(VPN_REQUEST, mNetworkCallback);

        // Trigger a refresh
        mUpdaterThread = new HandlerThread("Refresh VPN list in background");
        mUpdaterThread.start();
        mUpdater = new Handler(mUpdaterThread.getLooper(), this);
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

        synchronized (this) {
            mUpdater.removeCallbacksAndMessages(null);
            mUpdater = null;
            mUpdaterThread.quit();
            mUpdaterThread = null;
        }

        super.onPause();
    }

    @Override @WorkerThread
    public boolean handleMessage(Message message) {
        //Return if activity has been recycled
        final Activity activity = getActivity();
        if (activity == null) {
            return true;
        }
        final Context context = activity.getApplicationContext();

        // Run heavy RPCs before switching to UI thread
        final List<VpnProfile> vpnProfiles = loadVpnProfiles(mKeyStore);
        final List<AppVpnInfo> vpnApps = getVpnApps(context, /* includeProfiles */ true);

        final Map<String, LegacyVpnInfo> connectedLegacyVpns = getConnectedLegacyVpns();
        final Set<AppVpnInfo> connectedAppVpns = getConnectedAppVpns();

        final Set<AppVpnInfo> alwaysOnAppVpnInfos = getAlwaysOnAppVpnInfos();
        final String lockdownVpnKey = VpnUtils.getLockdownVpn();

        // Refresh list of VPNs
        activity.runOnUiThread(new UpdatePreferences(this)
                .legacyVpns(vpnProfiles, connectedLegacyVpns, lockdownVpnKey)
                .appVpns(vpnApps, connectedAppVpns, alwaysOnAppVpnInfos));

        synchronized (this) {
            if (mUpdater != null) {
                mUpdater.removeMessages(RESCAN_MESSAGE);
                mUpdater.sendEmptyMessageDelayed(RESCAN_MESSAGE, RESCAN_INTERVAL_MS);
            }
        }
        return true;
    }

    @VisibleForTesting
    static class UpdatePreferences implements Runnable {
        private List<VpnProfile> vpnProfiles = Collections.<VpnProfile>emptyList();
        private List<AppVpnInfo> vpnApps = Collections.<AppVpnInfo>emptyList();

        private Map<String, LegacyVpnInfo> connectedLegacyVpns =
                Collections.<String, LegacyVpnInfo>emptyMap();
        private Set<AppVpnInfo> connectedAppVpns = Collections.<AppVpnInfo>emptySet();

        private Set<AppVpnInfo> alwaysOnAppVpnInfos = Collections.<AppVpnInfo>emptySet();
        private String lockdownVpnKey = null;

        private final VpnSettings mSettings;

        public UpdatePreferences(VpnSettings settings) {
            mSettings = settings;
        }

        public final UpdatePreferences legacyVpns(List<VpnProfile> vpnProfiles,
                Map<String, LegacyVpnInfo> connectedLegacyVpns, String lockdownVpnKey) {
            this.vpnProfiles = vpnProfiles;
            this.connectedLegacyVpns = connectedLegacyVpns;
            this.lockdownVpnKey = lockdownVpnKey;
            return this;
        }

        public final UpdatePreferences appVpns(List<AppVpnInfo> vpnApps,
                Set<AppVpnInfo> connectedAppVpns, Set<AppVpnInfo> alwaysOnAppVpnInfos) {
            this.vpnApps = vpnApps;
            this.connectedAppVpns = connectedAppVpns;
            this.alwaysOnAppVpnInfos = alwaysOnAppVpnInfos;
            return this;
        }

        @Override @UiThread
        public void run() {
            if (!mSettings.canAddPreferences()) {
                return;
            }

            // Find new VPNs by subtracting existing ones from the full set
            final Set<Preference> updates = new ArraySet<>();

            // Add legacy VPNs
            for (VpnProfile profile : vpnProfiles) {
                LegacyVpnPreference p = mSettings.findOrCreatePreference(profile, true);
                if (connectedLegacyVpns.containsKey(profile.key)) {
                    p.setState(connectedLegacyVpns.get(profile.key).state);
                } else {
                    p.setState(LegacyVpnPreference.STATE_NONE);
                }
                p.setAlwaysOn(lockdownVpnKey != null && lockdownVpnKey.equals(profile.key));
                updates.add(p);
            }

            // Show connected VPNs even if the original entry in keystore is gone
            for (LegacyVpnInfo vpn : connectedLegacyVpns.values()) {
                final VpnProfile stubProfile = new VpnProfile(vpn.key);
                LegacyVpnPreference p = mSettings.findOrCreatePreference(stubProfile, false);
                p.setState(vpn.state);
                p.setAlwaysOn(lockdownVpnKey != null && lockdownVpnKey.equals(vpn.key));
                updates.add(p);
            }

            // Add VpnService VPNs
            for (AppVpnInfo app : vpnApps) {
                AppPreference p = mSettings.findOrCreatePreference(app);
                if (connectedAppVpns.contains(app)) {
                    p.setState(AppPreference.STATE_CONNECTED);
                } else {
                    p.setState(AppPreference.STATE_DISCONNECTED);
                }
                p.setAlwaysOn(alwaysOnAppVpnInfos.contains(app));
                updates.add(p);
            }

            // Trim out deleted VPN preferences
            mSettings.setShownPreferences(updates);
        }
    }

    @VisibleForTesting
    public boolean canAddPreferences() {
        return isAdded();
    }

    @VisibleForTesting @UiThread
    public void setShownPreferences(final Collection<Preference> updates) {
        mLegacyVpnPreferences.values().retainAll(updates);
        mAppPreferences.values().retainAll(updates);

        // Change {@param updates} in-place to only contain new preferences that were not already
        // added to the preference screen.
        final PreferenceGroup vpnGroup = getPreferenceScreen();
        for (int i = vpnGroup.getPreferenceCount() - 1; i >= 0; i--) {
            Preference p = vpnGroup.getPreference(i);
            if (updates.contains(p)) {
                updates.remove(p);
            } else {
                vpnGroup.removePreference(p);
            }
        }

        // Show any new preferences on the screen
        for (Preference pref : updates) {
            vpnGroup.addPreference(pref);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof LegacyVpnPreference) {
            LegacyVpnPreference pref = (LegacyVpnPreference) preference;
            VpnProfile profile = pref.getProfile();
            if (mConnectedLegacyVpn != null && profile.key.equals(mConnectedLegacyVpn.key) &&
                    mConnectedLegacyVpn.state == LegacyVpnInfo.STATE_CONNECTED) {
                try {
                    mConnectedLegacyVpn.intent.send();
                    return true;
                } catch (Exception e) {
                    Log.w(LOG_TAG, "Starting config intent failed", e);
                }
            }
            ConfigDialogFragment.show(this, profile, false /* editing */, true /* exists */);
            return true;
        } else if (preference instanceof AppPreference) {
            AppPreference pref = (AppPreference) preference;
            boolean connected = (pref.getState() == AppPreference.STATE_CONNECTED);

            if (!connected) {
                try {
                    UserHandle user = UserHandle.of(pref.getUserId());
                    Context userContext = getActivity().createPackageContextAsUser(
                            getActivity().getPackageName(), 0 /* flags */, user);
                    PackageManager pm = userContext.getPackageManager();
                    Intent appIntent = pm.getLaunchIntentForPackage(pref.getPackageName());
                    if (appIntent != null) {
                        userContext.startActivityAsUser(appIntent, user);
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException nnfe) {
                    Log.w(LOG_TAG, "VPN provider does not exist: " + pref.getPackageName(), nnfe);
                }
            }

            // Already connected or no launch intent available - show an info dialog
            PackageInfo pkgInfo = pref.getPackageInfo();
            AppDialogFragment.show(this, pkgInfo, pref.getLabel(), false /* editing */, connected);
            return true;
        }
        return false;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_vpn;
    }

    private OnGearClickListener mGearListener = new OnGearClickListener() {
        @Override
        public void onGearClick(GearPreference p) {
            if (p instanceof LegacyVpnPreference) {
                LegacyVpnPreference pref = (LegacyVpnPreference) p;
                ConfigDialogFragment.show(VpnSettings.this, pref.getProfile(), true /* editing */,
                        true /* exists */);
            } else if (p instanceof AppPreference) {
                AppPreference pref = (AppPreference) p;
                AppManagementFragment.show(getPrefContext(), pref, getMetricsCategory());
            }
        }
    };

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

    @VisibleForTesting @UiThread
    public LegacyVpnPreference findOrCreatePreference(VpnProfile profile, boolean update) {
        LegacyVpnPreference pref = mLegacyVpnPreferences.get(profile.key);
        boolean created = false;
        if (pref == null ) {
            pref = new LegacyVpnPreference(getPrefContext());
            pref.setOnGearClickListener(mGearListener);
            pref.setOnPreferenceClickListener(this);
            mLegacyVpnPreferences.put(profile.key, pref);
            created = true;
        }
        if (created || update) {
            // This can change call-to-call because the profile can update and keep the same key.
            pref.setProfile(profile);
        }
        return pref;
    }

    @VisibleForTesting @UiThread
    public AppPreference findOrCreatePreference(AppVpnInfo app) {
        AppPreference pref = mAppPreferences.get(app);
        if (pref == null) {
            pref = new AppPreference(getPrefContext(), app.userId, app.packageName);
            pref.setOnGearClickListener(mGearListener);
            pref.setOnPreferenceClickListener(this);
            mAppPreferences.put(app, pref);
        }
        return pref;
    }

    @WorkerThread
    private Map<String, LegacyVpnInfo> getConnectedLegacyVpns() {
        try {
            mConnectedLegacyVpn = mConnectivityService.getLegacyVpnInfo(UserHandle.myUserId());
            if (mConnectedLegacyVpn != null) {
                return Collections.singletonMap(mConnectedLegacyVpn.key, mConnectedLegacyVpn);
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failure updating VPN list with connected legacy VPNs", e);
        }
        return Collections.emptyMap();
    }

    @WorkerThread
    private Set<AppVpnInfo> getConnectedAppVpns() {
        // Mark connected third-party services
        Set<AppVpnInfo> connections = new ArraySet<>();
        try {
            for (UserHandle profile : mUserManager.getUserProfiles()) {
                VpnConfig config = mConnectivityService.getVpnConfig(profile.getIdentifier());
                if (config != null && !config.legacy) {
                    connections.add(new AppVpnInfo(profile.getIdentifier(), config.user));
                }
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failure updating VPN list with connected app VPNs", e);
        }
        return connections;
    }

    @WorkerThread
    private Set<AppVpnInfo> getAlwaysOnAppVpnInfos() {
        Set<AppVpnInfo> result = new ArraySet<>();
        for (UserHandle profile : mUserManager.getUserProfiles()) {
            final int profileId = profile.getIdentifier();
            final String packageName = mConnectivityManager.getAlwaysOnVpnPackageForUser(profileId);
            if (packageName != null) {
                result.add(new AppVpnInfo(profileId, packageName));
            }
        }
        return result;
    }

    static List<AppVpnInfo> getVpnApps(Context context, boolean includeProfiles) {
        List<AppVpnInfo> result = Lists.newArrayList();

        final Set<Integer> profileIds;
        if (includeProfiles) {
            profileIds = new ArraySet<>();
            for (UserHandle profile : UserManager.get(context).getUserProfiles()) {
                profileIds.add(profile.getIdentifier());
            }
        } else {
            profileIds = Collections.singleton(UserHandle.myUserId());
        }

        // Fetch VPN-enabled apps from AppOps.
        AppOpsManager aom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> apps =
                aom.getPackagesForOps(new int[] {OP_ACTIVATE_VPN, OP_ACTIVATE_PLATFORM_VPN});
        if (apps != null) {
            for (AppOpsManager.PackageOps pkg : apps) {
                int userId = UserHandle.getUserId(pkg.getUid());
                if (!profileIds.contains(userId)) {
                    // Skip packages for users outside of our profile group.
                    continue;
                }
                // Look for a MODE_ALLOWED permission to activate VPN.
                boolean allowed = false;
                for (AppOpsManager.OpEntry op : pkg.getOps()) {
                    if ((op.getOp() == OP_ACTIVATE_VPN || op.getOp() == OP_ACTIVATE_PLATFORM_VPN)
                            && op.getMode() == AppOpsManager.MODE_ALLOWED) {
                        allowed = true;
                    }
                }
                if (allowed) {
                    result.add(new AppVpnInfo(userId, pkg.getPackageName()));
                }
            }
        }

        Collections.sort(result);
        return result;
    }

    static List<VpnProfile> loadVpnProfiles(KeyStore keyStore, int... excludeTypes) {
        final ArrayList<VpnProfile> result = Lists.newArrayList();

        for (String key : keyStore.list(Credentials.VPN)) {
            final VpnProfile profile = VpnProfile.decode(key, keyStore.get(Credentials.VPN + key));
            if (profile != null && !ArrayUtils.contains(excludeTypes, profile.type)) {
                result.add(profile);
            }
        }
        return result;
    }
}
