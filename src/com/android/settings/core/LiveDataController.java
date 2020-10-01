/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

/**
 * This controller is targeted at the time consuming-bound controller.
 */
public abstract class LiveDataController extends BasePreferenceController {
    @VisibleForTesting
    protected CharSequence mSummary;

    private Preference mPreference;
    private MutableLiveData<CharSequence> mData;


    public LiveDataController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mData = new MutableLiveData<>();
        mSummary = context.getText(R.string.summary_placeholder);
    }

    /**
     * Initialize controller as live data controller and
     * execute the background task for retrieving the summary text right away.
     * @param fragment the owner of the live data observer.
     */
    public void initLifeCycleOwner(@NonNull Fragment fragment) {
        mData.observe(fragment, str -> {
            mSummary = str;
            refreshSummary(mPreference);
        });
        ThreadUtils.postOnBackgroundThread(() -> {
            mData.postValue(getSummaryTextInBackground());
        });
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        return mSummary;
    }

    /**
     * Execute the time consuming work.
     */
    @WorkerThread
    protected abstract CharSequence getSummaryTextInBackground();
}
