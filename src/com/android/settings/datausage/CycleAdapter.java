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
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory;
import android.text.format.DateUtils;
import android.util.Pair;
import android.widget.AdapterView;

import com.android.settings.Utils;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.NetworkCycleData;
import com.android.settingslib.widget.settingsspinner.SettingsSpinnerAdapter;

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class CycleAdapter extends SettingsSpinnerAdapter<CycleAdapter.CycleItem> {

    private final SpinnerInterface mSpinner;
    private final AdapterView.OnItemSelectedListener mListener;

    public CycleAdapter(Context context, SpinnerInterface spinner,
            AdapterView.OnItemSelectedListener listener) {
        super(context);
        mSpinner = spinner;
        mListener = listener;
        mSpinner.setAdapter(this);
        mSpinner.setOnItemSelectedListener(mListener);
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

    /**
     * Rebuild list based on {@link NetworkPolicy} and available
     * {@link NetworkStatsHistory} data. Always selects the newest item,
     * updating the inspection range on chartData.
     */
    @Deprecated
    public boolean updateCycleList(NetworkPolicy policy, ChartData chartData) {
        // stash away currently selected cycle to try restoring below
        final CycleAdapter.CycleItem previousItem = (CycleAdapter.CycleItem)
                mSpinner.getSelectedItem();
        clear();

        final Context context = getContext();
        NetworkStatsHistory.Entry entry = null;

        long historyStart = Long.MAX_VALUE;
        long historyEnd = Long.MIN_VALUE;
        if (chartData != null) {
            historyStart = chartData.network.getStart();
            historyEnd = chartData.network.getEnd();
        }

        final long now = System.currentTimeMillis();
        if (historyStart == Long.MAX_VALUE) historyStart = now;
        if (historyEnd == Long.MIN_VALUE) historyEnd = now + 1;

        boolean hasCycles = false;
        if (policy != null) {
            final Iterator<Pair<ZonedDateTime, ZonedDateTime>> it = NetworkPolicyManager
                    .cycleIterator(policy);
            while (it.hasNext()) {
                final Pair<ZonedDateTime, ZonedDateTime> cycle = it.next();
                final long cycleStart = cycle.first.toInstant().toEpochMilli();
                final long cycleEnd = cycle.second.toInstant().toEpochMilli();

                final boolean includeCycle;
                if (chartData != null) {
                    entry = chartData.network.getValues(cycleStart, cycleEnd, entry);
                    includeCycle = (entry.rxBytes + entry.txBytes) > 0;
                } else {
                    includeCycle = true;
                }

                if (includeCycle) {
                    add(new CycleAdapter.CycleItem(context, cycleStart, cycleEnd));
                    hasCycles = true;
                }
            }
        }

        if (!hasCycles) {
            // no policy defined cycles; show entry for each four-week period
            long cycleEnd = historyEnd;
            while (cycleEnd > historyStart) {
                final long cycleStart = cycleEnd - (DateUtils.WEEK_IN_MILLIS * 4);

                final boolean includeCycle;
                if (chartData != null) {
                    entry = chartData.network.getValues(cycleStart, cycleEnd, entry);
                    includeCycle = (entry.rxBytes + entry.txBytes) > 0;
                } else {
                    includeCycle = true;
                }

                if (includeCycle) {
                    add(new CycleAdapter.CycleItem(context, cycleStart, cycleEnd));
                }
                cycleEnd = cycleStart;
            }
        }

        // force pick the current cycle (first item)
        if (getCount() > 0) {
            final int position = findNearestPosition(previousItem);
            mSpinner.setSelection(position);

            // only force-update cycle when changed; skipping preserves any
            // user-defined inspection region.
            final CycleAdapter.CycleItem selectedItem = getItem(position);
            if (!Objects.equals(selectedItem, previousItem)) {
                mListener.onItemSelected(null, null, position, 0);
                return false;
            }
        }
        return true;
    }

    /**
     * Rebuild list based on network data. Always selects the newest item,
     * updating the inspection range on chartData.
     */
    public boolean updateCycleList(List<? extends NetworkCycleData> cycleData) {
        // stash away currently selected cycle to try restoring below
        final CycleAdapter.CycleItem previousItem = (CycleAdapter.CycleItem)
                mSpinner.getSelectedItem();
        clear();

        final Context context = getContext();
        for (NetworkCycleData data : cycleData) {
            add(new CycleAdapter.CycleItem(context, data.getStartTime(), data.getEndTime()));
        }

        // force pick the current cycle (first item)
        if (getCount() > 0) {
            final int position = findNearestPosition(previousItem);
            mSpinner.setSelection(position);

            // only force-update cycle when changed; skipping preserves any
            // user-defined inspection region.
            final CycleAdapter.CycleItem selectedItem = getItem(position);
            if (!Objects.equals(selectedItem, previousItem)) {
                mListener.onItemSelected(null, null, position, 0);
                return false;
            }
        }
        return true;
    }

    /**
     * List item that reflects a specific data usage cycle.
     */
    public static class CycleItem implements Comparable<CycleItem> {
        public CharSequence label;
        public long start;
        public long end;

        public CycleItem(CharSequence label) {
            this.label = label;
        }

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

        void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener);

        Object getSelectedItem();

        void setSelection(int position);
    }
}
