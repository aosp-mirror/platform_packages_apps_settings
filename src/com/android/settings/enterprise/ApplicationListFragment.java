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
 * limitations under the License
 */

package com.android.settings.enterprise;

import android.Manifest;
import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Base fragment for displaying a list of applications on a device.
 * Inner static classes are concrete implementations.
 */
public abstract class ApplicationListFragment extends DashboardFragment
        implements ApplicationListPreferenceController.ApplicationListBuilder {

    static final String TAG = "EnterprisePrivacySettings";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_list_disclosure_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        ApplicationListPreferenceController controller = new ApplicationListPreferenceController(
                context, this, context.getPackageManager(), this);
        controllers.add(controller);
        return controllers;
    }

    private abstract static class AdminGrantedPermission extends ApplicationListFragment {
        private final String[] mPermissions;

        public AdminGrantedPermission(String[] permissions) {
            mPermissions = permissions;
        }

        @Override
        public void buildApplicationList(Context context,
                ApplicationFeatureProvider.ListOfAppsCallback callback) {
            FeatureFactory.getFactory(context).getApplicationFeatureProvider(context)
                    .listAppsWithAdminGrantedPermissions(mPermissions, callback);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.ENTERPRISE_PRIVACY_PERMISSIONS;
        }
    }

    public static class AdminGrantedPermissionCamera extends AdminGrantedPermission {
        public AdminGrantedPermissionCamera() {
            super(new String[] {Manifest.permission.CAMERA});
        }
    }

    public static class AdminGrantedPermissionLocation extends AdminGrantedPermission {
        public AdminGrantedPermissionLocation() {
            super(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    public static class AdminGrantedPermissionMicrophone extends AdminGrantedPermission {
        public AdminGrantedPermissionMicrophone() {
            super(new String[] {Manifest.permission.RECORD_AUDIO});
        }
    }

    public static class EnterpriseInstalledPackages extends ApplicationListFragment {
        public EnterpriseInstalledPackages() {
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.ENTERPRISE_PRIVACY_INSTALLED_APPS;
        }

        @Override
        public void buildApplicationList(Context context,
                ApplicationFeatureProvider.ListOfAppsCallback callback) {
            FeatureFactory.getFactory(context).getApplicationFeatureProvider(context).
                    listPolicyInstalledApps(callback);
        }
    }
}
