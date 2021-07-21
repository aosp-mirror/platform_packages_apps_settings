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

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

@SearchIndexable
public class EnterprisePrivacySettings extends DashboardFragment {

    static final String TAG = "EnterprisePrivacySettings";

    @VisibleForTesting
    PrivacySettingsPreference mPrivacySettingsPreference;

    @Override
    public void onAttach(Context context) {
        mPrivacySettingsPreference =
                PrivacySettingsPreferenceFactory.createPrivacySettingsPreference(context);

        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        mPrivacySettingsPreference = null;
        super.onDetach();
    }

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
        return mPrivacySettingsPreference.getPreferenceScreenResId();
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return mPrivacySettingsPreference.createPreferenceControllers(true /* async */);
    }

    public static boolean isPageEnabled(Context context) {
        return FeatureFactory.getFactory(context)
                .getEnterprisePrivacyFeatureProvider(context)
                .hasDeviceOwner();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                private PrivacySettingsPreference mPrivacySettingsPreference;

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isPageEnabled(context);
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    mPrivacySettingsPreference =
                            PrivacySettingsPreferenceFactory.createPrivacySettingsPreference(
                                    context);
                    return mPrivacySettingsPreference.getXmlResourcesToIndex();
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    mPrivacySettingsPreference =
                            PrivacySettingsPreferenceFactory.createPrivacySettingsPreference(
                                    context);
                    return mPrivacySettingsPreference.createPreferenceControllers(
                            false /* async */);
                }
            };
}
