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

import static android.net.NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY;
import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.CaptivePortalData;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Telephony.CarrierId;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.core.text.BidiFormatter;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.FeatureFlags;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.datausage.WifiDataUsageSummaryPreferenceController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settings.wifi.WifiDialog2;
import com.android.settings.wifi.WifiDialog2.WifiDialog2Listener;
import com.android.settings.wifi.WifiEntryShell;
import com.android.settings.wifi.WifiUtils;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.ActionButtonsPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiEntry.ConnectCallback;
import com.android.wifitrackerlib.WifiEntry.ConnectCallback.ConnectStatus;
import com.android.wifitrackerlib.WifiEntry.ConnectedInfo;
import com.android.wifitrackerlib.WifiEntry.DisconnectCallback;
import com.android.wifitrackerlib.WifiEntry.DisconnectCallback.DisconnectStatus;
import com.android.wifitrackerlib.WifiEntry.ForgetCallback;
import com.android.wifitrackerlib.WifiEntry.ForgetCallback.ForgetStatus;
import com.android.wifitrackerlib.WifiEntry.SignInCallback;
import com.android.wifitrackerlib.WifiEntry.SignInCallback.SignInStatus;
import com.android.wifitrackerlib.WifiEntry.WifiEntryCallback;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

// TODO(b/151133650): Replace AbstractPreferenceController with BasePreferenceController.
/**
 * Controller for logic pertaining to displaying Wifi information for the
 * {@link WifiNetworkDetailsFragment}.
 */
