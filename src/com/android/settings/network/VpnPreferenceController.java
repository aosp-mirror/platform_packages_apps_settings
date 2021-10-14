/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.security.Credentials;
import android.security.LegacyVpnProfileStore;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.vpn2.VpnInfoPreference;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

public class VpnPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_VPN_SETTINGS = "vpn_settings";
    private static final NetworkRequest REQUEST = new NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
            .build();
    private static final String TAG = "VpnPreferenceController";

    private final String mToggleable;
    private final UserManager mUserManager;
    private final ConnectivityManager mConnectivityManager;
    private final VpnManager mVpnManager;
    private Preference mPreference;

    public VpnPreferenceController(Context context) {
        super(context);
        mToggleable = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mVpnManager = context.getSystemService(VpnManager.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_VPN_SETTINGS);
        // Manually set dependencies for Wifi when not toggleable.
        if (mToggleable == null || !mToggleable.contains(Settings.Global.RADIO_WIFI)) {
            if (mPreference != null) {
                mPreference.setDependency(SettingsSlicesContract.KEY_AIRPLANE_MODE);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return !RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_CONFIG_VPN, UserHandle.myUserId());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VPN_SETTINGS;
    }

    @Override
    public void onPause() {
        if (isAvailable()) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        }
    }

    @Override
    public void onResume() {
        if (isAvailable()) {
            mConnectivityManager.registerNetworkCallback(REQUEST, mNetworkCallback);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void updateSummary() {
        if (mPreference == null) {
            return;
        }
        // Copied from SystemUI::SecurityControllerImpl
        SparseArray<VpnConfig> vpns = new SparseArray<>();
        final List<UserInfo> users = mUserManager.getUsers();
        int connectedLegacyVpnCount = 0;
        for (UserInfo user : users) {
            VpnConfig cfg = mVpnManager.getVpnConfig(user.id);
            if (cfg == null) {
                continue;
            } else if (cfg.legacy) {
                // Legacy VPNs should do nothing if the network is disconnected. Third-party
                // VPN warnings need to continue as traffic can still go to the app.
                final LegacyVpnInfo legacyVpn = mVpnManager.getLegacyVpnInfo(user.id);
                if (legacyVpn == null || legacyVpn.state != LegacyVpnInfo.STATE_CONNECTED) {
                    continue;
                } else {
                    connectedLegacyVpnCount++;
                }
            }
            vpns.put(user.id, cfg);
        }
        final UserInfo userInfo = mUserManager.getUserInfo(UserHandle.myUserId());
        final int uid;
        if (userInfo.isRestricted()) {
            uid = userInfo.restrictedProfileParentId;
        } else {
            uid = userInfo.id;
        }
        VpnConfig vpn = vpns.get(uid);
        String summary;
        if (vpn == null) {
            summary = mContext.getString(R.string.vpn_disconnected_summary);
        } else {
            summary = getNameForVpnConfig(vpn, UserHandle.of(uid));
        }
        // Optionally add warning icon if an insecure VPN is present.
        if (Utils.isProviderModelEnabled(mContext) && mPreference instanceof VpnInfoPreference) {
            final int insecureVpnCount = getInsecureVpnCount();
            boolean isInsecureVPN = insecureVpnCount > 0;
            ((VpnInfoPreference) mPreference).setInsecureVpn(isInsecureVPN);
            // Set the summary based on the total number of VPNs and insecure VPNs.
            if (isInsecureVPN) {
                // Add the users and the number of legacy vpns to determine if there is more than
                // one vpn, since there can be more than one VPN per user.
                final int vpnCount = vpns.size()
                        + LegacyVpnProfileStore.list(Credentials.VPN).length
                        - connectedLegacyVpnCount;
                if (vpnCount == 1) {
                    summary = mContext.getString(R.string.vpn_settings_insecure_single);
                } else if (insecureVpnCount == 1) {
                    summary = mContext.getString(
                            R.string.vpn_settings_single_insecure_multiple_total,
                            insecureVpnCount);
                } else {
                    summary = mContext.getString(
                            R.string.vpn_settings_multiple_insecure_multiple_total,
                            insecureVpnCount);
                }
            }
        }
        final String finalSummary = summary;
        ThreadUtils.postOnMainThread(() -> mPreference.setSummary(finalSummary));
    }

    @VisibleForTesting
    String getNameForVpnConfig(VpnConfig cfg, UserHandle user) {
        if (cfg.legacy) {
            return mContext.getString(R.string.wifi_display_status_connected);
        }
        // The package name for an active VPN is stored in the 'user' field of its VpnConfig
        final String vpnPackage = cfg.user;
        try {
            Context userContext = mContext.createPackageContextAsUser(mContext.getPackageName(),
                    0 /* flags */, user);
            return VpnConfig.getVpnLabel(userContext, vpnPackage).toString();
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, "Package " + vpnPackage + " is not present", nnfe);
            return null;
        }
    }

    @VisibleForTesting
    protected int getInsecureVpnCount() {
        int count = 0;
        for (String key : LegacyVpnProfileStore.list(Credentials.VPN)) {
            final VpnProfile profile = VpnProfile.decode(key,
                    LegacyVpnProfileStore.get(Credentials.VPN + key));
            // Return whether any profile is an insecure type.
            if (VpnProfile.isLegacyType(profile.type)) {
                count++;
            }
        }
        // We did not find any insecure VPNs.
        return count;
    }

    // Copied from SystemUI::SecurityControllerImpl
    private final ConnectivityManager.NetworkCallback
            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            updateSummary();
        }

        @Override
        public void onLost(Network network) {
            updateSummary();
        }
    };
}
