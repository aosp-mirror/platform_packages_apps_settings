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

package com.android.settings.connecteddevice.audiosharing;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsCategoryController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.SettingsMainSwitchBar;

public class AudioSharingDashboardFragment extends DashboardFragment
        implements AudioSharingSwitchBarController.OnSwitchBarChangedListener {
    private static final String TAG = "AudioSharingDashboardFrag";

    SettingsMainSwitchBar mMainSwitchBar;
    private AudioSharingSwitchBarController mSwitchBarController;
    private AudioSharingDeviceVolumeGroupController mAudioSharingDeviceVolumeGroupController;
    private CallsAndAlarmsPreferenceController mCallsAndAlarmsPreferenceController;
    private AudioSharingNamePreferenceController mAudioSharingNamePreferenceController;
    private AudioStreamsCategoryController mAudioStreamsCategoryController;

    public AudioSharingDashboardFragment() {
        super();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUDIO_SHARING_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_audio_sharing;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_audio_sharing;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAudioSharingDeviceVolumeGroupController =
                use(AudioSharingDeviceVolumeGroupController.class);
        mAudioSharingDeviceVolumeGroupController.init(this);
        mCallsAndAlarmsPreferenceController = use(CallsAndAlarmsPreferenceController.class);
        mCallsAndAlarmsPreferenceController.init(this);
        mAudioSharingNamePreferenceController = use(AudioSharingNamePreferenceController.class);
        mAudioStreamsCategoryController = use(AudioStreamsCategoryController.class);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Assume we are in a SettingsActivity. This is only safe because we currently use
        // SettingsActivity as base for all preference fragments.
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mMainSwitchBar = activity.getSwitchBar();
        mMainSwitchBar.setTitle(getText(R.string.audio_sharing_switch_title));
        mSwitchBarController = new AudioSharingSwitchBarController(activity, mMainSwitchBar, this);
        mSwitchBarController.init(this);
        getSettingsLifecycle().addObserver(mSwitchBarController);
        mMainSwitchBar.show();
    }

    @Override
    public void onSwitchBarChanged() {
        updateVisibilityForAttachedPreferences();
    }

    private void updateVisibilityForAttachedPreferences() {
        mAudioSharingDeviceVolumeGroupController.updateVisibility();
        mCallsAndAlarmsPreferenceController.updateVisibility();
        mAudioSharingNamePreferenceController.updateVisibility();
        mAudioStreamsCategoryController.updateVisibility();
    }
}
