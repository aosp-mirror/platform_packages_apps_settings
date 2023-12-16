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

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.stream.Collectors;

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
        if (!AudioSharingUtils.isFeatureEnabled()) return;
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
                new AlertDialog.Builder(getActivity()).setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        View customTitle = inflater.inflate(R.layout.dialog_custom_title_audio_sharing, null);
        ImageView icon = customTitle.findViewById(R.id.title_icon);
        icon.setImageResource(R.drawable.ic_bt_audio_sharing);
        TextView title = customTitle.findViewById(R.id.title_text);
        View rootView = inflater.inflate(R.layout.dialog_audio_sharing, /* parent= */ null);
        TextView subTitle1 = rootView.findViewById(R.id.share_audio_subtitle1);
        TextView subTitle2 = rootView.findViewById(R.id.share_audio_subtitle2);
        RecyclerView recyclerView = rootView.findViewById(R.id.btn_list);
        Button shareBtn = rootView.findViewById(R.id.share_btn);
        Button cancelBtn = rootView.findViewById(R.id.cancel_btn);
        if (deviceItems.isEmpty()) {
            title.setText("Share your audio");
            subTitle2.setText(
                    "To start sharing audio, "
                            + "connect two pairs of headphones that support LE Audio");
            ImageView image = rootView.findViewById(R.id.share_audio_guidance);
            image.setVisibility(View.VISIBLE);
            builder.setNegativeButton("Close", null);
        } else if (deviceItems.size() == 1) {
            title.setText("Share your audio");
            subTitle1.setText(
                    deviceItems.stream()
                            .map(AudioSharingDeviceItem::getName)
                            .collect(Collectors.joining(" and ")));
            subTitle2.setText(
                    "This device's music and videos will play on both pairs of headphones");
            shareBtn.setText("Share audio");
            shareBtn.setOnClickListener(
                    v -> {
                        sListener.onItemClick(Iterables.getOnlyElement(deviceItems));
                        dismiss();
                    });
            cancelBtn.setOnClickListener(v -> dismiss());
            subTitle1.setVisibility(View.VISIBLE);
            shareBtn.setVisibility(View.VISIBLE);
            cancelBtn.setVisibility(View.VISIBLE);
        } else {
            title.setText("Share audio with another device");
            subTitle2.setText(
                    "This device's music and videos will play on the headphones you connect");
            recyclerView.setAdapter(
                    new AudioSharingDeviceAdapter(
                            deviceItems,
                            (AudioSharingDeviceItem item) -> {
                                sListener.onItemClick(item);
                                dismiss();
                            },
                            "Connect "));
            recyclerView.setLayoutManager(
                    new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
            recyclerView.setVisibility(View.VISIBLE);
            cancelBtn.setOnClickListener(v -> dismiss());
            cancelBtn.setVisibility(View.VISIBLE);
        }
        AlertDialog dialog = builder.setCustomTitle(customTitle).setView(rootView).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
