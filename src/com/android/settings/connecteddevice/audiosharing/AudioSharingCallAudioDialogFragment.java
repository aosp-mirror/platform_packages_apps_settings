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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;

import java.util.List;

/** Provides a dialog to choose the active device for calls and alarms. */
public class AudioSharingCallAudioDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "CallsAndAlarmsDialog";
    private static final String BUNDLE_KEY_DEVICE_ITEMS = "bundle_key_device_items";
    private static final String BUNDLE_KEY_CHECKED_ITEM_INDEX = "bundle_key_checked_index";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /**
         * Called when users click the device item to set active for calls and alarms in the dialog.
         *
         * @param item The device item clicked.
         */
        void onItemClick(AudioSharingDeviceItem item);
    }

    @Nullable private static DialogEventListener sListener;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_AUDIO_SHARING_CALL_AUDIO;
    }

    /**
     * Display the {@link AudioSharingCallAudioDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The connected device items in audio sharing session.
     * @param listener The callback to handle the user action on this dialog.
     */
    public static void show(
            @NonNull Fragment host,
            @NonNull List<AudioSharingDeviceItem> deviceItems,
            int checkedItemIndex,
            @NonNull DialogEventListener listener) {
        if (!BluetoothUtils.isAudioSharingEnabled()) return;
        final FragmentManager manager;
        try {
            manager = host.getChildFragmentManager();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to show dialog: " + e.getMessage());
            return;
        }
        sListener = listener;
        if (manager.findFragmentByTag(TAG) == null) {
            final Bundle bundle = new Bundle();
            bundle.putParcelableList(BUNDLE_KEY_DEVICE_ITEMS, deviceItems);
            bundle.putInt(BUNDLE_KEY_CHECKED_ITEM_INDEX, checkedItemIndex);
            final AudioSharingCallAudioDialogFragment dialog =
                    new AudioSharingCallAudioDialogFragment();
            dialog.setArguments(bundle);
            dialog.show(manager, TAG);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        List<AudioSharingDeviceItem> deviceItems =
                arguments.getParcelable(BUNDLE_KEY_DEVICE_ITEMS, List.class);
        int checkedItemIndex = arguments.getInt(BUNDLE_KEY_CHECKED_ITEM_INDEX, -1);
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.audio_sharing_call_audio_title);
        if (deviceItems == null) {
            Log.d(TAG, "Create dialog error: null deviceItems");
            return builder.create();
        }
        String[] choices =
                deviceItems.stream().map(AudioSharingDeviceItem::getName).toArray(String[]::new);
        builder.setSingleChoiceItems(
                choices,
                checkedItemIndex,
                (dialog, which) -> {
                    if (sListener != null) {
                        sListener.onItemClick(deviceItems.get(which));
                    }
                });
        return builder.create();
    }
}
