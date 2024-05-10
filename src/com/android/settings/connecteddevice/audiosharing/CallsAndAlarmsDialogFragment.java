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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.ArrayList;

/** Provides a dialog to choose the active device for calls and alarms. */
public class CallsAndAlarmsDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = "CallsAndAlarmsDialog";
    private static final String BUNDLE_KEY_DEVICE_ITEMS = "bundle_key_device_items";

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

    private static DialogEventListener sListener;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_ACTIVE;
    }

    /**
     * Display the {@link CallsAndAlarmsDialogFragment} dialog.
     *
     * @param host The Fragment this dialog will be hosted.
     * @param deviceItems The connected device items in audio sharing session.
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
            final CallsAndAlarmsDialogFragment dialog = new CallsAndAlarmsDialogFragment();
            dialog.setArguments(bundle);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        ArrayList<AudioSharingDeviceItem> deviceItems =
                arguments.getParcelableArrayList(BUNDLE_KEY_DEVICE_ITEMS);
        int checkedItem = -1;
        // deviceItems is ordered. The active device is put in the first place if it does exist
        if (!deviceItems.isEmpty() && deviceItems.get(0).isActive()) checkedItem = 0;
        String[] choices =
                deviceItems.stream().map(AudioSharingDeviceItem::getName).toArray(String[]::new);
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.calls_and_alarms_device_title)
                        .setSingleChoiceItems(
                                choices,
                                checkedItem,
                                (dialog, which) -> {
                                    sListener.onItemClick(deviceItems.get(which));
                                });
        return builder.create();
    }
}
