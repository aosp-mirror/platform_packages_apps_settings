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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.Settings.TetherSettingsActivity;
import com.android.settings.Settings.WifiSettings2Activity;
import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
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

    static final String SHORTCUT_ID_PREFIX = "component-shortcut-";
    static final Intent SHORTCUT_PROBE = new Intent(Intent.ACTION_MAIN)
            .addCategory("com.android.settings.SHORTCUT")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

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
        mMetricsFeatureProvider = FeatureFactory.getFactory(context)
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
                final Intent shortcutIntent = createResultIntent(
                        buildShortcutIntent(info),
                        info, clickTarget.getTitle());
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
    Intent createResultIntent(Intent shortcutIntent, ResolveInfo resolveInfo,
            CharSequence label) {
        ShortcutInfo info = createShortcutInfo(mContext, shortcutIntent, resolveInfo, label);
        Intent intent = mShortcutManager.createShortcutResultIntent(info);
        if (intent == null) {
            intent = new Intent();
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(mContext, R.mipmap.ic_launcher_settings))
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, label);

        final ActivityInfo activityInfo = resolveInfo.activityInfo;
        if (activityInfo.icon != 0) {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, createIcon(
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
            if (info.activityInfo.name.endsWith(TetherSettingsActivity.class.getSimpleName())) {
                if (!mConnectivityManager.isTetheringSupported()) {
                    continue;
                }
            }
            if (!info.activityInfo.applicationInfo.isSystemApp()) {
                Log.d(TAG, "Skipping non-system app: " + info.activityInfo);
                continue;
            }
            if (FeatureFlagUtils.isEnabled(mContext, FeatureFlagUtils.SETTINGS_WIFITRACKER2)) {
                if (info.activityInfo.name.endsWith(WifiSettingsActivity.class.getSimpleName())) {
                    continue;
                }
            } else {
                if (info.activityInfo.name.endsWith(WifiSettings2Activity.class.getSimpleName())) {
                    continue;
                }
            }
            shortcuts.add(info);
        }
        Collections.sort(shortcuts, SHORTCUT_COMPARATOR);
        return shortcuts;
    }

    private void logCreateShortcut(ResolveInfo info) {
        if (info == null || info.activityInfo == null) {
            return;
        }
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_SETTINGS_CREATE_SHORTCUT,
                info.activityInfo.name);
    }

    private static Intent buildShortcutIntent(ResolveInfo info) {
        return new Intent(SHORTCUT_PROBE)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setClassName(info.activityInfo.packageName, info.activityInfo.name);
    }

    private static ShortcutInfo createShortcutInfo(Context context, Intent shortcutIntent,
            ResolveInfo resolveInfo, CharSequence label) {
        final ActivityInfo activityInfo = resolveInfo.activityInfo;

        final Icon maskableIcon;
        if (activityInfo.icon != 0 && activityInfo.applicationInfo != null) {
            maskableIcon = Icon.createWithAdaptiveBitmap(createIcon(
                    context,
                    activityInfo.applicationInfo, activityInfo.icon,
                    R.layout.shortcut_badge_maskable,
                    context.getResources().getDimensionPixelSize(R.dimen.shortcut_size_maskable)));
        } else {
            maskableIcon = Icon.createWithResource(context, R.drawable.ic_launcher_settings);
        }
        final String shortcutId = SHORTCUT_ID_PREFIX +
                shortcutIntent.getComponent().flattenToShortString();
        return new ShortcutInfo.Builder(context, shortcutId)
                .setShortLabel(label)
                .setIntent(shortcutIntent)
                .setIcon(maskableIcon)
                .build();
    }

    private static Bitmap createIcon(Context context, ApplicationInfo app, int resource,
            int layoutRes, int size) {
        final Context themedContext = new ContextThemeWrapper(context,
                android.R.style.Theme_Material);
        final View view = LayoutInflater.from(themedContext).inflate(layoutRes, null);
        final int spec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
        view.measure(spec, spec);
        final Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        Drawable iconDrawable;
        try {
            iconDrawable = context.getPackageManager().getResourcesForApplication(app)
                    .getDrawable(resource, themedContext.getTheme());
            if (iconDrawable instanceof LayerDrawable) {
                iconDrawable = ((LayerDrawable) iconDrawable).getDrawable(1);
            }
            ((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(iconDrawable);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Cannot load icon from app " + app + ", returning a default icon");
            Icon icon = Icon.createWithResource(context, R.drawable.ic_launcher_settings);
            ((ImageView) view.findViewById(android.R.id.icon)).setImageIcon(icon);
        }

        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        return bitmap;
    }

    public static void updateRestoredShortcuts(Context context) {
        ShortcutManager sm = context.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> updatedShortcuts = new ArrayList<>();
        for (ShortcutInfo si : sm.getPinnedShortcuts()) {
            if (si.getId().startsWith(SHORTCUT_ID_PREFIX)) {
                ResolveInfo ri = context.getPackageManager().resolveActivity(si.getIntent(), 0);

                if (ri != null) {
                    updatedShortcuts.add(createShortcutInfo(context, buildShortcutIntent(ri), ri,
                            si.getShortLabel()));
                }
            }
        }
        if (!updatedShortcuts.isEmpty()) {
            sm.updateShortcuts(updatedShortcuts);
        }
    }

    private static final Comparator<ResolveInfo> SHORTCUT_COMPARATOR =
            (i1, i2) -> i1.priority - i2.priority;
}
