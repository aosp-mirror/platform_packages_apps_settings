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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppInfoDashboardFragment;
import com.android.settings.core.BasePreferenceController;

/*
 * Abstract base controller for the app detail preferences that refresh the state when the app state
 * changes and launch a specific detail fragment when the preference is clicked.
 */
public abstract class AppInfoPreferenceControllerBase extends BasePreferenceController
        implements AppInfoDashboardFragment.Callback {

    protected final AppInfoDashboardFragment mParent;
    private final Class<? extends SettingsPreferenceFragment> mDetailFragmenClass;

    protected Preference mPreference;

    public AppInfoPreferenceControllerBase(Context context, AppInfoDashboardFragment parent,
            String preferenceKey) {
        super(context, preferenceKey);
        mParent = parent;
        mDetailFragmenClass = getDetailFragmentClass();
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
        if (TextUtils.equals(preference.getKey(), mPreferenceKey) && mDetailFragmenClass != null) {
            AppInfoDashboardFragment.startAppInfoFragment(
                    mDetailFragmenClass, -1, mParent, mParent.getAppEntry());
            return true;
        }
        return false;
    }

    @Override
    public void refreshUi() {
        updateState(mPreference);
    }

    /**
     * Gets the fragment class to be launched when the preference is clicked.
     * @return the fragment to launch
     */
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return null;
    }

}
