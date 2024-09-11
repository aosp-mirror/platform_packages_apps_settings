/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.service.notification.ConditionProviderService;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import com.google.common.collect.ImmutableList;

import java.util.List;

@SearchIndexable
public class ZenModesListFragment extends ZenModesFragmentBase {

    private static final ManagedServiceSettings.Config CONFIG = getConditionProviderConfig();

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ZenServiceListing serviceListing = new ZenServiceListing(getContext(), CONFIG);
        serviceListing.reloadApprovedServices();
        return buildPreferenceControllers(context, this, serviceListing);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            @Nullable Fragment parent, @Nullable ZenServiceListing serviceListing) {
        // We need to redefine ZenModesBackend here even though mBackend exists so that this method
        // can be static; it must be static to be able to be used in SEARCH_INDEX_DATA_PROVIDER.
        ZenModesBackend backend = ZenModesBackend.getInstance(context);

        return ImmutableList.of(
                new ZenModesListPreferenceController(context, parent, backend),
                new ZenModesListAddModePreferenceController(context, backend, serviceListing)
        );
    }

    @Override
    protected void updateZenModeState() {
        // TODO: b/322373473 -- update any overall description of modes state here if necessary.
        // Note the preferences linking to individual rules do not need to be updated, as
        // updateState() is called on all preference controllers whenever the page is resumed.
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_list_settings;
    }

    @Override
    public int getMetricsCategory() {
        // TODO: b/332937635 - add new & set metrics categories correctly
        return SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

    private static ManagedServiceSettings.Config getConditionProviderConfig() {
        return new ManagedServiceSettings.Config.Builder()
                .setTag(TAG)
                .setIntentAction(ConditionProviderService.SERVICE_INTERFACE)
                .setConfigurationIntentAction(NotificationManager.ACTION_AUTOMATIC_ZEN_RULE)
                .setPermission(android.Manifest.permission.BIND_CONDITION_PROVIDER_SERVICE)
                .setNoun("condition provider")
                .build();
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.modes_list_settings) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    // TODO: b/332937523 - determine if this should be removed once the preference
                    //                     controller adds dynamic data to index
                    keys.add(ZenModesListPreferenceController.KEY);
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null, null);
                }
            };
}
