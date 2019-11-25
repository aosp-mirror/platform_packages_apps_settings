/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

/**
 * Interface for storing Activity request codes in development options
 */
public interface DevelopmentOptionsActivityRequestCodes {
    int REQUEST_CODE_ENABLE_OEM_UNLOCK = 0;

    int REQUEST_CODE_DEBUG_APP = 1;

    int REQUEST_MOCK_LOCATION_APP = 2;

    int REQUEST_CODE_ANGLE_ALL_USE_ANGLE = 3;

    int REQUEST_CODE_ANGLE_DRIVER_PKGS = 4;

    int REQUEST_CODE_ANGLE_DRIVER_VALUES = 5;

    int REQUEST_COMPAT_CHANGE_APP = 6;
}
