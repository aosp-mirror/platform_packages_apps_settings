/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.applications;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.preference.Preference;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.view.View;

import com.android.settings.R;

public class ProcessStatsPreference extends Preference {

    private ProcStatsPackageEntry mEntry;
    private final int mColor;
    private final int mRemainingColor;
    private float mRatio;
    private float mRemainingRatio;

    public ProcessStatsPreference(Context context) {
        this(context, null);
    }

    public ProcessStatsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProcessStatsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ProcessStatsPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.app_item_linear_color);
        mColor = context.getColor(R.color.memory_max_use);
        mRemainingColor = context.getColor(R.color.memory_remaining);
    }

    public void init(ProcStatsPackageEntry entry, PackageManager pm, double maxMemory,
            double weightToRam, double totalScale, boolean avg) {
        mEntry = entry;
        setTitle(TextUtils.isEmpty(entry.mUiLabel) ? entry.mPackage : entry.mUiLabel);
        if (entry.mUiTargetApp != null) {
            setIcon(entry.mUiTargetApp.loadIcon(pm));
        } else {
            setIcon(new ColorDrawable(0));
        }
        boolean statsForeground = entry.mRunWeight > entry.mBgWeight;
        double amount = avg ? (statsForeground ? entry.mRunWeight : entry.mBgWeight) * weightToRam
                : (statsForeground ? entry.mMaxRunMem : entry.mMaxBgMem) * totalScale * 1024;
        setSummary(Formatter.formatShortFileSize(getContext(), (long) amount));
        mRatio = (float) (amount / maxMemory);
        mRemainingRatio = 1 - mRatio;
    }

    public ProcStatsPackageEntry getEntry() {
        return mEntry;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        LinearColorBar linearColorBar = (LinearColorBar) view.findViewById(R.id.linear_color_bar);
        linearColorBar.setColors(mColor, mColor, mRemainingColor);
        linearColorBar.setRatios(mRatio, 0, mRemainingRatio);
    }
}
