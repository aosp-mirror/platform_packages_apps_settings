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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.RouteInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.ActionButtonsPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDevicePolicyManager.class, ShadowEntityHeaderController.class})
public class WifiDetailPreferenceControllerTest {

    private static final int LEVEL = 1;
    private static final int RSSI = -55;
    private static final int TX_LINK_SPEED = 123;
    private static final int RX_LINK_SPEED = 54;
    private static final String MAC_ADDRESS = WifiInfo.DEFAULT_MAC_ADDRESS;
    private static final String SECURITY = "None";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mockScreen;

    @Mock
    private AccessPoint mockAccessPoint;
    @Mock
    private FragmentActivity mockActivity;
    @Mock
    private ConnectivityManager mockConnectivityManager;
    @Mock
    private Network mockNetwork;
    @Mock
    private NetworkInfo mockNetworkInfo;
    @Mock
    private WifiConfiguration mockWifiConfig;
    @Mock
    private WifiInfo mockWifiInfo;
    @Mock
    private WifiNetworkDetailsFragment mockFragment;
    @Mock
    private WifiManager mockWifiManager;
    @Mock
    private MetricsFeatureProvider mockMetricsFeatureProvider;
    @Mock
    private WifiDetailPreferenceController.IconInjector mockIconInjector;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityHeaderController mockHeaderController;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LayoutPreference mockHeaderLayoutPreference;
    @Mock
    private ImageView mockHeaderIcon;

    @Mock
    private ActionButtonsPreference mockButtonsPref;
    @Mock
    private Preference mockSignalStrengthPref;
    @Mock
    private Preference mockTxLinkSpeedPref;
    @Mock
    private Preference mockRxLinkSpeedPref;
    @Mock
    private Preference mockFrequencyPref;
    @Mock
    private Preference mockSecurityPref;
    @Mock
    private Preference mockMacAddressPref;
    @Mock
    private Preference mockIpAddressPref;
    @Mock
    private Preference mockGatewayPref;
    @Mock
    private Preference mockSubnetPref;
    @Mock
    private Preference mockDnsPref;
    @Mock
    private PreferenceCategory mockIpv6Category;
    @Mock
    private Preference mockIpv6AddressesPref;
    @Mock
    private PackageManager mockPackageManager;

    @Captor
    private ArgumentCaptor<NetworkCallback> mCallbackCaptor;
    @Captor
    private ArgumentCaptor<View.OnClickListener> mForgetClickListener;

    private Context mContext;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private LinkProperties mLinkProperties;
    private WifiDetailPreferenceController mController;

    // This class exists so that these values can be made static final. They can't be static final
    // members of the test class, because any attempt to call IpPrefix or RouteInfo constructors
    // during static initialization of the test class results in NoSuchMethorError being thrown
    // when the test is run.
    private static class Constants {
        static final int IPV4_PREFIXLEN = 25;
        static final LinkAddress IPV4_ADDR;
        static final Inet4Address IPV4_GATEWAY;
        static final RouteInfo IPV4_DEFAULT;
        static final RouteInfo IPV4_SUBNET;
        static final LinkAddress IPV6_LINKLOCAL;
        static final LinkAddress IPV6_GLOBAL1;
        static final LinkAddress IPV6_GLOBAL2;
        static final InetAddress IPV4_DNS1;
        static final InetAddress IPV4_DNS2;
        static final InetAddress IPV6_DNS;

        private static LinkAddress ipv6LinkAddress(String addr) throws UnknownHostException {
            return new LinkAddress(InetAddress.getByName(addr), 64);
        }

        private static LinkAddress ipv4LinkAddress(String addr, int prefixlen)
                throws UnknownHostException {
            return new LinkAddress(InetAddress.getByName(addr), prefixlen);
        }

