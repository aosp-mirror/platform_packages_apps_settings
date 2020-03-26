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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController;
import com.android.settings.display.AmbientDisplayNotificationsPreferenceController;
import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.screenlock.LockScreenPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for lock screen preference
 */
@SearchIndexable
public class LockscreenDashboardFragment extends DashboardFragment
        implements OwnerInfoPreferenceController.OwnerInfoCallback {

    public static final String KEY_AMBIENT_DISPLAY_ALWAYS_ON = "ambient_display_always_on";

    private static final String TAG = "LockscreenDashboardFragment";

    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON = "security_setting_lock_screen_notif";
    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER =
            "security_setting_lock_screen_notif_work_header";
    @VisibleForTesting
    static final String KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE =
            "security_setting_lock_screen_notif_work";
    @VisibleForTesting
    static final String KEY_ADD_USER_FROM_LOCK_SCREEN =
            "security_lockscreen_add_users_when_locked";


    private AmbientDisplayConfiguration mConfig;
    private OwnerInfoPreferenceController mOwnerInfoPreferenceController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_LOCK_SCREEN_PREFERENCES;
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
    public int getHelpResource() {
        return R.string.help_url_lockscreen;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(AmbientDisplayAlwaysOnPreferenceController.class)
                .setConfig(getConfig(context))
                .setCallback(this::updatePreferenceStates);
        use(AmbientDisplayNotificationsPreferenceController.class).setConfig(getConfig(context));
        use(DoubleTapScreenPreferenceController.class).setConfig(getConfig(context));
        use(PickupGesturePreferenceController.class).setConfig(getConfig(context));
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getSettingsLifecycle();
        final LockScreenNotificationPreferenceController notificationController =
                new LockScreenNotificationPreferenceController(context,
                        KEY_LOCK_SCREEN_NOTIFICATON,
                        KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER,
                        KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE);
        lifecycle.addObserver(notificationController);
        controllers.add(notificationController);
        mOwnerInfoPreferenceController = new OwnerInfoPreferenceController(context, this);
        controllers.add(mOwnerInfoPreferenceController);

        return controllers;
    }

    @Override
    public void onOwnerInfoUpdated() {
        if (mOwnerInfoPreferenceController != null) {
            mOwnerInfoPreferenceController.updateSummary();
        }
    }

    private AmbientDisplayConfiguration getConfig(Context context) {
        if (mConfig == null) {
            mConfig = new AmbientDisplayConfiguration(context);
        }
        return mConfig;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.security_lockscreen_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    final List<AbstractPreferenceController> controllers = new ArrayList<>();
                    controllers.add(new LockScreenNotificationPreferenceController(context));
                    controllers.add(new OwnerInfoPreferenceController(
                            context, null /* fragment */));
                    return controllers;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> niks = super.getNonIndexableKeys(context);
                    niks.add(KEY_ADD_USER_FROM_LOCK_SCREEN);
                    return niks;
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return new LockScreenPreferenceController(context, "anykey")
                            .isAvailable();
                }
            };
}
