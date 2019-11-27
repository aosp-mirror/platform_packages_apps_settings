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
package com.android.settings.wifi.details2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
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
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.MacAddress;
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
import com.android.settings.Utils;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.ActionButtonsPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

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
import org.robolectric.shadows.ShadowToast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDevicePolicyManager.class, ShadowEntityHeaderController.class})
public class WifiDetailPreferenceController2Test {

    private static final int LEVEL = 1;
    private static final int RSSI = -55;
    private static final int TX_LINK_SPEED = 123;
    private static final int RX_LINK_SPEED = 54;
    private static final String SSID = "ssid";
    private static final String MAC_ADDRESS = "01:23:45:67:89:ab";
    private static final String RANDOMIZED_MAC_ADDRESS = "RANDOMIZED_MAC_ADDRESS";
    private static final String FACTORY_MAC_ADDRESS = "FACTORY_MAC_ADDRESS";
    private static final String SECURITY = "None";
    private static final String FQDN = "fqdn";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mMockScreen;

    @Mock
    private AccessPoint mMockAccessPoint;
    @Mock
    private FragmentActivity mMockActivity;
    @Mock
    private ConnectivityManager mMockConnectivityManager;
    @Mock
    private Network mMockNetwork;
    @Mock
    private NetworkInfo mMockNetworkInfo;
    @Mock
    private WifiConfiguration mMockWifiConfig;
    @Mock
    private WifiInfo mMockWifiInfo;
    @Mock
    private WifiNetworkDetailsFragment2 mMockFragment;
    @Mock
    private WifiManager mMockWifiManager;
    @Mock
    private WifiTracker mMockWifiTracker;
    @Mock
    private MetricsFeatureProvider mMockMetricsFeatureProvider;
    @Mock
    private WifiDetailPreferenceController2.IconInjector mMockIconInjector;
    @Mock
    private MacAddress mMockMacAddress;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityHeaderController mMockHeaderController;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LayoutPreference mMockHeaderLayoutPreference;
    @Mock
    private ImageView mMockHeaderIcon;

    @Mock
    private ActionButtonsPreference mMockButtonsPref;
    @Mock
    private Preference mMockSignalStrengthPref;
    @Mock
    private Preference mMockTxLinkSpeedPref;
    @Mock
    private Preference mMockRxLinkSpeedPref;
    @Mock
    private Preference mMockFrequencyPref;
    @Mock
    private Preference mMockSecurityPref;
    @Mock
    private Preference mMockSsidPref;
    @Mock
    private Preference mMockMacAddressPref;
    @Mock
    private Preference mMockIpAddressPref;
    @Mock
    private Preference mMockGatewayPref;
    @Mock
    private Preference mMockSubnetPref;
    @Mock
    private Preference mMockDnsPref;
    @Mock
    private PreferenceCategory mMockIpv6Category;
    @Mock
    private Preference mMockIpv6AddressesPref;
    @Mock
    private PackageManager mMockPackageManager;

    @Captor
    private ArgumentCaptor<NetworkCallback> mCallbackCaptor;
    @Captor
    private ArgumentCaptor<View.OnClickListener> mForgetClickListener;

    private Context mContext;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private LinkProperties mLinkProperties;
    private WifiDetailPreferenceController2 mController;

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

        when(mContext.getPackageManager()).thenReturn(mMockPackageManager);
        when(mMockAccessPoint.getConfig()).thenReturn(mMockWifiConfig);
        when(mMockAccessPoint.getLevel()).thenReturn(LEVEL);
        when(mMockAccessPoint.getSecurityString(false)).thenReturn(SECURITY);
        when(mMockAccessPoint.getSsidStr()).thenReturn(SSID);
        when(mMockConnectivityManager.getNetworkInfo(any(Network.class)))
                .thenReturn(mMockNetworkInfo);
        doNothing().when(mMockConnectivityManager).registerNetworkCallback(
                nullable(NetworkRequest.class), mCallbackCaptor.capture(), nullable(Handler.class));
        mMockButtonsPref = createMock();
        when(mMockButtonsPref.setButton1OnClickListener(mForgetClickListener.capture()))
                .thenReturn(mMockButtonsPref);

        when(mMockWifiInfo.getTxLinkSpeedMbps()).thenReturn(TX_LINK_SPEED);
        when(mMockWifiInfo.getRxLinkSpeedMbps()).thenReturn(RX_LINK_SPEED);
        when(mMockWifiInfo.getRssi()).thenReturn(RSSI);
        when(mMockWifiInfo.getMacAddress()).thenReturn(MAC_ADDRESS);
        when(mMockWifiManager.getConnectionInfo()).thenReturn(mMockWifiInfo);

        when(mMockWifiManager.getCurrentNetwork()).thenReturn(mMockNetwork);
        mLinkProperties = new LinkProperties();
        when(mMockConnectivityManager.getLinkProperties(mMockNetwork)).thenReturn(mLinkProperties);

        when(mMockFragment.getActivity()).thenReturn(mMockActivity);

        ShadowEntityHeaderController.setUseMock(mMockHeaderController);
        // builder pattern
        when(mMockHeaderController.setRecyclerView(mMockFragment.getListView(), mLifecycle))
                .thenReturn(mMockHeaderController);
        when(mMockHeaderController.setSummary(anyString())).thenReturn(mMockHeaderController);
        when(mMockIconInjector.getIcon(anyInt())).thenReturn(new ColorDrawable());

