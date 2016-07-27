/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;

import com.android.internal.app.NightDisplayController;
import com.android.settings.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class NightDisplayPreference extends SwitchPreference
        implements NightDisplayController.Callback {

    private NightDisplayController mController;
    private DateFormat mTimeFormatter;

    public NightDisplayPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mController = new NightDisplayController(context);
        mTimeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        mTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void onAttached() {
        super.onAttached();

        // Listen for changes only while attached.
        mController.setListener(this);

        // Update the summary since the state may have changed while not attached.
        updateSummary();
    }

    @Override
    public void onDetached() {
        super.onDetached();

        // Stop listening for state changes.
        mController.setListener(null);
    }

    private String getFormattedTimeString(NightDisplayController.LocalTime localTime) {
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(mTimeFormatter.getTimeZone());
        c.set(Calendar.HOUR_OF_DAY, localTime.hourOfDay);
        c.set(Calendar.MINUTE, localTime.minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return mTimeFormatter.format(c.getTime());
    }

    private void updateSummary() {
        final Context context = getContext();

        final boolean isActivated = mController.isActivated();
        final int autoMode = mController.getAutoMode();

        final String autoModeSummary;
        switch (autoMode) {
            default:
            case NightDisplayController.AUTO_MODE_DISABLED:
                autoModeSummary = context.getString(isActivated
                        ? R.string.night_display_summary_on_auto_mode_never
                        : R.string.night_display_summary_off_auto_mode_never);
                break;
            case NightDisplayController.AUTO_MODE_CUSTOM:
                if (isActivated) {
                    autoModeSummary = context.getString(
                            R.string.night_display_summary_on_auto_mode_custom,
                            getFormattedTimeString(mController.getCustomEndTime()));
                } else {
                    autoModeSummary = context.getString(
                            R.string.night_display_summary_off_auto_mode_custom,
                            getFormattedTimeString(mController.getCustomStartTime()));
                }
                break;
            case NightDisplayController.AUTO_MODE_TWILIGHT:
                autoModeSummary = context.getString(isActivated
                        ? R.string.night_display_summary_on_auto_mode_twilight
                        : R.string.night_display_summary_off_auto_mode_twilight);
                break;
        }

        final int summaryFormatResId = isActivated ? R.string.night_display_summary_on
                : R.string.night_display_summary_off;
        setSummary(context.getString(summaryFormatResId, autoModeSummary));
    }

    @Override
    public void onActivated(boolean activated) {
        updateSummary();
    }

    @Override
    public void onAutoModeChanged(int autoMode) {
        updateSummary();
    }

    @Override
    public void onCustomStartTimeChanged(NightDisplayController.LocalTime startTime) {
        updateSummary();
    }

    @Override
    public void onCustomEndTimeChanged(NightDisplayController.LocalTime endTime) {
        updateSummary();
    }
}
