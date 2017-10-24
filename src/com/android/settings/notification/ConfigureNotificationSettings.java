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
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.applications.NotificationApps;
import com.android.settings.applications.NotificationApps.SummaryProvider;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.gestures.SwipeToNotificationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigureNotificationSettings extends DashboardFragment {
    private static final String TAG = "ConfigNotiSettings";

    private static final String KEY_SWIPE_DOWN = "gesture_swipe_down_fingerprint_notifications";

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
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<PreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<PreferenceController> controllers = new ArrayList<>();
        final BadgingNotificationPreferenceController badgeController =
                new BadgingNotificationPreferenceController(context);
        final PulseNotificationPreferenceController pulseController =
                new PulseNotificationPreferenceController(context);
        final LockScreenNotificationPreferenceController lockScreenNotificationController =
                new LockScreenNotificationPreferenceController(context,
                        "lock_screen_notifications",
                        "lock_screen_notifications_profile_header",
                        "lock_screen_notifications_profile");
        if (lifecycle != null) {
            lifecycle.addObserver(pulseController);
            lifecycle.addObserver(lockScreenNotificationController);
        }
        controllers.add(new SwipeToNotificationPreferenceController(context, lifecycle,
                KEY_SWIPE_DOWN));
        controllers.add(badgeController);
        controllers.add(pulseController);
        controllers.add(lockScreenNotificationController);
        return controllers;
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
                public List<PreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}
