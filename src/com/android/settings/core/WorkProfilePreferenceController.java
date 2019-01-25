/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.core;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.Utils;

/**
 * Abstract class to provide additional logic to deal with optional {@link Preference} entries that
 * are used only when work profile is enabled.
 *
 * <p>TODO(b/123376083): Consider merging this into {@link BasePreferenceController}.</p>
 */
public abstract class WorkProfilePreferenceController extends BasePreferenceController {
    @Nullable
    private final UserHandle mWorkProfileUser;

    /**
     * Constructor of {@link WorkProfilePreferenceController}. Called by
     * {@link BasePreferenceController#createInstance(Context, String)} through reflection.
     *
     * @param context {@link Context} to instantiate this controller.
     * @param preferenceKey Preference key to be associated with the {@link Preference}.
     */
    public WorkProfilePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mWorkProfileUser = Utils.getManagedProfile(UserManager.get(context));
    }

    /**
     * @return Non-{@code null} {@link UserHandle} when a work profile is enabled.
     *         Otherwise {@code null}.
     */
    @Nullable
    protected UserHandle getWorkProfileUser() {
        return mWorkProfileUser;
    }

    /**
     * Called back from {@link #handlePreferenceTreeClick(Preference)} to associate source metrics
     * category.
     *
     * @return One of {@link android.app.settings.SettingsEnums}.
     */
    protected abstract int getSourceMetricsCategory();

    /**
     * {@inheritDoc}
     *
     * <p>When you override this method, do not forget to check {@link #getWorkProfileUser()} to
     * see if work profile user actually exists or not.</p>
     */
    @AvailabilityStatus
    @Override
    public int getAvailabilityStatus() {
        return mWorkProfileUser != null ? AVAILABLE : DISABLED_FOR_USER;
    }

    /**
     * Launches the specified fragment for the work profile user if the associated
     * {@link Preference} is clicked.  Otherwise just forward it to the super class.
     *
     * @param preference the preference being clicked.
     * @return {@code true} if handled.
     */
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        new SubSettingLauncher(preference.getContext())
                .setDestination(preference.getFragment())
                .setSourceMetricsCategory(getSourceMetricsCategory())
                .setArguments(preference.getExtras())
                .setUserHandle(mWorkProfileUser)
                .launch();
        return true;
    }
}
