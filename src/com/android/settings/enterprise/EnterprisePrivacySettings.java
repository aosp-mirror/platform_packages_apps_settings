/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.enterprise;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class EnterprisePrivacySettings extends DashboardFragment {

    static final String TAG = "EnterprisePrivacySettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ENTERPRISE_PRIVACY_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.enterprise_privacy_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, true /* async */);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            boolean async) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new NetworkLogsPreferenceController(context));
        controllers.add(new BugReportsPreferenceController(context));
        controllers.add(new SecurityLogsPreferenceController(context));
        final List<AbstractPreferenceController> exposureChangesCategoryControllers =
                new ArrayList<>();
        exposureChangesCategoryControllers.add(new EnterpriseInstalledPackagesPreferenceController(
                context, async));
        exposureChangesCategoryControllers.add(
                new AdminGrantedLocationPermissionsPreferenceController(context, async));
        exposureChangesCategoryControllers.add(
                new AdminGrantedMicrophonePermissionPreferenceController(context, async));
        exposureChangesCategoryControllers.add(new AdminGrantedCameraPermissionPreferenceController(
                context, async));
        exposureChangesCategoryControllers.add(new EnterpriseSetDefaultAppsPreferenceController(
                context));
        exposureChangesCategoryControllers.add(new AlwaysOnVpnCurrentUserPreferenceController(
                context));
        exposureChangesCategoryControllers.add(new AlwaysOnVpnManagedProfilePreferenceController(
                context));
        exposureChangesCategoryControllers.add(new ImePreferenceController(context));
        exposureChangesCategoryControllers.add(new GlobalHttpProxyPreferenceController(context));
        exposureChangesCategoryControllers.add(new CaCertsCurrentUserPreferenceController(context));
        exposureChangesCategoryControllers.add(new CaCertsManagedProfilePreferenceController(
                context));
        controllers.addAll(exposureChangesCategoryControllers);
        controllers.add(new PreferenceCategoryController(context, "exposure_changes_category")
                .setChildren(exposureChangesCategoryControllers));
        controllers.add(new FailedPasswordWipeCurrentUserPreferenceController(context));
        controllers.add(new FailedPasswordWipeManagedProfilePreferenceController(context));
        return controllers;
    }

    public static boolean isPageEnabled(Context context) {
        return FeatureFactory.getFactory(context)
                .getEnterprisePrivacyFeatureProvider(context)
                .hasDeviceOwner();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.enterprise_privacy_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isPageEnabled(context);
                }


                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, false /* async */);
                }
            };
}
