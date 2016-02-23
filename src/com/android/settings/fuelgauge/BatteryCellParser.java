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
import android.telephony.ServiceState;
import android.util.SparseIntArray;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryActiveView.BatteryActiveProvider;
import com.android.settingslib.BatteryInfo;

public class BatteryCellParser implements BatteryInfo.BatteryDataParser, BatteryActiveProvider {

    private final SparseIntArray mData = new SparseIntArray();

    private int mLastValue;
    private long mLength;
    private long mLastTime;

    protected int getValue(HistoryItem rec) {
        int bin;
        if (((rec.states & HistoryItem.STATE_PHONE_STATE_MASK)
                >> HistoryItem.STATE_PHONE_STATE_SHIFT)
                == ServiceState.STATE_POWER_OFF) {
            bin = 0;
        } else if ((rec.states & HistoryItem.STATE_PHONE_SCANNING_FLAG) != 0) {
            bin = 1;
        } else {
            bin = (rec.states & HistoryItem.STATE_PHONE_SIGNAL_STRENGTH_MASK)
                    >> HistoryItem.STATE_PHONE_SIGNAL_STRENGTH_SHIFT;
            bin += 2;
        }
        return bin;
    }

    @Override
    public void onParsingStarted(long startTime, long endTime) {
        mLength = endTime - startTime;
    }

    @Override
    public void onDataPoint(long time, HistoryItem record) {
        int value = getValue(record);
        if (value != mLastValue) {
            mData.put((int) time, value);
            mLastValue = value;
        }
        mLastTime = time;
    }

    @Override
    public void onDataGap() {
        if (mLastValue != 0) {
            mData.put((int) mLastTime, 0);
            mLastValue = 0;
        }
    }

    @Override
    public void onParsingDone() {
        if (mLastValue != 0) {
            mData.put((int) mLastTime, 0);
            mLastValue = 0;
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

    private int getColor(int i) {
        return Utils.BADNESS_COLORS[i];
    }
}
