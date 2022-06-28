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

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Collections;
import java.util.List;

/** Privacy Settings preferences for a financed device. */
public class PrivacySettingsFinancedPreference implements PrivacySettingsPreference {
    private static final String KEY_EXPOSURE_CHANGES_CATEGORY = "exposure_changes_category";

    private final Context mContext;

    public PrivacySettingsFinancedPreference(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Returns the XML Res Id that is used for financed devices in the Privacy Settings screen.
     */
    @Override
    public int getPreferenceScreenResId() {
        return R.xml.financed_privacy_settings;
    }

    /**
     * Returns the XML resources to index for a financed device.
     */
    @Override
    public List<SearchIndexableResource> getXmlResourcesToIndex() {
        final SearchIndexableResource sir = new SearchIndexableResource(mContext);
        sir.xmlResId = getPreferenceScreenResId();
        return Collections.singletonList(sir);
    }

    /**
     * Returns the preference controllers used to populate the privacy preferences in the Privacy
     * Settings screen for a financed device.
     */
    @Override
    public List<AbstractPreferenceController> createPreferenceControllers(boolean async) {
        return Collections.emptyList();
    }
}
