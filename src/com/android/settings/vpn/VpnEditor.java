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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.vpn.L2tpIpsecProfile;
import android.net.vpn.SingleServerProfile;
import android.net.vpn.VpnProfile;
import android.net.vpn.VpnType;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.view.Menu;
import android.view.MenuItem;

/**
 * The activity class for editing a new or existing VPN profile.
 */
public class VpnEditor extends PreferenceActivity {
    private static final String TAG = VpnEditor.class.getSimpleName();

    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_CANCEL = Menu.FIRST + 1;

    private EditTextPreference mName;

    private VpnProfileEditor mProfileEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Loads the XML preferences file
        addPreferencesFromResource(R.xml.vpn_edit);

        mName = (EditTextPreference) findPreference("vpn_name");
        mName.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(
                            Preference pref, Object newValue) {
                        setName((String) newValue);
                        return true;
                    }
                });

        if (savedInstanceState == null) {
            VpnProfile p = getIntent().getParcelableExtra(
                    VpnSettings.KEY_VPN_PROFILE);
            initViewFor(p);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SAVE, 0, R.string.vpn_menu_save)
            .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_CANCEL, 0, R.string.vpn_menu_cancel)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                if (validateAndSetResult()) {
                    finish();
                }
                return true;
            case MENU_CANCEL:
                showCancellationConfirmDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViewFor(VpnProfile profile) {
        VpnProfileEditor editor = getEditor(profile);
        VpnType type = profile.getType();
        PreferenceGroup subsettings = getPreferenceScreen();

        setTitle(profile);
        setName(profile.getName());

        editor.loadPreferencesTo(subsettings);
        mProfileEditor = editor;
    }

    private void setTitle(VpnProfile profile) {
        if (Util.isNullOrEmpty(profile.getName())) {
            setTitle(String.format(getString(R.string.vpn_edit_title_add),
                    profile.getType().getDisplayName()));
        } else {
            setTitle(String.format(getString(R.string.vpn_edit_title_edit),
                    profile.getType().getDisplayName()));
        }
    }

    private void setName(String newName) {
        newName = (newName == null) ? "" : newName.trim();
        mName.setText(newName);
        mName.setSummary(Util.isNullOrEmpty(newName)
                ? getString(R.string.vpn_name_summary)
                : newName);
    }

    /**
     * Checks the validity of the inputs and set the profile as result if valid.
     * @return true if the result is successfully set
     */
    private boolean validateAndSetResult() {
        String errorMsg = null;
        if (Util.isNullOrEmpty(mName.getText())) {
            errorMsg = getString(R.string.vpn_error_name_empty);
        } else {
            errorMsg = mProfileEditor.validate(this);
        }

        if (errorMsg != null) {
            Util.showErrorMessage(this, errorMsg);
            return false;
        }

        setResult(mProfileEditor.getProfile());
        return true;
    }

    private void setResult(VpnProfile p) {
        p.setName(mName.getText());
        p.setId(Util.base64Encode(p.getName().getBytes()));
        Intent intent = new Intent(this, VpnSettings.class);
        intent.putExtra(VpnSettings.KEY_VPN_PROFILE, (Parcelable) p);
        setResult(RESULT_OK, intent);
    }

    private VpnProfileEditor getEditor(VpnProfile p) {
        if (p instanceof L2tpIpsecProfile) {
            return new L2tpIpsecEditor((L2tpIpsecProfile) p);
        } else if (p instanceof SingleServerProfile) {
            return new SingleServerEditor((SingleServerProfile) p);
        } else {
            throw new RuntimeException("Unknown profile type: " + p.getType());
        }
    }

    private void showCancellationConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.vpn_error_title)
                .setMessage(R.string.vpn_confirm_profile_cancellation)
                .setPositiveButton(R.string.vpn_yes_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int w) {
                                finish();
                            }
                        })
                .setNegativeButton(R.string.vpn_mistake_button, null)
                .show();
    }
}
