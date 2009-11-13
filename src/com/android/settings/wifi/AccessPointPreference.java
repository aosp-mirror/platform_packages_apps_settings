/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.wifi;

import com.android.settings.R;

import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.view.View;
import android.widget.ImageView;

public class AccessPointPreference extends Preference implements
        AccessPointState.AccessPointStateCallback {
    
    // UI states
    private static final int[] STATE_ENCRYPTED = { R.attr.state_encrypted };
    private static final int[] STATE_EMPTY = { };
    
    // Signal strength indicator
    private static final int UI_SIGNAL_LEVELS = 4;

    private AccessPointState mState;

    public AccessPointPreference(WifiSettings wifiSettings, AccessPointState state) {
        super(wifiSettings, null);
        
        mState = state;
        
        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);
        
        state.setCallback(this);
        
        refresh();
    }
    
    public void refresh() {
        setTitle(mState.getHumanReadableSsid());
        setSummary(mState.getSummarizedStatus());

        notifyChanged();
    }
    
    public void refreshAccessPointState() {
        refresh();
        
        // The ordering of access points could have changed due to the state change, so
        // re-evaluate ordering
        notifyHierarchyChanged();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        ImageView signal = (ImageView) view.findViewById(R.id.signal);
        if (mState.seen) {
            signal.setImageResource(R.drawable.wifi_signal);
            signal.setImageState(mState.hasSecurity() ? STATE_ENCRYPTED : STATE_EMPTY, true);
            signal.setImageLevel(getUiSignalLevel());
        } else {
            signal.setImageDrawable(null);
        }
    }

    private int getUiSignalLevel() {
        return mState != null ? WifiManager.calculateSignalLevel(mState.signal, UI_SIGNAL_LEVELS)
                : 0; 
    }

    /**
     * Returns the {@link AccessPointState} associated with this preference.
     * @return The {@link AccessPointState}.
     */
    public AccessPointState getAccessPointState() {
        return mState;
    }
    
    @Override
    public int compareTo(Preference another) {
        if (!(another instanceof AccessPointPreference)) {
            // Let normal preferences go before us.
            // NOTE: we should only be compared to Preference in our
            //       category.
            return 1;
        }
        
        return mState.compareTo(((AccessPointPreference) another).mState);
    }
    
}

