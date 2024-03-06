/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import android.content.Context;
import android.util.AttributeSet;

import com.android.settings.ProgressCategory;
import com.android.settings.R;

public class AudioStreamsProgressCategoryPreference extends ProgressCategory {

    public AudioStreamsProgressCategoryPreference(Context context) {
        super(context);
        init();
    }

    public AudioStreamsProgressCategoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioStreamsProgressCategoryPreference(
            Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AudioStreamsProgressCategoryPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setEmptyTextRes(R.string.audio_streams_empty);
    }
}
