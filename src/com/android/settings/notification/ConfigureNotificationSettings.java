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

import android.content.Context;
import android.os.Bundle;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.gestures.SwipeToNotificationPreferenceController;
import com.android.settings.overlay.FeatureFactory;

import java.util.ArrayList;
import java.util.List;

public class ConfigureNotificationSettings extends DashboardFragment {
    private static final String TAG = "ConfigNotiSettings";

    private LockScreenNotificationPreferenceController mLockScreenNotificationController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CONFIGURE_NOTIFICATION;
    }

    @Override
    protected String getCategoryKey() {
        return "";
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
        final List<PreferenceController> controllers = new ArrayList<>();
        final PulseNotificationPreferenceController pulseController =
                new PulseNotificationPreferenceController(context);
        mLockScreenNotificationController = new LockScreenNotificationPreferenceController(context);
        getLifecycle().addObserver(pulseController);
        getLifecycle().addObserver(mLockScreenNotificationController);
        controllers.add(new SwipeToNotificationPreferenceController(context, getLifecycle()));
        controllers.add(pulseController);
        controllers.add(mLockScreenNotificationController);
        return controllers;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        final Context context = getContext();
        if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context).isEnabled()) {
            final String prefKey = getPreferenceController(
                    SwipeToNotificationPreferenceController.class)
                    .getPreferenceKey();
            removePreference(prefKey);
        }
    }
}
