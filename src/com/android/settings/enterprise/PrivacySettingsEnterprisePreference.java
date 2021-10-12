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

package com.android.settings.enterprise;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Privacy Settings preferences for an Enterprise device. */
public class PrivacySettingsEnterprisePreference implements PrivacySettingsPreference {
    private static final String KEY_EXPOSURE_CHANGES_CATEGORY = "exposure_changes_category";

    private final Context mContext;

    public PrivacySettingsEnterprisePreference(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Returns the XML Res Id that is used for an Enterprise device in the Privacy Settings screen.
     */
    @Override
    public int getPreferenceScreenResId() {
        return R.xml.enterprise_privacy_settings;
    }

    /**
     * Returns the Enterprise XML resources to index for an Enterprise device.
     */
    @Override
    public List<SearchIndexableResource> getXmlResourcesToIndex() {
        final SearchIndexableResource sir = new SearchIndexableResource(mContext);
        sir.xmlResId = getPreferenceScreenResId();
        return Collections.singletonList(sir);
    }

    /**
     * Returns the preference controllers used to populate the privacy preferences in the Privacy
     * Settings screen for Enterprise devices.
     */
    @Override
    public List<AbstractPreferenceController> createPreferenceControllers(boolean async) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new NetworkLogsPreferenceController(mContext));
        controllers.add(new BugReportsPreferenceController(mContext));
        controllers.add(new SecurityLogsPreferenceController(mContext));
        final List<AbstractPreferenceController> exposureChangesCategoryControllers =
                new ArrayList<>();
        exposureChangesCategoryControllers.add(new EnterpriseInstalledPackagesPreferenceController(
                mContext, async));
        exposureChangesCategoryControllers.add(
                new AdminGrantedLocationPermissionsPreferenceController(mContext, async));
        exposureChangesCategoryControllers.add(
                new AdminGrantedMicrophonePermissionPreferenceController(mContext, async));
        exposureChangesCategoryControllers.add(new AdminGrantedCameraPermissionPreferenceController(
                mContext, async));
        exposureChangesCategoryControllers.add(new EnterpriseSetDefaultAppsPreferenceController(
                mContext));
        exposureChangesCategoryControllers.add(new AlwaysOnVpnCurrentUserPreferenceController(
                mContext));
        exposureChangesCategoryControllers.add(new AlwaysOnVpnManagedProfilePreferenceController(
                mContext));
        exposureChangesCategoryControllers.add(new ImePreferenceController(mContext));
        exposureChangesCategoryControllers.add(new GlobalHttpProxyPreferenceController(mContext));
        exposureChangesCategoryControllers.add(new CaCertsCurrentUserPreferenceController(
                mContext));
        exposureChangesCategoryControllers.add(new CaCertsManagedProfilePreferenceController(
                mContext));
        controllers.addAll(exposureChangesCategoryControllers);
        controllers.add(new PreferenceCategoryController(mContext, KEY_EXPOSURE_CHANGES_CATEGORY)
                .setChildren(exposureChangesCategoryControllers));
        controllers.add(new FailedPasswordWipeCurrentUserPreferenceController(mContext));
        controllers.add(new FailedPasswordWipeManagedProfilePreferenceController(mContext));
        return controllers;
    }
}
