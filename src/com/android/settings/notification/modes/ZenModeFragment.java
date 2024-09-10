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

import static com.android.settingslib.notification.modes.ZenMode.Status.DISABLED_BY_OTHER;

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;

import java.util.ArrayList;
import java.util.List;

public class ZenModeFragment extends ZenModeFragmentBase {

    // for mode context menu
    private static final int RENAME_MODE = 1;
    private static final int DELETE_MODE = 2;

    private ModeMenuProvider mModeMenuProvider;
    private boolean mSettingsObserverRegistered = false; // for ManualDurationPreferenceController

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_rule_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> prefControllers = new ArrayList<>();
        prefControllers.add(
                new ZenModeHeaderController(context, ZenIconLoader.getInstance(), "header", this));
        prefControllers.add(new ZenModeBlurbPreferenceController(context, "mode_blurb"));
        prefControllers.add(
                new ZenModeButtonPreferenceController(context, "activate", this, mBackend));
        prefControllers.add(new ZenModePreferenceCategoryController(context, "modes_filters"));
        prefControllers.add(new ZenModePeopleLinkPreferenceController(
                context, "zen_mode_people", mHelperBackend));
        prefControllers.add(new ZenModeAppsLinkPreferenceController(
                context, "zen_mode_apps", this, mBackend, mHelperBackend));
        prefControllers.add(new ZenModeOtherLinkPreferenceController(
                context, "zen_other_settings", mHelperBackend));
        prefControllers.add(
                new ZenModePreferenceCategoryController(context, "modes_additional_actions"));
        prefControllers.add(new ZenModeDisplayLinkPreferenceController(
                context, "mode_display_settings", mBackend, mHelperBackend));
        prefControllers.add(new ZenModeTriggerCategoryPreferenceController(context,
                "zen_automatic_trigger_category"));
        prefControllers.add(new ZenModeTriggerUpdatePreferenceController(context,
                "zen_automatic_trigger_settings", mBackend));
        prefControllers.add(
                new ZenModeTriggerAddPreferenceController(context, "zen_add_automatic_trigger",
                        this, mBackend));
        prefControllers.add(new InterruptionFilterPreferenceController(
                context, "allow_all", mBackend));
        prefControllers.add(new ManualDurationPreferenceController(
                context, "mode_manual_duration", this, mBackend));
        return prefControllers;
    }

    @Override
    public void onStart() {
        super.onStart();
        ZenMode mode = getMode();

        // Consider redirecting to the interstitial if the mode is disabled (but not by the user).
        if (maybeRedirectToInterstitial(mode)) {
            return;
        }

        // Set title for the entire screen
        ComponentActivity activity = getActivity();
        if (mode != null && activity != null) {
            activity.setTitle(mode.getName());
            mModeMenuProvider = new ModeMenuProvider(mode);
            activity.addMenuProvider(mModeMenuProvider);
        }

        // allow duration preference controller to listen for settings changes
        use(ManualDurationPreferenceController.class).registerSettingsObserver();
        mSettingsObserverRegistered = true;
    }

    private boolean maybeRedirectToInterstitial(@Nullable ZenMode mode) {
        if (mode == null || mode.getStatus() != DISABLED_BY_OTHER) {
            return false;
        }

        mContext.startActivity(SetupInterstitialActivity.getIntent(mContext, mode));
        // don't come back here from the interstitial
        finish();
        return true;
    }

    @Override
    public void onStop() {
        if (getActivity() != null && mModeMenuProvider != null) {
            getActivity().removeMenuProvider(mModeMenuProvider);
        }
        if (mSettingsObserverRegistered) {
            use(ManualDurationPreferenceController.class).unregisterSettingsObserver();
        }
        super.onStop();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_PRIORITY_MODE;
    }

    @Override
    protected void onUpdatedZenModeState() {
        // Because this fragment may be asked to finish by the delete menu but not be done doing
        // so yet, ignore any attempts to update info in that case.
        if (getActivity() != null && getActivity().isFinishing()) {
            return;
        }
        super.onUpdatedZenModeState();
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
                ZenSubSettingLauncher.forModeFragment(mContext, ZenModeEditNameIconFragment.class,
                        mZenMode.getId(), getMetricsCategory()).launch();
            } else if (menuItem.getItemId() == DELETE_MODE) {
                new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.zen_mode_delete_mode_confirmation,
                                mZenMode.getName()))
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
