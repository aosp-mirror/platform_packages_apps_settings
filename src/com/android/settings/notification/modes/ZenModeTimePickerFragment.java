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

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.annotation.NonNull;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Dialog that shows when a user selects a (start or end) time to edit for a schedule-based mode.
 */
public class ZenModeTimePickerFragment extends InstrumentedDialogFragment implements
        TimePickerDialog.OnTimeSetListener {

    private static final String TAG = "ZenModeTimePickerFragment";

    private TimeSetter mTimeSetter;
    private int mHour;
    private int mMinute;

    public static void show(DashboardFragment parent, int hour, int minute,
            @NonNull TimeSetter timeSetter) {
        ZenModeTimePickerFragment fragment = new ZenModeTimePickerFragment();
        fragment.mHour = hour;
        fragment.mMinute = minute;
        fragment.mTimeSetter = timeSetter;

        fragment.show(parent.getParentFragmentManager(), TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mTimeSetter == null) {
            // Probably the dialog fragment was recreated after its activity was destroyed.
            // It's pointless to re-show the dialog if we can't do anything when its options are
            // selected, so we don't.
            dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new TimePickerDialog(getContext(), this, mHour, mMinute,
                DateFormat.is24HourFormat(getContext()));
    }

    /**
     * Calls the provided TimeSetter's setTime() method when a time is set on the TimePicker.
     */
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        checkNotNull(mTimeSetter).setTime(hourOfDay, minute);
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
