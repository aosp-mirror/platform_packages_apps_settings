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
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MagnificationPreferenceFragment extends SettingsPreferenceFragment implements
        Indexable {

    // Settings App preference keys
    private static final String PREFERENCE_TITLE_KEY = "magnification_preference_screen_title";
    private static final String MAGNIFICATION_GESTURES_PREFERENCE_SCREEN_KEY =
            "screen_magnification_gestures_preference_screen";
    private static final String MAGNIFICATION_NAVBAR_PREFERENCE_SCREEN_KEY =
            "screen_magnification_navbar_preference_screen";

    // Pseudo ComponentName used to represent navbar magnification in Settings.Secure.
    private static final String MAGNIFICATION_COMPONENT_ID =
            "com.android.server.accessibility.MagnificationController";

    private Preference mMagnificationGesturesPreference;
    private Preference mMagnificationNavbarPreference;

    private boolean mLaunchedFromSuw = false;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.accessibility_magnification_settings);
        mMagnificationGesturesPreference = findPreference(
                MAGNIFICATION_GESTURES_PREFERENCE_SCREEN_KEY);
        mMagnificationNavbarPreference = findPreference(MAGNIFICATION_NAVBAR_PREFERENCE_SCREEN_KEY);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_magnification;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle args = getArguments();
        if ((args != null) && args.containsKey(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW)) {
            mLaunchedFromSuw = args.getBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.accessibility_screen_magnification_title);
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                mMagnificationGesturesPreference);
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED,
                mMagnificationNavbarPreference);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_SCREEN_MAGNIFICATION_SETTINGS;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mLaunchedFromSuw) {
            // If invoked from SUW, redirect to fragment instrumented for Vision Settings metrics
            preference.setFragment(
                    ToggleScreenMagnificationPreferenceFragmentForSetupWizard.class.getName());
        }
        if (mMagnificationGesturesPreference == preference) {
            handleMagnificationGesturesPreferenceScreenClick();
            super.onPreferenceTreeClick(mMagnificationGesturesPreference);
            return true;
        } else if (mMagnificationNavbarPreference == preference) {
            handleMagnificationNavbarPreferenceScreenClick();
            super.onPreferenceTreeClick(mMagnificationNavbarPreference);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void updateFeatureSummary(String prefKey, Preference pref) {
        if (!mLaunchedFromSuw) {
            final boolean enabled = Settings.Secure.getInt(getContentResolver(), prefKey, 0) == 1;
            pref.setSummary(enabled ? R.string.accessibility_feature_state_on
                    : R.string.accessibility_feature_state_off);
        } else {
            if (Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED.equals(prefKey)) {
                pref.setSummary(R.string.accessibility_screen_magnification_short_summary);
            } else if (Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED.equals(
                    prefKey)) {
                pref.setSummary(R.string.accessibility_screen_magnification_navbar_short_summary);
            }
        }
    }

    private void handleMagnificationGesturesPreferenceScreenClick() {
        Bundle extras = mMagnificationGesturesPreference.getExtras();
        populateMagnificationGesturesPreferenceExtras(extras, getContext());
        extras.putBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW, mLaunchedFromSuw);
    }

    private void handleMagnificationNavbarPreferenceScreenClick() {
        Bundle extras = mMagnificationNavbarPreference.getExtras();
        extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED);
        extras.putString(AccessibilitySettings.EXTRA_TITLE, getString(
                R.string.accessibility_screen_magnification_navbar_title));
        extras.putCharSequence(AccessibilitySettings.EXTRA_SUMMARY,
                getActivity().getResources().getText(
                        R.string.accessibility_screen_magnification_navbar_summary));
        extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0)
                        == 1);
        extras.putBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW, mLaunchedFromSuw);
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
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGET_COMPONENT);
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
                    return context.getString(
                            R.string.accessibility_screen_magnification_navbar_configuration_warning,
                            assignedServiceName);
                }
            }
        }
        return null;
    }

    static void populateMagnificationGesturesPreferenceExtras(Bundle extras, Context context) {
        extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED);
        extras.putString(AccessibilitySettings.EXTRA_TITLE, context.getString(
                R.string.accessibility_screen_magnification_gestures_title));
        extras.putCharSequence(AccessibilitySettings.EXTRA_SUMMARY, context.getResources().getText(
                R.string.accessibility_screen_magnification_summary));
        extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED,
                Settings.Secure.getInt(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1);
        extras.putInt(AccessibilitySettings.EXTRA_VIDEO_RAW_RESOURCE_ID,
                R.raw.accessibility_screen_magnification);
    }

    /**
     * @return {@code true} if this fragment should be shown, {@code false} otherwise. This
     * fragment is shown in the case that more than one magnification mode is available.
     */
    static boolean isApplicable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    if (isApplicable(context.getResources())) {
                        final SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.accessibility_magnification_settings;
                        return Arrays.asList(sir);
                    } else {
                        return Collections.emptyList();
                    }
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(PREFERENCE_TITLE_KEY);
                    return keys;
                }
            };
}
