/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.print;

import android.content.Context;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintManager;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public abstract class PrintJobPreferenceControllerBase extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, PrintManager.PrintJobStateChangeListener {
    private static final String TAG = "PrintJobPrefCtrlBase";

    private static final String EXTRA_PRINT_JOB_ID = "EXTRA_PRINT_JOB_ID";

    private final PrintManager mPrintManager;
    protected Preference mPreference;
    protected PrintJobSettingsFragment mFragment;
    protected PrintJobId mPrintJobId;

    public PrintJobPreferenceControllerBase(Context context, String key) {
        super(context, key);
        mPrintManager = ((PrintManager) mContext.getSystemService(
                Context.PRINT_SERVICE)).getGlobalPrintManagerForUser(mContext.getUserId());
    }

    @Override
    public void onStart() {
        mPrintManager.addPrintJobStateChangeListener(this);
        updateUi();
    }

    @Override
    public void onStop() {
        mPrintManager.removePrintJobStateChangeListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onPrintJobStateChanged(PrintJobId printJobId) {
        updateUi();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    public void init(PrintJobSettingsFragment fragment) {
        mFragment = fragment;
        processArguments();
    }

    protected PrintJob getPrintJob() {
        return mPrintManager.getPrintJob(mPrintJobId);
    }

    protected abstract void updateUi();

    private void processArguments() {
        String printJobId = mFragment.getArguments().getString(EXTRA_PRINT_JOB_ID);
        if (printJobId == null) {
            printJobId = mFragment.getActivity().getIntent().getStringExtra(EXTRA_PRINT_JOB_ID);

            if (printJobId == null) {
                Log.w(TAG, EXTRA_PRINT_JOB_ID + " not set");
                mFragment.finish();
                return;
            }
        }
        mPrintJobId = PrintJobId.unflattenFromString(printJobId);
    }
}
