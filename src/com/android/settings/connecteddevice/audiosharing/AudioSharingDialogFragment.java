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
import android.widget.Button;
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
import java.util.Locale;

public class AudioSharingDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingDialog";

    private static final String BUNDLE_KEY_DEVICE_ITEMS = "bundle_key_device_items";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /**
         * Called when users click the device item for sharing in the dialog.
         *
         * @param item The device item clicked.
         */
        void onItemClick(AudioSharingDeviceItem item);
    }

    private static DialogEventListener sListener;

    private View mRootView;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_START_AUDIO_SHARING;
    }

    /**
     * Display the {@link AudioSharingDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The connected device items eligible for audio sharing.
     * @param listener The callback to handle the user action on this dialog.
     */
    public static void show(
            Fragment host,
            ArrayList<AudioSharingDeviceItem> deviceItems,
            DialogEventListener listener) {
        if (!Flags.enableLeAudioSharing()) return;
        final FragmentManager manager = host.getChildFragmentManager();
        sListener = listener;
        if (manager.findFragmentByTag(TAG) == null) {
            final Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(BUNDLE_KEY_DEVICE_ITEMS, deviceItems);
            AudioSharingDialogFragment dialog = new AudioSharingDialogFragment();
            dialog.setArguments(bundle);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        ArrayList<AudioSharingDeviceItem> deviceItems =
                arguments.getParcelableArrayList(BUNDLE_KEY_DEVICE_ITEMS);
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity()).setTitle("Share audio").setCancelable(false);
        mRootView =
                LayoutInflater.from(builder.getContext())
                        .inflate(R.layout.dialog_audio_sharing, /* parent= */ null);
        TextView subTitle1 = mRootView.findViewById(R.id.share_audio_subtitle1);
        TextView subTitle2 = mRootView.findViewById(R.id.share_audio_subtitle2);
        if (deviceItems.isEmpty()) {
            subTitle1.setVisibility(View.INVISIBLE);
            subTitle2.setText(
                    "To start sharing audio, connect additional headphones that support LE audio");
        } else {
            subTitle1.setText(
                    String.format(
                            Locale.US,
                            "%d additional device%s connected",
                            deviceItems.size(),
                            deviceItems.size() > 1 ? "" : "s"));
            subTitle2.setText(
                    "The headphones you share audio with will hear videos and music playing on this"
                            + " phone");
        }
        RecyclerView recyclerView = mRootView.findViewById(R.id.btn_list);
        recyclerView.setAdapter(
                new AudioSharingDeviceAdapter(
                        deviceItems,
                        (AudioSharingDeviceItem item) -> {
                            sListener.onItemClick(item);
                            dismiss();
                        }));
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        Button cancelBtn = mRootView.findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(v -> dismiss());
        AlertDialog dialog = builder.setView(mRootView).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
