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

import static com.android.internal.net.LegacyVpnInfo.STATE_CONNECTED;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.Preference;

import com.android.internal.net.VpnProfile;
import com.android.settings.R;

/**
 * {@link androidx.preference.Preference} tracks the underlying legacy vpn profile and
 * its connection state.
 */
public class LegacyVpnPreference extends ManageablePreference {
    private VpnProfile mProfile;

    LegacyVpnPreference(Context context) {
        super(context, null /* attrs */);
        setIcon(R.drawable.ic_vpn_key);
        setIconSize(ICON_SIZE_SMALL);
    }

    public VpnProfile getProfile() {
        return mProfile;
    }

    public void setProfile(VpnProfile profile) {
        final String oldLabel = (mProfile != null ? mProfile.name : null);
        final String newLabel = (profile != null ? profile.name : null);
        if (!TextUtils.equals(oldLabel, newLabel)) {
            setTitle(newLabel);
            notifyHierarchyChanged();
        }
        mProfile = profile;
    }

    @Override
    public int compareTo(Preference preference) {
        if (preference instanceof LegacyVpnPreference) {
            LegacyVpnPreference another = (LegacyVpnPreference) preference;
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.settings_button && isDisabledByAdmin()) {
            performClick();
            return;
        }
        super.onClick(v);
    }
}
