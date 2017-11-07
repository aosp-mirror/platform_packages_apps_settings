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

package com.android.settings.enterprise;

import android.Manifest;
import android.content.Context;

public class AdminGrantedLocationPermissionsPreferenceController extends
        AdminGrantedPermissionsPreferenceControllerBase {

    private static final String KEY_ENTERPRISE_PRIVACY_NUMBER_LOCATION_ACCESS_PACKAGES
            = "enterprise_privacy_number_location_access_packages";

    public AdminGrantedLocationPermissionsPreferenceController(Context context, boolean async) {
        super(context, async, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION});
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ENTERPRISE_PRIVACY_NUMBER_LOCATION_ACCESS_PACKAGES;
    }
}
