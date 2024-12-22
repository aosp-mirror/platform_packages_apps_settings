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

package com.android.settings.notification.zen;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.notification.modes.ZenDurationDialog;

public class SettingsZenDurationDialog extends InstrumentedDialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new ZenDurationDialog(getContext()).createDialog();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE_DURATION_DIALOG;
    }
}
