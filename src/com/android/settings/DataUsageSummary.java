/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.net.ConnectivityManager.TYPE_WIMAX;
import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicy.WARNING_DISABLED;
import static android.net.NetworkPolicyManager.EXTRA_NETWORK_TEMPLATE;
import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.computeLastCycleBoundary;
import static android.net.NetworkPolicyManager.computeNextCycleBoundary;
import static android.net.NetworkTemplate.MATCH_MOBILE_3G_LOWER;
import static android.net.NetworkTemplate.MATCH_MOBILE_4G;
import static android.net.NetworkTemplate.MATCH_MOBILE_ALL;
import static android.net.NetworkTemplate.MATCH_WIFI;
import static android.net.NetworkTemplate.buildTemplateEthernet;
import static android.net.NetworkTemplate.buildTemplateMobile3gLower;
import static android.net.NetworkTemplate.buildTemplateMobile4g;
import static android.net.NetworkTemplate.buildTemplateMobileAll;
import static android.net.NetworkTemplate.buildTemplateWifiWildcard;
import static android.net.TrafficStats.GB_IN_BYTES;
import static android.net.TrafficStats.MB_IN_BYTES;
import static android.net.TrafficStats.UID_REMOVED;
import static android.net.TrafficStats.UID_TETHERING;
import static android.telephony.TelephonyManager.SIM_STATE_READY;
import static android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.android.internal.util.Preconditions.checkNotNull;
import static com.android.settings.Utils.prepareCustomPreferencesList;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.drawable.InsetBoundsDrawable;
import com.android.settings.net.ChartData;
import com.android.settings.net.ChartDataLoader;
import com.android.settings.net.DataUsageMeteredSettings;
import com.android.settings.net.NetworkPolicyEditor;
import com.android.settings.net.SummaryForAllUidLoader;
import com.android.settings.net.UidDetail;
import com.android.settings.net.UidDetailProvider;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.sim.SimSettings;
import com.android.settings.widget.ChartDataUsageView;
import com.android.settings.widget.ChartDataUsageView.DataUsageChartListener;
import com.android.settings.widget.ChartNetworkSeriesView;

import com.google.android.collect.Lists;

import libcore.util.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Panel showing data usage history across various networks, including options
 * to inspect based on usage cycle and control through {@link NetworkPolicy}.
 */
public class DataUsageSummary extends HighlightingFragment implements Indexable {
    private static final String TAG = "DataUsage";
    private static final boolean LOGD = false;

    // TODO: remove this testing code
    private static final boolean TEST_ANIM = false;
    private static final boolean TEST_RADIOS = false;

    private static final String TEST_RADIOS_PROP = "test.radios";
    private static final String TEST_SUBSCRIBER_PROP = "test.subscriberid";

    private static final String TAB_3G = "3g";
    private static final String TAB_4G = "4g";
    private static final String TAB_MOBILE = "mobile";
    private static final String TAB_WIFI = "wifi";
    private static final String TAB_ETHERNET = "ethernet";

    private static final String TAG_CONFIRM_DATA_DISABLE = "confirmDataDisable";
    private static final String TAG_CONFIRM_LIMIT = "confirmLimit";
    private static final String TAG_CYCLE_EDITOR = "cycleEditor";
    private static final String TAG_WARNING_EDITOR = "warningEditor";
    private static final String TAG_LIMIT_EDITOR = "limitEditor";
    private static final String TAG_CONFIRM_RESTRICT = "confirmRestrict";
    private static final String TAG_DENIED_RESTRICT = "deniedRestrict";
    private static final String TAG_CONFIRM_APP_RESTRICT = "confirmAppRestrict";
    private static final String TAG_APP_DETAILS = "appDetails";

    private static final String DATA_USAGE_ENABLE_MOBILE_KEY = "data_usage_enable_mobile";
    private static final String DATA_USAGE_DISABLE_MOBILE_LIMIT_KEY =
            "data_usage_disable_mobile_limit";
    private static final String DATA_USAGE_CYCLE_KEY = "data_usage_cycle";

    private static final int LOADER_CHART_DATA = 2;
    private static final int LOADER_SUMMARY = 3;

    private INetworkManagementService mNetworkService;
    private INetworkStatsService mStatsService;
    private NetworkPolicyManager mPolicyManager;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    private INetworkStatsSession mStatsSession;

    private static final String PREF_FILE = "data_usage";
    private static final String PREF_SHOW_WIFI = "show_wifi";
    private static final String PREF_SHOW_ETHERNET = "show_ethernet";

    private SharedPreferences mPrefs;

    private TabHost mTabHost;
    private ViewGroup mTabsContainer;
    private TabWidget mTabWidget;
    private ListView mListView;
    private ChartNetworkSeriesView mSeries;
    private ChartNetworkSeriesView mDetailedSeries;
    private DataUsageAdapter mAdapter;

    /** Distance to inset content from sides, when needed. */
    private int mInsetSide = 0;

    private ViewGroup mHeader;

    private ViewGroup mNetworkSwitchesContainer;
    private LinearLayout mNetworkSwitches;
    private boolean mDataEnabledSupported;
    private Switch mDataEnabled;
    private View mDataEnabledView;
    private boolean mDisableAtLimitSupported;
    private Switch mDisableAtLimit;
    private View mDisableAtLimitView;

    private View mCycleView;
    private Spinner mCycleSpinner;
    private CycleAdapter mCycleAdapter;
    private TextView mCycleSummary;

    private ChartDataUsageView mChart;
    private View mDisclaimer;
    private TextView mEmpty;
    private View mStupidPadding;

    private View mAppDetail;
    private ImageView mAppIcon;
    private ViewGroup mAppTitles;
    private TextView mAppTotal;
    private TextView mAppForeground;
    private TextView mAppBackground;
    private Button mAppSettings;

    private LinearLayout mAppSwitches;
    private Switch mAppRestrict;
    private View mAppRestrictView;

    private boolean mShowWifi = false;
    private boolean mShowEthernet = false;

    private NetworkTemplate mTemplate;
    private ChartData mChartData;

    private AppItem mCurrentApp = null;

    private Intent mAppSettingsIntent;

    private NetworkPolicyEditor mPolicyEditor;

    private String mCurrentTab = null;
    private String mIntentTab = null;

    private MenuItem mMenuRestrictBackground;
    private MenuItem mMenuShowWifi;
    private MenuItem mMenuShowEthernet;
    private MenuItem mMenuSimCards;
    private MenuItem mMenuCellularNetworks;

    private List<SubscriptionInfo> mSubInfoList;
    private Map<Integer,String> mMobileTagMap;

    /** Flag used to ignore listeners during binding. */
    private boolean mBinding;

    private UidDetailProvider mUidDetailProvider;

    /**
     * Local cache of data enabled for subId, used to work around delays.
     */
    private final Map<String, Boolean> mMobileDataEnabled = new HashMap<String, Boolean>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();

        mNetworkService = INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyManager = NetworkPolicyManager.from(context);
        mTelephonyManager = TelephonyManager.from(context);
        mSubscriptionManager = SubscriptionManager.from(context);

        mPrefs = getActivity().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

        mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
        mPolicyEditor.read();

        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        mMobileTagMap = initMobileTabTag(mSubInfoList);

        try {
            if (!mNetworkService.isBandwidthControlEnabled()) {
                Log.w(TAG, "No bandwidth control; leaving");
                getActivity().finish();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "No bandwidth control; leaving");
            getActivity().finish();
        }

