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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.ArrayList;

public class AudioSharingDisconnectDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingDisconnectDialog";

    private static final String BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS =
            "bundle_key_device_to_disconnect_items";
    private static final String BUNDLE_KEY_NEW_DEVICE_NAME = "bundle_key_new_device_name";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /**
         * Called when users click the device item to disconnect from the audio sharing in the
         * dialog.
         *
         * @param item The device item clicked.
         */
        void onItemClick(AudioSharingDeviceItem item);
    }

    private static DialogEventListener sListener;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE;
    }

    /**
     * Display the {@link AudioSharingDisconnectDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The existing connected device items in audio sharing session.
     * @param newDeviceName The name of the latest connected device triggered this dialog.
     * @param listener The callback to handle the user action on this dialog.
     */
    public static void show(
            Fragment host,
            ArrayList<AudioSharingDeviceItem> deviceItems,
            String newDeviceName,
            DialogEventListener listener) {
        if (!AudioSharingUtils.isFeatureEnabled()) return;
        final FragmentManager manager = host.getChildFragmentManager();
        sListener = listener;
        final Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS, deviceItems);
        bundle.putString(BUNDLE_KEY_NEW_DEVICE_NAME, newDeviceName);
        AudioSharingDisconnectDialogFragment dialog = new AudioSharingDisconnectDialogFragment();
        dialog.setArguments(bundle);
        dialog.show(manager, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        ArrayList<AudioSharingDeviceItem> deviceItems =
                arguments.getParcelableArrayList(BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS);
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity()).setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        View customTitle = inflater.inflate(R.layout.dialog_custom_title_audio_sharing, null);
        ImageView icon = customTitle.findViewById(R.id.title_icon);
        icon.setImageResource(R.drawable.ic_bt_audio_sharing);
        TextView title = customTitle.findViewById(R.id.title_text);
        title.setText("Choose a device to disconnect");
        View rootView =
                inflater.inflate(R.layout.dialog_audio_sharing_disconnect, /* parent= */ null);
        TextView subTitle = rootView.findViewById(R.id.share_audio_disconnect_description);
        subTitle.setText("Only 2 devices can share audio at a time");
        RecyclerView recyclerView = rootView.findViewById(R.id.device_btn_list);
        recyclerView.setAdapter(
                new AudioSharingDeviceAdapter(
                        deviceItems,
                        (AudioSharingDeviceItem item) -> {
                            sListener.onItemClick(item);
                            dismiss();
                        },
                        "Disconnect "));
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        Button cancelBtn = rootView.findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(
                v -> {
                    dismiss();
                });
        AlertDialog dialog = builder.setCustomTitle(customTitle).setView(rootView).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
