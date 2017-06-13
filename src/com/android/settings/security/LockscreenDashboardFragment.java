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

package com.android.settings.security;

import android.content.Context;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.accounts.AddUserWhenLockedPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Settings screen for lock screen preference
 */
public class LockscreenDashboardFragment extends DashboardFragment
        implements OwnerInfoPreferenceController.OwnerInfoCallback {

    private static final String TAG = "LockscreenDashboardFragment";

    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON = "security_setting_lock_screen_notif";
    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER =
            "security_setting_lock_screen_notif_work_header";
    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE =
            "security_setting_lock_screen_notif_work";

    private OwnerInfoPreferenceController mOwnerInfoPreferenceController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_LOCK_SCREEN_PREFERENCES;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_lockscreen_settings;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_lockscreen;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getLifecycle();
        final LockScreenNotificationPreferenceController notificationController =
            new LockScreenNotificationPreferenceController(context,
                    KEY_LOCK_SCREEN_NOTIFICATON,
                    KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER,
                    KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE);
        lifecycle.addObserver(notificationController);
        controllers.add(notificationController);
        final AddUserWhenLockedPreferenceController addUserWhenLockedController =
            new AddUserWhenLockedPreferenceController(context);
        lifecycle.addObserver(addUserWhenLockedController);
        controllers.add(addUserWhenLockedController);
        mOwnerInfoPreferenceController =
            new OwnerInfoPreferenceController(context, this, lifecycle);
        controllers.add(mOwnerInfoPreferenceController);
        return controllers;
    }

    @Override
    public void onOwnerInfoUpdated() {
        if (mOwnerInfoPreferenceController != null) {
            mOwnerInfoPreferenceController.updateSummary();
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.security_lockscreen_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                final List<AbstractPreferenceController> controllers = new ArrayList<>();
                controllers.add(new LockScreenNotificationPreferenceController(context));
                controllers.add(new AddUserWhenLockedPreferenceController(context));
                controllers.add(new OwnerInfoPreferenceController(
                    context, null /* fragment */, null /* lifecycle */));
                return controllers;
            }
        };
}