        try {
            mStatsSession = mStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mShowWifi = mPrefs.getBoolean(PREF_SHOW_WIFI, false);
        mShowEthernet = mPrefs.getBoolean(PREF_SHOW_ETHERNET, false);

        // override preferences when no mobile radio
        if (!hasReadyMobileRadio(context)) {
            mShowWifi = true;
            mShowEthernet = true;
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final Context context = inflater.getContext();
        final View view = inflater.inflate(R.layout.data_usage_summary, container, false);

        mUidDetailProvider = new UidDetailProvider(context);

        mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        mTabsContainer = (ViewGroup) view.findViewById(R.id.tabs_container);
        mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
        mListView = (ListView) view.findViewById(android.R.id.list);

        // decide if we need to manually inset our content, or if we should rely
        // on parent container for inset.
        final boolean shouldInset = mListView.getScrollBarStyle()
                == View.SCROLLBARS_OUTSIDE_OVERLAY;
        mInsetSide = 0;

        // adjust padding around tabwidget as needed
        prepareCustomPreferencesList(container, view, mListView, false);

        mTabHost.setup();
        mTabHost.setOnTabChangedListener(mTabListener);

        mHeader = (ViewGroup) inflater.inflate(R.layout.data_usage_header, mListView, false);
        mHeader.setClickable(true);

        mListView.addHeaderView(new View(context), null, true);
        mListView.addHeaderView(mHeader, null, true);
        mListView.setItemsCanFocus(true);

        if (mInsetSide > 0) {
            // inset selector and divider drawables
            insetListViewDrawables(mListView, mInsetSide);
            mHeader.setPaddingRelative(mInsetSide, 0, mInsetSide, 0);
        }

        {
            // bind network switches
            mNetworkSwitchesContainer = (ViewGroup) mHeader.findViewById(
                    R.id.network_switches_container);
            mNetworkSwitches = (LinearLayout) mHeader.findViewById(R.id.network_switches);

            mDataEnabled = new Switch(inflater.getContext());
            mDataEnabled.setClickable(false);
            mDataEnabled.setFocusable(false);
            mDataEnabledView = inflatePreference(inflater, mNetworkSwitches, mDataEnabled);
            mDataEnabledView.setTag(R.id.preference_highlight_key,
                    DATA_USAGE_ENABLE_MOBILE_KEY);
            mDataEnabledView.setClickable(true);
            mDataEnabledView.setFocusable(true);
            mDataEnabledView.setOnClickListener(mDataEnabledListener);
            mNetworkSwitches.addView(mDataEnabledView);

            mDisableAtLimit = new Switch(inflater.getContext());
            mDisableAtLimit.setClickable(false);
            mDisableAtLimit.setFocusable(false);
            mDisableAtLimitView = inflatePreference(inflater, mNetworkSwitches, mDisableAtLimit);
            mDisableAtLimitView.setTag(R.id.preference_highlight_key,
                    DATA_USAGE_DISABLE_MOBILE_LIMIT_KEY);
            mDisableAtLimitView.setClickable(true);
            mDisableAtLimitView.setFocusable(true);
            mDisableAtLimitView.setOnClickListener(mDisableAtLimitListener);
            mNetworkSwitches.addView(mDisableAtLimitView);

            mCycleView = inflater.inflate(R.layout.data_usage_cycles, mNetworkSwitches, false);
            mCycleView.setTag(R.id.preference_highlight_key, DATA_USAGE_CYCLE_KEY);
            mCycleSpinner = (Spinner) mCycleView.findViewById(R.id.cycles_spinner);
            mCycleAdapter = new CycleAdapter(context);
            mCycleSpinner.setAdapter(mCycleAdapter);
            mCycleSpinner.setOnItemSelectedListener(mCycleListener);
            mCycleSummary = (TextView) mCycleView.findViewById(R.id.cycle_summary);
            mNetworkSwitches.addView(mCycleView);
            mSeries = (ChartNetworkSeriesView)view.findViewById(R.id.series);
            mDetailedSeries = (ChartNetworkSeriesView)view.findViewById(R.id.detail_series);
        }

        mChart = (ChartDataUsageView) mHeader.findViewById(R.id.chart);
        mChart.setListener(mChartListener);
        mChart.bindNetworkPolicy(null);

        {
            // bind app detail controls
            mAppDetail = mHeader.findViewById(R.id.app_detail);
            mAppIcon = (ImageView) mAppDetail.findViewById(R.id.app_icon);
            mAppTitles = (ViewGroup) mAppDetail.findViewById(R.id.app_titles);
            mAppForeground = (TextView) mAppDetail.findViewById(R.id.app_foreground);
            mAppBackground = (TextView) mAppDetail.findViewById(R.id.app_background);
            mAppSwitches = (LinearLayout) mAppDetail.findViewById(R.id.app_switches);

            mAppSettings = (Button) mAppDetail.findViewById(R.id.app_settings);

            mAppRestrict = new Switch(inflater.getContext());
            mAppRestrict.setClickable(false);
            mAppRestrict.setFocusable(false);
            mAppRestrictView = inflatePreference(inflater, mAppSwitches, mAppRestrict);
            mAppRestrictView.setClickable(true);
            mAppRestrictView.setFocusable(true);
            mAppRestrictView.setOnClickListener(mAppRestrictListener);
            mAppSwitches.addView(mAppRestrictView);
        }

        mDisclaimer = mHeader.findViewById(R.id.disclaimer);
        mEmpty = (TextView) mHeader.findViewById(android.R.id.empty);
        mStupidPadding = mHeader.findViewById(R.id.stupid_padding);

        final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mAdapter = new DataUsageAdapter(um, mUidDetailProvider, mInsetSide);
        mListView.setOnItemClickListener(mListListener);
        mListView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        // pick default tab based on incoming intent
        final Intent intent = getActivity().getIntent();
        mIntentTab = computeTabFromIntent(intent);

        // this kicks off chain reaction which creates tabs, binds the body to
        // selected network, and binds chart, cycles and detail list.
        updateTabs();
    }

    @Override
    public void onResume() {
        super.onResume();

        getView().post(new Runnable() {
            @Override
            public void run() {
                highlightViewIfNeeded();
            }
        });

        // kick off background task to update stats
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // wait a few seconds before kicking off
                    Thread.sleep(2 * DateUtils.SECOND_IN_MILLIS);
                    mStatsService.forceUpdate();
                } catch (InterruptedException e) {
                } catch (RemoteException e) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (isAdded()) {
                    updateBody();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.data_usage, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final Context context = getActivity();
        final boolean appDetailMode = isAppDetailMode();
        final boolean isOwner = ActivityManager.getCurrentUser() == UserHandle.USER_OWNER;

        mMenuShowWifi = menu.findItem(R.id.data_usage_menu_show_wifi);
        if (hasWifiRadio(context) && hasReadyMobileRadio(context)) {
            mMenuShowWifi.setVisible(!appDetailMode);
        } else {
            mMenuShowWifi.setVisible(false);
        }

        mMenuShowEthernet = menu.findItem(R.id.data_usage_menu_show_ethernet);
        if (hasEthernet(context) && hasReadyMobileRadio(context)) {
            mMenuShowEthernet.setVisible(!appDetailMode);
        } else {
            mMenuShowEthernet.setVisible(false);
        }

        mMenuRestrictBackground = menu.findItem(R.id.data_usage_menu_restrict_background);
        mMenuRestrictBackground.setVisible(
                hasReadyMobileRadio(context) && isOwner && !appDetailMode);

        final MenuItem metered = menu.findItem(R.id.data_usage_menu_metered);
        if (hasReadyMobileRadio(context) || hasWifiRadio(context)) {
            metered.setVisible(!appDetailMode);
        } else {
            metered.setVisible(false);
        }

        // TODO: show when multiple sims available
        mMenuSimCards = menu.findItem(R.id.data_usage_menu_sim_cards);
        mMenuSimCards.setVisible(false);

        mMenuCellularNetworks = menu.findItem(R.id.data_usage_menu_cellular_networks);
        mMenuCellularNetworks.setVisible(hasReadyMobileRadio(context)
                && !appDetailMode && isOwner);

        final MenuItem help = menu.findItem(R.id.data_usage_menu_help);
        String helpUrl;
        if (!TextUtils.isEmpty(helpUrl = getResources().getString(R.string.help_url_data_usage))) {
            HelpUtils.prepareHelpMenuItem(context, help, helpUrl);
        } else {
            help.setVisible(false);
        }

        updateMenuTitles();
    }

    private void updateMenuTitles() {
        if (mPolicyManager.getRestrictBackground()) {
            mMenuRestrictBackground.setTitle(R.string.data_usage_menu_allow_background);
        } else {
            mMenuRestrictBackground.setTitle(R.string.data_usage_menu_restrict_background);
        }

        if (mShowWifi) {
            mMenuShowWifi.setTitle(R.string.data_usage_menu_hide_wifi);
        } else {
            mMenuShowWifi.setTitle(R.string.data_usage_menu_show_wifi);
        }

        if (mShowEthernet) {
            mMenuShowEthernet.setTitle(R.string.data_usage_menu_hide_ethernet);
        } else {
            mMenuShowEthernet.setTitle(R.string.data_usage_menu_show_ethernet);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.data_usage_menu_restrict_background: {
                final boolean restrictBackground = !mPolicyManager.getRestrictBackground();
                if (restrictBackground) {
                    ConfirmRestrictFragment.show(this);
                } else {
                    // no confirmation to drop restriction
                    setRestrictBackground(false);
                }
                return true;
            }
            case R.id.data_usage_menu_show_wifi: {
                mShowWifi = !mShowWifi;
                mPrefs.edit().putBoolean(PREF_SHOW_WIFI, mShowWifi).apply();
                updateMenuTitles();
                updateTabs();
                return true;
            }
            case R.id.data_usage_menu_show_ethernet: {
                mShowEthernet = !mShowEthernet;
                mPrefs.edit().putBoolean(PREF_SHOW_ETHERNET, mShowEthernet).apply();
                updateMenuTitles();
                updateTabs();
                return true;
            }
            case R.id.data_usage_menu_sim_cards: {
                // TODO: hook up to sim cards
                return true;
            }
            case R.id.data_usage_menu_cellular_networks: {
                final Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(new ComponentName("com.android.phone",
                        "com.android.phone.MobileNetworkSettings"));
                startActivity(intent);
                return true;
            }
            case R.id.data_usage_menu_metered: {
                final SettingsActivity sa = (SettingsActivity) getActivity();
                sa.startPreferencePanel(DataUsageMeteredSettings.class.getCanonicalName(), null,
                        R.string.data_usage_metered_title, null, this, 0);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        mDataEnabledView = null;
        mDisableAtLimitView = null;

        mUidDetailProvider.clearCache();
        mUidDetailProvider = null;

        TrafficStats.closeQuietly(mStatsSession);

        super.onDestroy();
    }

    /**
     * Build and assign {@link LayoutTransition} to various containers. Should
     * only be assigned after initial layout is complete.
     */
    private void ensureLayoutTransitions() {
        // skip when already setup
        if (mChart.getLayoutTransition() != null) return;

        mTabsContainer.setLayoutTransition(buildLayoutTransition());
        mHeader.setLayoutTransition(buildLayoutTransition());
        mNetworkSwitchesContainer.setLayoutTransition(buildLayoutTransition());

        final LayoutTransition chartTransition = buildLayoutTransition();
        chartTransition.disableTransitionType(LayoutTransition.APPEARING);
        chartTransition.disableTransitionType(LayoutTransition.DISAPPEARING);
        mChart.setLayoutTransition(chartTransition);
    }

    private static LayoutTransition buildLayoutTransition() {
        final LayoutTransition transition = new LayoutTransition();
        if (TEST_ANIM) {
            transition.setDuration(1500);
        }
        transition.setAnimateParentHierarchy(false);
        return transition;
    }

    /**
     * Rebuild all tabs based on {@link NetworkPolicyEditor} and
     * {@link #mShowWifi}, hiding the tabs entirely when applicable. Selects
     * first tab, and kicks off a full rebind of body contents.
     */
    private void updateTabs() {
        final Context context = getActivity();
        mTabHost.clearAllTabs();

        int simCount = mTelephonyManager.getSimCount();

        for (int i = 0; i < simCount; i++) {
            final SubscriptionInfo sir = Utils.findRecordBySlotId(context, i);
            if (sir != null) {
                addMobileTab(context, sir, (simCount > 1));
            }
        }

        if (mShowWifi && hasWifiRadio(context)) {
            mTabHost.addTab(buildTabSpec(TAB_WIFI, R.string.data_usage_tab_wifi));
        }

        if (mShowEthernet && hasEthernet(context)) {
            mTabHost.addTab(buildTabSpec(TAB_ETHERNET, R.string.data_usage_tab_ethernet));
        }

        final boolean noTabs = mTabWidget.getTabCount() == 0;
        final boolean multipleTabs = mTabWidget.getTabCount() > 1;
        mTabWidget.setVisibility(multipleTabs ? View.VISIBLE : View.GONE);
        if (mIntentTab != null) {
            if (Objects.equal(mIntentTab, mTabHost.getCurrentTabTag())) {
                // already hit updateBody() when added; ignore
                updateBody();
            } else {
                mTabHost.setCurrentTabByTag(mIntentTab);
            }
            mIntentTab = null;
        } else if (noTabs) {
            // no usable tabs, so hide body
            updateBody();
        } else {
            // already hit updateBody() when added; ignore
        }
    }

    /**
     * Factory that provide empty {@link View} to make {@link TabHost} happy.
     */
    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    /**
     * Build {@link TabSpec} with thin indicator, and empty content.
     */
    private TabSpec buildTabSpec(String tag, int titleRes) {
        return mTabHost.newTabSpec(tag).setIndicator(getText(titleRes)).setContent(
                mEmptyTabContent);
    }

    /**
     * Build {@link TabSpec} with thin indicator, and empty content.
     */
    private TabSpec buildTabSpec(String tag, CharSequence title) {
        return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                mEmptyTabContent);
    }


    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            // user changed tab; update body
            updateBody();
        }
    };

