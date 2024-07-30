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

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.modes.ZenModesListAddModePreferenceController.ModeType;
import com.android.settings.notification.modes.ZenModesListAddModePreferenceController.OnAddModeListener;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
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
        return buildPreferenceControllers(context, this::onAvailableModeTypesForAdd);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            OnAddModeListener onAddModeListener) {
        // We need to redefine ZenModesBackend here even though mBackend exists so that this method
        // can be static; it must be static to be able to be used in SEARCH_INDEX_DATA_PROVIDER.
        ZenModesBackend backend = ZenModesBackend.getInstance(context);

        return ImmutableList.of(
                new ZenModesListPreferenceController(context, backend),
                new ZenModesListAddModePreferenceController(context, onAddModeListener)
        );
    }

    @Override
    protected void onUpdatedZenModeState() {
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
            // TODO: b/332937635 - set metrics categories correctly
            new SubSettingLauncher(requireContext())
                    .setDestination(ZenModeNewCustomFragment.class.getName())
                    .setSourceMetricsCategory(0)
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
                mode -> ZenSubSettingLauncher.forMode(mContext, mode.getId()).launch());
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
                    return buildPreferenceControllers(context, ignoredType -> {});
                }
            };
}
