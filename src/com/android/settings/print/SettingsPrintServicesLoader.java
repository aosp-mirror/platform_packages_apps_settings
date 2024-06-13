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
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.printservice.PrintServiceInfo;

import androidx.annotation.NonNull;
import androidx.loader.content.Loader;

import com.android.internal.util.Preconditions;

import java.util.List;

/**
 * Loader for the list of print services. Can be parametrized to select a subset.
 */
public class SettingsPrintServicesLoader extends Loader<List<PrintServiceInfo>> {

    private PrintServicesLoader mLoader;

    public SettingsPrintServicesLoader(@NonNull PrintManager printManager, @NonNull Context context,
            int selectionFlags) {
        super(Preconditions.checkNotNull(context));

        mLoader = new PrintServicesLoader(printManager, context, selectionFlags) {
            @Override
            public void deliverResult(List<PrintServiceInfo> data) {
                super.deliverResult(data);

                // deliver the result to outer Loader class
                SettingsPrintServicesLoader.this.deliverResult(data);
            }
        };
    }

    @Override
    protected void onForceLoad() {
        mLoader.forceLoad();
    }

    @Override
    protected void onStartLoading() {
        mLoader.startLoading();
    }

    @Override
    protected void onStopLoading() {
        mLoader.stopLoading();
    }

    @Override
    protected boolean onCancelLoad() {
        return mLoader.cancelLoad();
    }

    @Override
    protected void onAbandon() {
        mLoader.abandon();
    }

    @Override
    protected void onReset() {
        mLoader.reset();
    }
}
