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

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;

import java.util.List;

/**
 * Base class for Settings pages used to configure individual modes.
 */
abstract class ZenModeFragmentBase extends ZenModesFragmentBase {
    static final String TAG = "ZenModeSettings";

    @Nullable private ZenMode mZenMode;
    @Nullable private ZenMode mModeOnLastControllerUpdate;

    @Override
    public void onCreate(Bundle icicle) {
        mZenMode = loadModeFromArguments();
        if (mZenMode != null) {
            // Propagate mode info through to controllers. Must be done before super.onCreate(),
            // because that one calls AbstractPreferenceController.isAvailable().
            for (var controller : getZenPreferenceControllers()) {
                controller.setZenMode(mZenMode);
            }
        } else {
            toastAndFinish();
        }

        super.onCreate(icicle);
    }

    @Nullable
    private ZenMode loadModeFromArguments() {
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
            return null;
        }

        ZenMode mode = mBackend.getMode(id);
        if (mode == null) {
            Log.d(TAG, "Mode with id " + id + " not found");
            return null;
        }
        return mode;
    }

    private Iterable<AbstractZenModePreferenceController> getZenPreferenceControllers() {
        return getPreferenceControllers().stream()
                .flatMap(List::stream)
                .filter(AbstractZenModePreferenceController.class::isInstance)
                .map(AbstractZenModePreferenceController.class::cast)
                .toList();
    }

    @Override
    protected void onUpdatedZenModeState() {
        if (mZenMode == null) {
            Log.wtf(TAG, "mZenMode is null in onUpdatedZenModeState");
            toastAndFinish();
            return;
        }

        String id = mZenMode.getId();
        ZenMode mode = mBackend.getMode(id);
        if (mode == null) {
            Log.d(TAG, "Mode id=" + id + " not found");
            toastAndFinish();
            return;
        }

        mZenMode = mode;
        maybeUpdateControllersState(mode);
    }

    /**
     * Updates all {@link AbstractZenModePreferenceController} based on the loaded mode info.
     * For each controller, {@link AbstractZenModePreferenceController#setZenMode} will be called.
     * Then, {@link AbstractZenModePreferenceController#updateState} will be called as well, unless
     * we determine it's not necessary (for example, if we know that {@code DashboardFragment} will
     * do it soon).
     */
    private void maybeUpdateControllersState(@NonNull ZenMode zenMode) {
        boolean needsFullUpdate =
                getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)
                && (mModeOnLastControllerUpdate == null
                        || !mModeOnLastControllerUpdate.equals(zenMode));
        mModeOnLastControllerUpdate = zenMode.copy();

        for (var controller : getZenPreferenceControllers()) {
            controller.setZenMode(zenMode);
        }

        if (needsFullUpdate) {
            forceUpdatePreferences();
        }
    }

    private void toastAndFinish() {
        Toast.makeText(mContext, R.string.zen_mode_not_found_text, Toast.LENGTH_SHORT)
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
}
