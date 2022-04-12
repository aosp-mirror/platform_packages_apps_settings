/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.ArrayList;

/**
 * This Dialog allowed users to do some actions for broadcast media or find the
 * nearby broadcast sources.
 */
public class BluetoothBroadcastDialog extends InstrumentedDialogFragment {

    private static final String TAG = "BTBroadcastsDialog";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowsDialog(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final boolean isMediaPlaying = isMediaPlaying();

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(isMediaPlaying ? R.string.bluetooth_find_broadcast
                : R.string.bluetooth_broadcast_dialog_title);
        builder.setMessage(isMediaPlaying ? R.string.bluetooth_broadcast_dialog_find_message
                : R.string.bluetooth_broadcast_dialog_broadcast_message);

        ArrayList<String> optionList = new ArrayList<String>();
        if (!isMediaPlaying) {
            optionList.add(context.getString(R.string.bluetooth_broadcast_dialog_title));
        }
        optionList.add(context.getString(R.string.bluetooth_find_broadcast));
        optionList.add(context.getString(android.R.string.cancel));

        View content = LayoutInflater.from(context).inflate(
                R.layout.sim_confirm_dialog_multiple_enabled_profiles_supported, null);

        if (content != null) {
            Log.i(TAG, "list =" + optionList.toString());

            final ArrayAdapter<String> arrayAdapterItems = new ArrayAdapter<String>(
                    context,
                    R.layout.sim_confirm_dialog_item_multiple_enabled_profiles_supported,
                    optionList);
            final ListView lvItems = content.findViewById(R.id.carrier_list);
            if (lvItems != null) {
                lvItems.setVisibility(View.VISIBLE);
                lvItems.setAdapter(arrayAdapterItems);
                lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        Log.i(TAG, "list onClick =" + position);
                        Log.i(TAG, "list item =" + optionList.get(position));

                        if (position == optionList.size() - 1) {
                            // The last position in the options is the Cancel button. So when
                            // the user clicks the button, we do nothing but dismiss the dialog.
                            dismiss();
                        } else {
                            if (optionList.get(position).equals(
                                    context.getString(R.string.bluetooth_find_broadcast))) {
                                launchFindBroadcastsActivity();
                            } else {
                                launchMediaOutputBroadcastDialog();
                            }
                        }
                    }
                });
            }
            builder.setView(content);
        } else {
            Log.i(TAG, "optionList is empty");
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private boolean isMediaPlaying() {
        return true;
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public int getMetricsCategory() {
        //TODO(b/228255796) : add new enum for find broadcast fragment
        return SettingsEnums.PAGE_UNKNOWN;
    }

    private void launchFindBroadcastsActivity() {

    }

    private void launchMediaOutputBroadcastDialog() {

    }
}
