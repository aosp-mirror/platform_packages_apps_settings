/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.enterprise;

import android.provider.SearchIndexableResource;

import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

/** Interface for configuring what is displayed on the Privacy Settings. */
public interface PrivacySettingsPreference {

    /**
     * Returns the XML Res Id that is used in the Privacy Settings screen.
     */
    int getPreferenceScreenResId();

    /**
     * Returns the XML resources to index.
     */
    List<SearchIndexableResource> getXmlResourcesToIndex();

    /**
     * Returns the preference controllers used to populate the privacy preferences.
     */
    List<AbstractPreferenceController> createPreferenceControllers(boolean async);
}
