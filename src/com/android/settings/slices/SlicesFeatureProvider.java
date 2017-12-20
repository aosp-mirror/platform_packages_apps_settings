package com.android.settings.slices;

import android.content.Context;

/**
 * Manages Slices in Settings.
 */
public interface SlicesFeatureProvider {

    boolean DEBUG = false;

    SlicesIndexer getSliceIndexer(Context context);

    SliceDataConverter getSliceDataConverter(Context context);

    void indexSliceData(Context context);
}