    /**
     * Update body content based on current tab. Loads
     * {@link NetworkStatsHistory} and {@link NetworkPolicy} from system, and
     * binds them to visible controls.
     */
    private void updateBody() {
        mBinding = true;
        if (!isAdded()) return;

        final Context context = getActivity();
        final Resources resources = context.getResources();
        final String currentTab = mTabHost.getCurrentTabTag();
        final boolean isOwner = ActivityManager.getCurrentUser() == UserHandle.USER_OWNER;

        if (currentTab == null) {
            Log.w(TAG, "no tab selected; hiding body");
            mListView.setVisibility(View.GONE);
            return;
        } else {
            mListView.setVisibility(View.VISIBLE);
        }

        mCurrentTab = currentTab;

        if (LOGD) Log.d(TAG, "updateBody() with currentTab=" + currentTab);

        mDataEnabledSupported = isOwner;
        mDisableAtLimitSupported = true;

        // TODO: remove mobile tabs when SIM isn't ready probably by
        // TODO: using SubscriptionManager.getActiveSubscriptionInfoList.
        if (LOGD) Log.d(TAG, "updateBody() isMobileTab=" + isMobileTab(currentTab));

        if (isMobileTab(currentTab)) {
            if (LOGD) Log.d(TAG, "updateBody() mobile tab");
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_mobile);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_mobile_limit);
            mDataEnabledSupported = isMobileDataAvailable(getSubId(currentTab));

