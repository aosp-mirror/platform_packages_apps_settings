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

package com.android.settings.connecteddevice.audiosharing;

import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class AudioSharingDialogHelper {
    private static final String TAG = "AudioSharingDialogHelper";

    /** Updates the alert dialog message style. */
    public static void updateMessageStyle(@NonNull AlertDialog dialog) {
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            Typeface typeface = Typeface.create(Typeface.DEFAULT_FAMILY, Typeface.NORMAL);
            messageView.setTypeface(typeface);
            messageView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
            messageView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        } else {
            Log.w(TAG, "Fail to update dialog: message view is null");
        }
    }

    /** Returns the alert dialog by tag if it is showing. */
    @Nullable
    public static AlertDialog getDialogIfShowing(
            @NonNull FragmentManager manager, @NonNull String tag) {
        Fragment dialog = manager.findFragmentByTag(tag);
        return dialog instanceof DialogFragment
                && ((DialogFragment) dialog).getDialog() != null
                && ((DialogFragment) dialog).getDialog().isShowing()
                && ((DialogFragment) dialog).getDialog() instanceof AlertDialog
                ? (AlertDialog) ((DialogFragment) dialog).getDialog()
                : null;
    }

    private AudioSharingDialogHelper() {}
}
