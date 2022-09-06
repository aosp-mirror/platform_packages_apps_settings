/*
 * Copyright (C) 2020 The Calyx Institute
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
 * limitations under the License
 */

package com.android.settings.backup.transport;

import android.app.settings.SettingsEnums;
import android.content.Context;
import com.android.settings.R;
import com.android.settings.backup.transport.TransportPreferenceController.OnTransportChangedListener;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class TransportFragment extends DashboardFragment implements OnTransportChangedListener {

    private static final String TAG = "TransportFragment";

    /**
     * Get the tag string for logging.
     */
    @Override
    protected String getLogTag() {
        return TAG;
    }

    /**
     * Get the res id for static preference xml for this fragment.
     */
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.backup_transport_settings;
    }

    /**
     * Get a list of {@link AbstractPreferenceController} for this fragment.
     */
    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new TransportPreferenceController(context, this));
        return controllers;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BACKUP_SETTINGS;
    }

    @Override
    public void onTransportChanged(String transportName) {
        requireActivity().finish();
    }

}
