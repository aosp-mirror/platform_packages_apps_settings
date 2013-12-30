/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Handler;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.settings.R;
import com.google.android.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * Creates a percentage bar chart inside a preference.
 */
public class UsageBarPreference extends Preference {

    public interface OnRequestMediaRescanListener {
        void onRequestMediaRescan();
    }

    private ImageView mRescanMedia = null;
    private ProgressBar mRescanMediaWaiting = null;
    private PercentageBarChart mChart = null;

    private boolean mAllowMediaScan;

    private OnRequestMediaRescanListener mOnRequestMediaRescanListener;

    private final List<PercentageBarChart.Entry> mEntries = Lists.newArrayList();

    private Handler mHandler;

    public UsageBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public UsageBarPreference(Context context) {
        super(context);
        init();
    }

    public UsageBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_memoryusage);
        mHandler = new Handler();
        mAllowMediaScan = false;
    }

    public void addEntry(int order, float percentage, int color) {
        mEntries.add(PercentageBarChart.createEntry(order, percentage, color));
        Collections.sort(mEntries);
    }

    protected void setOnRequestMediaRescanListener(OnRequestMediaRescanListener listener) {
        mOnRequestMediaRescanListener = listener;
    }

    protected void setAllowMediaScan(boolean allow) {
        mAllowMediaScan = allow;
        notifyScanCompleted();
    }

    protected void notifyScanCompleted() {
        if (mRescanMedia != null) {
            mRescanMedia.setVisibility(mAllowMediaScan ? View.VISIBLE : View.INVISIBLE);
            mRescanMediaWaiting.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mChart = (PercentageBarChart) view.findViewById(R.id.percentage_bar_chart);
        mChart.setEntries(mEntries);

        mRescanMediaWaiting = (ProgressBar) view.findViewById(R.id.memory_usage_rescan_media_waiting);

        mRescanMedia = (ImageView) view.findViewById(R.id.memory_usage_rescan_media);
        mRescanMedia.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnRequestMediaRescanListener != null) {
                    mRescanMedia.setVisibility(View.GONE);
                    mRescanMediaWaiting.setVisibility(View.VISIBLE);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mOnRequestMediaRescanListener.onRequestMediaRescan();
                        }
                    });
                }
            }
        });

        notifyScanCompleted();
    }

    public void commit() {
        if (mChart != null) {
            mChart.invalidate();
        }
    }

    public void clear() {
        mEntries.clear();
    }
}
