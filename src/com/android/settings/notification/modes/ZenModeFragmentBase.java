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

import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for Settings pages used to configure individual modes.
 */
abstract class ZenModeFragmentBase extends ZenModesFragmentBase {
    static final String TAG = "ZenModeSettings";

    @Nullable  // only until reloadMode() is called
    private ZenMode mZenMode;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        String id = null;
        if (getActivity() != null && getActivity().getIntent() != null) {
            id = getActivity().getIntent().getStringExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID);
        }
        Bundle bundle = getArguments();
        if (id == null && bundle != null && bundle.containsKey(EXTRA_AUTOMATIC_ZEN_RULE_ID)) {
            id = bundle.getString(EXTRA_AUTOMATIC_ZEN_RULE_ID);
        }
        if (id == null) {
            Log.d(TAG, "No id provided");
            toastAndFinish();
            return;
        }
        if (!reloadMode(id)) {
            Log.d(TAG, "Mode id " + id + " not found");
            toastAndFinish();
            return;
        }
        if (mZenMode != null) {
            // Propagate mode info through to controllers.
            for (List<AbstractPreferenceController> list : getPreferenceControllers()) {
                try {
                    for (AbstractPreferenceController controller : list) {
                        // mZenMode guaranteed non-null from reloadMode() above
                        ((AbstractZenModePreferenceController) controller).setZenMode(mZenMode);
                    }
                } catch (ClassCastException e) {
                    // ignore controllers that aren't AbstractZenModePreferenceController
                }
            }
        }
    }

    /**
     * Refresh stored ZenMode data.
     * @param id the mode ID
     * @return whether we successfully got mode data from the backend.
     */
    private boolean reloadMode(String id) {
        mZenMode = mBackend.getMode(id);
        if (mZenMode == null) {
            return false;
        }
        return true;
    }

    /**
     * Refresh ZenMode data any time the system's zen mode state changes (either the zen mode value
     * itself, or the config), and also (once updated) update the info for all controllers.
     */
    @Override
    protected void updateZenModeState() {
        if (mZenMode == null) {
            // This shouldn't happen, but guard against it in case
            toastAndFinish();
            return;
        }
        String id = mZenMode.getId();
        if (!reloadMode(id)) {
            Log.d(TAG, "Mode id=" + id + " not found");
            toastAndFinish();
            return;
        }
        updateControllers();
    }

    private void updateControllers() {
        if (getPreferenceControllers() == null || mZenMode == null) {
            return;
        }

        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            Log.d(TAG, "PreferenceScreen not found");
            return;
        }
        for (List<AbstractPreferenceController> list : getPreferenceControllers()) {
            for (AbstractPreferenceController controller : list) {
                try {
                    // Find preference associated with controller
                    final String key = controller.getPreferenceKey();
                    final Preference preference = screen.findPreference(key);
                    if (preference != null) {
                        AbstractZenModePreferenceController zenController =
                                (AbstractZenModePreferenceController) controller;
                        zenController.updateZenMode(preference, mZenMode);
                    } else {
                        Log.d(TAG,
                                String.format("Cannot find preference with key %s in Controller %s",
                                        key, controller.getClass().getSimpleName()));
                    }
                    controller.displayPreference(screen);
                } catch (ClassCastException e) {
                    // Skip any controllers that aren't AbstractZenModePreferenceController.
                    Log.d(TAG, "Could not cast: " + controller.getClass().getSimpleName());
                }
            }
        }
    }

    private void toastAndFinish() {
        Toast.makeText(mContext, R.string.zen_mode_rule_not_found_text, Toast.LENGTH_SHORT)
                .show();
        this.finish();
    }

    /**
     * Get current mode data.
     */
    @Nullable
    public ZenMode getMode() {
        return mZenMode;
    }

    protected final boolean saveMode(Consumer<ZenMode> updater) {
        Preconditions.checkState(mBackend != null);
        ZenMode mode = mZenMode;
        if (mode == null) {
            Log.wtf(TAG, "Cannot save mode, it hasn't been loaded (" + getClass() + ")");
            return false;
        }
        updater.accept(mode);
        mBackend.updateMode(mode);
        return true;
    }
}
