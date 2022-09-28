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
import static com.android.settings.slices.CustomSliceRegistry.VOLUME_NOTIFICATION_URI;
import static com.android.settings.slices.CustomSliceRegistry.VOLUME_RINGER_URI;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.media.MediaOutputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Panel data class for Volume settings.
 */
public class VolumePanel implements PanelContent, LifecycleObserver {
    private static final String TAG = "VolumePanel";

    private final Context mContext;

    private PanelContentCallback mCallback;
    private LocalBluetoothProfileManager mProfileManager;
    private int mControlSliceWidth;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MediaOutputConstants.ACTION_CLOSE_PANEL.equals(intent.getAction())) {
                mCallback.forceClose();
            }
        }
    };

    public static VolumePanel create(Context context) {
        return new VolumePanel(context);
    }

    private VolumePanel(Context context) {
        mContext = context.getApplicationContext();
        if (context instanceof Activity) {
            int panelWidth =
                    ((Activity) context).getWindowManager().getCurrentWindowMetrics().getBounds()
                            .width();
            // The control slice width = panel width - two left and right horizontal paddings
            mControlSliceWidth = panelWidth - context.getResources().getDimensionPixelSize(
                    R.dimen.panel_slice_Horizontal_padding) * 2;
        }

        final FutureTask<LocalBluetoothManager> localBtManagerFutureTask = new FutureTask<>(
                // Avoid StrictMode ThreadPolicy violation
                () -> Utils.getLocalBtManager(mContext));
        LocalBluetoothManager localBluetoothManager;
        try {
            localBtManagerFutureTask.run();
            localBluetoothManager = localBtManagerFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting LocalBluetoothManager.");
            return;
        }
        if (localBluetoothManager != null) {
            mProfileManager = localBluetoothManager.getProfileManager();
        }
    }

    /** Invoked when the panel is resumed. */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MediaOutputConstants.ACTION_CLOSE_PANEL);
        mContext.registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED_UNAUDITED);
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
        Uri controlUri = getExtraControlUri();
        if (controlUri != null) {
            Log.d(TAG, "add extra control slice");
            uris.add(controlUri);
        }
        uris.add(MEDIA_OUTPUT_INDICATOR_SLICE_URI);
        uris.add(VOLUME_CALL_URI);
        uris.add(VOLUME_RINGER_URI);
        if (com.android.settings.Utils.isVoiceCapable(mContext) && Settings.Secure.getInt(
                mContext.getContentResolver(), Settings.Secure.VOLUME_LINK_NOTIFICATION, 1) == 0) {
            uris.add(VOLUME_NOTIFICATION_URI);
        }
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

    private Uri getExtraControlUri() {
        Uri controlUri = null;
        final BluetoothDevice bluetoothDevice = findActiveDevice();
        if (bluetoothDevice != null) {
            final String uri = BluetoothUtils.getControlUriMetaData(bluetoothDevice);
            if (!TextUtils.isEmpty(uri)) {
                try {
                    controlUri = Uri.parse(uri + mControlSliceWidth);
                } catch (NullPointerException exception) {
                    Log.d(TAG, "unable to parse uri");
                    controlUri = null;
                }
            }
        }
        return controlUri;
    }

    private BluetoothDevice findActiveDevice() {
        if (mProfileManager != null) {
            final A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
            if (a2dpProfile != null) {
                return a2dpProfile.getActiveDevice();
            }
        }
        return null;
    }
}