        setupMockedPreferenceScreen();
    }

    private void setUpForConnectedNetwork() {
        when(mMockAccessPoint.isActive()).thenReturn(true);
        ArrayList list = new ArrayList<>();
        list.add(mMockAccessPoint);
        when(mMockWifiTracker.getAccessPoints()).thenReturn(list);
        WifiTrackerFactory.setTestingWifiTracker(mMockWifiTracker);
        when(mMockAccessPoint.matches(any(AccessPoint.class))).thenReturn(true);
        when(mMockAccessPoint.isReachable()).thenReturn(true);

        mController = newWifiDetailPreferenceController2();
    }

    private void setUpForDisconnectedNetwork() {
        when(mMockAccessPoint.isActive()).thenReturn(false);
        ArrayList list = new ArrayList<>();
        list.add(mMockAccessPoint);
        when(mMockWifiTracker.getAccessPoints()).thenReturn(list);
        WifiTrackerFactory.setTestingWifiTracker(mMockWifiTracker);
        when(mMockAccessPoint.matches(any(AccessPoint.class))).thenReturn(true);
        when(mMockAccessPoint.isReachable()).thenReturn(true);

        mController = newWifiDetailPreferenceController2();
    }

    private void setUpForNotInRangeNetwork() {
        when(mMockAccessPoint.isActive()).thenReturn(false);
        ArrayList list = new ArrayList<>();
        list.add(mMockAccessPoint);
        when(mMockWifiTracker.getAccessPoints()).thenReturn(list);
        WifiTrackerFactory.setTestingWifiTracker(mMockWifiTracker);
        when(mMockAccessPoint.matches(any(AccessPoint.class))).thenReturn(false);
        when(mMockAccessPoint.isReachable()).thenReturn(false);

        mController = newWifiDetailPreferenceController2();
    }

    private WifiDetailPreferenceController2 newWifiDetailPreferenceController2() {
        return new WifiDetailPreferenceController2(
                mMockAccessPoint,
                mMockConnectivityManager,
                mContext,
                mMockFragment,
                null,  // Handler
                mLifecycle,
                mMockWifiManager,
                mMockMetricsFeatureProvider,
                mMockIconInjector);
    }

    private void setupMockedPreferenceScreen() {
        when(mMockScreen.getPreferenceManager().getContext()).thenReturn(mContext);

        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_HEADER))
                .thenReturn(mMockHeaderLayoutPreference);
        when(mMockHeaderLayoutPreference.findViewById(R.id.entity_header_icon))
                .thenReturn(mMockHeaderIcon);

        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_BUTTONS_PREF))
                .thenReturn(mMockButtonsPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_SIGNAL_STRENGTH_PREF))
                .thenReturn(mMockSignalStrengthPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_TX_LINK_SPEED))
                .thenReturn(mMockTxLinkSpeedPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_RX_LINK_SPEED))
                .thenReturn(mMockRxLinkSpeedPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_FREQUENCY_PREF))
                .thenReturn(mMockFrequencyPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_SECURITY_PREF))
                .thenReturn(mMockSecurityPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_SSID_PREF))
                .thenReturn(mMockSsidPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_MAC_ADDRESS_PREF))
                .thenReturn(mMockMacAddressPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_IP_ADDRESS_PREF))
                .thenReturn(mMockIpAddressPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_GATEWAY_PREF))
                .thenReturn(mMockGatewayPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_SUBNET_MASK_PREF))
                .thenReturn(mMockSubnetPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_DNS_PREF))
                .thenReturn(mMockDnsPref);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_IPV6_CATEGORY))
                .thenReturn(mMockIpv6Category);
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_IPV6_ADDRESSES_PREF))
                .thenReturn(mMockIpv6AddressesPref);
    }

    private void displayAndResume() {
        mController.displayPreference(mMockScreen);
        mController.onResume();
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        setUpForConnectedNetwork();
        mController.displayPreference(mMockScreen);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void securityPreference_stringShouldBeSet() {
        setUpForConnectedNetwork();
        displayAndResume();

        verify(mMockSecurityPref).setSummary(SECURITY);
    }

    @Test
    public void latestWifiInfo_shouldBeFetchedInDisplayPreferenceForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mMockWifiManager, times(1)).getConnectionInfo();
    }

    @Test
    public void latestWifiInfo_shouldNotBeFetchedInDisplayPreferenceForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockWifiManager, never()).getConnectionInfo();
    }

    @Test
    public void latestWifiInfo_shouldNotBeFetchedInDisplayPreferenceForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mMockWifiManager, never()).getConnectionInfo();
    }

    @Test
    public void latestNetworkInfo_shouldBeFetchedInDisplayPreferenceForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mMockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
    }

    @Test
    public void latestNetworkInfo_shouldNotBeFetchedInDisplayPreferenceForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockConnectivityManager, never()).getNetworkInfo(any(Network.class));
    }

    @Test
    public void latestNetworkInfo_shouldNotBeFetchedInDisplayPreferenceForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mMockConnectivityManager, never()).getNetworkInfo(any(Network.class));
    }

    @Test
    public void networkCallback_shouldBeRegisteredOnResume() {
        setUpForConnectedNetwork();
        displayAndResume();

        verify(mMockConnectivityManager, times(1)).registerNetworkCallback(
                nullable(NetworkRequest.class), mCallbackCaptor.capture(), nullable(Handler.class));
    }

    @Test
    public void networkCallback_shouldBeUnregisteredOnPause() {
        setUpForConnectedNetwork();
        displayAndResume();
        mController.onPause();

        verify(mMockConnectivityManager, times(1))
                .unregisterNetworkCallback(mCallbackCaptor.getValue());
    }

    @Test
    public void entityHeader_shouldHaveIconSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        Drawable expectedIcon = mMockIconInjector.getIcon(LEVEL);

        displayAndResume();

        verify(mMockHeaderController).setIcon(expectedIcon);
    }

    @Test
    public void entityHeader_shouldHaveIconSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        Drawable expectedIcon = mMockIconInjector.getIcon(LEVEL);

        displayAndResume();

        verify(mMockHeaderController).setIcon(expectedIcon);
    }

    @Test
    public void entityHeader_shouldNotHaveIconSetForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mMockHeaderController, never()).setIcon(any(Drawable.class));
    }

    @Test
    public void entityHeader_shouldHaveLabelSetToTitle() {
        setUpForConnectedNetwork();
        String label = "title";
        when(mMockAccessPoint.getTitle()).thenReturn(label);

        displayAndResume();

        verify(mMockHeaderController).setLabel(label);
    }

    @Test
    public void entityHeader_shouldHaveSummarySet() {
        setUpForConnectedNetwork();
        String summary = "summary";
        when(mMockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);

        displayAndResume();

        verify(mMockHeaderController).setSummary(summary);
    }

    @Test
    public void entityHeader_shouldConvertSavedAsDisconnected() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockAccessPoint, times(1)).getSettingsSummary(true /*convertSavedAsDisconnected*/);
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSetForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mMockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldNotHaveIconSetForOutOfRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mMockSignalStrengthPref, never()).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        displayAndResume();

        verify(mMockSignalStrengthPref).setSummary(expectedStrength);
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        displayAndResume();

        verify(mMockSignalStrengthPref).setSummary(expectedStrength);
    }

    @Test
    public void signalStrengthPref_shouldNotHaveDetailTextSetForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mMockSignalStrengthPref, never()).setSummary(any(String.class));
    }

    @Test
    public void linkSpeedPref_shouldNotShowIfNotSet() {
        setUpForConnectedNetwork();
        when(mMockWifiInfo.getTxLinkSpeedMbps()).thenReturn(WifiInfo.LINK_SPEED_UNKNOWN);

        displayAndResume();

        verify(mMockTxLinkSpeedPref).setVisible(false);
    }

    @Test
    public void linkSpeedPref_shouldVisibleForConnectedNetwork() {
        setUpForConnectedNetwork();
        String expectedLinkSpeed = mContext.getString(R.string.tx_link_speed, TX_LINK_SPEED);

        displayAndResume();

        verify(mMockTxLinkSpeedPref).setVisible(true);
        verify(mMockTxLinkSpeedPref).setSummary(expectedLinkSpeed);
    }

    @Test
    public void linkSpeedPref_shouldInvisibleForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockTxLinkSpeedPref).setVisible(false);
        verify(mMockTxLinkSpeedPref, never()).setSummary(any(String.class));
    }

    @Test
    public void linkSpeedPref_shouldInvisibleForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mMockTxLinkSpeedPref).setVisible(false);
        verify(mMockTxLinkSpeedPref, never()).setSummary(any(String.class));
    }

    @Test
    public void rxLinkSpeedPref_shouldNotShowIfNotSet() {
        setUpForConnectedNetwork();
        when(mMockWifiInfo.getRxLinkSpeedMbps()).thenReturn(WifiInfo.LINK_SPEED_UNKNOWN);

        displayAndResume();

        verify(mMockRxLinkSpeedPref).setVisible(false);
    }

    @Test
    public void rxLinkSpeedPref_shouldVisibleForConnectedNetwork() {
        setUpForConnectedNetwork();
        String expectedLinkSpeed = mContext.getString(R.string.rx_link_speed, RX_LINK_SPEED);

        displayAndResume();

        verify(mMockRxLinkSpeedPref).setVisible(true);
        verify(mMockRxLinkSpeedPref).setSummary(expectedLinkSpeed);
    }

    @Test
    public void rxLinkSpeedPref_shouldInvisibleForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockRxLinkSpeedPref).setVisible(false);
        verify(mMockRxLinkSpeedPref, never()).setSummary(any(String.class));
    }

    @Test
    public void rxLinkSpeedPref_shouldInvisibleForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mMockRxLinkSpeedPref).setVisible(false);
        verify(mMockRxLinkSpeedPref, never()).setSummary(any(String.class));
    }

    @Test
    public void ssidPref_shouldHaveDetailTextSetForPasspointR1() {
        setUpForConnectedNetwork();
        when(mMockAccessPoint.isPasspoint()).thenReturn(true);
        when(mMockAccessPoint.isOsuProvider()).thenReturn(false);

        displayAndResume();

        verify(mMockSsidPref, times(1)).setSummary(SSID);
        verify(mMockSsidPref, times(1)).setVisible(true);
    }

    @Test
    public void ssidPref_shouldHaveDetailTextSetForPasspointR2() {
        setUpForConnectedNetwork();
        when(mMockAccessPoint.isPasspoint()).thenReturn(false);
        when(mMockAccessPoint.isOsuProvider()).thenReturn(true);

        displayAndResume();

        verify(mMockSsidPref, times(1)).setSummary(SSID);
        verify(mMockSsidPref, times(1)).setVisible(true);
    }

    @Test
    public void ssidPref_shouldNotShowIfNotPasspoint() {
        setUpForConnectedNetwork();
        when(mMockAccessPoint.isPasspoint()).thenReturn(false);
        when(mMockAccessPoint.isOsuProvider()).thenReturn(false);

        displayAndResume();

        verify(mMockSsidPref).setVisible(false);
    }

    @Test
    public void macAddressPref_shouldVisibleForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mMockMacAddressPref).setVisible(true);
        verify(mMockMacAddressPref).setSummary(MAC_ADDRESS);
    }

    @Test
    public void macAddressPref_shouldVisibleAsRandomizedForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        mMockWifiConfig.macRandomizationSetting = WifiConfiguration.RANDOMIZATION_PERSISTENT;
        when(mMockWifiConfig.getRandomizedMacAddress()).thenReturn(mMockMacAddress);
        when(mMockMacAddress.toString()).thenReturn(RANDOMIZED_MAC_ADDRESS);

        displayAndResume();

        verify(mMockMacAddressPref).setVisible(true);
        verify(mMockMacAddressPref).setSummary(RANDOMIZED_MAC_ADDRESS);
    }

    @Test
    public void macAddressPref_shouldVisibleAsFactoryForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        mMockWifiConfig.macRandomizationSetting = WifiConfiguration.RANDOMIZATION_NONE;
        when(mMockWifiManager.getFactoryMacAddresses())
                .thenReturn(new String[]{FACTORY_MAC_ADDRESS});

        displayAndResume();

        verify(mMockMacAddressPref).setVisible(true);
        verify(mMockMacAddressPref).setSummary(FACTORY_MAC_ADDRESS);
    }

    @Test
    public void ipAddressPref_shouldHaveDetailTextSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        mLinkProperties.addLinkAddress(Constants.IPV4_ADDR);

        displayAndResume();

        verify(mMockIpAddressPref).setSummary(Constants.IPV4_ADDR.getAddress().getHostAddress());
        verify(mMockIpAddressPref).setVisible(true);
    }

    @Test
    public void ipAddressPref_shouldInvisibleForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockIpAddressPref).setVisible(false);
    }

    @Test
    public void gatewayAndSubnet_shouldHaveDetailTextSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        mLinkProperties.addLinkAddress(Constants.IPV4_ADDR);
        mLinkProperties.addRoute(Constants.IPV4_DEFAULT);
        mLinkProperties.addRoute(Constants.IPV4_SUBNET);

        displayAndResume();

        verify(mMockSubnetPref).setSummary("255.255.255.128");
        verify(mMockGatewayPref).setSummary("192.0.2.127");
        verify(mMockSubnetPref).setVisible(true);
    }

    @Test
    public void gatewayAndSubnet_shouldInvisibleSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockSubnetPref).setVisible(false);
    }

    @Test
    public void dnsServersPref_shouldHaveDetailTextSetForConnectedNetwork()
            throws UnknownHostException {
        setUpForConnectedNetwork();
        mLinkProperties.addDnsServer(InetAddress.getByAddress(new byte[] {8, 8, 4, 4}));
        mLinkProperties.addDnsServer(InetAddress.getByAddress(new byte[] {8, 8, 8, 8}));
        mLinkProperties.addDnsServer(Constants.IPV6_DNS);

        displayAndResume();

        verify(mMockDnsPref).setSummary(
                "8.8.4.4\n" + "8.8.8.8\n" + Constants.IPV6_DNS.getHostAddress());
        verify(mMockDnsPref).setVisible(true);
    }

    @Test
    public void dnsServersPref_shouldInvisibleSetForDisconnectedNetwork()
            throws UnknownHostException {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockDnsPref).setVisible(false);
    }

    @Test
    public void noCurrentNetwork_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();
        when(mMockWifiManager.getCurrentNetwork()).thenReturn(null);

        displayAndResume();

        verify(mMockActivity, never()).finish();
    }

    @Test
    public void noLinkProperties_allIpDetailsHidden() {
        setUpForConnectedNetwork();
        when(mMockConnectivityManager.getLinkProperties(mMockNetwork)).thenReturn(null);
        reset(mMockIpv6Category, mMockIpAddressPref, mMockSubnetPref, mMockGatewayPref,
                mMockDnsPref);

        displayAndResume();

        verify(mMockIpv6Category).setVisible(false);
        verify(mMockIpAddressPref).setVisible(false);
        verify(mMockSubnetPref).setVisible(false);
        verify(mMockGatewayPref).setVisible(false);
        verify(mMockDnsPref).setVisible(false);
        verify(mMockIpv6Category, never()).setVisible(true);
        verify(mMockIpAddressPref, never()).setVisible(true);
        verify(mMockSubnetPref, never()).setVisible(true);
        verify(mMockGatewayPref, never()).setVisible(true);
        verify(mMockDnsPref, never()).setVisible(true);
    }

    @Test
    public void disconnectedNetwork_allIpDetailsHidden() {
        setUpForDisconnectedNetwork();
        reset(mMockIpv6Category, mMockIpAddressPref, mMockSubnetPref, mMockGatewayPref,
                mMockDnsPref);

        displayAndResume();

        verify(mMockIpv6Category).setVisible(false);
        verify(mMockIpAddressPref).setVisible(false);
        verify(mMockSubnetPref).setVisible(false);
        verify(mMockGatewayPref).setVisible(false);
        verify(mMockDnsPref).setVisible(false);
        verify(mMockIpv6Category, never()).setVisible(true);
        verify(mMockIpAddressPref, never()).setVisible(true);
        verify(mMockSubnetPref, never()).setVisible(true);
        verify(mMockGatewayPref, never()).setVisible(true);
        verify(mMockDnsPref, never()).setVisible(true);
    }

    // Convenience method to convert a LinkAddress to a string without a prefix length.
    private String asString(LinkAddress l) {
        return l.getAddress().getHostAddress();
    }

    // Pretend that the NetworkCallback was triggered with a new copy of lp. We need to create a
    // new copy because the code only updates if !mLinkProperties.equals(lp).
    private void updateLinkProperties(LinkProperties lp) {
        mCallbackCaptor.getValue().onLinkPropertiesChanged(mMockNetwork, new LinkProperties(lp));
    }

    private void updateNetworkCapabilities(NetworkCapabilities nc) {
        mCallbackCaptor.getValue().onCapabilitiesChanged(mMockNetwork, new NetworkCapabilities(nc));
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
        inOrder.verify(mMockIpv6AddressesPref).setSummary(text);
    }

    @Test
    public void onLinkPropertiesChanged_updatesFields() {
        setUpForConnectedNetwork();
        displayAndResume();

        InOrder inOrder = inOrder(mMockIpAddressPref, mMockGatewayPref, mMockSubnetPref,
                mMockDnsPref, mMockIpv6Category, mMockIpv6AddressesPref);

        LinkProperties lp = new LinkProperties();

        lp.addLinkAddress(Constants.IPV6_LINKLOCAL);
        updateLinkProperties(lp);
        verifyDisplayedIpv6Addresses(inOrder, Constants.IPV6_LINKLOCAL);
        inOrder.verify(mMockIpv6Category).setVisible(true);

        lp.addRoute(Constants.IPV4_DEFAULT);
        updateLinkProperties(lp);
        inOrder.verify(mMockGatewayPref).setSummary(Constants.IPV4_GATEWAY.getHostAddress());
        inOrder.verify(mMockGatewayPref).setVisible(true);

        lp.addLinkAddress(Constants.IPV4_ADDR);
        lp.addRoute(Constants.IPV4_SUBNET);
        updateLinkProperties(lp);
        inOrder.verify(mMockIpAddressPref).setSummary(asString(Constants.IPV4_ADDR));
        inOrder.verify(mMockIpAddressPref).setVisible(true);
        inOrder.verify(mMockSubnetPref).setSummary("255.255.255.128");
        inOrder.verify(mMockSubnetPref).setVisible(true);

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
        inOrder.verify(mMockDnsPref).setSummary(Constants.IPV6_DNS.getHostAddress());
        inOrder.verify(mMockDnsPref).setVisible(true);

        lp.addDnsServer(Constants.IPV4_DNS1);
        lp.addDnsServer(Constants.IPV4_DNS2);
        updateLinkProperties(lp);
        inOrder.verify(mMockDnsPref).setSummary(
                Constants.IPV6_DNS.getHostAddress() + "\n"
                        + Constants.IPV4_DNS1.getHostAddress() + "\n"
                        + Constants.IPV4_DNS2.getHostAddress());
        inOrder.verify(mMockDnsPref).setVisible(true);
    }

    @Test
    public void onCapabilitiesChanged_callsRefreshIfNecessary() {
        setUpForConnectedNetwork();
        NetworkCapabilities nc = makeNetworkCapabilities();
        when(mMockConnectivityManager.getNetworkCapabilities(mMockNetwork))
                .thenReturn(new NetworkCapabilities(nc));

        String summary = "Connected, no Internet";
        when(mMockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);

        InOrder inOrder = inOrder(mMockHeaderController);
        displayAndResume();
        inOrder.verify(mMockHeaderController).setSummary(summary);

        // Check that an irrelevant capability update does not update the access point summary, as
        // doing so could cause unnecessary jank...
        summary = "Connected";
        when(mMockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController, never()).setSummary(any(CharSequence.class));

        // ... but that if the network validates, then we do refresh.
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);

        summary = "Connected, no Internet";
        when(mMockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);

        // Another irrelevant update won't cause the UI to refresh...
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController, never()).setSummary(any(CharSequence.class));

        // ... but if the network is no longer validated, then we display "connected, no Internet".
        nc.removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);

        // UI will be refreshed when private DNS is broken.
        summary = "Private DNS server cannot be accessed";
        when(mMockAccessPoint.getSettingsSummary(true /* convertSavedAsDisconnected */))
                .thenReturn(summary);
        nc.setPrivateDnsBroken(true);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);

        // UI will be refreshed when device connects to a partial connectivity network.
        summary = "Limited connection";
        when(mMockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);

        // Although UI will be refreshed when network become validated. The Settings should
        // continue to display "Limited connection" if network still provides partial connectivity.
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);
    }

    @Test
    public void canForgetNetwork_shouldInvisibleIfWithoutConfiguration() {
        setUpForConnectedNetwork();
        when(mMockAccessPoint.getConfig()).thenReturn(null);
        mController = newWifiDetailPreferenceController2();

        displayAndResume();

        verify(mMockButtonsPref).setButton1Visible(false);
    }

    @Test
    public void canForgetNetwork_ephemeral() {
        setUpForConnectedNetwork();
        when(mMockWifiInfo.isEphemeral()).thenReturn(true);
        when(mMockAccessPoint.getConfig()).thenReturn(null);

        displayAndResume();

        verify(mMockButtonsPref).setButton1Visible(true);
    }

    @Test
    public void canForgetNetwork_saved() {
        setUpForConnectedNetwork();
        displayAndResume();

        verify(mMockButtonsPref).setButton1Visible(true);
    }

    @Test
    public void canForgetNetwork_lockedDown() {
        setUpForConnectedNetwork();
        lockDownNetwork();

        displayAndResume();

        verify(mMockButtonsPref).setButton1Visible(false);
    }

    @Test
    public void canShareNetwork_shouldInvisibleIfWithoutConfiguration() {
        setUpForConnectedNetwork();
        when(mMockAccessPoint.getConfig()).thenReturn(null);

        displayAndResume();

        verify(mMockButtonsPref).setButton4Visible(false);
    }

    @Test
    public void canModifyNetwork_saved() {
        setUpForConnectedNetwork();
        assertThat(mController.canModifyNetwork()).isTrue();
    }

    @Test
    public void canModifyNetwork_lockedDown() {
        setUpForConnectedNetwork();
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

        mMockWifiConfig.creatorUid = doUid;
        ComponentName doComponent = new ComponentName(doPackage, "some.Class");
        try {
            when(mMockPackageManager.getPackageUidAsUser(Matchers.anyString(), Matchers.anyInt()))
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
        setUpForConnectedNetwork();
        String ssid = "ssid";
        when(mMockWifiInfo.isEphemeral()).thenReturn(true);
        when(mMockWifiInfo.getSSID()).thenReturn(ssid);

        displayAndResume();
        mForgetClickListener.getValue().onClick(null);

        verify(mMockWifiManager).disableEphemeralNetwork(ssid);
        verify(mMockMetricsFeatureProvider)
                .action(mMockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
    }

    @Test
    public void forgetNetwork_saved() {
        setUpForConnectedNetwork();
        mMockWifiConfig.networkId = 5;

        mController.displayPreference(mMockScreen);
        mForgetClickListener.getValue().onClick(null);

        verify(mMockWifiManager).forget(mMockWifiConfig.networkId, null);
        verify(mMockMetricsFeatureProvider)
                .action(mMockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
    }

    @Test
    public void forgetNetwork_shouldShowDialog() {
        setUpForConnectedNetwork();
        final WifiDetailPreferenceController2 spyController = spy(mController);

        mMockWifiConfig.networkId = 5;
        when(mMockAccessPoint.isPasspoint()).thenReturn(true);
        when(mMockAccessPoint.getPasspointFqdn()).thenReturn(FQDN);
        spyController.displayPreference(mMockScreen);

        mForgetClickListener.getValue().onClick(null);

        verify(mMockWifiManager, times(0)).removePasspointConfiguration(FQDN);
        verify(mMockMetricsFeatureProvider, times(0))
                .action(mMockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
        verify(spyController).showConfirmForgetDialog();
    }

    @Test
    public void networkStateChangedIntent_shouldRefetchInfo() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mMockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mMockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mMockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mMockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void networkStateChangedIntent_shouldRefetchInfoForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mMockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mMockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mMockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mMockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void rssiChangedIntent_shouldRefetchInfo() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mMockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mMockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mMockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mMockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void rssiChangedIntent_shouldRefetchInfoForConnectedNetwork() {
        setUpForConnectedNetwork();
        displayAndResume();

        verify(mMockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mMockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mMockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mMockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void networkDisconnectedState_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        when(mMockConnectivityManager.getNetworkInfo(any(Network.class))).thenReturn(null);
        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mMockActivity, never()).finish();
    }

    @Test
    public void networkOnLost_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        mCallbackCaptor.getValue().onLost(mMockNetwork);

        verify(mMockActivity, never()).finish();
    }

    @Test
    public void ipv6AddressPref_shouldHaveHostAddressTextSet() {
        setUpForConnectedNetwork();
        mLinkProperties.addLinkAddress(Constants.IPV6_LINKLOCAL);
        mLinkProperties.addLinkAddress(Constants.IPV6_GLOBAL1);
        mLinkProperties.addLinkAddress(Constants.IPV6_GLOBAL2);

        displayAndResume();

        String expectedAddresses = String.join("\n",
                asString(Constants.IPV6_LINKLOCAL),
                asString(Constants.IPV6_GLOBAL1),
                asString(Constants.IPV6_GLOBAL2));

        verify(mMockIpv6AddressesPref).setSummary(expectedAddresses);
    }

    @Test
    public void ipv6AddressPref_shouldNotBeSelectable() {
        setUpForConnectedNetwork();
        mLinkProperties.addLinkAddress(Constants.IPV6_GLOBAL2);

        displayAndResume();

        assertThat(mMockIpv6AddressesPref.isSelectable()).isFalse();
    }

    @Test
    public void captivePortal_shouldShowSignInButton() {
        setUpForConnectedNetwork();

        InOrder inOrder = inOrder(mMockButtonsPref);

        displayAndResume();

        inOrder.verify(mMockButtonsPref).setButton2Visible(false);

        NetworkCapabilities nc = makeNetworkCapabilities();
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockButtonsPref).setButton2Visible(false);

        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockButtonsPref).setButton2Visible(true);

        nc.removeCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockButtonsPref).setButton2Visible(false);
    }

    @Test
    public void testSignInButton_shouldStartCaptivePortalApp() {
        setUpForConnectedNetwork();

        displayAndResume();

        ArgumentCaptor<OnClickListener> captor = ArgumentCaptor.forClass(OnClickListener.class);
        verify(mMockButtonsPref).setButton2OnClickListener(captor.capture());
        captor.getValue().onClick(null);
        verify(mMockConnectivityManager).startCaptivePortalApp(mMockNetwork);
        verify(mMockMetricsFeatureProvider)
                .action(mMockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_SIGNIN);
    }

    @Test
    public void testSignInButton_shouldHideSignInButtonForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        NetworkCapabilities nc = makeNetworkCapabilities();
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        when(mMockConnectivityManager.getNetworkCapabilities(mMockNetwork))
                .thenReturn(new NetworkCapabilities(nc));

        // verify onResume
        displayAndResume();

        verify(mMockButtonsPref, never()).setButton2Visible(true);
        verify(mMockButtonsPref).setButton2Visible(false);

        // verify onCapabilitiesChanged
        updateNetworkCapabilities(nc);

        verify(mMockButtonsPref, never()).setButton2Visible(true);
        verify(mMockButtonsPref).setButton2Visible(false);
    }

    @Test
    public void testConnectButton_shouldInvisibleForConnectNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mMockButtonsPref, times(1)).setButton3Visible(false);
    }

    @Test
    public void testConnectButton_shouldVisibleForDisconnectNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockButtonsPref, times(1)).setButton3Visible(true);
        verify(mMockButtonsPref, times(1)).setButton3Text(R.string.wifi_connect);
    }

    private void setUpForToast() {
        Resources res = mContext.getResources();
        when(mMockActivity.getResources()).thenReturn(res);
    }

    @Test
    public void testConnectButton_clickConnect_displayAsSuccess() {
        setUpForDisconnectedNetwork();
        when(mMockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mMockButtonsPref);
        String label = "title";
        when(mMockAccessPoint.getTitle()).thenReturn(label);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mMockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as connected
        when(mMockAccessPoint.isActive()).thenReturn(true);
        mController.updateAccessPoint();

        // check connect button invisible, be init as default state and toast success message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(false);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_connected_to_message, label));
    }

    @Test
    public void testConnectButton_clickConnectButFailed_displayFailMessage() {
        setUpForDisconnectedNetwork();
        ArgumentCaptor<WifiManager.ActionListener> connectListenerCaptor =
                ArgumentCaptor.forClass(WifiManager.ActionListener.class);
        when(mMockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mMockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mMockWifiManager, times(1)).connect(anyInt(), connectListenerCaptor.capture());
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        connectListenerCaptor.getValue().onFailure(-1);

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    private void verifyConnectBtnSetUpAsVisible(InOrder inOrder) {
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Icon(R.drawable.ic_settings_wireless);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(true);
    }

    private void verifyConnectBtnSetUpAsConnecting(InOrder inOrder) {
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Text(R.string.wifi_connecting);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Enabled(false);
    }

    private void verifyConnectBtnBeInitAsDefault(InOrder inOrder) {
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Icon(R.drawable.ic_settings_wireless);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Enabled(true);
    }

    @Test
    public void testConnectButton_clickConnectButTimeout_displayFailMessage() {
        setUpForDisconnectedNetwork();
        when(mMockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mMockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mMockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        mController.sTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    @Test
    public void testConnectButton_clickConnectButTimeout_displayNotInRangeMessage() {
        setUpForNotInRangeNetwork();
        when(mMockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mMockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mMockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        mController.sTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_not_in_range_message));
    }

    @Test
    public void testConnectButton_clickConnectWhenWiFiDisabled_displaySuccessMessage() {
        setUpForDisconnectedNetwork();
        when(mMockWifiManager.isWifiEnabled()).thenReturn(false); // wifi disabled
        InOrder inOrder = inOrder(mMockButtonsPref);
        String label = "title";
        when(mMockAccessPoint.getTitle()).thenReturn(label);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check turn on Wi-Fi, display button as connecting and toast turn on Wi-Fi message
        verify(mMockWifiManager, times(1)).setWifiEnabled(true);
        verifyConnectBtnSetUpAsConnecting(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_turned_on_message));

        // notify Wi-Fi enabled
        mController.mWifiListener.onWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        // check had connect network and icon display as expected
        verify(mMockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as connected
        when(mMockAccessPoint.isActive()).thenReturn(true);
        mController.updateAccessPoint();

        // check connect button invisible, be init as default state and toast success message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(false);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_connected_to_message, label));
    }

    @Test
    public void testConnectButton_clickConnectWhenWiFiDisabled_failedToConnectWiFi() {
        setUpForDisconnectedNetwork();
        when(mMockWifiManager.isWifiEnabled()).thenReturn(false); // wifi disabled
        InOrder inOrder = inOrder(mMockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check turn on Wi-Fi, display button as connecting and toast turn on Wi-Fi message
        verify(mMockWifiManager, times(1)).setWifiEnabled(true);
        verifyConnectBtnSetUpAsConnecting(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_turned_on_message));

        // notify Wi-Fi enabled
        mController.mWifiListener.onWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        // check had connect network and icon display as expected
        verify(mMockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        mController.sTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    @Test
    public void
            testConnectButton_clickConnectWhenWiFiDisabled_failedToConnectWifiBecauseNotInRange() {
        setUpForNotInRangeNetwork();
        when(mMockWifiManager.isWifiEnabled()).thenReturn(false); // wifi disabled
        InOrder inOrder = inOrder(mMockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check turn on Wi-Fi, display button as connecting and toast turn on Wi-Fi message
        verify(mMockWifiManager, times(1)).setWifiEnabled(true);
        verifyConnectBtnSetUpAsConnecting(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_turned_on_message));

        // notify Wi-Fi enabled
        mController.mWifiListener.onWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        // check had connect network and icon display as expected
        verify(mMockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        mController.sTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_not_in_range_message));
    }

    @Test
    public void testConnectButton_clickConnectWhenWiFiDisabled_failedToEnableWifi() {
        setUpForDisconnectedNetwork();
        when(mMockWifiManager.isWifiEnabled()).thenReturn(false); // wifi disabled
        InOrder inOrder = inOrder(mMockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check turn on Wi-Fi, display button as connecting and toast turn on Wi-Fi message
        verify(mMockWifiManager, times(1)).setWifiEnabled(true);
        verifyConnectBtnSetUpAsConnecting(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_turned_on_message));

        // notify turn on Wi-Fi failed
        mController.sTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mMockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    @Test
    public void testConnectButton_clickConnectAndBackKey_ignoreTimeoutEvent() {
        setUpForDisconnectedNetwork();
        when(mMockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mMockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mMockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // leave detail page
        when(mMockFragment.getActivity()).thenReturn(null);

        // timeout happened
        mController.sTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        inOrder.verify(mMockButtonsPref, never()).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mMockButtonsPref, never()).setButton3Icon(R.drawable.ic_settings_wireless);
        inOrder.verify(mMockButtonsPref, never()).setButton3Enabled(true);
        inOrder.verify(mMockButtonsPref, never()).setButton3Visible(true);
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);
    }

    @Test
    public void updateAccessPoint_returnFalseForNothingChanged() {
        setUpForDisconnectedNetwork();

        displayAndResume();
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isFalse();
    }

    @Test
    public void updateAccessPoint_returnTrueForSignalLevelChanged() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        // Level changed
        when(mMockAccessPoint.getLevel()).thenReturn(LEVEL + 1);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForChangeAsNotInRange() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        // change as not in range
        when(mMockAccessPoint.matches(any(AccessPoint.class))).thenReturn(false);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForChangeAsInRange() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        // change as in range
        when(mMockAccessPoint.matches(any(AccessPoint.class))).thenReturn(true);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForChangeAsConnected() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        // change as connected
        when(mMockAccessPoint.isActive()).thenReturn(true);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForChangeAsDisconnected() {
        setUpForConnectedNetwork();

        displayAndResume();

        // change as disconnected
        when(mMockAccessPoint.isActive()).thenReturn(false);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForAccessPointUpdated() {
        setUpForConnectedNetwork();

        displayAndResume();

        // change as disconnected
        when(mMockAccessPoint.update(mMockWifiConfig, mMockWifiInfo, mMockNetworkInfo))
                .thenReturn(true);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void testRefreshRssiViews_shouldNotUpdateIfLevelIsSameForConnectedNetwork() {
        setUpForConnectedNetwork();
        displayAndResume();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mMockAccessPoint, times(3)).getLevel();
        verify(mMockIconInjector, times(1)).getIcon(anyInt());
    }

    @Test
    public void testRefreshRssiViews_shouldUpdateOnLevelChangeForConnectedNetwork() {
        setUpForConnectedNetwork();
        displayAndResume();

        when(mMockAccessPoint.getLevel()).thenReturn(0);
        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mMockAccessPoint, times(4)).getLevel();
        verify(mMockIconInjector, times(2)).getIcon(anyInt());
    }

    @Test
    public void testRefreshRssiViews_shouldNotUpdateForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        when(mMockAccessPoint.getLevel()).thenReturn(0);
        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mMockSignalStrengthPref, times(2)).setVisible(false);
    }

    @Test
    public void testRedrawIconForHeader_shouldEnlarge() {
        setUpForConnectedNetwork();
        ArgumentCaptor<BitmapDrawable> drawableCaptor =
                ArgumentCaptor.forClass(BitmapDrawable.class);
        Drawable original = mContext.getDrawable(Utils.getWifiIconResource(LEVEL)).mutate();
        when(mMockIconInjector.getIcon(anyInt())).thenReturn(original);

        displayAndResume();

        verify(mMockHeaderController, times(1)).setIcon(drawableCaptor.capture());

        int expectedSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.wifi_detail_page_header_image_size);
        BitmapDrawable icon = drawableCaptor.getValue();
        assertThat(icon.getMinimumWidth()).isEqualTo(expectedSize);
        assertThat(icon.getMinimumHeight()).isEqualTo(expectedSize);
    }

    @Test
    public void testRedrawIconForHeader_shouldEnlargeForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        ArgumentCaptor<BitmapDrawable> drawableCaptor =
                ArgumentCaptor.forClass(BitmapDrawable.class);
        Drawable original = mContext.getDrawable(Utils.getWifiIconResource(LEVEL)).mutate();
        when(mMockIconInjector.getIcon(anyInt())).thenReturn(original);

        displayAndResume();

        verify(mMockHeaderController, times(1)).setIcon(drawableCaptor.capture());

        int expectedSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.wifi_detail_page_header_image_size);
        BitmapDrawable icon = drawableCaptor.getValue();
        assertThat(icon.getMinimumWidth()).isEqualTo(expectedSize);
        assertThat(icon.getMinimumHeight()).isEqualTo(expectedSize);
    }

    @Test
    public void testRedrawIconForHeader_shouldNotEnlargeIfNotVectorDrawable() {
        setUpForConnectedNetwork();
        ArgumentCaptor<ColorDrawable> drawableCaptor =
                ArgumentCaptor.forClass(ColorDrawable.class);

        displayAndResume();

        verify(mMockHeaderController, times(1)).setIcon(drawableCaptor.capture());
        ColorDrawable icon = drawableCaptor.getValue();
        assertThat(icon).isNotNull();
    }

    @Test
    public void checkMacTitle_whenPrivacyRandomizedMac_shouldBeRandom() {
        setUpForDisconnectedNetwork();
        mMockWifiConfig.macRandomizationSetting = WifiConfiguration.RANDOMIZATION_PERSISTENT;
        when(mMockWifiConfig.getRandomizedMacAddress()).thenReturn(mMockMacAddress);
        when(mMockMacAddress.toString()).thenReturn(RANDOMIZED_MAC_ADDRESS);

        displayAndResume();

        verify(mMockMacAddressPref).setTitle(R.string.wifi_advanced_randomized_mac_address_title);
    }

    @Test
    public void checkMacTitle_whenPrivacyDeviceMac_shouldBeFactory() {
        setUpForDisconnectedNetwork();
        mMockWifiConfig.macRandomizationSetting = WifiConfiguration.RANDOMIZATION_NONE;
        when(mMockWifiConfig.getRandomizedMacAddress()).thenReturn(mMockMacAddress);
        when(mMockWifiManager.getFactoryMacAddresses())
                .thenReturn(new String[]{FACTORY_MAC_ADDRESS});

        displayAndResume();

        verify(mMockMacAddressPref).setTitle(R.string.wifi_advanced_device_mac_address_title);
    }

    @Test
    public void entityHeader_expiredPasspointR1_shouldHandleExpiration() {
        when(mMockAccessPoint.isPasspoint()).thenReturn(true);
        when(mMockAccessPoint.isPasspointConfigurationR1()).thenReturn(true);
        when(mMockAccessPoint.isExpired()).thenReturn(true);
        setUpForDisconnectedNetwork();
        String expireSummary = mContext.getResources().getString(
                com.android.settingslib.R.string.wifi_passpoint_expired);

        displayAndResume();

        verify(mMockButtonsPref, atLeastOnce()).setButton3Visible(false);
        verify(mMockHeaderController).setSummary(expireSummary);
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

        when(pref.setButton4Text(anyInt())).thenReturn(pref);
        when(pref.setButton4Icon(anyInt())).thenReturn(pref);
        when(pref.setButton4Enabled(anyBoolean())).thenReturn(pref);
        when(pref.setButton4Visible(anyBoolean())).thenReturn(pref);
        when(pref.setButton4OnClickListener(any(View.OnClickListener.class))).thenReturn(pref);

        return pref;
    }
}
