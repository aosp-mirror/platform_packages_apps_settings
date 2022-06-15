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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.BasePreferenceController;

/*
 * Abstract base controller for the app detail preferences that refresh the state when the app state
 * changes and launch a specific detail fragment when the preference is clicked.
 */
public abstract class AppInfoPreferenceControllerBase extends BasePreferenceController
        implements AppInfoDashboardFragment.Callback {

    protected AppInfoDashboardFragment mParent;
    protected Preference mPreference;

    private final Class<? extends SettingsPreferenceFragment> mDetailFragmentClass;

    public AppInfoPreferenceControllerBase(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mDetailFragmentClass = getDetailFragmentClass();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), mPreferenceKey) && mDetailFragmentClass != null) {
            AppInfoDashboardFragment.startAppInfoFragment(
                    mDetailFragmentClass, -1, getArguments(), mParent, mParent.getAppEntry());
            return true;
        }
        return false;
    }

    @Override
    public void refreshUi() {
        updateState(mPreference);
    }

    public void setParentFragment(AppInfoDashboardFragment parent) {
        mParent = parent;
        parent.addToCallbackList(this);
    }

    /**
     * Gets the fragment class to be launched when the preference is clicked.
     * @return the fragment to launch
     */
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return null;
    }

    /**
     * Gets any extras that should be passed to the fragment class when the preference is clicked.
     * @return a bundle of extras to include in the launch intent
     */
    protected Bundle getArguments() {
        return null;
    }

}
