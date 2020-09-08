/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.notification.app;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.notification.AppBubbleListPreferenceController;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * App level settings for bubbles.
 */
@SearchIndexable
public class AppBubbleNotificationSettings extends NotificationSettings implements
        GlobalBubblePermissionObserverMixin.Listener {
    private static final String TAG = "AppBubNotiSettings";
    private GlobalBubblePermissionObserverMixin mObserverMixin;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APP_BUBBLE_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_bubble_notification_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = getPreferenceControllers(context, this, mDependentFieldListener);
        return new ArrayList<>(mControllers);
    }

    protected static List<NotificationPreferenceController> getPreferenceControllers(
            Context context, AppBubbleNotificationSettings fragment,
            DependentFieldListener listener) {
        List<NotificationPreferenceController> controllers = new ArrayList<>();
        controllers.add(new HeaderPreferenceController(context, fragment));
        controllers.add(new BubblePreferenceController(context, fragment != null
                ? fragment.getChildFragmentManager()
                : null,
                new NotificationBackend(), true /* isAppPage */, listener));
        controllers.add(new AppBubbleListPreferenceController(context, new NotificationBackend()));
        return controllers;
    }

    @Override
    public void onGlobalBubblePermissionChanged() {
        updatePreferenceStates();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }

        for (NotificationPreferenceController controller : mControllers) {
            controller.onResume(mAppRow, null, null, null, null, mSuspendedAppsAdmin);
            controller.displayPreference(getPreferenceScreen());
        }
        updatePreferenceStates();

        mObserverMixin = new GlobalBubblePermissionObserverMixin(getContext(), this);
        mObserverMixin.onStart();
    }

    @Override
    public void onPause() {
        mObserverMixin.onStop();
        super.onPause();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return false;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return new ArrayList<>(AppBubbleNotificationSettings.getPreferenceControllers(
                            context, null, null));
                }
            };
}
