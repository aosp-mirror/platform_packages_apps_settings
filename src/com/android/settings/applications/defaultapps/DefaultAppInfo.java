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

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;

/**
 * Data model representing an app in DefaultAppPicker UI.
 */
public class DefaultAppInfo {

    public final int userId;
    public final ComponentName componentName;
    public final PackageItemInfo packageItemInfo;
    public final String summary;
    // Description for why this item is disabled, if null, the item is enabled.
    public final String disabledDescription;
    public final boolean enabled;

    public DefaultAppInfo(int uid, ComponentName cn) {
        this(uid, cn, null /* summary */);
    }

    public DefaultAppInfo(int uid, ComponentName cn, String summary) {
        this(uid, cn, summary, true /* enabled */);
    }

    public DefaultAppInfo(int uid, ComponentName cn, String summary, boolean enabled) {
        packageItemInfo = null;
        userId = uid;
        componentName = cn;
        this.summary = summary;
        this.disabledDescription = null;
        this.enabled = enabled;
    }

    public DefaultAppInfo(PackageItemInfo info, String description) {
        userId = UserHandle.myUserId();
        packageItemInfo = info;
        componentName = null;
        summary = null;
        this.disabledDescription = description;
        enabled = true;
    }

    public DefaultAppInfo(PackageItemInfo info) {
        this(info, null);
    }

    public CharSequence loadLabel(PackageManager pm) {
        if (componentName != null) {
            try {
                final ActivityInfo actInfo = AppGlobals.getPackageManager().getActivityInfo(
                        componentName, 0, userId);
                if (actInfo != null) {
                    return actInfo.loadLabel(pm);
                } else {
                    final ApplicationInfo appInfo = pm.getApplicationInfoAsUser(
                            componentName.getPackageName(), 0, userId);
                    return appInfo.loadLabel(pm);
                }
            } catch (RemoteException | PackageManager.NameNotFoundException e) {
                return null;
            }
        } else if (packageItemInfo != null) {
            return packageItemInfo.loadLabel(pm);
        } else {
            return null;
        }

    }

    public Drawable loadIcon(PackageManager pm) {
        if (componentName != null) {
            try {
                final ActivityInfo actInfo = AppGlobals.getPackageManager().getActivityInfo(
                        componentName, 0, userId);
                if (actInfo != null) {
                    return actInfo.loadIcon(pm);
                } else {
                    final ApplicationInfo appInfo = pm.getApplicationInfoAsUser(
                            componentName.getPackageName(), 0, userId);
                    return appInfo.loadIcon(pm);
                }
            } catch (RemoteException | PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        if (packageItemInfo != null) {
            return packageItemInfo.loadIcon(pm);
        } else {
            return null;
        }
    }

    public String getKey() {
        if (componentName != null) {
            return componentName.flattenToString();
        } else if (packageItemInfo != null) {
            return packageItemInfo.packageName;
        } else {
            return null;
        }
    }
}
