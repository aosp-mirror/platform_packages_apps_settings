/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo.imei;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ImeiInfoDialogFragment extends InstrumentedDialogFragment {

    @VisibleForTesting
    static final String TAG = "ImeiInfoDialog";

    private static final String SLOT_ID_BUNDLE_KEY = "arg_key_slot_id";
    private static final String DIALOG_TITLE_BUNDLE_KEY = "arg_key_dialog_title";

    private View mRootView;

    public static void show(@NonNull Fragment host, int slotId, String dialogTitle) {
        final FragmentManager manager = host.getChildFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final Bundle bundle = new Bundle();
            bundle.putInt(SLOT_ID_BUNDLE_KEY, slotId);
            bundle.putString(DIALOG_TITLE_BUNDLE_KEY, dialogTitle);
            final ImeiInfoDialogFragment dialog = new ImeiInfoDialogFragment();
            dialog.setArguments(bundle);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_IMEI_INFO;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final int slotId = bundle.getInt(SLOT_ID_BUNDLE_KEY);
        final String dialogTitle = bundle.getString(DIALOG_TITLE_BUNDLE_KEY);

        final ImeiInfoDialogController controller = new ImeiInfoDialogController(this, slotId);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setPositiveButton(android.R.string.ok, null);
        mRootView = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.dialog_imei_info, null /* parent */);
        controller.populateImeiInfo();
        return builder.setView(mRootView).create();
    }

    public void removeViewFromScreen(int viewId) {
        final View view = mRootView.findViewById(viewId);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public void setText(int viewId, CharSequence text) {
        final TextView textView = mRootView.findViewById(viewId);
        if (TextUtils.isEmpty(text)) {
            text = getResources().getString(R.string.device_info_default);
        }
        if (textView != null) {
            textView.setText(text);
        }
    }
}
