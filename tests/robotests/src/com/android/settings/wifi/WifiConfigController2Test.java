/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.wifi.WifiConfigController2.DEFAULT_ANONYMOUS_ID;
import static com.android.settings.wifi.WifiConfigController2.WIFI_EAP_METHOD_SIM;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.net.wifi.WifiManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.utils.AndroidKeystoreAliasLoader;
import com.android.settings.wifi.details2.WifiPrivacyPreferenceController2;
import com.android.wifitrackerlib.WifiEntry;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowInputMethodManager;
import org.robolectric.shadows.ShadowSubscriptionManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(RobolectricTestRunner.class)
public class WifiConfigController2Test {

    static final String WIFI_EAP_TLS_V1_3 = "TLS v1.3";

    @Mock
    private WifiConfigUiBase2 mConfigUiBase;
    @Mock
    private Context mContext;
    @Mock
    private WifiEntry mWifiEntry;
    @Mock
    private AndroidKeystoreAliasLoader mAndroidKeystoreAliasLoader;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    Spinner mEapMethodSimSpinner;
    private View mView;
    private Spinner mHiddenSettingsSpinner;
    private Spinner mEapCaCertSpinner;
    private Spinner mEapUserCertSpinner;
    private String mUnspecifiedCertString;
    private String mUseSystemCertsString;
    private String mDoNotProvideEapUserCertString;
    private ShadowSubscriptionManager mShadowSubscriptionManager;

    public WifiConfigController2 mController;
    private static final String HEX_PSK = "01234567012345670123456701234567012345670123456701234567"
            + "01abcdef";
    // An invalid ASCII PSK pass phrase. It is 64 characters long, must not be greater than 63
    private static final String LONG_PSK =
            "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijkl";
    // An invalid PSK pass phrase. It is 7 characters long, must be at least 8
    private static final String SHORT_PSK = "abcdefg";
    // Valid PSK pass phrase
    private static final String GOOD_PSK = "abcdefghijklmnopqrstuvwxyz";
    private static final String GOOD_SSID = "abc";
    private static final String VALID_HEX_PSK =
            "123456789012345678901234567890123456789012345678901234567890abcd";
    private static final String INVALID_HEX_PSK =
            "123456789012345678901234567890123456789012345678901234567890ghij";
    private static final String NUMBER_AND_CHARACTER_KEY = "123456abcd";
    private static final String PARTIAL_NUMBER_AND_CHARACTER_KEY = "123456abc?";
    private static final int DHCP = 0;
    // Saved certificates
    private static final String SAVED_CA_CERT = "saved CA cert";
    private static final String SAVED_USER_CERT = "saved user cert";

