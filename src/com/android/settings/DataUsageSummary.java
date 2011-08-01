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
import static android.net.ConnectivityManager.TYPE_WIMAX;
import static android.net.NetworkPolicy.LIMIT_DISABLED;
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
import static android.net.NetworkTemplate.buildTemplateWifi;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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

import com.android.internal.telephony.Phone;
import com.android.settings.net.NetworkPolicyEditor;
import com.android.settings.net.SummaryForAllUidLoader;
import com.android.settings.widget.DataUsageChartView;
import com.android.settings.widget.DataUsageChartView.DataUsageChartListener;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import libcore.util.Objects;

/**
 * Panel show data usage history across various networks, including options to
 * inspect based on usage cycle and control through {@link NetworkPolicy}.
 */
public class DataUsageSummary extends Fragment {
    private static final String TAG = "DataUsage";
    private static final boolean LOGD = true;

    // TODO: remove this testing code
    private static final boolean TEST_ANIM = false;
    private static final boolean TEST_RADIOS = false;
    private static final String TEST_RADIOS_PROP = "test.radios";

    private static final String TAB_3G = "3g";
    private static final String TAB_4G = "4g";
    private static final String TAB_MOBILE = "mobile";
    private static final String TAB_WIFI = "wifi";
    private static final String TAB_ETHERNET = "ethernet";

    private static final String TAG_CONFIRM_ROAMING = "confirmRoaming";
    private static final String TAG_CONFIRM_LIMIT = "confirmLimit";
    private static final String TAG_CYCLE_EDITOR = "cycleEditor";
    private static final String TAG_CONFIRM_RESTRICT = "confirmRestrict";
    private static final String TAG_CONFIRM_APP_RESTRICT = "confirmAppRestrict";
    private static final String TAG_APP_DETAILS = "appDetails";

    private static final int LOADER_SUMMARY = 2;

    private static final long KB_IN_BYTES = 1024;
    private static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    private static final long GB_IN_BYTES = MB_IN_BYTES * 1024;

    private INetworkStatsService mStatsService;
    private INetworkPolicyManager mPolicyService;
    private ConnectivityManager mConnService;

    private static final String PREF_FILE = "data_usage";
    private static final String PREF_SHOW_WIFI = "show_wifi";
    private static final String PREF_SHOW_ETHERNET = "show_ethernet";

    private SharedPreferences mPrefs;

    private TabHost mTabHost;
    private ViewGroup mTabsContainer;
    private TabWidget mTabWidget;
    private ListView mListView;
    private DataUsageAdapter mAdapter;

    private ViewGroup mHeader;

    private ViewGroup mNetworkSwitchesContainer;
    private LinearLayout mNetworkSwitches;
    private Switch mDataEnabled;
    private View mDataEnabledView;
    private CheckBox mDisableAtLimit;
    private View mDisableAtLimitView;

    private View mCycleView;
    private Spinner mCycleSpinner;
    private CycleAdapter mCycleAdapter;

    private DataUsageChartView mChart;
    private TextView mUsageSummary;
    private TextView mEmpty;

    private View mAppDetail;
    private TextView mAppTitle;
    private TextView mAppSubtitle;
    private Button mAppSettings;

    private LinearLayout mAppSwitches;
    private CheckBox mAppRestrict;
    private View mAppRestrictView;

    private boolean mShowWifi = false;
    private boolean mShowEthernet = false;

    private NetworkTemplate mTemplate = null;

    private static final int UID_NONE = -1;
    private int mUid = UID_NONE;

    private Intent mAppSettingsIntent;

    private NetworkPolicyEditor mPolicyEditor;

    private NetworkStatsHistory mHistory;
    private NetworkStatsHistory mDetailHistory;

    private String mCurrentTab = null;
    private String mIntentTab = null;

    private MenuItem mMenuDataRoaming;
    private MenuItem mMenuRestrictBackground;

    /** Flag used to ignore listeners during binding. */
    private boolean mBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyService = INetworkPolicyManager.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
        mConnService = (ConnectivityManager) getActivity().getSystemService(
                Context.CONNECTIVITY_SERVICE);

        mPrefs = getActivity().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

        mPolicyEditor = new NetworkPolicyEditor(mPolicyService);
        mPolicyEditor.read();

        mShowWifi = mPrefs.getBoolean(PREF_SHOW_WIFI, false);
        mShowEthernet = mPrefs.getBoolean(PREF_SHOW_ETHERNET, false);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final Context context = inflater.getContext();
        final View view = inflater.inflate(R.layout.data_usage_summary, container, false);

        mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        mTabsContainer = (ViewGroup) view.findViewById(R.id.tabs_container);
        mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
        mListView = (ListView) view.findViewById(android.R.id.list);

        mTabHost.setup();
        mTabHost.setOnTabChangedListener(mTabListener);

