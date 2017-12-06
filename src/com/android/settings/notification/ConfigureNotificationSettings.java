/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.applications.NotificationApps;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.gestures.SwipeToNotificationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigureNotificationSettings extends DashboardFragment {
    private static final String TAG = "ConfigNotiSettings";

    @VisibleForTesting
    static final String KEY_LOCKSCREEN = "lock_screen_notifications";
    @VisibleForTesting
    static final String KEY_LOCKSCREEN_WORK_PROFILE_HEADER =
            "lock_screen_notifications_profile_header";
    @VisibleForTesting
    static final String KEY_LOCKSCREEN_WORK_PROFILE = "lock_screen_notifications_profile";
    @VisibleForTesting
    static final String KEY_SWIPE_DOWN = "gesture_swipe_down_fingerprint_notifications";

    private static final String KEY_NOTI_DEFAULT_RINGTONE = "notification_default_ringtone";

    private RingtonePreference mRequestPreference;
    private static final int REQUEST_CODE = 200;
    private static final String SELECTED_PREFERENCE_KEY = "selected_preference";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CONFIGURE_NOTIFICATION;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.configure_notification_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final BadgingNotificationPreferenceController badgeController =
                new BadgingNotificationPreferenceController(context);
        final PulseNotificationPreferenceController pulseController =
                new PulseNotificationPreferenceController(context);
        final LockScreenNotificationPreferenceController lockScreenNotificationController =
                new LockScreenNotificationPreferenceController(context,
                        KEY_LOCKSCREEN,
                        KEY_LOCKSCREEN_WORK_PROFILE_HEADER,
                        KEY_LOCKSCREEN_WORK_PROFILE);
        if (lifecycle != null) {
            lifecycle.addObserver(pulseController);
            lifecycle.addObserver(lockScreenNotificationController);
        }
        controllers.add(new SwipeToNotificationPreferenceController(context, lifecycle,
                KEY_SWIPE_DOWN));
        controllers.add(badgeController);
        controllers.add(pulseController);
        controllers.add(lockScreenNotificationController);
        controllers.add(new NotificationRingtonePreferenceController(context) {
            @Override
            public String getPreferenceKey() {
                return KEY_NOTI_DEFAULT_RINGTONE;
            }

        });
        return controllers;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            mRequestPreference = (RingtonePreference) preference;
            mRequestPreference.onPrepareRingtonePickerIntent(mRequestPreference.getIntent());
            startActivityForResultAsUser(
                    mRequestPreference.getIntent(),
                    REQUEST_CODE,
                    null,
                    UserHandle.of(mRequestPreference.getUserId()));
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRequestPreference != null) {
            mRequestPreference.onActivityResult(requestCode, resultCode, data);
            mRequestPreference = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRequestPreference != null) {
            outState.putString(SELECTED_PREFERENCE_KEY, mRequestPreference.getKey());
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
        = new SummaryLoader.SummaryProviderFactory() {
            @Override
            public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                    SummaryLoader summaryLoader) {
                return new NotificationApps.SummaryProvider(activity, summaryLoader);
            }
    };

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.configure_notification_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_SWIPE_DOWN);
                    keys.add(KEY_LOCKSCREEN);
                    keys.add(KEY_LOCKSCREEN_WORK_PROFILE);
                    keys.add(KEY_LOCKSCREEN_WORK_PROFILE_HEADER);
                    return keys;
                }
            };
}
