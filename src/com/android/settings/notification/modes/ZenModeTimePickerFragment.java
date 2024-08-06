/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.annotation.NonNull;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Dialog that shows when a user selects a (start or end) time to edit for a schedule-based mode.
 */
public class ZenModeTimePickerFragment extends InstrumentedDialogFragment implements
        TimePickerDialog.OnTimeSetListener {
    private final Context mContext;
    private final TimeSetter mTimeSetter;
    private final int mHour;
    private final int mMinute;

    public ZenModeTimePickerFragment(Context context, int hour, int minute,
            @NonNull TimeSetter timeSetter) {
        super();
        mContext = context;
        mHour = hour;
        mMinute = minute;
        mTimeSetter = timeSetter;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new TimePickerDialog(mContext, this, mHour, mMinute,
                DateFormat.is24HourFormat(mContext));
    }

    /**
     * Calls the provided TimeSetter's setTime() method when a time is set on the TimePicker.
     */
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mTimeSetter.setTime(hourOfDay, minute);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ZEN_TIMEPICKER;
    }

    /**
     * Interface for a method to pass into the TimePickerFragment that specifies what to do when the
     * time is updated.
     */
    public interface TimeSetter {
        void setTime(int hour, int minute);
    }
}
