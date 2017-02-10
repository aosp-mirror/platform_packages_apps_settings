/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import android.util.AttributeSet;

import com.android.settings.R;

public class StorageItemPreferenceAlternate extends Preference {
    public StorageItemPreferenceAlternate(Context context) {
        this(context, null);
    }

    public StorageItemPreferenceAlternate(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.storage_item_alternate);
        setSummary(R.string.memory_calculating_size);
    }

    public void setStorageSize(long size) {
        setSummary(size == 0
                ? String.valueOf(0)
                : Formatter.formatFileSize(getContext(), size));
    }
}