            // Match mobile traffic for this subscriber, but normalize it to
            // catch any other merged subscribers.
            mTemplate = buildTemplateMobileAll(
                    getActiveSubscriberId(context, getSubId(currentTab)));
            mTemplate = NetworkTemplate.normalize(mTemplate,
                    mTelephonyManager.getMergedSubscriberIds());

        } else if (TAB_3G.equals(currentTab)) {
            if (LOGD) Log.d(TAG, "updateBody() 3g tab");
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_3g);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_3g_limit);
            // TODO: bind mDataEnabled to 3G radio state
            mTemplate = buildTemplateMobile3gLower(getActiveSubscriberId(context));

        } else if (TAB_4G.equals(currentTab)) {
            if (LOGD) Log.d(TAG, "updateBody() 4g tab");
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_4g);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_4g_limit);
            // TODO: bind mDataEnabled to 4G radio state
            mTemplate = buildTemplateMobile4g(getActiveSubscriberId(context));

        } else if (TAB_WIFI.equals(currentTab)) {
            // wifi doesn't have any controls
            if (LOGD) Log.d(TAG, "updateBody() wifi tab");
            mDataEnabledSupported = false;
            mDisableAtLimitSupported = false;
            mTemplate = buildTemplateWifiWildcard();

        } else if (TAB_ETHERNET.equals(currentTab)) {
            // ethernet doesn't have any controls
            if (LOGD) Log.d(TAG, "updateBody() ethernet tab");
            mDataEnabledSupported = false;
            mDisableAtLimitSupported = false;
            mTemplate = buildTemplateEthernet();

        } else {
            if (LOGD) Log.d(TAG, "updateBody() unknown tab");
            throw new IllegalStateException("unknown tab: " + currentTab);
        }

        // kick off loader for network history
        // TODO: consider chaining two loaders together instead of reloading
        // network history when showing app detail.
        getLoaderManager().restartLoader(LOADER_CHART_DATA,
                ChartDataLoader.buildArgs(mTemplate, mCurrentApp), mChartDataCallbacks);

        // detail mode can change visible menus, invalidate
        getActivity().invalidateOptionsMenu();

        mBinding = false;

        int seriesColor = resources.getColor(R.color.sim_noitification);
        if (mCurrentTab != null && mCurrentTab.length() > TAB_MOBILE.length() ){
            final int slotId = Integer.parseInt(mCurrentTab.substring(TAB_MOBILE.length(),
                    mCurrentTab.length()));
            final SubscriptionInfo sir = com.android.settings.Utils.findRecordBySlotId(context,
                    slotId);

            if (sir != null) {
                seriesColor = sir.getIconTint();
            }
        }

        final int secondaryColor = Color.argb(127, Color.red(seriesColor), Color.green(seriesColor),
                Color.blue(seriesColor));
        mSeries.setChartColor(Color.BLACK, seriesColor, secondaryColor);
        mDetailedSeries.setChartColor(Color.BLACK, seriesColor, secondaryColor);
    }

    private boolean isAppDetailMode() {
        return mCurrentApp != null;
    }

    /**
     * Update UID details panels to match {@link #mCurrentApp}, showing or
     * hiding them depending on {@link #isAppDetailMode()}.
     */
    private void updateAppDetail() {
        final Context context = getActivity();
        final PackageManager pm = context.getPackageManager();
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        if (isAppDetailMode()) {
            mAppDetail.setVisibility(View.VISIBLE);
            mCycleAdapter.setChangeVisible(false);
        } else {
            mAppDetail.setVisibility(View.GONE);
            mCycleAdapter.setChangeVisible(true);

            // hide detail stats when not in detail mode
            mChart.bindDetailNetworkStats(null);
            return;
        }

        // remove warning/limit sweeps while in detail mode
        mChart.bindNetworkPolicy(null);

        // show icon and all labels appearing under this app
        final int uid = mCurrentApp.key;
        final UidDetail detail = mUidDetailProvider.getUidDetail(uid, true);
        mAppIcon.setImageDrawable(detail.icon);

        mAppTitles.removeAllViews();

        View title = null;
        if (detail.detailLabels != null) {
            final int n = detail.detailLabels.length;
            for (int i = 0; i < n; ++i) {
                CharSequence label = detail.detailLabels[i];
                CharSequence contentDescription = detail.detailContentDescriptions[i];
                title = inflater.inflate(R.layout.data_usage_app_title, mAppTitles, false);
                TextView appTitle = (TextView) title.findViewById(R.id.app_title);
                appTitle.setText(label);
                appTitle.setContentDescription(contentDescription);
                mAppTitles.addView(title);
            }
        } else {
            title = inflater.inflate(R.layout.data_usage_app_title, mAppTitles, false);
            TextView appTitle = (TextView) title.findViewById(R.id.app_title);
            appTitle.setText(detail.label);
            appTitle.setContentDescription(detail.contentDescription);
            mAppTitles.addView(title);
        }

        // Remember last slot for summary
        if (title != null) {
            mAppTotal = (TextView) title.findViewById(R.id.app_summary);
        } else {
            mAppTotal = null;
        }

        // enable settings button when package provides it
        final String[] packageNames = pm.getPackagesForUid(uid);
        if (packageNames != null && packageNames.length > 0) {
            mAppSettingsIntent = new Intent(Intent.ACTION_MANAGE_NETWORK_USAGE);
            mAppSettingsIntent.addCategory(Intent.CATEGORY_DEFAULT);

            // Search for match across all packages
            boolean matchFound = false;
            for (String packageName : packageNames) {
                mAppSettingsIntent.setPackage(packageName);
                if (pm.resolveActivity(mAppSettingsIntent, 0) != null) {
                    matchFound = true;
                    break;
                }
            }

            mAppSettings.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isAdded()) {
                        return;
                    }

                    // TODO: target towards entire UID instead of just first package
                    getActivity().startActivityAsUser(mAppSettingsIntent,
                            new UserHandle(UserHandle.getUserId(uid)));
                }
            });
            mAppSettings.setEnabled(matchFound);
            mAppSettings.setVisibility(View.VISIBLE);

        } else {
            mAppSettingsIntent = null;
            mAppSettings.setOnClickListener(null);
            mAppSettings.setVisibility(View.GONE);
        }

        updateDetailData();

        if (UserHandle.isApp(uid) && !mPolicyManager.getRestrictBackground()
                && isBandwidthControlEnabled() && hasReadyMobileRadio(context)) {
            setPreferenceTitle(mAppRestrictView, R.string.data_usage_app_restrict_background);
            setPreferenceSummary(mAppRestrictView,
                    getString(R.string.data_usage_app_restrict_background_summary));

            mAppRestrictView.setVisibility(View.VISIBLE);
            mAppRestrict.setChecked(getAppRestrictBackground());

        } else {
            mAppRestrictView.setVisibility(View.GONE);
        }
    }

    private void setPolicyWarningBytes(long warningBytes) {
        if (LOGD) Log.d(TAG, "setPolicyWarningBytes()");
        mPolicyEditor.setPolicyWarningBytes(mTemplate, warningBytes);
        updatePolicy(false);
    }

    private void setPolicyLimitBytes(long limitBytes) {
        if (LOGD) Log.d(TAG, "setPolicyLimitBytes()");
        mPolicyEditor.setPolicyLimitBytes(mTemplate, limitBytes);
        updatePolicy(false);
    }

    private boolean isMobileDataEnabled(int subId) {
        if (LOGD) Log.d(TAG, "isMobileDataEnabled:+ subId=" + subId);
        boolean isEnable = false;
        if (mMobileDataEnabled.get(String.valueOf(subId)) != null) {
            //TODO: deprecate and remove this once enabled flag is on policy
            //Multiple Subscriptions, the value need to be reseted
            isEnable = mMobileDataEnabled.get(String.valueOf(subId)).booleanValue();
            if (LOGD) {
                Log.d(TAG, "isMobileDataEnabled: != null, subId=" + subId
                        + " isEnable=" + isEnable);
            }
            mMobileDataEnabled.put(String.valueOf(subId), null);
        } else {
            // SUB SELECT
            isEnable = mTelephonyManager.getDataEnabled(subId);
            if (LOGD) {
                Log.d(TAG, "isMobileDataEnabled: == null, subId=" + subId
                        + " isEnable=" + isEnable);
            }
        }
        return isEnable;
    }

    private void setMobileDataEnabled(int subId, boolean enabled) {
        if (LOGD) Log.d(TAG, "setMobileDataEnabled()");
        mTelephonyManager.setDataEnabled(subId, enabled);
        mMobileDataEnabled.put(String.valueOf(subId), enabled);
        updatePolicy(false);
    }

    private boolean isNetworkPolicyModifiable(NetworkPolicy policy) {
        return policy != null && isBandwidthControlEnabled() && mDataEnabled.isChecked()
                && ActivityManager.getCurrentUser() == UserHandle.USER_OWNER;
    }

    private boolean isBandwidthControlEnabled() {
        try {
            return mNetworkService.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            return false;
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        mPolicyManager.setRestrictBackground(restrictBackground);
        updateMenuTitles();
    }

    private boolean getAppRestrictBackground() {
        final int uid = mCurrentApp.key;
        final int uidPolicy = mPolicyManager.getUidPolicy(uid);
        return (uidPolicy & POLICY_REJECT_METERED_BACKGROUND) != 0;
    }

    private void setAppRestrictBackground(boolean restrictBackground) {
        if (LOGD) Log.d(TAG, "setAppRestrictBackground()");
        final int uid = mCurrentApp.key;
        mPolicyManager.setUidPolicy(
                uid, restrictBackground ? POLICY_REJECT_METERED_BACKGROUND : POLICY_NONE);
        mAppRestrict.setChecked(restrictBackground);
    }

    /**
     * Update chart sweeps and cycle list to reflect {@link NetworkPolicy} for
     * current {@link #mTemplate}.
     */
    private void updatePolicy(boolean refreshCycle) {
        boolean dataEnabledVisible = mDataEnabledSupported;
        boolean disableAtLimitVisible = mDisableAtLimitSupported;

        if (isAppDetailMode()) {
            dataEnabledVisible = false;
            disableAtLimitVisible = false;
        }

        // TODO: move enabled state directly into policy
        if (isMobileTab(mCurrentTab)) {
            mBinding = true;
            mDataEnabled.setChecked(isMobileDataEnabled(getSubId(mCurrentTab)));
            mBinding = false;
        }

        final NetworkPolicy policy = mPolicyEditor.getPolicy(mTemplate);
        //SUB SELECT
        if (isNetworkPolicyModifiable(policy) && isMobileDataAvailable(getSubId(mCurrentTab))) {
            mDisableAtLimit.setChecked(policy != null && policy.limitBytes != LIMIT_DISABLED);
            if (!isAppDetailMode()) {
                mChart.bindNetworkPolicy(policy);
            }

        } else {
            // controls are disabled; don't bind warning/limit sweeps
            disableAtLimitVisible = false;
            mChart.bindNetworkPolicy(null);
        }

        mDataEnabledView.setVisibility(dataEnabledVisible ? View.VISIBLE : View.GONE);
        mDisableAtLimitView.setVisibility(disableAtLimitVisible ? View.VISIBLE : View.GONE);

        if (refreshCycle) {
            // generate cycle list based on policy and available history
            updateCycleList(policy);
        }
    }

    /**
     * Rebuild {@link #mCycleAdapter} based on {@link NetworkPolicy#cycleDay}
     * and available {@link NetworkStatsHistory} data. Always selects the newest
     * item, updating the inspection range on {@link #mChart}.
     */
    private void updateCycleList(NetworkPolicy policy) {
        // stash away currently selected cycle to try restoring below
        final CycleItem previousItem = (CycleItem) mCycleSpinner.getSelectedItem();
        mCycleAdapter.clear();

        final Context context = mCycleSpinner.getContext();

        long historyStart = Long.MAX_VALUE;
        long historyEnd = Long.MIN_VALUE;
        if (mChartData != null) {
            historyStart = mChartData.network.getStart();
            historyEnd = mChartData.network.getEnd();
        }

        final long now = System.currentTimeMillis();
        if (historyStart == Long.MAX_VALUE) historyStart = now;
        if (historyEnd == Long.MIN_VALUE) historyEnd = now + 1;

        boolean hasCycles = false;
        if (policy != null) {
            // find the next cycle boundary
            long cycleEnd = computeNextCycleBoundary(historyEnd, policy);

            // walk backwards, generating all valid cycle ranges
            while (cycleEnd > historyStart) {
                final long cycleStart = computeLastCycleBoundary(cycleEnd, policy);
                Log.d(TAG, "generating cs=" + cycleStart + " to ce=" + cycleEnd + " waiting for hs="
                        + historyStart);
                mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                cycleEnd = cycleStart;
                hasCycles = true;
            }

            // one last cycle entry to modify policy cycle day
            mCycleAdapter.setChangePossible(isNetworkPolicyModifiable(policy));
        }

        if (!hasCycles) {
            // no policy defined cycles; show entry for each four-week period
            long cycleEnd = historyEnd;
            while (cycleEnd > historyStart) {
                final long cycleStart = cycleEnd - (DateUtils.WEEK_IN_MILLIS * 4);
                mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                cycleEnd = cycleStart;
            }

            mCycleAdapter.setChangePossible(false);
        }

        // force pick the current cycle (first item)
        if (mCycleAdapter.getCount() > 0) {
            final int position = mCycleAdapter.findNearestPosition(previousItem);
            mCycleSpinner.setSelection(position);

            // only force-update cycle when changed; skipping preserves any
            // user-defined inspection region.
            final CycleItem selectedItem = mCycleAdapter.getItem(position);
            if (!Objects.equal(selectedItem, previousItem)) {
                mCycleListener.onItemSelected(mCycleSpinner, null, position, 0);
            } else {
                // but still kick off loader for detailed list
                updateDetailData();
            }
        } else {
            updateDetailData();
        }
    }

    private void disableDataForOtherSubscriptions(SubscriptionInfo currentSir) {
        if (mSubInfoList != null) {
            for (SubscriptionInfo subInfo : mSubInfoList) {
                if (subInfo.getSubscriptionId() != currentSir.getSubscriptionId()) {
                    setMobileDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
    }

    private View.OnClickListener mDataEnabledListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mBinding) return;

            final boolean dataEnabled = !mDataEnabled.isChecked();
            final String currentTab = mCurrentTab;
            if (isMobileTab(currentTab)) {
                if (dataEnabled) {
                    // If we are showing the Sim Card tile then we are a Multi-Sim device.
                    if (Utils.showSimCardTile(getActivity())) {
                        handleMultiSimDataDialog();
                    } else {
                        setMobileDataEnabled(getSubId(currentTab), true);
                    }
                } else {
                    // disabling data; show confirmation dialog which eventually
                    // calls setMobileDataEnabled() once user confirms.
                    ConfirmDataDisableFragment.show(DataUsageSummary.this, getSubId(mCurrentTab));
                }
            }

            updatePolicy(false);
        }
    };

    private void handleMultiSimDataDialog() {
        final Context context = getActivity();
        final SubscriptionInfo currentSir = getCurrentTabSubInfo(context);

        //If sim has not loaded after toggling data switch, return.
        if (currentSir == null) {
            return;
        }

        final SubscriptionInfo nextSir = mSubscriptionManager.getActiveSubscriptionInfo(
                mSubscriptionManager.getDefaultDataSubId());

        // If the device is single SIM or is enabling data on the active data SIM then forgo
        // the pop-up.
        if (!Utils.showSimCardTile(context) ||
                (nextSir != null && currentSir != null &&
                currentSir.getSubscriptionId() == nextSir.getSubscriptionId())) {
            setMobileDataEnabled(currentSir.getSubscriptionId(), true);
            if (nextSir != null && currentSir != null &&
                currentSir.getSubscriptionId() == nextSir.getSubscriptionId()) {
                disableDataForOtherSubscriptions(currentSir);
            }
            updateBody();
            return;
        }

        final String previousName = (nextSir == null)
            ? context.getResources().getString(R.string.sim_selection_required_pref)
            : nextSir.getDisplayName().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.sim_change_data_title);
        builder.setMessage(getActivity().getResources().getString(R.string.sim_change_data_message,
                    currentSir.getDisplayName(), previousName));

        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mSubscriptionManager.setDefaultDataSubId(currentSir.getSubscriptionId());
                setMobileDataEnabled(currentSir.getSubscriptionId(), true);
                disableDataForOtherSubscriptions(currentSir);
                updateBody();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        builder.create().show();
    }

    private View.OnClickListener mDisableAtLimitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final boolean disableAtLimit = !mDisableAtLimit.isChecked();
            if (disableAtLimit) {
                // enabling limit; show confirmation dialog which eventually
                // calls setPolicyLimitBytes() once user confirms.
                ConfirmLimitFragment.show(DataUsageSummary.this);
            } else {
                setPolicyLimitBytes(LIMIT_DISABLED);
            }
        }
    };

    private View.OnClickListener mAppRestrictListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final boolean restrictBackground = !mAppRestrict.isChecked();

            if (restrictBackground) {
                // enabling restriction; show confirmation dialog which
                // eventually calls setRestrictBackground() once user
                // confirms.
                ConfirmAppRestrictFragment.show(DataUsageSummary.this);
            } else {
                setAppRestrictBackground(false);
            }
        }
    };

    private OnItemClickListener mListListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Context context = view.getContext();
            final AppItem app = (AppItem) parent.getItemAtPosition(position);

            // TODO: sigh, remove this hack once we understand 6450986
            if (mUidDetailProvider == null || app == null) return;

            final UidDetail detail = mUidDetailProvider.getUidDetail(app.key, true);
            AppDetailsFragment.show(DataUsageSummary.this, app, detail.label);
        }
    };

    private OnItemSelectedListener mCycleListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final CycleItem cycle = (CycleItem) parent.getItemAtPosition(position);
            if (cycle instanceof CycleChangeItem) {
                // show cycle editor; will eventually call setPolicyCycleDay()
                // when user finishes editing.
                CycleEditorFragment.show(DataUsageSummary.this);

                // reset spinner to something other than "change cycle..."
                mCycleSpinner.setSelection(0);

            } else {
                if (LOGD) {
                    Log.d(TAG, "showing cycle " + cycle + ", start=" + cycle.start + ", end="
                            + cycle.end + "]");
                }

                // update chart to show selected cycle, and update detail data
                // to match updated sweep bounds.
                mChart.setVisibleRange(cycle.start, cycle.end);

                updateDetailData();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // ignored
        }
    };

    /**
     * Update details based on {@link #mChart} inspection range depending on
     * current mode. In network mode, updates {@link #mAdapter} with sorted list
     * of applications data usage, and when {@link #isAppDetailMode()} update
     * app details.
     */
    private void updateDetailData() {
        if (LOGD) Log.d(TAG, "updateDetailData()");

        final long start = mChart.getInspectStart();
        final long end = mChart.getInspectEnd();
        final long now = System.currentTimeMillis();

        final Context context = getActivity();

        NetworkStatsHistory.Entry entry = null;
        if (isAppDetailMode() && mChartData != null && mChartData.detail != null) {
            // bind foreground/background to piechart and labels
            entry = mChartData.detailDefault.getValues(start, end, now, entry);
            final long defaultBytes = entry.rxBytes + entry.txBytes;
            entry = mChartData.detailForeground.getValues(start, end, now, entry);
            final long foregroundBytes = entry.rxBytes + entry.txBytes;
            final long totalBytes = defaultBytes + foregroundBytes;

            if (mAppTotal != null) {
                mAppTotal.setText(Formatter.formatFileSize(context, totalBytes));
            }
            mAppBackground.setText(Formatter.formatFileSize(context, defaultBytes));
            mAppForeground.setText(Formatter.formatFileSize(context, foregroundBytes));

            // and finally leave with summary data for label below
            entry = mChartData.detail.getValues(start, end, now, null);

            getLoaderManager().destroyLoader(LOADER_SUMMARY);

            mCycleSummary.setVisibility(View.GONE);

        } else {
            if (mChartData != null) {
                entry = mChartData.network.getValues(start, end, now, null);
            }

            mCycleSummary.setVisibility(View.VISIBLE);

            // kick off loader for detailed stats
            getLoaderManager().restartLoader(LOADER_SUMMARY,
                    SummaryForAllUidLoader.buildArgs(mTemplate, start, end), mSummaryCallbacks);
        }

        final long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;
        final String totalPhrase = Formatter.formatFileSize(context, totalBytes);
        mCycleSummary.setText(totalPhrase);

        if (isMobileTab(mCurrentTab) || TAB_3G.equals(mCurrentTab)
                || TAB_4G.equals(mCurrentTab)) {
            if (isAppDetailMode()) {
                mDisclaimer.setVisibility(View.GONE);
            } else {
                mDisclaimer.setVisibility(View.VISIBLE);
            }
        } else {
            mDisclaimer.setVisibility(View.GONE);
        }

        // initial layout is finished above, ensure we have transitions
        ensureLayoutTransitions();
    }

    private final LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderCallbacks<
            ChartData>() {
        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(getActivity(), mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            mChartData = data;
            mChart.bindNetworkStats(mChartData.network);
            mChart.bindDetailNetworkStats(mChartData.detail);

            // calcuate policy cycles based on available data
            updatePolicy(true);
            updateAppDetail();

            // force scroll to top of body when showing detail
            if (mChartData.detail != null) {
                mListView.smoothScrollToPosition(0);
            }
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
            mChartData = null;
            mChart.bindNetworkStats(null);
            mChart.bindDetailNetworkStats(null);
        }
    };

    private final LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderCallbacks<
            NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(getActivity(), mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            final int[] restrictedUids = mPolicyManager.getUidsWithPolicy(
                    POLICY_REJECT_METERED_BACKGROUND);
            mAdapter.bindStats(data, restrictedUids);
            updateEmptyVisible();
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {
            mAdapter.bindStats(null, new int[0]);
            updateEmptyVisible();
        }

        private void updateEmptyVisible() {
            final boolean isEmpty = mAdapter.isEmpty() && !isAppDetailMode();
            mEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            mStupidPadding.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    };

    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager tele = TelephonyManager.from(context);
        final String actualSubscriberId = tele.getSubscriberId();
        String retVal = SystemProperties.get(TEST_SUBSCRIBER_PROP, actualSubscriberId);
        if (LOGD) Log.d(TAG, "getActiveSubscriberId=" + retVal + " actualSubscriberId=" + actualSubscriberId);
        return retVal;
    }

    private static String getActiveSubscriberId(Context context, int subId) {
        final TelephonyManager tele = TelephonyManager.from(context);
        String retVal = tele.getSubscriberId(subId);
        if (LOGD) Log.d(TAG, "getActiveSubscriberId=" + retVal + " subId=" + subId);
        return retVal;
    }

    private DataUsageChartListener mChartListener = new DataUsageChartListener() {
        @Override
        public void onWarningChanged() {
            setPolicyWarningBytes(mChart.getWarningBytes());
        }

        @Override
        public void onLimitChanged() {
            setPolicyLimitBytes(mChart.getLimitBytes());
        }

        @Override
        public void requestWarningEdit() {
            WarningEditorFragment.show(DataUsageSummary.this);
        }

        @Override
        public void requestLimitEdit() {
            LimitEditorFragment.show(DataUsageSummary.this);
        }
    };

    /**
     * List item that reflects a specific data usage cycle.
     */
    public static class CycleItem implements Comparable<CycleItem> {
        public CharSequence label;
        public long start;
        public long end;

        CycleItem(CharSequence label) {
            this.label = label;
        }

        public CycleItem(Context context, long start, long end) {
            this.label = formatDateRange(context, start, end);
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return label.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CycleItem) {
                final CycleItem another = (CycleItem) o;
                return start == another.start && end == another.end;
            }
            return false;
        }

        @Override
        public int compareTo(CycleItem another) {
            return Long.compare(start, another.start);
        }
    }

    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final java.util.Formatter sFormatter = new java.util.Formatter(
            sBuilder, Locale.getDefault());

    public static String formatDateRange(Context context, long start, long end) {
        final int flags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH;

        synchronized (sBuilder) {
            sBuilder.setLength(0);
            return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null)
                    .toString();
        }
    }

    /**
     * Special-case data usage cycle that triggers dialog to change
     * {@link NetworkPolicy#cycleDay}.
     */
    public static class CycleChangeItem extends CycleItem {
        public CycleChangeItem(Context context) {
            super(context.getString(R.string.data_usage_change_cycle));
        }
    }

    public static class CycleAdapter extends ArrayAdapter<CycleItem> {
        private boolean mChangePossible = false;
        private boolean mChangeVisible = false;

        private final CycleChangeItem mChangeItem;

        public CycleAdapter(Context context) {
            super(context, R.layout.data_usage_cycle_item);
            setDropDownViewResource(R.layout.data_usage_cycle_item_dropdown);
            mChangeItem = new CycleChangeItem(context);
        }

        public void setChangePossible(boolean possible) {
            mChangePossible = possible;
            updateChange();
        }

        public void setChangeVisible(boolean visible) {
            mChangeVisible = visible;
            updateChange();
        }

        private void updateChange() {
            remove(mChangeItem);
            if (mChangePossible && mChangeVisible) {
                add(mChangeItem);
            }
        }

        /**
         * Find position of {@link CycleItem} in this adapter which is nearest
         * the given {@link CycleItem}.
         */
        public int findNearestPosition(CycleItem target) {
            if (target != null) {
                final int count = getCount();
                for (int i = count - 1; i >= 0; i--) {
                    final CycleItem item = getItem(i);
                    if (item instanceof CycleChangeItem) {
                        continue;
                    } else if (item.compareTo(target) >= 0) {
                        return i;
                    }
                }
            }
            return 0;
        }
    }

    public static class AppItem implements Comparable<AppItem>, Parcelable {
        public static final int CATEGORY_USER = 0;
        public static final int CATEGORY_APP_TITLE = 1;
        public static final int CATEGORY_APP = 2;

        public final int key;
        public boolean restricted;
        public int category;

        public SparseBooleanArray uids = new SparseBooleanArray();
        public long total;

        public AppItem() {
            this.key = 0;
        }

        public AppItem(int key) {
            this.key = key;
        }

        public AppItem(Parcel parcel) {
            key = parcel.readInt();
            uids = parcel.readSparseBooleanArray();
            total = parcel.readLong();
        }

        public void addUid(int uid) {
            uids.put(uid, true);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(key);
            dest.writeSparseBooleanArray(uids);
            dest.writeLong(total);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public int compareTo(AppItem another) {
            int comparison = Integer.compare(category, another.category);
            if (comparison == 0) {
                comparison = Long.compare(another.total, total);
            }
            return comparison;
        }

        public static final Creator<AppItem> CREATOR = new Creator<AppItem>() {
            @Override
            public AppItem createFromParcel(Parcel in) {
                return new AppItem(in);
            }

            @Override
            public AppItem[] newArray(int size) {
                return new AppItem[size];
            }
        };
    }

    /**
     * Adapter of applications, sorted by total usage descending.
     */
    public static class DataUsageAdapter extends BaseAdapter {
        private final UidDetailProvider mProvider;
        private final int mInsetSide;
        private final UserManager mUm;

        private ArrayList<AppItem> mItems = Lists.newArrayList();
        private long mLargest;

        public DataUsageAdapter(final UserManager userManager, UidDetailProvider provider, int insetSide) {
            mProvider = checkNotNull(provider);
            mInsetSide = insetSide;
            mUm = userManager;
        }

        /**
         * Bind the given {@link NetworkStats}, or {@code null} to clear list.
         */
        public void bindStats(NetworkStats stats, int[] restrictedUids) {
            mItems.clear();
            mLargest = 0;

            final int currentUserId = ActivityManager.getCurrentUser();
            final List<UserHandle> profiles = mUm.getUserProfiles();
            final SparseArray<AppItem> knownItems = new SparseArray<AppItem>();

            NetworkStats.Entry entry = null;
            final int size = stats != null ? stats.size() : 0;
            for (int i = 0; i < size; i++) {
                entry = stats.getValues(i, entry);

                // Decide how to collapse items together
                final int uid = entry.uid;

                final int collapseKey;
                final int category;
                final int userId = UserHandle.getUserId(uid);
                if (UserHandle.isApp(uid)) {
                    if (profiles.contains(new UserHandle(userId))) {
                        if (userId != currentUserId) {
                            // Add to a managed user item.
                            final int managedKey = UidDetailProvider.buildKeyForUser(userId);
                            accumulate(managedKey, knownItems, entry,
                                    AppItem.CATEGORY_USER);
                        }
                        // Add to app item.
                        collapseKey = uid;
                        category = AppItem.CATEGORY_APP;
                    } else {
                        // If it is a removed user add it to the removed users' key
                        final UserInfo info = mUm.getUserInfo(userId);
                        if (info == null) {
                            collapseKey = UID_REMOVED;
                            category = AppItem.CATEGORY_APP;
                        } else {
                            // Add to other user item.
                            collapseKey = UidDetailProvider.buildKeyForUser(userId);
                            category = AppItem.CATEGORY_USER;
                        }
                    }
                } else if (uid == UID_REMOVED || uid == UID_TETHERING) {
                    collapseKey = uid;
                    category = AppItem.CATEGORY_APP;
                } else {
                    collapseKey = android.os.Process.SYSTEM_UID;
                    category = AppItem.CATEGORY_APP;
                }
                accumulate(collapseKey, knownItems, entry, category);
            }

            final int restrictedUidsMax = restrictedUids.length;
            for (int i = 0; i < restrictedUidsMax; ++i) {
                final int uid = restrictedUids[i];
                // Only splice in restricted state for current user or managed users
                if (!profiles.contains(new UserHandle(UserHandle.getUserId(uid)))) {
                    continue;
                }

                AppItem item = knownItems.get(uid);
                if (item == null) {
                    item = new AppItem(uid);
                    item.total = -1;
                    mItems.add(item);
                    knownItems.put(item.key, item);
                }
                item.restricted = true;
            }

            if (!mItems.isEmpty()) {
                final AppItem title = new AppItem();
                title.category = AppItem.CATEGORY_APP_TITLE;
                mItems.add(title);
            }

            Collections.sort(mItems);
            notifyDataSetChanged();
        }

        /**
         * Accumulate data usage of a network stats entry for the item mapped by the collapse key.
         * Creates the item if needed.
         *
         * @param collapseKey the collapse key used to map the item.
         * @param knownItems collection of known (already existing) items.
         * @param entry the network stats entry to extract data usage from.
         * @param itemCategory the item is categorized on the list view by this category. Must be
         *            either AppItem.APP_ITEM_CATEGORY or AppItem.MANAGED_USER_ITEM_CATEGORY
         */
        private void accumulate(int collapseKey, final SparseArray<AppItem> knownItems,
                NetworkStats.Entry entry, int itemCategory) {
            final int uid = entry.uid;
            AppItem item = knownItems.get(collapseKey);
            if (item == null) {
                item = new AppItem(collapseKey);
                item.category = itemCategory;
                mItems.add(item);
                knownItems.put(item.key, item);
            }
            item.addUid(uid);
            item.total += entry.rxBytes + entry.txBytes;
            if (mLargest < item.total) {
                mLargest = item.total;
            }
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position).key;
        }

        /**
         * See {@link #getItemViewType} for the view types.
         */
        @Override
        public int getViewTypeCount() {
            return 2;
        }

        /**
         * Returns 1 for separator items and 0 for anything else.
         */
        @Override
        public int getItemViewType(int position) {
            final AppItem item = mItems.get(position);
            if (item.category == AppItem.CATEGORY_APP_TITLE) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (position > mItems.size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return getItemViewType(position) == 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final AppItem item = mItems.get(position);
            if (getItemViewType(position) == 1) {
                if (convertView == null) {
                    convertView = inflateCategoryHeader(LayoutInflater.from(parent.getContext()),
                            parent);
                }

                final TextView title = (TextView) convertView.findViewById(android.R.id.title);
                title.setText(R.string.data_usage_app);

            } else {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.data_usage_item, parent, false);

                    if (mInsetSide > 0) {
                        convertView.setPaddingRelative(mInsetSide, 0, mInsetSide, 0);
                    }
                }

                final Context context = parent.getContext();

                final TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                final ProgressBar progress = (ProgressBar) convertView.findViewById(
                        android.R.id.progress);

                // kick off async load of app details
                UidDetailTask.bindView(mProvider, item, convertView);

                if (item.restricted && item.total <= 0) {
                    text1.setText(R.string.data_usage_app_restricted);
                    progress.setVisibility(View.GONE);
                } else {
                    text1.setText(Formatter.formatFileSize(context, item.total));
                    progress.setVisibility(View.VISIBLE);
                }

                final int percentTotal = mLargest != 0 ? (int) (item.total * 100 / mLargest) : 0;
                progress.setProgress(percentTotal);
            }

            return convertView;
        }
    }

    /**
     * Empty {@link Fragment} that controls display of UID details in
     * {@link DataUsageSummary}.
     */
    public static class AppDetailsFragment extends Fragment {
        private static final String EXTRA_APP = "app";

        public static void show(DataUsageSummary parent, AppItem app, CharSequence label) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_APP, app);

            final AppDetailsFragment fragment = new AppDetailsFragment();
            fragment.setArguments(args);
            fragment.setTargetFragment(parent, 0);
            final FragmentTransaction ft = parent.getFragmentManager().beginTransaction();
            ft.add(fragment, TAG_APP_DETAILS);
            ft.addToBackStack(TAG_APP_DETAILS);
            ft.setBreadCrumbTitle(
                    parent.getResources().getString(R.string.data_usage_app_summary_title));
            ft.commitAllowingStateLoss();
        }

        @Override
        public void onStart() {
            super.onStart();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mCurrentApp = getArguments().getParcelable(EXTRA_APP);
            target.updateBody();
        }

        @Override
        public void onStop() {
            super.onStop();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mCurrentApp = null;
            target.updateBody();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link NetworkPolicy#limitBytes}.
     */
    public static class ConfirmLimitFragment extends DialogFragment {
        private static final String EXTRA_MESSAGE = "message";
        private static final String EXTRA_LIMIT_BYTES = "limitBytes";

        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final NetworkPolicy policy = parent.mPolicyEditor.getPolicy(parent.mTemplate);
            if (policy == null) return;

            final Resources res = parent.getResources();
            final CharSequence message;
            final long minLimitBytes = (long) (policy.warningBytes * 1.2f);
            final long limitBytes;

            // TODO: customize default limits based on network template
            final String currentTab = parent.mCurrentTab;
            if (TAB_3G.equals(currentTab)) {
                message = res.getString(R.string.data_usage_limit_dialog_mobile);
                limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);
            } else if (TAB_4G.equals(currentTab)) {
                message = res.getString(R.string.data_usage_limit_dialog_mobile);
                limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);
            } else if (isMobileTab(currentTab)) {
                message = res.getString(R.string.data_usage_limit_dialog_mobile);
                limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);
            } else {
                throw new IllegalArgumentException("unknown current tab: " + currentTab);
            }

            final Bundle args = new Bundle();
            args.putCharSequence(EXTRA_MESSAGE, message);
            args.putLong(EXTRA_LIMIT_BYTES, limitBytes);

            final ConfirmLimitFragment dialog = new ConfirmLimitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_LIMIT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final CharSequence message = getArguments().getCharSequence(EXTRA_MESSAGE);
            final long limitBytes = getArguments().getLong(EXTRA_LIMIT_BYTES);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_limit_dialog_title);
            builder.setMessage(message);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        target.setPolicyLimitBytes(limitBytes);
                    }
                }
            });

            return builder.create();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#cycleDay}.
     */
    public static class CycleEditorFragment extends DialogFragment {
        private static final String EXTRA_TEMPLATE = "template";

        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mTemplate);

            final CycleEditorFragment dialog = new CycleEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CYCLE_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false);
            final NumberPicker cycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final int cycleDay = editor.getPolicyCycleDay(template);

            cycleDayPicker.setMinValue(1);
            cycleDayPicker.setMaxValue(31);
            cycleDayPicker.setValue(cycleDay);
            cycleDayPicker.setWrapSelectorWheel(true);

            builder.setTitle(R.string.data_usage_cycle_editor_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            cycleDayPicker.clearFocus();

                            final int cycleDay = cycleDayPicker.getValue();
                            final String cycleTimezone = new Time().timezone;
                            editor.setPolicyCycleDay(template, cycleDay, cycleTimezone);
                            target.updatePolicy(true);
                        }
                    });

            return builder.create();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#warningBytes}.
     */
    public static class WarningEditorFragment extends DialogFragment {
        private static final String EXTRA_TEMPLATE = "template";

        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mTemplate);

            final WarningEditorFragment dialog = new WarningEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_WARNING_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_bytes_editor, null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final long warningBytes = editor.getPolicyWarningBytes(template);
            final long limitBytes = editor.getPolicyLimitBytes(template);

            bytesPicker.setMinValue(0);
            if (limitBytes != LIMIT_DISABLED) {
                bytesPicker.setMaxValue((int) (limitBytes / MB_IN_BYTES) - 1);
            } else {
                bytesPicker.setMaxValue(Integer.MAX_VALUE);
            }
            bytesPicker.setValue((int) (warningBytes / MB_IN_BYTES));
            bytesPicker.setWrapSelectorWheel(false);

            builder.setTitle(R.string.data_usage_warning_editor_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            bytesPicker.clearFocus();

                            final long bytes = bytesPicker.getValue() * MB_IN_BYTES;
                            editor.setPolicyWarningBytes(template, bytes);
                            target.updatePolicy(false);
                        }
                    });

            return builder.create();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#limitBytes}.
     */
    public static class LimitEditorFragment extends DialogFragment {
        private static final String EXTRA_TEMPLATE = "template";

        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mTemplate);

            final LimitEditorFragment dialog = new LimitEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_LIMIT_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_bytes_editor, null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final long warningBytes = editor.getPolicyWarningBytes(template);
            final long limitBytes = editor.getPolicyLimitBytes(template);

            bytesPicker.setMaxValue(Integer.MAX_VALUE);
            if (warningBytes != WARNING_DISABLED && limitBytes > 0) {
                bytesPicker.setMinValue((int) (warningBytes / MB_IN_BYTES) + 1);
            } else {
                bytesPicker.setMinValue(0);
            }
            bytesPicker.setValue((int) (limitBytes / MB_IN_BYTES));
            bytesPicker.setWrapSelectorWheel(false);

            builder.setTitle(R.string.data_usage_limit_editor_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            bytesPicker.clearFocus();

                            final long bytes = bytesPicker.getValue() * MB_IN_BYTES;
                            editor.setPolicyLimitBytes(template, bytes);
                            target.updatePolicy(false);
                        }
                    });

            return builder.create();
        }
    }
    /**
     * Dialog to request user confirmation before disabling data.
     */
    public static class ConfirmDataDisableFragment extends DialogFragment {
        static int mSubId;
        public static void show(DataUsageSummary parent, int subId) {
            mSubId = subId;
            if (!parent.isAdded()) return;

            final ConfirmDataDisableFragment dialog = new ConfirmDataDisableFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_DATA_DISABLE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.data_usage_disable_mobile);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        // TODO: extend to modify policy enabled flag.
                        target.setMobileDataEnabled(mSubId, false);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link INetworkPolicyManager#setRestrictBackground(boolean)}.
     */
    public static class ConfirmRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final ConfirmRestrictFragment dialog = new ConfirmRestrictFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_RESTRICT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_restrict_background_title);
            if (Utils.hasMultipleUsers(context)) {
                builder.setMessage(R.string.data_usage_restrict_background_multiuser);
            } else {
                builder.setMessage(R.string.data_usage_restrict_background);
            }

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        target.setRestrictBackground(true);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Dialog to inform user that {@link #POLICY_REJECT_METERED_BACKGROUND}
     * change has been denied, usually based on
     * {@link DataUsageSummary#hasLimitedNetworks()}.
     */
    public static class DeniedRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final DeniedRestrictFragment dialog = new DeniedRestrictFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_DENIED_RESTRICT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_app_restrict_background);
            builder.setMessage(R.string.data_usage_restrict_denied_dialog);
            builder.setPositiveButton(android.R.string.ok, null);

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link #POLICY_REJECT_METERED_BACKGROUND}.
     */
    public static class ConfirmAppRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final ConfirmAppRestrictFragment dialog = new ConfirmAppRestrictFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_APP_RESTRICT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_app_restrict_dialog_title);
            builder.setMessage(R.string.data_usage_app_restrict_dialog);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        target.setAppRestrictBackground(true);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Compute default tab that should be selected, based on
     * {@link NetworkPolicyManager#EXTRA_NETWORK_TEMPLATE} extra.
     */
    private static String computeTabFromIntent(Intent intent) {
        final NetworkTemplate template = intent.getParcelableExtra(EXTRA_NETWORK_TEMPLATE);
        if (template == null) {
            final int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                return TAB_MOBILE + String.valueOf(subId);
            }
            return null;
        }

        switch (template.getMatchRule()) {
            case MATCH_MOBILE_3G_LOWER:
                return TAB_3G;
            case MATCH_MOBILE_4G:
                return TAB_4G;
            case MATCH_MOBILE_ALL:
                return TAB_MOBILE;
            case MATCH_WIFI:
                return TAB_WIFI;
            default:
                return null;
        }
    }

    /**
     * Background task that loads {@link UidDetail}, binding to
     * {@link DataUsageAdapter} row item when finished.
     */
    private static class UidDetailTask extends AsyncTask<Void, Void, UidDetail> {
        private final UidDetailProvider mProvider;
        private final AppItem mItem;
        private final View mTarget;

        private UidDetailTask(UidDetailProvider provider, AppItem item, View target) {
            mProvider = checkNotNull(provider);
            mItem = checkNotNull(item);
            mTarget = checkNotNull(target);
        }

        public static void bindView(
                UidDetailProvider provider, AppItem item, View target) {
            final UidDetailTask existing = (UidDetailTask) target.getTag();
            if (existing != null) {
                existing.cancel(false);
            }

            final UidDetail cachedDetail = provider.getUidDetail(item.key, false);
            if (cachedDetail != null) {
                bindView(cachedDetail, target);
            } else {
                target.setTag(new UidDetailTask(provider, item, target).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR));
            }
        }

        private static void bindView(UidDetail detail, View target) {
            final ImageView icon = (ImageView) target.findViewById(android.R.id.icon);
            final TextView title = (TextView) target.findViewById(android.R.id.title);

            if (detail != null) {
                icon.setImageDrawable(detail.icon);
                title.setText(detail.label);
                title.setContentDescription(detail.contentDescription);
            } else {
                icon.setImageDrawable(null);
                title.setText(null);
            }
        }

        @Override
        protected void onPreExecute() {
            bindView(null, mTarget);
        }

        @Override
        protected UidDetail doInBackground(Void... params) {
            return mProvider.getUidDetail(mItem.key, true);
        }

        @Override
        protected void onPostExecute(UidDetail result) {
            bindView(result, mTarget);
        }
    }

    /**
     * Test if device has a mobile data radio with SIM in ready state.
     */
    public static boolean hasReadyMobileRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("mobile");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final TelephonyManager tele = TelephonyManager.from(context);

        final List<SubscriptionInfo> subInfoList =
                SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        // No activated Subscriptions
        if (subInfoList == null) {
            if (LOGD) Log.d(TAG, "hasReadyMobileRadio: subInfoList=null");
            return false;
        }
        // require both supported network and ready SIM
        boolean isReady = true;
        for (SubscriptionInfo subInfo : subInfoList) {
            isReady = isReady & tele.getSimState(subInfo.getSimSlotIndex()) == SIM_STATE_READY;
            if (LOGD) Log.d(TAG, "hasReadyMobileRadio: subInfo=" + subInfo);
        }
        boolean retVal = conn.isNetworkSupported(TYPE_MOBILE) && isReady;
        if (LOGD) {
            Log.d(TAG, "hasReadyMobileRadio:"
                    + " conn.isNetworkSupported(TYPE_MOBILE)="
                                            + conn.isNetworkSupported(TYPE_MOBILE)
                    + " isReady=" + isReady);
        }
        return retVal;
    }

    /*
     * TODO: consider adding to TelephonyManager or SubscritpionManager.
     */
    public static boolean hasReadyMobileRadio(Context context, int subId) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("mobile");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final TelephonyManager tele = TelephonyManager.from(context);
        final int slotId = SubscriptionManager.getSlotId(subId);
        final boolean isReady = tele.getSimState(slotId) == SIM_STATE_READY;

        boolean retVal =  conn.isNetworkSupported(TYPE_MOBILE) && isReady;
        if (LOGD) Log.d(TAG, "hasReadyMobileRadio: subId=" + subId
                + " conn.isNetworkSupported(TYPE_MOBILE)=" + conn.isNetworkSupported(TYPE_MOBILE)
                + " isReady=" + isReady);
        return retVal;
    }

    /**
     * Test if device has a mobile 4G data radio.
     */
    public static boolean hasReadyMobile4gRadio(Context context) {
        if (!NetworkPolicyEditor.ENABLE_SPLIT_POLICIES) {
            return false;
        }
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("4g");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final TelephonyManager tele = TelephonyManager.from(context);

        final boolean hasWimax = conn.isNetworkSupported(TYPE_WIMAX);
        final boolean hasLte = (tele.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE)
                && hasReadyMobileRadio(context);
        return hasWimax || hasLte;
    }

    /**
     * Test if device has a Wi-Fi data radio.
     */
    public static boolean hasWifiRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("wifi");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        return conn.isNetworkSupported(TYPE_WIFI);
    }

    /**
     * Test if device has an ethernet network connection.
     */
    public boolean hasEthernet(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("ethernet");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final boolean hasEthernet = conn.isNetworkSupported(TYPE_ETHERNET);

        final long ethernetBytes;
        if (mStatsSession != null) {
            try {
                ethernetBytes = mStatsSession.getSummaryForNetwork(
                        NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE)
                        .getTotalBytes();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        } else {
            ethernetBytes = 0;
        }

        // only show ethernet when both hardware present and traffic has occurred
        return hasEthernet && ethernetBytes > 0;
    }

    /**
     * Inflate a {@link Preference} style layout, adding the given {@link View}
     * widget into {@link android.R.id#widget_frame}.
     */
    private static View inflatePreference(LayoutInflater inflater, ViewGroup root, View widget) {
        final View view = inflater.inflate(R.layout.preference, root, false);
        final LinearLayout widgetFrame = (LinearLayout) view.findViewById(
                android.R.id.widget_frame);
        widgetFrame.addView(widget, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        return view;
    }

    private static View inflateCategoryHeader(LayoutInflater inflater, ViewGroup root) {
        final TypedArray a = inflater.getContext().obtainStyledAttributes(null,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.attr.preferenceCategoryStyle, 0);
        final int resId = a.getResourceId(com.android.internal.R.styleable.Preference_layout, 0);
        return inflater.inflate(resId, root, false);
    }

    /**
     * Test if any networks are currently limited.
     */
    private boolean hasLimitedNetworks() {
        return !buildLimitedNetworksList().isEmpty();
    }

    /**
     * Build string describing currently limited networks, which defines when
     * background data is restricted.
     */
    @Deprecated
    private CharSequence buildLimitedNetworksString() {
        final List<CharSequence> limited = buildLimitedNetworksList();

        // handle case where no networks limited
        if (limited.isEmpty()) {
            limited.add(getText(R.string.data_usage_list_none));
        }

        return TextUtils.join(limited);
    }

    /**
     * Build list of currently limited networks, which defines when background
     * data is restricted.
     */
    @Deprecated
    private List<CharSequence> buildLimitedNetworksList() {
        final Context context = getActivity();

        // build combined list of all limited networks
        final ArrayList<CharSequence> limited = Lists.newArrayList();

        final TelephonyManager tele = TelephonyManager.from(context);
        if (tele.getSimState() == SIM_STATE_READY) {
            final String subscriberId = getActiveSubscriberId(context);
            if (mPolicyEditor.hasLimitedPolicy(buildTemplateMobileAll(subscriberId))) {
                limited.add(getText(R.string.data_usage_list_mobile));
            }
            if (mPolicyEditor.hasLimitedPolicy(buildTemplateMobile3gLower(subscriberId))) {
                limited.add(getText(R.string.data_usage_tab_3g));
            }
            if (mPolicyEditor.hasLimitedPolicy(buildTemplateMobile4g(subscriberId))) {
                limited.add(getText(R.string.data_usage_tab_4g));
            }
        }

        if (mPolicyEditor.hasLimitedPolicy(buildTemplateWifiWildcard())) {
            limited.add(getText(R.string.data_usage_tab_wifi));
        }
        if (mPolicyEditor.hasLimitedPolicy(buildTemplateEthernet())) {
            limited.add(getText(R.string.data_usage_tab_ethernet));
        }

        return limited;
    }

    /**
     * Inset both selector and divider {@link Drawable} on the given
     * {@link ListView} by the requested dimensions.
     */
    private static void insetListViewDrawables(ListView view, int insetSide) {
        final Drawable selector = view.getSelector();
        final Drawable divider = view.getDivider();

        // fully unregister these drawables so callbacks can be maintained after
        // wrapping below.
        final Drawable stub = new ColorDrawable(Color.TRANSPARENT);
        view.setSelector(stub);
        view.setDivider(stub);

        view.setSelector(new InsetBoundsDrawable(selector, insetSide));
        view.setDivider(new InsetBoundsDrawable(divider, insetSide));
    }

    /**
     * Set {@link android.R.id#title} for a preference view inflated with
     * {@link #inflatePreference(LayoutInflater, ViewGroup, View)}.
     */
    private static void setPreferenceTitle(View parent, int resId) {
        final TextView title = (TextView) parent.findViewById(android.R.id.title);
        title.setText(resId);
    }

    /**
     * Set {@link android.R.id#summary} for a preference view inflated with
     * {@link #inflatePreference(LayoutInflater, ViewGroup, View)}.
     */
    private static void setPreferenceSummary(View parent, CharSequence string) {
        final TextView summary = (TextView) parent.findViewById(android.R.id.summary);
        summary.setVisibility(View.VISIBLE);
        summary.setText(string);
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.data_usage_summary_title);
                data.screenTitle = res.getString(R.string.data_usage_summary_title);
                result.add(data);

                // Mobile data
                data = new SearchIndexableRaw(context);
                data.key = DATA_USAGE_ENABLE_MOBILE_KEY;
                data.title = res.getString(R.string.data_usage_enable_mobile);
                data.screenTitle = res.getString(R.string.data_usage_summary_title);
                result.add(data);

                // Set mobile data limit
                data = new SearchIndexableRaw(context);
                data.key = DATA_USAGE_DISABLE_MOBILE_LIMIT_KEY;
                data.title = res.getString(R.string.data_usage_disable_mobile_limit);
                data.screenTitle = res.getString(R.string.data_usage_summary_title);
                result.add(data);

                // Data usage cycle
                data = new SearchIndexableRaw(context);
                data.key = DATA_USAGE_CYCLE_KEY;
                data.title = res.getString(R.string.data_usage_cycle);
                data.screenTitle = res.getString(R.string.data_usage_summary_title);
                result.add(data);

                return result;
            }
        };

        private void addMobileTab(Context context, SubscriptionInfo subInfo, boolean isMultiSim) {
            if (subInfo != null && mMobileTagMap != null) {
                if (hasReadyMobileRadio(context, subInfo.getSubscriptionId())) {
                    if (isMultiSim) {
                        mTabHost.addTab(buildTabSpec(mMobileTagMap.get(subInfo.getSubscriptionId()),
                                subInfo.getDisplayName()));
                    } else {
                        mTabHost.addTab(buildTabSpec(mMobileTagMap.get(subInfo.getSubscriptionId()),
                                R.string.data_usage_tab_mobile));
                    }
                }
            } else {
                if (LOGD) Log.d(TAG, "addMobileTab: subInfoList is null");
            }
        }

        private SubscriptionInfo getCurrentTabSubInfo(Context context) {
            if (mSubInfoList != null && mTabHost != null) {
                final int currentTagIndex = mTabHost.getCurrentTab();
                int i = 0;
                for (SubscriptionInfo subInfo : mSubInfoList) {
                    if (hasReadyMobileRadio(context, subInfo.getSubscriptionId())) {
                        if (i++ == currentTagIndex) {
                            return subInfo;
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Init a map with subId key and mobile tag name
         * @param subInfoList The subscription Info List
         * @return The map or null if no activated subscription
         */
        private Map<Integer, String> initMobileTabTag(List<SubscriptionInfo> subInfoList) {
            Map<Integer, String> map = null;
            if (subInfoList != null) {
                String mobileTag;
                map = new HashMap<Integer, String>();
                for (SubscriptionInfo subInfo : subInfoList) {
                    mobileTag = TAB_MOBILE + String.valueOf(subInfo.getSubscriptionId());
                    map.put(subInfo.getSubscriptionId(), mobileTag);
                }
            }
            return map;
        }

        private static boolean isMobileTab(String currentTab) {
            return currentTab != null ? currentTab.contains(TAB_MOBILE) : false;
        }

        private int getSubId(String currentTab) {
            if (mMobileTagMap != null) {
                Set<Integer> set = mMobileTagMap.keySet();
                for (Integer subId : set) {
                    if (mMobileTagMap.get(subId).equals(currentTab)) {
                        return subId;
                    }
                }
            }
            Log.e(TAG, "currentTab = " + currentTab + " non mobile tab called this function");
            return -1;
        }

        //SUB SELECT
        private boolean isMobileDataAvailable(long subId) {
            int[] subIds = SubscriptionManager.getSubId(PhoneConstants.SUB1);
            if (subIds != null && subIds[0] == subId) {
                return true;
            }

            subIds = SubscriptionManager.getSubId(PhoneConstants.SUB2);
            if (subIds != null && subIds[0] == subId) {
                return true;
            }

            subIds = SubscriptionManager.getSubId(PhoneConstants.SUB3);
            if (subIds != null && subIds[0] == subId) {
                return true;
            }
            return false;
        }
}
