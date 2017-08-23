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

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.DynamicAvailabilityPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnterprisePrivacySettings extends DashboardFragment {

    static final String TAG = "EnterprisePrivacySettings";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ENTERPRISE_PRIVACY_SETTINGS;
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
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), true /* async */);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, boolean async) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new NetworkLogsPreferenceController(context));
        controllers.add(new BugReportsPreferenceController(context));
        controllers.add(new SecurityLogsPreferenceController(context));
        final List<DynamicAvailabilityPreferenceController> exposureChangesCategoryControllers =
                new ArrayList<>();
        exposureChangesCategoryControllers.add(new EnterpriseInstalledPackagesPreferenceController(
                context, lifecycle, async));
        exposureChangesCategoryControllers.add(
                new AdminGrantedLocationPermissionsPreferenceController(context, lifecycle, async));
        exposureChangesCategoryControllers.add(
                new AdminGrantedMicrophonePermissionPreferenceController(context, lifecycle,
                        async));
        exposureChangesCategoryControllers.add(new AdminGrantedCameraPermissionPreferenceController(
                context, lifecycle, async));
        exposureChangesCategoryControllers.add(new EnterpriseSetDefaultAppsPreferenceController(
                context, lifecycle));
        exposureChangesCategoryControllers.add(new AlwaysOnVpnCurrentUserPreferenceController(
                context, lifecycle));
        exposureChangesCategoryControllers.add(new AlwaysOnVpnManagedProfilePreferenceController(
                context, lifecycle));
        exposureChangesCategoryControllers.add(new ImePreferenceController(context, lifecycle));
        exposureChangesCategoryControllers.add(new GlobalHttpProxyPreferenceController(context,
                lifecycle));
        exposureChangesCategoryControllers.add(new CaCertsCurrentUserPreferenceController(
                context, lifecycle));
        exposureChangesCategoryControllers.add(new CaCertsManagedProfilePreferenceController(
                context, lifecycle));
        controllers.addAll(exposureChangesCategoryControllers);
        controllers.add(new ExposureChangesCategoryPreferenceController(context, lifecycle,
                exposureChangesCategoryControllers, async));
        controllers.add(new FailedPasswordWipeCurrentUserPreferenceController(context, lifecycle));
        controllers.add(new FailedPasswordWipeManagedProfilePreferenceController(context,
                lifecycle));
        return controllers;
    }

    public static boolean isPageEnabled(Context context) {
        return FeatureFactory.getFactory(context)
                .getEnterprisePrivacyFeatureProvider(context)
                .hasDeviceOwner();
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isPageEnabled(context);
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.enterprise_privacy_settings;
                    return Arrays.asList(sir);
            }

            @Override
            public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                return buildPreferenceControllers(context, null /* lifecycle */, false /* async */);
                }
            };
}
