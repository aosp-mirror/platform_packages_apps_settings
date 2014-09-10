/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.service.notification.ZenModeConfig;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

import com.android.settings.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ZenModeDowntimeDaysSelection extends ScrollView {
    public static final int[] DAYS = {
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY,
        Calendar.SATURDAY, Calendar.SUNDAY
    };
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("EEEE");

    private final SparseBooleanArray mDays = new SparseBooleanArray();
    private final LinearLayout mLayout;

    public ZenModeDowntimeDaysSelection(Context context, String mode) {
        super(context);
        mLayout = new LinearLayout(mContext);
        final int hPad = context.getResources().getDimensionPixelSize(R.dimen.zen_downtime_margin);
        mLayout.setPadding(hPad, 0, hPad, 0);
        addView(mLayout);
        final int[] days = ZenModeConfig.tryParseDays(mode);
        if (days != null) {
            for (int i = 0; i < days.length; i++) {
                mDays.put(days[i], true);
            }
        }
        mLayout.setOrientation(LinearLayout.VERTICAL);
        final Calendar c = Calendar.getInstance();
        final LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < DAYS.length; i++) {
            final int day = DAYS[i];
            final CheckBox checkBox = (CheckBox) inflater.inflate(R.layout.zen_downtime_day,
                    this, false);
            c.set(Calendar.DAY_OF_WEEK, day);
            checkBox.setText(DAY_FORMAT.format(c.getTime()));
            checkBox.setChecked(mDays.get(day));
            checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mDays.put(day, isChecked);
                    onChanged(getMode());
                }
            });
            mLayout.addView(checkBox);
        }
    }

    private String getMode() {
        final StringBuilder sb = new StringBuilder(ZenModeConfig.SLEEP_MODE_DAYS_PREFIX);
        boolean empty = true;
        for (int i = 0; i < mDays.size(); i++) {
            final int day = mDays.keyAt(i);
            if (!mDays.valueAt(i)) continue;
            if (empty) {
                empty = false;
            } else {
                sb.append(',');
            }
            sb.append(day);
        }
        return empty ? null : sb.toString();
    }

    protected void onChanged(String mode) {
        // event hook for subclasses
    }
}
