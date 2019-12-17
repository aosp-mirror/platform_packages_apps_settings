/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.utils;

import android.graphics.drawable.Drawable;

import com.android.settingslib.widget.CandidateInfo;

public class CandidateInfoExtra extends CandidateInfo {
    private final CharSequence mLabel;
    private final CharSequence mSummary;
    private final String mKey;

    public CandidateInfoExtra(CharSequence label, CharSequence summary, String key,
            boolean enabled) {
        super(enabled);
        mLabel = label;
        mSummary = summary;
        mKey = key;
    }

    @Override
    public CharSequence loadLabel() {
        return mLabel;
    }

    public CharSequence loadSummary() {
        return mSummary;
    }

    @Override
    public Drawable loadIcon() {
        return null;
    }

    @Override
    public String getKey() {
        return mKey;
    }
}
