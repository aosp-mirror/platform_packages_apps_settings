/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.print;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

/**
 * {@link BasePreferenceController} for Print settings.
 */
public class PrintSettingPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, PrintManager.PrintJobStateChangeListener {

    private static final String KEY_PRINTING_SETTINGS = "connected_device_printing";

    private final PackageManager mPackageManager;
    private final PrintManager mPrintManager;

    private Preference mPreference;

    public PrintSettingPreferenceController(Context context) {
        super(context, KEY_PRINTING_SETTINGS);
        mPackageManager = context.getPackageManager();
        mPrintManager = ((PrintManager) context.getSystemService(Context.PRINT_SERVICE))
                .getGlobalPrintManagerForUser(context.getUserId());
    }

    @Override
    public int getAvailabilityStatus() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_PRINTING)
                && mPrintManager != null
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        if (mPrintManager != null) {
            mPrintManager.addPrintJobStateChangeListener(this);
        }
    }

    @Override
    public void onStop() {
        if (mPrintManager != null) {
            mPrintManager.removePrintJobStateChangeListener(this);
        }
    }

    @Override
    public void onPrintJobStateChanged(PrintJobId printJobId) {
        updateState(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        ((RestrictedPreference) preference).checkRestrictionAndSetDisabled(
                UserManager.DISALLOW_PRINTING);
    }

    @Override
    public CharSequence getSummary() {
        final List<PrintJob> printJobs = mPrintManager.getPrintJobs();

        int numActivePrintJobs = 0;
        if (printJobs != null) {
            for (PrintJob job : printJobs) {
                if (shouldShowToUser(job.getInfo())) {
                    numActivePrintJobs++;
                }
            }
        }

        if (numActivePrintJobs > 0) {
            return mContext.getResources().getQuantityString(
                    R.plurals.print_jobs_summary, numActivePrintJobs, numActivePrintJobs);
        } else {
            final List<PrintServiceInfo> services =
                    mPrintManager.getPrintServices(PrintManager.ENABLED_SERVICES);
            if (services == null || services.isEmpty()) {
                return mContext.getText(R.string.print_settings_summary_no_service);
            } else {
                final int count = services.size();
                return mContext.getResources().getQuantityString(
                        R.plurals.print_settings_summary, count, count);
            }
        }
    }

    /**
     * Should the print job the shown to the user in the settings app.
     *
     * @param printJob The print job in question.
     * @return true iff the print job should be shown.
     */
    static boolean shouldShowToUser(PrintJobInfo printJob) {
        switch (printJob.getState()) {
            case PrintJobInfo.STATE_QUEUED:
            case PrintJobInfo.STATE_STARTED:
            case PrintJobInfo.STATE_BLOCKED:
            case PrintJobInfo.STATE_FAILED: {
                return true;
            }
        }
        return false;
    }
}
