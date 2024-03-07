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
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class AudioStreamDetailsFragment extends DashboardFragment {
    static final String BROADCAST_NAME_ARG = "broadcast_name";
    static final String BROADCAST_ID_ARG = "broadcast_id";
    private static final String TAG = "AudioStreamDetailsFragment";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        if (arguments != null) {
            use(AudioStreamHeaderController.class)
                    .init(
                            this,
                            arguments.getString(BROADCAST_NAME_ARG),
                            arguments.getInt(BROADCAST_ID_ARG));
            use(AudioStreamButtonController.class).init(arguments.getInt(BROADCAST_ID_ARG));
        }
    }

    @Override
    public int getMetricsCategory() {
        // TODO(chelseahao): update metrics id
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.audio_stream_details_fragment;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
