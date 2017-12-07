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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.IWebViewUpdateService;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.DeviceAdminAdd;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settings.widget.EntityHeaderController;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settings.wrapper.DevicePolicyManagerWrapper;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
    private static final int UNINSTALL_ALL_USERS_MENU = 1;
    private static final int UNINSTALL_UPDATES = 2;

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
    private static final String KEY_HEADER = "header_view";
    private static final String KEY_ACTION_BUTTONS = "action_buttons";
    private static final String KEY_ADVANCED_APP_INFO_CATEGORY = "advanced_app_info";

    public static final String ARG_PACKAGE_NAME = "package";
    public static final String ARG_PACKAGE_UID = "uid";

    private static final boolean localLOGV = false;

    private EnforcedAdmin mAppsControlDisallowedAdmin;
    private boolean mAppsControlDisallowedBySystem;

    private ApplicationFeatureProvider mApplicationFeatureProvider;
    private ApplicationsState mState;
    private ApplicationsState.Session mSession;
    private ApplicationsState.AppEntry mAppEntry;
    private PackageInfo mPackageInfo;
    private int mUserId;
    private String mPackageName;

    private DevicePolicyManagerWrapper mDpm;
    private UserManager mUserManager;
    private PackageManager mPm;

    private boolean mFinishing;
    private boolean mListeningToPackageRemove;


    private final HashSet<String> mHomePackages = new HashSet<>();

    private boolean mInitialized;
    private boolean mShowUninstalled;
    private LayoutPreference mHeader;
    private boolean mUpdatedSysApp = false;
    private boolean mDisableAfterUninstall;

    private List<Callback> mCallbacks = new ArrayList<>();

    @VisibleForTesting
    ActionButtonPreference mActionButtons;

    private InstantAppButtonsPreferenceController mInstantAppButtonPreferenceController;

    /**
     * Callback to invoke when app info has been changed.
     */
    public interface Callback {
        void refreshUi();
    }

    @VisibleForTesting
    boolean handleDisableable() {
        boolean disableable = false;
        // Try to prevent the user from bricking their phone
        // by not allowing disabling of apps signed with the
        // system cert and any launcher app in the system.
        if (mHomePackages.contains(mAppEntry.info.packageName)
                || Utils.isSystemPackage(getContext().getResources(), mPm, mPackageInfo)) {
            // Disable button for core system applications.
            mActionButtons
                    .setButton1Text(R.string.disable_text)
                    .setButton1Positive(false);
        } else if (mAppEntry.info.enabled && !isDisabledUntilUsed()) {
            mActionButtons
                    .setButton1Text(R.string.disable_text)
                    .setButton1Positive(false);
            disableable = !mApplicationFeatureProvider.getKeepEnabledPackages()
                    .contains(mAppEntry.info.packageName);
        } else {
            mActionButtons
                    .setButton1Text(R.string.enable_text)
                    .setButton1Positive(true);
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
        boolean enabled;
        if (isBundled) {
            enabled = handleDisableable();
        } else {
            enabled = initUninstallButtonForUserApp();
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
            final IWebViewUpdateService webviewUpdateService =
                IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate"));
            if (webviewUpdateService.isFallbackPackage(mAppEntry.info.packageName)) {
                enabled = false;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mActionButtons.setButton1Enabled(enabled);
        if (enabled) {
            // Register listener
            mActionButtons.setButton1OnClickListener(v -> handleUninstallButtonClick());
        }
    }

    @VisibleForTesting
    boolean initUninstallButtonForUserApp() {
        boolean enabled = true;
        if ((mPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0
                && mUserManager.getUsers().size() >= 2) {
            // When we have multiple users, there is a separate menu
            // to uninstall for all users.
            enabled = false;
        } else if (AppUtils.isInstant(mPackageInfo.applicationInfo)) {
            enabled = false;
            mActionButtons.setButton1Visible(false);
        }
        mActionButtons.setButton1Text(R.string.uninstall_text).setButton1Positive(false);
        return enabled;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFinishing = false;
        final Activity activity = getActivity();
        mApplicationFeatureProvider = FeatureFactory.getFactory(activity)
                .getApplicationFeatureProvider(activity);
        mDpm = new DevicePolicyManagerWrapper(
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE));
        mUserManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mPm = activity.getPackageManager();

        retrieveAppEntry();
        startListeningToPackageRemove();

        if (!ensurePackageInfoAvailable(activity)) {
            return;
        }

        setHasOptionsMenu(true);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_INSTALLED_APP_DETAILS;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAppsControlDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(),
                UserManager.DISALLOW_APPS_CONTROL, mUserId);
        mAppsControlDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
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
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final String packageName = getPackageName();
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getLifecycle();

        // The following are controllers for preferences that needs to refresh the preference state
        // when app state changes.
        controllers.add(new AppStoragePreferenceController(context, this, lifecycle));
        controllers.add(new AppDataUsagePreferenceController(context, this, lifecycle));
        controllers.add(new AppNotificationPreferenceController(context, this));
        controllers.add(new AppOpenByDefaultPreferenceController(context, this));
        controllers.add(new AppPermissionPreferenceController(context, this, packageName));
        controllers.add(new AppVersionPreferenceController(context, this));
        controllers.add(new InstantAppDomainsPreferenceController(context, this));
        final AppInstallerInfoPreferenceController appInstallerInfoPreferenceController =
                new AppInstallerInfoPreferenceController(context, this, packageName);
        controllers.add(appInstallerInfoPreferenceController);

        for (AbstractPreferenceController controller : controllers) {
            mCallbacks.add((Callback) controller);
        }

        // The following are controllers for preferences that don't need to refresh the preference
        // state when app state changes.
        mInstantAppButtonPreferenceController =
                new InstantAppButtonsPreferenceController(context, this, packageName);
        controllers.add(mInstantAppButtonPreferenceController);
        controllers.add(new AppBatteryPreferenceController(context, this, packageName, lifecycle));
        controllers.add(new AppMemoryPreferenceController(context, this, lifecycle));
        controllers.add(new DefaultHomeShortcutPreferenceController(context, packageName));
        controllers.add(new DefaultBrowserShortcutPreferenceController(context, packageName));
        controllers.add(new DefaultPhoneShortcutPreferenceController(context, packageName));
        controllers.add(new DefaultEmergencyShortcutPreferenceController(context, packageName));
        controllers.add(new DefaultSmsShortcutPreferenceController(context, packageName));

        final List<AbstractPreferenceController> advancedAppInfoControllers = new ArrayList<>();
        advancedAppInfoControllers.add(new DrawOverlayDetailPreferenceController(context, this));
        advancedAppInfoControllers.add(new WriteSystemSettingsPreferenceController(context, this));
        advancedAppInfoControllers.add(
                new PictureInPictureDetailPreferenceController(context, this, packageName));
        advancedAppInfoControllers.add(
                new ExternalSourceDetailPreferenceController(context, this, packageName));
        controllers.addAll(advancedAppInfoControllers);
        controllers.add(new PreferenceCategoryController(
                context, KEY_ADVANCED_APP_INFO_CATEGORY, advancedAppInfoControllers));

        controllers.add(new AppInstallerPreferenceCategoryController(
                context, Arrays.asList(appInstallerInfoPreferenceController)));

        return controllers;
    }

    ApplicationsState.AppEntry getAppEntry() {
        if (mAppEntry == null) {
            retrieveAppEntry();
        }
        return mAppEntry;
    }

    PackageInfo getPackageInfo() {
        if (mAppEntry == null) {
            retrieveAppEntry();
        }
        return mPackageInfo;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mFinishing) {
            return;
        }
        final Activity activity = getActivity();
        mHeader = (LayoutPreference) findPreference(KEY_HEADER);
        mActionButtons = ((ActionButtonPreference) findPreference(KEY_ACTION_BUTTONS))
                .setButton2Text(R.string.force_stop)
                .setButton2Positive(false)
                .setButton2Enabled(false);
        EntityHeaderController.newInstance(activity, this, mHeader.findViewById(R.id.entity_header))
                .setRecyclerView(getListView(), getLifecycle())
                .setPackageName(mPackageName)
                .setHasAppInfoLink(false)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_APP_PREFERENCE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .styleActionBar(activity)
                .bindHeaderButtons();

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
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
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
        menu.findItem(UNINSTALL_ALL_USERS_MENU).setVisible(shouldShowUninstallForAll(mAppEntry));
        mUpdatedSysApp = (mAppEntry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        final MenuItem uninstallUpdatesItem = menu.findItem(UNINSTALL_UPDATES);
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
                            .execute((Object) null);
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

    @VisibleForTesting
    boolean refreshUi() {
        retrieveAppEntry();
        if (mAppEntry == null) {
            return false; // onCreate must have failed, make sure to exit
        }

        if (mPackageInfo == null) {
            return false; // onCreate must have failed, make sure to exit
        }

        // Get list of "home" apps and trace through any meta-data references
        final List<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        mPm.getHomeActivities(homeActivities);
        mHomePackages.clear();
        for (int i = 0; i< homeActivities.size(); i++) {
            final ResolveInfo ri = homeActivities.get(i);
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
        final Activity context = getActivity();
        for (Callback callback : mCallbacks) {
            callback.refreshUi();
        }

        if (!mInitialized) {
            // First time init: are we displaying an uninstalled app?
            mInitialized = true;
            mShowUninstalled = (mAppEntry.info.flags&ApplicationInfo.FLAG_INSTALLED) == 0;
        } else {
            // All other times: if the app no longer exists then we want
            // to go away.
            try {
                final ApplicationInfo ainfo = context.getPackageManager().getApplicationInfo(
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
                                new DisableChanger(AppInfoDashboardFragment.this, mAppEntry.info,
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
        return mInstantAppButtonPreferenceController.createDialog(id);
    }

    private void uninstallPkg(String packageName, boolean allUsers, boolean andDisable) {
        stopListeningToPackageRemove();
         // Create new intent to launch Uninstaller activity
        final Uri packageURI = Uri.parse("package:"+packageName);
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
        checkForceStop();
    }

    private void updateForceStopButton(boolean enabled) {
        mActionButtons
                .setButton2Enabled(mAppsControlDisallowedBySystem ? false : enabled)
                .setButton2OnClickListener(mAppsControlDisallowedBySystem
                        ? null : v -> handleForceStopButtonClick());
    }

    @VisibleForTesting
    void checkForceStop() {
        if (mDpm.packageHasActiveAdmins(mPackageInfo.packageName)) {
            // User can't force stop device admin.
            Log.w(TAG, "User can't force stop device admin");
            updateForceStopButton(false);
        } else if (AppUtils.isInstant(mPackageInfo.applicationInfo)) {
            updateForceStopButton(false);
            mActionButtons.setButton2Visible(false);
        } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_STOPPED) == 0) {
            // If the app isn't explicitly stopped, then always show the
            // force stop button.
            Log.w(TAG, "App is not explicitly stopped");
            updateForceStopButton(true);
        } else {
            final Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                    Uri.fromParts("package", mAppEntry.info.packageName, null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[] { mAppEntry.info.packageName });
            intent.putExtra(Intent.EXTRA_UID, mAppEntry.info.uid);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mAppEntry.info.uid));
            Log.d(TAG, "Sending broadcast to query restart status for "
                    + mAppEntry.info.packageName);
            getActivity().sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null,
                    mCheckKillProcessesReceiver, null, Activity.RESULT_CANCELED, null, null);
        }
    }

    public static void startAppInfoFragment(Class<?> fragment, int title,
            SettingsPreferenceFragment caller, AppEntry appEntry) {
        // start new fragment to display extended information
        final Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, appEntry.info.packageName);
        args.putInt(ARG_PACKAGE_UID, appEntry.info.uid);

        final SettingsActivity sa = (SettingsActivity) caller.getActivity();
        sa.startPreferencePanel(caller, fragment.getName(), args, title, null, caller,
                SUB_INFO_FRAGMENT);
    }

    private void handleUninstallButtonClick() {
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

    private void handleForceStopButtonClick() {
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
            } catch(NameNotFoundException e) {
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

    private final BroadcastReceiver mCheckKillProcessesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean enabled = getResultCode() != Activity.RESULT_CANCELED;
            Log.d(TAG, "Got broadcast response: Restart status for "
                    + mAppEntry.info.packageName + " " + enabled);
            updateForceStopButton(enabled);
        }
    };

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

    private void retrieveAppEntry() {
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
        if (localLOGV) Log.i(TAG, "appChanged="+appChanged);
        final Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        final SettingsActivity sa = (SettingsActivity)getActivity();
        sa.finishPreferencePanel(this, Activity.RESULT_OK, intent);
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

    private void startListeningToPackageRemove() {
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

    private final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() {
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
