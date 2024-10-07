/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.shortcut;

import static com.android.settings.shortcut.Shortcuts.SHORTCUT_PROBE;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Settings.DataUsageSummaryActivity;
import com.android.settings.Settings.TetherSettingsActivity;
import com.android.settings.Settings.WifiTetherSettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.gestures.OneHandedSettingsUtils;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.WifiUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link BasePreferenceController} that populates a list of widgets that Settings app support.
 */
public class CreateShortcutPreferenceController extends BasePreferenceController {

    private static final String TAG = "CreateShortcutPrefCtrl";

    private final ShortcutManager mShortcutManager;
    private final PackageManager mPackageManager;
    private final ConnectivityManager mConnectivityManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private Activity mHost;

    public CreateShortcutPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mShortcutManager = context.getSystemService(ShortcutManager.class);
        mPackageManager = context.getPackageManager();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory()
                .getMetricsFeatureProvider();
    }

    public void setActivity(Activity host) {
        mHost = host;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof PreferenceGroup)) {
            return;
        }
        final PreferenceGroup group = (PreferenceGroup) preference;
        group.removeAll();
        final List<ResolveInfo> shortcuts = queryShortcuts();
        final Context uiContext = preference.getContext();
        if (shortcuts.isEmpty()) {
            return;
        }
        PreferenceCategory category = new PreferenceCategory(uiContext);
        group.addPreference(category);
        int bucket = 0;
        for (ResolveInfo info : shortcuts) {
            // Priority is not consecutive (aka, jumped), add a divider between prefs.
            final int currentBucket = info.priority / 10;
            boolean needDivider = currentBucket != bucket;
            bucket = currentBucket;
            if (needDivider) {
                // add a new Category
                category = new PreferenceCategory(uiContext);
                group.addPreference(category);
            }

            final Preference pref = new Preference(uiContext);
            pref.setTitle(info.loadLabel(mPackageManager));
            pref.setKey(info.activityInfo.getComponentName().flattenToString());
            pref.setOnPreferenceClickListener(clickTarget -> {
                if (mHost == null) {
                    return false;
                }
                final Intent shortcutIntent = createResultIntent(info);
                mHost.setResult(Activity.RESULT_OK, shortcutIntent);
                logCreateShortcut(info);
                mHost.finish();
                return true;
            });
            category.addPreference(pref);
        }
    }

    /**
     * Create {@link Intent} that will be consumed by ShortcutManager, which later generates a
     * launcher widget using this intent.
     */
    @VisibleForTesting
    Intent createResultIntent(ResolveInfo resolveInfo) {
        ShortcutInfo info = Shortcuts.createShortcutInfo(mContext, resolveInfo);
        Intent intent = mShortcutManager.createShortcutResultIntent(info);
        if (intent == null) {
            intent = new Intent();
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(mContext, R.mipmap.ic_launcher_settings))
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, info.getIntent())
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, info.getShortLabel());

        final ActivityInfo activityInfo = resolveInfo.activityInfo;
        if (activityInfo.icon != 0) {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, Shortcuts.createIcon(
                    mContext,
                    activityInfo.applicationInfo,
                    activityInfo.icon,
                    R.layout.shortcut_badge,
                    mContext.getResources().getDimensionPixelSize(R.dimen.shortcut_size)));
        }
        return intent;
    }

    /**
     * Finds all shortcut supported by Settings.
     */
    @VisibleForTesting
    List<ResolveInfo> queryShortcuts() {
        final List<ResolveInfo> shortcuts = new ArrayList<>();
        final List<ResolveInfo> activities = mPackageManager.queryIntentActivities(SHORTCUT_PROBE,
                PackageManager.GET_META_DATA);

        if (activities == null) {
            return null;
        }
        for (ResolveInfo info : activities) {
            if (info.activityInfo.name.contains(
                    Settings.OneHandedSettingsActivity.class.getSimpleName())) {
                if (!OneHandedSettingsUtils.isSupportOneHandedMode()) {
                    continue;
                }
            }
            if (info.activityInfo.name.endsWith(TetherSettingsActivity.class.getSimpleName())) {
                if (!mConnectivityManager.isTetheringSupported()) {
                    continue;
                }
            }
            if (info.activityInfo.name.endsWith(WifiTetherSettingsActivity.class.getSimpleName())) {
                if (!canShowWifiHotspot()) {
                    Log.d(TAG, "Skipping Wi-Fi hotspot settings:" + info.activityInfo);
                    continue;
                }
            }
            if (!info.activityInfo.applicationInfo.isSystemApp()) {
                Log.d(TAG, "Skipping non-system app: " + info.activityInfo);
                continue;
            }
            if (info.activityInfo.name.endsWith(DataUsageSummaryActivity.class.getSimpleName())) {
                if (!canShowDataUsage()) {
                    Log.d(TAG, "Skipping data usage settings:" + info.activityInfo);
                    continue;
                }
            }
            shortcuts.add(info);
        }
        Collections.sort(shortcuts, SHORTCUT_COMPARATOR);
        return shortcuts;
    }

    @VisibleForTesting
    boolean canShowDataUsage() {
        return SubscriptionUtil.isSimHardwareVisible(mContext)
                && !MobileNetworkUtils.isMobileNetworkUserRestricted(mContext);
    }

    @VisibleForTesting
    boolean canShowWifiHotspot() {
        return WifiUtils.canShowWifiHotspot(mContext);
    }

    private void logCreateShortcut(ResolveInfo info) {
        if (info == null || info.activityInfo == null) {
            return;
        }
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_SETTINGS_CREATE_SHORTCUT,
                info.activityInfo.name);
    }

    private static final Comparator<ResolveInfo> SHORTCUT_COMPARATOR =
            (i1, i2) -> i1.priority - i2.priority;
}