    private static final int POSITION_SYSTEM_CERT = 1;
    private static final int POSITION_INSTALL_CERT = 2;
    private static final String ACTION_INSTALL_CERTS = "android.credentials.INSTALL";
    private static final String KEY_INSTALL_CERTIFICATE = "certificate_install_usage";
    private static final String INSTALL_CERTIFICATE_VALUE = "wifi";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(eq(WifiManager.class))).thenReturn(mWifiManager);
        when(mWifiManager.isConnectedMacRandomizationSupported()).thenReturn(true);
        when(mConfigUiBase.getContext()).thenReturn(mContext);
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_PSK);
        mView = LayoutInflater.from(mContext).inflate(R.layout.wifi_dialog, null);
        final Spinner ipSettingsSpinner = mView.findViewById(R.id.ip_settings);
        mHiddenSettingsSpinner = mView.findViewById(R.id.hidden_settings);
        mEapCaCertSpinner = mView.findViewById(R.id.ca_cert);
        mEapUserCertSpinner = mView.findViewById(R.id.user_cert);
        mUseSystemCertsString = mContext.getString(R.string.wifi_use_system_certs);
        mUnspecifiedCertString = mContext.getString(R.string.wifi_unspecified);
        mDoNotProvideEapUserCertString =
                mContext.getString(R.string.wifi_do_not_provide_eap_user_cert);
        ipSettingsSpinner.setSelection(DHCP);
        mShadowSubscriptionManager = shadowOf(mContext.getSystemService(SubscriptionManager.class));
        when(mEapMethodSimSpinner.getSelectedItemPosition()).thenReturn(WIFI_EAP_METHOD_SIM);
    }

    private void createController(
            WifiEntry mWifiEntry, int modeConnect, boolean hideMeteredAndPrivacy) {
        mController = new WifiConfigController2(mConfigUiBase, mView, mWifiEntry,
                modeConnect, hideMeteredAndPrivacy, mWifiManager, mAndroidKeystoreAliasLoader);
    }

    @Test
    public void ssidExceeds32Bytes_shouldShowSsidTooLongWarning() {
        createController(null, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView ssid = mView.findViewById(R.id.ssid);
        assertThat(ssid).isNotNull();
        ssid.setText("☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎");
        mController.showWarningMessagesIfAppropriate();

        assertThat(mView.findViewById(R.id.ssid_too_long_warning).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void ssidShorterThan32Bytes_shouldNotShowSsidTooLongWarning() {
        createController(null, WifiConfigUiBase2.MODE_CONNECT, false);

        final TextView ssid = mView.findViewById(R.id.ssid);
        assertThat(ssid).isNotNull();
        ssid.setText("123456789012345678901234567890");
        mController.showWarningMessagesIfAppropriate();

        assertThat(mView.findViewById(R.id.ssid_too_long_warning).getVisibility())
                .isEqualTo(View.GONE);

        ssid.setText("123");
        mController.showWarningMessagesIfAppropriate();

        assertThat(mView.findViewById(R.id.ssid_too_long_warning).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void isSubmittable_noSSID_shouldReturnFalse() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView ssid = mView.findViewById(R.id.ssid);
        assertThat(ssid).isNotNull();
        ssid.setText("");
        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_longPsk_shouldReturnFalse() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText(LONG_PSK);
        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_shortPsk_shouldReturnFalse() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText(SHORT_PSK);
        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_goodPsk_shouldReturnTrue() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText(GOOD_PSK);
        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_hexPsk_shouldReturnTrue() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText(HEX_PSK);
        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_savedConfigZeroLengthPassword_shouldReturnTrue() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText("");
        when(mWifiEntry.isSaved()).thenReturn(true);
        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_nullWifiEntry_noException() {
        createController(null, WifiConfigUiBase2.MODE_CONNECT, false);
        mController.isSubmittable();
    }

    @Test
    public void isSubmittable_EapToPskWithValidPassword_shouldReturnTrue() {
        createController(null, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView ssid = mView.findViewById(R.id.ssid);
        final TextView password = mView.findViewById(R.id.password);
        final Spinner securitySpinner = mView.findViewById(R.id.security);
        assertThat(password).isNotNull();
        assertThat(securitySpinner).isNotNull();
        when(mWifiEntry.isSaved()).thenReturn(true);

        // Change it from EAP to PSK
        mController.onItemSelected(securitySpinner, null, WifiEntry.SECURITY_EAP, 0);
        mController.onItemSelected(securitySpinner, null, WifiEntry.SECURITY_PSK, 0);
        password.setText(GOOD_PSK);
        ssid.setText(GOOD_SSID);

        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_EapWithAkaMethod_shouldReturnTrue() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        when(mWifiEntry.isSaved()).thenReturn(true);
        mController.mWifiEntrySecurity = WifiEntry.SECURITY_EAP;
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.GONE);

        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_caCertWithoutDomain_shouldReturnFalse() {
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.VISIBLE);
        final Spinner eapCaCertSpinner = mView.findViewById(R.id.ca_cert);
        eapCaCertSpinner.setAdapter(mController.getSpinnerAdapter(new String[]{"certificate"}));
        eapCaCertSpinner.setSelection(0);
        mView.findViewById(R.id.l_domain).setVisibility(View.VISIBLE);

        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_caCertWithDomain_shouldReturnTrue() {
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.VISIBLE);
        final Spinner eapCaCertSpinner = mView.findViewById(R.id.ca_cert);
        eapCaCertSpinner.setAdapter(mController.getSpinnerAdapter(new String[]{"certificate"}));
        eapCaCertSpinner.setSelection(0);
        mView.findViewById(R.id.l_domain).setVisibility(View.VISIBLE);
        ((TextView) mView.findViewById(R.id.domain)).setText("fakeDomain");

        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void getSignalString_notReachable_shouldHaveNoSignalString() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        when(mWifiEntry.getLevel()).thenReturn(WifiEntry.WIFI_LEVEL_UNREACHABLE);

        assertThat(mController.getSignalString()).isNull();
    }

    @Test
    public void loadCertificates_undesiredCertificates_shouldNotLoadUndesiredCertificates() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final Spinner spinner = new Spinner(mContext);

        mController.loadCertificates(spinner,
                Arrays.asList(WifiConfigController.UNDESIRED_CERTIFICATES),
                "doNotProvideEapUserCertString",
                false /* showMultipleCerts */,
                false /* showUsePreinstalledCertOption */);

        assertThat(spinner.getAdapter().getCount()).isEqualTo(1);   // doNotProvideEapUserCertString
    }

    @Test
    public void ssidGetFocus_addNewNetwork_shouldReturnTrue() {
        createController(null, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView ssid = mView.findViewById(R.id.ssid);
        // Verify ssid text get focus when add new network (wifiEntry is null)
        assertThat(ssid.isFocused()).isTrue();
    }

    @Test
    public void passwordGetFocus_connectSecureWifi_shouldReturnTrue() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        // Verify password get focus when connect to secure wifi without eap type
        assertThat(password.isFocused()).isTrue();
    }

    @Test
    public void hiddenWarning_warningVisibilityProperlyUpdated() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        View warningView = mView.findViewById(R.id.hidden_settings_warning);
        mController.onItemSelected(mHiddenSettingsSpinner, null, mController.HIDDEN_NETWORK, 0);
        assertThat(warningView.getVisibility()).isEqualTo(View.VISIBLE);

        mController.onItemSelected(mHiddenSettingsSpinner, null, mController.NOT_HIDDEN_NETWORK, 0);
        assertThat(warningView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void hiddenField_visibilityUpdatesCorrectly() {
        View hiddenField = mView.findViewById(R.id.hidden_settings_field);
        assertThat(hiddenField.getVisibility()).isEqualTo(View.GONE);

        createController(null, WifiConfigUiBase2.MODE_CONNECT, false);
        assertThat(hiddenField.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void securitySpinner_saeSuitebAndOweNotVisible() {
        securitySpinnerTestHelper(false, false, false);
    }

    @Test
    public void securitySpinner_saeSuitebAndOweVisible() {
        securitySpinnerTestHelper(true, true, true);
    }

    @Test
    public void securitySpinner_saeVisible_suitebAndOweNotVisible() {
        securitySpinnerTestHelper(true, false, false);
    }

    @Test
    public void securitySpinner_oweVisible_suitebAndSaeNotVisible() {
        securitySpinnerTestHelper(false, false, true);
    }

    private void securitySpinnerTestHelper(boolean saeVisible, boolean suitebVisible,
            boolean oweVisible) {
        when(mWifiManager.isWpa3SaeSupported()).thenReturn(saeVisible);
        when(mWifiManager.isWpa3SuiteBSupported()).thenReturn(suitebVisible);
        when(mWifiManager.isEnhancedOpenSupported()).thenReturn(oweVisible);

        createController(null, WifiConfigUiBase2.MODE_MODIFY, false);

        final Spinner securitySpinner = mView.findViewById(R.id.security);
        final ArrayAdapter<String> adapter = (ArrayAdapter) securitySpinner.getAdapter();
        boolean saeFound = false;
        boolean suitebFound = false;
        boolean oweFound = false;
        for (int i = 0; i < adapter.getCount(); i++) {
            String val = adapter.getItem(i);

            if (val.compareTo(mContext.getString(
                    com.android.settingslib.R.string.wifi_security_sae)) == 0) {
                saeFound = true;
            }

            if (val.compareTo(mContext.getString(
                    com.android.settingslib.R.string.wifi_security_eap_suiteb)) == 0) {
                suitebFound = true;
            }

            if (val.compareTo(mContext.getString(
                    com.android.settingslib.R.string.wifi_security_owe)) == 0) {
                oweFound = true;
            }
        }

        if (saeVisible) {
            assertThat(saeFound).isTrue();
        } else {
            assertThat(saeFound).isFalse();
        }
        if (suitebVisible) {
            assertThat(suitebFound).isTrue();
        } else {
            assertThat(suitebFound).isFalse();
        }
        if (oweVisible) {
            assertThat(oweFound).isTrue();
        } else {
            assertThat(oweFound).isFalse();
        }
    }

    @Test
    public void whenHideMeteredAndPrivacy_shouldHideMetered() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, true);

        View view = mView.findViewById(R.id.metered_settings_fields);

        assertThat(view.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void whenHideMeteredAndPrivacy_shouldHidePrivacy() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, true);

        View view = mView.findViewById(R.id.privacy_settings_fields);

        assertThat(view.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void loadMacRandomizedValue_shouldPersistentAsDefault() {
        final Spinner privacySetting = mView.findViewById(R.id.privacy_settings);
        final int prefPersist =
                WifiPrivacyPreferenceController2.translateMacRandomizedValueToPrefValue(
                        WifiConfiguration.RANDOMIZATION_PERSISTENT);

        assertThat(privacySetting.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(privacySetting.getSelectedItemPosition()).isEqualTo(prefPersist);
    }

    @Test
    public void loadSavedMacRandomizedPersistentValue_shouldCorrectMacValue() {
        checkSavedMacRandomizedValue(WifiConfiguration.RANDOMIZATION_PERSISTENT);
    }

    @Test
    public void loadSavedMacRandomizedNoneValue_shouldCorrectMacValue() {
        checkSavedMacRandomizedValue(WifiConfiguration.RANDOMIZATION_NONE);
    }

    private void checkSavedMacRandomizedValue(int macRandomizedValue) {
        when(mWifiEntry.isSaved()).thenReturn(true);
        final WifiConfiguration mockWifiConfig = spy(new WifiConfiguration());
        when(mockWifiConfig.getIpConfiguration()).thenReturn(mock(IpConfiguration.class));
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mockWifiConfig);
        mockWifiConfig.macRandomizationSetting = macRandomizedValue;
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);

        final Spinner privacySetting = mView.findViewById(R.id.privacy_settings);
        final int expectedPrefValue =
                WifiPrivacyPreferenceController2.translateMacRandomizedValueToPrefValue(
                        macRandomizedValue);

        assertThat(privacySetting.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(privacySetting.getSelectedItemPosition()).isEqualTo(expectedPrefValue);
    }

    @Test
    public void saveMacRandomizedValue_noChanged_shouldPersistentAsDefault() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        WifiConfiguration config = mController.getConfig();
        assertThat(config.macRandomizationSetting).isEqualTo(
                WifiConfiguration.RANDOMIZATION_PERSISTENT);
    }

    @Test
    public void saveMacRandomizedValue_ChangedToNone_shouldGetNone() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final Spinner privacySetting = mView.findViewById(R.id.privacy_settings);
        final int prefMacNone =
                WifiPrivacyPreferenceController2.translateMacRandomizedValueToPrefValue(
                        WifiConfiguration.RANDOMIZATION_NONE);
        privacySetting.setSelection(prefMacNone);

        WifiConfiguration config = mController.getConfig();
        assertThat(config.macRandomizationSetting).isEqualTo(WifiConfiguration.RANDOMIZATION_NONE);
    }

    @Test
    public void replaceTtsString_whenTargetMatched_shouldSuccess() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final CharSequence[] display = {"PEAP", "AKA1", "AKA2'"};
        final CharSequence[] target = {"AKA1", "AKA2'"};
        final CharSequence[] ttsString = {"AKA1_TTS", "AKA2_TTS"};

        final CharSequence[] resultTts = mController.findAndReplaceTargetStrings(display, target,
            ttsString);

        assertThat(resultTts[0]).isEqualTo("PEAP");
        assertThat(resultTts[1]).isEqualTo("AKA1_TTS");
        assertThat(resultTts[2]).isEqualTo("AKA2_TTS");
    }

    @Test
    public void replaceTtsString_whenNoTargetStringMatched_originalStringShouldNotChanged() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final CharSequence[] display = {"PEAP", "AKA1", "AKA2"};
        final CharSequence[] target = {"WEP1", "WEP2'"};
        final CharSequence[] ttsString = {"WEP1_TTS", "WEP2_TTS"};

        final CharSequence[] resultTts = mController.findAndReplaceTargetStrings(display, target,
            ttsString);

        assertThat(resultTts[0]).isEqualTo("PEAP");
        assertThat(resultTts[1]).isEqualTo("AKA1");
        assertThat(resultTts[2]).isEqualTo("AKA2");
    }

    @Test
    public void checktEapMethodTargetAndTtsArraylength_shouldHaveSameCount() {
        final Resources resources = mContext.getResources();
        final String[] targetStringArray = resources.getStringArray(
            R.array.wifi_eap_method_target_strings);
        final String[] ttsStringArray = resources.getStringArray(
            R.array.wifi_eap_method_tts_strings);

        assertThat(targetStringArray.length).isEqualTo(ttsStringArray.length);
    }

    @Test
    public void selectSecurity_wpa3Eap192bit_eapMethodTls() {
        when(mWifiManager.isWpa3SuiteBSupported()).thenReturn(true);
        createController(null, WifiConfigUiBase2.MODE_MODIFY, false);
        final Spinner securitySpinner = mView.findViewById(R.id.security);
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        int wpa3Eap192bitPosition = -1;
        final int securityCount = mController.mSecurityInPosition.length;
        for (int i = 0; i < securityCount; i++) {
            if (mController.mSecurityInPosition[i] != null
                    && mController.mSecurityInPosition[i] == WifiEntry.SECURITY_EAP_SUITE_B) {
                wpa3Eap192bitPosition = i;
            }
        }

        mController.onItemSelected(securitySpinner, /* view */ null, wpa3Eap192bitPosition,
                /* id */ 0);

        final int selectedItemPosition = eapMethodSpinner.getSelectedItemPosition();
        assertThat(eapMethodSpinner.getSelectedItem().toString()).isEqualTo("TLS");
    }

    @Test
    public void checkImeStatus_whenAdvancedToggled_shouldBeHide() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final InputMethodManager inputMethodManager = mContext
                .getSystemService(InputMethodManager.class);
        final ShadowInputMethodManager shadowImm = Shadows.shadowOf(inputMethodManager);
        final CheckBox advButton = mView.findViewById(R.id.wifi_advanced_togglebox);

        inputMethodManager.showSoftInput(null /* view */, 0 /* flags */);
        advButton.performClick();

        assertThat(shadowImm.isSoftInputVisible()).isFalse();
    }

    @Test
    public void selectEapMethod_savedWifiEntry_shouldGetCorrectPosition() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        WifiConfiguration wifiConfiguration;

        // Test EAP method PEAP
        eapMethodSpinner.setSelection(Eap.PEAP);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_PEAP_PHASE2_MSCHAPV2);
        wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getEapMethod()).isEqualTo(Eap.PEAP);
        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(
                Phase2.MSCHAPV2);

        // Test EAP method TTLS
        eapMethodSpinner.setSelection(Eap.TTLS);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_TTLS_PHASE2_MSCHAPV2);
        wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getEapMethod()).isEqualTo(Eap.TTLS);
        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(
                Phase2.MSCHAPV2);
    }

    @Test
    public void getHiddenSettingsPosition_whenAdvancedToggled_shouldBeFirst() {
        final LinearLayout advancedFieldsLayout = mView.findViewById(R.id.wifi_advanced_fields);
        final LinearLayout hiddenSettingLayout = mView.findViewById(R.id.hidden_settings_field);

        final LinearLayout firstChild = (LinearLayout) advancedFieldsLayout.getChildAt(0);

        assertThat(firstChild).isEqualTo(hiddenSettingLayout);
    }

    @Test
    public void getAdvancedOptionContentDescription_whenViewInitialed_shouldBeCorrect() {
        final CheckBox advButton = mView.findViewById(R.id.wifi_advanced_togglebox);

        assertThat(advButton.getContentDescription()).isEqualTo(
                mContext.getString(R.string.wifi_advanced_toggle_description));
    }

    @Test
    public void getWepConfig_withNumberAndCharacterKey_shouldContainTheSameKey() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        password.setText(NUMBER_AND_CHARACTER_KEY);
        mController.mWifiEntrySecurity = WifiEntry.SECURITY_WEP;

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.wepKeys[0]).isEqualTo(NUMBER_AND_CHARACTER_KEY);
    }

    @Test
    public void getWepConfig_withPartialNumberAndCharacterKey_shouldContainDifferentKey() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        password.setText(PARTIAL_NUMBER_AND_CHARACTER_KEY);
        mController.mWifiEntrySecurity = WifiEntry.SECURITY_WEP;

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.wepKeys[0]).isNotEqualTo(PARTIAL_NUMBER_AND_CHARACTER_KEY);
    }

    @Test
    public void getPskConfig_withValidHexKey_shouldContainTheSameKey() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        password.setText(VALID_HEX_PSK);
        mController.mWifiEntrySecurity = WifiEntry.SECURITY_PSK;

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.preSharedKey).isEqualTo(VALID_HEX_PSK);
    }

    @Test
    public void getPskConfig_withInvalidHexKey_shouldContainDifferentKey() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        final TextView password = mView.findViewById(R.id.password);
        password.setText(INVALID_HEX_PSK);
        mController.mWifiEntrySecurity = WifiEntry.SECURITY_PSK;

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.preSharedKey).isNotEqualTo(INVALID_HEX_PSK);
    }

    @Test
    public void getEapConfig_withPhase2Gtc_shouldContainGtcMethod() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();

        // Test EAP method PEAP
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        eapMethodSpinner.setSelection(Eap.PEAP);

        // Test phase2 GTC
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_PEAP_PHASE2_GTC);

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(Phase2.GTC);
    }

    @Test
    public void getEapConfig_withPhase2Sim_shouldContainSimMethod() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();

        // Test EAP method PEAP
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        eapMethodSpinner.setSelection(Eap.PEAP);

        // Test phase2 SIM
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_PEAP_PHASE2_SIM);

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(Phase2.SIM);
    }

    @Test
    public void getEapConfig_withPhase2Aka_shouldContainAkaMethod() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();

        // Test EAP method PEAP
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        eapMethodSpinner.setSelection(Eap.PEAP);

        // Test phase2 AKA
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_PEAP_PHASE2_AKA);

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(Phase2.AKA);
    }

    @Test
    public void getEapConfig_withPhase2AkaPrime_shouldContainAkaPrimeMethod() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();

        // Test EAP method PEAP
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        eapMethodSpinner.setSelection(Eap.PEAP);

        // Test phase2 AKA PRIME
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_PEAP_PHASE2_AKA_PRIME);

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(
                Phase2.AKA_PRIME);
    }

    @Test
    public void getEapConfig_withPeapPhase2Unknown_shouldContainNoneMethod() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();

        // Test EAP method PEAP
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        eapMethodSpinner.setSelection(Eap.PEAP);

        // Test phase2 Unknown
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        phase2Spinner.setSelection(-1);

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(Phase2.NONE);
    }

    @Test
    public void getEapConfig_withTTLSPhase2Pap_shouldContainPapMethod() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();

        // Test EAP method TTLS
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        eapMethodSpinner.setSelection(Eap.TTLS);

        // Test phase2 PAP
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_TTLS_PHASE2_PAP);

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(Phase2.PAP);
    }

    @Test
    public void getEapConfig_withTTLSPhase2Mschap_shouldContainMschapMethod() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();

        // Test EAP method TTLS
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        eapMethodSpinner.setSelection(Eap.TTLS);

        // Test phase2 MSCHAP
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_TTLS_PHASE2_MSCHAP);

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(Phase2.MSCHAP);
    }

    @Test
    public void getEapConfig_withTTLSPhase2Gtc_shouldContainGtcMethod() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        setUpModifyingSavedPeapConfigController();

        // Test EAP method TTLS
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        eapMethodSpinner.setSelection(Eap.TTLS);

        // Test phase2 GTC
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        phase2Spinner.setSelection(WifiConfigController2.WIFI_TTLS_PHASE2_GTC);

        WifiConfiguration wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(Phase2.GTC);
    }

    private void setUpModifyingSavedPeapConfigController() {
        when(mWifiEntry.isSaved()).thenReturn(true);
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        final WifiConfiguration mockWifiConfig = spy(new WifiConfiguration());
        mockWifiConfig.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP);
        when(mockWifiConfig.getIpConfiguration()).thenReturn(mock(IpConfiguration.class));
        final WifiEnterpriseConfig mockWifiEnterpriseConfig = spy(new WifiEnterpriseConfig());
        when(mockWifiEnterpriseConfig.getEapMethod()).thenReturn(Eap.PEAP);
        mockWifiConfig.enterpriseConfig = mockWifiEnterpriseConfig;
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mockWifiConfig);
        createController(mWifiEntry, WifiConfigUiBase2.MODE_MODIFY, false);
    }

    @Test
    public void loadSims_noSim_simSpinnerDefaultNoSim() {
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        mController.mEapMethodSpinner = mEapMethodSimSpinner;

        mController.loadSims();

        final WifiConfiguration wifiConfiguration = mController.getConfig();
        assertThat(wifiConfiguration.carrierId).isEqualTo(TelephonyManager.UNKNOWN_CARRIER_ID);
    }

    @Test
    public void loadSims_oneSim_simSpinnerDefaultSubscription() {
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        final SubscriptionInfo subscriptionInfo = mock(SubscriptionInfo.class);
        final int carrierId = 6;
        when(subscriptionInfo.getSubscriptionId()).thenReturn(carrierId);
        when(subscriptionInfo.getDisplayName()).thenReturn("FAKE-CARRIER");
        when(subscriptionInfo.getCarrierId()).thenReturn(carrierId);
        when(subscriptionInfo.getCarrierName()).thenReturn("FAKE-CARRIER");
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(subscriptionInfo));
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(Arrays.asList(subscriptionInfo));
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        mController.mEapMethodSpinner = mEapMethodSimSpinner;

        mController.loadSims();

        final WifiConfiguration wifiConfiguration = mController.getConfig();
        assertThat(wifiConfiguration.carrierId).isEqualTo(carrierId);
    }

    @Test
    public void loadSims_twoSimsWithDifferentCarrierId_showTwoSims() {
        SubscriptionInfo sub1 = createMockSubscription(1, "sub1", 8888);
        SubscriptionInfo sub2 = createMockSubscription(2, "sub2", 9999);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(Arrays.asList(sub1, sub2));
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        mController.mEapMethodSpinner = mEapMethodSimSpinner;
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(1);

        mController.loadSims();

        assertThat(mController.mEapSimSpinner.getAdapter().getCount()).isEqualTo(2);
    }

    @Test
    public void loadSims_twoSimsWithSameCarrierId_showOneDefaultDataSim() {
        SubscriptionInfo sub1 = createMockSubscription(1, "sub1", 9999);
        SubscriptionInfo sub2 = createMockSubscription(2, "sub2", 9999);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mShadowSubscriptionManager.setActiveSubscriptionInfoList(Arrays.asList(sub1, sub2));
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        mController.mEapMethodSpinner = mEapMethodSimSpinner;
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(1);

        mController.loadSims();

        assertThat(mController.mEapSimSpinner.getAdapter().getCount()).isEqualTo(1);
        assertThat(mController.mEapSimSpinner.getSelectedItem().toString()).isEqualTo("sub1");

        ShadowSubscriptionManager.setDefaultDataSubscriptionId(2);

        mController.loadSims();

        assertThat(mController.mEapSimSpinner.getAdapter().getCount()).isEqualTo(1);
        assertThat(mController.mEapSimSpinner.getSelectedItem().toString()).isEqualTo("sub2");
    }

    @Test
    public void loadCaCertificateValue_shouldPersistentAsDefault() {
        setUpModifyingSavedCertificateConfigController(null, null);

        mEapCaCertSpinner.setSelection(POSITION_SYSTEM_CERT);
        assertThat(mEapCaCertSpinner.getSelectedItem()).isEqualTo(mUseSystemCertsString);
    }

    @Test
    public void onItemSelected_shouldPersistentInstallCertsAndStartInstallActivity() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        String installCertsString = "install_certs";
        Spinner eapCaCertSpinner = mock(Spinner.class);
        AdapterView view = mock(AdapterView.class);
        when(eapCaCertSpinner.getItemAtPosition(anyInt())).thenReturn(view);
        when(view.toString()).thenReturn(installCertsString);
        mController.mInstallCertsString = installCertsString;
        mController.mEapCaCertSpinner = eapCaCertSpinner;

        mController.onItemSelected(eapCaCertSpinner, null, POSITION_INSTALL_CERT, 0);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(argumentCaptor.capture());
        final Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(ACTION_INSTALL_CERTS);
        assertThat(intent.getExtra(KEY_INSTALL_CERTIFICATE, ""))
                .isEqualTo(INSTALL_CERTIFICATE_VALUE);
    }

    @Test
    public void loadSavedCaCertificateValue_shouldBeCorrectValue() {
        setUpModifyingSavedCertificateConfigController(SAVED_CA_CERT, null);

        assertThat(mEapCaCertSpinner.getSelectedItem()).isEqualTo(SAVED_CA_CERT);
    }

    @Test
    public void loadUserCertificateValue_shouldPersistentAsDefault() {
        setUpModifyingSavedCertificateConfigController(null, null);

        assertThat(mEapUserCertSpinner.getSelectedItem()).isEqualTo(mDoNotProvideEapUserCertString);
    }

    @Test
    public void loadSavedUserCertificateValue_shouldBeCorrectValue() {
        setUpModifyingSavedCertificateConfigController(null, SAVED_USER_CERT);

        assertThat(mEapUserCertSpinner.getSelectedItem()).isEqualTo(SAVED_USER_CERT);
    }

    @Test
    public void getEapMinTlsVerSpinner_isTlsV13Supported_containsTlsV13() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        Spinner spinner = mController.getEapMinTlsVerSpinner(true /* isTlsV13Supported */);

        List<Object> list = IntStream.range(0, spinner.getAdapter().getCount())
                .mapToObj(spinner.getAdapter()::getItem)
                .collect(Collectors.toList());
        assertThat(list).contains(WIFI_EAP_TLS_V1_3);
    }

    @Test
    public void getEapMinTlsVerSpinner_isNotTlsV13Supported_doesNotContainTlsV13() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        Spinner spinner = mController.getEapMinTlsVerSpinner(false /* isTlsV13Supported */);

        List<Object> list = IntStream.range(0, spinner.getAdapter().getCount())
                .mapToObj(spinner.getAdapter()::getItem)
                .collect(Collectors.toList());
        assertThat(list).doesNotContain(WIFI_EAP_TLS_V1_3);
    }

    @Test
    public void setAnonymousIdVisible_showAnonymousIdAndSetDefaultId() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        View anonymousLayout = mView.findViewById(R.id.l_anonymous);
        TextView anonymousId = mView.findViewById(R.id.anonymous);
        mController.mEapAnonymousView = anonymousId;
        anonymousLayout.setVisibility(View.GONE);
        anonymousId.setText("");

        mController.setAnonymousIdVisible();

        assertThat(anonymousLayout.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(anonymousId.getText().toString()).isEqualTo(DEFAULT_ANONYMOUS_ID);
    }

    @Test
    public void setAnonymousIdVisible_viewIsVisible_doNotSetText() {
        createController(mWifiEntry, WifiConfigUiBase2.MODE_CONNECT, false);
        View anonymousLayout = mView.findViewById(R.id.l_anonymous);
        mController.mEapAnonymousView = mock(TextView.class);
        anonymousLayout.setVisibility(View.VISIBLE);

        mController.setAnonymousIdVisible();

        verify(mController.mEapAnonymousView, never()).setText(any(String.class));
    }

    private void setUpModifyingSavedCertificateConfigController(String savedCaCertificate,
            String savedUserCertificate) {
        final WifiConfiguration mockWifiConfig = spy(new WifiConfiguration());
        final WifiEnterpriseConfig mockWifiEnterpriseConfig = spy(new WifiEnterpriseConfig());

        mockWifiConfig.enterpriseConfig = mockWifiEnterpriseConfig;
        when(mWifiEntry.isSaved()).thenReturn(true);
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mockWifiConfig);
        when(mockWifiConfig.getIpConfiguration()).thenReturn(mock(IpConfiguration.class));
        when(mockWifiEnterpriseConfig.getEapMethod()).thenReturn(Eap.TLS);
        if (savedCaCertificate != null) {
            String[] savedCaCertificates = new String[]{savedCaCertificate};
            when(mockWifiEnterpriseConfig.getCaCertificateAliases())
                    .thenReturn(savedCaCertificates);
            when(mAndroidKeystoreAliasLoader.getCaCertAliases())
                    .thenReturn(ImmutableList.of(savedCaCertificate));
        }
        if (savedUserCertificate != null) {
            String[] savedUserCertificates = new String[]{savedUserCertificate};
            when(mockWifiEnterpriseConfig.getClientCertificateAlias())
                    .thenReturn(savedUserCertificate);
            when(mAndroidKeystoreAliasLoader.getKeyCertAliases())
                    .thenReturn(ImmutableList.of(savedUserCertificate));
        }

        createController(mWifiEntry, WifiConfigUiBase2.MODE_MODIFY, false);

        //  Because Robolectric has a different behavior from normal flow.
        //
        //  Normal flow:
        //    showSecurityFields start -> mEapMethodSpinner.setSelection
        //        -> showSecurityFields end -> mController.onItemSelected
        //
        //  Robolectric flow:
        //    showSecurityFields start -> mEapMethodSpinner.setSelection
        //        -> mController.onItemSelected -> showSecurityFields end
        //
        //  We need to add a redundant mEapMethodSpinner.setSelection here to verify whether the
        //  certificates are covered by mController.onItemSelected after showSecurityFields end.
        mController.mEapMethodSpinner.setSelection(Eap.TLS);
    }

    private SubscriptionInfo createMockSubscription(int subId, String displayName, int carrierId) {
        SubscriptionInfo sub = mock(SubscriptionInfo.class);
        when(sub.getSubscriptionId()).thenReturn(subId);
        when(sub.getDisplayName()).thenReturn(displayName);
        when(sub.getCarrierId()).thenReturn(carrierId);
        return sub;
    }
}
