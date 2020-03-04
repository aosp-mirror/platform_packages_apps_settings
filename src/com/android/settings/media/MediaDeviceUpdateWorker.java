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

package com.android.settings.media;

import static android.media.AudioManager.STREAM_DEVICES_CHANGED_ACTION;

import static com.android.settings.media.MediaOutputSlice.MEDIA_PACKAGE_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.Utils;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SliceBackgroundWorker for get MediaDevice list and handle MediaDevice state change event.
 */
public class MediaDeviceUpdateWorker extends SliceBackgroundWorker
        implements LocalMediaManager.DeviceCallback {

    private final Context mContext;
    private final Collection<MediaDevice> mMediaDevices = new CopyOnWriteArrayList<>();
    private final DevicesChangedBroadcastReceiver mReceiver;
    private final String mPackageName;

    private boolean mIsTouched;
    private MediaDevice mTopDevice;

    @VisibleForTesting
    LocalMediaManager mLocalMediaManager;

    public MediaDeviceUpdateWorker(Context context, Uri uri) {
        super(context, uri);
        mContext = context;
        mPackageName = uri.getQueryParameter(MEDIA_PACKAGE_NAME);
        mReceiver = new DevicesChangedBroadcastReceiver();
    }

    @Override
    protected void onSlicePinned() {
        mMediaDevices.clear();
        mIsTouched = false;
        if (mLocalMediaManager == null) {
            mLocalMediaManager = new LocalMediaManager(mContext, mPackageName, null);
        }

        mLocalMediaManager.registerCallback(this);
        final IntentFilter intentFilter = new IntentFilter(STREAM_DEVICES_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, intentFilter);
        mLocalMediaManager.startScan();
    }

    @Override
    protected void onSliceUnpinned() {
        mLocalMediaManager.unregisterCallback(this);
        mContext.unregisterReceiver(mReceiver);
        mLocalMediaManager.stopScan();
    }

    @Override
    public void close() {
        mLocalMediaManager = null;
    }

    @Override
    public void onDeviceListUpdate(List<MediaDevice> devices) {
        buildMediaDevices(devices);
        notifySliceChange();
    }

    private void buildMediaDevices(List<MediaDevice> devices) {
        mMediaDevices.clear();
        mMediaDevices.addAll(devices);
    }

    @Override
    public void onSelectedDeviceStateChanged(MediaDevice device, int state) {
        notifySliceChange();
    }

    @Override
    public void onDeviceAttributesChanged() {
        notifySliceChange();
    }

    public Collection<MediaDevice> getMediaDevices() {
        return mMediaDevices;
    }

    public void connectDevice(MediaDevice device) {
        ThreadUtils.postOnBackgroundThread(() -> {
            mLocalMediaManager.connectDevice(device);
        });
    }

    public MediaDevice getMediaDeviceById(String id) {
        return mLocalMediaManager.getMediaDeviceById(new ArrayList<>(mMediaDevices), id);
    }

    public MediaDevice getCurrentConnectedMediaDevice() {
        return mLocalMediaManager.getCurrentConnectedDevice();
    }

    void setIsTouched(boolean isTouched) {
        mIsTouched = isTouched;
    }

    boolean getIsTouched() {
        return mIsTouched;
    }

    void setTopDevice(MediaDevice device) {
        mTopDevice = device;
    }

    MediaDevice getTopDevice() {
        return mTopDevice;
    }

    boolean addDeviceToPlayMedia(MediaDevice device) {
        return mLocalMediaManager.addDeviceToPlayMedia(device);
    }

    boolean removeDeviceFromPlayMedia(MediaDevice device) {
        return mLocalMediaManager.removeDeviceFromPlayMedia(device);
    }

    List<MediaDevice> getSelectableMediaDevice() {
        return mLocalMediaManager.getSelectableMediaDevice();
    }

    List<MediaDevice> getSelectedMediaDevice() {
        return mLocalMediaManager.getSelectedMediaDevice();
    }

    void adjustSessionVolume(int volume) {
        mLocalMediaManager.adjustSessionVolume(volume);
    }

    int getSessionVolumeMax() {
        return mLocalMediaManager.getSessionVolumeMax();
    }

    int getSessionVolume() {
        return mLocalMediaManager.getSessionVolume();
    }

    /**
     * Find the active MediaDevice.
     *
     * @param type the media device type.
     * @return MediaDevice list
     *
     */
    public List<MediaDevice> getActiveMediaDevice(@MediaDevice.MediaDeviceType int type) {
        return mLocalMediaManager.getActiveMediaDevice(type);
    }

    /**
     * Request to set volume.
     *
     * @param device for the targeted device.
     * @param volume for the new value.
     *
     */
    public void adjustVolume(MediaDevice device, int volume) {
        ThreadUtils.postOnBackgroundThread(() -> {
            device.requestSetVolume(volume);
        });
    }

    String getPackageName() {
        return mPackageName;
    }

    private class DevicesChangedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TextUtils.equals(AudioManager.STREAM_DEVICES_CHANGED_ACTION, action)
                    && Utils.isAudioModeOngoingCall(mContext)) {
                notifySliceChange();
            }
        }
    }
}
