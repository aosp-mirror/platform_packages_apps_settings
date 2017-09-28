/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.AbstractEnableAdbPreferenceController;

/**
 * @deprecated in favor of {@link AdbPreferenceController}
 */
@Deprecated
public class EnableAdbPreferenceController extends AbstractEnableAdbPreferenceController
        implements PreferenceControllerMixin {

    private Dialog mAdbDialog;
    private boolean mDialogClicked;

    public EnableAdbPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void showConfirmationDialog(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        final TwoStatePreference twoStatePreference = (TwoStatePreference) preference;
        mDialogClicked = false;
        dismissConfirmationDialog();
        mAdbDialog = new AlertDialog.Builder(mContext).setMessage(
                mContext.getString(R.string.adb_warning_message))
                .setTitle(R.string.adb_warning_title)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    mDialogClicked = true;
                    writeAdbSetting(true);
                    twoStatePreference.setChecked(true);
                })
                .setNegativeButton(android.R.string.no,
                        (dialog, which) -> twoStatePreference.setChecked(false))
                .show();
        mAdbDialog.setOnDismissListener(dialog -> {
            // Assuming that onClick gets called first
            if (!mDialogClicked) {
                twoStatePreference.setChecked(false);
            }
            mAdbDialog = null;
        });
    }

    @Override
    public void dismissConfirmationDialog() {
        if (mAdbDialog != null) {
            mAdbDialog.dismiss();
            mAdbDialog = null;
        }
    }

    @Override
    public boolean isConfirmationDialogShowing() {
        return mAdbDialog != null;
    }
}
