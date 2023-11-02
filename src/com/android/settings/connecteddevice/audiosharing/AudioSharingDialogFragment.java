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

package com.android.settings.connecteddevice.audiosharing;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.internal.widget.LinearLayoutManager;
import com.android.internal.widget.RecyclerView;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.flags.Flags;

import java.util.ArrayList;

public class AudioSharingDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingDialog";

    private View mRootView;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_START_AUDIO_SHARING;
    }

    /**
     * Display the {@link AudioSharingDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     */
    public static void show(Fragment host) {
        if (!Flags.enableLeAudioSharing()) return;
        final FragmentManager manager = host.getChildFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final AudioSharingDialogFragment dialog = new AudioSharingDialogFragment();
            dialog.show(manager, TAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity()).setTitle("Share audio");
        mRootView =
                LayoutInflater.from(builder.getContext())
                        .inflate(R.layout.dialog_audio_sharing, /* parent= */ null);
        // TODO: use real subtitle according to device count.
        TextView subTitle1 = mRootView.findViewById(R.id.share_audio_subtitle1);
        TextView subTitle2 = mRootView.findViewById(R.id.share_audio_subtitle2);
        subTitle1.setText("2 devices connected");
        subTitle2.setText("placeholder");
        RecyclerView recyclerView = mRootView.findViewById(R.id.btn_list);
        // TODO: use real audio sharing device list.
        ArrayList<String> devices = new ArrayList<>();
        devices.add("Buds 1");
        devices.add("Buds 2");
        recyclerView.setAdapter(
                new AudioSharingDeviceAdapter(
                        devices,
                        (int position) -> {
                            // TODO: add on click callback.
                        }));
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        return builder.setView(mRootView).create();
    }
}
