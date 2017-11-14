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

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * A base controller for preferences that listens to location settings change and modifies location
 * settings.
 */
public abstract class LocationBasePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LocationEnabler.LocationModeChangeListener {

    protected final UserManager mUserManager;
    protected final LocationEnabler mLocationEnabler;

    public LocationBasePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLocationEnabler = new LocationEnabler(context, this /* listener */, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

}
