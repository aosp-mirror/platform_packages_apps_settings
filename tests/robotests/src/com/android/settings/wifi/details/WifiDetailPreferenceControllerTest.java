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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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
import android.net.CaptivePortalData;
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
import android.net.Uri;
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
import com.android.settingslib.utils.StringUtil;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDevicePolicyManager.class, ShadowEntityHeaderController.class})
public class WifiDetailPreferenceControllerTest {

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
    private WifiTracker mockWifiTracker;
    @Mock
    private MetricsFeatureProvider mockMetricsFeatureProvider;
    @Mock
    private WifiDetailPreferenceController.IconInjector mockIconInjector;
    @Mock
    private WifiDetailPreferenceController.Clock mMockClock;
    @Mock
    private MacAddress mockMacAddress;

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
    private Preference mockSsidPref;
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
        when(mockAccessPoint.getSsidStr()).thenReturn(SSID);
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
        when(mockHeaderController.setSummary(nullable(String.class)))
                .thenReturn(mockHeaderController);
        when(mockHeaderController.setSecondSummary(nullable(String.class)))
                .thenReturn(mockHeaderController);
        when(mockIconInjector.getIcon(anyInt())).thenReturn(new ColorDrawable());

        setupMockedPreferenceScreen();
    }

    private void setUpForConnectedNetwork() {
        when(mockAccessPoint.isActive()).thenReturn(true);
        ArrayList list = new ArrayList<>();
        list.add(mockAccessPoint);
        when(mockWifiTracker.getAccessPoints()).thenReturn(list);
        WifiTrackerFactory.setTestingWifiTracker(mockWifiTracker);
        when(mockAccessPoint.matches(any(AccessPoint.class))).thenReturn(true);
        when(mockAccessPoint.isReachable()).thenReturn(true);

        mController = newWifiDetailPreferenceController();
    }

    private void setUpForDisconnectedNetwork() {
        when(mockAccessPoint.isActive()).thenReturn(false);
        ArrayList list = new ArrayList<>();
        list.add(mockAccessPoint);
        when(mockWifiTracker.getAccessPoints()).thenReturn(list);
        WifiTrackerFactory.setTestingWifiTracker(mockWifiTracker);
        when(mockAccessPoint.matches(any(AccessPoint.class))).thenReturn(true);
        when(mockAccessPoint.isReachable()).thenReturn(true);

        mController = newWifiDetailPreferenceController();
    }

    private void setUpForNotInRangeNetwork() {
        when(mockAccessPoint.isActive()).thenReturn(false);
        ArrayList list = new ArrayList<>();
        list.add(mockAccessPoint);
        when(mockWifiTracker.getAccessPoints()).thenReturn(list);
        WifiTrackerFactory.setTestingWifiTracker(mockWifiTracker);
        when(mockAccessPoint.matches(any(AccessPoint.class))).thenReturn(false);
        when(mockAccessPoint.isReachable()).thenReturn(false);

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
                mockIconInjector,
                mMockClock);
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
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_SSID_PREF))
                .thenReturn(mockSsidPref);
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
        setUpForConnectedNetwork();
        mController.displayPreference(mockScreen);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void securityPreference_stringShouldBeSet() {
        setUpForConnectedNetwork();
        displayAndResume();

        verify(mockSecurityPref).setSummary(SECURITY);
    }

    @Test
    public void latestWifiInfo_shouldBeFetchedInDisplayPreferenceForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mockWifiManager, times(1)).getConnectionInfo();
    }

    @Test
    public void latestWifiInfo_shouldNotBeFetchedInDisplayPreferenceForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockWifiManager, never()).getConnectionInfo();
    }

    @Test
    public void latestWifiInfo_shouldNotBeFetchedInDisplayPreferenceForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mockWifiManager, never()).getConnectionInfo();
    }

    @Test
    public void latestNetworkInfo_shouldBeFetchedInDisplayPreferenceForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
    }

    @Test
    public void latestNetworkInfo_shouldNotBeFetchedInDisplayPreferenceForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockConnectivityManager, never()).getNetworkInfo(any(Network.class));
    }

    @Test
    public void latestNetworkInfo_shouldNotBeFetchedInDisplayPreferenceForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mockConnectivityManager, never()).getNetworkInfo(any(Network.class));
    }

    @Test
    public void networkCallback_shouldBeRegisteredOnResume() {
        setUpForConnectedNetwork();
        displayAndResume();

        verify(mockConnectivityManager, times(1)).registerNetworkCallback(
                nullable(NetworkRequest.class), mCallbackCaptor.capture(), nullable(Handler.class));
    }

    @Test
    public void networkCallback_shouldBeUnregisteredOnPause() {
        setUpForConnectedNetwork();
        displayAndResume();
        mController.onPause();

        verify(mockConnectivityManager, times(1))
                .unregisterNetworkCallback(mCallbackCaptor.getValue());
    }

    @Test
    public void entityHeader_shouldHaveIconSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        Drawable expectedIcon = mockIconInjector.getIcon(LEVEL);

        displayAndResume();

        verify(mockHeaderController).setIcon(expectedIcon);
    }

    @Test
    public void entityHeader_shouldHaveIconSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        Drawable expectedIcon = mockIconInjector.getIcon(LEVEL);

        displayAndResume();

        verify(mockHeaderController).setIcon(expectedIcon);
    }

    @Test
    public void entityHeader_shouldNotHaveIconSetForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mockHeaderController, never()).setIcon(any(Drawable.class));
    }

    @Test
    public void entityHeader_shouldHaveLabelSetToTitle() {
        setUpForConnectedNetwork();
        String label = "title";
        when(mockAccessPoint.getTitle()).thenReturn(label);

        displayAndResume();

        verify(mockHeaderController).setLabel(label);
    }

    @Test
    public void entityHeader_shouldHaveSummarySet() {
        setUpForConnectedNetwork();
        String summary = "summary";
        when(mockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);

        displayAndResume();

        verify(mockHeaderController).setSummary(summary);
    }

    private void doShouldShowRemainingTimeTest(ZonedDateTime now, long timeRemainingMs) {
        when(mMockClock.now()).thenReturn(now);
        setUpForConnectedNetwork();
        displayAndResume();

        final CaptivePortalData data = new CaptivePortalData.Builder()
                .setExpiryTime(now.toInstant().getEpochSecond() * 1000 + timeRemainingMs)
                .build();
        final LinkProperties lp = new LinkProperties();
        lp.setCaptivePortalData(data);

        updateLinkProperties(lp);
    }

    @Test
    public void entityHeader_shouldShowShortRemainingTime() {
        // Expires in 1h, 2min, 15sec
        final long timeRemainingMs = (3600 + 2 * 60 + 15) * 1000;
        final ZonedDateTime fakeNow = ZonedDateTime.of(2020, 1, 2, 3, 4, 5, 6,
                ZoneId.of("Europe/London"));
        doShouldShowRemainingTimeTest(fakeNow, timeRemainingMs);
        final String expectedSummary = mContext.getString(R.string.wifi_time_remaining,
                StringUtil.formatElapsedTime(mContext, timeRemainingMs, false /* withSeconds */));
        final InOrder inOrder = inOrder(mockHeaderController);
        inOrder.verify(mockHeaderController).setSecondSummary(expectedSummary);

        updateLinkProperties(new LinkProperties());
        inOrder.verify(mockHeaderController).setSecondSummary((String) null);
    }

    @Test
    public void entityHeader_shouldShowExpiryDate() {
        // Expires in 49h, 2min, 15sec
        final long timeRemainingMs = (49 * 3600 + 2 * 60 + 15) * 1000;
        final ZonedDateTime fakeNow = ZonedDateTime.of(2020, 1, 2, 3, 4, 5, 6,
                ZoneId.of("Europe/London"));
        doShouldShowRemainingTimeTest(fakeNow, timeRemainingMs);
        final String expectedSummary = mContext.getString(
                R.string.wifi_expiry_time,
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(
                        fakeNow.plusNanos(timeRemainingMs * 1_000_000)));
        final InOrder inOrder = inOrder(mockHeaderController);
        inOrder.verify(mockHeaderController).setSecondSummary(expectedSummary);

        updateLinkProperties(new LinkProperties());
        inOrder.verify(mockHeaderController).setSecondSummary((String) null);
    }

    @Test
    public void entityHeader_shouldConvertSavedAsDisconnected() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockAccessPoint, times(1)).getSettingsSummary(true /*convertSavedAsDisconnected*/);
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSetForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldNotHaveIconSetForOutOfRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mockSignalStrengthPref, never()).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        displayAndResume();

        verify(mockSignalStrengthPref).setSummary(expectedStrength);
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        displayAndResume();

        verify(mockSignalStrengthPref).setSummary(expectedStrength);
    }

    @Test
    public void signalStrengthPref_shouldNotHaveDetailTextSetForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mockSignalStrengthPref, never()).setSummary(any(String.class));
    }

    @Test
    public void linkSpeedPref_shouldNotShowIfNotSet() {
        setUpForConnectedNetwork();
        when(mockWifiInfo.getTxLinkSpeedMbps()).thenReturn(WifiInfo.LINK_SPEED_UNKNOWN);

        displayAndResume();

        verify(mockTxLinkSpeedPref).setVisible(false);
    }

    @Test
    public void linkSpeedPref_shouldVisibleForConnectedNetwork() {
        setUpForConnectedNetwork();
        String expectedLinkSpeed = mContext.getString(R.string.tx_link_speed, TX_LINK_SPEED);

        displayAndResume();

        verify(mockTxLinkSpeedPref).setVisible(true);
        verify(mockTxLinkSpeedPref).setSummary(expectedLinkSpeed);
    }

    @Test
    public void linkSpeedPref_shouldInvisibleForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockTxLinkSpeedPref).setVisible(false);
        verify(mockTxLinkSpeedPref, never()).setSummary(any(String.class));
    }

    @Test
    public void linkSpeedPref_shouldInvisibleForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mockTxLinkSpeedPref).setVisible(false);
        verify(mockTxLinkSpeedPref, never()).setSummary(any(String.class));
    }

    @Test
    public void rxLinkSpeedPref_shouldNotShowIfNotSet() {
        setUpForConnectedNetwork();
        when(mockWifiInfo.getRxLinkSpeedMbps()).thenReturn(WifiInfo.LINK_SPEED_UNKNOWN);

        displayAndResume();

        verify(mockRxLinkSpeedPref).setVisible(false);
    }

    @Test
    public void rxLinkSpeedPref_shouldVisibleForConnectedNetwork() {
        setUpForConnectedNetwork();
        String expectedLinkSpeed = mContext.getString(R.string.rx_link_speed, RX_LINK_SPEED);

        displayAndResume();

        verify(mockRxLinkSpeedPref).setVisible(true);
        verify(mockRxLinkSpeedPref).setSummary(expectedLinkSpeed);
    }

    @Test
    public void rxLinkSpeedPref_shouldInvisibleForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockRxLinkSpeedPref).setVisible(false);
        verify(mockRxLinkSpeedPref, never()).setSummary(any(String.class));
    }

    @Test
    public void rxLinkSpeedPref_shouldInvisibleForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mockRxLinkSpeedPref).setVisible(false);
        verify(mockRxLinkSpeedPref, never()).setSummary(any(String.class));
    }

    @Test
    public void ssidPref_shouldHaveDetailTextSetForPasspointR1() {
        setUpForConnectedNetwork();
        when(mockAccessPoint.isPasspoint()).thenReturn(true);
        when(mockAccessPoint.isOsuProvider()).thenReturn(false);

        displayAndResume();

        verify(mockSsidPref, times(1)).setSummary(SSID);
        verify(mockSsidPref, times(1)).setVisible(true);
    }

    @Test
    public void ssidPref_shouldHaveDetailTextSetForPasspointR2() {
        setUpForConnectedNetwork();
        when(mockAccessPoint.isPasspoint()).thenReturn(false);
        when(mockAccessPoint.isOsuProvider()).thenReturn(true);

        displayAndResume();

        verify(mockSsidPref, times(1)).setSummary(SSID);
        verify(mockSsidPref, times(1)).setVisible(true);
    }

    @Test
    public void ssidPref_shouldNotShowIfNotPasspoint() {
        setUpForConnectedNetwork();
        when(mockAccessPoint.isPasspoint()).thenReturn(false);
        when(mockAccessPoint.isOsuProvider()).thenReturn(false);

        displayAndResume();

        verify(mockSsidPref).setVisible(false);
    }

    @Test
    public void macAddressPref_shouldVisibleForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mockMacAddressPref).setVisible(true);
        verify(mockMacAddressPref).setSummary(MAC_ADDRESS);
    }

    @Test
    public void macAddressPref_shouldVisibleAsRandomizedForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        mockWifiConfig.macRandomizationSetting = WifiConfiguration.RANDOMIZATION_PERSISTENT;
        when(mockWifiConfig.getRandomizedMacAddress()).thenReturn(mockMacAddress);
        when(mockMacAddress.toString()).thenReturn(RANDOMIZED_MAC_ADDRESS);

        displayAndResume();

        verify(mockMacAddressPref).setVisible(true);
        verify(mockMacAddressPref).setSummary(RANDOMIZED_MAC_ADDRESS);
    }

    @Test
    public void macAddressPref_shouldVisibleAsFactoryForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        mockWifiConfig.macRandomizationSetting = WifiConfiguration.RANDOMIZATION_NONE;
        when(mockWifiManager.getFactoryMacAddresses())
                .thenReturn(new String[]{FACTORY_MAC_ADDRESS});

        displayAndResume();

        verify(mockMacAddressPref).setVisible(true);
        verify(mockMacAddressPref).setSummary(FACTORY_MAC_ADDRESS);
    }

    @Test
    public void ipAddressPref_shouldHaveDetailTextSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        mLinkProperties.addLinkAddress(Constants.IPV4_ADDR);

        displayAndResume();

        verify(mockIpAddressPref).setSummary(Constants.IPV4_ADDR.getAddress().getHostAddress());
        verify(mockIpAddressPref).setVisible(true);
    }

    @Test
    public void ipAddressPref_shouldInvisibleForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockIpAddressPref).setVisible(false);
    }

    @Test
    public void gatewayAndSubnet_shouldHaveDetailTextSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        mLinkProperties.addLinkAddress(Constants.IPV4_ADDR);
        mLinkProperties.addRoute(Constants.IPV4_DEFAULT);
        mLinkProperties.addRoute(Constants.IPV4_SUBNET);

        displayAndResume();

        verify(mockSubnetPref).setSummary("255.255.255.128");
        verify(mockGatewayPref).setSummary("192.0.2.127");
        verify(mockSubnetPref).setVisible(true);
    }

    @Test
    public void gatewayAndSubnet_shouldInvisibleSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockSubnetPref).setVisible(false);
    }

    @Test
    public void dnsServersPref_shouldHaveDetailTextSetForConnectedNetwork()
            throws UnknownHostException {
        setUpForConnectedNetwork();
        mLinkProperties.addDnsServer(InetAddress.getByAddress(new byte[] {8, 8, 4, 4}));
        mLinkProperties.addDnsServer(InetAddress.getByAddress(new byte[] {8, 8, 8, 8}));
        mLinkProperties.addDnsServer(Constants.IPV6_DNS);

        displayAndResume();

        verify(mockDnsPref).setSummary(
                "8.8.4.4\n" +
                        "8.8.8.8\n" +
                        Constants.IPV6_DNS.getHostAddress());
        verify(mockDnsPref).setVisible(true);
    }

    @Test
    public void dnsServersPref_shouldInvisibleSetForDisconnectedNetwork()
            throws UnknownHostException {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockDnsPref).setVisible(false);
    }

    @Test
    public void noCurrentNetwork_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();
        when(mockWifiManager.getCurrentNetwork()).thenReturn(null);

        displayAndResume();

        verify(mockActivity, never()).finish();
    }

    @Test
    public void noLinkProperties_allIpDetailsHidden() {
        setUpForConnectedNetwork();
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

    @Test
    public void disconnectedNetwork_allIpDetailsHidden() {
        setUpForDisconnectedNetwork();
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
        setUpForConnectedNetwork();
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
        setUpForConnectedNetwork();
        NetworkCapabilities nc = makeNetworkCapabilities();
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(new NetworkCapabilities(nc));

        String summary = "Connected, no Internet";
        when(mockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);

        InOrder inOrder = inOrder(mockHeaderController);
        displayAndResume();
        inOrder.verify(mockHeaderController).setSummary(summary);

        // Check that an irrelevant capability update does not update the access point summary, as
        // doing so could cause unnecessary jank...
        summary = "Connected";
        when(mockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController, never()).setSummary(any(CharSequence.class));

        // ... but that if the network validates, then we do refresh.
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController).setSummary(summary);

        summary = "Connected, no Internet";
        when(mockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);

        // Another irrelevant update won't cause the UI to refresh...
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController, never()).setSummary(any(CharSequence.class));

        // ... but if the network is no longer validated, then we display "connected, no Internet".
        nc.removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController).setSummary(summary);

        // UI will be refreshed when private DNS is broken.
        summary = "Private DNS server cannot be accessed";
        when(mockAccessPoint.getSettingsSummary(true /* convertSavedAsDisconnected */))
                .thenReturn(summary);
        nc.setPrivateDnsBroken(true);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController).setSummary(summary);

        // UI will be refreshed when device connects to a partial connectivity network.
        summary = "Limited connection";
        when(mockAccessPoint.getSettingsSummary(true /*convertSavedAsDisconnected*/))
                .thenReturn(summary);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController).setSummary(summary);

        // Although UI will be refreshed when network become validated. The Settings should
        // continue to display "Limited connection" if network still provides partial connectivity.
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockHeaderController).setSummary(summary);
    }

    @Test
    public void canForgetNetwork_shouldInvisibleIfWithoutConfiguration() {
        setUpForConnectedNetwork();
        when(mockAccessPoint.getConfig()).thenReturn(null);
        mController = newWifiDetailPreferenceController();

        displayAndResume();

        verify(mockButtonsPref).setButton1Visible(false);
    }

    @Test
    public void canForgetNetwork_ephemeral() {
        setUpForConnectedNetwork();
        when(mockWifiInfo.isEphemeral()).thenReturn(true);
        when(mockAccessPoint.getConfig()).thenReturn(null);

        displayAndResume();

        verify(mockButtonsPref).setButton1Visible(true);
    }

    @Test
    public void canForgetNetwork_saved() {
        setUpForConnectedNetwork();
        displayAndResume();

        verify(mockButtonsPref).setButton1Visible(true);
    }

    @Test
    public void canForgetNetwork_lockedDown() {
        setUpForConnectedNetwork();
        lockDownNetwork();

        displayAndResume();

        verify(mockButtonsPref).setButton1Visible(false);
    }

    @Test
    public void canShareNetwork_shouldInvisibleIfWithoutConfiguration() {
        setUpForConnectedNetwork();
        when(mockAccessPoint.getConfig()).thenReturn(null);

        displayAndResume();

        verify(mockButtonsPref).setButton4Visible(false);
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
        setUpForConnectedNetwork();
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
        setUpForConnectedNetwork();
        mockWifiConfig.networkId = 5;

        mController.displayPreference(mockScreen);
        mForgetClickListener.getValue().onClick(null);

        verify(mockWifiManager).forget(mockWifiConfig.networkId, null);
        verify(mockMetricsFeatureProvider)
                .action(mockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
    }

    @Test
    public void forgetNetwork_shouldShowDialog() {
        setUpForConnectedNetwork();
        final WifiDetailPreferenceController spyController = spy(mController);

        mockWifiConfig.networkId = 5;
        when(mockAccessPoint.isPasspoint()).thenReturn(true);
        when(mockAccessPoint.getPasspointFqdn()).thenReturn(FQDN);
        spyController.displayPreference(mockScreen);

        mForgetClickListener.getValue().onClick(null);

        verify(mockWifiManager, times(0)).removePasspointConfiguration(FQDN);
        verify(mockMetricsFeatureProvider, times(0))
                .action(mockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
        verify(spyController).showConfirmForgetDialog();
    }

    @Test
    public void networkStateChangedIntent_shouldRefetchInfo() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void networkStateChangedIntent_shouldRefetchInfoForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void rssiChangedIntent_shouldRefetchInfo() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void rssiChangedIntent_shouldRefetchInfoForConnectedNetwork() {
        setUpForConnectedNetwork();
        displayAndResume();

        verify(mockConnectivityManager, times(1)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(1)).getConnectionInfo();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockConnectivityManager, times(2)).getNetworkInfo(any(Network.class));
        verify(mockWifiManager, times(2)).getConnectionInfo();
    }

    @Test
    public void networkDisconnectedState_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        when(mockConnectivityManager.getNetworkInfo(any(Network.class))).thenReturn(null);
        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mockActivity, never()).finish();
    }

    @Test
    public void networkOnLost_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        mCallbackCaptor.getValue().onLost(mockNetwork);

        verify(mockActivity, never()).finish();
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

        verify(mockIpv6AddressesPref).setSummary(expectedAddresses);
    }

    @Test
    public void ipv6AddressPref_shouldNotBeSelectable() {
        setUpForConnectedNetwork();
        mLinkProperties.addLinkAddress(Constants.IPV6_GLOBAL2);

        displayAndResume();

        assertThat(mockIpv6AddressesPref.isSelectable()).isFalse();
    }

    @Test
    public void captivePortal_shouldShowSignInButton() {
        setUpForConnectedNetwork();

        InOrder inOrder = inOrder(mockButtonsPref);

        displayAndResume();

        inOrder.verify(mockButtonsPref).setButton2Visible(false);

        NetworkCapabilities nc = makeNetworkCapabilities();
        updateNetworkCapabilities(nc);
        inOrder.verify(mockButtonsPref).setButton2Visible(false);

        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        updateNetworkCapabilities(nc);

        inOrder.verify(mockButtonsPref).setButton2Text(R.string.wifi_sign_in_button_text);
        inOrder.verify(mockButtonsPref).setButton2Visible(true);

        nc.removeCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        updateNetworkCapabilities(nc);
        inOrder.verify(mockButtonsPref).setButton2Visible(false);
    }

    @Test
    public void captivePortal_shouldShowVenueInfoButton() {
        setUpForConnectedNetwork();

        InOrder inOrder = inOrder(mockButtonsPref);

        displayAndResume();

        inOrder.verify(mockButtonsPref).setButton2Visible(false);

        LinkProperties lp = new LinkProperties();
        final CaptivePortalData data = new CaptivePortalData.Builder()
                .setVenueInfoUrl(Uri.parse("https://example.com/info"))
                .build();
        lp.setCaptivePortalData(data);
        updateLinkProperties(lp);

        inOrder.verify(mockButtonsPref).setButton2Text(R.string.wifi_venue_website_button_text);
        inOrder.verify(mockButtonsPref).setButton2Visible(true);

        lp.setCaptivePortalData(null);
        updateLinkProperties(lp);
        inOrder.verify(mockButtonsPref).setButton2Visible(false);
    }

    @Test
    public void testSignInButton_shouldStartCaptivePortalApp() {
        setUpForConnectedNetwork();

        displayAndResume();

        ArgumentCaptor<OnClickListener> captor = ArgumentCaptor.forClass(OnClickListener.class);
        verify(mockButtonsPref, atLeastOnce()).setButton2OnClickListener(captor.capture());
        // getValue() returns the last captured value
        captor.getValue().onClick(null);
        verify(mockConnectivityManager).startCaptivePortalApp(mockNetwork);
        verify(mockMetricsFeatureProvider)
                .action(mockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_SIGNIN);
    }

    @Test
    public void testSignInButton_shouldHideSignInButtonForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        NetworkCapabilities nc = makeNetworkCapabilities();
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
                .thenReturn(new NetworkCapabilities(nc));

        // verify onResume
        displayAndResume();

        verify(mockButtonsPref, never()).setButton2Visible(true);
        verify(mockButtonsPref).setButton2Visible(false);

        // verify onCapabilitiesChanged
        updateNetworkCapabilities(nc);

        verify(mockButtonsPref, never()).setButton2Visible(true);
        verify(mockButtonsPref).setButton2Visible(false);
    }

    @Test
    public void testConnectButton_shouldInvisibleForConnectNetwork() {
        setUpForConnectedNetwork();

        displayAndResume();

        verify(mockButtonsPref, times(1)).setButton3Visible(false);
    }

    @Test
    public void testConnectButton_shouldVisibleForDisconnectNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mockButtonsPref, times(1)).setButton3Visible(true);
        verify(mockButtonsPref, times(1)).setButton3Text(R.string.wifi_connect);
    }

    private void setUpForToast() {
        Resources res = mContext.getResources();
        when(mockActivity.getResources()).thenReturn(res);
    }

    @Test
    public void testConnectButton_clickConnect_displayAsSuccess() {
        setUpForDisconnectedNetwork();
        when(mockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mockButtonsPref);
        String label = "title";
        when(mockAccessPoint.getTitle()).thenReturn(label);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as connected
        when(mockAccessPoint.isActive()).thenReturn(true);
        mController.updateAccessPoint();

        // check connect button invisible, be init as default state and toast success message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(false);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_connected_to_message, label));
    }

    @Test
    public void testConnectButton_clickConnectButFailed_displayFailMessage() {
        setUpForDisconnectedNetwork();
        ArgumentCaptor<WifiManager.ActionListener> connectListenerCaptor =
                ArgumentCaptor.forClass(WifiManager.ActionListener.class);
        when(mockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mockWifiManager, times(1)).connect(anyInt(), connectListenerCaptor.capture());
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        connectListenerCaptor.getValue().onFailure(-1);

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    private void verifyConnectBtnSetUpAsVisible(InOrder inOrder) {
        inOrder.verify(mockButtonsPref, times(1)).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Icon(R.drawable.ic_settings_wireless);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(true);
    }

    private void verifyConnectBtnSetUpAsConnecting(InOrder inOrder) {
        inOrder.verify(mockButtonsPref, times(1)).setButton3Text(R.string.wifi_connecting);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Enabled(false);
    }

    private void verifyConnectBtnBeInitAsDefault(InOrder inOrder) {
        inOrder.verify(mockButtonsPref, times(1)).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Icon(R.drawable.ic_settings_wireless);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Enabled(true);
    }

    @Test
    public void testConnectButton_clickConnectButTimeout_displayFailMessage() {
        setUpForDisconnectedNetwork();
        when(mockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        mController.mTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    @Test
    public void testConnectButton_clickConnectButTimeout_displayNotInRangeMessage() {
        setUpForNotInRangeNetwork();
        when(mockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        mController.mTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_not_in_range_message));
    }

    @Test
    public void testConnectButton_clickConnectWhenWiFiDisabled_displaySuccessMessage() {
        setUpForDisconnectedNetwork();
        when(mockWifiManager.isWifiEnabled()).thenReturn(false); // wifi disabled
        InOrder inOrder = inOrder(mockButtonsPref);
        String label = "title";
        when(mockAccessPoint.getTitle()).thenReturn(label);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check turn on Wi-Fi, display button as connecting and toast turn on Wi-Fi message
        verify(mockWifiManager, times(1)).setWifiEnabled(true);
        verifyConnectBtnSetUpAsConnecting(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_turned_on_message));

        // notify Wi-Fi enabled
        mController.mWifiListener.onWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        // check had connect network and icon display as expected
        verify(mockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as connected
        when(mockAccessPoint.isActive()).thenReturn(true);
        mController.updateAccessPoint();

        // check connect button invisible, be init as default state and toast success message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(false);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_connected_to_message, label));
    }

    @Test
    public void testConnectButton_clickConnectWhenWiFiDisabled_failedToConnectWiFi() {
        setUpForDisconnectedNetwork();
        when(mockWifiManager.isWifiEnabled()).thenReturn(false); // wifi disabled
        InOrder inOrder = inOrder(mockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check turn on Wi-Fi, display button as connecting and toast turn on Wi-Fi message
        verify(mockWifiManager, times(1)).setWifiEnabled(true);
        verifyConnectBtnSetUpAsConnecting(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_turned_on_message));

        // notify Wi-Fi enabled
        mController.mWifiListener.onWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        // check had connect network and icon display as expected
        verify(mockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        mController.mTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    @Test
    public void
            testConnectButton_clickConnectWhenWiFiDisabled_failedToConnectWifiBecauseNotInRange() {
        setUpForNotInRangeNetwork();
        when(mockWifiManager.isWifiEnabled()).thenReturn(false); // wifi disabled
        InOrder inOrder = inOrder(mockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check turn on Wi-Fi, display button as connecting and toast turn on Wi-Fi message
        verify(mockWifiManager, times(1)).setWifiEnabled(true);
        verifyConnectBtnSetUpAsConnecting(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_turned_on_message));

        // notify Wi-Fi enabled
        mController.mWifiListener.onWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        // check had connect network and icon display as expected
        verify(mockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // update as failed
        mController.mTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_not_in_range_message));
    }

    @Test
    public void testConnectButton_clickConnectWhenWiFiDisabled_failedToEnableWifi() {
        setUpForDisconnectedNetwork();
        when(mockWifiManager.isWifiEnabled()).thenReturn(false); // wifi disabled
        InOrder inOrder = inOrder(mockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check turn on Wi-Fi, display button as connecting and toast turn on Wi-Fi message
        verify(mockWifiManager, times(1)).setWifiEnabled(true);
        verifyConnectBtnSetUpAsConnecting(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_turned_on_message));

        // notify turn on Wi-Fi failed
        mController.mTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        verifyConnectBtnBeInitAsDefault(inOrder);
        inOrder.verify(mockButtonsPref, times(1)).setButton3Visible(true);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    @Test
    public void testConnectButton_clickConnectAndBackKey_ignoreTimeoutEvent() {
        setUpForDisconnectedNetwork();
        when(mockWifiManager.isWifiEnabled()).thenReturn(true);
        InOrder inOrder = inOrder(mockButtonsPref);
        setUpForToast();

        displayAndResume();

        // check connect button exist
        verifyConnectBtnSetUpAsVisible(inOrder);

        // click connect button
        mController.connectNetwork();

        // check display button as connecting
        verify(mockWifiManager, times(1)).connect(anyInt(), any(WifiManager.ActionListener.class));
        verifyConnectBtnSetUpAsConnecting(inOrder);

        // leave detail page
        when(mockFragment.getActivity()).thenReturn(null);

        // timeout happened
        mController.mTimer.onFinish();

        // check connect button visible, be init as default and toast failed message
        inOrder.verify(mockButtonsPref, never()).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mockButtonsPref, never()).setButton3Icon(R.drawable.ic_settings_wireless);
        inOrder.verify(mockButtonsPref, never()).setButton3Enabled(true);
        inOrder.verify(mockButtonsPref, never()).setButton3Visible(true);
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
        when(mockAccessPoint.getLevel()).thenReturn(LEVEL + 1);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForChangeAsNotInRange() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        // change as not in range
        when(mockAccessPoint.matches(any(AccessPoint.class))).thenReturn(false);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForChangeAsInRange() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        // change as in range
        when(mockAccessPoint.matches(any(AccessPoint.class))).thenReturn(true);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForChangeAsConnected() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        // change as connected
        when(mockAccessPoint.isActive()).thenReturn(true);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForChangeAsDisconnected() {
        setUpForConnectedNetwork();

        displayAndResume();

        // change as disconnected
        when(mockAccessPoint.isActive()).thenReturn(false);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void updateAccessPoint_returnTrueForAccessPointUpdated() {
        setUpForConnectedNetwork();

        displayAndResume();

        // change as disconnected
        when(mockAccessPoint.update(mockWifiConfig, mockWifiInfo, mockNetworkInfo))
                .thenReturn(true);
        boolean changed = mController.updateAccessPoint();

        assertThat(changed).isTrue();
    }

    @Test
    public void testRefreshRssiViews_shouldNotUpdateIfLevelIsSameForConnectedNetwork() {
        setUpForConnectedNetwork();
        displayAndResume();

        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockAccessPoint, times(3)).getLevel();
        verify(mockIconInjector, times(1)).getIcon(anyInt());
    }

    @Test
    public void testRefreshRssiViews_shouldUpdateOnLevelChangeForConnectedNetwork() {
        setUpForConnectedNetwork();
        displayAndResume();

        when(mockAccessPoint.getLevel()).thenReturn(0);
        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockAccessPoint, times(4)).getLevel();
        verify(mockIconInjector, times(2)).getIcon(anyInt());
    }

    @Test
    public void testRefreshRssiViews_shouldNotUpdateForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        when(mockAccessPoint.getLevel()).thenReturn(0);
        mContext.sendBroadcast(new Intent(WifiManager.RSSI_CHANGED_ACTION));

        verify(mockSignalStrengthPref, times(2)).setVisible(false);
    }

    @Test
    public void testRedrawIconForHeader_shouldEnlarge() {
        setUpForConnectedNetwork();
        ArgumentCaptor<BitmapDrawable> drawableCaptor =
                ArgumentCaptor.forClass(BitmapDrawable.class);
        Drawable original = mContext.getDrawable(Utils.getWifiIconResource(LEVEL)).mutate();
        when(mockIconInjector.getIcon(anyInt())).thenReturn(original);

        displayAndResume();

        verify(mockHeaderController, times(1)).setIcon(drawableCaptor.capture());

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
        when(mockIconInjector.getIcon(anyInt())).thenReturn(original);

        displayAndResume();

        verify(mockHeaderController, times(1)).setIcon(drawableCaptor.capture());

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

        verify(mockHeaderController, times(1)).setIcon(drawableCaptor.capture());
        ColorDrawable icon = drawableCaptor.getValue();
        assertThat(icon).isNotNull();
    }

    @Test
    public void checkMacTitle_whenPrivacyRandomizedMac_shouldBeRandom() {
        setUpForDisconnectedNetwork();
        mockWifiConfig.macRandomizationSetting = WifiConfiguration.RANDOMIZATION_PERSISTENT;
        when(mockWifiConfig.getRandomizedMacAddress()).thenReturn(mockMacAddress);
        when(mockMacAddress.toString()).thenReturn(RANDOMIZED_MAC_ADDRESS);

        displayAndResume();

        verify(mockMacAddressPref).setTitle(R.string.wifi_advanced_randomized_mac_address_title);
    }

    @Test
    public void checkMacTitle_whenPrivacyDeviceMac_shouldBeFactory() {
        setUpForDisconnectedNetwork();
        mockWifiConfig.macRandomizationSetting = WifiConfiguration.RANDOMIZATION_NONE;
        when(mockWifiConfig.getRandomizedMacAddress()).thenReturn(mockMacAddress);
        when(mockWifiManager.getFactoryMacAddresses())
                .thenReturn(new String[]{FACTORY_MAC_ADDRESS});

        displayAndResume();

        verify(mockMacAddressPref).setTitle(R.string.wifi_advanced_device_mac_address_title);
    }

    @Test
    public void entityHeader_expiredPasspointR1_shouldHandleExpiration() {
        when(mockAccessPoint.isPasspoint()).thenReturn(true);
        when(mockAccessPoint.isPasspointConfigurationR1()).thenReturn(true);
        when(mockAccessPoint.isExpired()).thenReturn(true);
        setUpForDisconnectedNetwork();
        String expireSummary = mContext.getResources().getString(
                com.android.settingslib.R.string.wifi_passpoint_expired);

        displayAndResume();

        verify(mockButtonsPref, atLeastOnce()).setButton3Visible(false);
        verify(mockHeaderController).setSummary(expireSummary);
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
