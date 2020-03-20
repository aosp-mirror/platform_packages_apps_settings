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

package com.android.settings.deviceinfo.simstatus;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class SimStatusDialogFragment extends InstrumentedDialogFragment {

    private static final String SIM_SLOT_BUNDLE_KEY = "arg_key_sim_slot";
    private static final String DIALOG_TITLE_BUNDLE_KEY = "arg_key_dialog_title";

    private static final String TAG = "SimStatusDialog";

    private View mRootView;
    private SimStatusDialogController mController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_SIM_STATUS;
    }

    public static void show(Fragment host, int slotId, String dialogTitle) {
        final FragmentManager manager = host.getChildFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final Bundle bundle = new Bundle();
            bundle.putInt(SIM_SLOT_BUNDLE_KEY, slotId);
            bundle.putString(DIALOG_TITLE_BUNDLE_KEY, dialogTitle);
            final SimStatusDialogFragment dialog =
                    new SimStatusDialogFragment();
            dialog.setArguments(bundle);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final int slotId = bundle.getInt(SIM_SLOT_BUNDLE_KEY);
        final String dialogTitle = bundle.getString(DIALOG_TITLE_BUNDLE_KEY);
        mController = new SimStatusDialogController(this, mLifecycle, slotId);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setPositiveButton(android.R.string.ok, null /* onClickListener */);
        mRootView = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.dialog_sim_status, null /* parent */);
        mController.initialize();
        return builder.setView(mRootView).create();
    }

    @Override
    public void onDestroy() {
        mController.deinitialize();
        super.onDestroy();
    }

    public void removeSettingFromScreen(int viewId) {
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
