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

import android.app.AutomaticZenRule;
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

import java.util.List;

/**
 * Base class for Settings pages used to configure individual modes.
 */
abstract class ZenModeFragmentBase extends ZenModesFragmentBase {
    static final String TAG = "ZenModeSettings";
    static final String MODE_ID = "MODE_ID";

    @Nullable  // only until reloadMode() is called
    private ZenMode mZenMode;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // TODO: b/322373473 - Update if modes page ends up using a different method of passing id
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(MODE_ID)) {
            String id = bundle.getString(MODE_ID);
            if (!reloadMode(id)) {
                Log.e(TAG, "Mode id " + id + " not found");
                toastAndFinish();
                return;
            }
        } else {
            Log.e(TAG, "Mode id required to set mode config settings");
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
                if (!controller.isAvailable()) {
                    continue;
                }

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

    /**
     * Get AutomaticZenRule associated with current mode data, or null if it doesn't exist.
     */
    @Nullable
    public AutomaticZenRule getAZR() {
        if (mZenMode == null) {
            return null;
        }
        return mZenMode.getRule();
    }
}
