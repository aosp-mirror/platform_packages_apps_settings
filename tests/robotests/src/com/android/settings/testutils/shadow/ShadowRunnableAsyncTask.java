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
 *
 */

package com.android.settings.testutils.shadow;

import android.os.AsyncTask;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowAsyncTask;

import java.util.concurrent.Executor;

/**
 * Shadow async task to handle runnables in roboletric
 */
@Implements(AsyncTask.class)
public class ShadowRunnableAsyncTask<Params, Progress, Result> extends
        ShadowAsyncTask<Params, Progress, Result> {

    @Implementation
    public AsyncTask<Params, Progress, Result> executeOnExecutor(Executor executor,
            Params... params) {
        return super.execute(params);
    }
}