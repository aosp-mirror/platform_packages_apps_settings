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
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.android.settings.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ZenModeScheduleDaysSelection extends ScrollView {
    // per-instance to ensure we're always using the current locale
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEEE");
    private final SparseBooleanArray mDays = new SparseBooleanArray();
    private final LinearLayout mLayout;

    public ZenModeScheduleDaysSelection(Context context, int[] days) {
        super(context);
        mLayout = new LinearLayout(mContext);
        final int hPad = context.getResources()
                .getDimensionPixelSize(R.dimen.zen_schedule_day_margin);
        mLayout.setPadding(hPad, 0, hPad, 0);
        addView(mLayout);
        if (days != null) {
            for (int i = 0; i < days.length; i++) {
                mDays.put(days[i], true);
            }
        }
        mLayout.setOrientation(LinearLayout.VERTICAL);
        final Calendar c = Calendar.getInstance();
        int[] daysOfWeek = getDaysOfWeekForLocale(c);
        final LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < daysOfWeek.length; i++) {
            final int day = daysOfWeek[i];
            final CheckBox checkBox = (CheckBox) inflater.inflate(R.layout.zen_schedule_rule_day,
                    this, false);
            c.set(Calendar.DAY_OF_WEEK, day);
            checkBox.setText(mDayFormat.format(c.getTime()));
            checkBox.setChecked(mDays.get(day));
            checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mDays.put(day, isChecked);
                    onChanged(getDays());
                }
            });
            mLayout.addView(checkBox);
        }
    }

    private int[] getDays() {
        final SparseBooleanArray rt = new SparseBooleanArray(mDays.size());
        for (int i = 0; i < mDays.size(); i++) {
            final int day = mDays.keyAt(i);
            if (!mDays.valueAt(i)) continue;
            rt.put(day, true);
        }
        final int[] rta = new int[rt.size()];
        for (int i = 0; i < rta.length; i++) {
            rta[i] = rt.keyAt(i);
        }
        Arrays.sort(rta);
        return rta;
    }

    protected static int[] getDaysOfWeekForLocale(Calendar c) {
        int[] daysOfWeek = new int[7];
        int currentDay = c.getFirstDayOfWeek();
        for (int i = 0; i < daysOfWeek.length; i++) {
            if (currentDay > 7) currentDay = 1;
            daysOfWeek[i] = currentDay;
            currentDay++;
        }
        return daysOfWeek;
    }

    protected void onChanged(int[] days) {
        // event hook for subclasses
    }
}
