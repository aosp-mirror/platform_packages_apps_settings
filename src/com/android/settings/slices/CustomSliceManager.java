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
import android.net.Uri;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages custom {@link androidx.slice.Slice Slices}, which are all Slices not backed by
 * preferences.
 */
public class CustomSliceManager {

    private final Context mContext;
    private final Map<Uri, CustomSliceable> mSliceableCache;

    public CustomSliceManager(Context context) {
        mContext = context.getApplicationContext();
        mSliceableCache = new WeakHashMap<>();
    }

    /**
     * Return a {@link CustomSliceable} associated to the Uri.
     * <p>
     * Do not change this method signature to accommodate for a special-case slicable - a context is
     * the only thing that should be needed to create the object.
     */
    public CustomSliceable getSliceableFromUri(Uri uri) {
        final Uri newUri = CustomSliceRegistry.removeParameterFromUri(uri);
        if (mSliceableCache.containsKey(newUri)) {
            return mSliceableCache.get(newUri);
        }

        final Class clazz = CustomSliceRegistry.getSliceClassByUri(newUri);
        if (clazz == null) {
            throw new IllegalArgumentException("No Slice found for uri: " + uri);
        }

        final CustomSliceable sliceable = CustomSliceable.createInstance(mContext, clazz);
        mSliceableCache.put(newUri, sliceable);
        return sliceable;
    }


    /**
     * Return a {@link CustomSliceable} associated to the Action.
     * <p>
     * Do not change this method signature to accommodate for a special-case sliceable - a context
     * is the only thing that should be needed to create the object.
     */
    public CustomSliceable getSliceableFromIntentAction(String action) {
        return getSliceableFromUri(Uri.parse(action));
    }
}
