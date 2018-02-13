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

package com.android.settings.wrapper;

import android.content.Context;
import android.print.PrintJob;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;

import java.util.List;

/**
 * Wrapper class for {@link PrintManager}. This is necessary to increase testability in Robolectric.
 */
public class PrintManagerWrapper {

    private final PrintManager mPrintManager;

    public PrintManagerWrapper(Context context) {
        mPrintManager = ((PrintManager) context.getSystemService(Context.PRINT_SERVICE))
                .getGlobalPrintManagerForUser(context.getUserId());
    }

    public List<PrintServiceInfo> getPrintServices(int selectionFlags) {
        return mPrintManager.getPrintServices(selectionFlags);
    }

    public void addPrintJobStateChanegListener(PrintManager.PrintJobStateChangeListener listener) {
        mPrintManager.addPrintJobStateChangeListener(listener);
    }

    public void removePrintJobStateChangeListener(
            PrintManager.PrintJobStateChangeListener listener) {
        mPrintManager.removePrintJobStateChangeListener(listener);
    }

    public List<PrintJob> getPrintJobs() {
        return mPrintManager.getPrintJobs();
    }
}
