/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.List;

/**
 * This is base fragment of {@link NetworkRequestDialogFragment} and
 * {@link NetworkRequestSingleSsidDialogFragment} to handle activity callback methods.
 */
abstract public class NetworkRequestDialogBaseFragment extends InstrumentedDialogFragment {

    @VisibleForTesting
    final static String EXTRA_APP_NAME = "com.android.settings.wifi.extra.APP_NAME";

    NetworkRequestDialogActivity mActivity = null;
    private String mAppName = "";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_SCANNING_NEEDED_DIALOG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NetworkRequestDialogActivity) {
            mActivity = (NetworkRequestDialogActivity) context;
        }

        final Intent intent = getActivity().getIntent();
        if (intent != null) {
            mAppName = intent.getStringExtra(EXTRA_APP_NAME);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);

        if (mActivity != null) {
            mActivity.onCancel();
        }
    }

    protected String getTitle() {
        return getString(R.string.network_connection_request_dialog_title);
    }

    protected String getSummary() {
        return getString(R.string.network_connection_request_dialog_summary, mAppName);
    }

    protected void onUserSelectionCallbackRegistration(
            NetworkRequestUserSelectionCallback userSelectionCallback) {
    }

    protected void onMatch(List<ScanResult> scanResults) {
    }
}
