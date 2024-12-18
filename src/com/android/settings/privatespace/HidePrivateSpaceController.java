/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import static com.android.settings.privatespace.PrivateSpaceMaintainer.HIDE_PRIVATE_SPACE_ENTRY_POINT_DISABLED_VAL;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.HIDE_PRIVATE_SPACE_ENTRY_POINT_ENABLED_VAL;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Toggle Preference Controller responsible for managing the visibility of private space entry point
 * in the "All Apps" section. This includes:
 *
 * <p>Toggling the entry point's visibility (hiding/unhiding)
 *
 * <p>Displaying a dialog to inform the user that the entry point will be hidden when private space
 * is locked (if the hide option is enabled)
 */
public class HidePrivateSpaceController extends TogglePreferenceController {
    private final PrivateSpaceMaintainer mPrivateSpaceMaintainer;

    public HidePrivateSpaceController(Context context, String key) {
        super(context, key);
        mPrivateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(context);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return android.os.Flags.allowPrivateProfile()
                        && android.multiuser.Flags.enablePrivateSpaceFeatures()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return mPrivateSpaceMaintainer.getHidePrivateSpaceEntryPointSetting()
                != HIDE_PRIVATE_SPACE_ENTRY_POINT_DISABLED_VAL;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mPrivateSpaceMaintainer.setHidePrivateSpaceEntryPointSetting(
                isChecked ? HIDE_PRIVATE_SPACE_ENTRY_POINT_ENABLED_VAL
                        : HIDE_PRIVATE_SPACE_ENTRY_POINT_DISABLED_VAL);
        if (isChecked) {
            showAlertDialog();
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    private void showAlertDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.private_space_hide_dialog_title)
                .setMessage(R.string.private_space_hide_dialog_message)
                .setPositiveButton(
                        R.string.private_space_hide_dialog_button,
                        (DialogInterface dialog, int which) -> {
                            dialog.dismiss();
                        })
                .show();
    }
}
