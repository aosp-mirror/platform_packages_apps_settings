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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;

/**
 * Base class for Settings pages used to configure individual modes.
 */
abstract class ZenModesRuleSettingsBase extends ZenModesSettingsBase {
    static final String TAG = "ZenModesRuleSettings";
    static final String MODE_ID = "MODE_ID";

    @Nullable
    protected ZenMode mZenMode;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // TODO: b/322373473 - Update if modes page ends up using a different method of passing id
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(MODE_ID)) {
            String id = bundle.getString(MODE_ID);
            if (!reloadMode(id)) {
                Log.d(TAG, "Mode id " + id + " not found");
                toastAndFinish();
            }
        } else {
            Log.d(TAG, "Mode id required to set mode config settings");
            toastAndFinish();
        }
    }

    /**
     * Refresh stored ZenMode data.
     * @param id the mode ID
     * @return whether we successfully got mode data from the backend.
     */
    private boolean reloadMode(String id) {
        mZenMode = mBackend.getMode(id);
        return mZenMode != null;
    }

    /**
     * Refresh ZenMode data any time the system's zen mode state changes (either the zen mode value
     * itself, or the config).
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
        }
    }

    private void toastAndFinish() {
        Toast.makeText(mContext, R.string.zen_mode_rule_not_found_text, Toast.LENGTH_SHORT)
                .show();
        this.finish();
    }
}
