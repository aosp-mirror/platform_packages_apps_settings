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
package com.android.settings.datausage;

import android.content.Context;
import android.util.Range;

import com.android.settings.Utils;
import com.android.settingslib.widget.SettingsSpinnerAdapter;

import java.util.List;

public class CycleAdapter extends SettingsSpinnerAdapter<CycleAdapter.CycleItem> {

    private final SpinnerInterface mSpinner;

    public CycleAdapter(Context context, SpinnerInterface spinner) {
        super(context);
        mSpinner = spinner;
        mSpinner.setAdapter(this);
    }

    /**
     * Find position of {@link CycleItem} in this adapter which is nearest
     * the given {@link CycleItem}.
     */
    public int findNearestPosition(CycleItem target) {
        if (target != null) {
            final int count = getCount();
            for (int i = count - 1; i >= 0; i--) {
                final CycleItem item = getItem(i);
                if (item.compareTo(target) >= 0) {
                    return i;
                }
            }
        }
        return 0;
    }

    void setInitialCycleList(List<Long> cycles, long selectedCycle) {
        clear();
        for (int i = 0; i < cycles.size() - 1; i++) {
            add(new CycleAdapter.CycleItem(getContext(), cycles.get(i + 1), cycles.get(i)));
            if (cycles.get(i) == selectedCycle) {
                mSpinner.setSelection(i);
            }
        }
    }

    /**
     * Rebuild list based on network data. Always selects the newest item,
     * updating the inspection range on chartData.
     */
    public void updateCycleList(List<Range<Long>> cycleData) {
        // stash away currently selected cycle to try restoring below
        final CycleAdapter.CycleItem previousItem = (CycleAdapter.CycleItem)
                mSpinner.getSelectedItem();
        clear();

        final Context context = getContext();
        for (Range<Long> cycle : cycleData) {
            add(new CycleAdapter.CycleItem(context, cycle.getLower(), cycle.getUpper()));
        }

        // force pick the current cycle (first item)
        if (getCount() > 0) {
            final int position = findNearestPosition(previousItem);
            mSpinner.setSelection(position);
        }
    }

    /**
     * List item that reflects a specific data usage cycle.
     */
    public static class CycleItem implements Comparable<CycleItem> {
        public CharSequence label;
        public long start;
        public long end;

        public CycleItem(Context context, long start, long end) {
            this.label = Utils.formatDateRange(context, start, end);
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return label.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CycleItem) {
                final CycleItem another = (CycleItem) o;
                return start == another.start && end == another.end;
            }
            return false;
        }

        @Override
        public int compareTo(CycleItem another) {
            return Long.compare(start, another.start);
        }
    }

    public interface SpinnerInterface {
        void setAdapter(CycleAdapter cycleAdapter);

        Object getSelectedItem();

        void setSelection(int position);
    }
}
