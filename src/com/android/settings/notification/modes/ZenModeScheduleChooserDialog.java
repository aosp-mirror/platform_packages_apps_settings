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
import static com.google.common.base.Preconditions.checkState;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.ZenModeConfig;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;

import com.google.common.collect.ImmutableList;

public class ZenModeScheduleChooserDialog extends InstrumentedDialogFragment {

    private static final String TAG = "ZenModeScheduleChooserDialog";

    static final int OPTION_TIME = 0;
    static final int OPTION_CALENDAR = 1;

    private record ScheduleOption(@StringRes int nameResId,
                                  @Nullable @StringRes Integer exampleResId,
                                  @DrawableRes int iconResId) { }

    private static final ImmutableList<ScheduleOption> SCHEDULE_OPTIONS = ImmutableList.of(
            new ScheduleOption(R.string.zen_mode_select_schedule_time,
                    R.string.zen_mode_select_schedule_time_example,
                    com.android.internal.R.drawable.ic_zen_mode_type_schedule_time),
            new ScheduleOption(R.string.zen_mode_select_schedule_calendar,
                    null,
                    com.android.internal.R.drawable.ic_zen_mode_type_schedule_calendar));

    private OnScheduleOptionListener mOptionListener;

    interface OnScheduleOptionListener {
        void onScheduleSelected(Uri conditionId);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_SCHEDULE_CHOOSER_DIALOG;
    }

    static void show(DashboardFragment parent, OnScheduleOptionListener optionListener) {
        ZenModeScheduleChooserDialog dialog = new ZenModeScheduleChooserDialog();
        dialog.mOptionListener = optionListener;
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getParentFragmentManager(), TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mOptionListener == null) {
            // Probably the dialog fragment was recreated after its activity being destroyed.
            // It's pointless to re-show the dialog if we can't do anything when its options are
            // selected, so we don't.
            dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        checkState(getContext() != null);
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.zen_mode_select_schedule_title)
                .setAdapter(new OptionsAdapter(getContext()),
                        (dialog, which) -> onScheduleTypeSelected(which))
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    private static class OptionsAdapter extends ArrayAdapter<ScheduleOption> {

        private final LayoutInflater mInflater;

        OptionsAdapter(@NonNull Context context) {
            super(context, R.layout.zen_mode_type_item, SCHEDULE_OPTIONS);
            mInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.zen_mode_type_item, parent, false);
            }
            // No need for holder pattern since we have only 2 items.
            ImageView imageView = checkNotNull(convertView.findViewById(R.id.icon));
            TextView title = checkNotNull(convertView.findViewById(R.id.title));
            TextView subtitle = checkNotNull(convertView.findViewById(R.id.subtitle));

            ScheduleOption option = checkNotNull(getItem(position));
            imageView.setImageResource(option.iconResId());
            title.setText(option.nameResId());
            if (option.exampleResId() != null) {
                subtitle.setVisibility(View.VISIBLE);
                subtitle.setText(option.exampleResId());
            } else {
                subtitle.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    private void onScheduleTypeSelected(int whichOption) {
        Uri conditionId = switch (whichOption) {
            case OPTION_TIME -> getDefaultScheduleTimeCondition();
            case OPTION_CALENDAR -> getDefaultScheduleCalendarCondition();
            default -> ZenModeConfig.toCustomManualConditionId();
        };

        mOptionListener.onScheduleSelected(conditionId);
    }

    private static Uri getDefaultScheduleTimeCondition() {
        ZenModeConfig.ScheduleInfo schedule = new ZenModeConfig.ScheduleInfo();
        schedule.days = ZenModeConfig.ALL_DAYS;
        schedule.startHour = 9;
        schedule.startMinute = 30;
        schedule.endHour = 17;
        return ZenModeConfig.toScheduleConditionId(schedule);
    }

    private static Uri getDefaultScheduleCalendarCondition() {
        ZenModeConfig.EventInfo eventInfo = new ZenModeConfig.EventInfo();
        eventInfo.calendarId = null; // All calendars of the current user.
        eventInfo.reply = ZenModeConfig.EventInfo.REPLY_ANY_EXCEPT_NO;
        return ZenModeConfig.toEventConditionId(eventInfo);
    }
}
