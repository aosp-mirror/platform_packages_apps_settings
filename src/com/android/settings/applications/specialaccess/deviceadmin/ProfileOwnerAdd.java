/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.deviceadmin;

import android.os.Bundle;

/**
 * ProfileOwnerAdd uses the DeviceAdminAdd logic to handle SET_PROFILE_OWNER intents
 *
 * TODO(b/131713071): Move profile owner add logic from DeviceAdminAdd to here
 */
public class ProfileOwnerAdd extends DeviceAdminAdd {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }
}
