/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.Context;
import android.net.vpn.VpnProfile;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;

/**
 * The common class for editing {@link VpnProfile}.
 */
class VpnProfileEditor {
    private EditTextPreference mServerName;
    private EditTextPreference mDomainSuffices;
    private VpnProfile mProfile;

    public VpnProfileEditor(VpnProfile p) {
        mProfile = p;
    }

    //@Override
    public VpnProfile getProfile() {
        return mProfile;
    }

    /**
     * Adds the preferences to the panel. Subclasses should override
     * {@link #loadExtraPreferencesTo(PreferenceGroup)} instead of this method.
     */
    public void loadPreferencesTo(PreferenceGroup subpanel) {
        Context c = subpanel.getContext();
        subpanel.addPreference(createServerNamePreference(c));
        loadExtraPreferencesTo(subpanel);
        subpanel.addPreference(createDomainSufficesPreference(c));
    }

    /**
     * Adds the extra preferences to the panel. Subclasses should add
     * additional preferences in this method.
     */
    protected void loadExtraPreferencesTo(PreferenceGroup subpanel) {
    }

    /**
     * Validates the inputs in the preferences.
     *
     * @return an error message that is ready to be displayed in a dialog; or
     *      null if all the inputs are valid
     */
    public String validate(Context c) {
        return (Util.isNullOrEmpty(mServerName.getText())
                        ? c.getString(R.string.vpn_error_server_name_empty)
                        : null);
    }

    /**
     * Creates a preference for users to input domain suffices.
     */
    protected EditTextPreference createDomainSufficesPreference(Context c) {
        EditTextPreference pref = mDomainSuffices = new EditTextPreference(c);
        pref.setTitle(R.string.vpn_dns_search_list_title);
        pref.setDialogTitle(R.string.vpn_dns_search_list_title);
        pref.setPersistent(true);
        pref.setText(mProfile.getDomainSuffices());
        pref.setSummary(mProfile.getDomainSuffices());
        pref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(
                            Preference pref, Object newValue) {
                        String v = ((String) newValue).trim();
                        mProfile.setDomainSuffices(v);
                        pref.setSummary(checkNull(v, pref.getContext()));
                        return true;
                    }
                });
        return pref;
    }

    private Preference createServerNamePreference(Context c) {
        EditTextPreference serverName = mServerName = new EditTextPreference(c);
        String title = c.getString(R.string.vpn_server_name_title);
        serverName.setTitle(title);
        serverName.setDialogTitle(title);
        serverName.setSummary(checkNull(mProfile.getServerName(), c));
        serverName.setText(mProfile.getServerName());
        serverName.setPersistent(true);
        serverName.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(
                            Preference pref, Object newValue) {
                        String v = ((String) newValue).trim();
                        mProfile.setServerName(v);
                        pref.setSummary(checkNull(v, pref.getContext()));
                        return true;
                    }
                });
        return mServerName;
    }


   String checkNull(String value, Context c) {
        return ((value != null && value.length() > 0)
                ? value
                : c.getString(R.string.vpn_not_set));
   }
}
