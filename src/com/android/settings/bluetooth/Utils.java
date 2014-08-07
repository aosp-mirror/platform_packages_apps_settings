/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;

/**
 * Utils is a helper class that contains constants for various
 * Android resource IDs, debug logging flags, and static methods
 * for creating dialogs.
 */
final class Utils {
    static final boolean V = false; // verbose logging
    static final boolean D = true;  // regular logging

    private Utils() {
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
        case BluetoothProfile.STATE_CONNECTED:
            return R.string.bluetooth_connected;
        case BluetoothProfile.STATE_CONNECTING:
            return R.string.bluetooth_connecting;
        case BluetoothProfile.STATE_DISCONNECTED:
            return R.string.bluetooth_disconnected;
        case BluetoothProfile.STATE_DISCONNECTING:
            return R.string.bluetooth_disconnecting;
        default:
            return 0;
        }
    }

    // Create (or recycle existing) and show disconnect dialog.
    static AlertDialog showDisconnectDialog(Context context,
            AlertDialog dialog,
            DialogInterface.OnClickListener disconnectListener,
            CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context)
                    .setPositiveButton(android.R.string.ok, disconnectListener)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            // use disconnectListener for the correct profile(s)
            CharSequence okText = context.getText(android.R.string.ok);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    // TODO: wire this up to show connection errors...
    static void showConnectingError(Context context, String name) {
        // if (!mIsConnectingErrorPossible) {
        //     return;
        // }
        // mIsConnectingErrorPossible = false;

        showError(context, name, R.string.bluetooth_connecting_error_message);
    }

    static void showError(Context context, String name, int messageResId) {
        String message = context.getString(messageResId, name);
        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(context);
        Context activity = manager.getForegroundActivity();
        if(manager.isForegroundActivity()) {
            new AlertDialog.Builder(activity)
                .setTitle(R.string.bluetooth_error_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update the search Index for a specific class name and resources.
     */
    public static void updateSearchIndex(Context context, String className, String title,
            String screenTitle, int iconResId, boolean enabled) {
        SearchIndexableRaw data = new SearchIndexableRaw(context);
        data.className = className;
        data.title = title;
        data.screenTitle = screenTitle;
        data.iconResId = iconResId;
        data.enabled = enabled;

        Index.getInstance(context).updateFromSearchIndexableData(data);
    }
}
