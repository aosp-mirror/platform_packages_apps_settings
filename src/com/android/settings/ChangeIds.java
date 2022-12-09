/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings;

import android.compat.annotation.ChangeId;
import android.compat.annotation.LoggingOnly;

/**
 * All the {@link ChangeId} used for Settings App.
 */
public class ChangeIds {
    /**
     * Intents with action {@code android.settings.MANAGE_APP_OVERLAY_PERMISSION}
     * and data URI scheme {@code package} don't go to the app-specific screen for managing the
     * permission anymore. Instead, they redirect to this screen for managing all the apps that have
     * requested such permission.
     */
    @ChangeId
    @LoggingOnly
    public static final long CHANGE_RESTRICT_SAW_INTENT = 135920175L;
}
