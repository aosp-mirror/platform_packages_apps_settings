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
package com.android.settings.utils;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ZenServiceListing {

    private final Context mContext;
    private final ManagedServiceSettings.Config mConfig;
    private final Set<ServiceInfo> mApprovedServices = new ArraySet<ServiceInfo>();
    private final List<Callback> mZenCallbacks = new ArrayList<>();
    private final NotificationManager mNm;

    public ZenServiceListing(Context context, ManagedServiceSettings.Config config) {
        mContext = context;
        mConfig = config;
        mNm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public ServiceInfo findService(final ComponentName cn) {
        for (ServiceInfo service : mApprovedServices) {
            final ComponentName serviceCN = new ComponentName(service.packageName, service.name);
            if (serviceCN.equals(cn)) {
                return service;
            }
        }
        return null;
    }

    public void addZenCallback(Callback callback) {
        mZenCallbacks.add(callback);
    }

    public void removeZenCallback(Callback callback) {
        mZenCallbacks.remove(callback);
    }

    public void reloadApprovedServices() {
        mApprovedServices.clear();

        List<String> enabledNotificationListenerPkgs = mNm.getEnabledNotificationListenerPackages();
        List<ServiceInfo> services = new ArrayList<>();
        getServices(mConfig, services, mContext.getPackageManager());
        for (ServiceInfo service : services) {
            final String servicePackage = service.getComponentName().getPackageName();
            if (mNm.isNotificationPolicyAccessGrantedForPackage(servicePackage)
                || enabledNotificationListenerPkgs.contains(servicePackage)) {
                mApprovedServices.add(service);
            }
        }

        if (!mApprovedServices.isEmpty()) {
            for (Callback callback : mZenCallbacks) {
                callback.onServicesReloaded(mApprovedServices);
            }
        }
    }

    private static int getServices(ManagedServiceSettings.Config c, List<ServiceInfo> list,
            PackageManager pm) {
        int services = 0;
        if (list != null) {
            list.clear();
        }
        final int user = ActivityManager.getCurrentUser();

        List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(
                new Intent(c.intentAction),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA,
                user);

        for (int i = 0, count = installedServices.size(); i < count; i++) {
            ResolveInfo resolveInfo = installedServices.get(i);
            ServiceInfo info = resolveInfo.serviceInfo;

            if (!c.permission.equals(info.permission)) {
                Slog.w(c.tag, "Skipping " + c.noun + " service "
                        + info.packageName + "/" + info.name
                        + ": it does not require the permission "
                        + c.permission);
                continue;
            }
            if (list != null) {
                list.add(info);
            }
            services++;
        }
        return services;
    }

    public interface Callback {
        void onServicesReloaded(Set<ServiceInfo> services);
    }
}
