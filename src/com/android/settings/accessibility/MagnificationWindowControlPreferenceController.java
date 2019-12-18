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

package com.android.settings.accessibility;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Controller that shows and updates the magnification window control switch. */
public class MagnificationWindowControlPreferenceController extends TogglePreferenceController {

    // TODO(b/146019459): Use magnification_window_control_enabled.
    private static final String KEY_CONTROL = Settings.System.MASTER_MONO;

    public MagnificationWindowControlPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                KEY_CONTROL, State.OFF, UserHandle.USER_CURRENT) == State.ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(),
                KEY_CONTROL, isChecked ? State.ON : State.OFF, UserHandle.USER_CURRENT);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
        int OFF = 0;
        int ON = 1;
    }
}
