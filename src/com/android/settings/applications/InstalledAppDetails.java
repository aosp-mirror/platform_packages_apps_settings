/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.Manifest.permission;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.icu.text.ListFormatter;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.IWebViewUpdateService;
import android.widget.Button;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.DeviceAdminAdd;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultEmergencyPreferenceController;
import com.android.settings.applications.defaultapps.DefaultHomePreferenceController;
import com.android.settings.applications.defaultapps.DefaultPhonePreferenceController;
import com.android.settings.applications.defaultapps.DefaultSmsPreferenceController;
import com.android.settings.applications.instantapps.InstantAppButtonsController;
import com.android.settings.datausage.AppDataUsage;
import com.android.settings.datausage.DataUsageList;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.development.DevelopmentSettingsEnabler;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.BatteryEntry;
import com.android.settings.fuelgauge.BatteryStatsHelperLoader;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.AppItem;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.PermissionsSummaryHelper;
import com.android.settingslib.applications.PermissionsSummaryHelper.PermissionsResultCallback;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.applications.StorageStatsSource.AppStorageStats;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Activity to display application information from Settings. This activity presents
 * extended information associated with a package like code, data, total size, permissions
 * used by the application and also the set of default launchable activities.
 * For system applications, an option to clear user data is displayed only if data size is > 0.
 * System applications that do not want clear user data do not have this option.
 * For non-system applications, there is no option to clear data. Instead there is an option to
 * uninstall the application.
 */
