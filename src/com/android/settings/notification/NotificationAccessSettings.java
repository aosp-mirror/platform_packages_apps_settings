/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.notification;

import android.annotation.Nullable;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.specialaccess.notificationaccess.NotificationAccessDetails;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.widget.EmptyTextSettings;
import com.android.settingslib.applications.ServiceListing;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

/**
 * Settings screen for managing notification listener permissions
 */
@SearchIndexable
public class NotificationAccessSettings extends EmptyTextSettings {
    private static final String TAG = "NotifAccessSettings";

    private static final int MAX_CN_LENGTH = 500;

    private static final ManagedServiceSettings.Config CONFIG =
            new ManagedServiceSettings.Config.Builder()
                    .setTag(TAG)
                    .setSetting(Settings.Secure.ENABLED_NOTIFICATION_LISTENERS)
                    .setIntentAction(NotificationListenerService.SERVICE_INTERFACE)
                    .setPermission(android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                    .setNoun("notification listener")
                    .setWarningDialogTitle(R.string.notification_listener_security_warning_title)
                    .setWarningDialogSummary(
                            R.string.notification_listener_security_warning_summary)
                    .setEmptyText(R.string.no_notification_listeners)
                    .build();

    private NotificationManager mNm;
    protected Context mContext;
    private PackageManager mPm;
    private DevicePolicyManager mDpm;
    private ServiceListing mServiceListing;
    private IconDrawableFactory mIconDrawableFactory;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();
        mPm = mContext.getPackageManager();
        mDpm = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
        mServiceListing = new ServiceListing.Builder(mContext)
                .setPermission(CONFIG.permission)
                .setIntentAction(CONFIG.intentAction)
                .setNoun(CONFIG.noun)
                .setSetting(CONFIG.setting)
                .setTag(CONFIG.tag)
                .setValidator(info -> {
                    if (info.getComponentName().flattenToString().length() > MAX_CN_LENGTH) {
                        return false;
                    }
                    return true;
                })
                .build();
        mServiceListing.addCallback(this::updateList);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(mContext));

        if (UserManager.get(mContext).isManagedProfile()) {
            // Apps in the work profile do not support notification listeners.
            Toast.makeText(mContext, R.string.notification_settings_work_profile,
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(CONFIG.emptyText);
    }

    @Override
    public void onResume() {
        super.onResume();
        mServiceListing.reload();
        mServiceListing.setListening(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mServiceListing.setListening(false);
    }

    private void updateList(List<ServiceInfo> services) {
        final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        final int managedProfileId = Utils.getManagedProfileId(um, UserHandle.myUserId());

        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        services.sort(new PackageItemInfo.DisplayNameComparator(mPm));
        for (ServiceInfo service : services) {
            final ComponentName cn = new ComponentName(service.packageName, service.name);
            CharSequence title = null;
            try {
                title = mPm.getApplicationInfoAsUser(
                        service.packageName, 0, UserHandle.myUserId()).loadLabel(mPm);
            } catch (PackageManager.NameNotFoundException e) {
                // unlikely, as we are iterating over live services.
                Log.e(TAG, "can't find package name", e);
            }

            final Preference pref = new Preference(getPrefContext());
            pref.setTitle(title);
            pref.setIcon(mIconDrawableFactory.getBadgedIcon(service, service.applicationInfo,
                    UserHandle.getUserId(service.applicationInfo.uid)));
            pref.setKey(cn.flattenToString());
            pref.setSummary(mNm.isNotificationListenerAccessGranted(cn)
                    ? R.string.app_permission_summary_allowed
                    : R.string.app_permission_summary_not_allowed);
            if (managedProfileId != UserHandle.USER_NULL
                    && !mDpm.isNotificationListenerServicePermitted(
                    service.packageName, managedProfileId)) {
                pref.setSummary(R.string.work_profile_notification_access_blocked_summary);
            }
            pref.setOnPreferenceClickListener(preference -> {
                final Bundle args = new Bundle();
                args.putString(AppInfoBase.ARG_PACKAGE_NAME, cn.getPackageName());
                args.putInt(AppInfoBase.ARG_PACKAGE_UID, service.applicationInfo.uid);

                Bundle extras = new Bundle();
                extras.putString(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        cn.flattenToString());

                new SubSettingLauncher(getContext())
                        .setDestination(NotificationAccessDetails.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setTitleRes(R.string.manage_zen_access_title)
                        .setArguments(args)
                        .setExtras(extras)
                        .setUserHandle(UserHandle.getUserHandleForUid(service.applicationInfo.uid))
                        .launch();
                        return true;
                    });
            pref.setKey(cn.flattenToString());
            screen.addPreference(pref);
        }
        highlightPreferenceIfNeeded();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ACCESS;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mNm = context.getSystemService(NotificationManager.class);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.notification_access_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.notification_access_settings);
}
