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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.text.format.DateUtils;

import com.android.settings.R;

import java.text.DateFormat;

public class PrintJobPreferenceController extends PrintJobPreferenceControllerBase {

    public PrintJobPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    protected void updateUi() {
        final PrintJob printJob = getPrintJob();

        if (printJob == null) {
            mFragment.finish();
            return;
        }

        if (printJob.isCancelled() || printJob.isCompleted()) {
            mFragment.finish();
            return;
        }

        PrintJobInfo info = printJob.getInfo();

        switch (info.getState()) {
            case PrintJobInfo.STATE_CREATED: {
                mPreference.setTitle(mContext.getString(
                        R.string.print_configuring_state_title_template, info.getLabel()));
            }
            break;
            case PrintJobInfo.STATE_QUEUED:
            case PrintJobInfo.STATE_STARTED: {
                if (!printJob.getInfo().isCancelling()) {
                    mPreference.setTitle(mContext.getString(
                            R.string.print_printing_state_title_template, info.getLabel()));
                } else {
                    mPreference.setTitle(mContext.getString(
                            R.string.print_cancelling_state_title_template, info.getLabel()));
                }
            }
            break;

            case PrintJobInfo.STATE_FAILED: {
                mPreference.setTitle(mContext.getString(
                        R.string.print_failed_state_title_template, info.getLabel()));
            }
            break;

            case PrintJobInfo.STATE_BLOCKED: {
                if (!printJob.getInfo().isCancelling()) {
                    mPreference.setTitle(mContext.getString(
                            R.string.print_blocked_state_title_template, info.getLabel()));
                } else {
                    mPreference.setTitle(mContext.getString(
                            R.string.print_cancelling_state_title_template, info.getLabel()));
                }
            }
            break;
        }

        mPreference.setSummary(mContext.getString(R.string.print_job_summary,
                info.getPrinterName(), DateUtils.formatSameDayTime(
                        info.getCreationTime(), info.getCreationTime(), DateFormat.SHORT,
                        DateFormat.SHORT)));

        TypedArray a = mContext.obtainStyledAttributes(new int[]{
                android.R.attr.colorControlNormal});
        int tintColor = a.getColor(0, 0);
        a.recycle();

        switch (info.getState()) {
            case PrintJobInfo.STATE_QUEUED:
            case PrintJobInfo.STATE_STARTED: {
                Drawable icon = mContext.getDrawable(com.android.internal.R.drawable.ic_print);
                icon.setTint(tintColor);
                mPreference.setIcon(icon);
                break;
            }

            case PrintJobInfo.STATE_FAILED:
            case PrintJobInfo.STATE_BLOCKED: {
                Drawable icon = mContext.getDrawable(
                        com.android.internal.R.drawable.ic_print_error);
                icon.setTint(tintColor);
                mPreference.setIcon(icon);
                break;
            }
        }
    }
}
