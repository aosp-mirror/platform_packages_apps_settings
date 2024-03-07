/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.security;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.safetycenter.SafetyCenterUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.trustagent.TrustAgentListPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

/**
 * An overflow menu for {@code SecuritySettings} containing advanced security settings.
 *
 * <p>This includes all work-profile related settings.
 */
@SearchIndexable
public class SecurityAdvancedSettings extends DashboardFragment {

    private static final String TAG = "SecurityAdvancedSettings";

    /** Used in case of old Security settings when SafetyCenter is disabled */
    private static final String CATEGORY_SECURITY_LEGACY_ADVANCED_SETTINGS =
            "com.android.settings.category.ia.legacy_advanced_security";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SafetyCenterUtils.replaceEnterpriseStringsForSecurityEntries(this);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SECURITY_ADVANCED;
    }

    @Override
    public String getCategoryKey() {
        final Context context = getContext();
        if (context == null) {
            return CATEGORY_SECURITY_LEGACY_ADVANCED_SETTINGS;
        } else if (SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS;
        } else {
            final SecuritySettingsFeatureProvider securitySettingsFeatureProvider =
                    FeatureFactory.getFeatureFactory().getSecuritySettingsFeatureProvider();

            if (securitySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment()) {
                return securitySettingsFeatureProvider.getAlternativeAdvancedSettingsCategoryKey();
            } else {
                return CATEGORY_SECURITY_LEGACY_ADVANCED_SETTINGS;
            }
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_advanced_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle(), this /* host*/);
    }

    /** see confirmPatternThenDisableAndClear */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (use(TrustAgentListPreferenceController.class)
                .handleActivityResult(requestCode, resultCode)) {
            return;
        }
        if (use(LockUnificationPreferenceController.class)
                .handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle, DashboardFragment host) {
        return SafetyCenterUtils.getControllersForAdvancedSecurity(context, lifecycle, host);
    }

    /** For Search. Please keep it in sync when updating "createPreferenceHierarchy()" */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.security_advanced_settings) {
                /**
                 * If SafetyCenter is enabled, all of these entries will be in the More Settings
                 * page, and we don't want to index these entries.
                 */
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    // NOTE: This check likely should be moved to the super method. This is done
                    // here to avoid potentially undesired side effects for existing implementors.
                    if (!isPageSearchEnabled(context)) {
                        return null;
                    }
                    return super.getXmlResourcesToIndex(context, enabled);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(
                            context, null /* lifecycle */, null /* host*/);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return !SafetyCenterManagerWrapper.get().isEnabled(context);
                }
            };
}
