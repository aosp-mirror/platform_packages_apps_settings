/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.net.wifi.WifiManager;
import android.os.ServiceSpecificException;
import android.security.KeyStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settings.wifi.details.WifiPrivacyPreferenceController;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class WifiConfigControllerTest {

    @Mock
    private WifiConfigUiBase mConfigUiBase;
    @Mock
    private Context mContext;
    @Mock
    private AccessPoint mAccessPoint;
    @Mock
    private KeyStore mKeyStore;
    private View mView;
    private Spinner mHiddenSettingsSpinner;

    public WifiConfigController mController;
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
    private static final int DHCP = 0;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mConfigUiBase.getContext()).thenReturn(mContext);
        when(mAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_PSK);
        mView = LayoutInflater.from(mContext).inflate(R.layout.wifi_dialog, null);
        final Spinner ipSettingsSpinner = mView.findViewById(R.id.ip_settings);
        mHiddenSettingsSpinner = mView.findViewById(R.id.hidden_settings);
        ipSettingsSpinner.setSelection(DHCP);

        mController = new TestWifiConfigController(mConfigUiBase, mView, mAccessPoint,
                WifiConfigUiBase.MODE_CONNECT);
    }

    @Test
    public void ssidExceeds32Bytes_shouldShowSsidTooLongWarning() {
        mController = new TestWifiConfigController(mConfigUiBase, mView, null /* accessPoint */,
                WifiConfigUiBase.MODE_CONNECT);
        final TextView ssid = mView.findViewById(R.id.ssid);
        assertThat(ssid).isNotNull();
        ssid.setText("☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎");
        mController.showWarningMessagesIfAppropriate();

        assertThat(mView.findViewById(R.id.ssid_too_long_warning).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void ssidShorterThan32Bytes_shouldNotShowSsidTooLongWarning() {
        mController = new TestWifiConfigController(mConfigUiBase, mView, null /* accessPoint */,
                WifiConfigUiBase.MODE_CONNECT);

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
        final TextView ssid = mView.findViewById(R.id.ssid);
        assertThat(ssid).isNotNull();
        ssid.setText("");
        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_longPsk_shouldReturnFalse() {
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText(LONG_PSK);
        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_shortPsk_shouldReturnFalse() {
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText(SHORT_PSK);
        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_goodPsk_shouldReturnTrue() {
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText(GOOD_PSK);
        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_hexPsk_shouldReturnTrue() {
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText(HEX_PSK);
        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_savedConfigZeroLengthPassword_shouldReturnTrue() {
        final TextView password = mView.findViewById(R.id.password);
        assertThat(password).isNotNull();
        password.setText("");
        when(mAccessPoint.isSaved()).thenReturn(true);
        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_nullAccessPoint_noException() {
        mController =
            new TestWifiConfigController(mConfigUiBase, mView, null, WifiConfigUiBase.MODE_CONNECT);
        mController.isSubmittable();
    }

    @Test
    public void isSubmittable_EapToPskWithValidPassword_shouldReturnTrue() {
        mController = new TestWifiConfigController(mConfigUiBase, mView, null,
                WifiConfigUiBase.MODE_CONNECT);
        final TextView ssid = mView.findViewById(R.id.ssid);
        final TextView password = mView.findViewById(R.id.password);
        final Spinner securitySpinner = mView.findViewById(R.id.security);
        assertThat(password).isNotNull();
        assertThat(securitySpinner).isNotNull();
        when(mAccessPoint.isSaved()).thenReturn(true);

        // Change it from EAP to PSK
        mController.onItemSelected(securitySpinner, null, AccessPoint.SECURITY_EAP, 0);
        mController.onItemSelected(securitySpinner, null, AccessPoint.SECURITY_PSK, 0);
        password.setText(GOOD_PSK);
        ssid.setText(GOOD_SSID);

        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_EapWithAkaMethod_shouldReturnTrue() {
        when(mAccessPoint.isSaved()).thenReturn(true);
        mController.mAccessPointSecurity = AccessPoint.SECURITY_EAP;
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.GONE);

        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void getSignalString_notReachable_shouldHaveNoSignalString() {
        when(mAccessPoint.isReachable()).thenReturn(false);

        assertThat(mController.getSignalString()).isNull();
    }

    @Test
    public void showForCarrierAp() {
        // Setup the mock view for wifi dialog.
        View view = mock(View.class);
        TextView nameText = mock(TextView.class);
        TextView valueText = mock(TextView.class);
        when(view.findViewById(R.id.name)).thenReturn(nameText);
        when(view.findViewById(R.id.value)).thenReturn(valueText);
        LayoutInflater inflater = mock(LayoutInflater.class);
        when(inflater.inflate(anyInt(), any(ViewGroup.class), anyBoolean())).thenReturn(view);
        when(mConfigUiBase.getLayoutInflater()).thenReturn(inflater);

        String carrierName = "Test Carrier";
        when(mAccessPoint.isCarrierAp()).thenReturn(true);
        when(mAccessPoint.getCarrierName()).thenReturn(carrierName);
        mController = new TestWifiConfigController(mConfigUiBase, mView, mAccessPoint,
                WifiConfigUiBase.MODE_CONNECT);
        // Verify the content of the text fields.
        verify(nameText).setText(R.string.wifi_carrier_connect);
        verify(valueText).setText(
                String.format(mContext.getString(R.string.wifi_carrier_content), carrierName));
        // Verify that the advance toggle is not visible.
        assertThat(mView.findViewById(R.id.wifi_advanced_toggle).getVisibility())
                .isEqualTo(View.GONE);
        // Verify that the EAP method menu is not visible.
        assertThat(mView.findViewById(R.id.eap).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void loadCertificates_keyStoreListFail_shouldNotCrash() {
        // Set up
        when(mAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_EAP);
        when(mKeyStore.list(anyString()))
            .thenThrow(new ServiceSpecificException(-1, "permission error"));

        mController = new TestWifiConfigController(mConfigUiBase, mView, mAccessPoint,
              WifiConfigUiBase.MODE_CONNECT);

        // Verify that the EAP method menu is visible.
        assertThat(mView.findViewById(R.id.eap).getVisibility()).isEqualTo(View.VISIBLE);
        // No Crash
    }

    @Test
    public void ssidGetFocus_addNewNetwork_shouldReturnTrue() {
        mController = new TestWifiConfigController(mConfigUiBase, mView, null /* accessPoint */,
                WifiConfigUiBase.MODE_CONNECT);
        final TextView ssid = mView.findViewById(R.id.ssid);
        // Verify ssid text get focus when add new network (accesspoint is null)
        assertThat(ssid.isFocused()).isTrue();
    }

    @Test
    public void passwordGetFocus_connectSecureWifi_shouldReturnTrue() {
        final TextView password = mView.findViewById(R.id.password);
        // Verify password get focus when connect to secure wifi without eap type
        assertThat(password.isFocused()).isTrue();
    }

    @Test
    public void hiddenWarning_warningVisibilityProperlyUpdated() {
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

        mController = new TestWifiConfigController(mConfigUiBase, mView, null /* accessPoint */,
                WifiConfigUiBase.MODE_CONNECT);
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
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWpa3SaeSupported()).thenReturn(saeVisible);
        when(wifiManager.isWpa3SuiteBSupported()).thenReturn(suitebVisible);
        when(wifiManager.isEnhancedOpenSupported()).thenReturn(oweVisible);

        mController = new TestWifiConfigController(mConfigUiBase, mView, null /* accessPoint */,
                WifiConfigUiBase.MODE_MODIFY, wifiManager);

        final Spinner securitySpinner = mView.findViewById(R.id.security);
        final ArrayAdapter<String> adapter = (ArrayAdapter) securitySpinner.getAdapter();
        boolean saeFound = false;
        boolean suitebFound = false;
        boolean oweFound = false;
        for (int i = 0; i < adapter.getCount(); i++) {
            String val = adapter.getItem(i);

            if (val.compareTo(mContext.getString(R.string.wifi_security_sae)) == 0) {
                saeFound = true;
            }

            if (val.compareTo(mContext.getString(R.string.wifi_security_eap_suiteb)) == 0) {
                suitebFound = true;
            }

            if (val.compareTo(mContext.getString(R.string.wifi_security_owe)) == 0) {
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

    public class TestWifiConfigController extends WifiConfigController {

        private TestWifiConfigController(
            WifiConfigUiBase parent, View view, AccessPoint accessPoint, int mode) {
            super(parent, view, accessPoint, mode);
        }

        private TestWifiConfigController(
                WifiConfigUiBase parent, View view, AccessPoint accessPoint, int mode,
                    WifiManager wifiManager) {
            super(parent, view, accessPoint, mode, wifiManager);
        }

        @Override
        boolean isSplitSystemUser() {
            return false;
        }

        @Override
        KeyStore getKeyStore() { return mKeyStore; }
    }

    @Test
    public void loadMacRandomizedValue_shouldPersistentAsDefault() {
        final Spinner privacySetting = mView.findViewById(R.id.privacy_settings);
        final int prefPersist =
                WifiPrivacyPreferenceController.translateMacRandomizedValueToPrefValue(
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
        when(mAccessPoint.isSaved()).thenReturn(true);
        final WifiConfiguration mockWifiConfig = mock(WifiConfiguration.class);
        when(mAccessPoint.getConfig()).thenReturn(mockWifiConfig);
        mockWifiConfig.macRandomizationSetting = macRandomizedValue;
        mController = new TestWifiConfigController(mConfigUiBase, mView, mAccessPoint,
                WifiConfigUiBase.MODE_CONNECT);

        final Spinner privacySetting = mView.findViewById(R.id.privacy_settings);
        final int expectedPrefValue =
                WifiPrivacyPreferenceController.translateMacRandomizedValueToPrefValue(
                        macRandomizedValue);

        assertThat(privacySetting.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(privacySetting.getSelectedItemPosition()).isEqualTo(expectedPrefValue);
    }

    @Test
    public void saveMacRandomizedValue_noChanged_shouldPersistentAsDefault() {
        WifiConfiguration config = mController.getConfig();
        assertThat(config.macRandomizationSetting).isEqualTo(
                WifiConfiguration.RANDOMIZATION_PERSISTENT);
    }

    @Test
    public void saveMacRandomizedValue_ChangedToNone_shouldGetNone() {
        final Spinner privacySetting = mView.findViewById(R.id.privacy_settings);
        final int prefMacNone =
                WifiPrivacyPreferenceController.translateMacRandomizedValueToPrefValue(
                        WifiConfiguration.RANDOMIZATION_NONE);
        privacySetting.setSelection(prefMacNone);

        WifiConfiguration config = mController.getConfig();
        assertThat(config.macRandomizationSetting).isEqualTo(WifiConfiguration.RANDOMIZATION_NONE);
    }

    @Test
    public void selectSecurity_wpa3Eap192bit_eapMethodTls() {
        final WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWpa3SuiteBSupported()).thenReturn(true);
        mController = new TestWifiConfigController(mConfigUiBase, mView, null /* accessPoint */,
                WifiConfigUiBase.MODE_MODIFY, wifiManager);
        final Spinner securitySpinner = mView.findViewById(R.id.security);
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        int wpa3Eap192bitPosition = -1;
        final int securityCount = mController.mSecurityInPosition.length;
        for (int i = 0; i < securityCount; i++) {
            if (mController.mSecurityInPosition[i] != null &&
                    mController.mSecurityInPosition[i] == AccessPoint.SECURITY_EAP_SUITE_B) {
                wpa3Eap192bitPosition = i;
            }
        }

        mController.onItemSelected(securitySpinner, null /* view */, wpa3Eap192bitPosition,
                0 /* id */);

        final int selectedItemPosition = eapMethodSpinner.getSelectedItemPosition();
        assertThat(eapMethodSpinner.getSelectedItem().toString()).isEqualTo("TLS");
    }

    @Test
    public void selectEapMethod_savedAccessPoint_shouldGetCorrectPosition() {
        when(mAccessPoint.isSaved()).thenReturn(true);
        when(mAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_EAP);
        final WifiConfiguration mockWifiConfig = mock(WifiConfiguration.class);
        final WifiEnterpriseConfig mockWifiEnterpriseConfig = mock(WifiEnterpriseConfig.class);
        when(mockWifiEnterpriseConfig.getEapMethod()).thenReturn(Eap.PEAP);
        mockWifiConfig.enterpriseConfig = mockWifiEnterpriseConfig ;
        when(mAccessPoint.getConfig()).thenReturn(mockWifiConfig);
        mController = new TestWifiConfigController(mConfigUiBase, mView, mAccessPoint,
                WifiConfigUiBase.MODE_MODIFY);
        final Spinner eapMethodSpinner = mView.findViewById(R.id.method);
        final Spinner phase2Spinner = mView.findViewById(R.id.phase2);
        WifiConfiguration wifiConfiguration;

        // Test EAP method PEAP
        eapMethodSpinner.setSelection(Eap.PEAP);
        phase2Spinner.setSelection(WifiConfigController.WIFI_PEAP_PHASE2_MSCHAPV2);
        wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getEapMethod()).isEqualTo(Eap.PEAP);
        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(
                Phase2.MSCHAPV2);

        // Test EAP method TTLS
        eapMethodSpinner.setSelection(Eap.TTLS);
        phase2Spinner.setSelection(WifiConfigController.WIFI_TTLS_PHASE2_MSCHAPV2);
        wifiConfiguration = mController.getConfig();

        assertThat(wifiConfiguration.enterpriseConfig.getEapMethod()).isEqualTo(Eap.TTLS);
        assertThat(wifiConfiguration.enterpriseConfig.getPhase2Method()).isEqualTo(
                Phase2.MSCHAPV2);
    }
}
