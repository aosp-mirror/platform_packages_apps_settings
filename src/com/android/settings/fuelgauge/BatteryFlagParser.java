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

package com.android.settings.fuelgauge;

import android.os.BatteryStats.HistoryItem;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.android.settings.fuelgauge.BatteryActiveView.BatteryActiveProvider;

public class BatteryFlagParser implements BatteryInfo.BatteryDataParser, BatteryActiveProvider {

    private final SparseBooleanArray mData = new SparseBooleanArray();
    private final int mFlag;
    private final boolean mState2;
    private final int mAccentColor;

    private boolean mLastSet;
    private long mLength;
    private long mLastTime;

    public BatteryFlagParser(int accent, boolean state2, int flag) {
        mAccentColor = accent;
        mFlag = flag;
        mState2 = state2;
    }

    protected boolean isSet(HistoryItem record) {
        return ((mState2 ? record.states2 : record.states) & mFlag) != 0;
    }

    @Override
    public void onParsingStarted(long startTime, long endTime) {
        mLength = endTime - startTime;
    }

    @Override
    public void onDataPoint(long time, HistoryItem record) {
        boolean isSet = isSet(record);
        if (isSet != mLastSet) {
            mData.put((int) time, isSet);
            mLastSet = isSet;
        }
        mLastTime = time;
    }

    @Override
    public void onDataGap() {
        if (mLastSet) {
            mData.put((int) mLastTime, false);
            mLastSet = false;
        }
    }

    @Override
    public void onParsingDone() {
        if (mLastSet) {
            mData.put((int) mLastTime, false);
            mLastSet = false;
        }
    }

    @Override
    public long getPeriod() {
        return mLength;
    }

    @Override
    public boolean hasData() {
        return mData.size() > 1;
    }

    @Override
    public SparseIntArray getColorArray() {
        SparseIntArray ret = new SparseIntArray();
        for (int i = 0; i < mData.size(); i++) {
            ret.put(mData.keyAt(i), getColor(mData.valueAt(i)));
        }
        return ret;
    }

    private int getColor(boolean b) {
        if (b) {
            return mAccentColor;
        }
        return 0;
    }
}
