/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.flags.Flags;

public class DataSaverPreference extends Preference implements DataSaverBackend.Listener {

    private final @Nullable DataSaverBackend mDataSaverBackend;

    public DataSaverPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDataSaverBackend = isCatalystEnabled() ? null : new DataSaverBackend(context);
    }

    private boolean isCatalystEnabled() {
        return Flags.catalyst() && Flags.catalystRestrictBackgroundParentEntry();
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (mDataSaverBackend != null) {
            mDataSaverBackend.addListener(this);
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (mDataSaverBackend != null) {
            mDataSaverBackend.remListener(this);
        }
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        setSummary(isDataSaving ? R.string.data_saver_on : R.string.data_saver_off);
    }

    @Override
    public void onAllowlistStatusChanged(int uid, boolean isAllowlisted) {
    }

    @Override
    public void onDenylistStatusChanged(int uid, boolean isDenylisted) {
    }
}