public class InstalledAppDetails extends AppInfoBase
        implements View.OnClickListener, OnPreferenceClickListener,
        LoaderManager.LoaderCallbacks<AppStorageStats> {

    private static final String LOG_TAG = "InstalledAppDetails";

    // Menu identifiers
    public static final int UNINSTALL_ALL_USERS_MENU = 1;
    public static final int UNINSTALL_UPDATES = 2;

    // Result code identifiers
    public static final int REQUEST_UNINSTALL = 0;
    private static final int REQUEST_REMOVE_DEVICE_ADMIN = 1;

    private static final int SUB_INFO_FRAGMENT = 1;

    private static final int LOADER_CHART_DATA = 2;
    private static final int LOADER_STORAGE = 3;
    @VisibleForTesting
    static final int LOADER_BATTERY = 4;

    private static final int DLG_FORCE_STOP = DLG_BASE + 1;
    private static final int DLG_DISABLE = DLG_BASE + 2;
    private static final int DLG_SPECIAL_DISABLE = DLG_BASE + 3;
    private static final String EXTRA_HIDE_INFO_BUTTON = "hideInfoButton";
    private static final String KEY_HEADER = "header_view";
    private static final String KEY_INSTANT_APP_BUTTONS = "instant_app_buttons";
    private static final String KEY_ACTION_BUTTONS = "action_buttons";
    private static final String KEY_NOTIFICATION = "notification_settings";
    private static final String KEY_STORAGE = "storage_settings";
    private static final String KEY_PERMISSION = "permission_settings";
    private static final String KEY_DATA = "data_settings";
    private static final String KEY_LAUNCH = "preferred_settings";
    private static final String KEY_BATTERY = "battery";
    private static final String KEY_MEMORY = "memory";
    private static final String KEY_VERSION = "app_version";
    private static final String KEY_INSTANT_APP_SUPPORTED_LINKS =
            "instant_app_launch_supported_domain_urls";

    private final HashSet<String> mHomePackages = new HashSet<>();

    private boolean mInitialized;
    private boolean mShowUninstalled;
    private LayoutPreference mHeader;
    private LayoutPreference mActionButtons;
    private Button mUninstallButton;
    private boolean mUpdatedSysApp = false;
    private Button mForceStopButton;
    private Preference mNotificationPreference;
    private Preference mStoragePreference;
    private Preference mPermissionsPreference;
    private Preference mLaunchPreference;
    private Preference mDataPreference;
    private Preference mMemoryPreference;
    private Preference mVersionPreference;
    private AppDomainsPreference mInstantAppDomainsPreference;
    private DevelopmentSettingsEnabler mDevelopmentSettingsEnabler;
    private boolean mDisableAfterUninstall;

    // Used for updating notification preference.
    private final NotificationBackend mBackend = new NotificationBackend();

    private ChartData mChartData;
    private INetworkStatsSession mStatsSession;

    @VisibleForTesting
    Preference mBatteryPreference;
    @VisibleForTesting
    BatterySipper mSipper;
    @VisibleForTesting
    BatteryStatsHelper mBatteryHelper;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    protected ProcStatsData mStatsManager;
    protected ProcStatsPackageEntry mStats;

    private InstantAppButtonsController mInstantAppButtonsController;

    private AppStorageStats mLastResult;
    private String mBatteryPercent;

    @VisibleForTesting
    final LoaderCallbacks<BatteryStatsHelper> mBatteryCallbacks =
            new LoaderCallbacks<BatteryStatsHelper>() {

                @Override
                public Loader<BatteryStatsHelper> onCreateLoader(int id, Bundle args) {
                    return new BatteryStatsHelperLoader(getContext());
                }

                @Override
                public void onLoadFinished(Loader<BatteryStatsHelper> loader,
                        BatteryStatsHelper batteryHelper) {
                    mBatteryHelper = batteryHelper;
                    if (mPackageInfo != null) {
                        mSipper = findTargetSipper(batteryHelper, mPackageInfo.applicationInfo.uid);
                        if (getActivity() != null) {
                            updateBattery();
                        }
                    }
                }

                @Override
                public void onLoaderReset(Loader<BatteryStatsHelper> loader) {
                }
            };

    @VisibleForTesting
    boolean handleDisableable(Button button) {
        boolean disableable = false;
        // Try to prevent the user from bricking their phone
        // by not allowing disabling of apps signed with the
        // system cert and any launcher app in the system.
        if (mHomePackages.contains(mAppEntry.info.packageName)
                || Utils.isSystemPackage(getContext().getResources(), mPm, mPackageInfo)) {
            // Disable button for core system applications.
            button.setText(R.string.disable_text);
        } else if (mAppEntry.info.enabled && !isDisabledUntilUsed()) {
            button.setText(R.string.disable_text);
            disableable = !mApplicationFeatureProvider.getKeepEnabledPackages()
                    .contains(mAppEntry.info.packageName);
        } else {
            button.setText(R.string.enable_text);
            disableable = true;
        }

        return disableable;
    }

    private boolean isDisabledUntilUsed() {
        return mAppEntry.info.enabledSetting
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
    }

    private void initUninstallButtons() {
        final boolean isBundled = (mAppEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean enabled = true;
        if (isBundled) {
            enabled = handleDisableable(mUninstallButton);
        } else {
            enabled = initUnintsallButtonForUserApp();
        }
        // If this is a device admin, it can't be uninstalled or disabled.
        // We do this here so the text of the button is still set correctly.
        if (isBundled && mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            enabled = false;
        }

        // We don't allow uninstalling DO/PO on *any* users, because if it's a system app,
        // "uninstall" is actually "downgrade to the system version + disable", and "downgrade"
        // will clear data on all users.
        if (Utils.isProfileOrDeviceOwner(mUserManager, mDpm, mPackageInfo.packageName)) {
            enabled = false;
        }

        // Don't allow uninstalling the device provisioning package.
        if (Utils.isDeviceProvisioningPackage(getResources(), mAppEntry.info.packageName)) {
            enabled = false;
        }

        // If the uninstall intent is already queued, disable the uninstall button
        if (mDpm.isUninstallInQueue(mPackageName)) {
            enabled = false;
        }

        // Home apps need special handling.  Bundled ones we don't risk downgrading
        // because that can interfere with home-key resolution.  Furthermore, we
        // can't allow uninstallation of the only home app, and we don't want to
        // allow uninstallation of an explicitly preferred one -- the user can go
        // to Home settings and pick a different one, after which we'll permit
        // uninstallation of the now-not-default one.
        if (enabled && mHomePackages.contains(mPackageInfo.packageName)) {
            if (isBundled) {
                enabled = false;
            } else {
                ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
                ComponentName currentDefaultHome  = mPm.getHomeActivities(homeActivities);
                if (currentDefaultHome == null) {
                    // No preferred default, so permit uninstall only when
                    // there is more than one candidate
                    enabled = (mHomePackages.size() > 1);
                } else {
                    // There is an explicit default home app -- forbid uninstall of
                    // that one, but permit it for installed-but-inactive ones.
                    enabled = !mPackageInfo.packageName.equals(currentDefaultHome.getPackageName());
                }
            }
        }

        if (mAppsControlDisallowedBySystem) {
            enabled = false;
        }

        try {
            IWebViewUpdateService webviewUpdateService =
                IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate"));
            if (webviewUpdateService.isFallbackPackage(mAppEntry.info.packageName)) {
                enabled = false;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mUninstallButton.setEnabled(enabled);
        if (enabled) {
            // Register listener
            mUninstallButton.setOnClickListener(this);
        }
    }

    @VisibleForTesting
    boolean initUnintsallButtonForUserApp() {
        boolean enabled = true;
        if ((mPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0
                && mUserManager.getUsers().size() >= 2) {
            // When we have multiple users, there is a separate menu
            // to uninstall for all users.
            enabled = false;
        } else if (AppUtils.isInstant(mPackageInfo.applicationInfo)) {
            enabled = false;
            mUninstallButton.setVisibility(View.GONE);
        }
        mUninstallButton.setText(R.string.uninstall_text);
        return enabled;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();

        if (!ensurePackageInfoAvailable(activity)) {
            return;
        }

        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.installed_app_details);

        addDynamicPrefs();
        if (Utils.isBandwidthControlEnabled()) {
            INetworkStatsService statsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            try {
                mStatsSession = statsService.openSession();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        } else {
            removePreference(KEY_DATA);
        }
        mBatteryUtils = BatteryUtils.getInstance(getContext());
        mDevelopmentSettingsEnabler = new DevelopmentSettingsEnabler(
                activity, null /* lifecycle */);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_INSTALLED_APP_DETAILS;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFinishing) {
            return;
        }
        AppItem app = new AppItem(mAppEntry.info.uid);
        app.addUid(mAppEntry.info.uid);
        if (mStatsSession != null) {
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.restartLoader(LOADER_CHART_DATA,
                    ChartDataLoader.buildArgs(getTemplate(getContext()), app),
                    mDataCallbacks);
            loaderManager.restartLoader(LOADER_STORAGE, Bundle.EMPTY, this);
        }
        restartBatteryStatsLoader();
        if (mDevelopmentSettingsEnabler.getLastEnabledState()) {
            new MemoryUpdater().execute();
        }
        updateDynamicPrefs();
    }

    @VisibleForTesting
    public void restartBatteryStatsLoader() {
        getLoaderManager().restartLoader(LOADER_BATTERY, Bundle.EMPTY, mBatteryCallbacks);
    }

    @Override
    public void onPause() {
        getLoaderManager().destroyLoader(LOADER_CHART_DATA);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        TrafficStats.closeQuietly(mStatsSession);
        super.onDestroy();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mFinishing) {
            return;
        }
        final Activity activity = getActivity();
        mHeader = (LayoutPreference) findPreference(KEY_HEADER);
        mActionButtons = (LayoutPreference) findPreference(KEY_ACTION_BUTTONS);
        EntityHeaderController.newInstance(activity, this, mHeader.findViewById(R.id.entity_header))
                .setRecyclerView(getListView(), getLifecycle())
                .setPackageName(mPackageName)
                .setHasAppInfoLink(false)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_APP_PREFERENCE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .styleActionBar(activity)
                .bindHeaderButtons();
        prepareUninstallAndStop();

        mNotificationPreference = findPreference(KEY_NOTIFICATION);
        mNotificationPreference.setOnPreferenceClickListener(this);
        mStoragePreference = findPreference(KEY_STORAGE);
        mStoragePreference.setOnPreferenceClickListener(this);
        mPermissionsPreference = findPreference(KEY_PERMISSION);
        mPermissionsPreference.setOnPreferenceClickListener(this);
        mDataPreference = findPreference(KEY_DATA);
        if (mDataPreference != null) {
            mDataPreference.setOnPreferenceClickListener(this);
        }
        mBatteryPreference = findPreference(KEY_BATTERY);
        mBatteryPreference.setEnabled(false);
        mBatteryPreference.setOnPreferenceClickListener(this);
        mMemoryPreference = findPreference(KEY_MEMORY);
        mMemoryPreference.setOnPreferenceClickListener(this);
        mMemoryPreference.setVisible(mDevelopmentSettingsEnabler.getLastEnabledState());
        mVersionPreference = findPreference(KEY_VERSION);
        mInstantAppDomainsPreference =
                (AppDomainsPreference) findPreference(KEY_INSTANT_APP_SUPPORTED_LINKS);
        mLaunchPreference = findPreference(KEY_LAUNCH);
        if (mAppEntry != null && mAppEntry.info != null) {
            if ((mAppEntry.info.flags&ApplicationInfo.FLAG_INSTALLED) == 0 ||
                    !mAppEntry.info.enabled) {
                mLaunchPreference.setEnabled(false);
            } else {
                mLaunchPreference.setOnPreferenceClickListener(this);
            }
        } else {
            mLaunchPreference.setEnabled(false);
        }
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        if (!TextUtils.equals(packageName, mPackageName)) {
            Log.d(LOG_TAG, "Package change irrelevant, skipping");
          return;
        }
        refreshUi();
    }

    /**
     * Ensures the {@link PackageInfo} is available to proceed. If it's not available, the fragment
     * will finish.
     *
     * @return true if packageInfo is available.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    boolean ensurePackageInfoAvailable(Activity activity) {
        if (mPackageInfo == null) {
            mFinishing = true;
            Log.w(LOG_TAG, "Package info not available. Is this package already uninstalled?");
            activity.finishAndRemoveTask();
            return false;
        }
        return true;
    }

    private void prepareUninstallAndStop() {
        mForceStopButton = (Button) mActionButtons.findViewById(R.id.right_button);
        mForceStopButton.setText(R.string.force_stop);
        mUninstallButton = (Button) mActionButtons.findViewById(R.id.left_button);
        mForceStopButton.setEnabled(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, UNINSTALL_UPDATES, 0, R.string.app_factory_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, UNINSTALL_ALL_USERS_MENU, 1, R.string.uninstall_all_users_text)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mFinishing) {
            return;
        }
        menu.findItem(UNINSTALL_ALL_USERS_MENU).setVisible(shouldShowUninstallForAll(mAppEntry));
        mUpdatedSysApp = (mAppEntry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        MenuItem uninstallUpdatesItem = menu.findItem(UNINSTALL_UPDATES);
        uninstallUpdatesItem.setVisible(mUpdatedSysApp && !mAppsControlDisallowedBySystem);
        if (uninstallUpdatesItem.isVisible()) {
            RestrictedLockUtils.setMenuItemAsDisabledByAdmin(getActivity(),
                    uninstallUpdatesItem, mAppsControlDisallowedAdmin);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case UNINSTALL_ALL_USERS_MENU:
                uninstallPkg(mAppEntry.info.packageName, true, false);
                return true;
            case UNINSTALL_UPDATES:
                uninstallPkg(mAppEntry.info.packageName, false, false);
                return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_UNINSTALL:
                // Refresh option menu
                getActivity().invalidateOptionsMenu();

                if (mDisableAfterUninstall) {
                    mDisableAfterUninstall = false;
                    new DisableChanger(this, mAppEntry.info,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
                            .execute((Object)null);
                }
                // continue with following operations
            case REQUEST_REMOVE_DEVICE_ADMIN:
                if (!refreshUi()) {
                    setIntentAndFinish(true, true);
                } else {
                    startListeningToPackageRemove();
                }
                break;
        }
    }

    @Override
    public Loader<AppStorageStats> onCreateLoader(int id, Bundle args) {
        Context context = getContext();
        return new FetchPackageStorageAsyncLoader(
                context, new StorageStatsSource(context), mAppEntry.info, UserHandle.of(mUserId));
    }

    @Override
    public void onLoadFinished(Loader<AppStorageStats> loader, AppStorageStats result) {
        mLastResult = result;
        refreshUi();
    }

    @Override
    public void onLoaderReset(Loader<AppStorageStats> loader) {
    }

    /**
     * Utility method to hide and show specific preferences based on whether the app being displayed
     * is an Instant App or an installed app.
     */
    @VisibleForTesting
    void prepareInstantAppPrefs() {
        final boolean isInstant = AppUtils.isInstant(mPackageInfo.applicationInfo);
        if (isInstant) {
            Set<String> handledDomainSet = Utils.getHandledDomains(mPm, mPackageInfo.packageName);
            String[] handledDomains = handledDomainSet.toArray(new String[handledDomainSet.size()]);
            mInstantAppDomainsPreference.setTitles(handledDomains);
            // Dummy values, unused in the implementation
            mInstantAppDomainsPreference.setValues(new int[handledDomains.length]);
            getPreferenceScreen().removePreference(mLaunchPreference);
        } else {
            getPreferenceScreen().removePreference(mInstantAppDomainsPreference);
        }
    }

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        final View appSnippet = mHeader.findViewById(R.id.entity_header);
        mState.ensureIcon(mAppEntry);
        final Activity activity = getActivity();
        final boolean isInstantApp = AppUtils.isInstant(mPackageInfo.applicationInfo);
        final CharSequence summary =
                isInstantApp ? null : getString(Utils.getInstallationStatus(mAppEntry.info));
        EntityHeaderController.newInstance(activity, this, appSnippet)
                .setLabel(mAppEntry)
                .setIcon(mAppEntry)
                .setSummary(summary)
                .setIsInstantApp(isInstantApp)
                .done(activity, false /* rebindActions */);
        mVersionPreference.setSummary(getString(R.string.version_text,
                BidiFormatter.getInstance().unicodeWrap(pkgInfo.versionName)));
    }

    @VisibleForTesting
    boolean shouldShowUninstallForAll(ApplicationsState.AppEntry appEntry) {
        boolean showIt = true;
        if (mUpdatedSysApp) {
            showIt = false;
        } else if (appEntry == null) {
            showIt = false;
        } else if ((appEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            showIt = false;
        } else if (mPackageInfo == null || mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            showIt = false;
        } else if (UserHandle.myUserId() != 0) {
            showIt = false;
        } else if (mUserManager.getUsers().size() < 2) {
            showIt = false;
        } else if (PackageUtil.countPackageInUsers(mPm, mUserManager, mPackageName) < 2
                && (appEntry.info.flags & ApplicationInfo.FLAG_INSTALLED) != 0) {
            showIt = false;
        } else if (AppUtils.isInstant(appEntry.info)) {
            showIt = false;
        }
        return showIt;
    }

    @VisibleForTesting
    BatterySipper findTargetSipper(BatteryStatsHelper batteryHelper, int uid) {
        List<BatterySipper> usageList = batteryHelper.getUsageList();
        for (int i = 0, size = usageList.size(); i < size; i++) {
            BatterySipper sipper = usageList.get(i);
            if (sipper.getUid() == uid) {
                return sipper;
            }
        }

        return null;
    }

    private boolean signaturesMatch(String pkg1, String pkg2) {
        if (pkg1 != null && pkg2 != null) {
            try {
                final int match = mPm.checkSignatures(pkg1, pkg2);
                if (match >= PackageManager.SIGNATURE_MATCH) {
                    return true;
                }
            } catch (Exception e) {
                // e.g. named alternate package not found during lookup;
                // this is an expected case sometimes
            }
        }
        return false;
    }

    @Override
    protected boolean refreshUi() {
        retrieveAppEntry();
        if (mAppEntry == null) {
            return false; // onCreate must have failed, make sure to exit
        }

        if (mPackageInfo == null) {
            return false; // onCreate must have failed, make sure to exit
        }

        // Get list of "home" apps and trace through any meta-data references
        List<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        mPm.getHomeActivities(homeActivities);
        mHomePackages.clear();
        for (int i = 0; i< homeActivities.size(); i++) {
            ResolveInfo ri = homeActivities.get(i);
            final String activityPkg = ri.activityInfo.packageName;
            mHomePackages.add(activityPkg);

            // Also make sure to include anything proxying for the home app
            final Bundle metadata = ri.activityInfo.metaData;
            if (metadata != null) {
                final String metaPkg = metadata.getString(ActivityManager.META_HOME_ALTERNATE);
                if (signaturesMatch(metaPkg, activityPkg)) {
                    mHomePackages.add(metaPkg);
                }
            }
        }

        checkForceStop();
        setAppLabelAndIcon(mPackageInfo);
        initUninstallButtons();
        prepareInstantAppPrefs();

        // Update the preference summaries.
        Activity context = getActivity();
        boolean isExternal = ((mAppEntry.info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0);
        mStoragePreference.setSummary(getStorageSummary(context, mLastResult, isExternal));

        PermissionsSummaryHelper.getPermissionSummary(getContext(),
                mPackageName, mPermissionCallback);
        mLaunchPreference.setSummary(AppUtils.getLaunchByDefaultSummary(mAppEntry, mUsbManager,
                mPm, context));
        mNotificationPreference.setSummary(getNotificationSummary(mAppEntry, context,
                mBackend));
        if (mDataPreference != null) {
            mDataPreference.setSummary(getDataSummary());
        }

        if (!mInitialized) {
            // First time init: are we displaying an uninstalled app?
            mInitialized = true;
            mShowUninstalled = (mAppEntry.info.flags&ApplicationInfo.FLAG_INSTALLED) == 0;
        } else {
            // All other times: if the app no longer exists then we want
            // to go away.
            try {
                ApplicationInfo ainfo = context.getPackageManager().getApplicationInfo(
                        mAppEntry.info.packageName,
                        PackageManager.MATCH_DISABLED_COMPONENTS
                        | PackageManager.MATCH_ANY_USER);
                if (!mShowUninstalled) {
                    // If we did not start out with the app uninstalled, then
                    // it transitioning to the uninstalled state for the current
                    // user means we should go away as well.
                    return (ainfo.flags&ApplicationInfo.FLAG_INSTALLED) != 0;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    @VisibleForTesting
    void updateBattery() {
        mBatteryPreference.setEnabled(true);
        if (isBatteryStatsAvailable()) {
            final int dischargeAmount = mBatteryHelper.getStats().getDischargeAmount(
                    BatteryStats.STATS_SINCE_CHARGED);

            final List<BatterySipper> usageList = new ArrayList<>(mBatteryHelper.getUsageList());
            final double hiddenAmount = mBatteryUtils.removeHiddenBatterySippers(usageList);
            final int percentOfMax = (int) mBatteryUtils.calculateBatteryPercent(
                    mSipper.totalPowerMah, mBatteryHelper.getTotalPower(), hiddenAmount,
                    dischargeAmount);
            mBatteryPercent = Utils.formatPercentage(percentOfMax);
            mBatteryPreference.setSummary(getString(R.string.battery_summary, mBatteryPercent));
        } else {
            mBatteryPreference.setSummary(getString(R.string.no_battery_summary));
        }
    }

    private CharSequence getDataSummary() {
        if (mChartData != null) {
            long totalBytes = mChartData.detail.getTotalBytes();
            if (totalBytes == 0) {
                return getString(R.string.no_data_usage);
            }
            Context context = getActivity();
            return getString(R.string.data_summary_format,
                    Formatter.formatFileSize(context, totalBytes),
                    DateUtils.formatDateTime(context, mChartData.detail.getStart(),
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        }
        return getString(R.string.computing_size);
    }

    @VisibleForTesting
    static CharSequence getStorageSummary(
            Context context, AppStorageStats stats, boolean isExternal) {
        if (stats == null) {
            return context.getText(R.string.computing_size);
        } else {
            CharSequence storageType = context.getString(isExternal
                    ? R.string.storage_type_external
                    : R.string.storage_type_internal);
            return context.getString(R.string.storage_summary_format,
                    getSize(context, stats), storageType.toString().toLowerCase());
        }
    }

    @VisibleForTesting
    boolean isBatteryStatsAvailable() {
        return mBatteryHelper != null && mSipper != null;
    }

    private static CharSequence getSize(Context context, AppStorageStats stats) {
        return Formatter.formatFileSize(context, stats.getTotalBytes());
    }


    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        switch (id) {
            case DLG_DISABLE:
                return new AlertDialog.Builder(getActivity())
                        .setMessage(getActivity().getText(R.string.app_disable_dlg_text))
                        .setPositiveButton(R.string.app_disable_dlg_positive,
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Disable the app
                                mMetricsFeatureProvider.action(getContext(),
                                        MetricsEvent.ACTION_SETTINGS_DISABLE_APP);
                                new DisableChanger(InstalledAppDetails.this, mAppEntry.info,
                                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
                                .execute((Object)null);
                            }
                        })
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
            case DLG_SPECIAL_DISABLE:
                return new AlertDialog.Builder(getActivity())
                        .setMessage(getActivity().getText(R.string.app_disable_dlg_text))
                        .setPositiveButton(R.string.app_disable_dlg_positive,
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Disable the app and ask for uninstall
                                mMetricsFeatureProvider.action(getContext(),
                                        MetricsEvent.ACTION_SETTINGS_DISABLE_APP);
                                uninstallPkg(mAppEntry.info.packageName,
                                        false, true);
                            }
                        })
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
            case DLG_FORCE_STOP:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getText(R.string.force_stop_dlg_title))
                        .setMessage(getActivity().getText(R.string.force_stop_dlg_text))
                        .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Force stop
                                forceStopPackage(mAppEntry.info.packageName);
                            }
                        })
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
        }
        if (mInstantAppButtonsController != null) {
            return mInstantAppButtonsController.createDialog(id);
        }
        return null;
    }

    private void uninstallPkg(String packageName, boolean allUsers, boolean andDisable) {
        stopListeningToPackageRemove();
         // Create new intent to launch Uninstaller activity
        Uri packageURI = Uri.parse("package:"+packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, allUsers);
        mMetricsFeatureProvider.action(
                getContext(), MetricsEvent.ACTION_SETTINGS_UNINSTALL_APP);
        startActivityForResult(uninstallIntent, REQUEST_UNINSTALL);
        mDisableAfterUninstall = andDisable;
    }

    private void forceStopPackage(String pkgName) {
        mMetricsFeatureProvider.action(getContext(), MetricsEvent.ACTION_APP_FORCE_STOP, pkgName);
        ActivityManager am = (ActivityManager) getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        Log.d(LOG_TAG, "Stopping package " + pkgName);
        am.forceStopPackage(pkgName);
        int userId = UserHandle.getUserId(mAppEntry.info.uid);
        mState.invalidatePackage(pkgName, userId);
        ApplicationsState.AppEntry newEnt = mState.getEntry(pkgName, userId);
        if (newEnt != null) {
            mAppEntry = newEnt;
        }
        checkForceStop();
    }

    private void updateForceStopButton(boolean enabled) {
        if (mAppsControlDisallowedBySystem) {
            mForceStopButton.setEnabled(false);
        } else {
            mForceStopButton.setEnabled(enabled);
            mForceStopButton.setOnClickListener(this);
        }
    }

    @VisibleForTesting
    void checkForceStop() {
        if (mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            // User can't force stop device admin.
            Log.w(LOG_TAG, "User can't force stop device admin");
            updateForceStopButton(false);
        } else if (AppUtils.isInstant(mPackageInfo.applicationInfo)) {
            updateForceStopButton(false);
            mForceStopButton.setVisibility(View.GONE);
        } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_STOPPED) == 0) {
            // If the app isn't explicitly stopped, then always show the
            // force stop button.
            Log.w(LOG_TAG, "App is not explicitly stopped");
            updateForceStopButton(true);
        } else {
            Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                    Uri.fromParts("package", mAppEntry.info.packageName, null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[] { mAppEntry.info.packageName });
            intent.putExtra(Intent.EXTRA_UID, mAppEntry.info.uid);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mAppEntry.info.uid));
            Log.d(LOG_TAG, "Sending broadcast to query restart status for "
                    + mAppEntry.info.packageName);
            getActivity().sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null,
                    mCheckKillProcessesReceiver, null, Activity.RESULT_CANCELED, null, null);
        }
    }

    private void startManagePermissionsActivity() {
        // start new activity to manage app permissions
        Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mAppEntry.info.packageName);
        intent.putExtra(EXTRA_HIDE_INFO_BUTTON, true);
        try {
            getActivity().startActivityForResult(intent, SUB_INFO_FRAGMENT);
        } catch (ActivityNotFoundException e) {
            Log.w(LOG_TAG, "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }

    private void startAppInfoFragment(Class<?> fragment, CharSequence title) {
        startAppInfoFragment(fragment, title, this, mAppEntry);
    }

    public static void startAppInfoFragment(Class<?> fragment, CharSequence title,
            SettingsPreferenceFragment caller, AppEntry appEntry) {
        // start new fragment to display extended information
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, appEntry.info.packageName);
        args.putInt(ARG_PACKAGE_UID, appEntry.info.uid);

        SettingsActivity sa = (SettingsActivity) caller.getActivity();
        sa.startPreferencePanel(caller, fragment.getName(), args, -1, title, caller,
                SUB_INFO_FRAGMENT);
    }

    /*
     * Method implementing functionality of buttons clicked
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View v) {
        if (mAppEntry == null) {
            setIntentAndFinish(true, true);
            return;
        }
        String packageName = mAppEntry.info.packageName;
        if (v == mUninstallButton) {
            if (mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
                stopListeningToPackageRemove();
                Activity activity = getActivity();
                Intent uninstallDAIntent = new Intent(activity, DeviceAdminAdd.class);
                uninstallDAIntent.putExtra(DeviceAdminAdd.EXTRA_DEVICE_ADMIN_PACKAGE_NAME,
                        mPackageName);
                mMetricsFeatureProvider.action(
                        activity, MetricsEvent.ACTION_SETTINGS_UNINSTALL_DEVICE_ADMIN);
                activity.startActivityForResult(uninstallDAIntent, REQUEST_REMOVE_DEVICE_ADMIN);
                return;
            }
            EnforcedAdmin admin = RestrictedLockUtils.checkIfUninstallBlocked(getActivity(),
                    packageName, mUserId);
            boolean uninstallBlockedBySystem = mAppsControlDisallowedBySystem ||
                    RestrictedLockUtils.hasBaseUserRestriction(getActivity(), packageName, mUserId);
            if (admin != null && !uninstallBlockedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), admin);
            } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                if (mAppEntry.info.enabled && !isDisabledUntilUsed()) {
                    // If the system app has an update and this is the only user on the device,
                    // then offer to downgrade the app, otherwise only offer to disable the
                    // app for this user.
                    if (mUpdatedSysApp && isSingleUser()) {
                        showDialogInner(DLG_SPECIAL_DISABLE, 0);
                    } else {
                        showDialogInner(DLG_DISABLE, 0);
                    }
                } else {
                    mMetricsFeatureProvider.action(
                            getActivity(),
                            mAppEntry.info.enabled
                                    ? MetricsEvent.ACTION_SETTINGS_DISABLE_APP
                                    : MetricsEvent.ACTION_SETTINGS_ENABLE_APP);
                    new DisableChanger(this, mAppEntry.info,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                                    .execute((Object) null);
                }
            } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
                uninstallPkg(packageName, true, false);
            } else {
                uninstallPkg(packageName, false, false);
            }
        } else if (v == mForceStopButton) {
            if (mAppsControlDisallowedAdmin != null && !mAppsControlDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                        getActivity(), mAppsControlDisallowedAdmin);
            } else {
                showDialogInner(DLG_FORCE_STOP, 0);
                //forceStopPackage(mAppInfo.packageName);
            }
        }
    }

    /** Returns whether there is only one user on this device, not including the system-only user */
    private boolean isSingleUser() {
        final int userCount = mUserManager.getUserCount();
        return userCount == 1
                || (mUserManager.isSplitSystemUser() && userCount == 2);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mStoragePreference) {
            startAppInfoFragment(AppStorageSettings.class, mStoragePreference.getTitle());
        } else if (preference == mNotificationPreference) {
            startAppInfoFragment(AppNotificationSettings.class,
                    getString(R.string.app_notifications_title));
        } else if (preference == mPermissionsPreference) {
            startManagePermissionsActivity();
        } else if (preference == mLaunchPreference) {
            startAppInfoFragment(AppLaunchSettings.class, mLaunchPreference.getTitle());
        } else if (preference == mMemoryPreference) {
            ProcessStatsBase.launchMemoryDetail((SettingsActivity) getActivity(),
                    mStatsManager.getMemInfo(), mStats, false);
        } else if (preference == mDataPreference) {
            startAppInfoFragment(AppDataUsage.class, getString(R.string.app_data_usage));
        } else if (preference == mBatteryPreference) {
            if (isBatteryStatsAvailable()) {
                BatteryEntry entry = new BatteryEntry(getContext(), null, mUserManager, mSipper);
                entry.defaultPackageName = mPackageName;
                AdvancedPowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(),
                        this, mBatteryHelper, BatteryStats.STATS_SINCE_CHARGED, entry,
                        mBatteryPercent, null /* mAnomalies */);
            } else {
                AdvancedPowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(),
                        this, mPackageName);
            }
        } else {
            return false;
        }
        return true;
    }

    private void addDynamicPrefs() {
        if (UserManager.get(getContext()).isManagedProfile()) {
            return;
        }
        final PreferenceScreen screen = getPreferenceScreen();
        final Context context = getContext();
        if (DefaultHomePreferenceController.hasHomePreference(mPackageName, context)) {
            screen.addPreference(new ShortcutPreference(getPrefContext(),
                    DefaultAppSettings.class, "default_home", R.string.home_app,
                    R.string.configure_apps));
        }
        if (DefaultBrowserPreferenceController.hasBrowserPreference(mPackageName, context)) {
            screen.addPreference(new ShortcutPreference(getPrefContext(),
                    DefaultAppSettings.class, "default_browser", R.string.default_browser_title,
                    R.string.configure_apps));
        }
        if (DefaultPhonePreferenceController.hasPhonePreference(mPackageName, context)) {
            screen.addPreference(new ShortcutPreference(getPrefContext(),
                    DefaultAppSettings.class, "default_phone_app", R.string.default_phone_title,
                    R.string.configure_apps));
        }
        if (DefaultEmergencyPreferenceController.hasEmergencyPreference(mPackageName, context)) {
            screen.addPreference(new ShortcutPreference(getPrefContext(),
                    DefaultAppSettings.class, "default_emergency_app",
                    R.string.default_emergency_app, R.string.configure_apps));
        }
        if (DefaultSmsPreferenceController.hasSmsPreference(mPackageName, context)) {
            screen.addPreference(new ShortcutPreference(getPrefContext(),
                    DefaultAppSettings.class, "default_sms_app", R.string.sms_application_title,
                    R.string.configure_apps));
        }

        // Get the package info with the activities
        PackageInfo packageInfoWithActivities = null;
        try {
            packageInfoWithActivities = mPm.getPackageInfoAsUser(mPackageName,
                    PackageManager.GET_ACTIVITIES, UserHandle.myUserId());
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception while retrieving the package info of " + mPackageName, e);
        }

        boolean hasDrawOverOtherApps = hasPermission(permission.SYSTEM_ALERT_WINDOW);
        boolean hasWriteSettings = hasPermission(permission.WRITE_SETTINGS);
        boolean hasPictureInPictureActivities = (packageInfoWithActivities != null) &&
                PictureInPictureSettings.checkPackageHasPictureInPictureActivities(
                        packageInfoWithActivities.packageName,
                        packageInfoWithActivities.activities);
        boolean isPotentialAppSource = isPotentialAppSource();
        if (hasDrawOverOtherApps || hasWriteSettings || hasPictureInPictureActivities ||
                isPotentialAppSource) {
            PreferenceCategory category = new PreferenceCategory(getPrefContext());
            category.setTitle(R.string.advanced_apps);
            screen.addPreference(category);

            if (hasDrawOverOtherApps) {
                Preference pref = new Preference(getPrefContext());
                pref.setTitle(R.string.draw_overlay);
                pref.setKey("system_alert_window");
                pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startAppInfoFragment(DrawOverlayDetails.class,
                                getString(R.string.draw_overlay));
                        return true;
                    }
                });
                category.addPreference(pref);
            }
            if (hasWriteSettings) {
                Preference pref = new Preference(getPrefContext());
                pref.setTitle(R.string.write_settings);
                pref.setKey("write_settings_apps");
                pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startAppInfoFragment(WriteSettingsDetails.class,
                                getString(R.string.write_settings));
                        return true;
                    }
                });
                category.addPreference(pref);
            }
            if (hasPictureInPictureActivities) {
                Preference pref = new Preference(getPrefContext());
                pref.setTitle(R.string.picture_in_picture_app_detail_title);
                pref.setKey("picture_in_picture");
                pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AppInfoBase.startAppInfoFragment(PictureInPictureDetails.class,
                                R.string.picture_in_picture_app_detail_title, mPackageName,
                                mPackageInfo.applicationInfo.uid, InstalledAppDetails.this,
                                -1, getMetricsCategory());
                        return true;
                    }
                });
                category.addPreference(pref);
            }
            if (isPotentialAppSource) {
                Preference pref = new Preference(getPrefContext());
                pref.setTitle(R.string.install_other_apps);
                pref.setKey("install_other_apps");
                pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startAppInfoFragment(ExternalSourcesDetails.class,
                                getString(R.string.install_other_apps));
                        return true;
                    }
                });
                category.addPreference(pref);
            }
        }

        addAppInstallerInfoPref(screen);
        maybeAddInstantAppButtons();
    }

    private boolean isPotentialAppSource() {
        AppStateInstallAppsBridge.InstallAppsState appState =
                new AppStateInstallAppsBridge(getContext(), null, null)
                        .createInstallAppsStateFor(mPackageName, mPackageInfo.applicationInfo.uid);
        return appState.isPotentialAppSource();
    }

    private void addAppInstallerInfoPref(PreferenceScreen screen) {
        String installerPackageName =
                AppStoreUtil.getInstallerPackageName(getContext(), mPackageName);

        final CharSequence installerLabel = Utils.getApplicationLabel(getContext(),
                installerPackageName);
        if (installerLabel == null) {
            return;
        }
        final int detailsStringId = AppUtils.isInstant(mPackageInfo.applicationInfo)
                ? R.string.instant_app_details_summary
                : R.string.app_install_details_summary;
        PreferenceCategory category = new PreferenceCategory(getPrefContext());
        category.setTitle(R.string.app_install_details_group_title);
        screen.addPreference(category);
        Preference pref = new Preference(getPrefContext());
        pref.setTitle(R.string.app_install_details_title);
        pref.setKey("app_info_store");
        pref.setSummary(getString(detailsStringId, installerLabel));

        Intent intent =
                AppStoreUtil.getAppStoreLink(getContext(), installerPackageName, mPackageName);
        if (intent != null) {
            pref.setIntent(intent);
        } else {
            pref.setEnabled(false);
        }
        category.addPreference(pref);
    }

    @VisibleForTesting
    void maybeAddInstantAppButtons() {
        if (AppUtils.isInstant(mPackageInfo.applicationInfo)) {
            LayoutPreference buttons = (LayoutPreference) findPreference(KEY_INSTANT_APP_BUTTONS);
            mInstantAppButtonsController = mApplicationFeatureProvider
                    .newInstantAppButtonsController(this,
                            buttons.findViewById(R.id.instant_app_button_container),
                            id -> showDialogInner(id, 0))
                    .setPackageName(mPackageName)
                    .show();
        }
    }

    private boolean hasPermission(String permission) {
        if (mPackageInfo == null || mPackageInfo.requestedPermissions == null) {
            return false;
        }
        for (int i = 0; i < mPackageInfo.requestedPermissions.length; i++) {
            if (mPackageInfo.requestedPermissions[i].equals(permission)) {
                return true;
            }
        }
        return false;
    }

    private void updateDynamicPrefs() {
        final Context context = getContext();
        Preference pref = findPreference("default_home");

        if (pref != null) {
            pref.setSummary(DefaultHomePreferenceController.isHomeDefault(mPackageName,
                    new PackageManagerWrapperImpl(context.getPackageManager()))
                    ? R.string.yes : R.string.no);
        }
        pref = findPreference("default_browser");
        if (pref != null) {
            pref.setSummary(new DefaultBrowserPreferenceController(context)
                    .isBrowserDefault(mPackageName, mUserId)
                    ? R.string.yes : R.string.no);
        }
        pref = findPreference("default_phone_app");
        if (pref != null) {
            pref.setSummary(
                    DefaultPhonePreferenceController.isPhoneDefault(mPackageName, context)
                    ? R.string.yes : R.string.no);
        }
        pref = findPreference("default_emergency_app");
        if (pref != null) {
            pref.setSummary(DefaultEmergencyPreferenceController.isEmergencyDefault(mPackageName,
                    getContext()) ? R.string.yes : R.string.no);
        }
        pref = findPreference("default_sms_app");
        if (pref != null) {
            pref.setSummary(DefaultSmsPreferenceController.isSmsDefault(mPackageName, context)
                    ? R.string.yes : R.string.no);
        }
        pref = findPreference("system_alert_window");
        if (pref != null) {
            pref.setSummary(DrawOverlayDetails.getSummary(getContext(), mAppEntry));
        }
        pref = findPreference("picture_in_picture");
        if (pref != null) {
            pref.setSummary(PictureInPictureDetails.getPreferenceSummary(getContext(),
                    mPackageInfo.applicationInfo.uid, mPackageName));
        }
        pref = findPreference("write_settings_apps");
        if (pref != null) {
            pref.setSummary(WriteSettingsDetails.getSummary(getContext(), mAppEntry));
        }
        pref = findPreference("install_other_apps");
        if (pref != null) {
            pref.setSummary(ExternalSourcesDetails.getPreferenceSummary(getContext(), mAppEntry));
        }
    }

    public static NetworkTemplate getTemplate(Context context) {
        if (DataUsageList.hasReadyMobileRadio(context)) {
            return NetworkTemplate.buildTemplateMobileWildcard();
        }
        if (DataUsageUtils.hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        }
        return NetworkTemplate.buildTemplateEthernet();
    }

    public static CharSequence getNotificationSummary(AppEntry appEntry, Context context,
            NotificationBackend backend) {
        AppRow appRow = backend.loadAppRow(context, context.getPackageManager(), appEntry.info);
        return getNotificationSummary(appRow, context);
    }

    public static CharSequence getNotificationSummary(AppRow appRow, Context context) {
        // TODO: implement summary when it is known what it should say
        return "";
    }

    @Override
    protected void onPackageRemoved() {
        getActivity().finishActivity(SUB_INFO_FRAGMENT);
        super.onPackageRemoved();
    }

    private class MemoryUpdater extends AsyncTask<Void, Void, ProcStatsPackageEntry> {

        @Override
        protected ProcStatsPackageEntry doInBackground(Void... params) {
            if (getActivity() == null) {
                return null;
            }
            if (mPackageInfo == null) {
                return null;
            }
            if (mStatsManager == null) {
                mStatsManager = new ProcStatsData(getActivity(), false);
                mStatsManager.setDuration(ProcessStatsBase.sDurations[0]);
            }
            mStatsManager.refreshStats(true);
            for (ProcStatsPackageEntry pkgEntry : mStatsManager.getEntries()) {
                for (ProcStatsEntry entry : pkgEntry.mEntries) {
                    if (entry.mUid == mPackageInfo.applicationInfo.uid) {
                        pkgEntry.updateMetrics();
                        return pkgEntry;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ProcStatsPackageEntry entry) {
            if (getActivity() == null) {
                return;
            }
            if (entry != null) {
                mStats = entry;
                mMemoryPreference.setEnabled(true);
                double amount = Math.max(entry.mRunWeight, entry.mBgWeight)
                        * mStatsManager.getMemInfo().weightToRam;
                mMemoryPreference.setSummary(getString(R.string.memory_use_summary,
                        Formatter.formatShortFileSize(getContext(), (long) amount)));
            } else {
                mMemoryPreference.setEnabled(false);
                mMemoryPreference.setSummary(getString(R.string.no_memory_use_summary));
            }
        }

    }

    /**
     * Elicit this class for testing. Test cannot be done in robolectric because it
     * invokes the new API.
     */
    @VisibleForTesting
    public static class PackageUtil {
        /**
         * Count how many users in device have installed package {@paramref packageName}
         */
        public static int countPackageInUsers(PackageManager packageManager, UserManager
                userManager, String packageName) {
            final List<UserInfo> userInfos = userManager.getUsers(true);
            int count = 0;

            for (final UserInfo userInfo : userInfos) {
                try {
                    // Use this API to check whether user has this package
                    final ApplicationInfo info = packageManager.getApplicationInfoAsUser(
                            packageName, PackageManager.GET_META_DATA, userInfo.id);
                    if ((info.flags & ApplicationInfo.FLAG_INSTALLED) != 0) {
                        count++;
                    }
                } catch(NameNotFoundException e) {
                    Log.e(TAG, "Package: " + packageName + " not found for user: " + userInfo.id);
                }
            }

            return count;
        }
    }

    private static class DisableChanger extends AsyncTask<Object, Object, Object> {
        final PackageManager mPm;
        final WeakReference<InstalledAppDetails> mActivity;
        final ApplicationInfo mInfo;
        final int mState;

        DisableChanger(InstalledAppDetails activity, ApplicationInfo info, int state) {
            mPm = activity.mPm;
            mActivity = new WeakReference<InstalledAppDetails>(activity);
            mInfo = info;
            mState = state;
        }

        @Override
        protected Object doInBackground(Object... params) {
            mPm.setApplicationEnabledSetting(mInfo.packageName, mState, 0);
            return null;
        }
    }

    private final LoaderCallbacks<ChartData> mDataCallbacks = new LoaderCallbacks<ChartData>() {

        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(getActivity(), mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            mChartData = data;
            mDataPreference.setSummary(getDataSummary());
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
            // Leave last result.
        }
    };

    private final BroadcastReceiver mCheckKillProcessesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean enabled = getResultCode() != Activity.RESULT_CANCELED;
            Log.d(LOG_TAG, "Got broadcast response: Restart status for "
                    + mAppEntry.info.packageName + " " + enabled);
            updateForceStopButton(enabled);
        }
    };

    private final PermissionsResultCallback mPermissionCallback
            = new PermissionsResultCallback() {
        @Override
        public void onPermissionSummaryResult(int standardGrantedPermissionCount,
                int requestedPermissionCount, int additionalGrantedPermissionCount,
                List<CharSequence> grantedGroupLabels) {
            if (getActivity() == null) {
                return;
            }
            final Resources res = getResources();
            CharSequence summary = null;

            if (requestedPermissionCount == 0) {
                summary = res.getString(
                        R.string.runtime_permissions_summary_no_permissions_requested);
                mPermissionsPreference.setOnPreferenceClickListener(null);
                mPermissionsPreference.setEnabled(false);
            } else {
                final ArrayList<CharSequence> list = new ArrayList<>(grantedGroupLabels);
                if (additionalGrantedPermissionCount > 0) {
                    // N additional permissions.
                    list.add(res.getQuantityString(
                            R.plurals.runtime_permissions_additional_count,
                            additionalGrantedPermissionCount, additionalGrantedPermissionCount));
                }
                if (list.size() == 0) {
                    summary = res.getString(
                            R.string.runtime_permissions_summary_no_permissions_granted);
                } else {
                    summary = ListFormatter.getInstance().format(list);
                }
                mPermissionsPreference.setOnPreferenceClickListener(InstalledAppDetails.this);
                mPermissionsPreference.setEnabled(true);
            }
            mPermissionsPreference.setSummary(summary);
        }
    };
}
