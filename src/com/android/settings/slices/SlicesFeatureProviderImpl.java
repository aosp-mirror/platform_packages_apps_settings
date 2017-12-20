package com.android.settings.slices;

import android.content.Context;

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
            mSlicesIndexer = new SlicesIndexer(context.getApplicationContext());
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
    public void indexSliceData(Context context) {
        // TODO (b/67996923) add indexing time log
        SlicesIndexer indexer = getSliceIndexer(context);
        ThreadUtils.postOnBackgroundThread(indexer);
    }
}
