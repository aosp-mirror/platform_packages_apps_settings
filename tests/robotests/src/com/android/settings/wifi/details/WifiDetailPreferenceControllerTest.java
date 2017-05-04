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
package com.android.settings.wifi.details;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkBadging;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.RouteInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.wifi.WifiDetailPreference;
import com.android.settings.vpn2.ConnectivityManagerWrapper;
import com.android.settings.vpn2.ConnectivityManagerWrapperImpl;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiDetailPreferenceControllerTest {

    private static final int LEVEL = 1;
    private static final int RSSI = -55;
    private static final int LINK_SPEED = 123;
    private static final String MAC_ADDRESS = WifiInfo.DEFAULT_MAC_ADDRESS;
    private static final String SECURITY = "None";

    private InetAddress mIpv4Address;
    private Inet6Address mIpv6Address;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mockScreen;

    @Mock private AccessPoint mockAccessPoint;
    @Mock private Activity mockActivity;
    @Mock private ConnectivityManager mockConnectivityManager;
    @Mock private ConnectivityManagerWrapperImpl mockConnectivityManagerWrapper;
    @Mock private Network mockNetwork;
    @Mock private NetworkInfo mockNetworkInfo;
    @Mock private WifiConfiguration mockWifiConfig;
    @Mock private WifiInfo mockWifiInfo;
    @Mock private WifiNetworkDetailsFragment mockFragment;
    @Mock private WifiManager mockWifiManager;

    @Mock private Preference mockConnectionDetailPref;
    @Mock private LayoutPreference mockButtonsPref;
    @Mock private Button mockSignInButton;
    @Mock private WifiDetailPreference mockSignalStrengthPref;
    @Mock private WifiDetailPreference mockLinkSpeedPref;
    @Mock private WifiDetailPreference mockFrequencyPref;
    @Mock private WifiDetailPreference mockSecurityPref;
    @Mock private WifiDetailPreference mockMacAddressPref;
    @Mock private WifiDetailPreference mockIpAddressPref;
    @Mock private WifiDetailPreference mockGatewayPref;
    @Mock private WifiDetailPreference mockSubnetPref;
    @Mock private WifiDetailPreference mockDnsPref;
    @Mock private PreferenceCategory mockIpv6AddressCategory;

    private ArgumentCaptor<NetworkCallback> mCallbackCaptor;
    private Context mContext = RuntimeEnvironment.application;
    private Lifecycle mLifecycle;
    private LinkProperties mLinkProperties;
    private WifiDetailPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mLifecycle = new Lifecycle();

        try {
            mIpv4Address = InetAddress.getByAddress(
                    new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 });
            mIpv6Address = Inet6Address.getByAddress(
                    "123", /* host */
                    new byte[] {
                            (byte) 0xFE, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0x02, 0x11, 0x25,
                            (byte) 0xFF, (byte) 0xFE, (byte) 0xF8, (byte) 0x7C, (byte) 0xB2},
                    1  /*scope id */);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        mCallbackCaptor = ArgumentCaptor.forClass(NetworkCallback.class);

        when(mockAccessPoint.getConfig()).thenReturn(mockWifiConfig);
        when(mockAccessPoint.getLevel()).thenReturn(LEVEL);
        when(mockAccessPoint.getNetworkInfo()).thenReturn(mockNetworkInfo);
        when(mockAccessPoint.getRssi()).thenReturn(RSSI);
        when(mockAccessPoint.getSecurityString(false)).thenReturn(SECURITY);

        when(mockConnectivityManagerWrapper.getConnectivityManager())
                .thenReturn(mockConnectivityManager);
        when(mockConnectivityManager.getNetworkInfo(any(Network.class)))
                .thenReturn(mockNetworkInfo);
        doNothing().when(mockConnectivityManagerWrapper).registerNetworkCallback(
                any(NetworkRequest.class), mCallbackCaptor.capture(), any(Handler.class));

        when(mockWifiInfo.getLinkSpeed()).thenReturn(LINK_SPEED);
        when(mockWifiInfo.getRssi()).thenReturn(RSSI);
        when(mockWifiInfo.getMacAddress()).thenReturn(MAC_ADDRESS);
        when(mockWifiManager.getConnectionInfo()).thenReturn(mockWifiInfo);

        when(mockWifiManager.getCurrentNetwork()).thenReturn(mockNetwork);
        mLinkProperties = new LinkProperties();
        when(mockConnectivityManager.getLinkProperties(mockNetwork)).thenReturn(mLinkProperties);

        when(mockFragment.getActivity()).thenReturn(mockActivity);

        mController = newWifiDetailPreferenceController();

        setupMockedPreferenceScreen();
    }

    private WifiDetailPreferenceController newWifiDetailPreferenceController() {
        return new WifiDetailPreferenceController(
                mockAccessPoint,
                mockConnectivityManagerWrapper,
                mContext,
                mockFragment,
                null,  // Handler
                mLifecycle,
                mockWifiManager);
    }

    private void setupMockedPreferenceScreen() {
        when(mockScreen.getPreferenceManager().getContext()).thenReturn(mContext);

        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_CONNECTION_DETAIL_PREF))
                .thenReturn(mockConnectionDetailPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_BUTTONS_PREF))
                .thenReturn(mockButtonsPref);
        when(mockButtonsPref.findViewById(R.id.right_button))
                .thenReturn(mockSignInButton);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_SIGNAL_STRENGTH_PREF))
                .thenReturn(mockSignalStrengthPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_LINK_SPEED))
                .thenReturn(mockLinkSpeedPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_FREQUENCY_PREF))
                .thenReturn(mockFrequencyPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_SECURITY_PREF))
                .thenReturn(mockSecurityPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_MAC_ADDRESS_PREF))
                .thenReturn(mockMacAddressPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_IP_ADDRESS_PREF))
                .thenReturn(mockIpAddressPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_GATEWAY_PREF))
                .thenReturn(mockGatewayPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_SUBNET_MASK_PREF))
                .thenReturn(mockSubnetPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_DNS_PREF))
                .thenReturn(mockDnsPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_IPV6_ADDRESS_CATEGORY))
                .thenReturn(mockIpv6AddressCategory);

        mController.displayPreference(mockScreen);
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void securityPreference_stringShouldBeSet() {
        verify(mockSecurityPref).setDetailText(SECURITY);
    }

    @Test
    public void latestWifiInfo_shouldBeFetchedOnResume() {
        mController.onResume();

        verify(mockWifiManager, times(1)).getConnectionInfo();
    }

    @Test
    public void latestNetworkInfo_shouldBeFetchedOnResume() {
        mController.onResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
    }

    @Test
    public void networkCallback_shouldBeRegisteredOnResume() {
        mController.onResume();

        verify(mockConnectivityManagerWrapper, times(1)).registerNetworkCallback(
                any(NetworkRequest.class), mCallbackCaptor.capture(), any(Handler.class));
    }

    @Test
    public void networkCallback_shouldBeUnregisteredOnPause() {
        mController.onResume();
        mController.onPause();

        verify(mockConnectivityManager, times(1)).unregisterNetworkCallback(
                mCallbackCaptor.getValue());
    }

    @Test
    public void connectionDetailPref_shouldHaveIconSet() {
        Drawable expectedIcon =
                NetworkBadging.getWifiIcon(LEVEL, NetworkBadging.BADGING_NONE, mContext.getTheme());

        mController.onResume();

        verify(mockConnectionDetailPref).setIcon(expectedIcon);
    }

    @Test
    public void connectionDetailPref_shouldHaveTitleSet() {
        String summary = "summary";
        when(mockAccessPoint.getSettingsSummary()).thenReturn(summary);

        mController.onResume();

        verify(mockConnectionDetailPref).setTitle(summary);
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSet() {
        mController.onResume();

        verify(mockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSet() {
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        mController.onResume();

        verify(mockSignalStrengthPref).setDetailText(expectedStrength);
    }

    @Test
    public void linkSpeedPref_shouldHaveDetailTextSet() {
        String expectedLinkSpeed = mContext.getString(R.string.link_speed, LINK_SPEED);

        mController.onResume();

        verify(mockLinkSpeedPref).setDetailText(expectedLinkSpeed);
    }

    @Test
    public void linkSpeedPref_shouldNotShowIfNotSet() {
        when(mockWifiInfo.getLinkSpeed()).thenReturn(-1);

        mController.onResume();

        verify(mockLinkSpeedPref).setVisible(false);
    }

    @Test
    public void macAddressPref_shouldHaveDetailTextSet() {
        mController.onResume();

        verify(mockMacAddressPref).setDetailText(MAC_ADDRESS);
    }

    @Test
    public void ipAddressPref_shouldHaveDetailTextSet() {
        LinkAddress ipv4Address = new LinkAddress(mIpv4Address, 32);

        mLinkProperties.addLinkAddress(ipv4Address);

        mController.onResume();

        verify(mockIpAddressPref).setDetailText(mIpv4Address.getHostAddress());
    }

    @Test
    public void gatewayAndSubnet_shouldHaveDetailTextSet() {
        int prefixLength = 24;
        IpPrefix subnet = new IpPrefix(mIpv4Address, prefixLength);
        InetAddress gateway = mIpv4Address;
        mLinkProperties.addRoute(new RouteInfo(subnet, gateway));

        mController.onResume();

        verify(mockSubnetPref).setDetailText("255.255.255.0");
        verify(mockGatewayPref).setDetailText(mIpv4Address.getHostAddress());
    }

    @Test
    public void dnsServersPref_shouldHaveDetailTextSet() throws UnknownHostException {
        mLinkProperties.addDnsServer(InetAddress.getByAddress(new byte[]{8,8,4,4}));
        mLinkProperties.addDnsServer(InetAddress.getByAddress(new byte[]{8,8,8,8}));

        mController.onResume();

        verify(mockDnsPref).setDetailText("8.8.4.4,8.8.8.8");
    }

    @Test
    public void noCurrentNetwork_shouldFinishActivity() {
        // If WifiManager#getCurrentNetwork() returns null, then the network is neither connected
        // nor connecting and WifiStateMachine has not reached L2ConnectedState.
        when(mockWifiManager.getCurrentNetwork()).thenReturn(null);

        mController.onResume();

        verify(mockActivity).finish();
    }

    @Test
    public void noLinkProperties_allIpDetailsHidden() {
        when(mockConnectivityManager.getLinkProperties(mockNetwork)).thenReturn(null);
        reset(mockIpv6AddressCategory, mockIpAddressPref, mockSubnetPref, mockGatewayPref,
                mockDnsPref);

        mController.onResume();

        verify(mockIpv6AddressCategory).setVisible(false);
        verify(mockIpAddressPref).setVisible(false);
        verify(mockSubnetPref).setVisible(false);
        verify(mockGatewayPref).setVisible(false);
        verify(mockDnsPref).setVisible(false);
        verify(mockIpv6AddressCategory, never()).setVisible(true);
        verify(mockIpAddressPref, never()).setVisible(true);
        verify(mockSubnetPref, never()).setVisible(true);
        verify(mockGatewayPref, never()).setVisible(true);
        verify(mockDnsPref, never()).setVisible(true);
    }

    @Test
    public void canForgetNetwork_noNetwork() {
        when(mockAccessPoint.getConfig()).thenReturn(null);
        mController = newWifiDetailPreferenceController();
        mController.displayPreference(mockScreen);
        mController.onResume();

        assertThat(mController.canForgetNetwork()).isFalse();
    }

    @Test
    public void canForgetNetwork_ephemeral() {
        when(mockWifiInfo.isEphemeral()).thenReturn(true);
        when(mockAccessPoint.getConfig()).thenReturn(null);
        mController = newWifiDetailPreferenceController();
        mController.displayPreference(mockScreen);
        mController.onResume();

        assertThat(mController.canForgetNetwork()).isTrue();
    }

    @Test
    public void canForgetNetwork_saved() {
        assertThat(mController.canForgetNetwork()).isTrue();
    }

    @Test
    public void forgetNetwork_ephemeral() {
        String ssid = "ssid";
        when(mockWifiInfo.isEphemeral()).thenReturn(true);
        when(mockWifiInfo.getSSID()).thenReturn(ssid);

        mController.onResume();

        mController.forgetNetwork();

        verify(mockWifiManager).disableEphemeralNetwork(ssid);
    }

    @Test
    public void forgetNetwork_saved() {
        mockWifiConfig.networkId = 5;

        mController.forgetNetwork();

        verify(mockWifiManager).forget(mockWifiConfig.networkId, null);
    }

    @Test
    public void networkStateChangedIntent_shouldRefetchInfo() {
        mController.onResume();
        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void rssiChangedIntent_shouldRefetchInfo() {
        mController.onResume();
        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void networkDisconnectdState_shouldFinishActivity() {
        mController.onResume();

        when(mockConnectivityManager.getNetworkInfo(any(Network.class))).thenReturn(null);
        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mockActivity).finish();
    }

    @Test
    public void networkOnLost_shouldFinishActivity() {
        mController.onResume();

        mCallbackCaptor.getValue().onLost(mockNetwork);

        verify(mockActivity).finish();
    }

    @Test
    public void ipv6AddressPref_shouldHaveHostAddressTextSet() {
        LinkAddress ipv6Address = new LinkAddress(mIpv6Address, 128);

        mLinkProperties.addLinkAddress(ipv6Address);

        mController.onResume();

        ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mockIpv6AddressCategory).addPreference(preferenceCaptor.capture());
        assertThat(preferenceCaptor.getValue().getTitle()).isEqualTo(mIpv6Address.getHostAddress());
    }

    @Test
    public void ipv6AddressPref_shouldNotBeSelectable() {
        LinkAddress ipv6Address = new LinkAddress(mIpv6Address, 128);

        mLinkProperties.addLinkAddress(ipv6Address);

        mController.onResume();

        ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mockIpv6AddressCategory).addPreference(preferenceCaptor.capture());
        assertThat(preferenceCaptor.getValue().isSelectable()).isFalse();
    }

    @Test
    public void captivePortal_shouldShowSignInButton() {
        reset(mockSignInButton);
        InOrder inOrder = inOrder(mockSignInButton);
        mController.onResume();
        inOrder.verify(mockSignInButton).setVisibility(View.INVISIBLE);

        NetworkCapabilities nc = new NetworkCapabilities();
        nc.clearAll();
        nc.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        NetworkCallback callback = mCallbackCaptor.getValue();
        callback.onCapabilitiesChanged(mockNetwork, nc);
        inOrder.verify(mockSignInButton).setVisibility(View.INVISIBLE);

        nc = new NetworkCapabilities(nc);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        callback.onCapabilitiesChanged(mockNetwork, nc);
        inOrder.verify(mockSignInButton).setVisibility(View.VISIBLE);

        nc = new NetworkCapabilities(nc);
        nc.removeCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        callback.onCapabilitiesChanged(mockNetwork, nc);
        inOrder.verify(mockSignInButton).setVisibility(View.INVISIBLE);
    }

    @Test
    public void testSignInButton_shouldStartCaptivePortalApp() {
        mController.onResume();

        ArgumentCaptor<OnClickListener> captor = ArgumentCaptor.forClass(OnClickListener.class);
        verify(mockSignInButton).setOnClickListener(captor.capture());
        captor.getValue().onClick(mockSignInButton);
        verify(mockConnectivityManagerWrapper).startCaptivePortalApp(mockNetwork);
    }
}
