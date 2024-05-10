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

package com.android.settings.accessibility;

import static android.app.Activity.RESULT_CANCELED;

import static com.android.settings.Utils.getAdaptiveIcon;
import static com.android.settingslib.widget.TwoTargetPreference.ICON_SIZE_MEDIUM;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.RestrictedPreference;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

import java.util.List;

/**
 * Activity with the accessibility settings specific to Setup Wizard.
 */
public class AccessibilitySettingsForSetupWizard extends DashboardFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "AccessibilitySettingsForSetupWizard";

    // Preferences.
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE =
            "screen_magnification_preference";
    private static final String SCREEN_READER_PREFERENCE = "screen_reader_preference";
    private static final String SELECT_TO_SPEAK_PREFERENCE = "select_to_speak_preference";

    // Package names and service names used to identify screen reader and SelectToSpeak services.
    @VisibleForTesting
    static final String SCREEN_READER_PACKAGE_NAME = "com.google.android.marvin.talkback";
    @VisibleForTesting
    static final String SCREEN_READER_SERVICE_NAME =
            "com.google.android.marvin.talkback.TalkBackService";
    @VisibleForTesting
    static final String SELECT_TO_SPEAK_PACKAGE_NAME = "com.google.android.marvin.talkback";
    @VisibleForTesting
    static final String SELECT_TO_SPEAK_SERVICE_NAME =
            "com.google.android.accessibility.selecttospeak.SelectToSpeakService";

    // Preference controls.
    protected Preference mDisplayMagnificationPreference;
    protected RestrictedPreference mScreenReaderPreference;
    protected RestrictedPreference mSelectToSpeakPreference;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SUW_ACCESSIBILITY;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            final String title = getContext().getString(R.string.vision_settings_title);
            final String description = getContext().getString(R.string.vision_settings_description);
            final Drawable icon = getContext().getDrawable(R.drawable.ic_accessibility_visibility);
            AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(getContext(), layout, title,
                    description, icon);

            final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
            AccessibilitySetupWizardUtils.setPrimaryButton(getContext(), mixin, R.string.done,
                    () -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        if (parent instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mDisplayMagnificationPreference = findPreference(DISPLAY_MAGNIFICATION_PREFERENCE);
        mScreenReaderPreference = findPreference(SCREEN_READER_PREFERENCE);
        mSelectToSpeakPreference = findPreference(SELECT_TO_SPEAK_PREFERENCE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccessibilityServicePreference(mScreenReaderPreference,
                SCREEN_READER_PACKAGE_NAME, SCREEN_READER_SERVICE_NAME);
        updateAccessibilityServicePreference(mSelectToSpeakPreference,
                SELECT_TO_SPEAK_PACKAGE_NAME, SELECT_TO_SPEAK_SERVICE_NAME);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mDisplayMagnificationPreference == preference) {
            Bundle extras = mDisplayMagnificationPreference.getExtras();
            extras.putBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW, true);
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_settings_for_setup_wizard;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    /**
     * Returns accessibility service info by given package name and service name.
     *
     * @param packageName Package of accessibility service
     * @param serviceName Class of accessibility service
     * @return {@link AccessibilityServiceInfo} instance if available, null otherwise.
     */
    private AccessibilityServiceInfo findService(String packageName, String serviceName) {
        final AccessibilityManager manager =
                getActivity().getSystemService(AccessibilityManager.class);
        final List<AccessibilityServiceInfo> accessibilityServices =
                manager.getInstalledAccessibilityServiceList();
        for (AccessibilityServiceInfo info : accessibilityServices) {
            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            if (TextUtils.equals(packageName, serviceInfo.packageName)
                    && TextUtils.equals(serviceName, serviceInfo.name)) {
                return info;
            }
        }

        return null;
    }

    private void updateAccessibilityServicePreference(RestrictedPreference preference,
            String packageName, String serviceName) {
        final AccessibilityServiceInfo info = findService(packageName, serviceName);
        if (info == null) {
            getPreferenceScreen().removePreference(preference);
            return;
        }

        final ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
        final Drawable icon = info.getResolveInfo().loadIcon(getPackageManager());
        preference.setIcon(getAdaptiveIcon(getContext(), icon, Color.WHITE));
        preference.setIconSize(ICON_SIZE_MEDIUM);
        final String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();
        preference.setTitle(title);
        final ComponentName componentName =
                new ComponentName(serviceInfo.packageName, serviceInfo.name);
        preference.setKey(componentName.flattenToString());

        // Update the extras.
        final Bundle extras = preference.getExtras();
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, componentName);

        extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
            preference.getKey());
        extras.putString(AccessibilitySettings.EXTRA_TITLE, title);

        final String description = info.loadDescription(getPackageManager());
        extras.putString(AccessibilitySettings.EXTRA_SUMMARY, description);

        extras.putInt(AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES, info.getAnimatedImageRes());

        final String htmlDescription = info.loadHtmlDescription(getPackageManager());
        extras.putString(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, htmlDescription);
    }
}
