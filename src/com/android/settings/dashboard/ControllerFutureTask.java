/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.settings.dashboard;

import com.android.settingslib.core.AbstractPreferenceController;

import java.util.concurrent.FutureTask;

/**
 *  {@link FutureTask} of the Controller.
 */
public class ControllerFutureTask extends FutureTask<Void> {
    private final AbstractPreferenceController mController;

    public ControllerFutureTask(ControllerTask task, Void result) {
        super(task, result);
        mController = task.getController();
    }

    AbstractPreferenceController getController() {
        return mController;
    }
}
