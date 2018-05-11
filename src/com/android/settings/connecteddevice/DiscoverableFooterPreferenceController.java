/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.connecteddevice;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixin;

/**
 * Controller that shows and updates the bluetooth device name
 */
public class DiscoverableFooterPreferenceController extends BasePreferenceController {
    private static final String KEY = "discoverable_footer_preference";

    private FooterPreference mPreference;
    private FooterPreferenceMixin mFooterPreferenceMixin;


    public DiscoverableFooterPreferenceController(Context context) { super(context, KEY); }

    public void init(DashboardFragment fragment) {
        mFooterPreferenceMixin = new FooterPreferenceMixin(fragment, fragment.getLifecycle());
    }

    @VisibleForTesting
    void init(FooterPreferenceMixin footerPreferenceMixin, FooterPreference preference) {
        mFooterPreferenceMixin = footerPreferenceMixin;
        mPreference = preference;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        addFooterPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    private void addFooterPreference(PreferenceScreen screen) {
        mPreference = mFooterPreferenceMixin.createFooterPreference();
        mPreference.setKey(KEY);
        screen.addPreference(mPreference);
    }
}