package com.android.settings.wifi.tether;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiTetherSecurityPreferenceControllerTest {

    private static final String WPA2_PSK = String.valueOf(WifiConfiguration.KeyMgmt.WPA2_PSK);
    private static final String NONE = String.valueOf(WifiConfiguration.KeyMgmt.NONE);
    @Mock
    private WifiTetherBasePreferenceController.OnTetherConfigUpdateListener mListener;
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    private WifiTetherSecurityPreferenceController mController;
    private ListPreference mPreference;
    private WifiConfiguration mConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mConfig = new WifiConfiguration();
        mConfig.SSID = "test_1234";
        mConfig.preSharedKey = "test_password";
        mConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA2_PSK);
        mContext = spy(RuntimeEnvironment.application);

        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getWifiApConfiguration()).thenReturn(mConfig);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = new WifiTetherSecurityPreferenceController(mContext, mListener);
        mPreference = new ListPreference(RuntimeEnvironment.application);
        mController.mPreference = mPreference;
    }

    @Test
    public void onPreferenceChange_securityValueUpdated() {
        mController.onPreferenceChange(mPreference, WPA2_PSK);
        assertThat(mController.getSecurityType()).isEqualTo(WifiConfiguration.KeyMgmt.WPA2_PSK);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2-Personal");

        mController.onPreferenceChange(mPreference, NONE);
        assertThat(mController.getSecurityType()).isEqualTo(WifiConfiguration.KeyMgmt.NONE);
        assertThat(mPreference.getSummary().toString()).isEqualTo("None");
    }

    @Test
    public void updateDisplay_preferenceUpdated() {
        // test defaulting to WPA2-Personal on new config
        when(mWifiManager.getWifiApConfiguration()).thenReturn(null);
        mController.updateDisplay();
        assertThat(mController.getSecurityType()).isEqualTo(WifiConfiguration.KeyMgmt.WPA2_PSK);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2-Personal");

        // test open tether network
        when(mWifiManager.getWifiApConfiguration()).thenReturn(mConfig);
        mConfig.allowedKeyManagement.clear();
        mConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        mController.updateDisplay();
        assertThat(mController.getSecurityType()).isEqualTo(WifiConfiguration.KeyMgmt.NONE);
        assertThat(mPreference.getSummary().toString()).isEqualTo("None");

        // test WPA2-Personal tether network
        mConfig.allowedKeyManagement.clear();
        mConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA2_PSK);
        mController.updateDisplay();
        assertThat(mController.getSecurityType()).isEqualTo(WifiConfiguration.KeyMgmt.WPA2_PSK);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2-Personal");
    }
}
