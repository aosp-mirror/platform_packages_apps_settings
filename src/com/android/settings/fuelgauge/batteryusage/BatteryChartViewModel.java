/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** The view model of {@code BatteryChartView} */
class BatteryChartViewModel {
    private static final String TAG = "BatteryChartViewModel";

    public static final int SELECTED_INDEX_ALL = -1;
    public static final int SELECTED_INDEX_INVALID = -2;

    // We need at least 2 levels to draw a trapezoid.
    private static final int MIN_LEVELS_DATA_SIZE = 2;

    enum AxisLabelPosition {
        BETWEEN_TRAPEZOIDS,
        CENTER_OF_TRAPEZOIDS,
    }

    private final List<Integer> mLevels;
    private final List<String> mTexts;
    private final AxisLabelPosition mAxisLabelPosition;
    private int mSelectedIndex = SELECTED_INDEX_ALL;

    BatteryChartViewModel(
            @NonNull List<Integer> levels, @NonNull List<String> texts,
            @NonNull AxisLabelPosition axisLabelPosition) {
        Preconditions.checkArgument(
                levels.size() == texts.size() && levels.size() >= MIN_LEVELS_DATA_SIZE,
                String.format(Locale.ENGLISH,
                        "Invalid BatteryChartViewModel levels.size: %d, texts.size: %d.",
                        levels.size(), texts.size()));
        mLevels = levels;
        mTexts = texts;
        mAxisLabelPosition = axisLabelPosition;
    }

    public int size() {
        return mLevels.size();
    }

    public List<Integer> levels() {
        return mLevels;
    }

    public List<String> texts() {
        return mTexts;
    }

    public AxisLabelPosition axisLabelPosition() {
        return mAxisLabelPosition;
    }

    public int selectedIndex() {
        return mSelectedIndex;
    }

    public void setSelectedIndex(int index) {
        mSelectedIndex = index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLevels, mTexts, mSelectedIndex, mAxisLabelPosition);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof BatteryChartViewModel)) {
            return false;
        }
        final BatteryChartViewModel batteryChartViewModel = (BatteryChartViewModel) other;
        return Objects.equals(mLevels, batteryChartViewModel.mLevels)
                && Objects.equals(mTexts, batteryChartViewModel.mTexts)
                && mAxisLabelPosition == batteryChartViewModel.mAxisLabelPosition
                && mSelectedIndex == batteryChartViewModel.mSelectedIndex;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "levels: %s,\ntexts: %s,\naxisLabelPosition: %s, selectedIndex: %d",
                Objects.toString(mLevels), Objects.toString(mTexts), mAxisLabelPosition,
                mSelectedIndex);
    }
}
