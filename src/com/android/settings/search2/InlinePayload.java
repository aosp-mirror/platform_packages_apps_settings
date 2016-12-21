/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.search2;

/**
 * Abstract Payload for inline settings results.
 */
public abstract class InlinePayload extends ResultPayload {
    /**
     * Defines the URI to access and store the Setting the inline result represents
     */
    public String settingsUri;

    /**
     * The UI type for the inline result.
     */
    @PayloadType public int inlineType;

    /**
     * Defines where the Setting is stored.
     */
    @SettingsSource public int settingSource;

    public InlinePayload(String uri, @PayloadType int type, @SettingsSource int source) {
        settingsUri = uri;
        inlineType = type;
        settingSource = source;
    }
}
