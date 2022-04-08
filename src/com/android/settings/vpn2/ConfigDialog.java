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

import static com.android.internal.net.VpnProfile.isLegacyType;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Proxy;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.android.settings.R;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final KeyStore mKeyStore = KeyStore.getInstance();
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
    private TextView mSearchDomains;
    private TextView mDnsServers;
    private TextView mRoutes;
    private Spinner mProxySettings;
    private TextView mProxyHost;
    private TextView mProxyPort;
    private CheckBox mMppe;
    private TextView mL2tpSecret;
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
        mSearchDomains = (TextView) mView.findViewById(R.id.search_domains);
        mDnsServers = (TextView) mView.findViewById(R.id.dns_servers);
        mRoutes = (TextView) mView.findViewById(R.id.routes);
        mProxySettings = (Spinner) mView.findViewById(R.id.vpn_proxy_settings);
        mProxyHost = (TextView) mView.findViewById(R.id.vpn_proxy_host);
        mProxyPort = (TextView) mView.findViewById(R.id.vpn_proxy_port);
        mMppe = (CheckBox) mView.findViewById(R.id.mppe);
        mL2tpSecret = (TextView) mView.findViewById(R.id.l2tp_secret);
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
        mType.setSelection(mProfile.type);
        mServer.setText(mProfile.server);
        if (mProfile.saveLogin) {
            mUsername.setText(mProfile.username);
            mPassword.setText(mProfile.password);
        }
        mSearchDomains.setText(mProfile.searchDomains);
        mDnsServers.setText(mProfile.dnsServers);
        mRoutes.setText(mProfile.routes);
        if (mProfile.proxy != null) {
            mProxyHost.setText(mProfile.proxy.getHost());
            int port = mProfile.proxy.getPort();
            mProxyPort.setText(port == 0 ? "" : Integer.toString(port));
        }
        mMppe.setChecked(mProfile.mppe);
        mL2tpSecret.setText(mProfile.l2tpSecret);
        mL2tpSecret.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium);
        mIpsecIdentifier.setText(mProfile.ipsecIdentifier);
        mIpsecSecret.setText(mProfile.ipsecSecret);
        loadCertificates(mIpsecUserCert, Credentials.USER_PRIVATE_KEY, 0, mProfile.ipsecUserCert);
        loadCertificates(mIpsecCaCert, Credentials.CA_CERTIFICATE,
                R.string.vpn_no_ca_cert, mProfile.ipsecCaCert);
        loadCertificates(mIpsecServerCert, Credentials.USER_CERTIFICATE,
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
        mDnsServers.addTextChangedListener(this);
        mRoutes.addTextChangedListener(this);
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

            // Create a button to forget the profile if it has already been saved..
            if (mExists) {
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
            changeType(position);
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
            if (!profile.isTypeValidForLockdown()) {
                mAlwaysOnInvalidReason.setText(R.string.vpn_always_on_invalid_reason_type);
            } else if (isLegacyType(profile.type) && !profile.isServerAddressNumeric()) {
                mAlwaysOnInvalidReason.setText(R.string.vpn_always_on_invalid_reason_server);
            } else if (isLegacyType(profile.type) && !profile.hasDns()) {
                mAlwaysOnInvalidReason.setText(R.string.vpn_always_on_invalid_reason_no_dns);
            } else if (isLegacyType(profile.type) && !profile.areDnsAddressesNumeric()) {
                mAlwaysOnInvalidReason.setText(R.string.vpn_always_on_invalid_reason_dns);
            } else {
                mAlwaysOnInvalidReason.setText(R.string.vpn_always_on_invalid_reason_other);
            }
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

    private boolean hasAdvancedOptionsEnabled() {
        return mSearchDomains.getText().length() > 0 || mDnsServers.getText().length() > 0 ||
                    mRoutes.getText().length() > 0 || mProxyHost.getText().length() > 0
                    || mProxyPort.getText().length() > 0;
    }

    private void configureAdvancedOptionsVisibility() {
        if (mShowOptions.isChecked() || hasAdvancedOptionsEnabled()) {
            mView.findViewById(R.id.options).setVisibility(View.VISIBLE);
            mShowOptions.setVisibility(View.GONE);

            // Configure networking option visibility
            // TODO(b/149070123): Add ability for platform VPNs to support DNS & routes
            final int visibility =
                    isLegacyType(mType.getSelectedItemPosition()) ? View.VISIBLE : View.GONE;
            mView.findViewById(R.id.network_options).setVisibility(visibility);
        } else {
            mView.findViewById(R.id.options).setVisibility(View.GONE);
            mShowOptions.setVisibility(View.VISIBLE);
        }
    }

    private void changeType(int type) {
        // First, hide everything.
        mMppe.setVisibility(View.GONE);
        mView.findViewById(R.id.l2tp).setVisibility(View.GONE);
        mView.findViewById(R.id.ipsec_psk).setVisibility(View.GONE);
        mView.findViewById(R.id.ipsec_user).setVisibility(View.GONE);
        mView.findViewById(R.id.ipsec_peer).setVisibility(View.GONE);
        mView.findViewById(R.id.options_ipsec_identity).setVisibility(View.GONE);

        setUsernamePasswordVisibility(type);

        // Always enable identity for IKEv2/IPsec profiles.
        if (!isLegacyType(type)) {
            mView.findViewById(R.id.options_ipsec_identity).setVisibility(View.VISIBLE);
        }

        // Then, unhide type-specific fields.
        switch (type) {
            case VpnProfile.TYPE_PPTP:
                mMppe.setVisibility(View.VISIBLE);
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
                mView.findViewById(R.id.l2tp).setVisibility(View.VISIBLE);
                // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_PSK: // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                mView.findViewById(R.id.ipsec_psk).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.options_ipsec_identity).setVisibility(View.VISIBLE);
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
                mView.findViewById(R.id.l2tp).setVisibility(View.VISIBLE);
                // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_RSA: // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                mView.findViewById(R.id.ipsec_user).setVisibility(View.VISIBLE);
                // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS: // fall through
            case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
                mView.findViewById(R.id.ipsec_peer).setVisibility(View.VISIBLE);
                break;
        }

        configureAdvancedOptionsVisibility();
    }

    private boolean validate(boolean editing) {
        if (mAlwaysOnVpn.isChecked() && !getProfile().isValidLockdownProfile()) {
            return false;
        }

        final int type = mType.getSelectedItemPosition();
        if (!editing && requiresUsernamePassword(type)) {
            return mUsername.getText().length() != 0 && mPassword.getText().length() != 0;
        }
        if (mName.getText().length() == 0 || mServer.getText().length() == 0) {
            return false;
        }

        // TODO(b/149070123): Add ability for platform VPNs to support DNS & routes
        if (isLegacyType(mProfile.type)
                && (!validateAddresses(mDnsServers.getText().toString(), false)
                        || !validateAddresses(mRoutes.getText().toString(), true))) {
            return false;
        }

        // All IKEv2 methods require an identifier
        if (!isLegacyType(mProfile.type) && mIpsecIdentifier.getText().length() == 0) {
            return false;
        }

        if (!validateProxy()) {
            return false;
        }

        switch (type) {
            case VpnProfile.TYPE_PPTP: // fall through
            case VpnProfile.TYPE_IPSEC_HYBRID_RSA: // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS:
                return true;

            case VpnProfile.TYPE_IKEV2_IPSEC_PSK: // fall through
            case VpnProfile.TYPE_L2TP_IPSEC_PSK: // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                return mIpsecSecret.getText().length() != 0;

            case VpnProfile.TYPE_IKEV2_IPSEC_RSA: // fall through
            case VpnProfile.TYPE_L2TP_IPSEC_RSA: // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                return mIpsecUserCert.getSelectedItemPosition() != 0;
        }
        return false;
    }

    private boolean validateAddresses(String addresses, boolean cidr) {
        try {
            for (String address : addresses.split(" ")) {
                if (address.isEmpty()) {
                    continue;
                }
                // Legacy VPN currently only supports IPv4.
                int prefixLength = 32;
                if (cidr) {
                    String[] parts = address.split("/", 2);
                    address = parts[0];
                    prefixLength = Integer.parseInt(parts[1]);
                }
                byte[] bytes = InetAddress.parseNumericAddress(address).getAddress();
                int integer = (bytes[3] & 0xFF) | (bytes[2] & 0xFF) << 8 |
                        (bytes[1] & 0xFF) << 16 | (bytes[0] & 0xFF) << 24;
                if (bytes.length != 4 || prefixLength < 0 || prefixLength > 32 ||
                        (prefixLength < 32 && (integer << prefixLength) != 0)) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void setTypesByFeature(Spinner typeSpinner) {
        String[] types = getContext().getResources().getStringArray(R.array.vpn_types);
        if (!getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_IPSEC_TUNNELS)) {
            final List<String> typesList = new ArrayList<>(Arrays.asList(types));

            // This must be removed from back to front in order to ensure index consistency
            typesList.remove(VpnProfile.TYPE_IKEV2_IPSEC_RSA);
            typesList.remove(VpnProfile.TYPE_IKEV2_IPSEC_PSK);
            typesList.remove(VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS);

            types = typesList.toArray(new String[0]);
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
    }

    private void loadCertificates(Spinner spinner, String prefix, int firstId, String selected) {
        Context context = getContext();
        String first = (firstId == 0) ? "" : context.getString(firstId);
        String[] certificates = mKeyStore.list(prefix);

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
        profile.type = mType.getSelectedItemPosition();
        profile.server = mServer.getText().toString().trim();
        profile.username = mUsername.getText().toString();
        profile.password = mPassword.getText().toString();

        // Save fields based on VPN type.
        if (isLegacyType(profile.type)) {
            // TODO(b/149070123): Add ability for platform VPNs to support DNS & routes
            profile.searchDomains = mSearchDomains.getText().toString().trim();
            profile.dnsServers = mDnsServers.getText().toString().trim();
            profile.routes = mRoutes.getText().toString().trim();
        } else {
            profile.ipsecIdentifier = mIpsecIdentifier.getText().toString();
        }

        if (hasProxy()) {
            String proxyHost = mProxyHost.getText().toString().trim();
            String proxyPort = mProxyPort.getText().toString().trim();
            // 0 is a last resort default, but the interface validates that the proxy port is
            // present and non-zero.
            int port = proxyPort.isEmpty() ? 0 : Integer.parseInt(proxyPort);
            profile.proxy = new ProxyInfo(proxyHost, port, null);
        } else {
            profile.proxy = null;
        }
        // Then, save type-specific fields.
        switch (profile.type) {
            case VpnProfile.TYPE_PPTP:
                profile.mppe = mMppe.isChecked();
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
                profile.l2tpSecret = mL2tpSecret.getText().toString();
                // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_PSK: // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                profile.ipsecIdentifier = mIpsecIdentifier.getText().toString();
                profile.ipsecSecret = mIpsecSecret.getText().toString();
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
                profile.l2tpSecret = mL2tpSecret.getText().toString();
                // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_RSA: // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                if (mIpsecUserCert.getSelectedItemPosition() != 0) {
                    profile.ipsecUserCert = (String) mIpsecUserCert.getSelectedItem();
                }
                // fall through
            case VpnProfile.TYPE_IKEV2_IPSEC_USER_PASS: // fall through
            case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
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
        return Proxy.validate(host, port, "") == Proxy.PROXY_VALID;
    }

}
