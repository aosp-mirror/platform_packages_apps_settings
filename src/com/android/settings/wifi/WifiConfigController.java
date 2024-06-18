/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.content.Context;
import android.content.res.Resources;
import android.net.InetAddresses;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkInfo.DetailedState;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.security.keystore.KeyProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;

import com.android.net.module.util.NetUtils;
import com.android.net.module.util.ProxyUtils;
import com.android.settings.ProxySelector;
import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.utils.AndroidKeystoreAliasLoader;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.wifi.flags.Flags;
import com.android.wifitrackerlib.WifiEntry;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The class for allowing UIs like {@link WifiDialog} and {@link WifiConfigUiBase} to
 * share the logic for controlling buttons, text fields, etc.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in {@link WifiConfigController2}.
 */
public class WifiConfigController implements TextWatcher,
        AdapterView.OnItemSelectedListener, OnCheckedChangeListener,
        TextView.OnEditorActionListener, View.OnKeyListener {
    private static final String TAG = "WifiConfigController";

    private static final String SYSTEM_CA_STORE_PATH = "/system/etc/security/cacerts";

    private final WifiConfigUiBase mConfigUi;
    private final View mView;
    private final AccessPoint mAccessPoint;

    /* This value comes from "wifi_ip_settings" resource array */
    private static final int DHCP = 0;
    private static final int STATIC_IP = 1;

    /* Constants used for referring to the hidden state of a network. */
    public static final int HIDDEN_NETWORK = 1;
    public static final int NOT_HIDDEN_NETWORK = 0;

    /* These values come from "wifi_proxy_settings" resource array */
    public static final int PROXY_NONE = 0;
    public static final int PROXY_STATIC = 1;
    public static final int PROXY_PAC = 2;

    /* These values come from "wifi_eap_method" resource array */
    public static final int WIFI_EAP_METHOD_PEAP = 0;
    public static final int WIFI_EAP_METHOD_TLS  = 1;
    public static final int WIFI_EAP_METHOD_TTLS = 2;
    public static final int WIFI_EAP_METHOD_PWD  = 3;
    public static final int WIFI_EAP_METHOD_SIM  = 4;
    public static final int WIFI_EAP_METHOD_AKA  = 5;
    public static final int WIFI_EAP_METHOD_AKA_PRIME  = 6;

    /* These values come from "wifi_peap_phase2_entries" resource array */
    public static final int WIFI_PEAP_PHASE2_MSCHAPV2   = 0;
    public static final int WIFI_PEAP_PHASE2_GTC        = 1;
    public static final int WIFI_PEAP_PHASE2_SIM        = 2;
    public static final int WIFI_PEAP_PHASE2_AKA        = 3;
    public static final int WIFI_PEAP_PHASE2_AKA_PRIME  = 4;

    /* These values come from "wifi_ttls_phase2_entries" resource array */
    public static final int WIFI_TTLS_PHASE2_PAP       = 0;
    public static final int WIFI_TTLS_PHASE2_MSCHAP    = 1;
    public static final int WIFI_TTLS_PHASE2_MSCHAPV2  = 2;
    public static final int WIFI_TTLS_PHASE2_GTC       = 3;

    private static final String UNDESIRED_CERTIFICATE_MACRANDSECRET = "MacRandSecret";
    private static final String UNDESIRED_CERTIFICATE_MACRANDSAPSECRET = "MacRandSapSecret";
    @VisibleForTesting
    static final String[] UNDESIRED_CERTIFICATES = {
        UNDESIRED_CERTIFICATE_MACRANDSECRET,
        UNDESIRED_CERTIFICATE_MACRANDSAPSECRET
    };

    // Should be the same index value as wifi_privacy_entries in arrays.xml
    @VisibleForTesting static final int PRIVACY_SPINNER_INDEX_RANDOMIZED_MAC = 0;
    @VisibleForTesting static final int PRIVACY_SPINNER_INDEX_DEVICE_MAC = 1;

    /* Phase2 methods supported by PEAP are limited */
    private ArrayAdapter<CharSequence> mPhase2PeapAdapter;
    /* Phase2 methods supported by TTLS are limited */
    private ArrayAdapter<CharSequence> mPhase2TtlsAdapter;

    // e.g. AccessPoint.SECURITY_NONE
    @VisibleForTesting
    int mAccessPointSecurity;
    private TextView mPasswordView;
    private ImageButton mSsidScanButton;

    private String mUnspecifiedCertString;
    private String mMultipleCertSetString;
    private String mUseSystemCertsString;
    private String mDoNotProvideEapUserCertString;

    private Spinner mSecuritySpinner;
    @VisibleForTesting Spinner mEapMethodSpinner;
    @VisibleForTesting Spinner mEapSimSpinner;    // For EAP-SIM, EAP-AKA and EAP-AKA-PRIME.
    private Spinner mEapCaCertSpinner;
    private Spinner mEapOcspSpinner;
    private TextView mEapDomainView;
    private Spinner mPhase2Spinner;
    // Associated with mPhase2Spinner, one of mPhase2TtlsAdapter or mPhase2PeapAdapter
    private ArrayAdapter<CharSequence> mPhase2Adapter;
    private Spinner mEapUserCertSpinner;
    private TextView mEapIdentityView;
    private TextView mEapAnonymousView;

    private Spinner mIpSettingsSpinner;
    private TextView mIpAddressView;
    private TextView mGatewayView;
    private TextView mNetworkPrefixLengthView;
    private TextView mDns1View;
    private TextView mDns2View;

    private Spinner mProxySettingsSpinner;
    private Spinner mMeteredSettingsSpinner;
    private Spinner mHiddenSettingsSpinner;
    private Spinner mPrivacySettingsSpinner;
    private TextView mHiddenWarningView;
    private TextView mProxyHostView;
    private TextView mProxyPortView;
    private TextView mProxyExclusionListView;
    private TextView mProxyPacView;
    private CheckBox mSharedCheckBox;

    private IpAssignment mIpAssignment = IpAssignment.UNASSIGNED;
    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;
    private boolean mRequestFocus = true;

    private String[] mLevels;
    private int mMode;
    private TextView mSsidView;

    private Context mContext;

    @VisibleForTesting
    Integer mSecurityInPosition[];

    private final WifiManager mWifiManager;

    private final List<SubscriptionInfo> mActiveSubscriptionInfos = new ArrayList<>();

    public WifiConfigController(WifiConfigUiBase parent, View view, AccessPoint accessPoint,
            int mode) {
        this (parent, view, accessPoint, mode, true /* requestFocus */);
    }

    public WifiConfigController(WifiConfigUiBase parent, View view, AccessPoint accessPoint,
            int mode, boolean requestFocus) {
        mConfigUi = parent;

        mView = view;
        mAccessPoint = accessPoint;
        mContext = mConfigUi.getContext();
        mRequestFocus = requestFocus;

        // Init Wi-Fi manager
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        initWifiConfigController(accessPoint, mode);
    }

    @VisibleForTesting
    public WifiConfigController(WifiConfigUiBase parent, View view, AccessPoint accessPoint,
            int mode, WifiManager wifiManager) {
        mConfigUi = parent;

        mView = view;
        mAccessPoint = accessPoint;
        mContext = mConfigUi.getContext();
        mWifiManager = wifiManager;
        initWifiConfigController(accessPoint, mode);
    }

    private void initWifiConfigController(AccessPoint accessPoint, int mode) {

        mAccessPointSecurity = (accessPoint == null) ? AccessPoint.SECURITY_NONE :
                accessPoint.getSecurity();
        mMode = mode;

        final Resources res = mContext.getResources();

        mLevels = res.getStringArray(R.array.wifi_signal);
        if (Utils.isWifiOnly(mContext) || !mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_eap_sim_based_auth_supported)) {
            mPhase2PeapAdapter = getSpinnerAdapter(R.array.wifi_peap_phase2_entries);
        } else {
            mPhase2PeapAdapter = getSpinnerAdapterWithEapMethodsTts(
                R.array.wifi_peap_phase2_entries_with_sim_auth);
        }

        mPhase2TtlsAdapter = getSpinnerAdapter(R.array.wifi_ttls_phase2_entries);

        mUnspecifiedCertString = mContext.getString(R.string.wifi_unspecified);
        mMultipleCertSetString = mContext.getString(R.string.wifi_multiple_cert_added);
        mUseSystemCertsString = mContext.getString(R.string.wifi_use_system_certs);
        mDoNotProvideEapUserCertString =
            mContext.getString(R.string.wifi_do_not_provide_eap_user_cert);

        if (Flags.androidVWifiApi() && mAccessPointSecurity == WifiEntry.SECURITY_WEP) {
            LinearLayout wepWarningLayout =
                    (LinearLayout) mView.findViewById(R.id.wep_warning_layout);
            wepWarningLayout.setVisibility(View.VISIBLE);
        }

        mSsidScanButton = (ImageButton) mView.findViewById(R.id.ssid_scanner_button);
        mIpSettingsSpinner = (Spinner) mView.findViewById(R.id.ip_settings);
        mIpSettingsSpinner.setOnItemSelectedListener(this);
        mProxySettingsSpinner = (Spinner) mView.findViewById(R.id.proxy_settings);
        mProxySettingsSpinner.setOnItemSelectedListener(this);
        mSharedCheckBox = (CheckBox) mView.findViewById(R.id.shared);
        mMeteredSettingsSpinner = mView.findViewById(R.id.metered_settings);
        mHiddenSettingsSpinner = mView.findViewById(R.id.hidden_settings);
        mPrivacySettingsSpinner = mView.findViewById(R.id.privacy_settings);
        if (mWifiManager.isConnectedMacRandomizationSupported()) {
            View privacySettingsLayout = mView.findViewById(R.id.privacy_settings_fields);
            privacySettingsLayout.setVisibility(View.VISIBLE);
        }
        mHiddenSettingsSpinner.setOnItemSelectedListener(this);
        mHiddenWarningView = mView.findViewById(R.id.hidden_settings_warning);
        mHiddenWarningView.setVisibility(
                mHiddenSettingsSpinner.getSelectedItemPosition() == NOT_HIDDEN_NETWORK
                        ? View.GONE
                        : View.VISIBLE);
        mSecurityInPosition = new Integer[AccessPoint.SECURITY_MAX_VAL];

        if (mAccessPoint == null) { // new network
            configureSecuritySpinner();
            mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
        } else {
            mConfigUi.setTitle(mAccessPoint.getTitle());

            ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);

            boolean showAdvancedFields = false;
            if (mAccessPoint.isSaved()) {
                WifiConfiguration config = mAccessPoint.getConfig();
                mMeteredSettingsSpinner.setSelection(config.meteredOverride);
                mHiddenSettingsSpinner.setSelection(config.hiddenSSID
                        ? HIDDEN_NETWORK
                        : NOT_HIDDEN_NETWORK);

                mPrivacySettingsSpinner.setSelection(
                        config.macRandomizationSetting == WifiConfiguration.RANDOMIZATION_PERSISTENT
                        ? PRIVACY_SPINNER_INDEX_RANDOMIZED_MAC : PRIVACY_SPINNER_INDEX_DEVICE_MAC);

                if (config.getIpConfiguration().getIpAssignment() == IpAssignment.STATIC) {
                    mIpSettingsSpinner.setSelection(STATIC_IP);
                    showAdvancedFields = true;
                    // Display IP address.
                    StaticIpConfiguration staticConfig = config.getIpConfiguration()
                            .getStaticIpConfiguration();
                    if (staticConfig != null && staticConfig.getIpAddress() != null) {
                        addRow(group, R.string.wifi_ip_address,
                                staticConfig.getIpAddress().getAddress().getHostAddress());
                    }
                } else {
                    mIpSettingsSpinner.setSelection(DHCP);
                }

                mSharedCheckBox.setEnabled(config.shared);
                if (!config.shared) {
                    showAdvancedFields = true;
                }

                ProxySettings proxySettings = config.getIpConfiguration().getProxySettings();
                if (proxySettings == ProxySettings.STATIC) {
                    mProxySettingsSpinner.setSelection(PROXY_STATIC);
                    showAdvancedFields = true;
                } else if (proxySettings == ProxySettings.PAC) {
                    mProxySettingsSpinner.setSelection(PROXY_PAC);
                    showAdvancedFields = true;
                } else {
                    mProxySettingsSpinner.setSelection(PROXY_NONE);
                }
                if (config != null && config.isPasspoint()) {
                    addRow(group, R.string.passpoint_label,
                            String.format(mContext.getString(R.string.passpoint_content),
                            config.providerFriendlyName));
                }
            }

            if ((!mAccessPoint.isSaved() && !mAccessPoint.isActive()
                    && !mAccessPoint.isPasspointConfig())
                    || mMode != WifiConfigUiBase.MODE_VIEW) {
                showSecurityFields(/* refreshEapMethods */ true, /* refreshCertificates */ true);
                showIpConfigFields();
                showProxyFields();
                final CheckBox advancedTogglebox =
                        (CheckBox) mView.findViewById(R.id.wifi_advanced_togglebox);
                if (!showAdvancedFields) {
                    // Need to show Advanced Option button.
                    mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
                    advancedTogglebox.setOnCheckedChangeListener(this);
                    advancedTogglebox.setChecked(showAdvancedFields);
                    setAdvancedOptionAccessibilityString();
                }
                mView.findViewById(R.id.wifi_advanced_fields)
                        .setVisibility(showAdvancedFields ? View.VISIBLE : View.GONE);
            }

            if (mMode == WifiConfigUiBase.MODE_MODIFY) {
                mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            } else if (mMode == WifiConfigUiBase.MODE_CONNECT) {
                mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
            } else {
                final DetailedState state = mAccessPoint.getDetailedState();
                final String signalLevel = getSignalString();

                if ((state == null || state == DetailedState.DISCONNECTED) && signalLevel != null) {
                    mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
                } else {
                    if (state != null) {
                        boolean isEphemeral = mAccessPoint.isEphemeral();
                        WifiConfiguration config = mAccessPoint.getConfig();
                        String providerFriendlyName = null;
                        if (config != null && config.isPasspoint()) {
                            providerFriendlyName = config.providerFriendlyName;
                        }
                        String suggestionOrSpecifierPackageName = null;
                        if (config != null
                                && (config.fromWifiNetworkSpecifier
                                || config.fromWifiNetworkSuggestion)) {
                            suggestionOrSpecifierPackageName = config.creatorName;
                        }
                        String summary = AccessPoint.getSummary(
                                mConfigUi.getContext(), /* ssid */ null, state, isEphemeral,
                                suggestionOrSpecifierPackageName);
                        addRow(group, R.string.wifi_status, summary);
                    }

                    if (signalLevel != null) {
                        addRow(group, R.string.wifi_signal, signalLevel);
                    }

                    WifiInfo info = mAccessPoint.getInfo();
                    if (info != null && info.getTxLinkSpeedMbps() != WifiInfo.LINK_SPEED_UNKNOWN) {
                        addRow(group, R.string.tx_wifi_speed, String.format(
                                res.getString(R.string.tx_link_speed), info.getTxLinkSpeedMbps()));
                    }

                    if (info != null && info.getRxLinkSpeedMbps() != WifiInfo.LINK_SPEED_UNKNOWN) {
                        addRow(group, R.string.rx_wifi_speed, String.format(
                                res.getString(R.string.rx_link_speed), info.getRxLinkSpeedMbps()));
                    }

                    if (info != null && info.getFrequency() != -1) {
                        final int frequency = info.getFrequency();
                        String band = null;

                        if (frequency >= AccessPoint.LOWER_FREQ_24GHZ
                                && frequency < AccessPoint.HIGHER_FREQ_24GHZ) {
                            band = res.getString(R.string.wifi_band_24ghz);
                        } else if (frequency >= AccessPoint.LOWER_FREQ_5GHZ
                                && frequency < AccessPoint.HIGHER_FREQ_5GHZ) {
                            band = res.getString(R.string.wifi_band_5ghz);
                        } else {
                            Log.e(TAG, "Unexpected frequency " + frequency);
                        }
                        if (band != null) {
                            addRow(group, R.string.wifi_frequency, band);
                        }
                    }

                    addRow(group, R.string.wifi_security, mAccessPoint.getSecurityString(false));
                    mView.findViewById(R.id.ip_fields).setVisibility(View.GONE);
                }
                if (mAccessPoint.isSaved() || mAccessPoint.isActive()
                        || mAccessPoint.isPasspointConfig()) {
                    mConfigUi.setForgetButton(res.getString(R.string.wifi_forget));
                }
            }

            mSsidScanButton.setVisibility(View.GONE);
        }
        mSharedCheckBox.setVisibility(View.GONE);

        mConfigUi.setCancelButton(res.getString(R.string.wifi_cancel));
        if (mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }

        // After done view show and hide, request focus from parameter.
        if (mRequestFocus) {
            mView.findViewById(R.id.l_wifidialog).requestFocus();
        }
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    @VisibleForTesting
    String getSignalString() {
        if (!mAccessPoint.isReachable()) {
            return null;
        }
        final int level = mAccessPoint.getLevel();

        return (level > -1 && level < mLevels.length) ? mLevels[level] : null;
    }

    void hideForgetButton() {
        Button forget = mConfigUi.getForgetButton();
        if (forget == null) return;

        forget.setVisibility(View.GONE);
    }

    void hideSubmitButton() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        submit.setVisibility(View.GONE);
    }

    /* show submit button if password, ip and proxy settings are valid */
    void enableSubmitIfAppropriate() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        submit.setEnabled(isSubmittable());
    }

    boolean isValidPsk(String password) {
        if (password.length() == 64 && password.matches("[0-9A-Fa-f]{64}")) {
            return true;
        } else if (password.length() >= 8 && password.length() <= 63) {
            return true;
        }
        return false;
    }

    boolean isValidSaePassword(String password) {
        if (password.length() >= 1 && password.length() <= 63) {
            return true;
        }
        return false;
    }

    boolean isSubmittable() {
        boolean enabled = false;
        boolean passwordInvalid = false;
        if (mPasswordView != null
                && ((mAccessPointSecurity == AccessPoint.SECURITY_WEP
                        && mPasswordView.length() == 0)
                    || (mAccessPointSecurity == AccessPoint.SECURITY_PSK
                           && !isValidPsk(mPasswordView.getText().toString()))
                    || (mAccessPointSecurity == AccessPoint.SECURITY_SAE
                        && !isValidSaePassword(mPasswordView.getText().toString())))) {
            passwordInvalid = true;
        }
        if ((mSsidView != null && mSsidView.length() == 0)
                // If Accesspoint is not saved, apply passwordInvalid check
                || ((mAccessPoint == null || !mAccessPoint.isSaved()) && passwordInvalid
                // If AccessPoint is saved (modifying network) and password is changed, apply
                // Invalid password check
                || mAccessPoint != null && mAccessPoint.isSaved() && passwordInvalid
                    && mPasswordView.length() > 0)) {
            enabled = false;
        } else {
            enabled = ipAndProxyFieldsAreValid();
        }
        if ((mAccessPointSecurity == AccessPoint.SECURITY_EAP
                || mAccessPointSecurity == AccessPoint.SECURITY_EAP_WPA3_ENTERPRISE
                || mAccessPointSecurity == AccessPoint.SECURITY_EAP_SUITE_B)
                && mEapCaCertSpinner != null
                && mView.findViewById(R.id.l_ca_cert).getVisibility() != View.GONE) {
            String caCertSelection = (String) mEapCaCertSpinner.getSelectedItem();
            if (caCertSelection.equals(mUnspecifiedCertString)) {
                // Disallow submit if the user has not selected a CA certificate for an EAP network
                // configuration.
                enabled = false;
            } else if (mEapDomainView != null
                    && mView.findViewById(R.id.l_domain).getVisibility() != View.GONE
                    && TextUtils.isEmpty(mEapDomainView.getText().toString())) {
                // Disallow submit if the user chooses to use a certificate for EAP server
                // validation, but does not provide a domain.
                enabled = false;
            }
        }
        if ((mAccessPointSecurity == AccessPoint.SECURITY_EAP
                || mAccessPointSecurity == AccessPoint.SECURITY_EAP_WPA3_ENTERPRISE
                || mAccessPointSecurity == AccessPoint.SECURITY_EAP_SUITE_B)
                && mEapUserCertSpinner != null
                && mView.findViewById(R.id.l_user_cert).getVisibility() != View.GONE
                && mEapUserCertSpinner.getSelectedItem().equals(mUnspecifiedCertString)) {
            // Disallow submit if the user has not selected a user certificate for an EAP network
            // configuration.
            enabled = false;
        }
        return enabled;
    }

    void showWarningMessagesIfAppropriate() {
        mView.findViewById(R.id.no_user_cert_warning).setVisibility(View.GONE);
        mView.findViewById(R.id.no_domain_warning).setVisibility(View.GONE);
        mView.findViewById(R.id.ssid_too_long_warning).setVisibility(View.GONE);

        if (mSsidView != null) {
            final String ssid = mSsidView.getText().toString();
            if (WifiUtils.isSSIDTooLong(ssid)) {
                mView.findViewById(R.id.ssid_too_long_warning).setVisibility(View.VISIBLE);
            }
        }
        if (mEapCaCertSpinner != null
                && mView.findViewById(R.id.l_ca_cert).getVisibility() != View.GONE) {
            if (mEapDomainView != null
                    && mView.findViewById(R.id.l_domain).getVisibility() != View.GONE
                    && TextUtils.isEmpty(mEapDomainView.getText().toString())) {
                // Display warning if user chooses to use a certificate without restricting the
                // server domain that these certificates can be used to validate.
                mView.findViewById(R.id.no_domain_warning).setVisibility(View.VISIBLE);
            }
        }

        if (mAccessPointSecurity == AccessPoint.SECURITY_EAP_SUITE_B &&
                mEapMethodSpinner.getSelectedItemPosition() == WIFI_EAP_METHOD_TLS) {
            String userCertSelection = (String) mEapUserCertSpinner.getSelectedItem();
            if (userCertSelection.equals(mUnspecifiedCertString)) {
                mView.findViewById(R.id.no_user_cert_warning).setVisibility(View.VISIBLE);
            }
        }
    }

    public WifiConfiguration getConfig() {
        if (mMode == WifiConfigUiBase.MODE_VIEW) {
            return null;
        }

        WifiConfiguration config = new WifiConfiguration();

        if (mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mSsidView.getText().toString());
            // If the user adds a network manually, assume that it is hidden.
            config.hiddenSSID = mHiddenSettingsSpinner.getSelectedItemPosition() == HIDDEN_NETWORK;
        } else if (!mAccessPoint.isSaved()) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mAccessPoint.getSsidStr());
        } else {
            config.networkId = mAccessPoint.getConfig().networkId;
            config.hiddenSSID = mAccessPoint.getConfig().hiddenSSID;
        }

        config.shared = mSharedCheckBox.isChecked();

        switch (mAccessPointSecurity) {
            case AccessPoint.SECURITY_NONE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
                break;

            case AccessPoint.SECURITY_WEP:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_WEP);
                if (mPasswordView.length() != 0) {
                    int length = mPasswordView.length();
                    String password = mPasswordView.getText().toString();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58)
                            && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_PSK:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_PSK);
                if (mPasswordView.length() != 0) {
                    String password = mPasswordView.getText().toString();
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_EAP:
            case AccessPoint.SECURITY_EAP_WPA3_ENTERPRISE:
            case AccessPoint.SECURITY_EAP_SUITE_B:
                if (mEapMethodSpinner == null || mPhase2Spinner == null) {
                    break;
                }
                if (mAccessPointSecurity == AccessPoint.SECURITY_EAP_SUITE_B) {
                    // allowedSuiteBCiphers will be set according to certificate type
                    config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP_SUITE_B);
                } else if (mAccessPointSecurity == AccessPoint.SECURITY_EAP_WPA3_ENTERPRISE) {
                    config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
                } else {
                    config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP);
                }
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = mEapMethodSpinner.getSelectedItemPosition();
                int phase2Method = mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        // PEAP supports limited phase2 values
                        // Map the index from the mPhase2PeapAdapter to the one used
                        // by the API which has the full list of PEAP methods.
                        switch(phase2Method) {
                            case WIFI_PEAP_PHASE2_MSCHAPV2:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAPV2);
                                break;
                            case WIFI_PEAP_PHASE2_GTC:
                                config.enterpriseConfig.setPhase2Method(Phase2.GTC);
                                break;
                            case WIFI_PEAP_PHASE2_SIM:
                                config.enterpriseConfig.setPhase2Method(Phase2.SIM);
                                break;
                            case WIFI_PEAP_PHASE2_AKA:
                                config.enterpriseConfig.setPhase2Method(Phase2.AKA);
                                break;
                            case WIFI_PEAP_PHASE2_AKA_PRIME:
                                config.enterpriseConfig.setPhase2Method(Phase2.AKA_PRIME);
                                break;
                            default:
                                Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    case Eap.TTLS:
                        // The default index from mPhase2TtlsAdapter maps to the API
                        switch(phase2Method) {
                            case WIFI_TTLS_PHASE2_PAP:
                                config.enterpriseConfig.setPhase2Method(Phase2.PAP);
                                break;
                            case WIFI_TTLS_PHASE2_MSCHAP:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAP);
                                break;
                            case WIFI_TTLS_PHASE2_MSCHAPV2:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAPV2);
                                break;
                            case WIFI_TTLS_PHASE2_GTC:
                                config.enterpriseConfig.setPhase2Method(Phase2.GTC);
                                break;
                            default:
                                Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    default:
                        break;
                }

                String caCert = (String) mEapCaCertSpinner.getSelectedItem();
                config.enterpriseConfig.setCaCertificateAliases(null);
                config.enterpriseConfig.setCaPath(null);
                config.enterpriseConfig.setDomainSuffixMatch(mEapDomainView.getText().toString());
                if (caCert.equals(mUnspecifiedCertString)) {
                    // ca_cert already set to null, so do nothing.
                } else if (caCert.equals(mUseSystemCertsString)) {
                    config.enterpriseConfig.setCaPath(SYSTEM_CA_STORE_PATH);
                } else if (caCert.equals(mMultipleCertSetString)) {
                    if (mAccessPoint != null) {
                        if (!mAccessPoint.isSaved()) {
                            Log.e(TAG, "Multiple certs can only be set "
                                    + "when editing saved network");
                        }
                        config.enterpriseConfig.setCaCertificateAliases(
                                mAccessPoint
                                        .getConfig()
                                        .enterpriseConfig
                                        .getCaCertificateAliases());
                    }
                } else {
                    config.enterpriseConfig.setCaCertificateAliases(new String[] {caCert});
                }

                // ca_cert or ca_path should not both be non-null, since we only intend to let
                // the use either their own certificate, or the system certificates, not both.
                // The variable that is not used must explicitly be set to null, so that a
                // previously-set value on a saved configuration will be erased on an update.
                if (config.enterpriseConfig.getCaCertificateAliases() != null
                        && config.enterpriseConfig.getCaPath() != null) {
                    Log.e(TAG, "ca_cert ("
                            + Arrays.toString(config.enterpriseConfig.getCaCertificateAliases())
                            + ") and ca_path ("
                            + config.enterpriseConfig.getCaPath()
                            + ") should not both be non-null");
                }

                // Only set OCSP option if there is a valid CA certificate.
                if (caCert.equals(mUnspecifiedCertString)) {
                    config.enterpriseConfig.setOcsp(WifiEnterpriseConfig.OCSP_NONE);
                } else {
                    config.enterpriseConfig.setOcsp(mEapOcspSpinner.getSelectedItemPosition());
                }

                String clientCert = (String) mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(mUnspecifiedCertString)
                        || clientCert.equals(mDoNotProvideEapUserCertString)) {
                    // Note: |clientCert| should not be able to take the value |unspecifiedCert|,
                    // since we prevent such configurations from being saved.
                    clientCert = "";
                }
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                if (eapMethod == Eap.SIM || eapMethod == Eap.AKA || eapMethod == Eap.AKA_PRIME) {
                    config.enterpriseConfig.setIdentity("");
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else if (eapMethod == Eap.PWD) {
                    config.enterpriseConfig.setIdentity(mEapIdentityView.getText().toString());
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else {
                    config.enterpriseConfig.setIdentity(mEapIdentityView.getText().toString());
                    config.enterpriseConfig.setAnonymousIdentity(
                            mEapAnonymousView.getText().toString());
                }

                if (mPasswordView.isShown()) {
                    // For security reasons, a previous password is not displayed to user.
                    // Update only if it has been changed.
                    if (mPasswordView.length() > 0) {
                        config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                    }
                } else {
                    // clear password
                    config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                }
                break;
            case AccessPoint.SECURITY_SAE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
                if (mPasswordView.length() != 0) {
                    String password = mPasswordView.getText().toString();
                    config.preSharedKey = '"' + password + '"';
                }
                break;

            case AccessPoint.SECURITY_OWE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OWE);
                break;

            default:
                return null;
        }

        if (config.enterpriseConfig.isAuthenticationSimBased()
                && mActiveSubscriptionInfos.size() > 0) {
            config.carrierId = mActiveSubscriptionInfos
                    .get(mEapSimSpinner.getSelectedItemPosition()).getCarrierId();
        }

        final IpConfiguration ipConfig = new IpConfiguration();
        ipConfig.setIpAssignment(mIpAssignment);
        ipConfig.setProxySettings(mProxySettings);
        ipConfig.setStaticIpConfiguration(mStaticIpConfiguration);
        ipConfig.setHttpProxy(mHttpProxy);
        config.setIpConfiguration(ipConfig);
        if (mMeteredSettingsSpinner != null) {
            config.meteredOverride = mMeteredSettingsSpinner.getSelectedItemPosition();
        }

        if (mPrivacySettingsSpinner != null) {
            config.macRandomizationSetting = mPrivacySettingsSpinner.getSelectedItemPosition()
                    == PRIVACY_SPINNER_INDEX_RANDOMIZED_MAC
                    ? WifiConfiguration.RANDOMIZATION_PERSISTENT
                    : WifiConfiguration.RANDOMIZATION_NONE;
        }

        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        mIpAssignment =
                (mIpSettingsSpinner != null
                    && mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP)
                ? IpAssignment.STATIC
                : IpAssignment.DHCP;

        if (mIpAssignment == IpAssignment.STATIC) {
            mStaticIpConfiguration = new StaticIpConfiguration();
            int result = validateIpConfigFields(mStaticIpConfiguration);
            if (result != 0) {
                return false;
            }
        }

        final int selectedPosition = mProxySettingsSpinner.getSelectedItemPosition();
        mProxySettings = ProxySettings.NONE;
        mHttpProxy = null;
        if (selectedPosition == PROXY_STATIC && mProxyHostView != null) {
            mProxySettings = ProxySettings.STATIC;
            String host = mProxyHostView.getText().toString();
            String portStr = mProxyPortView.getText().toString();
            String exclusionList = mProxyExclusionListView.getText().toString();
            int port = 0;
            int result = 0;
            try {
                port = Integer.parseInt(portStr);
                result = ProxySelector.validate(host, portStr, exclusionList);
            } catch (NumberFormatException e) {
                result = R.string.proxy_error_invalid_port;
            }
            if (result == 0) {
                mHttpProxy = ProxyInfo.buildDirectProxy(
                        host, port, Arrays.asList(exclusionList.split(",")));
            } else {
                return false;
            }
        } else if (selectedPosition == PROXY_PAC && mProxyPacView != null) {
            mProxySettings = ProxySettings.PAC;
            CharSequence uriSequence = mProxyPacView.getText();
            if (TextUtils.isEmpty(uriSequence)) {
                return false;
            }
            Uri uri = Uri.parse(uriSequence.toString());
            if (uri == null) {
                return false;
            }
            mHttpProxy = ProxyInfo.buildPacProxy(uri);
        }
        return true;
    }

    private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) InetAddresses.parseNumericAddress(text);
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        if (mIpAddressView == null) return 0;

        String ipAddr = mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr)) return R.string.wifi_ip_settings_invalid_ip_address;

        Inet4Address inetAddr = getIPv4Address(ipAddr);
        if (inetAddr == null || inetAddr.equals(Inet4Address.ANY)) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        // Copy all fields into the builder first and set desired value later with builder.
        final StaticIpConfiguration.Builder staticIPBuilder = new StaticIpConfiguration.Builder()
                .setDnsServers(staticIpConfiguration.getDnsServers())
                .setDomains(staticIpConfiguration.getDomains())
                .setGateway(staticIpConfiguration.getGateway())
                .setIpAddress(staticIpConfiguration.getIpAddress());
        try {
            int networkPrefixLength = -1;
            try {
                networkPrefixLength = Integer.parseInt(
                        mNetworkPrefixLengthView.getText().toString());
                if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                    return R.string.wifi_ip_settings_invalid_network_prefix_length;
                }
                staticIPBuilder.setIpAddress(new LinkAddress(inetAddr, networkPrefixLength));
            } catch (NumberFormatException e) {
                // Set the hint as default after user types in ip address
                mNetworkPrefixLengthView.setText(mConfigUi.getContext().getString(
                        com.android.settingslib.R.string.wifi_network_prefix_length_hint));
            } catch (IllegalArgumentException e) {
                return R.string.wifi_ip_settings_invalid_ip_address;
            }

            String gateway = mGatewayView.getText().toString();
            if (TextUtils.isEmpty(gateway)) {
                try {
                    //Extract a default gateway from IP address
                    InetAddress netPart = NetUtils.getNetworkPart(inetAddr, networkPrefixLength);
                    byte[] addr = netPart.getAddress();
                    addr[addr.length - 1] = 1;
                    mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
                } catch (RuntimeException ee) {
                } catch (java.net.UnknownHostException u) {
                }
            } else {
                InetAddress gatewayAddr = getIPv4Address(gateway);
                if (gatewayAddr == null) {
                    return R.string.wifi_ip_settings_invalid_gateway;
                }
                if (gatewayAddr.isMulticastAddress()) {
                    return R.string.wifi_ip_settings_invalid_gateway;
                }
                staticIPBuilder.setGateway(gatewayAddr);
            }

            String dns = mDns1View.getText().toString();
            InetAddress dnsAddr = null;
            final ArrayList<InetAddress> dnsServers = new ArrayList<>();

            if (TextUtils.isEmpty(dns)) {
                //If everything else is valid, provide hint as a default option
                mDns1View.setText(mConfigUi.getContext().getString(
                        com.android.settingslib.R.string.wifi_dns1_hint));
            } else {
                dnsAddr = getIPv4Address(dns);
                if (dnsAddr == null) {
                    return R.string.wifi_ip_settings_invalid_dns;
                }
                dnsServers.add(dnsAddr);
            }

            if (mDns2View.length() > 0) {
                dns = mDns2View.getText().toString();
                dnsAddr = getIPv4Address(dns);
                if (dnsAddr == null) {
                    return R.string.wifi_ip_settings_invalid_dns;
                }
                dnsServers.add(dnsAddr);
            }
            staticIPBuilder.setDnsServers(dnsServers);
            return 0;
        } finally {
            // Caller of this method may rely on staticIpConfiguration, so build the final result
            // at the end of the method.
            staticIpConfiguration = staticIPBuilder.build();
        }
    }

    private void showSecurityFields(boolean refreshEapMethods, boolean refreshCertificates) {
        if (mAccessPointSecurity == AccessPoint.SECURITY_NONE ||
                mAccessPointSecurity == AccessPoint.SECURITY_OWE) {
            mView.findViewById(R.id.security_fields).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.security_fields).setVisibility(View.VISIBLE);

        if (mPasswordView == null) {
            mPasswordView = (TextView) mView.findViewById(R.id.password);
            mPasswordView.addTextChangedListener(this);
            mPasswordView.setOnEditorActionListener(this);
            mPasswordView.setOnKeyListener(this);
            ((CheckBox) mView.findViewById(R.id.show_password))
                .setOnCheckedChangeListener(this);

            if (mAccessPoint != null && mAccessPoint.isSaved()) {
                mPasswordView.setHint(R.string.wifi_unchanged);
            }
        }

        if (mAccessPointSecurity != AccessPoint.SECURITY_EAP &&
                mAccessPointSecurity != AccessPoint.SECURITY_EAP_SUITE_B) {
            mView.findViewById(R.id.eap).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.eap).setVisibility(View.VISIBLE);

        // TODO (b/140541213): Maybe we can remove initiateEnterpriseNetworkUi by moving code block
        boolean initiateEnterpriseNetworkUi = false;
        if (mEapMethodSpinner == null) {
            initiateEnterpriseNetworkUi = true;
            mEapMethodSpinner = (Spinner) mView.findViewById(R.id.method);
            mEapMethodSpinner.setOnItemSelectedListener(this);
            mEapSimSpinner = (Spinner) mView.findViewById(R.id.sim);
            mPhase2Spinner = (Spinner) mView.findViewById(R.id.phase2);
            mPhase2Spinner.setOnItemSelectedListener(this);
            mEapCaCertSpinner = (Spinner) mView.findViewById(R.id.ca_cert);
            mEapCaCertSpinner.setOnItemSelectedListener(this);
            mEapOcspSpinner = (Spinner) mView.findViewById(R.id.ocsp);
            mEapDomainView = (TextView) mView.findViewById(R.id.domain);
            mEapDomainView.addTextChangedListener(this);
            mEapUserCertSpinner = (Spinner) mView.findViewById(R.id.user_cert);
            mEapUserCertSpinner.setOnItemSelectedListener(this);
            mEapIdentityView = (TextView) mView.findViewById(R.id.identity);
            mEapAnonymousView = (TextView) mView.findViewById(R.id.anonymous);

            setAccessibilityDelegateForSecuritySpinners();
        }

        if (refreshEapMethods) {
            ArrayAdapter<CharSequence> eapMethodSpinnerAdapter;
            if (mAccessPointSecurity == AccessPoint.SECURITY_EAP_SUITE_B) {
                eapMethodSpinnerAdapter = getSpinnerAdapter(R.array.wifi_eap_method);
                mEapMethodSpinner.setAdapter(eapMethodSpinnerAdapter);
                // WAP3-Enterprise 192-bit only allows EAP method TLS
                mEapMethodSpinner.setSelection(Eap.TLS);
                mEapMethodSpinner.setEnabled(false);
            } else if (Utils.isWifiOnly(mContext) || !mContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_eap_sim_based_auth_supported)) {
                eapMethodSpinnerAdapter = getSpinnerAdapter(R.array.eap_method_without_sim_auth);
                mEapMethodSpinner.setAdapter(eapMethodSpinnerAdapter);
                mEapMethodSpinner.setEnabled(true);
            } else {
                eapMethodSpinnerAdapter = getSpinnerAdapterWithEapMethodsTts(R.array.wifi_eap_method);
                mEapMethodSpinner.setAdapter(eapMethodSpinnerAdapter);
                mEapMethodSpinner.setEnabled(true);
            }
        }

        if (refreshCertificates) {
            loadSims();

            final AndroidKeystoreAliasLoader androidKeystoreAliasLoader =
                    getAndroidKeystoreAliasLoader();
            loadCertificates(
                    mEapCaCertSpinner,
                    androidKeystoreAliasLoader.getCaCertAliases(),
                    null /* noCertificateString */,
                    false /* showMultipleCerts */,
                    true /* showUsePreinstalledCertOption */);
            loadCertificates(
                    mEapUserCertSpinner,
                    androidKeystoreAliasLoader.getKeyCertAliases(),
                    mDoNotProvideEapUserCertString,
                    false /* showMultipleCerts */,
                    false /* showUsePreinstalledCertOption */);
            // To avoid the user connects to a non-secure network unexpectedly,
            // request using system trusted certificates by default
            // unless the user explicitly chooses "Do not validate" or other
            // CA certificates.
            setSelection(mEapCaCertSpinner, mUseSystemCertsString);
        }

        // Modifying an existing network
        if (initiateEnterpriseNetworkUi && mAccessPoint != null && mAccessPoint.isSaved()) {
            final WifiConfiguration wifiConfig = mAccessPoint.getConfig();
            final WifiEnterpriseConfig enterpriseConfig = wifiConfig.enterpriseConfig;
            final int eapMethod = enterpriseConfig.getEapMethod();
            final int phase2Method = enterpriseConfig.getPhase2Method();
            mEapMethodSpinner.setSelection(eapMethod);
            showEapFieldsByMethod(eapMethod);
            switch (eapMethod) {
                case Eap.PEAP:
                    switch (phase2Method) {
                        case Phase2.MSCHAPV2:
                            mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_MSCHAPV2);
                            break;
                        case Phase2.GTC:
                            mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_GTC);
                            break;
                        case Phase2.SIM:
                            mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_SIM);
                            break;
                        case Phase2.AKA:
                            mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_AKA);
                            break;
                        case Phase2.AKA_PRIME:
                            mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_AKA_PRIME);
                            break;
                        default:
                            Log.e(TAG, "Invalid phase 2 method " + phase2Method);
                            break;
                    }
                    break;
                case Eap.TTLS:
                    switch (phase2Method) {
                        case Phase2.PAP:
                            mPhase2Spinner.setSelection(WIFI_TTLS_PHASE2_PAP);
                            break;
                        case Phase2.MSCHAP:
                            mPhase2Spinner.setSelection(WIFI_TTLS_PHASE2_MSCHAP);
                            break;
                        case Phase2.MSCHAPV2:
                            mPhase2Spinner.setSelection(WIFI_TTLS_PHASE2_MSCHAPV2);
                            break;
                        case Phase2.GTC:
                            mPhase2Spinner.setSelection(WIFI_TTLS_PHASE2_GTC);
                            break;
                        default:
                            Log.e(TAG, "Invalid phase 2 method " + phase2Method);
                            break;
                    }
                    break;
                default:
                    break;
            }

            if (enterpriseConfig.isAuthenticationSimBased()) {
                for (int i = 0; i < mActiveSubscriptionInfos.size(); i++) {
                    if (wifiConfig.carrierId == mActiveSubscriptionInfos.get(i).getCarrierId()) {
                        mEapSimSpinner.setSelection(i);
                        break;
                    }
                }
            }

            if (!TextUtils.isEmpty(enterpriseConfig.getCaPath())) {
                setSelection(mEapCaCertSpinner, mUseSystemCertsString);
            } else {
                String[] caCerts = enterpriseConfig.getCaCertificateAliases();
                if (caCerts == null) {
                    setSelection(mEapCaCertSpinner, mUnspecifiedCertString);
                } else if (caCerts.length == 1) {
                    setSelection(mEapCaCertSpinner, caCerts[0]);
                } else {
                    final AndroidKeystoreAliasLoader androidKeystoreAliasLoader =
                            getAndroidKeystoreAliasLoader();

                    // Reload the cert spinner with an extra "multiple certificates added" item.
                    loadCertificates(
                            mEapCaCertSpinner,
                            androidKeystoreAliasLoader.getCaCertAliases(),
                            null /* noCertificateString */,
                            true /* showMultipleCerts */,
                            true /* showUsePreinstalledCertOption */);
                    setSelection(mEapCaCertSpinner, mMultipleCertSetString);
                }
            }
            mEapOcspSpinner.setSelection(enterpriseConfig.getOcsp());
            mEapDomainView.setText(enterpriseConfig.getDomainSuffixMatch());
            String userCert = enterpriseConfig.getClientCertificateAlias();
            if (TextUtils.isEmpty(userCert)) {
                setSelection(mEapUserCertSpinner, mDoNotProvideEapUserCertString);
            } else {
                setSelection(mEapUserCertSpinner, userCert);
            }
            mEapIdentityView.setText(enterpriseConfig.getIdentity());
            mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
        } else {
            showEapFieldsByMethod(mEapMethodSpinner.getSelectedItemPosition());
        }
    }

    private void setAccessibilityDelegateForSecuritySpinners() {
        final AccessibilityDelegate selectedEventBlocker = new AccessibilityDelegate() {
            @Override
            public void sendAccessibilityEvent(View host, int eventType) {
                if (eventType == AccessibilityEvent.TYPE_VIEW_SELECTED) {
                    // Ignore TYPE_VIEW_SELECTED or there will be multiple Spinner selected
                    // information for WifiController#showSecurityFields.
                    return;
                }
                super.sendAccessibilityEvent(host, eventType);
            }
        };

        mEapMethodSpinner.setAccessibilityDelegate(selectedEventBlocker);
        mPhase2Spinner.setAccessibilityDelegate(selectedEventBlocker);
        mEapCaCertSpinner.setAccessibilityDelegate(selectedEventBlocker);
        mEapOcspSpinner.setAccessibilityDelegate(selectedEventBlocker);
        mEapUserCertSpinner.setAccessibilityDelegate(selectedEventBlocker);
    }

    /**
     * EAP-PWD valid fields include
     *   identity
     *   password
     * EAP-PEAP valid fields include
     *   phase2: MSCHAPV2, GTC, SIM, AKA, AKA'
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password (not required for SIM, AKA, AKA')
     * EAP-TLS valid fields include
     *   user_cert
     *   ca_cert
     *   domain
     *   identity
     * EAP-TTLS valid fields include
     *   phase2: PAP, MSCHAP, MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     */
    private void showEapFieldsByMethod(int eapMethod) {
        // Common defaults
        mView.findViewById(R.id.l_method).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_identity).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_domain).setVisibility(View.VISIBLE);

        // Defaults for most of the EAP methods and over-riden by
        // by certain EAP methods
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_ocsp).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.password_layout).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_sim).setVisibility(View.VISIBLE);

        Context context = mConfigUi.getContext();
        switch (eapMethod) {
            case WIFI_EAP_METHOD_PWD:
                setPhase2Invisible();
                setCaCertInvisible();
                setOcspInvisible();
                setDomainInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                mView.findViewById(R.id.l_sim).setVisibility(View.GONE);
                break;
            case WIFI_EAP_METHOD_TLS:
                mView.findViewById(R.id.l_user_cert).setVisibility(View.VISIBLE);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                mView.findViewById(R.id.l_sim).setVisibility(View.GONE);
                break;
            case WIFI_EAP_METHOD_PEAP:
                // Reset adapter if needed
                if (mPhase2Adapter != mPhase2PeapAdapter) {
                    mPhase2Adapter = mPhase2PeapAdapter;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                showPeapFields();
                setUserCertInvisible();
                break;
            case WIFI_EAP_METHOD_TTLS:
                // Reset adapter if needed
                if (mPhase2Adapter != mPhase2TtlsAdapter) {
                    mPhase2Adapter = mPhase2TtlsAdapter;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                setUserCertInvisible();
                mView.findViewById(R.id.l_sim).setVisibility(View.GONE);
                break;
            case WIFI_EAP_METHOD_SIM:
            case WIFI_EAP_METHOD_AKA:
            case WIFI_EAP_METHOD_AKA_PRIME:
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setCaCertInvisible();
                setOcspInvisible();
                setDomainInvisible();
                setUserCertInvisible();
                setPasswordInvisible();
                setIdentityInvisible();
                break;
        }

        if (mView.findViewById(R.id.l_ca_cert).getVisibility() != View.GONE) {
            String eapCertSelection = (String) mEapCaCertSpinner.getSelectedItem();
            if (eapCertSelection.equals(mUnspecifiedCertString)) {
                // Domain suffix matching is not relevant if the user hasn't chosen a CA
                // certificate yet, or chooses not to validate the EAP server.
                setDomainInvisible();
                // Ocsp is an additional validation step for a server certifidate.
                // This field is not relevant if the user hasn't chosen a valid
                // CA certificate yet.
                setOcspInvisible();
            }
        }
    }

    private void showPeapFields() {
        int phase2Method = mPhase2Spinner.getSelectedItemPosition();
        if (phase2Method == WIFI_PEAP_PHASE2_SIM || phase2Method == WIFI_PEAP_PHASE2_AKA
                 || phase2Method == WIFI_PEAP_PHASE2_AKA_PRIME) {
            mEapIdentityView.setText("");
            mView.findViewById(R.id.l_identity).setVisibility(View.GONE);
            setPasswordInvisible();
            mView.findViewById(R.id.l_sim).setVisibility(View.VISIBLE);
        } else {
            mView.findViewById(R.id.l_identity).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.password_layout).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.show_password_layout).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.l_sim).setVisibility(View.GONE);
        }
    }

    private void setIdentityInvisible() {
        mView.findViewById(R.id.l_identity).setVisibility(View.GONE);
    }

    private void setPhase2Invisible() {
        mView.findViewById(R.id.l_phase2).setVisibility(View.GONE);
    }

    private void setCaCertInvisible() {
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.GONE);
        setSelection(mEapCaCertSpinner, mUnspecifiedCertString);
    }

    private void setOcspInvisible() {
        mView.findViewById(R.id.l_ocsp).setVisibility(View.GONE);
        mEapOcspSpinner.setSelection(WifiEnterpriseConfig.OCSP_NONE);
    }

    private void setDomainInvisible() {
        mView.findViewById(R.id.l_domain).setVisibility(View.GONE);
        mEapDomainView.setText("");
    }

    private void setUserCertInvisible() {
        mView.findViewById(R.id.l_user_cert).setVisibility(View.GONE);
        setSelection(mEapUserCertSpinner, mUnspecifiedCertString);
    }

    private void setAnonymousIdentInvisible() {
        mView.findViewById(R.id.l_anonymous).setVisibility(View.GONE);
        mEapAnonymousView.setText("");
    }

    private void setPasswordInvisible() {
        mPasswordView.setText("");
        mView.findViewById(R.id.password_layout).setVisibility(View.GONE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.GONE);
    }

    private void setEapMethodInvisible() {
        mView.findViewById(R.id.eap).setVisibility(View.GONE);
    }

    private void showIpConfigFields() {
        WifiConfiguration config = null;

        mView.findViewById(R.id.ip_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.isSaved()) {
            config = mAccessPoint.getConfig();
        }

        if (mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) {
            mView.findViewById(R.id.staticip).setVisibility(View.VISIBLE);
            if (mIpAddressView == null) {
                mIpAddressView = (TextView) mView.findViewById(R.id.ipaddress);
                mIpAddressView.addTextChangedListener(this);
                mGatewayView = (TextView) mView.findViewById(R.id.gateway);
                mGatewayView.addTextChangedListener(this);
                mNetworkPrefixLengthView = (TextView) mView.findViewById(
                        R.id.network_prefix_length);
                mNetworkPrefixLengthView.addTextChangedListener(this);
                mDns1View = (TextView) mView.findViewById(R.id.dns1);
                mDns1View.addTextChangedListener(this);
                mDns2View = (TextView) mView.findViewById(R.id.dns2);
                mDns2View.addTextChangedListener(this);
            }
            if (config != null) {
                StaticIpConfiguration staticConfig = config.getIpConfiguration()
                        .getStaticIpConfiguration();
                if (staticConfig != null) {
                    if (staticConfig.getIpAddress() != null) {
                        mIpAddressView.setText(
                                staticConfig.getIpAddress().getAddress().getHostAddress());
                        mNetworkPrefixLengthView.setText(Integer.toString(
                                staticConfig.getIpAddress().getPrefixLength()));
                    }

                    if (staticConfig.getGateway() != null) {
                        mGatewayView.setText(staticConfig.getGateway().getHostAddress());
                    }

                    Iterator<InetAddress> dnsIterator = staticConfig.getDnsServers().iterator();
                    if (dnsIterator.hasNext()) {
                        mDns1View.setText(dnsIterator.next().getHostAddress());
                    }
                    if (dnsIterator.hasNext()) {
                        mDns2View.setText(dnsIterator.next().getHostAddress());
                    }
                }
            }
        } else {
            mView.findViewById(R.id.staticip).setVisibility(View.GONE);
        }
    }

    private void showProxyFields() {
        WifiConfiguration config = null;

        mView.findViewById(R.id.proxy_settings_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.isSaved()) {
            config = mAccessPoint.getConfig();
        }

        if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) {
            setVisibility(R.id.proxy_warning_limited_support, View.VISIBLE);
            setVisibility(R.id.proxy_fields, View.VISIBLE);
            setVisibility(R.id.proxy_pac_field, View.GONE);
            if (mProxyHostView == null) {
                mProxyHostView = (TextView) mView.findViewById(R.id.proxy_hostname);
                mProxyHostView.addTextChangedListener(this);
                mProxyPortView = (TextView) mView.findViewById(R.id.proxy_port);
                mProxyPortView.addTextChangedListener(this);
                mProxyExclusionListView = (TextView) mView.findViewById(R.id.proxy_exclusionlist);
                mProxyExclusionListView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyProperties = config.getHttpProxy();
                if (proxyProperties != null) {
                    mProxyHostView.setText(proxyProperties.getHost());
                    mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                    mProxyExclusionListView.setText(
                            ProxyUtils.exclusionListAsString(proxyProperties.getExclusionList()));
                }
            }
        } else if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_PAC) {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE);
            setVisibility(R.id.proxy_fields, View.GONE);
            setVisibility(R.id.proxy_pac_field, View.VISIBLE);

            if (mProxyPacView == null) {
                mProxyPacView = (TextView) mView.findViewById(R.id.proxy_pac);
                mProxyPacView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyInfo = config.getHttpProxy();
                if (proxyInfo != null) {
                    mProxyPacView.setText(proxyInfo.getPacFileUrl().toString());
                }
            }
        } else {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE);
            setVisibility(R.id.proxy_fields, View.GONE);
            setVisibility(R.id.proxy_pac_field, View.GONE);
        }
    }

    private void setVisibility(int id, int visibility) {
        final View v = mView.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    @VisibleForTesting
    AndroidKeystoreAliasLoader getAndroidKeystoreAliasLoader() {
        return new AndroidKeystoreAliasLoader(KeyProperties.NAMESPACE_WIFI);
    }

    @VisibleForTesting
    void loadSims() {
        List<SubscriptionInfo> activeSubscriptionInfos = mContext
                .getSystemService(SubscriptionManager.class).getActiveSubscriptionInfoList();
        if (activeSubscriptionInfos == null) {
            activeSubscriptionInfos = Collections.EMPTY_LIST;
        }
        mActiveSubscriptionInfos.clear();

        // De-duplicates active subscriptions and caches in mActiveSubscriptionInfos.
        for (SubscriptionInfo newInfo : activeSubscriptionInfos) {
            for (SubscriptionInfo cachedInfo : mActiveSubscriptionInfos) {
                if (newInfo.getCarrierId() == cachedInfo.getCarrierId()) {
                    continue;
                }
            }
            mActiveSubscriptionInfos.add(newInfo);
        }

        // Shows disabled 'No SIM' when there is no active subscription.
        if (mActiveSubscriptionInfos.size() == 0) {
            final String[] noSim = new String[]{mContext.getString(R.string.wifi_no_sim_card)};
            mEapSimSpinner.setAdapter(getSpinnerAdapter(noSim));
            mEapSimSpinner.setSelection(0 /* position */);
            mEapSimSpinner.setEnabled(false);
            return;
        }

        // Shows display name of each active subscription.
        final ArrayList<CharSequence> displayNames = new ArrayList<>();
        for (SubscriptionInfo activeSubInfo : mActiveSubscriptionInfos) {
            displayNames.add(
                SubscriptionUtil.getUniqueSubscriptionDisplayName(activeSubInfo, mContext));
        }
        mEapSimSpinner.setAdapter(
            getSpinnerAdapter(displayNames.toArray(new String[displayNames.size()])));
        mEapSimSpinner.setSelection(0 /* position */);
        if (displayNames.size() == 1) {
            mEapSimSpinner.setEnabled(false);
        }
    }

    @VisibleForTesting
    void loadCertificates(
            Spinner spinner,
            Collection<String> choices,
            String noCertificateString,
            boolean showMultipleCerts,
            boolean showUsePreinstalledCertOption) {
        final Context context = mConfigUi.getContext();

        ArrayList<String> certs = new ArrayList<String>();
        certs.add(mUnspecifiedCertString);
        if (showMultipleCerts) {
            certs.add(mMultipleCertSetString);
        }
        if (showUsePreinstalledCertOption) {
            certs.add(mUseSystemCertsString);
        }

        if (choices != null && choices.size() != 0) {
            certs.addAll(choices.stream()
                    .filter(certificateName -> {
                        for (String undesired : UNDESIRED_CERTIFICATES) {
                            if (certificateName.startsWith(undesired)) {
                                return false;
                            }
                        }
                        return true;
                    }).collect(Collectors.toList()));
        }

        if (!TextUtils.isEmpty(noCertificateString)
                && mAccessPointSecurity != AccessPoint.SECURITY_EAP_SUITE_B) {
            certs.add(noCertificateString);
        }

        // If there is only mUnspecifiedCertString and one item to select, only shows the item
        if (certs.size() == 2) {
            certs.remove(mUnspecifiedCertString);
            spinner.setEnabled(false);
        } else {
            spinner.setEnabled(true);
        }

        final ArrayAdapter<CharSequence> adapter = getSpinnerAdapter(
                certs.toArray(new String[certs.size()]));
        spinner.setAdapter(adapter);
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; --i) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    public int getMode() {
        return mMode;
    }

    @Override
    public void afterTextChanged(Editable s) {
        ThreadUtils.postOnMainThread(() -> {
            showWarningMessagesIfAppropriate();
            enableSubmitIfAppropriate();
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // work done in afterTextChanged
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // work done in afterTextChanged
    }

    @Override
    public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
        if (textView == mPasswordView) {
            if (id == EditorInfo.IME_ACTION_DONE && isSubmittable()) {
                mConfigUi.dispatchSubmit();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (view == mPasswordView) {
            if (keyCode == KeyEvent.KEYCODE_ENTER && isSubmittable()) {
                mConfigUi.dispatchSubmit();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        if (view.getId() == R.id.show_password) {
            int pos = mPasswordView.getSelectionEnd();
            mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT
                    | (isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                 : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            if (pos >= 0) {
                ((EditText) mPasswordView).setSelection(pos);
            }
        } else if (view.getId() == R.id.wifi_advanced_togglebox) {
            // Hide the SoftKeyboard temporary to let user can see most of the expanded items.
            hideSoftKeyboard(mView.getWindowToken());
            view.setVisibility(View.GONE);
            mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mSecuritySpinner) {
            // Convert menu position to actual Wi-Fi security type
            mAccessPointSecurity = mSecurityInPosition[position];
            showSecurityFields(/* refreshEapMethods */ true, /* refreshCertificates */ true);

            if (WifiDppUtils.isSupportEnrolleeQrCodeScanner(mContext, mAccessPointSecurity)) {
                mSsidScanButton.setVisibility(View.VISIBLE);
            } else {
                mSsidScanButton.setVisibility(View.GONE);
            }
        } else if (parent == mEapMethodSpinner) {
            showSecurityFields(/* refreshEapMethods */ false, /* refreshCertificates */ true);
        } else if (parent == mEapCaCertSpinner) {
            showSecurityFields(/* refreshEapMethods */ false, /* refreshCertificates */ false);
        } else if (parent == mPhase2Spinner
                && mEapMethodSpinner.getSelectedItemPosition() == WIFI_EAP_METHOD_PEAP) {
            showPeapFields();
        } else if (parent == mProxySettingsSpinner) {
            showProxyFields();
        } else if (parent == mHiddenSettingsSpinner) {
            mHiddenWarningView.setVisibility(position == NOT_HIDDEN_NETWORK
                    ? View.GONE : View.VISIBLE);
        } else {
            showIpConfigFields();
        }
        showWarningMessagesIfAppropriate();
        enableSubmitIfAppropriate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    /**
     * Make the characters of the password visible if show_password is checked.
     */
    public void updatePassword() {
        TextView passwdView = (TextView) mView.findViewById(R.id.password);
        passwdView.setInputType(InputType.TYPE_CLASS_TEXT
                | (((CheckBox) mView.findViewById(R.id.show_password)).isChecked()
                   ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                   : InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
    }

    private void configureSecuritySpinner() {
        mConfigUi.setTitle(R.string.wifi_add_network);

        mSsidView = (TextView) mView.findViewById(R.id.ssid);
        mSsidView.addTextChangedListener(this);
        mSecuritySpinner = ((Spinner) mView.findViewById(R.id.security));
        mSecuritySpinner.setOnItemSelectedListener(this);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_item, android.R.id.text1);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSecuritySpinner.setAdapter(spinnerAdapter);
        int idx = 0;

        // Populate the Wi-Fi security spinner with the various supported key management types
        spinnerAdapter.add(mContext.getString(com.android.settingslib.R.string.wifi_security_none));
        mSecurityInPosition[idx++] = AccessPoint.SECURITY_NONE;
        if (mWifiManager.isEnhancedOpenSupported()) {
            spinnerAdapter.add(
                    mContext.getString(com.android.settingslib.R.string.wifi_security_owe));
            mSecurityInPosition[idx++] = AccessPoint.SECURITY_OWE;
        }
        spinnerAdapter.add(mContext.getString(com.android.settingslib.R.string.wifi_security_wep));
        mSecurityInPosition[idx++] = AccessPoint.SECURITY_WEP;
        spinnerAdapter.add(
                mContext.getString(com.android.settingslib.R.string.wifi_security_wpa_wpa2));
        mSecurityInPosition[idx++] = AccessPoint.SECURITY_PSK;
        if (mWifiManager.isWpa3SaeSupported()) {
            spinnerAdapter.add(
                    mContext.getString(com.android.settingslib.R.string.wifi_security_sae));
            mSecurityInPosition[idx++] = AccessPoint.SECURITY_SAE;
            spinnerAdapter.add(mContext.getString(
                    com.android.settingslib.R.string.wifi_security_eap_wpa_wpa2));
            mSecurityInPosition[idx++] = AccessPoint.SECURITY_EAP;
            spinnerAdapter.add(
                    mContext.getString(com.android.settingslib.R.string.wifi_security_eap_wpa3));
            mSecurityInPosition[idx++] = AccessPoint.SECURITY_EAP_WPA3_ENTERPRISE;
        } else {
            spinnerAdapter.add(
                    mContext.getString(com.android.settingslib.R.string.wifi_security_eap));
            mSecurityInPosition[idx++] = AccessPoint.SECURITY_EAP;
        }
        if (mWifiManager.isWpa3SuiteBSupported()) {
            spinnerAdapter.add(
                    mContext.getString(com.android.settingslib.R.string.wifi_security_eap_suiteb));
            mSecurityInPosition[idx++] = AccessPoint.SECURITY_EAP_SUITE_B;
        }

        spinnerAdapter.notifyDataSetChanged();

        mView.findViewById(R.id.type).setVisibility(View.VISIBLE);

        showIpConfigFields();
        showProxyFields();
        mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
        // Hidden option can be changed only when the user adds a network manually.
        mView.findViewById(R.id.hidden_settings_field).setVisibility(View.VISIBLE);
        ((CheckBox) mView.findViewById(R.id.wifi_advanced_togglebox))
                .setOnCheckedChangeListener(this);
        // Set correct accessibility strings.
        setAdvancedOptionAccessibilityString();
    }

    /**
     * For each target string in {@code targetStringArray} try to find if it appears in {@code
     * originalStringArray}, if found then use the corresponding string, which have the same index
     * of the target string in {@code replacementStringArray}, to replace it. And finally return the
     * whole new string array back to caller.
     */
    @VisibleForTesting
    CharSequence[] findAndReplaceTargetStrings(CharSequence originalStringArray[],
            CharSequence targetStringArray[], CharSequence replacementStringArray[]) {
        // The length of the targetStringArray and replacementStringArray should be the same, each
        // item in the targetStringArray should have a 1:1 mapping to replacementStringArray, so
        // just return the original string if the lengths are different.
        if (targetStringArray.length != replacementStringArray.length) {
            return originalStringArray;
        }

        final CharSequence[] returnEntries = new CharSequence[originalStringArray.length];
        for (int i = 0; i < originalStringArray.length; i++) {
            returnEntries[i] = originalStringArray[i];
            for (int j = 0; j < targetStringArray.length; j++) {
                if (TextUtils.equals(originalStringArray[i], targetStringArray[j])) {
                    returnEntries[i] = replacementStringArray[j];
                }
            }
        }
        return returnEntries;
    }

    private ArrayAdapter<CharSequence> getSpinnerAdapter(
            int contentStringArrayResId) {
        return getSpinnerAdapter(
                mContext.getResources().getStringArray(contentStringArrayResId));
    }

    @VisibleForTesting
    ArrayAdapter<CharSequence> getSpinnerAdapter(
            String[] contentStringArray) {
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_item, contentStringArray);
        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        return spinnerAdapter;
    }

    /**
     * This function is to span the TTS strings to each EAP method items in the
     * spinner to have detail TTS content for the TTS engine usage.
     */
    private ArrayAdapter<CharSequence> getSpinnerAdapterWithEapMethodsTts(
                int contentStringArrayResId) {
        final Resources res = mContext.getResources();
        CharSequence[] sourceStrings = res.getStringArray(
                contentStringArrayResId);
        CharSequence[] targetStrings = res.getStringArray(
                R.array.wifi_eap_method_target_strings);
        CharSequence[] ttsStrings = res.getStringArray(
                R.array.wifi_eap_method_tts_strings);

        // Replace the target strings with tts strings and save all in a new array.
        final CharSequence[] newTtsSourceStrings = findAndReplaceTargetStrings(
                sourceStrings, targetStrings, ttsStrings);

        // Build new TtsSpan text arrays for TalkBack.
        final CharSequence[] accessibilityArray = createAccessibleEntries(
                sourceStrings, newTtsSourceStrings);

        // Return a new ArrayAdapter with the new TalkBack array.
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(
                mContext, android.R.layout.simple_spinner_item, accessibilityArray);
        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        return spinnerAdapter;
    }

    private SpannableString[] createAccessibleEntries(CharSequence entries[],
            CharSequence[] contentDescriptions) {
        final SpannableString[] accessibleEntries = new SpannableString[entries.length];
        for (int i = 0; i < entries.length; i++) {
            accessibleEntries[i] = com.android.settings.Utils.createAccessibleSequence(entries[i],
                    contentDescriptions[i].toString());
        }
        return accessibleEntries;
    }

    private void hideSoftKeyboard(IBinder windowToken) {
        final InputMethodManager inputMethodManager = mContext.getSystemService(
                InputMethodManager.class);
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0 /* flags */);
    }

    private void setAdvancedOptionAccessibilityString() {
        final CheckBox advancedToggleBox = mView.findViewById(R.id.wifi_advanced_togglebox);
        advancedToggleBox.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    View v, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(v, info);
                // To let TalkBack don't pronounce checked/unchecked.
                info.setCheckable(false /* checkable */);
                // To let TalkBack don't pronounce CheckBox.
                info.setClassName(null /* className */);
                // Customize TalkBack's pronunciation which been appended to "Double-tap to".
                final AccessibilityAction customClick = new AccessibilityAction(
                        AccessibilityNodeInfo.ACTION_CLICK,
                        mContext.getString(R.string.wifi_advanced_toggle_description_collapsed));
                info.addAction(customClick);
            }
        });
    }
}
