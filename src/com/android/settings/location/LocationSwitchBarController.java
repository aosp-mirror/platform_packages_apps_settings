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
import android.os.UserManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * The switch controller for the location.
 */
public class LocationSwitchBarController implements OnCheckedChangeListener,
        LocationEnabler.LocationModeChangeListener, LifecycleObserver, OnStart, OnStop {

    private final SettingsMainSwitchBar mSwitchBar;
    private final LocationEnabler mLocationEnabler;
    private boolean mValidListener;

    public LocationSwitchBarController(Context context, SettingsMainSwitchBar switchBar,
            Lifecycle lifecycle) {
        mSwitchBar = switchBar;
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
        // Restricted user can't change the location mode, so disable the primary switch. But in
        // some corner cases, the location might still be enabled. In such case the primary switch
        // should be disabled but checked.
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
        } else if (restricted) {
            RestrictedLockUtils.EnforcedAdmin enforcedAdmin = RestrictedLockUtils.EnforcedAdmin
                    .createDefaultEnforcedAdminWithRestriction(UserManager.DISALLOW_SHARE_LOCATION);
            mSwitchBar.setDisabledByAdmin(enforcedAdmin);
        } else {
            mSwitchBar.setEnabled(true);
        }

        if (enabled != mSwitchBar.isChecked()) {
            // set listener to null so that that code below doesn't trigger onCheckedChanged()
            if (mValidListener) {
                mSwitchBar.removeOnSwitchChangeListener(this);
            }
            mSwitchBar.setChecked(enabled);
            if (mValidListener) {
                mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
    }

    /**
     * Listens to the state change of the location primary switch.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mLocationEnabler.setLocationEnabled(isChecked);
    }
}
