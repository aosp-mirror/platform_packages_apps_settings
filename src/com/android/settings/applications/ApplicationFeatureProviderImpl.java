/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.applications;

import static android.webkit.Flags.updateServiceV2;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.location.LocationManager;
import android.os.RemoteException;
import android.os.SystemConfigManager;
import android.os.UserManager;
import android.service.euicc.EuiccService;
import android.telecom.DefaultDialerManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.internal.telephony.SmsApplication;
import com.android.settings.R;
import com.android.settings.webview.WebViewUpdateServiceWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ApplicationFeatureProviderImpl implements ApplicationFeatureProvider {
    private static final String TAG = "AppFeatureProviderImpl";

    protected final Context mContext;
    private final PackageManager mPm;
    private final IPackageManager mPms;
    private final DevicePolicyManager mDpm;
    private final UserManager mUm;
    private final WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;
    private final SystemConfigManager mSystemConfigManager;

    /** Flags to use when querying PackageManager for Euicc component implementations. */
    private static final int EUICC_QUERY_FLAGS =
            PackageManager.MATCH_SYSTEM_ONLY | PackageManager.MATCH_DEBUG_TRIAGED_MISSING
                | PackageManager.GET_RESOLVED_FILTER;

    public ApplicationFeatureProviderImpl(Context context, PackageManager pm,
            IPackageManager pms, DevicePolicyManager dpm) {
        this(context, pm, pms, dpm, new WebViewUpdateServiceWrapper());
    }
    public ApplicationFeatureProviderImpl(Context context, PackageManager pm,
            IPackageManager pms, DevicePolicyManager dpm, WebViewUpdateServiceWrapper wvusWrapper) {
        mContext = context.getApplicationContext();
        mPm = pm;
        mPms = pms;
        mDpm = dpm;
        mUm = UserManager.get(mContext);
        mWebViewUpdateServiceWrapper = wvusWrapper;
        mSystemConfigManager = context.getSystemService(SystemConfigManager.class);
    }

    @Override
    public void calculateNumberOfPolicyInstalledApps(boolean async, NumberOfAppsCallback callback) {
        final CurrentUserAndManagedProfilePolicyInstalledAppCounter counter =
                new CurrentUserAndManagedProfilePolicyInstalledAppCounter(mContext, mPm, callback);
        if (async) {
            counter.execute();
        } else {
            counter.executeInForeground();
        }
    }

    @Override
    public void listPolicyInstalledApps(ListOfAppsCallback callback) {
        final CurrentUserPolicyInstalledAppLister lister =
                new CurrentUserPolicyInstalledAppLister(mPm, mUm, callback);
        lister.execute();
    }

    @Override
    public void calculateNumberOfAppsWithAdminGrantedPermissions(String[] permissions,
            boolean async, NumberOfAppsCallback callback) {
        final CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter counter =
                new CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter(mContext,
                        permissions, mPm, mPms, mDpm, callback);
        if (async) {
            counter.execute();
        } else {
            counter.executeInForeground();
        }
    }

    @Override
    public void listAppsWithAdminGrantedPermissions(String[] permissions,
            ListOfAppsCallback callback) {
        final CurrentUserAppWithAdminGrantedPermissionsLister lister =
                new CurrentUserAppWithAdminGrantedPermissionsLister(permissions, mPm, mPms, mDpm,
                        mUm, callback);
        lister.execute();
    }

    @Override
    public List<UserAppInfo> findPersistentPreferredActivities(int userId, Intent[] intents) {
        final List<UserAppInfo> preferredActivities = new ArrayList<>();
        final Set<UserAppInfo> uniqueApps = new ArraySet<>();
        final UserInfo userInfo = mUm.getUserInfo(userId);
        for (final Intent intent : intents) {
            try {
                final ResolveInfo resolveInfo =
                        mPms.findPersistentPreferredActivity(intent, userId);
                if (resolveInfo != null) {
                    ComponentInfo componentInfo = null;
                    if (resolveInfo.activityInfo != null) {
                        componentInfo = resolveInfo.activityInfo;
                    } else if (resolveInfo.serviceInfo != null) {
                        componentInfo = resolveInfo.serviceInfo;
                    } else if (resolveInfo.providerInfo != null) {
                        componentInfo = resolveInfo.providerInfo;
                    }
                    if (componentInfo != null) {
                        UserAppInfo info = new UserAppInfo(userInfo, componentInfo.applicationInfo);
                        if (uniqueApps.add(info)) {
                            preferredActivities.add(info);
                        }
                    }
                }
            } catch (RemoteException exception) {
            }
        }
        return preferredActivities;
    }

    @Override
    public Set<String> getKeepEnabledPackages() {
        // Find current default phone/sms app. We should keep them enabled.
        final Set<String> keepEnabledPackages = new ArraySet<>();
        final String defaultDialer = DefaultDialerManager.getDefaultDialerApplication(mContext);
        if (!TextUtils.isEmpty(defaultDialer)) {
            keepEnabledPackages.add(defaultDialer);
        }
        final ComponentName defaultSms = SmsApplication.getDefaultSmsApplication(
                mContext, true /* updateIfNeeded */);
        if (defaultSms != null) {
            keepEnabledPackages.add(defaultSms.getPackageName());
        }

        // Keep Euicc Service enabled.
        final ComponentInfo euicc = findEuiccService(mPm);
        if (euicc != null) {
            keepEnabledPackages.add(euicc.packageName);
        }

        // Keep WebView default package enabled.
        if (updateServiceV2()) {
            String packageName = mWebViewUpdateServiceWrapper.getDefaultWebViewPackageName();
            if (packageName != null) {
                keepEnabledPackages.add(packageName);
            }
        }

        keepEnabledPackages.addAll(getEnabledPackageAllowlist());

        final LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        final String locationHistoryPackage = locationManager.getExtraLocationControllerPackage();
        if (locationHistoryPackage != null) {
            keepEnabledPackages.add(locationHistoryPackage);
        }
        keepEnabledPackages.addAll(mSystemConfigManager.getPreventUserDisablePackages());
        return keepEnabledPackages;
    }

    private Set<String> getEnabledPackageAllowlist() {
        final Set<String> keepEnabledPackages = new ArraySet<>();

        // Keep Settings intelligence enabled, otherwise search feature will be disabled.
        keepEnabledPackages.add(
                mContext.getString(R.string.config_settingsintelligence_package_name));

        // Keep Package Installer enabled.
        keepEnabledPackages.add(mContext.getString(R.string.config_package_installer_package_name));

        if (mPm.getWellbeingPackageName() != null) {
            keepEnabledPackages.add(mPm.getWellbeingPackageName());
        }
        return keepEnabledPackages;
    }

    private static class CurrentUserAndManagedProfilePolicyInstalledAppCounter
            extends InstalledAppCounter {
        private NumberOfAppsCallback mCallback;

        CurrentUserAndManagedProfilePolicyInstalledAppCounter(Context context,
                PackageManager packageManager, NumberOfAppsCallback callback) {
            super(context, PackageManager.INSTALL_REASON_POLICY, packageManager);
            mCallback = callback;
        }

        @Override
        protected void onCountComplete(int num) {
            mCallback.onNumberOfAppsResult(num);
        }
    }

    private static class CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter
            extends AppWithAdminGrantedPermissionsCounter {
        private NumberOfAppsCallback mCallback;

        CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter(Context context,
                String[] permissions, PackageManager packageManager,
                IPackageManager packageManagerService,
                DevicePolicyManager devicePolicyManager, NumberOfAppsCallback callback) {
            super(context, permissions, packageManager, packageManagerService, devicePolicyManager);
            mCallback = callback;
        }

        @Override
        protected void onCountComplete(int num) {
            mCallback.onNumberOfAppsResult(num);
        }
    }

    private static class CurrentUserPolicyInstalledAppLister extends InstalledAppLister {
        private ListOfAppsCallback mCallback;

        CurrentUserPolicyInstalledAppLister(PackageManager packageManager,
                UserManager userManager, ListOfAppsCallback callback) {
            super(packageManager, userManager);
            mCallback = callback;
        }

        @Override
        protected void onAppListBuilt(List<UserAppInfo> list) {
            mCallback.onListOfAppsResult(list);
        }
    }

    private static class CurrentUserAppWithAdminGrantedPermissionsLister extends
            AppWithAdminGrantedPermissionsLister {
        private ListOfAppsCallback mCallback;

        CurrentUserAppWithAdminGrantedPermissionsLister(String[] permissions,
                PackageManager packageManager, IPackageManager packageManagerService,
                DevicePolicyManager devicePolicyManager, UserManager userManager,
                ListOfAppsCallback callback) {
            super(permissions, packageManager, packageManagerService, devicePolicyManager,
                    userManager);
            mCallback = callback;
        }

        @Override
        protected void onAppListBuilt(List<UserAppInfo> list) {
            mCallback.onListOfAppsResult(list);
        }
    }

    /**
     * Return the component info of the EuiccService to bind to, or null if none were found.
     */
    @VisibleForTesting
    ComponentInfo findEuiccService(PackageManager packageManager) {
        final Intent intent = new Intent(EuiccService.EUICC_SERVICE_INTERFACE);
        final List<ResolveInfo> resolveInfoList =
                packageManager.queryIntentServices(intent, EUICC_QUERY_FLAGS);
        final ComponentInfo bestComponent = findEuiccService(packageManager, resolveInfoList);
        if (bestComponent == null) {
            Log.w(TAG, "No valid EuiccService implementation found");
        }
        return bestComponent;
    }

    private ComponentInfo findEuiccService(
            PackageManager packageManager, List<ResolveInfo> resolveInfoList) {
        int bestPriority = Integer.MIN_VALUE;
        ComponentInfo bestComponent = null;
        if (resolveInfoList != null) {
            for (ResolveInfo resolveInfo : resolveInfoList) {
                if (!isValidEuiccComponent(packageManager, resolveInfo)) {
                    continue;
                }

                if (resolveInfo.filter.getPriority() > bestPriority) {
                    bestPriority = resolveInfo.filter.getPriority();
                    bestComponent = getComponentInfo(resolveInfo);
                }
            }
        }

        return bestComponent;
    }

    private boolean isValidEuiccComponent(
            PackageManager packageManager, ResolveInfo resolveInfo) {
        final ComponentInfo componentInfo = getComponentInfo(resolveInfo);
        final String packageName = componentInfo.packageName;

        // Verify that the app is privileged (via granting of a privileged permission).
        if (packageManager.checkPermission(
                Manifest.permission.WRITE_EMBEDDED_SUBSCRIPTIONS, packageName)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Package " + packageName
                    + " does not declare WRITE_EMBEDDED_SUBSCRIPTIONS");
            return false;
        }

        // Verify that only the system can access the component.
        final String permission;
        if (componentInfo instanceof ServiceInfo) {
            permission = ((ServiceInfo) componentInfo).permission;
        } else if (componentInfo instanceof ActivityInfo) {
            permission = ((ActivityInfo) componentInfo).permission;
        } else {
            throw new IllegalArgumentException("Can only verify services/activities");
        }
        if (!TextUtils.equals(permission, Manifest.permission.BIND_EUICC_SERVICE)) {
            Log.e(TAG, "Package " + packageName
                    + " does not require the BIND_EUICC_SERVICE permission");
            return false;
        }

        // Verify that the component declares a priority.
        if (resolveInfo.filter == null || resolveInfo.filter.getPriority() == 0) {
            Log.e(TAG, "Package " + packageName + " does not specify a priority");
            return false;
        }
        return true;
    }

    private ComponentInfo getComponentInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo;
        }
        if (resolveInfo.serviceInfo != null) {
            return resolveInfo.serviceInfo;
        }
        if (resolveInfo.providerInfo != null) {
            return resolveInfo.providerInfo;
        }
        throw new IllegalStateException("Missing ComponentInfo!");
    }

    @Override
    public boolean isLongBackgroundTaskPermissionToggleSupported() {
        // Since the RUN_USER_INITIATED_JOBS permission related to this controller is a normal
        // app-op permission allowed by default, this should always return false - if it is ever
        // converted to a special app-op permission, this should be updated.
        return false;
    }
}
