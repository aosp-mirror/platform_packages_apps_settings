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

package com.android.settings.wifi.p2p;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public abstract class P2pCategoryPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    protected PreferenceGroup mCategory;

    public P2pCategoryPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mCategory.getPreferenceCount() > 0;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mCategory = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
    }

    public void removeAllChildren() {
        if (mCategory != null) {
            mCategory.removeAll();
            mCategory.setVisible(false);
        }
    }

    public void addChild(Preference child) {
        if (mCategory != null) {
            mCategory.addPreference(child);
            mCategory.setVisible(true);
        }
    }

    public void setEnabled(boolean enabled) {
        if (mCategory != null) {
            mCategory.setEnabled(enabled);
        }
    }
}
