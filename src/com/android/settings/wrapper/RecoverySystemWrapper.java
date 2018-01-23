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
import android.os.RecoverySystem;

/**
 * This class replicates a subset of the {@link RecoverySystem}.
 * The interface exists so that we can use a thin wrapper around the RecoverySystem in
 * production code and a mock in tests.
 */
public class RecoverySystemWrapper {

    /**
     * Returns whether wipe Euicc data successfully or not.
     *
     * @param packageName the package name of the caller app.
     */
    public boolean wipeEuiccData(
            Context context, final String packageName) {
        return RecoverySystem.wipeEuiccData(context, packageName);
    }
}
