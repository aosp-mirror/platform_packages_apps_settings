/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.display;

import android.app.UiModeManager;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.settings.core.TogglePreferenceController;

public class DarkUIPreferenceController extends TogglePreferenceController {

    public static final String DARK_MODE_PREFS = "dark_mode_prefs";
    public static final String PREF_DARK_MODE_DIALOG_SEEN = "dark_mode_dialog_seen";
    public static final int DIALOG_SEEN = 1;
    private UiModeManager mUiModeManager;
    private Context mContext;
    private Fragment mFragment;

    public DarkUIPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mUiModeManager = context.getSystemService(UiModeManager.class);
    }

    @Override
    public boolean isChecked() {
        return mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final boolean dialogSeen =
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.DARK_MODE_DIALOG_SEEN, 0) == DIALOG_SEEN;
        if (!dialogSeen && isChecked) {
            showDarkModeDialog();
            return false;
        }
        mUiModeManager.setNightMode(isChecked
                ? UiModeManager.MODE_NIGHT_YES
                : UiModeManager.MODE_NIGHT_NO);
        return true;
    }

    private void showDarkModeDialog() {
        final DarkUIInfoDialogFragment frag = new DarkUIInfoDialogFragment();
        if (mFragment.getFragmentManager() != null) {
            frag.show(mFragment.getFragmentManager(), getClass().getName());
        }
    }

    @VisibleForTesting
    void setUiModeManager(UiModeManager uiModeManager) {
        mUiModeManager = uiModeManager;
    }

    public void setParentFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
