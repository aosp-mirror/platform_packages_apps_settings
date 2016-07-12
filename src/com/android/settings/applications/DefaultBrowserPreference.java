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

package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.content.PackageMonitor;
import com.android.settings.AppListPreference;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class DefaultBrowserPreference extends AppListPreference {

    private static final String TAG = "DefaultBrowserPref";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final long DELAY_UPDATE_BROWSER_MILLIS = 500;
    private static final Intent BROWSE_PROBE = new Intent()
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("http:"));

    private final Handler mHandler = new Handler();
    private final PackageManager mPm;

    public DefaultBrowserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPm = context.getPackageManager();
        refreshBrowserApps();
    }

    @Override
    public void onAttached() {
        super.onAttached();
        updateDefaultBrowserPreference();
        mPackageMonitor.register(getContext(), getContext().getMainLooper(), false);
    }

    @Override
    public void onDetached() {
        mPackageMonitor.unregister();
        super.onDetached();
    }

    @Override
    protected boolean persistString(String newValue) {
        final CharSequence packageName = newValue;
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        boolean result = mPm.setDefaultBrowserPackageNameAsUser(
                packageName.toString(), mUserId);
        if (result) {
            setSummary("%s");
        }
        return result && super.persistString(newValue);
    }

    public void refreshBrowserApps() {
        List<String> browsers = resolveBrowserApps();

        setPackageNames(browsers.toArray(new String[browsers.size()]), null);
    }

    private void updateDefaultBrowserPreference() {
        refreshBrowserApps();

        final PackageManager pm = getContext().getPackageManager();

        String packageName = pm.getDefaultBrowserPackageNameAsUser(mUserId);
        if (!TextUtils.isEmpty(packageName)) {
            // Check if the default Browser package is still there
            final Intent intent = new Intent(BROWSE_PROBE)
                    .setPackage(packageName);

            final ResolveInfo info = mPm.resolveActivityAsUser(intent, 0, mUserId);
            if (info != null) {
                setValue(packageName);
                setSummary("%s");
            } else {
                setSummary(R.string.default_browser_title_none);
            }
        } else {
            if (DEBUG) Log.d(TAG, "No default browser app.");
            setSoleAppLabelAsSummary();
        }
    }

    private List<String> resolveBrowserApps() {
        List<String> result = new ArrayList<>();

        // Resolve that intent and check that the handleAllWebDataURI boolean is set
        List<ResolveInfo> list = mPm.queryIntentActivitiesAsUser(BROWSE_PROBE,
                PackageManager.MATCH_ALL, mUserId);

        final int count = list.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo == null || result.contains(info.activityInfo.packageName)
                    || !info.handleAllWebDataURI) {
                continue;
            }

            result.add(info.activityInfo.packageName);
        }

        return result;
    }

    @Override
    protected CharSequence getSoleAppLabel() {
        // Resolve that intent and check that the handleAllWebDataURI boolean is set
        List<ResolveInfo> list = mPm.queryIntentActivitiesAsUser(BROWSE_PROBE,
                PackageManager.MATCH_ALL, mUserId);
        if (list.size() == 1) {
            return list.get(0).loadLabel(mPm);
        }
        return null;
    }

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateDefaultBrowserPreference();
        }
    };

    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            mHandler.postDelayed(mUpdateRunnable, DELAY_UPDATE_BROWSER_MILLIS);
        }
    };

    public static boolean hasBrowserPreference(String pkg, Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("http:"));
        intent.setPackage(pkg);
        final List<ResolveInfo> resolveInfos =
                context.getPackageManager().queryIntentActivities(intent, 0);
        return resolveInfos != null && resolveInfos.size() != 0;
    }

    public static boolean isBrowserDefault(String pkg, Context context) {
        String defaultPackage = context.getPackageManager()
                .getDefaultBrowserPackageNameAsUser(UserHandle.myUserId());
        return defaultPackage != null && defaultPackage.equals(pkg);
    }
}
