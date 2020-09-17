/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Install certificate from storage settings.
 */
@SearchIndexable
public class InstallCertificateFromStorage extends DashboardFragment {

    private static final String TAG = "InstallCertificateFromStorage";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.INSTALL_CERTIFICATE_FROM_STORAGE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.install_certificate_from_storage;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new InstallCaCertificatePreferenceController(context));
        controllers.add(new InstallUserCertificatePreferenceController(context));
        controllers.add(new InstallWifiCertificatePreferenceController(context));
        return controllers;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_install_certificate;
    }

    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.install_certificate_from_storage) {
                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* lifecycle */);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    final UserManager um = (UserManager) context.getSystemService(
                            Context.USER_SERVICE);
                    return um.isAdminUser();
                }
            };
}
