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
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.android.settings.R;

/** Fragment to show a progress dialog. */
public class ProgressDialogFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";

    private static final String TAG = "ProgressDialogFragment";

    private OnDismissCallback mDismissCallback;

    // Host fragment is optional to implement this interface.
    public interface OnDismissCallback {
        // Action performed when the progress dialog is dismissed.
        void onProgressDialogDismiss();
    }

    /**
     * Check whether there is already a showing progress dialog. If yes and the title of the showing
     * one is the same with the new coming one, just return and do nothing. If the title of the
     * showing one is different from the new one, remove the showing one and create a new dialog for
     * the new one. If there is no progress dialog right now, just create a new one.
     */
    public static void show(FragmentManager fm, String title, OnDismissCallback dismissCallback) {
        ProgressDialogFragment fragment = (ProgressDialogFragment) fm.findFragmentByTag(TAG);
        if (fragment != null
                && TextUtils.equals(fragment.getArguments().getString(ARG_TITLE), title)) {
            return;
        }

        FragmentTransaction ft = fm.beginTransaction();
        if (fragment != null) {
            ft.remove(fragment);
        }

        fragment = new ProgressDialogFragment();
        fragment.setDismissCallback(dismissCallback);

        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        fragment.setArguments(arguments);
        fragment.show(ft, TAG);
    }

    /**
     * Called by the caller activity or fragment when the progress is finished.
     *
     * @param fm The fragment manager.
     */
    public static void dismiss(FragmentManager fm) {
        DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(TAG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    @Override
    @SuppressWarnings("deprecation") // ProgressDialog is deprecated but is intended UX for now
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.sim_progress_dialog_rounded_bg);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage(getArguments().getString(ARG_TITLE));
        dialog.setOnKeyListener(
                (progressDialog, keyCode, event) -> KeyEvent.KEYCODE_BACK == keyCode);

        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mDismissCallback != null) {
            mDismissCallback.onProgressDialogDismiss();
        }
    }

    private void setDismissCallback(OnDismissCallback dismissCallback) {
        mDismissCallback = dismissCallback;
    }
}
