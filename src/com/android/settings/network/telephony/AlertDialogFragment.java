/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

/** Fragment to show an alert dialog which only has the positive button. */
public class AlertDialogFragment extends BaseDialogFragment
        implements DialogInterface.OnClickListener {
    private static final String TAG = "AlertDialogFragment";

    // Arguments
    private static final String ARG_TITLE = "title";
    private static final String ARG_MSG = "msg";

    /**
     * @param activity
     * @param title
     * @param msg
     */
    public static void show(FragmentActivity activity, String title, String msg) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putString(ARG_MSG, msg);
        fragment.setArguments(arguments);
        fragment.show(activity.getSupportFragmentManager(), TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getContext())
                        .setTitle(getArguments().getString(ARG_TITLE))
                        .setPositiveButton(android.R.string.ok, this);
        if (!TextUtils.isEmpty(getArguments().getString(ARG_MSG))) {
            builder.setMessage(getArguments().getString(ARG_MSG));
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (getActivity() != null) {
            getActivity().finish();
        }
        super.dismiss();
    }
}
