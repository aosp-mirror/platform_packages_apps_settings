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

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.modes.ZenModesListAddModePreferenceController.ModeType;
import com.android.settings.notification.modes.ZenModesListAddModePreferenceController.OnAddModeListener;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.search.SearchIndexable;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

@SearchIndexable
public class ZenModesListFragment extends ZenModesFragmentBase {

    static final int REQUEST_NEW_MODE = 101;

    @Nullable private ComponentName mActivityInvokedForAddNew;
    @Nullable private ImmutableList<String> mZenModeIdsBeforeAddNew;

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, mBackend, this::onAvailableModeTypesForAdd);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            ZenModesBackend backend, OnAddModeListener onAddModeListener) {
        return ImmutableList.of(
                new ZenModesListPreferenceController(context, backend, ZenIconLoader.getInstance()),
                new ZenModesListAddModePreferenceController(context, "add_mode", onAddModeListener)
        );
    }

    @Override
    protected void onUpdatedZenModeState() {
        // Preferences linking to individual rules do not need to be updated as part of onStart(),
        // because DashboardFragment does that in onResume(). However, we force the update if we
        // detect Modes changes in the background with the page open.
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            forceUpdatePreferences();
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_list_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_PRIORITY_MODES_LIST;
    }

    private void onAvailableModeTypesForAdd(List<ModeType> types) {
        if (types.size() > 1) {
            // Show dialog to choose the mode to be created. Continue once the user chooses.
            ZenModesListAddModeTypeChooserDialog.show(this, this::onChosenModeTypeForAdd, types);
        } else {
            // Will be custom_manual.
            onChosenModeTypeForAdd(types.get(0));
        }
    }

    @VisibleForTesting
    void onChosenModeTypeForAdd(ModeType type) {
        if (type.creationActivityIntent() != null) {
            mActivityInvokedForAddNew = type.creationActivityIntent().getComponent();
            mZenModeIdsBeforeAddNew = ImmutableList.copyOf(
                    mBackend.getModes().stream().map(ZenMode::getId).toList());
            startActivityForResult(type.creationActivityIntent(), REQUEST_NEW_MODE);
        } else {
            // Custom-manual mode -> "add a mode" screen.
            new SubSettingLauncher(requireContext())
                    .setDestination(ZenModeNewCustomFragment.class.getName())
                    .setSourceMetricsCategory(SettingsEnums.ZEN_PRIORITY_MODES_LIST)
                    .launch();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If coming back after starting a 3rd-party configuration activity to create a new mode,
        // try to identify the created mode. Ideally this would be part of the resultCode/data, but
        // the existing API doesn't work that way...
        ComponentName activityInvoked = mActivityInvokedForAddNew;
        ImmutableList<String> previousIds = mZenModeIdsBeforeAddNew;
        mActivityInvokedForAddNew = null;
        mZenModeIdsBeforeAddNew = null;
        if (requestCode != REQUEST_NEW_MODE || previousIds == null || activityInvoked == null) {
            return;
        }

        // If we find a new mode owned by the same package, presumably that's it. Open its page.
        Optional<ZenMode> createdZenMode = mBackend.getModes().stream()
                .filter(m -> !previousIds.contains(m.getId()))
                .filter(m -> m.getRule().getPackageName().equals(activityInvoked.getPackageName()))
                .findFirst();
        createdZenMode.ifPresent(
                mode ->
                        ZenSubSettingLauncher.forModeFragment(mContext, ZenModeFragment.class,
                                mode.getId(), getMetricsCategory()).launch());
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.modes_list_settings) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    // We need to redefine ZenModesBackend here even though mBackend exists so that
                    // SEARCH_INDEX_DATA_PROVIDER can be static.
                    return buildPreferenceControllers(context, ZenModesBackend.getInstance(context),
                            ignoredType -> {});
                }
            };
}
