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

import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.core.AbstractPreferenceController;

public class TransportPreferenceController extends AbstractPreferenceController {

    interface OnTransportChangedListener {
        void onTransportChanged(String transportName);
    }

    private final OnTransportChangedListener listener;
    private final TransportHelper transportHelper;

    public TransportPreferenceController(Context context, OnTransportChangedListener listener) {
        super(context);
        this.listener = listener;
        transportHelper = new TransportHelper(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        for (Transport transport : transportHelper.getTransports()) {
            screen.addPreference(getPreferenceForTransport(transport));
        }
    }

    private Preference getPreferenceForTransport(Transport transport) {
        Preference p = new Preference(mContext);
        p.setTitle(transport.dataManagementLabel);
        p.setSummary(transport.destinationString);
        p.setIconSpaceReserved(false);
        p.setOnPreferenceClickListener(preference -> {
            transportHelper.selectTransport(transport.name);
            listener.onTransportChanged(transport.name);
            return true;
        });
        return p;
    }

    /**
     * Returns true if preference is available (should be displayed)
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Returns the key for this preference.
     */
    @Override
    public String getPreferenceKey() {
        return null;
    }
}
