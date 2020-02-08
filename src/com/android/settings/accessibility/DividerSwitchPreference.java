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

package com.android.settings.accessibility;

import android.content.Context;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

/**
 * A switch preference that has a divider below and above. Used for Accessibility Settings use
 * service.
 */
public final class DividerSwitchPreference extends SwitchPreference {

    private Boolean mDividerAllowedAbove;
    private Boolean mDividerAllowBelow;

    public DividerSwitchPreference(Context context) {
        super(context);
        mDividerAllowedAbove = true;
        mDividerAllowBelow = true;
    }

    /**
     * Sets divider whether to show in preference above.
     *
     * @param allowed true will be drawn on above this item
     */
    public void setDividerAllowedAbove(boolean allowed) {
        if (mDividerAllowedAbove != allowed) {
            mDividerAllowedAbove = allowed;
            notifyChanged();
        }
    }

    /**
     * Sets divider whether to show in preference below.
     *
     * @param allowed true will be drawn on below this item
     */
    public void setDividerAllowedBelow(boolean allowed) {
        if (mDividerAllowedAbove != allowed) {
            mDividerAllowBelow = allowed;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(mDividerAllowedAbove);
        holder.setDividerAllowedBelow(mDividerAllowBelow);
    }
}
