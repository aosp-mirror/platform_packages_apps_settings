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
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.accessibility.AccessibilityUtils;

/**
 * Settings page for accessibility shortcut
 */
public class AccessibilityShortcutPreferenceFragment extends ToggleFeaturePreferenceFragment
        implements Indexable {

    public static final String SHORTCUT_SERVICE_KEY = "accessibility_shortcut_service";
    public static final String ON_LOCK_SCREEN_KEY = "accessibility_shortcut_on_lock_screen";

    private Preference mServicePreference;
    private SwitchPreference mOnLockScreenSwitchPreference;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_TOGGLE_GLOBAL_GESTURE;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_accessibility_shortcut;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.accessibility_shortcut_settings);
        mServicePreference = findPreference(SHORTCUT_SERVICE_KEY);
        mOnLockScreenSwitchPreference = (SwitchPreference) findPreference(ON_LOCK_SCREEN_KEY);
        mOnLockScreenSwitchPreference.setOnPreferenceChangeListener((Preference p, Object o) -> {
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN,
                    ((Boolean) o) ? 1 : 0);
            return true;
        });
        mFooterPreferenceMixin.createFooterPreference()
                .setTitle(R.string.accessibility_shortcut_description);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mSwitchBar.addOnSwitchChangeListener((Switch switchView, boolean enabled) -> {
            Context context = getContext();
            if (enabled && (getServiceInfo(context) == null)) {
                // If no service is configured, we'll disable the shortcut shortly. Give the
                // user a chance to select a service. We'll update the preferences when we resume.
                Settings.Secure.putInt(
                        getContentResolver(), Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED, 1);
                mServicePreference.setEnabled(true);
                mServicePreference.performClick();
            } else {
                onPreferenceToggled(Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED, enabled);
            }
        });
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), preferenceKey, enabled ? 1 : 0);
        updatePreferences();
    }

    private void updatePreferences() {
        ContentResolver cr = getContentResolver();
        Context context = getContext();
        mServicePreference.setSummary(getServiceName(context));
        if (getServiceInfo(context) == null) {
            // If no service is configured, make sure the overall shortcut is turned off
            Settings.Secure.putInt(
                    getContentResolver(), Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED, 0);
        }
        boolean isEnabled = Settings.Secure
                .getInt(cr, Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED, 1) == 1;
        mSwitchBar.setChecked(isEnabled);
        mOnLockScreenSwitchPreference.setChecked(Settings.Secure.getInt(
                cr, Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, 0) == 1);
        // Only enable changing the service and lock screen behavior if the shortcut is on
        mServicePreference.setEnabled(mToggleSwitch.isChecked());
        mOnLockScreenSwitchPreference.setEnabled(mToggleSwitch.isChecked());
    }

    /**
     * Get the user-visible name of the service currently selected for the shortcut.
     *
     * @param context The current context
     * @return The name of the service or a string saying that none is selected.
     */
    public static CharSequence getServiceName(Context context) {
        AccessibilityServiceInfo shortcutServiceInfo = getServiceInfo(context);
        if (shortcutServiceInfo != null) {
            return shortcutServiceInfo.getResolveInfo().loadLabel(context.getPackageManager());
        }
        return context.getString(R.string.accessibility_no_service_selected);
    }

    private static AccessibilityServiceInfo getServiceInfo(Context context) {
        ComponentName shortcutServiceName = ComponentName.unflattenFromString(
                AccessibilityUtils.getShortcutTargetServiceComponentNameString(
                        context, UserHandle.myUserId()));
        return AccessibilityManager.getInstance(context)
                .getInstalledServiceInfoWithComponentName(shortcutServiceName);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                // This fragment is for details of the shortcut. Only the shortcut itself needs
                // to be indexed.
                protected boolean isPageSearchEnabled(Context context) {
                    return false;
                }
            };
}
