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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/** Fragment to show a confirm dialog. The caller should implement onConfirmListener. */
public class ConfirmDialogFragment extends BaseDialogFragment
        implements DialogInterface.OnClickListener {
    private static final String TAG = "ConfirmDialogFragment";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MSG = "msg";
    private static final String ARG_POS_BUTTON_STRING = "pos_button_string";
    private static final String ARG_NEG_BUTTON_STRING = "neg_button_string";
    private static final String ARG_LIST = "list";

    /**
     * Interface defining the method that will be invoked when the user has done with the dialog.
     */
    public interface OnConfirmListener {
        /**
         * @param tag          The tag in the caller.
         * @param confirmed    True if the user has clicked the positive button. False if the
         *                     user has
         *                     clicked the negative button or cancel the dialog.
         * @param itemPosition It is the position of item, if user selects one of the list item.
         *                     If the user select "cancel" or the dialog does not have list, then
         *                     the value is -1.
         */
        void onConfirm(int tag, boolean confirmed, int itemPosition);
    }

    /** Displays a confirmation dialog which has confirm and cancel buttons. */
    public static <T> void show(
            FragmentActivity activity,
            Class<T> callbackInterfaceClass,
            int tagInCaller,
            String title,
            String msg,
            String posButtonString,
            String negButtonString) {
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putCharSequence(ARG_MSG, msg);
        arguments.putString(ARG_POS_BUTTON_STRING, posButtonString);
        arguments.putString(ARG_NEG_BUTTON_STRING, negButtonString);
        setListener(activity, null, callbackInterfaceClass, tagInCaller, arguments);
        fragment.setArguments(arguments);
        fragment.show(activity.getSupportFragmentManager(), TAG);
    }

    /** Displays a confirmation dialog which has confirm and cancel buttons and carrier list.*/
    public static <T> void show(
            FragmentActivity activity,
            Class<T> callbackInterfaceClass,
            int tagInCaller,
            String title,
            String msg,
            String posButtonString,
            String negButtonString,
            ArrayList<String> list) {
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putCharSequence(ARG_MSG, msg);
        arguments.putString(ARG_POS_BUTTON_STRING, posButtonString);
        arguments.putString(ARG_NEG_BUTTON_STRING, negButtonString);
        arguments.putStringArrayList(ARG_LIST, list);
        setListener(activity, null, callbackInterfaceClass, tagInCaller, arguments);
        fragment.setArguments(arguments);
        fragment.show(activity.getSupportFragmentManager(), TAG);
    }

    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString(ARG_TITLE);
        String message = getArguments().getString(ARG_MSG);
        String posBtnString = getArguments().getString(ARG_POS_BUTTON_STRING);
        String negBtnString = getArguments().getString(ARG_NEG_BUTTON_STRING);
        ArrayList<String> list = getArguments().getStringArrayList(ARG_LIST);

        Log.i(TAG, "Showing dialog with title =" + title);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setPositiveButton(posBtnString, this)
                .setNegativeButton(negBtnString, this);
        View content = LayoutInflater.from(getContext()).inflate(
                R.layout.sim_confirm_dialog_multiple_enabled_profiles_supported, null);

        if (list != null && !list.isEmpty() && content != null) {
            Log.i(TAG, "list =" + list.toString());

            if (!TextUtils.isEmpty(title)) {
                View titleView = LayoutInflater.from(getContext()).inflate(
                        R.layout.sim_confirm_dialog_title_multiple_enabled_profiles_supported,
                        null);
                TextView titleTextView = titleView.findViewById(R.id.title);
                titleTextView.setText(title);
                builder.setCustomTitle(titleTextView);
            }
            TextView dialogMessage = content.findViewById(R.id.msg);
            if (!TextUtils.isEmpty(message) && dialogMessage != null) {
                dialogMessage.setText(message);
                dialogMessage.setVisibility(View.VISIBLE);
            }

            final ListView lvItems = content.findViewById(R.id.carrier_list);
            if (lvItems != null) {
                lvItems.setVisibility(View.VISIBLE);
                lvItems.setAdapter(new ButtonArrayAdapter(getContext(), list));
            }
            final LinearLayout infoOutline = content.findViewById(R.id.info_outline_layout);
            if (infoOutline != null) {
                infoOutline.setVisibility(View.VISIBLE);
            }
            builder.setView(content);
        } else {
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            if (!TextUtils.isEmpty(message)) {
                builder.setMessage(message);
            }
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.i(TAG, "dialog onClick =" + which);

        informCaller(which == DialogInterface.BUTTON_POSITIVE, -1);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        informCaller(false, -1);
    }

    private void informCaller(boolean confirmed, int itemPosition) {
        OnConfirmListener listener;
        try {
            listener = getListener(OnConfirmListener.class);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Do nothing and return.", e);
            return;
        }
        if (listener == null) {
            return;
        }
        listener.onConfirm(getTagInCaller(), confirmed, itemPosition);
    }

    private class ButtonArrayAdapter extends ArrayAdapter<String> {
        private final List<String> mList;

        ButtonArrayAdapter(Context context, List<String> list) {
            super(context, R.layout.sim_confirm_dialog_item_multiple_enabled_profiles_supported,
                    list);
            mList = list;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setOnClickListener(v -> {
                Log.i(TAG, "list onClick =" + position);
                Log.i(TAG, "list item =" + mList.get(position));

                if (position == mList.size() - 1) {
                    // user select the "cancel" item;
                    informCaller(false, -1);
                } else {
                    informCaller(true, position);
                }
            });
            return view;
        }
    }
}
