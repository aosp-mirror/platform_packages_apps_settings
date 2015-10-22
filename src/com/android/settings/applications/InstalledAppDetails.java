/**
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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
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
import android.graphics.drawable.Drawable;
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
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.DataUsageSummary;
import com.android.settings.DataUsageSummary.AppItem;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.PermissionsSummaryHelper.PermissionsResultCallback;
import com.android.settings.fuelgauge.BatteryEntry;
import com.android.settings.fuelgauge.PowerUsageDetail;
import com.android.settings.net.ChartData;
import com.android.settings.net.ChartDataLoader;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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
        implements View.OnClickListener, OnPreferenceClickListener {

    private static final String LOG_TAG = "InstalledAppDetails";

    // Menu identifiers
    public static final int UNINSTALL_ALL_USERS_MENU = 1;
    public static final int UNINSTALL_UPDATES = 2;

    // Result code identifiers
    public static final int REQUEST_UNINSTALL = 0;
    private static final int SUB_INFO_FRAGMENT = 1;

    private static final int LOADER_CHART_DATA = 2;

    private static final int DLG_FORCE_STOP = DLG_BASE + 1;
    private static final int DLG_DISABLE = DLG_BASE + 2;
    private static final int DLG_SPECIAL_DISABLE = DLG_BASE + 3;
    private static final int DLG_FACTORY_RESET = DLG_BASE + 4;

    private static final String KEY_HEADER = "header_view";
    private static final String KEY_NOTIFICATION = "notification_settings";
    private static final String KEY_STORAGE = "storage_settings";
    private static final String KEY_PERMISSION = "permission_settings";
    private static final String KEY_DATA = "data_settings";
    private static final String KEY_LAUNCH = "preferred_settings";
    private static final String KEY_BATTERY = "battery";
    private static final String KEY_MEMORY = "memory";

    private final HashSet<String> mHomePackages = new HashSet<String>();

    private boolean mInitialized;
    private boolean mShowUninstalled;
    private LayoutPreference mHeader;
    private Button mUninstallButton;
    private boolean mUpdatedSysApp = false;
    private Button mForceStopButton;
    private Preference mNotificationPreference;
    private Preference mStoragePreference;
    private Preference mPermissionsPreference;
    private Preference mLaunchPreference;
    private Preference mDataPreference;
    private Preference mMemoryPreference;

    private boolean mDisableAfterUninstall;
    // Used for updating notification preference.
    private final NotificationBackend mBackend = new NotificationBackend();

    private ChartData mChartData;
    private INetworkStatsSession mStatsSession;

    private Preference mBatteryPreference;

    private BatteryStatsHelper mBatteryHelper;
    private BatterySipper mSipper;

    protected ProcStatsData mStatsManager;
    protected ProcStatsPackageEntry mStats;

    private BroadcastReceiver mPermissionReceiver;

    private boolean handleDisableable(Button button) {
        boolean disableable = false;
        // Try to prevent the user from bricking their phone
        // by not allowing disabling of apps signed with the
        // system cert and any launcher app in the system.
        if (mHomePackages.contains(mAppEntry.info.packageName)
                || Utils.isSystemPackage(mPm, mPackageInfo)) {
            // Disable button for core system applications.
            button.setText(R.string.disable_text);
        } else if (mAppEntry.info.enabled && !isDisabledUntilUsed()) {
            button.setText(R.string.disable_text);
            disableable = true;
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
            if ((mPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0
                    && mUserManager.getUsers().size() >= 2) {
                // When we have multiple users, there is a separate menu
                // to uninstall for all users.
                enabled = false;
            }
            mUninstallButton.setText(R.string.uninstall_text);
        }
        // If this is a device admin, it can't be uninstalled or disabled.
        // We do this here so the text of the button is still set correctly.
        if (mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            enabled = false;
        }

        if (isProfileOrDeviceOwner(mPackageInfo.packageName)) {
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

        if (mAppControlRestricted) {
            enabled = false;
        }

        mUninstallButton.setEnabled(enabled);
        if (enabled) {
            // Register listener
            mUninstallButton.setOnClickListener(this);
        }
    }

    /** Returns if the supplied package is device owner or profile owner of at least one user */
    private boolean isProfileOrDeviceOwner(String packageName) {
        List<UserInfo> userInfos = mUserManager.getUsers();
        DevicePolicyManager dpm = (DevicePolicyManager)
                getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (packageName.equals(dpm.getDeviceOwner())) {
            return true;
        }
        for (UserInfo userInfo : userInfos) {
            ComponentName cn = dpm.getProfileOwnerAsUser(userInfo.id);
            if (cn != null && cn.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.installed_app_details);

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
        mBatteryHelper = new BatteryStatsHelper(getActivity(), true);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_INSTALLED_APP_DETAILS;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFinishing) {
            return;
        }
        mState.requestSize(mPackageName, mUserId);
        AppItem app = new AppItem(mAppEntry.info.uid);
        app.addUid(mAppEntry.info.uid);
        if (mStatsSession != null) {
            getLoaderManager().restartLoader(LOADER_CHART_DATA,
                    ChartDataLoader.buildArgs(getTemplate(getContext()), app),
                    mDataCallbacks);
        }
        new BatteryUpdater().execute();
        new MemoryUpdater().execute();
    }

    @Override
    public void onPause() {
        getLoaderManager().destroyLoader(LOADER_CHART_DATA);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        TrafficStats.closeQuietly(mStatsSession);
        if (mPermissionReceiver != null) {
            getContext().unregisterReceiver(mPermissionReceiver);
            mPermissionReceiver = null;
        }

        super.onDestroy();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mFinishing) {
            return;
        }
        handleHeader();

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

    private void handleHeader() {
        mHeader = (LayoutPreference) findPreference(KEY_HEADER);

        // Get Control button panel
        View btnPanel = mHeader.findViewById(R.id.control_buttons_panel);
        mForceStopButton = (Button) btnPanel.findViewById(R.id.right_button);
        mForceStopButton.setText(R.string.force_stop);
        mUninstallButton = (Button) btnPanel.findViewById(R.id.left_button);
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
        boolean showIt = true;
        if (mUpdatedSysApp) {
            showIt = false;
        } else if (mAppEntry == null) {
            showIt = false;
        } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            showIt = false;
        } else if (mPackageInfo == null || mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            showIt = false;
        } else if (UserHandle.myUserId() != 0) {
            showIt = false;
        } else if (mUserManager.getUsers().size() < 2) {
            showIt = false;
        }
        menu.findItem(UNINSTALL_ALL_USERS_MENU).setVisible(showIt);
        mUpdatedSysApp = (mAppEntry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        menu.findItem(UNINSTALL_UPDATES).setVisible(mUpdatedSysApp && !mAppControlRestricted);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case UNINSTALL_ALL_USERS_MENU:
                uninstallPkg(mAppEntry.info.packageName, true, false);
                return true;
            case UNINSTALL_UPDATES:
                showDialogInner(DLG_FACTORY_RESET, 0);
                return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UNINSTALL) {
            if (mDisableAfterUninstall) {
                mDisableAfterUninstall = false;
                try {
                    ApplicationInfo ainfo = getActivity().getPackageManager().getApplicationInfo(
                            mAppEntry.info.packageName, PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_DISABLED_COMPONENTS);
                    if ((ainfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                        new DisableChanger(this, mAppEntry.info,
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
                                .execute((Object)null);
                    }
                } catch (NameNotFoundException e) {
                }
            }
            if (!refreshUi()) {
                setIntentAndFinish(true, true);
            }
        }
    }

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        final View appSnippet = mHeader.findViewById(R.id.app_snippet);
        mState.ensureIcon(mAppEntry);
        setupAppSnippet(appSnippet, mAppEntry.label, mAppEntry.icon,
                pkgInfo != null ? pkgInfo.versionName : null);
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

        // Update the preference summaries.
        Activity context = getActivity();
        mStoragePreference.setSummary(AppStorageSettings.getSummary(mAppEntry, context));
        if (mPermissionReceiver != null) {
            getContext().unregisterReceiver(mPermissionReceiver);
        }
        mPermissionReceiver = PermissionsSummaryHelper.getPermissionSummary(getContext(),
                mPackageName, mPermissionCallback);
        mLaunchPreference.setSummary(Utils.getLaunchByDeafaultSummary(mAppEntry, mUsbManager,
                mPm, context));
        mNotificationPreference.setSummary(getNotificationSummary(mAppEntry, context,
                mBackend));
        if (mDataPreference != null) {
            mDataPreference.setSummary(getDataSummary());
        }

        updateBattery();

        if (!mInitialized) {
            // First time init: are we displaying an uninstalled app?
            mInitialized = true;
            mShowUninstalled = (mAppEntry.info.flags&ApplicationInfo.FLAG_INSTALLED) == 0;
        } else {
            // All other times: if the app no longer exists then we want
            // to go away.
            try {
                ApplicationInfo ainfo = context.getPackageManager().getApplicationInfo(
                        mAppEntry.info.packageName, PackageManager.GET_UNINSTALLED_PACKAGES
                        | PackageManager.GET_DISABLED_COMPONENTS);
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

    private void updateBattery() {
        if (mSipper != null) {
            mBatteryPreference.setEnabled(true);
            int dischargeAmount = mBatteryHelper.getStats().getDischargeAmount(
                    BatteryStats.STATS_SINCE_CHARGED);
            final int percentOfMax = (int) ((mSipper.totalPowerMah)
                    / mBatteryHelper.getTotalPower() * dischargeAmount + .5f);
            mBatteryPreference.setSummary(getString(R.string.battery_summary, percentOfMax));
        } else {
            mBatteryPreference.setEnabled(false);
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
                                new DisableChanger(InstalledAppDetails.this, mAppEntry.info,
                                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
                                .execute((Object)null);
                            }
                        })
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
            case DLG_SPECIAL_DISABLE:
                return new AlertDialog.Builder(getActivity())
                        .setMessage(getActivity().getText(R.string.app_special_disable_dlg_text))
                        .setPositiveButton(R.string.app_disable_dlg_positive,
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Clear user data here
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
            case DLG_FACTORY_RESET:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getActivity().getText(R.string.app_factory_reset_dlg_title))
                        .setMessage(getActivity().getText(R.string.app_factory_reset_dlg_text))
                        .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Clear user data here
                                uninstallPkg(mAppEntry.info.packageName,
                                        false, false);
                            }
                        })
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
        }
        return null;
    }

    private void uninstallPkg(String packageName, boolean allUsers, boolean andDisable) {
         // Create new intent to launch Uninstaller activity
        Uri packageURI = Uri.parse("package:"+packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, allUsers);
        startActivityForResult(uninstallIntent, REQUEST_UNINSTALL);
        mDisableAfterUninstall = andDisable;
    }

    private void forceStopPackage(String pkgName) {
        ActivityManager am = (ActivityManager)getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
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
        if (mAppControlRestricted) {
            mForceStopButton.setEnabled(false);
        } else {
            mForceStopButton.setEnabled(enabled);
            mForceStopButton.setOnClickListener(InstalledAppDetails.this);
        }
    }

    private void checkForceStop() {
        if (mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            // User can't force stop device admin.
            updateForceStopButton(false);
        } else if ((mAppEntry.info.flags&ApplicationInfo.FLAG_STOPPED) == 0) {
            // If the app isn't explicitly stopped, then always show the
            // force stop button.
            updateForceStopButton(true);
        } else {
            Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                    Uri.fromParts("package", mAppEntry.info.packageName, null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[] { mAppEntry.info.packageName });
            intent.putExtra(Intent.EXTRA_UID, mAppEntry.info.uid);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mAppEntry.info.uid));
            getActivity().sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null,
                    mCheckKillProcessesReceiver, null, Activity.RESULT_CANCELED, null, null);
        }
    }

    private void startManagePermissionsActivity() {
        // start new activity to manage app permissions
        Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mAppEntry.info.packageName);
        intent.putExtra(AppInfoWithHeader.EXTRA_HIDE_INFO_BUTTON, true);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(LOG_TAG, "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }

    private void startAppInfoFragment(Class<?> fragment, CharSequence title) {
        // start new fragment to display extended information
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, mAppEntry.info.packageName);
        args.putInt(ARG_PACKAGE_UID, mAppEntry.info.uid);
        args.putBoolean(AppInfoWithHeader.EXTRA_HIDE_INFO_BUTTON, true);

        SettingsActivity sa = (SettingsActivity) getActivity();
        sa.startPreferencePanel(fragment.getName(), args, -1, title, this, SUB_INFO_FRAGMENT);
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
        if(v == mUninstallButton) {
            if ((mAppEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                if (mAppEntry.info.enabled && !isDisabledUntilUsed()) {
                    if (mUpdatedSysApp) {
                        showDialogInner(DLG_SPECIAL_DISABLE, 0);
                    } else {
                        showDialogInner(DLG_DISABLE, 0);
                    }
                } else {
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
            showDialogInner(DLG_FORCE_STOP, 0);
            //forceStopPackage(mAppInfo.packageName);
        }
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
                    mStatsManager.getMemInfo(), mStats);
        } else if (preference == mDataPreference) {
            Bundle args = new Bundle();
            args.putString(DataUsageSummary.EXTRA_SHOW_APP_IMMEDIATE_PKG,
                    mAppEntry.info.packageName);

            SettingsActivity sa = (SettingsActivity) getActivity();
            sa.startPreferencePanel(DataUsageSummary.class.getName(), args, -1,
                    getString(R.string.app_data_usage), this, SUB_INFO_FRAGMENT);
        } else if (preference == mBatteryPreference) {
            BatteryEntry entry = new BatteryEntry(getActivity(), null, mUserManager, mSipper);
            PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(),
                    mBatteryHelper, BatteryStats.STATS_SINCE_CHARGED, entry, true);
        } else {
            return false;
        }
        return true;
    }

    public static void setupAppSnippet(View appSnippet, CharSequence label, Drawable icon,
            CharSequence versionName) {
        LayoutInflater.from(appSnippet.getContext()).inflate(R.layout.widget_text_views,
                (ViewGroup) appSnippet.findViewById(android.R.id.widget_frame));

        ImageView iconView = (ImageView) appSnippet.findViewById(android.R.id.icon);
        iconView.setImageDrawable(icon);
        // Set application name.
        TextView labelView = (TextView) appSnippet.findViewById(android.R.id.title);
        labelView.setText(label);
        // Version number of application
        TextView appVersion = (TextView) appSnippet.findViewById(R.id.widget_text1);

        if (!TextUtils.isEmpty(versionName)) {
            appVersion.setSelected(true);
            appVersion.setVisibility(View.VISIBLE);
            appVersion.setText(appSnippet.getContext().getString(R.string.version_text,
                    String.valueOf(versionName)));
        } else {
            appVersion.setVisibility(View.INVISIBLE);
        }
    }

    private static NetworkTemplate getTemplate(Context context) {
        if (DataUsageSummary.hasReadyMobileRadio(context)) {
            return NetworkTemplate.buildTemplateMobileWildcard();
        }
        if (DataUsageSummary.hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        }
        return NetworkTemplate.buildTemplateEthernet();
    }

    public static CharSequence getNotificationSummary(AppEntry appEntry, Context context) {
        return getNotificationSummary(appEntry, context, new NotificationBackend());
    }

    public static CharSequence getNotificationSummary(AppEntry appEntry, Context context,
            NotificationBackend backend) {
        AppRow appRow = backend.loadAppRow(context.getPackageManager(), appEntry.info);
        return getNotificationSummary(appRow, context);
    }

    public static CharSequence getNotificationSummary(AppRow appRow, Context context) {
        if (appRow.banned) {
            return context.getString(R.string.notifications_disabled);
        }
        ArrayList<CharSequence> notifSummary = new ArrayList<>();
        if (appRow.priority) {
            notifSummary.add(context.getString(R.string.notifications_priority));
        }
        if (appRow.sensitive) {
            notifSummary.add(context.getString(R.string.notifications_sensitive));
        }
        if (!appRow.peekable) {
            notifSummary.add(context.getString(R.string.notifications_no_peeking));
        }
        switch (notifSummary.size()) {
            case 3:
                return context.getString(R.string.notifications_three_items,
                        notifSummary.get(0), notifSummary.get(1), notifSummary.get(2));
            case 2:
                return context.getString(R.string.notifications_two_items,
                        notifSummary.get(0), notifSummary.get(1));
            case 1:
                return notifSummary.get(0);
            default:
                return context.getString(R.string.notifications_enabled);
        }
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

    private class BatteryUpdater extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            mBatteryHelper.create((Bundle) null);
            mBatteryHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED,
                    mUserManager.getUserProfiles());
            List<BatterySipper> usageList = mBatteryHelper.getUsageList();
            final int N = usageList.size();
            for (int i = 0; i < N; i++) {
                BatterySipper sipper = usageList.get(i);
                if (sipper.getUid() == mPackageInfo.applicationInfo.uid) {
                    mSipper = sipper;
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (getActivity() == null) {
                return;
            }
            refreshUi();
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
            updateForceStopButton(getResultCode() != Activity.RESULT_CANCELED);
        }
    };

    private final PermissionsResultCallback mPermissionCallback
            = new PermissionsResultCallback() {
        @Override
        public void onPermissionSummaryResult(int[] counts, CharSequence[] groupLabels) {
            if (getActivity() == null) {
                return;
            }
            mPermissionReceiver = null;
            final Resources res = getResources();
            CharSequence summary = null;
            if (counts != null) {
                int totalCount = counts[1];
                int additionalCounts = counts[2];

                if (totalCount == 0) {
                    summary = res.getString(
                            R.string.runtime_permissions_summary_no_permissions_requested);
                } else {
                    final ArrayList<CharSequence> list = new ArrayList(Arrays.asList(groupLabels));
                    if (additionalCounts > 0) {
                        // N additional permissions.
                        list.add(res.getQuantityString(
                                R.plurals.runtime_permissions_additional_count,
                                additionalCounts, additionalCounts));
                    }
                    if (list.size() == 0) {
                        summary = res.getString(
                                R.string.runtime_permissions_summary_no_permissions_granted);
                    } else {
                        summary = ListFormatter.getInstance().format(list);
                    }
                }
            }
            mPermissionsPreference.setSummary(summary);
        }
    };
}
