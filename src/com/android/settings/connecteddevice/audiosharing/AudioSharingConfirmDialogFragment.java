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

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class AudioSharingConfirmDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingConfirmDialog";

    @Override
    public int getMetricsCategory() {
        // TODO: add metrics category.
        return 0;
    }

    /**
     * Display the {@link AudioSharingConfirmDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     */
    public static void show(Fragment host) {
        if (!AudioSharingUtils.isFeatureEnabled()) return;
        FragmentManager manager = host.getChildFragmentManager();
        AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
        if (dialog != null) {
            Log.d(TAG, "Dialog is showing, return.");
            return;
        }
        Log.d(TAG, "Show up the confirm dialog.");
        AudioSharingConfirmDialogFragment dialogFrag = new AudioSharingConfirmDialogFragment();
        dialogFrag.show(manager, TAG);
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog dialog =
                AudioSharingDialogFactory.newBuilder(getActivity())
                        .setTitle(R.string.audio_sharing_confirm_dialog_title)
                        .setTitleIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setIsCustomBodyEnabled(true)
                        .setCustomMessage(R.string.audio_sharing_comfirm_dialog_content)
                        .setPositiveButton(com.android.settings.R.string.okay, (d, w) -> dismiss())
                        .build();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
}