        mHeader = (ViewGroup) inflater.inflate(R.layout.data_usage_header, mListView, false);
        mListView.addHeaderView(mHeader, null, false);

        {
            // bind network switches
            mNetworkSwitchesContainer = (ViewGroup) mHeader.findViewById(
                    R.id.network_switches_container);
            mNetworkSwitches = (LinearLayout) mHeader.findViewById(R.id.network_switches);

            mDataEnabled = new Switch(inflater.getContext());
            mDataEnabledView = inflatePreference(inflater, mNetworkSwitches, mDataEnabled);
            mDataEnabled.setOnCheckedChangeListener(mDataEnabledListener);
            mNetworkSwitches.addView(mDataEnabledView);

            mDisableAtLimit = new CheckBox(inflater.getContext());
            mDisableAtLimit.setClickable(false);
            mDisableAtLimitView = inflatePreference(inflater, mNetworkSwitches, mDisableAtLimit);
            mDisableAtLimitView.setOnClickListener(mDisableAtLimitListener);
            mNetworkSwitches.addView(mDisableAtLimitView);
        }

        // bind cycle dropdown
        mCycleView = mHeader.findViewById(R.id.cycles);
        mCycleSpinner = (Spinner) mCycleView.findViewById(R.id.cycles_spinner);
        mCycleAdapter = new CycleAdapter(context);
        mCycleSpinner.setAdapter(mCycleAdapter);
        mCycleSpinner.setOnItemSelectedListener(mCycleListener);

        mChart = (DataUsageChartView) mHeader.findViewById(R.id.chart);
        mChart.setListener(mChartListener);

        {
            // bind app detail controls
            mAppDetail = mHeader.findViewById(R.id.app_detail);
            mAppTitle = (TextView) mAppDetail.findViewById(R.id.app_title);
            mAppSubtitle = (TextView) mAppDetail.findViewById(R.id.app_subtitle);
            mAppSwitches = (LinearLayout) mAppDetail.findViewById(R.id.app_switches);

            mAppSettings = (Button) mAppDetail.findViewById(R.id.app_settings);
            mAppSettings.setOnClickListener(mAppSettingsListener);

            mAppRestrict = new CheckBox(inflater.getContext());
            mAppRestrict.setClickable(false);
            mAppRestrictView = inflatePreference(inflater, mAppSwitches, mAppRestrict);
            setPreferenceTitle(mAppRestrictView, R.string.data_usage_app_restrict_background);
            setPreferenceSummary(
                    mAppRestrictView, R.string.data_usage_app_restrict_background_summary);
            mAppRestrictView.setOnClickListener(mAppRestrictListener);
            mAppSwitches.addView(mAppRestrictView);
        }

        mUsageSummary = (TextView) mHeader.findViewById(R.id.usage_summary);
        mEmpty = (TextView) mHeader.findViewById(android.R.id.empty);

        // only assign layout transitions once first layout is finished
        mListView.getViewTreeObserver().addOnGlobalLayoutListener(mFirstLayoutListener);

        mAdapter = new DataUsageAdapter();
        mListView.setOnItemClickListener(mListListener);
        mListView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // pick default tab based on incoming intent
        final Intent intent = getActivity().getIntent();
        mIntentTab = computeTabFromIntent(intent);

        // this kicks off chain reaction which creates tabs, binds the body to
        // selected network, and binds chart, cycles and detail list.
        updateTabs();

