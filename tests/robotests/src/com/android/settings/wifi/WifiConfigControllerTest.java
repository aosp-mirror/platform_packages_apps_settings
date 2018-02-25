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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = ShadowConnectivityManager.class)
public class WifiConfigControllerTest {

    @Mock
    private WifiConfigUiBase mConfigUiBase;
    @Mock
    private Context mContext;
    @Mock
    private View mView;
    @Mock
    private AccessPoint mAccessPoint;

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
    private static final int DHCP = 0;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mConfigUiBase.getContext()).thenReturn(mContext);
        when(mAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_PSK);
        mView = LayoutInflater.from(mContext).inflate(R.layout.wifi_dialog, null);
        final Spinner ipSettingsSpinner = mView.findViewById(R.id.ip_settings);
        ipSettingsSpinner.setSelection(DHCP);

        mController = new TestWifiConfigController(mConfigUiBase, mView, mAccessPoint,
                WifiConfigUiBase.MODE_CONNECT);
    }

    @Test
    public void ssidExceeds32Bytes_shouldShowSsidTooLongWarning() {
        mController = new TestWifiConfigController(mConfigUiBase, mView, null /* accessPoint */,
                WifiConfigUiBase.MODE_CONNECT);
        final TextView ssid = mView.findViewById(R.id.ssid);
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
        ssid.setText("");
        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_longPsk_shouldReturnFalse() {
        final TextView password = mView.findViewById(R.id.password);
        password.setText(LONG_PSK);
        assertThat(mController.isSubmittable()).isFalse();

    }

    @Test
    public void isSubmittable_shortPsk_shouldReturnFalse() {
        final TextView password = mView.findViewById(R.id.password);
        password.setText(SHORT_PSK);
        assertThat(mController.isSubmittable()).isFalse();
    }

    @Test
    public void isSubmittable_goodPsk_shouldReturnTrue() {
        final TextView password = mView.findViewById(R.id.password);
        password.setText(GOOD_PSK);
        assertThat(mController.isSubmittable()).isTrue();

    }

    @Test
    public void isSubmittable_hexPsk_shouldReturnTrue() {
        final TextView password = mView.findViewById(R.id.password);
        password.setText(HEX_PSK);
        assertThat(mController.isSubmittable()).isTrue();

    }

    @Test
    public void isSubmittable_savedConfigZeroLengthPassword_shouldReturnTrue() {
        final TextView password = mView.findViewById(R.id.password);
        password.setText("");
        when(mAccessPoint.isSaved()).thenReturn(true);
        assertThat(mController.isSubmittable()).isTrue();
    }

    @Test
    public void isSubmittable_nullAccessPoint_noException() {
        mController = new TestWifiConfigController(mConfigUiBase, mView, null,
                WifiConfigUiBase.MODE_CONNECT);
        mController.isSubmittable();
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

    public class TestWifiConfigController extends WifiConfigController {

        public TestWifiConfigController(WifiConfigUiBase parent, View view,
                AccessPoint accessPoint, int mode) {
            super(parent, view, accessPoint, mode);
        }

        @Override
        boolean isSplitSystemUser() {
            return false;
        }
    }
}
