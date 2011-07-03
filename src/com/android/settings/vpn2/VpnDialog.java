/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

class VpnDialog extends AlertDialog implements TextWatcher, OnItemSelectedListener {
    private static final String DUMMY = "\r";

    private static String getDummy(String secret) {
        return secret.isEmpty() ? "" : DUMMY;
    }

    private static String getSecret(String oldSecret, TextView view) {
        String newSecret = view.getText().toString();
        return DUMMY.equals(newSecret) ? oldSecret : newSecret;
    }

    private final KeyStore mKeyStore = KeyStore.getInstance();
    private final DialogInterface.OnClickListener mListener;
    private final VpnProfile mProfile;

    private boolean mEditing;

    private View mView;

    private TextView mName;
    private Spinner mType;
    private TextView mServer;
    private TextView mUsername;
    private TextView mPassword;
    private TextView mSearchDomains;
    private TextView mRoutes;
    private CheckBox mMppe;
    private TextView mL2tpSecret;
    private TextView mIpsecIdentifier;
    private TextView mIpsecSecret;
    private Spinner mIpsecUserCert;
    private Spinner mIpsecCaCert;
    private CheckBox mSaveLogin;

    VpnDialog(Context context, DialogInterface.OnClickListener listener,
            VpnProfile profile, boolean editing) {
        super(context);
        mListener = listener;
        mProfile = profile;
        mEditing = editing;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        mView = getLayoutInflater().inflate(R.layout.vpn_dialog, null);
        setView(mView);
        setInverseBackgroundForced(true);

        Context context = getContext();

        // First, find out all the fields.
        mName = (TextView) mView.findViewById(R.id.name);
        mType = (Spinner) mView.findViewById(R.id.type);
        mServer = (TextView) mView.findViewById(R.id.server);
        mUsername = (TextView) mView.findViewById(R.id.username);
        mPassword = (TextView) mView.findViewById(R.id.password);
        mSearchDomains = (TextView) mView.findViewById(R.id.search_domains);
        mRoutes = (TextView) mView.findViewById(R.id.routes);
        mMppe = (CheckBox) mView.findViewById(R.id.mppe);
        mL2tpSecret = (TextView) mView.findViewById(R.id.l2tp_secret);
        mIpsecIdentifier = (TextView) mView.findViewById(R.id.ipsec_identifier);
        mIpsecSecret = (TextView) mView.findViewById(R.id.ipsec_secret);
        mIpsecUserCert = (Spinner) mView.findViewById(R.id.ipsec_user_cert);
        mIpsecCaCert = (Spinner) mView.findViewById(R.id.ipsec_ca_cert);
        mSaveLogin = (CheckBox) mView.findViewById(R.id.save_login);

        // Second, copy values from the profile.
        mName.setText(mProfile.name);
        mType.setSelection(mProfile.type);
        mServer.setText(mProfile.server);
        mUsername.setText(mProfile.username);
        mPassword.setText(getDummy(mProfile.password));
        mSearchDomains.setText(mProfile.searchDomains);
        mRoutes.setText(mProfile.routes);
        mMppe.setChecked(mProfile.mppe);
        mL2tpSecret.setText(getDummy(mProfile.l2tpSecret));
        mIpsecIdentifier.setText(mProfile.ipsecIdentifier);
        mIpsecSecret.setText(getDummy(mProfile.ipsecSecret));
        loadCertificates(mIpsecUserCert, Credentials.USER_CERTIFICATE,
                0, mProfile.ipsecUserCert);
        loadCertificates(mIpsecCaCert, Credentials.CA_CERTIFICATE,
                R.string.vpn_no_ca_cert, mProfile.ipsecCaCert);
        mSaveLogin.setChecked(mProfile.saveLogin);

        // Third, add listeners to required fields.
        mName.addTextChangedListener(this);
        mType.setOnItemSelectedListener(this);
        mServer.addTextChangedListener(this);
        mUsername.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        mIpsecSecret.addTextChangedListener(this);
        mIpsecUserCert.setOnItemSelectedListener(this);

        // Forth, determine to do editing or connecting.
        boolean valid = validate(true);
        mEditing = mEditing || !valid;

        if (mEditing) {
            setTitle(R.string.vpn_edit);

            // Show common fields.
            mView.findViewById(R.id.editor).setVisibility(View.VISIBLE);

            // Show type-specific fields.
            changeType(mProfile.type);

            // Create a button to save the profile.
            setButton(DialogInterface.BUTTON_POSITIVE,
                    context.getString(R.string.vpn_save), mListener);
        } else {
            setTitle(context.getString(R.string.vpn_connect_to, mProfile.name));

            // Not editing, just show username and password.
            mView.findViewById(R.id.login).setVisibility(View.VISIBLE);

            // Create a button to connect the network.
            setButton(DialogInterface.BUTTON_POSITIVE,
                    context.getString(R.string.vpn_connect), mListener);
        }

        // Always provide a cancel button.
        setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(R.string.vpn_cancel), mListener);

