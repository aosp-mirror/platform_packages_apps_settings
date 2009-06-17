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
import android.net.vpn.L2tpIpsecProfile;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.security.Keystore;

/**
 * The class for editing {@link L2tpIpsecProfile}.
 */
class L2tpIpsecEditor extends VpnProfileEditor {
    private static final String TAG = L2tpIpsecEditor.class.getSimpleName();

    private ListPreference mUserCertificate;
    private ListPreference mCaCertificate;

    private L2tpIpsecProfile mProfile;

    public L2tpIpsecEditor(L2tpIpsecProfile p) {
        super(p);
        mProfile = p;
    }

    @Override
    protected void loadExtraPreferencesTo(PreferenceGroup subpanel) {
        Context c = subpanel.getContext();
        subpanel.addPreference(createUserCertificatePreference(c));
        subpanel.addPreference(createCaCertificatePreference(c));
    }

    @Override
    public String validate(Context c) {
        String result = super.validate(c);
        if (result != null) {
            return result;
        } else if (Util.isNullOrEmpty(mUserCertificate.getValue())) {
            return c.getString(R.string.vpn_error_user_certificate_not_selected);
        } else if (Util.isNullOrEmpty(mCaCertificate.getValue())) {
            return c.getString(R.string.vpn_error_ca_certificate_not_selected);
        } else {
            return null;
        }
    }

    private Preference createUserCertificatePreference(Context c) {
        mUserCertificate = createListPreference(c,
                R.string.vpn_user_certificate_title,
                mProfile.getUserCertificate(),
                Keystore.getInstance().getAllUserCertificateKeys(),
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(
                            Preference pref, Object newValue) {
                        mProfile.setUserCertificate((String) newValue);
                        return onPreferenceChangeCommon(pref, newValue);
                    }
                });
        return mUserCertificate;
    }

    private Preference createCaCertificatePreference(Context c) {
        mCaCertificate = createListPreference(c,
                R.string.vpn_ca_certificate_title,
                mProfile.getCaCertificate(),
                Keystore.getInstance().getAllCaCertificateKeys(),
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(
                            Preference pref, Object newValue) {
                        mProfile.setCaCertificate((String) newValue);
                        return onPreferenceChangeCommon(pref, newValue);
                    }
                });
        return mCaCertificate;
    }

    private ListPreference createListPreference(Context c, int titleResId,
            String text, String[] keys,
            Preference.OnPreferenceChangeListener listener) {
        ListPreference pref = new ListPreference(c);
        pref.setTitle(titleResId);
        pref.setDialogTitle(titleResId);
        pref.setPersistent(true);
        pref.setEntries(keys);
        pref.setEntryValues(keys);
        pref.setValue(text);
        pref.setSummary(checkNull(text, c));
        pref.setOnPreferenceChangeListener(listener);
        return pref;
    }

    private boolean onPreferenceChangeCommon(Preference pref, Object newValue) {
        pref.setSummary(checkNull(newValue.toString(), pref.getContext()));
        return true;
    }
}
