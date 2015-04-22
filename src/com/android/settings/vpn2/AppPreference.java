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

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.Preference;
import android.view.View.OnClickListener;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.settings.R;

/**
 * {@link android.preference.Preference} containing information about a VPN
 * application. Tracks the package name and connection state.
 */
public class AppPreference extends ManageablePreference {
    public static final int STATE_CONNECTED = LegacyVpnInfo.STATE_CONNECTED;
    public static final int STATE_DISCONNECTED = LegacyVpnInfo.STATE_DISCONNECTED;

    private int mState = STATE_DISCONNECTED;
    private String mPackageName;
    private String mName;
    private int mUid;

    public AppPreference(Context context, OnClickListener onManage, final String packageName,
            int uid) {
        super(context, null /* attrs */, onManage);
        mPackageName = packageName;
        mUid = uid;
        update();
    }

    public PackageInfo getPackageInfo() {
        UserHandle user = new UserHandle(UserHandle.getUserId(mUid));
        try {
            IPackageManager ipm = AppGlobals.getPackageManager();
            return ipm.getPackageInfo(mPackageName, 0 /* flags */, user.getIdentifier());
        } catch (RemoteException rme) {
            return null;
        }
    }

    public String getPackageName() {
        return mPackageName;
    }

    public int getUid() {
        return mUid;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        mState = state;
        update();
    }

    private void update() {
        final String[] states = getContext().getResources().getStringArray(R.array.vpn_states);
        setSummary(mState != STATE_DISCONNECTED ? states[mState] : "");

        mName = mPackageName;
        Drawable icon = null;
        try {
            // Make all calls to the package manager as the appropriate user.
            int userId = UserHandle.getUserId(mUid);
            Context userContext = getContext().createPackageContextAsUser(
                    getContext().getPackageName(), 0 /* flags */, new UserHandle(userId));
            PackageManager pm = userContext.getPackageManager();

            // Fetch icon and VPN label
            PackageInfo pkgInfo = pm.getPackageInfo(mPackageName, 0 /* flags */);
            if (pkgInfo != null) {
                icon = pkgInfo.applicationInfo.loadIcon(pm);
                mName = VpnConfig.getVpnLabel(userContext, mPackageName).toString();
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
            // Failed - use default app label and icon as fallback
        }
        if (icon == null) {
            icon = getContext().getPackageManager().getDefaultActivityIcon();
        }
        setTitle(mName);
        setIcon(icon);

        notifyHierarchyChanged();
    }

    public int compareTo(Preference preference) {
        if (preference instanceof AppPreference) {
            AppPreference another = (AppPreference) preference;
            int result;
            if ((result = another.mState - mState) == 0 &&
                    (result = mName.compareToIgnoreCase(another.mName)) == 0 &&
                    (result = mPackageName.compareTo(another.mPackageName)) == 0) {
                result = mUid - another.mUid;
            }
            return result;
        } else if (preference instanceof ConfigPreference) {
            // Use comparator from ConfigPreference
            ConfigPreference another = (ConfigPreference) preference;
            return -another.compareTo(this);
        } else {
            return super.compareTo(preference);
        }
    }
}