        // kick off background task to update stats
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mStatsService.forceUpdate();
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
        }.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.data_usage, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final Context context = getActivity();

        mMenuDataRoaming = menu.findItem(R.id.data_usage_menu_roaming);
        mMenuDataRoaming.setVisible(hasMobileRadio(context));
        mMenuDataRoaming.setChecked(getDataRoaming());

        mMenuRestrictBackground = menu.findItem(R.id.data_usage_menu_restrict_background);
        mMenuRestrictBackground.setChecked(getRestrictBackground());

        final MenuItem split4g = menu.findItem(R.id.data_usage_menu_split_4g);
        split4g.setVisible(hasMobile4gRadio(context));
        split4g.setChecked(isMobilePolicySplit());

        final MenuItem showWifi = menu.findItem(R.id.data_usage_menu_show_wifi);
        if (hasWifiRadio(context) && hasMobileRadio(context)) {
            showWifi.setVisible(true);
            showWifi.setChecked(mShowWifi);
        } else {
            showWifi.setVisible(false);
            mShowWifi = true;
        }

        final MenuItem showEthernet = menu.findItem(R.id.data_usage_menu_show_ethernet);
        if (hasEthernet(context) && hasMobileRadio(context)) {
            showEthernet.setVisible(true);
            showEthernet.setChecked(mShowEthernet);
        } else {
            showEthernet.setVisible(false);
            mShowEthernet = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.data_usage_menu_roaming: {
                final boolean dataRoaming = !item.isChecked();
                if (dataRoaming) {
                    ConfirmDataRoamingFragment.show(this);
                } else {
                    // no confirmation to disable roaming
                    setDataRoaming(false);
                }
                return true;
            }
            case R.id.data_usage_menu_restrict_background: {
                final boolean restrictBackground = !item.isChecked();
                if (restrictBackground) {
                    ConfirmRestrictFragment.show(this);
                } else {
                    // no confirmation to drop restriction
                    setRestrictBackground(false);
                }
                return true;
            }
            case R.id.data_usage_menu_split_4g: {
                final boolean mobileSplit = !item.isChecked();
                setMobilePolicySplit(mobileSplit);
                item.setChecked(isMobilePolicySplit());
                updateTabs();
                return true;
            }
            case R.id.data_usage_menu_show_wifi: {
                mShowWifi = !item.isChecked();
                mPrefs.edit().putBoolean(PREF_SHOW_WIFI, mShowWifi).apply();
                item.setChecked(mShowWifi);
                updateTabs();
                return true;
            }
            case R.id.data_usage_menu_show_ethernet: {
                mShowEthernet = !item.isChecked();
                mPrefs.edit().putBoolean(PREF_SHOW_ETHERNET, mShowEthernet).apply();
                item.setChecked(mShowEthernet);
                updateTabs();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mDataEnabledView = null;
        mDisableAtLimitView = null;
    }

    /**
     * Listener to setup {@link LayoutTransition} after first layout pass.
     */
    private OnGlobalLayoutListener mFirstLayoutListener = new OnGlobalLayoutListener() {
        /** {@inheritDoc} */
        public void onGlobalLayout() {
            mListView.getViewTreeObserver().removeGlobalOnLayoutListener(mFirstLayoutListener);

            mTabsContainer.setLayoutTransition(buildLayoutTransition());
            mHeader.setLayoutTransition(buildLayoutTransition());
            mNetworkSwitchesContainer.setLayoutTransition(buildLayoutTransition());

            final LayoutTransition chartTransition = buildLayoutTransition();
            chartTransition.setStartDelay(LayoutTransition.APPEARING, 0);
            chartTransition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
            mChart.setLayoutTransition(chartTransition);
        }
    };

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

        final boolean mobileSplit = isMobilePolicySplit();
        if (mobileSplit && hasMobile4gRadio(context)) {
            mTabHost.addTab(buildTabSpec(TAB_3G, R.string.data_usage_tab_3g));
            mTabHost.addTab(buildTabSpec(TAB_4G, R.string.data_usage_tab_4g));
        } else if (hasMobileRadio(context)) {
            mTabHost.addTab(buildTabSpec(TAB_MOBILE, R.string.data_usage_tab_mobile));
        }
        if (mShowWifi && hasWifiRadio(context)) {
            mTabHost.addTab(buildTabSpec(TAB_WIFI, R.string.data_usage_tab_wifi));
        }
        if (mShowEthernet && hasEthernet(context)) {
            mTabHost.addTab(buildTabSpec(TAB_ETHERNET, R.string.data_usage_tab_ethernet));
        }

        final boolean multipleTabs = mTabWidget.getTabCount() > 1;
        mTabWidget.setVisibility(multipleTabs ? View.VISIBLE : View.GONE);
        if (mIntentTab != null) {
            if (Objects.equal(mIntentTab, mTabHost.getCurrentTabTag())) {
                updateBody();
            } else {
                mTabHost.setCurrentTabByTag(mIntentTab);
            }
            mIntentTab = null;
        } else {
            if (mTabHost.getCurrentTab() == 0) {
                updateBody();
            } else {
                mTabHost.setCurrentTab(0);
            }
        }
    }

    /**
     * Factory that provide empty {@link View} to make {@link TabHost} happy.
     */
    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        /** {@inheritDoc} */
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    /**
     * Build {@link TabSpec} with thin indicator, and empty content.
     */
    private TabSpec buildTabSpec(String tag, int titleRes) {
        final LayoutInflater inflater = LayoutInflater.from(mTabWidget.getContext());
        final View indicator = inflater.inflate(
                R.layout.tab_indicator_thin_holo, mTabWidget, false);
        final TextView title = (TextView) indicator.findViewById(android.R.id.title);
        title.setText(titleRes);
        return mTabHost.newTabSpec(tag).setIndicator(indicator).setContent(mEmptyTabContent);
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        /** {@inheritDoc} */
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

        final Context context = getActivity();
        final String currentTab = mTabHost.getCurrentTabTag();

        if (currentTab == null) {
            Log.w(TAG, "no tab selected; hiding body");
            mListView.setVisibility(View.GONE);
            return;
        } else {
            mListView.setVisibility(View.VISIBLE);
        }

        final boolean tabChanged = !currentTab.equals(mCurrentTab);
        mCurrentTab = currentTab;

        if (LOGD) Log.d(TAG, "updateBody() with currentTab=" + currentTab);

        if (TAB_WIFI.equals(currentTab)) {
            // wifi doesn't have any controls
            mDataEnabledView.setVisibility(View.GONE);
            mDisableAtLimitView.setVisibility(View.GONE);
            mTemplate = buildTemplateWifi();

        } else if (TAB_ETHERNET.equals(currentTab)) {
            // ethernet doesn't have any controls
            mDataEnabledView.setVisibility(View.GONE);
            mDisableAtLimitView.setVisibility(View.GONE);
            mTemplate = buildTemplateEthernet();

        } else {
            mDataEnabledView.setVisibility(View.VISIBLE);
            mDisableAtLimitView.setVisibility(View.VISIBLE);
        }

        if (TAB_MOBILE.equals(currentTab)) {
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_mobile);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_mobile_limit);
            mDataEnabled.setChecked(mConnService.getMobileDataEnabled());
            mTemplate = buildTemplateMobileAll(getActiveSubscriberId(context));

        } else if (TAB_3G.equals(currentTab)) {
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_3g);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_3g_limit);
            // TODO: bind mDataEnabled to 3G radio state
            mTemplate = buildTemplateMobile3gLower(getActiveSubscriberId(context));

        } else if (TAB_4G.equals(currentTab)) {
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_4g);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_4g_limit);
            // TODO: bind mDataEnabled to 4G radio state
            mTemplate = buildTemplateMobile4g(getActiveSubscriberId(context));
        }

        try {
            // load stats for current template
            mHistory = mStatsService.getHistoryForNetwork(mTemplate);
        } catch (RemoteException e) {
            // since we can't do much without policy or history, and we don't
            // want to leave with half-baked UI, we bail hard.
            throw new RuntimeException("problem reading network policy or stats", e);
        }

        // bind chart to historical stats
        mChart.bindNetworkStats(mHistory);

        // only update policy when switching tabs
        updatePolicy(tabChanged);
        updateAppDetail();

        // force scroll to top of body
        mListView.smoothScrollToPosition(0);

        mBinding = false;
    }

    private boolean isAppDetailMode() {
        return mUid != UID_NONE;
    }

    /**
     * Update UID details panels to match {@link #mUid}, showing or hiding them
     * depending on {@link #isAppDetailMode()}.
     */
    private void updateAppDetail() {
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

        final PackageManager pm = getActivity().getPackageManager();
        mAppTitle.setText(pm.getNameForUid(mUid));

        // enable settings button when package provides it
        // TODO: target torwards entire UID instead of just first package
        final String[] packageNames = pm.getPackagesForUid(mUid);
        if (packageNames != null && packageNames.length > 0) {
            mAppSettingsIntent = new Intent(Intent.ACTION_MANAGE_NETWORK_USAGE);
            mAppSettingsIntent.setPackage(packageNames[0]);
            mAppSettingsIntent.addCategory(Intent.CATEGORY_DEFAULT);

            final boolean matchFound = pm.resolveActivity(mAppSettingsIntent, 0) != null;
            mAppSettings.setEnabled(matchFound);

        } else {
            mAppSettingsIntent = null;
            mAppSettings.setEnabled(false);
        }

        try {
            // load stats for current uid and template
            // TODO: read template from extras
            mDetailHistory = mStatsService.getHistoryForUid(mTemplate, mUid, NetworkStats.TAG_NONE);
        } catch (RemoteException e) {
            // since we can't do much without history, and we don't want to
            // leave with half-baked UI, we bail hard.
            throw new RuntimeException("problem reading network stats", e);
        }

        // bind chart to historical stats
        mChart.bindDetailNetworkStats(mDetailHistory);

        updateDetailData();

        final Context context = getActivity();
        if (NetworkPolicyManager.isUidValidForPolicy(context, mUid) && !getRestrictBackground()) {
            mAppRestrictView.setVisibility(View.VISIBLE);
            mAppRestrict.setChecked(getAppRestrictBackground());

        } else {
            mAppRestrictView.setVisibility(View.GONE);
        }

    }

    private void setPolicyCycleDay(int cycleDay) {
        if (LOGD) Log.d(TAG, "setPolicyCycleDay()");
        mPolicyEditor.setPolicyCycleDay(mTemplate, cycleDay);
        updatePolicy(true);
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

    private boolean getDataRoaming() {
        final ContentResolver resolver = getActivity().getContentResolver();
        return Settings.Secure.getInt(resolver, Settings.Secure.DATA_ROAMING, 0) != 0;
    }

    private void setDataRoaming(boolean enabled) {
        // TODO: teach telephony DataConnectionTracker to watch and apply
        // updates when changed.
        final ContentResolver resolver = getActivity().getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.DATA_ROAMING, enabled ? 1 : 0);
        mMenuDataRoaming.setChecked(enabled);
    }

    private boolean getRestrictBackground() {
        try {
            return mPolicyService.getRestrictBackground();
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with policy service: " + e);
            return false;
        }
    }

    private void setRestrictBackground(boolean restrictBackground) {
        if (LOGD) Log.d(TAG, "setRestrictBackground()");
        try {
            mPolicyService.setRestrictBackground(restrictBackground);
            mMenuRestrictBackground.setChecked(restrictBackground);
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with policy service: " + e);
        }
    }

    private boolean getAppRestrictBackground() {
        final int uidPolicy;
        try {
            uidPolicy = mPolicyService.getUidPolicy(mUid);
        } catch (RemoteException e) {
            // since we can't do much without policy, we bail hard.
            throw new RuntimeException("problem reading network policy", e);
        }

        return (uidPolicy & POLICY_REJECT_METERED_BACKGROUND) != 0;
    }

    private void setAppRestrictBackground(boolean restrictBackground) {
        if (LOGD) Log.d(TAG, "setAppRestrictBackground()");
        try {
            mPolicyService.setUidPolicy(
                    mUid, restrictBackground ? POLICY_REJECT_METERED_BACKGROUND : POLICY_NONE);
        } catch (RemoteException e) {
            throw new RuntimeException("unable to save policy", e);
        }

        mAppRestrict.setChecked(restrictBackground);
    }

    /**
     * Update chart sweeps and cycle list to reflect {@link NetworkPolicy} for
     * current {@link #mTemplate}.
     */
    private void updatePolicy(boolean refreshCycle) {
        if (isAppDetailMode()) {
            mNetworkSwitches.setVisibility(View.GONE);
            // we fall through to update cycle list for detail mode
        } else {
            mNetworkSwitches.setVisibility(View.VISIBLE);

            // when heading back to summary without cycle refresh, kick details
            // update to repopulate list.
            if (!refreshCycle) {
                updateDetailData();
            }
        }

        final NetworkPolicy policy = mPolicyEditor.getPolicy(mTemplate);

        // reflect policy limit in checkbox
        mDisableAtLimit.setChecked(policy != null && policy.limitBytes != LIMIT_DISABLED);
        mChart.bindNetworkPolicy(policy);

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
        mCycleAdapter.clear();

        final Context context = mCycleSpinner.getContext();
        long historyStart = mHistory.getStart();
        long historyEnd = mHistory.getEnd();

        if (historyStart == Long.MAX_VALUE || historyEnd == Long.MIN_VALUE) {
            historyStart = System.currentTimeMillis();
            historyEnd = System.currentTimeMillis();
        }

        boolean hasCycles = false;
        if (policy != null) {
            // find the next cycle boundary
            long cycleEnd = computeNextCycleBoundary(historyEnd, policy);

            int guardCount = 0;

            // walk backwards, generating all valid cycle ranges
            while (cycleEnd > historyStart) {
                final long cycleStart = computeLastCycleBoundary(cycleEnd, policy);
                Log.d(TAG, "generating cs=" + cycleStart + " to ce=" + cycleEnd + " waiting for hs="
                        + historyStart);
                mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                cycleEnd = cycleStart;
                hasCycles = true;

                // TODO: remove this guard once we have better testing
                if (guardCount++ > 50) {
                    Log.wtf(TAG, "stuck generating ranges for historyStart=" + historyStart
                            + ", historyEnd=" + historyEnd + " and policy=" + policy);
                }
            }

            // one last cycle entry to modify policy cycle day
            mCycleAdapter.setChangePossible(true);
        }

        if (!hasCycles) {
            // no valid cycles; show all data
            // TODO: offer simple ranges like "last week" etc
            mCycleAdapter.add(new CycleItem(context, historyStart, historyEnd));
            mCycleAdapter.setChangePossible(false);
        }

        // force pick the current cycle (first item)
        mCycleSpinner.setSelection(0);
        mCycleListener.onItemSelected(mCycleSpinner, null, 0, 0);
    }

    private OnCheckedChangeListener mDataEnabledListener = new OnCheckedChangeListener() {
        /** {@inheritDoc} */
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mBinding) return;

            final boolean dataEnabled = isChecked;
            mDataEnabled.setChecked(dataEnabled);

            final String currentTab = mCurrentTab;
            if (TAB_MOBILE.equals(currentTab)) {
                mConnService.setMobileDataEnabled(dataEnabled);
            }
        }
    };

    private View.OnClickListener mDisableAtLimitListener = new View.OnClickListener() {
        /** {@inheritDoc} */
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
        /** {@inheritDoc} */
        public void onClick(View v) {
            final boolean restrictBackground = !mAppRestrict.isChecked();

            if (restrictBackground) {
                // enabling restriction; show confirmation dialog which
                // eventually calls setRestrictBackground() once user confirms.
                ConfirmAppRestrictFragment.show(DataUsageSummary.this);
            } else {
                setAppRestrictBackground(false);
            }
        }
    };

    private OnClickListener mAppSettingsListener = new OnClickListener() {
        /** {@inheritDoc} */
        public void onClick(View v) {
            // TODO: target torwards entire UID instead of just first package
            startActivity(mAppSettingsIntent);
        }
    };

    private OnItemClickListener mListListener = new OnItemClickListener() {
        /** {@inheritDoc} */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AppUsageItem app = (AppUsageItem) parent.getItemAtPosition(position);
            AppDetailsFragment.show(DataUsageSummary.this, app.uid);
        }
    };

    private OnItemSelectedListener mCycleListener = new OnItemSelectedListener() {
        /** {@inheritDoc} */
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

        /** {@inheritDoc} */
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
        final NetworkStatsHistory.Entry entry;

        if (isAppDetailMode()) {
            if (mDetailHistory != null) {
                entry = mDetailHistory.getValues(start, end, now, null);

                mAppSubtitle.setText(
                        getString(R.string.data_usage_received_sent,
                                Formatter.formatFileSize(context, entry.rxBytes),
                                Formatter.formatFileSize(context, entry.txBytes)));
            } else {
                entry = null;
            }

            getLoaderManager().destroyLoader(LOADER_SUMMARY);

        } else {
            entry = mHistory.getValues(start, end, now, null);

            // kick off loader for detailed stats
            getLoaderManager().restartLoader(LOADER_SUMMARY,
                    SummaryForAllUidLoader.buildArgs(mTemplate, start, end), mSummaryForAllUid);

        }

        final long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;
        final String totalPhrase = Formatter.formatFileSize(context, totalBytes);
        final String rangePhrase = formatDateRange(context, start, end, null);

        mUsageSummary.setText(
                getString(R.string.data_usage_total_during_range, totalPhrase, rangePhrase));
    }

    private final LoaderCallbacks<NetworkStats> mSummaryForAllUid = new LoaderCallbacks<
            NetworkStats>() {
        /** {@inheritDoc} */
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(getActivity(), mStatsService, args);
        }

        /** {@inheritDoc} */
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            mAdapter.bindStats(data);
            updateEmptyVisible();
        }

        /** {@inheritDoc} */
        public void onLoaderReset(Loader<NetworkStats> loader) {
            mAdapter.bindStats(null);
            updateEmptyVisible();
        }

        private void updateEmptyVisible() {
            final boolean isEmpty = mAdapter.isEmpty() && !isAppDetailMode();
            mEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    };

    private boolean isMobilePolicySplit() {
        final Context context = getActivity();
        if (hasMobileRadio(context)) {
            final String subscriberId = getActiveSubscriberId(context);
            return mPolicyEditor.isMobilePolicySplit(subscriberId);
        } else {
            return false;
        }
    }

    private void setMobilePolicySplit(boolean split) {
        final String subscriberId = getActiveSubscriberId(getActivity());
        mPolicyEditor.setMobilePolicySplit(subscriberId, split);
    }

    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager telephony = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        return telephony.getSubscriberId();
    }

    private DataUsageChartListener mChartListener = new DataUsageChartListener() {
        /** {@inheritDoc} */
        public void onInspectRangeChanged() {
            if (LOGD) Log.d(TAG, "onInspectRangeChanged()");
            updateDetailData();
        }

        /** {@inheritDoc} */
        public void onWarningChanged() {
            setPolicyWarningBytes(mChart.getWarningBytes());
        }

        /** {@inheritDoc} */
        public void onLimitChanged() {
            setPolicyLimitBytes(mChart.getLimitBytes());
        }
    };


    /**
     * List item that reflects a specific data usage cycle.
     */
    public static class CycleItem {
        public CharSequence label;
        public long start;
        public long end;

        CycleItem(CharSequence label) {
            this.label = label;
        }

        public CycleItem(Context context, long start, long end) {
            this.label = formatDateRange(context, start, end, Time.TIMEZONE_UTC);
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return label.toString();
        }
    }

    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final java.util.Formatter sFormatter = new java.util.Formatter(
            sBuilder, Locale.getDefault());

    private static String formatDateRange(Context context, long start, long end, String timezone) {
        synchronized (sBuilder) {
            int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
            if (Time.getJulianDay(start, 0) == Time.getJulianDay(end, 0)) {
                // when times are on same day, include time detail
                flags |= DateUtils.FORMAT_SHOW_TIME;
            }

            sBuilder.setLength(0);
            return DateUtils
                    .formatDateRange(context, sFormatter, start, end, flags, timezone).toString();
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
            super(context, android.R.layout.simple_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
    }

    private static class AppUsageItem implements Comparable<AppUsageItem> {
        public int uid;
        public long total;

        /** {@inheritDoc} */
        public int compareTo(AppUsageItem another) {
            return Long.compare(another.total, total);
        }
    }

    /**
     * Adapter of applications, sorted by total usage descending.
     */
    public static class DataUsageAdapter extends BaseAdapter {
        private ArrayList<AppUsageItem> mItems = Lists.newArrayList();
        private long mLargest;

        /**
         * Bind the given {@link NetworkStats}, or {@code null} to clear list.
         */
        public void bindStats(NetworkStats stats) {
            mItems.clear();

            if (stats != null) {
                final AppUsageItem systemItem = new AppUsageItem();
                systemItem.uid = android.os.Process.SYSTEM_UID;

                NetworkStats.Entry entry = null;
                for (int i = 0; i < stats.size(); i++) {
                    entry = stats.getValues(i, entry);

                    final boolean isApp = entry.uid >= android.os.Process.FIRST_APPLICATION_UID
                            && entry.uid <= android.os.Process.LAST_APPLICATION_UID;
                    if (isApp || entry.uid == TrafficStats.UID_REMOVED) {
                        final AppUsageItem item = new AppUsageItem();
                        item.uid = entry.uid;
                        item.total = entry.rxBytes + entry.txBytes;
                        mItems.add(item);
                    } else {
                        systemItem.total += entry.rxBytes + entry.txBytes;
                    }
                }

                if (systemItem.total > 0) {
                    mItems.add(systemItem);
                }
            }

            Collections.sort(mItems);
            mLargest = (mItems.size() > 0) ? mItems.get(0).total : 0;
            notifyDataSetChanged();
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
            return mItems.get(position).uid;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.data_usage_item, parent, false);
            }

            final Context context = parent.getContext();

            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            final TextView summary = (TextView) convertView.findViewById(android.R.id.summary);
            final ProgressBar progress = (ProgressBar) convertView.findViewById(
                    android.R.id.progress);

            final AppUsageItem item = mItems.get(position);
            title.setText(resolveLabelForUid(context, item.uid));
            summary.setText(Formatter.formatFileSize(context, item.total));

            final int percentTotal = mLargest != 0 ? (int) (item.total * 100 / mLargest) : 0;
            progress.setProgress(percentTotal);

            return convertView;
        }

    }

    /**
     * Empty {@link Fragment} that controls display of UID details in
     * {@link DataUsageSummary}.
     */
    public static class AppDetailsFragment extends Fragment {
        private static final String EXTRA_UID = "uid";

        public static void show(DataUsageSummary parent, int uid) {
            final Bundle args = new Bundle();
            args.putInt(EXTRA_UID, uid);

            final AppDetailsFragment fragment = new AppDetailsFragment();
            fragment.setArguments(args);
            fragment.setTargetFragment(parent, 0);

            final FragmentTransaction ft = parent.getFragmentManager().beginTransaction();
            ft.add(fragment, TAG_APP_DETAILS);
            ft.addToBackStack(TAG_APP_DETAILS);
            ft.commit();
        }

        @Override
        public void onStart() {
            super.onStart();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mUid = getArguments().getInt(EXTRA_UID);
            target.updateBody();
        }

        @Override
        public void onStop() {
            super.onStop();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mUid = UID_NONE;
            target.updateBody();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link NetworkPolicy#limitBytes}.
     */
    public static class ConfirmLimitFragment extends DialogFragment {
        private static final String EXTRA_MESSAGE_ID = "messageId";
        private static final String EXTRA_LIMIT_BYTES = "limitBytes";

        public static void show(DataUsageSummary parent) {
            final Bundle args = new Bundle();

            // TODO: customize default limits based on network template
            final String currentTab = parent.mCurrentTab;
            if (TAB_3G.equals(currentTab)) {
                args.putInt(EXTRA_MESSAGE_ID, R.string.data_usage_limit_dialog_3g);
                args.putLong(EXTRA_LIMIT_BYTES, 5 * GB_IN_BYTES);
            } else if (TAB_4G.equals(currentTab)) {
                args.putInt(EXTRA_MESSAGE_ID, R.string.data_usage_limit_dialog_4g);
                args.putLong(EXTRA_LIMIT_BYTES, 5 * GB_IN_BYTES);
            } else if (TAB_MOBILE.equals(currentTab)) {
                args.putInt(EXTRA_MESSAGE_ID, R.string.data_usage_limit_dialog_mobile);
                args.putLong(EXTRA_LIMIT_BYTES, 5 * GB_IN_BYTES);
            }

            final ConfirmLimitFragment dialog = new ConfirmLimitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_LIMIT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final int messageId = getArguments().getInt(EXTRA_MESSAGE_ID);
            final long limitBytes = getArguments().getLong(EXTRA_LIMIT_BYTES);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_limit_dialog_title);
            builder.setMessage(messageId);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
        private static final String EXTRA_CYCLE_DAY = "cycleDay";

        public static void show(DataUsageSummary parent) {
            final NetworkPolicy policy = parent.mPolicyEditor.getPolicy(parent.mTemplate);
            final Bundle args = new Bundle();
            args.putInt(CycleEditorFragment.EXTRA_CYCLE_DAY, policy.cycleDay);

            final CycleEditorFragment dialog = new CycleEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CYCLE_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false);
            final NumberPicker cycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);

            final int oldCycleDay = getArguments().getInt(EXTRA_CYCLE_DAY, 1);

            cycleDayPicker.setMinValue(1);
            cycleDayPicker.setMaxValue(31);
            cycleDayPicker.setValue(oldCycleDay);
            cycleDayPicker.setWrapSelectorWheel(true);

            builder.setTitle(R.string.data_usage_cycle_editor_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            final int cycleDay = cycleDayPicker.getValue();
                            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                            if (target != null) {
                                target.setPolicyCycleDay(cycleDay);
                            }
                        }
                    });

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link Settings.Secure#DATA_ROAMING}.
     */
    public static class ConfirmDataRoamingFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            final Bundle args = new Bundle();

            final ConfirmDataRoamingFragment dialog = new ConfirmDataRoamingFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_ROAMING);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.roaming_reenable_title);
            builder.setMessage(R.string.roaming_warning);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        target.setDataRoaming(true);
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
            final Bundle args = new Bundle();

            final ConfirmRestrictFragment dialog = new ConfirmRestrictFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_RESTRICT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_restrict_background_title);
            builder.setMessage(R.string.data_usage_restrict_background);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
     * Dialog to request user confirmation before setting
     * {@link #POLICY_REJECT_METERED_BACKGROUND}.
     */
    public static class ConfirmAppRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
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
        if (template == null) return null;

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
     * Resolve best descriptive label for the given UID.
     */
    public static CharSequence resolveLabelForUid(Context context, int uid) {
        final Resources res = context.getResources();
        final PackageManager pm = context.getPackageManager();

        // handle special case labels
        switch (uid) {
            case android.os.Process.SYSTEM_UID:
                return res.getText(R.string.process_kernel_label);
            case TrafficStats.UID_REMOVED:
                return res.getText(R.string.data_usage_uninstalled_apps);
        }

        // otherwise fall back to using packagemanager labels
        final String[] packageNames = pm.getPackagesForUid(uid);
        final int length = packageNames != null ? packageNames.length : 0;

        CharSequence label = pm.getNameForUid(uid);
        try {
            if (length == 1) {
                final ApplicationInfo info = pm.getApplicationInfo(packageNames[0], 0);
                label = info.loadLabel(pm);
            } else if (length > 1) {
                for (String packageName : packageNames) {
                    final PackageInfo info = pm.getPackageInfo(packageName, 0);
                    if (info.sharedUserLabel != 0) {
                        label = pm.getText(packageName, info.sharedUserLabel, info.applicationInfo);
                        if (!TextUtils.isEmpty(label)) {
                            break;
                        }
                    }
                }
            }
        } catch (NameNotFoundException e) {
        }

        if (TextUtils.isEmpty(label)) {
            label = Integer.toString(uid);
        }
        return label;
    }

    /**
     * Test if device has a mobile data radio.
     */
    private static boolean hasMobileRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("mobile");
        }

        final ConnectivityManager conn = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);

        // mobile devices should have MOBILE network tracker regardless of
        // connection status.
        return conn.getNetworkInfo(TYPE_MOBILE) != null;
    }

    /**
     * Test if device has a mobile 4G data radio.
     */
    private static boolean hasMobile4gRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("4g");
        }

        final ConnectivityManager conn = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        final TelephonyManager telephony = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);

        // WiMAX devices should have WiMAX network tracker regardless of
        // connection status.
        final boolean hasWimax = conn.getNetworkInfo(TYPE_WIMAX) != null;
        final boolean hasLte = telephony.getLteOnCdmaMode() == Phone.LTE_ON_CDMA_TRUE;
        return hasWimax || hasLte;
    }

    /**
     * Test if device has a Wi-Fi data radio.
     */
    private static boolean hasWifiRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("wifi");
        }

        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    /**
     * Test if device has an ethernet network connection.
     */
    private static boolean hasEthernet(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("ethernet");
        }

        final ConnectivityManager conn = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return conn.getNetworkInfo(TYPE_ETHERNET) != null;
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
    private static void setPreferenceSummary(View parent, int resId) {
        final TextView summary = (TextView) parent.findViewById(android.R.id.summary);
        summary.setVisibility(View.VISIBLE);
        summary.setText(resId);
    }
}
