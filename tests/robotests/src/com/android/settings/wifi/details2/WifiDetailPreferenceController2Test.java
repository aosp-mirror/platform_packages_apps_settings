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

import static android.net.wifi.sharedconnectivity.app.NetworkProviderInfo.DEVICE_TYPE_PHONE;

import static com.android.settingslib.wifi.WifiUtils.getHotspotIconResource;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.nullable;
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
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.RouteInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
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
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.ActionButtonsPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiEntry.ConnectCallback;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TODO(b/143326832): Should add test cases for connect button.
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowDevicePolicyManager.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowEntityHeaderController.class})
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
    private WifiEntry mMockWifiEntry;
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
    private WifiNetworkDetailsFragment mMockFragment;
    @Mock
    private WifiManager mMockWifiManager;
    @Mock
    private NetworkDetailsTracker mMockNetworkDetailsTracker;
    @Mock
    private MetricsFeatureProvider mMockMetricsFeatureProvider;
    @Mock
    private WifiDetailPreferenceController2.IconInjector mMockIconInjector;
    @Mock
    private WifiDetailPreferenceController2.Clock mMockClock;

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
    private Preference mMockEapSimSubscriptionPref;
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
    private Preference mMockTypePref;
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
                IPV4_SUBNET = new RouteInfo(subnet, any4, null /* iface */, RouteInfo.RTN_UNICAST);
                IPV4_DEFAULT = new RouteInfo(new IpPrefix(any4, 0), IPV4_GATEWAY, null /* iface */,
                        RouteInfo.RTN_UNICAST);

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
        when(mMockWifiEntry.getLevel()).thenReturn(LEVEL);
        when(mMockWifiEntry.getSecurityString(false /* concise */)).thenReturn(SECURITY);
        when(mMockWifiEntry.getTitle()).thenReturn(SSID);
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mMockWifiConfig);
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
        when(mMockHeaderController.setLabel(any(CharSequence.class)))
                .thenReturn(mMockHeaderController);
        when(mMockHeaderController.setSummary(nullable(String.class)))
                .thenReturn(mMockHeaderController);
        when(mMockHeaderController.setSecondSummary(nullable(String.class)))
                .thenReturn(mMockHeaderController);
        when(mMockIconInjector.getIcon(anyBoolean(), anyInt())).thenReturn(new ColorDrawable());

        setupMockedPreferenceScreen();
    }

    private void setUpForConnectedNetwork() {
        when(mMockNetworkDetailsTracker.getWifiEntry()).thenReturn(mMockWifiEntry);
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTED);
    }

    private void setUpController() {
        mController = new WifiDetailPreferenceController2(
                mMockWifiEntry,
                mMockConnectivityManager,
                mContext,
                mMockFragment,
                null,  // Handler
                mLifecycle,
                mMockWifiManager,
                mMockMetricsFeatureProvider,
                mMockIconInjector,
                mMockClock);
    }

    private void setUpSpyController() {
        mController = newSpyWifiDetailPreferenceController2();
    }

    private void setUpForDisconnectedNetwork() {
        when(mMockNetworkDetailsTracker.getWifiEntry()).thenReturn(mMockWifiEntry);
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_DISCONNECTED);

        mController = newSpyWifiDetailPreferenceController2();
    }

    private void setUpForNotInRangeNetwork() {
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_DISCONNECTED);
        when(mMockNetworkDetailsTracker.getWifiEntry()).thenReturn(mMockWifiEntry);
        when(mMockWifiEntry.getLevel()).thenReturn(WifiEntry.WIFI_LEVEL_UNREACHABLE);

        mController = newSpyWifiDetailPreferenceController2();
    }

    private WifiDetailPreferenceController2 newSpyWifiDetailPreferenceController2() {
        return spy(new WifiDetailPreferenceController2(
                mMockWifiEntry,
                mMockConnectivityManager,
                mContext,
                mMockFragment,
                null,  // Handler
                mLifecycle,
                mMockWifiManager,
                mMockMetricsFeatureProvider,
                mMockIconInjector,
                mMockClock));
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
        when(mMockScreen.findPreference(WifiDetailPreferenceController2
                .KEY_EAP_SIM_SUBSCRIPTION_PREF)).thenReturn(mMockEapSimSubscriptionPref);
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
        when(mMockScreen.findPreference(WifiDetailPreferenceController2.KEY_WIFI_TYPE_PREF))
                .thenReturn(mMockTypePref);
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
        setUpSpyController();
        mController.displayPreference(mMockScreen);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void securityPreference_stringShouldBeSet() {
        setUpForConnectedNetwork();
        setUpSpyController();
        displayAndResume();

        verify(mMockSecurityPref).setSummary(SECURITY);
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
        setUpSpyController();
        displayAndResume();

        verify(mMockConnectivityManager, times(1)).registerNetworkCallback(
                nullable(NetworkRequest.class), mCallbackCaptor.capture(), nullable(Handler.class));
    }

    @Test
    public void networkCallback_shouldBeUnregisteredOnPause() {
        setUpForConnectedNetwork();
        setUpSpyController();
        displayAndResume();
        mController.onPause();

        verify(mMockConnectivityManager, times(1))
                .unregisterNetworkCallback(mCallbackCaptor.getValue());
    }

    @Test
    public void entityHeader_shouldHaveIconSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();
        Drawable expectedIcon = mMockIconInjector.getIcon(false /* showX */, LEVEL);

        displayAndResume();

        verify(mMockHeaderController).setIcon(expectedIcon);
    }

    @Test
    public void entityHeader_shouldHaveIconSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        Drawable expectedIcon = mMockIconInjector.getIcon(false /* showX */, LEVEL);

        displayAndResume();

        verify(mMockHeaderController).setIcon(expectedIcon);
    }

    @Test
    public void entityHeader_shouldHaveIconSetForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        verify(mMockHeaderController).setIcon(any(Drawable.class));
    }

    @Test
    public void entityHeader_shouldHaveLabelSetToTitle() {
        setUpForConnectedNetwork();
        setUpSpyController();
        String label = "title";
        when(mMockWifiEntry.getTitle()).thenReturn(label);

        displayAndResume();

        verify(mMockHeaderController).setLabel(label);
    }

    @Test
    public void entityHeader_shouldHaveSummarySet() {
        setUpForConnectedNetwork();
        setUpSpyController();
        String summary = "summary";
        when(mMockWifiEntry.getSummary()).thenReturn(summary);

        displayAndResume();

        verify(mMockHeaderController).setSummary(summary);
    }

    private void doShouldShowRemainingTimeTest(ZonedDateTime now, long timeRemainingMs) {
        when(mMockClock.now()).thenReturn(now);
        setUpForConnectedNetwork();
        setUpController();
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
                StringUtil.formatElapsedTime(
                        mContext,
                        timeRemainingMs,
                        false /* withSeconds */,
                        false /* collapseTimeUnit */));
        final InOrder inOrder = inOrder(mMockHeaderController);
        inOrder.verify(mMockHeaderController).setSecondSummary(expectedSummary);

        updateLinkProperties(new LinkProperties());
        inOrder.verify(mMockHeaderController).setSecondSummary((String) null);
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
        final InOrder inOrder = inOrder(mMockHeaderController);
        inOrder.verify(mMockHeaderController).setSecondSummary(expectedSummary);

        updateLinkProperties(new LinkProperties());
        inOrder.verify(mMockHeaderController).setSecondSummary((String) null);
    }

    @Test
    public void entityHeader_shouldConvertSavedAsDisconnected() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockWifiEntry, times(1)).getSummary();
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();

        displayAndResume();

        assertThat(mController.mShowX).isFalse();
        verify(mMockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        assertThat(mController.mShowX).isFalse();
        verify(mMockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldNotHaveIconSetForOutOfRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        assertThat(mController.mShowX).isFalse();
        verify(mMockSignalStrengthPref, never()).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        displayAndResume();

        assertThat(mController.mShowX).isFalse();
        verify(mMockSignalStrengthPref).setSummary(expectedStrength);
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSetForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        displayAndResume();

        assertThat(mController.mShowX).isFalse();
        verify(mMockSignalStrengthPref).setSummary(expectedStrength);
    }

    @Test
    public void signalStrengthPref_shouldNotHaveDetailTextSetForNotInRangeNetwork() {
        setUpForNotInRangeNetwork();

        displayAndResume();

        assertThat(mController.mShowX).isFalse();
        verify(mMockSignalStrengthPref, never()).setSummary(any(String.class));
    }

    @Test
    public void signalStrengthPref_shouldShowXLevelIcon_showXTrue() {
        setUpForConnectedNetwork();
        setUpSpyController();
        final String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];
        when(mMockWifiEntry.shouldShowXLevelIcon()).thenReturn(true);

        displayAndResume();

        assertThat(mController.mShowX).isTrue();
        verify(mMockSignalStrengthPref).setSummary(expectedStrength);
    }

    @Test
    public void linkSpeedPref_shouldNotShowIfSpeedStringIsEmpty() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.getTxSpeedString()).thenReturn("");

        displayAndResume();

        verify(mMockTxLinkSpeedPref).setVisible(false);
    }

    @Test
    public void linkSpeedPref_shouldBeVisibleIfSpeedStringIsNotEmpty() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.getTxSpeedString()).thenReturn("100 Mbps");

        displayAndResume();

        verify(mMockTxLinkSpeedPref).setVisible(true);
        verify(mMockTxLinkSpeedPref).setSummary("100 Mbps");
    }

    @Test
    public void rxLinkSpeedPref_shouldNotShowIfSpeedStringIsEmpty() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.getRxSpeedString()).thenReturn("");

        displayAndResume();

        verify(mMockRxLinkSpeedPref).setVisible(false);
    }

    @Test
    public void rxLinkSpeedPref_shouldBeVisibleIfSpeedStringIsNotEmpty() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.getRxSpeedString()).thenReturn("100 Mbps");

        displayAndResume();

        verify(mMockRxLinkSpeedPref).setVisible(true);
        verify(mMockRxLinkSpeedPref).setSummary("100 Mbps");
    }

    @Ignore("b/313536962")
    @Test
    public void ssidPref_isSubscription_show() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.isSubscription()).thenReturn(true);
        when(mMockWifiEntry.getSsid()).thenReturn(SSID);

        displayAndResume();

        verify(mMockSsidPref).setSummary(SSID);
        verify(mMockSsidPref).setVisible(true);
    }

    @Test
    public void ssidPref_notSubscription_hide() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.isSubscription()).thenReturn(false);

        displayAndResume();

        verify(mMockSsidPref, never()).setSummary(SSID);
        verify(mMockSsidPref).setVisible(false);
    }

    @Test
    public void macAddressPref_shouldVisibleForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.isSaved()).thenReturn(true);
        when(mMockWifiEntry.getPrivacy()).thenReturn(WifiEntry.PRIVACY_DEVICE_MAC);
        when(mMockWifiEntry.getMacAddress()).thenReturn(MAC_ADDRESS);

        displayAndResume();

        verify(mMockMacAddressPref).setVisible(true);
        verify(mMockMacAddressPref).setSummary(MAC_ADDRESS);
        verify(mMockMacAddressPref).setTitle(R.string.wifi_advanced_device_mac_address_title);
    }

    @Test
    public void macAddressPref_shouldVisibleAsRandomizedForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.isSaved()).thenReturn(true);
        when(mMockWifiEntry.getPrivacy()).thenReturn(WifiEntry.PRIVACY_RANDOMIZED_MAC);
        when(mMockWifiEntry.getMacAddress()).thenReturn(RANDOMIZED_MAC_ADDRESS);

        displayAndResume();

        verify(mMockMacAddressPref).setVisible(true);
        verify(mMockMacAddressPref).setSummary(RANDOMIZED_MAC_ADDRESS);
        verify(mMockMacAddressPref).setTitle(
                R.string.wifi_advanced_randomized_mac_address_title);
    }

    @Test
    public void macAddressPref_shouldVisibleAsRandomizedForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.isSaved()).thenReturn(true);
        when(mMockWifiEntry.getPrivacy()).thenReturn(WifiEntry.PRIVACY_RANDOMIZED_MAC);
        when(mMockWifiEntry.getMacAddress()).thenReturn(RANDOMIZED_MAC_ADDRESS);

        displayAndResume();

        verify(mMockMacAddressPref).setVisible(true);
        verify(mMockMacAddressPref).setSummary(RANDOMIZED_MAC_ADDRESS);
        verify(mMockMacAddressPref).setTitle(
                R.string.wifi_advanced_randomized_mac_address_disconnected_title);
    }

    @Test
    public void macAddressPref_shouldVisibleAsFactoryForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.isSaved()).thenReturn(true);
        when(mMockWifiEntry.getPrivacy()).thenReturn(WifiEntry.PRIVACY_DEVICE_MAC);
        when(mMockWifiEntry.getMacAddress()).thenReturn(FACTORY_MAC_ADDRESS);

        displayAndResume();

        verify(mMockMacAddressPref).setVisible(true);
        verify(mMockMacAddressPref).setSummary(FACTORY_MAC_ADDRESS);
        verify(mMockMacAddressPref).setTitle(R.string.wifi_advanced_device_mac_address_title);
    }

    @Test
    public void ipAddressPref_shouldHaveDetailTextSetForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();
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
        setUpSpyController();
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
        setUpSpyController();
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
    public void onConnectedNetwork_getStandardString_visibleWifiTypePref() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.getStandardString()).thenReturn("Standard");

        displayAndResume();

        verify(mMockTypePref).setSummary("Standard");
        verify(mMockTypePref).setVisible(true);
    }

    @Test
    public void onConnectedNetwork_getEmptyStandardString_invisibleWifiTypePref() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.getStandardString()).thenReturn("");

        displayAndResume();

        verify(mMockTypePref).setVisible(false);
    }

    @Test
    public void onDisconnectedNetwork_resumeUI_invisibleWifiTypePref() {
        setUpForDisconnectedNetwork();

        displayAndResume();

        verify(mMockTypePref).setVisible(false);
    }

    @Test
    public void noCurrentNetwork_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiManager.getCurrentNetwork()).thenReturn(null);

        displayAndResume();

        verify(mMockActivity, never()).finish();
    }

    @Ignore("b/313536962")
    @Test
    public void noLinkProperties_allIpDetailsHidden() {
        setUpForConnectedNetwork();
        setUpSpyController();
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

    @Ignore("b/313536962")
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
        final NetworkCapabilities nc = NetworkCapabilities.Builder.withoutDefaultCapabilities()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        return nc;
    }

    private void verifyDisplayedIpv6Addresses(InOrder inOrder, LinkAddress... addresses) {
        String text = Arrays.stream(addresses)
                .map(address -> asString(address))
                .collect(Collectors.joining("\n"));
        inOrder.verify(mMockIpv6AddressesPref).setSummary(text);
    }

    @Ignore("b/313536962")
    @Test
    public void onLinkPropertiesChanged_updatesFields() {
        setUpForConnectedNetwork();
        setUpController();
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
        setUpController();
        NetworkCapabilities nc = makeNetworkCapabilities();
        when(mMockConnectivityManager.getNetworkCapabilities(mMockNetwork))
                .thenReturn(new NetworkCapabilities(nc));

        String summary = "Connected, no Internet";
        when(mMockWifiEntry.getSummary()).thenReturn(summary);

        InOrder inOrder = inOrder(mMockHeaderController);
        displayAndResume();
        inOrder.verify(mMockHeaderController).setSummary(summary);

        // Check that an irrelevant capability update does not update the access point summary, as
        // doing so could cause unnecessary jank...
        summary = "Connected";
        when(mMockWifiEntry.getSummary()).thenReturn(summary);
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController, never()).setSummary(any(CharSequence.class));

        // ... but that if the network validates, then we do refresh.
        nc = new NetworkCapabilities.Builder(nc)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).build();
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);

        summary = "Connected, no Internet";
        when(mMockWifiEntry.getSummary()).thenReturn(summary);

        // Another irrelevant update won't cause the UI to refresh...
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController, never()).setSummary(any(CharSequence.class));

        // ... but if the network is no longer validated, then we display "connected, no Internet".
        nc = new NetworkCapabilities.Builder(nc)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).build();
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);

        // UI will be refreshed when private DNS is broken.
        summary = "Private DNS server cannot be accessed";
        when(mMockWifiEntry.getSummary()).thenReturn(summary);
        NetworkCapabilities mockNc = mock(NetworkCapabilities.class);
        when(mockNc.isPrivateDnsBroken()).thenReturn(true);
        mCallbackCaptor.getValue().onCapabilitiesChanged(mMockNetwork, mockNc);
        inOrder.verify(mMockHeaderController).setSummary(summary);

        // UI will be refreshed when device connects to a partial connectivity network.
        summary = "Limited connection";
        when(mMockWifiEntry.getSummary()).thenReturn(summary);
        nc = new NetworkCapabilities.Builder(nc)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY).build();
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);

        // Although UI will be refreshed when network become validated. The Settings should
        // continue to display "Limited connection" if network still provides partial connectivity.
        nc = new NetworkCapabilities.Builder(nc)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).build();
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockHeaderController).setSummary(summary);
    }

    @Test
    public void canForgetNetwork_shouldInvisibleIfWithoutConfiguration() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.isSaved()).thenReturn(false);
        mController = newSpyWifiDetailPreferenceController2();

        displayAndResume();

        verify(mMockButtonsPref).setButton1Visible(false);
    }

    @Test
    public void onUpdated_canForget_showForgetButton() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.canForget()).thenReturn(true);

        displayAndResume();
        mController.onUpdated();

        verify(mMockButtonsPref, times(2)).setButton1Visible(true);
    }

    @Test
    public void onUpdated_canNotForget_hideForgetButton() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.canForget()).thenReturn(false);

        displayAndResume();
        mController.onUpdated();

        verify(mMockButtonsPref, times(2)).setButton1Visible(false);
    }

    @Test
    public void canShareNetwork_shouldInvisibleIfWithoutConfiguration() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.isSaved()).thenReturn(false);

        displayAndResume();

        verify(mMockButtonsPref).setButton4Visible(false);
    }

    @Test
    public void canModifyNetwork_savedNetwork_returnTrue() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.isSaved()).thenReturn(true);

        assertThat(mController.canModifyNetwork()).isTrue();
    }

    @Test
    public void canModifyNetwork_lockedDown() {
        setUpForConnectedNetwork();
        setUpSpyController();
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
            when(mMockPackageManager.getPackageUidAsUser(anyString(), anyInt()))
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
    public void forgetNetwork_activityGone_ignoreFinish() {
        setUpForConnectedNetwork();
        setUpSpyController();
        displayAndResume();
        when(mMockFragment.getActivity()).thenReturn(null);

        mForgetClickListener.getValue().onClick(null);

        verify(mMockActivity, never()).finish();
    }

    @Test
    public void forgetNetwork_standardWifiNetwork_forget() {
        setUpForConnectedNetwork();
        setUpSpyController();
        displayAndResume();

        mForgetClickListener.getValue().onClick(null);

        verify(mMockWifiEntry).forget(mController);
        verify(mMockMetricsFeatureProvider)
                .action(mMockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
    }

    @Test
    public void forgetNetwork_isSubscription_shouldShowDialog() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.isSubscription()).thenReturn(true);
        displayAndResume();

        mForgetClickListener.getValue().onClick(null);

        verify(mMockWifiEntry, never()).forget(mController);
        verify(mMockMetricsFeatureProvider, never())
                .action(mMockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_FORGET);
        verify(mController).showConfirmForgetDialog();
    }

    @Test
    public void networkStateChangedIntent_shouldRefetchInfo() {
        setUpForConnectedNetwork();
        setUpSpyController();

        displayAndResume();

        verify(mMockWifiManager, times(1)).getCurrentNetwork();
        verify(mMockConnectivityManager, times(1)).getLinkProperties(any(Network.class));
        verify(mMockConnectivityManager, times(1)).getNetworkCapabilities(any(Network.class));
    }

    @Test
    public void onUpdated_shouldUpdateNetworkInfo() {
        setUpForConnectedNetwork();
        setUpSpyController();

        displayAndResume();

        verify(mController, times(1)).updateNetworkInfo();

        mController.onUpdated();

        verify(mController, times(2)).updateNetworkInfo();
    }

    @Test
    public void networkDisconnectedState_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();

        displayAndResume();

        when(mMockConnectivityManager.getNetworkInfo(any(Network.class))).thenReturn(null);
        mContext.sendBroadcast(new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        verify(mMockActivity, never()).finish();
    }

    @Test
    public void networkOnLost_shouldNotFinishActivityForConnectedNetwork() {
        setUpForConnectedNetwork();
        setUpSpyController();

        displayAndResume();

        mCallbackCaptor.getValue().onLost(mMockNetwork);

        verify(mMockActivity, never()).finish();
    }

    @Test
    public void ipv6AddressPref_shouldHaveHostAddressTextSet() {
        setUpForConnectedNetwork();
        setUpSpyController();
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
        setUpSpyController();
        mLinkProperties.addLinkAddress(Constants.IPV6_GLOBAL2);

        displayAndResume();

        assertThat(mMockIpv6AddressesPref.isSelectable()).isFalse();
    }

    @Test
    public void captivePortal_shouldShowSignInButton() {
        setUpForConnectedNetwork();
        setUpController();

        InOrder inOrder = inOrder(mMockButtonsPref);

        displayAndResume();

        inOrder.verify(mMockButtonsPref).setButton2Visible(false);

        NetworkCapabilities nc = makeNetworkCapabilities();
        updateNetworkCapabilities(nc);
        inOrder.verify(mMockButtonsPref).setButton2Visible(false);

        when(mMockWifiEntry.canSignIn()).thenReturn(true);
        nc = new NetworkCapabilities.Builder(nc)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL).build();
        updateNetworkCapabilities(nc);

        inOrder.verify(mMockButtonsPref).setButton2Text(R.string.wifi_sign_in_button_text);
        inOrder.verify(mMockButtonsPref).setButton2Visible(true);

        when(mMockWifiEntry.canSignIn()).thenReturn(false);
        nc = new NetworkCapabilities.Builder(nc)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL).build();
        updateNetworkCapabilities(nc);

        inOrder.verify(mMockButtonsPref).setButton2Visible(false);
    }

    @Test
    public void captivePortal_shouldShowVenueInfoButton() {
        setUpForConnectedNetwork();
        setUpController();

        InOrder inOrder = inOrder(mMockButtonsPref);

        displayAndResume();

        inOrder.verify(mMockButtonsPref).setButton2Visible(false);

        LinkProperties lp = new LinkProperties();
        final CaptivePortalData data = new CaptivePortalData.Builder()
                .setVenueInfoUrl(Uri.parse("https://example.com/info"))
                .build();
        lp.setCaptivePortalData(data);
        updateLinkProperties(lp);

        inOrder.verify(mMockButtonsPref).setButton2Text(R.string.wifi_venue_website_button_text);
        inOrder.verify(mMockButtonsPref).setButton2Visible(true);

        lp.setCaptivePortalData(null);
        updateLinkProperties(lp);
        inOrder.verify(mMockButtonsPref).setButton2Visible(false);
    }

    @Test
    public void testSignInButton_shouldStartCaptivePortalApp() {
        setUpForConnectedNetwork();
        setUpSpyController();

        displayAndResume();

        ArgumentCaptor<OnClickListener> captor = ArgumentCaptor.forClass(OnClickListener.class);
        verify(mMockButtonsPref, atLeastOnce()).setButton2OnClickListener(captor.capture());
        // getValue() returns the last captured value
        captor.getValue().onClick(null);
        verify(mMockWifiEntry).signIn(any(WifiEntry.SignInCallback.class));
        verify(mMockMetricsFeatureProvider)
                .action(mMockActivity, MetricsProto.MetricsEvent.ACTION_WIFI_SIGNIN);
    }

    @Test
    public void testSignInButton_shouldHideSignInButtonForDisconnectedNetwork() {
        setUpForDisconnectedNetwork();
        NetworkCapabilities nc = makeNetworkCapabilities();
        nc = new NetworkCapabilities.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL).build();
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
    public void testDisconnectButton_connectedNetwork_shouldVisible() {
        setUpForConnectedNetwork();
        setUpSpyController();
        when(mMockWifiEntry.getLevel()).thenReturn(WifiEntry.WIFI_LEVEL_MAX);
        when(mMockWifiEntry.canDisconnect()).thenReturn(true);

        displayAndResume();

        verify(mMockButtonsPref).setButton3Visible(true);
        verify(mMockButtonsPref, times(2)).setButton3Text(R.string.wifi_disconnect_button_text);
    }

    @Test
    public void testConnectButton_disconnectedNetwork_shouldVisibleIfReachable() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.getLevel()).thenReturn(WifiEntry.WIFI_LEVEL_MAX);
        when(mMockWifiEntry.canConnect()).thenReturn(true);

        displayAndResume();

        verify(mMockButtonsPref).setButton3Visible(true);
        verify(mMockButtonsPref, times(2)).setButton3Text(R.string.wifi_connect);
    }

    @Test
    public void testConnectButton_disconnectedNetwork_shouldInvisibleIfUnreachable() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.getLevel()).thenReturn(WifiEntry.WIFI_LEVEL_UNREACHABLE);

        displayAndResume();

        verify(mMockButtonsPref).setButton3Visible(false);
    }

    private void setUpForToast() {
        Resources res = mContext.getResources();
        when(mMockActivity.getResources()).thenReturn(res);
    }

    @Test
    public void testConnectButton_clickConnect_displayAsSuccess() {
        setUpForDisconnectedNetwork();
        final ArgumentCaptor<ConnectCallback> connectCallbackCaptor =
                ArgumentCaptor.forClass(ConnectCallback.class);
        final InOrder inOrder = inOrder(mMockButtonsPref);
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_DISCONNECTED);
        when(mMockWifiEntry.canConnect()).thenReturn(true);
        final String label = "title";
        when(mMockWifiEntry.getTitle()).thenReturn(label);
        setUpForToast();

        displayAndResume();

        // check connect button displayed
        inOrder.verify(mMockButtonsPref).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mMockButtonsPref).setButton3Icon(R.drawable.ic_settings_wireless);

        // click connect button
        mController.connectDisconnectNetwork();
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTING);
        when(mMockWifiEntry.canConnect()).thenReturn(false);
        when(mMockWifiEntry.canDisconnect()).thenReturn(false);
        mController.onUpdated();

        // check display button as connecting
        verify(mMockWifiEntry, times(1)).connect(connectCallbackCaptor.capture());
        verifyConnectingBtnAvailable(inOrder);

        // update as connected
        connectCallbackCaptor.getValue().onConnectResult(
                ConnectCallback.CONNECT_STATUS_SUCCESS);
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTED);
        when(mMockWifiEntry.canDisconnect()).thenReturn(true);
        mController.onUpdated();

        // check disconnect button invisible, be init as default state and toast success message
        verifyDisconnecBtnAvailable(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_connected_to_message, label));
    }

    @Test
    public void testConnectButton_clickConnectButFailed_displayFailMessage() {
        setUpForDisconnectedNetwork();
        final ArgumentCaptor<ConnectCallback> connectCallbackCaptor =
                ArgumentCaptor.forClass(ConnectCallback.class);
        final InOrder inOrder = inOrder(mMockButtonsPref);
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_DISCONNECTED);
        when(mMockWifiEntry.canDisconnect()).thenReturn(true);
        setUpForToast();

        displayAndResume();

        // check connect button displayed
        inOrder.verify(mMockButtonsPref).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mMockButtonsPref).setButton3Icon(R.drawable.ic_settings_wireless);

        // click connect button
        mController.connectDisconnectNetwork();
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTING);
        when(mMockWifiEntry.canConnect()).thenReturn(false);
        when(mMockWifiEntry.canDisconnect()).thenReturn(false);
        mController.onUpdated();

        // check display button as connecting
        verify(mMockWifiEntry, times(1)).connect(connectCallbackCaptor.capture());
        verifyConnectingBtnAvailable(inOrder);

        // update as failed
        connectCallbackCaptor.getValue().onConnectResult(
                ConnectCallback.CONNECT_STATUS_FAILURE_UNKNOWN);
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_DISCONNECTED);
        when(mMockWifiEntry.canConnect()).thenReturn(true);
        mController.onUpdated();

        // check connect button available, be init as default and toast failed message
        verifyConnectBtnAvailable(inOrder);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_connect_message));
    }

    private void verifyConnectBtnAvailable(InOrder inOrder) {
        inOrder.verify(mMockButtonsPref).setButton3Visible(true);
        inOrder.verify(mMockButtonsPref).setButton3Enabled(true);
        inOrder.verify(mMockButtonsPref).setButton3Text(R.string.wifi_connect);
        inOrder.verify(mMockButtonsPref).setButton3Icon(R.drawable.ic_settings_wireless);
    }

    private void verifyDisconnecBtnAvailable(InOrder inOrder) {
        inOrder.verify(mMockButtonsPref).setButton3Visible(true);
        inOrder.verify(mMockButtonsPref).setButton3Enabled(true);
        inOrder.verify(mMockButtonsPref).setButton3Text(R.string.wifi_disconnect_button_text);
        inOrder.verify(mMockButtonsPref).setButton3Icon(R.drawable.ic_settings_close);
    }

    private void verifyConnectingBtnAvailable(InOrder inOrder) {
        inOrder.verify(mMockButtonsPref).setButton3Visible(true);
        inOrder.verify(mMockButtonsPref).setButton3Enabled(false);
        inOrder.verify(mMockButtonsPref).setButton3Text(R.string.wifi_connecting);
    }

    @Test
    public void testRedrawIconForHeader_shouldEnlarge() {
        setUpForConnectedNetwork();
        setUpSpyController();
        ArgumentCaptor<BitmapDrawable> drawableCaptor =
                ArgumentCaptor.forClass(BitmapDrawable.class);
        Drawable original = mContext.getDrawable(Utils.getWifiIconResource(LEVEL)).mutate();
        when(mMockIconInjector.getIcon(anyBoolean(), anyInt())).thenReturn(original);

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
        when(mMockIconInjector.getIcon(anyBoolean(), anyInt())).thenReturn(original);

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
        setUpSpyController();
        ArgumentCaptor<ColorDrawable> drawableCaptor =
                ArgumentCaptor.forClass(ColorDrawable.class);

        displayAndResume();

        verify(mMockHeaderController, times(1)).setIcon(drawableCaptor.capture());
        ColorDrawable icon = drawableCaptor.getValue();
        assertThat(icon).isNotNull();
    }

    @Test
    public void entityHeader_expired_shouldHandleExpiration() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.isExpired()).thenReturn(true);
        final String expired = "Expired";
        when(mMockWifiEntry.getSummary()).thenReturn(expired);

        displayAndResume();

        verify(mMockButtonsPref, atLeastOnce()).setButton3Visible(false);
        verify(mMockHeaderController).setSummary(expired);
    }

    @Test
    public void refreshEapSimSubscription_nonEapSecurity_invisibleEapSimSubscriptionPref() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_NONE);

        displayAndResume();

        verify(mMockEapSimSubscriptionPref, times(1)).setVisible(false);

        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_OWE);

        displayAndResume();

        verify(mMockEapSimSubscriptionPref, times(2)).setVisible(false);

        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_PSK);

        displayAndResume();

        verify(mMockEapSimSubscriptionPref, times(3)).setVisible(false);

        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_SAE);

        displayAndResume();

        verify(mMockEapSimSubscriptionPref, times(4)).setVisible(false);
        verify(mMockEapSimSubscriptionPref, never()).setVisible(true);
    }

    @Test
    public void refreshEapSimSubscription_nonSimEapMethod_invisibleEapSimSubscriptionPref() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        final WifiConfiguration mockWifiConfiguration = mock(WifiConfiguration.class);
        final WifiEnterpriseConfig mockWifiEnterpriseConfig = mock(WifiEnterpriseConfig.class);
        when(mockWifiEnterpriseConfig.isAuthenticationSimBased()).thenReturn(false);
        mockWifiConfiguration.enterpriseConfig = mockWifiEnterpriseConfig;
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mockWifiConfiguration);

        displayAndResume();

        verify(mMockEapSimSubscriptionPref, times(1)).setVisible(false);
    }

    @Test
    public void refreshEapSimSubscription_simEapMethod_visibleEapSimSubscriptionPref() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        final WifiConfiguration mockWifiConfiguration = mock(WifiConfiguration.class);
        final WifiEnterpriseConfig mockWifiEnterpriseConfig = mock(WifiEnterpriseConfig.class);
        when(mockWifiEnterpriseConfig.isAuthenticationSimBased()).thenReturn(true);
        mockWifiConfiguration.enterpriseConfig = mockWifiEnterpriseConfig;
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mockWifiConfiguration);

        displayAndResume();

        verify(mMockEapSimSubscriptionPref).setVisible(true);
    }

    @Test
    public void refreshEapSimSubscription_unknownCarrierId_noSimEapSimSubscriptionPref() {
        setUpForDisconnectedNetwork();
        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);
        final WifiConfiguration mockWifiConfiguration = mock(WifiConfiguration.class);
        mockWifiConfiguration.carrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
        final WifiEnterpriseConfig mockWifiEnterpriseConfig = mock(WifiEnterpriseConfig.class);
        when(mockWifiEnterpriseConfig.isAuthenticationSimBased()).thenReturn(true);
        mockWifiConfiguration.enterpriseConfig = mockWifiEnterpriseConfig;
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mockWifiConfiguration);

        displayAndResume();

        verify(mMockEapSimSubscriptionPref).setVisible(true);
        verify(mMockEapSimSubscriptionPref).setSummary(R.string.wifi_no_related_sim_card);
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

    @Test
    public void fineSubscriptionInfo_noMatchedCarrierId_returnNull() {
        setUpSpyController();
        SubscriptionInfo sub1 = mockSubscriptionInfo(1, "sim1", 1111);
        SubscriptionInfo sub2 = mockSubscriptionInfo(2, "sim2", 2222);
        List<SubscriptionInfo> activeSubInfos = Arrays.asList(sub1, sub2);

        SubscriptionInfo info = mController.fineSubscriptionInfo(3333, activeSubInfos, 1);

        assertThat(info).isNull();

        info = mController.fineSubscriptionInfo(3333, activeSubInfos, 2);

        assertThat(info).isNull();
    }

    @Test
    public void fineSubscriptionInfo_diffCarrierId_returnMatchedOne() {
        setUpSpyController();
        SubscriptionInfo sub1 = mockSubscriptionInfo(1, "sim1", 1111);
        SubscriptionInfo sub2 = mockSubscriptionInfo(2, "sim2", 2222);
        List<SubscriptionInfo> activeSubInfos = Arrays.asList(sub1, sub2);

        SubscriptionInfo info = mController.fineSubscriptionInfo(1111, activeSubInfos, 1);

        assertThat(info).isNotNull();
        assertThat(info.getDisplayName().toString()).isEqualTo("sim1");

        info = mController.fineSubscriptionInfo(1111, activeSubInfos, 2);

        assertThat(info).isNotNull();
        assertThat(info.getDisplayName().toString()).isEqualTo("sim1");

        info = mController.fineSubscriptionInfo(2222, activeSubInfos, 1);

        assertThat(info).isNotNull();
        assertThat(info.getDisplayName().toString()).isEqualTo("sim2");

        info = mController.fineSubscriptionInfo(2222, activeSubInfos, 2);

        assertThat(info).isNotNull();
        assertThat(info.getDisplayName().toString()).isEqualTo("sim2");
    }

    @Test
    public void fineSubscriptionInfo_sameCarrierId_returnDefaultDataOne() {
        setUpSpyController();
        SubscriptionInfo sub1 = mockSubscriptionInfo(1, "sim1", 1111);
        SubscriptionInfo sub2 = mockSubscriptionInfo(2, "sim2", 1111);
        List<SubscriptionInfo> activeSubInfos = Arrays.asList(sub1, sub2);

        SubscriptionInfo info = mController.fineSubscriptionInfo(1111, activeSubInfos, 1);

        assertThat(info).isNotNull();
        assertThat(info.getDisplayName().toString()).isEqualTo("sim1");

        info = mController.fineSubscriptionInfo(1111, activeSubInfos, 2);

        assertThat(info).isNotNull();
        assertThat(info.getDisplayName().toString()).isEqualTo("sim2");
    }

    @Test
    public void refreshEntryHeaderIcon_entityHeaderControllerNull_doNothing() {
        setUpSpyController();
        mController.mEntityHeaderController = null;

        mController.refreshEntryHeaderIcon();

        verify(mController, never()).getWifiDrawable(any());
    }

    @Test
    public void refreshEntryHeaderIcon_entityHeaderControllerNotNull_setIcon() {
        setUpSpyController();
        mController.mEntityHeaderController = mMockHeaderController;

        mController.refreshEntryHeaderIcon();

        verify(mController).getWifiDrawable(any());
        verify(mMockHeaderController).setIcon(any(Drawable.class));
    }

    @Test
    public void getWifiDrawable_withHotspotNetworkEntry_returnHotspotDrawable() {
        setUpSpyController();
        HotspotNetworkEntry entry = mock(HotspotNetworkEntry.class);
        when(entry.getDeviceType()).thenReturn(DEVICE_TYPE_PHONE);

        mController.getWifiDrawable(entry);

        verify(mContext).getDrawable(getHotspotIconResource(DEVICE_TYPE_PHONE));
    }

    @Test
    public void getWifiDrawable_withWifiEntryNotShowXLevelIcon_getIconWithInternet() {
        setUpSpyController();
        when(mMockWifiEntry.getLevel()).thenReturn(WifiEntry.WIFI_LEVEL_MAX);
        when(mMockWifiEntry.shouldShowXLevelIcon()).thenReturn(false);

        mController.getWifiDrawable(mMockWifiEntry);

        verify(mMockIconInjector).getIcon(eq(false) /* noInternet */, anyInt());
    }

    @Test
    public void getWifiDrawable_withWifiEntryShowXLevelIcon_getIconWithNoInternet() {
        setUpSpyController();
        when(mMockWifiEntry.getLevel()).thenReturn(WifiEntry.WIFI_LEVEL_MAX);
        when(mMockWifiEntry.shouldShowXLevelIcon()).thenReturn(true);

        mController.getWifiDrawable(mMockWifiEntry);

        verify(mMockIconInjector).getIcon(eq(true) /* noInternet */, anyInt());
        verify(mMockIconInjector).getIcon(eq(true) /* noInternet */, anyInt());
    }

    @Test
    public void setSignalStrengthTitle_prefNotNull_setPrefTitle() {
        setUpSpyController();
        mController.displayPreference(mMockScreen);

        mController.setSignalStrengthTitle(R.string.hotspot_connection_strength);

        verify(mMockSignalStrengthPref).setTitle(R.string.hotspot_connection_strength);
    }

    private SubscriptionInfo mockSubscriptionInfo(int subId, String displayName, int carrierId) {
        SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(info.getSubscriptionId()).thenReturn(subId);
        when(info.getDisplayName()).thenReturn(displayName);
        when(info.getCarrierId()).thenReturn(carrierId);
        return info;
    }
}
