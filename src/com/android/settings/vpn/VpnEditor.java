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
import com.android.settings.SettingsPreferenceFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.vpn.L2tpIpsecProfile;
import android.net.vpn.L2tpIpsecPskProfile;
import android.net.vpn.L2tpProfile;
import android.net.vpn.PptpProfile;
import android.net.vpn.VpnProfile;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * The activity class for editing a new or existing VPN profile.
 */
public class VpnEditor extends SettingsPreferenceFragment {
    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_CANCEL = Menu.FIRST + 1;
    private static final int CONFIRM_DIALOG_ID = 0;
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_ORIGINAL_PROFILE_NAME = "orig_profile_name";

    private VpnProfileEditor mProfileEditor;
    private boolean mAddingProfile;
    private byte[] mOriginalProfileData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Loads the XML preferences file
        addPreferencesFromResource(R.xml.vpn_edit);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        VpnProfile p;
        if (savedInstanceState != null) {
            p = (VpnProfile)savedInstanceState.getParcelable(KEY_PROFILE);
        } else {
            p = (VpnProfile)getArguments().getParcelable(VpnSettings.KEY_VPN_PROFILE);
            if (p == null) {
                p = getActivity().getIntent().getParcelableExtra(VpnSettings.KEY_VPN_PROFILE);
            }
        }

        mProfileEditor = getEditor(p);
        mAddingProfile = TextUtils.isEmpty(p.getName());

        initViewFor(p);

        Parcel parcel = Parcel.obtain();
        p.writeToParcel(parcel, 0);
        mOriginalProfileData = parcel.marshall();

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public synchronized void onSaveInstanceState(Bundle outState) {
        if (mProfileEditor == null) return;

        outState.putParcelable(KEY_PROFILE, getProfile());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, MENU_SAVE, 0, R.string.vpn_menu_done)
            .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_CANCEL, 0,
                mAddingProfile ? R.string.vpn_menu_cancel
                               : R.string.vpn_menu_revert)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                if (validateAndSetResult()) finishFragment();
                return true;

            case MENU_CANCEL:
                if (profileChanged()) {
                    showDialog(CONFIRM_DIALOG_ID);
                } else {
                    finishFragment();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (validateAndSetResult()) finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }*/

    private void initViewFor(VpnProfile profile) {
        setTitle(profile);
        mProfileEditor.loadPreferencesTo(getPreferenceScreen());
    }

    private void setTitle(VpnProfile profile) {
        final Activity activity = getActivity();
        String formatString = mAddingProfile
                ? activity.getString(R.string.vpn_edit_title_add)
                : activity.getString(R.string.vpn_edit_title_edit);
        activity.setTitle(String.format(formatString,
                profile.getType().getDisplayName()));
    }

    /**
     * Checks the validity of the inputs and set the profile as result if valid.
     * @return true if the result is successfully set
     */
    private boolean validateAndSetResult() {
        String errorMsg = mProfileEditor.validate();

        if (errorMsg != null) {
            Util.showErrorMessage(getActivity(), errorMsg);
            return false;
        }

        if (profileChanged()) setResult(getProfile());
        return true;
    }

    private void setResult(VpnProfile p) {
        Intent intent = new Intent(getActivity(), VpnSettings.class);
        intent.putExtra(VpnSettings.KEY_VPN_PROFILE, (Parcelable) p);
        setResult(Activity.RESULT_OK, intent);
    }

    private VpnProfileEditor getEditor(VpnProfile p) {
        switch (p.getType()) {
            case L2TP_IPSEC:
                return new L2tpIpsecEditor((L2tpIpsecProfile) p);

            case L2TP_IPSEC_PSK:
                return new L2tpIpsecPskEditor((L2tpIpsecPskProfile) p);

            case L2TP:
                return new L2tpEditor((L2tpProfile) p);

            case PPTP:
                return new PptpEditor((PptpProfile) p);

            default:
                return new VpnProfileEditor(p);
        }
    }


    @Override
    public Dialog onCreateDialog(int id) {
        if (id == CONFIRM_DIALOG_ID) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(mAddingProfile
                            ? R.string.vpn_confirm_add_profile_cancellation
                            : R.string.vpn_confirm_edit_profile_cancellation)
                    .setPositiveButton(R.string.vpn_yes_button,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int w) {
                                    finishFragment();
                                }
                            })
                    .setNegativeButton(R.string.vpn_mistake_button, null)
                    .create();
        }

        return super.onCreateDialog(id);
    }

    /*
    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        if (id == CONFIRM_DIALOG_ID) {
            ((AlertDialog)dialog).setMessage(mAddingProfile
                    ? getString(R.string.vpn_confirm_add_profile_cancellation)
                    : getString(R.string.vpn_confirm_edit_profile_cancellation));
        }
    }*/

    private VpnProfile getProfile() {
        return mProfileEditor.getProfile();
    }

    private boolean profileChanged() {
        Parcel newParcel = Parcel.obtain();
        getProfile().writeToParcel(newParcel, 0);
        byte[] newData = newParcel.marshall();
        if (mOriginalProfileData.length == newData.length) {
            for (int i = 0, n = mOriginalProfileData.length; i < n; i++) {
                if (mOriginalProfileData[i] != newData[i]) return true;
            }
            return false;
        }
        return true;
    }
}
