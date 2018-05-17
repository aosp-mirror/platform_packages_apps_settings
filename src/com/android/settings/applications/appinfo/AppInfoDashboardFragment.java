/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.DeviceAdminAdd;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dashboard fragment to display application information from Settings. This activity presents
 * extended information associated with a package like code, data, total size, permissions
 * used by the application and also the set of default launchable activities.
 * For system applications, an option to clear user data is displayed only if data size is > 0.
 * System applications that do not want clear user data do not have this option.
 * For non-system applications, there is no option to clear data. Instead there is an option to
 * uninstall the application.
 */
public class AppInfoDashboardFragment extends DashboardFragment
        implements ApplicationsState.Callbacks {

    private static final String TAG = "AppInfoDashboard";

    // Menu identifiers
    @VisibleForTesting
    static final int UNINSTALL_ALL_USERS_MENU = 1;
    @VisibleForTesting
    static final int UNINSTALL_UPDATES = 2;
    static final int INSTALL_INSTANT_APP_MENU = 3;

    // Result code identifiers
    @VisibleForTesting
    static final int REQUEST_UNINSTALL = 0;
    private static final int REQUEST_REMOVE_DEVICE_ADMIN = 1;

    static final int SUB_INFO_FRAGMENT = 1;

    static final int LOADER_CHART_DATA = 2;
    static final int LOADER_STORAGE = 3;
    static final int LOADER_BATTERY = 4;

    // Dialog identifiers used in showDialog
    private static final int DLG_BASE = 0;
    private static final int DLG_FORCE_STOP = DLG_BASE + 1;
    private static final int DLG_DISABLE = DLG_BASE + 2;
    private static final int DLG_SPECIAL_DISABLE = DLG_BASE + 3;
    static final int DLG_CLEAR_INSTANT_APP = DLG_BASE + 4;

    public static final String ARG_PACKAGE_NAME = "package";
    public static final String ARG_PACKAGE_UID = "uid";

    private static final boolean localLOGV = false;

    private EnforcedAdmin mAppsControlDisallowedAdmin;
    private boolean mAppsControlDisallowedBySystem;

    private ApplicationsState mState;
    private ApplicationsState.Session mSession;
    private ApplicationsState.AppEntry mAppEntry;
    private PackageInfo mPackageInfo;
    private int mUserId;
    private String mPackageName;

    private DevicePolicyManager mDpm;
    private UserManager mUserManager;
    private PackageManager mPm;

    private boolean mFinishing;
    private boolean mListeningToPackageRemove;


    private boolean mInitialized;
    private boolean mShowUninstalled;
    private boolean mUpdatedSysApp = false;
    private boolean mDisableAfterUninstall;

    private List<Callback> mCallbacks = new ArrayList<>();

    private InstantAppButtonsPreferenceController mInstantAppButtonPreferenceController;
    private AppActionButtonPreferenceController mAppActionButtonPreferenceController;

    /**
     * Callback to invoke when app info has been changed.
     */
    public interface Callback {
        void refreshUi();
    }

    private boolean isDisabledUntilUsed() {
        return mAppEntry.info.enabledSetting
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final String packageName = getPackageName();
        use(TimeSpentInAppPreferenceController.class).setPackageName(packageName);

        use(AppDataUsagePreferenceController.class).setParentFragment(this);
        final AppInstallerInfoPreferenceController installer =
                use(AppInstallerInfoPreferenceController.class);
        installer.setPackageName(packageName);
        installer.setParentFragment(this);
        use(AppInstallerPreferenceCategoryController.class).setChildren(Arrays.asList(installer));
        use(AppNotificationPreferenceController.class).setParentFragment(this);
        use(AppOpenByDefaultPreferenceController.class).setParentFragment(this);
        use(AppPermissionPreferenceController.class).setParentFragment(this);
        use(AppPermissionPreferenceController.class).setPackageName(packageName);
        use(AppSettingPreferenceController.class)
                .setPackageName(packageName)
                .setParentFragment(this);
        use(AppStoragePreferenceController.class).setParentFragment(this);
        use(AppVersionPreferenceController.class).setParentFragment(this);
        use(InstantAppDomainsPreferenceController.class).setParentFragment(this);

        final WriteSystemSettingsPreferenceController writeSystemSettings =
                use(WriteSystemSettingsPreferenceController.class);
        writeSystemSettings.setParentFragment(this);

        final DrawOverlayDetailPreferenceController drawOverlay =
                use(DrawOverlayDetailPreferenceController.class);
        drawOverlay.setParentFragment(this);

        final PictureInPictureDetailPreferenceController pip =
                use(PictureInPictureDetailPreferenceController.class);
        pip.setPackageName(packageName);
        pip.setParentFragment(this);
        final ExternalSourceDetailPreferenceController externalSource =
                use(ExternalSourceDetailPreferenceController.class);
        externalSource.setPackageName(packageName);
        externalSource.setParentFragment(this);

        use(AdvancedAppInfoPreferenceCategoryController.class).setChildren(Arrays.asList(
                writeSystemSettings, drawOverlay, pip, externalSource));
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFinishing = false;
        final Activity activity = getActivity();
        mDpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUserManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mPm = activity.getPackageManager();

        if (!ensurePackageInfoAvailable(activity)) {
            return;
        }

        startListeningToPackageRemove();

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        stopListeningToPackageRemove();
        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_INSTALLED_APP_DETAILS;
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        mAppsControlDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(activity,
                UserManager.DISALLOW_APPS_CONTROL, mUserId);
        mAppsControlDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(activity,
                UserManager.DISALLOW_APPS_CONTROL, mUserId);

        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_info_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        retrieveAppEntry();
        if (mPackageInfo == null) {
            return null;
        }
        final String packageName = getPackageName();
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getLifecycle();

        // The following are controllers for preferences that needs to refresh the preference state
        // when app state changes.
        controllers.add(
                new AppHeaderViewPreferenceController(context, this, packageName, lifecycle));
        mAppActionButtonPreferenceController =
                new AppActionButtonPreferenceController(context, this, packageName);
        controllers.add(mAppActionButtonPreferenceController);

        for (AbstractPreferenceController controller : controllers) {
            mCallbacks.add((Callback) controller);
        }

        // The following are controllers for preferences that don't need to refresh the preference
        // state when app state changes.
        mInstantAppButtonPreferenceController =
                new InstantAppButtonsPreferenceController(context, this, packageName, lifecycle);
        controllers.add(mInstantAppButtonPreferenceController);
        controllers.add(new AppBatteryPreferenceController(context, this, packageName, lifecycle));
        controllers.add(new AppMemoryPreferenceController(context, this, lifecycle));
        controllers.add(new DefaultHomeShortcutPreferenceController(context, packageName));
        controllers.add(new DefaultBrowserShortcutPreferenceController(context, packageName));
        controllers.add(new DefaultPhoneShortcutPreferenceController(context, packageName));
        controllers.add(new DefaultEmergencyShortcutPreferenceController(context, packageName));
        controllers.add(new DefaultSmsShortcutPreferenceController(context, packageName));

        return controllers;
    }

    void addToCallbackList(Callback callback) {
        if (callback != null) {
            mCallbacks.add(callback);
        }
    }

    ApplicationsState.AppEntry getAppEntry() {
        return mAppEntry;
    }

    void setAppEntry(ApplicationsState.AppEntry appEntry) {
        mAppEntry = appEntry;
    }

    PackageInfo getPackageInfo() {
        return mPackageInfo;
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        if (!TextUtils.equals(packageName, mPackageName)) {
            Log.d(TAG, "Package change irrelevant, skipping");
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
    @VisibleForTesting
    boolean ensurePackageInfoAvailable(Activity activity) {
        if (mPackageInfo == null) {
            mFinishing = true;
            Log.w(TAG, "Package info not available. Is this package already uninstalled?");
            activity.finishAndRemoveTask();
            return false;
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
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
        super.onPrepareOptionsMenu(menu);
        menu.findItem(UNINSTALL_ALL_USERS_MENU).setVisible(shouldShowUninstallForAll(mAppEntry));
        mUpdatedSysApp = (mAppEntry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        final MenuItem uninstallUpdatesItem = menu.findItem(UNINSTALL_UPDATES);
        final boolean uninstallUpdateDisabled = getContext().getResources().getBoolean(
                R.bool.config_disable_uninstall_update);
        uninstallUpdatesItem.setVisible(
                mUpdatedSysApp && !mAppsControlDisallowedBySystem && !uninstallUpdateDisabled);
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
        return super.onOptionsItemSelected(item);
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
                            .execute((Object) null);
                }
                if (!refreshUi()) {
                    onPackageRemoved();
                } else {
                    startListeningToPackageRemove();
                }
                break;
            case REQUEST_REMOVE_DEVICE_ADMIN:
                if (!refreshUi()) {
                    setIntentAndFinish(true, true);
                } else {
                    startListeningToPackageRemove();
                }
                break;
        }
    }

    @VisibleForTesting
    boolean shouldShowUninstallForAll(AppEntry appEntry) {
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
        } else if (getNumberOfUserWithPackageInstalled(mPackageName) < 2
                && (appEntry.info.flags & ApplicationInfo.FLAG_INSTALLED) != 0) {
            showIt = false;
        } else if (AppUtils.isInstant(appEntry.info)) {
            showIt = false;
        }
        return showIt;
    }

    @VisibleForTesting
    boolean refreshUi() {
        retrieveAppEntry();
        if (mAppEntry == null) {
            return false; // onCreate must have failed, make sure to exit
        }

        if (mPackageInfo == null) {
            return false; // onCreate must have failed, make sure to exit
        }

        mState.ensureIcon(mAppEntry);

        // Update the preference summaries.
        for (Callback callback : mCallbacks) {
            callback.refreshUi();
        }

        if (!mInitialized) {
            // First time init: are we displaying an uninstalled app?
            mInitialized = true;
            mShowUninstalled = (mAppEntry.info.flags & ApplicationInfo.FLAG_INSTALLED) == 0;
        } else {
            // All other times: if the app no longer exists then we want
            // to go away.
            try {
                final ApplicationInfo ainfo = getActivity().getPackageManager().getApplicationInfo(
                        mAppEntry.info.packageName,
                        PackageManager.MATCH_DISABLED_COMPONENTS
                                | PackageManager.MATCH_ANY_USER);
                if (!mShowUninstalled) {
                    // If we did not start out with the app uninstalled, then
                    // it transitioning to the uninstalled state for the current
                    // user means we should go away as well.
                    return (ainfo.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    @VisibleForTesting
    AlertDialog createDialog(int id, int errorCode) {
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
                                        new DisableChanger(AppInfoDashboardFragment.this,
                                                mAppEntry.info,
                                                PackageManager
                                                        .COMPONENT_ENABLED_STATE_DISABLED_USER)
                                                .execute((Object) null);
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
        return mInstantAppButtonPreferenceController.createDialog(id);
    }

    private void uninstallPkg(String packageName, boolean allUsers, boolean andDisable) {
        stopListeningToPackageRemove();
        // Create new intent to launch Uninstaller activity
        final Uri packageURI = Uri.parse("package:" + packageName);
        final Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, allUsers);
        mMetricsFeatureProvider.action(
                getContext(), MetricsEvent.ACTION_SETTINGS_UNINSTALL_APP);
        startActivityForResult(uninstallIntent, REQUEST_UNINSTALL);
        mDisableAfterUninstall = andDisable;
    }

    private void forceStopPackage(String pkgName) {
        mMetricsFeatureProvider.action(getContext(), MetricsEvent.ACTION_APP_FORCE_STOP, pkgName);
        final ActivityManager am = (ActivityManager) getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        Log.d(TAG, "Stopping package " + pkgName);
        am.forceStopPackage(pkgName);
        final int userId = UserHandle.getUserId(mAppEntry.info.uid);
        mState.invalidatePackage(pkgName, userId);
        final AppEntry newEnt = mState.getEntry(pkgName, userId);
        if (newEnt != null) {
            mAppEntry = newEnt;
        }
        mAppActionButtonPreferenceController.checkForceStop(mAppEntry, mPackageInfo);
    }

    public static void startAppInfoFragment(Class<?> fragment, int title, Bundle args,
            SettingsPreferenceFragment caller, AppEntry appEntry) {
        // start new fragment to display extended information
        if (args == null) {
            args = new Bundle();
        }
        args.putString(ARG_PACKAGE_NAME, appEntry.info.packageName);
        args.putInt(ARG_PACKAGE_UID, appEntry.info.uid);
        new SubSettingLauncher(caller.getContext())
                .setDestination(fragment.getName())
                .setArguments(args)
                .setTitle(title)
                .setResultListener(caller, SUB_INFO_FRAGMENT)
                .setSourceMetricsCategory(caller.getMetricsCategory())
                .launch();
    }

    void handleUninstallButtonClick() {
        if (mAppEntry == null) {
            setIntentAndFinish(true, true);
            return;
        }
        final String packageName = mAppEntry.info.packageName;
        if (mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            stopListeningToPackageRemove();
            final Activity activity = getActivity();
            final Intent uninstallDAIntent = new Intent(activity, DeviceAdminAdd.class);
            uninstallDAIntent.putExtra(DeviceAdminAdd.EXTRA_DEVICE_ADMIN_PACKAGE_NAME,
                    mPackageName);
            mMetricsFeatureProvider.action(
                    activity, MetricsEvent.ACTION_SETTINGS_UNINSTALL_DEVICE_ADMIN);
            activity.startActivityForResult(uninstallDAIntent, REQUEST_REMOVE_DEVICE_ADMIN);
            return;
        }
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfUninstallBlocked(getActivity(),
                packageName, mUserId);
        final boolean uninstallBlockedBySystem = mAppsControlDisallowedBySystem ||
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
                        MetricsEvent.ACTION_SETTINGS_ENABLE_APP);
                new DisableChanger(this, mAppEntry.info,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
                        .execute((Object) null);
            }
        } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
            uninstallPkg(packageName, true, false);
        } else {
            uninstallPkg(packageName, false, false);
        }
    }

    void handleForceStopButtonClick() {
        if (mAppEntry == null) {
            setIntentAndFinish(true, true);
            return;
        }
        if (mAppsControlDisallowedAdmin != null && !mAppsControlDisallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                    getActivity(), mAppsControlDisallowedAdmin);
        } else {
            showDialogInner(DLG_FORCE_STOP, 0);
            //forceStopPackage(mAppInfo.packageName);
        }
    }

    /** Returns whether there is only one user on this device, not including the system-only user */
    private boolean isSingleUser() {
        final int userCount = mUserManager.getUserCount();
        return userCount == 1 || (mUserManager.isSplitSystemUser() && userCount == 2);
    }

    private void onPackageRemoved() {
        getActivity().finishActivity(SUB_INFO_FRAGMENT);
        getActivity().finishAndRemoveTask();
    }

    @VisibleForTesting
    int getNumberOfUserWithPackageInstalled(String packageName) {
        final List<UserInfo> userInfos = mUserManager.getUsers(true);
        int count = 0;

        for (final UserInfo userInfo : userInfos) {
            try {
                // Use this API to check whether user has this package
                final ApplicationInfo info = mPm.getApplicationInfoAsUser(
                        packageName, PackageManager.GET_META_DATA, userInfo.id);
                if ((info.flags & ApplicationInfo.FLAG_INSTALLED) != 0) {
                    count++;
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Package: " + packageName + " not found for user: " + userInfo.id);
            }
        }

        return count;
    }

    private static class DisableChanger extends AsyncTask<Object, Object, Object> {
        final PackageManager mPm;
        final WeakReference<AppInfoDashboardFragment> mActivity;
        final ApplicationInfo mInfo;
        final int mState;

        DisableChanger(AppInfoDashboardFragment activity, ApplicationInfo info, int state) {
            mPm = activity.mPm;
            mActivity = new WeakReference<AppInfoDashboardFragment>(activity);
            mInfo = info;
            mState = state;
        }

        @Override
        protected Object doInBackground(Object... params) {
            mPm.setApplicationEnabledSetting(mInfo.packageName, mState, 0);
            return null;
        }
    }

    private String getPackageName() {
        if (mPackageName != null) {
            return mPackageName;
        }
        final Bundle args = getArguments();
        mPackageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        if (mPackageName == null) {
            final Intent intent = (args == null) ?
                    getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                mPackageName = intent.getData().getSchemeSpecificPart();
            }
        }
        return mPackageName;
    }

    @VisibleForTesting
    void retrieveAppEntry() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mState == null) {
            mState = ApplicationsState.getInstance(activity.getApplication());
            mSession = mState.newSession(this, getLifecycle());
        }
        mUserId = UserHandle.myUserId();
        mAppEntry = mState.getEntry(getPackageName(), UserHandle.myUserId());
        if (mAppEntry != null) {
            // Get application info again to refresh changed properties of application
            try {
                mPackageInfo = activity.getPackageManager().getPackageInfo(
                        mAppEntry.info.packageName,
                        PackageManager.MATCH_DISABLED_COMPONENTS |
                                PackageManager.MATCH_ANY_USER |
                                PackageManager.GET_SIGNATURES |
                                PackageManager.GET_PERMISSIONS);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Exception when retrieving package:" + mAppEntry.info.packageName, e);
            }
        } else {
            Log.w(TAG, "Missing AppEntry; maybe reinstalling?");
            mPackageInfo = null;
        }
    }

    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        if (localLOGV) Log.i(TAG, "appChanged=" + appChanged);
        final Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        final SettingsActivity sa = (SettingsActivity) getActivity();
        sa.finishPreferencePanel(Activity.RESULT_OK, intent);
        mFinishing = true;
    }

    void showDialogInner(int id, int moveErrorCode) {
        final DialogFragment newFragment = MyAlertDialogFragment.newInstance(id, moveErrorCode);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        // No op.
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        // No op.
    }

    @Override
    public void onPackageIconChanged() {
        // No op.
    }

    @Override
    public void onAllSizesComputed() {
        // No op.
    }

    @Override
    public void onLauncherInfoChanged() {
        // No op.
    }

    @Override
    public void onLoadEntriesCompleted() {
        // No op.
    }

    @Override
    public void onPackageListChanged() {
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    public static class MyAlertDialogFragment extends InstrumentedDialogFragment {

        private static final String ARG_ID = "id";

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_APP_INFO_ACTION;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int id = getArguments().getInt(ARG_ID);
            final int errorCode = getArguments().getInt("moveError");
            final Dialog dialog =
                    ((AppInfoDashboardFragment) getTargetFragment()).createDialog(id, errorCode);
            if (dialog == null) {
                throw new IllegalArgumentException("unknown id " + id);
            }
            return dialog;
        }

        public static MyAlertDialogFragment newInstance(int id, int errorCode) {
            final MyAlertDialogFragment dialogFragment = new MyAlertDialogFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_ID, id);
            args.putInt("moveError", errorCode);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }

    @VisibleForTesting
    void startListeningToPackageRemove() {
        if (mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = true;
        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        getContext().registerReceiver(mPackageRemovedReceiver, filter);
    }

    private void stopListeningToPackageRemove() {
        if (!mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = false;
        getContext().unregisterReceiver(mPackageRemovedReceiver);
    }

    @VisibleForTesting
    final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String packageName = intent.getData().getSchemeSpecificPart();
            if (!mFinishing && (mAppEntry == null || mAppEntry.info == null
                    || TextUtils.equals(mAppEntry.info.packageName, packageName))) {
                onPackageRemoved();
            }
        }
    };

}
