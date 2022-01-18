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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class VpnPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_VPN_SETTINGS = "vpn_settings";
    private static final NetworkRequest REQUEST = new NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
            .build();
    private static final String TAG = "VpnPreferenceController";

    private ConnectivityManager mConnectivityManager;
    private Preference mPreference;

    public VpnPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = getEffectivePreference(screen);
    }

    @VisibleForTesting
    protected Preference getEffectivePreference(PreferenceScreen screen) {
        Preference preference = screen.findPreference(KEY_VPN_SETTINGS);
        if (preference == null) {
            return null;
        }
        String toggleable = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        // Manually set dependencies for Wifi when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIFI)) {
            preference.setDependency(SettingsSlicesContract.KEY_AIRPLANE_MODE);
        }
        return preference;
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
        if (mConnectivityManager != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mConnectivityManager = null;
        }
    }

    @Override
    public void onResume() {
        if (isAvailable()) {
            mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
            mConnectivityManager.registerNetworkCallback(REQUEST, mNetworkCallback);
        } else {
            mConnectivityManager = null;
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void updateSummary() {
        if (mPreference == null) {
            return;
        }
        UserManager userManager = mContext.getSystemService(UserManager.class);
        VpnManager vpnManager = mContext.getSystemService(VpnManager.class);
        String summary = getInsecureVpnSummaryOverride(userManager, vpnManager);
        if (summary == null) {
            final UserInfo userInfo = userManager.getUserInfo(UserHandle.myUserId());
            final int uid;
            if (userInfo.isRestricted()) {
                uid = userInfo.restrictedProfileParentId;
            } else {
                uid = userInfo.id;
            }
            VpnConfig vpn = vpnManager.getVpnConfig(uid);
            if ((vpn != null) && vpn.legacy) {
                // Copied from SystemUI::SecurityControllerImpl
                // Legacy VPNs should do nothing if the network is disconnected. Third-party
                // VPN warnings need to continue as traffic can still go to the app.
                final LegacyVpnInfo legacyVpn = vpnManager.getLegacyVpnInfo(uid);
                if (legacyVpn == null || legacyVpn.state != LegacyVpnInfo.STATE_CONNECTED) {
                    vpn = null;
                }
            }
            if (vpn == null) {
                summary = mContext.getString(R.string.vpn_disconnected_summary);
            } else {
                summary = getNameForVpnConfig(vpn, UserHandle.of(uid));
            }
        }
        final String finalSummary = summary;
        ThreadUtils.postOnMainThread(() -> mPreference.setSummary(finalSummary));
    }

    protected int getNumberOfNonLegacyVpn(UserManager userManager, VpnManager vpnManager) {
        // Converted from SystemUI::SecurityControllerImpl
        return (int) userManager.getUsers().stream()
                .map(user -> vpnManager.getVpnConfig(user.id))
                .filter(cfg -> (cfg != null) && (!cfg.legacy))
                .count();
    }

    protected String getInsecureVpnSummaryOverride(UserManager userManager,
            VpnManager vpnManager) {
        // Optionally add warning icon if an insecure VPN is present.
        if (mPreference instanceof VpnInfoPreference) {
            String [] legacyVpnProfileKeys = LegacyVpnProfileStore.list(Credentials.VPN);
            final int insecureVpnCount = getInsecureVpnCount(legacyVpnProfileKeys);
            boolean isInsecureVPN = insecureVpnCount > 0;
            ((VpnInfoPreference) mPreference).setInsecureVpn(isInsecureVPN);

            // Set the summary based on the total number of VPNs and insecure VPNs.
            if (isInsecureVPN) {
                // Add the users and the number of legacy vpns to determine if there is more than
                // one vpn, since there can be more than one VPN per user.
                int vpnCount = legacyVpnProfileKeys.length;
                if (vpnCount <= 1) {
                    vpnCount += getNumberOfNonLegacyVpn(userManager, vpnManager);
                    if (vpnCount == 1) {
                        return mContext.getString(R.string.vpn_settings_insecure_single);
                    }
                }
                if (insecureVpnCount == 1) {
                    return mContext.getString(
                            R.string.vpn_settings_single_insecure_multiple_total,
                            insecureVpnCount);
                } else {
                    return mContext.getString(
                            R.string.vpn_settings_multiple_insecure_multiple_total,
                            insecureVpnCount);
                }
            }
        }
        return null;
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
    protected int getInsecureVpnCount(String [] legacyVpnProfileKeys) {
        final Function<String, VpnProfile> keyToProfile = key ->
                VpnProfile.decode(key, LegacyVpnProfileStore.get(Credentials.VPN + key));
        return (int) Arrays.stream(legacyVpnProfileKeys)
                .map(keyToProfile)
                // Return whether any profile is an insecure type.
                .filter(profile -> VpnProfile.isLegacyType(profile.type))
                .count();
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
