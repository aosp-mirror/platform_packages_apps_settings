/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.os.Bundle;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.print.PrintManager.PrintJobStateChangeListener;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.view.ViewGroup;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.text.DateFormat;

/**
 * Fragment for management of a print job.
 */
public class PrintJobSettingsFragment extends SettingsPreferenceFragment {
    private static final int MENU_ITEM_ID_CANCEL = 1;
    private static final int MENU_ITEM_ID_RESTART = 2;

    private static final String EXTRA_PRINT_JOB_ID = "EXTRA_PRINT_JOB_ID";

    private static final String PRINT_JOB_PREFERENCE = "print_job_preference";
    private static final String PRINT_JOB_MESSAGE_PREFERENCE = "print_job_message_preference";

    private final PrintJobStateChangeListener mPrintJobStateChangeListener =
            new PrintJobStateChangeListener() {
        @Override
        public void onPrintJobStateChanged(PrintJobId printJobId) {
            updateUi();
        }
    };

    private PrintManager mPrintManager;

    private Preference mPrintJobPreference;
    private Preference mMessagePreference;

    private PrintJobId mPrintJobId;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.PRINT_JOB_SETTINGS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        addPreferencesFromResource(R.xml.print_job_settings);
        mPrintJobPreference = findPreference(PRINT_JOB_PREFERENCE);
        mMessagePreference = findPreference(PRINT_JOB_MESSAGE_PREFERENCE);

        mPrintManager = ((PrintManager) getActivity().getSystemService(
                Context.PRINT_SERVICE)).getGlobalPrintManagerForUser(
                        getActivity().getUserId());

        getActivity().getActionBar().setTitle(R.string.print_print_job);

        processArguments();

        setHasOptionsMenu(true);

        return  view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        mPrintManager.addPrintJobStateChangeListener(
                mPrintJobStateChangeListener);
        updateUi();
    }

    @Override
    public void onStop() {
        super.onStop();
        mPrintManager.removePrintJobStateChangeListener(
                mPrintJobStateChangeListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        PrintJob printJob = getPrintJob();
        if (printJob == null) {
            return;
        }

        if (!printJob.getInfo().isCancelling()) {
            MenuItem cancel = menu.add(0, MENU_ITEM_ID_CANCEL, Menu.NONE,
                    getString(R.string.print_cancel));
            cancel.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        if (printJob.isFailed()) {
            MenuItem restart = menu.add(0, MENU_ITEM_ID_RESTART, Menu.NONE,
                    getString(R.string.print_restart));
            restart.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PrintJob printJob = getPrintJob();

        if (printJob != null) {
            switch (item.getItemId()) {
                case MENU_ITEM_ID_CANCEL: {
                    printJob.cancel();
                    finish();
                    return true;
                }

                case MENU_ITEM_ID_RESTART: {
                    printJob.restart();
                    finish();
                    return true;
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void processArguments() {
        String printJobId = getArguments().getString(EXTRA_PRINT_JOB_ID);
        if (printJobId == null) {
            finish();
        } else {
            mPrintJobId = PrintJobId.unflattenFromString(printJobId);
        }
    }

    private PrintJob getPrintJob() {
        return mPrintManager.getPrintJob(mPrintJobId);
    }

    private void updateUi() {
        PrintJob printJob = getPrintJob();

        if (printJob == null) {
            finish();
            return;
        }

        if (printJob.isCancelled() || printJob.isCompleted()) {
            finish();
            return;
        }

        PrintJobInfo info = printJob.getInfo();

        switch (info.getState()) {
            case PrintJobInfo.STATE_QUEUED:
            case PrintJobInfo.STATE_STARTED: {
                if (!printJob.getInfo().isCancelling()) {
                    mPrintJobPreference.setTitle(getString(
                            R.string.print_printing_state_title_template, info.getLabel()));
                } else {
                    mPrintJobPreference.setTitle(getString(
                            R.string.print_cancelling_state_title_template, info.getLabel()));
                }
            } break;

            case PrintJobInfo.STATE_FAILED: {
                mPrintJobPreference.setTitle(getString(
                        R.string.print_failed_state_title_template, info.getLabel()));
            } break;

            case PrintJobInfo.STATE_BLOCKED: {
                if (!printJob.getInfo().isCancelling()) {
                    mPrintJobPreference.setTitle(getString(
                            R.string.print_blocked_state_title_template, info.getLabel()));
                } else {
                    mPrintJobPreference.setTitle(getString(
                            R.string.print_cancelling_state_title_template, info.getLabel()));
                }
            } break;
        }

        mPrintJobPreference.setSummary(getString(R.string.print_job_summary,
                info.getPrinterName(), DateUtils.formatSameDayTime(
                        info.getCreationTime(), info.getCreationTime(), DateFormat.SHORT,
                        DateFormat.SHORT)));

        switch (info.getState()) {
            case PrintJobInfo.STATE_QUEUED:
            case PrintJobInfo.STATE_STARTED: {
                mPrintJobPreference.setIcon(R.drawable.ic_print);
            } break;

            case PrintJobInfo.STATE_FAILED:
            case PrintJobInfo.STATE_BLOCKED: {
                mPrintJobPreference.setIcon(R.drawable.ic_print_error);
            } break;
        }

        CharSequence status = info.getStatus(getPackageManager());
        if (!TextUtils.isEmpty(status)) {
            if (getPreferenceScreen().findPreference(PRINT_JOB_MESSAGE_PREFERENCE) == null) {
                getPreferenceScreen().addPreference(mMessagePreference);
            }
            mMessagePreference.setSummary(status);
        } else {
            getPreferenceScreen().removePreference(mMessagePreference);
        }

        getActivity().invalidateOptionsMenu();
    }
}
