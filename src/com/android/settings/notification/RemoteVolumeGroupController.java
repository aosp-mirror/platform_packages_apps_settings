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
import android.media.MediaRouter2Manager;
import android.media.RoutingSessionInfo;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputConstants;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A group preference controller to add/remove/update preference
 * {@link com.android.settings.notification.RemoteVolumeSeekBarPreference}
 **/
public class RemoteVolumeGroupController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnDestroy,
        LocalMediaManager.DeviceCallback {

    private static final String KEY_REMOTE_VOLUME_GROUP = "remote_media_group";
    private static final String TAG = "RemoteVolumePrefCtr";
    @VisibleForTesting
    static final String SWITCHER_PREFIX = "OUTPUT_SWITCHER";

    private PreferenceCategory mPreferenceCategory;
    private final List<RoutingSessionInfo> mRoutingSessionInfos = new ArrayList<>();

    @VisibleForTesting
    LocalMediaManager mLocalMediaManager;
    @VisibleForTesting
    MediaRouter2Manager mRouterManager;

    public RemoteVolumeGroupController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        if (mLocalMediaManager == null) {
            mLocalMediaManager = new LocalMediaManager(mContext, null, null);
            mLocalMediaManager.registerCallback(this);
            mLocalMediaManager.startScan();
        }
        mRouterManager = MediaRouter2Manager.getInstance(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mRoutingSessionInfos.isEmpty()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        initRemoteMediaSession();
        refreshPreference();
    }

    private void initRemoteMediaSession() {
        mRoutingSessionInfos.clear();
        mRoutingSessionInfos.addAll(mLocalMediaManager.getRemoteRoutingSessions());
    }

    @Override
    public void onDestroy() {
        mLocalMediaManager.unregisterCallback(this);
        mLocalMediaManager.stopScan();
    }

    private synchronized void refreshPreference() {
        if (!isAvailable()) {
            mPreferenceCategory.setVisible(false);
            return;
        }
        final CharSequence castVolume = mContext.getText(R.string.remote_media_volume_option_title);
        mPreferenceCategory.setVisible(true);
        for (RoutingSessionInfo info : mRoutingSessionInfos) {
            final CharSequence appName = Utils.getApplicationLabel(mContext,
                    info.getClientPackageName());
            RemoteVolumeSeekBarPreference seekBarPreference = mPreferenceCategory.findPreference(
                    info.getId());
            if (seekBarPreference != null) {
                // Update slider
                if (seekBarPreference.getProgress() != info.getVolume()) {
                    seekBarPreference.setProgress(info.getVolume());
                }
                seekBarPreference.setEnabled(mLocalMediaManager.shouldEnableVolumeSeekBar(info));
            } else {
                // Add slider
                seekBarPreference = new RemoteVolumeSeekBarPreference(mContext);
                seekBarPreference.setKey(info.getId());
                seekBarPreference.setTitle(castVolume);
                seekBarPreference.setMax(info.getVolumeMax());
                seekBarPreference.setProgress(info.getVolume());
                seekBarPreference.setMin(0);
                seekBarPreference.setOnPreferenceChangeListener(this);
                seekBarPreference.setIcon(com.android.settingslib.R.drawable.ic_volume_remote);
                seekBarPreference.setEnabled(mLocalMediaManager.shouldEnableVolumeSeekBar(info));
                mPreferenceCategory.addPreference(seekBarPreference);
            }

            Preference switcherPreference = mPreferenceCategory.findPreference(
                    SWITCHER_PREFIX + info.getId());

            // TODO: b/291277292 - Remove references to MediaRouter2Manager and implement long-term
            //  solution in SettingsLib.
            final boolean isMediaOutputDisabled =
                    mRouterManager.getTransferableRoutes(info.getClientPackageName()).isEmpty();
            final CharSequence outputTitle = mContext.getString(R.string.media_output_label_title,
                    appName);
            if (switcherPreference != null) {
                // Update output indicator
                switcherPreference.setTitle(isMediaOutputDisabled ? appName : outputTitle);
                switcherPreference.setSummary(info.getName());
                switcherPreference.setEnabled(!isMediaOutputDisabled);
            } else {
                // Add output indicator
                switcherPreference = new Preference(mContext);
                switcherPreference.setKey(SWITCHER_PREFIX + info.getId());
                switcherPreference.setTitle(isMediaOutputDisabled ? appName : outputTitle);
                switcherPreference.setSummary(info.getName());
                switcherPreference.setEnabled(!isMediaOutputDisabled);
                mPreferenceCategory.addPreference(switcherPreference);
            }
        }

        // Check and remove non-active session preference
        // There is a pair of preferences for each session. First one is a seekBar preference.
        // The second one shows the session information and provide an entry-point to launch output
        // switcher. It is unnecessary to go through all preferences. It is fine ignore the second
        // preference and only to check the seekBar's key value.
        for (int i = 0; i < mPreferenceCategory.getPreferenceCount(); i = i + 2) {
            final Preference preference = mPreferenceCategory.getPreference(i);
            boolean isActive = false;
            for (RoutingSessionInfo info : mRoutingSessionInfos) {
                if (TextUtils.equals(preference.getKey(), info.getId())) {
                    isActive = true;
                    break;
                }
            }
            if (isActive) {
                continue;
            }
            final Preference switcherPreference = mPreferenceCategory.getPreference(i + 1);
            if (switcherPreference != null) {
                mPreferenceCategory.removePreference(preference);
                mPreferenceCategory.removePreference(switcherPreference);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ThreadUtils.postOnBackgroundThread(() -> {
            mLocalMediaManager.adjustSessionVolume(preference.getKey(), (int) newValue);
        });
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!preference.getKey().startsWith(SWITCHER_PREFIX)) {
            return false;
        }
        for (RoutingSessionInfo info : mRoutingSessionInfos) {
            if (TextUtils.equals(info.getId(),
                    preference.getKey().substring(SWITCHER_PREFIX.length()))) {
                final Intent intent = new Intent()
                        .setAction(MediaOutputConstants.ACTION_LAUNCH_MEDIA_OUTPUT_DIALOG)
                        .setPackage(MediaOutputConstants.SYSTEMUI_PACKAGE_NAME)
                        .putExtra(MediaOutputConstants.EXTRA_PACKAGE_NAME,
                                info.getClientPackageName());
                mContext.sendBroadcast(intent);
                return true;
            }
        }
        return false;
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
        ThreadUtils.postOnMainThread(() -> {
            initRemoteMediaSession();
            refreshPreference();
        });
    }

    @Override
    public void onSelectedDeviceStateChanged(MediaDevice device, int state) {
    }
}
