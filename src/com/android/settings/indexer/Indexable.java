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

package com.android.settings.indexer;

import android.content.Context;

import java.util.List;

/**
 * Interface for classes whose instances can provide data for indexing.
 *
 * Classes implementing the Indexable interface must have a static field called
 * <code>INDEX_DATA_PROVIDER</code>, which is an object implementing the
 * {@link Indexable.IndexDataProvider Indexable.IndexDataProvider} interface.
 *
 * See {@link IndexableRef} and {@link IndexableData}.
 *
 */
public interface Indexable {

    public interface IndexDataProvider {
        /**
         * Return a list of references for indexing. See {@link IndexableRef}
         *
         * @param context the context
         * @return a list of {@link IndexableRef} references. Can be null.
         */
        List<IndexableRef> getRefsToIndex(Context context);

        /**
         * Return a list of raw data for indexing. See {@link IndexableData}
         *
         * @param context the context
         * @return a list of {@link IndexableData} references. Can be null.
         */
        List<IndexableData> getRawDataToIndex(Context context);
    }
}
