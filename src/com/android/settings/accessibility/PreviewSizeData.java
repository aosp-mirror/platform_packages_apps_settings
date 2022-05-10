/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Abstract data class for storing and fetching the configurations related to the preview of the
 * text and reading options.
 */
abstract class PreviewSizeData<T extends Number> {
    private final Context mContext;
    private int mInitialIndex;
    private T mDefaultValue;
    private List<T> mValues;

    PreviewSizeData(@NonNull Context context) {
        mContext = context;
    }

    Context getContext() {
        return mContext;
    }

    List<T> getValues() {
        return mValues;
    }

    void setValues(List<T> values) {
        mValues = values;
    }

    T getDefaultValue() {
        return mDefaultValue;
    }

    void setDefaultValue(T defaultValue) {
        mDefaultValue = defaultValue;
    }

    int getInitialIndex() {
        return mInitialIndex;
    }

    void setInitialIndex(int initialIndex) {
        mInitialIndex = initialIndex;
    }

    /**
     * Persists the selected size.
     */
    abstract void commit(int currentProgress);
}
