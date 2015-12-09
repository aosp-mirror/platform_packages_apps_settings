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

import android.content.Context;
import android.support.v7.preference.Preference;
import android.view.View.OnClickListener;

import com.android.internal.net.VpnProfile;
import com.android.settings.R;

import static com.android.internal.net.LegacyVpnInfo.STATE_CONNECTED;

/**
 * {@link android.support.v7.preference.Preference} referencing a VPN
 * configuration. Tracks the underlying profile and its connection
 * state.
 */
public class ConfigPreference extends ManageablePreference {
    public static int STATE_NONE = -1;

    private VpnProfile mProfile;

    /** One of the STATE_* fields from LegacyVpnInfo, or STATE_NONE */
    private int mState = STATE_NONE;

    ConfigPreference(Context context, OnClickListener onManage) {
        super(context, null /* attrs */, onManage);
    }

    public VpnProfile getProfile() {
        return mProfile;
    }

    public void setProfile(VpnProfile profile) {
        mProfile = profile;
        update();
    }

    public void setState(int state) {
        mState = state;
        update();
    }

    private void update() {
        if (mState == STATE_NONE) {
            setSummary("");
        } else {
            final String[] states = getContext().getResources().getStringArray(R.array.vpn_states);
            setSummary(states[mState]);
        }
        if (mProfile != null) {
            setIcon(R.mipmap.ic_launcher_settings);
            setTitle(mProfile.name);
        }
        notifyHierarchyChanged();
    }

    @Override
    public int compareTo(Preference preference) {
        if (preference instanceof ConfigPreference) {
            ConfigPreference another = (ConfigPreference) preference;
            int result;
            if ((result = another.mState - mState) == 0 &&
                    (result = mProfile.name.compareToIgnoreCase(another.mProfile.name)) == 0 &&
                    (result = mProfile.type - another.mProfile.type) == 0) {
                result = mProfile.key.compareTo(another.mProfile.key);
            }
            return result;
        } else if (preference instanceof AppPreference) {
            // Try to sort connected VPNs first
            AppPreference another = (AppPreference) preference;
            if (mState != STATE_CONNECTED && another.getState() == AppPreference.STATE_CONNECTED) {
                return 1;
            }
            // Show configured VPNs before app VPNs
            return -1;
        } else {
            return super.compareTo(preference);
        }
    }
}

