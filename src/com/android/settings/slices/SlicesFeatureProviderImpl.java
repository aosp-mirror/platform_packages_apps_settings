package com.android.settings.slices;

import android.content.Context;

import com.android.settings.wifi.calling.WifiCallingSliceHelper;
import com.android.settingslib.utils.ThreadUtils;

/**
 * Manages Slices in Settings.
 */
public class SlicesFeatureProviderImpl implements SlicesFeatureProvider {

    private SlicesIndexer mSlicesIndexer;
    private SliceDataConverter mSliceDataConverter;

    @Override
    public SlicesIndexer getSliceIndexer(Context context) {
        if (mSlicesIndexer == null) {
            mSlicesIndexer = new SlicesIndexer(context);
        }
        return mSlicesIndexer;
    }

    @Override
    public SliceDataConverter getSliceDataConverter(Context context) {
        if(mSliceDataConverter == null) {
            mSliceDataConverter = new SliceDataConverter(context.getApplicationContext());
        }
        return mSliceDataConverter;
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
}
