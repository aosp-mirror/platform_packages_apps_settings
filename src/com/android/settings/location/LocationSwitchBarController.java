/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.location;

import android.content.Context;
import android.os.UserHandle;
import android.widget.Switch;

import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class LocationSwitchBarController implements SwitchBar.OnSwitchChangeListener,
        LocationEnabler.LocationModeChangeListener, LifecycleObserver, OnStart, OnStop {

    private final SwitchBar mSwitchBar;
    private final Switch mSwitch;
    private final LocationEnabler mLocationEnabler;
    private boolean mValidListener;

    public LocationSwitchBarController(Context context, SwitchBar switchBar,
            Lifecycle lifecycle) {
        mSwitchBar = switchBar;
        mSwitch = mSwitchBar.getSwitch();
        mLocationEnabler = new LocationEnabler(context, this /* listener */, lifecycle);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onStart() {
        if (!mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mValidListener = true;
        }
    }

    @Override
    public void onStop() {
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mValidListener = false;
        }
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        // Restricted user can't change the location mode, so disable the master switch. But in some
        // corner cases, the location might still be enabled. In such case the master switch should
        // be disabled but checked.
        final boolean enabled = mLocationEnabler.isEnabled(mode);
        final int userId = UserHandle.myUserId();
        final RestrictedLockUtils.EnforcedAdmin admin =
                mLocationEnabler.getShareLocationEnforcedAdmin(userId);
        final boolean hasBaseUserRestriction =
                mLocationEnabler.hasShareLocationRestriction(userId);
        // Disable the whole switch bar instead of the switch itself. If we disabled the switch
        // only, it would be re-enabled again if the switch bar is not disabled.
        if (!hasBaseUserRestriction && admin != null) {
            mSwitchBar.setDisabledByAdmin(admin);
        } else {
            mSwitchBar.setEnabled(!restricted);
        }

        if (enabled != mSwitch.isChecked()) {
            // set listener to null so that that code below doesn't trigger onCheckedChanged()
            if (mValidListener) {
                mSwitchBar.removeOnSwitchChangeListener(this);
            }
            mSwitch.setChecked(enabled);
            if (mValidListener) {
                mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
    }

    /**
     * Listens to the state change of the location master switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mLocationEnabler.setLocationEnabled(isChecked);
    }
}
