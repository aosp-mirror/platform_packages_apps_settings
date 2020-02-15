/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.Intent;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputSliceConstants;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A group preference controller to add/remove/update preference
 * {@link com.android.settings.notification.RemoteVolumeSeekBarPreference}
 **/
public class RemoteVolumeGroupController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, LocalMediaManager.DeviceCallback {

    private static final String KEY_REMOTE_VOLUME_GROUP = "remote_media_group";
    private static final String TAG = "RemoteVolumePrefCtr";
    @VisibleForTesting
    static final String SWITCHER_PREFIX = "OUTPUT_SWITCHER";

    private PreferenceCategory mPreferenceCategory;
    private List<MediaDevice> mActiveRemoteMediaDevices = new ArrayList<>();

    @VisibleForTesting
    LocalMediaManager mLocalMediaManager;

    public RemoteVolumeGroupController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        if (mLocalMediaManager == null) {
            mLocalMediaManager = new LocalMediaManager(mContext, null, null);
            mLocalMediaManager.registerCallback(this);
            mLocalMediaManager.startScan();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (mActiveRemoteMediaDevices.isEmpty()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mActiveRemoteMediaDevices.clear();
        mActiveRemoteMediaDevices.addAll(mLocalMediaManager.getActiveMediaDevice(
                MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE));
        refreshPreference();
    }

    /**
     * onDestroy()
     * {@link androidx.lifecycle.OnLifecycleEvent}
     **/
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        mLocalMediaManager.unregisterCallback(this);
        mLocalMediaManager.stopScan();
    }

    private void refreshPreference() {
        mPreferenceCategory.removeAll();
        if (!isAvailable()) {
            mPreferenceCategory.setVisible(false);
            return;
        }
        final CharSequence outputTitle = mContext.getText(R.string.media_output_title);
        final CharSequence castVolume = mContext.getText(R.string.remote_media_volume_option_title);
        mPreferenceCategory.setVisible(true);
        int i = 0;
        for (MediaDevice device : mActiveRemoteMediaDevices) {
            if (mPreferenceCategory.findPreference(device.getId()) != null) {
                continue;
            }
            // Add slider
            final RemoteVolumeSeekBarPreference seekBarPreference =
                    new RemoteVolumeSeekBarPreference(mContext);
            seekBarPreference.setKey(device.getId());
            seekBarPreference.setTitle(castVolume + " (" + device.getClientAppLabel() + ")");
            seekBarPreference.setMax(device.getMaxVolume());
            seekBarPreference.setProgress(device.getCurrentVolume());
            seekBarPreference.setMin(0);
            seekBarPreference.setOnPreferenceChangeListener(this);
            seekBarPreference.setIcon(R.drawable.ic_volume_remote);
            mPreferenceCategory.addPreference(seekBarPreference);
            // Add output indicator
            final Preference preference = new Preference(mContext);
            preference.setKey(SWITCHER_PREFIX + device.getId());
            preference.setTitle(outputTitle);
            preference.setSummary(device.getName());
            mPreferenceCategory.addPreference(preference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final MediaDevice device = mLocalMediaManager.getMediaDeviceById(preference.getKey());
        if (device == null) {
            Log.e(TAG, "Unable to find " + preference.getKey() + " to set volume");
            return false;
        }
        ThreadUtils.postOnBackgroundThread(() -> {
            device.requestSetVolume((int) newValue);
        });
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!preference.getKey().startsWith(SWITCHER_PREFIX)) {
            return false;
        }
        final String key = preference.getKey().substring(SWITCHER_PREFIX.length());
        final MediaDevice device = mLocalMediaManager.getMediaDeviceById(key);
        if (device == null) {
            return false;
        }
        final Intent intent = new Intent()
                .setAction(MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MediaOutputSliceConstants.EXTRA_PACKAGE_NAME,
                        device.getClientPackageName());
        mContext.startActivity(intent);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_REMOTE_VOLUME_GROUP;
    }

    @Override
    public void onDeviceListUpdate(List<MediaDevice> devices) {
        if (mPreferenceCategory == null) {
            // Preference group is not ready.
            return;
        }
        mActiveRemoteMediaDevices.clear();
        mActiveRemoteMediaDevices.addAll(mLocalMediaManager.getActiveMediaDevice(
                MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE));
        refreshPreference();
    }

    @Override
    public void onSelectedDeviceStateChanged(MediaDevice device, int state) {

    }
}
