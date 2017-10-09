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

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.development.AbstractLogpersistPreferenceController;

/**
 * depreacted in favor of {@link LogdSizePreferenceControllerV2}
 */
@Deprecated
public class LogpersistPreferenceController extends AbstractLogpersistPreferenceController
        implements PreferenceControllerMixin {

    private Dialog mLogpersistClearDialog;

    LogpersistPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    @Override
    public void showConfirmationDialog(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        if (mLogpersistClearDialog != null) dismissConfirmationDialog();
        mLogpersistClearDialog = new AlertDialog.Builder(mContext)
                .setMessage(R.string.dev_logpersist_clear_warning_message)
                .setTitle(R.string.dev_logpersist_clear_warning_title)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> setLogpersistOff(true))
                .setNegativeButton(android.R.string.no, (dialog, which) -> updateLogpersistValues())
                .show();
        mLogpersistClearDialog.setOnDismissListener(dialog -> mLogpersistClearDialog = null);
    }

    @Override
    public void dismissConfirmationDialog() {
        if (mLogpersistClearDialog != null) {
            mLogpersistClearDialog.dismiss();
            mLogpersistClearDialog = null;
        }
    }

    @Override
    public boolean isConfirmationDialogShowing() {
        return mLogpersistClearDialog != null;
    }
}
