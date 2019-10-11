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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintJob;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Fragment for management of a print job.
 */
public class PrintJobSettingsFragment extends DashboardFragment {
    private static final String TAG = "PrintJobSettingsFragment";

    private static final int MENU_ITEM_ID_CANCEL = 1;
    private static final int MENU_ITEM_ID_RESTART = 2;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.print_job_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(PrintJobPreferenceController.class).init(this);
        use(PrintJobMessagePreferenceController.class).init(this);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRINT_JOB_SETTINGS;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setEnabled(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        final PrintJob printJob = use(PrintJobPreferenceController.class).getPrintJob();
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
        final PrintJob printJob = use(PrintJobPreferenceController.class).getPrintJob();

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
}
