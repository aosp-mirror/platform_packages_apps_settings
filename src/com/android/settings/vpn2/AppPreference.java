/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import androidx.preference.Preference;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * {@link androidx.preference.Preference} containing information about a VPN
 * application. Tracks the package name and connection state.
 */
public class AppPreference extends ManageablePreference {
    public static final int STATE_CONNECTED = LegacyVpnInfo.STATE_CONNECTED;
    public static final int STATE_DISCONNECTED = STATE_NONE;

    private final String mPackageName;
    private final String mName;

    public AppPreference(Context context, int userId, String packageName) {
        super(context, null /* attrs */);
        super.setUserId(userId);

        mPackageName = packageName;
        disableIfConfiguredByAdmin();

        // Fetch icon and VPN label
        String label = packageName;
        Drawable icon = null;
        try {
            // Make all calls to the package manager as the appropriate user.
            Context userContext = getUserContext();
            PackageManager pm = userContext.getPackageManager();
            // The nested catch block is for the case that the app doesn't exist, so we can fall
            // back to the default activity icon.
            try {
                PackageInfo pkgInfo = pm.getPackageInfo(mPackageName, 0 /* flags */);
                if (pkgInfo != null) {
                    icon = pkgInfo.applicationInfo.loadIcon(pm);
                    label = VpnConfig.getVpnLabel(userContext, mPackageName).toString();
                }
            } catch (PackageManager.NameNotFoundException pkgNotFound) {
                // Use default app label and icon as fallback
            }
            if (icon == null) {
                icon = pm.getDefaultActivityIcon();
            }
        } catch (PackageManager.NameNotFoundException userNotFound) {
            // No user, no useful information to obtain. Quietly fail.
        }
        mName = label;

        setTitle(mName);
        setIcon(icon);
    }

    /**
     * Disable this preference if VPN is set as always on by a profile or device owner.
     * NB: it should be called after super.setUserId() otherwise admin information can be lost.
     */
    private void disableIfConfiguredByAdmin() {
        if (isDisabledByAdmin()) {
            // Already disabled due to user restriction.
            return;
        }
        final DevicePolicyManager dpm = getContext()
                .createContextAsUser(UserHandle.of(getUserId()), /* flags= */ 0)
                .getSystemService(DevicePolicyManager.class);
        if (mPackageName.equals(dpm.getAlwaysOnVpnPackage())) {
            final EnforcedAdmin admin = RestrictedLockUtils.getProfileOrDeviceOwner(
                    getContext(), UserHandle.of(mUserId));
            setDisabledByAdmin(admin);
        }
    }

    public PackageInfo getPackageInfo() {
        try {
            PackageManager pm = getUserContext().getPackageManager();
            return pm.getPackageInfo(mPackageName, 0 /* flags */);
        } catch (PackageManager.NameNotFoundException nnfe) {
            return null;
        }
    }

    public String getLabel() {
        return mName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    private Context getUserContext() throws PackageManager.NameNotFoundException {
        UserHandle user = UserHandle.of(mUserId);
        return getContext().createPackageContextAsUser(
                getContext().getPackageName(), 0 /* flags */, user);
    }

    public int compareTo(Preference preference) {
        if (preference instanceof AppPreference) {
            AppPreference another = (AppPreference) preference;
            int result;
            if ((result = another.mState - mState) == 0 &&
                    (result = mName.compareToIgnoreCase(another.mName)) == 0 &&
                    (result = mPackageName.compareTo(another.mPackageName)) == 0) {
                result = mUserId - another.mUserId;
            }
            return result;
        } else if (preference instanceof LegacyVpnPreference) {
            // Use comparator from ConfigPreference
            LegacyVpnPreference another = (LegacyVpnPreference) preference;
            return -another.compareTo(this);
        } else {
            return super.compareTo(preference);
        }
    }
}

