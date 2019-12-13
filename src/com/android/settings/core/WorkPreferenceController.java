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
 * limitations under the License.
 */

package com.android.settings.core;

import android.content.Context;

import androidx.annotation.CallSuper;

/**
 * Base class to be used directly in Xml with settings:forWork="true" attribute.
 * It is used specifically for work profile only preference
 */
public class WorkPreferenceController extends BasePreferenceController {

    public WorkPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Only available when work profile user is existed
     */
    @CallSuper
    public int getAvailabilityStatus() {
        return getWorkProfileUser() != null ? AVAILABLE : DISABLED_FOR_USER;
    }
}
