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
import android.print.PrintJobInfo;
import android.text.TextUtils;

public class PrintJobMessagePreferenceController extends PrintJobPreferenceControllerBase {

    public PrintJobMessagePreferenceController(Context context, String key) {
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

        final PrintJobInfo info = printJob.getInfo();
        final CharSequence status = info.getStatus(mContext.getPackageManager());
        mPreference.setVisible(!TextUtils.isEmpty(status));
        mPreference.setSummary(status);
    }
}
