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
 * limitations under the License
 */

package com.android.settings.applications;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;

import com.android.settings.AppListPreference;
import com.android.settings.R;
import com.android.settings.SelfAvailablePreference;
import com.android.settings.Utils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A preference for choosing the default emergency app
 */
public class DefaultEmergencyPreference extends AppListPreference
        implements SelfAvailablePreference {

    private static final boolean DEFAULT_EMERGENCY_APP_IS_CONFIGURABLE = false;
    private final ContentResolver mContentResolver;

    public static final Intent QUERY_INTENT = new Intent(
            TelephonyManager.ACTION_EMERGENCY_ASSISTANCE);

    public DefaultEmergencyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContentResolver = context.getContentResolver();
        load();
    }

    @Override
    protected CharSequence getConfirmationMessage(String value) {
        return Utils.isPackageDirectBootAware(getContext(), value) ? null
                : getContext().getText(R.string.direct_boot_unaware_dialog_message);
    }

    @Override
    protected boolean persistString(String value) {
        String previousValue = Settings.Secure.getString(mContentResolver,
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);

        if (!TextUtils.isEmpty(value) && !Objects.equals(value, previousValue)) {
            Settings.Secure.putString(mContentResolver,
                    Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION,
                    value);
        }
        setSummary(getEntry());
        return true;
    }

    private void load() {
        new AsyncTask<Void, Void, Set<String>>() {
            @Override
            protected Set<String> doInBackground(Void[] params) {
                return resolveAssistPackageAndQueryApps();
            }

            @Override
            protected void onPostExecute(Set<String> entries) {
                String currentPkg = Settings.Secure.getString(mContentResolver,
                        Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);
                setPackageNames(entries.toArray(new String[entries.size()]), currentPkg);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private Set<String> resolveAssistPackageAndQueryApps() {
        Set<String> packages = new ArraySet<>();

        PackageManager packageManager = getContext().getPackageManager();
        List<ResolveInfo> infos = packageManager.queryIntentActivities(QUERY_INTENT, 0);

        PackageInfo bestMatch = null;
        final int size = infos.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo info = infos.get(i);
            if (info == null || info.activityInfo == null
                    || packages.contains(info.activityInfo.packageName)) {
                continue;
            }

            String packageName = info.activityInfo.packageName;

            packages.add(packageName);

            PackageInfo packageInfo;
            try {
                packageInfo = packageManager.getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }

            // Get earliest installed system app.
            if (isSystemApp(packageInfo) && (bestMatch == null ||
                    bestMatch.firstInstallTime > packageInfo.firstInstallTime)) {
                bestMatch = packageInfo;
            }
        }

        String defaultPackage = Settings.Secure.getString(mContentResolver,
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);
        boolean defaultMissing = TextUtils.isEmpty(defaultPackage)
                || !packages.contains(defaultPackage);
        if (bestMatch != null && defaultMissing) {
            Settings.Secure.putString(mContentResolver,
                    Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION,
                    bestMatch.packageName);
        }

        return packages;
    }

    private static boolean isCapable(Context context) {
        return TelephonyManager.EMERGENCY_ASSISTANCE_ENABLED
                && context.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
    }

    private static boolean isSystemApp(PackageInfo info) {
        return info.applicationInfo != null
                && (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public boolean isAvailable(Context context) {
        return DEFAULT_EMERGENCY_APP_IS_CONFIGURABLE
                && isCapable(context)
                && context.getPackageManager().resolveActivity(QUERY_INTENT, 0) != null;
    }

    public static boolean hasEmergencyPreference(String pkg, Context context) {
        Intent i = new Intent(QUERY_INTENT);
        i.setPackage(pkg);
        final List<ResolveInfo> resolveInfos =
                context.getPackageManager().queryIntentActivities(i, 0);
        return resolveInfos != null && resolveInfos.size() != 0;
    }

    public static boolean isEmergencyDefault(String pkg, Context context) {
        String defaultPackage = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION);
        return defaultPackage != null && defaultPackage.equals(pkg);
    }
}
