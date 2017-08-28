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

package com.android.settings.anomaly.tester.service;

import android.annotation.Nullable;
import android.app.IntentService;
import android.content.Intent;
import android.os.ResultReceiver;

import com.android.settings.anomaly.tester.utils.AnomalyActions;

/**
 * Service to run the anomaly action
 */
public class AnomalyService extends IntentService {
    private static final String TAG = AnomalyService.class.getSimpleName();

    public AnomalyService() {
        super(AnomalyService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final String action = intent.getStringExtra(AnomalyActions.KEY_ACTION);
        final long durationMs = intent.getLongExtra(AnomalyActions.KEY_DURATION_MS, 0);
        final ResultReceiver resultReceiver = intent.getParcelableExtra(
                AnomalyActions.KEY_RESULT_RECEIVER);

        AnomalyActions.doAction(this, action, durationMs);

        if (resultReceiver != null) {
            resultReceiver.send(0 /* resultCode */, intent.getExtras());
        }
    }
}
