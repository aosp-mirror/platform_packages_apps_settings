/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datausage;

import android.content.Context;
import android.net.NetworkTemplate;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

public class TemplatePreferenceCategory extends PreferenceCategory implements TemplatePreference {

    private NetworkTemplate mTemplate;
    private int mSubId;

    public TemplatePreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId) {
        mTemplate = template;
        mSubId = subId;
    }

    @Override
    public boolean addPreference(Preference preference) {
        if (!(preference instanceof TemplatePreference)) {
            throw new IllegalArgumentException(
                    "TemplatePreferenceCategories can only hold TemplatePreferences");
        }
        return super.addPreference(preference);
    }

    /** Pushes the templates. */
    public void pushTemplates() {
        if (mTemplate == null) {
            throw new RuntimeException("null mTemplate for " + getKey());
        }
        for (int i = 0; i < getPreferenceCount(); i++) {
            ((TemplatePreference) getPreference(i)).setTemplate(mTemplate, mSubId);
        }
    }

}
