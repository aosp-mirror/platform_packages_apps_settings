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
package com.android.settings.vpn2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;

/**
 * Utility functions for vpn.
 *
 * Keystore methods should only be called in system user
 */
public class VpnUtils {

    private static final String TAG = "VpnUtils";

    public static String getLockdownVpn() {
        final byte[] value = KeyStore.getInstance().get(
            Credentials.LOCKDOWN_VPN, true /* suppressKeyNotFoundWarning */);
        return value == null ? null : new String(value);
    }

    public static void clearLockdownVpn(Context context) {
        KeyStore.getInstance().delete(Credentials.LOCKDOWN_VPN);
        // Always notify ConnectivityManager after keystore update
        getConnectivityManager(context).updateLockdownVpn();
    }

    public static void setLockdownVpn(Context context, String lockdownKey) {
        KeyStore.getInstance().put(Credentials.LOCKDOWN_VPN, lockdownKey.getBytes(),
                KeyStore.UID_SELF, /* flags */ 0);
        // Always notify ConnectivityManager after keystore update
        getConnectivityManager(context).updateLockdownVpn();
    }

    public static boolean isVpnLockdown(String key) {
        return key.equals(getLockdownVpn());
    }

    public static boolean isAnyLockdownActive(Context context) {
        final int userId = context.getUserId();
        if (getLockdownVpn() != null) {
            return true;
        }
        return getConnectivityManager(context).getAlwaysOnVpnPackageForUser(userId) != null
                && Settings.Secure.getIntForUser(context.getContentResolver(),
                        Settings.Secure.ALWAYS_ON_VPN_LOCKDOWN, /* default */ 0, userId) != 0;
    }

    public static boolean isVpnActive(Context context) throws RemoteException {
        return getIConnectivityManager().getVpnConfig(context.getUserId()) != null;
    }

    public static String getConnectedPackage(IConnectivityManager service, final int userId)
            throws RemoteException {
        final VpnConfig config = service.getVpnConfig(userId);
        return config != null ? config.user : null;
    }

    private static ConnectivityManager getConnectivityManager(Context context) {
        return context.getSystemService(ConnectivityManager.class);
    }

    private static IConnectivityManager getIConnectivityManager() {
        return IConnectivityManager.Stub.asInterface(
                ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
    }

    public static boolean isAlwaysOnVpnSet(ConnectivityManager cm, final int userId) {
        return cm.getAlwaysOnVpnPackageForUser(userId) != null;
    }

    public static boolean disconnectLegacyVpn(Context context) {
        try {
            int userId = context.getUserId();
            IConnectivityManager connectivityService = getIConnectivityManager();
            LegacyVpnInfo currentLegacyVpn = connectivityService.getLegacyVpnInfo(userId);
            if (currentLegacyVpn != null) {
                clearLockdownVpn(context);
                connectivityService.prepareVpn(null, VpnConfig.LEGACY_VPN, userId);
                return true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Legacy VPN could not be disconnected", e);
        }
        return false;
    }
}
