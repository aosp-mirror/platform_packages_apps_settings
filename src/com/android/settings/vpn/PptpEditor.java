/* * Copyright (C) 2009 The Android Open Source Project
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
import android.net.vpn.PptpProfile;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;

/**
 * The class for editing {@link PptpProfile}.
 */
class PptpEditor extends VpnProfileEditor {
    private CheckBoxPreference mEncryption;

    public PptpEditor(PptpProfile p) {
        super(p);
    }

    @Override
    protected void loadExtraPreferencesTo(PreferenceGroup subpanel) {
        Context c = subpanel.getContext();
        subpanel.addPreference(createEncryptionPreference(c));

        PptpProfile profile = (PptpProfile) getProfile();
    }

    private Preference createEncryptionPreference(Context c) {
        final PptpProfile profile = (PptpProfile) getProfile();
        CheckBoxPreference encryption = mEncryption = new CheckBoxPreference(c);
        boolean enabled = profile.isEncryptionEnabled();
        setCheckBoxTitle(encryption, R.string.vpn_pptp_encryption_title);
        encryption.setChecked(enabled);
        setEncryptionSummary(encryption, enabled);
        encryption.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(
                            Preference pref, Object newValue) {
                        boolean enabled = (Boolean) newValue;
                        profile.setEncryptionEnabled(enabled);
                        setEncryptionSummary(mEncryption, enabled);
                        return true;
                    }
                });
        return encryption;
    }

    private void setEncryptionSummary(CheckBoxPreference encryption,
            boolean enabled) {
        Context c = encryption.getContext();
        String formatString = c.getString(enabled
                ? R.string.vpn_is_enabled
                : R.string.vpn_is_disabled);
        encryption.setSummary(String.format(
                formatString, c.getString(R.string.vpn_pptp_encryption)));
    }
}
