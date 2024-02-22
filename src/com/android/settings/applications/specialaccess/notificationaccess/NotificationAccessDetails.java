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

package com.android.settings.applications.specialaccess.notificationaccess;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.android.settings.applications.AppInfoBase.ARG_PACKAGE_NAME;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.companion.ICompanionDeviceManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.util.List;
import java.util.Objects;

public class NotificationAccessDetails extends DashboardFragment {
    private static final String TAG = "NotifAccessDetails";

    private NotificationBackend mNm = new NotificationBackend();
    private ComponentName mComponentName;
    private CharSequence mServiceName;
    protected ServiceInfo mServiceInfo;
    protected PackageInfo mPackageInfo;
    protected int mUserId;
    protected String mPackageName;
    protected RestrictedLockUtils.EnforcedAdmin mAppsControlDisallowedAdmin;
    protected boolean mAppsControlDisallowedBySystem;
    private boolean mIsNls;
    private PackageManager mPm;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Intent intent = getIntent();
        if (mComponentName == null && intent != null) {
            String cn = intent.getStringExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME);
            if (cn != null) {
                mComponentName = ComponentName.unflattenFromString(cn);
                if (mComponentName != null) {
                    final Bundle args = getArguments();
                    args.putString(ARG_PACKAGE_NAME, mComponentName.getPackageName());
                }
            }
        }
        mPm = getPackageManager();
        retrieveAppEntry();
        loadNotificationListenerService();
        NotificationBackend backend = new NotificationBackend();
        int listenerTargetSdk = Build.VERSION_CODES.S;
        try {
            listenerTargetSdk = mPm.getTargetSdkVersion(mComponentName.getPackageName());
        } catch (PackageManager.NameNotFoundException e){
            // how did we get here?
        }
        use(ApprovalPreferenceController.class)
                .setPkgInfo(mPackageInfo)
                .setCn(mComponentName)
                .setNm(context.getSystemService(NotificationManager.class))
                .setPm(mPm)
                .setSettingIdentifier(AppOpsManager.OPSTR_ACCESS_NOTIFICATIONS)
                .setParent(this);
        use(HeaderPreferenceController.class)
                .setFragment(this)
                .setPackageInfo(mPackageInfo)
                .setPm(context.getPackageManager())
                .setServiceName(mServiceName)
                .setBluetoothManager(Utils.getLocalBtManager(context))
                .setCdm(ICompanionDeviceManager.Stub.asInterface(
                        ServiceManager.getService(Context.COMPANION_DEVICE_SERVICE)))
                .setCn(mComponentName)
                .setUserId(mUserId);
        use(PreUpgradePreferenceController.class)
                .setNm(backend)
                .setCn(mComponentName)
                .setUserId(mUserId)
                .setTargetSdk(listenerTargetSdk);
        use(BridgedAppsLinkPreferenceController.class)
                .setNm(backend)
                .setCn(mComponentName)
                .setUserId(mUserId)
                .setTargetSdk(listenerTargetSdk);
        use(MoreSettingsPreferenceController.class)
                .setPackage(mComponentName.getPackageName())
                .setPackageManager(mPm);
        final int finalListenerTargetSdk = listenerTargetSdk;
        getPreferenceControllers().forEach(controllers -> {
            controllers.forEach(controller -> {
                if (controller instanceof TypeFilterPreferenceController) {
                    TypeFilterPreferenceController tfpc =
                            (TypeFilterPreferenceController) controller;
                    tfpc.setNm(backend)
                            .setCn(mComponentName)
                            .setServiceInfo(mServiceInfo)
                            .setUserId(mUserId)
                            .setTargetSdk(finalListenerTargetSdk);
                }
            });
        });
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ACCESS_DETAIL;
    }

    protected boolean refreshUi() {
        if (mComponentName == null) {
            // No service given
            Slog.d(TAG, "No component name provided");
            return false;
        }
        if (!mIsNls) {
            // This component doesn't have the right androidmanifest definition to be an NLS
            Slog.d(TAG, "Provided component name is not an NLS");
            return false;
        }
        if (UserManager.get(getContext()).isManagedProfile()) {
            // Apps in the work profile do not support notification listeners.
            Slog.d(TAG, "NLSes aren't allowed in work profiles");
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAppsControlDisallowedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_APPS_CONTROL, mUserId);
        mAppsControlDisallowedBySystem = RestrictedLockUtilsInternal.hasBaseUserRestriction(
                getActivity(), UserManager.DISALLOW_APPS_CONTROL, mUserId);

        if (!refreshUi()) {
            finish();
        }
        Preference apps = getPreferenceScreen().findPreference(
                use(BridgedAppsLinkPreferenceController.class).getPreferenceKey());
        if (apps != null) {

            apps.setOnPreferenceClickListener(preference -> {
                final Bundle args = new Bundle();
                args.putString(AppInfoBase.ARG_PACKAGE_NAME, mPackageName);
                args.putString(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        mComponentName.flattenToString());

                new SubSettingLauncher(getContext())
                        .setDestination(BridgedAppsSettings.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setTitleRes(R.string.notif_listener_excluded_app_screen_title)
                        .setArguments(args)
                        .setUserHandle(UserHandle.of(mUserId))
                        .launch();
                return true;
            });
        }
    }

    protected void retrieveAppEntry() {
        final Bundle args = getArguments();
        mPackageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        Intent intent = (args == null) ?
                getIntent() : (Intent) args.getParcelable("intent");
        if (mPackageName == null) {
            if (intent != null && intent.getData() != null) {
                mPackageName = intent.getData().getSchemeSpecificPart();
            }
        }
        if (intent != null && intent.hasExtra(Intent.EXTRA_USER_HANDLE)) {
            if (hasInteractAcrossUsersPermission()) {
                mUserId = ((UserHandle) intent.getParcelableExtra(
                        Intent.EXTRA_USER_HANDLE)).getIdentifier();
            } else {
                finish();
            }
        } else {
            mUserId = UserHandle.myUserId();
        }

        try {
            mPackageInfo = mPm.getPackageInfoAsUser(mPackageName,
                    PackageManager.MATCH_DISABLED_COMPONENTS |
                            PackageManager.GET_SIGNING_CERTIFICATES |
                            PackageManager.GET_PERMISSIONS, mUserId);
        } catch (PackageManager.NameNotFoundException e) {
            // oh well
        }
    }

    private boolean hasInteractAcrossUsersPermission() {
        final String callingPackageName =
                ((SettingsActivity) getActivity()).getInitialCallingPackage();

        if (TextUtils.isEmpty(callingPackageName)) {
            Log.w(TAG, "Not able to get calling package name for permission check");
            return false;
        }

        if (getContext().getPackageManager().checkPermission(
                Manifest.permission.INTERACT_ACROSS_USERS_FULL, callingPackageName)
                != PERMISSION_GRANTED) {
            Log.w(TAG, "Package " + callingPackageName + " does not have required permission "
                    + Manifest.permission.INTERACT_ACROSS_USERS_FULL);
            return false;
        }

        return true;
    }

    // Dialogs only have access to the parent fragment, not the controller, so pass the information
    // along to keep business logic out of this file
    public void disable(final ComponentName cn) {
        final PreferenceScreen screen = getPreferenceScreen();
        ApprovalPreferenceController apc = use(ApprovalPreferenceController.class);
        apc.disable(cn);
        apc.updateState(screen.findPreference(apc.getPreferenceKey()));
        getPreferenceControllers().forEach(controllers -> {
            controllers.forEach(controller -> {
                controller.updateState(screen.findPreference(controller.getPreferenceKey()));
            });
        });
    }

    protected void enable(ComponentName cn) {
        final PreferenceScreen screen = getPreferenceScreen();
        ApprovalPreferenceController apc = use(ApprovalPreferenceController.class);
        apc.enable(cn);
        apc.updateState(screen.findPreference(apc.getPreferenceKey()));
        getPreferenceControllers().forEach(controllers -> {
            controllers.forEach(controller -> {
                controller.updateState(screen.findPreference(controller.getPreferenceKey()));
            });
        });
    }

    // To save binder calls, load this in the fragment rather than each preference controller
    protected void loadNotificationListenerService() {
        mIsNls = false;

        if (mComponentName == null) {
            return;
        }
        Intent intent = new Intent(NotificationListenerService.SERVICE_INTERFACE)
                .setComponent(mComponentName);
        List<ResolveInfo> installedServices = mPm.queryIntentServicesAsUser(
                intent, PackageManager.GET_SERVICES | PackageManager.GET_META_DATA, mUserId);
        for (ResolveInfo resolveInfo : installedServices) {
            ServiceInfo info = resolveInfo.serviceInfo;
            if (android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE.equals(
                    info.permission)) {
                if (Objects.equals(mComponentName, info.getComponentName())) {
                    mIsNls = true;
                    mServiceName = info.loadLabel(mPm);
                    mServiceInfo = info;
                    break;
                }
            }
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.notification_access_permission_details;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}