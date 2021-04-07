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

package com.android.settings.panel;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.REMOTE_MEDIA_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.VOLUME_ALARM_URI;
import static com.android.settings.slices.CustomSliceRegistry.VOLUME_CALL_URI;
import static com.android.settings.slices.CustomSliceRegistry.VOLUME_MEDIA_URI;
import static com.android.settings.slices.CustomSliceRegistry.VOLUME_RINGER_URI;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.Settings;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settingslib.media.MediaOutputSliceConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel data class for Volume settings.
 */
public class VolumePanel implements PanelContent, LifecycleObserver {

    private final Context mContext;

    private PanelContentCallback mCallback;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MediaOutputSliceConstants.ACTION_CLOSE_PANEL.equals(intent.getAction())) {
                mCallback.forceClose();
            }
        }
    };

    public static VolumePanel create(Context context) {
        return new VolumePanel(context);
    }

    private VolumePanel(Context context) {
        mContext = context.getApplicationContext();
    }

    /** Invoked when the panel is resumed. */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MediaOutputSliceConstants.ACTION_CLOSE_PANEL);
        mContext.registerReceiver(mReceiver, filter);
    }

    /** Invoked when the panel is paused. */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public CharSequence getTitle() {
        return mContext.getText(R.string.sound_settings);
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();

        uris.add(REMOTE_MEDIA_SLICE_URI);
        uris.add(VOLUME_MEDIA_URI);
        uris.add(MEDIA_OUTPUT_INDICATOR_SLICE_URI);
        uris.add(VOLUME_CALL_URI);
        uris.add(VOLUME_RINGER_URI);
        uris.add(VOLUME_ALARM_URI);
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        return new Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_VOLUME;
    }

    @Override
    public int getViewType() {
        return PanelContent.VIEW_TYPE_SLIDER;
    }

    @Override
    public void registerCallback(PanelContentCallback callback) {
        mCallback = callback;
    }
}