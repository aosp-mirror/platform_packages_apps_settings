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

package com.android.settings.widget;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller for generic Preference categories. If all controllers for its children reports
 * not-available, this controller will also report not-available, and subsequently will be hidden by
 * UI.
 */
public class PreferenceCategoryController extends BasePreferenceController {

    private final String mKey;
    private final List<AbstractPreferenceController> mChildren;

    public PreferenceCategoryController(Context context, String key) {
        super(context, key);
        mKey = key;
        mChildren = new ArrayList<>();
    }

    @Override
    public int getAvailabilityStatus() {
        if (mChildren == null || mChildren.isEmpty()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        // Category is available if any child is available
        for (AbstractPreferenceController controller : mChildren) {
            if (controller.isAvailable()) {
                return AVAILABLE;
            }
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    public PreferenceCategoryController setChildren(
            List<AbstractPreferenceController> childrenController) {
        mChildren.clear();
        if (childrenController != null) {
            mChildren.addAll(childrenController);
        }
        return this;
    }
}
