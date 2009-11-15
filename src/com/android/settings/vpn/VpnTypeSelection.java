/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.vpn;

import com.android.settings.R;

import android.content.Intent;
import android.net.vpn.VpnManager;
import android.net.vpn.VpnType;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import java.util.HashMap;
import java.util.Map;

/**
 * The activity to select a VPN type.
 */
public class VpnTypeSelection extends PreferenceActivity {
    private Map<String, VpnType> mTypeMap = new HashMap<String, VpnType>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.vpn_type);
        initTypeList();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen ps, Preference pref) {
        setResult(mTypeMap.get(pref.getTitle().toString()));
        finish();
        return true;
    }

    private void initTypeList() {
        PreferenceScreen root = getPreferenceScreen();
        for (VpnType t : VpnManager.getSupportedVpnTypes()) {
            String displayName = t.getDisplayName();
            String message = String.format(
                    getString(R.string.vpn_edit_title_add), displayName);
            mTypeMap.put(message, t);

            Preference pref = new Preference(this);
            pref.setTitle(message);
            pref.setSummary(t.getDescriptionId());
            root.addPreference(pref);
        }
    }

    private void setResult(VpnType type) {
        Intent intent = new Intent(this, VpnSettings.class);
        intent.putExtra(VpnSettings.KEY_VPN_TYPE, type.toString());
        setResult(RESULT_OK, intent);
    }
}
