/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static android.provider.Settings.Global.PRIVATE_DNS_DEFAULT_MODE;
import static android.provider.Settings.Global.PRIVATE_DNS_MODE;
import static android.provider.Settings.Global.PRIVATE_DNS_SPECIFIER;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.net.InetAddress;
import java.util.List;

public class PrivateDnsPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {
    private static final String KEY_PRIVATE_DNS_SETTINGS = "private_dns_settings";

    private static final Uri[] SETTINGS_URIS = new Uri[]{
        Settings.Global.getUriFor(PRIVATE_DNS_MODE),
        Settings.Global.getUriFor(PRIVATE_DNS_DEFAULT_MODE),
        Settings.Global.getUriFor(PRIVATE_DNS_SPECIFIER),
    };

    private final Handler mHandler;
    private final ContentObserver mSettingsObserver;
    private final ConnectivityManager mConnectivityManager;
    private LinkProperties mLatestLinkProperties;
    private Preference mPreference;

    public PrivateDnsPreferenceController(Context context) {
        super(context, KEY_PRIVATE_DNS_SETTINGS);
        mHandler = new Handler(Looper.getMainLooper());
        mSettingsObserver = new PrivateDnsSettingsObserver(mHandler);
        mConnectivityManager = context.getSystemService(ConnectivityManager.class);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PRIVATE_DNS_SETTINGS;
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_private_dns_settings)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        for (Uri uri : SETTINGS_URIS) {
            mContext.getContentResolver().registerContentObserver(uri, false, mSettingsObserver);
        }
        final Network defaultNetwork = mConnectivityManager.getActiveNetwork();
        if (defaultNetwork != null) {
            mLatestLinkProperties = mConnectivityManager.getLinkProperties(defaultNetwork);
        }
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback, mHandler);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    @Override
    public CharSequence getSummary() {
        final Resources res = mContext.getResources();
        final ContentResolver cr = mContext.getContentResolver();
        final String mode = PrivateDnsModeDialogPreference.getModeFromSettings(cr);
        final LinkProperties lp = mLatestLinkProperties;
        final List<InetAddress> dnses = (lp == null) ? null : lp.getValidatedPrivateDnsServers();
        final boolean dnsesResolved = !ArrayUtils.isEmpty(dnses);
        switch (mode) {
            case PRIVATE_DNS_MODE_OFF:
                return res.getString(R.string.private_dns_mode_off);
            case PRIVATE_DNS_MODE_OPPORTUNISTIC:
                return dnsesResolved ? res.getString(R.string.private_dns_mode_on)
                        : res.getString(R.string.private_dns_mode_opportunistic);
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return dnsesResolved
                        ? PrivateDnsModeDialogPreference.getHostnameFromSettings(cr)
                        : res.getString(R.string.private_dns_mode_provider_failure);
        }
        return "";
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!isManagedByAdmin());
    }

    private boolean isManagedByAdmin() {
        EnforcedAdmin enforcedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_CONFIG_PRIVATE_DNS, UserHandle.myUserId());
        return enforcedAdmin != null;
    }

    private class PrivateDnsSettingsObserver extends ContentObserver {
        public PrivateDnsSettingsObserver(Handler h) {
            super(h);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mPreference != null) {
                updateState(mPreference);
            }
        }
    }

    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties lp) {
            mLatestLinkProperties = lp;
            if (mPreference != null) {
                updateState(mPreference);
            }
        }
        @Override
        public void onLost(Network network) {
            mLatestLinkProperties = null;
            if (mPreference != null) {
                updateState(mPreference);
            }
        }
    };
}
