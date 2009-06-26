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

import android.content.Context;
import android.net.vpn.L2tpProfile;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;

/**
 * The class for editing {@link L2tpProfile}.
 */
class L2tpEditor extends VpnProfileEditor {
    private CheckBoxPreference mSecret;
    private EditTextPreference mSecretString;
    private String mOriginalSecret;
    private boolean mOriginalSecretEnabled;

    public L2tpEditor(L2tpProfile p) {
        super(p);
    }

    @Override
    protected void loadExtraPreferencesTo(PreferenceGroup subpanel) {
        Context c = subpanel.getContext();
        subpanel.addPreference(createSecretPreference(c));
        subpanel.addPreference(createSecretStringPreference(c));
        mSecretString.setEnabled(mSecret.isChecked());

        L2tpProfile profile = (L2tpProfile) getProfile();
        mOriginalSecret = profile.getSecretString();
        mOriginalSecretEnabled = profile.isSecretEnabled();
    }

    @Override
    public String validate() {
        String result = super.validate();
        if (!mSecret.isChecked()) return result;

        return ((result != null)
                ? result
                : validate(mSecretString, R.string.vpn_l2tp_secret));
    }

    @Override
    public void saveSecrets(String originalProfileName) {
        L2tpProfile profile = (L2tpProfile) getProfile();
        // TODO: fill up the implementation after keystore is available
    }

    private Preference createSecretPreference(Context c) {
        final L2tpProfile profile = (L2tpProfile) getProfile();
        CheckBoxPreference secret = mSecret = new CheckBoxPreference(c);
        boolean enabled = profile.isSecretEnabled();
        setSecretTitle(secret, R.string.vpn_l2tp_secret, enabled);
        secret.setChecked(enabled);
        setSecretSummary(secret, enabled);
        secret.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(
                            Preference pref, Object newValue) {
                        boolean enabled = (Boolean) newValue;
                        profile.setSecretEnabled(enabled);
                        mSecretString.setEnabled(enabled);
                        setSecretTitle(mSecret, R.string.vpn_l2tp_secret,
                                enabled);
                        setSecretSummary(mSecret, enabled);
                        return true;
                    }
                });
        return secret;
    }

    private Preference createSecretStringPreference(Context c) {
        final L2tpProfile profile = (L2tpProfile) getProfile();
        mSecretString = createSecretPreference(c,
                R.string.vpn_l2tp_secret_string_title,
                R.string.vpn_l2tp_secret,
                profile.getSecretString(),
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(
                            Preference pref, Object newValue) {
                        profile.setSecretString((String) newValue);
                        setSecretSummary(mSecretString,
                                R.string.vpn_l2tp_secret,
                                (String) newValue);
                        return true;
                    }
                });
        return mSecretString;
    }

    private void setSecretSummary(CheckBoxPreference secret, boolean enabled) {
        Context c = secret.getContext();
        String formatString = c.getString(enabled
                ? R.string.vpn_is_enabled
                : R.string.vpn_is_disabled);
        secret.setSummary(String.format(
                formatString, c.getString(R.string.vpn_l2tp_secret)));
    }
}
