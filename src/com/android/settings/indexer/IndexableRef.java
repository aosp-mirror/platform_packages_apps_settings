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

/**
 * Indexable Reference.
 *
 * This class wraps a set of information representing data that can be indexed for a high
 * level (see {@link android.preference.PreferenceScreen}).
 *
 * rank: is the rank of the data (basically its order in the list of Settings)
 * xmlResId: is the resource Id of a PreferenceScreen xml file
 * fragmentName: is the fragment class name associated with the data
 * iconRedId: is the resource Id of an icon that represents the data
 *
 * See {@link Indexable} and {@link IndexableData}.
 *
 */
public class IndexableRef {

    public int rank;
    public int xmlResId;
    public String fragmentName;
    public int iconResId;

    public IndexableRef(int rank, int dataResId, String name, int iconResId) {
        this.rank = rank;
        this.xmlResId = dataResId;
        this.fragmentName = name;
        this.iconResId = iconResId;
    }
}