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

package com.android.settings.support;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;

import java.util.List;

public class NewDeviceIntroSuggestionActivity extends Activity {

    private static final String TAG = "NewDeviceIntroSugg";
    @VisibleForTesting
    static final String PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME =
            "pref_new_device_intro_suggestion_first_display_time_ms";
    @VisibleForTesting
    static final String PREF_KEY_SUGGGESTION_COMPLETE =
            "pref_new_device_intro_suggestion_complete";
    @VisibleForTesting
    static final long PERMANENT_DISMISS_THRESHOLD = DateUtils.DAY_IN_MILLIS * 14;

    public static final String TIPS_PACKAGE_NAME = "com.google.android.apps.tips";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getLaunchIntent(this);
        if (intent != null) {
            final SuggestionFeatureProvider featureProvider = FeatureFactory.getFactory(this)
                    .getSuggestionFeatureProvider(this);
            final SharedPreferences prefs = featureProvider.getSharedPrefs(this);
            prefs.edit().putBoolean(PREF_KEY_SUGGGESTION_COMPLETE, true).commit();
            startActivity(intent);
        }
        finish();
    }

    public static boolean isSuggestionComplete(Context context) {
        // Always returns 'true' if Tips application exists. Check b/77652536 for more details.
        return isTipsInstalledAsSystemApp(context)
                || !isSupported(context)
                || isExpired(context)
                || hasLaunchedBefore(context)
                || !canOpenUrlInBrowser(context);
    }

    private static boolean isSupported(Context context) {
        return context.getResources()
                .getBoolean(R.bool.config_new_device_intro_suggestion_supported);
    }

    private static boolean isExpired(Context context) {
        final SuggestionFeatureProvider featureProvider = FeatureFactory.getFactory(context)
                .getSuggestionFeatureProvider(context);
        final SharedPreferences prefs = featureProvider.getSharedPrefs(context);
        final long currentTimeMs = System.currentTimeMillis();
        final long firstDisplayTimeMs;

        if (!prefs.contains(PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME)) {
            firstDisplayTimeMs = currentTimeMs;
            prefs.edit().putLong(PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME, currentTimeMs).commit();
        } else {
            firstDisplayTimeMs = prefs.getLong(PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME, -1);
        }

        final long dismissTimeMs = firstDisplayTimeMs + PERMANENT_DISMISS_THRESHOLD;

        final boolean expired = currentTimeMs > dismissTimeMs;

        Log.d(TAG, "is suggestion expired: " + expired);
        return expired;
    }

    private static boolean canOpenUrlInBrowser(Context context) {
        final Intent intent = getLaunchIntent(context);
        if (intent == null) {
            // No url/intent to launch.
            return false;
        }
        // Make sure we can handle the intent.
        final List<ResolveInfo> resolveInfos =
                context.getPackageManager().queryIntentActivities(intent, 0);
        return resolveInfos != null && resolveInfos.size() != 0;
    }

    private static boolean hasLaunchedBefore(Context context) {
        final SuggestionFeatureProvider featureProvider = FeatureFactory.getFactory(context)
                .getSuggestionFeatureProvider(context);
        final SharedPreferences prefs = featureProvider.getSharedPrefs(context);
        return prefs.getBoolean(PREF_KEY_SUGGGESTION_COMPLETE, false);
    }

    @VisibleForTesting
    static Intent getLaunchIntent(Context context) {
        final SupportFeatureProvider supportProvider = FeatureFactory.getFactory(context)
                .getSupportFeatureProvider(context);
        if (supportProvider == null) {
            return null;
        }
        final String url = supportProvider.getNewDeviceIntroUrl(context);
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        return new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(url));
    }

    /**
     * Check if the specified package exists and is marked with <i>FLAG_SYSTEM</i>
     */
    private static boolean isTipsInstalledAsSystemApp(@NonNull Context context) {
        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(TIPS_PACKAGE_NAME,
                    PackageManager.MATCH_SYSTEM_ONLY);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Cannot find the package: " + TIPS_PACKAGE_NAME, e);
            return false;
        }
    }
}
