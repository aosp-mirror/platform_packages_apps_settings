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

package com.android.settings.notification;

import android.content.Context;
import android.net.Uri;
import android.preference.SeekBarVolumizer;

/**
 * Testable wrapper around {@link SeekBarVolumizer} constructor.
 */
public class SeekBarVolumizerFactory {
    private final Context mContext;

    public SeekBarVolumizerFactory(Context context) {
        mContext = context;
    }

    /**
     * Creates a new SeekBarVolumizer.
     *
     * @param streamType of the audio manager.
     * @param defaultUri of the volume.
     * @param sbvc callback of the seekbar volumizer.
     * @return a SeekBarVolumizer.
     */
    public SeekBarVolumizer create(int streamType, Uri defaultUri, SeekBarVolumizer.Callback sbvc) {
        return new SeekBarVolumizer(mContext, streamType, defaultUri, sbvc);
    }
}
