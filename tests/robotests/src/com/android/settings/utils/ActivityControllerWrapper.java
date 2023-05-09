/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.utils;

import android.os.Bundle;

import org.robolectric.android.controller.ActivityController;

/*
 * b/275023433
 * This class is a workaround for Robolectric, in order to re-enable presubmit
 * We don't use ActivityController#visible() to avoid test crash
 */
public class ActivityControllerWrapper {

    private static final boolean ENABLE_WORKAROUND = true;


    public static ActivityController setup(ActivityController controller) {
        if (ENABLE_WORKAROUND) {
            return controller.create().start().postCreate(null).resume();
        } else {
            return controller.setup();
        }
    }

    public static ActivityController setup(ActivityController controller, Bundle savedState) {
        return controller.create(savedState)
                .start()
                .restoreInstanceState(savedState)
                .postCreate(savedState)
                .resume();
    }

}