        // Let AlertDialog create everything.
        super.onCreate(null);

        // Disable the action button if necessary.
        getButton(DialogInterface.BUTTON_POSITIVE)
                .setEnabled(mEditing ? valid : validate(false));
    }

    @Override
    public void afterTextChanged(Editable field) {
        getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(validate(mEditing));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mType) {
            changeType(position);
        }
        getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(validate(mEditing));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void changeType(int type) {
        // First, hide everything.
        mMppe.setVisibility(View.GONE);
        mView.findViewById(R.id.l2tp).setVisibility(View.GONE);
        mView.findViewById(R.id.ipsec_psk).setVisibility(View.GONE);
        mView.findViewById(R.id.ipsec_user).setVisibility(View.GONE);
        mView.findViewById(R.id.ipsec_ca).setVisibility(View.GONE);

        // Then, unhide type-specific fields.
        switch (type) {
            case VpnProfile.TYPE_PPTP:
                mMppe.setVisibility(View.VISIBLE);
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
                mView.findViewById(R.id.l2tp).setVisibility(View.VISIBLE);
                // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                mView.findViewById(R.id.ipsec_psk).setVisibility(View.VISIBLE);
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
                mView.findViewById(R.id.l2tp).setVisibility(View.VISIBLE);
                // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                mView.findViewById(R.id.ipsec_user).setVisibility(View.VISIBLE);
                // fall through
            case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
                mView.findViewById(R.id.ipsec_ca).setVisibility(View.VISIBLE);
                break;
        }
    }

    private boolean validate(boolean editing) {
        if (!editing) {
            return mUsername.getText().length() != 0 && mPassword.getText().length() != 0;
        }
        if (mName.getText().length() == 0 || mServer.getText().length() == 0) {
            return false;
        }
        switch (mType.getSelectedItemPosition()) {
            case VpnProfile.TYPE_PPTP:
                return true;

            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                return mIpsecSecret.getText().length() != 0;

            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                return mIpsecUserCert.getSelectedItemPosition() != 0;
        }
        return false;
    }

    private void loadCertificates(Spinner spinner, String prefix, int firstId, String selected) {
        Context context = getContext();
        String first = (firstId == 0) ? "" : context.getString(firstId);
        String[] certificates = mKeyStore.saw(prefix);

        if (certificates == null || certificates.length == 0) {
            certificates = new String[] {first};
        } else {
            String[] array = new String[certificates.length + 1];
            array[0] = first;
            System.arraycopy(certificates, 0, array, 1, certificates.length);
            certificates = array;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, certificates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        for (int i = 1; i < certificates.length; ++i) {
            if (certificates[i].equals(selected)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    boolean isEditing() {
        return mEditing;
    }

    VpnProfile getProfile() {
        // First, save common fields.
        VpnProfile profile = new VpnProfile(mProfile.key);
        profile.name = mName.getText().toString();
        profile.type = mType.getSelectedItemPosition();
        profile.server = mServer.getText().toString().trim();
        profile.username = mUsername.getText().toString();
        profile.password = getSecret(mProfile.password, mPassword);
        profile.searchDomains = mSearchDomains.getText().toString().trim();
        profile.routes = mRoutes.getText().toString().trim();

        // Then, save type-specific fields.
        switch (profile.type) {
            case VpnProfile.TYPE_PPTP:
                profile.mppe = mMppe.isChecked();
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
                profile.l2tpSecret = getSecret(mProfile.l2tpSecret, mL2tpSecret);
                // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                profile.ipsecSecret = getSecret(mProfile.ipsecSecret, mIpsecSecret);
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
                profile.l2tpSecret = getSecret(mProfile.l2tpSecret, mL2tpSecret);
                // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                if (mIpsecUserCert.getSelectedItemPosition() != 0) {
                    profile.ipsecUserCert = (String) mIpsecUserCert.getSelectedItem();
                }
                // fall through
            case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
                if (mIpsecCaCert.getSelectedItemPosition() != 0) {
                    profile.ipsecCaCert = (String) mIpsecCaCert.getSelectedItem();
                }
                break;
        }

        profile.saveLogin = mSaveLogin.isChecked();
        return profile;
    }
}
