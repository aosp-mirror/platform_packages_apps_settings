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
import android.os.ServiceSpecificException;
import android.security.KeyStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
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

    public class TestWifiConfigController extends WifiConfigController {

        private TestWifiConfigController(
            WifiConfigUiBase parent, View view, AccessPoint accessPoint, int mode) {
            super(parent, view, accessPoint, mode);
        }

        @Override
        boolean isSplitSystemUser() {
            return false;
        }

        @Override
        KeyStore getKeyStore() { return mKeyStore; }
    }
}
