/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.Activity;
import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class ZenModePeopleSettings extends ZenModeSettingsBase implements Indexable {

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final Activity activity = getActivity();
        final Application app;
        if (activity != null) {
            app = activity.getApplication();
        } else {
            app = null;
        }
        return buildPreferenceControllers(
                context, getSettingsLifecycle(), app, this, getFragmentManager(),
                new NotificationBackend());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, Application app, Fragment host, FragmentManager fragmentManager,
            NotificationBackend notificationBackend) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModeConversationsImagePreferenceController(context,
                "zen_mode_conversations_image", lifecycle, notificationBackend));
        controllers.add(new ZenModeConversationsPreferenceController(context,
                "zen_mode_conversations", lifecycle));
        controllers.add(new ZenModeCallsPreferenceController(context, lifecycle,
                "zen_mode_people_calls"));
        controllers.add(new ZenModeMessagesPreferenceController(context, lifecycle,
                "zen_mode_people_messages"));
        controllers.add(new ZenModeSettingsFooterPreferenceController(context, lifecycle,
                fragmentManager));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_people_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DND_PEOPLE;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.zen_mode_people_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null, null, null,
                            null, null);
                }
            };
}
