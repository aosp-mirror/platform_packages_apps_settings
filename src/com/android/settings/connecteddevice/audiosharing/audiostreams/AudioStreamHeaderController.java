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

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.widget.LayoutPreference;

import javax.annotation.Nullable;

public class AudioStreamHeaderController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String KEY = "audio_stream_header";
    private @Nullable EntityHeaderController mHeaderController;
    private @Nullable DashboardFragment mFragment;
    private String mBroadcastName = "";
    private int mBroadcastId = -1;

    public AudioStreamHeaderController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public final void displayPreference(PreferenceScreen screen) {
        LayoutPreference headerPreference = screen.findPreference(KEY);
        if (headerPreference != null && mFragment != null) {
            mHeaderController =
                    EntityHeaderController.newInstance(
                            mFragment.getActivity(),
                            mFragment,
                            headerPreference.findViewById(R.id.entity_header));
            if (mBroadcastName != null) {
                mHeaderController.setLabel(mBroadcastName);
            }
            mHeaderController.setIcon(
                    screen.getContext().getDrawable(R.drawable.ic_bt_audio_sharing));
            // TODO(chelseahao): update this based on stream connection state
            mHeaderController.setSummary("Listening now");
            mHeaderController.done(true);
            screen.addPreference(headerPreference);
        }
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /** Initialize with {@link AudioStreamDetailsFragment} and broadcast name and id */
    void init(
            AudioStreamDetailsFragment audioStreamDetailsFragment,
            String broadcastName,
            int broadcastId) {
        mFragment = audioStreamDetailsFragment;
        mBroadcastName = broadcastName;
        mBroadcastId = broadcastId;
    }
}
