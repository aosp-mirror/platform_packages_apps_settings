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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.ArraySet;
import android.util.Slog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ZenServiceListing {

    private final Context mContext;
    private final ManagedServiceSettings.Config mConfig;
    private final Set<ComponentInfo> mApprovedComponents = new ArraySet<>();
    private final List<Callback> mZenCallbacks = new ArrayList<>();
    private final NotificationManager mNm;

    public ZenServiceListing(Context context, ManagedServiceSettings.Config config) {
        mContext = context;
        mConfig = config;
        mNm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public ComponentInfo findService(final ComponentName cn) {
        if (cn == null) {
            return null;
        }
        for (ComponentInfo component : mApprovedComponents) {
            final ComponentName ci = new ComponentName(component.packageName, component.name);
            if (ci.equals(cn)) {
                return component;
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
        mApprovedComponents.clear();

        List<String> enabledNotificationListenerPkgs = mNm.getEnabledNotificationListenerPackages();
        List<ComponentInfo> components = new ArrayList<>();
        getServices(mConfig, components, mContext.getPackageManager());
        getActivities(mConfig, components, mContext.getPackageManager());
        for (ComponentInfo componentInfo : components) {
            final String pkg = componentInfo.getComponentName().getPackageName();
            if (mNm.isNotificationPolicyAccessGrantedForPackage(pkg)
                || enabledNotificationListenerPkgs.contains(pkg)) {
                mApprovedComponents.add(componentInfo);
            }
        }

        if (!mApprovedComponents.isEmpty()) {
            for (Callback callback : mZenCallbacks) {
                callback.onComponentsReloaded(mApprovedComponents);
            }
        }
    }

    private static void getServices(ManagedServiceSettings.Config c, List<ComponentInfo> list,
            PackageManager pm) {
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
        }
    }

    private static void getActivities(ManagedServiceSettings.Config c, List<ComponentInfo> list,
            PackageManager pm) {
        final int user = ActivityManager.getCurrentUser();

        List<ResolveInfo> resolveInfos = pm.queryIntentActivitiesAsUser(
                new Intent(c.configIntentAction),
                PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA,
                user);

        for (int i = 0, count = resolveInfos.size(); i < count; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            ActivityInfo info = resolveInfo.activityInfo;
            if (list != null) {
                list.add(info);
            }
        }
    }

    public interface Callback {
        void onComponentsReloaded(Set<ComponentInfo> components);
    }
}
