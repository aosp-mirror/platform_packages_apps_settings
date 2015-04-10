/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Slog;

import com.android.settings.notification.ManagedServiceSettings.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ServiceListing {
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final Config mConfig;
    private final HashSet<ComponentName> mEnabledServices = new HashSet<ComponentName>();
    private final List<ServiceInfo> mServices = new ArrayList<ServiceInfo>();
    private final List<Callback> mCallbacks = new ArrayList<Callback>();

    private boolean mListening;

    public ServiceListing(Context context, Config config) {
        mContext = context;
        mConfig = config;
        mContentResolver = context.getContentResolver();
    }

    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (mListening) {
            // listen for package changes
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addDataScheme("package");
            mContext.registerReceiver(mPackageReceiver, filter);
            mContentResolver.registerContentObserver(Settings.Secure.getUriFor(mConfig.setting),
                    false, mSettingsObserver);
        } else {
            mContext.unregisterReceiver(mPackageReceiver);
            mContentResolver.unregisterContentObserver(mSettingsObserver);
        }
    }

    public static int getEnabledServicesCount(Config config, Context context) {
        final String flat = Settings.Secure.getString(context.getContentResolver(), config.setting);
        if (flat == null || "".equals(flat)) return 0;
        final String[] components = flat.split(":");
        return components.length;
    }

    public static int getServicesCount(Config c, PackageManager pm) {
        return getServices(c, null, pm);
    }

    public static ServiceInfo findService(Context context, Config config, final ComponentName cn) {
        final ServiceListing listing = new ServiceListing(context, config);
        final List<ServiceInfo> services = listing.reload();
        for (ServiceInfo service : services) {
            final ComponentName serviceCN = new ComponentName(service.packageName, service.name);
            if (serviceCN.equals(cn)) {
                return service;
            }
        }
        return null;
    }

    private static int getServices(Config c, List<ServiceInfo> list, PackageManager pm) {
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

    private void saveEnabledServices() {
        StringBuilder sb = null;
        for (ComponentName cn : mEnabledServices) {
            if (sb == null) {
                sb = new StringBuilder();
            } else {
                sb.append(':');
            }
            sb.append(cn.flattenToString());
        }
        Settings.Secure.putString(mContentResolver, mConfig.setting,
                sb != null ? sb.toString() : "");
    }

    private void loadEnabledServices() {
        mEnabledServices.clear();
        final String flat = Settings.Secure.getString(mContentResolver, mConfig.setting);
        if (flat != null && !"".equals(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    mEnabledServices.add(cn);
                }
            }
        }
    }

    public List<ServiceInfo> reload() {
        loadEnabledServices();
        getServices(mConfig, mServices, mContext.getPackageManager());
        for (Callback callback : mCallbacks) {
            callback.onServicesReloaded(mServices);
        }
        return mServices;
    }

    public boolean isEnabled(ComponentName cn) {
        return mEnabledServices.contains(cn);
    }

    public void setEnabled(ComponentName cn, boolean enabled) {
        if (enabled) {
            mEnabledServices.add(cn);
        } else {
            mEnabledServices.remove(cn);
        }
        saveEnabledServices();
    }

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            reload();
        }
    };

    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reload();
        }
    };

    public interface Callback {
        void onServicesReloaded(List<ServiceInfo> services);
    }
}
