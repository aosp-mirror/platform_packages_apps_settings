/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.content.Intent;
import android.os.storage.StorageManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.widget.DonutView;

/**
 * StorageSummaryDonutPreference is a preference which summarizes the used and remaining storage left
 * on a given storage volume. It is visualized with a donut graphing the % used.
 */
public class StorageSummaryDonutPreference extends Preference implements View.OnClickListener {
    private int mPercent = -1;

    public StorageSummaryDonutPreference(Context context) {
        this(context, null);
    }

    public StorageSummaryDonutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayoutResource(R.layout.storage_summary_donut);
        setEnabled(false);
    }

    public void setPercent(long usedBytes, long totalBytes) {
        if (totalBytes == 0) {
            return;
        }

        mPercent = MathUtils.constrain((int) ((usedBytes * 100) / totalBytes),
                (usedBytes > 0) ? 1 : 0, 100);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final DonutView donut = (DonutView) view.findViewById(R.id.donut);
        if (donut != null) {
            donut.setPercentage(mPercent);
        }

        final Button deletionHelperButton = (Button) view.findViewById(R.id.deletion_helper_button);
        if (deletionHelperButton != null) {
            deletionHelperButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null && R.id.deletion_helper_button == v.getId()) {
            Intent intent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
            getContext().startActivity(intent);
        }
    }
}
