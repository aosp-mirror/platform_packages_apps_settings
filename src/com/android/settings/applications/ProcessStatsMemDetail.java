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

package com.android.settings.applications;

import android.app.Fragment;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.app.ProcessStats;
import com.android.settings.R;

import static com.android.settings.Utils.prepareCustomPreferencesList;

public class ProcessStatsMemDetail extends Fragment {
    public static final String EXTRA_MEM_TIMES = "mem_times";
    public static final String EXTRA_MEM_STATE_WEIGHTS = "mem_state_weights";
    public static final String EXTRA_MEM_CACHED_WEIGHT = "mem_cached_weight";
    public static final String EXTRA_MEM_FREE_WEIGHT = "mem_free_weight";
    public static final String EXTRA_MEM_ZRAM_WEIGHT = "mem_zram_weight";
    public static final String EXTRA_MEM_KERNEL_WEIGHT = "mem_kernel_weight";
    public static final String EXTRA_MEM_NATIVE_WEIGHT = "mem_native_weight";
    public static final String EXTRA_MEM_TOTAL_WEIGHT = "mem_total_weight";
    public static final String EXTRA_USE_USS = "use_uss";
    public static final String EXTRA_TOTAL_TIME = "total_time";

    long[] mMemTimes;
    double[] mMemStateWeights;
    double mMemCachedWeight;
    double mMemFreeWeight;
    double mMemZRamWeight;
    double mMemKernelWeight;
    double mMemNativeWeight;
    double mMemTotalWeight;
    boolean mUseUss;
    long mTotalTime;

    private View mRootView;
    private ViewGroup mMemStateParent;
    private ViewGroup mMemUseParent;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Bundle args = getArguments();
        mMemTimes = args.getLongArray(EXTRA_MEM_TIMES);
        mMemStateWeights = args.getDoubleArray(EXTRA_MEM_STATE_WEIGHTS);
        mMemCachedWeight = args.getDouble(EXTRA_MEM_CACHED_WEIGHT);
        mMemFreeWeight = args.getDouble(EXTRA_MEM_FREE_WEIGHT);
        mMemZRamWeight = args.getDouble(EXTRA_MEM_ZRAM_WEIGHT);
        mMemKernelWeight = args.getDouble(EXTRA_MEM_KERNEL_WEIGHT);
        mMemNativeWeight = args.getDouble(EXTRA_MEM_NATIVE_WEIGHT);
        mMemTotalWeight = args.getDouble(EXTRA_MEM_TOTAL_WEIGHT);
        mUseUss = args.getBoolean(EXTRA_USE_USS);
        mTotalTime = args.getLong(EXTRA_TOTAL_TIME);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.process_stats_mem_details, container, false);
        prepareCustomPreferencesList(container, view, view, false);

        mRootView = view;
        createDetails();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void createDetails() {
        mMemStateParent = (ViewGroup)mRootView.findViewById(R.id.mem_state);
        mMemUseParent = (ViewGroup)mRootView.findViewById(R.id.mem_use);

        fillMemStateSection();
        fillMemUseSection();
    }

    private void addDetailsItem(ViewGroup parent, CharSequence title,
            float level, CharSequence value) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup item = (ViewGroup) inflater.inflate(R.layout.app_percentage_item,
                null);
        parent.addView(item);
        item.findViewById(android.R.id.icon).setVisibility(View.GONE);
        TextView titleView = (TextView) item.findViewById(android.R.id.title);
        TextView valueView = (TextView) item.findViewById(android.R.id.text1);
        titleView.setText(title);
        valueView.setText(value);
        ProgressBar progress = (ProgressBar) item.findViewById(android.R.id.progress);
        progress.setProgress(Math.round(level*100));
    }

    private void fillMemStateSection() {
        CharSequence[] labels = getResources().getTextArray(R.array.proc_stats_memory_states);
        for (int i=0; i<ProcessStats.ADJ_MEM_FACTOR_COUNT; i++) {
            if (mMemTimes[i] > 0) {
                float level = ((float)mMemTimes[i])/mTotalTime;
                addDetailsItem(mMemStateParent, labels[i], level,
                        Formatter.formatShortElapsedTime(getActivity(), mMemTimes[i]));
            }
        }
    }

    private void addMemUseDetailsItem(ViewGroup parent, CharSequence title, double weight) {
        if (weight > 0) {
            float level = (float)(weight/mMemTotalWeight);
            String value = Formatter.formatShortFileSize(getActivity(),
                    (long)((weight * 1024) / mTotalTime));
            addDetailsItem(parent, title, level, value);
        }
    }

    private void fillMemUseSection() {
        CharSequence[] labels = getResources().getTextArray(R.array.proc_stats_process_states);
        addMemUseDetailsItem(mMemUseParent,
                getResources().getText(R.string.mem_use_kernel_type), mMemKernelWeight);
        addMemUseDetailsItem(mMemUseParent,
                getResources().getText(R.string.mem_use_zram_type), mMemZRamWeight);
        addMemUseDetailsItem(mMemUseParent,
                getResources().getText(R.string.mem_use_native_type), mMemNativeWeight);
        for (int i=0; i<ProcessStats.STATE_COUNT; i++) {
            addMemUseDetailsItem(mMemUseParent, labels[i], mMemStateWeights[i]);
        }
        addMemUseDetailsItem(mMemUseParent,
                getResources().getText(R.string.mem_use_kernel_cache_type), mMemCachedWeight);
        addMemUseDetailsItem(mMemUseParent,
                getResources().getText(R.string.mem_use_free_type), mMemFreeWeight);
        addMemUseDetailsItem(mMemUseParent,
                getResources().getText(R.string.mem_use_total), mMemTotalWeight);
    }
}
