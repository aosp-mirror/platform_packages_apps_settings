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

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.notification.modes.ZenMode;

import java.util.ArrayList;
import java.util.List;

public class ZenModeFragment extends ZenModeFragmentBase {

    // for mode context menu
    private static final int RENAME_MODE = 1;
    private static final int DELETE_MODE = 2;

    private ModeMenuProvider mModeMenuProvider;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_rule_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> prefControllers = new ArrayList<>();
        prefControllers.add(new ZenModeHeaderController(context, "header", this));
        prefControllers.add(
                new ZenModeButtonPreferenceController(context, "activate", this, mBackend));
        prefControllers.add(new ZenModePeopleLinkPreferenceController(
                context, "zen_mode_people", mHelperBackend));
        prefControllers.add(new ZenModeAppsLinkPreferenceController(
                context, "zen_mode_apps", this, mBackend, mHelperBackend));
        prefControllers.add(new ZenModeOtherLinkPreferenceController(
                context, "zen_other_settings", mHelperBackend));
        prefControllers.add(new ZenModeDisplayLinkPreferenceController(
                context, "mode_display_settings", mBackend, mHelperBackend));
        prefControllers.add(new ZenModeSetTriggerLinkPreferenceController(context,
                "zen_automatic_trigger_category", this, mBackend));
        prefControllers.add(new InterruptionFilterPreferenceController(
                context, "allow_filtering", mBackend));
        prefControllers.add(new ManualDurationPreferenceController(
                context, "mode_manual_duration", this, mBackend));
        return prefControllers;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // allow duration preference controller to listen for settings changes
        use(ManualDurationPreferenceController.class).registerSettingsObserver();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set title for the entire screen
        ZenMode mode = getMode();
        ComponentActivity activity = getActivity();
        if (mode != null && activity != null) {
            activity.setTitle(mode.getName());
            mModeMenuProvider = new ModeMenuProvider(mode);
            activity.addMenuProvider(mModeMenuProvider);
        }
    }

    @Override
    public void onStop() {
        if (getActivity() != null) {
            getActivity().removeMenuProvider(mModeMenuProvider);
        }
        super.onStop();
    }

    @Override
    public void onDetach() {
        use(ManualDurationPreferenceController.class).unregisterSettingsObserver();
        super.onDetach();
    }

    @Override
    public int getMetricsCategory() {
        // TODO: b/332937635 - make this the correct metrics category
        return SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

    @Override
    protected void updateZenModeState() {
        // Because this fragment may be asked to finish by the delete menu but not be done doing
        // so yet, ignore any attempts to update info in that case.
        if (getActivity() != null && getActivity().isFinishing()) {
            return;
        }
        super.updateZenModeState();
    }

    private class ModeMenuProvider implements MenuProvider {
        @NonNull private final ZenMode mZenMode;

        ModeMenuProvider(@NonNull ZenMode mode) {
            mZenMode = mode;
        }

        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            if (mZenMode.canEditNameAndIcon()) {
                menu.add(Menu.NONE, RENAME_MODE, Menu.NONE, R.string.zen_mode_menu_rename_mode);
            }
            if (mZenMode.canBeDeleted()) {
                // Only deleteable modes should get a delete menu option.
                menu.add(Menu.NONE, DELETE_MODE, Menu.NONE, R.string.zen_mode_menu_delete_mode);
            }
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == RENAME_MODE) {
                // TODO: b/332937635 - Update metrics category
                ZenSubSettingLauncher.forModeFragment(mContext, ZenModeEditNameIconFragment.class,
                        mZenMode.getId(), 0).launch();
            } else if (menuItem.getItemId() == DELETE_MODE) {
                new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.zen_mode_delete_mode_confirmation,
                                mZenMode.getRule().getName()))
                        .setPositiveButton(R.string.zen_mode_schedule_delete,
                                (dialog, which) -> {
                                    // start finishing before calling removeMode() so that we
                                    // don't try to update this activity with a nonexistent mode
                                    // when the zen mode config is updated
                                    finish();
                                    mBackend.removeMode(mZenMode);
                                })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            }
            return false;
        }
    }
}
