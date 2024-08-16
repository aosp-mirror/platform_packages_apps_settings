/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.android.settings.widget.PreferenceCategoryController;

public class HidePrivateSpaceCategoryController extends PreferenceCategoryController {
    public HidePrivateSpaceCategoryController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (android.multiuser.Flags.privateSpaceSearchIllustrationConfig()) {
            boolean configValue =
                    Resources.getSystem()
                            .getBoolean(
                                    com.android.internal.R.bool
                                            .config_enableSearchTileHideIllustrationInPrivateSpace);
            return configValue ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }
}
