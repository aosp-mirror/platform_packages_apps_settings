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
 * limitations under the License.
 */

package com.android.settings.notification;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

/**
 * A slider preference that controls remote volume, which doesn't go through
 * {@link android.media.AudioManager}
 **/
public class RemoteVolumeSeekBarPreference extends VolumeSeekBarPreference {

    public RemoteVolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RemoteVolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RemoteVolumeSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RemoteVolumeSeekBarPreference(Context context) {
        super(context);
    }

    @Override
    public void setStream(int stream) {
        // Do nothing here, volume is not controlled by AudioManager
    }

    @Override
    protected void init() {
        if (mSeekBar == null) return;
        setContinuousUpdates(true);
        updateIconView();
        updateSuppressionText();
        notifyHierarchyChanged();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        if (fromUser) {
            notifyChanged();
        }
    }
}
