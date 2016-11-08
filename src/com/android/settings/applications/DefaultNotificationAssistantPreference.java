/**
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

import com.android.settings.AppListPreference;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.service.notification.NotificationAssistantService;
import android.util.AttributeSet;
import android.util.Slog;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.R;
import com.android.settings.utils.ManagedServiceSettings;

public class DefaultNotificationAssistantPreference extends AppListPreference {
    private static final String TAG = "DefaultNotiAssist";

    private PackageManager mPm;
    private final ManagedServiceSettings.Config mConfig;
    private final Context mContext;

    public DefaultNotificationAssistantPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mPm = context.getPackageManager();
        mConfig = getConfig();
        setShowItemNone(true);
        updateList(getServices());
    }

    @Override
    protected boolean persistString(String value) {
        Settings.Secure.putString(mContext.getContentResolver(), mConfig.setting, value);
        setSummary(getEntry());
        return true;
    }

    private void updateList(List<ServiceInfo> services) {
        final ComponentName[] assistants = new ComponentName[services.size()];
        for (int i = 0; i < services.size(); i++) {
            assistants[i] = new ComponentName(services.get(i).packageName, services.get(i).name);
        }
        final String assistant =
                Settings.Secure.getString(mContext.getContentResolver(), mConfig.setting);
        setComponentNames(assistants, assistant == null ? null
                : ComponentName.unflattenFromString(assistant));
    }

    private List<ServiceInfo> getServices() {
        List<ServiceInfo> services = new ArrayList<>();
        final int user = ActivityManager.getCurrentUser();

        List<ResolveInfo> installedServices = mPm.queryIntentServicesAsUser(
                new Intent(mConfig.intentAction),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA,
                user);

        for (int i = 0, count = installedServices.size(); i < count; i++) {
            ResolveInfo resolveInfo = installedServices.get(i);
            ServiceInfo info = resolveInfo.serviceInfo;

            if (!mConfig.permission.equals(info.permission)) {
                Slog.w(mConfig.tag, "Skipping " + mConfig.noun + " service "
                        + info.packageName + "/" + info.name
                        + ": it does not require the permission "
                        + mConfig.permission);
                continue;
            }
            services.add(info);
        }
        return services;
    }

    private ManagedServiceSettings.Config getConfig() {
        final ManagedServiceSettings.Config c = new ManagedServiceSettings.Config();
        c.tag = TAG;
        c.setting = Settings.Secure.ENABLED_NOTIFICATION_ASSISTANT;
        c.intentAction = NotificationAssistantService.SERVICE_INTERFACE;
        c.permission = android.Manifest.permission.BIND_NOTIFICATION_ASSISTANT_SERVICE;
        c.noun = "notification assistant";
        c.warningDialogTitle = R.string.notification_listener_security_warning_title;
        c.warningDialogSummary = R.string.notification_listener_security_warning_summary;
        c.emptyText = R.string.no_notification_listeners;
        return c;
    }
}