public class WifiDetailPreferenceController2 extends AbstractPreferenceController
        implements PreferenceControllerMixin, WifiDialog2Listener, LifecycleObserver, OnPause,
        OnResume, WifiEntryCallback, ConnectCallback, DisconnectCallback, ForgetCallback,
        SignInCallback {

    private static final String TAG = "WifiDetailsPrefCtrl2";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    static final String KEY_HEADER = "connection_header";
    @VisibleForTesting
    static final String KEY_DATA_USAGE_HEADER = "status_header";
    @VisibleForTesting
    static final String KEY_BUTTONS_PREF = "buttons";
    @VisibleForTesting
    static final String KEY_SIGNAL_STRENGTH_PREF = "signal_strength";
    @VisibleForTesting
    static final String KEY_TX_LINK_SPEED = "tx_link_speed";
    @VisibleForTesting
    static final String KEY_RX_LINK_SPEED = "rx_link_speed";
    @VisibleForTesting
    static final String KEY_FREQUENCY_PREF = "frequency";
    @VisibleForTesting
    static final String KEY_SECURITY_PREF = "security";
    @VisibleForTesting
    static final String KEY_SSID_PREF = "ssid";
    @VisibleForTesting
    static final String KEY_EAP_SIM_SUBSCRIPTION_PREF = "eap_sim_subscription";
    @VisibleForTesting
    static final String KEY_MAC_ADDRESS_PREF = "mac_address";
    @VisibleForTesting
    static final String KEY_IP_ADDRESS_PREF = "ip_address";
    @VisibleForTesting
    static final String KEY_GATEWAY_PREF = "gateway";
    @VisibleForTesting
    static final String KEY_SUBNET_MASK_PREF = "subnet_mask";
    @VisibleForTesting
    static final String KEY_DNS_PREF = "dns";
    @VisibleForTesting
    static final String KEY_IPV6_CATEGORY = "ipv6_category";
    @VisibleForTesting
    static final String KEY_IPV6_ADDRESSES_PREF = "ipv6_addresses";

    private final WifiEntry mWifiEntry;
    private final ConnectivityManager mConnectivityManager;
    private final PreferenceFragmentCompat mFragment;
    private final Handler mHandler;
    private LinkProperties mLinkProperties;
    private Network mNetwork;
    private NetworkInfo mNetworkInfo;
    private NetworkCapabilities mNetworkCapabilities;
    private int mRssiSignalLevel = -1;
    @VisibleForTesting boolean mShowX; // Shows the Wi-Fi signal icon of Pie+x when it's true.
    private String[] mSignalStr;
    private WifiInfo mWifiInfo;
    private final WifiManager mWifiManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    // UI elements - in order of appearance
    private ActionButtonsPreference mButtonsPref;
    private EntityHeaderController mEntityHeaderController;
    private Preference mSignalStrengthPref;
    private Preference mTxLinkSpeedPref;
    private Preference mRxLinkSpeedPref;
    private Preference mFrequencyPref;
    private Preference mSecurityPref;
    private Preference mSsidPref;
    private Preference mEapSimSubscriptionPref;
    private Preference mMacAddressPref;
    private Preference mIpAddressPref;
    private Preference mGatewayPref;
    private Preference mSubnetPref;
    private Preference mDnsPref;
    private PreferenceCategory mIpv6Category;
    private Preference mIpv6AddressPref;
    private Lifecycle mLifecycle;
    Preference mDataUsageSummaryPref;
    WifiDataUsageSummaryPreferenceController mSummaryHeaderController;

    private final IconInjector mIconInjector;
    private final Clock mClock;

    private final NetworkRequest mNetworkRequest = new NetworkRequest.Builder()
            .clearCapabilities().addTransportType(TRANSPORT_WIFI).build();

    private CarrierIdAsyncQueryHandler mCarrierIdAsyncQueryHandler;
    private static final int TOKEN_QUERY_CARRIER_ID_AND_UPDATE_SIM_SUMMARY = 1;
    private static final int COLUMN_CARRIER_NAME = 0;

    private class CarrierIdAsyncQueryHandler extends AsyncQueryHandler {

        private CarrierIdAsyncQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (token == TOKEN_QUERY_CARRIER_ID_AND_UPDATE_SIM_SUMMARY) {
                if (mContext == null || cursor == null || !cursor.moveToFirst()) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    mEapSimSubscriptionPref.setSummary(R.string.wifi_require_sim_card_to_connect);
                    return;
                }
                mEapSimSubscriptionPref.setSummary(mContext.getString(
                        R.string.wifi_require_specific_sim_card_to_connect,
                        cursor.getString(COLUMN_CARRIER_NAME)));
                cursor.close();
                return;
            }
        }
    }

    // Must be run on the UI thread since it directly manipulates UI state.
    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties lp) {
            if (network.equals(mNetwork) && !lp.equals(mLinkProperties)) {
                mLinkProperties = lp;
                refreshEntityHeader();
                refreshButtons();
                refreshIpLayerInfo();
            }
        }

        private boolean hasCapabilityChanged(NetworkCapabilities nc, int cap) {
            // If this is the first time we get NetworkCapabilities, report that something changed.
            if (mNetworkCapabilities == null) return true;

            // nc can never be null, see ConnectivityService#callCallbackForRequest.
            return mNetworkCapabilities.hasCapability(cap) != nc.hasCapability(cap);
        }

        private boolean hasPrivateDnsStatusChanged(NetworkCapabilities nc) {
            // If this is the first time that WifiDetailPreferenceController2 gets
            // NetworkCapabilities, report that something has changed and assign nc to
            // mNetworkCapabilities in onCapabilitiesChanged. Note that the NetworkCapabilities
            // from onCapabilitiesChanged() will never be null, so calling
            // mNetworkCapabilities.isPrivateDnsBroken() would be safe next time.
            if (mNetworkCapabilities == null) {
                return true;
            }

            return mNetworkCapabilities.isPrivateDnsBroken() != nc.isPrivateDnsBroken();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
            // If the network just validated or lost Internet access or detected partial internet
            // connectivity or private dns was broken, refresh network state. Don't do this on
            // every NetworkCapabilities change because refreshEntityHeader sends IPCs to the
            // system server from the UI thread, which can cause jank.
            if (network.equals(mNetwork) && !nc.equals(mNetworkCapabilities)) {
                if (hasPrivateDnsStatusChanged(nc)
                        || hasCapabilityChanged(nc, NET_CAPABILITY_VALIDATED)
                        || hasCapabilityChanged(nc, NET_CAPABILITY_CAPTIVE_PORTAL)
                        || hasCapabilityChanged(nc, NET_CAPABILITY_PARTIAL_CONNECTIVITY)) {
                    refreshEntityHeader();
                }
                mNetworkCapabilities = nc;
                refreshButtons();
                refreshIpLayerInfo();
            }
        }

        @Override
        public void onLost(Network network) {
            // Ephemeral network not a saved network, leave detail page once disconnected
            if (!mWifiEntry.isSaved() && network.equals(mNetwork)) {
                if (DEBUG) {
                    Log.d(TAG, "OnLost and exit WifiNetworkDetailsPage");
                }
                mFragment.getActivity().finish();
            }
        }
    };

    /**
     * To get an instance of {@link WifiDetailPreferenceController2}
     */
    public static WifiDetailPreferenceController2 newInstance(
            WifiEntry wifiEntry,
            ConnectivityManager connectivityManager,
            Context context,
            PreferenceFragmentCompat fragment,
            Handler handler,
            Lifecycle lifecycle,
            WifiManager wifiManager,
            MetricsFeatureProvider metricsFeatureProvider) {
        return new WifiDetailPreferenceController2(
                wifiEntry, connectivityManager, context, fragment, handler, lifecycle,
                wifiManager, metricsFeatureProvider, new IconInjector(context), new Clock());
    }

    @VisibleForTesting
        /* package */ WifiDetailPreferenceController2(
            WifiEntry wifiEntry,
            ConnectivityManager connectivityManager,
            Context context,
            PreferenceFragmentCompat fragment,
            Handler handler,
            Lifecycle lifecycle,
            WifiManager wifiManager,
            MetricsFeatureProvider metricsFeatureProvider,
            IconInjector injector,
            Clock clock) {
        super(context);

        mWifiEntry = wifiEntry;
        mWifiEntry.setListener(this);
        mConnectivityManager = connectivityManager;
        mFragment = fragment;
        mHandler = handler;
        mSignalStr = context.getResources().getStringArray(R.array.wifi_signal);
        mWifiManager = wifiManager;
        mMetricsFeatureProvider = metricsFeatureProvider;
        mIconInjector = injector;
        mClock = clock;

        mLifecycle = lifecycle;
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        // Returns null since this controller contains more than one Preference
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        setupEntityHeader(screen);

        mButtonsPref = ((ActionButtonsPreference) screen.findPreference(KEY_BUTTONS_PREF))
                .setButton1Text(R.string.forget)
                .setButton1Icon(R.drawable.ic_settings_delete)
                .setButton1OnClickListener(view -> forgetNetwork())
                .setButton2Text(R.string.wifi_sign_in_button_text)
                .setButton2Icon(R.drawable.ic_settings_sign_in)
                .setButton2OnClickListener(view -> signIntoNetwork())
                .setButton3Text(getConnectDisconnectButtonTextResource())
                .setButton3Icon(getConnectDisconnectButtonIconResource())
                .setButton3OnClickListener(view -> connectDisconnectNetwork())
                .setButton4Text(R.string.share)
                .setButton4Icon(R.drawable.ic_qrcode_24dp)
                .setButton4OnClickListener(view -> shareNetwork());
        updateCaptivePortalButton();

        mSignalStrengthPref = screen.findPreference(KEY_SIGNAL_STRENGTH_PREF);
        mTxLinkSpeedPref = screen.findPreference(KEY_TX_LINK_SPEED);
        mRxLinkSpeedPref = screen.findPreference(KEY_RX_LINK_SPEED);
        mFrequencyPref = screen.findPreference(KEY_FREQUENCY_PREF);
        mSecurityPref = screen.findPreference(KEY_SECURITY_PREF);

        mSsidPref = screen.findPreference(KEY_SSID_PREF);
        mEapSimSubscriptionPref = screen.findPreference(KEY_EAP_SIM_SUBSCRIPTION_PREF);
        mMacAddressPref = screen.findPreference(KEY_MAC_ADDRESS_PREF);
        mIpAddressPref = screen.findPreference(KEY_IP_ADDRESS_PREF);
        mGatewayPref = screen.findPreference(KEY_GATEWAY_PREF);
        mSubnetPref = screen.findPreference(KEY_SUBNET_MASK_PREF);
        mDnsPref = screen.findPreference(KEY_DNS_PREF);

        mIpv6Category = screen.findPreference(KEY_IPV6_CATEGORY);
        mIpv6AddressPref = screen.findPreference(KEY_IPV6_ADDRESSES_PREF);

        mSecurityPref.setSummary(mWifiEntry.getSecurityString(false /* concise */));
    }

    /**
     * Update text, icon and listener of the captive portal button.
     * @return True if the button should be shown.
     */
    private boolean updateCaptivePortalButton() {
        final Uri venueInfoUrl = getCaptivePortalVenueInfoUrl();
        if (venueInfoUrl == null) {
            mButtonsPref.setButton2Text(R.string.wifi_sign_in_button_text)
                    .setButton2Icon(R.drawable.ic_settings_sign_in)
                    .setButton2OnClickListener(view -> signIntoNetwork());
            return canSignIntoNetwork();
        }

        mButtonsPref.setButton2Text(R.string.wifi_venue_website_button_text)
                .setButton2Icon(R.drawable.ic_settings_sign_in)
                .setButton2OnClickListener(view -> {
                    final Intent infoIntent = new Intent(Intent.ACTION_VIEW);
                    infoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    infoIntent.setData(venueInfoUrl);
                    mContext.startActivity(infoIntent);
                });
        // Only show the venue website when the network is connected.
        return mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED;
    }

    private Uri getCaptivePortalVenueInfoUrl() {
        final LinkProperties lp = mLinkProperties;
        if (lp == null) {
            return null;
        }
        final CaptivePortalData data = lp.getCaptivePortalData();
        if (data == null) {
            return null;
        }
        return data.getVenueInfoUrl();
    }

    private void setupEntityHeader(PreferenceScreen screen) {
        LayoutPreference headerPref = screen.findPreference(KEY_HEADER);

        if (usingDataUsageHeader(mContext)) {
            headerPref.setVisible(false);
            mDataUsageSummaryPref = screen.findPreference(KEY_DATA_USAGE_HEADER);
            mDataUsageSummaryPref.setVisible(true);
            mSummaryHeaderController =
                new WifiDataUsageSummaryPreferenceController(mFragment.getActivity(),
                        mLifecycle, (PreferenceFragmentCompat) mFragment,
                        mWifiEntry.getTitle());
            return;
        }

        mEntityHeaderController =
                EntityHeaderController.newInstance(
                        mFragment.getActivity(), mFragment,
                        headerPref.findViewById(R.id.entity_header));

        ImageView iconView = headerPref.findViewById(R.id.entity_header_icon);

        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        mEntityHeaderController.setLabel(mWifiEntry.getTitle());
    }

    private String getExpiryTimeSummary() {
        if (mLinkProperties == null || mLinkProperties.getCaptivePortalData() == null) {
            return null;
        }

        final long expiryTimeMillis = mLinkProperties.getCaptivePortalData().getExpiryTimeMillis();
        if (expiryTimeMillis <= 0) {
            return null;
        }
        final ZonedDateTime now = mClock.now();
        final ZonedDateTime expiryTime = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(expiryTimeMillis),
                now.getZone());

        if (now.isAfter(expiryTime)) {
            return null;
        }

        if (now.plusDays(2).isAfter(expiryTime)) {
            // Expiration within 2 days: show a duration
            return mContext.getString(R.string.wifi_time_remaining, StringUtil.formatElapsedTime(
                    mContext,
                    Duration.between(now, expiryTime).getSeconds() * 1000,
                    false /* withSeconds */));
        }

        // For more than 2 days, show the expiry date
        return mContext.getString(R.string.wifi_expiry_time,
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(expiryTime));
    }

    private void refreshEntityHeader() {
        if (usingDataUsageHeader(mContext)) {
            mSummaryHeaderController.updateState(mDataUsageSummaryPref);
        } else {
            mEntityHeaderController
                    .setSummary(mWifiEntry.getSummary())
                    .setSecondSummary(getExpiryTimeSummary())
                    .setRecyclerView(mFragment.getListView(), mLifecycle)
                    .done(mFragment.getActivity(), true /* rebind */);
        }
    }

    @VisibleForTesting
    void updateNetworkInfo() {
        if (mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED) {
            mNetwork = mWifiManager.getCurrentNetwork();
            mLinkProperties = mConnectivityManager.getLinkProperties(mNetwork);
            mNetworkCapabilities = mConnectivityManager.getNetworkCapabilities(mNetwork);
            mNetworkInfo = mConnectivityManager.getNetworkInfo(mNetwork);
            mWifiInfo = mWifiManager.getConnectionInfo();
        } else {
            mNetwork = null;
            mLinkProperties = null;
            mNetworkCapabilities = null;
            mNetworkInfo = null;
            mWifiInfo = null;
        }
    }

    @Override
    public void onResume() {
        // Ensure mNetwork is set before any callbacks above are delivered, since our
        // NetworkCallback only looks at changes to mNetwork.
        updateNetworkInfo();
        refreshPage();
        mConnectivityManager.registerNetworkCallback(mNetworkRequest, mNetworkCallback,
                mHandler);
    }

    @Override
    public void onPause() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    private void refreshPage() {
        Log.d(TAG, "Update UI!");

        // refresh header
        refreshEntityHeader();

        // refresh Buttons
        refreshButtons();

        // Update Connection Header icon and Signal Strength Preference
        refreshRssiViews();
        // Frequency Pref
        refreshFrequency();
        // Transmit Link Speed Pref
        refreshTxSpeed();
        // Receive Link Speed Pref
        refreshRxSpeed();
        // IP related information
        refreshIpLayerInfo();
        // SSID Pref
        refreshSsid();
        // EAP SIM subscription
        refreshEapSimSubscription();
        // MAC Address Pref
        refreshMacAddress();
    }

    private void refreshRssiViews() {
        final int signalLevel = mWifiEntry.getLevel();

        // Disappears signal view if not in range. e.g. for saved networks.
        if (signalLevel == WifiEntry.WIFI_LEVEL_UNREACHABLE) {
            mSignalStrengthPref.setVisible(false);
            mRssiSignalLevel = -1;
            return;
        }

        final boolean showX = mWifiEntry.shouldShowXLevelIcon();

        if (mRssiSignalLevel == signalLevel && mShowX == showX) {
            return;
        }
        mRssiSignalLevel = signalLevel;
        mShowX = showX;
        Drawable wifiIcon = mIconInjector.getIcon(mShowX, mRssiSignalLevel);

        if (mEntityHeaderController != null) {
            mEntityHeaderController
                    .setIcon(redrawIconForHeader(wifiIcon)).done(mFragment.getActivity(),
                            true /* rebind */);
        }

        Drawable wifiIconDark = wifiIcon.getConstantState().newDrawable().mutate();
        wifiIconDark.setTintList(Utils.getColorAttr(mContext, android.R.attr.colorControlNormal));
        mSignalStrengthPref.setIcon(wifiIconDark);

        mSignalStrengthPref.setSummary(mSignalStr[mRssiSignalLevel]);
        mSignalStrengthPref.setVisible(true);
    }

    private Drawable redrawIconForHeader(Drawable original) {
        final int iconSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.wifi_detail_page_header_image_size);
        final int actualWidth = original.getMinimumWidth();
        final int actualHeight = original.getMinimumHeight();

        if ((actualWidth == iconSize && actualHeight == iconSize)
                || !VectorDrawable.class.isInstance(original)) {
            return original;
        }

        // clear tint list to make sure can set 87% black after enlarge
        original.setTintList(null);

        // enlarge icon size
        final Bitmap bitmap = Utils.createBitmap(original,
                iconSize /*width*/,
                iconSize /*height*/);
        Drawable newIcon = new BitmapDrawable(null /*resource*/, bitmap);

        // config color for 87% black after enlarge
        newIcon.setTintList(Utils.getColorAttr(mContext, android.R.attr.textColorPrimary));

        return newIcon;
    }

    private void refreshFrequency() {
        final ConnectedInfo connectedInfo = mWifiEntry.getConnectedInfo();
        if (connectedInfo == null) {
            mFrequencyPref.setVisible(false);
            return;
        }

        final int frequency = connectedInfo.frequencyMhz;
        String band = null;
        if (frequency >= WifiEntryShell.LOWER_FREQ_24GHZ
                && frequency < WifiEntryShell.HIGHER_FREQ_24GHZ) {
            band = mContext.getResources().getString(R.string.wifi_band_24ghz);
        } else if (frequency >= WifiEntryShell.LOWER_FREQ_5GHZ
                && frequency < WifiEntryShell.HIGHER_FREQ_5GHZ) {
            band = mContext.getResources().getString(R.string.wifi_band_5ghz);
        } else {
            // Connecting state is unstable, make it disappeared if unexpected
            if (mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTING) {
                mFrequencyPref.setVisible(false);
            } else {
                Log.e(TAG, "Unexpected frequency " + frequency);
            }
            return;
        }
        mFrequencyPref.setSummary(band);
        mFrequencyPref.setVisible(true);
    }

    private void refreshTxSpeed() {
        if (mWifiInfo == null
                || mWifiEntry.getConnectedState() != WifiEntry.CONNECTED_STATE_CONNECTED) {
            mTxLinkSpeedPref.setVisible(false);
            return;
        }

        int txLinkSpeedMbps = mWifiInfo.getTxLinkSpeedMbps();
        mTxLinkSpeedPref.setVisible(txLinkSpeedMbps >= 0);
        mTxLinkSpeedPref.setSummary(mContext.getString(
                R.string.tx_link_speed, mWifiInfo.getTxLinkSpeedMbps()));
    }

    private void refreshRxSpeed() {
        if (mWifiInfo == null
                || mWifiEntry.getConnectedState() != WifiEntry.CONNECTED_STATE_CONNECTED) {
            mRxLinkSpeedPref.setVisible(false);
            return;
        }

        int rxLinkSpeedMbps = mWifiInfo.getRxLinkSpeedMbps();
        mRxLinkSpeedPref.setVisible(rxLinkSpeedMbps >= 0);
        mRxLinkSpeedPref.setSummary(mContext.getString(
                R.string.rx_link_speed, mWifiInfo.getRxLinkSpeedMbps()));
    }

    private void refreshSsid() {
        if (mWifiEntry.isSubscription() && mWifiEntry.getSsid() != null) {
            mSsidPref.setVisible(true);
            mSsidPref.setSummary(mWifiEntry.getSsid());
        } else {
            mSsidPref.setVisible(false);
        }
    }

    private void refreshEapSimSubscription() {
        mEapSimSubscriptionPref.setVisible(false);

        if (mWifiEntry.getSecurity() != WifiEntry.SECURITY_EAP) {
            return;
        }
        final WifiConfiguration config = mWifiEntry.getWifiConfiguration();
        if (config == null || config.enterpriseConfig == null) {
            return;
        }
        if (!config.enterpriseConfig.isAuthenticationSimBased()) {
            return;
        }

        mEapSimSubscriptionPref.setVisible(true);

        // Checks if the SIM subscription is active.
        final List<SubscriptionInfo> activeSubscriptionInfos = mContext
                .getSystemService(SubscriptionManager.class).getActiveSubscriptionInfoList();
        final int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (activeSubscriptionInfos != null) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfos) {
                if (config.carrierId == subscriptionInfo.getCarrierId()) {
                    mEapSimSubscriptionPref.setSummary(subscriptionInfo.getDisplayName());
                    return;
                }

                // When it's UNKNOWN_CARRIER_ID, devices connects it with the SIM subscription of
                // defaultDataSubscriptionId.
                if (config.carrierId == TelephonyManager.UNKNOWN_CARRIER_ID
                        && defaultDataSubscriptionId == subscriptionInfo.getSubscriptionId()) {
                    mEapSimSubscriptionPref.setSummary(subscriptionInfo.getDisplayName());
                    return;
                }
            }
        }

        if (config.carrierId == TelephonyManager.UNKNOWN_CARRIER_ID) {
            mEapSimSubscriptionPref.setSummary(R.string.wifi_no_related_sim_card);
            return;
        }

        // The Wi-Fi network has specified carrier id, query carrier name from CarrierIdProvider.
        if (mCarrierIdAsyncQueryHandler == null) {
            mCarrierIdAsyncQueryHandler = new CarrierIdAsyncQueryHandler(mContext);
        }
        mCarrierIdAsyncQueryHandler.cancelOperation(TOKEN_QUERY_CARRIER_ID_AND_UPDATE_SIM_SUMMARY);
        mCarrierIdAsyncQueryHandler.startQuery(TOKEN_QUERY_CARRIER_ID_AND_UPDATE_SIM_SUMMARY,
                null /* cookie */,
                CarrierId.All.CONTENT_URI,
                new String[]{CarrierId.CARRIER_NAME},
                CarrierId.CARRIER_ID + "=?",
                new String[] {Integer.toString(config.carrierId)},
                null /* orderBy */);
    }

    private void refreshMacAddress() {
        final String macAddress = mWifiEntry.getMacAddress();
        if (TextUtils.isEmpty(macAddress)) {
            mMacAddressPref.setVisible(false);
            return;
        }

        mMacAddressPref.setVisible(true);

        mMacAddressPref.setTitle((mWifiEntry.getPrivacy() == WifiEntry.PRIVACY_RANDOMIZED_MAC)
                ? R.string.wifi_advanced_randomized_mac_address_title
                : R.string.wifi_advanced_device_mac_address_title);

        if (macAddress.equals(WifiInfo.DEFAULT_MAC_ADDRESS)) {
            mMacAddressPref.setSummary(R.string.device_info_not_available);
        } else {
            mMacAddressPref.setSummary(macAddress);
        }
    }

    private void updatePreference(Preference pref, String detailText) {
        if (!TextUtils.isEmpty(detailText)) {
            pref.setSummary(detailText);
            pref.setVisible(true);
        } else {
            pref.setVisible(false);
        }
    }

    private void refreshButtons() {
        final boolean canForgetNetwork = canForgetNetwork();
        final boolean showCaptivePortalButton = updateCaptivePortalButton();
        final boolean canConnectDisconnectNetwork = mWifiEntry.canConnect()
                || mWifiEntry.canDisconnect();
        final boolean canShareNetwork = canShareNetwork();

        mButtonsPref.setButton1Visible(canForgetNetwork);
        mButtonsPref.setButton2Visible(showCaptivePortalButton);
        // Keep the connect/disconnected button visible if we can connect/disconnect, or if we are
        // in the middle of connecting (greyed out).
        mButtonsPref.setButton3Visible(canConnectDisconnectNetwork
                || mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTING);
        mButtonsPref.setButton3Enabled(canConnectDisconnectNetwork);
        mButtonsPref.setButton3Text(getConnectDisconnectButtonTextResource());
        mButtonsPref.setButton3Icon(getConnectDisconnectButtonIconResource());
        mButtonsPref.setButton4Visible(canShareNetwork);
        mButtonsPref.setVisible(canForgetNetwork
                || showCaptivePortalButton
                || canConnectDisconnectNetwork
                || canShareNetwork);
    }

    private int getConnectDisconnectButtonTextResource() {
        switch (mWifiEntry.getConnectedState()) {
            case WifiEntry.CONNECTED_STATE_DISCONNECTED:
                return R.string.wifi_connect;
            case WifiEntry.CONNECTED_STATE_CONNECTED:
                return R.string.wifi_disconnect_button_text;
            case WifiEntry.CONNECTED_STATE_CONNECTING:
                return R.string.wifi_connecting;
            default:
                throw new IllegalStateException("Invalid WifiEntry connected state");
        }
    }

    private int getConnectDisconnectButtonIconResource() {
        switch (mWifiEntry.getConnectedState()) {
            case WifiEntry.CONNECTED_STATE_DISCONNECTED:
            case WifiEntry.CONNECTED_STATE_CONNECTING:
                return R.drawable.ic_settings_wireless;
            case WifiEntry.CONNECTED_STATE_CONNECTED:
                return R.drawable.ic_settings_close;
            default:
                throw new IllegalStateException("Invalid WifiEntry connected state");
        }
    }

    private void refreshIpLayerInfo() {
        // Hide IP layer info if not a connected network.
        if (mWifiEntry.getConnectedState() != WifiEntry.CONNECTED_STATE_CONNECTED
                || mNetwork == null || mLinkProperties == null) {
            mIpAddressPref.setVisible(false);
            mSubnetPref.setVisible(false);
            mGatewayPref.setVisible(false);
            mDnsPref.setVisible(false);
            mIpv6Category.setVisible(false);
            return;
        }

        // Find IPv4 and IPv6 addresses.
        String ipv4Address = null;
        String subnet = null;
        StringJoiner ipv6Addresses = new StringJoiner("\n");

        for (LinkAddress addr : mLinkProperties.getLinkAddresses()) {
            if (addr.getAddress() instanceof Inet4Address) {
                ipv4Address = addr.getAddress().getHostAddress();
                subnet = ipv4PrefixLengthToSubnetMask(addr.getPrefixLength());
            } else if (addr.getAddress() instanceof Inet6Address) {
                ipv6Addresses.add(addr.getAddress().getHostAddress());
            }
        }

        // Find IPv4 default gateway.
        String gateway = null;
        for (RouteInfo routeInfo : mLinkProperties.getRoutes()) {
            if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
                gateway = routeInfo.getGateway().getHostAddress();
                break;
            }
        }

        // Find all (IPv4 and IPv6) DNS addresses.
        String dnsServers = mLinkProperties.getDnsServers().stream()
                .map(InetAddress::getHostAddress)
                .collect(Collectors.joining("\n"));

        // Update UI.
        updatePreference(mIpAddressPref, ipv4Address);
        updatePreference(mSubnetPref, subnet);
        updatePreference(mGatewayPref, gateway);
        updatePreference(mDnsPref, dnsServers);

        if (ipv6Addresses.length() > 0) {
            mIpv6AddressPref.setSummary(
                    BidiFormatter.getInstance().unicodeWrap(ipv6Addresses.toString()));
            mIpv6Category.setVisible(true);
        } else {
            mIpv6Category.setVisible(false);
        }
    }

    private static String ipv4PrefixLengthToSubnetMask(int prefixLength) {
        try {
            InetAddress all = InetAddress.getByAddress(
                    new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255});
            return NetworkUtils.getNetworkPart(all, prefixLength).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Returns whether the network represented by this preference can be modified.
     */
    public boolean canModifyNetwork() {
        return mWifiEntry.isSaved()
                && !WifiUtils.isNetworkLockedDown(mContext, mWifiEntry.getWifiConfiguration());
    }

    /**
     * Returns whether the network represented by this preference can be forgotten.
     */
    public boolean canForgetNetwork() {
        return mWifiEntry.canForget()
                && !WifiUtils.isNetworkLockedDown(mContext, mWifiEntry.getWifiConfiguration());
    }

    /**
     * Returns whether the user can sign into the network represented by this preference.
     */
    private boolean canSignIntoNetwork() {
        return mWifiEntry.canSignIn();
    }

    /**
     * Returns whether the user can share the network represented by this preference with QR code.
     */
    private boolean canShareNetwork() {
        return mWifiEntry.canShare();
    }

    /**
     * Forgets the wifi network associated with this preference.
     */
    private void forgetNetwork() {
        if (mWifiEntry.isSubscription()) {
            // Post a dialog to confirm if user really want to forget the passpoint network.
            showConfirmForgetDialog();
            return;
        } else {
            mWifiEntry.forget(this);
        }

        mMetricsFeatureProvider.action(
                mFragment.getActivity(), SettingsEnums.ACTION_WIFI_FORGET);
        mFragment.getActivity().finish();
    }

    @VisibleForTesting
    protected void showConfirmForgetDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setPositiveButton(R.string.forget, ((dialog1, which) -> {
                    try {
                        mWifiEntry.forget(this);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to remove Passpoint configuration: " + e);
                    }
                    mMetricsFeatureProvider.action(
                            mFragment.getActivity(), SettingsEnums.ACTION_WIFI_FORGET);
                    mFragment.getActivity().finish();
                }))
                .setNegativeButton(R.string.cancel, null /* listener */)
                .setTitle(R.string.wifi_forget_dialog_title)
                .setMessage(R.string.forget_passpoint_dialog_message)
                .create();
        dialog.show();
    }

    /**
     * Show QR code to share the network represented by this preference.
     */
    private void launchWifiDppConfiguratorActivity() {
        final Intent intent = WifiDppUtils.getConfiguratorQrCodeGeneratorIntentOrNull(mContext,
                mWifiManager, mWifiEntry);

        if (intent == null) {
            Log.e(TAG, "Launch Wi-Fi DPP QR code generator with a wrong Wi-Fi network!");
        } else {
            mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_SETTINGS_SHARE_WIFI_QR_CODE,
                    SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR,
                    /* key */ null,
                    /* value */ Integer.MIN_VALUE);

            mContext.startActivity(intent);
        }
    }

    /**
     * Share the wifi network with QR code.
     */
    private void shareNetwork() {
        WifiDppUtils.showLockScreen(mContext, () -> launchWifiDppConfiguratorActivity());
    }

    /**
     * Sign in to the captive portal found on this wifi network associated with this preference.
     */
    private void signIntoNetwork() {
        mMetricsFeatureProvider.action(
                mFragment.getActivity(), SettingsEnums.ACTION_WIFI_SIGNIN);
        mWifiEntry.signIn(this);
    }

    @Override
    public void onSubmit(WifiDialog2 dialog) {
        if (dialog.getController() != null) {
            mWifiManager.save(dialog.getController().getConfig(), new WifiManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int reason) {
                    Activity activity = mFragment.getActivity();
                    if (activity != null) {
                        Toast.makeText(activity,
                                R.string.wifi_failed_save_message,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * Wrapper for testing compatibility.
     */
    @VisibleForTesting
    static class IconInjector {
        private final Context mContext;

        IconInjector(Context context) {
            mContext = context;
        }

        public Drawable getIcon(boolean showX, int level) {
            return mContext.getDrawable(Utils.getWifiIconResource(showX, level)).mutate();
        }
    }

    @VisibleForTesting
    static class Clock {
        public ZonedDateTime now() {
            return ZonedDateTime.now();
        }
    }

    private boolean usingDataUsageHeader(Context context) {
        return FeatureFlagUtils.isEnabled(context, FeatureFlags.WIFI_DETAILS_DATAUSAGE_HEADER);
    }

    @VisibleForTesting
    void connectDisconnectNetwork() {
        if (mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_DISCONNECTED) {
            mWifiEntry.connect(this);
        } else {
            mWifiEntry.disconnect(this);
        }
    }

    /**
     * Indicates the state of the WifiEntry has changed and clients may retrieve updates through
     * the WifiEntry getter methods.
     */
    @Override
    public void onUpdated() {
        updateNetworkInfo();
        refreshPage();

        // Refresh the Preferences in fragment.
        ((WifiNetworkDetailsFragment2) mFragment).refreshPreferences();
    }

    /**
     * Result of the connect request indicated by the CONNECT_STATUS constants.
     */
    @Override
    public void onConnectResult(@ConnectStatus int status) {
        if (status == ConnectCallback.CONNECT_STATUS_SUCCESS) {
            Toast.makeText(mContext,
                    mContext.getString(R.string.wifi_connected_to_message, mWifiEntry.getTitle()),
                    Toast.LENGTH_SHORT).show();
        } else if (mWifiEntry.getLevel() == WifiEntry.WIFI_LEVEL_UNREACHABLE) {
            Toast.makeText(mContext,
                    R.string.wifi_not_in_range_message,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext,
                    R.string.wifi_failed_connect_message,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Result of the disconnect request indicated by the DISCONNECT_STATUS constants.
     */
    @Override
    public void onDisconnectResult(@DisconnectStatus int status) {
        if (status == DisconnectCallback.DISCONNECT_STATUS_SUCCESS) {
            final Activity activity = mFragment.getActivity();
            if (activity != null) {
                Toast.makeText(activity,
                        activity.getString(R.string.wifi_disconnected_from, mWifiEntry.getTitle()),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Disconnect Wi-Fi network failed");
        }
    }

    /**
     * Result of the forget request indicated by the FORGET_STATUS constants.
     */
    @Override
    public void onForgetResult(@ForgetStatus int status) {
        if (status != ForgetCallback.FORGET_STATUS_SUCCESS) {
            Log.e(TAG, "Forget Wi-Fi network failed");
        }

        mMetricsFeatureProvider.action(mFragment.getActivity(), SettingsEnums.ACTION_WIFI_FORGET);
        mFragment.getActivity().finish();
    }

    /**
     * Result of the sign-in request indicated by the SIGNIN_STATUS constants.
     */
    @Override
    public void onSignInResult(@SignInStatus int status) {
        refreshPage();
    }
}
