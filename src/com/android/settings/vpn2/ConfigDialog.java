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

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.internal.net.VpnProfile;
import com.android.net.module.util.ProxyUtils;
import com.android.settings.R;
import com.android.settings.utils.AndroidKeystoreAliasLoader;

import java.util.Collection;
import java.util.List;

/**
 * Dialog showing information about a VPN configuration. The dialog
 * can be launched to either edit or prompt for credentials to connect
 * to a user-added VPN.
 *
 * {@see AppDialog}
 */
class ConfigDialog extends AlertDialog implements TextWatcher,
        View.OnClickListener, AdapterView.OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "ConfigDialog";
    // Vpn profile constants to match with R.array.vpn_types.
    private static final List<Integer> VPN_TYPES = List.of(
            VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS,
            VpnProfile.TYPE_IKEV2_IPSEC_PSK,
            VpnProfile.TYPE_IKEV2_IPSEC_RSA
    );

    private final DialogInterface.OnClickListener mListener;
    private final VpnProfile mProfile;

    private boolean mEditing;
    private boolean mExists;

    private View mView;

    private TextView mName;
    private Spinner mType;
    private TextView mServer;
    private TextView mUsername;
    private TextView mPassword;
    private Spinner mProxySettings;
    private TextView mProxyHost;
    private TextView mProxyPort;
    private TextView mIpsecIdentifier;
    private TextView mIpsecSecret;
    private Spinner mIpsecUserCert;
    private Spinner mIpsecCaCert;
    private Spinner mIpsecServerCert;
    private CheckBox mSaveLogin;
    private CheckBox mShowOptions;
    private CheckBox mAlwaysOnVpn;
    private TextView mAlwaysOnInvalidReason;

    ConfigDialog(Context context, DialogInterface.OnClickListener listener,
            VpnProfile profile, boolean editing, boolean exists) {
        super(context);

        mListener = listener;
        mProfile = profile;
        mEditing = editing;
        mExists = exists;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        mView = getLayoutInflater().inflate(R.layout.vpn_dialog, null);
        setView(mView);

        Context context = getContext();

        // First, find out all the fields.
        mName = (TextView) mView.findViewById(R.id.name);
        mType = (Spinner) mView.findViewById(R.id.type);
        mServer = (TextView) mView.findViewById(R.id.server);
        mUsername = (TextView) mView.findViewById(R.id.username);
        mPassword = (TextView) mView.findViewById(R.id.password);
        mProxySettings = (Spinner) mView.findViewById(R.id.vpn_proxy_settings);
        mProxyHost = (TextView) mView.findViewById(R.id.vpn_proxy_host);
        mProxyPort = (TextView) mView.findViewById(R.id.vpn_proxy_port);
        mIpsecIdentifier = (TextView) mView.findViewById(R.id.ipsec_identifier);
        mIpsecSecret = (TextView) mView.findViewById(R.id.ipsec_secret);
        mIpsecUserCert = (Spinner) mView.findViewById(R.id.ipsec_user_cert);
        mIpsecCaCert = (Spinner) mView.findViewById(R.id.ipsec_ca_cert);
        mIpsecServerCert = (Spinner) mView.findViewById(R.id.ipsec_server_cert);
        mSaveLogin = (CheckBox) mView.findViewById(R.id.save_login);
        mShowOptions = (CheckBox) mView.findViewById(R.id.show_options);
        mAlwaysOnVpn = (CheckBox) mView.findViewById(R.id.always_on_vpn);
        mAlwaysOnInvalidReason = (TextView) mView.findViewById(R.id.always_on_invalid_reason);

        // Second, copy values from the profile.
        mName.setText(mProfile.name);
        setTypesByFeature(mType);
        mType.setSelection(convertVpnProfileConstantToTypeIndex(mProfile.type));
        mServer.setText(mProfile.server);
        if (mProfile.saveLogin) {
            mUsername.setText(mProfile.username);
            mPassword.setText(mProfile.password);
        }
        if (mProfile.proxy != null) {
            mProxyHost.setText(mProfile.proxy.getHost());
            int port = mProfile.proxy.getPort();
            mProxyPort.setText(port == 0 ? "" : Integer.toString(port));
        }
        mIpsecIdentifier.setText(mProfile.ipsecIdentifier);
        mIpsecSecret.setText(mProfile.ipsecSecret);
        final AndroidKeystoreAliasLoader androidKeystoreAliasLoader =
                new AndroidKeystoreAliasLoader(null);
        loadCertificates(mIpsecUserCert, androidKeystoreAliasLoader.getKeyCertAliases(), 0,
                mProfile.ipsecUserCert);
        loadCertificates(mIpsecCaCert, androidKeystoreAliasLoader.getCaCertAliases(),
                R.string.vpn_no_ca_cert, mProfile.ipsecCaCert);
        loadCertificates(mIpsecServerCert, androidKeystoreAliasLoader.getKeyCertAliases(),
                R.string.vpn_no_server_cert, mProfile.ipsecServerCert);
        mSaveLogin.setChecked(mProfile.saveLogin);
        mAlwaysOnVpn.setChecked(mProfile.key.equals(VpnUtils.getLockdownVpn()));
        mPassword.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);

        // Hide lockdown VPN on devices that require IMS authentication
        if (SystemProperties.getBoolean("persist.radio.imsregrequired", false)) {
            mAlwaysOnVpn.setVisibility(View.GONE);
        }

        // Third, add listeners to required fields.
        mName.addTextChangedListener(this);
        mType.setOnItemSelectedListener(this);
        mServer.addTextChangedListener(this);
        mUsername.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        mProxySettings.setOnItemSelectedListener(this);
        mProxyHost.addTextChangedListener(this);
        mProxyPort.addTextChangedListener(this);
        mIpsecIdentifier.addTextChangedListener(this);
        mIpsecSecret.addTextChangedListener(this);
        mIpsecUserCert.setOnItemSelectedListener(this);
        mShowOptions.setOnClickListener(this);
        mAlwaysOnVpn.setOnCheckedChangeListener(this);

        // Fourth, determine whether to do editing or connecting.
        mEditing = mEditing || !validate(true /*editing*/);

        if (mEditing) {
            setTitle(R.string.vpn_edit);

            // Show common fields.
            mView.findViewById(R.id.editor).setVisibility(View.VISIBLE);

            // Show type-specific fields.
            changeType(mProfile.type);

            // Hide 'save login' when we are editing.
            mSaveLogin.setVisibility(View.GONE);

            configureAdvancedOptionsVisibility();

            if (mExists) {
                // Create a button to forget the profile if it has already been saved..
                setButton(DialogInterface.BUTTON_NEUTRAL,
                        context.getString(R.string.vpn_forget), mListener);
            }

            // Create a button to save the profile.
            setButton(DialogInterface.BUTTON_POSITIVE,
                    context.getString(R.string.vpn_save), mListener);
        } else {
            setTitle(context.getString(R.string.vpn_connect_to, mProfile.name));

            setUsernamePasswordVisibility(mProfile.type);

            // Create a button to connect the network.
            setButton(DialogInterface.BUTTON_POSITIVE,
                    context.getString(R.string.vpn_connect), mListener);
        }

        // Always provide a cancel button.
        setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(R.string.vpn_cancel), mListener);

        // Let AlertDialog create everything.
        super.onCreate(savedState);

        // Update UI controls according to the current configuration.
        updateUiControls();

        // Workaround to resize the dialog for the input method.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);

        // Visibility isn't restored by super.onRestoreInstanceState, so re-show the advanced
        // options here if they were already revealed or set.
        configureAdvancedOptionsVisibility();
    }

    @Override
    public void afterTextChanged(Editable field) {
        updateUiControls();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onClick(View view) {
        if (view == mShowOptions) {
            configureAdvancedOptionsVisibility();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mType) {
            changeType(VPN_TYPES.get(position));
        } else if (parent == mProxySettings) {
            updateProxyFieldsVisibility(position);
        }
        updateUiControls();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == mAlwaysOnVpn) {
            updateUiControls();
        }
    }

    public boolean isVpnAlwaysOn() {
        return mAlwaysOnVpn.isChecked();
    }

    /**
     * Updates the UI according to the current configuration entered by the user.
     *
     * These include:
     * "Always-on VPN" checkbox
     * Reason for "Always-on VPN" being disabled, when necessary
     * Proxy info if manually configured
     * "Save account information" checkbox
     * "Save" and "Connect" buttons
     */
    private void updateUiControls() {
        VpnProfile profile = getProfile();

        // Always-on VPN
        if (profile.isValidLockdownProfile()) {
            mAlwaysOnVpn.setEnabled(true);
            mAlwaysOnInvalidReason.setVisibility(View.GONE);
        } else {
            mAlwaysOnVpn.setChecked(false);
            mAlwaysOnVpn.setEnabled(false);
            mAlwaysOnInvalidReason.setText(R.string.vpn_always_on_invalid_reason_other);
            mAlwaysOnInvalidReason.setVisibility(View.VISIBLE);
        }

        // Show proxy fields if any proxy field is filled.
        if (mProfile.proxy != null && (!mProfile.proxy.getHost().isEmpty() ||
                mProfile.proxy.getPort() != 0)) {
            mProxySettings.setSelection(VpnProfile.PROXY_MANUAL);
            updateProxyFieldsVisibility(VpnProfile.PROXY_MANUAL);
        }

        // Save account information
        if (mAlwaysOnVpn.isChecked()) {
            mSaveLogin.setChecked(true);
            mSaveLogin.setEnabled(false);
        } else {
            mSaveLogin.setChecked(mProfile.saveLogin);
            mSaveLogin.setEnabled(true);
        }

        // Save or Connect button
        getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(validate(mEditing));
    }

    private void updateProxyFieldsVisibility(int position) {
        final int visible = position == VpnProfile.PROXY_MANUAL ? View.VISIBLE : View.GONE;
        mView.findViewById(R.id.vpn_proxy_fields).setVisibility(visible);
    }

    private boolean isAdvancedOptionsEnabled() {
        return mProxyHost.getText().length() > 0 || mProxyPort.getText().length() > 0;
    }

    private void configureAdvancedOptionsVisibility() {
        if (mShowOptions.isChecked() || isAdvancedOptionsEnabled()) {
            mView.findViewById(R.id.options).setVisibility(View.VISIBLE);
            mShowOptions.setVisibility(View.GONE);
            // TODO(b/149070123): Add ability for platform VPNs to support DNS & routes
        } else {
            mView.findViewById(R.id.options).setVisibility(View.GONE);
            mShowOptions.setVisibility(View.VISIBLE);
        }
    }

    private void changeType(int type) {
        // First, hide everything.
        mView.findViewById(R.id.ipsec_psk).setVisibility(View.GONE);
        mView.findViewById(R.id.ipsec_user).setVisibility(View.GONE);
        mView.findViewById(R.id.ipsec_peer).setVisibility(View.GONE);
        mView.findViewById(R.id.options_ipsec_identity).setVisibility(View.GONE);

        setUsernamePasswordVisibility(type);

        // Always enable identity for IKEv2/IPsec profiles.
        mView.findViewById(R.id.options_ipsec_identity).setVisibility(View.VISIBLE);

        // Then, unhide type-specific fields.
        switch (type) {
            case VpnProfile.TYPE_IKEV2_IPSEC_PSK:
                mView.findViewById(R.id.ipsec_psk).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.options_ipsec_identity).setVisibility(View.VISIBLE);
                break;
            case VpnProfile.TYPE_IKEV2_IPSEC_RSA:
                mView.findViewById(R.id.ipsec_user).setVisibility(View.VISIBLE);
                // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS:
                mView.findViewById(R.id.ipsec_peer).setVisibility(View.VISIBLE);
                break;
        }

        configureAdvancedOptionsVisibility();
    }

    private boolean validate(boolean editing) {
        if (mAlwaysOnVpn.isChecked() && !getProfile().isValidLockdownProfile()) {
            return false;
        }

        final int position = mType.getSelectedItemPosition();
        final int type = VPN_TYPES.get(position);
        if (!editing && requiresUsernamePassword(type)) {
            return mUsername.getText().length() != 0 && mPassword.getText().length() != 0;
        }
        if (mName.getText().length() == 0 || mServer.getText().length() == 0) {
            return false;
        }

        // All IKEv2 methods require an identifier
        if (mIpsecIdentifier.getText().length() == 0) {
            return false;
        }

        if (!validateProxy()) {
            return false;
        }

        switch (type) {
            case VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS:
                return true;

            case VpnProfile.TYPE_IKEV2_IPSEC_PSK:
                return mIpsecSecret.getText().length() != 0;

            case VpnProfile.TYPE_IKEV2_IPSEC_RSA:
                return mIpsecUserCert.getSelectedItemPosition() != 0;
        }
        return false;
    }

    private void setTypesByFeature(Spinner typeSpinner) {
        String[] types = getContext().getResources().getStringArray(R.array.vpn_types);
        if (types.length != VPN_TYPES.size()) {
            Log.wtf(TAG, "VPN_TYPES array length does not match string array");
        }
        // Although FEATURE_IPSEC_TUNNELS should always be present in android S and beyond,
        // keep this check here just to be safe.
        if (!getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_IPSEC_TUNNELS)) {
            Log.wtf(TAG, "FEATURE_IPSEC_TUNNELS missing from system");
        }
        // If the vpn is new or is not already a legacy type,
        // don't allow the user to set the type to a legacy option.

        // Set the mProfile.type to TYPE_IKEV2_IPSEC_USER_PASS if the VPN not exist
        if (!mExists) {
            mProfile.type = VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
    }

    private void loadCertificates(Spinner spinner, Collection<String> choices, int firstId,
            String selected) {
        Context context = getContext();
        String first = (firstId == 0) ? "" : context.getString(firstId);
        String[] myChoices;

        if (choices == null || choices.size() == 0) {
            myChoices = new String[] {first};
        } else {
            myChoices = new String[choices.size() + 1];
            myChoices[0] = first;
            int i = 1;
            for (String c : choices) {
                myChoices[i++] = c;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, myChoices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        for (int i = 1; i < myChoices.length; ++i) {
            if (myChoices[i].equals(selected)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setUsernamePasswordVisibility(int type) {
        mView.findViewById(R.id.userpass).setVisibility(
                requiresUsernamePassword(type) ? View.VISIBLE : View.GONE);
    }

    private boolean requiresUsernamePassword(int type) {
        switch (type) {
            case VpnProfile.TYPE_IKEV2_IPSEC_RSA: // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_PSK:
                return false;
            default:
                return true;
        }
    }

    boolean isEditing() {
        return mEditing;
    }

    boolean hasProxy() {
        return mProxySettings.getSelectedItemPosition() == VpnProfile.PROXY_MANUAL;
    }

    VpnProfile getProfile() {
        // First, save common fields.
        VpnProfile profile = new VpnProfile(mProfile.key);
        profile.name = mName.getText().toString();
        final int position = mType.getSelectedItemPosition();
        profile.type = VPN_TYPES.get(position);
        profile.server = mServer.getText().toString().trim();
        profile.username = mUsername.getText().toString();
        profile.password = mPassword.getText().toString();

        // Save fields based on VPN type.
        profile.ipsecIdentifier = mIpsecIdentifier.getText().toString();

        if (hasProxy()) {
            String proxyHost = mProxyHost.getText().toString().trim();
            String proxyPort = mProxyPort.getText().toString().trim();
            // 0 is a last resort default, but the interface validates that the proxy port is
            // present and non-zero.
            int port = 0;
            if (!proxyPort.isEmpty()) {
                try {
                    port = Integer.parseInt(proxyPort);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Could not parse proxy port integer ", e);
                }
            }
            profile.proxy = ProxyInfo.buildDirectProxy(proxyHost, port);
        } else {
            profile.proxy = null;
        }
        // Then, save type-specific fields.
        switch (profile.type) {
            case VpnProfile.TYPE_IKEV2_IPSEC_PSK:
                profile.ipsecSecret = mIpsecSecret.getText().toString();
                break;

            case VpnProfile.TYPE_IKEV2_IPSEC_RSA:
                if (mIpsecUserCert.getSelectedItemPosition() != 0) {
                    profile.ipsecUserCert = (String) mIpsecUserCert.getSelectedItem();
                    profile.ipsecSecret = profile.ipsecUserCert;
                }
                // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS:
                if (mIpsecCaCert.getSelectedItemPosition() != 0) {
                    profile.ipsecCaCert = (String) mIpsecCaCert.getSelectedItem();
                }
                if (mIpsecServerCert.getSelectedItemPosition() != 0) {
                    profile.ipsecServerCert = (String) mIpsecServerCert.getSelectedItem();
                }
                break;
        }

        final boolean hasLogin = !profile.username.isEmpty() || !profile.password.isEmpty();
        profile.saveLogin = mSaveLogin.isChecked() || (mEditing && hasLogin);
        return profile;
    }

    private boolean validateProxy() {
        if (!hasProxy()) {
            return true;
        }

        final String host = mProxyHost.getText().toString().trim();
        final String port = mProxyPort.getText().toString().trim();
        return ProxyUtils.validate(host, port, "") == ProxyUtils.PROXY_VALID;
    }

    private int convertVpnProfileConstantToTypeIndex(int vpnType) {
        final int typeIndex = VPN_TYPES.indexOf(vpnType);
        if (typeIndex == -1) {
            // Existing legacy profile type
            Log.wtf(TAG, "Invalid existing profile type");
            return 0;
        }
        return typeIndex;
    }
}
