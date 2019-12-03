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
import android.os.UserManager;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * A base controller for preferences that listens to location settings change and modifies location
 * settings.
 */
public abstract class LocationBasePreferenceController extends BasePreferenceController
        implements LocationEnabler.LocationModeChangeListener {

    protected UserManager mUserManager;
    protected LocationEnabler mLocationEnabler;
    protected DashboardFragment mFragment;
    protected Lifecycle mLifecycle;

    /**
     * Constructor of LocationBasePreferenceController. {@link BasePreferenceController} uses
     * reflection to create controller, all controllers extends {@link BasePreferenceController}
     * should have this function.
     */
    public LocationBasePreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
    }

    /**
     * Initialize {@link LocationEnabler} in this controller
     *
     * @param fragment The {@link DashboardFragment} uses the controller.
     */
    public void init(DashboardFragment fragment) {
        mFragment = fragment;
        mLifecycle = mFragment.getSettingsLifecycle();
        mLocationEnabler = new LocationEnabler(mContext, this /* listener */, mLifecycle);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

}
