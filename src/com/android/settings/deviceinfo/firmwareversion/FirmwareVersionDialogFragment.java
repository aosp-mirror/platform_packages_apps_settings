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

package com.android.settings.deviceinfo.firmwareversion;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class FirmwareVersionDialogFragment extends InstrumentedDialogFragment {

    private static final String TAG = "firmwareVersionDialog";

    private View mRootView;

    public static void show(Fragment host) {
        final FragmentManager manager = host.getChildFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final FirmwareVersionDialogFragment dialog = new FirmwareVersionDialogFragment();
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FIRMWARE_VERSION;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.firmware_title)
                .setPositiveButton(android.R.string.ok, null /* listener */);

        mRootView = LayoutInflater.from(getActivity()).inflate(
                R.layout.dialog_firmware_version, null /* parent */);

        initializeControllers();

        return builder.setView(mRootView).create();
    }

    public void setText(int viewId, CharSequence text) {
        final TextView view = mRootView.findViewById(viewId);
        if (view != null) {
            view.setText(text);
        }
    }

    public void removeSettingFromScreen(int viewId) {
        final View view = mRootView.findViewById(viewId);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public void registerClickListener(int viewId, View.OnClickListener listener) {
        final View view = mRootView.findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }

    private void initializeControllers() {
        new FirmwareVersionDialogController(this).initialize();
        new SecurityPatchLevelDialogController(this).initialize();
        new BasebandVersionDialogController(this).initialize();
        new KernelVersionDialogController(this).initialize();
        new BuildNumberDialogController(this).initialize();
        new ModuleVersionDialogController(this).initialize();
    }
}
