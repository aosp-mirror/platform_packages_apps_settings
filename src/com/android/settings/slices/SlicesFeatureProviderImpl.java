/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import android.content.Context;

import com.android.settings.network.telephony.Enhanced4gLteSliceHelper;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;
import com.android.settingslib.utils.ThreadUtils;

/**
 * Manages Slices in Settings.
 */
public class SlicesFeatureProviderImpl implements SlicesFeatureProvider {

    private SlicesIndexer mSlicesIndexer;
    private SliceDataConverter mSliceDataConverter;
    private CustomSliceManager mCustomSliceManager;

    @Override
    public SlicesIndexer getSliceIndexer(Context context) {
        if (mSlicesIndexer == null) {
            mSlicesIndexer = new SlicesIndexer(context.getApplicationContext());
        }
        return mSlicesIndexer;
    }

    @Override
    public SliceDataConverter getSliceDataConverter(Context context) {
        if (mSliceDataConverter == null) {
            mSliceDataConverter = new SliceDataConverter(context.getApplicationContext());
        }
        return mSliceDataConverter;
    }

    @Override
    public CustomSliceManager getCustomSliceManager(Context context) {
        if (mCustomSliceManager == null) {
            mCustomSliceManager = new CustomSliceManager(context.getApplicationContext());
        }
        return mCustomSliceManager;
    }

    @Override
    public void indexSliceDataAsync(Context context) {
        SlicesIndexer indexer = getSliceIndexer(context);
        ThreadUtils.postOnBackgroundThread(indexer);
    }

    @Override
    public void indexSliceData(Context context) {
        SlicesIndexer indexer = getSliceIndexer(context);
        indexer.indexSliceData();
    }

    @Override
    public WifiCallingSliceHelper getNewWifiCallingSliceHelper(Context context) {
        return new WifiCallingSliceHelper(context);
    }

    @Override
    public Enhanced4gLteSliceHelper getNewEnhanced4gLteSliceHelper(Context context) {
        return new Enhanced4gLteSliceHelper(context);
    }
}
