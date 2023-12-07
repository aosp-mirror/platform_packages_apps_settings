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

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;

public class AudioSharingJoinDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingJoinDialog";
    private static final String BUNDLE_KEY_DEVICE_ITEMS = "bundle_key_device_items";
    private static final String BUNDLE_KEY_NEW_DEVICE_NAME = "bundle_key_new_device_name";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /** Called when users click the share audio button in the dialog. */
        void onShareClick();
    }

    private static DialogEventListener sListener;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_START_AUDIO_SHARING;
    }

    /**
     * Display the {@link AudioSharingJoinDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The existing connected device items eligible for audio sharing.
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
        bundle.putParcelableArrayList(BUNDLE_KEY_DEVICE_ITEMS, deviceItems);
        bundle.putString(BUNDLE_KEY_NEW_DEVICE_NAME, newDeviceName);
        final AudioSharingJoinDialogFragment dialog = new AudioSharingJoinDialogFragment();
        dialog.setArguments(bundle);
        dialog.show(manager, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        ArrayList<AudioSharingDeviceItem> deviceItems =
                arguments.getParcelableArrayList(BUNDLE_KEY_DEVICE_ITEMS);
        String newDeviceName = arguments.getString(BUNDLE_KEY_NEW_DEVICE_NAME);
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity()).setCancelable(false);
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        View customTitle =
                inflater.inflate(R.layout.dialog_custom_title_audio_sharing, /* parent= */ null);
        ImageView icon = customTitle.findViewById(R.id.title_icon);
        icon.setImageResource(R.drawable.ic_bt_audio_sharing);
        TextView title = customTitle.findViewById(R.id.title_text);
        title.setText("Share your audio");
        View rootView = inflater.inflate(R.layout.dialog_audio_sharing_join, /* parent= */ null);
        TextView subtitle1 = rootView.findViewById(R.id.share_audio_subtitle1);
        TextView subtitle2 = rootView.findViewById(R.id.share_audio_subtitle2);
        if (deviceItems.isEmpty()) {
            subtitle1.setText(newDeviceName);
        } else {
            subtitle1.setText(
                    String.format(
                            Locale.US,
                            "%s and %s",
                            deviceItems.stream()
                                    .map(AudioSharingDeviceItem::getName)
                                    .collect(Collectors.joining(", ")),
                            newDeviceName));
        }
        subtitle2.setText("This device's music and videos will play on both pairs of headphones");
        Button shareBtn = rootView.findViewById(R.id.share_btn);
        Button cancelBtn = rootView.findViewById(R.id.cancel_btn);
        shareBtn.setOnClickListener(
                v -> {
                    sListener.onShareClick();
                    dismiss();
                });
        shareBtn.setText("Share audio");
        cancelBtn.setOnClickListener(v -> dismiss());
        Dialog dialog = builder.setCustomTitle(customTitle).setView(rootView).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
