package com.android.settings.slices;

import android.content.Context;
import android.net.Uri;

import com.android.settings.network.telephony.Enhanced4gLteSliceHelper;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;

/**
 * Manages Slices in Settings.
 */
public interface SlicesFeatureProvider {

    boolean DEBUG = false;

    SliceDataConverter getSliceDataConverter(Context context);

    /**
     * Starts a new UI session for the purpose of using Slices.
     *
     * A UI session is defined as a duration of time when user stays in a UI screen. Screen rotation
     * does not break the continuation of session, going to a sub-page and coming out does not break
     * the continuation either. Leaving the page and coming back breaks it.
     */
    void newUiSession();

    /**
     * Returns the token created in {@link #newUiSession}.
     */
    long getUiSessionToken();

    /**
     * Asynchronous call to index the data used to build Slices.
     * If the data is already indexed, the data will not change.
     */
    void indexSliceDataAsync(Context context);

    /**
     * Indexes the data used to build Slices.
     * If the data is already indexed, the data will not change.
     */
    void indexSliceData(Context context);


    /**
     * Return a {@link CustomSliceable} associated to the Uri.
     * <p>
     * Do not change this method signature to accommodate for a special-case sliceable - a context
     * is the only thing that should be needed to create the object.
     */
    CustomSliceable getSliceableFromUri(Context context, Uri uri);

    /**
     * Gets new WifiCallingSliceHelper object
     */
    WifiCallingSliceHelper getNewWifiCallingSliceHelper(Context context);

    /**
     * Gets new Enhanced4gLteSliceHelper object
     */
    Enhanced4gLteSliceHelper getNewEnhanced4gLteSliceHelper(Context context);
}

