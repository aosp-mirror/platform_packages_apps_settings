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
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;
import java.util.Locale;

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

    @Nullable private static DialogEventListener sListener;
    @Nullable private static CachedBluetoothDevice sNewDevice;
    private static Pair<Integer, Object>[] sEventData = new Pair[0];

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE;
    }

    /**
     * Display the {@link AudioSharingDisconnectDialogFragment} dialog.
     *
     * <p>If the dialog is showing for the same group, update the dialog event listener.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The existing connected device items in audio sharing session.
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
        AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
        if (dialog != null) {
            int newGroupId = BluetoothUtils.getGroupId(newDevice);
            if (sNewDevice != null && newGroupId == BluetoothUtils.getGroupId(sNewDevice)) {
                Log.d(
                        TAG,
                        String.format(
                                Locale.US,
                                "Dialog is showing for the same device group %d, "
                                        + "update the content.",
                                newGroupId));
                sListener = listener;
                sNewDevice = newDevice;
                sEventData = eventData;
                return;
            } else {
                Log.d(
                        TAG,
                        String.format(
                                Locale.US,
                                "Dialog is showing for new device group %d, "
                                        + "dismiss current dialog.",
                                newGroupId));
                dialog.dismiss();
                var unused =
                        ThreadUtils.postOnBackgroundThread(
                                () ->
                                        FeatureFactory.getFeatureFactory()
                                                .getMetricsFeatureProvider()
                                                .action(
                                                        dialog.getContext(),
                                                        SettingsEnums
                                                        .ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS,
                                                        SettingsEnums
                                                        .DIALOG_AUDIO_SHARING_SWITCH_DEVICE));
            }
        }
        sListener = listener;
        sNewDevice = newDevice;
        sEventData = eventData;
        Log.d(TAG, "Show up the dialog.");
        final Bundle bundle = new Bundle();
        bundle.putParcelableList(BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS, deviceItems);
        bundle.putString(BUNDLE_KEY_NEW_DEVICE_NAME, newDevice.getName());
        AudioSharingDisconnectDialogFragment dialogFrag =
                new AudioSharingDisconnectDialogFragment();
        dialogFrag.setArguments(bundle);
        dialogFrag.show(manager, TAG);
    }

    /** Return the tag of {@link AudioSharingDisconnectDialogFragment} dialog. */
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
                arguments.getParcelable(BUNDLE_KEY_DEVICE_TO_DISCONNECT_ITEMS, List.class);
        AudioSharingDialogFactory.DialogBuilder builder =
                AudioSharingDialogFactory.newBuilder(getActivity())
                        .setTitle(R.string.audio_sharing_disconnect_dialog_title)
                        .setTitleIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setIsCustomBodyEnabled(true)
                        .setCustomMessage(R.string.audio_sharing_dialog_disconnect_content)
                        .setCustomNegativeButton(
                                com.android.settings.R.string.cancel,
                                v -> {
                                    mMetricsFeatureProvider.action(
                                            getContext(),
                                            SettingsEnums
                                            .ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED,
                                            sEventData);
                                    dismiss();
                                });
        if (deviceItems == null) {
            Log.d(TAG, "Create dialog error: null deviceItems");
            return builder.build();
        }
        builder.setCustomDeviceActions(
                new AudioSharingDeviceAdapter(
                        getContext(),
                        deviceItems,
                        (AudioSharingDeviceItem item) -> {
                            if (sListener != null) {
                                sListener.onItemClick(item);
                                mMetricsFeatureProvider.action(
                                        getContext(),
                                        SettingsEnums
                                        .ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED,
                                        sEventData);
                            }
                            dismiss();
                        },
                        AudioSharingDeviceAdapter.ActionType.REMOVE));
        return builder.build();
    }
}
