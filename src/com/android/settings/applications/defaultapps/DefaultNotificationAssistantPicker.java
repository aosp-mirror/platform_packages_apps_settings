/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.defaultapps;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.service.notification.NotificationAssistantService;
import android.util.Slog;

import com.android.settings.R;
import com.android.settings.utils.ManagedServiceSettings;

import java.util.ArrayList;
import java.util.List;

public class DefaultNotificationAssistantPicker extends DefaultAppPickerFragment {
    private static final String TAG = "DefaultNotiAssist";

    private final ManagedServiceSettings.Config mConfig = getConfig();

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    protected String getDefaultKey() {
        return Settings.Secure.getString(getContext().getContentResolver(), mConfig.setting);
    }

    @Override
    protected boolean setDefaultKey(String value) {
        Settings.Secure.putString(getContext().getContentResolver(), mConfig.setting, value);
        return true;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        List<DefaultAppInfo> candidates = new ArrayList<>();
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

            candidates.add(new DefaultAppInfo(mPm,
                    mUserId, new ComponentName(info.packageName, info.name)));
        }
        return candidates;
    }

    @Override
    protected boolean shouldShowItemNone() {
        return true;
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
