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
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import java.util.List;

public class AudioSharingJoinDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "AudioSharingJoinDialog";

    private static final String BUNDLE_KEY_DEVICE_ITEMS = "bundle_key_device_items";
    private static final String BUNDLE_KEY_NEW_DEVICE_NAME = "bundle_key_new_device_name";

    // The host creates an instance of this dialog fragment must implement this interface to receive
    // event callbacks.
    public interface DialogEventListener {
        /** Called when users click the share audio button in the dialog. */
        void onShareClick();

        /** Called when users click the cancel button in the dialog. */
        void onCancelClick();
    }

    @Nullable private static DialogEventListener sListener;
    @Nullable private static CachedBluetoothDevice sNewDevice;
    private static Pair<Integer, Object>[] sEventData = new Pair[0];

    @Override
    public int getMetricsCategory() {
        return BluetoothUtils.isBroadcasting(Utils.getLocalBtManager(getContext()))
                ? SettingsEnums.DIALOG_AUDIO_SHARING_ADD_DEVICE
                : SettingsEnums.DIALOG_START_AUDIO_SHARING;
    }

    /**
     * Display the {@link AudioSharingJoinDialogFragment} dialog.
     *
     * <p>If the dialog is showing, update the dialog message and event listener.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The existing connected device items eligible for audio sharing.
     * @param newDevice The latest connected device triggered this dialog.
     * @param listener The callback to handle the user action on this dialog.
     * @param eventData The eventData to log with for dialog onClick events.
     */
    public static void show(
            @NonNull Fragment host,
            @NonNull List<AudioSharingDeviceItem> deviceItems,
            @NonNull CachedBluetoothDevice newDevice,
            @NonNull DialogEventListener listener,
            @NonNull Pair<Integer, Object>[] eventData) {
        if (!BluetoothUtils.isAudioSharingEnabled()) return;
        final FragmentManager manager;
        try {
            manager = host.getChildFragmentManager();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to show dialog: " + e.getMessage());
            return;
        }
        sListener = listener;
        sNewDevice = newDevice;
        sEventData = eventData;
        AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
        if (dialog != null) {
            Log.d(TAG, "Dialog is showing, update the content.");
            updateDialog(deviceItems, newDevice.getName(), dialog);
        } else {
            Log.d(TAG, "Show up the dialog.");
            final Bundle bundle = new Bundle();
            bundle.putParcelableList(BUNDLE_KEY_DEVICE_ITEMS, deviceItems);
            bundle.putString(BUNDLE_KEY_NEW_DEVICE_NAME, newDevice.getName());
            final AudioSharingJoinDialogFragment dialogFrag = new AudioSharingJoinDialogFragment();
            dialogFrag.setArguments(bundle);
            dialogFrag.show(manager, TAG);
        }
    }

    /** Return the tag of {@link AudioSharingJoinDialogFragment} dialog. */
    public static @NonNull String tag() {
        return TAG;
    }

    /** Get the latest connected device which triggers the dialog. */
    public @Nullable CachedBluetoothDevice getDevice() {
        return sNewDevice;
    }

    /** Test only: get the {@link DialogEventListener} passed to the dialog. */
    @VisibleForTesting
    @Nullable
    DialogEventListener getListener() {
        return sListener;
    }

    /** Test only: get the event data passed to the dialog. */
    @VisibleForTesting
    @NonNull
    Pair<Integer, Object>[] getEventData() {
        return sEventData;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        List<AudioSharingDeviceItem> deviceItems =
                arguments.getParcelable(BUNDLE_KEY_DEVICE_ITEMS, List.class);
        String newDeviceName = arguments.getString(BUNDLE_KEY_NEW_DEVICE_NAME);
        AlertDialog dialog =
                AudioSharingDialogFactory.newBuilder(getActivity())
                        .setTitle(R.string.audio_sharing_share_dialog_title)
                        .setTitleIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setIsCustomBodyEnabled(true)
                        .setCustomMessage(R.string.audio_sharing_dialog_share_content)
                        .setCustomPositiveButton(
                                R.string.audio_sharing_share_button_label,
                                v -> {
                                    if (sListener != null) {
                                        sListener.onShareClick();
                                        mMetricsFeatureProvider.action(
                                                getContext(),
                                                SettingsEnums
                                                .ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED,
                                                sEventData);
                                    }
                                    dismiss();
                                })
                        .setCustomNegativeButton(
                                getMetricsCategory() == SettingsEnums.DIALOG_START_AUDIO_SHARING
                                        ? getString(
                                                R.string.audio_sharing_switch_active_button_label,
                                                newDeviceName)
                                        : getString(R.string.audio_sharing_no_thanks_button_label),
                                v -> {
                                    if (sListener != null) {
                                        sListener.onCancelClick();
                                        mMetricsFeatureProvider.action(
                                                getContext(),
                                                SettingsEnums
                                                .ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED,
                                                sEventData);
                                    }
                                    dismiss();
                                })
                        .build();
        if (deviceItems == null) {
            Log.d(TAG, "Fail to create dialog: null deviceItems");
        } else {
            updateDialog(deviceItems, newDeviceName, dialog);
        }
        dialog.show();
        AudioSharingDialogHelper.updateMessageStyle(dialog);
        return dialog;
    }

    private static void updateDialog(
            List<AudioSharingDeviceItem> deviceItems,
            String newDeviceName,
            @NonNull AlertDialog dialog) {
        // Only dialog message can be updated when the dialog is showing.
        // Thus we put the device name for sharing as the dialog message.
        if (deviceItems.isEmpty()) {
            dialog.setMessage(newDeviceName);
        } else {
            dialog.setMessage(
                    dialog.getContext()
                            .getString(
                                    R.string.audio_sharing_share_dialog_subtitle,
                                    deviceItems.get(0).getName(),
                                    newDeviceName));
        }
    }
}
