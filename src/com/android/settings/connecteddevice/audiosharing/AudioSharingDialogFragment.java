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

import com.google.common.collect.Iterables;

import java.util.List;

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

        /** Called when users click the cancel button in the dialog. */
        void onCancelClick();
    }

    @Nullable private static DialogEventListener sListener;
    private static Pair<Integer, Object>[] sEventData = new Pair[0];

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_AUDIO_SHARING_ADD_DEVICE;
    }

    /**
     * Display the {@link AudioSharingDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The connected device items eligible for audio sharing.
     * @param listener The callback to handle the user action on this dialog.
     * @param eventData The eventData to log with for dialog onClick events.
     */
    public static void show(
            @NonNull Fragment host,
            @NonNull List<AudioSharingDeviceItem> deviceItems,
            @NonNull DialogEventListener listener,
            @NonNull Pair<Integer, Object>[] eventData) {
        if (!AudioSharingUtils.isFeatureEnabled()) return;
        final FragmentManager manager = host.getChildFragmentManager();
        sListener = listener;
        sEventData = eventData;
        AlertDialog dialog = AudioSharingDialogHelper.getDialogIfShowing(manager, TAG);
        if (dialog != null) {
            Log.d(TAG, "Dialog is showing, return.");
            return;
        }
        Log.d(TAG, "Show up the dialog.");
        final Bundle bundle = new Bundle();
        bundle.putParcelableList(BUNDLE_KEY_DEVICE_ITEMS, deviceItems);
        AudioSharingDialogFragment dialogFrag = new AudioSharingDialogFragment();
        dialogFrag.setArguments(bundle);
        dialogFrag.show(manager, TAG);
    }

    /** Return the tag of {@link AudioSharingDialogFragment} dialog. */
    public static @NonNull String tag() {
        return TAG;
    }

    /** Test only: get the event data passed to the dialog. */
    @VisibleForTesting
    @NonNull
    Pair<Integer, Object>[] getEventData() {
        return sEventData;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        List<AudioSharingDeviceItem> deviceItems =
                arguments.getParcelable(BUNDLE_KEY_DEVICE_ITEMS, List.class);
        AudioSharingDialogFactory.DialogBuilder builder =
                AudioSharingDialogFactory.newBuilder(getActivity())
                        .setTitleIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setIsCustomBodyEnabled(true);
        if (deviceItems == null) {
            Log.d(TAG, "Create dialog error: null deviceItems");
            return builder.build();
        }
        if (deviceItems.isEmpty()) {
            builder.setTitle(R.string.audio_sharing_share_dialog_title)
                    .setCustomImage(R.drawable.audio_sharing_guidance)
                    .setCustomMessage(R.string.audio_sharing_dialog_connect_device_content)
                    .setNegativeButton(
                            R.string.audio_sharing_close_button_label,
                            (dig, which) -> onCancelClick());
        } else if (deviceItems.size() == 1) {
            AudioSharingDeviceItem deviceItem = Iterables.getOnlyElement(deviceItems);
            builder.setTitle(
                            getString(
                                    R.string.audio_sharing_share_with_dialog_title,
                                    deviceItem.getName()))
                    .setCustomMessage(R.string.audio_sharing_dialog_share_content)
                    .setCustomPositiveButton(
                            R.string.audio_sharing_share_button_label,
                            v -> {
                                if (sListener != null) {
                                    sListener.onItemClick(deviceItem);
                                    mMetricsFeatureProvider.action(
                                            getContext(),
                                            SettingsEnums
                                            .ACTION_AUDIO_SHARING_DIALOG_POSITIVE_BTN_CLICKED,
                                            sEventData);
                                }
                                dismiss();
                            })
                    .setCustomNegativeButton(
                            R.string.audio_sharing_no_thanks_button_label, v -> onCancelClick());
        } else {
            builder.setTitle(R.string.audio_sharing_share_with_more_dialog_title)
                    .setCustomMessage(R.string.audio_sharing_dialog_share_more_content)
                    .setCustomDeviceActions(
                            new AudioSharingDeviceAdapter(
                                    getContext(),
                                    deviceItems,
                                    (AudioSharingDeviceItem item) -> {
                                        if (sListener != null) {
                                            sListener.onItemClick(item);
                                        }
                                        dismiss();
                                    },
                                    AudioSharingDeviceAdapter.ActionType.SHARE))
                    .setCustomNegativeButton(
                            com.android.settings.R.string.cancel, v -> onCancelClick());
        }
        return builder.build();
    }

    private void onCancelClick() {
        if (sListener != null) {
            sListener.onCancelClick();
            mMetricsFeatureProvider.action(
                    getContext(),
                    SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_NEGATIVE_BTN_CLICKED,
                    sEventData);
        }
        dismiss();
    }
}
