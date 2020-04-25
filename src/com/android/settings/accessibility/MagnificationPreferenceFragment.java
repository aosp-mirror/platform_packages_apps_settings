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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

/** Settings fragment containing magnification preference. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public final class MagnificationPreferenceFragment extends DashboardFragment {
    @VisibleForTesting static final int ON = 1;
    @VisibleForTesting static final int OFF = 0;

    private static final String TAG = "MagnificationPreferenceFragment";

    // Settings App preference keys
    private static final String PREFERENCE_TITLE_KEY = "magnification_preference_screen_title";

    // Pseudo ComponentName used to represent navbar magnification in Settings.Secure.
    private static final String MAGNIFICATION_COMPONENT_ID =
            "com.android.server.accessibility.MagnificationController";

    private boolean mLaunchedFromSuw = false;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_SCREEN_MAGNIFICATION_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_magnification;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_magnification_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Bundle args = getArguments();
        if ((args != null) && args.containsKey(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW)) {
            mLaunchedFromSuw = args.getBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW);
        }
        use(MagnificationGesturesPreferenceController.class)
                .setIsFromSUW(mLaunchedFromSuw);
        use(MagnificationNavbarPreferenceController.class)
                .setIsFromSUW(mLaunchedFromSuw);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mLaunchedFromSuw) {
            // If invoked from SUW, redirect to fragment instrumented for Vision Settings metrics
            preference.setFragment(
                    ToggleScreenMagnificationPreferenceFragmentForSetupWizard.class.getName());
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mLaunchedFromSuw) {
            // Do not call super. We don't want to see the "Help & feedback" on OOBE page.
        } else {
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    static CharSequence getConfigurationWarningStringForSecureSettingsKey(String key,
            Context context) {
        if (!Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED.equals(key)) {
            return null;
        }
        if (Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0) == 0) {
            return null;
        }
        final AccessibilityManager am = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE);
        final String assignedId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
        if (!TextUtils.isEmpty(assignedId) && !MAGNIFICATION_COMPONENT_ID.equals(assignedId)) {
            final ComponentName assignedComponentName = ComponentName.unflattenFromString(
                    assignedId);
            final List<AccessibilityServiceInfo> activeServices =
                    am.getEnabledAccessibilityServiceList(
                            AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            final int serviceCount = activeServices.size();
            for (int i = 0; i < serviceCount; i++) {
                final AccessibilityServiceInfo info = activeServices.get(i);
                if (info.getComponentName().equals(assignedComponentName)) {
                    final CharSequence assignedServiceName = info.getResolveInfo().loadLabel(
                            context.getPackageManager());
                    final int messageId = AccessibilityUtil.isGestureNavigateEnabled(context)
                            ? R.string.accessibility_screen_magnification_gesture_navigation_warning
                            : R.string.accessibility_screen_magnification_navbar_configuration_warning;
                    return context.getString(messageId, assignedServiceName);
                }
            }
        }
        return null;
    }

    static boolean isChecked(ContentResolver contentResolver, String settingsKey) {
        return Settings.Secure.getInt(contentResolver, settingsKey, OFF) == ON;
    }

    static boolean setChecked(ContentResolver contentResolver, String settingsKey,
            boolean isChecked) {
        return Settings.Secure.putInt(contentResolver, settingsKey, isChecked ? ON : OFF);
    }

    /**
     * @return {@code true} if this fragment should be shown, {@code false} otherwise. This
     * fragment is shown in the case that more than one magnification mode is available.
     */
    static boolean isApplicable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_magnification_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isApplicable(context.getResources());
                }
            };
}
