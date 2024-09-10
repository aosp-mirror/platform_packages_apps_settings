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

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;

public class ZenModeEditNameIconFragment extends ZenModeEditNameIconFragmentBase {

    @Nullable
    @Override
    protected ZenMode onCreateInstantiateZenMode() {
        String modeId = getModeIdFromArguments();
        return modeId != null ? requireBackend().getMode(modeId) : null;
    }

    @Override
    public void onStart() {
        super.onStart();
        requireActivity().setTitle(R.string.zen_mode_rename_title);
    }

    @Override
    void saveMode(ZenMode mode) {
        String modeId = getModeIdFromArguments();
        ZenMode modeToUpdate = modeId != null ? requireBackend().getMode(modeId) : null;
        if (modeToUpdate == null) {
            // Huh, maybe it was deleted while we were choosing the icon? Unusual...
            Log.w(getLogTag(), "Couldn't fetch mode with id " + modeId
                    + " from the backend for saving. Discarding changes!");
            finish();
            return;
        }

        modeToUpdate.getRule().setName(mode.getRule().getName());
        modeToUpdate.getRule().setIconResId(mode.getRule().getIconResId());
        requireBackend().updateMode(modeToUpdate);
        finish();
    }

    @Nullable
    private String getModeIdFromArguments() {
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(EXTRA_AUTOMATIC_ZEN_RULE_ID)) {
            return bundle.getString(EXTRA_AUTOMATIC_ZEN_RULE_ID);
        } else {
            return null;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_MODE_EDIT_NAME_ICON;
    }

    @Override
    protected String getLogTag() {
        return "ZenModeEditNameIconFragment";
    }
}
