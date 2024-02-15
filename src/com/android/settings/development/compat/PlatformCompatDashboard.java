/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.compat;

import static com.android.internal.compat.OverrideAllowedState.ALLOWED;

import android.app.settings.SettingsEnums;
import android.compat.Compatibility.ChangeConfig;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.internal.compat.CompatibilityChangeInfo;
import com.android.internal.compat.IPlatformCompat;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dashboard for Platform Compat preferences.
 */
public class PlatformCompatDashboard extends DashboardFragment {
    private static final String TAG = "PlatformCompatDashboard";
    public static final String COMPAT_APP = "compat_app";

    private IPlatformCompat mPlatformCompat;

    private CompatibilityChangeInfo[] mChanges;

    @VisibleForTesting
    String mSelectedApp;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_PLATFORM_COMPAT_DASHBOARD;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.platform_compat_settings;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    IPlatformCompat getPlatformCompat() {
        if (mPlatformCompat == null) {
            mPlatformCompat = IPlatformCompat.Stub
                    .asInterface(ServiceManager.getService(Context.PLATFORM_COMPAT_SERVICE));
        }
        return mPlatformCompat;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        try {
            mChanges = getPlatformCompat().listUIChanges();
        } catch (RemoteException e) {
            throw new RuntimeException("Could not list changes!", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishingOrDestroyed()) {
            return;
        }
        Bundle arguments = getArguments();
        if (arguments == null) {
            finish();
            return;
        }
        mSelectedApp = arguments.getString(COMPAT_APP);
        try {
            final ApplicationInfo applicationInfo = getApplicationInfo();
            addPreferences(applicationInfo);
        } catch (PackageManager.NameNotFoundException ignored) {
            finish();
        }
    }

    private void addPreferences(ApplicationInfo applicationInfo) {
        getPreferenceScreen().removeAll();
        getPreferenceScreen().addPreference(createAppPreference(applicationInfo));
        // Differentiate compatibility changes into default enabled, default disabled and enabled
        // after target sdk.
        final CompatibilityChangeConfig configMappings = getAppChangeMappings();
        final List<CompatibilityChangeInfo> enabledChanges = new ArrayList<>();
        final List<CompatibilityChangeInfo> disabledChanges = new ArrayList<>();
        final Map<Integer, List<CompatibilityChangeInfo>> targetSdkChanges = new TreeMap<>();
        for (CompatibilityChangeInfo change : mChanges) {
            if (change.getEnableSinceTargetSdk() > 0) {
                List<CompatibilityChangeInfo> sdkChanges;
                if (!targetSdkChanges.containsKey(change.getEnableSinceTargetSdk())) {
                    sdkChanges = new ArrayList<>();
                    targetSdkChanges.put(change.getEnableSinceTargetSdk(), sdkChanges);
                } else {
                    sdkChanges = targetSdkChanges.get(change.getEnableSinceTargetSdk());
                }
                sdkChanges.add(change);
            } else if (change.getDisabled()) {
                disabledChanges.add(change);
            } else {
                enabledChanges.add(change);
            }
        }
        createChangeCategoryPreference(enabledChanges, configMappings,
                getString(R.string.platform_compat_default_enabled_title));
        createChangeCategoryPreference(disabledChanges, configMappings,
                getString(R.string.platform_compat_default_disabled_title));
        for (Integer sdk : targetSdkChanges.keySet()) {
            createChangeCategoryPreference(targetSdkChanges.get(sdk), configMappings,
                    getString(R.string.platform_compat_target_sdk_title, sdk));
        }
    }

    private CompatibilityChangeConfig getAppChangeMappings() {
        try {
            final ApplicationInfo applicationInfo = getApplicationInfo();
            return getPlatformCompat().getAppConfig(applicationInfo);
        } catch (RemoteException | PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get app config!", e);
        }
    }

    /**
     * Create a {@link Preference} for a changeId.
     *
     * <p>The {@link Preference} is a toggle switch that can enable or disable the given change for
     * the currently selected app.</p>
     */
    Preference createPreferenceForChange(Context context, CompatibilityChangeInfo change,
            CompatibilityChangeConfig configMappings) {
        final boolean currentValue = configMappings.isChangeEnabled(change.getId());
        final TwoStatePreference item = new SwitchPreferenceCompat(context);
        final String changeName =
                change.getName() != null ? change.getName() : "Change_" + change.getId();
        item.setSummary(changeName);
        item.setKey(changeName);
        boolean shouldEnable = true;
        try {
            shouldEnable = getPlatformCompat().getOverrideValidator()
                           .getOverrideAllowedState(change.getId(), mSelectedApp)
                           .state == ALLOWED;
        } catch (RemoteException e) {
            throw new RuntimeException("Could not check if change can be overridden for app.", e);
        }
        item.setEnabled(shouldEnable);
        item.setChecked(currentValue);
        item.setOnPreferenceChangeListener(
                new CompatChangePreferenceChangeListener(change.getId()));
        return item;
    }

    /**
     * Get {@link ApplicationInfo} for the currently selected app.
     *
     * @return an {@link ApplicationInfo} instance.
     */
    ApplicationInfo getApplicationInfo() throws PackageManager.NameNotFoundException {
        return getPackageManager().getApplicationInfo(mSelectedApp, 0);
    }

    /**
     * Create a {@link Preference} for the selected app.
     *
     * <p>The {@link Preference} contains the icon, package name and target SDK for the selected
     * app. Selecting this preference will also re-trigger the app selection dialog.</p>
     */
    Preference createAppPreference(ApplicationInfo applicationInfo) {
        final Context context = getPreferenceScreen().getContext();
        final Drawable icon = applicationInfo.loadIcon(context.getPackageManager());
        final Preference appPreference = new Preference(context);
        appPreference.setIcon(icon);
        appPreference.setSummary(getString(R.string.platform_compat_selected_app_summary,
                                         mSelectedApp, applicationInfo.targetSdkVersion));
        return appPreference;
    }

    PreferenceCategory createChangeCategoryPreference(List<CompatibilityChangeInfo> changes,
            CompatibilityChangeConfig configMappings, String title) {
        final PreferenceCategory category =
                new PreferenceCategory(getPreferenceScreen().getContext());
        category.setTitle(title);
        getPreferenceScreen().addPreference(category);
        addChangePreferencesToCategory(changes, category, configMappings);
        return category;
    }

    private void addChangePreferencesToCategory(List<CompatibilityChangeInfo> changes,
            PreferenceCategory category, CompatibilityChangeConfig configMappings) {
        for (CompatibilityChangeInfo change : changes) {
            final Preference preference = createPreferenceForChange(getPreferenceScreen().getContext(),
                    change, configMappings);
            category.addPreference(preference);
        }
    }

    private class CompatChangePreferenceChangeListener implements OnPreferenceChangeListener {
        private final long changeId;

        CompatChangePreferenceChangeListener(long changeId) {
            this.changeId = changeId;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            try {
                final ArraySet<Long> enabled = new ArraySet<>();
                final ArraySet<Long> disabled = new ArraySet<>();
                if ((Boolean) newValue) {
                    enabled.add(changeId);
                } else {
                    disabled.add(changeId);
                }
                final CompatibilityChangeConfig overrides =
                        new CompatibilityChangeConfig(new ChangeConfig(enabled, disabled));
                getPlatformCompat().setOverrides(overrides, mSelectedApp);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