        static {
            try {
                // We create our test constants in these roundabout ways because the robolectric
                // shadows don't contain NetworkUtils.parseNumericAddress and other utility methods,
                // so the easy ways to do things fail with NoSuchMethodError.
                IPV4_ADDR = ipv4LinkAddress("192.0.2.2", IPV4_PREFIXLEN);
                IPV4_GATEWAY = (Inet4Address) InetAddress.getByName("192.0.2.127");

                final Inet4Address any4 = (Inet4Address) InetAddress.getByName("0.0.0.0");
                IpPrefix subnet = new IpPrefix(IPV4_ADDR.getAddress(), IPV4_PREFIXLEN);
                IPV4_SUBNET = new RouteInfo(subnet, any4);
                IPV4_DEFAULT = new RouteInfo(new IpPrefix(any4, 0), IPV4_GATEWAY);

                IPV6_LINKLOCAL = ipv6LinkAddress("fe80::211:25ff:fef8:7cb2%1");
                IPV6_GLOBAL1 = ipv6LinkAddress("2001:db8:1::211:25ff:fef8:7cb2");
                IPV6_GLOBAL2 = ipv6LinkAddress("2001:db8:1::3dfe:8902:f98f:739d");

                IPV4_DNS1 = InetAddress.getByName("8.8.8.8");
                IPV4_DNS2 = InetAddress.getByName("8.8.4.4");
                IPV6_DNS = InetAddress.getByName("2001:4860:4860::64");
            } catch (UnknownHostException e) {
                throw new RuntimeException("Invalid hardcoded IP addresss: " + e);
            }
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);

        when(mContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mockAccessPoint.getConfig()).thenReturn(mockWifiConfig);
        when(mockAccessPoint.getLevel()).thenReturn(LEVEL);
        when(mockAccessPoint.getSecurityString(false)).thenReturn(SECURITY);
        when(mockConnectivityManager.getNetworkInfo(any(Network.class)))
                .thenReturn(mockNetworkInfo);
        doNothing().when(mockConnectivityManager).registerNetworkCallback(
                nullable(NetworkRequest.class), mCallbackCaptor.capture(), nullable(Handler.class));
        mockButtonsPref = createMock();
        when(mockButtonsPref.setButton1OnClickListener(mForgetClickListener.capture()))
                .thenReturn(mockButtonsPref);

        when(mockWifiInfo.getTxLinkSpeedMbps()).thenReturn(TX_LINK_SPEED);
        when(mockWifiInfo.getRxLinkSpeedMbps()).thenReturn(RX_LINK_SPEED);
        when(mockWifiInfo.getRssi()).thenReturn(RSSI);
        when(mockWifiInfo.getMacAddress()).thenReturn(MAC_ADDRESS);
        when(mockWifiManager.getConnectionInfo()).thenReturn(mockWifiInfo);

        when(mockWifiManager.getCurrentNetwork()).thenReturn(mockNetwork);
        mLinkProperties = new LinkProperties();
        when(mockConnectivityManager.getLinkProperties(mockNetwork)).thenReturn(mLinkProperties);

        when(mockFragment.getActivity()).thenReturn(mockActivity);

        ShadowEntityHeaderController.setUseMock(mockHeaderController);
        // builder pattern
        when(mockHeaderController.setRecyclerView(mockFragment.getListView(), mLifecycle))
                .thenReturn(mockHeaderController);
        when(mockHeaderController.setSummary(anyString())).thenReturn(mockHeaderController);
        when(mockIconInjector.getIcon(anyInt())).thenReturn(new ColorDrawable());

        setupMockedPreferenceScreen();
        mController = newWifiDetailPreferenceController();
    }

    private WifiDetailPreferenceController newWifiDetailPreferenceController() {
        return new WifiDetailPreferenceController(
                mockAccessPoint,
                mockConnectivityManager,
                mContext,
                mockFragment,
                null,  // Handler
                mLifecycle,
                mockWifiManager,
                mockMetricsFeatureProvider,
                mockIconInjector);
    }

