
/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.core.DynamicAvailabilityPreferenceController;
import com.android.settings.core.PreferenceAvailabilityObserver;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A controller that hides a {@link android.support.v7.preference.PreferenceGroup} when none of the
 * {@link Preference}s inside it are visible.
 *
 * TODO(b/62051162): Use {@link android.support.v7.preference.PreferenceGroup}'s native ability to
 * hide itself when all {@link Preference}s inside it are invisible when that functionality becomes
 * available. This custom controller will still be needed to remove the
 * {@link android.support.v7.preference.PreferenceGroup} from the search index as required (by
 * having {@link #isAvailable()} return {@code false} if the method returns {@code false} for all
 * {@link Preference}s in the {@link android.support.v7.preference.PreferenceGroup}).
 */
public class ExposureChangesCategoryPreferenceController
        extends DynamicAvailabilityPreferenceController implements PreferenceAvailabilityObserver {

    private static final String KEY_EXPOSURE_CHANGES_CATEGORY = "exposure_changes_category";
    private final Set<String> mAvailablePrefs = new HashSet<String>();
    private Preference mPreference = null;
    private boolean mControllingUi;

    /**
     * When {@code controllingUi} is {@code true}, some of the preferences may have their visibility
     * determined asynchronously. In this case, {@link #isAvailable()} must always return {@code
     * true} and the group should be hidden using {@link Preference#setVisible()} if all preferences
     * report that they are invisible.
     * When {@code controllingUi} is {@code false}, we are running on the search indexer thread and
     * visibility must be determined synchronously. {@link #isAvailable()} can rely on all
     * preferences having their visibility determined already and should return whether the group is
     * visible or not.
     */
    public ExposureChangesCategoryPreferenceController(Context context, Lifecycle lifecycle,
            List<DynamicAvailabilityPreferenceController> controllers, boolean controllingUi) {
        super(context, lifecycle);
        mControllingUi = controllingUi;
        for (final DynamicAvailabilityPreferenceController controller : controllers) {
            controller.setAvailabilityObserver(this);
        }
    }

    @Override
    public void onPreferenceAvailabilityUpdated(String key, boolean available) {
        if (available) {
            mAvailablePrefs.add(key);
        } else {
            mAvailablePrefs.remove(key);
        }
        available = haveAnyVisiblePreferences();
        if (mControllingUi) {
            notifyOnAvailabilityUpdate(available);
        }
        if (mPreference != null) {
            mPreference.setVisible(available);
        }
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = preference;
        mPreference.setVisible(haveAnyVisiblePreferences());
    }

    @Override
    public boolean isAvailable() {
        if (mControllingUi) {
            // When running on the main UI thread, some preferences determine their visibility
            // asynchronously. Always return true here and determine the pref group's actual
            // visibility as the other preferences report their visibility asynchronously via
            // onPreferenceAvailabilityUpdated().
            return true;
        }
        final boolean available = haveAnyVisiblePreferences();
        notifyOnAvailabilityUpdate(available);
        return available;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_EXPOSURE_CHANGES_CATEGORY;
    }

    private boolean haveAnyVisiblePreferences() {
        return mAvailablePrefs.size() > 0;
    }
}
