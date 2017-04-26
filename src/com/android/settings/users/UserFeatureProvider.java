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
 * limitations under the License
 */

package com.android.settings.users;

import android.os.UserHandle;

import java.util.List;

public interface UserFeatureProvider {
    /**
     * Returns a list of UserHandles for profiles associated with the user that the calling process
     * is running on, including the user itself.
     *
     * @return A non-empty list of UserHandles associated with the calling user.
     */
    List<UserHandle> getUserProfiles();
}