    private void setupMockedPreferenceScreen() {
        when(mockScreen.getPreferenceManager().getContext()).thenReturn(mContext);

        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_HEADER))
                .thenReturn(mockHeaderLayoutPreference);
        when(mockHeaderLayoutPreference.findViewById(R.id.entity_header_icon))
                .thenReturn(mockHeaderIcon);

        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_BUTTONS_PREF))
                .thenReturn(mockButtonsPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_SIGNAL_STRENGTH_PREF))
                .thenReturn(mockSignalStrengthPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_TX_LINK_SPEED))
                .thenReturn(mockTxLinkSpeedPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_RX_LINK_SPEED))
                .thenReturn(mockRxLinkSpeedPref);
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
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_IPV6_CATEGORY))
                .thenReturn(mockIpv6Category);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_IPV6_ADDRESSES_PREF))
                .thenReturn(mockIpv6AddressesPref);
    }

    private void displayAndResume() {
        mController.displayPreference(mockScreen);
        mController.onResume();
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        mController.displayPreference(mockScreen);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void securityPreference_stringShouldBeSet() {
        displayAndResume();

        verify(mockSecurityPref).setSummary(SECURITY);
    }

    @Test
    public void latestWifiInfo_shouldBeFetchedInDisplayPreference() {
        displayAndResume();

        verify(mockWifiManager, times(1)).getConnectionInfo();
    }

    @Test
    public void latestNetworkInfo_shouldBeFetchedInDisplayPreference() {
        displayAndResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
    }

    @Test
    public void networkCallback_shouldBeRegisteredOnResume() {
        displayAndResume();

        verify(mockConnectivityManager, times(1)).registerNetworkCallback(
                nullable(NetworkRequest.class), mCallbackCaptor.capture(), nullable(Handler.class));
    }

    @Test
    public void networkCallback_shouldBeUnregisteredOnPause() {
        displayAndResume();
        mController.onPause();

        verify(mockConnectivityManager, times(1))
                .unregisterNetworkCallback(mCallbackCaptor.getValue());
    }

    @Test
    public void entityHeader_shouldHaveIconSet() {
        Drawable expectedIcon = mockIconInjector.getIcon(LEVEL);

        displayAndResume();

        verify(mockHeaderController).setIcon(expectedIcon);
    }

    @Test
    public void entityHeader_shouldHaveLabelSetToSsid() {
        String label = "ssid";
        when(mockAccessPoint.getSsidStr()).thenReturn(label);

        displayAndResume();

        verify(mockHeaderController).setLabel(label);
    }

    @Test
    public void entityHeader_shouldHaveSummarySet() {
        String summary = "summary";
        when(mockAccessPoint.getSettingsSummary()).thenReturn(summary);

        displayAndResume();

        verify(mockHeaderController).setSummary(summary);
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSet() {
        displayAndResume();

        verify(mockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSet() {
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        displayAndResume();

        verify(mockSignalStrengthPref).setSummary(expectedStrength);
    }

    @Test
    public void linkSpeedPref_shouldHaveDetailTextSet() {
        String expectedLinkSpeed = mContext.getString(R.string.tx_link_speed, TX_LINK_SPEED);

        displayAndResume();

        verify(mockTxLinkSpeedPref).setSummary(expectedLinkSpeed);
    }

    @Test
    public void linkSpeedPref_shouldNotShowIfNotSet() {
        when(mockWifiInfo.getTxLinkSpeedMbps()).thenReturn(-1);

        displayAndResume();

        verify(mockTxLinkSpeedPref).setVisible(false);
    }

    @Test
    public void rxLinkSpeedPref_shouldHaveDetailTextSet() {
        String expectedLinkSpeed = mContext.getString(R.string.rx_link_speed, RX_LINK_SPEED);

        displayAndResume();

        verify(mockRxLinkSpeedPref).setSummary(expectedLinkSpeed);
    }

    @Test
    public void rxLinkSpeedPref_shouldNotShowIfNotSet() {
        when(mockWifiInfo.getRxLinkSpeedMbps()).thenReturn(-1);

        displayAndResume();

        verify(mockRxLinkSpeedPref).setVisible(false);
    }

    @Test
    public void macAddressPref_shouldHaveDetailTextSet() {
        displayAndResume();

        verify(mockMacAddressPref).setSummary(MAC_ADDRESS);
    }

    @Test
    public void ipAddressPref_shouldHaveDetailTextSet() {
        mLinkProperties.addLinkAddress(Constants.IPV4_ADDR);

        displayAndResume();

        verify(mockIpAddressPref).setSummary(Constants.IPV4_ADDR.getAddress().getHostAddress());
    }

    @Test
    public void gatewayAndSubnet_shouldHaveDetailTextSet() {
        mLinkProperties.addLinkAddress(Constants.IPV4_ADDR);
        mLinkProperties.addRoute(Constants.IPV4_DEFAULT);
        mLinkProperties.addRoute(Constants.IPV4_SUBNET);

        displayAndResume();

        verify(mockSubnetPref).setSummary("255.255.255.128");
        verify(mockGatewayPref).setSummary("192.0.2.127");
    }

    @Test
    public void dnsServersPref_shouldHaveDetailTextSet() throws UnknownHostException {
        mLinkProperties.addDnsServer(InetAddress.getByAddress(new byte[] {8, 8, 4, 4}));
        mLinkProperties.addDnsServer(InetAddress.getByAddress(new byte[] {8, 8, 8, 8}));
        mLinkProperties.addDnsServer(Constants.IPV6_DNS);

        displayAndResume();

        verify(mockDnsPref).setSummary(
                "8.8.4.4\n" +
                        "8.8.8.8\n" +
                        Constants.IPV6_DNS.getHostAddress());
    }

    @Test
    public void noCurrentNetwork_shouldFinishActivity() {
        // If WifiManager#getCurrentNetwork() returns null, then the network is neither connected
        // nor connecting and WifiStateMachine has not reached L2ConnectedState.
        when(mockWifiManager.getCurrentNetwork()).thenReturn(null);

        displayAndResume();

        verify(mockActivity).finish();
    }

    @Test
    public void noLinkProperties_allIpDetailsHidden() {
        when(mockConnectivityManager.getLinkProperties(mockNetwork)).thenReturn(null);
        reset(mockIpv6Category, mockIpAddressPref, mockSubnetPref, mockGatewayPref, mockDnsPref);

        displayAndResume();

        verify(mockIpv6Category).setVisible(false);
        verify(mockIpAddressPref).setVisible(false);
        verify(mockSubnetPref).setVisible(false);
        verify(mockGatewayPref).setVisible(false);
        verify(mockDnsPref).setVisible(false);
        verify(mockIpv6Category, never()).setVisible(true);
        verify(mockIpAddressPref, never()).setVisible(true);
        verify(mockSubnetPref, never()).setVisible(true);
        verify(mockGatewayPref, never()).setVisible(true);
        verify(mockDnsPref, never()).setVisible(true);
    }

    // Convenience method to convert a LinkAddress to a string without a prefix length.
    private String asString(LinkAddress l) {
        return l.getAddress().getHostAddress();
    }

    // Pretend that the NetworkCallback was triggered with a new copy of lp. We need to create a
    // new copy because the code only updates if !mLinkProperties.equals(lp).
    private void updateLinkProperties(LinkProperties lp) {
        mCallbackCaptor.getValue().onLinkPropertiesChanged(mockNetwork, new LinkProperties(lp));
    }

    private void updateNetworkCapabilities(NetworkCapabilities nc) {
        mCallbackCaptor.getValue().onCapabilitiesChanged(mockNetwork, new NetworkCapabilities(nc));
    }

    private NetworkCapabilities makeNetworkCapabilities() {
        NetworkCapabilities nc = new NetworkCapabilities();
        nc.clearAll();
        nc.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        return nc;
    }

    private void verifyDisplayedIpv6Addresses(InOrder inOrder, LinkAddress... addresses) {
        String text = Arrays.stream(addresses)
                .map(address -> asString(address))
                .collect(Collectors.joining("\n"));
        inOrder.verify(mockIpv6AddressesPref).setSummary(text);
    }

    @Test
    public void onLinkPropertiesChanged_updatesFields() {
        displayAndResume();

        InOrder inOrder = inOrder(mockIpAddressPref, mockGatewayPref, mockSubnetPref,
                mockDnsPref, mockIpv6Category, mockIpv6AddressesPref);

        LinkProperties lp = new LinkProperties();

        lp.addLinkAddress(Constants.IPV6_LINKLOCAL);
        updateLinkProperties(lp);
        verifyDisplayedIpv6Addresses(inOrder, Constants.IPV6_LINKLOCAL);
        inOrder.verify(mockIpv6Category).setVisible(true);

        lp.addRoute(Constants.IPV4_DEFAULT);
        updateLinkProperties(lp);
        inOrder.verify(mockGatewayPref).setSummary(Constants.IPV4_GATEWAY.getHostAddress());
        inOrder.verify(mockGatewayPref).setVisible(true);

        lp.addLinkAddress(Constants.IPV4_ADDR);
        lp.addRoute(Constants.IPV4_SUBNET);
        updateLinkProperties(lp);
        inOrder.verify(mockIpAddressPref).setSummary(asString(Constants.IPV4_ADDR));
        inOrder.verify(mockIpAddressPref).setVisible(true);
        inOrder.verify(mockSubnetPref).setSummary("255.255.255.128");
        inOrder.verify(mockSubnetPref).setVisible(true);

        lp.addLinkAddress(Constants.IPV6_GLOBAL1);
        lp.addLinkAddress(Constants.IPV6_GLOBAL2);
        updateLinkProperties(lp);
        verifyDisplayedIpv6Addresses(inOrder,
                Constants.IPV6_LINKLOCAL,
                Constants.IPV6_GLOBAL1,
                Constants.IPV6_GLOBAL2);

        lp.removeLinkAddress(Constants.IPV6_GLOBAL1);
        updateLinkProperties(lp);
        verifyDisplayedIpv6Addresses(inOrder,
                Constants.IPV6_LINKLOCAL,
                Constants.IPV6_GLOBAL2);

        lp.addDnsServer(Constants.IPV6_DNS);
        updateLinkProperties(lp);
        inOrder.verify(mockDnsPref).setSummary(Constants.IPV6_DNS.getHostAddress());
        inOrder.verify(mockDnsPref).setVisible(true);

        lp.addDnsServer(Constants.IPV4_DNS1);
        lp.addDnsServer(Constants.IPV4_DNS2);
        updateLinkProperties(lp);
        inOrder.verify(mockDnsPref).setSummary(
                Constants.IPV6_DNS.getHostAddress() + "\n" +
                        Constants.IPV4_DNS1.getHostAddress() + "\n" +
                        Constants.IPV4_DNS2.getHostAddress());
        inOrder.verify(mockDnsPref).setVisible(true);
    }

    @Test
    public void onCapabilitiesChanged_callsRefreshIfNecessary() {
        NetworkCapabilities nc = makeNetworkCapabilities();
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(new NetworkCapabilities(nc));

        String summary = "Connected, no Internet";
        when(mockAccessPoint.getSettingsSummary()).thenReturn(summary);

        InOrder inOrder = inOrder(mockHeaderController);
        displayAndResume();
        inOrder.verify(mockHeaderController).setSummary(summary);

        // Check that an irrelevant capability update does not update the access point summary, as
        // doing so could cause unnecessary jank...
        summary = "Connected";
        when(mockAccessPoint.getSettingsSummary()).thenReturn(summary);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController, never()).setSummary(any(CharSequence.class));

        // ... but that if the network validates, then we do refresh.
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController).setSummary(summary);

        summary = "Connected, no Internet";
        when(mockAccessPoint.getSettingsSummary()).thenReturn(summary);

        // Another irrelevant update won't cause the UI to refresh...
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController, never()).setSummary(any(CharSequence.class));

        // ... but if the network is no longer validated, then we display "connected, no Internet".
        nc.removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController).setSummary(summary);
    }

    @Test
    public void canForgetNetwork_noNetwork() {
        when(mockAccessPoint.getConfig()).thenReturn(null);

        mController = newWifiDetailPreferenceController();
        displayAndResume();

        verify(mockButtonsPref).setButton1Visible(false);
    }

    @Test
    public void canForgetNetwork_ephemeral() {
        when(mockWifiInfo.isEphemeral()).thenReturn(true);
        when(mockAccessPoint.getConfig()).thenReturn(null);

        displayAndResume();

        verify(mockButtonsPref).setButton1Visible(true);
    }

    @Test
    public void canForgetNetwork_saved() {
        displayAndResume();

        verify(mockButtonsPref).setButton1Visible(true);
    }

    @Test
    public void canForgetNetwork_lockedDown() {
        lockDownNetwork();

        displayAndResume();

        verify(mockButtonsPref).setButton1Visible(false);
    }

    @Test
    public void canModifyNetwork_saved() {
        assertThat(mController.canModifyNetwork()).isTrue();
    }

    @Test
    public void canModifyNetwork_lockedDown() {
        lockDownNetwork();

        assertThat(mController.canModifyNetwork()).isFalse();
    }

    /**
     * Pretends that current network is locked down by device owner.
     */
    private void lockDownNetwork() {
        final int doUserId = 123;
        final int doUid = 1234;
        String doPackage = "some.package";

        mockWifiConfig.creatorUid = doUid;
        ComponentName doComponent = new ComponentName(doPackage, "some.Class");
        try {
            when(mockPackageManager.getPackageUidAsUser(Matchers.anyString(), Matchers.anyInt()))
                    .thenReturn(doUid);
        } catch (PackageManager.NameNotFoundException e) {
            //do nothing
        }
        ShadowDevicePolicyManager.getShadow().setDeviceOwnerComponentOnAnyUser(doComponent);
        ShadowDevicePolicyManager.getShadow().setDeviceOwnerUserId(doUserId);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN, 1);
    }

    @Test
    public void forgetNetwork_ephemeral() {
        String ssid = "ssid";
        when(mockWifiInfo.isEphemeral()).thenReturn(true);
        when(mockWifiInfo.getSSID()).thenReturn(ssid);

        displayAndResume();
        mForgetClickListener.getValue().onClick(null);

        verify(mockWifiManager).disableEphemeralNetwork(ssid);
        verify(mockMetricsFeatureProvider)
                .action(mockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
    }

    @Test
    public void forgetNetwork_saved() {
        mockWifiConfig.networkId = 5;

        mController.displayPreference(mockScreen);
        mForgetClickListener.getValue().onClick(null);

        verify(mockWifiManager).forget(mockWifiConfig.networkId, null);
        verify(mockMetricsFeatureProvider)
                .action(mockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
    }

    @Test
    public void networkStateChangedIntent_shouldRefetchInfo() {
        displayAndResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void rssiChangedIntent_shouldRefetchInfo() {
        displayAndResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void networkDisconnectedState_shouldFinishActivity() {
        displayAndResume();

        when(mockConnectivityManager.getNetworkInfo(any(Network.class))).thenReturn(null);
        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mockActivity).finish();
    }

    @Test
    public void networkOnLost_shouldFinishActivity() {
        displayAndResume();

        mCallbackCaptor.getValue().onLost(mockNetwork);

        verify(mockActivity).finish();
    }

    @Test
    public void ipv6AddressPref_shouldHaveHostAddressTextSet() {
        mLinkProperties.addLinkAddress(Constants.IPV6_LINKLOCAL);
        mLinkProperties.addLinkAddress(Constants.IPV6_GLOBAL1);
        mLinkProperties.addLinkAddress(Constants.IPV6_GLOBAL2);

        displayAndResume();

        String expectedAddresses = String.join("\n",
                asString(Constants.IPV6_LINKLOCAL),
                asString(Constants.IPV6_GLOBAL1),
                asString(Constants.IPV6_GLOBAL2));

        verify(mockIpv6AddressesPref).setSummary(expectedAddresses);
    }

    @Test
    public void ipv6AddressPref_shouldNotBeSelectable() {
        mLinkProperties.addLinkAddress(Constants.IPV6_GLOBAL2);

        displayAndResume();

        assertThat(mockIpv6AddressesPref.isSelectable()).isFalse();
    }

    @Test
    public void captivePortal_shouldShowSignInButton() {
        InOrder inOrder = inOrder(mockButtonsPref);

        displayAndResume();

        inOrder.verify(mockButtonsPref).setButton2Visible(false);

        NetworkCapabilities nc = makeNetworkCapabilities();
        updateNetworkCapabilities(nc);
        inOrder.verify(mockButtonsPref).setButton2Visible(false);

        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockButtonsPref).setButton2Visible(true);

        nc.removeCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockButtonsPref).setButton2Visible(false);
    }

    @Test
    public void testSignInButton_shouldStartCaptivePortalApp() {
        displayAndResume();

        ArgumentCaptor<OnClickListener> captor = ArgumentCaptor.forClass(OnClickListener.class);
        verify(mockButtonsPref).setButton2OnClickListener(captor.capture());
        captor.getValue().onClick(null);
        verify(mockConnectivityManager).startCaptivePortalApp(mockNetwork);
        verify(mockMetricsFeatureProvider)
                .action(mockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_SIGNIN);
    }

    @Test
    public void testRefreshRssiViews_shouldNotUpdateIfLevelIsSame() {
        displayAndResume();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockAccessPoint, times(2)).getLevel();
        verify(mockIconInjector, times(1)).getIcon(anyInt());
    }

    @Test
    public void testRefreshRssiViews_shouldUpdateOnLevelChange() {
        displayAndResume();

        when(mockAccessPoint.getLevel()).thenReturn(0);
        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockAccessPoint, times(2)).getLevel();
        verify(mockIconInjector, times(2)).getIcon(anyInt());
    }

    private ActionButtonsPreference createMock() {
        final ActionButtonsPreference pref = mock(ActionButtonsPreference.class);
        when(pref.setButton1Text(anyInt())).thenReturn(pref);
        when(pref.setButton1Icon(anyInt())).thenReturn(pref);
        when(pref.setButton1Enabled(anyBoolean())).thenReturn(pref);
        when(pref.setButton1Visible(anyBoolean())).thenReturn(pref);
        when(pref.setButton1OnClickListener(any(View.OnClickListener.class))).thenReturn(pref);

        when(pref.setButton2Text(anyInt())).thenReturn(pref);
        when(pref.setButton2Icon(anyInt())).thenReturn(pref);
        when(pref.setButton2Enabled(anyBoolean())).thenReturn(pref);
        when(pref.setButton2Visible(anyBoolean())).thenReturn(pref);
        when(pref.setButton2OnClickListener(any(View.OnClickListener.class))).thenReturn(pref);

        when(pref.setButton3Text(anyInt())).thenReturn(pref);
        when(pref.setButton3Icon(anyInt())).thenReturn(pref);
        when(pref.setButton3Enabled(anyBoolean())).thenReturn(pref);
        when(pref.setButton3Visible(anyBoolean())).thenReturn(pref);
        when(pref.setButton3OnClickListener(any(View.OnClickListener.class))).thenReturn(pref);

        return pref;
    }
}
