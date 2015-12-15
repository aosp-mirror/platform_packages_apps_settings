/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.accessibility;

import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.PreviewSeekBarPreferenceFragment;

import android.annotation.Nullable;
import android.app.ActivityManagerNative;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * Preference fragment used to control font size.
 */
public class ToggleFontSizePreferenceFragment extends PreviewSeekBarPreferenceFragment {
    private static final String LOG_TAG = "ToggleFontSizePreferenceFragment";

    private float[] mValues;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityLayoutResId = R.layout.font_size_activity;
        mPreviewSampleResId = R.layout.font_size_preview;

        Resources res = getContext().getResources();
        // Mark the appropriate item in the preferences list.
        final Configuration origConfig = res.getConfiguration();
        mEntries = res.getStringArray(R.array.entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        mInitialIndex = floatToIndex(origConfig.fontScale, strEntryValues);
        mValues = new float[strEntryValues.length];
        for (int i = 0; i < strEntryValues.length; ++i) {
            mValues[i] = Float.parseFloat(strEntryValues[i]);
        }
    }

    @Override
    protected Configuration createConfig(Configuration origConfig, int index) {
        // Populate the sample layouts.
        final Configuration config = new Configuration(origConfig);
        config.fontScale = mValues[index];
        return config;
    }

    /**
     * Persists the selected font size and sends a configuration change.
     */
    @Override
    protected void commit() {
        Configuration config = getContext().getResources().getConfiguration();
        config.fontScale = mValues[mCurrentIndex];
        try {
            ActivityManagerNative.getDefault().updatePersistentConfiguration(config);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Unable to save font size setting");
        }
    }

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.ACCESSIBILITY_FONT_SIZE;
    }

    private int floatToIndex(float val, String[] indices) {
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }

}