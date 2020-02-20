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

import android.app.Activity;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.util.Slog;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;

import java.util.List;
import java.util.Objects;

public class NotificationAccessDetails extends AppInfoBase {
    private static final String TAG = "NotifAccessDetails";
    private static final String SWITCH_PREF_KEY = "notification_access_switch";

    private boolean mCreated;
    private ComponentName mComponentName;
    private CharSequence mServiceName;
    private boolean mIsNls;

    private NotificationManager mNm;
    private PackageManager mPm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        super.onCreate(savedInstanceState);
        mNm = getContext().getSystemService(NotificationManager.class);
        mPm = getPackageManager();
        addPreferencesFromResource(R.xml.notification_access_permission_details);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mPackageInfo == null) return;
        loadNotificationListenerService();
        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this, null /* header */)
                .setRecyclerView(getListView(), getSettingsLifecycle())
                .setIcon(IconDrawableFactory.newInstance(getContext())
                        .getBadgedIcon(mPackageInfo.applicationInfo))
                .setLabel(mPackageInfo.applicationInfo.loadLabel(mPm))
                .setSummary(mServiceName)
                .setIsInstantApp(AppUtils.isInstant(mPackageInfo.applicationInfo))
                .setPackageName(mPackageName)
                .setUid(mPackageInfo.applicationInfo.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .done(activity, getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ACCESS_DETAIL;
    }

    @Override
    protected boolean refreshUi() {
        final Context context = getContext();
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
        updatePreference(findPreference(SWITCH_PREF_KEY));
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    public void updatePreference(SwitchPreference preference) {
        final CharSequence label = mPackageInfo.applicationInfo.loadLabel(mPm);
        preference.setChecked(isServiceEnabled(mComponentName));
        preference.setOnPreferenceChangeListener((p, newValue) -> {
            final boolean access = (Boolean) newValue;
            if (!access) {
                if (!isServiceEnabled(mComponentName)) {
                    return true; // already disabled
                }
                // show a friendly dialog
                new FriendlyWarningDialogFragment()
                        .setServiceInfo(mComponentName, label, this)
                        .show(getFragmentManager(), "friendlydialog");
                return false;
            } else {
                if (isServiceEnabled(mComponentName)) {
                    return true; // already enabled
                }
                // show a scary dialog
                new ScaryWarningDialogFragment()
                        .setServiceInfo(mComponentName, label, this)
                        .show(getFragmentManager(), "dialog");
                return false;
            }
        });
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean enable, String packageName) {
        int logCategory = enable ? SettingsEnums.APP_SPECIAL_PERMISSION_NOTIVIEW_ALLOW
                : SettingsEnums.APP_SPECIAL_PERMISSION_NOTIVIEW_DENY;
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(),
                logCategory, packageName);
    }

    public void disable(final ComponentName cn) {
        logSpecialPermissionChange(true, cn.getPackageName());
        mNm.setNotificationListenerAccessGranted(cn, false);
        AsyncTask.execute(() -> {
            if (!mNm.isNotificationPolicyAccessGrantedForPackage(
                    cn.getPackageName())) {
                mNm.removeAutomaticZenRules(cn.getPackageName());
            }
        });
        refreshUi();
    }

    protected void enable(ComponentName cn) {
        logSpecialPermissionChange(true, cn.getPackageName());
        mNm.setNotificationListenerAccessGranted(cn, true);
        refreshUi();
    }

    protected boolean isServiceEnabled(ComponentName cn) {
        return mNm.isNotificationListenerAccessGranted(cn);
    }

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
                    break;
                }
            }
        }
    }
}